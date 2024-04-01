package catdata.cql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.Util;
import catdata.cql.Kind;
import catdata.cql.Transform;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.fdm.DeltaTransform;

public final class TransExpDelta<Gen, Sk1, Gen2, Sk2, X1, Y1, X2, Y2>
    extends TransExp<Pair<String, X1>, Y1, Pair<String, X2>, Y2, Pair<String, X1>, Y1, Pair<String, X2>, Y2> {

  public final MapExp F;
  public final TransExp<Gen, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> t;

  @Override
  public Map<String, String> options() {
    return Collections.emptyMap();
  }

  public TransExpDelta(MapExp F, TransExp<Gen, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> t) {
    this.F = F;
    this.t = t;
  }

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {
    F.map(f);
    t.map(f);
  }

  public <R, P, E extends Exception> R accept(P params, TransExpVisitor<R, P, E> v) throws E {
    return v.visit(params, this);
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    result = prime * result + ((F == null) ? 0 : F.hashCode());
    result = prime * result + ((t == null) ? 0 : t.hashCode());
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
    TransExpDelta<?, ?, ?, ?, ?, ?, ?, ?> other = (TransExpDelta<?, ?, ?, ?, ?, ?, ?, ?>) obj;
    if (F == null) {
      if (other.F != null)
        return false;
    } else if (!F.equals(other.F))
      return false;
    if (t == null) {
      if (other.t != null)
        return false;
    } else if (!t.equals(other.t))
      return false;
    return true;
  }

  @Override
  public synchronized Pair<InstExp<Pair<String, X1>, Y1, Pair<String, X1>, Y1>, InstExp<Pair<String, X2>, Y2, Pair<String, X2>, Y2>> type(
      AqlTyping G) {
    Pair<InstExp<Gen, Sk1, X1, Y1>, InstExp<Gen2, Sk2, X2, Y2>> x = t.type(G);
    if (!G.eq(x.first.type(G), F.type(G).second)) {
      throw new RuntimeException("In " + this + ", mapping codomain is " + F.type(G).second
          + " but transform domain schema is " + x.first.type(G));
    }
    InstExp<Pair<String, X1>, Y1, Pair<String, X1>, Y1> a = new InstExpDelta<>(F, x.first);
    InstExp<Pair<String, X2>, Y2, Pair<String, X2>, Y2> b = new InstExpDelta<>(F, x.second);
    return new Pair<>(a, b);
  }

  @Override
  public Transform<String, String, Sym, Fk, Att, Pair<String, X1>, Y1, Pair<String, X2>, Y2, Pair<String, X1>, Y1, Pair<String, X2>, Y2> eval0(
      AqlEnv env, boolean isC) {
    if (isC) {
      F.eval(env, true);
      t.eval(env, true);
      throw new IgnoreException();
    }
    return new DeltaTransform<>(F.eval(env, false), t.eval(env, false));
  }

  @Override
  public String toString() {
    return "delta " + F + " " + t;
  }

  @Override
  public Collection<Pair<String, Kind>> deps() {
    return Util.union(F.deps(), t.deps());
  }

  @Override
  protected void allowedOptions(Set<AqlOption> set) {
  }

}