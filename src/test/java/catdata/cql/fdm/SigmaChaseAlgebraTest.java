package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import catdata.Pair;
import catdata.cql.AqlOptions;

class SigmaChaseAlgebraTest {

  @Nested
  class Constructor {

    @Test
    void constructorCreatesAlgebra() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var mapping = FdmTestHelpers.identityMapping(schema);
      var inst = FdmTestHelpers.singleElementInstance();
      Map<String, Set<Pair<String, String>>> extra = new HashMap<>();
      extra.put("E", Collections.emptySet());
      var alg = new SigmaChaseAlgebra<>(mapping, inst, extra, AqlOptions.initialOptions);
      assertNotNull(alg);
    }

    @Test
    void schemaReturnsMappingDst() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var mapping = FdmTestHelpers.identityMapping(schema);
      var inst = FdmTestHelpers.singleElementInstance();
      Map<String, Set<Pair<String, String>>> extra = new HashMap<>();
      extra.put("E", Collections.emptySet());
      var alg = new SigmaChaseAlgebra<>(mapping, inst, extra, AqlOptions.initialOptions);
      assertEquals(schema.ens, alg.schema().ens);
    }

    @Test
    void algebraHasElements() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var mapping = FdmTestHelpers.identityMapping(schema);
      var inst = FdmTestHelpers.singleElementInstance();
      Map<String, Set<Pair<String, String>>> extra = new HashMap<>();
      extra.put("E", Collections.emptySet());
      var alg = new SigmaChaseAlgebra<>(mapping, inst, extra, AqlOptions.initialOptions);
      assertEquals(1, alg.size("E"));
    }
  }

  @Nested
  class TheInstance {

    @Test
    void theInstIsNotNull() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var mapping = FdmTestHelpers.identityMapping(schema);
      var inst = FdmTestHelpers.singleElementInstance();
      Map<String, Set<Pair<String, String>>> extra = new HashMap<>();
      extra.put("E", Collections.emptySet());
      var alg = new SigmaChaseAlgebra<>(mapping, inst, extra, AqlOptions.initialOptions);
      assertNotNull(alg.theInst);
    }

    @Test
    void theInstSchemaMatchesDst() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var mapping = FdmTestHelpers.identityMapping(schema);
      var inst = FdmTestHelpers.singleElementInstance();
      Map<String, Set<Pair<String, String>>> extra = new HashMap<>();
      extra.put("E", Collections.emptySet());
      var alg = new SigmaChaseAlgebra<>(mapping, inst, extra, AqlOptions.initialOptions);
      assertEquals(schema.ens, alg.theInst.schema().ens);
    }
  }
}
