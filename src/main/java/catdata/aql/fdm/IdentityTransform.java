package catdata.aql.fdm;

import java.util.Map;
import java.util.Optional;

import catdata.Util;
import catdata.aql.Instance;
import catdata.aql.Term;
import catdata.aql.Transform;
import gnu.trove.map.hash.THashMap;

public class IdentityTransform<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y>
		extends Transform<Ty, En, Sym, Fk, Att, Gen, Sk, Gen, Sk, X, Y, X, Y> {

	private final Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> I;
	private final Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> J;
	private final Map<Gen, Term<Void, En, Void, Fk, Void, Gen, Void>> gens = new THashMap<>();
	private final Map<Sk, Term<Ty, En, Sym, Fk, Att, Gen, Sk>> sks = new THashMap<>();

	public IdentityTransform(Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> i,
			Optional<Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y>> j) {
		Util.assertNotNull(i);
		I = i;
		for (Gen gen : i.gens().keySet()) {
			gens.put(gen, Term.Gen(gen));
		}
		for (Sk sk : i.sks().keySet()) {
			sks.put(sk, Term.Sk(sk));
		}
		if (j.isEmpty()) {
			J = i;
		} else {
			J = j.get();
		}
		this.validate(j.isEmpty());
	}

	@Override
	public Map<Gen, Term<Void, En, Void, Fk, Void, Gen, Void>> gens() {
		return gens;
	}

	@Override
	public Map<Sk, Term<Ty, En, Sym, Fk, Att, Gen, Sk>> sks() {
		return sks;
	}

	@Override
	public Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> src() {
		return I;
	}

	@Override
	public Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> dst() {
		return J;
	}

}
