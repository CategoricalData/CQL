package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.collections4.list.TreeList;

import catdata.InteriorLabel;
import catdata.LocStr;
import catdata.Pair;
import catdata.Quad;
import catdata.Raw;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.ColimitSchema;
import catdata.aql.Kind;
import catdata.aql.Schema;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.exp.SchExp.SchExpVar;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public class ColimSchExpQuotient extends ColimSchExp implements Raw {

  @Override
  public <R, P, E extends Exception> R accept(P param, ColimSchExpVisitor<R, P, E> v) throws E {
    return v.visit(param, this);
  }

  @Override
  protected void allowedOptions(Set<AqlOption> set) {
    set.add(AqlOption.allow_java_eqs_unsafe);
    set.add(AqlOption.simplify_names);
    set.add(AqlOption.left_bias);
  }

  private Map<String, List<InteriorLabel<Object>>> raw = new THashMap<>(Collections.emptyMap());

  @Override
  public Map<String, List<InteriorLabel<Object>>> raw() {
    return raw;
  }

  public final TyExp ty;

  public final Map<String, SchExp> nodes;

  public final Set<Quad<String, String, String, String>> eqEn;

  public final Set<Quad<String, String, RawTerm, RawTerm>> eqTerms;

  public final Set<Pair<List<String>, List<String>>> eqTerms2;

  @Override
  public Map<String, String> options() {
    return options;
  }

  public Map<String, String> options;

  public ColimSchExpQuotient(TyExp ty, List<LocStr> nodes,
      List<Pair<Integer, Quad<String, String, String, String>>> eqEn,
      List<Pair<Integer, Quad<String, String, RawTerm, RawTerm>>> eqTerms,
      List<Pair<Integer, Pair<List<String>, List<String>>>> eqTerms2, List<Pair<String, String>> options) {
    this.ty = ty;
    this.nodes = new LinkedHashMap<>(nodes.size());
    this.eqEn = LocStr.proj2(eqEn).stream()
        .map(x -> new Quad<>(x.first, (x.second), x.third, (x.fourth)))
        .collect(Collectors.toSet());
    this.eqTerms = LocStr.proj2(eqTerms);
    this.eqTerms2 = LocStr.proj2(eqTerms2);
    this.options = Util.toMapSafely(options);
    for (LocStr n : nodes) {
      if (this.nodes.containsKey(n.str)) {
        throw new RuntimeException("In schema colimit " + this + " duplicate schema " + n
            + " - please create new schema variable if necessary.");
      }
      this.nodes.put(n.str, new SchExpVar(n.str));
    }

    List<InteriorLabel<Object>> f = new TreeList<>();
    for (Pair<Integer, Quad<String, String, String, String>> p : eqEn) {
      f.add(new InteriorLabel<>("entities", p.second, p.first,
          x -> x.first + "." + x.second + " = " + x.third + "." + x.fourth).conv());
    }
    raw.put("entities", f);

    f = new TreeList<>();
    for (Pair<Integer, Quad<String, String, RawTerm, RawTerm>> p : eqTerms) {
      f.add(new InteriorLabel<>("path eqs", p.second, p.first, x -> x.third + " = " + x.fourth).conv());
    }
    raw.put("path eqs", f);

    f = new TreeList<>();
    for (Pair<Integer, Pair<List<String>, List<String>>> p : eqTerms2) {
      f.add(new InteriorLabel<>("obs eqs", p.second, p.first,
          x -> Util.sep(x.first, ".") + " = " + Util.sep(x.second, ".")).conv());
    }
    raw.put("obs eqs", f);
  }

  @Override
  public synchronized ColimitSchema<String> eval0(AqlEnv env, boolean isC) {
    Map<String, Schema<String, String, Sym, Fk, Att>> nodes0 = new THashMap<>();
    Set<String> ens = new THashSet<>(nodes.size());
    for (String n : nodes.keySet()) {
      SchExp w = nodes.get(n);
      Schema<String, String, Sym, Fk, Att> z = w.eval(env, isC);
      nodes0.put(n, z);
      ens.addAll(nodes0.get(n).ens.stream().map(x -> (n + "_" + x)).collect(Collectors.toSet()));
    }

    return new ColimitSchema<>(nodes.keySet(), ty.eval(env, isC), nodes0, eqEn, eqTerms, eqTerms2,
        new AqlOptions(options, env.defaults));
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();

    if (nodes.keySet().isEmpty() & eqEn.isEmpty() && eqTerms.isEmpty() && eqTerms2.isEmpty()) {
      return sb.append("coproduct : ").append(this.ty).toString();
    }

    if (eqEn.isEmpty() && eqTerms.isEmpty() && eqTerms2.isEmpty()) {
      return sb.append("coproduct ").append(Util.sep(nodes.keySet(), " + ")).append(" : ").append(this.ty)
          .toString();
    }
    sb.append("quotient ").append(Util.sep(nodes.keySet(), " + ")).append(" : ").append(this.ty).append(" ")
        .append(" {\n");

    if (!eqEn.isEmpty()) {
      sb.append("\tentity_equations")
          .append(this.eqEn.stream().map(x -> x.first + "." + x.second + " = " + x.third + "." + x.fourth)
              .collect(Collectors.joining("\n\t\t", "\n\t\t", "\n")));
    }

    if (!eqTerms2.isEmpty()) {
      sb.append("\tpath_equations")
          .append(this.eqTerms2.stream()
              .map(x -> Util.sep(x.first, ".") + " = " + Util.sep(x.second, "."))
              .collect(Collectors.joining("\n\t\t", "\n\t\t", "\n")));
    }

    if (!eqTerms.isEmpty()) {
      sb.append("\tobservation_equations")
          .append(this.eqTerms.stream().map(x -> "forall " + x.first + ". " + x.third + " = " + x.fourth)
              .collect(Collectors.joining("\n\t\t", "\n\t\t", "\n")));
    }

    if (!options.isEmpty()) {
      sb.append("\toptions")
          .append(this.options.entrySet().stream().map(sym -> sym.getKey() + " = " + sym.getValue())
              .collect(Collectors.joining("\n\t\t", "\n\t\t", "\n")));
    }
    return sb.append("\n}").toString();
  }

  @Override
  public Collection<Pair<String, Kind>> deps() {
    Set<Pair<String, Kind>> ret = new THashSet<>();
    ret.addAll(ty.deps());
    for (SchExp v : nodes.values()) {
      ret.addAll(v.deps());
    }
    return ret;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((eqEn == null) ? 0 : eqEn.hashCode());
    result = prime * result + ((eqTerms == null) ? 0 : eqTerms.hashCode());
    result = prime * result + ((eqTerms2 == null) ? 0 : eqTerms2.hashCode());
    result = prime * result + ((nodes == null) ? 0 : nodes.hashCode());
    result = prime * result + ((options == null) ? 0 : options.hashCode());
    result = prime * result + ((ty == null) ? 0 : ty.hashCode());
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
    ColimSchExpQuotient other = (ColimSchExpQuotient) obj;
    if (eqEn == null) {
      if (other.eqEn != null)
        return false;
    } else if (!eqEn.equals(other.eqEn))
      return false;
    if (eqTerms == null) {
      if (other.eqTerms != null)
        return false;
    } else if (!eqTerms.equals(other.eqTerms))
      return false;
    if (eqTerms2 == null) {
      if (other.eqTerms2 != null)
        return false;
    } else if (!eqTerms2.equals(other.eqTerms2))
      return false;
    if (nodes == null) {
      if (other.nodes != null)
        return false;
    } else if (!nodes.equals(other.nodes))
      return false;
    if (options == null) {
      if (other.options != null)
        return false;
    } else if (!options.equals(other.options))
      return false;
    if (ty == null) {
      if (other.ty != null)
        return false;
    } else if (!ty.equals(other.ty))
      return false;
    return true;
  }

  @Override
  public SchExp getNode(String n, AqlTyping G) {
    return nodes.get(n);
  }

  @Override
  public Set<String> type(AqlTyping G) {
    ty.type(G);
    for (String n : nodes.keySet()) {
      TyExp w = nodes.get(n).type(G);
      if (!w.equals(ty)) {
        throw new RuntimeException("Schema for " + n + " is on typeside " + w + " and not on " + ty);
      }
    }
    return nodes.keySet();
  }

  @Override
  public Set<Pair<SchExp, SchExp>> gotos(ColimSchExp ths) {
    Set<Pair<SchExp, SchExp>> ret = new THashSet<>(nodes.size());
    SchExp t = new SchExpColim(ths);
    for (SchExp s : nodes.values()) {
      ret.add(new Pair<>(s, t));
    }
    return ret;
  }

  @Override
  public TyExp typeOf(AqlTyping G) {
    ty.type(G);
    for (String n : nodes.keySet()) {
      TyExp w = nodes.get(n).type(G);
      if (!w.equals(ty)) {
        throw new RuntimeException("Schema for " + n + " is on typeside " + w + " and not on " + ty);
      }
    }
    return ty;
  }

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {
    ty.map(f);
    for (SchExp x : nodes.values()) {
      x.map(f);
    }
  }

}