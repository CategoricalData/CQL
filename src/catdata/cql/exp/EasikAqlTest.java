package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.ParseException;
import catdata.cql.Constraints;
import catdata.cql.ED;
import catdata.cql.Kind;
import catdata.cql.Schema;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for {@link EasikAql}, which converts between EASIK sketches and CQL. EASIK's "To CQL"
 * button calls {@link EasikAql#easikToAql(String)} on the saved overview and opens the result in a
 * new CQL editor; its "From last run of CQL" button calls {@link EasikAql#aqlToEasik} on the last
 * run of the selected editor.
 */
class EasikAqlTest {

  private static final String WARNINGS = "/* Warnings:";

  /** A built-in EASIK example, as EASIK saves it. */
  private static String example(String name) throws IOException {
    try (InputStream in = EasikAqlTest.class.getResourceAsStream("/examples/easik/" + name)) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /** An overview with one sketch, {@code My Sketch}, whose children are the given elements. */
  private static String overview(String sketch, String views) {
    return """
        <?xml version="1.0" encoding="UTF-8" standalone="no"?>
        <easketch_overview>
        <header><title>Overview</title></header>
        <sketches>
        <easketch name="My Sketch" x="0" y="0">
        <header><title>My Sketch</title></header>
        %s
        </easketch>
        </sketches>
        %s
        </easketch_overview>
        """
        .formatted(sketch, views);
  }

  private static String overview(String sketch) {
    return overview(sketch, "<views/>");
  }

  /** Entities A, B and C, with an integer attribute n on A, and edges f : A -> B, g : B -> C. */
  private static String abc(String edgeType, String constraints) {
    return """
        <entities>
        <entity name="A" x="0" y="0">
        <attribute attributeTypeClass="easik.database.types.Int" name="n"/>
        </entity>
        <entity name="B" x="0" y="0"/>
        <entity name="C" x="0" y="0"/>
        </entities>
        <edges>
        <edge id="f" source="A" target="B" type="%s"/>
        <edge id="g" source="B" target="C" type="normal"/>
        </edges>
        <keys/>
        <constraints>%s</constraints>
        """
        .formatted(edgeType, constraints);
  }

  private static String path(String domain, String codomain, String... edges) {
    StringBuilder sb = new StringBuilder();
    sb.append("<path codomain=\"").append(codomain).append("\" domain=\"").append(domain);
    sb.append("\">");
    for (String e : edges) {
      sb.append("<edgeref id=\"").append(e).append("\"/>");
    }
    return sb.append("</path>").toString();
  }

  /** The program with its warnings block rewritten as {@code #} comments, which CQL parses. */
  private static String withLineComments(String cql) {
    int i = cql.indexOf(WARNINGS);
    if (i < 0) {
      return cql;
    }
    String warnings = cql.substring(i + 2, cql.length() - 2);
    return cql.substring(0, i) + warnings.replaceAll("(?m)^", "# ");
  }

  @Nested
  class EasikToAql {

    @Test
    void convertsTheSampleWithoutWarningsToAProgramThatRuns() throws Exception {
      String cql = EasikAql.easikToAql(example("sample.easik"));
      assertTrue(cql.startsWith("typeside SqlTypeSide = sql \n\n"), cql);
      assertFalse(cql.contains(WARNINGS), cql);
      AqlMultiDriver d = ExpTestHelpers.run(cql);
      Schema<String, String, Sym, Fk, Att> s =
          ExpTestHelpers.value(d, "NewSketch2_schema", Kind.SCHEMA);
      assertEquals(4, s.ens.size());
      assertEquals(6, s.fks.size());
      Constraints c = ExpTestHelpers.value(d, "NewSketch2_constraints", Kind.CONSTRAINTS);
      assertEquals(2, c.eds.size());
    }

    /**
     * The warnings are written as a {@code /* ... *\/} block, which CQL does not parse, so the
     * program EASIK opens in CQL fails at the first character of the block.
     */
    @ParameterizedTest
    @ValueSource(
        strings = {
          "constraintOverview.easik",
          "constraints.easik",
          "demo.easik",
          "sampleOverview.easik"
        })
    void writesWarningsInACommentCqlCannotParse(String file) throws Exception {
      String cql = EasikAql.easikToAql(example(file));
      assertTrue(cql.endsWith("*/"), cql);
      ParseException e = assertThrows(ParseException.class, () -> ExpTestHelpers.parse(cql));
      int line =
          (int) cql.substring(0, cql.indexOf(WARNINGS)).chars().filter(c -> c == '\n').count();
      assertEquals(line + 1, e.line);
    }

    @ParameterizedTest
    @ValueSource(strings = {"constraintOverview.easik", "constraints.easik", "demo.easik"})
    void convertsTheOtherSamplesOnceTheWarningsAreLineComments(String file) throws Exception {
      AqlMultiDriver d = ExpTestHelpers.run(withLineComments(EasikAql.easikToAql(example(file))));
      assertNull(d.env.exn, String.valueOf(d.env.exn));
    }

    /** Easik.database.types.DoublePrecision becomes DoublePrecision, not Double precision. */
    @Test
    void namesTypesTheSqlTypesideLacks() throws Exception {
      String cql = withLineComments(EasikAql.easikToAql(example("sampleOverview.easik")));
      String msg = ExpTestHelpers.error(cql);
      assertTrue(
          msg.contains("On symbol Airport_towerfreq, the return Type inl DoublePrecision is not"),
          msg);
    }

    @Test
    void translatesEntitiesAttributesAndEdges() throws Exception {
      String cql = EasikAql.easikToAql(overview(abc("normal", "")));
      Schema<String, String, Sym, Fk, Att> s =
          ExpTestHelpers.eval(cql, "My_Sketch_schema", Kind.SCHEMA);
      assertEquals(Set.of("A", "B", "C"), s.ens);
      assertEquals(Map.of("A_n", "Integer"), attributeTypes(s));
      assertEquals(2, s.fks.size());
      assertFalse(cql.contains("constraints"), cql);
    }

    @Test
    void makesNamesSafe() {
      String cql =
          EasikAql.easikToAql(
              overview(
                  """
                  <entities>
                  <entity name="a b-c.d/e" x="0" y="0">
                  <attribute attributeTypeClass="easik.database.types.Text" name="x y"/>
                  </entity>
                  </entities>
                  """));
      assertTrue(cql.contains("a_b_c_d_e"), cql);
      assertTrue(cql.contains("a_b_c_d_e_x_y : a_b_c_d_e -> Text"), cql);
    }

    @Test
    void anInjectiveEdgeBecomesAConstraint() throws Exception {
      String cql = EasikAql.easikToAql(overview(abc("injective", "")));
      Constraints c = ExpTestHelpers.eval(cql, "My_Sketch_constraints", Kind.CONSTRAINTS);
      assertEquals(1, c.eds.size());
      ED ed = c.eds.get(0);
      assertEquals(Set.of("x", "y"), ed.As.keySet());
      assertTrue(ed.Es.isEmpty());
      assertFalse(ed.isUnique);
      assertTrue(cql.contains("constraints My_Sketch_injective0 = "), cql);
    }

    /**
     * The warning says partial edges are not exported, but the edge is exported as a total foreign
     * key.
     */
    @Test
    void exportsAPartialEdgeAsATotalForeignKey() throws Exception {
      String cql = EasikAql.easikToAql(overview(abc("partial", "")));
      assertTrue(
          cql.contains("Not exported - partial edges.  Their CQL semantics is unclear"), cql);
      Schema<String, String, Sym, Fk, Att> s =
          ExpTestHelpers.eval(withLineComments(cql), "My_Sketch_schema", Kind.SCHEMA);
      assertTrue(s.fks.containsKey(Fk.Fk("A", "f")), s.fks.toString());
    }

    @Test
    void aUniqueKeyBecomesAConstraintOnItsAttributes() throws Exception {
      String sketch =
          abc("normal", "")
              .replace(
                  "<keys/>",
                  """
                  <keys><uniqueKey name="k" noderef="A">
                  <attref name="n"/>
                  <other/>
                  </uniqueKey></keys>
                  """);
      String cql = EasikAql.easikToAql(overview(sketch));
      assertTrue(cql.contains("x.A_n = y.A_n"), cql);
      Constraints c = ExpTestHelpers.eval(cql, "My_Sketch_constraints", Kind.CONSTRAINTS);
      assertEquals(1, c.eds.size());
      assertTrue(cql.contains("constraints My_Sketch_key0 = "), cql);
    }

    @Test
    void aCommutativeDiagramBecomesAPathEquation() throws Exception {
      String sketch =
          abc(
              "normal",
              "<commutativediagram>"
                  + path("A", "C", "f", "g")
                  + "<other/>"
                  + path("A", "C", "f", "g")
                  + "</commutativediagram>");
      Schema<String, String, Sym, Fk, Att> s =
          ExpTestHelpers.eval(
              EasikAql.easikToAql(overview(sketch)), "My_Sketch_schema", Kind.SCHEMA);
      assertEquals(1, s.eqs.size());
    }

    @Test
    void aCommutativeDiagramNeedsTwoPaths() {
      String sketch =
          abc(
              "normal",
              "<commutativediagram>" + path("A", "C", "f", "g") + "</commutativediagram>");
      assertEquals("Easik to CQL internal error", EasikAql.easikToAql(overview(sketch)));
    }

    @Test
    void aPullbackBecomesAnExistsUniqueConstraint() throws Exception {
      // P is the pullback of f : A -> B and g : C -> B, with projections p : P -> A, q : P -> C
      String sketch =
          """
          <entities>
          <entity name="A" x="0" y="0"/><entity name="B" x="0" y="0"/>
          <entity name="C" x="0" y="0"/><entity name="P" x="0" y="0"/>
          </entities>
          <edges>
          <edge id="f" source="A" target="B" type="normal"/>
          <edge id="g" source="C" target="B" type="normal"/>
          <edge id="p" source="P" target="A" type="normal"/>
          <edge id="q" source="P" target="C" type="normal"/>
          </edges>
          <constraints><pullbackconstraint>%s%s%s<other/>%s</pullbackconstraint></constraints>
          """
              .formatted(
                  path("P", "C", "q"),
                  path("C", "B", "g"),
                  path("P", "A", "p"),
                  path("A", "B", "f"));
      String cql = EasikAql.easikToAql(overview(sketch));
      assertTrue(cql.contains("exists unique"), cql);
      // Constraints splits exists unique into an existence and a uniqueness constraint
      Constraints c = ExpTestHelpers.eval(cql, "My_Sketch_constraints", Kind.CONSTRAINTS);
      assertEquals(2, c.eds.size());
      ED ed = c.eds.get(0);
      assertEquals(Set.of("b", "c"), ed.As.keySet());
      assertEquals(Set.of("a"), ed.Es.keySet());
      assertEquals(2, ed.Ewh.size());
      assertTrue(cql.contains("constraints My_Sketch_pullback0 = "), cql);
    }

    @Test
    void aProductBecomesAnExistsUniqueConstraint() throws Exception {
      String sketch =
          """
          <entities>
          <entity name="A" x="0" y="0"/><entity name="B" x="0" y="0"/>
          <entity name="C" x="0" y="0"/>
          </entities>
          <edges>
          <edge id="p" source="A" target="B" type="normal"/>
          <edge id="q" source="A" target="C" type="normal"/>
          </edges>
          <constraints><productconstraint>%s<other/>%s</productconstraint></constraints>
          """
              .formatted(path("A", "B", "p"), path("A", "C", "q"));
      String cql = EasikAql.easikToAql(overview(sketch));
      assertTrue(cql.contains("exists unique"), cql);
      Constraints c = ExpTestHelpers.eval(cql, "My_Sketch_constraints", Kind.CONSTRAINTS);
      assertEquals(2, c.eds.size());
      ED ed = c.eds.get(0);
      assertEquals(Set.of("b", "c"), ed.As.keySet());
      assertEquals(Set.of("a"), ed.Es.keySet());
      assertTrue(ed.Awh.isEmpty());
      assertTrue(cql.contains("constraints My_Sketch_product0 = "), cql);
    }

    @Test
    void anEqualizerBecomesAnExistsUniqueConstraint() throws Exception {
      // E equalizes f, g : A -> B through h : E -> A
      String sketch =
          """
          <entities>
          <entity name="A" x="0" y="0"/><entity name="B" x="0" y="0"/>
          <entity name="E" x="0" y="0"/>
          </entities>
          <edges>
          <edge id="f" source="A" target="B" type="normal"/>
          <edge id="g" source="A" target="B" type="normal"/>
          <edge id="h" source="E" target="A" type="injective"/>
          </edges>
          <constraints><equalizerconstraint>%s%s<other/>%s</equalizerconstraint></constraints>
          """
              .formatted(path("E", "A", "h"), path("A", "B", "f"), path("A", "B", "g"));
      String cql = EasikAql.easikToAql(overview(sketch));
      // the injective edge h, then the existence and uniqueness halves of the equalizer
      Constraints c = ExpTestHelpers.eval(cql, "My_Sketch_constraints", Kind.CONSTRAINTS);
      assertEquals(3, c.eds.size());
      assertEquals(Set.of("b"), c.eds.get(1).As.keySet());
      assertEquals(1, c.eds.get(1).Awh.size());
      assertTrue(cql.contains("constraints My_Sketch_equalizer1 = "), cql);
    }

    @ParameterizedTest
    @ValueSource(strings = {"pullbackconstraint", "productconstraint", "equalizerconstraint"})
    void aLimitConstraintWithoutItsPathsIsAnAnomaly(String tag) {
      String sketch = abc("normal", "<" + tag + ">" + path("A", "B", "f") + "</" + tag + ">");
      assertEquals("Anomaly - please report", EasikAql.easikToAql(overview(sketch)));
    }

    @Test
    void warnsAboutWhatItCannotExport() {
      String sketch = abc("normal", "<sumconstraint/><limitconstraint/><unknownconstraint/>");
      String cql = EasikAql.easikToAql(overview(sketch, "<views><view/></views>"));
      String warnings = cql.substring(cql.indexOf(WARNINGS));
      assertEquals(
          """
          /* Warnings:

          Cannot export views - CQL does not currently support views\s
          limit constraints not exported - if you see this, please report
          sum constraints not exported - CQL does not currently support sum constraints
          */""",
          warnings);
    }

    @Test
    void doesNotWarnAboutAnOverviewWithoutViews() {
      String xml = overview(abc("normal", ""), "");
      assertFalse(EasikAql.easikToAql(xml).contains(WARNINGS));
    }

    @Test
    void skipsChildrenOfSketchesThatAreNotSketches() {
      String xml = overview(abc("normal", "")).replace("<sketches>", "<sketches><note/>");
      assertTrue(EasikAql.easikToAql(xml).contains("schema My_Sketch_schema = "));
    }

    @Test
    void rejectsMoreThanOneSketchesElement() {
      String xml = overview("").replace("</easketch_overview>", "<sketches/></easketch_overview>");
      assertEquals("multiple sketches tags", EasikAql.easikToAql(xml));
    }

    @Test
    void returnsTheMessageOfAnUnknownType() {
      String sketch =
          """
          <entities><entity name="A" x="0" y="0">
          <attribute attributeTypeClass="easik.database.types.Geometry" name="n"/>
          </entity></entities>
          """;
      assertEquals(
          "Unknown type: easik.database.types.Geometry", EasikAql.easikToAql(overview(sketch)));
    }

    /** The result of input EASIK cannot read is the parser's message, not a program. */
    @Test
    void returnsTheMessageOfMalformedXml() {
      assertEquals("Content is not allowed in prolog.", EasikAql.easikToAql("not an overview"));
    }
  }

  /**
   * The type each EASIK type becomes. The sql typeside spells four of them differently: Bigint,
   * Double precision, Smallint and Timestamp.
   */
  @ParameterizedTest
  @CsvSource({
    "BigInt, BigInt, false",
    "Blob, Blob, true",
    "Boolean, Boolean, true",
    "Char, Char, true",
    "Date, Date, true",
    "Decimal, Decimal, true",
    "DoublePrecision, DoublePrecision, false",
    "Float, Float, true",
    "Int, Integer, true",
    "SmallInt, SmallInt, false",
    "Text, Text, true",
    "Time, Time, true",
    "TimeStamp, TimeStamp, false",
    "Varchar, Varchar, true",
    "Custom, Custom, true"
  })
  void translatesEachEasikType(String easik, String cql, boolean declared) throws Exception {
    String sketch =
        """
        <entities><entity name="A" x="0" y="0">
        <attribute attributeTypeClass="easik.database.types.%s" name="v"/>
        </entity></entities>
        """
            .formatted(easik);
    String program = EasikAql.easikToAql(overview(sketch));
    assertTrue(program.contains("A_v : A -> " + cql + "}"), program);
    AqlMultiDriver d = ExpTestHelpers.run(program);
    assertEquals(declared, d.env.exn == null, String.valueOf(d.env.exn));
  }

  @ParameterizedTest
  @CsvSource({
    "java.lang.Long, easik.database.types.BigInt",
    "java.lang.Boolean, easik.database.types.Boolean",
    "java.lang.Character, easik.database.types.Char",
    "java.lang.Double, easik.database.types.DoublePrecision",
    "java.lang.Float, easik.database.types.Float",
    "java.lang.Integer, easik.database.types.Int",
    "java.lang.Short, easik.database.types.SmallInt",
    "java.lang.String, easik.database.types.Varchar",
    "java.util.Optional, easik.database.types.Varchar"
  })
  void picksTheEasikTypeOfAJavaClass(String javaClass, String easik) {
    assertEquals(easik, EasikAql.easikTypeFor(javaClass));
  }

  @Nested
  class AqlToEasik {

    private static final String TYPESIDE =
        """
        typeside Ty = literal {
          external_types
            Int -> "java.lang.Integer"
          external_parsers
            Int -> "x => java.lang.Integer.parseInt(x)"
        }
        """;

    private String export(String program, Set<String> warnings) throws Exception {
      AqlMultiDriver d = ExpTestHelpers.run(program);
      assertNull(d.env.exn, String.valueOf(d.env.exn));
      return EasikAql.aqlToEasik(d.env, "Title", warnings);
    }

    @Test
    void wrapsTheSchemasInAnOverview() throws Exception {
      Set<String> warnings = new TreeSet<>();
      String xml = export(TYPESIDE + "schema S = literal : Ty { entities A }", warnings);
      assertTrue(
          xml.startsWith(
              "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n<easketch_overview>"
                  + "\n<header>\n<title>Title</title>\n<author>Translated from CQL</author>"),
          xml);
      assertTrue(xml.endsWith("\n</sketches><views/>\n</easketch_overview>"), xml);
      assertTrue(
          xml.contains(
              "\n<easketch cascade=\"cascade\" name=\"S\" partial-cascade=\"set_null\" x=\"0\""
                  + " y=\"0\">"),
          xml);
      assertTrue(warnings.isEmpty(), warnings.toString());
    }

    @Test
    void exportsEntitiesAttributesAndForeignKeys() throws Exception {
      String xml =
          export(
              TYPESIDE
                  + """
                  schema S = literal : Ty {
                    entities A B
                    foreign_keys f : A -> B
                    attributes A_n : A -> Int
                  }
                  """,
              new TreeSet<>());
      assertTrue(
          xml.contains(
              "\n<entity name=\"A\" x=\"0\" y=\"0\">\n<attribute"
                  + " attributeTypeClass=\"easik.database.types.Int\" name=\"n\" />\n</entity>"),
          xml);
      assertTrue(xml.contains("\n<entity name=\"B\" x=\"100\" y=\"0\">\n</entity>"), xml);
      assertTrue(
          xml.contains(
              "\n<edge cascade=\"cascade\" id=\"f\" source=\"A\" target=\"B\" type=\"normal\"/>"),
          xml);
    }

    @Test
    void laysEntitiesOutInRowsOfFive() throws Exception {
      String xml =
          export(TYPESIDE + "schema S = literal : Ty { entities A B C D E F }", new TreeSet<>());
      assertTrue(xml.contains("<entity name=\"E\" x=\"400\" y=\"0\">"), xml);
      assertTrue(xml.contains("<entity name=\"F\" x=\"0\" y=\"100\">"), xml);
    }

    @Test
    void laysSchemasOutInRowsOfFive() throws Exception {
      StringBuilder sb = new StringBuilder(TYPESIDE);
      for (int i = 0; i < 6; i++) {
        sb.append("schema S").append(i).append(" = literal : Ty { entities A }\n");
      }
      String xml = export(sb.toString(), new TreeSet<>());
      Set<String> positions = new TreeSet<>();
      for (String line : xml.split("\n")) {
        if (line.startsWith("<easketch ")) {
          positions.add(line.substring(line.indexOf(" x=")));
        }
      }
      assertEquals(
          Set.of(
              " x=\"0\" y=\"0\">",
              " x=\"100\" y=\"0\">",
              " x=\"200\" y=\"0\">",
              " x=\"300\" y=\"0\">",
              " x=\"400\" y=\"0\">",
              " x=\"0\" y=\"100\">"),
          positions);
    }

    @Test
    void exportsPathEquationsAsCommutativeDiagrams() throws Exception {
      StringBuilder eqs = new StringBuilder();
      for (int i = 0; i < 6; i++) {
        eqs.append("A.f").append(i).append(".g = A.h\n");
      }
      StringBuilder fks = new StringBuilder("g : B -> C h : A -> C\n");
      for (int i = 0; i < 6; i++) {
        fks.append("f").append(i).append(" : A -> B\n");
      }
      String xml =
          export(
              TYPESIDE
                  + "schema S = literal : Ty { entities A B C foreign_keys "
                  + fks
                  + " path_equations "
                  + eqs
                  + "}",
              new TreeSet<>());
      assertTrue(
          xml.contains(
              "\n<path codomain=\"C\" domain=\"A\">\n<edgeref id=\"f0\"/>\n<edgeref"
                  + " id=\"g\"/>\n</path>"),
          xml);
      assertTrue(
          xml.contains("\n<path codomain=\"C\" domain=\"A\">\n<edgeref id=\"h\"/>\n</path>"), xml);
      assertEquals(6, xml.split("<commutativediagram ", -1).length - 1);
      // the diagrams continue the layout of the three entities, and wrap after x = 400
      assertTrue(xml.contains("<commutativediagram isVisible=\"true\" x=\"400\" y=\"0\">"), xml);
      assertTrue(xml.contains("<commutativediagram isVisible=\"true\" x=\"0\" y=\"100\">"), xml);
    }

    @Test
    void warnsAboutWhatItCannotExport() throws Exception {
      Set<String> warnings = new TreeSet<>();
      String xml =
          export(
              """
              typeside Ty = literal {
                external_types Int -> "java.lang.Integer"
                external_parsers Int -> "x => java.lang.Integer.parseInt(x)"
                equations forall x:Int. x = x
              }
              schema S = literal : Ty {
                entities A
                attributes n : A -> Int
                observation_equations forall x:A. x.n = x.n
              }
              constraints C = literal : S { forall a:A -> where a.n = a.n }
              """,
              warnings);
      assertEquals(
          Set.of(
              "constraints not exported",
              "observation_equations not exported.",
              "typeside equations not exported."),
          warnings);
      assertFalse(xml.contains("commutativediagram"), xml);
    }

    /**
     * The attribute name loses everything up to its first underscore, not the entity name, so
     * My_Table_name becomes Table_name.
     */
    @Test
    void cutsAttributeNamesAtTheirFirstUnderscore() throws Exception {
      String xml =
          export(
              TYPESIDE
                  + """
                  schema S = literal : Ty {
                    entities My_Table Other
                    attributes
                      My_Table_name : My_Table -> Int
                      Other_size : Other -> Int
                      plain : Other -> Int
                  }
                  """,
              new TreeSet<>());
      assertTrue(xml.contains("name=\"Table_name\" />"), xml);
      assertTrue(xml.contains("name=\"size\" />"), xml);
      assertTrue(xml.contains("name=\"plain\" />"), xml);
    }

    /** Every type of the sql typeside has the Java class Optional, so becomes Varchar. */
    @Test
    void exportsEveryAttributeOfTheSqlTypesideAsVarchar() throws Exception {
      String xml =
          export(
              """
              typeside Ty = sql
              schema S = literal : Ty {
                entities A
                attributes i : A -> Integer b : A -> Boolean d : A -> Double
              }
              """,
              new TreeSet<>());
      assertEquals(3, xml.split("easik.database.types.Varchar", -1).length - 1, xml);
    }

    @Test
    void exportsATypeWithoutAJavaClassAsVarchar() throws Exception {
      String xml =
          export(
              """
              typeside Ty = literal { types Name }
              schema S = literal : Ty { entities A attributes a : A -> Name }
              """,
              new TreeSet<>());
      assertTrue(
          xml.contains("attributeTypeClass=\"easik.database.types.Varchar\" name=\"a\""), xml);
    }

    /** Names are written into the XML without escaping. */
    @Test
    void doesNotEscapeNames() throws Exception {
      String xml =
          EasikAql.aqlToEasik(
              ExpTestHelpers.run(TYPESIDE + "schema S = literal : Ty { entities \"R&D\" }").env,
              "Q&A",
              new TreeSet<>());
      assertTrue(xml.contains("<title>Q&A</title>"), xml);
      assertTrue(xml.contains("<entity name=\"R&D\""), xml);
      assertEquals(
          "The reference to entity \"A\" must end with the ';' delimiter.",
          EasikAql.easikToAql(xml));
    }

    @Test
    void roundTripsASketchWithoutUnderscoresOrTypes() throws Exception {
      String xml = EasikAql.easikToAql(example("sample.easik"));
      AqlMultiDriver d = ExpTestHelpers.run(xml);
      String back = EasikAql.aqlToEasik(d.env, "Title", new TreeSet<>());
      Schema<String, String, Sym, Fk, Att> s =
          ExpTestHelpers.eval(EasikAql.easikToAql(back), "NewSketch2_schema_schema", Kind.SCHEMA);
      assertEquals(4, s.ens.size());
      assertEquals(6, s.fks.size());
    }
  }

  private static Map<String, String> attributeTypes(Schema<String, String, Sym, Fk, Att> s) {
    Map<String, String> ret = new HashMap<>();
    s.atts.forEach((a, p) -> ret.put(a.str, p.second));
    return ret;
  }
}
