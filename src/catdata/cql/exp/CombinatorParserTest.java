package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.Pair;
import catdata.ParseException;
import catdata.Program;
import catdata.Quad;
import catdata.Triple;
import catdata.apg.exp.ApgInstExp.ApgInstExpRaw;
import catdata.apg.exp.ApgSchExp.ApgSchExpRaw;
import catdata.apg.exp.ApgTransExp.ApgTransExpRaw;
import catdata.apg.exp.ApgTyExp.ApgTyExpRaw;
import catdata.cql.Kind;
import catdata.cql.exp.EdsExpRaw.EdExpRaw;
import catdata.cql.exp.GraphExp.GraphExpRaw;
import catdata.cql.gui.Links;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests for {@link CombinatorParser}: the declarations that no built-in example parses, each
 * with and without its optional blocks, and the static entry points that parse a fragment of the
 * language.
 */
class CombinatorParserTest {

  private static final CombinatorParser PARSER = new CombinatorParser();

  /** The expression the declaration binds to X. */
  private static Exp<?> parse(String declaration) throws ParseException {
    return PARSER.parseProgram(declaration).exps.get("X");
  }

  private static final String OPTIONS = "{ options timeout = 1 }";

  static Stream<Arguments> declarations() {
    return Stream.of(
        Arguments.of("schema X = unit : Ty", SchExpUnit.class),
        Arguments.of("schema X = except S T", SchExpDiff.class),
        Arguments.of("schema X = pivot I " + OPTIONS, SchExpPivot.class),
        Arguments.of("schema X = from_ms_catalog I " + OPTIONS, SchExpFromMsCatalog.class),
        Arguments.of(
            "schema X = literal : Ty { entity A foreign_keys f : B attributes n : String }",
            SchExpRaw.class),
        Arguments.of("schema X = literal : Ty { entity A }", SchExpRaw.class),
        Arguments.of(
            "schema X = literal : Ty { entities A attributes n m : A -> String"
                + " observation_equations n = m }",
            SchExpRaw.class),
        Arguments.of("command X = export_json_instance I \"f\"", PragmaExpJsonInstExport.class),
        Arguments.of("command X = export_rdf_instance_xml I \"f\"", PragmaExpRdfInstExport.class),
        Arguments.of("command X = export_rdf_direct_xml I \"f\"", PragmaExpRdfDirectExport.class),
        Arguments.of("command X = export_csv_instance I \"f\"", PragmaExpToCsvInst.class),
        Arguments.of("command X = export_excel_instance I \"f\"", PragmaExpToExcelInst.class),
        Arguments.of("command X = export_csv_transform t \"f\"", PragmaExpToCsvTrans.class),
        Arguments.of("command X = spawn_bitsy", PragmaExpBitsy.class),
        Arguments.of("command X = export_tinkerpop_instance I", PragmaExpTinkerpopInstExport.class),
        Arguments.of(
            "command X = export_jdbc_query Q \"c\" \"p\" \"q\"", PragmaExpToJdbcQuery.class),
        Arguments.of("command X = export_jdbc_transform t \"c\" \"p\"", PragmaExpToJdbcTrans.class),
        Arguments.of("command X = match \"m\" : G -> H", PragmaExpMatch.class),
        Arguments.of("instance X = pi F I", InstExpPi.class),
        Arguments.of("instance X = spanify I", InstExpSpanify.class),
        Arguments.of("instance X = sigma_chase F I", InstExpSigmaChase.class),
        Arguments.of("instance X = core I", InstExpCore.class),
        Arguments.of("instance X = pivot I", InstExpPivot.class),
        Arguments.of("instance X = eval Q I", InstExpEval.class),
        Arguments.of("instance X = chase C I", InstExpChase.class),
        Arguments.of(
            "instance X = import_csv \"d\" : S { A -> { a -> \"b\" options timeout = 1 } }",
            InstExpCsv.class),
        Arguments.of("mapping X = pivot I", MapExpPivot.class),
        Arguments.of("transform X = pi F t", TransExpPi.class),
        Arguments.of("transform X = eval Q t", TransExpEval.class),
        Arguments.of("transform X = unit_query Q I", TransExpCoEvalEvalUnit.class),
        Arguments.of("transform X = subseteq Q P", TransExpSubseteq.class),
        Arguments.of("query X = back C 0", QueryExpFront.class),
        Arguments.of("query X = front C 0", QueryExpFront.class),
        Arguments.of(
            "query X = literal : S -> T { entity E -> { from a : A attributes n -> from where"
                + " return \"0\" aggregate \"0\" lambda u v. u } }",
            QueryExpRaw.class),
        Arguments.of("schema_colimit X = wrap C F G", ColimSchExpWrap.class),
        Arguments.of("schema_colimit X = simplify C", ColimSchExpSimplify.class),
        Arguments.of(
            "schema_colimit X = modify C { rename foreign_keys rename attributes"
                + " remove attributes A.n -> lambda x : A. x.m B.n -> lambda x. x.m }",
            ColimSchExpModify.class),
        Arguments.of(
            "schema_colimit X = quotient S + T : Ty { observation_equations"
                + " forall x : S_A. x.n = x.m forall x. x.n = x.m S_A.n = S_A.m }",
            ColimSchExpQuotient.class),
        Arguments.of("typeside X = literal { constants functions }", TyExpRaw.class),
        Arguments.of("apg_typeside X = literal { }", ApgTyExpRaw.class),
        Arguments.of("apg_schema X = literal : T { }", ApgSchExpRaw.class),
        Arguments.of("apg_instance X = literal : S { }", ApgInstExpRaw.class),
        Arguments.of("apg_morphism X = literal : I -> J { }", ApgTransExpRaw.class),
        Arguments.of("graph X = literal { nodes a edges }", GraphExpRaw.class));
  }

