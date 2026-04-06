package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import catdata.cql.AqlOptions;

class SigmaLeftKanAlgebraTest {

  @Nested
  class ViaSigmaInstance {

    @Test
    void algebraComputesCorrectly() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var mapping = FdmTestHelpers.identityMapping(schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var sigma = new SigmaInstance<>(mapping, inst, AqlOptions.initialOptions);
      assertEquals(1, sigma.algebra().size("E"));
    }
  }
}
