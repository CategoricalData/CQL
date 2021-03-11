package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Formatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Chc;
import catdata.InteriorLabel;
import catdata.LocException;
import catdata.LocStr;
import catdata.Null;
import catdata.Pair;
import catdata.Raw;
import catdata.Triple;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Collage;
import catdata.aql.Collage.CCollage;
import catdata.aql.Eq;
import catdata.aql.Instance;
import catdata.aql.It.ID;
import catdata.aql.Kind;
import catdata.aql.NoAlgInstance;
import catdata.aql.Schema;
import catdata.aql.Term;

import catdata.aql.fdm.ImportAlgebra;
import catdata.aql.fdm.InitialAlgebra;
import catdata.aql.fdm.LiteralInstance;
import catdata.aql.fdm.SaturatedInstance;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public final class InstExpRaw extends InstExp<String, String, Integer, Chc<String, Pair<Integer, Att>>> implements Raw {

  public <R, P, E extends Exception> R accept(P param, InstExpVisitor<R, P, E> v) throws E {
    return v.visit(param, this);
  }

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {
    schema.map(f);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Collection<Exp<?>> imports() {
    return (Collection<Exp<?>>) (Object) imports;
  }


  private Map<String, List<InteriorLabel<Object>>> raw = new THashMap<>();

  public Map<String, List<InteriorLabel<Object>>> raw() {
    return raw;
  }

  @Override
  public Collection<Pair<String, Kind>> deps() {
    Set<Pair<String, Kind>> ret = new THashSet<>(schema.deps());
    for (InstExp<?, ?, ?, ?> x : imports) {
      ret.addAll(x.deps());
    }
    return ret;
  }

  public final SchExp schema;

  public final Set<InstExp<?, ?, ?, ?>> imports;

  public final Set<Pair<String, String>> gens;

  public final Set<Pair<RawTerm, RawTerm>> eqs;

  public final Map<String, String> options;

  @Override
  public Map<String, String> options() {
    return options;
  }

  // typesafe by covariance of read-only collections
  public InstExpRaw(SchExp schema, List<InstExp<?, ?, ?, ?>> imports, List<Pair<LocStr, String>> gens,
      List<Pair<Integer, Pair<RawTerm, RawTerm>>> eqs, List<Pair<String, String>> options) {
    this.schema = schema;
    this.imports = new THashSet<>(imports);
    this.gens = LocStr.set2(gens);
    this.eqs = LocStr.proj2(eqs);
    this.options = Util.toMapSafely(options);

    // List<InteriorLabel<Object>> i = InteriorLabel.imports("imports", imports);
    // raw.put("imports", i);

    List<InteriorLabel<Object>> e = new LinkedList<>();
    for (Pair<LocStr, String> p : gens) {
      e.add(new InteriorLabel<>("generators", new Pair<>(p.first.str, p.second), p.first.loc,
          x -> x.first + " : " + x.second).conv());
    }
    raw.put("generators", e);

    List<InteriorLabel<Object>> xx = new LinkedList<>();
    for (Pair<Integer, Pair<RawTerm, RawTerm>> p : eqs) {
      xx.add(new InteriorLabel<>("equations", p.second, p.first, x -> x.first + " = " + x.second).conv());
    }
    raw.put("equations", xx);
  }

  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("literal : " + schema + " {");

    if (!imports.isEmpty()) {
      sb.append("\n\timports");
      sb.append("\n\t\t" + Util.sep(imports, " ") + "\n");
    }

    List<String> temp = new LinkedList<>();

    if (!gens.isEmpty()) {
      sb.append("\n\tgenerators");

      Map<String, Set<String>> n = Util.revS(Util.toMapSafely(gens));

      temp = new LinkedList<>();
      for (Object x : Util.alphabetical(n.keySet())) {
        temp.add(Util.sep(Util.alphabetical(n.get(x)), " ") + " : " + x);
      }

      sb.append("\n\t\t" + Util.sep(temp, "\n\t\t"));
    }

    if (!eqs.isEmpty()) {
      sb.append("\n\tequations");
      temp = new LinkedList<>();
      for (Pair<RawTerm, RawTerm> sym : Util.alphabetical(eqs)) {
        temp.add(sym.first + " = " + sym.second);
      }
      if (eqs.size() < 9) {
        sb.append("\n\t\t" + Util.sep(temp, "\n\t\t"));
      } else {
        int step = 3;
        int longest = 32;
        for (String s : temp) {
          if (s.length() > longest) {
            longest = s.length() + 4;
          }
        }
        for (int i = 0; i < temp.size(); i += step) {
          Formatter formatter = new Formatter(new StringBuilder(), Locale.US);
          List<String> args = new LinkedList<>();
          List<String> format = new LinkedList<>();
          for (int j = i; j < Integer.min(temp.size(), i + step); j++) {
            args.add(temp.get(j));
            format.add("%-" + longest + "s");
          }
          final String formatStr = Util.sep(format, "");
          final Object[] formatTgt = args.toArray(new String[0]);
          final String x = formatter.format(formatStr, formatTgt).toString();
          formatter.close();
          sb.append("\n\t\t" + x);
        }
        sb.append("\n");
      }
    }

    if (!options.isEmpty()) {
      sb.append("\n\toptions");
      temp = new LinkedList<>();
      for (Entry<String, String> sym : options.entrySet()) {
        temp.add(sym.getKey() + " = " + Util.maybeQuote(sym.getValue()));
      }

      sb.append("\n\t\t" + Util.sep(temp, "\n\t\t"));
    }

    return sb.toString().trim() + "}";
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    result = prime * result + ((eqs == null) ? 0 : eqs.hashCode());
    result = prime * result + ((gens == null) ? 0 : gens.hashCode());
    result = prime * result + ((imports == null) ? 0 : imports.hashCode());
    result = prime * result + ((options == null) ? 0 : options.hashCode());
    result = prime * result + ((schema == null) ? 0 : schema.hashCode());
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
    InstExpRaw other = (InstExpRaw) obj;
    if (eqs == null) {
      if (other.eqs != null)
        return false;
    } else if (!eqs.equals(other.eqs))
      return false;
    if (gens == null) {
      if (other.gens != null)
        return false;
    } else if (!gens.equals(other.gens))
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
    if (schema == null) {
      if (other.schema != null)
        return false;
    } else if (!schema.equals(other.schema))
      return false;
    return true;
  }

  @SuppressWarnings("unchecked")
  @Override
  public synchronized Instance<String, String, Sym, Fk, Att, String, String, Integer, Chc<String, Pair<Integer, Att>>> eval0(AqlEnv env,
      boolean isC) {
    Schema<String, String, Sym, Fk, Att> sch = schema.eval(env, isC);
    Collage<String, String, Sym, Fk, Att, String, String> col = new CCollage<>(sch.collage());

    Set<Pair<Term<String, String, Sym, Fk, Att, String, String>, Term<String, String, Sym, Fk, Att, String, String>>> eqs0 = new THashSet<>();

    for (InstExp<?, ?, ?, ?> k : imports) {
      Instance<String, String, Sym, Fk, Att, String, String, ID, Chc<String, Pair<ID, Att>>> v = (Instance<String, String, Sym, Fk, Att, String, String, ID, Chc<String, Pair<ID, Att>>>) k
          .eval(env, isC);
      col.addAll(v.collage());
      v.eqs((a, b) -> {
        eqs0.add(new Pair<>(a, b));
      });
    }

    for (Pair<String, String> p : gens) {
      String gen = p.first;
      String ty = p.second;
      if (sch.ens.contains((ty))) {
        col.gens().put(gen, (ty));
      } else if (sch.typeSide.tys.contains((ty))) {
        col.sks().put(gen, (ty));
      } else {
        throw new LocException(find("generators", p),
            "The sort for " + gen + ", namely " + ty + ", is not declared as a type or entity");
      }
    }

    for (Pair<RawTerm, RawTerm> eq : eqs) {
      try {
        Triple<Map<String, Chc<String, String>>, Term<String, String, Sym, Fk, Att, String, String>, Term<String, String, Sym, Fk, Att, String, String>> eq0 = RawTerm
            .infer1x(Collections.emptyMap(), eq.first, eq.second, null, col, "", sch.typeSide.js).first3();

        eqs0.add(new Pair<>(eq0.second, eq0.third));
        col.eqs().add(new Eq<>(null, eq0.second, eq0.third));

      } catch (RuntimeException ex) {
        ex.printStackTrace();
        throw new LocException(find("equations", eq),
            "In equation " + eq.first + " = " + eq.second + ", " + ex.getMessage());
      }
    }

    AqlOptions strat = new AqlOptions(options, env.defaults);

    boolean interpret_as_algebra = (boolean) strat.getOrDefault(AqlOption.interpret_as_algebra);
    boolean dont_check_closure = (boolean) strat.getOrDefault(AqlOption.import_dont_check_closure_unsafe);
    boolean interpret_as_frozen = false;

    if (interpret_as_algebra) {
      return eval0_algebra(sch, col, eqs0, strat, dont_check_closure);
    }

    if (interpret_as_frozen) {
      return (Instance<String, String, Sym, Fk, Att, String, String, Integer, Chc<String, Pair<Integer, Att>>>) ((Object) new NoAlgInstance(
          col, sch, strat));
    }

    // col.validate();

    InitialAlgebra<String, String, Sym, Fk, Att, String, String> initial = new InitialAlgebra<>(strat, sch, col, (y) -> y,
        (x, y) -> y);

    return new LiteralInstance<>(sch, col.gens(), col.sks(), eqs0, initial.dp(), initial,
        (Boolean) strat.getOrDefault(AqlOption.require_consistency),
        (Boolean) strat.getOrDefault(AqlOption.allow_java_eqs_unsafe));
  }

  private static Instance<String, String, Sym, Fk, Att, String, String, Integer, Chc<String, Pair<Integer, Att>>> eval0_algebra(
      Schema<String, String, Sym, Fk, Att> sch, Collage<String, String, Sym, Fk, Att, String, String> col,
      Set<Pair<Term<String, String, Sym, Fk, Att, String, String>, Term<String, String, Sym, Fk, Att, String, String>>> eqs0, AqlOptions strat,
      boolean dont_check_closure) {
    @SuppressWarnings({ "unchecked" })
    Map<String, Collection<String>> ens0 = (Map<String, Collection<String>>) (Object) Util.newSetsFor(col.getEns());

    if (!col.sks().isEmpty()) {
      throw new RuntimeException("Cannot have generating labelled nulls with import_as_theory");
    }
    Map<String, Collection<Null<?>>> tys0 = Util.mk();
    for (String ty : sch.typeSide.tys) {
      tys0.put(ty, (new THashSet<>()));
    }
    Map<String, Map<String, Map<Fk, String>>> fks0x = new THashMap<>(col.getEns().size(), 2);
    Map<String, Map<String, Map<Att, Term<String, Void, Sym, Void, Void, Void, Null<?>>>>> atts0x = new THashMap<>(
        col.getEns().size(), 2);

    for (String en : sch.ens) {
      Map<String, Map<Fk, String>> fks0 = new THashMap<>(1024, 1);
      Map<String, Map<Att, Term<String, Void, Sym, Void, Void, Void, Null<?>>>> atts0 = new THashMap<>(1024, 1);
      fks0x.put(en, fks0);
      atts0x.put(en, atts0);
    }

    for (Entry<String, String> gen : col.gens().entrySet()) {
      ens0.get(gen.getValue()).add(gen.getKey());
      fks0x.get(gen.getValue()).put(gen.getKey(), new THashMap<>(sch.fksFrom(gen.getValue()).size(), 2));
      atts0x.get(gen.getValue()).put(gen.getKey(), new THashMap<>(sch.attsFrom(gen.getValue()).size(), 2));
    }

    for (Pair<Term<String, String, Sym, Fk, Att, String, String>, Term<String, String, Sym, Fk, Att, String, String>> e : eqs0) {
      Term<String, String, Sym, Fk, Att, String, String> lhs = e.first;
      Term<String, String, Sym, Fk, Att, String, String> rhs = e.second;
      if (rhs.gen() != null && lhs.fk() != null && lhs.arg.gen() != null) {
        String en = col.gens().get(lhs.arg.gen());
        fks0x.get(en).get(lhs.arg.gen()).put(lhs.fk(), rhs.gen());
      } else if (lhs.gen() != null && rhs.fk() != null && rhs.arg.gen() != null) {
        String en = col.gens().get(rhs.arg.gen());
        fks0x.get(en).get(rhs.arg.gen()).put(rhs.fk(), lhs.gen());
      } else if (rhs.obj() != null && lhs.att() != null && lhs.arg.gen() != null) {
        String en = col.gens().get(lhs.arg.gen());
        atts0x.get(en).get(lhs.arg.gen()).put(lhs.att(), Term.Obj(rhs.obj(), rhs.ty()));
      } else if (lhs.obj() != null && rhs.att() != null && rhs.arg.gen() != null) {
        String en = col.gens().get(rhs.arg.gen());
        atts0x.get(en).get(rhs.arg.gen()).put(rhs.att(), Term.Obj(lhs.obj(), lhs.ty()));
      } else if (rhs.sym() != null && rhs.args.isEmpty() && lhs.att() != null && lhs.arg.gen() != null) {
        String en = col.gens().get(lhs.arg.gen());
        atts0x.get(en).get(lhs.arg.gen()).put(lhs.att(), Term.Sym(rhs.sym(), Collections.emptyList()));
      } else if (lhs.sym() != null && lhs.args.isEmpty() && rhs.att() != null && rhs.arg.gen() != null) {
        String en = col.gens().get(rhs.arg.gen());
        atts0x.get(en).get(rhs.arg.gen()).put(rhs.att(), Term.Sym(lhs.sym(), Collections.emptyList()));
      } else {
        throw new RuntimeException("interpret_as_algebra not compatible with equation " + lhs + " = " + rhs
            + "; each equation must be of the form gen.fk=gen or gen.att=javaobject");
      }
    }

    for (Entry<String, String> gen : col.gens().entrySet()) {
      for (Att att : sch.attsFrom(gen.getValue())) {
        Map<Att, Term<String, Void, Sym, Void, Void, Void, Null<?>>> zz = atts0x.get(gen.getValue())
            .get(gen.getKey());
        if (!zz.containsKey(att)) {
          zz.put(att, objectToSk(sch, null, gen.getKey(), att, tys0, null, false, false));
        }
      }
    }

    ImportAlgebra alg = new ImportAlgebra<>(sch, x -> ens0.get(x), tys0, (en, x) -> fks0x.get(en).get(x), (en, x) -> atts0x.get(en).get(x),
        (x, y) -> y, (x, y) -> y, dont_check_closure, Collections.emptySet());
//Instance<String, String, Sym, Fk, Att, String, String, Integer, Chc<String, Pair<Integer, Att>>>
    Instance x = new SaturatedInstance<>(alg, alg, (Boolean) strat.getOrDefault(AqlOption.require_consistency),
        (Boolean) strat.getOrDefault(AqlOption.allow_java_eqs_unsafe), true, null);
    
    return x;
  }

  private static Term<String, Void, Sym, Void, Void, Void, Null<?>> objectToSk(Schema<String, String, Sym, Fk, Att> sch,
      Object rhs, String x, Att att, Map<String, Collection<Null<?>>> sks,
      Map<Null<?>, Term<String, String, Sym, Fk, Att, String, Null<?>>> extraRepr, boolean shouldJS, boolean errMeansNull) {
    String ty = sch.atts.get(att).second;
    if (rhs == null) {
      Null<?> n = new Null<>(Term.Att(att, Term.Gen(x)));
      // extraRepr.put(n, Term.Att(att, Term.Gen(x)));
      sks.get(ty).add(n);
      return Term.Sk(n);
    } else if (sch.typeSide.js.java_tys.containsKey(ty)) {
      if (shouldJS) {
        try {
          return Term.Obj(sch.typeSide.js.parse(ty, rhs.toString()), ty);
        } catch (Exception ex) {
          if (errMeansNull) {
            return objectToSk(sch, null, x, att, sks, extraRepr, shouldJS, errMeansNull);
          }
          ex.printStackTrace();
          throw new RuntimeException("On att " + att.en + "." + att.str + ", error while importing " + rhs
              + " of class " + rhs.getClass()
              + ".  Consider option import_null_on_err_unsafe.  Error was " + ex.getMessage());
        }
      }
      try {

        if (!Class.forName(sch.typeSide.js.java_tys.get(ty)).isInstance(rhs)) {
          if (errMeansNull) {
            return objectToSk(sch, null, x, att, sks, extraRepr, shouldJS, errMeansNull);
          }
          throw new RuntimeException("On " + x + "." + att + ", error while importing " + rhs + " of "
              + rhs.getClass() + " was expecting " + sch.typeSide.js.java_tys.get(ty) + " at type " + ty
              + ".\n\nConsider option " + AqlOption.import_null_on_err_unsafe);
        }

      } catch (ClassNotFoundException ex) {
        Util.anomaly();
      }
      return Term.Obj(rhs, ty);
    }
    return Util.anomaly();
  }

  @Override
  protected void allowedOptions(Set<AqlOption> set) {
    set.add(AqlOption.interpret_as_algebra);
    set.add(AqlOption.import_dont_check_closure_unsafe);
    set.add(AqlOption.diverge_limit);
    set.add(AqlOption.diverge_warn);
    set.add(AqlOption.require_consistency);
    set.add(AqlOption.allow_java_eqs_unsafe);
    set.addAll(AqlOptions.proverOptionNames());
  }

  @Override
  public SchExp type(AqlTyping G) {
    schema.type(G);
    for (Exp<?> z : imports()) {
      if (z.kind() != Kind.INSTANCE) {
        throw new RuntimeException("Import of wrong kind: " + z);
      }
    //  SchExp u = ((InstExp) z).type(G);

      // if (!schema.equals(u)) {
      // throw new RuntimeException("Import instance schema mismatch on " + z + ", is
      // " + u + " and not " + schema + " as expected.");
      // }
    }
    return schema;
  }

  @Override
  public Collection<InstExp<?, ?, ?, ?>> direct(AqlTyping G) {
    return Collections.emptySet();
  }

}
