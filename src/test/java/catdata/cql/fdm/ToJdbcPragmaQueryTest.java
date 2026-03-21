package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import catdata.cql.AqlOptions;
import catdata.cql.Query;

class ToJdbcPragmaQueryTest {

  @Nested
  class ToSQLViewsMethod {

    @Test
    void toSQLViewsReturnsNonNull() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var result = ToJdbcPragmaQuery.toSQLViews("src_", "dst_", "id", "VARCHAR(255)", "\"", query, 255);
      assertNotNull(result);
      assertNotNull(result.first);
      assertNotNull(result.second);
    }

    @Test
    void toSQLViewsGeneratesSQLStatements() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var result = ToJdbcPragmaQuery.toSQLViews("src_", "dst_", "id", "VARCHAR(255)", "\"", query, 255);
      assertFalse(result.first.isEmpty());
    }

    @Test
    void toSQLViewsGeneratesDropAndCreate() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var result = ToJdbcPragmaQuery.toSQLViews("src_", "dst_", "id", "VARCHAR(255)", "\"", query, 255);
      assertEquals(2, result.first.size());
      assertTrue(result.first.get(0).contains("drop view"));
      assertTrue(result.first.get(1).contains("create view"));
    }

    @Test
    void toSQLViewsPopulatesSecondMap() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var result = ToJdbcPragmaQuery.toSQLViews("src_", "dst_", "id", "VARCHAR(255)", "\"", query, 255);
      assertTrue(result.second.containsKey("E"));
    }
  }

  @Nested
  class ClassStructure {

    @Test
    void classExtendsPragma() {
      assertTrue(catdata.cql.Pragma.class.isAssignableFrom(ToJdbcPragmaQuery.class));
    }
  }
}
