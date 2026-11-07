package catdata.provers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.Chc;
import catdata.cql.Head;
import catdata.cql.Term;
import catdata.cql.TypeSide;
import catdata.cql.exp.AqlMultiDriver;
import catdata.cql.exp.AqlParserFactory;
import catdata.cql.exp.Sym;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link VampireProver}, run against stub executables that print what Vampire would:
 * the prover only scans the output for its SZS status lines.
 */
class VampireProverTest {

  @TempDir Path dir;

  /** An executable that ignores its arguments and prints the given line. */
  private String stub(String name, String line) throws Exception {
    Path p = dir.resolve(name);
    Files.writeString(p, "#!/bin/sh\necho '" + line + "'\n");
    assertTrue(p.toFile().setExecutable(true));
    return p.toString();
  }

  @SuppressWarnings("unchecked")
  private KBTheory<Chc<String, Void>, Head<String, Void, Sym, Void, Void, Void, Void>, String> kb()
      throws Exception {
    AqlMultiDriver d =
        new AqlMultiDriver(
            AqlParserFactory.getParser()
                .parseProgram("typeside Ty = literal { types T constants a b : T }\n"),
            null);
    d.start();
    var ts = (TypeSide<String, Sym>) d.env.defs.tys.get("Ty");
    return (KBTheory<Chc<String, Void>, Head<String, Void, Sym, Void, Void, Void, Void>, String>)
        (Object) ts.collage().toKB();
  }

  @SuppressWarnings("unchecked")
  private static KBExp<Head<String, Void, Sym, Void, Void, Void, Void>, String> c(String name) {
    return (KBExp<Head<String, Void, Sym, Void, Void, Void, Void>, String>)
        (Object) Term.Sym(Sym.Sym(name, new catdata.Pair<>(List.of(), "T")), List.of());
  }

  @Test
  void answersByTheStatusThatTheProverPrints() throws Exception {
    var kb = kb();
    var yes = new VampireProver<>(stub("yes", "% SZS status Theorem"), kb, 1);
    assertTrue(yes.eq(Map.of(), c("a"), c("b")));
    var no = new VampireProver<>(stub("no", "% SZS status CounterSatisfiable"), kb, 1);
    assertFalse(no.eq(Map.of(), c("a"), c("b")));
    assertEquals("Vampire prover", yes.toString());
    assertTrue(yes.supportsTrivialityCheck());
    yes.add(Head.SymHead(Sym.Sym("e", new catdata.Pair<>(List.of(), "T"))), Chc.inLeft("T"));
  }

  @Test
  void failsWhenTheProverDoesNotDecide() throws Exception {
    var kb = kb();
    for (String line : List.of("% Exception", "something else")) {
      var p = new VampireProver<>(stub("undecided", line), kb, 1);
      RuntimeException e =
          assertThrows(RuntimeException.class, () -> p.eq(Map.of(), c("a"), c("b")));
      assertTrue(
          e.getMessage().startsWith("Theorem prover error: did not decide "), e.getMessage());
    }
    var missing = new VampireProver<>(dir.resolve("none").toString(), kb, 1);
    RuntimeException e =
        assertThrows(RuntimeException.class, () -> missing.eq(Map.of(), c("a"), c("b")));
    assertTrue(e.getMessage().startsWith("File does not exist: "), e.getMessage());
  }

  @Test
  void checksATptpProblem() throws Exception {
    assertEquals(
        Optional.of(true),
        VampireProver.check(stub("t", "% SZS status Theorem"), 1, "fof(x,axiom,$true).").first);
    assertEquals(
        Optional.of(false),
        VampireProver.check(stub("f", "% SZS status CounterSatisfiable"), 1, "").first);
    assertEquals(Optional.empty(), VampireProver.check(stub("e", "% Exception"), 1, "").first);
    RuntimeException e =
        assertThrows(
            RuntimeException.class, () -> VampireProver.check(stub("n", "nothing"), 1, ""));
    assertEquals("0Internal theorem prover anomaly.", e.getMessage());
  }
}
