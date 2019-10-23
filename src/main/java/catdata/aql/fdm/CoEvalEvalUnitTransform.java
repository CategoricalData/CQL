package catdata.aql.fdm;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Instance;
import catdata.aql.Query;
import catdata.aql.Term;
import catdata.aql.Transform;
import catdata.aql.Var;
import gnu.trove.map.hash.THashMap;

public class CoEvalEvalUnitTransform<Ty, En1, Sym, Fk1, Att1, Gen, Sk, En2, Fk2, Att2, X, Y> extends
		Transform<Ty, En2, Sym, Fk2, Att2, Gen, Sk, Row<En2, Chc<Integer, Term<Ty, En1, Sym, Fk1, Att1, Triple<Var, X, En2>, Chc<Triple<Var, X, En2>, Y>>>>, Chc<Chc<Triple<Var, X, En2>, Y>, Pair<Integer, Att1>>, X, Y, Row<En2, Chc<Integer, Term<Ty, En1, Sym, Fk1, Att1, Triple<Var, X, En2>, Chc<Triple<Var, X, En2>, Y>>>>, Chc<Chc<Triple<Var, X, En2>, Y>, Pair<Integer, Att1>>> {
	// TODO aql recomputes
	private final Query<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> Q;
	private final Instance<Ty, En2, Sym, Fk2, Att2, Gen, Sk, X, Y> I;
	private final CoEvalInstance<Ty, En1, Sym, Fk1, Att1, Gen, Sk, En2, Fk2, Att2, X, Y> J;
	private final EvalInstance<Ty, En1, Sym, Fk1, Att1, Triple<Var, X, En2>, Chc<Triple<Var, X, En2>, Y>, En2, Fk2, Att2, Integer, Chc<Chc<Triple<Var, X, En2>, Y>, Pair<Integer, Att1>>> K;
	private final BiFunction<Gen, En2, Term<Void, En2, Void, Fk2, Void, Row<En2, Chc<Integer, Term<Ty, En1, Sym, Fk1, Att1, Triple<Var, X, En2>, Chc<Triple<Var, X, En2>, Y>>>>, Void>> gens; // = new THashMap<>();
	private final BiFunction<Sk, Ty, Term<Ty, En2, Sym, Fk2, Att2, Row<En2, Chc<Integer, Term<Ty, En1, Sym, Fk1, Att1, Triple<Var, X, En2>, Chc<Triple<Var, X, En2>, Y>>>>, Chc<Chc<Triple<Var, X, En2>, Y>, Pair<Integer, Att1>>>> sks; // = new THashMap<>();

	public CoEvalEvalUnitTransform(Query<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> q,
			Instance<Ty, En2, Sym, Fk2, Att2, Gen, Sk, X, Y> i, AqlOptions options) {
		if (!q.dst.equals(i.schema())) {
			throw new RuntimeException("Q has dst schema " + q.src + " but instance has schema " + i.schema());
		}
		Q = q;
		I = i;
		J = new CoEvalInstance<>(Q, I, options);
		K = new EvalInstance<>(Q, J, options);

		gens = (gen,en2) -> {
			X x = I.algebra().gen(gen);
			List<Var> l = Q.ens.get(en2).order;
			Map<Var, Chc<Integer, Term<Ty, En1, Sym, Fk1, Att1, Triple<Var, X, En2>, Chc<Triple<Var, X, En2>, Y>>>> tuple = new THashMap<>(
					l.size());
			for (Var v : l) {
				if (Q.ens.get(en2).gens.containsKey(v)) {
					tuple.put(v, Chc.inLeft(J.algebra().gen(new Triple<>(v, x, en2))));
				} else {
					tuple.put(v, Chc.inRight(J
							.reprT(Term.Sk(Chc.inLeft(Chc.inLeft(new Triple<>(v, x, en2)))))));
				}
			}

			return Term.Gen(Row.mkRow(l, tuple, en2, Q.ens.get(en2).gens, Q.ens.get(en2).sks));		
		};
		sks = (sk,t) -> trans0(I.algebra().sk(sk)).convert();

		validate((Boolean) options.getOrDefault(AqlOption.dont_validate_unsafe));
	}

	private Term<Ty, Void, Sym, Void, Void, Void, Chc<Chc<Triple<Var, X, En2>, Y>, Pair<Integer, Att1>>> trans0(
			Term<Ty, Void, Sym, Void, Void, Void, Y> term) {
		if (term.sk() != null) {
			return Term.Sk(Chc.inLeft(Chc.inRight(term.sk())));
		} else if (term.sym() != null) {
			return Term.Sym(term.sym(), term.args.stream().map(this::trans0).collect(Collectors.toList()));
		} else if (term.obj() != null) {
			return term.asObj();
		}
		throw new RuntimeException("Anomaly: please report");
	}

	@Override
	public BiFunction<Gen, En2, Term<Void, En2, Void, Fk2, Void, Row<En2, Chc<Integer, Term<Ty, En1, Sym, Fk1, Att1, Triple<Var, X, En2>, Chc<Triple<Var, X, En2>, Y>>>>, Void>> gens() {
		return gens;
	}

	@Override
	public BiFunction<Sk, Ty, Term<Ty, En2, Sym, Fk2, Att2, Row<En2, Chc<Integer, Term<Ty, En1, Sym, Fk1, Att1, Triple<Var, X, En2>, Chc<Triple<Var, X, En2>, Y>>>>, Chc<Chc<Triple<Var, X, En2>, Y>, Pair<Integer, Att1>>>> sks() {
		return sks;
	}

	@Override
	public Instance<Ty, En2, Sym, Fk2, Att2, Gen, Sk, X, Y> src() {
		return I;
	}

	@Override
	public Instance<Ty, En2, Sym, Fk2, Att2, Row<En2, Chc<Integer, Term<Ty, En1, Sym, Fk1, Att1, Triple<Var, X, En2>, Chc<Triple<Var, X, En2>, Y>>>>, Chc<Chc<Triple<Var, X, En2>, Y>, Pair<Integer, Att1>>, Row<En2, Chc<Integer, Term<Ty, En1, Sym, Fk1, Att1, Triple<Var, X, En2>, Chc<Triple<Var, X, En2>, Y>>>>, Chc<Chc<Triple<Var, X, En2>, Y>, Pair<Integer, Att1>>> dst() {
		return K;
	}

}
