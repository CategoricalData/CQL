package catdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Unit tests for {@link Ref}, a write-once reference, which nothing in CQL uses. */
class RefTest {

  @Test
  void isSetOnceAndToOneValue() {
    var r = new Ref<String>();
    assertFalse(r.isSet());
    assertTrue(r.toString().startsWith("?"), r.toString());
    assertEquals("x", r.set("x"));
    assertTrue(r.isSet());
    assertEquals("x", r.set("x"));
    assertEquals("x", r.toString());
    RuntimeException e = assertThrows(RuntimeException.class, () -> r.set("y"));
    assertEquals("Reference already set to x, cannot set to y", e.getMessage());
    assertThrows(RuntimeException.class, () -> r.set((String) null));
    assertThrows(RuntimeException.class, () -> new Ref<String>(null));
    assertEquals("z", new Ref<>("z").x);
  }

  @Test
  void takesTheValueOfAnotherReference() {
    var r = new Ref<String>();
    r.set(new Ref<>("v"));
    assertEquals("v", r.x);
    var s = new Ref<String>();
    s.set(new Ref<String>());
    assertFalse(s.isSet());
    assertThrows(RuntimeException.class, () -> s.set((Ref<String>) null));
  }

  /** Two unset references are told apart by their numbers, even though neither has a value. */
  @Test
  void comparesByNumberAndValue() {
    var a = new Ref<String>();
    var b = new Ref<String>();
    assertEquals(a, a);
    assertNotEquals(a, b);
    assertNotEquals(a, null);
    assertNotEquals(a, "ref");
    var c = new Ref<String>();
    c.set("x");
    var d = new Ref<String>();
    d.set("x");
    assertEquals(c, d);
    assertEquals(c.hashCode(), d.hashCode());
    d.x = "y";
    assertNotEquals(c, d);
  }
}
