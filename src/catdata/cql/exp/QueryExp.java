package catdata.cql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.cql.Kind;
import catdata.cql.Query;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.exp.SchExp.SchExpLit;

public abstract class QueryExp extends Exp<Query<String, String, Sym, Fk, Att, String, Fk, Att>> {

  @Override
  public Kind kind() {
    return Kind.QUERY;
  }

  public abstract Pair<SchExp, SchExp> type(AqlTyping G);

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  public Exp<Query<String, String, Sym, Fk, Att, String, Fk, Att>> Var(String v) {
    Exp ret = new QueryExpVar(v);
    return ret;
  }

  public static interface QueryExpVisitor<R, P, E extends Exception> {
    public R visit(P params, QueryExpCompose exp) throws E;

    public R visit(P params, QueryExpId exp) throws E;

    public R visit(P params, QueryExpLit exp) throws E;

    public R visit(P params, QueryExpVar exp) throws E;

    public R visit(P params, QueryExpRaw exp) throws E;

    public R visit(P params, QueryExpSpanify exp) throws E;

    public R visit(P params, QueryExpMapToSpanQuery exp) throws E;

    public R visit(P params, QueryExpDeltaCoEval exp) throws E;

    public R visit(P params, QueryExpDeltaEval exp) throws E;

    public R visit(P params, QueryExpRawSimple exp) throws E;

    public R visit(P params, QueryExpFromCoSpan exp) throws E;

    public R visit(P params, QueryExpFromEds exp) throws E;
    
    public R visit(P params, QueryExpFront exp) throws E;

	public R visit(P params, QueryExpDeltaMigrate exp) throws E;

	public R visit(P params, QueryExpFromInst queryExpFromInst) throws E;

	public R visit(P params, QueryExpRext queryExpRext) throws E;
	
  }

  public abstract <R, P, E extends Exception> R accept(P params, QueryExpVisitor<R, P, E> v) throws E;

  ///////////////////////////////////////////////////////////////////////////////////////////

  public static final class QueryExpVar extends QueryExp {
    public final String var;

    public <R, P, E extends Exception> R accept(P params, QueryExpVisitor<R, P, E> v) throws E {
      return v.visit(params, this);
    }

    @Override
    public void mapSubExps(Consumer<Exp<?>> f) {

    }

    @Override
    public boolean isVar() {
      return true;
    }

    @Override
    public Collection<Pair<String, Kind>> deps() {
      return Collections.singleton(new Pair<>(var, Kind.QUERY));
    }

    @Override
    public Map<String, String> options() {
      return Collections.emptyMap();
    }

    @Override
    public Query<String, String, catdata.cql.exp.Sym, catdata.cql.exp.Fk, catdata.cql.exp.Att, String, catdata.cql.exp.Fk, catdata.cql.exp.Att> eval0(
        AqlEnv env, boolean isC) {
      return env.defs.qs.get(var);
    }

    @Override
    public int hashCode() {
      return var.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      QueryExpVar other = (QueryExpVar) obj;
      return var.equals(other.var);
    }

    @Override
    public String toString() {
      return var;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Pair<SchExp, SchExp> type(AqlTyping G) {
      if (!G.defs.qs.containsKey(var)) {
        throw new RuntimeException("Not a query: " + var);
      }
      return (Pair<SchExp, SchExp>) ((Object) G.defs.qs.get(var));
    }

    public QueryExpVar(String var) {
      this.var = var;
    }

    @Override
    protected void allowedOptions(Set<AqlOption> set) {

    }

  }

  public static final class QueryExpLit extends QueryExp {

    public <R, P, E extends Exception> R accept(P params, QueryExpVisitor<R, P, E> v) throws E {
      return v.visit(params, this);
    }

    @Override
    public Map<String, String> options() {
      return Collections.emptyMap();
    }

    public final Query<String, String, Sym, Fk, Att, String, Fk, Att> q;

    @Override
    public Collection<Pair<String, Kind>> deps() {
      return Collections.emptyList();
    }

    public QueryExpLit(Query<String, String, Sym, Fk, Att, String, Fk, Att> q) {
      this.q = q;
    }

    @Override
    public Query<String, String, Sym, Fk, Att, String, Fk, Att> eval0(AqlEnv env, boolean isC) {
      return q;
    }

    @Override
    public int hashCode() {
      return q.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      QueryExpLit other = (QueryExpLit) obj;
      return q.equals(other.q);
    }

    @Override
    public String toString() {
      return "QueryExpLit " + q;
    }

    @Override
    public Pair<SchExp, SchExp> type(AqlTyping G) {
      return new Pair<>(new SchExpLit(q.src), new SchExpLit(q.dst));
    }

    @Override
    protected void allowedOptions(Set<AqlOption> set) {

    }

    @Override
    public void mapSubExps(Consumer<Exp<?>> f) {

    }

  }

}
