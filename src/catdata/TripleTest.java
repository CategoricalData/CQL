package catdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link Triple} and {@link Quad}. */
class TripleTest {

  @Test
  void ordersComponentByComponent() {
    var t = new Triple<>(1, "b", 2.0);
    assertEquals(0, t.compareTo(new Triple<>(1, "b", 2.0)));
    assertTrue(t.compareTo(new Triple<>(2, "a", 0.0)) < 0);
    assertTrue(t.compareTo(new Triple<>(1, "a", 9.0)) > 0);
    assertTrue(t.compareTo(new Triple<>(1, "b", 3.0)) < 0);
  }

  @Test
  void projectsAndCompares() {
    Map<String, Triple<Integer, String, Boolean>> m = new LinkedHashMap<>();
    m.put("k", new Triple<>(1, "x", true));
    var p = Triple.proj(m);
    assertEquals(Map.of("k", 1), p.first);
    assertEquals(Map.of("k", "x"), p.second);
    assertEquals(Map.of("k", true), p.third);
    var t = new Triple<>(1, "x", true);
    assertEquals(new Pair<>(1, "x"), t.first2());
    assertEquals(t, t);
    assertNotEquals(t, null);
    assertNotEquals(t, "triple");
    assertNotEquals(t, new Triple<>(1, "x", false));
    assertNotEquals(t, new Triple<>(1, "y", true));
    assertNotEquals(t, new Triple<>(2, "x", true));
    assertNotEquals(new Triple<>(null, "x", true), t);
    assertEquals(new Triple<>(null, null, null), new Triple<>(null, null, null));
  }

  @Test
  void dropsTheFirstOfFourAndCompares() {
    var q = new Quad<>(1, "a", 2, "b");
    assertEquals(new Triple<>("a", 2, "b"), q.last3());
    assertEquals(q, q);
    assertEquals(q, new Quad<>(1, "a", 2, "b"));
    assertNotEquals(q, null);
    assertNotEquals(q, "quad");
    assertNotEquals(q, new Quad<>(1, "a", 2, "c"));
    assertNotEquals(q, new Quad<>(1, "a", 3, "b"));
    assertNotEquals(q, new Quad<>(1, "z", 2, "b"));
    assertNotEquals(q, new Quad<>(0, "a", 2, "b"));
    assertEquals(new Quad<>(null, null, null, null), new Quad<>(null, null, null, null));
    assertNotEquals(new Quad<>(null, "a", 2, "b"), q);
  }
}
