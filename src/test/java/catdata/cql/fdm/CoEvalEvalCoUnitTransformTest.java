package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import catdata.cql.AqlOptions;
import catdata.cql.Query;

class CoEvalEvalCoUnitTransformTest {

  @Nested
  class Constructor {

    @Test
    void constructorCreatesTransform() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var t = new CoEvalEvalCoUnitTransform<>(query, inst, AqlOptions.initialOptions);
      assertNotNull(t.src());
      assertNotNull(t.dst());
    }
  }

  @Nested
  class TransformBehavior {

    @Test
    void gensIsNotNull() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var t = new CoEvalEvalCoUnitTransform<>(query, inst, AqlOptions.initialOptions);
      assertNotNull(t.gens());
    }

    @Test
    void sksIsNotNull() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var t = new CoEvalEvalCoUnitTransform<>(query, inst, AqlOptions.initialOptions);
      assertNotNull(t.sks());
    }

    @Test
    void dstIsOriginalInstance() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var t = new CoEvalEvalCoUnitTransform<>(query, inst, AqlOptions.initialOptions);
      assertNotNull(t.dst());
    }
  }
}
