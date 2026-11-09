package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.InteriorLabel;
import catdata.Pair;
import catdata.ParseException;
import catdata.Unit;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.ColimitSchema;
import catdata.cql.Kind;
import catdata.cql.exp.ColimSchExp.ColimSchExpVisitor;
import catdata.cql.exp.GraphExp.GraphExpVar;
import catdata.cql.exp.MapExp.MapExpVar;
import catdata.cql.exp.SchExp.SchExpVar;
import catdata.cql.exp.TyExp.TyExpVar;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the three schema colimit literals: {@link ColimSchExpRaw}, {@code literal G : Ty {
 * nodes ... edges ... }}, the colimit of a diagram of schemas; {@link ColimSchExpPseudo}, {@code
 * pseudo_quotient S + T : Ty { ... }}, which joins entities by isomorphisms; and {@link
 * ColimSchExpQuotient}, {@code quotient S + T : Ty { ... }}, which identifies entities and adds
 * equations.
 */
class ColimSchExpLiteralTest {

  private static final String BASE =
      """
      typeside Ty = literal { types String }
      typeside Ty2 = literal { types Int }
      schema S = literal : Ty { entities A attributes n : A -> String }
      schema T = literal : Ty { entities B attributes m : B -> String }
      schema U = literal : Ty2 { entities C }
      graph G = literal { nodes u v edges e : u -> v }
      mapping F = identity S
      """;

  private static <X extends ColimSchExp> X exp(String colimit) throws Exception {
    return ExpTestHelpers.exp(BASE + colimit, "C");
  }

  private static ColimitSchema<String> eval(String colimit) throws Exception {
    return ExpTestHelpers.eval(BASE + colimit, "C", Kind.SCHEMA_COLIMIT);
  }

  /** Each change, applied to the colimit, gives an expression that is not equal to it. */
  private static void assertEachChangeIsUnequal(String colimit, List<Pair<String, String>> changes)
      throws Exception {
    ColimSchExp e = exp(colimit);
    assertEquals(e, e);
    assertEquals(e, exp(colimit));
    assertEquals(e.hashCode(), exp(colimit).hashCode());
    for (Pair<String, String> change : changes) {
      assertNotEquals(e, exp(colimit.replace(change.first, change.second)), change.second);
    }
    assertNotEquals(e, null);
    assertNotEquals(e, "colimit");
  }

  @Nested
  class OfADiagram {

    private static final String COLIMIT =
        "schema_colimit C = literal G : Ty { nodes u -> S v -> S edges e -> F }";

    @Test
    void identifiesTheNodesAlongTheEdges() throws Exception {
      assertEquals(Set.of("u_A"), eval(COLIMIT).schemaStr.ens);
    }

    @Test
    void rejectsANodeGivenTwice() {
      ParseException e =
          assertThrows(
              ParseException.class,
              () -> ExpTestHelpers.parse(BASE + COLIMIT.replace("v -> S", "u -> S")));
      assertTrue(e.getMessage().contains("Duplicate node: u"), e.getMessage());
    }

    @Test
    void isTypedByItsNodesAndTypeside() throws Exception {
      ColimSchExpRaw e = exp(COLIMIT);
      AqlTyping g = ExpTestHelpers.typing(BASE + COLIMIT);
      assertEquals(Set.of("u", "v"), e.type(g));
      assertEquals(new TyExpVar("Ty"), e.typeOf(g));
      assertEquals(new SchExpVar("S"), e.getNode("u", g));
      assertEquals(Set.of(new Pair<>(new SchExpVar("S"), new SchExpColim(e))), e.gotos(e));
    }

    @Test
    void printsItsNodesAndEdges() throws Exception {
      assertEquals(
          "literal G : Ty {\n\tnodes\n\t\tu -> S\n\t\tv -> S\n\tedges\n\t\te -> F}",
          exp(COLIMIT).toString());
      assertEquals("literal G : Ty {}", exp("schema_colimit C = literal G : Ty { }").toString());
    }

