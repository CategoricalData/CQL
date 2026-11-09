package catdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link BinRelMap}, a binary relation stored by its left elements, which nothing
 * uses.
 */
class BinRelMapTest {

  private static BinRelMap<String, Integer> rel() {
    var r = new BinRelMap<String, Integer>();
    assertTrue(r.add("a", 1));
    assertFalse(r.add("a", 1));
    r.add("a", 2);
    r.add("b", 3);
    return r;
  }

  @Test
  void storesAndRemovesPairs() {
    var r = rel();
    assertEquals(3, r.size());
    assertEquals(Set.of("a", "b"), Set.copyOf(r.keySet()));
    assertEquals(Set.of(1, 2), Set.copyOf(r.get("a")));
    assertTrue(r.containsKey("b"));
    r.remove("a", 1);
    r.remove("c", 1);
    assertEquals(2, r.size());
    r.remove("b");
    assertTrue(r.get("b").isEmpty());
    r.add("c");
    assertThrows(RuntimeException.class, () -> r.add("c"));
    assertFalse(r.isEmpty());
    assertTrue(new BinRelMap<String, Integer>().isEmpty());
    assertTrue(r.toString().startsWith("BinRelMap [R="), r.toString());
  }

  @Test
  void composesCopiesAndIterates() {
    var r = rel();
    var s = new BinRelMap<Integer, String>();
    s.add(1, "x");
    s.add(3, "y");
    var rs = r.compose(s);
    assertEquals(Set.of("x"), Set.copyOf(rs.get("a")));
    assertEquals(Set.of("y"), Set.copyOf(rs.get("b")));
    var copy = new BinRelMap<>(r);
    assertEquals(r, copy);
    assertEquals(r.hashCode(), copy.hashCode());
    assertEquals(r, r);
    assertNotEquals(r, null);
    assertNotEquals(r, "rel");
    copy.add("z", 9);
    assertNotEquals(r, copy);
    var all = new BinRelMap<String, Integer>();
    all.addAll(r);
    all.addAll(r);
    assertEquals(r, all);
    List<Pair<String, Integer>> pairs = new ArrayList<>();
    r.forEach(pairs::add);
    assertEquals(3, pairs.size());
    assertTrue(pairs.contains(new Pair<>("b", 3)));
  }

  /** {@code toRel} looks the set of each left element up without creating it, so any pair fails. */
  @Test
  void cannotBeBuiltFromPairs() {
    assertTrue(new BinRelMap<String, Integer>(Set.of()).isEmpty());
    assertThrows(NullPointerException.class, () -> new BinRelMap<>(Set.of(new Pair<>("a", 1))));
  }
}
