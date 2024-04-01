package catdata.cql.exp;

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
import catdata.cql.AqlOptions;
import catdata.cql.Collage;
import catdata.cql.Eq;
import catdata.cql.Kind;
import catdata.cql.Query;
import catdata.cql.Schema;
import catdata.cql.Term;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.Collage.CCollage;
import catdata.cql.Query.Agg;
import gnu.trove.map.hash.THashMap;

public class QueryExpSpanify extends QueryExp {

  private final SchExp rel;

  public QueryExpSpanify(SchExp rel) {
    this.rel = rel;
  }

  @Override
  public Pair<SchExp, SchExp> type(AqlTyping G) {
    TyExp u = rel.type(G);
    TyExp t = InstExpRdfAll.makeSch().type(G);
    if (!t.equals(u)) {
      throw new RuntimeException("Typeside mismatch on " + this);
    }
    return new Pair<>(new SchExpRdf(), new SchExpSpan(rel));
  }

  @Override
  public <R, P, E extends Exception> R accept(P params, QueryExpVisitor<R, P, E> v) throws E {
    return v.visit(params, this);
  }

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {
    rel.map(f);
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
    Schema<String, String, Sym, Fk, Att> src = InstExpRdfAll.makeSch().eval(env, isCompileTime);
    Schema<String, String, Sym, Fk, Att> relOlog = rel.eval(env, isCompileTime);
    Collage<String, String, Sym, Fk, Att, Void, Void> col = new CCollage<>(src.typeSide.collage());
    if (!relOlog.eqs.isEmpty()) {
      throw new RuntimeException("RelOlog equations not supported yet");
    }

    Map<String, Triple<LinkedHashMap<String, Chc<String, String>>, Collection<Eq<String, String, Sym, Fk, Att, String, String>>, AqlOptions>> ens = new THashMap<>();
    Map<Att, Chc<Term<String, String, Sym, Fk, Att, String, String>, Agg<String, String, Sym, Fk, Att>>> atts = new THashMap<>();
    Map<Fk, Pair<Map<String, Term<Void, String, Void, Fk, Void, String, Void>>, AqlOptions>> fks = new THashMap<>();
    Map<Fk, Map<String, Term<String, String, Sym, Fk, Att, String, String>>> fks2 = new THashMap<>();

    Att subatt = Att.Att(("R"), "subject");
    Att predatt = Att.Att(("R"), "predicate");
    Att obatt = Att.Att(("R"), "object");
    for (String en : relOlog.ens) {
      col.getEns().add(en);
      List<Eq<String, String, Sym, Fk, Att, String, String>> eqs = new LinkedList<>();
      eqs.add(new Eq<>(null, Term.Att(predatt, Term.Gen(("c"))),
          Term.Obj("http://www.w3.org/1999/02/22-rdf-syntax-ns#type", ("Dom"))));
      eqs.add(new Eq<>(null, Term.Att(obatt, Term.Gen(("c"))), Term.Obj(en, ("Dom"))));
      LinkedHashMap<String, Chc<String, String>> xxx = new LinkedHashMap<String, Chc<String, String>>();
      xxx.put("c", Chc.inLeft(("R")));
      ens.put(en, new Triple<>(xxx, eqs, ops));
      atts.put(Att.Att(en, "subject"), Chc.inLeft(Term.Att(subatt, Term.Gen(("c")))));
      col.atts().put(Att.Att(en, "subject"), new Pair<>(en, ("Dom")));
    }
    for (Entry<Fk, Pair<String, String>> fk : relOlog.fks.entrySet()) {
      String en = (fk.getKey().str + "_" + fk.getValue().first + "_" + fk.getValue().second);
      col.getEns().add(en);
      col.fks().put(Fk.Fk(en, "subject"), new Pair<>(en, fk.getValue().first));
      col.fks().put(Fk.Fk(en, "object"), new Pair<>(en, fk.getValue().second));

      LinkedHashMap<String, Chc<String, String>> ctx = new LinkedHashMap<>();
      ctx.put(("r"), Chc.inLeft(("R")));
      ctx.put(("rs"), Chc.inLeft(("R")));
      ctx.put(("rt"), Chc.inLeft(("R")));
      List<Eq<String, String, Sym, Fk, Att, String, String>> eqs = new LinkedList<>();
      eqs.add(new Eq<>(null, Term.Att(predatt, Term.Gen(("r"))), Term.Obj(fk.getKey().str, ("Dom"))));
      eqs.add(new Eq<>(null, Term.Att(predatt, Term.Gen(("rs"))),
          Term.Obj("http://www.w3.org/1999/02/22-rdf-syntax-ns#type", ("Dom"))));
      eqs.add(new Eq<>(null, Term.Att(predatt, Term.Gen(("rt"))),
          Term.Obj("http://www.w3.org/1999/02/22-rdf-syntax-ns#type", ("Dom"))));
      eqs.add(new Eq<>(null, Term.Att(obatt, Term.Gen(("rs"))),
          Term.Obj(fk.getValue().first, ("Dom"))));
      eqs.add(new Eq<>(null, Term.Att(obatt, Term.Gen(("rt"))),
          Term.Obj(fk.getValue().second, ("Dom"))));
      eqs.add(new Eq<>(null, Term.Att(obatt, Term.Gen(("r"))), Term.Att(subatt, Term.Gen(("rt")))));
      eqs.add(new Eq<>(null, Term.Att(subatt, Term.Gen(("r"))),
          Term.Att(subatt, Term.Gen(("rs")))));

      ens.put(en, new Triple<>(ctx, eqs, ops));
      fks.put(Fk.Fk(en, "subject"),
          new Pair<>(Collections.singletonMap(("c"), Term.Gen(("rs"))), ops));
      fks.put(Fk.Fk(en, "object"),
          new Pair<>(Collections.singletonMap(("c"), Term.Gen(("rt"))), ops));
      fks2.put(Fk.Fk(en, "subject"), Collections.emptyMap());
      fks2.put(Fk.Fk(en, "object"), Collections.emptyMap());

    }
    for (Entry<Att, Pair<String, String>> att : relOlog.atts.entrySet()) {
      String en = (att.getKey().str + "_" + att.getValue().first);
      col.getEns().add(en);
      col.fks().put(Fk.Fk(en, "subject"), new Pair<>(en, att.getValue().first));
      col.atts().put(Att.Att(en, "object"), new Pair<>(en, att.getValue().second));

      LinkedHashMap<String, Chc<String, String>> ctx = new LinkedHashMap<>();
      ctx.put(("r"), Chc.inLeft(("R")));
      ctx.put(("rs"), Chc.inLeft(("R")));
      List<Eq<String, String, Sym, Fk, Att, String, String>> eqs = new LinkedList<>();
      eqs.add(new Eq<>(null, Term.Att(predatt, Term.Gen(("r"))),
          Term.Obj(att.getKey().str, ("Dom"))));
      eqs.add(new Eq<>(null, Term.Att(predatt, Term.Gen(("rs"))),
          Term.Obj("http://www.w3.org/1999/02/22-rdf-syntax-ns#type", ("Dom"))));
      eqs.add(new Eq<>(null, Term.Att(obatt, Term.Gen(("rs"))),
          Term.Obj(att.getValue().first, ("Dom"))));
      eqs.add(new Eq<>(null, Term.Att(subatt, Term.Gen(("r"))),
          Term.Att(subatt, Term.Gen(("rs")))));

      ens.put(en, new Triple<>(ctx, eqs, ops));
      fks.put(Fk.Fk(en, "subject"),
          new Pair<>(Collections.singletonMap(("c"), Term.Gen(("rs"))), ops));
      atts.put(Att.Att(en, "object"), Chc.inLeft(Term.Att(obatt, Term.Gen(("r")))));
      fks2.put(Fk.Fk(en, "subject"), Collections.emptyMap());

    }

    Schema<String, String, Sym, Fk, Att> dst = new Schema<String, String, Sym, Fk, Att>(src.typeSide, col, ops);

    return new Query<String, String, Sym, Fk, Att, String, Fk, Att>(Collections.emptyMap(), Collections.emptyMap(), ens, atts,
        fks, fks2, src, dst, ops);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((rel == null) ? 0 : rel.hashCode());
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
    QueryExpSpanify other = (QueryExpSpanify) obj;
    if (rel == null) {
      if (other.rel != null)
        return false;
    } else if (!rel.equals(other.rel))
      return false;
    return true;
  }

  @Override
  public Collection<Pair<String, Kind>> deps() {
    return rel.deps();
  }

  @Override
  public String toString() {
    return "spanify " + rel;
  }

}
