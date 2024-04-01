package catdata.cql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.cql.Kind;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.fdm.InitialInstance;

public final class InstExpEmpty extends InstExp<Void, Void, Void, Void> {

  public <R, P, E extends Exception> R accept(P param, InstExpVisitor<R, P, E> v) throws E {
    return v.visit(param, this);
  }

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {
    schema.map(f);
  }

  public final SchExp schema;

  @Override
  public Map<String, String> options() {
    return Collections.emptyMap();
  }

  @Override
  public Collection<InstExp<?, ?, ?, ?>> direct(AqlTyping G) {
    return Collections.emptySet();
  }

  @Override
  public Collection<Pair<String, Kind>> deps() {
    return schema.deps();
  }

  public InstExpEmpty(SchExp schema) {
    this.schema = schema;
  }

  @Override
  public int hashCode() {
    return schema.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    InstExpEmpty other = (InstExpEmpty) obj;
    return schema.equals(other.schema);
  }

  @Override
  public String toString() {
    return "empty : " + schema;
  }

  @Override
  public synchronized InitialInstance<String, String, Sym, Fk, Att> eval0(AqlEnv env, boolean isC) {
    return new InitialInstance<>(schema.eval(env, isC));
  }

  @Override
  public SchExp type(AqlTyping G) {
    schema.type(G);
    return schema;
  }

  @Override
  protected void allowedOptions(Set<AqlOption> set) {
  }

}