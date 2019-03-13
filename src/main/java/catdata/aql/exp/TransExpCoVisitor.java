package catdata.aql.exp;

import catdata.aql.exp.TransExp.TransExpId;
import catdata.aql.exp.TransExp.TransExpLit;
import catdata.aql.exp.TransExp.TransExpVar;

public interface TransExpCoVisitor<R, P, E extends Exception> {
	public  <Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> TransExpCoEval<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> visitTransExpCoEval(P params, R exp) throws E; 
	public  <Gen, Sk, X, Y> TransExpCoEvalEvalCoUnit<Gen, Sk, X, Y> visitTransExpCoEvalEvalCoUnit(P params, R exp)throws E; 
	public  <Gen, Sk, X, Y> TransExpCoEvalEvalUnit<Gen, Sk, X, Y> visitTransExpCoEvalEvalUnit(P params, R exp);  
	public  <Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> TransExpDelta<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> visitTransExpDelta(P params, R exp); 
	public  <Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> TransExpDistinct<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> visitTransExpDistinct(P params, R exp);
	public  <Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> TransExpEval<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> visitTransExpEval(P params, R exp);
	public  <Gen, Sk, X, Y> TransExpId<Gen, Sk, X, Y> visitTransExpId(P params, R exp); 
	public  <Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> TransExpLit<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> visitTransExpLit(P params, R exp);
	public  <Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> TransExpSigma<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> visitTransExpSigma(P params, R exp); 
	public  <Gen, Sk, X, Y> TransExpSigmaDeltaCounit<Gen, Sk, X, Y> visitTransExpSigmaDeltaCounit(P params, R exp); 
	public  <Gen, Sk, X, Y> TransExpSigmaDeltaUnit<Gen, Sk, X, Y> visitTransExpSigmaDeltaUnit(P params, R exp); 
	public  TransExpRaw visitTransExpRaw(P params, R exp);
	public  TransExpVar visitTransExpVar(P params, R exp);
	public  <Gen1,Sk1,Gen2,Sk2,X1,Y1,X2,Y2,Gen3,Sk3,X3,Y3> TransExpCompose<Gen1,Sk1,Gen2,Sk2,X1,Y1,X2,Y2,Gen3,Sk3,X3,Y3> visitTransExpCompose(P params, R exp);
	public  <X1, Y1, X2, Y2> TransExpCsv <X1, Y1, X2, Y2> visitTransExpCsv(P params, R exp); 
	public  <X1, Y1, X2, Y2> TransExpJdbc<X1, Y1, X2, Y2> visitTransExpJdbc(P params, R exp);
	public  < Gen, Sk, X, Y> TransExpDistinct2<Gen, Sk, X, Y> visitTransExpDistinct2(P params, R exp); 
	public <Gen, Sk, X, Y> TransExpDiffReturn<Gen,Sk,X,Y> visitTransExpDiffReturn(P params, R exp);
	public <Gen, Sk, X, Y, Gen1, Gen2, Sk1, Sk2, X1, X2> TransExpDiff<Gen, Sk, X, Y, Gen1, Gen2, Sk1, Sk2, X1, X2> visitTransExpDiff(P params,
			R exp);
	public <Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> TransExpPi<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> visitTransExpPi(P params,
			R exp);
	public TransExpFrozen visitTransExpFrozen(
			P params, R exp) throws RuntimeException;
	
	
}