package catdata.aql.fdm;

import java.util.function.BiFunction;

import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Instance;
import catdata.aql.Query;
import catdata.aql.Term;
import catdata.aql.Transform;
import catdata.aql.Var;

public class CoEvalEvalCoUnitTransform<Ty, En1, Sym, Fk1, Att1, Gen, Sk, En2, Fk2, Att2, X, Y> extends
		Transform<Ty, En1, Sym, Fk1, Att1, Triple<Var, Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>>, En2>, Chc<Triple<Var, Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>>, En2>, Y>, Gen, Sk, Integer, Chc<Chc<Triple<Var, Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>>, En2>, Y>, Pair<Integer, Att1>>, X, Y> {

	private final Query<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> Q;
	private final Instance<Ty, En1, Sym, Fk1, Att1, Gen, Sk, X, Y> I;
	private final EvalInstance<Ty, En1, Sym, Fk1, Att1, Gen, Sk, En2, Fk2, Att2, X, Y> J;
	private final CoEvalInstance<Ty, En1, Sym, Fk1, Att1, Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>>, Y, En2, Fk2, Att2, Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>>, Y> K; // TODO
																																																		// aql
																																																		// recomputes
	private final BiFunction<Triple<Var, Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>>, En2>, En1, Term<Void, En1, Void, Fk1, Void, Gen, Void>> gens; // = new THashMap<>(
	private final BiFunction<Chc<Triple<Var, Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>>, En2>, Y>, Ty, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>> sks; // = new THashMap<>(

	public CoEvalEvalCoUnitTransform(Query<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> q,
			Instance<Ty, En1, Sym, Fk1, Att1, Gen, Sk, X, Y> i, AqlOptions options) {
		if (!q.src.equals(i.schema())) {
			throw new RuntimeException("Q has src schema " + q.src + " but instance has schema " + i.schema());
		}
		Q = q;
		I = i;
		J = new EvalInstance<>(Q, I, options);
		K = new CoEvalInstance<>(Q, J, options);

		gens = (gen,t) -> {
			Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>> x = gen.second.get(gen.first);
			if (x.left) {
				return I.algebra().repr(t, x.l);
			} 
			throw new RuntimeException("Not supported - please report");
		};
		sks = (y,t) -> {	
			if (!y.left) {
				return I.reprT(Term.Sk(y.r));
			} 
				Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>> z = y.l.second.get(y.l.first);
				if (!z.left) {
					return z.r;
				}
				return Util.anomaly();
		};
		validate((Boolean) options.getOrDefault(AqlOption.dont_validate_unsafe));
	}

	@Override
	public BiFunction<Triple<Var, Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>>, En2>, En1, Term<Void, En1, Void, Fk1, Void, Gen, Void>> gens() {
		return gens;
	}

	@Override
	public BiFunction<Chc<Triple<Var, Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>>, En2>, Y>, Ty, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>> sks() {
		return sks;
	}

	@Override
	public CoEvalInstance<Ty, En1, Sym, Fk1, Att1, Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>>, Y, En2, Fk2, Att2, Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>>, Y> src() {
		return K;
	}

	@Override
	public Instance<Ty, En1, Sym, Fk1, Att1, Gen, Sk, X, Y> dst() {
		return I;
	}

}
