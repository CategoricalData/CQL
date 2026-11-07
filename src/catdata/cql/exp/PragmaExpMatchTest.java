package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.Pair;
import catdata.Unit;
import catdata.cql.Kind;
import catdata.cql.Pragma;
import catdata.cql.exp.GraphExp.GraphExpVar;
import catdata.cql.exp.PragmaExp.PragmaExpVisitor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PragmaExpMatch}, {@code match "naive" : G -> H}, the command that prints
 * the best match of the nodes and edges of one graph onto another, found by the naive or the
 * similarity flooding matcher.
 */
class PragmaExpMatchTest {

  private static final String GRAPHS =
      """
      graph G = literal { nodes a b edges e : a -> b }
      graph H = literal { nodes c d edges k : c -> d }
      """;

  private static String program(String method) {
    return GRAPHS + "command M = match \"" + method + "\" : G -> H\n";
  }

  private static PragmaExpMatch exp(String method, String src, String dst) {
    return new PragmaExpMatch(method, new GraphExpVar(src), new GraphExpVar(dst), List.of());
  }

  @Nested
  class Evaluation {

    @Test
    void printsTheBestNaiveMatch() throws Exception {
      Pragma p = ExpTestHelpers.eval(program("naive"), "M", Kind.PRAGMA);
      String s = p.toString();
      assertFalse(s.isEmpty());
      assertSame(s, p.toString());
    }

    /**
     * The similarity flooding matcher is unfinished: it fails on a null group when the graphs share
     * no edge label, and on a stub when they do.
     */
    @Test
    void failsWithTheSimilarityFloodingMatcher() throws Exception {
      String none = ExpTestHelpers.error(program("sf"));
      assertTrue(none.contains("because \"maxpr\" is null"), none);
      String shared = ExpTestHelpers.error(program("sf").replace("k : c -> d", "e : c -> d"));
      assertTrue(shared.contains("Todo - serena"), shared);
    }

    @Test
    void rejectsAnUnknownMatcher() throws Exception {
      String msg = ExpTestHelpers.error(program("best"));
      assertTrue(msg.contains("Please use naive or sf for String match desired, not best"), msg);
    }

    /** The first call fails and keeps the message, which every later call returns. */
    @Test
    void keepsTheMessageOfAFailedMatch() throws Exception {
      AqlEnv env = ExpTestHelpers.run(GRAPHS).env;
      Pragma p = exp("best", "G", "H").eval0(env, false);
      RuntimeException e = assertThrows(RuntimeException.class, p::toString);
      assertEquals(e.getMessage(), p.toString());
    }

    @Test
    void isNotEvaluatedAtCompileTime() throws Exception {
      AqlEnv env = ExpTestHelpers.run(GRAPHS).env;
      assertThrows(IgnoreException.class, () -> exp("naive", "G", "H").eval0(env, true));
    }
  }

  @Nested
  class Expression {

    private final PragmaExpMatch exp = exp("naive", "G", "H");

    @Test
    void printsTheMatcherAndTheGraphs() {
      assertEquals("match \"naive\" : G -> H", exp.toString());
    }

    @Test
    void dependsOnBothGraphs() throws Exception {
      assertEquals(
          Set.of(new Pair<>("G", Kind.GRAPH), new Pair<>("H", Kind.GRAPH)), Set.copyOf(exp.deps()));
      assertEquals(Unit.unit, exp.type(ExpTestHelpers.typing(GRAPHS)));
      assertTrue(exp.options().isEmpty());
      assertEquals(ExpTestHelpers.BASE_OPTIONS, exp.allowedOptions());
      List<Exp<?>> seen = new ArrayList<>();
      exp.map(seen::add);
      assertEquals(List.of(exp, new GraphExpVar("G"), new GraphExpVar("H")), seen);
    }

    @Test
    void equalityComparesEveryPart() {
      assertEquals(exp, exp);
      assertEquals(exp, exp("naive", "G", "H"));
      assertEquals(exp.hashCode(), exp("naive", "G", "H").hashCode());
      assertNotEquals(exp, exp("sf", "G", "H"));
      assertNotEquals(exp, exp("naive", "K", "H"));
      assertNotEquals(exp, exp("naive", "G", "K"));
      assertNotEquals(
          exp,
          new PragmaExpMatch(
              "naive",
              new GraphExpVar("G"),
              new GraphExpVar("H"),
              List.of(new Pair<>("timeout", "1"))));
      PragmaExpMatch none = new PragmaExpMatch(null, null, null, List.of());
      assertEquals(none, new PragmaExpMatch(null, null, null, List.of()));
      assertEquals(none.hashCode(), new PragmaExpMatch(null, null, null, List.of()).hashCode());
      assertNotEquals(none, exp);
      assertNotEquals(exp, none);
      assertNotEquals(
          new PragmaExpMatch(null, new GraphExpVar("G"), new GraphExpVar("H"), List.of()), exp);
      assertNotEquals(new PragmaExpMatch("naive", null, new GraphExpVar("H"), List.of()), exp);
      assertNotEquals(exp, null);
      assertNotEquals(exp, "match");
      assertEquals(Map.of(), none.options());
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
