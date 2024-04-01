package catdata.cql.exp;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import catdata.InteriorLabel;
import catdata.LocException;
import catdata.LocStr;
import catdata.Pair;
import catdata.Raw;
import catdata.Util;
import catdata.cql.Kind;
import catdata.cql.Mor;
import catdata.cql.TypeSide;
import catdata.cql.AqlOptions.AqlOption;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public class MorExpRaw extends MorExp implements Raw {

  public final TyExp src;
  public final TyExp dst;

  public final Set<MorExp> imports;

  public final Set<Pair<String, String>> tys;
  public final Set<Pair<String, Pair<List<Pair<String, String>>, RawTerm>>> syms;

  public final Map<String, String> options;

  public MorExpRaw(TyExp src, TyExp dst, List<MorExp> a, List<Pair<LocStr, String>> listA,
      List<Pair<LocStr, Pair<List<Pair<String, String>>, RawTerm>>> listB, List<Pair<String, String>> options) {
    this.src = src;
    this.dst = dst;
    this.imports = new THashSet<>(a);

    // Map<LocStr, Pair<String, List<Pair<String, Pair<List<Pair<String, String>>,
    // RawTerm>>>>> list2 = Util
    // .toMapSafely(list);

    this.tys = new THashSet<>();
    this.syms = new THashSet<>();

    for (Pair<LocStr, String> x : listA) {
      this.tys.add(new Pair<>(x.first.str, x.second));
      tyPos.put((x.first.str), x.first.loc);
    }
    for (Pair<LocStr, Pair<List<Pair<String, String>>, RawTerm>> x : listB) {
      Pair<List<Pair<String, String>>, RawTerm> p = new Pair<>(x.second.first, x.second.second);
      this.syms.add(new Pair<>(x.first.str, p));
  //    symPos.put(Sym.Sym(x.first.str), x.first.loc);
    }

    this.options = Util.toMapSafely(options);
    Util.toMapSafely(this.tys);
    Util.toMapSafely(this.syms);

    raw.put("types", new LinkedList<>());
    raw.put("functions", new LinkedList<>());

    for (Pair<String, String> p : tys) {
      List<InteriorLabel<Object>> inner = new LinkedList<>();
      raw.put(p.first, inner);
      String x = (p.first);
      inner.add(new InteriorLabel<>("types", p, tyPos.get(x), xx -> xx.first + " -> " + xx.second).conv());
    }
    for (Pair<String, Pair<List<Pair<String, String>>, RawTerm>> p : syms) {
      List<InteriorLabel<Object>> inner = new LinkedList<>();
      raw.put(p.first, inner);

   //   Sym x = Sym.Sym(p.first);
   //   inner.add(new InteriorLabel<>("functions", p, symPos.get(x),
   //       xx -> Util.sep(xx.second.first, ":") + " . " + xx.second.second).conv());
    }
  }

  @Override
  public Map<String, String> options() {
    return options;
  }

  private Map<String, Integer> tyPos = new THashMap<>();
 // private Map<Sym, Integer> symPos = new THashMap<>();

  @Override
  public Collection<Pair<String, Kind>> deps() {
    Set<Pair<String, Kind>> ret = new THashSet<>();
    ret.addAll(src.deps());
    ret.addAll(dst.deps());
    for (MorExp x : imports) {
      ret.addAll(x.deps());
    }
    return ret;
  }

  @Override
  public Collection<Exp<?>> imports() {
    return (Collection<Exp<?>>) (Object) imports;
  }

  @Override
  public <R, P, E extends Exception> MorExp coaccept(P params, MorExpCoVisitor<R, P, E> v, R r) throws E {
    return v.visitMorExpRaw(params, r);
  }

  @Override
  public <R, P, E extends Exception> R accept(P params, MorExpVisitor<R, P, E> v) throws E {
    return v.visit(params, this);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((dst == null) ? 0 : dst.hashCode());
    result = prime * result + ((imports == null) ? 0 : imports.hashCode());
    result = prime * result + ((options == null) ? 0 : options.hashCode());
    result = prime * result + ((src == null) ? 0 : src.hashCode());
    result = prime * result + ((syms == null) ? 0 : syms.hashCode());
    result = prime * result + ((tys == null) ? 0 : tys.hashCode());
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
    MorExpRaw other = (MorExpRaw) obj;
    if (dst == null) {
      if (other.dst != null)
        return false;
    } else if (!dst.equals(other.dst))
      return false;
    if (imports == null) {
      if (other.imports != null)
        return false;
    } else if (!imports.equals(other.imports))
      return false;
    if (options == null) {
      if (other.options != null)
        return false;
    } else if (!options.equals(other.options))
      return false;
    if (src == null) {
      if (other.src != null)
        return false;
    } else if (!src.equals(other.src))
      return false;
    if (syms == null) {
      if (other.syms != null)
        return false;
    } else if (!syms.equals(other.syms))
      return false;
    if (tys == null) {
      if (other.tys != null)
        return false;
    } else if (!tys.equals(other.tys))
      return false;
    return true;
  }

  Map<String, List<InteriorLabel<Object>>> raw = new THashMap<>();

  @Override
  public Map<String, List<InteriorLabel<Object>>> raw() {
    return raw;
  }

  @Override
  protected void allowedOptions(Set<AqlOption> set) {
    set.add(AqlOption.dont_validate_unsafe);
  }

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {
    src.map(f);
    dst.map(f);
  }

  @Override
  public Pair<TyExp, TyExp> type(AqlTyping G) {
    src.type(G);
    dst.type(G);
    return new Pair<>(src, dst);
  }

  @Override
  public Mor<String, Sym, String, Sym> eval0(AqlEnv env, boolean isC) {
    TypeSide<String, Sym> src0 = src.eval(env, isC);
    TypeSide<String, Sym> dst0 = dst.eval(env, isC);
    //Collage<String, String, Sym, Fk, Att, String, String> dcol = new CCollage<>();

    Map<String, String> ens0 = new THashMap<>(tys.size());
    // Map<Sym, Pair<List<Pair<String, String>>, Term<String, Void, Sym, Void, Void,
    // Void, Void>>> syms0 = new THashMap<>(
    // syms.size());

    for (MorExp k : imports) {
      Mor<String, Sym, String, Sym> v = k.eval(env, isC); // env.defs.maps.get(k);
      Util.putAllSafely(ens0, v.tys);
      // Util.putAllSafely(syms0, v.syms);
    }

    Util.putAllSafely(ens0, Util
        .toMapSafely(tys.stream().map(x -> new Pair<>((x.first), (x.second))).collect(Collectors.toList())));
    for (String k : ens0.keySet()) {
      if (!dst0.tys.contains(ens0.get(k))) {
        throw new LocException(find("types", new Pair<>(k, ens0.get(k))), "The mapping for " + k + ", namely "
            + ens0.get(k) + ", does not appear in the target typeside");
      } else if (!src0.tys.contains(k)) {
        throw new LocException(find("types", new Pair<>(k, ens0.get(k))),
            k + " does not appear in the source typeside");
      }
    }

    for (Pair<String, Pair<List<Pair<String, String>>, RawTerm>> eq : syms) {
      try {
      //  Chc<String, Void> et = Chc.inLeft(ens0.get(src0.syms.get(Sym.Sym(eq.first)).second));
      //  Triple<Map<String, Chc<String, Void>>, Term<String, Void, Sym, Void, Void, Void, Void>, Term<String, Void, Sym, Void, Void, Void, Void>> tr = TyExpRaw
      //      .infer1x(TyExpRaw.yyy(eq.second.first), eq.second.second, null, et, dcol, "", dst0.js);
        // x syms0.put(sym, new Pair)
        // col.eqs.add(new Eq<>(tr.first, tr.second, tr.third));
      } catch (Exception ex) {
        ex.printStackTrace();
        throw new LocException(find("functions", eq), "In mapping for " + eq.first + ", " + ex.getMessage());
      }

    }
  //  AqlOptions ops = new AqlOptions(options, env.defaults);

    Mor<String, Sym, String, Sym> ret = null; // new Mor<>(ens0, syms0, src0, dst0,
    // (Boolean) ops.getOrDefault(AqlOption.dont_validate_unsafe));
    return ret;
  }

  @Override
  public String toString() {
    // TODO Auto-generated method stub
    return null;
  }

}
