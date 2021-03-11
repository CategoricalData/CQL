package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Eq;
import catdata.aql.Kind;
import catdata.aql.Mapping;
import catdata.aql.Query;
import catdata.aql.Query.Agg;
import catdata.aql.Term;

import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public final class QueryExpDeltaEval extends QueryExp {

  public final MapExp F;
  public final Map<String, String> options;

  @Override
  protected void allowedOptions(Set<AqlOption> set) {
    set.add(AqlOption.dont_validate_unsafe);
    set.add(AqlOption.query_remove_redundancy);

  }

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {
    F.map(f);
  }

  public <R, P, E extends Exception> R accept(P params, QueryExpVisitor<R, P, E> v) throws E {
    return v.visit(params, this);
  }

  @Override
  public Map<String, String> options() {
    return options;
  }

  @Override
  public Collection<Pair<String, Kind>> deps() {
    return F.deps();
  }

  public QueryExpDeltaEval(MapExp F, List<Pair<String, String>> options) {
    this.F = F;
    this.options = Util.toMapSafely(options);
  }

  @Override
  public int hashCode() {
    final int prime = 3;
    int result = 1;
    result = prime * result + ((F == null) ? 0 : F.hashCode());
    result = prime * result + ((options == null) ? 0 : options.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof QueryExpDeltaEval))
      return false;
    QueryExpDeltaEval other = (QueryExpDeltaEval) obj;
    if (F == null) {
      if (other.F != null)
        return false;
    } else if (!F.equals(other.F))
      return false;
    if (options == null) {
      if (other.options != null)
        return false;
    } else if (!options.equals(other.options))
      return false;
    return true;
  }

  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer(" ");
    if (!this.options.isEmpty()) {
      sb.append("{ options ").append(this.options.entrySet().stream()
          .map(sym -> sym.getKey() + " = " + sym.getValue()).collect(Collectors.joining("\n\t")));
      sb.append(" }");
    }

    return "toQuery " + F + sb.toString();
  }

  @Override
  public Pair<SchExp, SchExp> type(AqlTyping G) {
    return new Pair<>(F.type(G).second, F.type(G).first);
  }

  @Override
  public Query<String, String, Sym, Fk, Att, String, Fk, Att> eval0(AqlEnv env, boolean isC) {
    Mapping<String, String, Sym, Fk, Att, String, Fk, Att> F0 = F.eval(env, isC);
    if (isC) {
      throw new IgnoreException();
    }

    Map<String, Triple<Map<String, Chc<String, String>>, Collection<Eq<String, String, Sym, Fk, Att, String, String>>, AqlOptions>> ens = new THashMap<>();
    Map<Att, Chc<Term<String, String, Sym, Fk, Att, String, String>, Agg<String, String, Sym, Fk, Att>>> atts = new THashMap<>();
    Map<Fk, Pair<Map<String, Term<Void, String, Void, Fk, Void, String, Void>>, AqlOptions>> fks = new THashMap<>();
    Map<Fk, Map<String, Term<String, String, Sym, Fk, Att, String, String>>> sks = new THashMap<>();

    AqlOptions ops = new AqlOptions(options, env.defaults);

    String v = ("v");
    for (String en : F0.src.ens) {
      Map<String, Chc<String, String>> fr = new THashMap<>();
      fr.put(v, Chc.inLeft(F0.ens.get(en)));
      ens.put(en, new Triple<>(fr, new THashSet<>(), ops));
    }
    for (Att att : F0.src.atts.keySet()) {
      Term<String, String, Sym, Fk, Att, String, String> h = F0.atts.get(att).third.mapGenSk(Util.voidFn(), Util.voidFn());
      Term<String, String, Sym, Fk, Att, String, String> g = Term.Gen(v);
      Term<String, String, Sym, Fk, Att, String, String> t = h.subst(Collections.singletonMap(F0.atts.get(att).first, g));
      atts.put(att, Chc.inLeft(t));
    }
    for (Fk fk : F0.src.fks.keySet()) {
      Map<String, Term<Void, String, Void, Fk, Void, String, Void>> g = new THashMap<>();
      g.put(v, Term.Fks(F0.fks.get(fk).second, Term.Gen(v)));
      fks.put(fk, new Pair<>(g, ops));
      sks.put(fk, new THashMap<>());
    }

    return Query.makeQuery(ens, atts, fks, sks, F0.dst, F0.src, ops);
  }

}