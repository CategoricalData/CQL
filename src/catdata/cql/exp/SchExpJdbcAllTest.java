package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import catdata.cql.Kind;
import catdata.cql.Schema;
import org.junit.jupiter.api.Test;

class SchExpJdbcAllTest {

  private static int databases = 0;

  /** Imports a fresh in-memory H2 database holding one table, with the program's options. */
  @SuppressWarnings("unchecked")
  private static synchronized Schema<String, String, Sym, Fk, Att> importAll(String options)
      throws Exception {
    String url = "jdbc:h2:mem:sch_exp_jdbc_all_test_" + databases++ + ";DB_CLOSE_DELAY=-1";
    try (Connection conn = DriverManager.getConnection(url);
        Statement stmt = conn.createStatement()) {
      stmt.execute("create table T(i int, b boolean, d decimal(5,2), c clob, j json)");
      // xdb is a system schema of Oracle, whose tables are skipped
      stmt.execute("create schema xdb");
      stmt.execute("create table xdb.U(i int)");
    }
    AqlMultiDriver d =
        new AqlMultiDriver(
            AqlParserFactory.getParser()
                .parseProgram(
                    options
                        + "schema S = import_jdbc_all \""
                        + url
                        + "\" { options allow_sql_import_all_unsafe = true }"),
            null);
    d.start();
    assertTrue(d.exn.isEmpty(), d.exn.toString());
    return (Schema<String, String, Sym, Fk, Att>) d.env.defs.get("S", Kind.SCHEMA);
  }

  private static Map<String, String> types(Schema<String, String, Sym, Fk, Att> s) {
    Map<String, String> types = new HashMap<>();
    s.atts.forEach((att, p) -> types.put(att.str, p.second));
    return types;
  }

  @Test
  void importsTheUserTablesWithTheTypesOfTheirJdbcTypes() throws Exception {
    Schema<String, String, Sym, Fk, Att> s = importAll("");
    // H2 2 reports its INFORMATION_SCHEMA tables as tables, which are skipped like XDB.U
    assertEquals(Set.of("PUBLIC.T"), s.ens);
    assertEquals(
        Map.of("I", "Integer", "B", "Boolean", "D", "Decimal", "C", "Clob", "J", "Other"),
        types(s));
  }

  @Test
  void prefixesTheColumnsWithTheTableForHive() throws Exception {
    assertEquals(
        Map.of(
            "T.I", "Integer", "T.B", "Boolean", "T.D", "Decimal", "T.C", "Clob", "T.J", "Other"),
        types(importAll("options jdbc_is_hive = true\n")));
  }
}
