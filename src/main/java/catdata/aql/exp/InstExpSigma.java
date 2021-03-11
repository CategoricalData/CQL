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
import catdata.aql.Mapping;
import catdata.aql.fdm.SigmaInstance;

public final class InstExpSigma<Gen, Sk, X, Y> extends InstExp<Gen, Sk, Integer, Chc<Sk, Pair<Integer, Att>>> {

  public final InstExp<Gen, Sk, X, Y> I;
  public final MapExp F;
  public final Map<String, String> options;

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

  @Override
  public Map<String, String> options() {
    return options;
  }

  @Override
  public Collection<Pair<String, Kind>> deps() {
    return Util.union(I.deps(), F.deps());
  }

  public InstExpSigma(MapExp f, InstExp<Gen, Sk, X, Y> i, Map<String, String> options) {
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
    InstExpSigma<?, ?, ?, ?> other = (InstExpSigma<?, ?, ?, ?>) obj;
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
  public synchronized SchExp type(AqlTyping G) {
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
    return "sigma " + F + " " + I;
  }

  @Override
  public synchronized SigmaInstance<String, String, Sym, Fk, Att, Gen, Sk, String, Fk, Att, X, Y> eval0(AqlEnv env, boolean isC) {
    Mapping<String, String, catdata.aql.exp.Sym, Fk, Att, String, Fk, Att> F = this.F.eval(env, isC);
    Instance<String, String, Sym, Fk, Att, Gen, Sk, X, Y> I = this.I.eval(env, isC);
    if (isC) {
      throw new IgnoreException();
    }

  //  Collage<Ty, En, Sym, Fk, Att, Gen, Sk> col = new Collage<>(F.dst.collage());

    /*col.sks.putAll(I.sks());
    for (Gen gen : I.gens().keySet()) {
      col.gens.put(gen, F.ens.get(I.gens().get(gen)));
    }

    for (Pair<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>> eq : I.eqs()) {
      col.eqs.add(new Eq<>(null, F.trans(eq.first), F.trans(eq.second)));
    }*/
    return new SigmaInstance<>(F, I, new AqlOptions(options, env.defaults));
  }

  @Override
  protected void allowedOptions(Set<AqlOption> set) {
    set.add(AqlOption.require_consistency);
    set.add(AqlOption.allow_java_eqs_unsafe);
    set.addAll(AqlOptions.proverOptionNames());
  }

}