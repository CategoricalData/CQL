package catdata.cql.fdm;

import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Function;

import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import catdata.cql.Algebra;
import catdata.cql.AqlOptions;
import catdata.cql.Collage;
import catdata.cql.DP;
import catdata.cql.Eq;
import catdata.cql.Instance;
import catdata.cql.Query;
import catdata.cql.Schema;
import catdata.cql.Term;
import catdata.cql.Transform;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.Collage.CCollage;
import catdata.cql.It.ID;
import catdata.cql.Query.Agg;

public class CoEvalInstance<Ty, En1, Sym, Fk1, Att1, Gen, Sk, En2, Fk2, Att2, X, Y> extends
		Instance<Ty, En1, Sym, Fk1, Att1, Triple<String, X, En2>, Chc<Triple<String, X, En2>, Y>, Integer, Chc<Chc<Triple<String, X, En2>, Y>, Pair<Integer, Att1>>> {

	private final Query<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> Q;
	@SuppressWarnings("unused")
	private final Instance<Ty, En2, Sym, Fk2, Att2, Gen, Sk, X, Y> J;
	private final InitialAlgebra<Ty, En1, Sym, Fk1, Att1, Triple<String, X, En2>, Chc<Triple<String, X, En2>, Y>> init;
	private final Instance<Ty, En1, Sym, Fk1, Att1, Triple<String, X, En2>, Chc<Triple<String, X, En2>, Y>, Integer, Chc<Chc<Triple<String, X, En2>, Y>, Pair<Integer, Att1>>> I;

	private static boolean isRefl(Eq x) {
		return x.lhs.equals(x.rhs);
	}

	public CoEvalInstance(Query<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> Q, Instance<Ty, En2, Sym, Fk2, Att2, Gen, Sk, X, Y> J,
			AqlOptions options) {
		if (!Q.dst.equals(J.schema())) {
			throw new RuntimeException(
					"In co-eval instance, target of query is " + Q.dst + ", but instance has type " + J.schema());
		} else if (!Q.consts.keySet().containsAll(Q.params.keySet())) {
			throw new RuntimeException("Missing bindings: " + Util.sep(Util.diff(Q.params.keySet(), Q.consts.keySet()), ",")); // TODO
																																// aql
		} else if (!Q.consts.keySet().isEmpty()) {
			Q = Q.deParam();
		}

		// J.algebra().talg();

		this.Q = Q;
		this.J = J;

		Collage<Ty, En1, Sym, Fk1, Att1, Triple<String, X, En2>, Chc<Triple<String, X, En2>, Y>> col = new CCollage<>();
		// System.out.println("zzzz1 " + col);
		col.sks().putAll(Util.map(J.algebra().talg().sks, (k, v) -> new Pair<>(Chc.<Triple<String, X, En2>, Y>inRight(k), v)));
		for (En2 t : J.schema().ens) {
			for (X j : J.algebra().en(t)) {
				for (String v : Q.ens.get(t).gens.keySet()) {
					En1 s = Q.ens.get(t).gens.get(v);
					col.gens().put(new Triple<>(v, j, t), s);
				}
				for (String v : Q.ens.get(t).sks.keySet()) {
					Ty s = Q.ens.get(t).sks.get(v);
					col.sks().put(Chc.inLeft(new Triple<>(v, j, t)), s);
				}
			}
		}
		// System.out.println("zzzz2 " + col);
		for (Pair<Term<Ty, Void, Sym, Void, Void, Void, Y>, Term<Ty, Void, Sym, Void, Void, Void, Y>> eq : J.algebra().talg()
				.allEqs()) {
			Eq a = new Eq<>(null, Term.upTalg(eq.first.mapGenSk(Util.voidFn(), Chc::inRight)),
					Term.upTalg(eq.second.mapGenSk(Util.voidFn(), Chc::inRight)));
			if (!isRefl(a)) {
				col.eqs().add(a);
			}
			// System.out.println("adding instance equation " + new Eq<>(null,
			// Term.upTalg(eq.first.mapGenSk(Util.voidFn(), Chc::inRight)),
			// Term.upTalg(eq.second.mapGenSk(Util.voidFn(), Chc::inRight))));
		}
		// col.validate();
		for (En2 t : J.schema().ens) {
			// col.validate();
			// System.out.println(t + "yyyy " + col);
			for (X j : J.algebra().en(t)) {
				for (Pair<Term<Ty, En1, Sym, Fk1, Att1, String, String>, Term<Ty, En1, Sym, Fk1, Att1, String, String>> eq : Q.ens
						.get(t).eqs) {
					// System.out.println("frozen instance equation " + eq);
					Eq a = new Eq<>(null, eq.first.mapGenSk(x -> new Triple<>(x, j, t), x -> Chc.inLeft(new Triple<>(x, j, t))),
							eq.second.mapGenSk(x -> new Triple<>(x, j, t), x -> Chc.inLeft(new Triple<>(x, j, t))));
					if (!isRefl(a)) {
						col.eqs().add(a);
					}
				}
				// System.out.println("zzzz4 " + col);
				// col.validate();

				for (Fk2 fk : J.schema().fksFrom(t)) {
					Transform<Ty, En1, Sym, Fk1, Att1, String, String, String, String, ID, Chc<String, Pair<ID, Att1>>, ID, Chc<String, Pair<ID, Att1>>> fk0 = Q.fks
							.get(fk);
					En2 tt = J.schema().fks.get(fk).second;
					// System.out.println("zzzz0 " + col);
					fk0.src().gens().entrySet((v0, k) -> {
						Term<Ty, En1, Sym, Fk1, Att1, Triple<String, X, En2>, Chc<Triple<String, X, En2>, Y>> rhs = fk0.gens()
								.apply(v0, k).map(Util.voidFn(), Util.voidFn(), Function.identity(), Util.voidFn(),
										x -> new Triple<>(x, j, t), Util::abort);
						Term<Ty, En1, Sym, Fk1, Att1, Triple<String, X, En2>, Chc<Triple<String, X, En2>, Y>> lhs = Term
								.Gen(new Triple<>(v0, J.algebra().fk(fk, j), tt));
						Eq a = new Eq<>(null, lhs, rhs);
						// System.out.println("fk1 instance equation " + a);
						if (!isRefl(a)) {
							col.eqs().add(a);
						}
					});
					// col.validate();
					// System.out.println("zzzz7 " + col);
					fk0.src().sks().entrySet((v0, k) -> {
						Term<Ty, En1, Sym, Fk1, Att1, Triple<String, X, En2>, Chc<Triple<String, X, En2>, Y>> rhs = fk0.sks()
								.apply(v0, k).mapGenSk(x -> new Triple<>(x, j, t), x -> Chc.inLeft(new Triple<>(x, j, t)));
						Term<Ty, En1, Sym, Fk1, Att1, Triple<String, X, En2>, Chc<Triple<String, X, En2>, Y>> lhs = Term
								.Sk(Chc.inLeft(new Triple<>(v0, J.algebra().fk(fk, j), tt)));
						Eq a = new Eq<>(null, lhs, rhs);
						// System.out.println("fk2 instance equation " + a);
						if (!isRefl(a)) {
							col.eqs().add(a);
						}
					});

				}
				for (Att2 att : J.schema().attsFrom(t)) {

					Chc<Term<Ty, En1, Sym, Fk1, Att1, String, String>, Agg<Ty, En1, Sym, Fk1, Att1>> att1 = Q.atts.get(att);
					if (!att1.left) {
						throw new RuntimeException("Cannot co-eval with aggregation");
					}
					Term<Ty, En1, Sym, Fk1, Att1, Triple<String, X, En2>, Chc<Triple<String, X, En2>, Y>> rhs = att1.l
							.mapGenSk(x -> new Triple<>(x, j, t), x -> Chc.inLeft(new Triple<>(x, j, t)));
					Term<Ty, En1, Sym, Fk1, Att1, Triple<String, X, En2>, Chc<Triple<String, X, En2>, Y>> lhs = J.algebra()
							.att(att, j).map(Function.identity(), Function.identity(), Util.voidFn(), Util.voidFn(),
									Util.voidFn(), Chc::inRight);
					Eq a = new Eq<>(null, lhs, rhs);
					// System.out.println("fk att equation " + a);
					if (!isRefl(a)) {
						col.eqs().add(a); // that one
					}
				}

			}
			// System.out.println("zzzz3 " + col);
			// col.validate();
		}
		// col.validate();
		// System.out.println(col);
		// AqlOptions strat = new AqlOptions(options, col);

		Function<Triple<String, X, En2>, Object> printGen = (x) -> "[" + x.first + "=" + J.algebra().repr(x.third, x.second)
				+ "]";

		BiFunction<Ty, Chc<Triple<String, X, En2>, Y>, Object> printSk = (y, x) -> "["
				+ (x.left ? x.l.first + " " + J.algebra().printX(x.l.third, x.l.second) : J.algebra().printY(y, x.r).toString())
				+ "]";

		// col.validate();
		Collection<Pair<Term<Ty, En1, Sym, Fk1, Att1, Triple<String, X, En2>, Chc<Triple<String, X, En2>, Y>>, Term<Ty, En1, Sym, Fk1, Att1, Triple<String, X, En2>, Chc<Triple<String, X, En2>, Y>>>> z = col
				.eqsAsPairs();
		// System.out.println("zzzz " + col);
		// z.removeAll(J.schema().typeSide.collage().eqsAsPairs());

		// System.out.println("coeval initcol pre" + col);
		init = new InitialAlgebra<>(options, schema(), col, printGen, printSk);

		// System.out.println(col);
		// System.out.println("coeval initcol post" + init.col);

		I = new LiteralInstance<>(schema(), col.gens(), col.sks(), z, init.dp(), init,
				(Boolean) options.getOrDefault(AqlOption.require_consistency),
				(Boolean) options.getOrDefault(AqlOption.allow_java_eqs_unsafe));

		if (size() < 16 * 1024) {
			validate();
//			validateMore();
		}
		// System.out.println("in coeval I: " + I);
		// System.out.println("in coeval this: " + this);
	}

	@Override
	public Schema<Ty, En1, Sym, Fk1, Att1> schema() {
		return Q.src;
	}

	@Override
	public IMap<Triple<String, X, En2>, En1> gens() {
		return I.gens();
	}

	@Override
	public IMap<Chc<Triple<String, X, En2>, Y>, Ty> sks() {
		return I.sks();
	}

	@Override
	public DP<Ty, En1, Sym, Fk1, Att1, Triple<String, X, En2>, Chc<Triple<String, X, En2>, Y>> dp() {
		return I.dp();
	}

	@Override
	public Algebra<Ty, En1, Sym, Fk1, Att1, Triple<String, X, En2>, Chc<Triple<String, X, En2>, Y>, Integer, Chc<Chc<Triple<String, X, En2>, Y>, Pair<Integer, Att1>>> algebra() {
		return I.algebra();
	}

	@Override
	public boolean requireConsistency() {
		return I.requireConsistency();
	}

	@Override
	public boolean allowUnsafeJava() {
		return I.allowUnsafeJava();
	}

}
