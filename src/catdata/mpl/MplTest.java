package catdata.mpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.Environment;
import catdata.LineException;
import catdata.Program;
import catdata.Unit;
import catdata.mpl.Mpl.MplExp;
import catdata.mpl.Mpl.MplExp.MplEval;
import catdata.mpl.Mpl.MplExp.MplSch;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the MPL language mode of the IDE, a language of monoidal theories with a trace,
 * through its parser, its driver and the strict graphs that its viewer draws.
 */
class MplTest {

  /** The built-in example, with its second definition of f renamed so that it parses. */
  private static String example() {
    return new MplExample1().getText().replace("\nf = eval S", "\nf2 = eval S");
  }

  private static Environment<MplObject> run(String text) {
    Program<MplExp<String, String>> p = MplParser.program(text);
    return MplDriver.makeEnv(text, p);
  }

  /** The example defines f twice, and the IDE does not list it because its language is null. */
  @Test
  void rejectsTheBuiltInExampleForItsDuplicateName() {
    var e = new MplExample1();
    assertEquals("Example 1", e.getName());
    assertNull(e.lang());
    RuntimeException ex =
        assertThrows(RuntimeException.class, () -> MplParser.program(e.getText()));
    assertEquals("Duplicate name: f", ex.getMessage());
  }

  @Test
  void evaluatesEachDefinitionOfTheExample() {
    Program<MplExp<String, String>> p = MplParser.program(example());
    assertEquals(List.of("T", "f", "h", "i", "fg", "idA", "idAidA", "tre", "S", "f2"), p.order);
    var env = MplDriver.makeEnv(example(), p);
    assertInstanceOf(MplSch.class, env.get("T"));
    assertInstanceOf(MplSch.class, env.get("S"));
    for (String k : List.of("f", "h", "i", "fg", "idA", "idAidA", "tre", "f2")) {
      var e = assertInstanceOf(MplEval.class, env.get(k));
      assertTrue(e.toString().startsWith("eval "), e.toString());
    }
  }

  /** The viewer turns each term into a strict string diagram, which it also prints in dot. */
  @Test
  @SuppressWarnings("unchecked")
  void drawsEachTermAsAStrictDiagram() {
    var env = run(example());
    for (String k : List.of("f", "h", "i", "fg", "idA", "idAidA", "f2")) {
      var e = (MplEval<String, String>) env.get(k);
      var r = e.a.forget().accept(Unit.unit, new MplStrict<String, String>(e.sch));
      assertTrue(r.third.startsWith("subgraph cluster"), k + ": " + r.third);
      assertTrue(r.third.contains("label=\"" + e.a.forget() + "\""), k + ": " + r.third);
    }
  }

  /** tr e traces C out of e : (A*C) -> (B*C), but the strict typing compares A with B. */
  @Test
  @SuppressWarnings("unchecked")
  void cannotDrawATrace() {
    var e = (MplEval<String, String>) run(example()).get("tre");
    var op = new MplStrict<String, String>(e.sch);
    RuntimeException ex =
        assertThrows(RuntimeException.class, () -> e.a.forget().accept(Unit.unit, op));
    assertEquals(
        "Second component of dom and cod do not match: are A and B in [A, C] -> [B, C]",
        ex.getMessage());
  }

  @Test
  void refusesAnEvaluationInSomethingOtherThanATheory() {
    String text =
        "T = theory { sorts A; symbols f : A -> A; equations; }\n"
            + "g = eval T f\n"
            + "h = eval g f\n";
    LineException e = assertThrows(LineException.class, () -> run(text));
    assertTrue(e.getMessage().contains("Not a theory: g"), e.getMessage());
  }

  /**
   * The viewer no longer calls its second writer, {@code MplStrict2}, which prints each term as
   * nested dot clusters without typing it. It therefore draws the trace that the strict typing
   * rejects, but it has no case for a symmetry, which the second f of the example contains.
   */
  @Test
  @SuppressWarnings("unchecked")
  void drawsEachTermWithTheUnusedSecondWriterExceptASymmetry() {
    var env =
        run(
            example()
                + "\nlr = eval T (rho1 A * lambda1 B)\n"
                + "al = eval T (alpha1 A I B ; alpha2 A I B)\n");
    for (String k : List.of("f", "h", "i", "fg", "idA", "idAidA", "tre", "lr", "al")) {
      var e = (MplEval<String, String>) env.get(k);
      var r = e.a.forget().accept(Unit.unit, new MplStrict2<String, String>(e.sch));
      assertTrue(r.second.startsWith("subgraph cluster"), k + ": " + r.second);
      assertTrue(r.second.contains("label=\"" + e.a.forget() + "\""), k + ": " + r.second);
      assertTrue(r.second.contains(r.first + " [label=\""), k + ": " + r.second);
    }
    var al = (MplEval<String, String>) env.get("al");
    var op = new MplStrict2<String, String>(al.sch);
    assertEquals(List.of("A", "B"), al.a.type(al.sch).first.accept(Unit.unit, op));
    var f2 = (MplEval<String, String>) env.get("f2");
    RuntimeException ex =
        assertThrows(RuntimeException.class, () -> f2.a.forget().accept(Unit.unit, op));
    assertNull(ex.getMessage());
  }
}
