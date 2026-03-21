package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DeltaTransformTest {

  @Nested
  class Constructor {

    @Test
    void constructorSetsSrcAndDst() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var mapping = FdmTestHelpers.identityMapping(schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var idTransform = new IdentityTransform<>(inst, Optional.empty());
      var dt = new DeltaTransform<>(mapping, idTransform);
      assertNotNull(dt.src());
      assertNotNull(dt.dst());
    }

    @Test
    void constructorRejectsSchemaMismatch() {
      var schema2 = FdmTestHelpers.schemaWithFk();
      var mapping = FdmTestHelpers.identityMapping(schema2);
      var inst = FdmTestHelpers.singleElementInstance();
      var idTransform = new IdentityTransform<>(inst, Optional.empty());
      assertThrows(RuntimeException.class, () -> new DeltaTransform<>(mapping, idTransform));
    }
  }

  @Nested
  class TransformBehavior {

    @Test
    void gensIsNotNull() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var mapping = FdmTestHelpers.identityMapping(schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var idTransform = new IdentityTransform<>(inst, Optional.empty());
      var dt = new DeltaTransform<>(mapping, idTransform);
      assertNotNull(dt.gens());
    }

    @Test
    void sksIsNotNull() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var mapping = FdmTestHelpers.identityMapping(schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var idTransform = new IdentityTransform<>(inst, Optional.empty());
      var dt = new DeltaTransform<>(mapping, idTransform);
      assertNotNull(dt.sks());
    }

    @Test
    void srcIsDeltaInstance() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var mapping = FdmTestHelpers.identityMapping(schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var idTransform = new IdentityTransform<>(inst, Optional.empty());
      var dt = new DeltaTransform<>(mapping, idTransform);
      assertTrue(dt.src() instanceof DeltaInstance);
    }

    @Test
    void dstIsDeltaInstance() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var mapping = FdmTestHelpers.identityMapping(schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var idTransform = new IdentityTransform<>(inst, Optional.empty());
      var dt = new DeltaTransform<>(mapping, idTransform);
      assertTrue(dt.dst() instanceof DeltaInstance);
    }
  }
}
