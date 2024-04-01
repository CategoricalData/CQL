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

public final class SchExpMsQuery extends SchExp {

  private final String dom;
  private final TyExp ty;

  @Override
  public Collection<Pair<String, Kind>> deps() {
    return ty.deps();
  }

  public <R, P, E extends Exception> R accept(P param, SchExpVisitor<R, P, E> v) throws E {
    return v.visit(param, this);
  }

  @Override
  public <R, P, E extends Exception> SchExp coaccept(P params, SchExpCoVisitor<R, P, E> v, R r) throws E {
    return v.visitSchExpMsQuery(params, r);
  }

  @Override
  public Map<String, String> options() {
    return Collections.emptyMap();
  }

  public SchExpMsQuery(TyExp t, String d) {
    this.dom = d;
    this.ty = t;
  }

  @Override
  public Schema<String, String, Sym, Fk, Att> eval0(AqlEnv env, boolean isC) {
    StringBuffer sb = new StringBuffer();
    try {
      for (int i = 0; i < MsSqlQuery.size; i++) {
        String s = (String) MsSqlQuery.class.getField("str" + i).get(null);
        sb.append("\n");
        sb.append(s.replace("CQL_RESERVED_CQL", dom));
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      throw new RuntimeException(ex);
    }
    return CombinatorParser.parseSchExpRaw("literal : " + ty + "{\n" + sb.toString() + "\n}").eval(env, isC);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((dom == null) ? 0 : dom.hashCode());
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
    SchExpMsQuery other = (SchExpMsQuery) obj;
    if (dom == null) {
      if (other.dom != null)
        return false;
    } else if (!dom.equals(other.dom))
      return false;
    if (ty == null) {
      if (other.ty != null)
        return false;
    } else if (!ty.equals(other.ty))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "ms_query " + ty + " " + dom;
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