    @Test
    void dependsOnEveryPart() throws Exception {
      ColimSchExpRaw e = exp(COLIMIT);
      assertEquals(
          Set.of(
              new Pair<>("Ty", Kind.TYPESIDE),
              new Pair<>("G", Kind.GRAPH),
              new Pair<>("S", Kind.SCHEMA),
              new Pair<>("F", Kind.MAPPING)),
          Set.copyOf(e.deps()));
      assertTrue(e.allowedOptions().contains(AqlOption.simplify_names));
      assertTrue(e.options().isEmpty());
      List<Exp<?>> seen = new ArrayList<>();
      e.map(seen::add);
      assertEquals(
          List.of(
              e,
              new TyExpVar("Ty"),
              new GraphExpVar("G"),
              new SchExpVar("S"),
              new SchExpVar("S"),
              new MapExpVar("F")),
          seen);
      Map<String, List<InteriorLabel<Object>>> raw = e.raw();
      assertEquals("u -> S", raw.get("nodes").get(0).toString());
      assertEquals("e -> F", raw.get("edges").get(0).toString());
    }

    @Test
    void equalityComparesEveryPart() throws Exception {
      assertEachChangeIsUnequal(
          COLIMIT,
          List.of(
              new Pair<>("literal G", "literal H"),
              new Pair<>(": Ty {", ": Ty2 {"),
              new Pair<>("v -> S", "v -> T"),
              new Pair<>("e -> F", "e -> F2"),
              new Pair<>("e -> F }", "e -> F options simplify_names = false }")));
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsAVisitor() throws Exception {
      ColimSchExpRaw e = exp(COLIMIT);
      ColimSchExpVisitor<String, Unit, RuntimeException> v = mock(ColimSchExpVisitor.class);
      when(v.visit(Unit.unit, e)).thenReturn("visited");
      assertEquals("visited", e.accept(Unit.unit, v));
    }
  }

  @Nested
  class PseudoQuotient {

    private static final String COLIMIT =
        "schema_colimit C = pseudo_quotient S + T : Ty {"
            + " entity_isomorphisms i : S.A -> T.B equations forall x:S.A, x.n = x.i.m }";

    @Test
    void joinsTheEntitiesByAnIsomorphism() throws Exception {
      ColimitSchema<String> c = eval(COLIMIT);
      assertEquals(Set.of("S_A", "T_B"), c.schemaStr.ens);
      assertEquals(Set.of(Fk.Fk("S_A", "i"), Fk.Fk("T_B", "i_inv")), c.schemaStr.fks.keySet());
    }

    @Test
    void rejectsASchemaGivenTwice() {
      ParseException e =
          assertThrows(
              ParseException.class,
              () -> ExpTestHelpers.parse(BASE + COLIMIT.replace("S + T", "S + S")));
      assertTrue(e.getMessage().contains("duplicate schema S"), e.getMessage());
    }

    @Test
    void rejectsASchemaOnAnotherTypeside() {
      String program = BASE + "schema_colimit C = pseudo_quotient S + U : Ty { }\n";
      RuntimeException e =
          assertThrows(RuntimeException.class, () -> ExpTestHelpers.typing(program));
      assertTrue(
          e.getMessage().contains("Schema for U is on typeside Ty2 and not on Ty"), e.getMessage());
    }

    @Test
    void isTypedByItsSchemasAndTypeside() throws Exception {
      ColimSchExpPseudo e = exp(COLIMIT);
      AqlTyping g = ExpTestHelpers.typing(BASE + COLIMIT);
      assertEquals(Set.of("S", "T"), e.type(g));
      assertEquals(new TyExpVar("Ty"), e.typeOf(g));
      assertEquals(new SchExpVar("T"), e.getNode("T", g));
      assertEquals(
          Set.of(
              new Pair<>(new SchExpVar("S"), new SchExpColim(e)),
              new Pair<>(new SchExpVar("T"), new SchExpColim(e))),
          e.gotos(e));
    }

