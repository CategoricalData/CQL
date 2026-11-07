package catdata.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.Pair;
import catdata.Triple;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link DMG}, a directed multigraph: x -g-> y -h-> z, and a second g2 : x -> y. */
class DMGTest {

  private static DMG<String, String> graph() {
    return new DMG<>(
        List.of("x", "y", "z"),
        Map.of("g", new Pair<>("x", "y"), "g2", new Pair<>("x", "y"), "h", new Pair<>("y", "z")));
  }

  @Test
  void typesAPathOrSaysWhereItBreaks() {
    var d = graph();
    assertEquals(new Pair<>("x", "z"), d.type("x", List.of("g", "h")));
    assertEquals(new Pair<>("y", "y"), d.type("y", List.of()));
    RuntimeException e = assertThrows(RuntimeException.class, () -> d.type("x", List.of("k")));
    assertEquals("Not an edge: k", e.getMessage());
    e = assertThrows(RuntimeException.class, () -> d.type("x", List.of("h")));
    assertEquals("Ill-typed: [h], edge h has source y but is applied to x", e.getMessage());
    assertEquals(Set.of("g", "g2"), Set.copyOf(d.edges("x", "y")));
  }

  @Test
  void isBuiltFromTriplesAndComparedByItsParts() {
    var d = graph();
    var t =
        new DMG<>(
            List.of("x", "y", "z"),
            Set.of(
                new Triple<>("g", "x", "y"),
                new Triple<>("g2", "x", "y"),
                new Triple<>("h", "y", "z")));
    assertEquals(d.edges, t.edges);
    assertEquals(d, d);
    assertNotEquals(d, null);
    assertNotEquals(d, "graph");
    assertNotEquals(d, new DMG<>(List.of("x"), Map.of()));
    assertTrue(d.toString().contains("x"), d.toString());
    RuntimeException e =
        assertThrows(
            RuntimeException.class,
            () -> new DMG<>(List.of("x"), Map.of("g", new Pair<>("x", "w"))));
    assertEquals("Not a node: w", e.getMessage());
    e =
        assertThrows(
            RuntimeException.class,
            () -> new DMG<>(List.of("x"), Map.of("g", new Pair<>("w", "x"))));
    assertEquals("Not a node: w", e.getMessage());
  }
}
