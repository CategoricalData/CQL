package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import catdata.cql.AqlOptions;

class InitialAlgebraTest {

  @Nested
  class ViaSigmaInstance {

    @Test
    void initialAlgebraCreatedBySigma() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var mapping = FdmTestHelpers.identityMapping(schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var sigma = new SigmaInstance<>(mapping, inst, AqlOptions.initialOptions);
      // SigmaInstance internally creates an InitialAlgebra
      assertNotNull(sigma.algebra());
      assertTrue(sigma.algebra().size("E") > 0);
    }
  }
}
