package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.Pair;
import catdata.Unit;
import catdata.cql.Instance;
import catdata.cql.Kind;
import catdata.cql.Term;
import catdata.cql.exp.InstExp.InstExpVar;
import catdata.cql.exp.InstExp.InstExpVisitor;
import catdata.cql.exp.SchExp.SchExpVar;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link InstExpInclude}, {@code instance J = include I : T}, which reads an
 * instance of a schema as an instance of a larger schema that imports it.
 */
class InstExpIncludeTest {

  private static final String SCHEMAS =
      """
      typeside Ty = literal { types String constants x : String }
      schema S = literal : Ty { entities A attributes n : A -> String }
      schema T = literal : Ty { imports S entities B }
      """;

  private static String program(String i) {
    return SCHEMAS + "instance I = literal : S { " + i + " }\ninstance J = include I : T\n";
  }

  @Nested
  class Evaluation {

    @Test
    void keepsTheRowsAndAddsEmptyEntities() throws Exception {
      Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?> j =
          ExpTestHelpers.eval(program("generators a : A"), "J", Kind.INSTANCE);
      assertEquals(Set.of("A", "B"), j.schema().ens);
      assertEquals(1, j.algebra().size("A"));
      assertEquals(0, j.algebra().size("B"));
      assertFalse(j.algebra().en("B").iterator().hasNext());
      assertTrue(j.dp().toStringProver().startsWith("InstExpInclude of "), j.dp().toStringProver());
      assertTrue(j.algebra().toStringProver().startsWith("Inst Exp Include Inst "));
    }

    /**
     * The decision procedure of the result answers false for every pair of terms, so the check of
     * the equations that the result runs on construction fails as soon as I has one.
     */
    @Test
    void cannotIncludeAnInstanceWithEquations() throws Exception {
      String msg = ExpTestHelpers.error(program("generators a : A equations a.n = x"));
      assertTrue(msg.contains("Anomaly: not equal: a.n and x"), msg);
    }

    @Test
    void decidesNoEquation() throws Exception {
      Instance<String, String, Sym, Fk, Att, String, String, ?, ?> j =
          ExpTestHelpers.eval(program("generators a : A"), "J", Kind.INSTANCE);
      Term<String, String, Sym, Fk, Att, String, String> a = Term.Gen("a");
      assertFalse(j.dp().eq(Map.of(), a, a));
    }

    /** An attribute the included schema lacks fails only when it is read. */
    @Test
    void failsWhenReadingAnAttributeTheIncludedSchemaLacks() throws Exception {
      String program =
          program("generators a : A")
              .replace(
                  "schema T = literal : Ty { imports S entities B }",
                  "schema T = literal : Ty { imports S attributes m : A -> String }");
      Instance<String, String, Sym, Fk, Att, ?, ?, Object, ?> j =
          ExpTestHelpers.eval(program, "J", Kind.INSTANCE);
      Object row = j.algebra().en("A").iterator().next();
      RuntimeException e =
          assertThrows(RuntimeException.class, () -> j.algebra().att(Att.Att("A", "m"), row));
      assertTrue(e.getMessage().startsWith("m on "), e.getMessage());
    }
  }

  @Nested
  class Expression {

    @SuppressWarnings({"unchecked", "rawtypes"})
    private InstExpInclude<?, ?, ?, ?> include(String i, String s) {
      return new InstExpInclude(new InstExpVar(i), new SchExpVar(s));
    }

    /** The schema is printed as the letter S, whatever it is. */
    @Test
    void printsTheLetterSForTheSchema() {
      assertEquals("include I : S", include("I", "T").toString());
    }

    @Test
    void dependsOnTheInstanceAndTheSchema() throws Exception {
      InstExpInclude<?, ?, ?, ?> e = include("I", "T");
      assertEquals(
          Set.of(new Pair<>("I", Kind.INSTANCE), new Pair<>("T", Kind.SCHEMA)),
          Set.copyOf(e.deps()));
      assertEquals(Set.of(new InstExpVar("I")), Set.copyOf(e.direct(new AqlTyping())));
      assertTrue(e.options().isEmpty());
      assertEquals(ExpTestHelpers.BASE_OPTIONS, e.allowedOptions());
      List<Exp<?>> seen = new ArrayList<>();
      e.map(seen::add);
      assertEquals(List.of(e, new InstExpVar("I"), new SchExpVar("T")), seen);
      AqlTyping g = ExpTestHelpers.typing(program("generators a : A"));
      assertEquals(new SchExpVar("T"), e.type(g));
    }

    @Test
    void equalityComparesTheInstanceAndTheSchema() {
      assertEquals(include("I", "T"), include("I", "T"));
      assertEquals(include("I", "T").hashCode(), include("I", "T").hashCode());
      assertNotEquals(include("I", "T"), include("J", "T"));
      assertNotEquals(include("I", "T"), include("I", "U"));
      assertNotEquals(include("I", "T"), null);
      assertNotEquals(include("I", "T"), "include");
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsAVisitor() {
      InstExpInclude<?, ?, ?, ?> e = include("I", "T");
      InstExpVisitor<String, Unit, RuntimeException> v = mock(InstExpVisitor.class);
      when(v.visit(Unit.unit, e)).thenReturn("visited");
      assertEquals("visited", e.accept(Unit.unit, v));
    }
  }
}
