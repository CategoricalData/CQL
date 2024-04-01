package catdata.cql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.Util;
import catdata.cql.Kind;
import catdata.cql.Mapping;
import catdata.cql.AqlOptions.AqlOption;

public final class MapExpId extends MapExp {

  public <R, P, E extends Exception> R accept(P params, MapExpVisitor<R, P, E> v) throws E {
    return v.visit(params, this);
  }

  @Override
  public <R, P, E extends Exception> MapExp coaccept(P params, MapExpCoVisitor<R, P, E> v, R r) throws E {
    return v.visitMapExpId(params, r);
  }

  @Override
  protected void allowedOptions(Set<AqlOption> set) {

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

  public MapExpId(SchExp sch) {
    this(sch, Optional.of(sch));
  }

  public MapExpId(SchExp sch, SchExp sch2) {
    this(sch, Optional.of(sch2));
  }

  public MapExpId(SchExp sch, Optional<SchExp> sch2) {
    this.sch = sch;
    this.sch2 = sch2;
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
    MapExpId other = (MapExpId) obj;
    return sch.equals(other.sch) && sch2.equals(other.sch2);
  }

  @Override
  public String toString() {
    if (sch2.isEmpty() || sch2.get().equals(sch)) {
      return "identity " + sch;
    }
    return "include " + sch + " " + sch2.get();

  }

  @Override
  public Mapping<String, String, Sym, Fk, Att, String, Fk, Att> eval0(AqlEnv env, boolean isC) {
    if (sch2.isEmpty()) {
      return Mapping.id(sch.eval(env, isC));
    }
    return Mapping.id(sch.eval(env, isC), Optional.of(sch2.get().eval(env, isC)));
  }

  @Override
  public Pair<SchExp, SchExp> type(AqlTyping G) {
    sch.type(G);
    if (sch2.isEmpty()) {
      return new Pair<>(sch, sch);
    }
    sch2.get().type(G);
    return new Pair<>(sch, sch2.get());
  }

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {
    sch.map(f);
    if (sch2.isPresent()) {
      sch2.get().map(f);
    }
  }

}