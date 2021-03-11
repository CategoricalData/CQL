package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.Unit;
import catdata.aql.Kind;
import catdata.aql.TypeSide;
import catdata.aql.AqlOptions.AqlOption;

public final class TyExpEmpty extends TyExp {
  public Unit type(AqlTyping t) {
    return Unit.unit;
  }

  @Override
  public <R, P, E extends Exception> R accept(P params, TyExpVisitor<R, P, E> v) throws E {
    return v.visit(params, this);
  }

  @Override
  public <R, P, E extends Exception> TyExpEmpty coaccept(P params, TyExpCoVisitor<R, P, E> v, R r) throws E {
    return v.visitTyExpEmpty(params, r);
  }

  @Override
  public Collection<Pair<String, Kind>> deps() {
    return Collections.emptyList();
  }

  @Override
  protected void allowedOptions(Set<AqlOption> set) {

  }

  @Override
  public Map<String, String> options() {
    return Collections.emptyMap();
  }

  @Override
  public synchronized TypeSide<String, Sym> eval0(AqlEnv env, boolean isC) {
    return TypeSide.initial(env.defaults);
  }

  @Override
  public String toString() {
    return "empty";
  }

  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public boolean equals(Object o) {
    return (o != null && o instanceof TyExpEmpty);
  }

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {
  }

}