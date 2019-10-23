package catdata.aql.fdm;

import java.util.Optional;
import java.util.function.BiFunction;

import catdata.Util;
import catdata.aql.Instance;
import catdata.aql.Term;
import catdata.aql.Transform;

public class IdentityTransform<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y>
		extends Transform<Ty, En, Sym, Fk, Att, Gen, Sk, Gen, Sk, X, Y, X, Y> {

	private final Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> I;
	private final Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> J;
	private final BiFunction<Gen, En, Term<Void, En, Void, Fk, Void, Gen, Void>> gens;
	private final BiFunction<Sk, Ty, Term<Ty, En, Sym, Fk, Att, Gen, Sk>> sks;

	public IdentityTransform(Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> i,
			Optional<Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y>> j) {
		Util.assertNotNull(i);
		I = i;
		gens = (gen,t) -> Term.Gen(gen);
		sks = (sk,t) -> Term.Sk(sk);

		if (j.isEmpty()) {
			J = i;
		} else {
			J = j.get();
		}
		//this.validate(j.isEmpty());
	}

	@Override
	public BiFunction<Gen, En, Term<Void, En, Void, Fk, Void, Gen, Void>> gens() {
		return gens;
	}

	@Override
	public BiFunction<Sk, Ty, Term<Ty, En, Sym, Fk, Att, Gen, Sk>> sks() {
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
