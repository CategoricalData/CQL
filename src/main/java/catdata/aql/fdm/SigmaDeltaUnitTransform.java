package catdata.aql.fdm;

import java.util.Map;

import catdata.Chc;
import catdata.Pair;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Instance;
import catdata.aql.Mapping;
import catdata.aql.Term;
import catdata.aql.Transform;
import gnu.trove.map.hash.THashMap;

public class SigmaDeltaUnitTransform<Ty, En1, Sym, Fk1, Att1, Gen, Sk, En2, Fk2, Att2, X, Y> extends
		Transform<Ty, En1, Sym, Fk1, Att1, Gen, Sk, Pair<En1, Integer>, Chc<Sk, Pair<Integer, Att2>>, X, Y, Pair<En1, Integer>, Chc<Sk, Pair<Integer, Att2>>> {

	private final Mapping<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> F;
	private final Instance<Ty, En1, Sym, Fk1, Att1, Gen, Sk, X, Y> I;
	private final SigmaInstance<Ty, En1, Sym, Fk1, Att1, Gen, Sk, En2, Fk2, Att2, X, Y> J;
	private final DeltaInstance<Ty, En1, Sym, Fk1, Att1, Gen, Sk, En2, Fk2, Att2, Integer, Chc<Sk, Pair<Integer, Att2>>> K; // TODO
																															// aql
																															// recomputes
	private final Map<Gen, Term<Void, En1, Void, Fk1, Void, Pair<En1, Integer>, Void>> gens = new THashMap<>();
	private final Map<Sk, Term<Ty, En1, Sym, Fk1, Att1, Pair<En1, Integer>, Chc<Sk, Pair<Integer, Att2>>>> sks = new THashMap<>();

	public SigmaDeltaUnitTransform(Mapping<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> f,
			Instance<Ty, En1, Sym, Fk1, Att1, Gen, Sk, X, Y> i, AqlOptions options) {
		F = f;
		I = i;
		J = new SigmaInstance<>(F, I, options);
		K = new DeltaInstance<>(F, J);

		src().gens().entrySet((k,v) -> {
			Integer guid = J.algebra().intoX(F.trans(Term.Gen(k)));
			gens.put(k, Term.Gen(new Pair<>(v, guid)));
		});
		src().sks().entrySet((k,v) -> {
			Term<Ty, En1, Sym, Fk1, Att1, Pair<En1, Integer>, Chc<Sk, Pair<Integer, Att2>>> term = J.algebra()
					.intoY(F.trans(Term.Sk(k))).convert(); 
			sks.put(k, term);
		});
		validate((Boolean) options.getOrDefault(AqlOption.dont_validate_unsafe));

	}

	@Override
	public Map<Gen, Term<Void, En1, Void, Fk1, Void, Pair<En1, Integer>, Void>> gens() {
		return gens;
	}

	@Override
	public Map<Sk, Term<Ty, En1, Sym, Fk1, Att1, Pair<En1, Integer>, Chc<Sk, Pair<Integer, Att2>>>> sks() {
		return sks;
	}

	@Override
	public Instance<Ty, En1, Sym, Fk1, Att1, Gen, Sk, X, Y> src() {
		return I;
	}

	@Override
	public Instance<Ty, En1, Sym, Fk1, Att1, Pair<En1, Integer>, Chc<Sk, Pair<Integer, Att2>>, Pair<En1, Integer>, Chc<Sk, Pair<Integer, Att2>>> dst() {
		return K;
	}

}
