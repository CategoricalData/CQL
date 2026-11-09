package catdata.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.Pair;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ShortestPath}, Dijkstra's algorithm with unit weights on a multigraph: s
 * reaches v through a in two steps, or directly; w hangs off v; z is unreachable.
 */
class ShortestPathTest {

  private static DMG<String, String> graph() {
    Map<String, Pair<String, String>> edges = new LinkedHashMap<>();
    edges.put("sa", new Pair<>("s", "a"));
    edges.put("av", new Pair<>("a", "v"));
    edges.put("sv", new Pair<>("s", "v"));
    edges.put("vw", new Pair<>("v", "w"));
    edges.put("wa", new Pair<>("w", "a"));
    return new DMG<>(List.of("s", "a", "v", "w", "z"), edges);
  }

  @Test
  void findsTheShortestPathToEachNode() {
    var g = graph();
    var sp = new ShortestPath<>(g, "s");
    assertEquals(0.0, sp.distTo("s"));
    assertEquals(1.0, sp.distTo("v"));
    assertEquals(2.0, sp.distTo("w"));
    assertEquals(List.of("sv", "vw"), sp.pathTo("w"));
    assertEquals(new Pair<>("s", "w"), g.type("s", sp.pathTo("w")));
    assertEquals(List.of(), sp.pathTo("s"));
  }

  @Test
  void reportsAnUnreachableNode() {
    var sp = new ShortestPath<>(graph(), "s");
    assertFalse(sp.hasPathTo("z"));
    assertTrue(sp.hasPathTo("a"));
    assertEquals(Double.POSITIVE_INFINITY, sp.distTo("z"));
    assertNull(sp.pathTo("z"));
  }

  /** From a node on the cycle a, v, w, every node of the cycle is reached and s is not. */
  @Test
  void followsACycleFromInsideIt() {
    var sp = new ShortestPath<>(graph(), "w");
    assertEquals(List.of("wa", "av"), sp.pathTo("v"));
    assertFalse(sp.hasPathTo("s"));
  }
}
