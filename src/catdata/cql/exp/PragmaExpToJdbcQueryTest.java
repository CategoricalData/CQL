package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.Pair;
import catdata.Unit;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.Kind;
import catdata.cql.Pragma;
import catdata.cql.exp.PragmaExp.PragmaExpVisitor;
import catdata.cql.exp.QueryExp.QueryExpVar;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PragmaExpToJdbcQuery}, {@code export_jdbc_query Q "url" "src" "dst"}, which
 * creates a view per target entity of Q over the tables, prefixed src, of its source entities.
 */
class PragmaExpToJdbcQueryTest {

  private static final String SCHEMA =
      """
      typeside Ty = sql
      schema S = literal : Ty { entities A attributes n : A -> Varchar }
      query Q = literal : S -> S { entity A -> { from a:A attributes n -> a.n } }
      """;

  /** A database with the source table src_A holding one row. */
  private static String database() throws SQLException {
    return ExpTestHelpers.h2(
        "CREATE TABLE \"src_A\" (\"id\" VARCHAR(255), \"n\" VARCHAR(255))",
        "INSERT INTO \"src_A\" VALUES ('1', 'hello')");
  }

  private static int rows(String url, String view) throws Exception {
    try (Connection c = DriverManager.getConnection(url);
        Statement s = c.createStatement();
        ResultSet r = s.executeQuery("SELECT COUNT(*) FROM \"" + view + "\"")) {
      r.next();
      return r.getInt(1);
    }
  }

  private static PragmaExpToJdbcQuery exp(String url, List<Pair<String, String>> options) {
    return new PragmaExpToJdbcQuery(new QueryExpVar("Q"), url, "src_", "dst_", options);
  }

  private final PragmaExpToJdbcQuery exp = exp("u", List.of());

  @Nested
  class Evaluation {

    @Test
    void createsAViewPerTargetEntity() throws Exception {
      String url = database();
      String program =
          SCHEMA + "command E = export_jdbc_query Q \"" + url + "\" \"src_\" \"dst_\"\n";
      Pragma p = ExpTestHelpers.eval(program, "E", Kind.PRAGMA);
      assertTrue(p.toString().startsWith("export_jdbc_query "), p.toString());
      assertTrue(p.toString().contains("\"src_A\""), p.toString());
      assertEquals(1, rows(url, "dst_A"));
    }

    @Test
    void readsAnEmptyUrlFromTheDefault() throws Exception {
      String url = database();
      String program =
          "options jdbc_default_string = \""
              + url
              + "\"\n"
              + SCHEMA
              + "command E = export_jdbc_query Q \"\" \"src_\" \"dst_\"\n";
      ExpTestHelpers.eval(program, "E", Kind.PRAGMA);
      assertEquals(1, rows(url, "dst_A"));
    }

    @Test
    void isNotEvaluatedAtCompileTime() throws Exception {
      AqlEnv env = ExpTestHelpers.run(SCHEMA).env;
      assertThrows(IgnoreException.class, () -> exp.eval0(env, true));
    }
  }

  @Nested
  class Expression {

    /** The options are printed with their key and value separators swapped. */
    @Test
    void printsTheQueryTheUrlThePrefixesAndTheOptions() {
      assertEquals("export_jdbc_query Q  \"u\" \"src_\" \"dst_\"", exp.toString());
      assertEquals(
          "export_jdbc_query Q  \"u\" \"src_\" \"dst_\" {\n\toptionsvarchar_length\n\t\t9}",
          exp("u", List.of(new Pair<>("varchar_length", "9"))).toString());
    }

    @Test
    void dependsOnTheQuery() throws Exception {
      assertEquals(List.of(new Pair<>("Q", Kind.QUERY)), new ArrayList<>(exp.deps()));
      assertEquals(Unit.unit, exp.type(ExpTestHelpers.typing(SCHEMA)));
      assertEquals(
          Map.of("varchar_length", "9"),
          exp("u", List.of(new Pair<>("varchar_length", "9"))).options());
      assertTrue(exp.allowedOptions().contains(AqlOption.varchar_length));
      List<Exp<?>> seen = new ArrayList<>();
      exp.map(seen::add);
      assertEquals(List.of(exp, new QueryExpVar("Q")), seen);
    }

    @Test
    void equalityComparesEveryPart() {
      assertEquals(exp, exp);
      assertEquals(exp, exp("u", List.of()));
      assertEquals(exp.hashCode(), exp("u", List.of()).hashCode());
      assertNotEquals(exp, exp("v", List.of()));
      assertNotEquals(exp, exp("u", List.of(new Pair<>("varchar_length", "9"))));
      assertNotEquals(
          exp, new PragmaExpToJdbcQuery(new QueryExpVar("P"), "u", "src_", "dst_", List.of()));
      assertNotEquals(
          exp, new PragmaExpToJdbcQuery(new QueryExpVar("Q"), "u", "s_", "dst_", List.of()));
      assertNotEquals(
          exp, new PragmaExpToJdbcQuery(new QueryExpVar("Q"), "u", "src_", "d_", List.of()));
      assertNotEquals(exp, null);
      assertNotEquals(exp, "export_jdbc_query");
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsAVisitor() {
      PragmaExpVisitor<String, Unit, RuntimeException> v = mock(PragmaExpVisitor.class);
      when(v.visit(Unit.unit, exp)).thenReturn("visited");
      assertEquals("visited", exp.accept(Unit.unit, v));
    }
  }
}
