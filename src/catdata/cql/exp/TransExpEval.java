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
import catdata.cql.Kind;
import catdata.cql.Term;
import catdata.cql.Transform;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.fdm.EvalTransform;
import catdata.cql.fdm.Row;

public class TransExpEval<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> extends
    TransExp<Row<String, Chc<X1, Term<String, String, Sym, Fk, Att, Gen1, Sk1>>, Chc<String,String>>, Y1, Row<String, Chc<X2, Term<String, String, Sym, Fk, Att, Gen2, Sk2>>, Chc<String,String>>, Y2, Row<String, Chc<X1, Term<String, String, Sym, Fk, Att, Gen1, Sk1>>, Chc<String,String>>, Y1, Row<String, Chc<X2, Term<String, String, Sym, Fk, Att, Gen2, Sk2>>, Chc<String,String>>, Y2> {

  public final QueryExp Q;
  public final TransExp<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> t;
  public final Map<String, String> options;

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {
    Q.map(f);
    t.map(f);
  }

  @Override
  public Map<String, String> options() {
    return options;
  }

  public TransExpEval(QueryExp q, TransExp<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> t,
      List<Pair<String, String>> options) {
    this.t = t;
    Q = q;
    this.options = Util.toMapSafely(options);
  }

  @Override
  public Pair<InstExp<Row<String, Chc<X1, Term<String, String, Sym, Fk, Att, Gen1, Sk1>>, Chc<String,String>>, Y1, Row<String, Chc<X1, Term<String, String, Sym, Fk, Att, Gen1, Sk1>>, Chc<String,String>>, Y1>, InstExp<Row<String, Chc<X2, Term<String, String, Sym, Fk, Att, Gen2, Sk2>>, Chc<String,String>>, Y2, Row<String, Chc<X2, Term<String, String, Sym, Fk, Att, Gen2, Sk2>>, Chc<String,String>>, Y2>> type(
      AqlTyping G) {
    if (!t.type(G).first.type(G).equals(Q.type(G).first)) {
      throw new RuntimeException(
          "Source of query is " + t.type(G).first.type(G) + " but transform is on " + t.type(G).first);
    }
    InstExpEval<Gen1, Sk1, X1, Y1> a = new InstExpEval<>(Q, t.type(G).first, Collections.emptyList());
    InstExpEval<Gen2, Sk2, X2, Y2> b = new InstExpEval<>(Q, t.type(G).second, Collections.emptyList());
    return new Pair<>(a, b);
  }

  @Override
  public Transform<String, String, Sym, Fk, Att, Row<String, Chc<X1, Term<String, String, Sym, Fk, Att, Gen1, Sk1>>, Chc<String,String>>, Y1, Row<String, Chc<X2, Term<String, String, Sym, Fk, Att, Gen2, Sk2>>, Chc<String,String>>, Y2, Row<String, Chc<X1, Term<String, String, Sym, Fk, Att, Gen1, Sk1>>, Chc<String,String>>, Y1, Row<String, Chc<X2, Term<String, String, Sym, Fk, Att, Gen2, Sk2>>, Chc<String,String>>, Y2> eval0(
      AqlEnv env, boolean isC) {
    if (isC) {
      Q.eval(env, true);
      t.eval(env, true);
      throw new IgnoreException();
    }
    return new EvalTransform<>(Q.eval(env, false), t.eval(env, false), new AqlOptions(options, env.defaults));
  }

  @Override
  public String toString() {
    return "eval " + Q + " " + t;
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    result = prime * result + ((Q == null) ? 0 : Q.hashCode());
    result = prime * result + ((t == null) ? 0 : t.hashCode());
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
    TransExpEval<?, ?, ?, ?, ?, ?, ?, ?> other = (TransExpEval<?, ?, ?, ?, ?, ?, ?, ?>) obj;
    if (Q == null) {
      if (other.Q != null)
        return false;
    } else if (!Q.equals(other.Q))
      return false;
    if (t == null) {
      if (other.t != null)
        return false;
    } else if (!t.equals(other.t))
      return false;
    if (options == null) {
      if (other.options != null)
        return false;
    } else if (!options.equals(other.options))
      return false;
    return true;
  }

  @Override
  public Collection<Pair<String, Kind>> deps() {
    return Util.union(Q.deps(), t.deps());
  }

  public <R, P, E extends Exception> R accept(P params, TransExpVisitor<R, P, E> v) throws E {
    return v.visit(params, this);
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
    set.add(AqlOption.require_consistency);
    set.add(AqlOption.allow_java_eqs_unsafe);
    set.addAll(AqlOptions.proverOptionNames());
  }

}