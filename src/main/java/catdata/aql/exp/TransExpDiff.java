package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.Util;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Instance;
import catdata.aql.Kind;
import catdata.aql.Term;
import catdata.aql.Transform;
import catdata.aql.fdm.DiffInstance;
import catdata.aql.fdm.LiteralTransform;

public final class TransExpDiff<Gen, Sk, X, Y, Gen1, Gen2, Sk1, Sk2, X1, X2>
    extends TransExp<X1, Y, X2, Y, X1, Y, X2, Y> {

  public final TransExp<Gen1, Sk1, Gen2, Sk2, X1, Y, X2, Y> h;

  public final InstExp<Gen, Sk, X, Y> I;

  public TransExpDiff(InstExp<Gen, Sk, X, Y> i, TransExp<Gen1, Sk1, Gen2, Sk2, X1, Y, X2, Y> h) {
    this.h = h;
    I = i;
  }

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {
    h.map(f);
    I.map(f);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((I == null) ? 0 : I.hashCode());
    result = prime * result + ((h == null) ? 0 : h.hashCode());
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
    TransExpDiff<?, ?, ?, ?, ?, ?, ?, ?, ?, ?> other = (TransExpDiff<?, ?, ?, ?, ?, ?, ?, ?, ?, ?>) obj;
    if (I == null) {
      if (other.I != null)
        return false;
    } else if (!I.equals(other.I))
      return false;
    if (h == null) {
      if (other.h != null)
        return false;
    } else if (!h.equals(other.h))
      return false;
    return true;
  }

  @Override
  public Map<String, String> options() {
    return Collections.emptyMap();
  }

  public <R, P, E extends Exception> R accept(P params, TransExpVisitor<R, P, E> v) throws E {
    return v.visit(params, this);
  }

  @Override
  public Pair<InstExp<X1, Y, X1, Y>, InstExp<X2, Y, X2, Y>> type(AqlTyping G) {
    InstExp<Gen1, Sk1, X1, Y> src = h.type(G).first;
    InstExp<Gen2, Sk2, X2, Y> dst = h.type(G).second;
    InstExpDiff<Gen1, Sk1, X1, Y, X, Gen, Sk> l = new InstExpDiff<>(src, I);
    InstExpDiff<Gen2, Sk2, X2, Y, X, Gen, Sk> r = new InstExpDiff<>(dst, I);
    InstExp<X1, Y, X1, Y> ll = l;
    InstExp<X2, Y, X2, Y> rr = r;
    return new Pair<>(ll, rr);
  }

  @Override
  public Transform<String, String, Sym, Fk, Att, X1, Y, X2, Y, X1, Y, X2, Y> eval0(AqlEnv env, boolean isC) {
    Instance<String, String, Sym, Fk, Att, Gen, Sk, X, Y> i = I.eval(env, isC);
    Transform<String, String, Sym, Fk, Att, Gen1, Sk1, Gen2, Sk2, X1, Y, X2, Y> H = h.eval(env, isC);
    if (isC) {
      throw new IgnoreException();
    }
    DiffInstance<String, String, Sym, Fk, Att, Gen1, Sk1, X1, Y> a = new DiffInstance<>(H.src(), i, true, false);
    DiffInstance<String, String, Sym, Fk, Att, Gen2, Sk2, X2, Y> b = new DiffInstance<>(H.dst(), i, true, false);

    BiFunction<X1, String, Term<Void, String, Void, Fk, Void, X2, Void>> m; 
    BiFunction<Y, String, Term<String, String, Sym, Fk, Att, X2, Y>> n; 

    m = (x1,t) -> Term.Gen(H.repr(t, x1));
    n = (y,t) -> Term.Sk(y);

    return new LiteralTransform<>(m, n, a, b, true);
  }

  @Override
  public String toString() {
    return "except " + h + " " + I;
  }

  @Override
  public Collection<Pair<String, Kind>> deps() {
    return Util.union(I.deps(), h.deps());
  }

  @Override
  protected void allowedOptions(Set<AqlOption> set) {

  }

}