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

public final class SchExpEmpty extends SchExp {

  public final TyExp typeSide;

  public SchExp resolve(AqlTyping G, Program<Exp<?>> prog) {
    return new SchExpEmpty(typeSide.resolve(prog));
  }

  @Override
  public <R, P, E extends Exception> SchExp coaccept(P params, SchExpCoVisitor<R, P, E> v, R r) throws E {
    return v.visitSchExpEmpty(params, r);
  }

  public <R, P, E extends Exception> R accept(P param, SchExpVisitor<R, P, E> v) throws E {
    return v.visit(param, this);
  }

  @Override
  public Map<String, String> options() {
    return Collections.emptyMap();
  }

  @Override
  public Collection<Pair<String, Kind>> deps() {
    return typeSide.deps();
  }

  public SchExpEmpty(TyExp typeSide) {
    this.typeSide = typeSide;
  }

  @Override
  public int hashCode() {
    return typeSide.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    SchExpEmpty other = (SchExpEmpty) obj;
    return typeSide.equals(other.typeSide);
  }

  @Override
  public String toString() {
    return "empty : " + typeSide;
  }

  @Override
  public Schema<String, String, Sym, Fk, Att> eval0(AqlEnv env, boolean isC) {
    return Schema.terminal(typeSide.eval(env, isC));
  }

  @Override
  public TyExp type(AqlTyping G) {
    return typeSide;
  }

  @Override
  protected void allowedOptions(Set<AqlOption> set) {
  }

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {
    typeSide.map(f);
  }

}