package catdata.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link PreOrder}, which enumerates the total preorders of a list of elements. */
class PreOrderTest {

  /**
   * The total preorders of n elements are counted by the Fubini numbers 1, 3, 13, 75. The
   * transitive closure adds pairs to the set it is iterating over, so it stops early on three
   * elements, leaving all 27 total relations, and fails on four.
   */
  @Test
  void enumeratesTheTotalRelationsWithoutClosingThem() {
    assertEquals(1, PreOrder.allTotal(List.of("a")).size());
    assertEquals(3, PreOrder.allTotal(List.of("a", "b")).size());
    assertEquals(27, PreOrder.allTotal(List.of("a", "b", "c")).size());
    assertThrows(
        ConcurrentModificationException.class,
        () -> PreOrder.allTotal(List.of("a", "b", "c", "d")));
  }

  /**
   * Every relation is total and reflexive, but some of those on three elements are not transitive.
   */
  @Test
  void givesTotalRelationsThatAreNotAllTransitive() {
    List<String> xs = List.of("a", "b", "c");
    var all = PreOrder.allTotal(xs);
    assertEquals(all.size(), new HashSet<>(all).size());
    int transitive = 0;
    for (PreOrder<String> p : all) {
      boolean closed = true;
      for (String x : xs) {
        assertTrue(p.gte(x, x));
        for (String y : xs) {
          assertTrue(p.gte(x, y) || p.gte(y, x), p.toString());
          for (String z : xs) {
            closed &= !(p.gte(x, y) && p.gte(y, z)) || p.gte(x, z);
          }
        }
      }
      transitive += closed ? 1 : 0;
    }
    assertEquals(13, transitive);
  }

  @Test
  void comparesAndCopiesByItsPairs() {
    Set<PreOrder<String>> all = new HashSet<>(PreOrder.allTotal(List.of("a", "b")));
    PreOrder<String> p = all.iterator().next();
    PreOrder<String> copy = new PreOrder<>(p);
    assertEquals(p, p);
    assertEquals(p, copy);
    assertEquals(p.hashCode(), copy.hashCode());
    assertNotEquals(p, null);
    assertNotEquals(p, "preorder");
    for (PreOrder<String> q : all) {
      if (q != p) {
        assertNotEquals(p, q);
      }
    }
    assertTrue(p.toString().startsWith("PreOrder [g="), p.toString());
  }
}
