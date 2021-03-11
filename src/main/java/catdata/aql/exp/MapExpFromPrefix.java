package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import catdata.aql.Kind;
import catdata.aql.Mapping;
import catdata.aql.Schema;
import catdata.aql.Term;
import catdata.aql.AqlOptions.AqlOption;
import gnu.trove.map.hash.THashMap;

public class MapExpFromPrefix extends MapExp {

  public <R, P, E extends Exception> R accept(P params, MapExpVisitor<R, P, E> v) throws E {
    return v.visit(params, this);
  }

  @Override
  public <R, P, E extends Exception> MapExp coaccept(P params, MapExpCoVisitor<R, P, E> v, R r) throws E {
    return v.visitMapExpFromPrefix(params, r);
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
    return sch.deps();
  }

  public final SchExp sch;
  public final String prefix;

  public MapExpFromPrefix(SchExp sch, String p) {
    this.sch = sch;
    this.prefix = p;
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    result = prime * result + (sch.hashCode());
    result = prime * result + (prefix.hashCode());
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
    MapExpFromPrefix other = (MapExpFromPrefix) obj;
    return sch.equals(other.sch) && prefix.equals(other.prefix);
  }

  @Override
  public String toString() {
    return "from_prefix " + sch + " " + prefix;

  }
  
  public static Mapping<String, String, Sym, Fk, Att, String, Fk, Att> fromPrefix(Schema<String, String, Sym, Fk, Att> s,
      Schema<String, String, Sym, Fk, Att> t, String prefix) {
    Map<String, String> ens = new THashMap<>(s.size() * 2);
    for (String en : s.ens) {
      ens.put(prefix + en, en);
    }
    Map<Fk, Pair<String, List<Fk>>> fks = Util.mk();
    for (Fk fk : s.fks.keySet()) {
      fks.put(Fk.Fk(prefix + s.fks.get(fk).first,fk.str), new Pair<>(s.fks.get(fk).first,
          Collections.singletonList(fk)));
    }
    Map<Att, Triple<String, String, Term<String, String, Sym, Fk, Att, Void, Void>>> atts = Util.mk();
    String v = ("v");
    for (Att att : s.atts.keySet()) {
      atts.put(Att.Att((prefix + s.atts.get(att).first), att.str), new Triple<>(v, s.atts.get(att).first,
          Term.Att(att, Term.Var(v))));
    }
    return new Mapping<>(ens, atts, fks, t, s, true);
  }

  @Override
  public Mapping<String, String, Sym, Fk, Att, String, Fk, Att> eval0(AqlEnv env, boolean isC) {
    var s = sch.eval(env, isC);
    var t = new SchExpPrefix(sch, prefix).eval(env, isC);
    return fromPrefix(s, t, prefix);
  }

  @Override
  public Pair<SchExp, SchExp> type(AqlTyping G) {
    sch.type(G);
    return new Pair<>(new SchExpPrefix(sch, prefix), sch);
  }

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {
    sch.map(f);
  }

}