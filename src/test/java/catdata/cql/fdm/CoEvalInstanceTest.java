package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import catdata.cql.AqlOptions;
import catdata.cql.Query;

class CoEvalInstanceTest {

  @Nested
  class Constructor {

    @Test
    void constructorSetsSchema() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var coeval = new CoEvalInstance<>(query, inst, AqlOptions.initialOptions);
      assertEquals(schema.ens, coeval.schema().ens);
    }
  }

  @Nested
  class InstanceBehavior {

    @Test
    void algebraIsNotNull() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var coeval = new CoEvalInstance<>(query, inst, AqlOptions.initialOptions);
      assertNotNull(coeval.algebra());
    }

    @Test
    void dpIsNotNull() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var coeval = new CoEvalInstance<>(query, inst, AqlOptions.initialOptions);
      assertNotNull(coeval.dp());
    }

    @Test
    void gensIsNotNull() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var coeval = new CoEvalInstance<>(query, inst, AqlOptions.initialOptions);
      assertNotNull(coeval.gens());
    }

    @Test
    void sksIsNotNull() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var coeval = new CoEvalInstance<>(query, inst, AqlOptions.initialOptions);
      assertNotNull(coeval.sks());
    }
  }
}
