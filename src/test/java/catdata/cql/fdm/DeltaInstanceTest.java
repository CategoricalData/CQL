package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DeltaInstanceTest {

  @Nested
  class Constructor {

    @Test
    void constructorSetsSchema() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var mapping = FdmTestHelpers.identityMapping(schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var delta = new DeltaInstance<>(mapping, inst);
      assertEquals(schema.ens, delta.schema().ens);
    }

    @Test
    void constructorRejectsSchemasMismatch() {
      var schema2 = FdmTestHelpers.schemaWithFk();
      var mapping = FdmTestHelpers.identityMapping(schema2);
      var inst = FdmTestHelpers.singleElementInstance(); // on schema1
      assertThrows(RuntimeException.class, () -> new DeltaInstance<>(mapping, inst));
    }
  }

  @Nested
  class InstanceBehavior {

    @Test
    void algebraReturnsElements() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var mapping = FdmTestHelpers.identityMapping(schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var delta = new DeltaInstance<>(mapping, inst);
      assertEquals(1, delta.algebra().size("E"));
    }

    @Test
    void dpIsNotNull() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var mapping = FdmTestHelpers.identityMapping(schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var delta = new DeltaInstance<>(mapping, inst);
      assertNotNull(delta.dp());
      assertSame(delta, delta.dp()); // implements DP itself
    }

    @Test
    void toStringProverDelegates() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var mapping = FdmTestHelpers.identityMapping(schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var delta = new DeltaInstance<>(mapping, inst);
      assertNotNull(delta.toStringProver());
    }

    @Test
    void requireConsistencyDelegates() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var mapping = FdmTestHelpers.identityMapping(schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var delta = new DeltaInstance<>(mapping, inst);
      assertFalse(delta.requireConsistency());
    }

    @Test
    void allowUnsafeJavaDelegates() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var mapping = FdmTestHelpers.identityMapping(schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var delta = new DeltaInstance<>(mapping, inst);
      assertFalse(delta.allowUnsafeJava());
    }
  }
}
