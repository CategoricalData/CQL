package catdata.cql;

import static catdata.cql.CqlTestHelpers.VARCHAR;
import static catdata.cql.CqlTestHelpers.literal;
import static catdata.cql.CqlTestHelpers.ops;
import static catdata.cql.CqlTestHelpers.sqlSchema;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import catdata.Chc;
import catdata.Pair;
import catdata.cql.Collage.CCollage;
import catdata.cql.exp.Att;
import catdata.cql.exp.Fk;
import catdata.cql.exp.Sym;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RdfExporterTest {

  private static final String TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
  private static final String PROPERTY = "http://www.w3.org/1999/02/22-rdf-syntax-ns#Property";
  private static final String DOMAIN = "http://www.w3.org/2000/01/rdf-schema#domain";
  private static final String RANGE = "http://www.w3.org/2000/01/rdf-schema#range";
  private static final String CLASS = "http://www.w3.org/2000/01/rdf-schema#Class";

  private static final Att N = Att.Att("E", "n");
  private static final Fk F = Fk.Fk("E", "f");

  /** f : E -> D and n : E -> Varchar. */
  private static final Schema<String, String, Sym, Fk, Att> SCHEMA =
      sqlSchema(List.of("D", "E"), Map.of(N, "E"), Map.of(F, new Pair<>("E", "D")));

  private static Resource res(String uri) {
    return ResourceFactory.createResource(uri);
  }

  private static Property prop(String uri) {
    return ResourceFactory.createProperty(uri);
  }

  private static boolean has(Model m, String s, String p, RDFNode o) {
    return m.contains(res(s), prop(p), o);
  }

  private static Term<String, String, Sym, Fk, Att, String, String> gen(String g) {
    return Term.Gen(g);
  }

  private static Term<String, String, Sym, Fk, Att, String, String> varchar(String s) {
    return Term.Obj(Optional.of(s), VARCHAR);
  }

  /** d : D and e1 e2 : E, with e1.f = e2.f = d and e1.n = ann; e2.n is unknown. */
  private static Instance<
          String, String, Sym, Fk, Att, String, String, Integer, Chc<String, Pair<Integer, Att>>>
      instance() {
    Map<String, String> gens = new LinkedHashMap<>();
    gens.put("d", "D");
    gens.put("e1", "E");
    gens.put("e2", "E");
    return literal(
        SCHEMA,
        gens,
        List.of(
            new Pair<>(Term.Fk(F, gen("e1")), gen("d")),
            new Pair<>(Term.Fk(F, gen("e2")), gen("d")),
            new Pair<>(Term.Att(N, gen("e1")), Term.Obj(Optional.of("ann"), VARCHAR))));
  }

  @Nested
  class Values {

    @Test
    void isAClassOfStaticMethods() {
      assertEquals(RdfExporter.class, new RdfExporter().getClass());
    }

    @Test
    void typeConvertFallsBackToXmlString() {
      assertEquals("xsd:int", RdfExporter.typeConvert("Integer", Map.of("Integer", "xsd:int")));
      assertEquals(
          "http://www.w3.org/2001/XMLSchema#String", RdfExporter.typeConvert("Integer", Map.of()));
    }

    @Test
    void valueConvertUnwrapsAnOptionalAndDropsAnEmptyOne() {
      Map<String, Function<Object, String>> none = Map.of();
      Term<String, Void, Sym, Void, Void, Void, String> ann = Term.Obj(Optional.of("ann"), VARCHAR);
      assertEquals("ann", RdfExporter.valueConvert(none, ann));
      assertNull(RdfExporter.valueConvert(none, Term.Obj(Optional.empty(), VARCHAR)));
      assertEquals("5", RdfExporter.valueConvert(none, Term.Obj(5, "Num")));
      assertNull(RdfExporter.valueConvert(none, Term.Sk("y")));
    }

    /** Issue 59: an Optional value reaches the printer as its term; any other value as itself. */
    @Test
    void valueConvertGivesThePrinterTheTermOfAnOptional() {
      List<Object> seen = new ArrayList<>();
      Map<String, Function<Object, String>> printer =
          Map.of(
              VARCHAR,
              o -> {
                seen.add(o);
                return "v";
              },
              "Num",
              o -> {
                seen.add(o);
                return "n";
              });
      Term<String, Void, Sym, Void, Void, Void, String> ann = Term.Obj(Optional.of("ann"), VARCHAR);
      assertEquals("v", RdfExporter.valueConvert(printer, ann));
      assertEquals("n", RdfExporter.valueConvert(printer, Term.Obj(5, "Num")));
      assertEquals(List.of(ann, 5), seen);
    }

    @Test
    void valueConvertGivesNullForAFailingPrinter() {
      Map<String, Function<Object, String>> failing =
          Map.of(
              VARCHAR,
              o -> {
                throw new IllegalStateException("no such method");
              });
      assertNull(RdfExporter.valueConvert(failing, Term.Obj(Optional.of("ann"), VARCHAR)));
    }
  }

  @Nested
  class Schemas {

    @Test
    void declaresEntitiesAsClassesAndAttributesAndForeignKeysAsProperties() {
      Model m = RdfExporter.xmlExport1(SCHEMA, Map.of(VARCHAR, "xsd:string"));
      assertTrue(has(m, "cql://entity/E", TYPE, res(CLASS)));
      assertTrue(has(m, "cql://entity/D", TYPE, res(CLASS)));
      assertTrue(has(m, "cql://attribute/E/n", TYPE, res(PROPERTY)));
      assertTrue(has(m, "cql://attribute/E/n", DOMAIN, res("cql://entity/E")));
      assertTrue(has(m, "cql://attribute/E/n", RANGE, res("xsd:string")));
      assertTrue(has(m, "cql://foreign_key/E/f", TYPE, res(PROPERTY)));
      assertTrue(has(m, "cql://foreign_key/E/f", DOMAIN, res("cql://entity/E")));
      assertTrue(has(m, "cql://foreign_key/E/f", RANGE, res("cql://entity/D")));
      assertEquals(2 + 3 + 3, m.size());
    }
  }

  @Nested
  class Instances {

    private Set<String> statements(Model m) {
      Set<String> ret = new HashSet<>();
      m.listStatements().forEachRemaining(st -> ret.add(st.toString()));
      return ret;
    }

    /** Rows are numbered from 0 within each entity, in the order of the algebra. */
    private String uri(
        Instance<
                String,
                String,
                Sym,
                Fk,
                Att,
                String,
                String,
                Integer,
                Chc<String, Pair<Integer, Att>>>
            i,
        String en,
        String g) {
      int k = 0;
      for (Integer x : i.algebra().en(en)) {
        if (x.equals(i.algebra().gen(g))) {
          return "cql://entity/" + en + "/" + k;
        }
        k++;
      }
      throw new IllegalArgumentException(g);
    }

    @Test
    void typesEveryRowAndWritesItsKnownValuesAndForeignKeys() {
      var i = instance();
      Model m = RdfExporter.xmlExport1(i, 0, Map.of(), Map.of());
      String e1 = uri(i, "E", "e1");
      String e2 = uri(i, "E", "e2");
      String d = uri(i, "D", "d");
      assertTrue(has(m, d, TYPE, res("cql://entity/D")));
      assertTrue(has(m, e1, TYPE, res("cql://entity/E")));
      assertTrue(has(m, e1, "cql://attribute/E/n", ResourceFactory.createStringLiteral("ann")));
      assertFalse(m.contains(res(e2), prop("cql://attribute/E/n")));
      assertTrue(has(m, e1, "cql://foreign_key/E/f", res(d)));
      assertTrue(has(m, e2, "cql://foreign_key/E/f", res(d)));
      assertEquals(8 + 3 + 1 + 2, m.size());
    }

    /** start_ids_at reaches xmlExport1 as start, which nothing reads. */
    @Test
    void ignoresTheStartNumber() {
      var i = instance();
      assertEquals(
          statements(RdfExporter.xmlExport1(i, 0, Map.of(), Map.of())),
          statements(RdfExporter.xmlExport1(i, 100, Map.of(), Map.of())));
    }
  }

  /** export_rdf_direct_xml, over the triple schema R(subject, predicate, object). */
  @Nested
  class Triples {

    private final Att subject = Att.Att("R", "subject");
    private final Att predicate = Att.Att("R", "predicate");
    private final Att object = Att.Att("R", "object");

    /**
     * Dom holds strings, Opt optional strings and Num integers; object has the type given, and
     * the instance is r1 with subject s and predicate p, plus the values given.
     */
    private Instance<
            String, String, Sym, Fk, Att, String, String, Integer, Chc<String, Pair<Integer, Att>>>
        triples(
            String objectType,
            Term<String, String, Sym, Fk, Att, String, String> obj,
            boolean knownSubject,
            boolean knownPredicate) {
      Map<String, String> jts =
          Map.of(
              "Dom", "java.lang.String", "Opt", "java.util.Optional", "Num", "java.lang.Integer");
      Map<String, String> jps =
          Map.of(
              "Dom", "x => x",
              "Opt", "x => java.util.Optional.of(x)",
              "Num", "x => java.lang.Integer.parseInt(x)");
      TypeSide<String, Sym> ts =
          new TypeSide<>(
              new HashSet<>(jts.keySet()),
              new LinkedHashMap<>(),
              new HashSet<>(),
              jts,
              jps,
              Map.of(),
              ops());
      Collage<String, String, Sym, Fk, Att, Void, Void> col = new CCollage<>();
      col.tys().addAll(ts.tys);
      col.java_tys().putAll(jts);
      col.java_parsers().putAll(jps);
      col.getEns().add("R");
      col.atts().put(subject, new Pair<>("R", "Dom"));
      col.atts().put(predicate, new Pair<>("R", "Dom"));
      col.atts().put(object, new Pair<>("R", objectType));
      Schema<String, String, Sym, Fk, Att> s = new Schema<>(ts, col, ops());
      List<
              Pair<
                  Term<String, String, Sym, Fk, Att, String, String>,
                  Term<String, String, Sym, Fk, Att, String, String>>>
          eqs = new ArrayList<>();
      if (knownSubject) {
        eqs.add(new Pair<>(Term.Att(subject, gen("r1")), Term.Obj("http://ex/s", "Dom")));
      }
      if (knownPredicate) {
        eqs.add(new Pair<>(Term.Att(predicate, gen("r1")), Term.Obj("http://ex/p", "Dom")));
      }
      if (obj != null) {
        eqs.add(new Pair<>(Term.Att(object, gen("r1")), obj));
      }
      return literal(s, Map.of("r1", "R"), eqs);
    }

    private Statement only(Model m) {
      List<Statement> l = m.listStatements().toList();
      assertEquals(1, l.size(), "" + l);
      return l.get(0);
    }

    @Test
    void writesOneTriplePerRow() {
      Statement st =
          only(RdfExporter.xmlExportRdf(triples("Dom", Term.Obj("o", "Dom"), true, true)));
      assertEquals(res("http://ex/s"), st.getSubject());
      assertEquals(prop("http://ex/p"), st.getPredicate());
      assertEquals(ResourceFactory.createPlainLiteral("o"), st.getObject());
    }

    @Test
    void writesAnUnknownSubjectOrObjectAsABlankNode() {
      Statement st = only(RdfExporter.xmlExportRdf(triples("Dom", null, false, true)));
      assertTrue(st.getSubject().isAnon());
      assertTrue(st.getObject().isAnon());
    }

    @Test
    void writesAnOptionalObjectByItsValueAndAnEmptyOneAsABlankNode() {
      var some = triples("Opt", Term.Obj(Optional.of("o"), "Opt"), true, true);
      Statement st = only(RdfExporter.xmlExportRdf(some));
      assertEquals(ResourceFactory.createPlainLiteral("o"), st.getObject());
      var none = triples("Opt", Term.Obj(Optional.empty(), "Opt"), true, true);
      Statement empty = only(RdfExporter.xmlExportRdf(none));
      assertTrue(empty.getObject().isAnon());
    }

    @Test
    void rejectsAnUnknownPredicateAndAnObjectOfAnotherClass() {
      var noPredicate = triples("Dom", Term.Obj("o", "Dom"), true, false);
      String m =
          assertThrows(RuntimeException.class, () -> RdfExporter.xmlExportRdf(noPredicate))
              .getMessage();
      assertEquals("Anonymous predicates not supported in Jena", m);
      var number = triples("Num", Term.Obj(5, "Num"), true, true);
      String n =
          assertThrows(RuntimeException.class, () -> RdfExporter.xmlExportRdf(number)).getMessage();
      assertEquals("Anomaly: please report", n);
    }

    @Test
    void rejectsAnInstanceThatIsNotOnTheTripleSchema() {
      var i = instance();
      assertEquals(
          "Not on triple schema",
          assertThrows(RuntimeException.class, () -> RdfExporter.xmlExportRdf(i)).getMessage());
    }

    /** Issue 59: over sql, the subject is an Optional, which is cast to String. */
    @Test
    void failsOverTheSqlTypeside() {
      Map<Att, String> atts = Map.of(subject, "R", predicate, "R", object, "R");
      var s = sqlSchema(List.of("R"), atts, Map.of());
      var i =
          literal(
              s,
              Map.of("r1", "R"),
              List.of(
                  new Pair<>(Term.Att(subject, gen("r1")), varchar("http://ex/s")),
                  new Pair<>(Term.Att(predicate, gen("r1")), varchar("http://ex/p")),
                  new Pair<>(Term.Att(object, gen("r1")), varchar("o"))));
      assertThrows(ClassCastException.class, () -> RdfExporter.xmlExportRdf(i));
    }
  }
}
