package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import catdata.cql.AqlOptions;
import catdata.cql.Query;

class CoEvalTransformTest {

  @Nested
  class Constructor {

    @Test
    void constructorSetsSrcAndDst() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var idTransform = new IdentityTransform<>(inst, Optional.empty());
      var coeval = new CoEvalTransform<>(query, idTransform, AqlOptions.initialOptions, AqlOptions.initialOptions);
      assertNotNull(coeval.src());
      assertNotNull(coeval.dst());
    }
  }

  @Nested
  class TransformBehavior {

    @Test
    void srcIsCoEvalInstance() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var idTransform = new IdentityTransform<>(inst, Optional.empty());
      var coeval = new CoEvalTransform<>(query, idTransform, AqlOptions.initialOptions, AqlOptions.initialOptions);
      assertTrue(coeval.src() instanceof CoEvalInstance);
    }

    @Test
    void gensIsNotNull() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var idTransform = new IdentityTransform<>(inst, Optional.empty());
      var coeval = new CoEvalTransform<>(query, idTransform, AqlOptions.initialOptions, AqlOptions.initialOptions);
      assertNotNull(coeval.gens());
    }

    @Test
    void sksIsNotNull() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var idTransform = new IdentityTransform<>(inst, Optional.empty());
      var coeval = new CoEvalTransform<>(query, idTransform, AqlOptions.initialOptions, AqlOptions.initialOptions);
      assertNotNull(coeval.sks());
    }
  }
}
