package catdata.provers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.jsonldjava.shaded.com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;

import catdata.Pair;
import catdata.Triple;
import catdata.Unit;
import catdata.Util;
import gnu.trove.set.hash.THashSet;

public class KBTheory<T, C, V> {

  public final KBExpFactory<T, C, V> factory;

  @Override
  public String toString() {
    return "KBTheory [tys=" + tys + ", syms=" + syms + ", eqs=" + eqs + "]";
  }

  public KBTheory(KBTheory<T, C, V> kb2) {
    this(kb2.factory, Unit.unit);
    this.tys.addAll(kb2.tys);
    this.syms.putAll(kb2.syms);
    this.eqs = kb2.eqs;
    // validate(); // TODO aql disable for production
  }

  public void add(KBTheory<T, C, V> kb2) {
    this.tys.addAll(kb2.tys);
    this.syms.putAll(kb2.syms);
    this.eqs = Iterables.concat(eqs, kb2.eqs);
  }

  public synchronized void validate() {
    for (C sym : syms.keySet()) {
      Pair<List<T>, T> T = syms.get(sym);
      if (!tys.contains(T.second)) {
        throw new RuntimeException("On symbol " + sym + ", the return Type " + T.second + " is not declared.");
      }
      for (T t : T.first) {
        if (!tys.contains(t)) {
          throw new RuntimeException("On symbol " + sym + ", the argument Type " + t + " is not declared.");
        }
      }

    }
    for (Triple<Map<V, T>, KBExp<C, V>, KBExp<C, V>> eq : eqs) {
      // check that the context is valid for each eq
      Set<T> used_Ts = (new THashSet<>(eq.first.values()));
      used_Ts.removeAll(tys);
      if (!used_Ts.isEmpty()) {
        throw new RuntimeException(
            "In equation " + eq + ", context uses types " + used_Ts + " that are not declared.");
      }
      // check lhs and rhs Types match in all eqs
      T lhs = eq.second.type(syms, eq.first);
      T rhs = eq.third.type(syms, eq.first);
      if (!lhs.equals(rhs)) {
        throw new RuntimeException("In equation " + eq + ", lhs type is " + lhs + " but rhs type is " + rhs);
      }
    }

  }

  public final Collection<T> tys;
  public Map<C, Pair<List<T>, T>> syms;

  public Iterable<Triple<Map<V, T>, KBExp<C, V>, KBExp<C, V>>> eqs;

  public KBTheory(KBExpFactory<T, C, V> factory, Unit unit) {
    this.tys = new THashSet<>();
    this.syms = Util.mk();
    this.eqs = new THashSet<>();
    this.factory = factory;
  }

  public Set<T> inhabGen() {
    Set<T> inhab = new THashSet<>();
    inhabGen(inhab);
    return inhab;
  }

  public KBTheory(KBExpFactory<T, C, V> factory, Collection<T> tys, Map<C, Pair<List<T>, T>> syms,
      Iterable<Triple<Map<V, T>, KBExp<C, V>, KBExp<C, V>>> eqs) {
    super();
    this.factory = factory;
    this.tys = tys;
    this.syms = syms;
    this.eqs = eqs;
  }

  public void inhabGen(Set<T> inhabited) {
    while (inhabGen1(inhabited))
      ;
  }

  private boolean inhabGen1(Set<T> ret) {
    boolean changed = false;
    outer: for (C c : syms.keySet()) {
      for (T t : syms.get(c).first) {
        if (!ret.contains(t)) {
          continue outer;
        }
      }
      changed = changed | ret.add(syms.get(c).second);
    }
    return changed;
  }

  public T type(Map<V, T> ctx, KBExp<C, V> e) {
    return e.type(syms, ctx);
  }

  private final Map<Object, String> isoC1 = Util.mk();
  private final Map<String, Object> isoC2 = Util.mk();

  private final Map<Object, String> isoV1 = Util.mk();
  private final Map<String, Object> isoV2 = Util.mk();

