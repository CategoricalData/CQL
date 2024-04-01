package catdata.cql.exp;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.Program;
import catdata.Util;
import catdata.cql.AqlOptions;
import catdata.cql.Kind;
import catdata.cql.Schema;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.fdm.CqlPivot;

public final class SchExpPivot<Gen, Sk, X, Y> extends SchExp {
  public final InstExp<Gen, Sk, X, Y> I;
  public final Map<String, String> options;

  public <R, P, E extends Exception> R accept(P param, SchExpVisitor<R, P, E> v) throws E {
    return v.visit(param, this);
  }

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {
    I.map(f);
  }

  @Override
  protected void allowedOptions(Set<AqlOption> set) {
    set.add(AqlOption.require_consistency);
    set.add(AqlOption.allow_java_eqs_unsafe);
  }

  @Override
  public <R, P, E extends Exception> SchExp coaccept(P params, SchExpCoVisitor<R, P, E> v, R r) throws E {
    return v.visitSchExpPivot(params, r);
  }

  public SchExpPivot(InstExp<Gen, Sk, X, Y> I, List<Pair<String, String>> options) {
    this.options = Util.toMapSafely(options);
    this.I = I;
  }

  @Override
  public SchExp resolve(AqlTyping G, Program<Exp<?>> prog) {
    return this;
  }

  @Override
  protected Map<String, String> options() {
    return options;
  }

  @Override
  public Schema<String, String, Sym, Fk, Att> eval0(AqlEnv env, boolean isC) {
    AqlOptions strat = new AqlOptions(options, env.defaults);
    Schema<String, String, Sym, Fk, Att> l = new CqlPivot<>(I.eval(env, isC), strat).intI;
    return l;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
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
    SchExpPivot<?, ?, ?, ?> other = (SchExpPivot<?, ?, ?, ?>) obj;
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
  public Collection<Pair<String, Kind>> deps() {
    return I.deps();
  }

  @Override
  public String toString() {
    return "pivot " + I;
  }

  @Override
  public TyExp type(AqlTyping G) {
    return I.type(G).type(G);
  }

}
//////////////////////////////////////////////////////////////