package catdata.cql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;
import org.junit.jupiter.api.Test;

/**
 * Fresh variable names for the program prover. The counter is static and other tests advance it,
 * so the tests compare consecutive names instead of fixed ones.
 */
class VarItTest {

  private static int number(String v) {
    assertTrue(v.startsWith("v"), v);
    return Integer.parseInt(v.substring(1));
  }

  @Test
  void neverRunsOut() {
    Iterator<String> it = VarIt.it();
    assertTrue(it.hasNext());
    String first = it.next();
    assertTrue(it.hasNext());
    assertEquals(number(first) + 1, number(it.next()));
  }

  /** Every iterator draws from the same counter, so two of them never hand out the same name. */
  @Test
  void sharesOneCounterAcrossIterators() {
    Iterator<String> a = VarIt.it();
    Iterator<String> b = VarIt.it();
    String x = a.next();
    String y = b.next();
    assertNotEquals(x, y);
    assertEquals(number(x) + 1, number(y));
  }
}
