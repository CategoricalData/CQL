package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.Pair;
import catdata.cql.AqlOptions;
import catdata.cql.Query;
import catdata.cql.exp.AqlMultiDriver;
import catdata.cql.exp.AqlParserFactory;
import catdata.cql.exp.Att;
import catdata.cql.exp.Fk;
import catdata.cql.exp.Sym;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ToJdbcPragmaQueryTest {

  @Nested
  class ToSQLViewsMethod {

    @Test
    void toSQLViewsReturnsNonNull() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var result =
          ToJdbcPragmaQuery.toSQLViews("src_", "dst_", "id", "VARCHAR(255)", "\"", query, 255);
      assertNotNull(result);
      assertNotNull(result.first);
      assertNotNull(result.second);
    }

    @Test
    void toSQLViewsGeneratesSQLStatements() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var result =
          ToJdbcPragmaQuery.toSQLViews("src_", "dst_", "id", "VARCHAR(255)", "\"", query, 255);
      assertFalse(result.first.isEmpty());
    }

    @Test
    void toSQLViewsGeneratesDropAndCreate() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var result =
          ToJdbcPragmaQuery.toSQLViews("src_", "dst_", "id", "VARCHAR(255)", "\"", query, 255);
      assertEquals(2, result.first.size());
      assertTrue(result.first.get(0).contains("drop view"));
      assertTrue(result.first.get(1).contains("create view"));
    }

    @Test
    void toSQLViewsPopulatesSecondMap() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var result =
          ToJdbcPragmaQuery.toSQLViews("src_", "dst_", "id", "VARCHAR(255)", "\"", query, 255);
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

  @SuppressWarnings("unchecked")
  private static Query<String, String, Sym, Fk, Att, String, Fk, Att> query(String program)
      throws Exception {
    AqlMultiDriver d = new AqlMultiDriver(AqlParserFactory.getParser().parseProgram(program), null);
    d.start();
    return (Query<String, String, Sym, Fk, Att, String, Fk, Att>) d.env.defs.qs.get("Q");
  }

  private static List<String> views(Query<String, String, Sym, Fk, Att, String, Fk, Att> q) {
    return ToJdbcPragmaQuery.toSQLViews("s_", "d_", "id", "Varchar", "\"", q, 64).first;
  }

  /** The views of query Q of a program, as {@code export_jdbc_query} writes them. */
  private static List<String> views(String program) throws Exception {
    return views(query(program).unnest());
  }

  /**
   * Runs the program against an H2 memory database whose table s_A holds the rows (1, hello) and
   * (2, bye), and returns the program's error and the rows of the view d_X.
   */
  private static Pair<String, List<String>> export(String program, String db) throws Exception {
    String url = "jdbc:h2:mem:" + db + ";DB_CLOSE_DELAY=-1";
    try (Connection c = DriverManager.getConnection(url);
        Statement s = c.createStatement()) {
      s.execute("create table \"s_A\" (\"id\" varchar(64), \"n\" varchar(64))");
      s.execute("insert into \"s_A\" values ('1', 'hello'), ('2', 'bye')");
      AqlMultiDriver d =
          new AqlMultiDriver(
              AqlParserFactory.getParser().parseProgram(program.replace("URL", url)), null);
      d.start();
      List<String> rows = new ArrayList<>();
      if (d.env.exn == null) {
        try (ResultSet rs = s.executeQuery("select * from \"d_X\" order by \"id\"")) {
          while (rs.next()) {
            rows.add(rs.getString(1) + " " + rs.getString(2));
          }
        }
      }
      s.execute("shutdown");
      return new Pair<>(d.env.exn == null ? "" : d.env.exn.getMessage(), rows);
    }
  }

  @Nested
  class OfAProgram {

    private static final String SCHEMAS =
        """
        typeside Ty = sql
        schema S = literal : Ty { entities A B foreign_keys f : A -> B attributes n : A -> Integer }
        """;

    @Test
    void joinsTheIdsOfEveryVariableAndFollowsForeignKeys() throws Exception {
      List<String> v =
          views(
              SCHEMAS
                  + """
                  schema T = literal : Ty { entities X Y foreign_keys g : X -> Y }
                  query Q = literal : S -> T {
                    entity Y -> { from b:B }
                    entity X -> { from a:A c:A foreign_keys g -> { b -> a.f } }
                  }
                  """);
      assertEquals(4, v.size());
      assertEquals("drop view if exists \"d_X\"", v.get(2));
      String x = v.get(3);
      assertTrue(x.startsWith("create view \"d_X\" as   select concat(concat('(a=', "), x);
      assertTrue(x.contains("concat('(c=', concat(cast(c.\"id\" as varchar(64)), ')'))"), x);
      assertTrue(
          x.contains(
              "concat('(b=', concat(cast(cast(a.\"f\" as varchar(64)) as varchar(64)), ')'))"),
          x);
      assertTrue(x.contains("from \"s_A\" as a, \"s_A\" as c"), x);
    }

    @Test
    void refusesAnEmptyFromClause() {
      RuntimeException e =
          assertThrows(
              RuntimeException.class,
              () ->
                  views(
                      SCHEMAS
                          + """
                          schema T = literal : Ty { entities X }
                          query Q = literal : S -> T { entity X -> { } }
                          """));
      assertEquals("Empty from clause doesn't work with sql", e.getMessage());
    }

    /** The command unnests the query first, which already refuses an aggregate. */
    @Test
    void refusesAnAggregate() throws Exception {
      String program =
          "options allow_aggregation_unsafe = true\n"
              + SCHEMAS
              + """
              schema T = literal : Ty { entities Y attributes total : Y -> Integer }
              query Q = literal : S -> T { entity Y -> { from b:B attributes total ->
                from a:A where a.f = b return a.n aggregate 0 lambda x y. "+"(x, y) } }
              """;
      var q = query(program);
      RuntimeException e = assertThrows(RuntimeException.class, q::unnest);
      assertEquals("Unnesting not yet supported with aggregation.", e.getMessage());
      e = assertThrows(RuntimeException.class, () -> views(q));
      assertEquals("SQL translation for aggregation not yet supported.", e.getMessage());
    }

    private static final String EXPORT =
        """
        typeside Ty = sql
        schema S = literal : Ty { entities A attributes n : A -> Varchar }
        schema T = literal : Ty { entities X attributes ATT : X -> Varchar }
        query Q = literal : S -> T { entity X -> { from a:A attributes ATT -> a.n } }
        command c = export_jdbc_query Q "URL" "s_" "d_"
        """;

    @Test
    void createsAViewOverTheSourceTables() throws Exception {
      Pair<String, List<String>> r = export(EXPORT.replace("ATT", "m"), "jdbcquery_view");
      assertEquals("", r.first);
      assertEquals(List.of("(a=1) hello", "(a=2) bye"), r.second);
    }

    @Test
    void reportsTheErrorOfTheDatabase() throws Exception {
      String program = EXPORT.replace("ATT", "m").replace("URL", "jdbc:h2:mem:jdbcquery_empty");
      AqlMultiDriver d =
          new AqlMultiDriver(AqlParserFactory.getParser().parseProgram(program), null);
      d.start();
      String msg = d.env.exn.getMessage();
      assertTrue(msg.contains("Table \"s_A\" not found"), msg);
      var p =
          new ToJdbcPragmaQuery<>(
              "s_", "d_", query(program), "jdbc:h2:mem:unused", AqlOptions.initialOptions);
      assertTrue(p.toString().startsWith("export_jdbc_query drop view"), p.toString());
    }

    /** The check meant to reject this compares attributes with the name of the id column. */
    @Test
    void leavesAnAttributeNamedAsTheIdColumnToTheDatabase() throws Exception {
      Pair<String, List<String>> r = export(EXPORT.replace("ATT", "id"), "jdbcquery_id");
      assertTrue(r.first.contains("Duplicate column name \"id\""), r.first);
    }
  }
}
