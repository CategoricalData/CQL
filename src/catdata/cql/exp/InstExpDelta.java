package catdata.cql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.Util;
import catdata.cql.Instance;
import catdata.cql.Kind;
import catdata.cql.Mapping;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.fdm.DeltaInstance;

public final class InstExpDelta<Gen, Sk, X, Y> extends InstExp<Pair<String, X>, Y, Pair<String, X>, Y> {

  public <R, P, E extends Exception> R accept(P param, InstExpVisitor<R, P, E> v) throws E {
    return v.visit(param, this);
  }

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {
    F.map(f);
    I.map(f);
  }

  @Override
  public Collection<InstExp<?, ?, ?, ?>> direct(AqlTyping G) {
    return Collections.singleton(I);
  }

  public final InstExp<Gen, Sk, X, Y> I;
  public final MapExp F;

  @Override
  public Map<String, String> options() {
    return Collections.emptyMap();
  }

  @Override
  public Collection<Pair<String, Kind>> deps() {
    return Util.union(I.deps(), F.deps());
  }

  public InstExpDelta(MapExp f, InstExp<Gen, Sk, X, Y> i) {
    I = i;
    F = f;
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    result = prime * result + ((F == null) ? 0 : F.hashCode());
    result = prime * result + ((I == null) ? 0 : I.hashCode());
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
    InstExpDelta<?, ?, ?, ?> other = (InstExpDelta<?, ?, ?, ?>) obj;
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
    return true;
  }

  @Override
  public synchronized SchExp type(AqlTyping G) {
    SchExp t0 = I.type(G);
    Pair<SchExp, SchExp> t1 = F.type(G);

    if (!G.eq(t1.second, t0)) {
      throw new RuntimeException("Type error: In " + this + " codomain of mapping is " + t1.first
          + " but instance has schema " + t0);
    }

    return t1.first;
  }

  @Override
  public String toString() {
    return "delta " + F + " " + I;
  }

  @Override
  public synchronized Instance<String, String, Sym, Fk, Att, Pair<String, X>, Y, Pair<String, X>, Y> eval0(AqlEnv env, boolean isC) {
    Mapping<String, String, Sym, Fk, Att, String, Fk, Att> f = F.eval(env, isC);
    Instance<String, String, Sym, Fk, Att, Gen, Sk, X, Y> i = I.eval(env, isC);
    if (isC) {
      throw new IgnoreException();
    }
    return new DeltaInstance<>(f, i);
  }

  @Override
  protected void allowedOptions(Set<AqlOption> set) {

  }

}