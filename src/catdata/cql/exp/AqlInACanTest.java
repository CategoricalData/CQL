package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AqlInACan}, the runner behind web-CQL: it evaluates a program without its
 * commands and returns one HTML page, with a table for each instance and the text of every other
 * definition.
 */
class AqlInACanTest {

  private static final String PROGRAM =
      """
      typeside Ty = literal { types String constants x : String }
      schema S = literal : Ty { entities A attributes n : A -> String }
      instance I = literal : S { generators a : A equations a.n = x }
      """;

  @Test
  void rendersEachDefinition() {
    String html = AqlInACan.openCan(PROGRAM);
    assertTrue(html.startsWith("<!DOCTYPE html><html><head>"), html);
    assertTrue(html.endsWith("</body></html>"), html);
    assertTrue(html.contains("<h2>Ty = </h2><pre>"), html);
    assertTrue(html.contains("<h2>S = </h2><pre>entities"), html);
    assertTrue(html.contains("<h2>I = </h2>"), html);
    assertTrue(html.contains("<table"), html);
  }

  @Test
  void refusesToRunACommand() {
    String html = AqlInACan.openCan(PROGRAM + "command c = exec_js { \"1\" }\n");
    assertEquals("ERROR Commands disabled in web-CQL", html);
  }

  @Test
  void reportsAProgramThatDoesNotParse() {
    assertTrue(AqlInACan.openCan("schema S = = ").startsWith("ERROR "));
  }

  @Test
  void mainPrintsThePage() {
    PrintStream out = System.out;
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    System.setOut(new PrintStream(buf, true, StandardCharsets.UTF_8));
    try {
      AqlInACan.main(PROGRAM);
    } finally {
      System.setOut(out);
    }
    assertTrue(buf.toString(StandardCharsets.UTF_8).startsWith("<!DOCTYPE html>"));
  }

  /** Without an argument main fails on args[0] and prints the stack trace. */
  @Test
  void mainWithoutAProgramPrintsNothing() {
    PrintStream out = System.out;
    PrintStream err = System.err;
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    System.setOut(new PrintStream(buf, true, StandardCharsets.UTF_8));
    System.setErr(new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8));
    try {
      AqlInACan.main();
    } finally {
      System.setOut(out);
      System.setErr(err);
    }
    assertEquals("", buf.toString(StandardCharsets.UTF_8));
  }
}
