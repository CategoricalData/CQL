package catdata.cql.exp;

import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.cql.AqlOptions;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.Eq;
import catdata.cql.Instance;
import catdata.cql.Kind;
import catdata.cql.Query;
import catdata.cql.Schema;
import catdata.cql.Term;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public class QueryExpFromInst extends QueryExp {

  InstExp I;

  public QueryExpFromInst(InstExp i) {
    I = i;
  }

  @Override
  public Pair<SchExp, SchExp> type(AqlTyping G) {
    var x = I.type(G);
    return new Pair<SchExp, SchExp>(x, new SchExpUnit(x.type(G)));
  }

  @Override
  public <R, P, E extends Exception> R accept(P params, QueryExpVisitor<R, P, E> v) throws E {
    return v.visit(params, this);
  }

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {
    I.mapSubExps(f);
  }

  @Override
  protected void allowedOptions(Set<AqlOption> set) {}

  @Override
  protected Map<String, String> options() {
    return Collections.emptyMap();
  }

  @Override
  protected Query<String, String, Sym, Fk, Att, String, Fk, Att> eval0(
      AqlEnv env, boolean isCompileTime) {
    Instance i = (Instance) I.eval(env, isCompileTime);

    //		Pair p = i.algebra().intifyX(0);

    return toQuery(env.defaults, i);
  }

  public static <Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2, Gen, Sk, X, Y>
      Query<Ty, En1, Sym, Fk1, Att1, String, Fk2, Att2> toQuery(
          AqlOptions ops, Instance<Ty, En1, Sym, Fk1, Att1, Gen, Sk, X, Y> i) {
    Map<
            String,
            Triple<
                LinkedHashMap<String, Chc<En1, Ty>>,
                Collection<Eq<Ty, En1, Sym, Fk1, Att1, String, String>>,
                AqlOptions>>
        m = new HashMap<>();
    LinkedHashMap<String, Chc<En1, Ty>> gens = new LinkedHashMap<>();
    List<Eq<Ty, En1, Sym, Fk1, Att1, String, String>> eqs = new LinkedList<>();

    i.gens()
        .forEach(
            (g, t) -> {
              gens.put(g.toString(), Chc.inLeft(t));
            });
    i.sks()
        .forEach(
            (g, t) -> {
              gens.put(g.toString(), Chc.inRight(t));
            });
    i.eqs(
        (l, r) -> {
          Term<Ty, En1, Sym, Fk1, Att1, String, String> l2 =
              l.mapGenSk(e -> e.toString(), e -> e.toString());
          Term<Ty, En1, Sym, Fk1, Att1, String, String> r2 =
              r.mapGenSk(e -> e.toString(), e -> e.toString());

          eqs.add(new Eq<>(Collections.emptyMap(), l2, r2));
        });

    m.put("", new Triple<>(gens, eqs, ops));

    Query<Ty, En1, Sym, Fk1, Att1, String, Fk2, Att2> ret =
        new Query<Ty, En1, Sym, Fk1, Att1, String, Fk2, Att2>(
            Collections.emptyMap(),
            Collections.emptyMap(),
            m,
            Collections.emptyMap(),
            Collections.emptyMap(),
            Collections.emptyMap(),
            i.schema(),
            Schema.unit(i.schema().typeSide),
            ops);
    //
    return ret;
  }

  @Override
  public int hashCode() {
    return Objects.hash(I);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    QueryExpFromInst other = (QueryExpFromInst) obj;
    return Objects.equals(I, other.I);
  }

  @Override
  public String toString() {
    return "fromInstance " + I;
  }

  @Override
  public Collection<Pair<String, Kind>> deps() {
    return I.deps();
  }
}
