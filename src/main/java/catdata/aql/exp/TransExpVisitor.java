package catdata.aql.exp;

import catdata.aql.exp.TransExp.TransExpId;
import catdata.aql.exp.TransExp.TransExpLit;
import catdata.aql.exp.TransExp.TransExpVar;

public interface TransExpVisitor<R, P, E extends Exception> {
	public <Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> R visit(P params,
			TransExpCoEval<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> exp) throws E;

	public <Gen, Sk, X, Y> R visit(P params, TransExpCoEvalEvalCoUnit<Gen, Sk, X, Y> exp) throws E;

	public <Gen, Sk, X, Y> R visit(P params, TransExpCoEvalEvalUnit<Gen, Sk, X, Y> exp) throws E;

	public <Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> R visit(P params,
			TransExpDelta<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> exp) throws E;

	public <Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> R visit(P params,
			TransExpDistinct<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> exp) throws E;

	public <Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> R visit(P params,
			TransExpEval<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> exp) throws E;

	public <Gen, Sk, X, Y> R visit(P params, TransExpId<Gen, Sk, X, Y> exp) throws E;

	public <Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> R visit(P params,
			TransExpLit<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> exp) throws E;

	public <Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> R visit(P params,
			TransExpSigma<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> exp) throws E;

	public <Gen, Sk, X, Y> R visit(P params, TransExpSigmaDeltaCounit<Gen, Sk, X, Y> exp) throws E;

	public <Gen, Sk, X, Y> R visit(P params, TransExpSigmaDeltaUnit<Gen, Sk, X, Y> exp) throws E;

	public R visit(P params, TransExpRaw exp) throws E;

	public R visit(P params, TransExpVar exp) throws E;

	public <Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2, Gen3, Sk3, X3, Y3> R visit(P params,
			TransExpCompose<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2, Gen3, Sk3, X3, Y3> exp) throws E;

	public <X1, Y1, X2, Y2> R visit(P params, TransExpCsv<X1, Y1, X2, Y2> exp) throws E;

	public <X1, Y1, X2, Y2> R visit(P params, TransExpJdbc<X1, Y1, X2, Y2> exp) throws E;

	public <Gen, Sk, X, Y> R visit(P params, TransExpDistinct2<Gen, Sk, X, Y> exp) throws E;

	public <Gen, Sk, X, Y> R visit(P params, TransExpDiffReturn<Gen, Sk, X, Y> exp) throws E;

	public <Gen, Sk, X, Y, Gen1, Gen2, Sk1, Sk2, X1, X2> R visit(P params,
			TransExpDiff<Gen, Sk, X, Y, Gen1, Gen2, Sk1, Sk2, X1, X2> exp) throws E;

	public <Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> R visit(P params,
			TransExpPi<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> transExpPi) throws E;

	public R visit(P params, TransExpFrozen transExpFrozen);

}