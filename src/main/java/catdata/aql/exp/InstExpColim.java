package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.collections4.list.TreeList;

import catdata.Chc;
import catdata.Pair;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Instance;
import catdata.aql.Kind;
import catdata.aql.Transform;
import catdata.aql.fdm.ColimitInstance;
import catdata.graph.DMG;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public class InstExpColim<N, E, Gen, Sk, X, Y>
		extends InstExp<Pair<N, Gen>, Pair<N, Sk>, Integer, Chc<Pair<N, Sk>, Pair<Integer, Att>>>
		implements Raw {
	
	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		schema.map(f);
		shape.map(f);
		for (InstExp<Gen, Sk, X, Y> x : nodes.values()) {
			x.map(f);
		}
		for (TransExp<Gen, Sk, Gen, Sk, X, Y, X, Y> x : edges.values()) {
			x.map(f);
		}
	}

	public <R, P, E extends Exception> R accept(P param, InstExpVisitor<R, P, E> v) throws E {
		return v.visit(param, this);
	}

	private Map<String, List<InteriorLabel<Object>>> raw = new THashMap<>(Collections.emptyMap());

	@SuppressWarnings("unchecked")
	@Override
	public Collection<InstExp<?, ?, ?, ?>> direct(AqlTyping G) {
		return (Collection<InstExp<?, ?, ?, ?>>) ((Object) nodes.values());
	}

	@Override
	public Map<String, List<InteriorLabel<Object>>> raw() {
		return raw;
	}

	public final SchExp schema;

	public final GraphExp<N, E> shape;

	public final Map<N, InstExp<Gen, Sk, X, Y>> nodes;
	public final Map<E, TransExp<Gen, Sk, Gen, Sk, X, Y, X, Y>> edges;

	public final Map<String, String> options;

	@Override
	public Map<String, String> options() {
		return options;
	}

	@SuppressWarnings("unchecked")
	public InstExpColim(GraphExp<N, E> shape, SchExp schema,
			List<Pair<LocStr, InstExp< Gen, Sk, X, Y>>> nodes,
			List<Pair<LocStr, TransExp<Gen, Sk, Gen, Sk, X, Y, X, Y>>> edges,
			List<Pair<String, String>> options) {
		this.schema = schema;
		this.shape = shape;
		this.nodes = Util.toMapSafely(LocStr.list2(nodes, x -> (N) x));
		this.edges = Util.toMapSafely(LocStr.list2(edges, x -> (E) x));
		this.options = Util.toMapSafely(options);

		List<InteriorLabel<Object>> f = new TreeList<>();
		for (Pair<LocStr, InstExp<Gen, Sk, X, Y>> p : nodes) {
			f.add(new InteriorLabel<>("nodes", new Pair<>(p.first.str, p.second), p.first.loc,
					x -> x.first + " -> " + x.second).conv());
		}
		raw.put("nodes", f);
		

		f = new LinkedList<>();
		for (Pair<LocStr, TransExp<Gen, Sk, Gen, Sk, X, Y, X, Y>> p : edges) {
			f.add(new InteriorLabel<>("edges", new Pair<>(p.first.str, p.second), p.first.loc,
					x -> x.first + " -> " + x.second).conv());
		}
		raw.put("edges", f);
	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((edges == null) ? 0 : edges.hashCode());
		result = prime * result + ((nodes == null) ? 0 : nodes.hashCode());
		result = prime * result + ((options == null) ? 0 : options.hashCode());
		result = prime * result + ((schema == null) ? 0 : schema.hashCode());
		result = prime * result + ((shape == null) ? 0 : shape.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		InstExpColim other = (InstExpColim) obj;
		if (edges == null) {
			if (other.edges != null)
				return false;
		} else if (!edges.equals(other.edges))
			return false;
		if (nodes == null) {
			if (other.nodes != null)
				return false;
		} else if (!nodes.equals(other.nodes))
			return false;
		if (options == null) {
			if (other.options != null)
				return false;
		} else if (!options.equals(other.options))
			return false;
		if (schema == null) {
			if (other.schema != null)
				return false;
		} else if (!schema.equals(other.schema))
			return false;
		if (shape == null) {
			if (other.shape != null)
				return false;
		} else if (!shape.equals(other.shape))
			return false;
		return true;
	}

	@Override
	public String makeString() {
		StringBuilder sb = new StringBuilder().append("colimit ").append(shape).append(" ").append(schema)
				.append(" {");
		if (!nodes.isEmpty()) {
			sb.append("\n\tnodes\n\t\t").append(Util.sep(nodes, " -> ", "\n\t\t")).append("\n\tedges\n\t\t")
					.append(Util.sep(edges, " -> ", "\n\t\t"));
		}
		return sb.append("}").toString();
	}

	@Override
	public synchronized Instance<Ty, En, Sym, Fk, Att, Pair<N, Gen>, Pair<N, Sk>, Integer, Chc<Pair<N, Sk>, Pair<Integer, Att>>> eval0(
			AqlEnv env, boolean isC) {
		Map<N, Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y>> nodes0 = Util.mk();
		Map<E, Transform<Ty, En, Sym, Fk, Att, Gen, Sk, Gen, Sk, X, Y, X, Y>> edges0 = Util.mk();

		for (N n : nodes.keySet()) {
			nodes0.put(n, nodes.get(n).eval(env, isC));
		}
		for (E e : edges.keySet()) {
			edges0.put(e, edges.get(e).eval(env, isC));
		}
		
		if (isC) {
			throw new IgnoreException();
		}

		return new ColimitInstance(schema.eval(env, false), shape.eval(env, false).dmg, nodes0, edges0,
				new AqlOptions(options, null, env.defaults));
	}

	@Override
	public SchExp type(AqlTyping G) {
		for (N n : nodes.keySet()) {
			if (!G.eq(nodes.get(n).type(G), schema)) { 
				throw new RuntimeException("The instance for " + n + " has schema " + nodes.get(n).type(G)
						+ ", not " + schema + " as expected");
			}
		}
		if (!(Boolean) new AqlOptions(options, null, G.prog.options).getOrDefault(AqlOption.static_typing)) {
			return schema;
		}
		
		DMG<N, E> g = shape.resolve(G.prog).dmg;

		for (E e : g.edges.keySet()) {
			InstExp< Gen, Sk, X, Y> reqdSrc = nodes.get(g.edges.get(e).first);
			InstExp< Gen, Sk, X, Y> reqdDst = nodes.get(g.edges.get(e).second);

			InstExp< Gen, Sk, X, Y> givenSrc = edges.get(e).type(G).first,
					givenDst = edges.get(e).type(G).second;

			if (!reqdSrc.equals(givenSrc)) {
				throw new RuntimeException("On " + e + ", its source is " + givenSrc + " but should be " + reqdSrc);
			} else if (!reqdDst.equals(givenDst)) {
				throw new RuntimeException("On " + e + ", its target is " + givenDst + " but should be " + reqdDst);
			}
		}

		return schema;
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		Collection<Pair<String, Kind>> ret = new THashSet<>();
		ret.addAll(schema.deps());
		ret.addAll(shape.deps());
		for (InstExp<Gen, Sk, X, Y> p : nodes.values()) {
			ret.addAll(p.deps());
		}
		for (TransExp<Gen, Sk, Gen, Sk, X, Y, X, Y> p : edges.values()) {
			ret.addAll(p.deps());
		}
		return ret;
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
		set.add(AqlOption.require_consistency);
		set.add(AqlOption.allow_java_eqs_unsafe);
		set.addAll(AqlOptions.proverOptionNames());
	}

}