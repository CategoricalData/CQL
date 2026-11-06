package catdata.provers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.Chc;
import catdata.Pair;
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
 * Unit tests for {@link EProver}, run against stub executables that print what E would: the prover
 * only scans the output for its proof lines.
 */
class EProverTest {

  @TempDir Path dir;

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
        (Object) Term.Sym(Sym.Sym(name, new Pair<>(List.of(), "T")), List.of());
  }

  @Test
  void answersByTheLineThatTheProverPrints() throws Exception {
    var kb = kb();
    for (boolean auto : List.of(true, false)) {
      var yes = new EProver<>(stub("yes", "# Proof found!"), kb, 1, auto);
      assertTrue(yes.eq(Map.of(), c("a"), c("b")));
      var no = new EProver<>(stub("no", "# No proof found!"), kb, 1, auto);
      assertFalse(no.eq(Map.of(), c("a"), c("b")));
    }
    var p = new EProver<>(stub("yes", "# Proof found!"), kb, 1, false);
    assertEquals("E prover", p.toString());
    assertTrue(p.supportsTrivialityCheck());
    p.add(Head.SymHead(Sym.Sym("e", new Pair<>(List.of(), "T"))), Chc.inLeft("T"));
  }

  @Test
  void failsWhenTheProverDoesNotDecideOrIsMissing() throws Exception {
    var kb = kb();
    var p = new EProver<>(stub("undecided", "# Failure: out of time"), kb, 1, false);
    RuntimeException e = assertThrows(RuntimeException.class, () -> p.eq(Map.of(), c("a"), c("b")));
    assertTrue(e.getMessage().startsWith("Theorem prover error: did not decide "), e.getMessage());
    String missing = dir.resolve("none").toString();
    var q = new EProver<>(missing, kb, 1, false);
    e = assertThrows(RuntimeException.class, () -> q.eq(Map.of(), c("a"), c("b")));
    assertEquals("File does not exist: " + missing, e.getMessage());
  }

  @Test
  void checksATptpProblem() throws Exception {
    assertEquals(Optional.of(true), EProver.check(stub("t", "# Proof found!"), 1, "", true).first);
    assertEquals(
        Optional.of(false), EProver.check(stub("f", "# No proof found!"), 1, "", false).first);
    assertEquals(Optional.empty(), EProver.check(stub("e", "# Failure: x"), 1, "", false).first);
    RuntimeException e =
        assertThrows(
            RuntimeException.class, () -> EProver.check(stub("n", "nothing"), 1, "", false));
    assertEquals("0Internal theorem prover anomaly.", e.getMessage());
    String missing = dir.resolve("none").toString();
    e = assertThrows(RuntimeException.class, () -> EProver.check(missing, 1, "", false));
    assertEquals("File does not exist: " + missing, e.getMessage());
  }
}