  private final Map<Object, String> isoT1 = Util.mk();
  private final Map<String, Object> isoT2 = Util.mk();

  private int i = 0;

  // TODO: port to open source cql
  public final synchronized <X> X convert(KBExp<C, V> e, Function<String, X> v,
      BiFunction<String, Iterator<X>, X> f) {
    if (e.isVar()) {
      return v.apply(convertV(e.getVar()));
    }
    Iterator<X> l = Iterators.transform(e.getArgs().iterator(), arg -> convert(arg, v, f));
    return f.apply(convertC(e.f()), l);
  }

  static String strip(Object s) {
    return "";
    // return s.toString().replace(" ", "").replace(",", "").replace("(",
    // "").replace(")", "").replace("inl", "")
    // .replace("inr", "");
  }

  public final synchronized String convert(KBExp<C, V> e) {
    if (e.isVar()) {
      return convertV(e.getVar());
    }
    List<String> l = new ArrayList<>(e.getArgs().size());
    for (KBExp<C, V> arg : e.getArgs()) {
      l.add(convert(arg));
    }
    if (l.isEmpty()) {
      return convertC(e.f());
    }
    return convertC(e.f()) + "(" + Util.sep(l, ",") + ")";
  }

  public final synchronized String convertV(V e) {
    if (isoV1.containsKey(e)) {
      return isoV1.get(e);
    }
    isoV1.put(e, "V" + i + strip(e));
    isoV2.put("V" + i + strip(e), e);
    i++;

    return isoV1.get(e);
  }

  public final synchronized String convertC(C e) {
    if (isoC1.containsKey(e)) {
      return isoC1.get(e);
    }
    isoC1.put(e, "s" + i + strip(e));
    isoC2.put("s" + i + strip(e), e);
    i++;

    return isoC1.get(e);
  }

  public final synchronized String convertT(T e) {
    if (isoT1.containsKey(e)) {
      return isoT1.get(e);
    }
    isoT1.put(e, "p" + i + strip(e));
    isoT2.put("p" + i + strip(e), e);
    i++;

    return isoT1.get(e);
  }

  // private String tptp = null;

  public synchronized String tptp(Map<V, T> ctx, KBExp<C, V> lhs, KBExp<C, V> rhs) {
    StringBuffer sb = new StringBuffer(tptp());
    sb.append("fof(eq" + j + ",conjecture,(");
    j++;
    if (!ctx.isEmpty()) {
      sb.append("! [");
      sb.append(Util.sep(ctx.keySet().stream().map(this::convertV).collect(Collectors.toList()), ","));
      sb.append("] : (");
    }

    if (ctx.keySet().isEmpty()) {

    } else if (ctx.keySet().size() == 1) {
      sb.append("(");
      for (V v : ctx.keySet()) {
        sb.append(convertT(ctx.get(v)) + "(" + convertV(v) + ")");
      }
      sb.append(") => ");
    } else {
      boolean first = true;
      sb.append("(");
      for (V v : ctx.keySet()) {
        if (!first) {
          sb.append(" & ");
        }
        sb.append(convertT(ctx.get(v)) + "(" + convertV(v) + ")");
        first = false;
      }
      sb.append(") => ");
    }

    sb.append(convert(lhs) + " = " + convert(rhs) + "))");
    if (!ctx.isEmpty()) {
      sb.append(")");
    }
    sb.append(".");
    sb.append(System.lineSeparator());
    return sb.toString();
  }

