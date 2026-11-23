package catdata.provers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import catdata.Chc;
import catdata.Pair;
import catdata.cql.Constraints;
import catdata.cql.Head;
import catdata.cql.Instance;
import catdata.cql.Term;
import catdata.cql.TypeSide;
import catdata.cql.exp.AqlMultiDriver;
import catdata.cql.exp.AqlParserFactory;
import catdata.cql.exp.Att;
import catdata.cql.exp.Fk;
import catdata.cql.exp.Sym;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for the decision procedure of {@link EgglogProver}, run against stub executables and,
 * in {@link AgainstEgglog}, against a real egglog. The prover writes egglog commands to the process
 * and reads its standard error until a check is reported; the stub answers every check the same way
 * and acknowledges the other commands of a question with variables.
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
  private KBTheory<Chc<String, Void>, Head<String, Void, Sym, Void, Void, Void, Void>, String> kb()
      throws Exception {
    return kb(
        "typeside Ty = literal { types T constants a b : T functions f : T -> T"
            + " equations a = b forall x. f(x) = x options prover = completion }\n");
  }

  /** The theory of the typeside Ty of a program. */
  @SuppressWarnings("unchecked")
  private static KBTheory<
          Chc<String, Void>, Head<String, Void, Sym, Void, Void, Void, Void>, String>
      kb(String program) throws Exception {
    return (KBTheory<Chc<String, Void>, Head<String, Void, Sym, Void, Void, Void, Void>, String>)
        (Object) typeside(program).collage().toKB();
  }

  @SuppressWarnings("unchecked")
  private static TypeSide<String, Sym> typeside(String program) throws Exception {
    AqlMultiDriver d = new AqlMultiDriver(AqlParserFactory.getParser().parseProgram(program), null);
    d.start();
    return (TypeSide<String, Sym>) d.env.defs.tys.get("Ty");
  }

  private static KBExp<Head<String, Void, Sym, Void, Void, Void, Void>, String> c(String name) {
    return kbExp(constant(name));
  }

  /** The constant of that name, of type T. */
  private static Term<String, Void, Sym, Void, Void, Void, Void> constant(String name) {
    return Term.Sym(Sym.Sym(name, new Pair<>(List.of(), "T")), List.of());
  }

  /** The term f(t), for the function f : T -> T. */
  private static Term<String, Void, Sym, Void, Void, Void, Void> f(
      Term<String, Void, Sym, Void, Void, Void, Void> t) {
    return Term.Sym(Sym.Sym("f", new Pair<>(List.of("T"), "T")), List.of(t));
  }

  /** A term of the typeside, as the prover takes it. */
  @SuppressWarnings("unchecked")
  private static KBExp<Head<String, Void, Sym, Void, Void, Void, Void>, String> kbExp(
      Term<String, Void, Sym, Void, Void, Void, Void> t) {
    return (KBExp<Head<String, Void, Sym, Void, Void, Void, Void>, String>) (Object) t;
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

  /**
   * The prover and the chase against a real egglog: the executable that the environment variable
   * {@code CQL_EGGLOG} names, which CI builds from egglog 0.5.0. Without the variable these tests
   * are skipped. They compare egglog with CQL's own provers and chase, so they keep their meaning
   * for any egglog release that CQL supports.
   */
  @Nested
  class AgainstEgglog {

    /** a = b, f is an involution, and c is equal to neither a nor b. */
    private static final String INVOLUTION =
        "typeside Ty = literal { types T constants a b c : T functions f : T -> T"
            + " equations a = b forall x. f(f(x)) = x options prover = completion }\n";

    /** The typeside of the chases. egglog declares a sort String itself, so the type is Name. */
    private static final String NAMES =
        """
        typeside Ty = literal { types Name constants Alice Bob Sales Ops : Name }
        """;

    /** Two people with distinct names, and a constraint whose witness every person already is. */
    private static final String MERGE =
        NAMES
            + """
            schema S = literal : Ty { entities Person attributes name : Person -> Name }
            instance I = literal : S {
              generators a b : Person equations a.name = Alice b.name = Bob }
            constraints C = literal : S {
              forall p q : Person where p.name = q.name -> where p = q
              forall p : Person -> exists q : Person where q.name = p.name }
            instance Native = chase C I
            instance Egg = chase C I { options use_egglog_for_chase = true egglog_path = "%s" }
            """;

    /** Sales has two members and Ops none; every department needs a member. */
    private static final String WITNESS =
        NAMES
            + """
            schema S = literal : Ty {
              entities Person Dept
              foreign_keys works : Person -> Dept
              attributes name : Person -> Name dname : Dept -> Name }
            instance I = literal : S {
              generators a b : Person d e : Dept
              equations a.name = Alice b.name = Bob a.works = d b.works = d
                d.dname = Sales e.dname = Ops }
            constraints C = literal : S {
              forall p q : Person where p.name = q.name -> where p = q
              forall x : Dept -> exists m : Person where m.works = x }
            instance Native = chase C I
            instance Egg = chase C I { options use_egglog_for_chase = true egglog_path = "%s" }
            """;

    private final String egglog = System.getenv("CQL_EGGLOG");

    @BeforeEach
    void needsEgglog() {
      assumeTrue(
          egglog != null && Files.isExecutable(Path.of(egglog)),
          "CQL_EGGLOG names no egglog executable");
    }

    private AqlMultiDriver run(String program) throws Exception {
      AqlMultiDriver d =
          new AqlMultiDriver(
              AqlParserFactory.getParser().parseProgram(program.formatted(egglog)), null);
      d.start();
      return d;
    }

    /**
     * The rows of an entity, sorted, each as its attributes and the rows its foreign keys reach,
     * with a labelled null shown as null. Two instances of an acyclic schema whose rows differ in
     * these renderings are not isomorphic.
     */
    @SuppressWarnings("unchecked")
    private <X, Y> List<String> rows(AqlMultiDriver d, String instance, String en) {
      Instance<String, String, Sym, Fk, Att, String, String, X, Y> i =
          (Instance<String, String, Sym, Fk, Att, String, String, X, Y>)
              d.env.defs.insts.get(instance);
      List<String> rows = new ArrayList<>();
      for (X x : i.algebra().en(en)) {
        rows.add(row(i, en, x));
      }
      Collections.sort(rows);
      return rows;
    }

    private <X, Y> String row(
        Instance<String, String, Sym, Fk, Att, String, String, X, Y> i, String en, X x) {
      List<String> parts = new ArrayList<>();
      for (Att att : i.schema().attsFrom(en)) {
        Term<String, Void, Sym, Void, Void, Void, Y> v = i.algebra().att(att, x);
        parts.add(att + " = " + (v.sk() != null ? "null" : v));
      }
      for (Fk fk : i.schema().fksFrom(en)) {
        String target = i.schema().fks.get(fk).second;
        parts.add(fk + " -> (" + row(i, target, i.algebra().fk(fk, x)) + ")");
      }
      Collections.sort(parts);
      return String.join(", ", parts);
    }

    /**
     * The answer of the prover. A question that egglog has not answered within 30 s fails the test,
     * and the end of the process releases the thread that waits for it.
     */
    private static <T, C, V> boolean answer(
        EgglogProver<T, C, V> prover, Map<V, T> ctx, KBExp<C, V> lhs, KBExp<C, V> rhs)
        throws Exception {
      CompletableFuture<Boolean> answer =
          CompletableFuture.supplyAsync(() -> prover.eq(ctx, lhs, rhs));
      try {
        return answer.get(30, TimeUnit.SECONDS);
      } finally {
        if (!answer.isDone()) {
          prover.proc.destroy();
        }
      }
    }

    /**
     * Asserts that the prover gives no answer within 3 s. The prover waits until the process ends,
     * and the end of the process ends the wait with a NullPointerException.
     */
    private static <T, C, V> void assertNoAnswer(
        EgglogProver<T, C, V> prover, Map<V, T> ctx, KBExp<C, V> lhs, KBExp<C, V> rhs) {
      CompletableFuture<Boolean> answer =
          CompletableFuture.supplyAsync(() -> prover.eq(ctx, lhs, rhs));
      assertThrows(TimeoutException.class, () -> answer.get(3, TimeUnit.SECONDS));
      prover.proc.destroy();
      ExecutionException e =
          assertThrows(ExecutionException.class, () -> answer.get(10, TimeUnit.SECONDS));
      assertTrue(e.getCause() instanceof NullPointerException, e.toString());
    }

    /** On ground terms egglog answers as completion, the prover of the typeside, does. */
    @Test
    void answersGroundQuestionsAsCompletionDoes() throws Exception {
      var completion = typeside(INVOLUTION).semantics();
      var prover = new EgglogProver<>(egglog, kb(INVOLUTION), 8);
      var a = constant("a");
      var b = constant("b");
      var c = constant("c");
      List<List<Term<String, Void, Sym, Void, Void, Void, Void>>> questions =
          List.of(
              List.of(a, b),
              List.of(a, c),
              List.of(f(a), f(b)),
              List.of(f(f(c)), c),
              List.of(f(a), a),
              List.of(f(f(f(a))), f(b)));
      List<Boolean> answers = List.of(true, false, true, true, false, true);
      for (int n = 0; n < questions.size(); n++) {
        var lhs = questions.get(n).get(0);
        var rhs = questions.get(n).get(1);
        assertEquals(answers.get(n), completion.eq(Map.of(), lhs, rhs), lhs + " = " + rhs);
        assertEquals(
            answers.get(n), answer(prover, Map.of(), kbExp(lhs), kbExp(rhs)), lhs + " = " + rhs);
      }
    }

    /**
     * A question with a variable v declares v as a constructor, which egglog calls (v), but writes
     * v bare in the run and the check, where egglog reads it as a pattern variable. The run stops
     * at once, since some row satisfies f(f(x)) = x, and egglog rejects the check because the
     * pattern variable shadows the constructor. It reports no check, so the prover never answers,
     * even though f(f(v)) = v holds.
     */
    @Test
    void neverAnswersAQuestionWithAVariable() throws Exception {
      var prover = new EgglogProver<>(egglog, kb(INVOLUTION), 8);
      Map<String, Chc<String, Void>> ctx = Map.of("v", Chc.inLeft("T"));
      Term<String, Void, Sym, Void, Void, Void, Void> v = Term.Var("v");
      assertTrue(typeside(INVOLUTION).semantics().eq(ctx, f(f(v)), v));
      assertNoAnswer(prover, ctx, kbExp(f(f(v))), kbExp(v));
    }

    /**
     * egglog rejects the sort String, which it declares itself, and everything declared with it, so
     * it reports no check and the prover never answers.
     */
    @Test
    void neverAnswersWhenATypeIsNamedLikeAnEgglogSort() throws Exception {
      var prover =
          new EgglogProver<>(
              egglog,
              kb(
                  "typeside Ty = literal { types String constants a b : String"
                      + " equations a = b options prover = completion }\n"),
              8);
      Term<String, Void, Sym, Void, Void, Void, Void> a =
          Term.Sym(Sym.Sym("a", new Pair<>(List.of(), "String")), List.of());
      Term<String, Void, Sym, Void, Void, Void, Void> b =
          Term.Sym(Sym.Sym("b", new Pair<>(List.of(), "String")), List.of());
      assertNoAnswer(prover, Map.of(), kbExp(a), kbExp(b));
    }

    @Test
    void mergesRowsAsTheNativeChaseDoes() throws Exception {
      AqlMultiDriver d = run(MERGE);
      assertNull(d.env.exn);
      assertEquals(List.of("name = Alice", "name = Bob"), rows(d, "Native", "Person"));
      assertEquals(rows(d, "Native", "Person"), rows(d, "Egg", "Person"));
    }

    /**
     * egglog fires an existential constraint on every row, whether a witness exists or not, so
     * Sales gets a nameless member that the native chase does not add. The two results are
     * homomorphically equivalent but not isomorphic.
     */
    @Test
    void addsAWitnessThatTheNativeChaseDoesNotNeed() throws Exception {
      AqlMultiDriver d = run(WITNESS);
      assertNull(d.env.exn);
      assertEquals(
          List.of(
              "name = Alice, works -> (dname = Sales)",
              "name = Bob, works -> (dname = Sales)",
              "name = null, works -> (dname = Ops)"),
          rows(d, "Native", "Person"));
      assertEquals(
          List.of(
              "name = Alice, works -> (dname = Sales)",
              "name = Bob, works -> (dname = Sales)",
              "name = null, works -> (dname = Ops)",
              "name = null, works -> (dname = Sales)"),
          rows(d, "Egg", "Person"));
    }

    /**
     * egglog rejects the type String and everything declared with it, but the chase still waits for
     * the size of a table that egglog no longer has, until the timeout of the definition.
     */
    @Test
    void waitsForTheTimeoutWhenATypeIsNamedLikeAnEgglogSort() throws Exception {
      String program =
          """
          typeside Ty = literal { types String constants Alice Bob : String }
          schema S = literal : Ty { entities Person attributes name : Person -> String }
          instance I = literal : S {
            generators a b : Person equations a.name = Alice b.name = Bob }
          constraints C = literal : S {
            forall p q : Person where p.name = q.name -> where p = q }
          instance Egg = chase C I {
            options use_egglog_for_chase = true egglog_path = "%s" timeout = 2 }
          """;
      String msg = run(program).env.exn.getMessage();
      assertTrue(msg.startsWith("Timeout after 2 seconds."), msg);
    }
  }
}
