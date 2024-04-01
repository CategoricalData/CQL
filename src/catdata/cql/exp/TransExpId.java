package catdata.cql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.Util;
import catdata.cql.Kind;
import catdata.cql.Transform;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.exp.InstExp.InstExpVar;
import catdata.cql.fdm.IdentityTransform;

public final class TransExpId<Gen, Sk, X, Y> extends TransExp<Gen, Sk, Gen, Sk, X, Y, X, Y> {

  @Override
  protected void allowedOptions(Set<AqlOption> set) {

  }

  @Override
  public Collection<Pair<String, Kind>> deps() {
    if (inst2.isEmpty()) {
      return inst.deps();
    }
    return Util.union(inst.deps(), inst2.get().deps());
  }

  @Override
  public Map<String, String> options() {
    return Collections.emptyMap();
  }

  public final InstExp<Gen, Sk, X, Y> inst;
  public final Optional<InstExp<Gen, Sk, X, Y>> inst2;

  public TransExpId(InstExp<Gen, Sk, X, Y> inst) {
    this.inst = inst;
    this.inst2 = Optional.empty();
  }

  public TransExpId(InstExp<Gen, Sk, X, Y> inst, InstExp<Gen, Sk, X, Y> inst2) {
    this.inst = inst;
    this.inst2 = Optional.of(inst2);
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    result = prime * result + inst.hashCode();
    result = prime * result + inst2.hashCode();
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
    TransExpId<?, ?, ?, ?> other = (TransExpId<?, ?, ?, ?>) obj;
    return inst.equals(other.inst) && inst2.equals(other.inst2);
  }

  @Override
  public String toString() {
    if (inst2.isEmpty()) {
      return "identity " + inst;
    }
    return "include " + inst + " " + inst2.get();
  }

  @Override
  public synchronized Transform<String, String, Sym, Fk, Att, Gen, Sk, Gen, Sk, X, Y, X, Y> eval0(AqlEnv env, boolean isC) {
    if (inst2.isEmpty()) {
      return new IdentityTransform<>(inst.eval(env, isC), Optional.empty());
    }
    return new IdentityTransform<>(inst.eval(env, isC), Optional.of(inst2.get().eval(env, isC)));
  }

  @Override
  public Pair<InstExp<Gen, Sk, X, Y>, InstExp<Gen, Sk, X, Y>> type(AqlTyping G) {
    SchExp sch = inst.type(G);
    if (inst2.isEmpty()) {
      return new Pair<>(inst, inst);
    }
    SchExp sch2 = inst2.get().type(G);
    if (!sch.equals(sch2)) {
      throw new RuntimeException("Schema mismatch: " + sch + " and " + sch2);
    }
    if (inst instanceof InstExpVar && inst2.get() instanceof InstExpVar) {
      if (!((G.prog.exps.get(((InstExpVar)inst).var) instanceof InstExpSigma) || (G.prog.exps.get(((InstExpVar)inst).var) instanceof InstExpRaw))) {
        throw new RuntimeException(inst + " not bound to a literal or sigma instance, as required for inclusion.");
      }
      if (!((G.prog.exps.get(((InstExpVar)inst2.get()).var) instanceof InstExpSigma) || (G.prog.exps.get(((InstExpVar)inst2.get()).var) instanceof InstExpRaw))) {
        throw new RuntimeException(inst2.get() + " not bound to a literal instance, as required for inclusion.");
      }
      return new Pair<>(inst, inst2.get());
    }
    throw new RuntimeException("Inclusion not of form include var var, as required.");
    
  }

  public <R, P, E extends Exception> R accept(P params, TransExpVisitor<R, P, E> v) throws E {
    return v.visit(params, this);
  }

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {
    inst.map(f);
    if (inst2.isPresent()) {
      inst2.get().map(f);
    }
  }

}