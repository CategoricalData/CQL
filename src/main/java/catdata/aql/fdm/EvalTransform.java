package catdata.aql.fdm;

import java.util.function.BiFunction;
import java.util.function.Function;

import catdata.Chc;
import catdata.aql.AqlOptions;
import catdata.aql.Instance;
import catdata.aql.Query;
import catdata.aql.Term;
import catdata.aql.Transform;

public class EvalTransform<Ty, En1, Sym, Fk1, Att1, Gen1, Sk1, En2, Fk2, Att2, Gen2, Sk2, X1, Y1, X2, Y2> extends
		Transform<Ty, En2, Sym, Fk2, Att2, Row<En2, Chc<X1, Term<Ty, En1, Sym, Fk1, Att1, Gen1, Sk1>>>, Y1, Row<En2, Chc<X2, Term<Ty, En1, Sym, Fk1, Att1, Gen2, Sk2>>>, Y2, Row<En2, Chc<X1, Term<Ty, En1, Sym, Fk1, Att1, Gen1, Sk1>>>, Y1, Row<En2, Chc<X2, Term<Ty, En1, Sym, Fk1, Att1, Gen2, Sk2>>>, Y2> {

	private final Query<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> Q;
	@SuppressWarnings("unused")
	private final Transform<Ty, En1, Sym, Fk1, Att1, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> h;

	private final EvalInstance<Ty, En1, Sym, Fk1, Att1, Gen1, Sk1, En2, Fk2, Att2, X1, Y1> src;
	private final EvalInstance<Ty, En1, Sym, Fk1, Att1, Gen2, Sk2, En2, Fk2, Att2, X2, Y2> dst;

	private final BiFunction<Row<En2, Chc<X1, Term<Ty, En1, Sym, Fk1, Att1, Gen1, Sk1>>>, En2, Term<Void, En2, Void, Fk2, Void, Row<En2, Chc<X2, Term<Ty, En1, Sym, Fk1, Att1, Gen2, Sk2>>>, Void>> gens; // = new THashMap<>();
	private final BiFunction<Y1, Ty, Term<Ty, En2, Sym, Fk2, Att2, Row<En2, Chc<X2, Term<Ty, En1, Sym, Fk1, Att1, Gen2, Sk2>>>, Y2>> sks; // = new THashMap<>();

	// TODO aql recomputes
	public EvalTransform(Query<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> q,
			Transform<Ty, En1, Sym, Fk1, Att1, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> h, AqlOptions options) {
		if (!h.src().schema().equals(q.src)) {
			throw new RuntimeException("Source of query is " + q.src + " but transform is on " + h.src().schema());
		}

		Q = q;
		this.h = h;

		src = new EvalInstance<>(Q, h.src(), options);
		dst = new EvalInstance<>(Q, h.dst(), options);

		gens = (gen1,e) -> {
			BiFunction<Chc<X1, Term<Ty, En1, Sym, Fk1, Att1, Gen1, Sk1>>, Object, Chc<X2, Term<Ty, En1, Sym, Fk1, Att1, Gen2, Sk2>>> fn = (
					z, t) -> z.left ? Chc.inLeft(h.repr(((Chc<Ty, En1>) t).r, z.l)) : Chc.inRight(h.trans(z.r));
			Row<En2, Chc<X2, Term<Ty, En1, Sym, Fk1, Att1, Gen2, Sk2>>> r = gen1.map(fn);
			Term<Void, En2, Void, Fk2, Void, Row<En2, Chc<X2, Term<Ty, En1, Sym, Fk1, Att1, Gen2, Sk2>>>, Void> t = Term
					.Gen(r);
			return t;
		};
		sks = (sk1,t) -> h.dst().algebra().intoY(h.reprT(sk1)).convert();
		
		validate(false);
	}

	@Override
	public BiFunction<Row<En2, Chc<X1, Term<Ty, En1, Sym, Fk1, Att1, Gen1, Sk1>>>, En2, Term<Void, En2, Void, Fk2, Void, Row<En2, Chc<X2, Term<Ty, En1, Sym, Fk1, Att1, Gen2, Sk2>>>, Void>> gens() {
		return gens;
	}

	@Override
	public BiFunction<Y1, Ty, Term<Ty, En2, Sym, Fk2, Att2, Row<En2, Chc<X2, Term<Ty, En1, Sym, Fk1, Att1, Gen2, Sk2>>>, Y2>> sks() {
		return sks;
	}

	@Override
	public Instance<Ty, En2, Sym, Fk2, Att2, Row<En2, Chc<X1, Term<Ty, En1, Sym, Fk1, Att1, Gen1, Sk1>>>, Y1, Row<En2, Chc<X1, Term<Ty, En1, Sym, Fk1, Att1, Gen1, Sk1>>>, Y1> src() {
		return src;
	}

	@Override
	public Instance<Ty, En2, Sym, Fk2, Att2, Row<En2, Chc<X2, Term<Ty, En1, Sym, Fk1, Att1, Gen2, Sk2>>>, Y2, Row<En2, Chc<X2, Term<Ty, En1, Sym, Fk1, Att1, Gen2, Sk2>>>, Y2> dst() {
		return dst;
	}

}
