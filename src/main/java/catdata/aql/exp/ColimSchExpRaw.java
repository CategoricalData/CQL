package catdata.aql.exp;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.collections4.list.TreeList;

import catdata.InteriorLabel;
import catdata.LocStr;
import catdata.Pair;
import catdata.Raw;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.ColimitSchema;
import catdata.aql.Kind;
import catdata.aql.Mapping;
import catdata.aql.Schema;
import catdata.aql.AqlOptions.AqlOption;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public class ColimSchExpRaw extends ColimSchExp implements Raw {

  public <R, P, E extends Exception> R accept(P param, ColimSchExpVisitor<R, P, E> v) throws E {
    return v.visit(param, this);
  }

  @Override
  public Set<Pair<SchExp, SchExp>> gotos(ColimSchExp ths) {
    Set<Pair<SchExp, SchExp>> ret = new THashSet<>();
    SchExp t = new SchExpColim(ths);
    for (SchExp s : nodes.values()) {
      ret.add(new Pair<>(s, t));
    }
    return ret;
  }

  private Map<String, List<InteriorLabel<Object>>> raw = new THashMap<>();

  @Override
  public Map<String, List<InteriorLabel<Object>>> raw() {
    return raw;
  }

  public final GraphExp shape;

  public final TyExp ty;

  public final Map<String, SchExp> nodes;

  public final Map<String, MapExp> edges;

  public final Map<String, String> options;

  @Override
  public Map<String, String> options() {
    return options;
  }

  @Override
  public SchExp getNode(String n, AqlTyping G) {
    return nodes.get(n);
  }

  public ColimSchExpRaw(GraphExp shape, TyExp ty, List<Pair<LocStr, SchExp>> nodes,
      List<Pair<LocStr, MapExp>> edges, List<Pair<String, String>> options) {
    this.shape = shape;
    this.ty = ty;
    this.nodes = new LinkedHashMap<>();
    for (Pair<String, SchExp> xx : LocStr.list2(nodes, x -> x)) {
      if (this.nodes.containsKey(xx.first)) {
        throw new RuntimeException("Duplicate node: " + xx.first);
      }
      this.nodes.put(xx.first, xx.second);
    }
    this.edges = Util.toMapSafely(LocStr.list2(edges, x -> x));
    this.options = Util.toMapSafely(options);

    List<InteriorLabel<Object>> f = new TreeList<>();
    for (Pair<LocStr, SchExp> p : nodes) {
      f.add(new InteriorLabel<>("nodes", new Pair<>(p.first.str, p.second), p.first.loc,
          x -> x.first + " -> " + x.second).conv());
    }
    raw.put("nodes", f);

    f = new TreeList<>();
    for (Pair<LocStr, MapExp> p : edges) {
      f.add(new InteriorLabel<>("edges", new Pair<>(p.first.str, p.second), p.first.loc,
          x -> x.first + " -> " + x.second).conv());
    }
    raw.put("edges", f);

  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("literal " + shape + " : " + ty + " {");
    if (!nodes.isEmpty()) {
      sb.append("\n\tnodes\n\t\t");
      sb.append(Util.sep(nodes, " -> ", "\n\t\t"));
    }
    if (!edges.isEmpty()) {
      sb.append("\n\tedges\n\t\t");
      sb.append(Util.sep(edges, " -> ", "\n\t\t"));
    }
    sb.append("}");
    return sb.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((edges == null) ? 0 : edges.hashCode());
    result = prime * result + ((nodes == null) ? 0 : nodes.hashCode());
    result = prime * result + ((options == null) ? 0 : options.hashCode());
    result = prime * result + ((shape == null) ? 0 : shape.hashCode());
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
    ColimSchExpRaw other = (ColimSchExpRaw) obj;
    if (edges == null) {
      if (other.edges != null)
        return false;
    } else if (!edges.equals(other.edges))
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
    if (shape == null) {
      if (other.shape != null)
        return false;
    } else if (!shape.equals(other.shape))
      return false;
    if (ty == null) {
      if (other.ty != null)
        return false;
    } else if (!ty.equals(other.ty))
      return false;
    return true;
  }

  @Override
  public Collection<Pair<String, Kind>> deps() {
    Collection<Pair<String, Kind>> ret = new THashSet<>();
    for (SchExp k : nodes.values()) {
      ret.addAll(k.deps());
    }
    for (MapExp k : edges.values()) {
      ret.addAll(k.deps());
    }
    ret.addAll(shape.deps());
    ret.addAll(ty.deps());
    return ret;
  }

  @Override
  public synchronized ColimitSchema<String> eval0(AqlEnv env, boolean isC) {
    Map<String, Schema<String, String, Sym, Fk, Att>> nodes0 = new THashMap<>(nodes.size());
    for (String n : nodes.keySet()) {
      nodes0.put(n, nodes.get(n).eval(env, isC));
    }
    Map<String, Mapping<String, String, Sym, Fk, Att, String, Fk, Att>> edges0 = new THashMap<>(edges.size());
    for (String e : edges.keySet()) {
      edges0.put(e, edges.get(e).eval(env, isC));
    }
    return new ColimitSchema<>(nodes.keySet(), shape.eval(env, isC).dmg, ty.eval(env, isC), nodes0, edges0,
        new AqlOptions(options, env.defaults));
  }

  @Override
  public Set<String> type(AqlTyping G) {
    ty.type(G);
    return nodes.keySet();
  }

  @Override
  protected void allowedOptions(Set<AqlOption> set) {
    set.add(AqlOption.allow_java_eqs_unsafe);
    set.add(AqlOption.simplify_names);
    set.add(AqlOption.left_bias);
  }

  @Override
  public TyExp typeOf(AqlTyping G) {
    ty.type(G);
    return ty;
  }

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {
    ty.map(f);
    shape.map(f);
    for (SchExp k : nodes.values()) {
      k.map(f);
    }
    for (MapExp k : edges.values()) {
      k.map(f);
    }
  }
}