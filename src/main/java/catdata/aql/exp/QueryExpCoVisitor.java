package catdata.aql.exp;

import catdata.aql.exp.QueryExp.QueryExpId;
import catdata.aql.exp.QueryExp.QueryExpLit;
import catdata.aql.exp.QueryExp.QueryExpVar;

public interface QueryExpCoVisitor<R, P, E extends Exception> {
	public QueryExpCompose visitQueryExpCompose(P params, R exp) throws E;

	public QueryExpId visitQueryExpId(P params, R exp) throws E;

	public QueryExpLit visitQueryExpLit(P params, R exp) throws E;

	public QueryExpVar visitQueryExpVar(P params, R exp) throws E;

	public QueryExpRaw visitQueryExpRaw(P params, R exp) throws E;

	public QueryExpDeltaCoEval visitQueryExpDeltaCoEval(P params, R exp) throws E;

	public QueryExpDeltaEval visitQueryExpDeltaEval(P params, R exp) throws E;

	public QueryExpRawSimple visitQueryExpRawSimple(P params, R exp) throws E;

	public QueryExpFromCoSpan visitQueryExpFromCoSpan(P params, R exp) throws E;

	public QueryExpFromEds visitQueryExpFromEds(P params, R exp) throws E;

}