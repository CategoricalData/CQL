package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import catdata.cql.AqlOptions;
import catdata.cql.Query;

class EvalTransformTest {

  @Nested
  class Constructor {

    @Test
    void constructorSetsSrcAndDst() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var idTransform = new IdentityTransform<>(inst, Optional.empty());
      var eval = new EvalTransform<>(query, idTransform, AqlOptions.initialOptions);
      assertNotNull(eval.src());
      assertNotNull(eval.dst());
    }
  }

  @Nested
  class TransformBehavior {

    @Test
    void srcIsEvalInstance() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var idTransform = new IdentityTransform<>(inst, Optional.empty());
      var eval = new EvalTransform<>(query, idTransform, AqlOptions.initialOptions);
      assertTrue(eval.src() instanceof EvalInstance);
    }

    @Test
    void gensIsNotNull() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var idTransform = new IdentityTransform<>(inst, Optional.empty());
      var eval = new EvalTransform<>(query, idTransform, AqlOptions.initialOptions);
      assertNotNull(eval.gens());
    }

    @Test
    void sksIsNotNull() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var idTransform = new IdentityTransform<>(inst, Optional.empty());
      var eval = new EvalTransform<>(query, idTransform, AqlOptions.initialOptions);
      assertNotNull(eval.sks());
    }
  }
}
