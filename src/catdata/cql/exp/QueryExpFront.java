package catdata.cql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Chc;
import catdata.LocStr;
import catdata.Pair;
import catdata.Util;
import catdata.cql.AqlOptions;
import catdata.cql.Kind;
import catdata.cql.Query;
import catdata.cql.Schema;
import catdata.cql.Term;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.exp.QueryExpRaw.PreAgg;
import catdata.cql.exp.QueryExpRaw.PreBlock;

public final class QueryExpFront extends QueryExp {

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((eds == null) ? 0 : eds.hashCode());
    result = prime * result + ((options == null) ? 0 : options.hashCode());
    result = prime * result + i;
    result = prime * result + (isBack ? 1231 : 1237);
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
    QueryExpFront other = (QueryExpFront) obj;
    if (eds == null) {
      if (other.eds != null)
        return false;
    } else if (!eds.equals(other.eds))
      return false;
    if (options == null) {
      if (other.options != null)
        return false;
    } else if (!options.equals(other.options))
      return false;
    if (i != other.i)
      return false;
    if (isBack != other.isBack)
      return false;
    return true;
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
    return eds.deps();
  }

  EdsExp eds;
  int i;
  boolean isBack;
  Map<String, String> options;

  public QueryExpFront(EdsExp eds, String i, boolean isBack, List<Pair<String, String>> ops) {
    this.eds = eds;
    this.i = Integer.parseInt(i);
    this.isBack = isBack;
    this.options = Util.toMapSafely(ops);
    if (options.containsKey(AqlOption.simple_query_entity.name())) {
      throw new RuntimeException("option reserved on front keyworkd: " + AqlOption.simple_query_entity);
    }
    options.put(AqlOption.simple_query_entity.name(), "Front");
  }

  @Override
  public String toString() {
    return (isBack ? "back" : "front") + " " + i + " " + eds;
  }

  @Override
  public Query<String, String, Sym, Fk, Att, String, Fk, Att> eval0(AqlEnv env, boolean isC) {
    var eds0 = eds.eval(env, isC);
    if (!eds0.schema.fks.isEmpty()) {
      throw new RuntimeException("Front/Back cannot be used with foreign keys.");
    }
    List<Pair<LocStr, String>> gens = new LinkedList<>();
    List<Pair<Integer, Pair<RawTerm, RawTerm>>> eqs = new LinkedList<>();
    List<Pair<LocStr, Chc<RawTerm, PreAgg>>> atts = new LinkedList<>();

    if (i < 0 || i >= eds0.eds.size()) {
      throw new RuntimeException("Expected a number between zero and " + (eds0.eds.size() - 1));
    }
    var ed = eds0.eds.get(i);

    f(eds0.schema, gens, atts, ed.As.entrySet(), ed.Awh, eqs);
    if (isBack) {
      f(eds0.schema, gens, new LinkedList<>(), ed.Es.entrySet(), ed.Ewh, eqs);
    }

    return new QueryExpRawSimple(eds.type(env.typing), 0,
        new PreBlock(gens, eqs, atts, Collections.emptyList(), Util.toList(options), false)).eval(env, isC);
  }

  static int eqC = 0;

  private static void f(Schema<String, String, Sym, Fk, Att> sch, List<Pair<LocStr, String>> gens,
      List<Pair<LocStr, Chc<RawTerm, PreAgg>>> atts, Set<Entry<String, Chc<String, String>>> www,
      Set<Pair<Term<String, String, Sym, Fk, Att, Void, Void>, Term<String, String, Sym, Fk, Att, Void, Void>>> wh,
      List<Pair<Integer, Pair<RawTerm, RawTerm>>> eqs) {
    for (Entry<String, Chc<String, String>> x : www) {
      String v = x.getKey();
      gens.add(new Pair<>(new LocStr(0, v), x.getValue().toStringMash()));
      if (x.getValue().left) {
        continue;
      }
      for (Att att : sch.attsFrom(x.getValue().r)) {
        RawTerm term = new RawTerm(att.str, Collections.singletonList(new RawTerm(v)));
        String s = v + "_" + att.str;
        atts.add(new Pair<>(new LocStr(0, s), Chc.inLeft(term)));
      }
    }

    for (Pair<Term<String, String, Sym, Fk, Att, Void, Void>, Term<String, String, Sym, Fk, Att, Void, Void>> x : wh) {
      eqs.add(new Pair<>(eqC++, new Pair<>(conv(x.first), conv(x.second))));
    }
  }

  static RawTerm conv(Term<String, String, Sym, Fk, Att, Void, Void> t) {
    if (t.var != null) {
      return new RawTerm(t.var);
    } else if (t.gen() != null) {
      return Util.abort(t.gen());
    } else if (t.sk() != null) {
      return Util.abort(t.gen());
    } else if (t.fk() != null) {
      return Util.anomaly();
    } else if (t.att() != null) {
      return new RawTerm(t.att().str, Collections.singletonList(conv(t.arg)));
    } else if (t.obj() != null) {
      return new RawTerm(t.obj().toString(), t.ty());
    }
    return Util.anomaly();
  }

//left is type

  @Override
  public Pair<SchExp, SchExp> type(AqlTyping G) {
    return new Pair<>(eds.type(G), new SchExpFront(eds, Integer.toString(i)));
  }

  @Override
  protected void allowedOptions(Set<AqlOption> set) {
    set.add(AqlOption.dont_validate_unsafe);
    set.add(AqlOption.query_remove_redundancy);
    set.add(AqlOption.require_consistency);
    set.add(AqlOption.allow_java_eqs_unsafe);
    set.add(AqlOption.simple_query_entity);
    set.addAll(AqlOptions.proverOptionNames());
  }

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {
    eds.map(f);
  }

}