  @ParameterizedTest
  @MethodSource("declarations")
  void parsesTheDeclaration(String declaration, Class<?> expected) throws Exception {
    assertEquals(expected, parse(declaration).getClass());
  }

  static Stream<Arguments> optionBlocks() {
    Map<String, String> one = Map.of("timeout", "1");
    Map<String, String> front = Map.of("timeout", "1", "simple_query_entity", "Front");
    return Stream.of(
        Arguments.of("schema X = pivot I " + OPTIONS, one),
        Arguments.of(
            "command X = export_json_instance I \"f\" { external_types String -> \"j\" \"p\""
                + " options timeout = 1 }",
            one),
        Arguments.of("command X = export_rdf_instance_xml I \"f\" " + OPTIONS, one),
        Arguments.of("command X = export_rdf_direct_xml I \"f\" " + OPTIONS, one),
        Arguments.of("command X = export_csv_instance I \"f\" " + OPTIONS, one),
        Arguments.of("command X = export_excel_instance I \"f\" " + OPTIONS, one),
        Arguments.of("command X = export_csv_transform t \"f\" " + OPTIONS + " " + OPTIONS, one),
        Arguments.of("command X = spawn_bitsy " + OPTIONS, one),
        Arguments.of("command X = export_tinkerpop_instance I " + OPTIONS, one),
        Arguments.of("command X = export_jdbc_query Q \"c\" \"p\" \"q\" " + OPTIONS, one),
        Arguments.of("command X = export_jdbc_transform t \"c\" \"p\" " + OPTIONS + " {}", one),
        Arguments.of("command X = match \"m\" : G -> H " + OPTIONS, one),
        Arguments.of("instance X = pi F I " + OPTIONS, one),
        Arguments.of("instance X = spanify I " + OPTIONS, one),
        Arguments.of("instance X = sigma_chase F I " + OPTIONS, one),
        Arguments.of("instance X = pivot I " + OPTIONS, one),
        Arguments.of("instance X = eval Q I " + OPTIONS, one),
        Arguments.of("instance X = chase C I " + OPTIONS, one),
        Arguments.of("mapping X = pivot I " + OPTIONS, one),
        Arguments.of("transform X = eval Q t " + OPTIONS, one),
        Arguments.of("transform X = unit_query Q I " + OPTIONS, one),
        Arguments.of("query X = back C 0 " + OPTIONS, front),
        Arguments.of("query X = front C 0 " + OPTIONS, front),
        Arguments.of("schema_colimit X = simplify C " + OPTIONS, one));
  }

  @ParameterizedTest
  @MethodSource("optionBlocks")
  void keepsTheOptionsOfTheDeclaration(String declaration, Map<String, String> expected)
      throws Exception {
    assertEquals(expected, parse(declaration).options());
  }

  /** The two blocks configure the pi of the source and of the target, so the transform has none. */
  @Test
  void keepsTheTwoOptionBlocksOfAPiTransformApart() throws Exception {
    TransExpPi<?, ?, ?, ?, ?, ?, ?, ?> e =
        (TransExpPi<?, ?, ?, ?, ?, ?, ?, ?>)
            parse("transform X = pi F t { options timeout = 1 } { options timeout = 2 }");
    assertEquals(Map.of("timeout", "1"), e.options1);
    assertEquals(Map.of("timeout", "2"), e.options2);
    assertEquals(Map.of(), e.options());
  }

  @Nested
  class Programs {

    @Test
    void namesTheKindOfEachDeclaration() throws Exception {
      Program<Exp<?>> p = PARSER.parseProgram("schema X = unit : Ty\ninstance Y = core I");
      assertEquals("schema", p.kind("X"));
      assertEquals("instance", p.kind("Y"));
    }