  public synchronized String tptp_typed_nonempty() {
    StringBuilder sb = new StringBuilder();
    for (Triple<Map<V, T>, KBExp<C, V>, KBExp<C, V>> eq : eqs) {
      Map<V, T> ctx = eq.first;
      sb.append("tff(eq" + j + ",axiom,(");
      if (!ctx.isEmpty()) {
        sb.append("! [");
        sb.append(Util.sep(ctx.keySet().stream().map(this::convertV).collect(Collectors.toList()), ","));
        sb.append("] : ((");

        boolean first = true;
        for (V v : ctx.keySet()) {
          if (first == false) {
            sb.append(" & ");
          }
          sb.append(convertT(ctx.get(v)) + "(" + convertV(v) + ")");
          first = false;
        }
        sb.append(") => ");
      }
      sb.append(convert(eq.second) + " = " + convert(eq.third) + "))");
      if (!ctx.isEmpty()) {
        sb.append(")");
      }
      sb.append(".");
      sb.append(System.lineSeparator());
      j++;
    }

    String tptp = sb.toString();
    return tptp;
  }

  public static int j = 0;

  public synchronized String tptp() {
    // if (tptp != null) {
    // return tptp;
    // }

    // int j = 1; // 0 reserved for other tptp fn
    StringBuilder sb = new StringBuilder();
    for (Triple<Map<V, T>, KBExp<C, V>, KBExp<C, V>> eq : eqs) {
      Map<V, T> ctx = eq.first;
      sb.append("fof(eq" + j + ",axiom,(");
      if (!ctx.isEmpty()) {
        sb.append("! [");
        sb.append(Util.sep(ctx.keySet().stream().map(this::convertV).collect(Collectors.toList()), ","));
        sb.append("] : ((");

        boolean first = true;
        for (V v : ctx.keySet()) {
          if (first == false) {
            sb.append(" & ");
          }
          sb.append(convertT(ctx.get(v)) + "(" + convertV(v) + ")");
          first = false;
        }
        sb.append(") => ");
      }
      sb.append(convert(eq.second) + " = " + convert(eq.third) + "))");
      if (!ctx.isEmpty()) {
        sb.append(")");
      }
      sb.append(".");
      sb.append(System.lineSeparator());
      j++;
    }

    String tptp = sb.toString();
    return tptp;
  }

  // todo: S(x) -> x=c1 or ... or Eyz. x = f(y,z)?
  // String preamble;
  public synchronized String tptp_preamble() {
    // if (preamble != null) {
    // return preamble;
    // }
    // int j = 0; // 0 reserved for other tptp fn
    StringBuilder sb = new StringBuilder();

    for (T t : tys) {
      List<String> y = new LinkedList<>();
      for (T t2 : tys) {
        if (t.equals(t2)) {
          continue;
        }
        y.add("(~" + convertT(t2) + "(X))");
      }
      if (y.isEmpty()) {
        continue;
      }
      sb.append("fof(sort" + (j++) + ",axiom,(");
      sb.append("! [ X ] ");
      sb.append(" : (");
      sb.append(convertT(t) + "(X) => (");
      sb.append(Util.sep(y, " & "));

      sb.append(")))).\n");
    }

    for (C c : syms.keySet()) {
      sb.append("fof(sym" + (j++) + ",axiom,(");
      List<String> l = new LinkedList<>();
      int i = 0;
      for (@SuppressWarnings("unused")
      T t : syms.get(c).first) {
        String x = "X" + (i++);
        l.add(x);
      }

      if (!syms.get(c).first.isEmpty()) {
        sb.append("![");
        sb.append(Util.sep(l, ", "));
        sb.append("] : (");
      }

      i = 0;
      if (!syms.get(c).first.isEmpty()) {
        sb.append("(");
        boolean first = true;
        for (T t : syms.get(c).first) {
          if (!first) {
            sb.append(" & ");
          }
          String x = "X" + (i++);
          sb.append(convertT(t) + "(" + x + ")");
          first = false;
        }
        sb.append(") => ");
      }
      String args = l.isEmpty() ? "" : "(" + Util.sep(l, ",") + ")";
      sb.append(convertT(syms.get(c).second) + "(" + convertC(c) + args + ")");
      if (!syms.get(c).first.isEmpty()) {
        sb.append(")");
      }
      sb.append(")).");

      sb.append(System.lineSeparator());
      j++;
    }

    String preamble = sb.toString();
    return preamble;
  }

