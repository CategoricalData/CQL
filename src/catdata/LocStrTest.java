package catdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link LocStr}, a string with its position in a program, and its helpers. */
class LocStrTest {

  @Test
  void comparesByPositionAndText() {
    var s = new LocStr(3, "x");
    assertEquals(s, s);
    assertEquals(s, new LocStr(3, "x"));
    assertNotEquals(s, null);
    assertNotEquals(s, "x");
    assertNotEquals(s, new LocStr(4, "x"));
    assertNotEquals(s, new LocStr(3, "y"));
    assertNotEquals(new LocStr(null, "x"), s);
    assertEquals(new LocStr(null, null), new LocStr(null, null));
    assertNotEquals(new LocStr(3, null), s);
  }

  @Test
  void indexesTheImportsOfASection() {
    Function<Pair<String, Object>, String> f = p -> p.second.toString();
    var m = LocStr.imports("schemas", List.of(new LocStr(1, "S"), new LocStr(5, "T")), f);
    assertEquals(Set.of(new Pair<>("schemas", "S"), new Pair<>("schemas", "T")), m.keySet());
    assertEquals(5, m.get(new Pair<>("schemas", "T")).first);
    var labels = LocStr.imports("schemas", List.of(new LocStr(1, "S")));
    assertEquals(1, labels.size());
    assertEquals("S", labels.get(0).s);
  }

  @Test
  void keepsTheAttributesWithTheirConvertedTargets() {
    var x =
        LocStr.set2x(
            List.of(new Pair<>(new LocStr(1, "a"), new Pair<>("E", "t"))), String::toUpperCase);
    assertEquals(Set.of(new Pair<>("a", new Pair<>("E", "T"))), x);
    assertEquals(Map.of(), Map.of());
  }
}
