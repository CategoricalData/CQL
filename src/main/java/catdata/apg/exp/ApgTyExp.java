package catdata.apg.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import catdata.InteriorLabel;
import catdata.LocStr;
import catdata.Pair;
import catdata.Raw;
import catdata.Triple;
import catdata.Unit;
import catdata.Util;
import catdata.apg.ApgTypeside;
import catdata.aql.AqlJs;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Kind;
import catdata.aql.exp.AqlEnv;
import catdata.aql.exp.AqlTyping;
import catdata.aql.exp.Exp;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public abstract class ApgTyExp extends Exp<ApgTypeside> {

  public static interface ApgTyExpCoVisitor<R, P, E extends Exception> {
    // public abstract TyExpSch visitTyExpSch(P params, R r) throws E;

    public abstract ApgTyExpVar visitApgTyExpVar(P params, R r) throws E;

    public abstract ApgTyExpRaw visitApgTyExpRaw(P params, R r) throws E;

    // public abstract TyExpSql visitTyExpSql(P params, R r) throws E;
  }

  public abstract <R, P, E extends Exception> ApgTyExp coaccept(P params, ApgTyExpCoVisitor<R, P, E> v, R r) throws E;

  public static interface ApgTyExpVisitor<R, P, E extends Exception> {
    // public abstract R visit(P params, TyExpSch exp) throws E;

    public abstract R visit(P params, ApgTyExpVar exp) throws E;

    public abstract R visit(P params, ApgTyExpRaw exp) throws E;

    // public abstract R visit(P params, TyExpSql exp) throws E;
  }

  public abstract <R, P, E extends Exception> R accept(P params, ApgTyExpVisitor<R, P, E> v) throws E;

  @Override
  public Unit type(AqlTyping G) {
    return Unit.unit;
  }

  @Override
  protected Map<String, String> options() {
    return Collections.emptyMap();
  }

  @Override
  public Kind kind() {
    return Kind.APG_typeside;
  }

  @Override
  public Exp<ApgTypeside> Var(String v) {
    return new ApgTyExpVar(v);
  }

  @Override
  protected void allowedOptions(Set<AqlOption> set) {

  }

  public static final class ApgTyExpRaw extends ApgTyExp implements Raw {

    public ApgTyExpRaw(List<ApgTyExp> imports, List<Pair<LocStr, Pair<String, String>>> functions,
        List<Pair<LocStr, Triple<List<String>, String, String>>> udfs) {
      this.imports = Util.toSetSafely(imports);
      this.types = Util.toMapSafely(LocStr.set2(functions));
      this.udfs = Util.toMapSafely(LocStr.set2(udfs));
      doGuiIndex(functions);
    }

    public void doGuiIndex(List<Pair<LocStr, Pair<String, String>>> functions) {
      List<InteriorLabel<Object>> f = new LinkedList<>();
      for (Pair<LocStr, Pair<String, String>> p : functions) {
        f.add(new InteriorLabel<>("types", new Pair<>(p.first.str, p.second), p.first.loc,
            x -> x.first + " " + x.second).conv());
      }
      raw.put("types", f);
    }

    @Override
    public String toString() {
      return "literal {" + (imports.isEmpty() ? "" : ("\n" + Util.sep(imports, " "))) + "\ntypes"
          + Util.sep(types, " -> ", "\n\t", x -> x.first + " " + x.second)
          + Util.sep(udfs, " : ", "\n\t", x -> Util.sep(x.first, ",") + " -> " + x.second + " = " + x.third)
          + "\n}";
    }

    public final Set<ApgTyExp> imports;
    public final Map<String, Pair<String, String>> types;

    public final Map<String, Triple<List<String>, String, String>> udfs;

    private Map<String, List<InteriorLabel<Object>>> raw = new THashMap<>();

    @Override
    public Map<String, List<InteriorLabel<Object>>> raw() {
      return raw;
    }

    @Override
    public Collection<Pair<String, Kind>> deps() {
      Set<Pair<String, Kind>> ret = new THashSet<>();
      for (ApgTyExp x : imports) {
        ret.addAll(x.deps());
      }
      return ret;
    }

    @Override
    public <R, P, E extends Exception> R accept(P params, ApgTyExpVisitor<R, P, E> v) throws E {
      return v.visit(params, this);
    }

    @Override
    public <R, P, E extends Exception> ApgTyExpRaw coaccept(P params, ApgTyExpCoVisitor<R, P, E> v, R r) throws E {
      return v.visitApgTyExpRaw(params, r);
    }

    @Override
    public void mapSubExps(Consumer<Exp<?>> f) {
    }

    @Override
    protected ApgTypeside eval0(AqlEnv env, boolean isCompileTime) {
      try {
        Map<String, String> tys = Util.map(types, (k, v) -> new Pair<>(k, v.first));
        Map<String, String> fns = Util.map(types, (k, v) -> new Pair<>(k, v.second));
        AqlJs<String, String> js = new AqlJs<>(env.defaults, Collections.emptyMap(), tys, fns,
            Collections.emptyMap());

        Map<String, Pair<Class<?>, Function<String, Object>>> types0 = new THashMap<>();
        for (ApgTyExp w : imports) {
          ApgTypeside x = w.eval(env, isCompileTime);
          Util.putAllSafely(types0, x.Bs);
        }

        for (Entry<String, Pair<String, String>> x : types.entrySet()) {
          types0.put(x.getKey(), new Pair<>(Class.forName(x.getValue().first), z -> js.parse(x.getKey(), z)));
        }

        Map<String, Triple<List<String>, String, Function<List<Object>, Object>>> udfs0 = new THashMap<>();
        for (Entry<String, Triple<List<String>, String, String>> e : udfs.entrySet()) {
          String name = e.getKey();
          List<String> domain = e.getValue().first;
          String codomain = e.getValue().second;
          String source = e.getValue().third;
          Function<List<Object>, Object> func = args -> js.invoke(codomain, source, args.toArray());
          udfs0.put(name, new Triple<>(domain, codomain, func));
        }

        return new ApgTypeside(types0, udfs0);
      } catch (ClassNotFoundException e) {
        throw new RuntimeException("Class not found: " + e.getMessage());
      }
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((imports == null) ? 0 : imports.hashCode());
      result = prime * result + ((types == null) ? 0 : types.hashCode());
      result = prime * result + ((udfs == null) ? 0 : udfs.hashCode());
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
      ApgTyExpRaw other = (ApgTyExpRaw) obj;
      if (imports == null) {
        if (other.imports != null)
          return false;
      } else if (!imports.equals(other.imports))
        return false;
      if (types == null) {
        if (other.types != null)
          return false;
      } else if (!types.equals(other.types))
        return false;
      if (udfs == null) {
        if (other.udfs != null)
          return false;
      } else if (!udfs.equals(other.udfs))
        return false;
      return true;
    }

  }

  public static final class ApgTyExpVar extends ApgTyExp {
    public final String var;

    @Override
    public <R, P, E extends Exception> R accept(P params, ApgTyExpVisitor<R, P, E> v) throws E {
      return v.visit(params, this);
    }

    @Override
    public <R, P, E extends Exception> ApgTyExpVar coaccept(P params, ApgTyExpCoVisitor<R, P, E> v, R r) throws E {
      return v.visitApgTyExpVar(params, r);
    }

    @Override
    public Collection<Pair<String, Kind>> deps() {
      return Collections.singletonList(new Pair<>(var, Kind.APG_typeside));
    }

    public ApgTyExpVar(String var) {
      this.var = var;
    }

    @Override
    public synchronized ApgTypeside eval0(AqlEnv env, boolean isC) {
      return env.defs.apgts.get(var);
    }

    public Unit type(AqlTyping t) {
      if (!t.defs.apgts.containsKey(var)) {
        throw new RuntimeException("Undefined APG typside variable: " + var);
      }
      return Unit.unit;
    }

    @Override
    public int hashCode() {
      return var.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      ApgTyExpVar other = (ApgTyExpVar) obj;
      return var.equals(other.var);
    }

    @Override
    public String toString() {
      return var;
    }

    @Override
    public void mapSubExps(Consumer<Exp<?>> f) {
    }

    @Override
    public boolean isVar() {
      return true;
    }

  }

}
