package catdata.provers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.Pair;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ThueSlow}, completion of a string rewriting system, which nothing in CQL
 * constructs. Only systems whose completion and closures terminate are used: the closure of a word
 * with exactly two rearrangements under ab = ba never returns.
 */
class ThueSlowTest {

  private static List<String> w(String s) {
    List<String> l = new ArrayList<>();
    for (char c : s.toCharArray()) {
      l.add(String.valueOf(c));
    }
    return l;
  }

  private static ThueSlow<String, Void> thue(String lhs, String rhs) {
    return new ThueSlow<>(new ArrayList<>(List.of(new Pair<>(w(lhs), w(rhs)))));
  }

  /** The rule a = aa is oriented as aa -> a. */
  @Test
  void decidesTheWordProblemOfAnIdempotent() {
    var t = thue("a", "aa");
    assertEquals("  a.a -> a", t.toString());
    assertTrue(t.equiv(w("aaaa"), w("a")));
    assertTrue(t.equiv(w("aaaa"), w("a")));
    assertFalse(t.equiv(w("a"), w("b")));
    assertEquals(w("ab"), t.normalize("", w("aaab")));
  }

  @Test
  void decidesTheWordProblemOfALeftAbsorbingLetter() {
    var t = thue("ab", "b");
    assertTrue(t.equiv(w("aab"), w("b")));
    assertFalse(t.equiv(w("ba"), w("ab")));
    assertEquals(w("ba"), t.normalize("", w("ba")));
  }

  /** A rule that keeps the length is used to close a word under rearrangement. */
  @Test
  void closesAWordUnderALengthPreservingRule() {
    var t = thue("ab", "ba");
    assertTrue(t.equiv(w("aab"), w("aba")));
    Set<List<String>> closed =
        new HashSet<>(ThueSlow.close(w("aab"), List.of(new Pair<>(w("ab"), w("ba")))));
    assertEquals(Set.of(w("aab"), w("aba"), w("baa")), closed);
  }
}
