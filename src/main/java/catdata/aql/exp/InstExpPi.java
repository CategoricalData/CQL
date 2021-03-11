package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Chc;
import catdata.Pair;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Instance;
import catdata.aql.Kind;
import catdata.aql.Term;
import catdata.aql.fdm.Row;

public final class InstExpPi<Gen, Sk, X, Y> extends
    InstExp<Row<String, Chc<X, Term<String, String, Sym, Fk, Att, Gen, Sk>>, Chc<String,String>>, Y, Row<String, Chc<X, Term<String, String, Sym, Fk, Att, Gen, Sk>>, Chc<String,String>>, Y> {

  public final InstExp<Gen, Sk, X, Y> I;
  public final MapExp F;
  public final Map<String, String> options;

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {
    F.map(f);
    I.map(f);
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
    set.addAll(AqlOptions.proverOptionNames());
  }

  @Override
  public Collection<InstExp<?, ?, ?, ?>> direct(AqlTyping G) {
    return Collections.singleton(I);
  }

  public <R, P, E extends Exception> R accept(P param, InstExpVisitor<R, P, E> v) throws E {
    return v.visit(param, this);
  }

  @Override
  public Map<String, String> options() {
    return options;
  }

  @Override
  public Collection<Pair<String, Kind>> deps() {
    return Util.union(I.deps(), F.deps());
  }

  public InstExpPi(MapExp f, InstExp<Gen, Sk, X, Y> i, Map<String, String> options) {
    I = i;
    F = f;
    this.options = options;
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    result = prime * result + ((F == null) ? 0 : F.hashCode());
    result = prime * result + ((I == null) ? 0 : I.hashCode());
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
    InstExpPi<?, ?, ?, ?> other = (InstExpPi<?, ?, ?, ?>) obj;
    if (F == null) {
      if (other.F != null)
        return false;
    } else if (!F.equals(other.F))
      return false;
    if (I == null) {
      if (other.I != null)
        return false;
    } else if (!I.equals(other.I))
      return false;
    if (options == null) {
      if (other.options != null)
        return false;
    } else if (!options.equals(other.options))
      return false;
    return true;
  }

  @Override
  public SchExp type(AqlTyping G) {
    SchExp t0 = I.type(G);
    Pair<SchExp, SchExp> t1 = F.type(G);

    if (!G.eq(t1.first, t0)) {
      throw new RuntimeException(
          "Type error: In " + this + " domain of mapping is " + t1.first + " but instance has schema " + t0);
    }

    return t1.second;
  }

  @Override
  public String toString() {
    return "pi " + F + " " + I;
  }

  @Override
  public synchronized Instance<String, String, Sym, Fk, Att, Row<String, Chc<X, Term<String, String, Sym, Fk, Att, Gen, Sk>>, Chc<String,String>>, Y, Row<String, Chc<X, Term<String, String, Sym, Fk, Att, Gen, Sk>>, Chc<String,String>>, Y> eval0(
      AqlEnv env, boolean isC) {
    QueryExp q = new QueryExpDeltaCoEval(F, Util.toList(options));
    InstExpEval<Gen, Sk, X, Y> r = new InstExpEval<>(q, I, Util.toList(options));
    
    Instance<String, String, Sym, Fk, Att, Row<String, Chc<X, Term<String, String, Sym, Fk, Att, Gen, Sk>>, Chc<String,String>>, Y, Row<String, Chc<X, Term<String, String, Sym, Fk, Att, Gen, Sk>>, Chc<String,String>>, Y> 
    w = r.eval(env, isC);
     
    return w;
  }

}