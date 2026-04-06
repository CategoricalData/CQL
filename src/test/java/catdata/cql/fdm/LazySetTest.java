package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class LazySetTest {

  private static LazySet<String> lazyOf(Collection<String> backing, int size) {
    return new LazySet<>(u -> backing, size);
  }

  private static LazySet<String> lazyOf(String... items) {
    List<String> list = new ArrayList<>(Arrays.asList(items));
    return new LazySet<>(u -> list, items.length);
  }

  @Nested
  class Constructor {

    @Test
    void constructorDoesNotEvaluateSupplier() {
      AtomicInteger callCount = new AtomicInteger(0);
      new LazySet<String>(u -> {
        callCount.incrementAndGet();
        return List.of();
      }, 0);
      assertEquals(0, callCount.get());
    }
  }

  @Nested
  class SizeAndEmpty {

    @Test
    void sizeReturnsCachedSize() {
      LazySet<String> set = new LazySet<>(u -> List.of("a", "b", "c"), 3);
      assertEquals(3, set.size());
    }

    @Test
    void isEmptyTrueWhenSizeZero() {
      assertTrue(new LazySet<>(u -> List.of(), 0).isEmpty());
    }

    @Test
    void isEmptyFalseWhenSizeNonZero() {
      assertFalse(new LazySet<>(u -> List.of("a"), 1).isEmpty());
    }

    @Test
    void sizeDoesNotTriggerInit() {
      AtomicInteger callCount = new AtomicInteger(0);
      LazySet<String> set = new LazySet<>(u -> {
        callCount.incrementAndGet();
        return List.of();
      }, 5);
      set.size();
      set.isEmpty();
      assertEquals(0, callCount.get());
    }
  }

  @Nested
  class LazyInitialization {

    @Test
    void initTriggeredByContains() {
      AtomicInteger callCount = new AtomicInteger(0);
      LazySet<String> set = new LazySet<>(u -> {
        callCount.incrementAndGet();
        return List.of("a");
      }, 1);
      set.contains("a");
      assertEquals(1, callCount.get());
    }

    @Test
    void initOnlyCalledOnce() {
      AtomicInteger callCount = new AtomicInteger(0);
      LazySet<String> set = new LazySet<>(u -> {
        callCount.incrementAndGet();
        return List.of("a");
      }, 1);
      set.contains("a");
      set.contains("b");
      set.iterator();
      assertEquals(1, callCount.get());
    }
  }

  @Nested
  class ContainsMethod {

    @Test
    void containsReturnsTrueForPresent() {
      assertTrue(lazyOf("a", "b").contains("a"));
    }

    @Test
    void containsReturnsFalseForAbsent() {
      assertFalse(lazyOf("a", "b").contains("c"));
    }
  }

  @Nested
  class IteratorMethod {

    @Test
    void iteratorReturnsAllElements() {
      LazySet<String> set = lazyOf("x", "y", "z");
      List<String> result = new ArrayList<>();
      Iterator<String> it = set.iterator();
      while (it.hasNext()) {
        result.add(it.next());
      }
      assertEquals(List.of("x", "y", "z"), result);
    }
  }

  @Nested
  class ToArrayMethod {

    @Test
    void toArrayReturnsElements() {
      Object[] arr = lazyOf("a", "b").toArray();
      assertArrayEquals(new Object[] {"a", "b"}, arr);
    }

    @Test
    void toArrayTypedReturnsElements() {
      String[] arr = lazyOf("a", "b").toArray(new String[0]);
      assertArrayEquals(new String[] {"a", "b"}, arr);
    }
  }

  @Nested
  class MutationMethods {

    @Test
    void addDelegatesToBacking() {
      List<String> backing = new ArrayList<>(List.of("a"));
      LazySet<String> set = lazyOf(backing, 1);
      set.add("b");
      assertTrue(backing.contains("b"));
    }

    @Test
    void removeDelegatesToBacking() {
      List<String> backing = new ArrayList<>(List.of("a", "b"));
      LazySet<String> set = lazyOf(backing, 2);
      set.remove("a");
      assertFalse(backing.contains("a"));
    }

    @Test
    void clearDelegatesToBacking() {
      List<String> backing = new ArrayList<>(List.of("a", "b"));
      LazySet<String> set = lazyOf(backing, 2);
      set.clear();
      assertTrue(backing.isEmpty());
    }

    @Test
    void addAllDelegatesToBacking() {
      List<String> backing = new ArrayList<>();
      LazySet<String> set = lazyOf(backing, 0);
      set.addAll(List.of("x", "y"));
      assertEquals(List.of("x", "y"), backing);
    }

    @Test
    void removeAllDelegatesToBacking() {
      List<String> backing = new ArrayList<>(List.of("a", "b", "c"));
      LazySet<String> set = lazyOf(backing, 3);
      set.removeAll(List.of("a", "c"));
      assertEquals(List.of("b"), backing);
    }

    @Test
    void retainAllDelegatesToBacking() {
      List<String> backing = new ArrayList<>(List.of("a", "b", "c"));
      LazySet<String> set = lazyOf(backing, 3);
      set.retainAll(List.of("b"));
      assertEquals(List.of("b"), backing);
    }
  }

  @Nested
  class ContainsAllMethod {

    @Test
    void containsAllReturnsTrueWhenAllPresent() {
      assertTrue(lazyOf("a", "b", "c").containsAll(List.of("a", "c")));
    }

    @Test
    void containsAllReturnsFalseWhenSomeMissing() {
      assertFalse(lazyOf("a", "b").containsAll(List.of("a", "z")));
    }
  }
}
