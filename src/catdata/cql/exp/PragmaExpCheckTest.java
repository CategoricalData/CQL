package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.LineException;
import catdata.Pair;
import catdata.Unit;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.Kind;
import catdata.cql.Pragma;
import catdata.cql.exp.EdsExp.EdsExpVar;
import catdata.cql.exp.InstExp.InstExpVar;
import catdata.cql.exp.PragmaExp.PragmaExpVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PragmaExpCheck}, {@code command c = check C I}, which fails with the
 * triggers of every constraint of {@code C} that {@code I} does not satisfy.
 */
class PragmaExpCheckTest {

  private static final String SCHEMA =
      """
      typeside Ty = literal { types String constants x y : String }
      schema S = literal : Ty { entities A B attributes a1 : A -> String b1 : B -> String }
      constraints C = literal : S { forall a:A -> exists b:B where a.a1 = b.b1 }
      """;

  private static String program(String bValue, String options) {
    return SCHEMA
        + "instance I = literal : S { generators a : A b : B equations a.a1 = x b.b1 = "
        + bValue
        + " }\n"
        + "command chk = check C I "
        + options
        + "\n";
  }

  @Nested
  class Evaluation {

    @Test
    void reportsASatisfiedInstance() throws Exception {
      Pragma p = ExpTestHelpers.eval(program("x", ""), "chk", Kind.PRAGMA);
      assertEquals("Satisfied", p.toString());
    }

    @Test
    void failsWithTheTriggersOfEachViolatedConstraint() throws Exception {
      String msg = ExpTestHelpers.error(program("y", ""));
      assertTrue(msg.contains("Not satisfied.\n\n======================\nOn constraint\n\n"), msg);
      assertTrue(msg.contains("\n\nthe failing triggers are:\n\n\ta -> "), msg);
      assertTrue(msg.contains("[a1:x]"), msg);
    }

    /** A trigger lists the entity variables only; the type variable s is left out. */
    @Test
    void listsOnlyTheEntityVariablesOfATrigger() throws Exception {
      String msg =
          ExpTestHelpers.error(
              program("y", "")
                  .replace(
                      "forall a:A -> exists b:B where a.a1 = b.b1",
                      "forall a:A s:String where a.a1 = s -> exists b:B where s = b.b1"));
      assertTrue(msg.endsWith("the failing triggers are:\n\n\ta -> a  [a1:x]\n"), msg);
    }

    @Test
    void writesTheFailingRowsAsJson() throws Exception {
      Path out = Files.createTempDirectory("check_test").resolve("failing.json");
      String msg =
          ExpTestHelpers.error(
              program("y", "{ options check_command_export_file = \"" + out + "\" }"));
      assertTrue(msg.contains("Not satisfied."), msg);
      String json = Files.readString(out);
      assertTrue(json.startsWith("{\""), json);
      assertTrue(json.contains("\"a1\":\"x\""), json);
    }

    @Test
    void writesOnlyTheEntityVariablesOfAFailingRow() throws Exception {
      Path out = Files.createTempDirectory("check_test").resolve("failing.json");
      ExpTestHelpers.error(
          program("y", "{ options check_command_export_file = \"" + out + "\" }")
              .replace(
                  "forall a:A -> exists b:B where a.a1 = b.b1",
                  "forall a:A s:String where a.a1 = s -> exists b:B where s = b.b1"));
      String json = Files.readString(out);
      assertTrue(json.endsWith(":[{\"a\":{\"a1\":\"x\"}}]}"), json);
    }

    @Test
    void writesNothingForASatisfiedInstance() throws Exception {
      Path out = Files.createTempDirectory("check_test").resolve("failing.json");
      ExpTestHelpers.eval(
          program("x", "{ options check_command_export_file = \"" + out + "\" }"),
          "chk",
          Kind.PRAGMA);
      assertFalse(Files.exists(out));
    }

    @Test
    void reportsAFileItCannotWrite() throws Exception {
      Path dir = Files.createTempDirectory("check_test");
      String msg =
          ExpTestHelpers.error(
              program("y", "{ options check_command_export_file = \"" + dir + "\" }"));
      assertTrue(msg.contains(dir.toString()), msg);
    }

    /**
     * The typing of a program skips commands, so the mismatch of schemas is found only when the
     * check runs, with a message about the query it evaluates.
     */
    @Test
    void reportsConstraintsOnAnotherSchemaWhenRun() throws Exception {
      String program =
          SCHEMA.replace("constraints C = literal : S", "constraints C0 = literal : S")
              + "instance I = literal : S { generators a : A b : B equations a.a1 = x b.b1 = x }\n"
              + "schema S2 = literal : Ty { entities A B attributes a1 : A -> String c1 : B -> String }\n"
              + "constraints C = literal : S2 { forall a:A -> exists b:B where a.a1 = b.c1 }\n"
              + "command chk = check C I\n";
      String msg = ExpTestHelpers.error(program);
      assertTrue(msg.startsWith("In eval instance, source of query is"), msg);
      PragmaExpCheck<?, ?> e = ExpTestHelpers.exp(program, "chk");
      AqlTyping g = ExpTestHelpers.typing(program);
      RuntimeException t = assertThrows(RuntimeException.class, () -> e.type(g));
      assertEquals("Schema of instance: S does not match constraint schema S2", t.getMessage());
    }

