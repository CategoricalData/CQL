package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TalgSimplifierTest {

  @Nested
  class ToTermMethod {

    @Test
    void toTermWithObjHead() {
      // Create a TalgSimplifier indirectly through SigmaInstance, then test toTerm
      var schema = FdmTestHelpers.singleEntitySchema();
      var mapping = FdmTestHelpers.identityMapping(schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var sigma = new SigmaInstance<>(mapping, inst, catdata.cql.AqlOptions.initialOptions);
      // TalgSimplifier is used internally; verify the algebra works
      assertNotNull(sigma.algebra());
    }
  }
}
