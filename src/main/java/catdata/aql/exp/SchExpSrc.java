package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.Program;
import catdata.aql.Kind;
import catdata.aql.Schema;
import catdata.aql.AqlOptions.AqlOption;

public class SchExpSrc extends SchExp {

  public <R, P, E extends Exception> R accept(P param, SchExpVisitor<R, P, E> v) throws E {
    return v.visit(param, this);
  }

  public final MapExp exp;

  public SchExpSrc(MapExp exp) {
    this.exp = exp;
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
    SchExpSrc other = (SchExpSrc) obj;
    return exp.equals(other.exp);
  }

  @Override
  public String toString() {
    return "dom_m " + exp;
  }

  @Override
  public Schema<String, String, Sym, Fk, Att> eval0(AqlEnv env, boolean isC) {
    return exp.eval(env, isC).src;
  }

  @Override
  public Collection<Pair<String, Kind>> deps() {
    return exp.deps();
  }

  @Override
  public TyExp type(AqlTyping G) {
    return exp.type(G).first.type(G);
  }

  @Override
  public <R, P, E extends Exception> SchExp coaccept(P params, SchExpCoVisitor<R, P, E> v, R r) throws E {
    return v.visitSchExpSrc(params, r);
  }

  @Override
  protected void allowedOptions(Set<AqlOption> set) {

  }

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {
    exp.map(f);
  }

}