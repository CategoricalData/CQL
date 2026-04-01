package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import catdata.Pair;
import catdata.cql.AqlOptions;

class SubInstanceTest {

  @Nested
  class Constructor {

    @Test
    void constructorWithFullSetPreservesAll() {
      var inst = FdmTestHelpers.singleElementInstance();
      Set<Pair<String, String>> xs = new HashSet<>();
      xs.add(new Pair<>("E", "e1"));
      var sub = new SubInstance<>(xs, inst, AqlOptions.initialOptions);
      assertEquals(1, sub.algebra().size("E"));
    }

    @Test
    void constructorWithEmptySetCreatesEmpty() {
      var inst = FdmTestHelpers.singleElementInstance();
      Set<Pair<String, String>> xs = Collections.emptySet();
      var sub = new SubInstance<>(xs, inst, AqlOptions.initialOptions);
      assertEquals(0, sub.algebra().size("E"));
    }
  }

  @Nested
  class InstanceBehavior {

    @Test
    void schemaDelegates() {
      var inst = FdmTestHelpers.singleElementInstance();
      Set<Pair<String, String>> xs = new HashSet<>();
      xs.add(new Pair<>("E", "e1"));
      var sub = new SubInstance<>(xs, inst, AqlOptions.initialOptions);
      assertEquals(inst.schema().ens, sub.schema().ens);
    }

    @Test
    void sksDelegates() {
      var inst = FdmTestHelpers.singleElementInstance();
      Set<Pair<String, String>> xs = new HashSet<>();
      xs.add(new Pair<>("E", "e1"));
      var sub = new SubInstance<>(xs, inst, AqlOptions.initialOptions);
      assertNotNull(sub.sks());
    }

    @Test
    void dpIsNotNull() {
      var inst = FdmTestHelpers.singleElementInstance();
      Set<Pair<String, String>> xs = new HashSet<>();
      xs.add(new Pair<>("E", "e1"));
      var sub = new SubInstance<>(xs, inst, AqlOptions.initialOptions);
      assertNotNull(sub.dp());
    }
  }

  @Nested
  class GensMethod {

    @Test
    void gensContainsKeyForIncludedElement() {
      var inst = FdmTestHelpers.singleElementInstance();
      Set<Pair<String, String>> xs = new HashSet<>();
      xs.add(new Pair<>("E", "e1"));
      var sub = new SubInstance<>(xs, inst, AqlOptions.initialOptions);
      assertTrue(sub.gens().containsKey(new Pair<>("E", "e1")));
    }

    @Test
    void gensGetReturnsEntity() {
      var inst = FdmTestHelpers.singleElementInstance();
      Set<Pair<String, String>> xs = new HashSet<>();
      xs.add(new Pair<>("E", "e1"));
      var sub = new SubInstance<>(xs, inst, AqlOptions.initialOptions);
      assertEquals("E", sub.gens().get(new Pair<>("E", "e1")));
    }

    @Test
    void gensSizeMatchesSubset() {
      var inst = FdmTestHelpers.twoElementInstance();
      Set<Pair<String, String>> xs = new HashSet<>();
      xs.add(new Pair<>("E", "e1"));
      var sub = new SubInstance<>(xs, inst, AqlOptions.initialOptions);
      assertEquals(1, sub.gens().size());
    }
  }

  @Nested
  class GetInclusionMethod {

    @Test
    void inclusionSourceIsSubInstance() {
      var inst = FdmTestHelpers.singleElementInstance();
      Set<Pair<String, String>> xs = new HashSet<>();
      xs.add(new Pair<>("E", "e1"));
      var sub = new SubInstance<>(xs, inst, AqlOptions.initialOptions);
      var inclusion = sub.getInclusion();
      assertSame(sub, inclusion.src());
    }

    @Test
    void inclusionDestIsOriginal() {
      var inst = FdmTestHelpers.singleElementInstance();
      Set<Pair<String, String>> xs = new HashSet<>();
      xs.add(new Pair<>("E", "e1"));
      var sub = new SubInstance<>(xs, inst, AqlOptions.initialOptions);
      var inclusion = sub.getInclusion();
      assertSame(inst, inclusion.dst());
    }
  }
}
