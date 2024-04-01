package catdata.cql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Chc;
import catdata.Pair;
import catdata.Util;
import catdata.cql.AqlOptions;
import catdata.cql.Instance;
import catdata.cql.Kind;
import catdata.cql.Term;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.fdm.EvalInstance;
import catdata.cql.fdm.Row;

public final class InstExpEval<Gen, Sk, X, Y> extends
    InstExp<Row<String, Chc<X, Term<String, String, Sym, Fk, Att, Gen, Sk>>, Chc<String,String>>, Y, Row<String, Chc<X, Term<String, String, Sym, Fk, Att, Gen, Sk>>, Chc<String,String>>, Y> {

  public final QueryExp Q;
  public final InstExp<Gen, Sk, X, Y> I;
  public final Map<String, String> options;

  public <R, P, E extends Exception> R accept(P param, InstExpVisitor<R, P, E> v) throws E {
    return v.visit(param, this);
  }

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {
    Q.map(f);
    I.map(f);
  }

  @Override
  public Collection<InstExp<?, ?, ?, ?>> direct(AqlTyping G) {
    return Collections.singleton(I);
  }

  @Override
  public Map<String, String> options() {
    return options;
  }

  public InstExpEval(QueryExp q, InstExp<Gen, Sk, X, Y> i, List<Pair<String, String>> options) {
    Q = q;
    I = i;
    this.options = Util.toMapSafely(options);
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    result = prime * result + ((I == null) ? 0 : I.hashCode());
    result = prime * result + ((Q == null) ? 0 : Q.hashCode());
    result = prime * result + ((options == null) ? 0 : options.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    InstExpEval<?, ?, ?, ?> other = (InstExpEval<?, ?, ?, ?>) obj;
    if (I == null) {
      if (other.I != null)
        return false;
    } else if (!I.equals(other.I))
      return false;
    if (Q == null) {
      if (other.Q != null)
        return false;
    } else if (!Q.equals(other.Q))
      return false;
    if (options == null) {
      if (other.options != null)
        return false;
    } else if (!options.equals(other.options))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "eval " + Q + " " + I;
  }

  @Override
  public SchExp type(AqlTyping G) {
    if (!G.eq(I.type(G), Q.type(G).first)) {
      throw new RuntimeException("In evaluating a query, schema of instance is " + I.type(G)
          + " but source of query is " + Q.type(G).first + "\nThe query is " + this);
    }
    return Q.type(G).second;
  }

  @Override
  public synchronized Instance<String, String, Sym, Fk, Att, Row<String, Chc<X, Term<String, String, Sym, Fk, Att, Gen, Sk>>, Chc<String,String>>, Y, Row<String, Chc<X, Term<String, String, Sym, Fk, Att, Gen, Sk>>, Chc<String,String>>, Y> eval0(
      AqlEnv env, boolean isC) {
    if (isC) {
      Q.eval(env, true);
      I.eval(env, true);
      throw new IgnoreException();
    }
    return new EvalInstance<>(Q.eval(env, false), I.eval(env, false), new AqlOptions(options, env.defaults));
  }

  @Override
  public Collection<Pair<String, Kind>> deps() {
    return Util.union(I.deps(), Q.deps());
  }

  @Override
  protected void allowedOptions(Set<AqlOption> set) {
    set.add(AqlOption.eval_max_temp_size);
    set.add(AqlOption.eval_reorder_joins);
    set.add(AqlOption.eval_max_plan_depth);
    set.add(AqlOption.eval_join_selectivity);
    set.add(AqlOption.eval_use_indices);
    set.add(AqlOption.eval_use_sql_above);
    set.add(AqlOption.eval_approx_sql_unsafe);
    set.add(AqlOption.eval_sql_persistent_indices);
    set.add(AqlOption.query_remove_redundancy);
    set.add(AqlOption.varchar_length);
    set.add(AqlOption.start_ids_at);
  }

}