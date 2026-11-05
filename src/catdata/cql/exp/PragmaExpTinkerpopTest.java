package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.Pair;
import catdata.Unit;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.Kind;
import catdata.cql.Pragma;
import catdata.cql.exp.PragmaExp.PragmaExpVisitor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PragmaExpTinkerpop}, {@code command c = exec_tinkerpop { "..." }}, which
 * evaluates each Gremlin script with the traversal source of the server bound, and prints each
 * result by its Java class.
 */
class PragmaExpTinkerpopTest {

  private static TinkerpopTestHelpers server;

  @BeforeAll
  static void startServer() throws Exception {
    server = TinkerpopTestHelpers.start();
  }

  @AfterAll
  static void stopServer() {
    server.close();
  }

  private static String exec(String... scripts) throws Exception {
    StringBuilder sb = new StringBuilder(server.options() + "command c = exec_tinkerpop {");
    for (String s : scripts) {
      sb.append(" \"").append(s).append('"');
    }
    Pragma p = ExpTestHelpers.eval(sb.append(" }").toString(), "c", Kind.PRAGMA);
    return p.toString();
  }

  @Test
  void printsEachResultByItsClass() throws Exception {
    String out = exec("1 + 1", "[1, 2]", "[1, 2] as Object[]", "g.V().count()");
    assertEquals(
        """
        Exec 1 + 1:
        Java class: class java.lang.Integer
        2Exec [1, 2]:
        Java class: Iterable
        1
        2Exec [1, 2] as Object[]:
        Java class: Array
        [1, 2]Exec g.V().count():
        Java class: GraphTraversal
        0  [ class java.lang.Long]

        """,
        out);
  }

  @Test
  void reportsAScriptThatFails() throws Exception {
    String out = exec("nope()");
    assertTrue(out.startsWith("\n\n"), out);
    assertTrue(out.contains("nope"), out);
  }

  @Test
  void isSkippedAtCompileTime() throws Exception {
    PragmaExpTinkerpop e = new PragmaExpTinkerpop(List.of("1"), List.of());
    AqlEnv env = new AqlEnv(ExpTestHelpers.parse(""));
    assertThrows(IgnoreException.class, () -> e.eval0(env, true));
  }

  @Test
  void runsOnce() throws Exception {
    PragmaExpTinkerpop e =
        new PragmaExpTinkerpop(
            List.of("1"), List.of(new Pair<>("tinkerpop_port", Integer.toString(server.port))));
    Pragma p = e.eval0(new AqlEnv(ExpTestHelpers.parse("")), false);
    p.execute();
    String first = p.toString();
    p.execute();
    assertEquals(first, p.toString());
  }

  @Test
  void printsItsScriptsAndOptions() {
    assertEquals(
        "exec_tinkerpop {\"a\"\"b\"}",
        new PragmaExpTinkerpop(List.of("a", "b"), List.of()).toString());
    // The separators are swapped, so the printed options do not parse
    assertEquals(
        "exec_tinkerpop {\"a\"\n\toptionstinkerpop_port\n\t\t1}",
        new PragmaExpTinkerpop(List.of("a"), List.of(new Pair<>("tinkerpop_port", "1")))
            .toString());
  }

  @Test
  void hasNoDependenciesAndAllowsTheTinkerpopOptions() {
    PragmaExpTinkerpop e =
        new PragmaExpTinkerpop(List.of("a"), List.of(new Pair<>("tinkerpop_port", "1")));
    assertTrue(e.deps().isEmpty());
    assertEquals(Map.of("tinkerpop_port", "1"), e.options());
    assertTrue(
        e.allowedOptions()
            .containsAll(
                Set.of(
                    AqlOption.tinkerpop_host,
                    AqlOption.tinkerpop_port,
                    AqlOption.tinkerpop_graph_name)));
    List<Exp<?>> seen = new ArrayList<>();
    e.map(seen::add);
    assertEquals(List.of(e), seen);
  }

  @Test
  void equalityComparesTheScriptsAndOptions() {
    PragmaExpTinkerpop a = new PragmaExpTinkerpop(List.of("a"), List.of());
    assertEquals(a, a);
    assertEquals(a, new PragmaExpTinkerpop(List.of("a"), List.of()));
    assertEquals(a.hashCode(), new PragmaExpTinkerpop(List.of("a"), List.of()).hashCode());
    assertNotEquals(a, new PragmaExpTinkerpop(List.of("b"), List.of()));
    assertNotEquals(a, new PragmaExpTinkerpop(List.of("a"), List.of(new Pair<>("timeout", "1"))));
    assertNotEquals(a, new PragmaExpTinkerpop(null, List.of()));
    assertNotEquals(new PragmaExpTinkerpop(null, List.of()), a);
    assertEquals(new PragmaExpTinkerpop(null, List.of()), new PragmaExpTinkerpop(null, List.of()));
    assertNotEquals(a, null);
    assertNotEquals(a, "exec_tinkerpop");
  }

  @Test
  @SuppressWarnings("unchecked")
  void acceptsAVisitor() {
    PragmaExpTinkerpop e = new PragmaExpTinkerpop(List.of("a"), List.of());
    PragmaExpVisitor<String, Unit, RuntimeException> v = mock(PragmaExpVisitor.class);
    when(v.visit(Unit.unit, e)).thenReturn("visited");
    assertEquals("visited", e.accept(Unit.unit, v));
  }
}
