package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class JdbcPragmaTest {

  @Nested
  class Constructor {

    @Test
    void constructorSetsFields() {
      var pragma = new JdbcPragma("jdbc:h2:mem:test", Collections.emptyList());
      assertNotNull(pragma);
    }
  }

  @Nested
  class ToStringMethod {

    @Test
    void toStringBeforeExecute() {
      var pragma = new JdbcPragma("jdbc:h2:mem:test", Collections.emptyList());
      assertNotNull(pragma.toString());
    }
  }

  @Nested
  class Execute {

    /** Runs the statements on an H2 memory database kept open until the test drops it. */
    private String run(String db, List<String> sqls) throws SQLException {
      String url = "jdbc:h2:mem:" + db + ";DB_CLOSE_DELAY=-1";
      try (Connection c = DriverManager.getConnection(url);
          Statement s = c.createStatement()) {
        try {
          var pragma = new JdbcPragma(url, sqls);
          pragma.execute();
          return pragma.toString();
        } finally {
          s.execute("shutdown");
        }
      }
    }

    @Test
    void reportsUpdatesAndResults() throws SQLException {
      String out =
          run(
              "jdbcpragma_ok",
              List.of("create table t (x int)", "insert into t values (1)", "select * from t"));
      assertTrue(out.contains("Updated 1 rows."), out);
      assertTrue(out.contains("END"), out);
    }

    @Test
    void failsOnAStatementTheDatabaseRejects() {
      RuntimeException e =
          assertThrows(
              RuntimeException.class,
              () -> run("jdbcpragma_bad", List.of("select * from nowhere")));
      assertInstanceOf(SQLException.class, e.getCause());
      assertTrue(e.getMessage().contains("Table \"NOWHERE\" not found"), e.getMessage());
    }

    @Test
    void failsWithoutADriver() {
      var pragma = new JdbcPragma("jdbc:nosuchdriver:x", List.of("select 1"));
      RuntimeException e = assertThrows(RuntimeException.class, pragma::execute);
      assertInstanceOf(SQLException.class, e.getCause());
      assertTrue(e.getMessage().contains("No suitable driver found"), e.getMessage());
    }
  }
}
