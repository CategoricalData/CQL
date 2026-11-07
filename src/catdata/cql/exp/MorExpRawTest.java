package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.LocStr;
import catdata.Pair;
import catdata.Unit;
import catdata.cql.Kind;
import catdata.cql.exp.MorExp.MorExpCoVisitor;
import catdata.cql.exp.MorExp.MorExpVar;
import catdata.cql.exp.MorExp.MorExpVisitor;
import catdata.cql.exp.TyExp.TyExpVar;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link MorExpRaw}, an unfinished literal for theory morphisms that no grammar rule
 * builds: it checks its type mapping, then evaluates to null.
 */
class MorExpRawTest {

  private static MorExpRaw mor(List<MorExp> imports, String from, String to) {
    return new MorExpRaw(
        new TyExpVar("A"),
        new TyExpVar("B"),
        imports,
        List.of(new Pair<>(new LocStr(3, from), to)),
        List.of(new Pair<>(new LocStr(5, "f"), new Pair<>(List.of(), new RawTerm("g")))),
        List.of());
  }

  private static AqlEnv env() throws Exception {
    return ExpTestHelpers.run(
            """
            typeside A = literal { types String }
            typeside B = literal { types Text }
            """)
        .env;
  }

  @Nested
  class Evaluation {

    @Test
    void checksItsTypesThenEvaluatesToNothing() throws Exception {
      assertNull(mor(List.of(), "String", "Text").eval0(env(), false));
    }

    /** As in mapping literals, the position lookup fails before the intended message. */
    @Test
    void reportsATypeOutsideTheTypesidesByThePositionLookup() throws Exception {
      AqlEnv env = env();
      RuntimeException target =
          assertThrows(
              RuntimeException.class, () -> mor(List.of(), "String", "Nope").eval0(env, false));
      assertEquals("For section types, cannot find (String, Nope)", target.getMessage());
      RuntimeException source =
          assertThrows(
              RuntimeException.class, () -> mor(List.of(), "Nope", "Text").eval0(env, false));
      assertEquals("For section types, cannot find (Nope, Text)", source.getMessage());
    }

    /** An import evaluates to null, which Exp.eval rejects. */
    @Test
    void cannotEvaluateAnImport() throws Exception {
      AqlEnv env = env();
      MorExpRaw outer = mor(List.of(mor(List.of(), "String", "Text")), "String", "Text");
      assertThrows(RuntimeException.class, () -> outer.eval0(env, false));
    }
  }

  @Nested
  class Expression {

    private final MorExpRaw exp = mor(List.of(new MorExpVar("M")), "String", "Text");

    @Test
    void printsNothing() {
      assertNull(exp.toString());
    }

    @Test
    void dependsOnItsTypesidesAndImports() {
      assertEquals(
          Set.of(
              new Pair<>("A", Kind.TYPESIDE),
              new Pair<>("B", Kind.TYPESIDE),
              new Pair<>("M", Kind.THEORY_MORPHISM)),
          Set.copyOf(exp.deps()));
      assertEquals(List.of(new MorExpVar("M")), new ArrayList<>(exp.imports()));
      assertEquals(Map.of(), exp.options());
    }

    @Test
    void indexesItsTypesAndFunctions() {
      assertTrue(exp.raw().keySet().containsAll(Set.of("types", "functions", "String", "f")));
      assertTrue(exp.raw().get("types").isEmpty());
      assertEquals("String -> Text", exp.raw().get("String").get(0).toString());
    }

    @Test
    void equalityComparesEveryPart() {
      assertEquals(exp, mor(List.of(new MorExpVar("M")), "String", "Text"));
      assertEquals(exp.hashCode(), mor(List.of(new MorExpVar("M")), "String", "Text").hashCode());
      assertNotEquals(exp, mor(List.of(), "String", "Text"));
      assertNotEquals(exp, mor(List.of(new MorExpVar("M")), "String", "Other"));
      assertNotEquals(
          exp,
          new MorExpRaw(
              new TyExpVar("X"),
              new TyExpVar("B"),
              List.of(new MorExpVar("M")),
              List.of(),
              List.of(),
              List.of()));
      assertNotEquals(exp, null);
      assertNotEquals(exp, "mor");
      assertEquals(exp, exp);
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsAVisitorAndACoVisitor() {
      MorExpVisitor<String, Unit, RuntimeException> v = mock(MorExpVisitor.class);
      when(v.visit(Unit.unit, exp)).thenReturn("visited");
      assertEquals("visited", exp.accept(Unit.unit, v));
      MorExpCoVisitor<Unit, Unit, RuntimeException> cv = mock(MorExpCoVisitor.class);
      when(cv.visitMorExpRaw(Unit.unit, Unit.unit)).thenReturn(exp);
      assertEquals(exp, exp.coaccept(Unit.unit, cv, Unit.unit));
    }
  }
}
