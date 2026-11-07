package catdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link Environment}, the name to object table that the MPL driver fills. */
class EnvironmentTest {

  @Test
  void bindsEachNameOnce() {
    var env = new Environment<Integer>();
    env.put("a", 1);
    env.put("b", 2);
    assertEquals(List.of("a", "b"), List.copyOf(env.keys()));
    assertEquals(2, env.get("b"));
    RuntimeException e = assertThrows(RuntimeException.class, () -> env.put("a", 3));
    assertEquals("Duplicate name: a", e.getMessage());
    e = assertThrows(RuntimeException.class, () -> env.get("c"));
    assertEquals("Unbound variable: c", e.getMessage());
    assertThrows(RuntimeException.class, () -> env.put(null, 1));
    assertThrows(RuntimeException.class, () -> env.put("d", null));
  }
}
