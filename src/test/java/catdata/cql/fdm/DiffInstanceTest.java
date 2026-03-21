package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DiffInstanceTest {

  @Nested
  class Constructor {

    @Test
    void constructorRejectsSchemaWithFks() {
      var inst1 = FdmTestHelpers.instanceWithFk();
      var inst2 = FdmTestHelpers.instanceWithFk();
      assertThrows(RuntimeException.class,
          () -> new DiffInstance<>(inst1, inst2, false, false));
    }

    @Test
    void constructorRejectsDifferentSchemas() {
      var schema1 = FdmTestHelpers.singleEntitySchema();
      var schema2 = FdmTestHelpers.emptySchema();
      var inst1 = FdmTestHelpers.emptyInstance(schema1);
      var inst2 = FdmTestHelpers.emptyInstance(schema2);
      assertThrows(RuntimeException.class,
          () -> new DiffInstance<>(inst1, inst2, false, false));
    }
  }

  @Nested
  class DiffBehavior {

    @Test
    void diffOfSameInstanceIsEmpty() {
      var inst = FdmTestHelpers.singleElementInstance();
      var diff = new DiffInstance<>(inst, inst, false, false);
      assertEquals(0, diff.algebra().size("E"));
    }

    @Test
    void diffWithEmptyKeepsAll() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var inst = FdmTestHelpers.singleElementInstance();
      var empty = FdmTestHelpers.emptyInstance(schema);
      var diff = new DiffInstance<>(inst, empty, false, false);
      assertEquals(1, diff.algebra().size("E"));
    }
  }

  @Nested
  class InstanceMethods {

    @Test
    void schemaMatchesInput() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var inst = FdmTestHelpers.singleElementInstance();
      var empty = FdmTestHelpers.emptyInstance(schema);
      var diff = new DiffInstance<>(inst, empty, false, false);
      assertEquals(schema.ens, diff.schema().ens);
    }

    @Test
    void dpIsNotNull() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var inst = FdmTestHelpers.singleElementInstance();
      var empty = FdmTestHelpers.emptyInstance(schema);
      var diff = new DiffInstance<>(inst, empty, false, false);
      assertNotNull(diff.dp());
    }

    @Test
    void transformIsNotNull() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var inst = FdmTestHelpers.singleElementInstance();
      var empty = FdmTestHelpers.emptyInstance(schema);
      var diff = new DiffInstance<>(inst, empty, false, false);
      assertNotNull(diff.h);
    }
  }
}
