package catdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.jparsec.error.ParserException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link Program}, the declarations of a parsed program in order. */
class ProgramTest {

  /** The text of two declarations of A, the second on line 3; both names are at column 10. */
  private static final String TEXT = "typeside A = empty\n\ntypeside A = empty\n";

  private static Program<String> program(List<Pair<String, String>> options) {
    return new Program<>(
        List.of(new Quad<>("A", 9, "x", 13), new Quad<>("B", 29, "y", 33)),
        TEXT,
        options,
        e -> e + "!",
        List.of());
  }

  @Nested
  class Declarations {

    @Test
    void keepsTheDeclarationsInOrder() {
      var p = program(List.of());
      assertEquals(List.of("A", "B"), p.order);
      assertEquals(List.of("A", "B"), List.copyOf(p.keySet()));
      assertEquals(Map.of("x", "A", "y", "B"), p.invert());
      assertEquals(9, p.getLine("A"));
      assertEquals("x!", p.kind("A"));
      assertEquals("A = x\n\nB = y\n\n", p.toString());
    }

    @Test
    void readsItsTimeoutFromItsOptions() {
      assertEquals(30, program(List.of()).timeout());
      assertEquals(5, program(List.of(new Pair<>("timeout", "5"))).timeout());
    }

    @Test
    void comparesItsDeclarationsAndOptions() {
      var p = program(List.of());
      var q = program(List.of());
      assertEquals(p, p);
      assertEquals(p, q);
      assertEquals(p.hashCode(), q.hashCode());
      assertTrue(p.comp(q));
      assertTrue(p.comp(p));
      assertFalse(p.comp(null));
      assertNotEquals(p, null);
      assertNotEquals(p, "program");
      var r = program(List.of(new Pair<>("timeout", "5")));
      assertNotEquals(p, r);
      assertFalse(p.comp(r));
      var s =
          new Program<>(List.of(new Quad<>("A", 9, "z", 13)), TEXT, List.of(), e -> e, List.of());
      assertNotEquals(p, s);
      assertFalse(p.comp(s));
    }
  }

  @Nested
  class Errors {

    @Test
    void rejectsADuplicateNameWithoutText() {
      List<Quad<String, Integer, String, Integer>> decls =
          List.of(new Quad<>("A", 9, "x", 13), new Quad<>("A", 29, "y", 33));
      RuntimeException e = assertThrows(RuntimeException.class, () -> new Program<>(decls, null));
      assertEquals("Duplicate name: A", e.getMessage());
    }

    /**
     * The first A is placed at line 1, column 10. The newline test reads one character ahead of the
     * count, so the second, at line 3, column 10, is placed at column 12.
     */
    @Test
    void placesADuplicateNameTwoColumnsTooFarAfterANewline() {
      List<Quad<String, Integer, String, Integer>> decls =
          List.of(new Quad<>("A", 9, "x", 13), new Quad<>("A", 29, "y", 33));
      ParserException e = assertThrows(ParserException.class, () -> new Program<>(decls, TEXT));
      assertTrue(e.getMessage().contains("(Duplicate name: A) line 3, column 12"), e.getMessage());
      var details = e.getErrorDetails();
      assertEquals("A", details.getEncountered());
      assertEquals("Other occurance: line 1, column 10", details.getFailureMessage());
      assertEquals(9, details.getIndex());
      assertEquals(List.of(), details.getExpected());
      assertEquals("", details.getUnexpected());
    }

    @Test
    void rejectsBadOptionsAndMissingImports() {
      List<Quad<String, Integer, String, Integer>> decls = List.of(new Quad<>("A", 9, "x", 13));
      ParserException e =
          assertThrows(
              ParserException.class,
              () ->
                  new Program<>(
                      decls, TEXT, List.of(new Pair<>("timeout", "x")), z -> z, List.of()));
      assertTrue(e.getMessage().contains("Error parsing global options."), e.getMessage());
      e =
          assertThrows(
              ParserException.class,
              () ->
                  new Program<>(
                      decls, TEXT, List.of(), z -> z, List.of("/no/such/file/for/cql/tests")));
      assertTrue(
          e.getMessage().contains("File does not exist: /no/such/file/for/cql/tests"),
          e.getMessage());
    }
  }
}
