package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import catdata.cql.AqlOptions;

class SigmaTransformTest {

  @Nested
  class Constructor {

    @Test
    void constructorSetsSrcAndDst() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var mapping = FdmTestHelpers.identityMapping(schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var idTransform = new IdentityTransform<>(inst, Optional.empty());
      var sigma = new SigmaTransform<>(mapping, idTransform, AqlOptions.initialOptions, AqlOptions.initialOptions);
      assertNotNull(sigma.src());
      assertNotNull(sigma.dst());
    }
  }

  @Nested
  class TransformBehavior {

    @Test
    void srcIsSigmaInstance() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var mapping = FdmTestHelpers.identityMapping(schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var idTransform = new IdentityTransform<>(inst, Optional.empty());
      var sigma = new SigmaTransform<>(mapping, idTransform, AqlOptions.initialOptions, AqlOptions.initialOptions);
      assertTrue(sigma.src() instanceof SigmaInstance);
    }

    @Test
    void gensIsNotNull() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var mapping = FdmTestHelpers.identityMapping(schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var idTransform = new IdentityTransform<>(inst, Optional.empty());
      var sigma = new SigmaTransform<>(mapping, idTransform, AqlOptions.initialOptions, AqlOptions.initialOptions);
      assertNotNull(sigma.gens());
    }

    @Test
    void sksIsNotNull() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var mapping = FdmTestHelpers.identityMapping(schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var idTransform = new IdentityTransform<>(inst, Optional.empty());
      var sigma = new SigmaTransform<>(mapping, idTransform, AqlOptions.initialOptions, AqlOptions.initialOptions);
      assertNotNull(sigma.sks());
    }
  }
}
