package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.Pair;
import catdata.Unit;
import catdata.cql.exp.PragmaExp.PragmaExpVisitor;
import catdata.cql.fdm.ProcPragma;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PragmaExpProc}, {@code exec_cmdline { "cmd" ... }}, the command that runs
 * each command line. Running is tested in {@code ProcPragmaTest}.
 */
class PragmaExpProcTest {

  private static PragmaExpProc exp(List<String> cmds, List<Pair<String, String>> options) {
    return new PragmaExpProc(cmds, options);
  }

  private final PragmaExpProc exp = exp(List.of("echo a", "echo b"), List.of());

  @Test
  void makesAProcessCommand() throws Exception {
    AqlEnv env = ExpTestHelpers.run("").env;
    assertInstanceOf(ProcPragma.class, exp.eval0(env, false));
    assertThrows(IgnoreException.class, () -> exp.eval0(env, true));
  }

  /** The commands run together, and the options are printed with their separators swapped. */
  @Test
  void printsTheCommandsAndTheOptions() {
    assertEquals("exec_cmdline {\"echo a\"\n\"echo b\"}", exp.toString());
    assertEquals(
        "exec_cmdline {\"x\"\n\toptionstimeout\n\t\t5}",
        exp(List.of("x"), List.of(new Pair<>("timeout", "5"))).toString());
  }

  @Test
  void hasNoDependenciesOrParts() {
    assertTrue(exp.deps().isEmpty());
    assertEquals(Unit.unit, exp.type(new AqlTyping()));
    assertEquals(
        Map.of("timeout", "5"), exp(List.of(), List.of(new Pair<>("timeout", "5"))).options());
    List<Exp<?>> seen = new ArrayList<>();
    exp.map(seen::add);
    assertEquals(List.of(exp), seen);
  }

  @Test
  void equalityComparesTheCommandsAndTheOptions() {
    assertEquals(exp, exp);
    assertEquals(exp, exp(List.of("echo a", "echo b"), List.of()));
    assertEquals(exp.hashCode(), exp(List.of("echo a", "echo b"), List.of()).hashCode());
    assertNotEquals(exp, exp(List.of("echo b", "echo a"), List.of()));
    assertNotEquals(exp, exp(List.of("echo a", "echo b"), List.of(new Pair<>("timeout", "5"))));
    PragmaExpProc none = exp(null, List.of());
    assertEquals(none, exp(null, List.of()));
    assertEquals(none.hashCode(), exp(null, List.of()).hashCode());
    assertNotEquals(none, exp);
    assertNotEquals(exp, none);
    assertNotEquals(exp, null);
    assertNotEquals(exp, "exec_cmdline");
  }

  @Test
  @SuppressWarnings("unchecked")
  void acceptsAVisitor() {
    PragmaExpVisitor<String, Unit, RuntimeException> v = mock(PragmaExpVisitor.class);
    when(v.visit(Unit.unit, exp)).thenReturn("visited");
    assertEquals("visited", exp.accept(Unit.unit, v));
  }
}
