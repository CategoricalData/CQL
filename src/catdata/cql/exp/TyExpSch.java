package catdata.cql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.Unit;
import catdata.cql.Kind;
import catdata.cql.TypeSide;
import catdata.cql.AqlOptions.AqlOption;

public final class TyExpSch extends TyExp {

  @Override
  public <R, P, E extends Exception> R accept(P params, TyExpVisitor<R, P, E> v) throws E {
    return v.visit(params, this);
  }

  @Override
  public Map<String, String> options() {
    return Collections.emptyMap();
  }

  public final SchExp schema;

  public TyExpSch(SchExp schema) {
    this.schema = schema;
  }

  @Override
  public int hashCode() {
    return schema.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    TyExpSch other = (TyExpSch) obj;
    return schema.equals(other.schema);
  }

  @Override
  public String toString() {
    return "typesideOf " + schema;
  }

  @Override
  public synchronized TypeSide<String, Sym> eval0(AqlEnv env, boolean isC) {
    return schema.eval(env, isC).typeSide;
  }

  @Override
  public Collection<Pair<String, Kind>> deps() {
    return schema.deps();
  }

  @Override
  public <R, P, E extends Exception> TyExp coaccept(P params, TyExpCoVisitor<R, P, E> v, R r) throws E {
    return v.visitTyExpSch(params, r);
  }

  @Override
  protected void allowedOptions(Set<AqlOption> set) {
  }

  public Unit type(AqlTyping t) {
    schema.type(t);
    return Unit.unit;
  }

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {
    schema.map(f);
  }

}