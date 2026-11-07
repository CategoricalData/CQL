package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.LineException;
import catdata.ParseException;
import catdata.cql.Kind;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AqlMultiDriver}, which evaluates a program on worker threads, as the IDE's
 * Run does, reusing the values of a previous run for the definitions that did not change.
 */
class AqlMultiDriverTest {

  private static final String PROGRAM =
      """
      typeside Ty = literal { types String }
      schema S = literal : Ty { entities A }
      schema T = literal : Ty { entities B }
      """;

  private static AqlMultiDriver run(String program, AqlEnv last) throws Exception {
    AqlMultiDriver d = new AqlMultiDriver(ExpTestHelpers.parse(program), last);
    d.start();
    return d;
  }

  @Nested
  class Imports {

    private Path file(String text) throws Exception {
      Path p = Files.createTempFile("imported", ".cql");
      Files.writeString(p, text);
      return p;
    }

    @Test
    void addsTheDefinitionsOfAnImportedFile() throws Exception {
      Path p = file("typeside Ty = literal { types String }\n");
      AqlMultiDriver d =
          run("imports \"" + p + "\"\ntypeside Ty2 = literal { types Integer }\n", null);
      assertTrue(d.exn.isEmpty(), d.exn.toString());
      assertTrue(d.env.defs.tys.containsKey("Ty"));
      assertTrue(d.env.defs.tys.containsKey("Ty2"));
      assertEquals(List.of("Ty2", "Ty"), d.env.prog.order);
    }

    /**
     * Imported definitions are added after every definition of the program, so the program cannot
     * refer to them: the typing reaches S before Ty.
     */
    @Test
    void cannotReferToAnImportedDefinition() throws Exception {
      Path p = file("typeside Ty = literal { types String }\n");
      LineException e =
          assertThrows(
              LineException.class,
              () -> run("imports \"" + p + "\"\nschema S = literal : Ty { entities A }\n", null));
      assertEquals("Undefined variable: Ty", e.getMessage());
    }

    @Test
    void followsImportsOfImports() throws Exception {
      Path inner = file("typeside Ty = literal { types String }\n");
      Path outer = file("imports \"" + inner + "\"\ntypeside Ty2 = literal { types Integer }\n");
      AqlMultiDriver d = run("imports \"" + outer + "\"\n", null);
      assertTrue(d.exn.isEmpty(), d.exn.toString());
      assertEquals(List.of("Ty2", "Ty"), d.env.prog.order);
    }

    /**
     * The definition is added to the order a second time, so it is evaluated twice, and the run
     * fails when the two evaluations record different times.
     */
    @Test
    void evaluatesTheSameDefinitionTwice() throws Exception {
      Path p = file("typeside Ty = literal { types String }\n");
      AqlMultiDriver d =
          run("imports \"" + p + "\"\ntypeside Ty = literal { types String }\n", null);
      assertEquals(List.of("Ty", "Ty"), d.env.prog.order);
      if (d.env.exn != null) {
        assertTrue(
            d.env.exn.getMessage().startsWith("Conflict on Ty, was "), d.env.exn.getMessage());
      }
    }

    @Test
    void rejectsAConflictingDefinition() throws Exception {
      Path p = file("typeside Ty = literal { types String }\n");
      RuntimeException e =
          assertThrows(
              RuntimeException.class,
              () -> run("imports \"" + p + "\"\ntypeside Ty = literal { types Integer }\n", null));
      assertTrue(
          e.getMessage()
              .startsWith(
                  "Nested Import Error (line numbers refer to import file "
                      + p
                      + "):\n\n"
                      + "Imported definition already exists: Ty"),
          e.getMessage());
    }

    @Test
    void rejectsAConflictingOption() throws Exception {
      Path p = file("options timeout = 5\ntypeside Ty = literal { types String }\n");
      RuntimeException e =
          assertThrows(
              RuntimeException.class,
              () -> run("imports \"" + p + "\"\noptions timeout = 7\n", null));
      assertTrue(
          e.getMessage().contains("Imported option already set: timeout in " + p), e.getMessage());
    }

