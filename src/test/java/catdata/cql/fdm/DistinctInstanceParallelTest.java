package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import catdata.cql.AqlOptions;

class DistinctInstanceParallelTest {

  @Nested
  class Constructor {

    @Test
    void constructorSetsSchema() {
      var inst = FdmTestHelpers.singleElementInstance();
      var distinct = new DistinctInstanceParallel<>(inst, AqlOptions.initialOptions);
      assertEquals(inst.schema().ens, distinct.schema().ens);
    }
  }

  @Nested
  class InstanceBehavior {

    @Test
    void gensDelegates() {
      var inst = FdmTestHelpers.singleElementInstance();
      var distinct = new DistinctInstanceParallel<>(inst, AqlOptions.initialOptions);
      assertNotNull(distinct.gens());
    }

    @Test
    void sksDelegates() {
      var inst = FdmTestHelpers.singleElementInstance();
      var distinct = new DistinctInstanceParallel<>(inst, AqlOptions.initialOptions);
      assertNotNull(distinct.sks());
    }

    @Test
    void algebraSizePreservedOrReduced() {
      var inst = FdmTestHelpers.singleElementInstance();
      var distinct = new DistinctInstanceParallel<>(inst, AqlOptions.initialOptions);
      assertTrue(distinct.algebra().size("E") <= inst.algebra().size("E"));
    }

    @Test
    void requireConsistencyDelegates() {
      var inst = FdmTestHelpers.singleElementInstance();
      var distinct = new DistinctInstanceParallel<>(inst, AqlOptions.initialOptions);
      assertEquals(inst.requireConsistency(), distinct.requireConsistency());
    }

    @Test
    void allowUnsafeJavaDelegates() {
      var inst = FdmTestHelpers.singleElementInstance();
      var distinct = new DistinctInstanceParallel<>(inst, AqlOptions.initialOptions);
      assertEquals(inst.allowUnsafeJava(), distinct.allowUnsafeJava());
    }
  }

  @Nested
  class EqsMethod {

    @Test
    void eqsDoesNotThrow() {
      var inst = FdmTestHelpers.singleElementInstance();
      var distinct = new DistinctInstanceParallel<>(inst, AqlOptions.initialOptions);
      distinct.eqs((l, r) -> {});
    }
  }
}
