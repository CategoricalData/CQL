package catdata.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import catdata.Pair;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SqlPath}, a chain of foreign keys, over the tables A, B and C with the keys
 * {@code ab : A -> B} and {@code bc : B -> C}.
 */
class SqlPathTest {

  private final SqlTable a = table("A");
  private final SqlTable b = table("B");
  private final SqlTable c = table("C");
  private final SqlForeignKey ab = fk("ab", a, b);
  private final SqlForeignKey bc = fk("bc", b, c);

  private static SqlTable table(String name) {
    SqlTable t = new SqlTable();
    t.name = new Pair<>("PUBLIC", name);
    return t;
  }

  private static SqlForeignKey fk(String name, SqlTable source, SqlTable target) {
    SqlForeignKey fk = new SqlForeignKey();
    fk.name = name;
    fk.source = source;
    fk.target = target;
    return fk;
  }

  @Test
  void endsWhereItsLastKeyPoints() {
    SqlPath empty = new SqlPath(a);
    assertEquals(a, empty.source);
    assertEquals(a, empty.target);
    assertEquals(List.of(), empty.edges);
    SqlPath two = new SqlPath(List.of(ab, bc));
    assertEquals(a, two.source);
    assertEquals(c, two.target);
    assertEquals(two, new SqlPath(a, List.of(ab, bc)));
    assertEquals(two.hashCode(), new SqlPath(a, List.of(ab, bc)).hashCode());
  }

  @Test
  void rejectsKeysThatDoNotMeet() {
    RuntimeException e = assertThrows(RuntimeException.class, () -> new SqlPath(List.of(ab, ab)));
    assertEquals("On edge ab, src is (PUBLIC, A) but last dst was (PUBLIC, B)", e.getMessage());
    e = assertThrows(RuntimeException.class, () -> new SqlPath(b, List.of(ab)));
    assertEquals("On edge ab, src is (PUBLIC, A) but last dst was (PUBLIC, B)", e.getMessage());
    assertNull(assertThrows(RuntimeException.class, () -> new SqlPath(List.of())).getMessage());
  }

  @Test
  void comparesItsEndsAndKeys() {
    SqlPath p = new SqlPath(List.of(ab));
    assertEquals(p, p);
    assertNotEquals(p, null);
    assertNotEquals(p, "ab");
    assertNotEquals(p, new SqlPath(a));
    assertNotEquals(p, new SqlPath(b));
    assertNotEquals(new SqlPath(a), new SqlPath(b));
    SqlPath noEdges = new SqlPath(a);
    noEdges.edges = null;
    SqlPath alsoNoEdges = new SqlPath(a);
    alsoNoEdges.edges = null;
    assertEquals(noEdges, alsoNoEdges);
    assertEquals(noEdges.hashCode(), alsoNoEdges.hashCode());
    assertNotEquals(noEdges, p);
    assertNotEquals(p, noEdges);
    SqlPath noEnds = new SqlPath(a);
    noEnds.source = null;
    noEnds.target = null;
    SqlPath alsoNoEnds = new SqlPath(a);
    alsoNoEnds.source = null;
    alsoNoEnds.target = null;
    assertEquals(noEnds, alsoNoEnds);
    assertEquals(noEnds.hashCode(), alsoNoEnds.hashCode());
    assertNotEquals(noEnds, new SqlPath(a));
    assertNotEquals(new SqlPath(a), noEnds);
    SqlPath noTarget = new SqlPath(a);
    noTarget.target = null;
    assertNotEquals(noTarget, new SqlPath(a));
    assertNotEquals(new SqlPath(a), noTarget);
  }
}