    @Test
    void printsItsOptionsAndVisitsItsParts() throws Exception {
      ColimSchExpPseudo e =
          exp("schema_colimit C = pseudo_quotient S : Ty { options allow_java_eqs_unsafe = true }");
      assertEquals(
          "pseudo_quotient S : Ty  {\n\toptions\n\t\tallow_java_eqs_unsafe = true\n\n}",
          e.toString());
      List<Exp<?>> seen = new ArrayList<>();
      e.map(seen::add);
      assertEquals(List.of(e, new TyExpVar("Ty"), new SchExpVar("S")), seen);
    }

    @Test
    void rejectsASchemaOnAnotherTypesideWhenAskedForItsTypeside() throws Exception {
      String program = BASE + "schema_colimit C = pseudo_quotient S + U : Ty { }\n";
      ColimSchExpPseudo e = ExpTestHelpers.exp(program, "C");
      AqlTyping g = ExpTestHelpers.typing(BASE);
      RuntimeException ex = assertThrows(RuntimeException.class, () -> e.typeOf(g));
      assertTrue(ex.getMessage().contains("Schema for U is on typeside Ty2 and not on Ty"));
    }

    /** The isomorphisms are printed with their separators swapped, and without line breaks. */
    @Test
    void printsItsSectionsRunTogether() throws Exception {
      assertEquals(
          "pseudo_quotient S + T : Ty  {\n\tentity_isomorphismsi\nS.A -> T.B"
              + "\tequations\n\t\tforall x. x.n = x.i.m\n\n}",
          exp(COLIMIT).toString());
    }

    @Test
    void dependsOnItsSchemasAndTypeside() throws Exception {
      ColimSchExpPseudo e = exp(COLIMIT);
      assertEquals(
          Set.of(
              new Pair<>("Ty", Kind.TYPESIDE),
              new Pair<>("S", Kind.SCHEMA),
              new Pair<>("T", Kind.SCHEMA)),
          Set.copyOf(e.deps()));
      assertTrue(e.allowedOptions().contains(AqlOption.allow_java_eqs_unsafe));
      Map<String, List<InteriorLabel<Object>>> raw = e.raw();
      assertEquals("S.A = T.B", raw.get("entities").get(0).toString());
      assertEquals("x.n = x.i.m", raw.get("eqs").get(0).toString());
    }

    @Test
    void equalityComparesEveryPart() throws Exception {
      assertEachChangeIsUnequal(
          COLIMIT,
          List.of(
              new Pair<>(": Ty {", ": Ty2 {"),
              new Pair<>("S + T", "S + U"),
              new Pair<>("i : S.A", "j : S.A"),
              new Pair<>("x.n = x.i.m", "x.i.m = x.n"),
              new Pair<>("x.i.m }", "x.i.m options allow_java_eqs_unsafe = true }")));
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsAVisitor() throws Exception {
      ColimSchExpPseudo e = exp(COLIMIT);
      ColimSchExpVisitor<String, Unit, RuntimeException> v = mock(ColimSchExpVisitor.class);
      when(v.visit(Unit.unit, e)).thenReturn("visited");
      assertEquals("visited", e.accept(Unit.unit, v));
    }
  }

  @Nested
  class Quotient {

    private static final String COLIMIT =
        "schema_colimit C = quotient S + T : Ty {"
            + " entity_equations S.A = T.B observation_equations forall x. x.n = x.m }";

    @Test
    void identifiesTheEntitiesAndTheirAttributes() throws Exception {
      ColimitSchema<String> c = eval(COLIMIT);
      assertEquals(Set.of("A"), c.schemaStr.ens);
      assertEquals(1, c.schemaStr.eqs.size());
    }

    @Test
    void rejectsASchemaGivenTwice() {
      ParseException e =
          assertThrows(
              ParseException.class,
              () -> ExpTestHelpers.parse(BASE + COLIMIT.replace("S + T", "S + S")));
      assertTrue(e.getMessage().contains("duplicate schema S"), e.getMessage());
    }

    /** Unlike the pseudo-quotient, the check of the typesides is commented out. */
    @Test
    void acceptsASchemaOnAnotherTypeside() throws Exception {
      String program = BASE + "schema_colimit C = quotient S + U : Ty\n";
      ColimSchExpQuotient e = ExpTestHelpers.exp(program, "C");
      assertEquals(Set.of("S", "U"), e.type(ExpTestHelpers.typing(program)));
    }

