package catdata;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit tests for the helpers of {@link Util} that nothing else exercises. */
class UtilTest {

  @Nested
  class Maps {

    /** Composing a bijection with its inverse gives the identity both ways, which it negates. */
    @Test
    void neverRecognizesABijection() {
      Map<Integer, String> m = Map.of(1, "a", 2, "b");
      assertFalse(Util.isBijection(m, Set.of(1, 2), Set.of("a", "b")));
      assertFalse(Util.isBijection(m, Set.of(1), Set.of("a", "b")));
      assertFalse(Util.isBijection(m, Set.of(1, 2), Set.of("a")));
      assertFalse(Util.isBijection(Map.of(1, "a", 2, "a"), Set.of(1, 2), Set.of("a")));
    }

    @Test
    void composesAndInvertsMaps() {
      Map<Integer, String> m = Map.of(1, "a", 2, "b");
      assertEquals(Map.of(1, "A", 2, "B"), Util.compose0(m, Map.of("a", "A", "b", "B")));
      Map<Integer, String> partial = Util.compose0(m, Map.of("a", "A"));
      assertTrue(partial.containsKey(2));
      assertNull(partial.get(2));
      assertEquals(Map.of(1, "A"), Util.compose1(m, Map.of("a", "A")));
      assertEquals(Map.of("a", 1, "b", 2), Util.rev0(m));
      assertNull(Util.rev0(Map.of(1, "a", 2, "a")));
      assertEquals(Map.of("a", 1, "b", 2), Util.invMap(m));
      RuntimeException e =
          assertThrows(RuntimeException.class, () -> Util.invMap(Map.of(1, "a", 2, "a")));
      assertTrue(e.getMessage().startsWith("Not injective: a is "), e.getMessage());
      Map<Integer, String> twice = new LinkedHashMap<>();
      twice.put(1, "a");
      twice.put(2, "a");
      assertEquals(Map.of("a", 1), Util.invMapX(twice));
    }

    @Test
    void looksUpAKeyByItsValue() {
      Map<String, Integer> m = Map.of("a", 1, "b", 2);
      assertEquals("b", Util.revLookup(m, 2));
      assertNull(Util.revLookup(m, 3));
      RuntimeException e =
          assertThrows(RuntimeException.class, () -> Util.revLookup(Map.of("a", 1, "b", 1), 1));
      assertTrue(e.getMessage().startsWith("Inverse is not a function: "), e.getMessage());
      assertEquals(2, Util.inverse((Integer x) -> x * 10, Set.of(1, 2)).apply(20));
      assertEquals(Map.of(1, 10, 2, 20), Util.reify((Integer x) -> x * 10, Set.of(1, 2)));
      assertTrue(Set.of("a", "b").contains(Util.anyKey(m)));
      assertThrows(RuntimeException.class, () -> Util.anyKey(Map.of()));
    }

    @Test
    void convertsBetweenMapsAndSetsOfPairs() {
      Set<Pair<String, Integer>> pairs = Set.of(new Pair<>("a", 1), new Pair<>("b", 2));
      assertEquals(Map.of("a", 1, "b", 2), Util.convert(pairs));
      assertEquals(pairs, Util.convert(Map.of("a", 1, "b", 2)));
      Set<Pair<String, Integer>> multi = Set.of(new Pair<>("a", 1), new Pair<>("a", 2));
      assertEquals(Map.of("a", Set.of(1, 2)), Util.convertMulti(multi));
      RuntimeException e = assertThrows(RuntimeException.class, () -> Util.convert(multi));
      assertTrue(
          e.getMessage().startsWith("Cannot convert to map (not functional): "), e.getMessage());
      assertTrue(Util.containsKey(pairs, "a"));
      assertFalse(Util.containsKey(pairs, "c"));
      assertEquals(Map.of("a", 1), Util.listToMap(List.of(new Pair<>("a", 1))));
      e =
          assertThrows(
              RuntimeException.class,
              () -> Util.listToMap(List.of(new Pair<>("a", 1), new Pair<>("a", 2))));
      assertTrue(e.getMessage().startsWith("Duplicate entry for a in "), e.getMessage());
    }

    @Test
    void makesAMapPerKey() {
      List<String> keys = List.of("a", "b");
      Map<String, Map<Integer, Integer>> maps = Util.newMapsFor(keys);
      assertEquals(Set.of("a", "b"), maps.keySet());
      assertTrue(maps.get("a").isEmpty());
      Map<String, Collection<Integer>> sets = Util.newSetsFor0(keys);
      assertTrue(sets.get("b").isEmpty());
      assertEquals(Map.of("a", 0, "b", 0), Util.constMap(keys, 0));
    }

