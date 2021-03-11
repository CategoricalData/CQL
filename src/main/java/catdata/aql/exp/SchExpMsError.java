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

public final class SchExpMsError extends SchExp {

  String jdbcString;
  TyExp ty;
  String dom;

  @Override
  public Collection<Pair<String, Kind>> deps() {
    return ty.deps();
  }

  public <R, P, E extends Exception> R accept(P param, SchExpVisitor<R, P, E> v) throws E {
    return v.visit(param, this);
  }

  @Override
  public <R, P, E extends Exception> SchExp coaccept(P params, SchExpCoVisitor<R, P, E> v, R r) throws E {
    return v.visitSchExpMsError(params, r);
  }

  @Override
  public Map<String, String> options() {
    return Collections.emptyMap();
  }

  public SchExpMsError(String dom, String jdbcString, TyExp ty) {
    this.jdbcString = jdbcString;
    this.ty = ty;
    this.dom = dom;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((dom == null) ? 0 : dom.hashCode());
    result = prime * result + ((jdbcString == null) ? 0 : jdbcString.hashCode());
    result = prime * result + ((ty == null) ? 0 : ty.hashCode());
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
    SchExpMsError other = (SchExpMsError) obj;
    if (dom == null) {
      if (other.dom != null)
        return false;
    } else if (!dom.equals(other.dom))
      return false;
    if (jdbcString == null) {
      if (other.jdbcString != null)
        return false;
    } else if (!jdbcString.equals(other.jdbcString))
      return false;
    if (ty == null) {
      if (other.ty != null)
        return false;
    } else if (!ty.equals(other.ty))
      return false;
    return true;
  }

  @Override
  public Schema<String, String, Sym, Fk, Att> eval0(AqlEnv env, boolean isC) {
    String body = """
        entities
          Error
        attributes
          message_id severity logged text : Error -> """;
    return CombinatorParser.parseSchExpRaw("literal : " + ty + "{\n" + body + dom + "\n}").eval(env, isC);
  }

  @Override
  public String toString() {
    return "ms_error " + dom + " " + jdbcString + " : " + ty;
  }

  @Override
  public SchExp resolve(AqlTyping G, Program<Exp<?>> prog) {
    return this;
  }

  @Override
  public TyExp type(AqlTyping G) {
    ty.type(G);
    return ty;
  }

  @Override
  protected void allowedOptions(Set<AqlOption> set) {
  }

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {
    ty.mapSubExps(f);
  }
}