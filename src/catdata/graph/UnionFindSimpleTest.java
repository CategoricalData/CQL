package catdata.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link UnionFindSimple}, quick-find union-find that counts its classes. */
class UnionFindSimpleTest {

  @Test
  void countsTheClassesItMerges() {
    var uf = new UnionFindSimple<>(List.of("a", "b", "c"));
    assertEquals(3, uf.count());
    uf.union("a", "b");
    assertEquals(2, uf.count());
    uf.union("b", "a");
    assertEquals(2, uf.count());
    assertTrue(uf.connected("a", "b"));
    assertFalse(uf.connected("a", "c"));
    assertTrue(uf.toString().startsWith("UnionFindSimple [id="), uf.toString());
  }

  @Test
  void addsAnUnknownElementAsAClassOfItsOwn() {
    var uf = new UnionFindSimple<>(List.of("a"));
    assertFalse(uf.connected("a", "z"));
    assertEquals(2, uf.count());
    uf.union("z", "a");
    assertEquals(1, uf.count());
  }
}