    /** A single entry is read at the key 0, so a map with one other key fails. */
    @Test
    void printsTheValuesForPi() {
      assertEquals("", Util.printForPi(Map.of()));
      assertEquals("x", Util.printForPi(Map.of(0, "x")));
      Map<String, String> two = new LinkedHashMap<>();
      two.put("p", "x");
      two.put("q", "y");
      assertEquals("(x, y)", Util.printForPi(two));
      assertThrows(NullPointerException.class, () -> Util.printForPi(Map.of("p", "x")));
    }

    @Test
    void printsMapsSortedByKey() {
      Map<String, Integer> m = new HashMap<>();
      m.put("b", 2);
      m.put("a", 1);
      assertEquals("a=1, b=2\nc=3", Util.printNicely(Set.of(m, Map.of("c", 3))));
    }
  }

  @Nested
  class Collections {

    @Test
    void mapsAndFoldsArrays() {
      assertArrayEquals(new Object[] {"x"}, Util.sing("x"));
      assertArrayEquals(new Object[] {2, 4}, Util.map(new Integer[] {1, 2}, x -> x * 2));
      assertEquals(6, Util.fold(new Integer[] {1, 2, 3}, 0, p -> p.first + p.second));
      assertEquals("[a, b][c]", Util.print(List.of(new String[] {"a", "b"}, new String[] {"c"})));
    }

    @Test
    void walksAndSplitsLists() {
      Iterable<Integer> both = Util.iterConcat(List.of(1), List.of(2));
      List<Integer> seen = new ArrayList<>();
      both.forEach(seen::add);
      both.forEach(seen::add);
      assertEquals(List.of(1, 2, 1, 2), seen);
      assertEquals(1, Util.get0Y(List.of(1, 2)));
      assertNull(Util.get0Y(List.of()));
      assertEquals(List.of(0, 1, 2), Util.range(3));
      var zipped = Util.zip(List.of("a", "b"), List.of(1, 2));
      assertEquals(List.of(new Pair<>("a", 1), new Pair<>("b", 2)), zipped);
      assertEquals(new Pair<>(List.of("a", "b"), List.of(1, 2)), Util.unzip(zipped));
      assertThrows(RuntimeException.class, () -> Util.zip(List.of("a"), List.of()));
      Util.assertNoDups(List.of("a", "b"));
      RuntimeException e =
          assertThrows(RuntimeException.class, () -> Util.assertNoDups(List.of("a", "a")));
      assertEquals("List contains duplicates, namely {a}", e.getMessage());
      assertEquals(List.of("a", "b"), Util.alphaMaybe(true, List.of("b", "a")));
      assertEquals(List.of("b", "a"), Util.alphaMaybe(false, List.of("b", "a")));
      assertTrue(Util.containsUpToCase(List.of("Ab"), "aB"));
      assertFalse(Util.containsUpToCase(List.of("Ab"), "ac"));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void stripsNestedChoices() {
      Pair<java.util.function.Function, Object> p = Util.stripChcs(Chc.inLeft(Chc.inRight("x")));
      assertEquals("x", p.second);
      assertEquals(Chc.inLeft(Chc.inRight("y")), p.first.apply("y"));
      assertEquals("z", Util.stripChcs("z").second);
    }
  }

  @Nested
  class Strings {

    @Test
    void quotesAndRecognizesStrings() {
      assertEquals("!!!NULL!!!", Util.q(null));
      assertEquals("\"a-b\"", Util.q("a-b"));
      assertEquals("\"\"", Util.q(""));
      assertEquals("x", Util.q("x"));
      assertEquals("a\"-b", Util.q("a\"-b"));
      assertTrue(Util.isInt("42"));
      assertFalse(Util.isInt("4x"));
    }

    @Test
    void comparesStrings() {
      assertEquals("fl", Util.longestCommonPrefix(List.of("flower", "flow", "flight")));
      assertEquals("", Util.longestCommonPrefix(List.of()));
      assertEquals("abc", Util.longestCommonPrefix(List.of("abc", "abc")));
      assertEquals(1.0, Util.similarity("abc", "ABC"));
      assertEquals(0.5, Util.similarity("abc", "abd"));
      assertEquals("cat", Util.fuzzyOverlap("the cat in a hat 42", "a cat and 42 hats"));
      assertTrue(Util.LengthComparator.compare("ab", "c") > 0);
      assertTrue(Util.LengthComparator.compare("a", "bc") < 0);
      assertTrue(Util.LengthComparator.compare("a", "b") < 0);
    }

