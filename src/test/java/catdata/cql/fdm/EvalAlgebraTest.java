package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import catdata.cql.AqlOptions;
import catdata.cql.Query;

class EvalAlgebraTest {

  @Nested
  class ViaEvalInstance {

    @Test
    void algebraSchemaMatchesQuery() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var eval = new EvalInstance<>(query, inst, AqlOptions.initialOptions);
      assertEquals(schema.ens, eval.algebra().schema().ens);
    }

    @Test
    void algebraEnReturnsElements() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var eval = new EvalInstance<>(query, inst, AqlOptions.initialOptions);
      assertTrue(eval.algebra().en("E").iterator().hasNext());
    }

    @Test
    void algebraSizeMatchesInput() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var eval = new EvalInstance<>(query, inst, AqlOptions.initialOptions);
      assertEquals(1, eval.algebra().size("E"));
    }

    @Test
    void algebraToStringProverNotNull() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var eval = new EvalInstance<>(query, inst, AqlOptions.initialOptions);
      assertNotNull(eval.algebra().toStringProver());
    }
  }
}
