package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.Pair;
import catdata.Unit;
import catdata.cql.Constraints;
import catdata.cql.Kind;
import catdata.cql.Pragma;
import catdata.cql.exp.EdsExp.EdsExpVar;
import catdata.cql.exp.PragmaExp.PragmaExpVisitor;
import catdata.cql.exp.QueryExp.QueryExpVar;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PragmaExpCheck2}, {@code command c = check_query Q C D}, which asks the E
 * theorem prover whether the constraints {@code D} on the target of {@code Q}, pulled back along
 * {@code Q}, follow from the constraints {@code C} on its source. The tests stand in for E with a
 * script that prints one of the lines E ends with.
 */
class PragmaExpCheck2Test {

  private static final String PROGRAM =
      """
      options num_threads = 1 %s
      typeside Ty = literal { types String }
      schema S = literal : Ty { entities A E foreign_keys f g : A -> E path_equations A.f = A.g }
      constraints C = literal : S { }
      schema T = literal : Ty { entities B }
      constraints D = literal : T { %s }
      query Q = literal : S -> T { entity B -> { from a:A } }
      command q = check_query Q C D
      """;

  private static final String D = "forall b:B -> where b = b";

  /** A stand-in for E that prints the given line. */
  private static String prover(String line) throws Exception {
    Path p = Files.createTempFile("eprover", ".sh");
    Files.writeString(p, "#!/bin/sh\necho '" + line + "'\n");
    Files.setPosixFilePermissions(p, PosixFilePermissions.fromString("rwx------"));
    return "e_path = \"" + p + "\"";
  }

  private static String program(String options, String d) {
    return PROGRAM.formatted(options, d);
  }

  @Nested
  class Evaluation {

    @Test
    void printsTheConstraintsBothWaysWhenTheyFollow() throws Exception {
      Pragma p = ExpTestHelpers.eval(program(prover("# Proof found!"), D), "q", Kind.PRAGMA);
      assertTrue(p.toString().startsWith("Success\n\n"), p.toString());
      assertTrue(p.toString().contains("\n\n\n==>\n\n\n"), p.toString());
    }

    @Test
    void doesNothingForNoConstraints() throws Exception {
      Pragma p = ExpTestHelpers.eval(program(prover("# Proof found!"), ""), "q", Kind.PRAGMA);
      assertEquals("", p.toString());
    }

    @Test
    void failsWhenAConstraintDoesNotFollow() throws Exception {
      String msg = ExpTestHelpers.error(program(prover("# No proof found!"), D));
      assertTrue(msg.contains("Failed: "), msg);
    }

    @Test
    void failsWhenTheProverRunsOut() throws Exception {
      String msg = ExpTestHelpers.error(program(prover("# Failure: resource limit"), D));
      assertTrue(msg.contains("Out of resources"), msg);
    }

    @Test
    void failsWhenTheProverSaysNothing() throws Exception {
      String msg = ExpTestHelpers.error(program(prover("hello"), D));
      assertTrue(msg.contains("0Internal theorem prover anomaly."), msg);
    }

    @Test
    void needsTheProver() throws Exception {
      String msg = ExpTestHelpers.error(program("e_path = \"/nonexistent/eprover\"", D));
      assertTrue(msg.contains("File does not exist: /nonexistent/eprover"), msg);
    }

    /**
     * The path equation of S is added to the constraints C themselves, so after the command C shows
     * a constraint the program does not declare.
     */
    @Test
    void addsTheSchemaEquationsToTheSourceConstraints() throws Exception {
      AqlMultiDriver d = ExpTestHelpers.run(program(prover("# Proof found!"), D));
      Constraints c = ExpTestHelpers.value(d, "C", Kind.CONSTRAINTS);
      assertEquals(1, c.eds.size());
      assertTrue(c.toString().contains("v.f = v.g"), c.toString());
    }