  ////////////////////////////////////////////////////////////////////////////////////

  private String tptp_cnf = null;

  public synchronized String tptp_cnf() {
    // if (tptp_cnf != null) {
    // return tptp;
    // }

    int j = 1; // 0 reserved for other tptp fn
    StringBuilder sb = new StringBuilder();
    for (Triple<Map<V, T>, KBExp<C, V>, KBExp<C, V>> eq : eqs) {
      // Map<V, T> ctx = eq.first;
      sb.append("cnf(eq" + j + ",axiom,(");
      sb.append(convert(eq.second) + " = " + convert(eq.third) + "))");
      sb.append(".");
      sb.append(System.lineSeparator());
      j++;
    }

    tptp_cnf = sb.toString();
    return tptp_cnf;
  }

  public String printIso() {
    return Util.sep(isoC1, "=", "\n") + "\n" + Util.sep(isoT1, "=", "\n");
  }

  // TODO deprecate other TPTP methods, the proliferation is ridiculous

  public String tff(Map<V, T> ctx, KBExp<C, V> lhs, KBExp<C, V> rhs) {
    return tff() + tffSentence(TffRole.CONJECTURE, tffEquation(ctx, lhs, rhs));
  }

  public String tff() {
    StringBuilder sb = new StringBuilder();
    for (T ty : tys) {
      sb.append(tffTypeDeclaration(ty));
    }
    for (C sym : syms.keySet()) {
      sb.append(tffSymDeclaration(sym));
    }
    for (T ty : inhabGen()) {
      sb.append(tffSentence(TffRole.AXIOM, tffInhabited(ty)));
    }
    for (Triple<Map<V, T>, KBExp<C, V>, KBExp<C, V>> eq : eqs) {
      sb.append(tffSentence(TffRole.AXIOM, tffEquation(eq.first, eq.second, eq.third)));
    }
    return sb.toString();
  }

  private String tffTypeDeclaration(T ty) {
    return tffSentence(TffRole.TYPE, String.format("%s : " + TYPE_TYPE, convertT(ty)));
  }

  private String tffSymDeclaration(C sym) {
    return tffSentence(TffRole.TYPE, String.format("%s : %s", convertC(sym), tffType(syms.get(sym))));
  }

  private String tffType(Pair<List<T>, T> type) {
    List<T> args = type.first;
    String argsStr = args.isEmpty() ? "" : Util.sep(args, " * ", this::convertT) + " > ";
    return argsStr + convertT(type.second);
  }

  private String tffEquation(Map<V, T> ctx, KBExp<C, V> lhs, KBExp<C, V> rhs) {
    String equation = String.format("%s = %s", convert(lhs), convert(rhs));
    if (ctx.isEmpty()) {
      return equation;
    } 
      String inhabCondition = Util.sep(ctx.values(), " & ", this::tffInhabited);
      String quantifiers = Util.sep(ctx.entrySet(), ", ", e -> {
        return String.format("%s:%s", convertV(e.getKey()), convertT(e.getValue()));
      });
      return String.format("(%s) => ![%s] : %s", inhabCondition, quantifiers, equation);
    
  }

  private String tffInhabited(T ty) {
    return String.format("%s(%s)", INHABITED_PREDICATE, convertT(ty));
  }

  private static final String INHABITED_PREDICATE = "inhabited",
                              TYPE_TYPE = "$tType";
//                              FORMULA_TYPE = "$o";

  private String tffSentence(TffRole role, String content) {
    return String.format("tff(%s%d, %s, (%s)).\n", role, fresh(), role, content);
  }

  private static enum TffRole {
    TYPE("type"), AXIOM("axiom"), CONJECTURE("conjecture");

    public final String role;

    TffRole(String role) {
      this.role = role;
    }

    public String toString() {
      return role;
    }
  }

  private java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger();
  private int fresh() {
    return counter.getAndIncrement();
  }
}
