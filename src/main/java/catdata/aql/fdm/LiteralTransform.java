package catdata.aql.fdm;

import java.util.Map;

import catdata.Util;
import catdata.aql.Instance;
import catdata.aql.Term;
import catdata.aql.Transform;

public class LiteralTransform<Ty, En, Sym, Fk, Att, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2>
		extends Transform<Ty, En, Sym, Fk, Att, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> {

	private final Map<Gen1, Term<Void, En, Void, Fk, Void, Gen2, Void>> gens;
	private final Map<Sk1, Term<Ty, En, Sym, Fk, Att, Gen2, Sk2>> sks;

	private final Instance<Ty, En, Sym, Fk, Att, Gen1, Sk1, X1, Y1> src;
	private final Instance<Ty, En, Sym, Fk, Att, Gen2, Sk2, X2, Y2> dst;

	public LiteralTransform(Map<Gen1, Term<Void, En, Void, Fk, Void, Gen2, Void>> gens,
			Map<Sk1, Term<Ty, En, Sym, Fk, Att, Gen2, Sk2>> sks, Instance<Ty, En, Sym, Fk, Att, Gen1, Sk1, X1, Y1> src,
			Instance<Ty, En, Sym, Fk, Att, Gen2, Sk2, X2, Y2> dst, boolean dontValidateEqs) {
		Util.assertNotNull(gens, sks, src, dst);
		this.gens = (gens);
		this.sks = (sks);
		this.src = src;
		this.dst = dst;
		validate(dontValidateEqs);
	}

	@Override
	public Map<Gen1, Term<Void, En, Void, Fk, Void, Gen2, Void>> gens() {
		return gens;
	}

	@Override
	public Map<Sk1, Term<Ty, En, Sym, Fk, Att, Gen2, Sk2>> sks() {
		return sks;
	}

	@Override
	public Instance<Ty, En, Sym, Fk, Att, Gen1, Sk1, X1, Y1> src() {
		return src;
	}

	@Override
	public Instance<Ty, En, Sym, Fk, Att, Gen2, Sk2, X2, Y2> dst() {
		return dst;
	}
}