    @Test
    void cannotValidateAggregation() throws Exception {
      String program =
          "options allow_aggregation_unsafe = true num_threads = 1 "
              + prover("# Proof found!")
              + "\n"
              + """
              typeside Ty = literal {
                external_types Int -> "java.lang.Integer"
                external_parsers Int -> "x => java.lang.Integer.parseInt(x)"
                external_functions plus : Int, Int -> Int = "(x, y) => x + y"
              }
              schema S = literal : Ty { entities A attributes n : A -> Int }
              constraints C = literal : S { }
              schema T = literal : Ty { entities B attributes total : B -> Int }
              constraints D = literal : T { forall b:B -> where b = b }
              query Q = literal : S -> T {
                entity B -> { from attributes total -> from a:A return a.n aggregate "0" lambda x y. plus(x, y) }
              }
              command q = check_query Q C D
              """;
      String msg = ExpTestHelpers.error(program);
      assertTrue(msg.contains("Cannot validate aggregation"), msg);
    }
  }

  @Nested
  class Expression {

    private final PragmaExpCheck2 exp =
        new PragmaExpCheck2(new QueryExpVar("Q"), new EdsExpVar("C"), new EdsExpVar("D"));

    @Test
    void isTypedWhenTheSchemasMatch() throws Exception {
      AqlTyping g = ExpTestHelpers.typing(program("", D));
      assertEquals(Unit.unit, exp.type(g));
    }

    @Test
    void rejectsConstraintsOnOtherSchemas() throws Exception {
      AqlTyping g = ExpTestHelpers.typing(program("", D));
      PragmaExpCheck2 swapped =
          new PragmaExpCheck2(new QueryExpVar("Q"), new EdsExpVar("D"), new EdsExpVar("D"));
      assertEquals(
          "Source of query: S does not match constraint schema T",
          assertThrows(RuntimeException.class, () -> swapped.type(g)).getMessage());
      PragmaExpCheck2 target =
          new PragmaExpCheck2(new QueryExpVar("Q"), new EdsExpVar("C"), new EdsExpVar("C"));
      assertEquals(
          "Target of query: T does not match constraint schema S",
          assertThrows(RuntimeException.class, () -> target.type(g)).getMessage());
    }

    @Test
    void printsAsItsKeyword() {
      assertEquals("check_query Q C D", exp.toString());
    }

    @Test
    void dependsOnTheQueryAndBothConstraints() {
      assertEquals(
          Set.of(
              new Pair<>("Q", Kind.QUERY),
              new Pair<>("C", Kind.CONSTRAINTS),
              new Pair<>("D", Kind.CONSTRAINTS)),
          Set.copyOf(exp.deps()));
      assertTrue(exp.options().isEmpty());
      assertEquals(ExpTestHelpers.BASE_OPTIONS, exp.allowedOptions());
    }

    @Test
    void mapsOverItsParts() {
      List<Exp<?>> seen = new ArrayList<>();
      exp.map(seen::add);
      assertEquals(
          List.of(exp, new QueryExpVar("Q"), new EdsExpVar("C"), new EdsExpVar("D")), seen);
    }

    @Test
    void equalityComparesEveryPart() {
      assertEquals(exp, exp);
      assertEquals(
          exp, new PragmaExpCheck2(new QueryExpVar("Q"), new EdsExpVar("C"), new EdsExpVar("D")));
      assertEquals(
          exp.hashCode(),
          new PragmaExpCheck2(new QueryExpVar("Q"), new EdsExpVar("C"), new EdsExpVar("D"))
              .hashCode());
      assertNotEquals(
          exp, new PragmaExpCheck2(new QueryExpVar("R"), new EdsExpVar("C"), new EdsExpVar("D")));
      assertNotEquals(
          exp, new PragmaExpCheck2(new QueryExpVar("Q"), new EdsExpVar("X"), new EdsExpVar("D")));
      assertNotEquals(
          exp, new PragmaExpCheck2(new QueryExpVar("Q"), new EdsExpVar("C"), new EdsExpVar("X")));
      PragmaExpCheck2 none = new PragmaExpCheck2(null, null, null);
      assertEquals(none, new PragmaExpCheck2(null, null, null));
      assertEquals(none.hashCode(), new PragmaExpCheck2(null, null, null).hashCode());
      assertNotEquals(none, exp);
      assertNotEquals(exp, none);
      assertNotEquals(new PragmaExpCheck2(null, new EdsExpVar("C"), null), exp);
      assertNotEquals(new PragmaExpCheck2(null, new EdsExpVar("C"), new EdsExpVar("D")), exp);
      assertNotEquals(exp, null);
      assertNotEquals(exp, "check_query");
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
