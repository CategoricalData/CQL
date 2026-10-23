package catdata.cql;

import static catdata.cql.CqlTestHelpers.stderr;
import static catdata.cql.CqlTestHelpers.stdout;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import catdata.ParseException;
import catdata.Program;
import catdata.cql.exp.AqlMultiDriver;
import catdata.cql.exp.AqlParserFactory;
import catdata.cql.exp.Exp;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** AqlCmdLine has no callers; its main runs a program file and prints its queries as SQL. */
class AqlCmdLineTest {

  /** A schema S, a schema T and a query Q : S -> T copying a into s. */
  static final String PROGRAM =
      """
      typeside Ty = sql

      schema S = literal : Ty {
      	entities
      		E
      	attributes
      		a : E -> Varchar
      }

      schema T = literal : Ty {
      	entities
      		R
      	attributes
      		s : R -> Varchar
      }

      query Q = literal : S -> T {
      	entity R -> {
      		from e:E
      		attributes s -> e.a
      	}
      }
      """;

  /** The type Nope does not exist. */
  static final String BROKEN =
      """
      typeside Ty = sql

      schema S = literal : Ty {
      	entities
      		E
      	attributes
      		a : E -> Nope
      }
      """;

  @TempDir Path dir;

  private String write(String name, String text) throws IOException {
    Path p = dir.resolve(name);
    Files.writeString(p, text);
    return p.toString();
  }

  /** Runs a program as the IDE does and returns its query Q. */
  static Query<?, ?, ?, ?, ?, ?, ?, ?> query(String program) throws ParseException {
    Program<Exp<?>> prog = AqlParserFactory.getParser().parseProgram(program);
    AqlMultiDriver d = new AqlMultiDriver(prog, null);
    d.start();
    return (Query<?, ?, ?, ?, ?, ?, ?, ?>) d.env.defs.get("Q", Kind.QUERY);
  }

  @Nested
  class Main {

    @Test
    void isAClassOfStaticMethods() {
      assertEquals(AqlCmdLine.class, new AqlCmdLine().getClass());
    }

    @Test
    void printsEveryQueryAsInserts() throws IOException {
      String path = write("prog.cql", PROGRAM);
      String out = stdout(() -> AqlCmdLine.main(path));
      assertTrue(out.startsWith("query Q = INSERT INTO R "), out);
      assertTrue(out.contains("from E as e"), out);
      assertTrue(out.trim().endsWith(";"), out);
    }

    /** Issue 60: the error is set on the driver's environment, not the one checked. */
    @Test
    void reportsAFailedDefinitionAsHavingNoResult() throws IOException {
      String path = write("broken.cql", BROKEN);
      assertEquals("schema S = no result for S", stdout(() -> AqlCmdLine.main(path)).trim());
    }

    @Test
    void reportsAMissingFile() {
      String path = dir.resolve("missing.cql").toString();
      String out = stdout(() -> stderr(() -> AqlCmdLine.main(path)));
      assertEquals("ERROR " + path + " (No such file or directory)", out.trim());
    }

    @Test
    void printsTheStackTraceWithoutAnArgument() {
      String err = stderr(() -> AqlCmdLine.main());
      assertTrue(err.startsWith("java.lang.ArrayIndexOutOfBoundsException"), err);
    }
  }

  @Nested
  class Sql {

    @Test
    void queryToSqlWritesOneInsertPerTargetEntity() throws ParseException {
      String sql = AqlCmdLine.queryToSql(query(PROGRAM));
      assertTrue(sql.startsWith("INSERT INTO R   select "), sql);
      assertTrue(sql.contains("as varchar(255)"), sql);
      assertTrue(sql.endsWith(";"), sql);
      assertEquals(1, sql.split("INSERT INTO ", -1).length - 1);
    }
  }
}
