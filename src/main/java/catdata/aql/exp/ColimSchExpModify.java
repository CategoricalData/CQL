package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.collections4.list.TreeList;

import catdata.Chc;
import catdata.InteriorLabel;
import catdata.LocStr;
import catdata.Pair;
import catdata.Raw;
import catdata.Triple;
import catdata.Util;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.ColimitSchema;
import catdata.aql.Collage;
import catdata.aql.Kind;
import catdata.aql.Term;

import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public final class ColimSchExpModify extends ColimSchExp implements Raw {

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {
    colim.map(f);
  }

  private Map<String, List<InteriorLabel<Object>>> raw = new THashMap<>();

  @Override
  public <R, P, E extends Exception> R accept(P param, ColimSchExpVisitor<R, P, E> v) throws E {
    return v.visit(param, this);
  }

  @Override
  public Set<Pair<SchExp, SchExp>> gotos(ColimSchExp ths) {
    Set<Pair<SchExp, SchExp>> ret = new THashSet<>();
    SchExp t = new SchExpColim(ths);
    SchExp s = new SchExpColim(colim);
    ret.add(new Pair<>(s, t));
    return ret;
  }

  @Override
  public Map<String, List<InteriorLabel<Object>>> raw() {
    return raw;
  }

  @Override
  public SchExp getNode(String n, AqlTyping G) {
    return colim.getNode(n, G);
  }

  public final ColimSchExp colim;

  public final List<Pair<String, String>> ens;

  public final List<Pair<Pair<String, String>, String>> fks0;

  public final List<Pair<Pair<String, String>, String>> atts0;
  public final List<Pair<Pair<String, String>, List<String>>> fks;
  public final List<Pair<Pair<String, String>, Triple<String, String, RawTerm>>> atts;

  public final Map<String, String> options;

  @Override
  public Map<String, String> options() {
    return options;
  }

  public ColimSchExpModify(ColimSchExp colim, List<Pair<LocStr, String>> ens,
      List<Pair<Pair<String, LocStr>, String>> fks0, List<Pair<Pair<String, LocStr>, String>> atts0,
      List<Pair<Pair<String, LocStr>, List<String>>> fks,
      List<Pair<Pair<String, LocStr>, Triple<String, String, RawTerm>>> atts,
      List<Pair<String, String>> options) {
    this.ens = LocStr.list2(ens);
    this.atts = LocStr.list2x(atts);
    this.fks = LocStr.list2x(fks);
    this.fks0 = LocStr.list2x(fks0);

    this.atts0 = LocStr.list2x(atts0);
    this.options = Util.toMapSafely(options);
    Util.toMapSafely(this.ens);
    Util.toMapSafely(this.fks);
    Util.toMapSafely(this.atts); // do here rather than wait
    this.colim = colim;

    List<InteriorLabel<Object>> f = new TreeList<>();
    for (Pair<LocStr, String> p : ens) {
      f.add(new InteriorLabel<>("rename_entities", new Pair<>(p.first.str, p.second), p.first.loc,
          x -> x.first + " -> " + x.second).conv());
    }
    raw.put("rename_entities", f);

    f = new TreeList<>();
    for (Pair<Pair<String, LocStr>, String> p : fks0) {
      f.add(new InteriorLabel<>("rename_fks", new Pair<>(p.first.second.str, p.second), p.first.second.loc,
          x -> x.first + " -> " + x.second).conv());
    }
    raw.put("rename_fks", f);

    f = new TreeList<>();
    for (Pair<Pair<String, LocStr>, String> p : atts0) {
      f.add(new InteriorLabel<>("rename_atts", new Pair<>(p.first.second.str, p.second), p.first.second.loc,
          x -> x.first + " -> " + x.second).conv());
    }
    raw.put("rename_atts", f);

    f = new TreeList<>();
    for (Pair<Pair<String, LocStr>, List<String>> p : fks) {
      f.add(new InteriorLabel<>("remove_fks", new Pair<>(p.first.second.str, p.second), p.first.second.loc,
          x -> x.first + " -> " + Util.sep(x.second, ".")).conv());
    }
    raw.put("remove_fks", f);

    f = new TreeList<>();
    for (Pair<Pair<String, LocStr>, Triple<String, String, RawTerm>> p : atts) {
      f.add(new InteriorLabel<>("remove_atts", new Pair<>(p.first.second.str, p.second), p.first.second.loc,
          x -> x.first + " -> \\" + x.second.first + ". " + x.second.third).conv());
    }
    raw.put("remove_atts", f);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((atts == null) ? 0 : atts.hashCode());
    result = prime * result + ((atts0 == null) ? 0 : atts0.hashCode());
    result = prime * result + ((colim == null) ? 0 : colim.hashCode());
    result = prime * result + ((ens == null) ? 0 : ens.hashCode());
    result = prime * result + ((fks == null) ? 0 : fks.hashCode());
    result = prime * result + ((fks0 == null) ? 0 : fks0.hashCode());
    result = prime * result + ((options == null) ? 0 : options.hashCode());
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
    ColimSchExpModify other = (ColimSchExpModify) obj;
    if (atts == null) {
      if (other.atts != null)
        return false;
    } else if (!atts.equals(other.atts))
      return false;
    if (atts0 == null) {
      if (other.atts0 != null)
        return false;
    } else if (!atts0.equals(other.atts0))
      return false;
    if (colim == null) {
      if (other.colim != null)
        return false;
    } else if (!colim.equals(other.colim))
      return false;
    if (ens == null) {
      if (other.ens != null)
        return false;
    } else if (!ens.equals(other.ens))
      return false;
    if (fks == null) {
      if (other.fks != null)
        return false;
    } else if (!fks.equals(other.fks))
      return false;
    if (fks0 == null) {
      if (other.fks0 != null)
        return false;
    } else if (!fks0.equals(other.fks0))
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
    final StringBuilder sb = new StringBuilder();
    sb.append("modify ").append(colim).append(" {\n");

    if (!ens.isEmpty()) {
      sb.append("\trename entities");
      List<String> temp = new LinkedList<>();
      for (Pair<String, String> x : ens) {
        temp.add(x.first + " -> " + x.second);
      }

      sb.append("\n\t\t").append(Util.sep(temp, "\n\t\t")).append("\n");
    }

    if (!fks0.isEmpty()) {
      sb.append("\trename foreign_keys");
      List<String> temp = new LinkedList<>();
      for (Pair<Pair<String, String>, String> x : fks0) {
        temp.add(x.first.first + "." + x.first.second + " -> " + x.second);
      }

      sb.append("\n\t\t").append(Util.sep(temp, "\n\t\t")).append("\n");
    }

    if (!atts0.isEmpty()) {
      sb.append("\trename attributes");
      List<String> temp = new LinkedList<>();
      for (Pair<Pair<String, String>, String> x : atts0) {
        temp.add(x.first.first + "." + x.first.second + " -> " + x.second);
      }

      sb.append("\n\t\t").append(Util.sep(temp, "\n\t\t")).append("\n");
    }

    if (!fks.isEmpty()) {
      List<String> temp = new LinkedList<>();
      sb.append("\tremove foreign_keys");
      for (Pair<Pair<String, String>, List<String>> sym : fks) {
        temp.add(sym.first.first + "." + sym.first.second + " -> " + Util.sep(sym.second, "."));
      }
      sb.append("\n\t\t").append(Util.sep(temp, "\n\t\t")).append("\n");
    }

    if (!fks.isEmpty()) {
      List<String> temp = new LinkedList<>();
      sb.append("\tremove attributes");
      for (Pair<Pair<String, String>, Triple<String, String, RawTerm>> sym : atts) {
        temp.add(sym.first.second + " -> lambda " + sym.second.first + ". " + sym.second.third);
      }
      sb.append("\n\t\t").append(Util.sep(temp, "\n\t\t")).append("\n");
    }

    if (!options.isEmpty()) {
      sb.append("\toptions");
      List<String> temp = new LinkedList<>();
      for (Entry<String, String> sym : options.entrySet()) {
        temp.add(sym.getKey() + " = " + sym.getValue());
      }
      sb.append("\n\t\t").append(Util.sep(temp, "\n\t\t")).append("\n");
    }

    return sb.toString().trim() + "}";
  }

  @Override
  public synchronized ColimitSchema<String> eval0(AqlEnv env, boolean isC) {
    boolean checkJava = !(Boolean) env.defaults.getOrDefault(options, AqlOption.allow_java_eqs_unsafe);
    ColimitSchema<String> colim0 = colim.eval(env, isC);

    for (Pair<String, String> k : ens) {
      colim0 = colim0.renameEntity(k.first, k.second, checkJava);
    }
    for (Pair<Pair<String, String>, String> k : fks0) {
      colim0 = colim0.renameFk(Fk.Fk((k.first.first), k.first.second), Fk.Fk((k.first.first), k.second),
          checkJava);
    }
    for (Pair<Pair<String, String>, String> k : atts0) {
      colim0 = colim0.renameAtt(Att.Att((k.first.first), k.first.second),
          Att.Att((k.first.first), k.second), checkJava);
    }
    for (Pair<Pair<String, String>, List<String>> k : fks) {
      if (!colim0.schemaStr.fks.containsKey(Fk.Fk((k.first.first), k.first.second))) {
        throw new RuntimeException("Not an fk: " + k.first + " in\n\n" + colim0.schemaStr);
      }
      String pre = "In processing " + k.first + " -> " + k.second + ", ";
      Collage<String, String, Sym, Fk, Att, Void, Void> xxx = colim0.schemaStr.collage();
      RawTerm term = RawTerm.fold(k.second, "v");
      String tr = colim0.schemaStr.fks.get(Fk.Fk((k.first.first), k.first.second)).second;

      Map<String, Chc<String, String>> Map = Collections.singletonMap("v", Chc.inRight((k.first.first)));
      Term<String, String, Sym, Fk, Att, String, String> t = RawTerm.infer1x(Map, term, null, Chc.inRight(tr), xxx.convert(),
          pre, colim0.schemaStr.typeSide.js).second;

      colim0 = colim0.removeFk(Fk.Fk((k.first.first), k.first.second), t.toFkList(), checkJava);
    }
    for (Pair<Pair<String, String>, Triple<String, String, RawTerm>> k : atts) {
      if (!colim0.schemaStr.atts.containsKey(Att.Att((k.first.first), k.first.second))) {
        throw new RuntimeException("Not an attribute: " + k.first + " in\n\n" + colim0.schemaStr);
      }
      String pre = "In processing " + k.first + " -> lambda " + k.second.first + "." + k.second.third + ", ";
      Pair<String, String> r = colim0.schemaStr.atts.get(Att.Att((k.first.first), k.first.second));
      if (k.second.second != null && !k.second.second.equals(r.first)) {
        throw new RuntimeException(pre + " given type is " + k.second.second + " but expected " + r.first);
      }
      Collage<String, String, Sym, Fk, Att, Void, Void> xxx = colim0.schemaStr.collage();
      Map<String, Chc<String, String>> Map = Collections.singletonMap(k.second.first, Chc.inRight(r.first));
      Term<String, String, Sym, Fk, Att, String, String> t = RawTerm.infer1x(Map, k.second.third, null, Chc.inLeft(r.second),
          xxx.convert(), pre, colim0.schemaStr.typeSide.js).second;
      colim0 = colim0.removeAtt(Att.Att((k.first.first), k.first.second), (k.second.first),
          t.convert(), checkJava);
    }

    return colim0;
  }

  @Override
  public Collection<Pair<String, Kind>> deps() {
    return colim.deps();
  }

  @Override
  public Set<String> type(AqlTyping G) {
    return colim.type(G);
  }

  @Override
  public TyExp typeOf(AqlTyping G) {
    return colim.typeOf(G);
  }

  @Override
  protected void allowedOptions(Set<AqlOption> set) {
    // shouldn't change allowed java
  }

}
