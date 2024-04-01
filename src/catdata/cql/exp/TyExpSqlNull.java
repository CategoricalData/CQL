package catdata.cql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.Unit;
import catdata.cql.Kind;
import catdata.cql.SqlTypeSide2;
import catdata.cql.TypeSide;
import catdata.cql.AqlOptions.AqlOption;

public final class TyExpSqlNull extends TyExp {

  public final TyExp parent;

  public TyExpSqlNull(TyExp parent) {
    this.parent = parent;
  }

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
    return parent.deps();
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
    return SqlTypeSide2.FOR_TY.make(parent.eval(env, isC), env.defaults);
  }

  @Override
  public String toString() {
    return "sqlNull " + parent;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((parent == null) ? 0 : parent.hashCode());
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
    TyExpSqlNull other = (TyExpSqlNull) obj;
    if (parent == null) {
      if (other.parent != null)
        return false;
    } else if (!parent.equals(other.parent))
      return false;
    return true;
  }

  @Override
  public Object type(AqlTyping G) {
    return Unit.unit;
  }

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {
    f.accept(parent);
    parent.mapSubExps(f);
  }

}
