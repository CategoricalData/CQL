package catdata.aql.exp;

import com.github.rjeschke.txtmark.Processor;

import catdata.Program;
import catdata.Unit;
import catdata.apg.ApgInstance;
import catdata.apg.ApgMapping;
import catdata.apg.ApgSchema;
import catdata.apg.ApgTransform;
import catdata.apg.ApgTypeside;
import catdata.aql.ColimitSchema;
import catdata.aql.Comment;
import catdata.aql.Constraints;
import catdata.aql.Graph;
import catdata.aql.Instance;
import catdata.aql.Kind;
import catdata.aql.Mapping;
import catdata.aql.Mor;
import catdata.aql.Pragma;
import catdata.aql.Query;
import catdata.aql.Schema;
import catdata.aql.SemanticsVisitor;
import catdata.aql.Transform;
import catdata.aql.TypeSide;
import catdata.graph.DMG;

public final class AqlDoc implements SemanticsVisitor<String, Unit, RuntimeException> {

  private final AqlEnv env;

  public AqlDoc(AqlEnv env) {
    this.env = env;
  }

  public static String doc(AqlEnv env, Program<Exp<?>> prog) {
    if (prog == null || env == null) {
      throw new RuntimeException("Must compile before using HTML output");
    }
    StringBuffer sb = new StringBuffer();
    AqlDoc doc = new AqlDoc(env);
    for (String k : prog.order) {
      Exp<?> e = prog.exps.get(k);
      if (e.kind() != Kind.COMMENT) {
        sb.append("<pre>\n");
        sb.append(e.kind() + " " + k + " = " + e.toString());
        sb.append("\n</pre>");
      }
      if (!env.defs.keySet().contains(k)) {
        continue;
      }
      sb.append(doc.visit("", Unit.unit, env.get(e.kind(), k)));
      sb.append("\n");

    }
    sb.append("\n");
    return sb.toString();
  }

  @Override
  public <T, C> String visit(String k, Unit arg, TypeSide<T, C> T) {
    return "";
  }

  @SuppressWarnings("unchecked")
  @Override
  public <Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> String visit(String k, Unit arg,
      Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> I) {
    return "\n" + AqlInACan.toHtml(env,
        (Instance<String, String, catdata.aql.exp.Sym, catdata.aql.exp.Fk, catdata.aql.exp.Att, String, String, X, Y>) I);
  }

  @Override
  public <Ty, En, Sym, Fk, Att, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> String visit(String k, Unit arg,
      Transform<Ty, En, Sym, Fk, Att, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> h) {
    return "";
  }

  @Override
  public String visit(String k, Unit arg, Pragma P) {
    return "\n<pre>" + P.toString() + "</pre>";
  }

  @Override
  public String visit(String k, Unit arg, Comment C) {
    if (C.isMarkdown) {
      String result = Processor.process(C.comment);
      return result;
    }
    return C.toString();
  }

  @Override
  public <Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> String visit(String k, Unit arg,
      Query<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> Q) {
    return "";
  }

  @Override
  public <Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> String visit(String k, Unit arg,
      Mapping<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> M) {
    return "";
  }

  @Override
  public <N, e> String visit(String k, Unit arg, Graph<N, e> G0) {
    DMG<N, e> G = G0.dmg;
    String ret = "";
    ret += "\n<script>";
    ret += "\nvar graph" + fresh + " = new Springy.Graph();";

    for (N en : G.nodes) {
      ret += "\ngraph" + fresh + ".addNodes('" + en + "');";
    }
    for (e fk : G.edges.keySet()) {
      ret += "\ngraph" + fresh + ".addEdges(['" + G.edges.get(fk).first + "', '" + G.edges.get(fk).second
          + "', {label: '" + fk + "'}]);";
    }

    ret += "\njQuery(function(){ var springy = jQuery('#canvas" + fresh + "').springy({graph: graph" + fresh
        + "});});";
    ret += "\n</script>";
    ret += "\n<div><canvas id=\"canvas" + fresh + "\" width=\"640\" height=\"320\" /></div>";
    fresh++;
    return ret;
  }

  private int fresh = 0;

  @Override
  public <Ty, En, Sym, Fk, Att> String visit(String k, Unit arg, Schema<Ty, En, Sym, Fk, Att> S) {
    return "";
  }

  @Override
  public <N> String visit(String k, Unit arg, ColimitSchema<N> S) throws RuntimeException {
    return "";
  }

  @Override
  public String visit(String k, Unit arg, Constraints S) throws RuntimeException {
    return "";
  }

  @Override
  public <T, C, T0, C0> String visit(String k, Unit arg, Mor<T, C, T0, C0> M) throws RuntimeException {
    return "";
  }

  @Override
  public String visit(String k, Unit arg, ApgTypeside t) throws RuntimeException {
    // TODO Auto-generated method stub
    return "";
  }

  @Override
  public <L, e> String visit(String k, Unit arg, ApgInstance<L, e> t) throws RuntimeException {
    // TODO Auto-generated method stub
    return "";
  }

  @Override
  public <l1, e1, l2, e2> String visit(String k, Unit arg, ApgTransform<l1, e1, l2, e2> t) throws RuntimeException {
    // TODO Auto-generated method stub
    return "";
  }

  @Override
  public <L> String visit(String k, Unit arg, ApgSchema<L> t) throws RuntimeException {
    // TODO Auto-generated method stub
    return "";
  }

  @Override
  public <L1, L2> String visit(String k, Unit arg, ApgMapping<L1, L2> t) throws RuntimeException {
    // TODO Auto-generated method stub
    return "";
  }

}