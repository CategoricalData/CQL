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
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.fdm.DiffInstance;

public final class InstExpDiff<Gen, Sk, X, Y, X1, Gen1, Sk1> extends InstExp<X, Y, X, Y> {

  public <R, P, E extends Exception> R accept(P param, InstExpVisitor<R, P, E> v) throws E {
    return v.visit(param, this);
  }

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {
    I.map(f);
    J.map(f);
  }

  @Override
  public Collection<InstExp<?, ?, ?, ?>> direct(AqlTyping G) {
    return Util.union(Collections.singletonList(I), Collections.singletonList(J));
  }

  @Override
  public String toString() {
    return "except " + I + " " + J;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((I == null) ? 0 : I.hashCode());
    result = prime * result + ((J == null) ? 0 : J.hashCode());
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
    InstExpDiff<?, ?, ?, ?, ?, ?, ?> other = (InstExpDiff<?, ?, ?, ?, ?, ?, ?>) obj;
    if (I == null) {
      if (other.I != null)
        return false;
    } else if (!I.equals(other.I))
      return false;
    if (J == null) {
      if (other.J != null)
        return false;
    } else if (!J.equals(other.J))
      return false;
    return true;
  }

  public final InstExp<Gen, Sk, X, Y> I;
  public final InstExp<Gen1, Sk1, X1, Y> J;

  public InstExpDiff(InstExp<Gen, Sk, X, Y> i, InstExp<Gen1, Sk1, X1, Y> j) {
    I = i;
    J = j;
  }

  @Override
  public SchExp type(AqlTyping G) {
    SchExp x = I.type(G);
    SchExp y = J.type(G);
    if (!x.equals(y)) { // TODO aql schema equality
      throw new RuntimeException("Instances have difference schemas");
    }
    return x;
  }

  @Override
  protected void allowedOptions(Set<AqlOption> set) {

  }

  @Override
  protected Map<String, String> options() {
    return Collections.emptyMap();
  }

  @Override
  public synchronized Instance<String, String, catdata.cql.exp.Sym, catdata.cql.exp.Fk, catdata.cql.exp.Att, X, Y, X, Y> eval0(
      AqlEnv env, boolean isC) {
    Instance<String, String, catdata.cql.exp.Sym, catdata.cql.exp.Fk, catdata.cql.exp.Att, Gen, Sk, X, Y> a = I
        .eval(env, isC);
    Instance<String, String, catdata.cql.exp.Sym, catdata.cql.exp.Fk, catdata.cql.exp.Att, Gen1, Sk1, X1, Y> b = J
        .eval(env, isC);
    if (!a.schema().fks.isEmpty()) {
      throw new RuntimeException("Cannot difference when there are foreign keys.");
    } else if (!a.algebra().talg().equals(b.algebra().talg())) {
      throw new RuntimeException("Cannot difference when type algebras not the same.");
    }
    if (isC) {
      throw new IgnoreException();
    }
    return new DiffInstance<>(a, b, true, false);
  }

  @Override
  public Collection<Pair<String, Kind>> deps() {
    return Util.union(I.deps(), J.deps());
  }

}