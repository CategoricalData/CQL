package catdata.cql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import catdata.Pair;
import catdata.graph.DMG;
import org.junit.jupiter.api.Test;

/** The value of a {@code graph} definition. */
class GraphTest {

  private static DMG<String, String> dmg(
      Set<String> nodes, Map<String, Pair<String, String>> edges) {
    return new DMG<>(nodes, edges);
  }

  /** a -f-> b and a -g-> b. */
  private static Graph<String, String> parallel() {
    return new Graph<>(
        dmg(Set.of("a", "b"), Map.of("f", new Pair<>("a", "b"), "g", new Pair<>("a", "b"))));
  }

  @Test
  void countsNodesAndEdges() {
    Graph<String, String> g = parallel();
    assertEquals(Kind.GRAPH, g.kind());
    assertEquals(4, g.size());
    assertEquals(g.dmg.toString(), g.toString());
  }

  @Test
  void comparesTheGraphs() {
    Graph<String, String> g = parallel();
    assertEquals(g, parallel());
    assertEquals(g.hashCode(), parallel().hashCode());
    assertNotEquals(g, new Graph<>(dmg(Set.of("a", "b"), Map.of("f", new Pair<>("a", "b")))));
    assertTrue(g.equals(g));
    assertFalse(g.equals(null));
    assertFalse(g.equals(g.dmg));
  }

  @Test
  void comparesAMissingGraph() {
    Graph<String, String> none = new Graph<>(null);
    assertEquals(none, new Graph<String, String>(null));
    assertEquals(none.hashCode(), new Graph<String, String>(null).hashCode());
    assertNotEquals(none, parallel());
    assertNotEquals(parallel(), none);
  }
}
