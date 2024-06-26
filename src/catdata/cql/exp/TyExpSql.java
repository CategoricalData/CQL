package catdata.cql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.Unit;
import catdata.cql.Kind;
import catdata.cql.SqlTypeSide;
import catdata.cql.TypeSide;
import catdata.cql.AqlOptions.AqlOption;

public final class TyExpSql extends TyExp {

  @Override
  public <R, P, E extends Exception> TyExp coaccept(P params, TyExpCoVisitor<R, P, E> v, R r) throws E {
    return v.visitTyExpSql(params, r);
  }

  @Override
  public <R, P, E extends Exception> R accept(P params, TyExpVisitor<R, P, E> v) throws E {
    return v.visit(params, this);
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
    return SqlTypeSide.SqlTypeSide(env.defaults);
  }

  @Override
  public String toString() {
    return "sql";
  }

  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public boolean equals(Object o) {
    return (o != null && o instanceof TyExpSql);
  }

  @Override
  public Object type(AqlTyping G) {
    return Unit.unit;
  }

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {

  }

}