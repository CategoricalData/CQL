package catdata.aql.fdm;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

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
import gnu.trove.map.hash.THashMap;

public class CoEvalEvalCoUnitTransform<Ty, En1, Sym, Fk1, Att1, Gen, Sk, En2, Fk2, Att2, X, Y> extends
		Transform<Ty, En1, Sym, Fk1, Att1, Triple<Var, Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>>, En2>, Chc<Triple<Var, Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>>, En2>, Y>, Gen, Sk, Integer, Chc<Chc<Triple<Var, Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>>, En2>, Y>, Pair<Integer, Att1>>, X, Y> {

	private final Query<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> Q;
	private final Instance<Ty, En1, Sym, Fk1, Att1, Gen, Sk, X, Y> I;
	private final EvalInstance<Ty, En1, Sym, Fk1, Att1, Gen, Sk, En2, Fk2, Att2, X, Y> J;
	private final CoEvalInstance<Ty, En1, Sym, Fk1, Att1, Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>>, Y, En2, Fk2, Att2, Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>>, Y> K; // TODO
																																																		// aql
																																																		// recomputes
	private final Map<Triple<Var, Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>>, En2>, Term<Void, En1, Void, Fk1, Void, Gen, Void>> gens = new THashMap<>(
			Collections.emptyMap());
	private final Map<Chc<Triple<Var, Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>>, En2>, Y>, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>> sks = new THashMap<>(
			Collections.emptyMap());

	public CoEvalEvalCoUnitTransform(Query<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> q,
			Instance<Ty, En1, Sym, Fk1, Att1, Gen, Sk, X, Y> i, AqlOptions options) {
		if (!q.src.equals(i.schema())) {
			throw new RuntimeException("Q has src schema " + q.src + " but instance has schema " + i.schema());
		}
		Q = q;
		I = i;
		J = new EvalInstance<>(Q, I, options);
		K = new CoEvalInstance<>(Q, J, options);

		for (Entry<Triple<Var, Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>>, En2>, En1> gen : src().gens()
				.entrySet()) {
			Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>> x = gen.getKey().second.get(gen.getKey().first);
			if (x.left) {
				gens.put(gen.getKey(), I.algebra().repr(gen.getValue(), x.l));
			} else {
				Util.anomaly(); // TODO aql
			}
		}
		for (Chc<Triple<Var, Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>>, En2>, Y> y : src().sks()
				.keySet()) {
			if (!y.left) {
				sks.put(y, I.reprT(Term.Sk(y.r)));
			} else {
				Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>> z = y.l.second.get(y.l.first);
				if (z.left) {
					Util.anomaly(); // sks.put(y, z.l);
				} else {
					sks.put(y, z.r);
				}
			}
		}
		validate((Boolean) options.getOrDefault(AqlOption.dont_validate_unsafe));
	}

	@Override
	public Map<Triple<Var, Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>>, En2>, Term<Void, En1, Void, Fk1, Void, Gen, Void>> gens() {
		return gens;
	}

	@Override
	public Map<Chc<Triple<Var, Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>>, En2>, Y>, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>> sks() {
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
