package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.list.TreeList;

import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Collage;
import catdata.aql.Collage.CCollage;
import catdata.aql.Eq;
import catdata.aql.Instance;
import catdata.aql.Kind;
import catdata.aql.Mapping;
import catdata.aql.Query;
import catdata.aql.Query.Agg;
import catdata.aql.Term;

import catdata.aql.fdm.DeltaInstance;
import catdata.aql.fdm.DeltaTransform;
import catdata.aql.fdm.InitialAlgebra;
import catdata.aql.fdm.LiteralInstance;
import catdata.aql.fdm.LiteralTransform;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public final class QueryExpDeltaCoEval extends QueryExp {

  public final MapExp F;
  public final Map<String, String> options;

  public <R, P, E extends Exception> R accept(P params, QueryExpVisitor<R, P, E> v) throws E {
    return v.visit(params, this);
  }

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {
    F.map(f);
  }

  @Override
  public Map<String, String> options() {
    return options;
  }

  @Override
  public Collection<Pair<String, Kind>> deps() {
    return F.deps();
  }

  public QueryExpDeltaCoEval(MapExp F, List<Pair<String, String>> options) {
    this.F = F;
    this.options = Util.toMapSafely(options);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
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
    if (!(obj instanceof QueryExpDeltaCoEval))
      return false;
    QueryExpDeltaCoEval other = (QueryExpDeltaCoEval) obj;
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
    return "toCoQuery " + F;
  }

  @Override
  public Pair<SchExp, SchExp> type(AqlTyping G) {
    return new Pair<>(F.type(G).first, F.type(G).second);
  }
  
  /**
   * only works on closed terms
   */
  private static <Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> Set<Term<Ty, En, Sym, Fk, Att, Gen, Sk>> 
  applyAllSymbolsNotSk(
      Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> J,
      Set<Term<Ty, En, Sym, Fk, Att, Gen, Sk>> set) {
    Set<Term<Ty, En, Sym, Fk, Att, Gen, Sk>> ret = new THashSet<>();

    J.gens().keySet((gen)->{
      ret.add(Term.Gen(gen));
    });
    
    for (Fk fk : J.schema().fks.keySet()) {
      for (Term<Ty, En, Sym, Fk, Att, Gen, Sk> arg : set) {
        Chc<Ty, En> x = J.type(arg.convert());
        if (x.equals(Chc.inRight(J.schema().fks.get(fk).first))) {
          ret.add(Term.Fk(fk, arg));
        }
      }
    }
    for (Att att : J.schema().atts.keySet()) {
      for (Term<Ty, En, Sym, Fk, Att, Gen, Sk> arg : set) {
        Chc<Ty, En> x = J.type(arg.convert());
        if (x.equals(Chc.inRight(J.schema().atts.get(att).first))) {
          ret.add(Term.Att(att, arg));
        }
      }
    }
    for (Sym sym : J.schema().typeSide.syms.keySet()) {
      for (List<Term<Ty, En, Sym, Fk, Att, Gen, Sk>> args : helper(J, J.schema().typeSide.syms.get(sym), set)) {
        ret.add(Term.Sym(sym, args));
      }
    }

    return ret;
  }

  private static <Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> List<Term<Ty, En, Sym, Fk, Att, Gen, Sk>> 
  getForTy(Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> J, Chc<Ty, En> t,
      Collection<Term<Ty, En, Sym, Fk, Att, Gen, Sk>> set) {
    return set.stream().filter(x ->  J.type(x.convert()).equals(t))
        .collect(Collectors.toList());
  }
  
  private static <Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> List<List<Term<Ty, En, Sym, Fk, Att, Gen, Sk>>> helper(
      Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> j,
      Pair<List<Ty>, Ty> pair,
      Set<Term<Ty, En, Sym, Fk, Att, Gen, Sk>> set) {
    List<List<Term<Ty, En, Sym, Fk, Att, Gen, Sk>>> ret = new TreeList<>();
    ret.add(new TreeList<>());

    for (Ty t : pair.first) {
      List<List<Term<Ty, En, Sym, Fk, Att, Gen, Sk>>> ret2 = new TreeList<>();
      for (Term<Ty, En, Sym, Fk, Att, Gen, Sk> l : getForTy(j, Chc.inLeft(t), set)) {
        for (List<Term<Ty, En, Sym, Fk, Att, Gen, Sk>> x : ret) {
          List<Term<Ty, En, Sym, Fk, Att, Gen, Sk>> z = new TreeList<>(x);
          z.add(l);
          ret2.add(z);
        }
      }
      ret = ret2;
    }
    return ret;
  }
  @Override
  public Query<String, String, Sym, Fk, Att, String, Fk, Att> eval0(AqlEnv env, boolean isC) {
    Mapping<String, String, Sym, Fk, Att, String, Fk, Att> F0 = F.eval(env, isC);
    if (isC) {
      throw new IgnoreException();
    }
    AqlOptions ops = new AqlOptions(options, env.defaults);

    Map<String, Triple<Map<String, Chc<String, String>>, Collection<Eq<String, String, Sym, Fk, Att, String, String>>, AqlOptions>> ens = new THashMap<>();
    Map<Att, Chc<Term<String, String, Sym, Fk, Att, String, String>,Agg<String, String, Sym, Fk, Att>>> atts = new THashMap<>();
    Map<Fk, Pair<Map<String, Term<Void, String, Void, Fk, Void, String, Void>>, AqlOptions>> fks = new THashMap<>();
    Map<Fk, Map<String, Term<String, String, Sym, Fk, Att, String, String>>> sks = new THashMap<>();

    Map<String, LiteralInstance<String, String, Sym, Fk, Att, String, Void, Integer, Chc<Void, Pair<Integer, Att>>>> ys = new THashMap<>();
    Map<String, DeltaInstance<String, String, Sym, Fk, Att, String, Void, String, Fk, Att, Integer, Chc<Void, Pair<Integer, Att>>>> js = new THashMap<>();

    Map<String, Pair<TObjectIntMap<Pair<String, Integer>>, TIntObjectMap<Pair<String, Integer>>>> isos = new THashMap<>();
    String v = ("v");
    Map<String, Map<Term<String, String, Sym, Fk, Att, String, Chc<Void, Pair<Integer, Att>>>, Term<String, String, Sym, Fk, Att, String, String>>> surj = new THashMap<>();

    Map<String, Pair<Map<Chc<Void, Pair<Integer, Att>>, Integer>, Map<Integer, Chc<Void, Pair<Integer, Att>>>>> isos2 = new THashMap<>();
    int skidx = 0;
    for (String en2 : F0.dst.ens) {
      Collage<String, String, Sym, Fk, Att, String, Void> col = new CCollage<>(F0.dst.collage());
      col.gens().put(v, en2);

      InitialAlgebra<String, String, Sym, Fk, Att, String, Void> initial = new InitialAlgebra<>(ops, F0.dst, col, (y) -> y,
          (x, y) -> y);
      LiteralInstance<String, String, Sym, Fk, Att, String, Void, Integer, Chc<Void, Pair<Integer, Att>>> y = new LiteralInstance<>(
          F0.dst, col.gens(), col.sks(), col.eqsAsPairs(), initial.dp(), initial,
          (Boolean) ops.getOrDefault(AqlOption.require_consistency),
          (Boolean) ops.getOrDefault(AqlOption.allow_java_eqs_unsafe));
      ys.put(en2, y);

      DeltaInstance<String, String, Sym, Fk, Att, String, Void, String, Fk, Att, Integer, Chc<Void, Pair<Integer, Att>>> J = new DeltaInstance<>(
          F0, y);
      js.put(en2, J);

      Pair<TObjectIntMap<Pair<String, Integer>>, TIntObjectMap<Pair<String, Integer>>> iso = J.algebra().intifyX(1000); // avoid
                                                            // sks
      Pair<Map<Chc<Void, Pair<Integer, Att>>, Integer>, Map<Integer, Chc<Void, Pair<Integer, Att>>>> iso2 = new Pair<>(
          new THashMap<>(), new THashMap<>());
      isos.put(en2, iso);
      isos2.put(en2, iso2);

      Map<String, Chc<String, String>> fr = new THashMap<>();
      Collection<Eq<String, String, Sym, Fk, Att, String, String>> wh = new LinkedList<>();

      for (String en1 : J.schema().ens) {
        for (Pair<String, Integer> id : J.algebra().en(en1)) {
          fr.put(("gen" + iso.first.get(id)), Chc.inLeft(en1));
        }
      }

      Map<Term<String, String, Sym, Fk, Att, String, Chc<Void, Pair<Integer, Att>>>, Term<String, String, Sym, Fk, Att, String, String>> surjX = new THashMap<>();
      for (Chc<Void, Pair<Integer, Att>> p : J.algebra().talg().sks.keySet()) {
        Set<Term<String, String, Sym, Fk, Att, Pair<String, Integer>, Void>> set = new THashSet<>();
        Term<String, String, Sym, Fk, Att, String, String> u = null;

        outer: for (int i = 0; i < (int) ops.getOrDefault(AqlOption.toCoQuery_max_term_size); i++) {
          Set<Term<String, String, Sym, Fk, Att, Pair<String, Integer>, Void>> set2 = 
              applyAllSymbolsNotSk((Instance)J, set);
          for (Term<String, String, Sym, Fk, Att, Pair<String, Integer>, Void> s : set2) {
            if (J.type(Term.Sk(p)).equals(J.type(s.convert()))) {
              if (J.dp().eq(null, Term.Sk(p), s.convert())) {
                u = s.mapGenSk(pp -> ("gen" + iso.first.get(pp)), Util.voidFn());
                break outer;
              }
            }
          }
          set.addAll(set2);
        }
        if (u == null) {
          String vv = ("sk" + skidx);
          fr.put(vv, Chc.inRight(J.algebra().talg().sks.get(p)));
          iso2.first.put(p, skidx);
          iso2.second.put(skidx, p);
          skidx++;
          surjX.put(Term.Sk(p), Term.Sk(vv));
          // surjective on attributes
        } else {
          String vv = ("sk" + skidx);
          fr.put(vv, Chc.inRight(J.algebra().talg().sks.get(p)));
          iso2.first.put(p, skidx);
          iso2.second.put(skidx, p);
          skidx++;
          //surjX.put(Term.Sk(p), Term.Sk(vv));
          //wh.
          
          wh.add(new Eq<>(Collections.emptyMap(), Term.Sk(vv), u));
          surjX.put(Term.Sk(p), u);  
          
        }

      }
      isos2.put(en2, iso2);
      surj.put(en2, surjX);
      //System.out.println(surj);
      //System.out.println("**");

      J.eqs((a,b)-> {
        Function<Pair<String, Integer>, String> genf = x -> ("gen" + iso.first.get(x));

        Term<String, String, Sym, Fk, Att, String, Chc<Void, Pair<Integer, Att>>> tz = a.mapGen(genf);
        Term tt0 = tz;
        Term tt = tt0.replace(surjX);

        Term<String, String, Sym, Fk, Att, String, Chc<Void, Pair<Integer, Att>>> qw = b.mapGen(genf);
        Term qw1 = qw;

        Term<String, String, Sym, Fk, Att, String, String> ttA = qw1.replace(surjX);

        if (!tt.equals(ttA)) {
          wh.add(new Eq<>(null, tt, ttA));
        }
      });
      
      ens.put(en2, new Triple<>(fr, wh, ops));
    }
    
    
    
    for (Fk fk2 : F0.dst.fks.keySet()) {
      //Map<Var, Term<Void, En, Void, Fk, Void, Var, Void>> gens = new THashMap<>();
      //gens.put(v, Term.Fk(fk2, Term.Gen(v)));
      
      LiteralTransform<String, String, Sym, Fk, Att, String, Void, String, Void, Integer, Chc<Void, Pair<Integer, Att>>, Integer, Chc<Void, Pair<Integer, Att>>> t = new LiteralTransform<>(
          (a,b)->Term.Fk(fk2, Term.Gen(v)), (a,b)->Util.abort(a), ys.get(F0.dst.fks.get(fk2).second), ys.get(F0.dst.fks.get(fk2).first),
          (Boolean) ops.getOrDefault(AqlOption.dont_validate_unsafe));

      DeltaTransform<String, String, Sym, Fk, Att, String, Void, String, Fk, Att, String, Void, Integer, Chc<Void, Pair<Integer, Att>>, Integer, Chc<Void, Pair<Integer, Att>>> h = new DeltaTransform<>(
          F0, t);

      Map<String, Term<Void, String, Void, Fk, Void, String, Void>> g = new THashMap<>();
      Map<String, Term<String, String, Sym, Fk, Att, String, String>> hh = new THashMap<>();

      for (Entry<String, Chc<String, String>> u : ens.get(F0.dst.fks.get(fk2).second).first.entrySet()) {
        Pair<TObjectIntMap<Pair<String, Integer>>, TIntObjectMap<Pair<String, Integer>>> iso1 = isos
            .get(F0.dst.fks.get(fk2).first);
        Pair<TObjectIntMap<Pair<String, Integer>>, TIntObjectMap<Pair<String, Integer>>> iso2 = isos
            .get(F0.dst.fks.get(fk2).second);
        Function<Pair<String, Integer>, String> genf = p -> {
          Integer y1 = iso1.first.get(p);
          return ("gen" + y1);
        };
        Integer u0;
        if (u.getValue().left) {
          u0 = Integer.parseInt(u.getKey().substring(3));
          Pair<String, Integer> x = iso2.second.get(u0);
          Pair<String, Integer> y = h.repr(x.first, x);
          Term<Void, String, Void, Fk, Void, String, Void> tt = h.dst().algebra().repr(y.first, y).mapGen(genf);
          g.put(u.getKey(), tt);
        } else {
          u0 = Integer.parseInt(u.getKey().substring(2));
          Pair<Map<Chc<Void, Pair<Integer, Att>>, Integer>, Map<Integer, Chc<Void, Pair<Integer, Att>>>> iso1x = isos2
              .get(F0.dst.fks.get(fk2).first);
          Pair<Map<Chc<Void, Pair<Integer, Att>>, Integer>, Map<Integer, Chc<Void, Pair<Integer, Att>>>> iso2x = isos2
              .get(F0.dst.fks.get(fk2).second);

          Chc<Void, Pair<Integer, Att>> x = iso2x.second.get(u0);
          Term<String, String, Sym, Fk, Att, Pair<String, Integer>, Chc<Void, Pair<Integer, Att>>> y = h.reprT(x);

          Function<Chc<Void, Pair<Integer, Att>>, String> skf = p -> {
            Integer y1 = iso1x.first.get(p);
            if (y1 == null) {
              //return surj.get(p.r.second.en).get(Term.Sk(p));
              throw new RuntimeException("No attribute mapping for " + p.r + "; available:\n\n" + iso1x.first + "\n\n" + isos2);
            }
            return ("sk" + y1);
          };

          Term<String, String, Sym, Fk, Att, String, String> tt = y.mapGenSk(genf, skf);
          hh.put(u.getKey(), tt);
        }
      }
      sks.put(fk2, hh);
      fks.put(fk2, new Pair<>(g, ops));
    }
    
    for (Att att2 : F0.dst.atts.keySet()) {
//      Term<Ty, En, Sym, Fk, Att, Var, Void> g = null;

      Term<String, Void, Sym, Void, Void, Void, Chc<Void, Pair<Integer, Att>>> t = ys.get(F0.dst.atts.get(att2).first)
          .algebra().intoY(Term.Att(att2, Term.Gen(v)));

      Term<String, String, Sym, Fk, Att, Pair<String, Integer>, Chc<Void, Pair<Integer, Att>>> s = js
          .get(F0.dst.atts.get(att2).first).reprT(t);

      Pair<TObjectIntMap<Pair<String, Integer>>, TIntObjectMap<Pair<String, Integer>>> iso1 = isos
          .get(F0.dst.atts.get(att2).first);

      Function<Pair<String, Integer>, String> genf = p -> {
        Integer y1 = iso1.first.get(p);
        return ("gen" + y1);
      };

      Term<String, String, Sym, Fk, Att, String, ?> tz = s.mapGen(genf);
      Term tt0 = tz;
      if (surj.containsKey(F0.dst.atts.get(att2).first)) {
        tt0 = tt0.replace(surj.get(F0.dst.atts.get(att2).first));
      }

      atts.put(att2, Chc.inLeft(tt0));
    }

    return Query.makeQuery(ens, atts, fks, sks, F0.src, F0.dst,
        new AqlOptions(ops, AqlOption.dont_validate_unsafe, true));
  }

  @Override
  protected void allowedOptions(Set<AqlOption> set) {
    set.add(AqlOption.dont_validate_unsafe);
    set.add(AqlOption.query_remove_redundancy);
    set.add(AqlOption.require_consistency);
    set.add(AqlOption.allow_java_eqs_unsafe);
    set.add(AqlOption.toCoQuery_max_term_size);
    set.add(AqlOption.dont_validate_unsafe);
    set.addAll(AqlOptions.proverOptionNames());

  }

}