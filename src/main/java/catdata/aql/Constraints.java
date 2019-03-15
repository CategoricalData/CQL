package catdata.aql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiPredicate;

import catdata.Chc;
import catdata.Pair;
import catdata.Util;
import catdata.aql.It.ID;
import catdata.aql.exp.Att;
import catdata.aql.exp.En;
import catdata.aql.exp.Fk;
import catdata.aql.exp.Sym;
import catdata.aql.exp.Ty;
//import catdata.aql.exp.InstExpRaw.Gen;
import catdata.aql.fdm.ColimitInstance;
import catdata.aql.fdm.EvalInstance;
import catdata.aql.fdm.LiteralTransform;
import catdata.aql.fdm.Row;
import catdata.graph.DMG;
import catdata.provers.KBTheory;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public class Constraints implements Semantics {

	public Set<LiteralTransform<Ty, En, Sym, Fk, Att, Var, Var, Var, Var, Integer, Chc<Var, Pair<Integer, Att>>, Integer, Chc<Var, Pair<Integer, Att>>>> asTransforms(
			Schema<Ty, En, Sym, Fk, Att> sch) {
		Set<LiteralTransform<Ty, En, Sym, Fk, Att, Var, Var, Var, Var, Integer, Chc<Var, Pair<Integer, Att>>, Integer, Chc<Var, Pair<Integer, Att>>>> ret = new THashSet<>();
		for (ED ed : eds) {
			ret.add(ed.asTransform(sch));
		}
		return ret;
	}

	public synchronized String tptp(String x, int[] i, boolean preamble,
			KBTheory<Chc<Ty, En>, Head<Ty, En, Sym, Fk, Att, Object, Object>, Var> kb) {
		StringBuffer sb = new StringBuffer();
		if (preamble) {
			sb.append(kb.tptp_preamble());
			sb.append("\n");
		}
		for (ED ed : eds) {
			sb.append(ed.tptp(x, i[0]++, kb));
			sb.append("\n");
		}
		String tptp = sb.toString();
		return tptp;
	}

	@Override
	public int size() {
		return eds.size();
	}

	public final Schema<Ty, En, Sym, Fk, Att> schema;

	public final List<ED> eds;

	@Override
	public String toString() {
		return Util.sep(eds, "\n=====================================================================\n\n");
	}

	public Constraints(Schema<Ty, En, Sym, Fk, Att> schema, List<ED> eds, AqlOptions options) {
		this.eds = desugar(eds, options);
		this.schema = schema;
	

	}

	public Constraints(AqlOptions ops, Schema<Ty, En, Sym, Fk, Att> ret) {
		this.schema = ret;
		this.eds = new ArrayList<>(schema.eqs.size() + ret.eqs.size());
		 
		
	} 

	private static List<ED> desugar(List<ED> eds, AqlOptions options) {
		List<ED> l = new ArrayList<>(eds.size() * 2);
		for (ED x : eds) {
			l.add(new ED(x.As, x.Es, x.Awh, x.Ewh, false, options));

			if (x.isUnique) {
				Map<Var, Chc<Ty, En>> es2 = Util.map(x.Es, (v, t) -> new Pair<>(Var.Var(v + "_des_0"), t));
				Map<Var, Term<Ty, En, Sym, Fk, Att, Void, Void>> subst = Util.mk();
				Set<Pair<Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>>> ewh = (new THashSet<>());

				for (Var v : x.Es.keySet()) {
					subst.put(v, Term.Var(Var.Var(v + "_des_0")));
					ewh.add(new Pair<>(Term.Var(v), subst.get(v)));
				}
				Map<Var, Chc<Ty, En>> as = new THashMap<>();
				as.putAll(x.As);
				as.putAll(x.Es);
				as.putAll(es2);
				Set<Pair<Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>>> awh = new THashSet<>(
						x.Ewh.size());
				awh.addAll(x.Awh);
				awh.addAll(x.Ewh);
				for (Pair<Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>> p : x.Ewh) {
					awh.add(new Pair<>(p.first.subst(subst), p.second.subst(subst)));
				}
				l.add(new ED(as, Collections.emptyMap(), awh, ewh, false, options));
			}

		}
		return l;
	}

	@Override
	public Kind kind() {
		return Kind.CONSTRAINTS;
	}

	public synchronized <Gen, Sk, X, Y> Instance<Ty, En, Sym, Fk, Att, ?, ?, X, ?> chase(
			Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> I, AqlOptions options) {
		Boolean b = (Boolean) options.getOrDefault(AqlOptions.AqlOption.allow_java_eqs_unsafe);
		for (ED ed : eds) {
			Frozen<Ty, En, Sym, Fk, Att> f = ed.getQ(schema).ens.get(ED.FRONT);
			if (!b) {
				if (!f.algebra().hasFreeTypeAlgebraOnJava()) {
					throw new RuntimeException(
							"Without allow_java_eqs_unsafe=true, Cannot chase, unsafe use of java in front of\n" + ed);
				}
			}
			f = ed.getQ(schema).ens.get(ED.BACK);
			if (!b) {
				if (!f.algebra().hasFreeTypeAlgebraOnJava()) {
					throw new RuntimeException(
							"Without allow_java_eqs_unsafe=true, Cannot chase, unsafe use of java in back of\n" + ed);
				}
			}
		}
		Instance<Ty, En, Sym, Fk, Att, ?, ?, X, ?> ret = I;
		for (;;) {
			Instance<Ty, En, Sym, Fk, Att, ?, ?, X, ?> ret2 = (Instance<Ty, En, Sym, Fk, Att, ?, ?, X, ?>) step(ret,
					options);

			if (ret2 == null) {
				return ret;
			}
			ret = ret2;
		}
	}

	public synchronized <Gen, Sk, X, Y> Collection<Pair<ED, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>>>> triggers(
			Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> I, AqlOptions options) {
		Collection<Pair<ED, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>>>> T = new LinkedList<>();

		BiPredicate<Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>> fn = (a,
				e) -> {
			if (a.left != e.left) {
				Util.anomaly();
			}
			if (a.left) {
				return a.l.equals(e.l);
			}
			return I.dp().eq(null, a.r, e.r);
		};
		for (ED ed : eds) {
			Query<Ty, En, Sym, Fk, Att, En, Fk, Att> Q = ed.getQ(schema);
			EvalInstance<Ty, En, Sym, Fk, Att, Gen, Sk, En, Fk, Att, X, Y> QI = new EvalInstance<>(Q, I, options);
			outer: for (Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>> e : QI.algebra().en(ED.FRONT)) {
				for (Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>> a : QI.algebra().en(ED.BACK)) {

					Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>> aa = QI.algebra().fk(ED.UNIT, a);
					if (aa.rowEquals(fn, e)) {
						continue outer;
					}

				}
				// System.out.println(ed.hashCode() + " and " + e + " " +
				// Thread.currentThread());
				T.add(new Pair<>(ed, e));
			}
		}
		return T;
	}

	public static enum THREE {
		A, B, C
	}

	public static enum TWO {
		A, B
	}

	public static <Ty, En, Sym, Fk, Att, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2, Gen3, Sk3, X3, Y3> ColimitInstance<THREE, TWO, Ty, En, Sym, Fk, Att, ?, ?, ?, ?> pushout(
			Transform<Ty, En, Sym, Fk, Att, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> j,
			Transform<Ty, En, Sym, Fk, Att, Gen1, Sk1, Gen3, Sk3, X1, Y1, X3, Y3> k, AqlOptions options) {
		if (!j.src().equals(k.src())) {
			throw new RuntimeException("Source of \n" + j + "\nnamely \n" + j.src() + "\n is not equal to source of\n"
					+ k + "\nnamely \n" + k.src());
		}
		Set<THREE> ns = new THashSet<>();
		ns.add(THREE.A);
		ns.add(THREE.B);
		ns.add(THREE.C);
		Map<TWO, Pair<THREE, THREE>> es = Util.mk();
		es.put(TWO.A, new Pair<>(THREE.A, THREE.B));
		es.put(TWO.B, new Pair<>(THREE.A, THREE.C));
		DMG<THREE, TWO> shape = new DMG<>(ns, es);

		Map<THREE, Instance<Ty, En, Sym, Fk, Att, ?, ?, ?, ?>> nodes = Util.mk();
		nodes.put(THREE.A, j.src());
		nodes.put(THREE.B, j.dst());
		nodes.put(THREE.C, k.dst());

		Map<TWO, Transform<Ty, En, Sym, Fk, Att, ?, ?, ?, ?, ?, ?, ?, ?>> edges = Util.mk();
		edges.put(TWO.A, j);
		edges.put(TWO.B, k);

		@SuppressWarnings({ "unchecked", "rawtypes" })
		ColimitInstance<THREE, TWO, Ty, En, Sym, Fk, Att, ?, ?, ?, ?> ret = new ColimitInstance(j.src().schema(), shape,
				nodes, edges, options);
		// System.out.println("-------------------------------");
		// System.out.println(ret);
		return ret;
	}

	public synchronized <Gen, Sk, X, Y> Instance<Ty, En, Sym, Fk, Att, ?, ?, ?, ?> step(
			Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> I, AqlOptions options) {
		Collection<Pair<ED, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>>>> T = triggers(I, options);
		if (T.isEmpty()) {
			return null;
		}

		DMG<Pair<ED, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>>>, Void> shape = new DMG<>(T,
				new THashMap<>());
		Map<Pair<ED, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>>>, Instance<Ty, En, Sym, Fk, Att, Var, Var, ID, Chc<Var, Pair<ID, Att>>>> nodesA = Util
				.mk(), nodesE = Util.mk();

		Map<Pair<Pair<ED, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>>>, Var>, Term<Void, En, Void, Fk, Void, Gen, Void>> aaa = Util
				.mk();
		Map<Pair<Pair<ED, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>>>, Var>, Term<Void, En, Void, Fk, Void, Pair<Pair<ED, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>>>, Var>, Void>> xxx = Util
				.mk();

		Map<Pair<Pair<ED, Row<catdata.aql.exp.En, Chc<X, Term<catdata.aql.exp.Ty, catdata.aql.exp.En, catdata.aql.exp.Sym, catdata.aql.exp.Fk, catdata.aql.exp.Att, Gen, Sk>>>>, Var>, Term<catdata.aql.exp.Ty, catdata.aql.exp.En, catdata.aql.exp.Sym, catdata.aql.exp.Fk, catdata.aql.exp.Att, Gen, Sk>> bbb = Util
				.mk();
		Map<Pair<Pair<ED, Row<catdata.aql.exp.En, Chc<X, Term<catdata.aql.exp.Ty, catdata.aql.exp.En, catdata.aql.exp.Sym, catdata.aql.exp.Fk, catdata.aql.exp.Att, Gen, Sk>>>>, Var>, Term<catdata.aql.exp.Ty, catdata.aql.exp.En, catdata.aql.exp.Sym, catdata.aql.exp.Fk, catdata.aql.exp.Att, Pair<Pair<ED, Row<catdata.aql.exp.En, Chc<X, Term<catdata.aql.exp.Ty, catdata.aql.exp.En, catdata.aql.exp.Sym, catdata.aql.exp.Fk, catdata.aql.exp.Att, Gen, Sk>>>>, Var>, Pair<Pair<ED, Row<catdata.aql.exp.En, Chc<X, Term<catdata.aql.exp.Ty, catdata.aql.exp.En, catdata.aql.exp.Sym, catdata.aql.exp.Fk, catdata.aql.exp.Att, Gen, Sk>>>>, Var>>> yyy = Util
				.mk();

		for (Pair<ED, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>>> t : T) {
			Query<Ty, En, Sym, Fk, Att, En, Fk, Att> Q = t.first.getQ(schema);
			Instance<Ty, En, Sym, Fk, Att, Var, Var, ID, Chc<Var, Pair<ID, Att>>> A = Q.ens.get(ED.FRONT);
			Instance<Ty, En, Sym, Fk, Att, Var, Var, ID, Chc<Var, Pair<ID, Att>>> E = Q.ens.get(ED.BACK);
			Transform<Ty, En, Sym, Fk, Att, Var, Var, Var, Var, ID, Chc<Var, Pair<ID, Att>>, ID, Chc<Var, Pair<ID, Att>>> AE = Q.fks
					.get(ED.UNIT);

			nodesA.put(t, A);
			nodesE.put(t, E);
			for (Entry<Var, Term<Void, En, Void, Fk, Void, Var, Void>> vv : AE.gens().entrySet()) {
				Var v = vv.getKey();
				En en = t.first.As.get(v).r;
				Term<Void, En, Void, Fk, Void, Var, Void> xx = vv.getValue();
				Pair<Pair<ED, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>>>, Var> p = new Pair<>(t, v);
				Term<Void, En, Void, Fk, Void, Pair<Pair<ED, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>>>, Var>, Void> ww = xx
						.mapGen(v0 -> new Pair<>(t, v0));
				xxx.put(p, ww);
				aaa.put(p, I.algebra().repr(en, t.second.get(v).l));
			}
			for (Entry<Var, Term<Ty, En, Sym, Fk, Att, Var, Var>> vv : AE.sks().entrySet()) {
				Var v = vv.getKey();
				// Ty en = t.first.As.get(v).l;
				Term<Ty, En, Sym, Fk, Att, Var, Var> xx = vv.getValue();
				Pair<Pair<ED, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>>>, Var> p = new Pair<>(t, v);
				yyy.put(p, xx.mapGenSk(v0 -> new Pair<>(t, v0), v0 -> new Pair<>(t, v0)));
				bbb.put(p, t.second.get(v).r);
			}
		}

		ColimitInstance<Pair<ED, Row<catdata.aql.exp.En, Chc<X, Term<catdata.aql.exp.Ty, catdata.aql.exp.En, catdata.aql.exp.Sym, catdata.aql.exp.Fk, catdata.aql.exp.Att, Gen, Sk>>>>, Void, catdata.aql.exp.Ty, catdata.aql.exp.En, catdata.aql.exp.Sym, catdata.aql.exp.Fk, catdata.aql.exp.Att, Var, Var, ID, Chc<Var, Pair<ID, catdata.aql.exp.Att>>> A0 = new ColimitInstance<>(
				schema, shape, nodesA, Collections.emptyMap(), options);

		ColimitInstance<Pair<ED, Row<catdata.aql.exp.En, Chc<X, Term<catdata.aql.exp.Ty, catdata.aql.exp.En, catdata.aql.exp.Sym, catdata.aql.exp.Fk, catdata.aql.exp.Att, Gen, Sk>>>>, Void, catdata.aql.exp.Ty, catdata.aql.exp.En, catdata.aql.exp.Sym, catdata.aql.exp.Fk, catdata.aql.exp.Att, Var, Var, ID, Chc<Var, Pair<ID, catdata.aql.exp.Att>>> E0 = new ColimitInstance<>(
				schema, shape, nodesE, Collections.emptyMap(), options);

		LiteralTransform<catdata.aql.exp.Ty, catdata.aql.exp.En, catdata.aql.exp.Sym, catdata.aql.exp.Fk, catdata.aql.exp.Att, Pair<Pair<ED, Row<catdata.aql.exp.En, Chc<X, Term<catdata.aql.exp.Ty, catdata.aql.exp.En, catdata.aql.exp.Sym, catdata.aql.exp.Fk, catdata.aql.exp.Att, Gen, Sk>>>>, Var>, Pair<Pair<ED, Row<catdata.aql.exp.En, Chc<X, Term<catdata.aql.exp.Ty, catdata.aql.exp.En, catdata.aql.exp.Sym, catdata.aql.exp.Fk, catdata.aql.exp.Att, Gen, Sk>>>>, Var>, Gen, Sk, Integer, Chc<Pair<Pair<ED, Row<catdata.aql.exp.En, Chc<X, Term<catdata.aql.exp.Ty, catdata.aql.exp.En, catdata.aql.exp.Sym, catdata.aql.exp.Fk, catdata.aql.exp.Att, Gen, Sk>>>>, Var>, Pair<Integer, catdata.aql.exp.Att>>, X, Y> A0I = new LiteralTransform<>(
				aaa, bbb, A0, I, false);

		LiteralTransform<catdata.aql.exp.Ty, catdata.aql.exp.En, catdata.aql.exp.Sym, catdata.aql.exp.Fk, catdata.aql.exp.Att, Pair<Pair<ED, Row<catdata.aql.exp.En, Chc<X, Term<catdata.aql.exp.Ty, catdata.aql.exp.En, catdata.aql.exp.Sym, catdata.aql.exp.Fk, catdata.aql.exp.Att, Gen, Sk>>>>, Var>, Pair<Pair<ED, Row<catdata.aql.exp.En, Chc<X, Term<catdata.aql.exp.Ty, catdata.aql.exp.En, catdata.aql.exp.Sym, catdata.aql.exp.Fk, catdata.aql.exp.Att, Gen, Sk>>>>, Var>, Pair<Pair<ED, Row<catdata.aql.exp.En, Chc<X, Term<catdata.aql.exp.Ty, catdata.aql.exp.En, catdata.aql.exp.Sym, catdata.aql.exp.Fk, catdata.aql.exp.Att, Gen, Sk>>>>, Var>, Pair<Pair<ED, Row<catdata.aql.exp.En, Chc<X, Term<catdata.aql.exp.Ty, catdata.aql.exp.En, catdata.aql.exp.Sym, catdata.aql.exp.Fk, catdata.aql.exp.Att, Gen, Sk>>>>, Var>, Integer, Chc<Pair<Pair<ED, Row<catdata.aql.exp.En, Chc<X, Term<catdata.aql.exp.Ty, catdata.aql.exp.En, catdata.aql.exp.Sym, catdata.aql.exp.Fk, catdata.aql.exp.Att, Gen, Sk>>>>, Var>, Pair<Integer, catdata.aql.exp.Att>>, Integer, Chc<Pair<Pair<ED, Row<catdata.aql.exp.En, Chc<X, Term<catdata.aql.exp.Ty, catdata.aql.exp.En, catdata.aql.exp.Sym, catdata.aql.exp.Fk, catdata.aql.exp.Att, Gen, Sk>>>>, Var>, Pair<Integer, catdata.aql.exp.Att>>> A0E0 = new LiteralTransform<>(
				xxx, yyy, A0, E0, false);

		return pushout(A0E0, A0I, options);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((eds == null) ? 0 : eds.hashCode());
		result = prime * result + ((schema == null) ? 0 : schema.hashCode());
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
		Constraints other = (Constraints) obj;
		if (eds == null) {
			if (other.eds != null)
				return false;
		} else if (!eds.equals(other.eds))
			return false;
		if (schema == null) {
			if (other.schema != null)
				return false;
		} else if (!schema.equals(other.schema))
			return false;
		return true;
	};

}
