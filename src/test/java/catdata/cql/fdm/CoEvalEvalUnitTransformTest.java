package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import catdata.cql.AqlOptions;
import catdata.cql.Query;

class CoEvalEvalUnitTransformTest {

  @Nested
  class Constructor {

    @Test
    void constructorCreatesTransform() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var t = new CoEvalEvalUnitTransform<>(query, inst, AqlOptions.initialOptions);
      assertNotNull(t.src());
      assertNotNull(t.dst());
    }
  }

  @Nested
  class TransformBehavior {

    @Test
    void srcIsOriginalInstance() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var t = new CoEvalEvalUnitTransform<>(query, inst, AqlOptions.initialOptions);
      assertNotNull(t.src());
    }

    @Test
    void dstIsEvalInstance() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var t = new CoEvalEvalUnitTransform<>(query, inst, AqlOptions.initialOptions);
      assertTrue(t.dst() instanceof EvalInstance);
    }

    @Test
    void gensIsNotNull() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var t = new CoEvalEvalUnitTransform<>(query, inst, AqlOptions.initialOptions);
      assertNotNull(t.gens());
    }

    @Test
    void sksIsNotNull() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var t = new CoEvalEvalUnitTransform<>(query, inst, AqlOptions.initialOptions);
      assertNotNull(t.sks());
    }
  }
}
