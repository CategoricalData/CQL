package catdata.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link UnionFindClassic}, weighted union-find with path compression. */
class UnionFindClassicTest {

  @Test
  void mergesClassesAndReportsThem() {
    var uf = new UnionFindClassic<>(4, List.of("a", "b", "c", "d"));
    assertFalse(uf.connected("a", "b"));
    uf.union("a", "b");
    uf.union("c", "a");
    uf.union("a", "c");
    assertTrue(uf.connected("b", "c"));
    assertFalse(uf.connected("a", "d"));
    assertEquals(Set.of("a", "b", "c"), uf.eqc("b"));
    assertEquals(Set.of("d"), uf.eqc("d"));
    Map<String, Set<String>> m = uf.toMap();
    assertEquals(Set.of("a", "b", "c", "d"), m.keySet());
    assertEquals(Set.of("a", "b", "c"), m.get("c"));
    assertTrue(uf.toString().startsWith("UnionFindClassic [parent="), uf.toString());
  }

  /** An element it has not seen is added as a class of its own. */
  @Test
  void addsAnUnknownElement() {
    var uf = new UnionFindClassic<>(1, List.of("a"));
    assertEquals("z", uf.find("z"));
    assertEquals(Set.of("a", "z"), Set.copyOf(uf.values()));
    uf.union("z", "a");
    assertEquals(uf.find("a"), uf.find("z"));
  }

  /** The smaller class is attached to the larger, whichever side of the union it is on. */
  @Test
  void attachesTheSmallerClassToTheLarger() {
    var uf = new UnionFindClassic<>(3, List.of("a", "b", "c"));
    uf.union("a", "b");
    uf.union("c", "a");
    assertEquals(uf.find("a"), uf.find("c"));
    assertEquals(uf.find("b"), uf.find("c"));
  }
}
