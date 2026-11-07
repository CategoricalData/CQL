package catdata.provers;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.cql.exp.AqlMultiDriver;
import catdata.cql.exp.AqlParserFactory;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ProgramProver}, behind {@code prover = program}, which accepts only
 * left-linear theories whose rules do not overlap. The definitional simplification that runs first
 * hides some overlaps, so the last test turns it off.
 */
class ProgramProverTest {

  private static String error(String typeside) throws Exception {
    AqlMultiDriver d =
        new AqlMultiDriver(
            AqlParserFactory.getParser()
                .parseProgram("typeside Ty = literal { types T " + typeside + " }\n"),
            null);
    d.start();
    return d.env.exn == null ? null : d.env.exn.getMessage();
  }

  @Test
  void acceptsAProgram() throws Exception {
    assertNull(
        error(
            "constants a b : T functions g : T -> T equations g(a) = b forall x. g(g(x)) = x"
                + " options prover = program"));
  }

  @Test
  void rejectsADuplicatedVariableOnTheLeft() throws Exception {
    String msg =
        error(
            "constants a : T functions f : T, T -> T equations forall x. f(x, x) = x"
                + " options prover = program");
    assertTrue(
        msg.contains("Not left linear (cotains duplicated variable on lhs): (x f x) = x"), msg);
  }

  /**
   * With simplification, g(x) = c defines g and the overlap goes away. Which equation the message
   * names first depends on the order of a hash set.
   */
  @Test
  void rejectsOverlappingRules() throws Exception {
    String typeside =
        "constants a b c : T functions g : T -> T equations g(a) = b forall x. g(x) = c"
            + " options prover = program";
    assertNull(error(typeside));
    String msg = error(typeside + " prover_simplify_max = 0");
    assertTrue(msg.contains("Is not a program: equation "), msg);
    assertTrue(msg.contains("g(a) = b") && msg.contains("g(x) = c"), msg);
    assertTrue(msg.contains("the critical pair is "), msg);
  }
}
