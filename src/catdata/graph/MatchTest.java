package catdata.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.Pair;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Match}, a map of the nodes of one graph to the nodes of another and of its
 * edges to paths. The source has a -f-> b; the target has x -g-> y -h-> z.
 */
class MatchTest {

  private static final DMG<String, String> SRC =
      new DMG<>(List.of("a", "b"), Map.of("f", new Pair<>("a", "b")));

  private static final DMG<String, String> DST =
      new DMG<>(
          List.of("x", "y", "z"), Map.of("g", new Pair<>("x", "y"), "h", new Pair<>("y", "z")));

  private static Match<String, String, String, String> match(
      Map<String, String> nodes, Map<String, List<String>> edges) {
    return new Match<>(SRC, DST, nodes, edges);
  }

  @Test
  void mapsEdgesToPathsAndComparesByMaps() {
    var m = match(Map.of("a", "x", "b", "z"), Map.of("f", List.of("g", "h")));
    var n = match(Map.of("a", "x", "b", "z"), Map.of("f", List.of("g", "h")));
    assertEquals(m, m);
    assertEquals(m, n);
    assertEquals(m.hashCode(), n.hashCode());
    assertNotEquals(m, null);
    assertNotEquals(m, "match");
    assertNotEquals(m, match(Map.of("a", "y", "b", "z"), Map.of("f", List.of("h"))));
    assertTrue(m.toString().contains("g"), m.toString());
  }

  /** The image of f is only checked to compose from x, not to end at the image of b. */
  @Test
  void acceptsAPathThatEndsAwayFromTheImageOfTheTarget() {
    var m = match(Map.of("a", "x", "b", "z"), Map.of("f", List.of("g")));
    assertTrue(m.toString().contains("g"), m.toString());
    assertEquals(new Pair<>("x", "y"), DST.type("x", List.of("g")));
  }

  @Test
  void rejectsAnIncompleteOrForeignMapping() {
    String msg =
        assertThrows(RuntimeException.class, () -> match(Map.of("a", "x"), Map.of())).getMessage();
    assertTrue(msg.endsWith("source node b has no mapping"), msg);
    msg =
        assertThrows(
                RuntimeException.class,
                () -> match(Map.of("a", "x", "b", "w"), Map.of("f", List.of())))
            .getMessage();
    assertTrue(msg.endsWith("source node b maps to w, which is not in target"), msg);
    msg =
        assertThrows(
                RuntimeException.class,
                () -> match(Map.of("a", "x", "b", "y", "c", "z"), Map.of("f", List.of("g"))))
            .getMessage();
    assertTrue(msg.endsWith("there is a mapping for c which is not a source node"), msg);
    msg =
        assertThrows(RuntimeException.class, () -> match(Map.of("a", "x", "b", "y"), Map.of()))
            .getMessage();
    assertTrue(msg.endsWith("source edge f has no mapping"), msg);
    msg =
        assertThrows(
                RuntimeException.class,
                () -> match(Map.of("a", "x", "b", "y"), Map.of("f", List.of("g"), "e", List.of())))
            .getMessage();
    assertTrue(msg.endsWith("there is a mapping for e which is not a source edge"), msg);
  }
}
