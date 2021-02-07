package catdata.aql.exp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.InteriorLabel;
import catdata.LocStr;
import catdata.Pair;
import catdata.Program;
import catdata.Raw;
import catdata.Triple;
import catdata.Unit;
import catdata.Util;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Graph;
import catdata.aql.Kind;
import catdata.graph.DMG;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public abstract class GraphExp extends Exp<Graph<String, String>> {

	@Override
	public Kind kind() {
		return Kind.GRAPH;
	}

	public Unit type(AqlTyping t) {
		return Unit.unit;
	}

	public abstract Graph<String, String> resolve(Program<Exp<?>> G);

	@Override
	public synchronized Graph<String, String> eval0(AqlEnv env, boolean isC) {
		Graph<String, String> ret = resolve(env.prog);
		return ret;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Exp<Graph<String, String>> Var(String v) {
		Exp ret = new GraphExpVar(v);
		return ret;
	}

	public static interface GraphExpVisitor<R, P, E extends Exception> {
		public abstract R visit(P params, GraphExpRaw exp) throws E;

		public abstract <N> R visit(P params, GraphExpVar exp) throws E;

		public abstract R visit(P params, GraphExpLiteral exp) throws E;
	}

	public static interface GraphExpCoVisitor<R, P, E extends Exception> {
		public abstract GraphExpRaw visitGraphExpRaw(P params, R exp) throws E;

		public abstract <N> GraphExpVar visitGraphExpVar(P params, R exp) throws E;

		public abstract GraphExpLiteral visitGraphExpLiteral(P params, R exp) throws E;
	}

	public abstract <R, P, Ex extends Exception> R accept(P params, GraphExpVisitor<R, P, Ex> v) throws Ex;

	////////////////////////////

	public static class GraphExpRaw extends GraphExp implements Raw {

		@Override
		public <R, P, E extends Exception> R accept(P param, GraphExpVisitor<R, P, E> v) throws E {
			return v.visit(param, this);
		}

		private Map<String, List<InteriorLabel<Object>>> raw = new THashMap<>();

		@Override
		public Map<String, List<InteriorLabel<Object>>> raw() {
			return raw;
		}

		@Override
		public Map<String, String> options() {
			return Collections.emptyMap();
		}

		public final Set<String> nodes;
		public final Map<String, Pair<String, String>> edges;

		public final Set<GraphExp> imports;

		public GraphExpRaw(List<LocStr> nodes, List<Pair<LocStr, Pair<String, String>>> edges, List<GraphExp> imports) {
			this.nodes = LocStr.set1(nodes);
			this.edges = Util.toMapSafely(LocStr.set2(edges));
			this.imports = new THashSet<>(imports);

			// List<InteriorLabel<Object>> t = InteriorLabel.imports( "imports", imports);
			// raw.put("imports", t);

			List<InteriorLabel<Object>> t = InteriorLabel.imports("nodes", nodes);
			raw.put("nodes", t);

			List<InteriorLabel<Object>> f = new ArrayList<>(edges.size());
			for (Pair<LocStr, Pair<String, String>> p : edges) {
				f.add(new InteriorLabel<>("edges", new Triple<>(p.first.str, p.second.first, p.second.second),
						p.first.loc, x -> x.first + " : " + x.second + " -> " + x.third).conv());
			}
			raw.put("edges", f);
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((edges == null) ? 0 : edges.hashCode());
			result = prime * result + ((imports == null) ? 0 : imports.hashCode());
			result = prime * result + ((nodes == null) ? 0 : nodes.hashCode());
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
			GraphExpRaw other = (GraphExpRaw) obj;
			if (edges == null) {
				if (other.edges != null)
					return false;
			} else if (!edges.equals(other.edges))
				return false;
			if (imports == null) {
				if (other.imports != null)
					return false;
			} else if (!imports.equals(other.imports))
				return false;
			if (nodes == null) {
				if (other.nodes != null)
					return false;
			} else if (!nodes.equals(other.nodes))
				return false;
			return true;
		}

		@Override
		public synchronized Graph<String, String> resolve(Program<Exp<?>> G) {
			Set<String> nodes = (new THashSet<>(this.nodes));
			Map<String, Pair<String, String>> edges = (new THashMap<>(this.edges));
			for (GraphExp s : imports) {
				Graph<String, String> g = s.resolve(G);
				nodes.addAll(g.dmg.nodes);
				edges.putAll(g.dmg.edges);
			}
			return new Graph<>(new DMG<>(nodes, edges));
		}

		@Override
		public String makeString() {
			if (nodes.isEmpty()) {
				return "literal {}";
			}
			final StringBuilder sb = new StringBuilder();

			List<String> l = new LinkedList<>();
			for (Object e : edges.keySet()) {
				l.add(e + ": " + edges.get(e).first + " -> " + edges.get(e).second);
			}
			return sb.append("literal {\n\tnodes\n\t\t").append(Util.sep(nodes, " ")).append("\n\tedges\n\t\t")
					.append(Util.sep(l, "\n\t\t")).append("\n}").toString();
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			Collection<Pair<String, Kind>> ret = new THashSet<>();
			for (GraphExp x : imports) {
				ret.addAll(x.deps());
			}
			return ret;
		}

		@Override
		protected void allowedOptions(Set<AqlOption> set) {

		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {

		}

	}

	////////////////////////////

	public static class GraphExpLiteral extends GraphExp {

		@Override
		public <R, P, Ex extends Exception> R accept(P param, GraphExpVisitor<R, P, Ex> v) throws Ex {
			return v.visit(param, this);
		}

		public final DMG<String, String> graph;

		@Override
		public Map<String, String> options() {
			return Collections.emptyMap();
		}

		public GraphExpLiteral(DMG<String, String> graph) {
			this.graph = graph;
		}

		@Override
		public String toString() {
			return graph.toString();
		}

		@Override
		public int hashCode() {
			return graph.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			GraphExpLiteral other = (GraphExpLiteral) obj;
			if (graph == null) {
				if (other.graph != null)
					return false;
			} else if (!graph.equals(other.graph))
				return false;
			return true;
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return Collections.emptyList();
		}

		@Override
		protected void allowedOptions(Set<AqlOption> set) {
		}

		@Override
		public Graph<String, String> resolve(Program<Exp<?>> G) {
			return new Graph<>(graph);
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {

		}

	}

	//////////////

	public static final class GraphExpVar extends GraphExp {
		public final String var;

		@Override
		public <R, P, Ex extends Exception> R accept(P param, GraphExpVisitor<R, P, Ex> v) throws Ex {
			return v.visit(param, this);
		}

		@Override
		public Map<String, String> options() {
			return Collections.emptyMap();
		}

		@Override
		public boolean isVar() {
			return true;
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return Collections.singleton(new Pair<>(var, Kind.GRAPH));
		}

		public GraphExpVar(String var) {
			this.var = var;
		}

		@Override
		public int hashCode() {
			return var.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			GraphExpVar other = (GraphExpVar) obj;
			if (var == null) {
				if (other.var != null)
					return false;
			} else if (!var.equals(other.var))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return var;
		}

		@Override
		protected void allowedOptions(Set<AqlOption> set) {
		}

		@Override
		public Graph<String, String> resolve(Program<Exp<?>> G) {
			return ((GraphExp) G.exps.get(var)).resolve(G);
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {

		}

	}

}
