package catdata.cql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.LocStr;
import catdata.Pair;
import catdata.Unit;
import catdata.cql.Kind;
import catdata.cql.TypeSide;
import catdata.cql.AqlOptions.AqlOption;

public final class TyExpRdf extends TyExp {

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
    List<LocStr> tys = Collections.singletonList(new LocStr(0, "Dom"));
    List<Pair<LocStr, String>> jtys = Collections.singletonList(new Pair<>(new LocStr(0, "Dom"), "java.lang.Object"));
    List<Pair<LocStr, String>> jps = Collections.singletonList(new Pair<>(new LocStr(0, "Dom"), "x => x"));
    return new TyExpRaw(Collections.emptyList(), tys, Collections.emptyList(), Collections.emptyList(), jtys, jps, Collections.emptyList(), Collections.emptyList()).eval(env, isC);
  }

  @Override
  public String toString() {
    return "rdf";
  }

  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public boolean equals(Object o) {
    return (o != null && o instanceof TyExpRdf);
  }

  @Override
  public Object type(AqlTyping G) {
    return Unit.unit;
  }

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {

  }

}