    /** Only the export file may be set in the command; the warning option must be global. */
    @Test
    void allowsOnlyTheExportFileOption() {
      LineException e =
          assertThrows(
              LineException.class,
              () ->
                  ExpTestHelpers.run(
                      program("x", "{ options check_warn_instead_of_fail = true }")));
      assertTrue(
          e.getMessage().startsWith("Option not allowed: check_warn_instead_of_fail."),
          e.getMessage());
    }
  }

  /** A check of a variable instance; the cast fixes the type parameters InstExpVar leaves open. */
  @SuppressWarnings("unchecked")
  private static PragmaExpCheck<Object, Object> check(
      InstExp<?, ?, ?, ?> i, EdsExp c, List<Pair<String, String>> ops) {
    return new PragmaExpCheck<>((InstExp<String, String, Object, Object>) i, c, ops);
  }

  @Nested
  class Expression {

    private final PragmaExpCheck<?, ?> exp =
        check(new InstExpVar("I"), new EdsExpVar("C"), List.of());

    @Test
    void isSkippedAtCompileTime() throws Exception {
      AqlEnv env = new AqlEnv(ExpTestHelpers.parse(""));
      assertThrows(IgnoreException.class, () -> exp.eval0(env, true));
    }

    @Test
    void printsAsItsKeyword() {
      assertEquals("check C I", exp.toString());
    }

    @Test
    void dependsOnTheConstraintsAndTheInstance() {
      assertEquals(
          Set.of(new Pair<>("C", Kind.CONSTRAINTS), new Pair<>("I", Kind.INSTANCE)),
          Set.copyOf(exp.deps()));
      assertTrue(exp.allowedOptions().contains(AqlOption.check_command_export_file));
      assertFalse(exp.allowedOptions().contains(AqlOption.check_warn_instead_of_fail));
    }

    @Test
    void mapsOverTheConstraintsAndTheInstance() {
      List<Exp<?>> seen = new ArrayList<>();
      exp.map(seen::add);
      assertEquals(List.of(exp, new EdsExpVar("C"), new InstExpVar("I")), seen);
    }

    @Test
    void equalityComparesEveryPart() {
      List<Pair<String, String>> ops = List.of(new Pair<>("timeout", "1"));
      assertEquals(exp, exp);
      assertEquals(exp, check(new InstExpVar("I"), new EdsExpVar("C"), List.of()));
      assertEquals(
          exp.hashCode(), check(new InstExpVar("I"), new EdsExpVar("C"), List.of()).hashCode());
      assertNotEquals(exp, check(new InstExpVar("J"), new EdsExpVar("C"), List.of()));
      assertNotEquals(exp, check(new InstExpVar("I"), new EdsExpVar("D"), List.of()));
      assertNotEquals(exp, check(new InstExpVar("I"), new EdsExpVar("C"), ops));
      assertNotEquals(exp, null);
      assertNotEquals(exp, "check C I");
    }

    @Test
    void equalityHandlesMissingParts() {
      PragmaExpCheck<?, ?> none = check(null, null, List.of());
      none.ops = null;
      PragmaExpCheck<?, ?> other = check(null, null, List.of());
      other.ops = null;
      assertEquals(none, other);
      assertEquals(none.hashCode(), other.hashCode());
      assertNotEquals(none, exp);
      assertNotEquals(exp, none);
      other.ops = Map.of();
      assertNotEquals(none, other);
      assertNotEquals(other, none);
      PragmaExpCheck<?, ?> noConstraints = check(new InstExpVar("I"), null, List.of());
      assertNotEquals(noConstraints, exp);
    }

    /** hashCode tests I, not ops, for null, so null options with an instance throw. */
    @Test
    void hashCodeChecksTheWrongFieldForNull() {
      PragmaExpCheck<?, ?> e = check(new InstExpVar("I"), new EdsExpVar("C"), List.of());
      e.ops = null;
      assertThrows(NullPointerException.class, e::hashCode);
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsAVisitor() {
      PragmaExpVisitor<String, Unit, RuntimeException> v = mock(PragmaExpVisitor.class);
      when(v.visit(Unit.unit, exp)).thenReturn("visited");
      assertEquals("visited", exp.accept(Unit.unit, v));
    }
  }
}
