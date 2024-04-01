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

public class SchExpColim extends SchExp {

  public final ColimSchExp exp;

  public <R, P, E extends Exception> R accept(P param, SchExpVisitor<R, P, E> v) throws E {
    return v.visit(param, this);
  }

  public SchExpColim(ColimSchExp exp) {
    this.exp = exp;
  }

  @Override
  public <R, P, E extends Exception> SchExp coaccept(P params, SchExpCoVisitor<R, P, E> v, R r) throws E {
    return v.visitSchExpColim(params, r);
  }

  @Override
  public SchExp resolve(AqlTyping G, Program<Exp<?>> prog) {
    return this;
  }

  @Override
  public Map<String, String> options() {
    return Collections.emptyMap();
  }

  @Override
  public int hashCode() {
    return exp.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    SchExpColim other = (SchExpColim) obj;
    if (exp == null) {
      if (other.exp != null)
        return false;
    } else if (!exp.equals(other.exp))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "getSchema " + exp;
  }

  @Override
  public synchronized Schema<String, String, Sym, Fk, Att> eval0(AqlEnv env, boolean isC) {
    return exp.eval(env, isC).schemaStr;
  }

  @Override
  public Collection<Pair<String, Kind>> deps() {
    return exp.deps();
  }

  @Override
  public TyExp type(AqlTyping G) {
    return exp.typeOf(G);
  }

  @Override
  protected void allowedOptions(Set<AqlOption> set) {

  }

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {
    exp.map(f);
  }

}