    @Test
    void rejectsAMissingFileWhenParsing() {
      ParseException e =
          assertThrows(ParseException.class, () -> run("imports \"/nonexistent/x.cql\"\n", null));
      assertTrue(
          e.getMessage().contains("File does not exist: /nonexistent/x.cql"), e.getMessage());
    }

    @Test
    void reportsAFileItCannotParse() throws Exception {
      Path p = file("typeside Ty = literal {\n");
      RuntimeException e =
          assertThrows(RuntimeException.class, () -> run("imports \"" + p + "\"\n", null));
      assertTrue(
          e.getMessage()
              .startsWith("Nested Import Error (line numbers refer to import file " + p + "):"),
          e.getMessage());
    }
  }

  @Nested
  class Rerun {

    @Test
    void reusesTheValuesThatDidNotChange() throws Exception {
      AqlMultiDriver first = run(PROGRAM, null);
      AqlMultiDriver second = run(PROGRAM.replace("entities B", "entities B C"), first.env);
      assertSame(first.env.defs.schs.get("S"), second.env.defs.schs.get("S"));
      assertNotSame(first.env.defs.schs.get("T"), second.env.defs.schs.get("T"));
      assertEquals(first.env.performance.get("S"), second.env.performance.get("S"));
    }

    @Test
    void recomputesADefinitionWhoseDependencyChanged() throws Exception {
      AqlMultiDriver first = run(PROGRAM, null);
      AqlMultiDriver second =
          run(PROGRAM.replace("types String", "types String Integer"), first.env);
      assertNotSame(first.env.defs.schs.get("S"), second.env.defs.schs.get("S"));
    }

    @Test
    void recomputesADefinitionThatAlwaysReloads() throws Exception {
      String program =
          PROGRAM.replace("{ entities A }", "{ entities A options always_reload = true }");
      AqlMultiDriver first = run(program, null);
      AqlMultiDriver second = run(program, first.env);
      assertNotSame(first.env.defs.schs.get("S"), second.env.defs.schs.get("S"));
      assertSame(first.env.defs.schs.get("T"), second.env.defs.schs.get("T"));
    }

    @Test
    void recomputesEverythingWhenTheOptionsChange() throws Exception {
      AqlMultiDriver first = run(PROGRAM, null);
      AqlMultiDriver second = run("options timeout = 7\n" + PROGRAM, first.env);
      assertNotSame(first.env.defs.schs.get("S"), second.env.defs.schs.get("S"));
    }

    @Test
    void recomputesANewDefinition() throws Exception {
      AqlMultiDriver first = run(PROGRAM, null);
      AqlMultiDriver second = run(PROGRAM + "schema U = literal : Ty { entities C }\n", first.env);
      assertTrue(second.env.defs.schs.containsKey("U"));
      assertSame(first.env.defs.schs.get("S"), second.env.defs.schs.get("S"));
    }
  }

  @Nested
  class Errors {

    @Test
    void namesTheDefinitionThatFailed() throws Exception {
      AqlMultiDriver d = run(PROGRAM + "instance I = literal : S { generators a : Nope }\n", null);
      assertTrue(d.env.exn instanceof LineException, String.valueOf(d.env.exn));
      assertTrue(d.env.exn.getMessage().contains("Nope"), d.env.exn.getMessage());
    }

    @Test
    void recordsAnAbort() throws Exception {
      AqlMultiDriver d = new AqlMultiDriver(ExpTestHelpers.parse(PROGRAM), null);
      d.abort();
      assertEquals(
          "Execution interrupted while waiting.  If execution was not aborted manually, please report.",
          d.exn.get(0).getMessage());
    }

    @Test
    void reportsItsMemoryAndQueues() throws Exception {
      AqlMultiDriver d = run(PROGRAM, null);
      String s = d.toString();
      assertTrue(s.startsWith("JVM Memory: "), s);
      assertTrue(s.endsWith("\nProcessing: \nTodo: "), s);
      assertEquals(Kind.SCHEMA, d.env.prog.exps.get("S").kind());
    }
  }
}
