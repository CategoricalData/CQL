package catdata.cql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.cql.AqlOptions;
import catdata.cql.Instance;
import catdata.cql.Kind;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.fdm.DistinctInstance;

public final class InstExpDistinct<Gen, Sk, X, Y> extends InstExp<Gen, Sk, X, Y> {

  public final InstExp<Gen, Sk, X, Y> I;

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {
    I.map(f);
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
    return Collections.emptyMap();
  }

  public InstExpDistinct(InstExp<Gen, Sk, X, Y> i) {
    I = i;
  }

  @Override
  public SchExp type(AqlTyping G) {
    return I.type(G);
  }

  @Override
  public synchronized Instance<String, String, Sym, Fk, Att, Gen, Sk, X, Y> eval0(AqlEnv env, boolean isC) {
    Instance<String, String, Sym, Fk, Att, Gen, Sk, X, Y> ii = I.eval(env, isC);
    if (isC) {
      throw new IgnoreException();
    }
    AqlOptions ops = env.defaults;
    
    return new DistinctInstance<>(ii, ops);
  }

  @Override
  public String toString() {
    return "distinct " + I;
  }

  @Override
  public int hashCode() {
    return I.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    InstExpDistinct<?, ?, ?, ?> other = (InstExpDistinct<?, ?, ?, ?>) obj;
    return I.equals(other.I);
  }

  @Override
  public Collection<Pair<String, Kind>> deps() {
    return I.deps();
  }

  @Override
  protected void allowedOptions(Set<AqlOption> set) {

  }

}