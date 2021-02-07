package catdata.aql.fdm;

import java.util.function.BiFunction;

import catdata.aql.AqlOptions;
import catdata.aql.Instance;
import catdata.aql.Term;
import catdata.aql.Transform;

public class DistinctTransform<Ty, En, Sym, Fk, Att, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2>
		extends Transform<Ty, En, Sym, Fk, Att, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> {

	private final Transform<Ty, En, Sym, Fk, Att, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> t;
	private final AqlOptions ops1;
	private final AqlOptions ops2;
	private final DistinctInstance<Ty, En, Sym, Fk, Att, Gen1, Sk1, X1, Y1> src;
	private final DistinctInstance<Ty, En, Sym, Fk, Att, Gen2, Sk2, X2, Y2> dst;

	public DistinctTransform(Transform<Ty, En, Sym, Fk, Att, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> t, AqlOptions ops1, AqlOptions ops2) {
		this.t = t;
		this.ops1 = ops1;
		this.ops2 = ops2;
		this.src = new DistinctInstance<>(t.src(), ops1);
		this.dst = new DistinctInstance<>(t.dst(), ops2);
	}

	@Override
	public BiFunction<Gen1, En, Term<Void, En, Void, Fk, Void, Gen2, Void>> gens() {
		return t.gens();
	}

	@Override
	public BiFunction<Sk1, Ty, Term<Ty, En, Sym, Fk, Att, Gen2, Sk2>> sks() {
		return t.sks();
	}

	@Override
	public Instance<Ty, En, Sym, Fk, Att, Gen1, Sk1, X1, Y1> src() {
		return src;
	}

	@Override
	public Instance<Ty, En, Sym, Fk, Att, Gen2, Sk2, X2, Y2> dst() { // TODO aql recomputes
		return dst;
	}

}
