package catdata.cql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.Util;
import catdata.cql.AqlOptions;
import catdata.cql.Kind;
import catdata.cql.Query;
import catdata.cql.Schema;
import catdata.cql.AqlOptions.AqlOption;

public final class QueryExpId extends QueryExp {

  public <R, P, E extends Exception> R accept(P params, QueryExpVisitor<R, P, E> v) throws E {
    return v.visit(params, this);
  }

  @Override
  public Map<String, String> options() {
    return Collections.emptyMap();
  }

  @Override
  public Collection<Pair<String, Kind>> deps() {
    if (sch2.isEmpty()) {
      return sch.deps();
    }
    return Util.union(sch.deps(), sch2.get().deps());
  }

  public final SchExp sch;
  public final Optional<SchExp> sch2;

  public QueryExpId(SchExp sch) {
    this.sch = sch;
    this.sch2 = Optional.empty();
  }

  public QueryExpId(SchExp sch, SchExp sch2) {
    this.sch = sch;
    this.sch2 = Optional.of(sch2);
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    result = prime * result + (sch.hashCode());
    result = prime * result + (sch2.hashCode());
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
    QueryExpId other = (QueryExpId) obj;
    return sch.equals(other.sch) && sch2.equals(other.sch2);
  }

  @Override
  public String toString() {
    if (sch2.isEmpty()) {
      return "identity " + sch;
    }
    return "include " + sch + " " + sch2.get();
  }

  @Override
  public Query<String, String, Sym, Fk, Att, String, Fk, Att> eval0(AqlEnv env, boolean isC) {
    Schema<String, String, catdata.cql.exp.Sym, catdata.cql.exp.Fk, catdata.cql.exp.Att> s = sch
        .eval(env, isC);
    if (sch2.isEmpty()) {
      return Query.id(new AqlOptions(env.defaults, AqlOption.dont_validate_unsafe, true), s, s);
    }
    return Query.id(new AqlOptions(env.defaults, AqlOption.dont_validate_unsafe, true),
        sch2.get().eval(env, isC), s);

  }

  @Override
  public Pair<SchExp, SchExp> type(AqlTyping G) {
    if (sch2.isEmpty()) {
      return new Pair<>(sch, sch);
    }
    return new Pair<>(sch2.get(), sch); // reverse directed vs mapping, transform
  }

  @Override
  protected void allowedOptions(Set<AqlOption> set) {

  }

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {
    sch.map(f);
    if (sch2.isPresent()) {
      sch2.get().map(f);
    }
  }

}