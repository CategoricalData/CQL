package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Instance;
import catdata.aql.Kind;
import catdata.aql.Term;
import catdata.aql.Transform;
import catdata.aql.fdm.DistinctInstance;

public final class TransExpDistinct2<Gen, Sk, X, Y> extends TransExp<Gen, Sk, Gen, Sk, X, Y, X, Y> {

  public final InstExp<Gen, Sk, X, Y> t;

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {
    t.map(f);
  }

  @Override
  public Map<String, String> options() {
    return Collections.emptyMap();
  }

  public TransExpDistinct2(InstExp<Gen, Sk, X, Y> t) {
    this.t = t;
  }

  public <R, P, E extends Exception> R accept(P params, TransExpVisitor<R, P, E> v) throws E {
    return v.visit(params, this);
  }

  @Override
  public int hashCode() {
    return t.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    TransExpDistinct2<?, ?, ?, ?> other = (TransExpDistinct2<?, ?, ?, ?>) obj;
    if (t == null) {
      if (other.t != null)
        return false;
    } else if (!t.equals(other.t))
      return false;
    return true;
  }

  @Override
  public Pair<InstExp<Gen, Sk, X, Y>, InstExp<Gen, Sk, X, Y>> type(AqlTyping G) {
    return new Pair<>(t, new InstExpDistinct<>(t));
  }

  @Override
  public Transform<String, String, Sym, Fk, Att, Gen, Sk, Gen, Sk, X, Y, X, Y> eval0(AqlEnv env, boolean isC) {
    Instance<String, String, Sym, Fk, Att, Gen, Sk, X, Y> x = t.eval(env, isC);
    if (isC) {
      throw new IgnoreException();
    }
    Instance<String, String, Sym, Fk, Att, Gen, Sk, X,Y> y = new DistinctInstance<>(x, env.defaults);
    
    
    return new Transform<>() {

      @Override
      public BiFunction<Gen, String, Term<Void, String, Void, Fk, Void, Gen, Void>> gens() {
        return (gen, en) -> Term.Gen(gen);
      }

      @Override
      public BiFunction<Sk, String, Term<String, String, Sym, Fk, Att, Gen, Sk>> sks() {
        return (sk, ty) -> Term.Sk(sk);
      }

      @Override
      public Instance<String, String, Sym, Fk, Att, Gen, Sk, X, Y> src() {
        return x;
      }

      @Override
      public Instance<String, String, Sym, Fk, Att, Gen, Sk, X, Y> dst() {
        return y;
      }
      
    };
  }

  @Override
  public String toString() {
    return "distinct_return " + t;
  }

  @Override
  public Collection<Pair<String, Kind>> deps() {
    return t.deps();
  }

  @Override
  protected void allowedOptions(Set<AqlOption> set) {

  }

}