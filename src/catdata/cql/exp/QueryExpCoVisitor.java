package catdata.cql.exp;

import catdata.Unit;
import catdata.cql.exp.QueryExp.QueryExpLit;
import catdata.cql.exp.QueryExp.QueryExpVar;

public interface QueryExpCoVisitor<R, P, E extends Exception> {
  public QueryExpCompose visitQueryExpCompose(P params, R exp) throws E;

  public QueryExpId visitQueryExpId(P params, R exp) throws E;

  public QueryExpLit visitQueryExpLit(P params, R exp) throws E;

  public QueryExpVar visitQueryExpVar(P params, R exp) throws E;

  public QueryExpRaw visitQueryExpRaw(P params, R exp) throws E;

  public QueryExpSpanify visitQueryExpSpanify(P params, R exp) throws E;
  
  public QueryExpMapToSpanQuery visitQueryExpMapToSpanQuery(P params, R exp) throws E;
  
  public QueryExpDeltaCoEval visitQueryExpDeltaCoEval(P params, R exp) throws E;

  public QueryExpDeltaEval visitQueryExpDeltaEval(P params, R exp) throws E;

  public QueryExpRawSimple visitQueryExpRawSimple(P params, R exp) throws E;

  public QueryExpFromCoSpan visitQueryExpFromCoSpan(P params, R exp) throws E;

  public QueryExpFromEds visitQueryExpFromEds(P params, R exp) throws E;
  
  public QueryExpFront visitQueryExpFront(P params, R exp) throws E;

  public QueryExpRext visitQueryExpRext(P params, R exp) throws E;
  
  public QueryExpFromInst visitQueryExpFromInst(P params, R exp) throws E;

}