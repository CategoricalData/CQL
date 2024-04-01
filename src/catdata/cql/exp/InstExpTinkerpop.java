package catdata.cql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.apache.tinkerpop.gremlin.structure.util.reference.ReferenceProperty;

import catdata.Null;
import catdata.Pair;
import catdata.Util;
import catdata.cql.AqlOptions;
import catdata.cql.Instance;
import catdata.cql.Kind;
import catdata.cql.Schema;
import catdata.cql.Term;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.fdm.ImportAlgebra;
import catdata.cql.fdm.SaturatedInstance;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public class InstExpTinkerpop extends InstExp<String, Null<?>, String, Null<?>> {

	private final Map<String, String> options;

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
	}

	public <R, P, E extends Exception> R accept(P param, InstExpVisitor<R, P, E> v) throws E {
		return v.visit(param, this);
	}

	@Override
	public Collection<InstExp<?, ?, ?, ?>> direct(AqlTyping G) {
		return Collections.emptySet();
	}

	@Override
	public Map<String, String> options() {
		return options;
	}

	public InstExpTinkerpop(List<Pair<String, String>> options) {
		this.options = Util.toMapSafely(options);
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
		set.add(AqlOption.tinkerpop_host);
		set.add(AqlOption.tinkerpop_port);
		set.add(AqlOption.tinkerpop_graph_name);
	}

	public Schema<String, String, Sym, Fk, Att> makeSchema(AqlEnv env, AqlOptions ops) {
		return makeSch().eval(env, false);
	}

	@Override
	public synchronized Instance<String, String, Sym, Fk, Att, String, Null<?>, String, Null<?>> eval0(AqlEnv env,
			boolean isC) {
		if (isC) {
			throw new IgnoreException();
		}
		
		AqlOptions ops = new AqlOptions(options, env.defaults);
		Schema<String, String, Sym, Fk, Att> sch = makeSchema(env, ops);
		String gname = (String) ops.getOrDefault(AqlOption.tinkerpop_graph_name);
		
		Map<String, Collection<String>> ens0 = new THashMap<>(sch.ens.size(), 2);
		Map<String, Map<String, Map<Fk, String>>> fks0x = new THashMap<>(sch.ens.size(), 2);
		Map<String, Map<String, Map<Att, Term<String, Void, Sym, Void, Void, Void, Null<?>>>>> atts0x = new THashMap<>(
				sch.ens.size(), 2);
		Map<String, Collection<Null<?>>> tys0 = new THashMap<>(sch.typeSide.tys.size(), 2);
		Map<Vertex, Integer> ids = new THashMap<>(4096, 1.0f);
		Map<Object, Integer> ids0 = new THashMap<>(4096, 1.0f);
		Map<Edge, Integer> idsE = new THashMap<>(4096, 1.0f);
		Map<Object, Integer> ids0E = new THashMap<>(4096, 1.0f);
//		Map<Property<?>, Integer> idsP = new THashMap<>(4096, 1.0f);
//		Map<Object, Integer> ids0P = new THashMap<>(4096, 1.0f);

		for (String en : sch.ens) {
	//		if (en.equals("Property")) {
		//		ens0.put(en, new THashSet<>(4096, 1.0f));
			//} else {
				ens0.put(en, new LinkedList<>());
		//	}
			fks0x.put(en, new THashMap<>(4096, 1.0f));
			atts0x.put(en, new THashMap<>(4096, 1.0f));
		}
		for (String ty : sch.typeSide.tys) {
			tys0.put(ty, new LinkedList<>());
		}
		int i = 0;
		try {
			List<String> cmds = new LinkedList<>();
			cmds.add(gname + ".V()");
			List<Object> results = PragmaExpTinkerpop.execGremlin(ops, cmds);
			Object o = results.get(0);
			GraphTraversal<?, ?> t = (GraphTraversal<?, ?>) o;
			Collection<String> vertices = ens0.get("Vertex");
			Map<String, Map<Att, Term<String, Void, Sym, Void, Void, Void, Null<?>>>> m = atts0x.get("Vertex");
			for (Object x : t.toList()) {
				Vertex v = (Vertex) x;
				Object id = v.id();
				String j = Integer.toString(i);
				ids.put(v, i);
				ids0.put(id, i);
				vertices.add(j);
				Map<Att, Term<String, Void, Sym, Void, Void, Void, Null<?>>> n = new THashMap<>(2, 1);
				n.put(Att.Att("Vertex", "id"), Term.Obj(id, "Dom"));
				m.put(j, n);

				String l = v.label();
				n.put(Att.Att("Vertex", "label"), Term.Obj(l == null ? Optional.empty() : Optional.of(l), "Dom"));

				i++;
			}
			t.close();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

		try {
			List<String> cmds = new LinkedList<>();
			cmds.add(gname + ".E()");
			List<Object> results = PragmaExpTinkerpop.execGremlin(ops, cmds);
			Object o = results.get(0);
			GraphTraversal<?, ?> t = (GraphTraversal<?, ?>) o;
			Collection<String> edges = ens0.get("Edge");
			Map<String, Map<Att, Term<String, Void, Sym, Void, Void, Void, Null<?>>>> m = atts0x.get("Edge");
			Map<String, Map<Fk, String>> m2 = fks0x.get("Edge");

			for (Object x : t.toList()) {
				Edge v = (Edge) x;
				Object id = v.id();
				String j = Integer.toString(i);
				idsE.put(v, i);
				ids0E.put(id, i);
				edges.add(j);
				Map<Att, Term<String, Void, Sym, Void, Void, Void, Null<?>>> n = new THashMap<>(2, 1);
				n.put(Att.Att("Edge", "id"), Term.Obj(id, "Dom"));
				m.put(j, n);

				String l = v.label();
				n.put(Att.Att("Edge", "label"), Term.Obj(l == null ? Optional.empty() : Optional.of(l), "Dom"));

				String inGen = Integer.toString(ids.get(v.inVertex()));
				String outGen = Integer.toString(ids.get(v.outVertex()));
				Map<Fk, String> n2 = new THashMap<>(4, 1);
				n2.put(Fk.Fk("Edge", "in"), inGen);
				n2.put(Fk.Fk("Edge", "out"), outGen);

				m2.put(j, n2);

				i++;
			}
			t.close();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		
		

		try {
			List<String> cmds = new LinkedList<>();
			cmds.add(gname + ".V().elementMap()");
			List<Object> results = PragmaExpTinkerpop.execGremlin(ops, cmds);
			Object o = results.get(0);
			GraphTraversal<?, ?> t = (GraphTraversal<?, ?>) o;

			for (Object x : t.toList()) {
				Map<?, ?> v = (Map<?, ?>) x;
				Object ii = v.get(org.apache.tinkerpop.gremlin.structure.T.id);
				String idX = Integer.toString(ids0.get(ii));
			//	System.out.println("----");

				for (Entry<?, ?> kv : v.entrySet()) {
					if (kv.getKey().toString().equals("id") || kv.getKey().toString().equals("label")) {
						continue;
					}
//					System.out.println(kv.getKey().getClass() + " " + kv.getKey());
					
					Collection<String> hvp = ens0.get("HasVertexProperty");
					String xxx = Integer.toString(i);
					hvp.add(xxx);
					Map<Att, Term<String, Void, Sym, Void, Void, Void, Null<?>>> mP = new THashMap<>(2, 1);
					Map<Fk, String> mQ = new THashMap<>(2, 1);
					atts0x.get("HasVertexProperty").put(xxx, mP);
					fks0x.get("HasVertexProperty").put(xxx, mQ);
					//System.out.println(kv.getKey());
					mP.put(Att.Att("HasVertexProperty", "value"), Term.Obj(kv.getValue(), "Dom"));
					mP.put(Att.Att("HasVertexProperty", "key"),  Term.Obj(kv.getKey(), "Dom"));
					
					mQ.put(Fk.Fk("HasVertexProperty", "vertex"), idX);

					i++;

				}
			}
			t.close();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

		try {
			List<String> cmds = new LinkedList<>();
			cmds.add(gname + ".E().elementMap()");
			List<Object> results = PragmaExpTinkerpop.execGremlin(ops, cmds);
			Object o = results.get(0);
			GraphTraversal<?, ?> t = (GraphTraversal<?, ?>) o;
			
			for (Object x : t.toList()) {
				Map<?, ?> v = (Map<?, ?>) x;
				Object ii = v.get(org.apache.tinkerpop.gremlin.structure.T.id);
				String idX = Integer.toString(ids0E.get(ii));
				for (Entry<?, ?> kv : v.entrySet()) {
					String pn = kv.getKey().toString();
					if (pn.equals("id") || pn.equals("label") || pn.equals("IN") || pn.equals("OUT")) {
						continue;
					}
					
					Collection<String> hvp = ens0.get("HasEdgeProperty");
					String xxx = Integer.toString(i);
					hvp.add(xxx);
					Map<Att, Term<String, Void, Sym, Void, Void, Void, Null<?>>> mP = new THashMap<>(2, 1);
					Map<Fk, String> mQ = new THashMap<>(2, 1);
					fks0x.get("HasEdgeProperty").put(xxx, mQ);
					atts0x.get("HasEdgeProperty").put(xxx, mP);
					mP.put(Att.Att("HasEdgeProperty", "value"), Term.Obj(kv.getValue(), "Dom"));
					mP.put(Att.Att("HasEdgeProperty", "key"), Term.Obj(kv.getKey(), "Dom"));

					mQ.put(Fk.Fk("HasEdgeProperty", "edge"), idX);

					i++;

				}
			}
			t.close();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

		ImportAlgebra<String, String, Sym, Fk, Att, String, Null<?>> alg = new ImportAlgebra<>(sch, x -> ens0.get(x), tys0,
				(en, x) -> fks0x.get(en).get(x), (en, x) -> atts0x.get(en).get(x), (x, y) -> y, (x, y) -> y, true,
				Collections.emptySet());

		alg.validateMore();

		return new SaturatedInstance<>(alg, alg, (Boolean) ops.getOrDefault(AqlOption.require_consistency),
				(Boolean) ops.getOrDefault(AqlOption.allow_java_eqs_unsafe), true, null);

	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder().append("import_tinkerpop_all ");
		if (!options.isEmpty()) {
			sb.append(" {\n\t").append("\n\toptions\n\t\t").append(Util.sep(options, " = ", "\n\t\t")).append("}");
		}
		return sb.toString().trim();
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return Collections.emptySet();
	}

	@Override
	public SchExp type(AqlTyping G) {
		return new SchExpTinkerpop();
	}

	public static SchExpRaw makeSch() {
		String s = """
					literal : rdf {
					  entities
					    Vertex Edge HasVertexProperty HasEdgeProperty 
					  foreign_keys
					    in out : Edge -> Vertex
					    vertex : HasVertexProperty -> Vertex
					    edge : HasEdgeProperty -> Edge
					  attributes
					    key value : HasEdgeProperty -> Dom
					    key value : HasVertexProperty -> Dom
					    id "label" : Vertex -> Dom
					    id "label" : Edge -> Dom
				}""";
		return CombinatorParser.parseSchExpRaw(s);
	}

	public static EdsExpRaw makeEds() {
		String s = """
				literal : tinkerpop {
					   forall v1 v2 : Vertex where v1.id = v2.id -> where v1 = v2
				       forall e1 e2 : Edge where e1.id = e2.id -> where e1 = e2
				       }      """;
		return CombinatorParser.parseEdsExpRaw(s);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((options == null) ? 0 : options.hashCode());
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
		InstExpTinkerpop other = (InstExpTinkerpop) obj;
		if (options == null) {
			if (other.options != null)
				return false;
		} else if (!options.equals(other.options))
			return false;
		return true;
	}

}
