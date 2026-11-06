package catdata.provers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.Pair;
import catdata.cql.ConsList;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Thue}, the string rewriting completion behind the monoidal prover, over the
 * alphabet {a, b}.
 */
class ThueTest {

  private static List<String> w(String s) {
    List<String> l = new ArrayList<>();
    for (char c : s.toCharArray()) {
      l.add(String.valueOf(c));
    }
    return l;
  }

  private static Thue<String> thue(String... rules) {
    List<Pair<List<String>, List<String>>> l = new ArrayList<>();
    for (int i = 0; i < rules.length; i += 2) {
      l.add(new Pair<>(w(rules[i]), w(rules[i + 1])));
    }
    return new Thue<>(l, List.of("a", "b"));
  }

  @Test
  void decidesTheWordProblemOfAnIdempotent() {
    var t = thue("aa", "a");
    assertTrue(t.equiv(w("aaaa"), w("a")));
    assertTrue(t.equiv0(w("aaab"), w("ab")));
    assertFalse(t.equiv(w("a"), w("b")));
    assertTrue(t.toString().contains("a.a -> a"), t.toString());
  }

  /** ab = ba keeps the length, so it stays an equation instead of becoming a rule. */
  @Test
  void decidesTheWordProblemOfACommutation() {
    var t = thue("ab", "ba");
    assertTrue(t.equiv(w("aab"), w("aba")));
    assertTrue(t.equiv(w("ab"), w("ba")));
    assertFalse(t.equiv(w("ab"), w("aab")));
    assertTrue(t.toString().contains("a.b = b.a"), t.toString());
  }

  @Test
  void ignoresAnEmptyRule() {
    var t = thue("", "", "aa", "a");
    assertTrue(t.equiv(w("aa"), w("a")));
  }

  private static Set<Pair<ConsList<String>, ConsList<String>>> rules(Thue<String> t, String x) {
    Set<Pair<ConsList<String>, ConsList<String>>> ret = new HashSet<>();
    t.rules.rules(x).forEach(ret::add);
    return ret;
  }

  /**
   * The completed system indexes each rule by the first letter of its left side, and each equation
   * also by the first letter of its right side. Nothing calls {@code size} or {@code rules}; size
   * counts an equation once per index, so twice.
   */
  @Test
  void indexesTheRulesByTheirFirstLetter() {
    var idempotent = thue("aa", "a");
    assertEquals(1, rules(idempotent, "a").size());
    assertTrue(rules(idempotent, "b").isEmpty());
    assertEquals(1, idempotent.rules.size());
    var commutation = thue("ab", "ba");
    assertEquals(1, rules(commutation, "a").size() + rules(commutation, "b").size());
    assertEquals(2, commutation.rules.size());
  }
}
