package catdata.cql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.Program;
import catdata.cql.Kind;
import catdata.cql.Schema;
import catdata.cql.AqlOptions.AqlOption;

public final class SchExpInst<Gen, Sk, X, Y> extends SchExp {
  public final InstExp<Gen, Sk, X, Y> inst;

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {
    inst.map(f);
  }

  @Override
  public <R, P, E extends Exception> SchExp coaccept(P params, SchExpCoVisitor<R, P, E> v, R r) throws E {
    return v.visitSchExpInst(params, r);
  }

  public <R, P, E extends Exception> R accept(P param, SchExpVisitor<R, P, E> v) throws E {
    return v.visit(param, this);
  }

  @Override
  public SchExp resolve(AqlTyping G, Program<Exp<?>> prog) {
    return inst.type(G);
  }

  @Override
  public Map<String, String> options() {
    return Collections.emptyMap();
  }

  @Override
  public Collection<Pair<String, Kind>> deps() {
    return inst.deps();
  }

  public SchExpInst(InstExp<Gen, Sk, X, Y> inst) {
    if (inst == null) {
      throw new RuntimeException("Attempt to get schema for null instance");
    }
    this.inst = inst;
  }

  @Override
  public int hashCode() {
    return inst.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    SchExpInst<?, ?, ?, ?> other = (SchExpInst<?, ?, ?, ?>) obj;
    return inst.equals(other.inst);
  }

  @Override
  public String toString() {
    return "schemaOf " + inst;
  }

  @Override
  public Schema<String, String, Sym, Fk, Att> eval0(AqlEnv env, boolean isC) {
    return inst.eval(env, isC).schema();
  }

  @Override
  public TyExp type(AqlTyping G) {
    inst.type(G);
    return new TyExpSch(this);
  }

  @Override
  protected void allowedOptions(Set<AqlOption> set) {
  }

}