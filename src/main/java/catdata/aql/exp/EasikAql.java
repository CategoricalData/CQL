package catdata.aql.exp;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import catdata.aql.Schema;
import catdata.aql.Term;

import catdata.aql.exp.EdsExpRaw.EdExpRaw;
import catdata.aql.exp.SchExp.SchExpVar;
import catdata.aql.exp.TyExp.TyExpVar;

public class EasikAql {

  private static String removePrefix(String en, String s) {
    if (s.startsWith(en + "_")) {
      return s.substring(s.indexOf("_") + 1, s.length());
    }
    return s;
  }

  public static String aqlToEasik(AqlEnv in, String title, Set<String> warnings) {
    String pre = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" + "\n<easketch_overview>"
        + "\n<header>" + "\n<title>" + title + "</title>" + "\n<author>Translated from CQL</author>"
        + "\n<description></description>" + "\n<creationDate></creationDate>"
        + "\n<lastModificationDate></lastModificationDate>" + "\n</header><sketches>";

    String post = "\n</sketches><views/>" + "\n</easketch_overview>";

    List<String> l = new LinkedList<>();
    int x0 = 0, y0 = 0;
    for (String s : in.defs.schs.keySet()) {
      @SuppressWarnings("unchecked")
      String x = aqlToEasik(s, in.defs.schs.get(s), x0, y0, 400, warnings);
      l.add(x);
      x0 += 100;
      if (x0 > 400) {
        x0 = 0;
        y0 += 100;
      }
    }
    if (!in.defs.eds.isEmpty()) {
      warnings.add("constraints not exported");
    }
    // TODO what of warnings?
    return pre + Util.sep(l, "\n") + post;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////
  @SuppressWarnings("hiding")
  private static <Ty, En, Sym, Fk, Att> String aqlToEasik(String name, Schema<Ty, En, Sym, Fk, Att> schema, int x,
      int y, int len, Set<String> warnings) {
    String pre = "\n<easketch cascade=\"cascade\" name=\"" + name + "\" partial-cascade=\"set_null\" x=\"" + x
        + "\" y=\"" + y + "\">" + "\n<header>" + "\n<title>" + name + "</title>" + "\n<description/>"
        + "\n<creationDate></creationDate>" + "\n<lastModificationDate></lastModificationDate>" + "\n</header>";

    String str = "";

    if (!schema.typeSide.eqs.isEmpty()) {
      warnings.add("typeside equations not exported.");
    }

    str += "\n<entities>";
    int x0 = 0, y0 = 0;
    for (En en : schema.ens) {
      str += "\n<entity name=\"" + en.toString() + "\" x=\"" + x0 + "\" y=\"" + y0 + "\">";
      for (Att att : schema.attsFrom(en)) {
        str += "\n<attribute attributeTypeClass=\"" + aqlTypeToString(schema, schema.atts.get(att).second)
            + "\" name=\"" + removePrefix(en.toString(), att.toString()) + "\" />";
      }
      str += "\n</entity>";

      x0 += 100;
      if (x0 > len) {
        x0 = 0;
        y0 += 100;
      }
    }
    str += "\n</entities>";

    str += "\n<edges>";
    for (Fk fk : schema.fks.keySet()) {
      str += "\n<edge cascade=\"cascade\" id=\"" + fk.toString() + "\" source=\"" + schema.fks.get(fk).first
          + "\" target=\"" + schema.fks.get(fk).second + "\" type=\"normal\"/>";
    }
    str += "\n</edges>";

    str += "\n<keys/>";

    str += "\n<constraints>";
    for (Triple<Pair<String, En>, Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>> eq : schema.eqs) {
      if (schema.type(eq.first, eq.second).left) {
        warnings.add("observation_equations not exported.");
        continue;
      }
      str += "\n<commutativediagram isVisible=\"true\" x=\"" + x0 + "\" y=\"" + y0 + "\">";
      str += aqlToEasik(schema, eq.first, eq.second);
      str += aqlToEasik(schema, eq.first, eq.third);
      str += "\n</commutativediagram>";
      x0 += 100;
      if (x0 > len) {
        x0 = 0;
        y0 += 100;
      }
    }

    str += "\n</constraints>";
    str += "\n</easketch>";
    return pre + str;
  }

  @SuppressWarnings("hiding")
  private static <Ty, En, Sym, Fk, Att> String aqlTypeToString(Schema<Ty, En, Sym, Fk, Att> schema, Ty t) {
    String s = schema.typeSide.js.java_tys.containsKey(t) ? schema.typeSide.js.java_tys.get(t) : "";
    return easikTypeFor(s);
  }

  @SuppressWarnings("hiding")
  private static <Ty, En, Sym, Fk, Att> String aqlToEasik(Schema<Ty, En, Sym, Fk, Att> schema, Pair<String, En> p,
      Term<Ty, En, Sym, Fk, Att, Void, Void> term) {
    String str = "\n<path codomain=\"" + schema.type(p, term).r + "\" domain=\"" + p.second + "\">";
    List<String> l = new LinkedList<>();
    while (term.fk() != null) {
      l.add("\n<edgeref id=\"" + term.fk() + "\"/>");
      term = term.arg;
    }
    for (String s : Util.reverse(l)) {
      str += s;
    }
    str += "\n</path>";
    return str;
  }

  private static String safe(String s) {
    return s.replace(" ", "_").replace("-", "_").replace(".", "_").replaceAll("/", "_");
  }

  private static Pair<SchExp, List<Pair<String, EdsExpRaw>>> translate1(Node sketch, Set<String> used,
      Set<String> warnings, String sname) {
    List<String> ens = new LinkedList<>();
    List<Pair<String, Pair<String, String>>> atts = new LinkedList<>();
    List<Pair<String, Pair<String, String>>> fks = new LinkedList<>();
    List<Pair<List<String>, List<String>>> eqs = new LinkedList<>();
    // there shouldn't be observation equations in easik
    // List<Quad<String, String, RawTerm, RawTerm>> eqs2 = new
    // LinkedList<>();
    List<Pair<String, EdsExpRaw>> edsExps = new LinkedList<>();

    NodeList l = sketch.getChildNodes();
    for (int temp = 0; temp < l.getLength(); temp++) {
      Node n = l.item(temp);
      NodeList j = n.getChildNodes();
      for (int temp2 = 0; temp2 < j.getLength(); temp2++) {
        Node m = j.item(temp2);

        if (m.getNodeName().equals("entity")) {
          String nodeName = safe(m.getAttributes().getNamedItem("name").getTextContent());
          ens.add(nodeName);
          NodeList k = m.getChildNodes();
          for (int temp3 = 0; temp3 < k.getLength(); temp3++) {
            Node w = k.item(temp3);
            if (w.getNodeName().equals("attribute")) {
              String attName = safe(w.getAttributes().getNamedItem("name").getTextContent());
              String tyName = w.getAttributes().getNamedItem("attributeTypeClass").getTextContent();
              used.add(tyName);
              atts.add(new Pair<>(nodeName + "_" + attName.replace(" ", "_"),
                  new Pair<>(nodeName, (easikTypeToString(tyName)))));
            }
          }
        } else if (m.getNodeName().equals("edge")) {
          String ename = safe(m.getAttributes().getNamedItem("id").getTextContent());
          String esrc = safe(m.getAttributes().getNamedItem("source").getTextContent());
          fks.add(new Pair<>(ename,
              new Pair<>(esrc, safe(m.getAttributes().getNamedItem("target").getTextContent()))));
          if (m.getAttributes().getNamedItem("type").getTextContent().equals("injective")) {
            List<EdExpRaw> eds = new LinkedList<>();
            List<Pair<String, String>> As = new LinkedList<>();
            As.add(new Pair<>("x", esrc));
            As.add(new Pair<>("y", esrc));
            List<Pair<RawTerm, RawTerm>> Aeqs = new LinkedList<>();
            Aeqs.add(new Pair<>(new RawTerm(ename, Collections.singletonList(new RawTerm("x"))),
                new RawTerm(ename, Collections.singletonList(new RawTerm("y")))));
            List<Pair<String, String>> Es = new LinkedList<>();
            List<Pair<RawTerm, RawTerm>> Eeqs = new LinkedList<>();
            Eeqs.add(new Pair<>(new RawTerm("x"), new RawTerm("y")));
            EdExpRaw ed = new EdExpRaw(As, Aeqs, Es, Eeqs, false, null);
            eds.add(ed);
            edsExps.add(new Pair<>("injective",
                new EdsExpRaw(new SchExpVar(sname), new LinkedList<>(), eds, null)));
          }
          if (m.getAttributes().getNamedItem("type").getTextContent().equals("partial")) {
            warnings.add("Not exported - partial edges.  Their CQL semantics is unclear");
          }
        } else if (m.getNodeName().equals("uniqueKey")) {
          String esrc = safe(m.getAttributes().getNamedItem("noderef").getTextContent());
          List<String> atts0 = new LinkedList<>();
          for (int w = 0; w < m.getChildNodes().getLength(); w++) {
            Node node = m.getChildNodes().item(w);
            if (!node.getNodeName().equals("attref")) {
              continue;
            }
            String att = safe(node.getAttributes().getNamedItem("name").getTextContent());
            atts0.add(att);
          }
          List<EdExpRaw> eds = new LinkedList<>();
          List<Pair<String, String>> As = new LinkedList<>();
          As.add(new Pair<>("x", esrc));
          As.add(new Pair<>("y", esrc));
          List<Pair<RawTerm, RawTerm>> Aeqs = new LinkedList<>();
          for (String att : atts0) {
            Aeqs.add(new Pair<>(new RawTerm(esrc + "_" + att, Collections.singletonList(new RawTerm("x"))),
                new RawTerm(esrc + "_" + att, Collections.singletonList(new RawTerm("y")))));
          }
          List<Pair<String, String>> Es = new LinkedList<>();
          List<Pair<RawTerm, RawTerm>> Eeqs = new LinkedList<>();
          Eeqs.add(new Pair<>(new RawTerm("x"), new RawTerm("y")));
          EdExpRaw ed = new EdExpRaw(As, Aeqs, Es, Eeqs, false, null);
          eds.add(ed);
          edsExps.add(new Pair<>("key", new EdsExpRaw(new SchExpVar(sname), new LinkedList<>(), eds, null)));
        }

        else if (m.getNodeName().equals("commutativediagram")) {
          NodeList k = m.getChildNodes();
          Node w1 = null;
          Node w2 = null;
          for (int temp4 = 0; temp4 < k.getLength(); temp4++) {
            Node wX = k.item(temp4);
            if (wX.getNodeName().equals("path") && w1 == null) {
              w1 = wX;
            } else if (wX.getNodeName().equals("path") && w2 == null) {
              w2 = wX;
            }
          }
          if (w1 == null || w2 == null) {
            throw new RuntimeException("Easik to CQL internal error");
          }
          String cod1 = safe(w1.getAttributes().getNamedItem("domain").getTextContent());
          String cod2 = safe(w2.getAttributes().getNamedItem("domain").getTextContent());
          List<String> lhs = new LinkedList<>();
          List<String> rhs = new LinkedList<>();
          lhs.add(cod1);
          rhs.add(cod2);

          NodeList lhsX = w1.getChildNodes();
          for (int temp3 = 0; temp3 < lhsX.getLength(); temp3++) {
            if (!lhsX.item(temp3).getNodeName().equals("edgeref")) {
              continue;
            }
            String toAdd = safe(lhsX.item(temp3).getAttributes().getNamedItem("id").getTextContent());
            lhs.add(toAdd);
          }
          NodeList rhsX = w2.getChildNodes();
          for (int temp3 = 0; temp3 < rhsX.getLength(); temp3++) {
            if (!rhsX.item(temp3).getNodeName().equals("edgeref")) {
              continue;
            }
            String toAdd = safe(rhsX.item(temp3).getAttributes().getNamedItem("id").getTextContent());
            rhs.add(toAdd);
          }
          eqs.add(new Pair<>(lhs, rhs));
        }
      }
    }
    SchExp schExp = new SchExpRaw(new TyExpVar("sql"), new LinkedList<>(), ens, fks, eqs, atts, new LinkedList<>(),
        new LinkedList<>(), null, null);

    return new Pair<>(schExp, edsExps);
  }

  private static Pair<List<String>, String> path(Node w1) {
    String dom = safe(w1.getAttributes().getNamedItem("domain").getTextContent());
    List<String> lhs = new LinkedList<>();
    lhs.add(dom);

    NodeList lhsX = w1.getChildNodes();
    for (int temp3 = 0; temp3 < lhsX.getLength(); temp3++) {
      if (!lhsX.item(temp3).getNodeName().equals("edgeref")) {
        continue;
      }
      String toAdd = safe(lhsX.item(temp3).getAttributes().getNamedItem("id").getTextContent());
      lhs.add(toAdd);
    }
    return new Pair<>(lhs, safe(w1.getAttributes().getNamedItem("codomain").getTextContent()));
  }

  private static RawTerm toTerm(List<String> l, String v) {
    RawTerm r = new RawTerm(v);
    for (String s : l) {
      r = new RawTerm(s, Collections.singletonList(r));
    }
    return r;
  }

  private static List<Pair<String, EdsExpRaw>> translateC(Node sketch, Set<String> warnings, SchExp schExp) {
    List<Pair<String, EdsExpRaw>> edsExps = new LinkedList<>();
    NodeList l = sketch.getChildNodes();
    for (int temp = 0; temp < l.getLength(); temp++) {
      Node n = l.item(temp);
      NodeList j = n.getChildNodes();
      for (int temp2 = 0; temp2 < j.getLength(); temp2++) {
        Node m = j.item(temp2);
        List<EdExpRaw> edExps = new LinkedList<>();
        String name = null;
        if (m.getNodeName().equals("sumconstraint")) {
          warnings.add("sum constraints not exported - CQL does not currently support sum constraints");
          continue;
        } else if (m.getNodeName().equals("limitconstraint")) {
          warnings.add("limit constraints not exported - if you see this, please report");
          continue;
        } else if (m.getNodeName().equals("pullbackconstraint")) {
          name = "pullback";
          Pair<List<String>, String> f1 = null, f2 = null, g1 = null, g2 = null;
          for (int i = 0; i < m.getChildNodes().getLength(); i++) {
            Node x = m.getChildNodes().item(i);
            if (x.getNodeName().equals("path")) {
              if (g1 == null) {
                g1 = path(x);
              } else if (g2 == null) {
                g2 = path(x);
              } else if (f1 == null) {
                f1 = path(x);
              } else if (f2 == null) {
                f2 = path(x);
              }
            }
          }
          if (f1 == null || g1 == null || f2 == null || g2 == null) {
            throw new RuntimeException("Anomaly - please report");
          }
          String A = f1.first.get(0);
          String B = f1.second;
          String C = g1.second;
          f1.first.remove(0);
          g1.first.remove(0);
          g2.first.remove(0);
          f2.first.remove(0);

          List<Pair<String, String>> as = new LinkedList<>();
          as.add(new Pair<>("b", B));
          as.add(new Pair<>("c", C));
          List<Pair<RawTerm, RawTerm>> a_eqs = new LinkedList<>();
          a_eqs.add(new Pair<>(toTerm(f2.first, "b"), toTerm(g2.first, "c")));

          List<Pair<String, String>> es = new LinkedList<>();
          es.add(new Pair<>("a", A));
          List<Pair<RawTerm, RawTerm>> e_eqs = new LinkedList<>();
          e_eqs.add(new Pair<>(toTerm(f1.first, "a"), new RawTerm("b")));
          e_eqs.add(new Pair<>(toTerm(g1.first, "a"), new RawTerm("c")));
          EdExpRaw ed1 = new EdExpRaw(as, a_eqs, es, e_eqs, true, null);
          edExps.add(ed1);
          /*
           * as = new LinkedList<>(); as.add(new Pair<>("a1", A)); as.add(new Pair<>("a2",
           * A)); a_eqs = new LinkedList<>(); a_eqs.add(new Pair<>(toTerm(f1.first,
           * "a1"),toTerm(f1.first, "a2"))); a_eqs.add(new Pair<>(toTerm(g1.first,
           * "a1"),toTerm(g1.first, "a2"))); es = new LinkedList<>(); e_eqs = new
           * LinkedList<>(); e_eqs.add(new Pair<>(new RawTerm("a1"),new RawTerm("a2")));
           * EdExpRaw ed2 = new EdExpRaw(as, a_eqs, es, e_eqs); edExps.add(ed2);
           */
        } else if (m.getNodeName().equals("productconstraint")) {
          name = "product";
          Pair<List<String>, String> f = null, g = null;
          for (int i = 0; i < m.getChildNodes().getLength(); i++) {
            Node x = m.getChildNodes().item(i);
            if (x.getNodeName().equals("path")) {
              if (f == null) {
                f = path(x);
              } else if (g == null) {
                g = path(x);
              }
            }
          }
          if (f == null || g == null) {
            throw new RuntimeException("Anomaly - please report");
          }
          String A = f.first.get(0);
          String B = f.second;
          String C = g.second;
          f.first.remove(0);
          g.first.remove(0);

          List<Pair<String, String>> as = new LinkedList<>();
          as.add(new Pair<>("b", B));
          as.add(new Pair<>("c", C));
          List<Pair<RawTerm, RawTerm>> a_eqs = new LinkedList<>();
          List<Pair<String, String>> es = new LinkedList<>();
          es.add(new Pair<>("a", A));
          List<Pair<RawTerm, RawTerm>> e_eqs = new LinkedList<>();
          e_eqs.add(new Pair<>(toTerm(f.first, "a"), new RawTerm("b")));
          e_eqs.add(new Pair<>(toTerm(g.first, "a"), new RawTerm("c")));
          EdExpRaw ed1 = new EdExpRaw(as, a_eqs, es, e_eqs, true, null);
          edExps.add(ed1);

        } else if (m.getNodeName().equals("equalizerconstraint")) {
          List<String> h = null, f = null, g = null;
          for (int i = 0; i < m.getChildNodes().getLength(); i++) {
            Node x = m.getChildNodes().item(i);
            if (x.getNodeName().equals("path")) {
              if (h == null) {
                h = path(x).first;
              } else if (f == null) {
                f = path(x).first;
              } else if (g == null) {
                g = path(x).first;
              }
            }
          }
          if (h == null || f == null || g == null) {
            throw new RuntimeException("Anomaly - please report");
          }
          String B = f.get(0);
          String A = h.get(0);
          h.remove(0);
          f.remove(0);
          g.remove(0);

          List<Pair<String, String>> as = new LinkedList<>();
          as.add(new Pair<>("b", B));
          List<Pair<RawTerm, RawTerm>> a_eqs = new LinkedList<>();
          a_eqs.add(new Pair<>(toTerm(f, "b"), toTerm(g, "b")));
          List<Pair<String, String>> es = new LinkedList<>();
          es.add(new Pair<>("a", A));
          List<Pair<RawTerm, RawTerm>> e_eqs = new LinkedList<>();
          e_eqs.add(new Pair<>(toTerm(h, "a"), new RawTerm("b")));
          EdExpRaw ed1 = new EdExpRaw(as, a_eqs, es, e_eqs, true, null);
          edExps.add(ed1);
          name = "equalizer";
        } else {
          continue;
        }

        EdsExpRaw edsExp = new EdsExpRaw(schExp, new LinkedList<>(), edExps, null);
        edsExps.add(new Pair<>(name, edsExp));
      }
    }
    return edsExps;
  }

  private static String easikTypeToString(String tyName) {
    switch (tyName) {
    case "easik.database.types.BigInt":
      return "BigInt"; // Long
    case "easik.database.types.Blob":
      return "Blob"; // byte[]
    case "easik.database.types.Boolean":
      return "Boolean"; // Boolean
    case "easik.database.types.Char":
      return "Char"; // Character
    case "easik.database.types.Date":
      return "Date"; // java.Sql.Date
    case "easik.database.types.Decimal":
      return "Decimal"; // java.math.BigDecimal
    case "easik.database.types.DoublePrecision":
      return "DoublePrecision"; // Double
    case "easik.database.types.Float":
      return "Float"; // Float
    case "easik.database.types.Int":
      return "Integer"; // Integer
    case "easik.database.types.SmallInt":
      return "SmallInt"; // Short
    case "easik.database.types.Text":
      return "Text"; // String
    case "easik.database.types.Time":
      return "Time"; // java.sql.time
    case "easik.database.types.TimeStamp":
      return "TimeStamp"; // java.sql.timestamp
    case "easik.database.types.Varchar":
      return "Varchar";
    case "easik.database.types.Custom":
      return "Custom"; // Object
    default:
      throw new RuntimeException("Unknown type: " + tyName);
    }
  }

  public static String easikTypeFor(String s) {
    switch (s) {
    case "java.lang.Long":
      return "easik.database.types.BigInt";
    case "java.lang.Boolean":
      return "easik.database.types.Boolean";
    case "java.lang.Character":
      return "easik.database.types.Char";
    case "java.lang.Double":
      return "easik.database.types.DoublePrecision";
    case "java.lang.Float":
      return "easik.database.types.Float";
    case "java.lang.Integer":
      return "easik.database.types.Int";
    case "java.lang.Short":
      return "easik.database.types.SmallInt";
    case "java.lang.String":
      return "easik.database.types.Varchar";
    default:
      return "easik.database.types.Varchar";
    }

  }
  /*
   * public static String javaClassFor(String s) { switch (s) { case "BigInt":
   * return "java.lang.Long"; case "Boolean": return "java.lang.Boolean"; case
   * "Char": return "java.lang.Character"; case "DoublePrecision": return
   * "java.lang.Double"; case "Float": return "java.lang.Float"; case "Int":
   * return "java.lang.Integer"; case "SmallInt": return "java.lang.Short"; case
   * "Text": return "java.lang.String"; case "Varchar": return "java.lang.String";
   * case "Custom": return "java.lang.Object"; // case "Blob" : return "Blob";
   * //byte[] // case "Date" : return "java.sql.Date"; // case "Decimal" : return
   * "java.math.BigDecimal"; // case "Time" : return "java.sql.time"; // case
   * "TimeStamp" : return "java.sql.TimeStamp"; default: return
   * "java.lang.Object"; }
   *
   * }
   *
   * public static String javaParserFor(String s) { switch (s) { case "BigInt":
   * return "return new java.lang.Long(input[0])"; case "Boolean": return
   * "return new java.lang.Boolean(input[0])"; case "Char": return
   * "return input[0].charAt(0)"; case "DoublePrecision": return
   * "return new java.lang.Double(input[0])"; case "Float": return
   * "return new java.lang.Float(input[0])"; // Float case "Int": return
   * "return new java.lang.Integer(input[0])"; // Integer case "SmallInt": return
   * "return new java.lang.Short(input[0])"; // Short case "Text": return
   * "return input[0]"; // String case "Varchar": return "return input[0]"; case
   * "Custom": return "return input[0]"; // Object // case "Blob" : return "Blob";
   * //byte[] // case "Date" : return "Date"; // case "Decimal" : return
   * "Decimal"; // case "Time" : return "Time"; //java.sql.time // case
   * "TimeStamp" : return "TimeStamp"; //java.sql.timestamp default: return
   * "return input[0]"; } }
   */
  /*
   * // TODO: CQL add operations here public static TyExpRaw sql(Set<String> used)
   * { List<Pair<String, String>> java_tys = new LinkedList<>(); List<Pair<String,
   * String>> java_parsers = new LinkedList<>();
   *
   * for (String s0 : used) { String s = easikTypeToString(s0); java_tys.add(new
   * Pair<>(s, javaClassFor(s))); java_parsers.add(new Pair<>(s,
   * javaParserFor(s))); }
   *
   * return new TyExpRaw(new LinkedList<>(), new LinkedList<>(), new
   * LinkedList<>(), new LinkedList<>(), java_tys, java_parsers, new
   * LinkedList<>(), new LinkedList<>()); }
   */

  public static String easikToAql(String in) {
    String ret = "";
    Set<String> tys = new TreeSet<>();
    Set<String> warnings = new TreeSet<>();
    try {
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      InputStream stream = new ByteArrayInputStream(in.getBytes(StandardCharsets.UTF_8));
      Document doc = dBuilder.parse(stream);
      doc.getDocumentElement().normalize();
      NodeList sketchesNodes = doc.getElementsByTagName("sketches");
      if (sketchesNodes.getLength() != 1) {
        throw new RuntimeException("multiple sketches tags");
      }
      Node sketchesNode = sketchesNodes.item(0);
      NodeList nList = sketchesNode.getChildNodes();
      String ret2 = "";
      for (int temp = 0; temp < nList.getLength(); temp++) {
        Node nNode = nList.item(temp);
        if (!nNode.getNodeName().equals("easketch")) {
          continue;
        }
        String s0 = nNode.getAttributes().getNamedItem("name").getTextContent().replace(" ", "_") + "_schema";
        Pair<SchExp, List<Pair<String, EdsExpRaw>>> schExp0 = translate1(nNode, tys, warnings, s0);
        SchExp schExp = schExp0.first;
        String s1 = "schema " + s0 + " = " + schExp + "\n\n";

        List<Pair<String, EdsExpRaw>> edsExps = schExp0.second;
        edsExps.addAll(translateC(nNode, warnings, new SchExpVar(s0)));
        ret2 += s1;
        int i = 0;
        List<String> imports = new LinkedList<>();
        for (Pair<String, EdsExpRaw> edsExp : edsExps) {
          String x = nNode.getAttributes().getNamedItem("name").getTextContent().replace(" ", "_") + "_"
              + edsExp.first + i;
          ret2 += "constraints " + x + " = " + edsExp.second + "\n\n";
          imports.add(x);
          i++;
        }
        if (!imports.isEmpty()) {
          ret2 += "constraints "
              + nNode.getAttributes().getNamedItem("name").getTextContent().replace(" ", "_")
              + "_constraints = literal : " + s0 + " {\n\timports\n\t\t" + Util.sep(imports, "\n\t\t")
              + "\n}\n\n";
        }
      }

      NodeList views = doc.getElementsByTagName("views");
      if (views != null && views.getLength() != 0 && views.item(0).hasChildNodes()) {
        warnings.add("Cannot export views - CQL does not currently support views ");
      }
      ret = "typeside SqlTypeSide = sql \n\n" + ret2;
      if (!warnings.isEmpty()) {
        ret += "/* Warnings:\n\n" + Util.sep(warnings, "\n") + "\n*/";
      }
      return ret;
    } catch (Exception e) {
      e.printStackTrace();
      return e.getMessage();
    }

  }

}
