package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import catdata.cql.exp.AqlMultiDriver;
import catdata.cql.exp.AqlParserFactory;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ToJdbcPragmaInstanceTest {

  @Test
  void classCanBeLoaded() throws ClassNotFoundException {
    Class<?> clazz = Class.forName("catdata.cql.fdm.ToJdbcPragmaInstance");
    assertEquals("ToJdbcPragmaInstance", clazz.getSimpleName());
  }

  @Test
  void classExtendsPragma() {
    assertTrue(catdata.cql.Pragma.class.isAssignableFrom(ToJdbcPragmaInstance.class));
  }

  private static int databases = 0;

  private static synchronized String database(String options) {
    return "jdbc:h2:mem:to_jdbc_pragma_instance_test_" + databases++ + options + ";DB_CLOSE_DELAY=-1";
  }

  /** Runs a program as the IDE does. */
  private static AqlMultiDriver run(String program) throws Exception {
    AqlMultiDriver d = new AqlMultiDriver(AqlParserFactory.getParser().parseProgram(program), null);
    d.start();
    return d;
  }

  /** Runs a query and renders each row as its columns joined by |. */
  private static List<String> rows(String url, String query) throws SQLException {
    List<String> ret = new ArrayList<>();
    try (Connection conn = DriverManager.getConnection(url);
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(query)) {
      int n = rs.getMetaData().getColumnCount();
      while (rs.next()) {
        List<String> cols = new ArrayList<>(n);
        for (int i = 1; i <= n; i++) {
          cols.add(String.valueOf(rs.getObject(i)));
        }
        ret.add(String.join("|", cols));
      }
    }
    return ret;
  }

  /** The type, precision and scale of each column of a table, by column name. */
  private static List<String> columns(String url, String table) throws SQLException {
    return rows(
        url,
        "select column_name, data_type, coalesce(character_maximum_length, numeric_precision),"
            + " numeric_scale from information_schema.columns where table_name = '"
            + table
            + "' order by column_name");
  }

  @Nested
  class Export {

    private static final String PROGRAM =
        "typeside Ty = sql\n"
            + "schema S = literal : Ty { entities E attributes n : E -> Numeric c : E -> Char"
            + " b : E -> Bit t : E -> Tinyint }\n"
            + "instance I = literal : S { generators e1 e2 : E equations"
            + " e1.n = \"9.99\" e2.n = \"123.4\" e1.c = abc e2.c = a e1.b = true e2.b = false"
            + " e1.t = 1 e2.t = 2 }\n";

    @Test
    void sizesTheColumnsToTheValuesItWrites() throws Exception {
      String url = database("");
      run(PROGRAM + "command x = export_jdbc_instance I \"" + url + "\" \"X\"");
      // n is numeric(5,2), so 9.99 is not rounded as in an unsized numeric column
      assertEquals(
          List.of(
              "b|BOOLEAN|null|null",
              "c|CHARACTER|3|null",
              "n|NUMERIC|5|2",
              "t|TINYINT|8|0"),
          columns(url, "XE"));
      assertEquals(
          List.of("9.99|abc", "123.40|a  "), rows(url, "select \"n\", \"c\" from \"XE\" order by \"n\""));
    }

    @Test
    void usesTheOracleColumnTypesWithIsOracle() throws Exception {
      String url = database(";MODE=Oracle");
      run(
          PROGRAM
              + "command x = export_jdbc_instance I \""
              + url
              + "\" \"X\" { options is_oracle = true }");
      // the Oracle dialect has no tinyint, and spells numeric as number
      assertEquals(
          List.of(
              "b|BOOLEAN|null|null",
              "c|CHARACTER|3|null",
              "n|NUMERIC|5|2",
              "t|SMALLINT|16|0"),
          columns(url, "XE"));
    }

    @Test
    void refusesAttributesWithoutAColumnTypeBeforeTouchingAnyTable() throws Exception {
      String url = database("");
      try (Connection conn = DriverManager.getConnection(url);
          Statement stmt = conn.createStatement()) {
        stmt.execute("create table \"XE\"(\"kept\" int)");
        stmt.execute("insert into \"XE\" values (1)");
      }
      AqlMultiDriver d =
          run(
              "typeside Ty = sql\n"
                  + "schema S = literal : Ty { entities E attributes b : E -> Array a : E -> Other"
                  + " i : E -> Integer }\n"
                  + "instance I = literal : S { generators e : E equations e.a = x e.b = y e.i = 1 }\n"
                  + "command x = export_jdbc_instance I \""
                  + url
                  + "\" \"X\"");
      assertEquals(1, d.exn.size(), d.exn.toString());
      String e = d.exn.get(0).getMessage();
      assertTrue(
          e.contains(
              "Cannot JDBC export: H2 has no column type for the attributes\n\n"
                  + "\ta : E -> Other\n\tb : E -> Array\n\nNo table was created."),
          e);
      assertEquals(List.of("1"), rows(url, "select * from \"XE\""));
    }
  }
}
