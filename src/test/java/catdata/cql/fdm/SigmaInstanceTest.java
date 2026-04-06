package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import catdata.cql.AqlOptions;

class SigmaInstanceTest {

  @Nested
  class Constructor {

    @Test
    void constructorSetsSchema() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var mapping = FdmTestHelpers.identityMapping(schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var sigma = new SigmaInstance<>(mapping, inst, AqlOptions.initialOptions);
      assertEquals(schema.ens, sigma.schema().ens);
    }
  }

  @Nested
  class InstanceBehavior {

    @Test
    void algebraIsNotNull() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var mapping = FdmTestHelpers.identityMapping(schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var sigma = new SigmaInstance<>(mapping, inst, AqlOptions.initialOptions);
      assertNotNull(sigma.algebra());
    }

    @Test
    void dpIsNotNull() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var mapping = FdmTestHelpers.identityMapping(schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var sigma = new SigmaInstance<>(mapping, inst, AqlOptions.initialOptions);
      assertNotNull(sigma.dp());
    }

    @Test
    void gensIsNotNull() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var mapping = FdmTestHelpers.identityMapping(schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var sigma = new SigmaInstance<>(mapping, inst, AqlOptions.initialOptions);
      assertNotNull(sigma.gens());
    }

    @Test
    void sksIsNotNull() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var mapping = FdmTestHelpers.identityMapping(schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var sigma = new SigmaInstance<>(mapping, inst, AqlOptions.initialOptions);
      assertNotNull(sigma.sks());
    }

    @Test
    void requireConsistencyDelegates() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var mapping = FdmTestHelpers.identityMapping(schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var sigma = new SigmaInstance<>(mapping, inst, AqlOptions.initialOptions);
      assertTrue(sigma.requireConsistency());
    }

    @Test
    void allowUnsafeJavaDelegates() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var mapping = FdmTestHelpers.identityMapping(schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var sigma = new SigmaInstance<>(mapping, inst, AqlOptions.initialOptions);
      assertTrue(sigma.allowUnsafeJava());
    }

    @Test
    void eqsDoesNotThrow() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var mapping = FdmTestHelpers.identityMapping(schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var sigma = new SigmaInstance<>(mapping, inst, AqlOptions.initialOptions);
      sigma.eqs((l, r) -> {});
    }
  }
}
