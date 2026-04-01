package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import catdata.cql.AqlOptions;
import catdata.cql.Query;

class EvalInstanceTest {

  @Nested
  class Constructor {

    @Test
    void constructorSetsSchema() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var eval = new EvalInstance<>(query, inst, AqlOptions.initialOptions);
      assertEquals(schema.ens, eval.schema().ens);
    }
  }

  @Nested
  class InstanceBehavior {

    @Test
    void algebraIsNotNull() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var eval = new EvalInstance<>(query, inst, AqlOptions.initialOptions);
      assertNotNull(eval.algebra());
    }

    @Test
    void dpIsNotNull() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var eval = new EvalInstance<>(query, inst, AqlOptions.initialOptions);
      assertNotNull(eval.dp());
    }

    @Test
    void gensIsNotNull() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var eval = new EvalInstance<>(query, inst, AqlOptions.initialOptions);
      assertNotNull(eval.gens());
    }

    @Test
    void sksIsNotNull() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var eval = new EvalInstance<>(query, inst, AqlOptions.initialOptions);
      assertNotNull(eval.sks());
    }

    @Test
    void algebraPreservesSize() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var eval = new EvalInstance<>(query, inst, AqlOptions.initialOptions);
      assertEquals(1, eval.algebra().size("E"));
    }
  }
}
