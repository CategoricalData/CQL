package catdata.cql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

import catdata.Chc;
import catdata.Pair;
import catdata.Program;
import catdata.Util;
import catdata.cql.It.ID;
import catdata.cql.exp.AqlEnv;
import catdata.cql.exp.Att;
import catdata.cql.exp.Fk;
import catdata.cql.exp.Sym;
import catdata.cql.fdm.ColimitInstance;
import catdata.cql.fdm.ComposeTransform;
import catdata.cql.fdm.EvalInstance;
import catdata.cql.fdm.IdentityTransform;
import catdata.cql.fdm.LiteralTransform;
import catdata.cql.fdm.Row;
import catdata.graph.DMG;
import catdata.provers.KBTheory;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public class Constraints implements Semantics {

	public Set<LiteralTransform<String, String, Sym, Fk, Att, String, String, String, String, Integer, Chc<String, Pair<Integer, Att>>, Integer, Chc<String, Pair<Integer, Att>>>> asTransforms(
			Schema<String, String, Sym, Fk, Att> sch) {
		Set<LiteralTransform<String, String, Sym, Fk, Att, String, String, String, String, Integer, Chc<String, Pair<Integer, Att>>, Integer, Chc<String, Pair<Integer, Att>>>> ret = new THashSet<>();
		for (ED ed : eds) {
			ret.add(ed.asTransform(sch));
		}
		return ret;
	}
	
	public Constraints sigma(Mapping F, AqlOptions options) {
		List<ED> ret = new LinkedList<>();
		for (ED ed : eds) {
			ret.add(ed.sigma(F));
		}
		return new Constraints(F.dst, ret, options);
	}

	static int i = 0;

	public synchronized <X, Y> String tptpSorted(String x, Schema<String, String, Sym, Fk, Att> kb) {
		StringBuffer sb = new StringBuffer();

		for (ED ed : eds) {
			sb.append("tff(ed" + i++ + "," + x + ",(" + ed.tptpXSorted(kb) + ")).\n");
		}
		String tptp = sb.toString();
		return tptp;
	}

	public synchronized <X, Y> String tptp(String x, boolean preamble,
			KBTheory<Chc<String, String>, Head<String, String, Sym, Fk, Att, X, Y>, String> kb) {
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

	public final Schema<String, String, Sym, Fk, Att> schema;

	public final List<ED> eds;

	@Override
	public String toString() {
		return Util.sep(eds, "\n\n");
	}

	public Constraints(Schema<String, String, Sym, Fk, Att> schema, List<ED> eds, AqlOptions options) {
		this.eds = desugar(eds, options);
		this.schema = schema;
		for (ED ed : this.eds) {
		//	ed.validate(schema);
		}
	}

	public Constraints(AqlOptions ops, Schema<String, String, Sym, Fk, Att> ret) {
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
				Map<String, Chc<String, String>> es2 = Util.map(x.Es, (v, t) -> new Pair<>((v + "_des_0"), t));
				Map<String, Term<String, String, Sym, Fk, Att, Void, Void>> subst = Util.mk();
				Set<Pair<Term<String, String, Sym, Fk, Att, Void, Void>, Term<String, String, Sym, Fk, Att, Void, Void>>> ewh = (new THashSet<>());

				for (String v : x.Es.keySet()) {
					subst.put(v, Term.Var((v + "_des_0")));
					ewh.add(new Pair<>(Term.Var(v), subst.get(v)));
				}
				Map<String, Chc<String, String>> as = new THashMap<>();
				as.putAll(x.As);
				as.putAll(x.Es);
				as.putAll(es2);
				Set<Pair<Term<String, String, Sym, Fk, Att, Void, Void>, Term<String, String, Sym, Fk, Att, Void, Void>>> awh = new THashSet<>(
						x.Ewh.size());
				awh.addAll(x.Awh);
				awh.addAll(x.Ewh);
				for (Pair<Term<String, String, Sym, Fk, Att, Void, Void>, Term<String, String, Sym, Fk, Att, Void, Void>> p : x.Ewh) {
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

	public synchronized <Gen, Sk, X, Y> Pair<Instance<String, String, Sym, Fk, Att, ?, ?, X, ?>, Transform> chase(
			Instance<String, String, Sym, Fk, Att, Gen, Sk, X, Y> I, AqlOptions options) {
		// free on java check redundant - checked during chase
		Instance<String, String, Sym, Fk, Att, ?, ?, X, ?> ret = I;
		
		Transform t = new IdentityTransform<>(ret, Optional.empty());
		
		AqlEnv env = new AqlEnv(new Program<>(Collections.emptyList(), ""));
		int iii=0;
		for (;;) {

		//	AqlViewer v = new AqlViewer(1024, env, true);
		//	JTabbedPane p = new JTabbedPane();
		//	v.visit("Round " + i , p, ret);
		//	JFrame f = new JFrame("Chase " + (iii++));
		//	f.add(p); 
		//	f.setLocationRelativeTo(null);
	//		f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	//		f.pack();
	//		f.setVisible(true); 
			
			var x = step(ret, options);
			
			if (x == null) {
				return new Pair<>(ret, t);
			}
			Instance<String, String, Sym, Fk, Att, ?, ?, X, ?> ret2 = (Instance<String, String, Sym, Fk, Att, ?, ?, X, ?>) x.first;
		
		//	ret2.validateMore();

			ret = ret2;
			t = new ComposeTransform<>(t, x.second);
			
			t.validate(false);
		}
		
		
	}

	public synchronized <Gen, Sk, X, Y> Collection<Pair<Integer, Row<String, Chc<X, Term<String, String, Sym, Fk, Att, Gen, Sk>>, Chc<String, String>>>> triggers(
			Instance<String, String, Sym, Fk, Att, Gen, Sk, X, Y> I, AqlOptions options) {
		Collection<Pair<Integer, Row<String, Chc<X, Term<String, String, Sym, Fk, Att, Gen, Sk>>, Chc<String, String>>>> T = new LinkedList<>();

		BiPredicate<Chc<X, Term<String, String, Sym, Fk, Att, Gen, Sk>>, Chc<X, Term<String, String, Sym, Fk, Att, Gen, Sk>>> fn = (
				a, e) -> {
			if (a.left != e.left) {
				Util.anomaly();
			}
			if (a.left) {
				return a.l.equals(e.l);
			}
			return I.dp().eq(null, a.r, e.r);
		};
		int i = 0;
		for (ED ed : eds) {
		//	System.out.println("On " + ed);
			i++;
			if (frontIsEmpty(ed, I)) {
	//			System.out.println("front is empty");
				continue;
			}
			Query<String, String, Sym, Fk, Att, String, Fk, Att> Q = ed.getQ(schema);
			EvalInstance<String, String, Sym, Fk, Att, Gen, Sk, String, Fk, Att, X, Y> QI = new EvalInstance<>(Q, I, options);

			outer: for (Row<String, Chc<X, Term<String, String, Sym, Fk, Att, Gen, Sk>>, Chc<String, String>> e : QI.algebra()
					.en(ED.FRONT)) {
				for (Row<String, Chc<X, Term<String, String, Sym, Fk, Att, Gen, Sk>>, Chc<String, String>> a : QI.algebra()
						.en(ED.BACK)) {

					Row<String, Chc<X, Term<String, String, Sym, Fk, Att, Gen, Sk>>, Chc<String, String>> aa = QI.algebra()
							.fk(ED.UNIT, a);
//					System.out.println("consider " + aa);
					if (aa.rowEquals(fn, e)) {
						continue outer;
					}
				}
		//		System.out.println("add");
				T.add(new Pair<>(i - 1, e));
			}
		}
		return T;
	}

	private static <Gen, Sk, X, Y> boolean frontIsEmpty(ED ed, Instance<String, String, Sym, Fk, Att, Gen, Sk, X, Y> I) {
		for (Chc<String, String> x : ed.As.values()) {
			if (!x.left) {
				if (I.algebra().size(x.r) == 0) {
				//	System.out.println("Size of " + x.r + " in " + I.size() + " total");
					return true;
				}
			}
		}
		return false;
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
			throw new RuntimeException("Source of \n" + j + "\nnamely \n" + j.src() + "\n is not equal to source of\n" + k
					+ "\nnamely \n" + k.src());
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
		ColimitInstance<THREE, TWO, Ty, En, Sym, Fk, Att, ?, ?, ?, ?> ret = new ColimitInstance(j.src().schema(), shape, nodes,
				edges, options);

		return ret;
	}

	public synchronized <Gen, Sk, X, Y> Pair<Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?>, Transform> step(
			Instance<String, String, Sym, Fk, Att, Gen, Sk, X, Y> I, AqlOptions options) {
		Collection<Pair<Integer, Row<String, Chc<X, Term<String, String, Sym, Fk, Att, Gen, Sk>>, Chc<String, String>>>> T = triggers(
				I, options);

		// System.out.println("------" + T.size() + " ");
		if (T.isEmpty()) {
			return null;
		}

		DMG<Pair<Integer, Row<String, Chc<X, Term<String, String, Sym, Fk, Att, Gen, Sk>>, Chc<String, String>>>, Void> shape = new DMG<>(
				T, new THashMap<>());
		Map<Pair<Integer, Row<String, Chc<X, Term<String, String, Sym, Fk, Att, Gen, Sk>>, Chc<String, String>>>, Instance<String, String, Sym, Fk, Att, String, String, ID, Chc<String, Pair<ID, Att>>>> nodesA = Util
				.mk(), nodesE = Util.mk();

		Map<Pair<Pair<Integer, Row<String, Chc<X, Term<String, String, Sym, Fk, Att, Gen, Sk>>, Chc<String, String>>>, String>, Term<Void, String, Void, Fk, Void, Gen, Void>> aaa = new THashMap<>();
		Map<Pair<Pair<Integer, Row<String, Chc<X, Term<String, String, Sym, Fk, Att, Gen, Sk>>, Chc<String, String>>>, String>, Term<Void, String, Void, Fk, Void, Pair<Pair<Integer, Row<String, Chc<X, Term<String, String, Sym, Fk, Att, Gen, Sk>>, Chc<String, String>>>, String>, Void>> xxx = new THashMap<>();
		Map<Pair<Pair<Integer, Row<String, Chc<X, Term<String, String, Sym, Fk, Att, Gen, Sk>>, Chc<String, String>>>, String>, Term<String, String, Sym, Fk, Att, Gen, Sk>> bbb = new THashMap<>();
		Map<Pair<Pair<Integer, Row<String, Chc<X, Term<String, String, Sym, Fk, Att, Gen, Sk>>, Chc<String, String>>>, String>, Term<String, String, Sym, Fk, Att, Pair<Pair<Integer, Row<String, Chc<X, Term<String, String, Sym, Fk, Att, Gen, Sk>>, Chc<String, String>>>, String>, Pair<Pair<Integer, Row<String, Chc<X, Term<String, String, Sym, Fk, Att, Gen, Sk>>, Chc<String, String>>>, String>>> yyy = new THashMap<>();

		for (Pair<Integer, Row<String, Chc<X, Term<String, String, Sym, Fk, Att, Gen, Sk>>, Chc<String, String>>> t : T) {
			ED tfirst = eds.get(t.first);
			Query<String, String, Sym, Fk, Att, String, Fk, Att> Q = tfirst.getQ(schema);
			Instance<String, String, Sym, Fk, Att, String, String, ID, Chc<String, Pair<ID, Att>>> A = Q.ens.get(ED.FRONT);
			Instance<String, String, Sym, Fk, Att, String, String, ID, Chc<String, Pair<ID, Att>>> E = Q.ens.get(ED.BACK);
			Transform<String, String, Sym, Fk, Att, String, String, String, String, ID, Chc<String, Pair<ID, Att>>, ID, Chc<String, Pair<ID, Att>>> AE = Q.fks
					.get(ED.UNIT);

			nodesA.put(t, A);
			nodesE.put(t, E);
			AE.src().gens().entrySet((v, to) -> {
				// En en = tfirst.As.get(v).r;
				Term<Void, String, Void, Fk, Void, String, Void> xx = AE.gens().apply(v, to);
				Pair<Pair<Integer, Row<String, Chc<X, Term<String, String, Sym, Fk, Att, Gen, Sk>>, Chc<String, String>>>, String> p = new Pair<>(
						t, v);
				Term<Void, String, Void, Fk, Void, Pair<Pair<Integer, Row<String, Chc<X, Term<String, String, Sym, Fk, Att, Gen, Sk>>, Chc<String, String>>>, String>, Void> ww = xx
						.mapGen(v0 -> new Pair<>(t, v0));
				xxx.put(p, ww);
				aaa.put(p, I.algebra().repr(to, t.second.get(v).l));

			});
			AE.src().sks().entrySet((v, to) -> {
				Term<String, String, Sym, Fk, Att, String, String> xx = AE.sks().apply(v, to);
				Pair<Pair<Integer, Row<String, Chc<X, Term<String, String, Sym, Fk, Att, Gen, Sk>>, Chc<String, String>>>, String> p = new Pair<>(
						t, v);
				yyy.put(p, xx.mapGenSk(v0 -> new Pair<>(t, v0), v0 -> new Pair<>(t, v0)));
				bbb.put(p, t.second.get(v).r);
			});

		}

		ColimitInstance<Pair<Integer, Row<String, Chc<X, Term<String, String, Sym, Fk, Att, Gen, Sk>>, Chc<String, String>>>, Void, String, String, Sym, Fk, Att, String, String, ID, Chc<String, Pair<ID, Att>>> A0 = new ColimitInstance<>(
				schema, shape, nodesA, Collections.emptyMap(), options);
		A0.validateMore();

		ColimitInstance<Pair<Integer, Row<String, Chc<X, Term<String, String, Sym, Fk, Att, Gen, Sk>>, Chc<String, String>>>, Void, String, String, Sym, Fk, Att, String, String, ID, Chc<String, Pair<ID, Att>>> E0 = new ColimitInstance<>(
				schema, shape, nodesE, Collections.emptyMap(), options);
		E0.validateMore();

		BiFunction<Pair<Pair<Integer, Row<String, Chc<X, Term<String, String, Sym, Fk, Att, Gen, Sk>>, Chc<String, String>>>, String>, String, Term<Void, String, Void, Fk, Void, Gen, Void>> aaa2 = (
				in, l) -> aaa.get(in);
		BiFunction<Pair<Pair<Integer, Row<String, Chc<X, Term<String, String, Sym, Fk, Att, Gen, Sk>>, Chc<String, String>>>, String>, String, Term<Void, String, Void, Fk, Void, Pair<Pair<Integer, Row<String, Chc<X, Term<String, String, Sym, Fk, Att, Gen, Sk>>, Chc<String, String>>>, String>, Void>> xxx2 = (
				in, l) -> xxx.get(in);
		BiFunction<Pair<Pair<Integer, Row<String, Chc<X, Term<String, String, Sym, Fk, Att, Gen, Sk>>, Chc<String, String>>>, String>, String, Term<String, String, Sym, Fk, Att, Gen, Sk>> bbb2 = (
				in, l) -> bbb.get(in);
		BiFunction<Pair<Pair<Integer, Row<String, Chc<X, Term<String, String, Sym, Fk, Att, Gen, Sk>>, Chc<String, String>>>, String>, String, Term<String, String, Sym, Fk, Att, Pair<Pair<Integer, Row<String, Chc<X, Term<String, String, Sym, Fk, Att, Gen, Sk>>, Chc<String, String>>>, String>, Pair<Pair<Integer, Row<String, Chc<X, Term<String, String, Sym, Fk, Att, Gen, Sk>>, Chc<String, String>>>, String>>> yyy2 = (
				in, l) -> yyy.get(in);

		LiteralTransform<String, String, Sym, Fk, Att, Pair<Pair<Integer, Row<String, Chc<X, Term<String, String, Sym, Fk, Att, Gen, Sk>>, Chc<String, String>>>, String>, Pair<Pair<Integer, Row<String, Chc<X, Term<String, String, Sym, Fk, Att, Gen, Sk>>, Chc<String, String>>>, String>, Gen, Sk, Integer, Chc<Pair<Pair<Integer, Row<String, Chc<X, Term<String, String, Sym, Fk, Att, Gen, Sk>>, Chc<String, String>>>, String>, Pair<Integer, Att>>, X, Y> A0I = new LiteralTransform<>(
				aaa2, bbb2, A0, I, true);

		LiteralTransform<String, String, Sym, Fk, Att, Pair<Pair<Integer, Row<String, Chc<X, Term<String, String, Sym, Fk, Att, Gen, Sk>>, Chc<String, String>>>, String>, Pair<Pair<Integer, Row<String, Chc<X, Term<String, String, Sym, Fk, Att, Gen, Sk>>, Chc<String, String>>>, String>, Pair<Pair<Integer, Row<String, Chc<X, Term<String, String, Sym, Fk, Att, Gen, Sk>>, Chc<String, String>>>, String>, Pair<Pair<Integer, Row<String, Chc<X, Term<String, String, Sym, Fk, Att, Gen, Sk>>, Chc<String, String>>>, String>, Integer, Chc<Pair<Pair<Integer, Row<String, Chc<X, Term<String, String, Sym, Fk, Att, Gen, Sk>>, Chc<String, String>>>, String>, Pair<Integer, Att>>, Integer, Chc<Pair<Pair<Integer, Row<String, Chc<X, Term<String, String, Sym, Fk, Att, Gen, Sk>>, Chc<String, String>>>, String>, Pair<Integer, Att>>> A0E0 = new LiteralTransform<>(
				xxx2, yyy2, A0, E0, true);

		var ret = pushout(A0E0, A0I, options); 

		var w = ret.get(THREE.C);
		if (w == null) Util.anomaly();
		return new Pair<>(ret, w);
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
	}

	public String toSql(Schema<String,String,Sym,Fk,Att> sch) {
		String ret = "";
		for (var x : eds) {
			ret += x.toSql(sch);
			ret += "\n";
		}
		return ret;
	};

}