    @Test
    void isTypedByItsSchemasAndTypeside() throws Exception {
      ColimSchExpQuotient e = exp(COLIMIT);
      AqlTyping g = ExpTestHelpers.typing(BASE + COLIMIT);
      assertEquals(new TyExpVar("Ty"), e.typeOf(g));
      assertEquals(new SchExpVar("S"), e.getNode("S", g));
      assertEquals(
          Set.of(
              new Pair<>(new SchExpVar("S"), new SchExpColim(e)),
              new Pair<>(new SchExpVar("T"), new SchExpColim(e))),
          e.gotos(e));
    }

    @Test
    void printsAsACoproductWithoutEquations() throws Exception {
      assertEquals(
          "quotient S + T : Ty  {\n\tentity_equations\n\t\tS.A = T.B\n"
              + "\tobservation_equations\n\t\tforall x. x.n = x.m\n\n}",
          exp(COLIMIT).toString());
      assertEquals(
          "coproduct S + T : Ty", exp("schema_colimit C = quotient S + T : Ty").toString());
      assertEquals(
          "coproduct : Ty",
          new ColimSchExpQuotient(
                  new TyExpVar("Ty"), List.of(), List.of(), List.of(), List.of(), List.of())
              .toString());
    }

    @Test
    void printsAndIndexesPathEquationsAndOptions() throws Exception {
      String program =
          BASE
              + "schema S3 = literal : Ty { entities A foreign_keys f g : A -> A }\n"
              + "schema_colimit C = quotient S3 : Ty {"
              + " path_equations S3_A.f = S3_A.g options simplify_names = false }";
      ColimSchExpQuotient e = ExpTestHelpers.exp(program, "C");
      assertEquals(
          "quotient S3 : Ty  {\n\tpath_equations\n\t\tS3_A.f = S3_A.g\n"
              + "\toptions\n\t\tsimplify_names = false\n\n}",
          e.toString());
      assertEquals("S3_A.f = S3_A.g", e.raw().get("obs eqs").get(0).toString());
      assertTrue(e.allowedOptions().contains(AqlOption.simplify_names));
      List<Exp<?>> seen = new ArrayList<>();
      e.map(seen::add);
      assertEquals(List.of(e, new TyExpVar("Ty"), new SchExpVar("S3")), seen);
      assertEquals(
          Set.of(new Pair<>("Ty", Kind.TYPESIDE), new Pair<>("S3", Kind.SCHEMA)),
          Set.copyOf(e.deps()));
    }

    /** The outline files the observation equations under path eqs, and the reverse. */
    @Test
    void indexesTheObservationEquationsAsPathEquations() throws Exception {
      ColimSchExpQuotient e = exp(COLIMIT);
      Map<String, List<InteriorLabel<Object>>> raw = e.raw();
      assertEquals("S.A = T.B", raw.get("entities").get(0).toString());
      assertEquals("x.n = x.m", raw.get("path eqs").get(0).toString());
      assertTrue(raw.get("obs eqs").isEmpty());
    }

    @Test
    void equalityComparesEveryPart() throws Exception {
      assertEachChangeIsUnequal(
          COLIMIT,
          List.of(
              new Pair<>(": Ty {", ": Ty2 {"),
              new Pair<>("S + T", "S + U"),
              new Pair<>("S.A = T.B", "T.B = S.A"),
              new Pair<>("x.n = x.m", "x.m = x.n"),
              new Pair<>("x.m }", "x.m options simplify_names = false }")));
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsAVisitor() throws Exception {
      ColimSchExpQuotient e = exp(COLIMIT);
      ColimSchExpVisitor<String, Unit, RuntimeException> v = mock(ColimSchExpVisitor.class);
      when(v.visit(Unit.unit, e)).thenReturn("visited");
      assertEquals("visited", e.accept(Unit.unit, v));
    }
  }
}
