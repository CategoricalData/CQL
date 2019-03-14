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

public class SigmaDeltaCounitTransform<Ty, En1, Sym, Fk1, Att1, Gen, Sk, En2, Fk2, Att2, X, Y> extends
		Transform<Ty, En2, Sym, Fk2, Att2, Pair<En1, X>, Y, Gen, Sk, Integer, Chc<Y, Pair<Integer, Att2>>, X, Y> {

	private final Mapping<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> F;
	private final Instance<Ty, En2, Sym, Fk2, Att2, Gen, Sk, X, Y> I;

	private final DeltaInstance<Ty, En1, Sym, Fk1, Att1, Gen, Sk, En2, Fk2, Att2, X, Y> J;
	private final SigmaInstance<Ty, En1, Sym, Fk1, Att1, Pair<En1, X>, Y, En2, Fk2, Att2, Pair<En1, X>, Y> K; // TODO
																												// aql
																												// recomputes

	private final Map<Pair<En1, X>, Term<Void, En2, Void, Fk2, Void, Gen, Void>> gens = new THashMap<>();
	private final Map<Y, Term<Ty, En2, Sym, Fk2, Att2, Gen, Sk>> sks = new THashMap<>();

	public SigmaDeltaCounitTransform(Mapping<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> f,
			Instance<Ty, En2, Sym, Fk2, Att2, Gen, Sk, X, Y> i, AqlOptions options) {
		F = f;
		I = i;
		J = new DeltaInstance<>(F, I);
		K = new SigmaInstance<>(F, J, options);

		for (Pair<En1, X> gen : src().gens().keySet()) {
			gens.put(gen, I.algebra().repr(F.ens.get(gen.first), gen.second).convert());
		}
		for (Y sk : src().sks().keySet()) {
			sks.put(sk, I.reprT(Term.Sk(sk)));
		}
		validate((Boolean) options.getOrDefault(AqlOption.dont_validate_unsafe));

	}

	@Override
	public Map<Pair<En1, X>, Term<Void, En2, Void, Fk2, Void, Gen, Void>> gens() {
		return gens;
	}

	@Override
	public Map<Y, Term<Ty, En2, Sym, Fk2, Att2, Gen, Sk>> sks() {
		return sks;
	}

	@Override
	public Instance<Ty, En2, Sym, Fk2, Att2, Pair<En1, X>, Y, Integer, Chc<Y, Pair<Integer, Att2>>> src() {
		return K;
	}

	@Override
	public Instance<Ty, En2, Sym, Fk2, Att2, Gen, Sk, X, Y> dst() {
		return I;
	}

}
