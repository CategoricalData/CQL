package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.cql.Kind;
import catdata.cql.Pragma;
import catdata.cql.gui.CqlCodeEditor.StaticParserNotice;
import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.parser.ParserNotice;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AqlStatic}, which the CQL editor runs as the program is typed: {@code
 * typeCheck} marks the definitions that do not type-check in red, then {@code validate} evaluates
 * the rest at compile time, marks their errors in magenta and their warnings in orange.
 */
class AqlStaticTest {

  private static final String GOOD =
      """
      typeside Ty = literal { types String }
      schema S = literal : Ty { entities A attributes n : A -> String }
      instance I = literal : S { generators a : A }
      """;

  private record Checked(AqlStatic s, RSyntaxTextArea area) {}

  private static Checked check(String program, boolean validate) throws Exception {
    AqlStatic s = new AqlStatic(ExpTestHelpers.parse(program));
    RSyntaxTextArea area = new RSyntaxTextArea(program);
    s.typeCheck(area);
    if (validate) {
      s.validate(area);
    }
    return new Checked(s, area);
  }

  /** The notices of a check, as line to message, with the color of each. */
  private static Map<Integer, String> notices(AqlStatic s, Color color) {
    Map<Integer, String> ret = new TreeMap<>();
    for (ParserNotice n : s.result.getNotices()) {
      if (n.getColor().equals(color)) {
        ret.put(n.getLine(), n.getMessage());
      }
    }
    return ret;
  }

  @Test
  void typesAGoodProgramWithoutNotices() throws Exception {
    AqlStatic s = check(GOOD, true).s();
    assertTrue(s.result.getNotices().isEmpty(), s.result.getNotices().toString());
    assertTrue(s.exns.values().stream().allMatch(Optional::isEmpty), s.exns.toString());
    assertTrue(s.env.typing.defs.schs.containsKey("S"));
    assertTrue(s.env.defs.schs.containsKey("S"));
  }

  @Test
  void marksATypeErrorAndItsDependentsInRed() throws Exception {
    String program =
        GOOD.replace("instance I = literal : S", "instance I = literal : Nope")
            + "instance J = literal : S { imports I }\n";
    AqlStatic s = check(program, false).s();
    Map<Integer, String> red = notices(s, Color.red);
    assertTrue(red.get(2).contains("Nope"), red.toString());
    assertEquals("Depends on I", red.get(3));
  }

  @Test
  void marksACompileTimeErrorInMagenta() throws Exception {
    String program = GOOD.replace("attributes n : A -> String", "attributes n : A -> Missing");
    AqlStatic s = check(program, true).s();
    Map<Integer, String> magenta = notices(s, Color.magenta);
    assertTrue(magenta.get(1).contains("Missing"), magenta.toString());
    assertTrue(notices(s, Color.red).isEmpty());
  }

  @Test
  void skipsWhatTheTypeCheckRejected() throws Exception {
    String program = GOOD.replace("instance I = literal : S", "instance I = literal : Nope");
    AqlStatic s = check(program, true).s();
    assertTrue(notices(s, Color.magenta).isEmpty(), s.result.getNotices().toString());
    assertTrue(s.env.defs.schs.containsKey("S"));
  }

  @Test
  void runsACheckOfAQueryAtCompileTime() throws Exception {
    String program =
        GOOD
            + """
            schema T = literal : Ty { entities B }
            constraints C = literal : S { }
            constraints D = literal : T { }
            query Q = literal : S -> T { entity B -> { from a:A } }
            command q = check_query Q C D
            """;
    AqlStatic s = check(program, true).s();
    Pragma p = (Pragma) s.env.defs.get("q", Kind.PRAGMA);
    assertEquals("", p.toString());
  }

  @Test
  void triesAQuotientQueryAtCompileTime() throws Exception {
    String program =
        GOOD
            + """
            instance Q = quotient_query I { entity A -> { from x:A y:A where x.n = y.n } }
            """;
    AqlStatic s = check(program, true).s();
    assertEquals(Optional.empty(), s.exns.get("Q"));
  }

  /** A colimit that gives the name E to two classes of entities, {A.E, B.F} and {B.E, C.H}. */
  private static final String CLASH =
      """
      typeside Ty = literal { types String }
      schema A = literal : Ty { entities E }
      schema B = literal : Ty { entities F E }
      schema C = literal : Ty { entities H }
      schema_colimit Q = quotient A + B + C : Ty { entity_equations A.E = B.F B.E = C.H }
      """;

  @Test
  void marksAWarningInOrangeOnTheLineOfItsDefinition() throws Exception {
    AqlStatic s = check(CLASH, true).s();
    List<String> warnings = s.env.get(Kind.SCHEMA_COLIMIT, "Q").warnings();
    assertEquals(1, warnings.size());
    assertEquals(warnings, s.warnings.get("Q"));
    assertEquals(Set.of(4), notices(s, Color.orange).keySet());
    assertEquals(1, s.result.getNotices().size(), s.result.getNotices().toString());
    StaticParserNotice notice = (StaticParserNotice) s.result.getNotices().get(0);
    assertEquals("Warning: " + warnings.get(0), notice.msg);
    assertEquals(ParserNotice.Level.WARNING, notice.getLevel());
    assertEquals(List.of(), s.warnings.get("A"));
  }

  @Test
  void skipsTheNoticeOfAWarningWhoseTextIsGone() throws Exception {
    // The editor parses its text in the background, so the text can be shorter by the time the
    // notices are placed.
    AqlStatic s = new AqlStatic(ExpTestHelpers.parse(CLASH));
    RSyntaxTextArea area = new RSyntaxTextArea("");
    s.typeCheck(area);
    s.validate(area);
    assertEquals(1, s.warnings.get("Q").size());
    assertEquals(List.of(), s.result.getNotices());
  }

  @Test
  void returnsItsResultAsTheParseResult() throws Exception {
    Checked c = check(GOOD, false);
    assertSame(c.s().result, c.s().parse(null, null));
    assertTrue(c.s().toString().startsWith("AqlStatic [result="), c.s().toString());
    assertEquals(List.of(), c.s().result.getNotices());
  }
}
