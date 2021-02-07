package catdata.aql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
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
//import InstExpRaw.Gen;
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

	/*
	 * public synchronized <X,Y> String tptp( KBTheory<Chc<Ty, En>, Head<Ty, En,
	 * Sym, Fk, Att, X, Y>, Var> kb) { List<String> lll = new LinkedList<>(); for
	 * (ED ed : eds) { lll.add(ed.tptpX(kb)); } return "fof(thegoal,conjecture,(" +
	 * (lll.isEmpty() ? "$true" : Util.sep(lll, " & ")) + "))."; }
	 */

	static int i = 0;

	public synchronized <X, Y> String tptpSorted(String x, Schema<Ty, En, Sym, Fk, Att> kb) {
		StringBuffer sb = new StringBuffer();

		for (ED ed : eds) {
			sb.append("tff(ed" + i++ + "," + x + ",(" + ed.tptpXSorted(kb) + ")).\n");
		}
		String tptp = sb.toString();
		return tptp;
	}

	public synchronized <X, Y> String tptp(String x, boolean preamble,
			KBTheory<Chc<Ty, En>, Head<Ty, En, Sym, Fk, Att, X, Y>, Var> kb) {
		StringBuffer sb = new StringBuffer();
		if (preamble) {
			sb.append(kb.tptp_preamble());
			sb.append("\n");
		}
		for (ED ed : eds) {
			sb.append(ed.tptp(x, KBTheory.j++, kb));
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
		return Util.sep(eds, "\n\n");
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
			if (x.Ewh.isEmpty() && x.Es.isEmpty()) {
				// continue;
			}

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
		// System.out.println("----");
		int k = 0;
		Instance<Ty, En, Sym, Fk, Att, ?, ?, X, ?> ret = I;
		for (;;) {
			@SuppressWarnings("unchecked")
			Instance<Ty, En, Sym, Fk, Att, ?, ?, X, ?> ret2 = ((Instance<Ty, En, Sym, Fk, Att, ?, ?, X, ?>) step(ret,
					options));

			if (ret2 == null) {
				return ret;
			}
			ret2.validateMore();
//			System.out.println(ret2.size());
//			System.out.println("------------------------------");

//			System.out.println(ret2);
//			System.out.println(ret2.algebra());

			ret = ret2;
		}
	}

	public synchronized <Gen, Sk, X, Y> Collection<Pair<Integer, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>, Chc<En,Ty>>>> triggers(
			Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> I, AqlOptions options) {
		Collection<Pair<Integer, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>, Chc<En,Ty>>>> T = new LinkedList<>();

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
		int i = 0;
		int j = 0;
		// System.out.println(I);
		// System.out.println(I.algebra());
		for (ED ed : eds) {
			i++;
			Query<Ty, En, Sym, Fk, Att, En, Fk, Att> Q = ed.getQ(schema);
			EvalInstance<Ty, En, Sym, Fk, Att, Gen, Sk, En, Fk, Att, X, Y> QI = new EvalInstance<>(Q, I, options);
			// System.out.println(Q);
			// System.out.println(QI);
			// System.out.println(QI.algebra());

			outer: for (Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>, Chc<En,Ty>> e : QI.algebra().en(ED.FRONT)) {
				// System.out.println("Consider " + e);
				for (Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>, Chc<En,Ty>> a : QI.algebra().en(ED.BACK)) {

					Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>, Chc<En,Ty>> aa = QI.algebra().fk(ED.UNIT, a);
					// System.out.println("try " + a);
					if (aa.rowEquals(fn, e)) {
						// System.out.println("hit");
						continue outer;
					}
					// System.out.println("Consider " + aa + " and " + e + ": miss");
				}
				// System.out.println("miss");
				T.add(new Pair<>(i - 1, e));
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

		/*
		 * System.out.println(j.src()); System.out.println(j.dst());
		 * System.out.println(j); System.out.println("&&0");
		 * System.out.println(k.src()); System.out.println(k.dst());
		 * System.out.println(k); System.out.println("&&1"); System.out.println(ret);
		 * System.out.println("&&2");
		 */

		return ret;
	}

	public synchronized <Gen, Sk, X, Y> Instance<Ty, En, Sym, Fk, Att, ?, ?, ?, ?> step(
			Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> I, AqlOptions options) {
		Collection<Pair<Integer, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>, Chc<En,Ty>>>> T = triggers(I, options);

		// System.out.println("------" + T.size() + " ");
		if (T.isEmpty()) {
			return null;
		}

		DMG<Pair<Integer, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>, Chc<En,Ty>>>, Void> shape = new DMG<>(T,
				new THashMap<>());
		Map<Pair<Integer, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>, Chc<En,Ty>>>, Instance<Ty, En, Sym, Fk, Att, Var, Var, ID, Chc<Var, Pair<ID, Att>>>> nodesA = Util
				.mk(), nodesE = Util.mk();

		Map<Pair<Pair<Integer, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>, Chc<En,Ty>>>, Var>, Term<Void, En, Void, Fk, Void, Gen, Void>> aaa = new THashMap<>();
		Map<Pair<Pair<Integer, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>, Chc<En,Ty>>>, Var>, Term<Void, En, Void, Fk, Void, Pair<Pair<Integer, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>, Chc<En,Ty>>>, Var>, Void>> xxx = new THashMap<>();
		Map<Pair<Pair<Integer, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>, Chc<En,Ty>>>, Var>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>> bbb = new THashMap<>();
		Map<Pair<Pair<Integer, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>, Chc<En,Ty>>>, Var>, Term<Ty, En, Sym, Fk, Att, Pair<Pair<Integer, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>, Chc<En,Ty>>>, Var>, Pair<Pair<Integer, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>, Chc<En,Ty>>>, Var>>> yyy = new THashMap<>();

		for (Pair<Integer, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>, Chc<En,Ty>>> t : T) {
			ED tfirst = eds.get(t.first);
			Query<Ty, En, Sym, Fk, Att, En, Fk, Att> Q = tfirst.getQ(schema);
			Instance<Ty, En, Sym, Fk, Att, Var, Var, ID, Chc<Var, Pair<ID, Att>>> A = Q.ens.get(ED.FRONT);
			Instance<Ty, En, Sym, Fk, Att, Var, Var, ID, Chc<Var, Pair<ID, Att>>> E = Q.ens.get(ED.BACK);
			Transform<Ty, En, Sym, Fk, Att, Var, Var, Var, Var, ID, Chc<Var, Pair<ID, Att>>, ID, Chc<Var, Pair<ID, Att>>> AE = Q.fks
					.get(ED.UNIT);

			nodesA.put(t, A);
			nodesE.put(t, E);
			AE.src().gens().entrySet((v, to) -> {
				// En en = tfirst.As.get(v).r;
				Term<Void, En, Void, Fk, Void, Var, Void> xx = AE.gens().apply(v, to);
				Pair<Pair<Integer, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>, Chc<En,Ty>>>, Var> p = new Pair<>(t, v);
				Term<Void, En, Void, Fk, Void, Pair<Pair<Integer, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>, Chc<En,Ty>>>, Var>, Void> ww = xx
						.mapGen(v0 -> new Pair<>(t, v0));
				xxx.put(p, ww);
				aaa.put(p, I.algebra().repr(to, t.second.get(v).l));

			});
			AE.src().sks().entrySet((v, to) -> {
				Term<Ty, En, Sym, Fk, Att, Var, Var> xx = AE.sks().apply(v, to);
				Pair<Pair<Integer, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>, Chc<En,Ty>>>, Var> p = new Pair<>(t, v);
				yyy.put(p, xx.mapGenSk(v0 -> new Pair<>(t, v0), v0 -> new Pair<>(t, v0)));
				bbb.put(p, t.second.get(v).r);
			});

			/*
			 * System.out.println("A"); System.out.println(A.toString());
			 * System.out.println(A.algebra().toString()); System.out.println("E");
			 * System.out.println(E.toString()); System.out.println(E.algebra().toString());
			 * System.out.println("A->E"); System.out.println(AE.toString());
			 */

		}

		ColimitInstance<Pair<Integer, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>, Chc<En,Ty>>>, Void, Ty, En, Sym, Fk, Att, Var, Var, ID, Chc<Var, Pair<ID, Att>>> A0 = new ColimitInstance<>(
				schema, shape, nodesA, Collections.emptyMap(), options);

		ColimitInstance<Pair<Integer, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>, Chc<En,Ty>>>, Void, Ty, En, Sym, Fk, Att, Var, Var, ID, Chc<Var, Pair<ID, Att>>> E0 = new ColimitInstance<>(
				schema, shape, nodesE, Collections.emptyMap(), options);

		BiFunction<Pair<Pair<Integer, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>, Chc<En,Ty>>>, Var>, En, Term<Void, En, Void, Fk, Void, Gen, Void>> aaa2 = (
				in, l) -> aaa.get(in);
		BiFunction<Pair<Pair<Integer, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>, Chc<En,Ty>>>, Var>, En, Term<Void, En, Void, Fk, Void, Pair<Pair<Integer, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>, Chc<En,Ty>>>, Var>, Void>> xxx2 = (
				in, l) -> xxx.get(in);
		BiFunction<Pair<Pair<Integer, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>, Chc<En,Ty>>>, Var>, Ty, Term<Ty, En, Sym, Fk, Att, Gen, Sk>> bbb2 = (
				in, l) -> bbb.get(in);
		BiFunction<Pair<Pair<Integer, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>, Chc<En,Ty>>>, Var>, Ty, Term<Ty, En, Sym, Fk, Att, Pair<Pair<Integer, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>, Chc<En,Ty>>>, Var>, Pair<Pair<Integer, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>, Chc<En,Ty>>>, Var>>> yyy2 = (
				in, l) -> yyy.get(in);

		LiteralTransform<Ty, En, Sym, Fk, Att, Pair<Pair<Integer, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>, Chc<En,Ty>>>, Var>, Pair<Pair<Integer, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>, Chc<En,Ty>>>, Var>, Gen, Sk, Integer, Chc<Pair<Pair<Integer, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>, Chc<En,Ty>>>, Var>, Pair<Integer, Att>>, X, Y> A0I = new LiteralTransform<>(
				aaa2, bbb2, A0, I, false);

		LiteralTransform<Ty, En, Sym, Fk, Att, Pair<Pair<Integer, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>, Chc<En,Ty>>>, Var>, Pair<Pair<Integer, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>, Chc<En,Ty>>>, Var>, Pair<Pair<Integer, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>, Chc<En,Ty>>>, Var>, Pair<Pair<Integer, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>, Chc<En,Ty>>>, Var>, Integer, Chc<Pair<Pair<Integer, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>, Chc<En,Ty>>>, Var>, Pair<Integer, Att>>, Integer, Chc<Pair<Pair<Integer, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>, Chc<En,Ty>>>, Var>, Pair<Integer, Att>>> A0E0 = new LiteralTransform<>(
				xxx2, yyy2, A0, E0, false);

		var ret = pushout(A0E0, A0I, options); /// wtf

		/*
		 * System.out.println("A0"); System.out.println(A0.toString());
		 * System.out.println(A0.algebra().toString()); System.out.println("I");
		 * System.out.println(I.toString()); System.out.println("E0");
		 * System.out.println(E0.toString()); System.out.println("ret");
		 * System.out.println(ret.toString()); System.out.println("A0->I");
		 * System.out.println(A0I.toString()); System.out.println("A0->E0");
		 * System.out.println(A0E0.toString()); System.out.println("&&");
		 */

		return ret;
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
