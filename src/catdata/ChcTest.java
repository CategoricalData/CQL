package catdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;
import org.junit.jupiter.api.Test;

/** Unit tests for the helpers of {@link Chc}, a choice between a left and a right value. */
class ChcTest {

  @Test
  void projectsASetOfChoicesOnOneSide() {
    Set<Chc<String, Integer>> lefts = Set.of(Chc.inLeft("a"), Chc.inLeft("b"));
    assertEquals(Set.of("a", "b"), Chc.projIfAllLeft(lefts));
    Set<Chc<String, Integer>> rights = Set.of(Chc.inRight(1));
    assertEquals(Set.of(1), Chc.projIfAllRight(rights));
    RuntimeException e = assertThrows(RuntimeException.class, () -> Chc.projIfAllLeft(rights));
    assertEquals("Cannot projLeft inr 1", e.getMessage());
    e = assertThrows(RuntimeException.class, () -> Chc.projIfAllRight(lefts));
    assertEquals(true, e.getMessage().startsWith("Cannot projRight inl "));
  }

  /** Despite the name, the NC builders refuse null too, so no choice ever holds two nulls. */
  @Test
  void buildsChoicesThatRefuseNull() {
    assertEquals(Chc.inLeft("a"), Chc.inLeftNC("a"));
    assertEquals(Chc.inRight(1), Chc.inRightNC(1));
    assertThrows(RuntimeException.class, () -> Chc.inLeftNC(null));
    assertThrows(RuntimeException.class, () -> Chc.inRightNC(null));
    Chc.inLeft("a").assertNeitherNull();
  }
}
