package catdata.cql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.LocStr;
import catdata.Pair;
import catdata.cql.Instance;
import catdata.cql.Kind;
import catdata.cql.AqlOptions.AqlOption;

public class InstExpMsError extends InstExp<String, String, String, String> {

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
    InstExpMsError other = (InstExpMsError) obj;
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

  public final String jdbcString;
  public final TyExp ty;
  public final String dom;

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {
    this.ty.map(f);
  }

  @Override
  public Collection<InstExp<?, ?, ?, ?>> direct(AqlTyping G) {
    return Collections.emptySet();
  }

  public <R, P, E extends Exception> R accept(P param, InstExpVisitor<R, P, E> v) throws E {
    return v.visit(param, this);
  }

  public InstExpMsError(String dom, String jdbcString, TyExp ty) {
    this.dom = dom;
    this.jdbcString = jdbcString;
    this.ty = ty;
  }

  @Override
  protected void allowedOptions(Set<AqlOption> set) {

  }

  @Override
  public SchExp type(AqlTyping G) {
    ty.type(G);
    return new SchExpMsError(dom, jdbcString, ty);
  }

  @Override
  protected Map<String, String> options() {
    return Collections.emptyMap();
  }

  @Override
  protected Instance<String, String, Sym, Fk, Att, String, String, String, String> eval0(AqlEnv env, boolean isCompileTime) {
    var schema = new SchExpMsError(dom, jdbcString, ty);

    List<Pair<LocStr, String>> l = new LinkedList<>();
    String s = """
        SELECT message_id, severity, text,
                logged = CASE is_event_logged WHEN 0 THEN 'false' ELSE 'true' END
                FROM sys.messages
                WHERE language_id = 1033
                ORDER BY message_id""";
 
    l.add(new Pair<>(new LocStr(0,"Error"), s));
    List<Pair<String, String>> map = new LinkedList<>();
    map.add(new Pair<>(AqlOption.import_col_seperator.toString(), ""));
    
//    map.add(new Pair<>(AqlOption.prepend_entity_on_ids.toString(), "false"));
    map.add(new Pair<>(AqlOption.id_column_name.toString(), "message_id"));

    return new InstExpJdbc(schema, map, jdbcString, l).eval(env, isCompileTime);
  }

  @Override
  public Collection<Pair<String, Kind>> deps() {
    return ty.deps();
  }

  @Override
  public String toString() {
    return "ms_error " + dom + " " + ty + " " + jdbcString;
  }

}
