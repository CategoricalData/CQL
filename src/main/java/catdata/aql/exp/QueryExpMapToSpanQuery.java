package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;

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
import catdata.aql.Schema;
import catdata.aql.Term;
import gnu.trove.map.hash.THashMap;

public class QueryExpMapToSpanQuery extends QueryExp {

  private final MapExp map;

  public QueryExpMapToSpanQuery(MapExp r) {
    this.map = r;
  }

  @Override
  public Pair<SchExp, SchExp> type(AqlTyping G) {
    Pair<SchExp, SchExp> u = map.type(G);
    return new Pair<>(new SchExpSpan(u.second), new SchExpSpan(u.first));
  }

  @Override
  public <R, P, E extends Exception> R accept(P params, QueryExpVisitor<R, P, E> v) throws E {
    return v.visit(params, this);
  }

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {
    map.mapSubExps(f);
  }

  @Override
  protected void allowedOptions(Set<AqlOption> set) {
  }

  @Override
  protected Map<String, String> options() {
    return Collections.emptyMap();
  }

  @Override
  protected Query<String, String, Sym, Fk, Att, String, Fk, Att> eval0(AqlEnv env, boolean isCompileTime) {
    if (isCompileTime) {
      throw new IgnoreException();
    }
    AqlOptions ops = env.defaults;
    Mapping<String, String, Sym, Fk, Att, String, Fk, Att> relMap = map.eval(env, isCompileTime);
    Schema<String, String, Sym, Fk, Att> srcR = relMap.src;
    Schema<String, String, Sym, Fk, Att> dstR = relMap.dst;

    Schema<String, String, Sym, Fk, Att> src = new SchExpSpan(new SchExpSrc(map)).eval(env, isCompileTime);
    Schema<String, String, Sym, Fk, Att> dst = new SchExpSpan(new SchExpDst(map)).eval(env, isCompileTime);

    Map<String, Triple<LinkedHashMap<String, Chc<String, String>>, Collection<Eq<String, String, Sym, Fk, Att, String, String>>, AqlOptions>> ens = new THashMap<>();
    Map<Att, Chc<Term<String, String, Sym, Fk, Att, String, String>, Agg<String, String, Sym, Fk, Att>>> atts = new THashMap<>();
    Map<Fk, Pair<Map<String, Term<Void, String, Void, Fk, Void, String, Void>>, AqlOptions>> fks = new THashMap<>();
    Map<Fk, Map<String, Term<String, String, Sym, Fk, Att, String, String>>> fks2 = new THashMap<>();

    for (String en : srcR.ens) {
      Att subatt = Att.Att(en, "subject");

      List<Eq<String, String, Sym, Fk, Att, String, String>> eqs = new LinkedList<>();
      LinkedHashMap<String, Chc<String, String>> xxx = new LinkedHashMap<String, Chc<String, String>>();
      xxx.put("c", Chc.inLeft(relMap.ens.get(en)));
      ens.put(en, new Triple<>(xxx , eqs, ops));
      atts.put(subatt, Chc.inLeft(Term.Att(Att.Att(relMap.ens.get(en), "subject"), Term.Gen(("c")))));
    }
    for (Entry<Fk, Pair<String, String>> fk : srcR.fks.entrySet()) {
      String enX = (fk.getKey().str + "_" + fk.getValue().first + "_" + fk.getValue().second);
      LinkedHashMap<String, Chc<String, String>> ctx = new LinkedHashMap<>();

      ctx.put(("rs"), Chc.inLeft(relMap.ens.get(fk.getValue().first)));
      ctx.put(("rt"), Chc.inLeft(relMap.ens.get(fk.getValue().second)));
      List<Eq<String, String, Sym, Fk, Att, String, String>> eqs = new LinkedList<>();

      Pair<String, List<Fk>> p = relMap.fks.get(fk.getKey());

      int i = 0;
      Term<String, String, Sym, Fk, Att, String, String> term = null;
      Term<String, String, Sym, Fk, Att, String, String> term2 = null;
      String first = null;
      for (Fk fk2 : p.second) {
        String en2 = (fk2.str + "_" + dstR.fks.get(fk2).first + "_" + dstR.fks.get(fk2).second);
        if (first == null) {
          first = en2;
        }
        ctx.put(("r" + i), Chc.inLeft(en2));
        Fk subfk1 = Fk.Fk(en2, "subject");
        Fk subfk2 = Fk.Fk(en2, "object");

        Term<String, String, Sym, Fk, Att, String, String> newterm = Term.Fk(subfk1, Term.Gen(("r" + i)));
        Term<String, String, Sym, Fk, Att, String, String> newterm2 = Term.Fk(subfk2, Term.Gen(("r" + i)));

        if (term != null) {
          eqs.add(new Eq<>(null, term2, newterm));
        }

        i++;
        term = newterm;
        term2 = newterm2;
      }

      if (p.second.size() > 0) {
        eqs.add(new Eq<>(null, Term.Fk(Fk.Fk(first, "subject"), Term.Gen(("r0"))),
            Term.Gen(("rs"))));

        eqs.add(new Eq<>(null, term2, Term.Gen(("rt"))));
      }

      ens.put(enX, new Triple<>(ctx, eqs, ops));

      fks.put(Fk.Fk(enX, "subject"),
          new Pair<>(Collections.singletonMap(("c"), Term.Gen(("rs"))), ops));

      fks.put(Fk.Fk(enX, "object"),
          new Pair<>(Collections.singletonMap(("c"), Term.Gen(("rt"))), ops));

      fks2.put(Fk.Fk(enX, "subject"), Collections.emptyMap());
      fks2.put(Fk.Fk(enX, "object"), Collections.emptyMap());
    }
    for (Entry<Att, Pair<String, String>> att : srcR.atts.entrySet()) {
      String enX = (att.getKey().str + "_" + att.getValue().first);
      LinkedHashMap<String, Chc<String, String>> ctx = new LinkedHashMap<>();

      ctx.put(("rs"), Chc.inLeft(relMap.ens.get(att.getValue().first)));
      List<Eq<String, String, Sym, Fk, Att, String, String>> eqs = new LinkedList<>();

      Triple<String, String, Term<String, String, Sym, Fk, Att, Void, Void>> p = relMap.atts.get(att.getKey());

      int i = 0;

      if (p.third.att() == null) {
        throw new RuntimeException("Require attribute in rel olog mapping");
      }
      List<Fk> xxx = p.third.arg.toFkList();

      Term<String, String, Sym, Fk, Att, String, String> term = null;
      Term<String, String, Sym, Fk, Att, String, String> term2 = null;
      String first = null;
      for (Fk fk2 : xxx) {
        String en2 = (fk2.str + "_" + dstR.fks.get(fk2).first + "_" + dstR.fks.get(fk2).second);
        if (first == null) {
          first = en2;
        }
        ctx.put(("r" + i), Chc.inLeft(en2));
        Fk subfk1 = Fk.Fk(en2, "subject");
        Fk subfk2 = Fk.Fk(en2, "object");

        Term<String, String, Sym, Fk, Att, String, String> newterm = Term.Fk(subfk1, Term.Gen(("r" + i)));
        Term<String, String, Sym, Fk, Att, String, String> newterm2 = Term.Fk(subfk2, Term.Gen(("r" + i)));

        if (term != null) {
          eqs.add(new Eq<>(null, term2, newterm));
        }

        i++;
        term = newterm;
        term2 = newterm2;
      }

      if (xxx.size() > 0) {
        eqs.add(new Eq<>(null, Term.Fk(Fk.Fk(first, "subject"), Term.Gen(("r0"))),
            Term.Gen(("rs"))));

        eqs.add(new Eq<>(null, term2, Term.Gen(("rt"))));
      }

      Att a = Util.get0(p.third.atts());
      String en2 = (a.str + "_" + dstR.atts.get(a).first);
      ctx.put(("rt"), Chc.inLeft(en2));

      Att att2 = Att.Att(en2, "object");

      ens.put(enX, new Triple<>(ctx, eqs, ops));

      fks.put(Fk.Fk(enX, "subject"),
          new Pair<>(Collections.singletonMap(("c"), Term.Gen(("rs"))), ops));

      fks2.put(Fk.Fk(enX, "subject"), Collections.emptyMap());
      atts.put(Att.Att(enX, "object"), Chc.inLeft(Term.Att(att2, Term.Gen(("rt")))));
    }

    return new Query<String, String, Sym, Fk, Att, String, Fk, Att>(Collections.emptyMap(), Collections.emptyMap(), ens, atts,
        fks, fks2, dst, src, ops);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((map == null) ? 0 : map.hashCode());
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
    QueryExpMapToSpanQuery other = (QueryExpMapToSpanQuery) obj;
    if (map == null) {
      if (other.map != null)
        return false;
    } else if (!map.equals(other.map))
      return false;
    return true;
  }

  @Override
  public Collection<Pair<String, Kind>> deps() {
    return map.deps();
  }

  @Override
  public String toString() {
    return "spanify_mapping " + map;
  }

}
