package catdata.provers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.Chc;
import catdata.Pair;
import catdata.cql.Constraints;
import catdata.cql.Head;
import catdata.cql.Instance;
import catdata.cql.Term;
import catdata.cql.TypeSide;
import catdata.cql.exp.AqlMultiDriver;
import catdata.cql.exp.AqlParserFactory;
import catdata.cql.exp.Sym;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for the decision procedure of {@link EgglogProver}, run against stub executables. The
 * prover writes egglog commands to the process and reads its standard error until a check is
 * reported; the stub answers every check the same way and acknowledges the other commands of a
 * question with variables.
 */
class EgglogProverTest {

  @TempDir Path dir;

  private String stub(String name, String answer) throws Exception {
    Path p = dir.resolve(name);
    Files.writeString(
        p,
        """
        #!/bin/sh
        while IFS= read -r line; do
          case "$line" in
            "(check"*) echo "%s" >&2 ;;
            "(push)"*|"(constructor"*|"(univ"*|"(rule ((= var0 ("*) echo ok >&2 ;;
          esac
        done
        """
            .formatted(answer));
    assertTrue(p.toFile().setExecutable(true));
    return p.toString();
  }

  /** The typeside has the constants a b : T, f : T -> T, a = b and f(x) = x. */
  @SuppressWarnings("unchecked")
  private KBTheory<Chc<String, Void>, Head<String, Void, Sym, Void, Void, Void, Void>, String> kb()
      throws Exception {
    AqlMultiDriver d =
        new AqlMultiDriver(
            AqlParserFactory.getParser()
                .parseProgram(
                    "typeside Ty = literal { types T constants a b : T functions f : T -> T"
                        + " equations a = b forall x. f(x) = x options prover = completion }\n"),
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
  void answersByTheCheckThatTheProverReports() throws Exception {
    var kb = kb();
    var yes = new EgglogProver<>(stub("yes", "Checked fact"), kb, 3);
    assertTrue(yes.eq(Map.of(), c("a"), c("b")));
    var no = new EgglogProver<>(stub("no", "Check failed"), kb, 3);
    assertFalse(no.eq(Map.of(), c("a"), c("b")));
    assertTrue(yes.toString().startsWith("Egglog prover\n\n(datatype*"), yes.toString());
    assertFalse(yes.supportsTrivialityCheck());
  }

  /** A variable becomes a constructor declared inside a push, and the answer pops it again. */
  @Test
  void declaresTheVariablesOfAQuestion() throws Exception {
    var kb = kb();
    Map<String, Chc<String, Void>> ctx = Map.of("v", Chc.inLeft("T"));
    @SuppressWarnings("unchecked")
    KBExp<Head<String, Void, Sym, Void, Void, Void, Void>, String> v =
        (KBExp<Head<String, Void, Sym, Void, Void, Void, Void>, String>) (Object) Term.Var("v");
    assertTrue(new EgglogProver<>(stub("yes", "Checked fact"), kb, 3).eq(ctx, v, v));
    assertFalse(new EgglogProver<>(stub("no", "Check failed"), kb, 3).eq(ctx, v, c("a")));
  }

  @Test
  void refusesAMissingExecutableAndNewConstants() throws Exception {
    var kb = kb();
    String missing = dir.resolve("none").toString();
    RuntimeException e =
        assertThrows(RuntimeException.class, () -> new EgglogProver<>(missing, kb, 3));
    assertEquals("File does not exist: " + missing, e.getMessage());
    var p = new EgglogProver<>(stub("yes", "Checked fact"), kb, 3);
    e =
        assertThrows(
            RuntimeException.class,
            () -> p.add(Head.SymHead(Sym.Sym("e", new Pair<>(List.of(), "T"))), Chc.inLeft("T")));
    assertEquals("Not supported yet", e.getMessage());
  }

  /**
   * The theory becomes a datatype per sort, a universe relation per sort, and a rewrite per axiom.
   */
  @Test
  void writesTheTheoryInEgglog() throws Exception {
    String s = EgglogProver.toEgglog(kb());
    assertTrue(s.startsWith("(datatype*"), s);
    assertTrue(s.contains("(relation univT (T))"), s);
    assertTrue(s.contains("(birewrite "), s);
    assertTrue(s.contains(" :when ((univT x))"), s);
  }

  /**
   * The chase of {@code use_egglog_for_chase = true}, run against a stub that records its input and
   * answers the commands that {@code egglogChase} sends with the lines that it reads: a size and
   * the rows of the universe of Person on standard output, the saturation and the checks on
   * standard error. The stub prints what the parser expects, not necessarily what egglog prints.
   */
  @Nested
  class Chase {

    private String stub() throws Exception {
      Path p = dir.resolve("chase");
      Files.writeString(
          p,
          """
          #!/bin/sh
          while IFS= read -r line; do
            printf "%s\\n" "$line" >> "$(dirname "$0")/input.txt"
            case "$line" in
              "(run-schedule"*) echo "Ruleset : search done" >&2 ;;
              "(print-size univPerson"*) echo 2 ;;
              "(print-function univPerson"*) printf '(\\n (univPerson (p1))\\n (univPerson (p2))\\n)\\n\\n' ;;
              "(extract (name(p1)))"*) echo "(Alice)" ;;
              "(extract (name(p2)))"*) echo "(Bob)" ;;
              "(check"*) echo "Check failed" >&2 ;;
            esac
          done
          """);
      assertTrue(p.toFile().setExecutable(true));
      return p.toString();
    }

    private AqlMultiDriver run(String path) throws Exception {
      String program =
          """
          typeside Ty = literal { types String constants Alice Bob : String }
          schema S = literal : Ty { entities Person attributes name : Person -> String }
          instance I = literal : S {
            generators a b : Person equations a.name = Alice b.name = Bob }
          constraints C = literal : S {
            forall p q : Person where p.name = q.name -> where p = q
            forall p : Person -> exists q : Person where q.name = p.name }
          instance J = chase C I { options use_egglog_for_chase = true egglog_path = "%s" }
          """
              .formatted(path);
      AqlMultiDriver d =
          new AqlMultiDriver(AqlParserFactory.getParser().parseProgram(program), null);
      d.start();
      return d;
    }

    @Test
    void readsTheRowsAndTheirAttributesFromTheProver() throws Exception {
      AqlMultiDriver d = run(stub());
      assertNull(d.env.exn);
      String j = d.env.defs.insts.get("J").toString();
      assertTrue(j.contains(".name = Alice"), j);
      assertTrue(j.contains(".name = Bob"), j);
      String input = Files.readString(dir.resolve("input.txt"));
      assertTrue(input.contains("(union (name (a )) (Alice ))"), input);
      assertTrue(input.contains("(= (name p) (name q))) ((union p q)))"), input);
      assertTrue(input.contains("(union (name (R2q p)) (name p))"), input);
      assertTrue(input.contains("(run-schedule (saturate (run)))\n(print-size univPerson)"), input);
      assertTrue(input.contains("(print-function univPerson 2)"), input);
      assertTrue(input.contains("(check (= "), input);
    }

    @Test
    void refusesAMissingExecutable() throws Exception {
      String missing = dir.resolve("none").toString();
      String msg = run(missing).env.exn.getMessage();
      assertTrue(msg.contains("File does not exist: " + missing), msg);
    }

    /**
     * Nothing calls {@code entails}: it translates only its first argument, starts the prover,
     * writes the problem without flushing it and answers false without reading a reply.
     */
    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void neverEntails() throws Exception {
      Path quiet = dir.resolve("quiet");
      Files.writeString(quiet, "#!/bin/sh\nexit 0\n");
      assertTrue(quiet.toFile().setExecutable(true));
      AqlMultiDriver d = run(stub());
      Constraints c = (Constraints) d.env.defs.eds.get("C");
      assertFalse(EgglogProver.entails(c, c, quiet.toString()));
      String missing = dir.resolve("none").toString();
      RuntimeException e =
          assertThrows(RuntimeException.class, () -> EgglogProver.entails(c, c, missing));
      assertEquals("File does not exist: " + missing, e.getMessage());
      String theory = EgglogProver.toEgglog(c, (Instance) d.env.defs.insts.get("I"));
      assertTrue(theory.startsWith("(datatype*"), theory);
      assertTrue(EgglogProver.toEgglog(c, c).contains("((union p q))"));
    }
  }
}
