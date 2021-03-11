package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Kind;
import catdata.aql.Mapping;
import catdata.aql.fdm.AqlPivot;

public final class MapExpPivot<Gen, Sk, X, Y> extends MapExp {

  @Override
  protected void allowedOptions(Set<AqlOption> set) {
    set.add(AqlOption.require_consistency);
    set.add(AqlOption.allow_java_eqs_unsafe);
  }

  public <R, P, E extends Exception> R accept(P params, MapExpVisitor<R, P, E> v) throws E {
    return v.visit(params, this);
  }

  @Override
  public String toString() {
    return "pivot " + I;
  }

  public final InstExp<Gen, Sk, X, Y> I;
  public final Map<String, String> ops;

  public MapExpPivot(InstExp<Gen, Sk, X, Y> i, List<Pair<String, String>> ops) {
    I = i;
    this.ops = Util.toMapSafely(ops);
  }

  @Override
  public Pair<SchExp, SchExp> type(AqlTyping G) {
    return new Pair<>(new SchExpPivot<>(I, Collections.emptyList()), I.type(G));
  }

  @Override
  protected Map<String, String> options() {
    return ops;
  }

  @Override
  public Mapping<String, String, Sym, Fk, catdata.aql.exp.Att, String, Fk, Att> eval0(AqlEnv env, boolean isC) {
    AqlOptions strat = new AqlOptions(ops, env.defaults);
    Mapping<String, String, Sym, catdata.aql.exp.Fk, catdata.aql.exp.Att, String, Fk, Att> l = new AqlPivot<>(
        I.eval(env, isC), strat).F;
    return l;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((I == null) ? 0 : I.hashCode());
    result = prime * result + ((ops == null) ? 0 : ops.hashCode());
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
    MapExpPivot<?, ?, ?, ?> other = (MapExpPivot<?, ?, ?, ?>) obj;
    if (I == null) {
      if (other.I != null)
        return false;
    } else if (!I.equals(other.I))
      return false;
    if (ops == null) {
      if (other.ops != null)
        return false;
    } else if (!ops.equals(other.ops))
      return false;
    return true;
  }

  @Override
  public Collection<Pair<String, Kind>> deps() {
    return I.deps();
  }

  @Override
  public <R, P, E extends Exception> MapExp coaccept(P params, MapExpCoVisitor<R, P, E> v, R r) throws E {
    return v.visitMapExpPivot(params, r);
  }

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {
    I.map(f);
  }

}