    @Test
    void reportsTheLineAndColumnOfASyntaxError() {
      ParseException e = assertThrows(ParseException.class, () -> parse("schema X = = Ty"));
      assertEquals(1, e.line);
    }

    /** The rule passes null for a missing labels section, which the literal rejects. */
    @Test
    void rejectsAnApgMappingLiteralWithoutLabels() throws Exception {
      ParseException e =
          assertThrows(ParseException.class, () -> parse("apg_mapping X = literal : S -> T { }"));
      assertTrue(e.getMessage().contains("Anomaly: please report"), e.getMessage());
      assertEquals(
          "ApgMapExpRaw",
          parse("apg_mapping X = literal : S -> T { labels }").getClass().getSimpleName());
    }
  }

  @Nested
  class Fragments {

    @Test
    void parsesAnEquationWithAndWithoutAContext() throws Exception {
      Triple<List<Pair<String, String>>, RawTerm, RawTerm> eq =
          PARSER.parseEq("forall x : A, y. x = y");
      assertEquals(List.of(new Pair<>("x", "A"), new Pair<>("y", null)), eq.first);
      assertEquals("x", eq.second.toString());
      assertEquals("y", eq.third.toString());
      assertEquals(List.of(), PARSER.parseEq("forall . a = b").first);
      assertEquals(List.of(), PARSER.parseEq("a = b").first);
      assertThrows(ParseException.class, () -> PARSER.parseEq("a ="));
    }

    @Test
    void parsesATermWithAndWithoutAContext() throws Exception {
      Pair<List<Pair<String, String>>, RawTerm> t = PARSER.parseTermInCtx("lambda x : A. g(x, x)");
      assertEquals(List.of(new Pair<>("x", "A")), t.first);
      assertEquals("g(x, x)", t.second.toString());
      assertEquals(List.of(), PARSER.parseTermInCtx("f(x)").first);
      assertThrows(ParseException.class, () -> PARSER.parseTermInCtx("lambda"));
      assertEquals("x.f", PARSER.parseTermNoCtx("f(x)").toString());
      assertThrows(ParseException.class, () -> PARSER.parseTermNoCtx("f("));
    }

    @Test
    void parsesAnEgglogTerm() throws Exception {
      RawTerm t = PARSER.parseEgglogRawTerm("(f (g) (h (a)))");
      assertEquals("f", t.head);
      assertEquals(2, t.args.size());
      assertEquals("h", t.args.get(1).head);
      assertThrows(ParseException.class, () -> PARSER.parseEgglogRawTerm("(f"));
    }

    @Test
    void parsesOptionsSchemasAndConstraints() {
      assertEquals(
          List.of(new Pair<>("timeout", "1")),
          CombinatorParser.parseOptions("options timeout = 1"));
      RuntimeException options =
          assertThrows(RuntimeException.class, () -> CombinatorParser.parseOptions("options = 1"));
      assertTrue(options.getMessage().contains(" col, line: 1: "), options.getMessage());
      assertEquals(
          "Ty", CombinatorParser.parseSchExpRaw("literal : Ty { entities A }").typeSide.toString());
      assertThrows(RuntimeException.class, () -> CombinatorParser.parseSchExpRaw("literal"));
      assertEquals(
          1,
          CombinatorParser.parseEdsExpRaw("literal : S { forall a:A -> where a = a }").eds.size());
      assertThrows(RuntimeException.class, () -> CombinatorParser.parseEdsExpRaw("literal"));
    }

    @Test
    void parsesTheLinksOfAWarehouse() {
      Links<String, String> links =
          CombinatorParser.parseLinks(
              "entity_isomorphisms i : S.A -> T.B equations e : forall x : S.A, x.n = x.m"
                  + " constraints c : forall a:A -> where a = a");
      assertEquals(Map.of("i", new Quad<>("S", "A", "T", "B")), links.enLinks);
      Quad<String, Pair<String, String>, RawTerm, RawTerm> e = links.colLinks.get("e");
      assertEquals("x", e.first);
      assertEquals(new Pair<>("S", "A"), e.second);
      assertEquals("x.n", e.third.toString());
      assertEquals("x.m", e.fourth.toString());
      EdExpRaw c = links.rowLinks.get("c");
      assertEquals("a : A", c.raw().get("forall").get(0).toString());
    }

    @Test
    void parsesTheHeaderOfALiteral() {
      assertEquals(
          new Quad<>(Kind.MAPPING, "F", "S", "T"),
          CombinatorParser.parseInfer("mapping F = literal : S -> T"));
      assertEquals(
          new Quad<>(Kind.QUERY, "Q", "S", "T"),
          CombinatorParser.parseInfer("query Q = literal : S -> T"));
      assertEquals("S", CombinatorParser.parseInfer1("literal : S"));
    }
  }
}