    /** Either string containing the other counts, whether or not it is a prefix. */
    @Test
    void testsContainmentAsAPrefix() {
      assertEquals(Optional.empty(), Util.stringPrefix(Optional.empty(), Optional.of("a")));
      assertEquals(Optional.of(true), Util.stringPrefix(Optional.of("ab"), Optional.of("abc")));
      assertEquals(Optional.of(true), Util.stringPrefix(Optional.of("bc"), Optional.of("abcd")));
      assertEquals(Optional.of(false), Util.stringPrefix(Optional.of("ab"), Optional.of("cd")));
    }
  }

  @Nested
  class Errors {

    @TempDir Path dir;

    @Test
    void abortsOnAVoidValue() {
      RuntimeException e = assertThrows(RuntimeException.class, () -> Util.abort(null));
      assertEquals("Anomaly: please report", e.getMessage());
      e = assertThrows(RuntimeException.class, () -> Util.<String>voidFn().apply(null));
      assertEquals("Anomaly: please report", e.getMessage());
    }

    @Test
    void checksAClassName() {
      Util.checkClass("java.lang.String");
      Util.checkClass("  ");
      RuntimeException e =
          assertThrows(RuntimeException.class, () -> Util.checkClass("no.such.Clazz"));
      assertInstanceOf(ClassNotFoundException.class, e.getCause());
    }

    @Test
    void readsAFile() throws Exception {
      File f = Files.writeString(dir.resolve("x.txt"), "hello").toFile();
      assertEquals("hello", Util.readFile2(f));
    }
  }

  @Nested
  class SafeMaps {

    @Test
    void refusesToRebindAKeyToAnotherValue() {
      Map<String, Integer> m = new HashMap<>(Map.of("a", 1));
      Util.putAllSafely(m, Map.of("a", 1, "b", 2));
      assertEquals(Map.of("a", 1, "b", 2), m);
      RuntimeException e =
          assertThrows(RuntimeException.class, () -> Util.putAllSafely(m, Map.of("a", 3)));
      assertEquals("Collision on a was 1 becomes 3", e.getMessage());
      Map<String, Integer> withNull = new HashMap<>();
      withNull.put("n", null);
      e = assertThrows(RuntimeException.class, () -> Util.putAllSafely(withNull, Map.of("n", 1)));
      assertTrue(e.getMessage().startsWith("Anomaly: please report: k=n "), e.getMessage());
      Util.putSafely(m, "a", 1);
      e = assertThrows(RuntimeException.class, () -> Util.putSafely(m, "a", 4));
      assertEquals("Two distinct bindings for a: 4 and 1", e.getMessage());
      e = assertThrows(RuntimeException.class, () -> Util.putSafelyNoDups(m, "a", 1));
      assertEquals("Two distinct bindings for a: 1 and 1", e.getMessage());
      assertEquals(
          Map.of("a", 1), Util.toMapSafely(List.of(new Pair<>("a", 1), new Pair<>("a", 1))));
      e =
          assertThrows(
              RuntimeException.class,
              () -> Util.toMapSafelyNoDupsList(List.of(new Pair<>("a", 1), new Pair<>("a", 1))));
      assertEquals("Two distinct bindings for a: 1 and 1", e.getMessage());
    }

    @Test
    void findsTheOnlyElementOrAValueByKey() {
      assertEquals("x", Util.get0(List.of("x")));
      RuntimeException e = assertThrows(RuntimeException.class, () -> Util.get0(List.of()));
      assertEquals("Size != 1 on []", e.getMessage());
      List<Pair<String, Integer>> pairs = List.of(new Pair<>("a", 1));
      assertEquals(1, Util.lookup(pairs, "a"));
      e = assertThrows(RuntimeException.class, () -> Util.lookup(pairs, "b"));
      assertEquals("Cannot find b in [(a, 1)]", e.getMessage());
    }
  }

  @Nested
  class Timeouts {

    @Test
    void returnsTheValueInTime() {
      assertEquals("done", Util.timeout(() -> "done", 10_000, "pre: "));
    }

    @Test
    void reportsAFailureWithItsPrefix() {
      RuntimeException e =
          assertThrows(
              RuntimeException.class,
              () ->
                  Util.timeout(
                      () -> {
                        throw new IllegalStateException("boom");
                      },
                      10_000,
                      "pre: "));
      assertEquals("pre: java.lang.IllegalStateException: boom", e.getMessage());
    }

    /** The task is cancelled, which interrupts the sleep, so no thread keeps running. */
    @Test
    void cancelsATaskThatRunsTooLong() {
      RuntimeException e =
          assertThrows(
              RuntimeException.class,
              () ->
                  Util.timeout(
                      () -> {
                        Thread.sleep(60_000);
                        return "late";
                      },
                      50,
                      "pre: "));
      assertTrue(e.getMessage().startsWith("pre: Timeout after 0 seconds."), e.getMessage());
    }
  }
}
