package catdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.function.Function;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link InteriorLabel}, an outline entry of a section of a program. */
class InteriorLabelTest {

  /** The position is left out, so two labels at different places in a section are equal. */
  @Test
  void comparesBySectionAndValue() {
    Function<String, String> f = x -> x;
    var l = new InteriorLabel<>("entities", "A", 3, f);
    assertEquals(l, l);
    assertEquals(l, new InteriorLabel<>("entities", "A", 3, f));
    assertEquals(l.hashCode(), new InteriorLabel<>("entities", "A", 3, f).hashCode());
    assertNotEquals(l, null);
    assertNotEquals(l, "A");
    assertEquals(l, new InteriorLabel<>("entities", "A", 4, f));
    assertNotEquals(l, new InteriorLabel<>("entities", "B", 3, f));
    assertNotEquals(l, new InteriorLabel<>("fks", "A", 3, f));
    assertNotEquals(new InteriorLabel<>(null, "A", 3, f), l);
    assertNotEquals(new InteriorLabel<>("entities", null, 3, f), l);
    assertEquals(
        new InteriorLabel<String>(null, null, null, f),
        new InteriorLabel<String>(null, null, null, f));
  }
}
