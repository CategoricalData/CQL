package catdata.aql.fdm;

import java.util.Map;

import catdata.aql.Instance;
import catdata.aql.Term;
import catdata.aql.Transform;

public class DistinctTransform<Ty, En, Sym, Fk, Att, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2>
		extends Transform<Ty, En, Sym, Fk, Att, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> {

	private final Transform<Ty, En, Sym, Fk, Att, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> t;

	public DistinctTransform(Transform<Ty, En, Sym, Fk, Att, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> t) {
		this.t = t;
	}

	@Override
	public Map<Gen1, Term<Void, En, Void, Fk, Void, Gen2, Void>> gens() {
		return t.gens();
	}

	@Override
	public Map<Sk1, Term<Ty, En, Sym, Fk, Att, Gen2, Sk2>> sks() {
		return t.sks();
	}

	@Override
	public Instance<Ty, En, Sym, Fk, Att, Gen1, Sk1, X1, Y1> src() {
		return new DistinctInstance<>(t.src());
	}

	@Override
	public Instance<Ty, En, Sym, Fk, Att, Gen2, Sk2, X2, Y2> dst() { // TODO aql recomputes
		return new DistinctInstance<>(t.dst());
	}

}
