package catdata.cql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The endless supply of fresh row names that the slow algebras use. */
class ItTest {

  @Test
  void namesRowsInOrderAndNeverRunsOut() {
    It it = new It();
    assertTrue(it.hasNext());
    assertEquals("id0", it.next().toString());
    assertEquals("id1", it.next().str);
    assertTrue(it.hasNext());
    assertEquals("id0", new It().next().toString());
  }

  @Test
  void comparesNamesByTheirText() {
    It.ID a = new It().next();
    It.ID b = new It().next();
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
    assertEquals("id0".hashCode(), a.hashCode());
    It it = new It();
    it.next();
    assertNotEquals(a, it.next());
    assertTrue(a.equals(a));
    assertFalse(a.equals(null));
    assertFalse(a.equals("id0"));
  }
}
