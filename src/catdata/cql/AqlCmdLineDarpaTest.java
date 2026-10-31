package catdata.cql;

import static catdata.cql.CqlTestHelpers.sqlSchema;
import static catdata.cql.CqlTestHelpers.stderr;
import static catdata.cql.CqlTestHelpers.stdout;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import catdata.ParseException;
import catdata.cql.exp.Att;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** AqlCmdLineDarpa has no callers in src. */
class AqlCmdLineDarpaTest {

  @TempDir Path dir;

  private String write(String name, String text) throws IOException {
    Path p = dir.resolve(name);
    Files.writeString(p, text);
    return p.toString();
  }

  @Nested
  class Sql {

    /** The drop quotes the table as the create does, which fixed issue 21. */
    @Test
    void schemaToSqlDropsAndCreatesOneTablePerEntity() {
      var s = sqlSchema(List.of("E"), Map.of(Att.Att("E", "a"), "E"), Map.of());
      assertEquals(
          "drop table if exists \"E\";\n"
              + "create table \"E\"(\"ID\" Integer primary key, \"a\" varchar(255))",
          AqlCmdLineDarpa.schemaToSql(s));
    }

    @Test
    void schemaToSqlWritesNothingForNoEntities() {
      assertEquals("", AqlCmdLineDarpa.schemaToSql(sqlSchema(List.of(), Map.of(), Map.of())));
    }

    @Test
    void queryToSqlQuotesNamesAndCastsIdsToChar() throws ParseException {
      String sql = AqlCmdLineDarpa.queryToSql(AqlCmdLineTest.query(AqlCmdLineTest.PROGRAM));
      assertEquals(
          "INSERT INTO R   select concat('(e=', concat(cast(e.\"ID\" as char), ')')) as \"ID\","
              + " e.\"a\" as \"s\"\nfrom \"E\" as e\n ;",
          sql);
    }
  }

  @Nested
  class Main {

    @Test
    void isAClassOfStaticMethods() {
      assertEquals(AqlCmdLineDarpa.class, new AqlCmdLineDarpa().getClass());
    }

    @Test
    void printsNothingForAProgramThatRuns() throws IOException {
      String path = write("prog.cql", AqlCmdLineTest.PROGRAM);
      String[] err = new String[1];
      String out = stdout(() -> err[0] = stderr(() -> AqlCmdLineDarpa.main(new String[] {path})));
      assertEquals("", out);
      assertFalse(err[0].contains("Exception"), err[0]);
    }

    @Test
    void printsTheErrorOfAFailedDefinition() throws IOException {
      String path = write("broken.cql", AqlCmdLineTest.BROKEN);
      String err = stderr(() -> AqlCmdLineDarpa.main(new String[] {path}));
      assertTrue(err.contains("Nope"), err);
    }

    @Test
    void printsTheErrorOfAMissingFile() {
      String path = dir.resolve("missing.cql").toString();
      String err = stderr(() -> AqlCmdLineDarpa.main(new String[] {path}));
      assertTrue(err.startsWith("java.io.FileNotFoundException: " + path), err);
    }
  }
}
