package catdata.cql.exp;

import catdata.Chc;
import catdata.LocStr;
import catdata.Pair;
import catdata.UPair;
import catdata.Util;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.Constraints;
import catdata.cql.ED;
import catdata.cql.Kind;
import catdata.cql.Query;
import catdata.cql.Schema;
import catdata.cql.Term;
import catdata.cql.Transform;
import catdata.cql.exp.QueryExpRaw.PreAgg;
import catdata.cql.exp.QueryExpRaw.PreBlock;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public class EdsExpAll<Gen, Sk, X, Y> extends EdsExp {
  InstExp<Gen, Sk, X, Y> I;

  SchExp T;

  public EdsExpAll(InstExp<Gen, Sk, X, Y> i, SchExp t) {
    I = i;
    T = t;
  }

  @Override
  public <R, P, E extends Exception> R accept(P params, EdsExpVisitor<R, P, E> v) throws E {
    return v.visit(params, this);
  }

  @Override
  public Map<String, String> options() {
    return Collections.emptyMap();
  }

  @Override
  public boolean isVar() {
    return false;
  }

  @Override
  public Collection<Pair<String, Kind>> deps() {
    return I.deps();
  }

  public static <Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> Set<Set<Pair<En, Boolean>>> gt(List<En> ens) {
    if (ens.isEmpty()) {
      Set<Set<Pair<En, Boolean>>> l = new HashSet<>();
      l.add(new HashSet<>());
      return l;
    }
    En en = ens.get(0);

    Set<Set<Pair<En, Boolean>>> ret = new HashSet<>();

    for (var rest : gt(ens.subList(1, ens.size()))) {

      var a = new HashSet<>(rest);
      a.add(new Pair<>(en, true));
      ret.add(a);

      var b = new HashSet<>(rest);
      b.add(new Pair<>(en, false));
      ret.add(b);

      var c = new HashSet<>(rest);
      c.add(new Pair<>(en, null));
      ret.add(c);
    }

    return ret;
  }

  @SuppressWarnings("unchecked")
  @Override
  public synchronized Constraints eval0(AqlEnv env, boolean isC) {
    if (isC) {
      throw new IgnoreException();
    }
    var i = I.eval(env, isC);
    var t = T.eval(env, isC);
    List<ED> ls = new LinkedList<>();
    int sss = 0;
    for (var l : gt(new LinkedList<>(i.schema().ens))) {

      Map<String, Chc<String, String>> as = new HashMap<>();
      Map<String, Chc<String, String>> es = new HashMap<>();

      for (var x : l) {
        if (x.second != null) {
          if (x.second) {
            as.put(x.first, Chc.inRight(x.first));
          } else {
            es.put(x.first, Chc.inRight(x.first));
          }
        }
      }
      if (as.isEmpty() && es.isEmpty()) {
        continue;
      }
      if (as.isEmpty()) {
        continue;
      }

      //			if (as.size() + es.size() > 4) {
      //				continue;
      //			}
      //			System.out.println(as.size() + es.size());
      //			System.out.println(1 + i.schema().ens.size() - t.ens.size());
      /*	if (as.size() + es.size() == 1 + i.schema().ens.size() - t.ens.size()) {
      //				System.out.println("esdf");
      				if (es.size() != 1) {
      					continue;
      				}
      				if (!srcToTgt(as.keySet(), es.keySet(), t.ens)) {
      					continue;
      				}
      //				System.out.println("sdfds");
      			} else if (as.size() + es.size() > 2) {
      				continue;
      			}
      			// else if (as.size() + es.size() > 2 && !srcToTgt(as.keySet(), es.keySet(),
      			// t.ens)) {
      			// continue;
      			// }
      */
      if (as.size() + es.size() > 2) {
        continue;
      }
      var allEqs1 = mkW(as, i.schema());
      var es2 = new HashMap<>(es);
      es2.putAll(as);
      var allEqs2 = mkW(es2, i.schema());
      allEqs2.removeAll(allEqs1);

      List<ED> ls2 = new LinkedList<>();

      List<ED> tru = new LinkedList<>();

      for (Set<UPair<Term<String, String, Sym, Fk, Att, Void, Void>>> awh :
          Util.powerSet2(allEqs1)) {

        for (Set<UPair<Term<String, String, Sym, Fk, Att, Void, Void>>> ewh :
            Util.powerSet2(allEqs2)) {

          if (ewh.isEmpty()) {
            continue;
          }

          ED ed = new ED(as, es, bar(awh), bar(ewh), false, env.defaults);

          // if (tauto(ed, i.schema(), env, isC)) {
          // continue;
          // }

          /*
           * var f = new EvalInstance<>(QueryExpFromInst.toQuery(env.defaults,
           * ed.front(i.schema())), i, env.defaults); if (f.size() == 0) { continue; }
           */

          // boolean found = false;
          // for (ED other : new LinkedList<>(ls2)) {
          // if (equiv(ed, other, i.schema(), env, isC)) {
          // found = true;
          // break;
          // }
          // }

          // if (!found) {
          ls2.add(ed);

          if (new Constraints(i.schema(), Collections.singletonList(ed), env.defaults)
              .triggers(i, env.defaults)
              .isEmpty()) {
            tru.add(ed);
          }
          //					System.out.println(sss++);

        }
      }

      ls.addAll(tru);
    }

    var m = new LinkedList<>(ls);
    var it = ls.iterator();
    while (it.hasNext()) {
      var ed = it.next();
      for (var ed2 : m) {
        if (ed == ed2) {
          continue;
        }
        var q = entails(ed2, ed, i.schema(), env, isC);
        if (null != q) {
          it.remove();
          break;
        }
      }
    }

    for (String en : i.schema().ens) {
      var cands = Util.powerSet(i.schema().attsFrom(en));
      cands.remove(Collections.emptySet());
      cands.remove(new HashSet<>(i.schema().attsFrom(en)));

      for (var cand : cands) {
        Map<String, Chc<String, String>> as = new HashMap<>();
        as.put("x", Chc.inRight(en));
        as.put("y", Chc.inRight(en));
        Map<String, Chc<String, String>> es = new HashMap<>();
        Set<
                Pair<
                    Term<String, String, Sym, Fk, Att, Void, Void>,
                    Term<String, String, Sym, Fk, Att, Void, Void>>>
            awh = new HashSet<>();
        for (Att att : cand) {
          awh.add(new Pair<>(Term.Att(att, Term.Var("x")), Term.Att(att, Term.Var("y"))));
        }
        Set<
                Pair<
                    Term<String, String, Sym, Fk, Att, Void, Void>,
                    Term<String, String, Sym, Fk, Att, Void, Void>>>
            ewh = new HashSet<>();
        ewh.add(new Pair<>(Term.Var("x"), Term.Var("y")));

        ED ed = new ED(as, es, awh, ewh, false, env.defaults); //

        if (new Constraints(i.schema(), Collections.singletonList(ed), env.defaults)
            .triggers(i, env.defaults)
            .isEmpty()) {
          //					System.out.println("aed");
          ls.add(ed);
        }
      }
    }

    //	System.out.println("***" + ls.size());

    var y = new Constraints(i.schema(), ls, env.defaults);
    return y;
  }

  private boolean srcToTgt(Set<String> as, Set<String> es, Collection<String> ts) {
    for (var a : as) {
      if (ts.contains(a)) {
        return false;
      }
    }
    for (var e : es) {
      if (!ts.contains(e)) {
        return false;
      }
    }
    return true;
  }

  static <X>
      Set<
              Pair<
                  Term<String, String, Sym, Fk, Att, Void, Void>,
                  Term<String, String, Sym, Fk, Att, Void, Void>>>
          bar(Set<UPair<Term<String, String, Sym, Fk, Att, Void, Void>>> ret) {
    Set<
            Pair<
                Term<String, String, Sym, Fk, Att, Void, Void>,
                Term<String, String, Sym, Fk, Att, Void, Void>>>
        ret2 = new HashSet<>();
    for (var x : ret) {
      ret2.add(new Pair<>(x.a, x.b));
    }
    return ret2;
  }

  private Query<String, String, Sym, Fk, Att, String, Fk, Att> foo(
      AqlEnv env, ED ed, Schema<String, String, Sym, Fk, Att> schema, boolean isBack, boolean isC) {
    List<Pair<LocStr, String>> gens = new LinkedList<>();
    List<Pair<Integer, Pair<RawTerm, RawTerm>>> eqs = new LinkedList<>();
    List<Pair<LocStr, Chc<RawTerm, PreAgg>>> atts = new LinkedList<>();

    QueryExpFront.f(schema, gens, atts, ed.As.entrySet(), ed.Awh, eqs);
    if (isBack) {
      QueryExpFront.f(schema, gens, new LinkedList<>(), ed.Es.entrySet(), ed.Ewh, eqs);
    }

    var xxx =
        new QueryExpRawSimple(
            new SchExpInst<Gen, Sk, X, Y>(I),
            0,
            new PreBlock(gens, eqs, atts, Collections.emptyList(), Collections.emptyList(), false));

    return xxx.eval(env, isC);
  }

  private Pair<Transform, Transform> entails(
      ED e1, ED e2, Schema<String, String, Sym, Fk, Att> schema, AqlEnv env, boolean isC) {

    var e1x = foo(env, e1, schema, false, isC);
    var e2x = foo(env, e2, schema, false, isC);
    var e3x = foo(env, e1, schema, true, isC);
    var e4x = foo(env, e2, schema, true, isC);

    var a = QueryExpReformulate.hom(e1x, e2x);

    if (a == null) {
      return null;
    }
    var d = QueryExpReformulate.hom(e4x, e3x);

    if (d == null) {
      return null;
    }
    return new Pair(a, d);
  }

  private boolean tauto(
      ED e1, Schema<String, String, Sym, Fk, Att> schema, AqlEnv env, boolean isC) {
    var e1x = foo(env, e1, schema, false, isC);
    var e3x = foo(env, e1, schema, true, isC);
    return QueryExpReformulate.hom(e3x, e1x) != null;
  }

  private boolean equiv(
      ED e1, ED e2, Schema<String, String, Sym, Fk, Att> schema, AqlEnv env, boolean isC) {

    var e1x = foo(env, e1, schema, false, isC);
    var e2x = foo(env, e2, schema, false, isC);
    var e3x = foo(env, e1, schema, true, isC);
    var e4x = foo(env, e2, schema, true, isC);

    var a = QueryExpReformulate.hom(e1x, e2x);
    if (a == null) {
      return false;
    }
    var b = QueryExpReformulate.hom(e2x, e1x);
    if (b == null) {
      return false;
    }
    var c = QueryExpReformulate.hom(e3x, e4x);
    if (c == null) {
      return false;
    }
    var d = QueryExpReformulate.hom(e4x, e3x);
    if (d == null) {
      return false;
    }

    return true;
  }

  private Set<UPair<Term<String, String, Sym, Fk, Att, Void, Void>>> mkW(
      Map<String, Chc<String, String>> as, Schema<String, String, Sym, Fk, Att> schema) {

    Set<UPair<Term<String, String, Sym, Fk, Att, Void, Void>>> ret = new HashSet<>();

    for (String en1 : as.keySet()) {
      for (Att att1 : schema.attsFrom(en1)) {
        for (String en2 : as.keySet()) {
          for (Att att2 : schema.attsFrom(en2)) {
            if (att1.equals(att2)) {
              continue;
            }
            if (!schema.atts.get(att1).second.equals(schema.atts.get(att2).second)) {
              continue;
            }
            ret.add(new UPair<>(Term.Att(att1, Term.Var(en1)), Term.Att(att2, Term.Var(en2))));
          }
        }
      }
    }

    return ret;
  }

  @Override
  public String toString() {
    return "all " + I + " " + T;
  }

  @Override
  public SchExp type(AqlTyping G) {
    var st = I.type(G);
    return st;
  }

  @Override
  protected void allowedOptions(Set<AqlOption> set) {}

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {
    I.mapSubExps(f);
  }

  @Override
  public int hashCode() {
    return Objects.hash(I, T);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    EdsExpAll other = (EdsExpAll) obj;
    return Objects.equals(I, other.I) && Objects.equals(T, other.T);
  }
}
