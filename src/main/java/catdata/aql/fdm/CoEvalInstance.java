package catdata.aql.fdm;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import catdata.aql.Algebra;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Collage;
import catdata.aql.DP;
import catdata.aql.Eq;
import catdata.aql.Instance;
import catdata.aql.It.ID;
import catdata.aql.Query;
import catdata.aql.Schema;
import catdata.aql.Term;
import catdata.aql.Transform;
import catdata.aql.Var;

public class CoEvalInstance<Ty, En1, Sym, Fk1, Att1, Gen, Sk, En2, Fk2, Att2, X, Y> extends
		Instance<Ty, En1, Sym, Fk1, Att1, Triple<Var, X, En2>, Chc<Triple<Var, X, En2>, Y>, Integer, Chc<Chc<Triple<Var, X, En2>, Y>, Pair<Integer, Att1>>> {

	private final Query<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> Q;
	@SuppressWarnings("unused")
	private final Instance<Ty, En2, Sym, Fk2, Att2, Gen, Sk, X, Y> J;
	private final InitialAlgebra<Ty, En1, Sym, Fk1, Att1, Triple<Var, X, En2>, Chc<Triple<Var, X, En2>, Y>> init;
	private final Instance<Ty, En1, Sym, Fk1, Att1, Triple<Var, X, En2>, Chc<Triple<Var, X, En2>, Y>, Integer, Chc<Chc<Triple<Var, X, En2>, Y>, Pair<Integer, Att1>>> I;

	public CoEvalInstance(Query<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> Q,
			Instance<Ty, En2, Sym, Fk2, Att2, Gen, Sk, X, Y> J, AqlOptions options) {
		if (!Q.dst.equals(J.schema())) {
			throw new RuntimeException(
					"In co-eval instance, target of query is " + Q.dst + ", but instance has type " + J.schema());
		} else if (!Q.consts.keySet().containsAll(Q.params.keySet())) {
			throw new RuntimeException(
					"Missing bindings: " + Util.sep(Util.diff(Q.params.keySet(), Q.consts.keySet()), ",")); // TODO aql
		} else if (!Q.consts.keySet().isEmpty()) {
			Q = Q.deParam();
		}

		// J.algebra().talg();

		this.Q = Q;
		this.J = J;

	
		Collage<Ty, En1, Sym, Fk1, Att1, Triple<Var, X, En2>, Chc<Triple<Var, X, En2>, Y>> col 
		= new Collage<>(Q.src.collage());
		
		col.sks.putAll(Util.map(J.algebra().talg().sks, (k, v) -> new Pair<>(Chc.<Triple<Var, X, En2>, Y>inRight(k), v)));
		for (En2 t : J.schema().ens) {
			for (X j : J.algebra().en(t)) {
				for (Var v : Q.ens.get(t).gens.keySet()) {
					En1 s = Q.ens.get(t).gens.get(v);
					col.gens.put(new Triple<>(v, j, t), s);
				}
				for (Var v : Q.ens.get(t).sks.keySet()) {
					Ty s = Q.ens.get(t).sks.get(v);
					col.sks.put(Chc.inLeft(new Triple<>(v, j, t)), s);
				}
			}
		}
		for (Eq<Ty, Void, Sym, Void, Void, Void, Y> eq : J.algebra().talg().eqs) {
			if (!eq.ctx.isEmpty()) {
				throw new RuntimeException("Anomaly: please report " + eq);
			}
			col.eqs.add(new Eq<>(null, Term.upTalg(eq.lhs.mapGenSk(Util.voidFn(), Chc::inRight)),
					Term.upTalg(eq.rhs.mapGenSk(Util.voidFn(), Chc::inRight))));
		}
		//col.validate();
		
		for (En2 t : J.schema().ens) {
			//col.validate();
			for (X j : J.algebra().en(t)) {
				for (Pair<Term<Ty, En1, Sym, Fk1, Att1, Var, Var>, Term<Ty, En1, Sym, Fk1, Att1, Var, Var>> eq : Q.ens.get(t).eqs) {
					col.eqs.add(new Eq<>(null, eq.first.mapGenSk(x -> new Triple<>(x, j, t), x -> Chc.inLeft(new Triple<>(x, j, t))),
							eq.second.mapGenSk(x -> new Triple<>(x, j, t), x -> Chc.inLeft(new Triple<>(x, j, t)))));
				}
				//col.validate();
				
				for (Fk2 fk : J.schema().fksFrom(t)) {
					Transform<Ty, En1, Sym, Fk1, Att1, Var, Var, Var, Var, ID, Chc<Var, Pair<ID, Att1>>, ID, Chc<Var, Pair<ID, Att1>>> fk0 = Q.fks
							.get(fk);
					En2 tt = J.schema().fks.get(fk).second;

					for (Var v0 : fk0.src().gens().keySet()) {
						//En1 ttt = fk0.src().gens().get(v0);
						Term<Ty, En1, Sym, Fk1, Att1, Triple<Var, X, En2>, Chc<Triple<Var, X, En2>, Y>> rhs = fk0.gens().get(v0).map(
								Util.voidFn(), Util.voidFn(), Function.identity(), Util.voidFn(), x -> new Triple<>(x, j, t),
								Util::abort);
						Term<Ty, En1, Sym, Fk1, Att1, Triple<Var, X, En2>, Chc<Triple<Var, X, En2>, Y>> lhs = Term
								.Gen(new Triple<>(v0, J.algebra().fk(fk, j), tt));
						col.eqs.add(new Eq<>(null, lhs, rhs));

					}
					//col.validate();

					for (Var v0 : fk0.src().sks().keySet()) {
						Term<Ty, En1, Sym, Fk1, Att1, Triple<Var, X, En2>, Chc<Triple<Var, X, En2>, Y>> rhs = fk0.sks().get(v0)
								.mapGenSk(x -> new Triple<>(x, j, t), x -> Chc.inLeft(new Triple<>(x, j, t)));
						Term<Ty, En1, Sym, Fk1, Att1, Triple<Var, X, En2>, Chc<Triple<Var, X, En2>, Y>> lhs = Term
								.Sk(Chc.inLeft(new Triple<>(v0, J.algebra().fk(fk, j), tt)));
						col.eqs.add(new Eq<>(null, lhs, rhs));
					}
					//col.validate();
				}
				for (Att2 att : J.schema().attsFrom(t)) {
					
					Term<Ty, En1, Sym, Fk1, Att1, Var, Var> att0 = Q.atts.get(att);
					Term<Ty, En1, Sym, Fk1, Att1, Triple<Var, X, En2>, Chc<Triple<Var, X, En2>, Y>> rhs = att0
							.mapGenSk(x -> new Triple<>(x, j, t), x -> Chc.inLeft(new Triple<>(x, j, t)));
					Term<Ty, En1, Sym, Fk1, Att1, Triple<Var, X, En2>, Chc<Triple<Var, X, En2>, Y>> lhs = J.algebra().att(att, j).map(
							Function.identity(), Function.identity(), Util.voidFn(), Util.voidFn(), Util.voidFn(),
							Chc::inRight);
					col.eqs.add(new Eq<>(null, lhs, rhs));
					
				}
			
			}
			//col.validate();
		}
		//col.validate();
		
		// AqlOptions strat = new AqlOptions(options, col);

		Function<Triple<Var, X, En2>, Object> printGen = (x) -> "[" + x.first + "=" + J.algebra().repr(x.third, x.second) + "]";
		
		BiFunction<Ty,Chc<Triple<Var, X, En2>, Y>, Object> printSk = (y,x) -> "[" + (x.left ? x.l.first + " " + J.algebra().printX(x.l.third,x.l.second) : J.algebra().printY(y,x.r).toString()) + "]"  ;


//		col.validate();
		
		init = new InitialAlgebra<>(options, schema(), col, printGen, printSk);
		I = new LiteralInstance<>(schema(), col.gens, col.sks, col.eqsAsPairs(), init.dp(), init,
				(Boolean) options.getOrDefault(AqlOption.require_consistency),
				(Boolean) options.getOrDefault(AqlOption.allow_java_eqs_unsafe));
		if (size() < 16*1024) {
			validate();
		}
	}

	@Override
	public Schema<Ty, En1, Sym, Fk1, Att1> schema() {
		return Q.src;
	}

	@Override
	public Map<Triple<Var, X, En2>, En1> gens() {
		return I.gens();
	}

	@Override
	public Map<Chc<Triple<Var, X, En2>, Y>, Ty> sks() {
		return I.sks();
	}

	@Override
	public Iterable<Pair<Term<Ty, En1, Sym, Fk1, Att1, Triple<Var, X, En2>, Chc<Triple<Var, X, En2>, Y>>, Term<Ty, En1, Sym, Fk1, Att1, Triple<Var, X, En2>, Chc<Triple<Var, X, En2>, Y>>>> eqs() {
		return I.eqs();
	}

	@Override
	public DP<Ty, En1, Sym, Fk1, Att1, Triple<Var, X, En2>, Chc<Triple<Var, X, En2>, Y>> dp() {
		return I.dp();
	}

	@Override
	public Algebra<Ty, En1, Sym, Fk1, Att1, Triple<Var, X, En2>, Chc<Triple<Var, X, En2>, Y>, Integer, Chc<Chc<Triple<Var, X, En2>, Y>, Pair<Integer, Att1>>> algebra() {
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
