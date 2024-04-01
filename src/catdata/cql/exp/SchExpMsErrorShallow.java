package catdata.cql.exp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Chc;
import catdata.Pair;
import catdata.Program;
import catdata.Util;
import catdata.cql.DP;
import catdata.cql.Kind;
import catdata.cql.Schema;
import catdata.cql.Term;
import catdata.cql.AqlOptions.AqlOption;
import gnu.trove.map.hash.THashMap;

public final class SchExpMsErrorShallow extends SchExp {

  String jdbcString;
  TyExp ty;
  String dom;

  @Override
  public Collection<Pair<String, Kind>> deps() {
    return ty.deps();
  }

  public <R, P, E extends Exception> R accept(P param, SchExpVisitor<R, P, E> v) throws E {
    return v.visit(param, this);
  }

  @Override
  public <R, P, E extends Exception> SchExp coaccept(P params, SchExpCoVisitor<R, P, E> v, R r) throws E {
    return v.visitSchExpMsError(params, r);
  }

  @Override
  public Map<String, String> options() {
    return Collections.emptyMap();
  }

  public SchExpMsErrorShallow(String dom, TyExp ty) {
    this.ty = ty;
    this.dom = dom;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((dom == null) ? 0 : dom.hashCode());
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
    SchExpMsErrorShallow other = (SchExpMsErrorShallow) obj;
    if (dom == null) {
      if (other.dom != null)
        return false;
    } else if (!dom.equals(other.dom))
      return false;
    if (ty == null) {
      if (other.ty != null)
        return false;
    } else if (!ty.equals(other.ty))
      return false;
    return true;
  }

  public Schema<String, String, Sym, Fk, Att> eval0_alt(AqlEnv env, boolean isC, String jdbcString, TyExp t) {
    var I = new InstExpMsError(dom, jdbcString, t).eval(env, isC);
    var ts = I.schema().typeSide;

    List<String> ens = new ArrayList<>(I.size());
    Map<Att, Pair<String, String>> atts = new THashMap<>(I.size() * 8, 2);

    DP<String, String, Sym, Fk, Att, Void, Void> dp = new DP<String, String, Sym, Fk, Att, Void, Void>() {
      @Override
      public String toStringProver() {
        return "SchExpMsErrorShallow";
      }

      @Override
      public boolean eq(Map<String, Chc<String, String>> ctx,
          Term<String, String, Sym, Fk, Att, Void, Void> lhs,
          Term<String, String, Sym, Fk, Att, Void, Void> rhs) {
        return lhs.equals(rhs);
      }
    };
    String ty = "CQL_RESERVED_CQL";
    List<String> lll = new LinkedList<>();
    StringBuffer sb = new StringBuffer();
    StringBuffer sb2 = new StringBuffer();
    List<String> jjj = new LinkedList<>();
    int i = 0;
    int j = 0;
    for (String en : I.schema().ens) {
      for (String x : I.algebra().en(en)) {
        String en2 = (x);
        ens.add(en2);
        sb.append(" ");
        sb.append(x);
        if (sb.length() > 32 * 1024) {
          lll.add("public static final String en" + (i++) + " = \"" + sb.toString() + "\";");
          sb = new StringBuffer();
        }

        Att att1 = Att.Att(en2, "row_id");
        Att att2 = Att.Att(en2, "level");
        Att att3 = Att.Att(en2, "cause_text");
        Att att4 = Att.Att(en2, "cause_json");

        atts.put(att1, new Pair<>(en2, ty));
        atts.put(att2, new Pair<>(en2, ty));
        atts.put(att3, new Pair<>(en2, ty));
        atts.put(att4, new Pair<>(en2, ty));

        sb2.append(
            " " + att1.str + " " + att2.str + " " + att3.str + " " + att4.str + " : " + x + "->" + ty);
        if (sb2.length() > 32 * 1024) {
          jjj.add("public static final String att" + (j++) + " = \"" + sb2.toString() + "\";");
          sb2 = new StringBuffer();
        }
      }
    }

    lll.add("public static final String en" + (i++) + " = \"" + sb.toString() + "\";");
    jjj.add("public static final String att" + (j++) + " = \"" + sb2.toString() + "\";");

    try {
      Util.writeFile(Util.sep(lll, "\n"), "ens.txt");
      Util.writeFile(Util.sep(jjj, "\n"), "atts.txt");

    } catch (Exception ex) {
      ex.printStackTrace();
    }

    return new Schema<String, String, Sym, Fk, Att>(ts, ens, atts, Collections.emptyMap(),
        Collections.emptySet(), dp, false);
  }

  @Override
  public Schema<String, String, Sym, Fk, Att> eval0(AqlEnv env, boolean isC) {
    StringBuffer sb1 = new StringBuffer();
    StringBuffer sb2 = new StringBuffer();
    try {
      for (int i = 0; i <= 4; i++) {
        String s = (String) MsSqlError.class.getField("en" + i).get(null);
        if (s == null) {
          Util.anomaly();
        }
        sb1.append("\n");
        sb1.append(s);
      }
      for (int i = 0; i <= 23; i++) {
        String s = (String) MsSqlError.class.getField("att" + i).get(null);
        if (s == null) {
          return Util.anomaly();
        }
        sb2.append("\n");
        sb2.append(s.replace("CQL_RESERVED_CQL", dom));
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      throw new RuntimeException(ex);
    }
    return CombinatorParser.parseSchExpRaw(
        "literal : " + ty + "{\nentities " + sb1.toString() + "\nattributes\n" + sb2.toString() + "\n}")
        .eval(env, isC);
  }

  @Override
  public String toString() {
    return "ms_error_shallow " + dom + " " + jdbcString + " : " + ty;
  }

  @Override
  public SchExp resolve(AqlTyping G, Program<Exp<?>> prog) {
    return this;
  }

  @Override
  public TyExp type(AqlTyping G) {
    ty.type(G);
    return ty;
  }

  @Override
  protected void allowedOptions(Set<AqlOption> set) {
  }

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {
    ty.mapSubExps(f);
  }
}