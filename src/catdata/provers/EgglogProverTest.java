package catdata.provers;

import static catdata.PathTestHelpers.inCqlString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import catdata.Chc;
import catdata.Pair;
import catdata.RuntimeInterruptedException;
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
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link EgglogProver}, run against stand-ins for egglog and, in {@link
 * AgainstEgglog}, against a real egglog. CQL starts egglog with {@code --mode interactive}, writes
 * one command a line, and reads its standard output up to the {@code (done)} or {@code (error)}
 * that ends each line. A stand-in is a shell script that records the lines it reads and ends each
 * with a mark.
 */
class EgglogProverTest {

  @TempDir Path dir;

  /**
   * A stand-in that records what it reads in a file named after it, with {@code .input} added, and
   * answers every check the same way: {@code (done)}, which says that it holds, or {@code (error)}.
   */
  private String stub(String name, String check) throws Exception {
    return script(
        name,
        """
        while IFS= read -r line; do
          printf '%%s\\n' "$line" >> "$0.input"
          case "$line" in
            "(check"*) echo "%s" ;;
            *) echo "(done)" ;;
          esac
        done
        """
            .formatted(check));
  }

  private String script(String name, String body) throws Exception {
    assumeFalse(OS.WINDOWS.isCurrentOs(), "the stand-in for egglog is a POSIX shell script");
    Path p = dir.resolve(name);
    Files.writeString(p, "#!/bin/sh\n" + body);
    assertTrue(p.toFile().setExecutable(true));
    return p.toString();
  }

  /** A stand-in that writes its process id to a file and never answers. */
  private String silent() throws Exception {
    return script("silent", "echo $$ > \"$0.pid\"\nwhile IFS= read -r line; do :; done\n");
  }

  /** The process id of {@link #silent()}, once it has started. */
  private long silentProcess() throws Exception {
    Path pid = dir.resolve("silent.pid");
    for (int n = 0; n < 500 && !(Files.exists(pid) && Files.size(pid) > 1); n++) {
      Thread.sleep(10);
    }
    return Long.parseLong(Files.readString(pid).trim());
  }

  private static boolean isRunning(long pid) {
    return ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false);
  }

  private static void assertEnds(long pid) throws Exception {
    for (int n = 0; n < 500 && isRunning(pid); n++) {
      Thread.sleep(10);
    }
    assertFalse(isRunning(pid), "the stand-in for egglog is still running");
  }

  private String input(String name) throws Exception {
    return Files.readString(dir.resolve(name + ".input"));
  }

  /**
   * egglog matches a premise against the terms it holds, and holds a function of the type side only
   * where something applied it. So CQL writes a rule that applies what a premise applies to every
   * row the premise speaks of.
   */
  @Test
  @SuppressWarnings({"rawtypes", "unchecked"})
  void appliesWhatAPremiseApplies() throws Exception {
    AqlMultiDriver d =
        new AqlMultiDriver(AqlParserFactory.getParser().parseProgram(INITIALS), null);
    d.start();
    assertNull(d.env.exn);
    String text =
        EgglogProver.toEgglog(
            (Constraints) d.env.defs.eds.get("C"), (Instance) d.env.defs.insts.get("I"));
    assertTrue(text.contains("(constructor F0_initial (S0_Name) S0_Name)\n"), text);
    assertTrue(
        text.matches(
            "(?s).*\\(rule \\(\\(U1_Person (V[01]_p)\\)\\) \\(\\(F0_initial \\(F1_name"
                + " \\1\\)\\)\\)\\)\n"
                + ".*"),
        text);
    assertTrue(
        text.matches(
            "(?s).*\\(rule \\(\\(U1_Person (V[01]_q)\\)\\) \\(\\(F0_initial \\(F1_name"
                + " \\1\\)\\)\\)\\)\n"
                + ".*"),
        text);
  }

  /** People whose names have the same initial are one person; initial is a function with no law. */
  private static final String INITIALS =
      """
      typeside Ty = literal {
        types Name constants Alice Bob : Name functions initial : Name -> Name }
      schema S = literal : Ty { entities Person attributes name : Person -> Name }
      instance I = literal : S {
        generators a b c : Person equations a.name = Alice b.name = Alice c.name = Bob }
      constraints C = literal : S {
        forall p q : Person where initial(p.name) = initial(q.name) -> where p = q }
      """;

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
    return constant(name, "T");
  }

  private static Term<String, Void, Sym, Void, Void, Void, Void> constant(
      String name, String type) {
    return Term.Sym(Sym.Sym(name, new Pair<>(List.of(), type)), List.of());
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

  /** Both terms are put into egglog before its rules run, and the mark of the check answers. */
  @Test
  void answersByTheMarkThatEndsTheCheck() throws Exception {
    var kb = kb();
    var yes = new EgglogProver<>(stub("yes", "(done)"), kb, 3);
    assertTrue(yes.eq(Map.of(), c("a"), c("b")));
    var no = new EgglogProver<>(stub("no", "(error)"), kb, 3);
    assertFalse(no.eq(Map.of(), c("a"), c("b")));
    assertTrue(yes.toString().startsWith("Egglog prover\n\n(sort S0_T)\n"), yes.toString());
    assertFalse(yes.supportsTrivialityCheck());
    String asked =
        """
        (C0_T 0)
        (C0_T 1)
        (run 3 :until (= (C0_T 0) (C0_T 1)))
        (check (= (C0_T 0) (C0_T 1)))
        """;
    assertTrue(input("yes").endsWith(asked), input("yes"));
  }

  /** A variable is a constant of its own inside a push, and the answer pops it again. */
  @Test
  void makesAConstantOfEachVariableOfAQuestion() throws Exception {
    var kb = kb();
    Map<String, Chc<String, Void>> ctx = Map.of("v", Chc.inLeft("T"));
    @SuppressWarnings("unchecked")
    KBExp<Head<String, Void, Sym, Void, Void, Void, Void>, String> v =
        (KBExp<Head<String, Void, Sym, Void, Void, Void, Void>, String>) (Object) Term.Var("v");
    assertTrue(new EgglogProver<>(stub("yes", "(done)"), kb, 3).eq(ctx, v, c("a")));
    assertFalse(new EgglogProver<>(stub("no", "(error)"), kb, 3).eq(ctx, v, c("a")));
    String asked =
        """
        (push)
        (U0_T (C0_T 2))
        (C0_T 2)
        (C0_T 0)
        (run 3 :until (= (C0_T 2) (C0_T 0)))
        (check (= (C0_T 2) (C0_T 0)))
        (pop)
        """;
    assertTrue(input("yes").endsWith(asked), input("yes"));
    assertTrue(input("no").endsWith(asked), input("no"));
    RuntimeException e =
        assertThrows(
            RuntimeException.class,
            () -> new EgglogProver<>(stub("yes", "(done)"), kb, 3).eq(Map.of(), v, c("a")));
    assertEquals("The variable v is not in the context.", e.getMessage());
  }

  @Test
  void refusesAMissingExecutable() throws Exception {
    var kb = kb();
    String missing = dir.resolve("none").toString();
    RuntimeException e =
        assertThrows(RuntimeException.class, () -> new EgglogProver<>(missing, kb, 3));
    assertEquals("File does not exist: " + missing, e.getMessage());
  }

  /** A value that a question mentions and the theory does not becomes the next constant, once. */
  @Test
  void addsAConstant() throws Exception {
    var kb = kb();
    var p = new EgglogProver<>(stub("yes", "(done)"), kb, 3);
    Head<String, Void, Sym, Void, Void, Void, Void> e =
        Head.SymHead(Sym.Sym("e", new Pair<>(List.of(), "T")));
    p.add(e, Chc.inLeft("T"));
    p.add(e, Chc.inLeft("T"));
    String added = "(union (C0_T 0) (C0_T 1))\n(U0_T (C0_T 2))\n";
    assertTrue(input("yes").endsWith(added), input("yes"));
    assertTrue(p.toString().endsWith(added), p.toString());
    assertTrue(p.kb.syms.containsKey(e));
    assertTrue(p.eq(Map.of(), c("e"), c("a")));
    assertTrue(input("yes").endsWith("(check (= (C0_T 2) (C0_T 0)))\n"), input("yes"));
  }

  /**
   * The theory becomes a sort for each sort, with its constants and its universe, a constructor for
   * each symbol with arguments, and a rewrite or a union for each axiom. A function of the type
   * side is not applied to the whole universe: only what takes a row of an entity is.
   */
  @Test
  void writesTheTheoryInEgglog() throws Exception {
    assertEquals(
        """
        (sort S0_T)
        (constructor C0_T (i64) S0_T)
        (relation U0_T (S0_T))
        (constructor F0_f (S0_T) S0_T)
        (U0_T (C0_T 0))
        (U0_T (C0_T 1))
        (rule ((= V0 (F0_f V1))) ((U0_T V0)))
        (birewrite (F0_f V0_x) V0_x :when ((U0_T V0_x)))
        (union (C0_T 0) (C0_T 1))
        """,
        EgglogProver.toEgglog(kb()));
  }

  /**
   * Whatever the names of CQL are, egglog gets its own words, numbers, and names made of a letter,
   * a number and at most 24 letters, digits and underscores, all different. A constant is a number.
   */
  @Test
  void givesEveryNameAnEgglogName() throws Exception {
    String longName = "a_name_that_goes_on_".repeat(40);
    String s =
        EgglogProver.toEgglog(
            kb(
                "typeside Ty = literal { types \"String\" \"i64\" \"a b\" constants \"rule\""
                    + " \"(done)\" \"1\" \"true\" \"two words\" \"a;b\" \"naïve\" \"é\""
                    + " \"+\" \""
                    + longName
                    + "\" : \"String\" \"union\" \"-inf\" : \"i64\" \"(\" \")\" : \"a b\""
                    + " functions \"set!\" : \"String\" -> \"i64\" \""
                    + longName
                    + "\" : \"i64\", \"a b\" -> \"a b\" }\n"));
    List<String> names = new ArrayList<>();
    for (String line : s.split("\n")) {
      for (String word : line.split("[ ()]+")) {
        if (!List.of("", "sort", "constructor", "relation", "rule", "i64", "=").contains(word)
            && !word.matches("[0-9]+")) {
          assertTrue(word.matches("[SCUFV][0-9]+(_[A-Za-z0-9_]{1,24})?"), line);
          names.add(word);
        }
      }
    }
    assertEquals(
        List.of(
            "S0_String",
            "C0_String",
            "U0_String",
            "S1_a_b",
            "C1_a_b",
            "U1_a_b",
            "S2_i64",
            "C2_i64",
            "U2_i64",
            "F0_a_name_that_goes_on_a_na",
            "F1_set",
            "V0",
            "V1",
            "V2"),
        names.stream().distinct().toList(),
        s);
    assertTrue(s.contains("(constructor F0_a_name_that_goes_on_a_na (S2_i64 S1_a_b) S1_a_b)\n"), s);
    assertTrue(s.contains("(constructor F1_set (S0_String) S2_i64)\n"), s);
    assertTrue(s.contains("(rule ((= V0 (F1_set V1))) ((U2_i64 V0)))\n"), s);
    assertEquals(10, s.split("\\(U0_String \\(C0_String [0-9]\\)\\)\n", -1).length - 1, s);
    assertEquals(2, s.split("\\(U1_a_b \\(C1_a_b [01]\\)\\)\n", -1).length - 1, s);
    assertEquals(2, s.split("\\(U2_i64 \\(C2_i64 [01]\\)\\)\n", -1).length - 1, s);
  }

  /** egglog's own words go into the message when it refuses a command. */
  @Test
  void quotesEgglogWhenItRefusesACommand() throws Exception {
    var kb = kb();
    String refusing =
        script(
            "refusing",
            "while IFS= read -r line; do echo 'Unbound function' >&2; echo '(error)'; done\n");
    RuntimeException e =
        assertThrows(RuntimeException.class, () -> new EgglogProver<>(refusing, kb, 3));
    assertTrue(
        e.getMessage().startsWith("egglog refused a command of CQL's: (sort S0_T)\n"),
        e.getMessage());
    assertTrue(e.getMessage().contains("Unbound function"), e.getMessage());
  }

  /** An egglog from before 3.0.0 does not know --mode interactive, and ends at once. */
  @Test
  void saysWhichEgglogItNeedsWhenEgglogEnds() throws Exception {
    var kb = kb();
    String old = script("old", "echo \"error: unexpected argument '--mode' found\" >&2\nexit 2\n");
    RuntimeException e = assertThrows(RuntimeException.class, () -> new EgglogProver<>(old, kb, 3));
    assertTrue(e.getMessage().contains("ended before it answered"), e.getMessage());
    assertTrue(e.getMessage().contains("CQL needs egglog 3.0.0 or later"), e.getMessage());
    assertTrue(e.getMessage().contains("unexpected argument '--mode' found"), e.getMessage());
  }

  /** The thread that waits for egglog can be interrupted, and the interrupt ends egglog. */
  @Test
  void endsEgglogWhenTheThreadThatWaitsIsInterrupted() throws Exception {
    var kb = kb();
    String silent = silent();
    AtomicReference<Throwable> thrown = new AtomicReference<>();
    Thread asking =
        new Thread(
            () -> {
              try {
                new EgglogProver<>(silent, kb, 3);
              } catch (Throwable t) {
                thrown.set(t);
              }
            });
    asking.start();
    long egglog = silentProcess();
    assertTrue(isRunning(egglog));
    asking.interrupt();
    asking.join(5000);
    assertTrue(thrown.get() instanceof RuntimeInterruptedException, String.valueOf(thrown.get()));
    assertEnds(egglog);
  }

  /**
   * The chase of {@code use_egglog_for_chase = true}, run against a stand-in that answers the
   * commands that print a table with the lines that egglog 3.0.0 prints for it.
   */
  @Nested
  class Chase {

    private int stubs = 0;

    /** The two rows of Person, in the order egglog happens to print them. */
    private static final String PERSONS =
        """
           (U1_Person (C1_Person 1)) -> (U1_Person (C1_Person 1))
           (U1_Person (C1_Person 0)) -> (U1_Person (C1_Person 0))
        """;

    /** The table of name: the row of each person, and the row of its name. */
    private static final String NAMES =
        """
           (T0_name 1 1) -> (T0_name 1 1)
           (T0_name 0 0) -> (T0_name 0 0)
        """;

    /**
     * A stand-in that prints the two names, the rows of Person and the rows of the table of name as
     * given, and that finds no check to hold. It is named chase1, chase2 and so on.
     */
    private String tables(String persons, String names) throws Exception {
      return script(
          "chase" + (++stubs),
          """
          while IFS= read -r line; do
            printf '%%s\\n' "$line" >> "$0.input"
            case "$line" in
              "(print-function U0_String)")
                echo "("
                echo "   (U0_String (C0_String 0)) -> (U0_String (C0_String 0))"
                echo "   (U0_String (C0_String 1)) -> (U0_String (C0_String 1))"
                echo ")" ;;
              "(print-function U1_Person)")
                echo "("
                printf '%%s' '%s'
                echo ")" ;;
              "(print-function T0_name)")
                echo "("
                printf '%%s' '%s'
                echo ")" ;;
              "(check"*) echo "(error)"; continue ;;
            esac
            echo "(done)"
          done
          """
              .formatted(persons, names));
    }

    private AqlMultiDriver run(String path) throws Exception {
      return run(path, "");
    }

    private AqlMultiDriver run(String path, String options) throws Exception {
      String program =
          """
          typeside Ty = literal { types String constants Alice Bob : String }
          schema S = literal : Ty { entities Person attributes name : Person -> String }
          instance I = literal : S {
            generators a b : Person equations a.name = Alice b.name = Bob }
          constraints C = literal : S {
            forall p q : Person where p.name = q.name -> where p = q
            forall p : Person -> exists q : Person where q.name = p.name }
          instance J = chase C I { options use_egglog_for_chase = true egglog_path = "%s" %s }
          """
              .formatted(inCqlString(path), options);
      AqlMultiDriver d =
          new AqlMultiDriver(AqlParserFactory.getParser().parseProgram(program), null);
      d.start();
      return d;
    }

    /** The rows of Person in J, each as the instance shows where it comes from. */
    @SuppressWarnings("unchecked")
    private <X, Y> List<String> shown(AqlMultiDriver d) {
      Instance<String, String, Sym, Fk, Att, String, String, X, Y> j =
          (Instance<String, String, Sym, Fk, Att, String, String, X, Y>) d.env.defs.insts.get("J");
      List<String> ret = new ArrayList<>();
      for (X x : j.algebra().en("Person")) {
        ret.add(j.algebra().printX("Person", x).toString());
      }
      Collections.sort(ret);
      return ret;
    }

    /**
     * CQL writes the program, has egglog run it, prints the rows of each sort, numbers them in the
     * order of their terms, and prints the table that egglog fills for the attribute.
     */
    @Test
    void readsTheRowsAndTheirAttributesFromEgglog() throws Exception {
      AqlMultiDriver d = run(tables(PERSONS, NAMES));
      assertNull(d.env.exn);
      String j = d.env.defs.insts.get("J").toString();
      assertTrue(j.contains(".name = Alice"), j);
      assertTrue(j.contains(".name = Bob"), j);
      assertEquals(List.of("a", "b"), shown(d));
      String input = input("chase1");
      assertTrue(
          input.startsWith(
              """
              (sort S0_String)
              (constructor C0_String (i64) S0_String)
              (relation U0_String (S0_String))
              (sort S1_Person)
              (constructor C1_Person (i64) S1_Person)
              (relation U1_Person (S1_Person))
              (constructor F0_name (S1_Person) S0_String :cost 1000000)
              (U0_String (C0_String 0))
              (U0_String (C0_String 1))
              (U1_Person (C1_Person 0))
              (U1_Person (C1_Person 1))
              (rule ((= V0 (F0_name V1))) ((U0_String V0)))
              (rule ((U1_Person V1)) ((F0_name V1)))
              (union (F0_name (C1_Person 0)) (C0_String 0))
              (union (F0_name (C1_Person 1)) (C0_String 1))
              (rule ((U1_Person V\
              """),
          input);
      assertTrue(
          input.matches(
              "(?s).*\\(rule \\(\\(U1_Person V0_[pq]\\) \\(U1_Person V1_[pq]\\)"
                  + " \\(= \\(F0_name V[01]_p\\) \\(F0_name V[01]_q\\)\\)\\)"
                  + " \\(\\(union V[01]_p V[01]_q\\)\\)\\)\n.*"),
          input);
      String witness =
          "(rule ((U1_Person V0_p))"
              + " ((U1_Person (K0_q V0_p)) (union (F0_name (K0_q V0_p)) (F0_name V0_p))))\n";
      String table =
          "(rule ((= V1 (F0_name V0)) (= V2 (I1_Person V0)) (= V3 (I0_String V1)))"
              + " ((T0_name V2 V3)) :ruleset R0)\n";
      assertTrue(
          input.contains(
              "(constructor K0_q (S1_Person) S1_Person)\n"
                  + witness
                  + """
                  (run-schedule (saturate (run)))
                  (print-function U0_String)
                  (print-function U1_Person)
                  (function I0_String (S0_String) i64 :no-merge)
                  (set (I0_String (C0_String 0)) 0)
                  (set (I0_String (C0_String 1)) 1)
                  (function I1_Person (S1_Person) i64 :no-merge)
                  (set (I1_Person (C1_Person 0)) 0)
                  (set (I1_Person (C1_Person 1)) 1)
                  (ruleset R0)
                  (relation T0_name (i64 i64))
                  """
                  + table
                  + "(run R0 1)\n(print-function T0_name)\n"),
          input);
    }

    /** A row that is the witness of a constraint is named as CQL named it before: R2q(a). */
    @Test
    void namesARowByItsTerm() throws Exception {
      String persons =
          """
             (U1_Person (K0_q (C1_Person 0))) -> (U1_Person (K0_q (C1_Person 0)))
             (U1_Person (C1_Person 0)) -> (U1_Person (C1_Person 0))
          """;
      AqlMultiDriver d = run(tables(persons, NAMES));
      assertNull(d.env.exn);
      assertEquals(List.of("R2q(a)", "a"), shown(d));
    }

    /**
     * CQL starts egglog once for a chase and ends it when the rows are read. What the instance is
     * asked after that, about its rows or about their values, CQL answers without egglog: the
     * values have the prover that CQL gives any instance (the option {@code second_prover}).
     */
    @Test
    void asksEgglogNothingOnceTheRowsAreRead() throws Exception {
      AqlMultiDriver d = run(tables(PERSONS, NAMES));
      assertNull(d.env.exn);
      String sent = input("chase1");
      assertEquals(2, sent.split("\\(sort S0_String\\)", -1).length, sent);
      askAboutItsTwoRows(d);
      assertEquals(sent, input("chase1"));
    }

    /** Asks J whether its two rows are equal, and whether their names are. */
    @SuppressWarnings("unchecked")
    private <X, Y> void askAboutItsTwoRows(AqlMultiDriver d) {
      Instance<String, String, Sym, Fk, Att, X, Y, X, Y> j =
          (Instance<String, String, Sym, Fk, Att, X, Y, X, Y>) d.env.defs.insts.get("J");
      List<X> rows = new ArrayList<>();
      for (X x : j.algebra().en("Person")) {
        rows.add(x);
      }
      assertEquals(2, rows.size());
      Att name = Att.Att("Person", "name");
      Term<String, String, Sym, Fk, Att, X, Y> a = Term.Gen(rows.get(0));
      Term<String, String, Sym, Fk, Att, X, Y> b = Term.Gen(rows.get(1));
      assertTrue(j.dp().eq(null, a, a));
      assertFalse(j.dp().eq(null, a, b));
      assertTrue(j.dp().eq(null, Term.Att(name, a), Term.Att(name, a)));
      assertFalse(j.dp().eq(null, Term.Att(name, a), Term.Att(name, b)));
      assertFalse(j.dp().toStringProver().contains("Egglog prover"), j.dp().toStringProver());
    }

    /** What a run says when the stand-in prints these rows of Person and of the table of name. */
    private String refusal(String persons, String names) throws Exception {
      return run(tables(persons.stripTrailing() + "\n", names.stripTrailing() + "\n"))
          .env
          .exn
          .getMessage();
    }

    @Test
    void refusesWhatItCannotRead() throws Exception {
      String one = "   (T0_name 0 0)";
      String msg = refusal(PERSONS, one);
      assertTrue(msg.contains("egglog gave no value for name on a row."), msg);
      msg = refusal(PERSONS, "   (T0_name 0 7)");
      assertTrue(msg.contains("CQL cannot read what egglog printed: (T0_name 0 7)"), msg);
      msg = refusal(PERSONS, "   (T0_name zero 0)");
      assertTrue(msg.contains("CQL cannot read what egglog printed:    (T0_name zero 0)"), msg);
      msg = refusal("   (U1_Person Unextractable)", one);
      assertTrue(msg.contains("egglog has no term for a row of inr Person."), msg);
      msg = refusal("   (U1_Person (F9_z))", one);
      assertTrue(msg.contains("egglog printed a name that CQL did not give it: (F9_z)"), msg);
      msg = refusal("   (U1_Person (C1_Person 9))", one);
      assertTrue(msg.contains("CQL did not give it: (C1_Person 9)"), msg);
      msg = refusal("   (U1_Person (C1_Person 0)", one);
      assertTrue(msg.contains("cannot read what egglog printed:    (U1_Person (C1_Person 0)"), msg);
    }

    @Test
    void refusesAMissingExecutable() throws Exception {
      String missing = dir.resolve("none").toString();
      String msg = run(missing).env.exn.getMessage();
      assertTrue(msg.contains("File does not exist: " + missing), msg);
    }

    /**
     * egglog knows a function of the type side by its name and the theory's equations. It does not
     * run the Java that computes one, so a constraint that uses one is refused before egglog
     * starts.
     */
    @Test
    void refusesAConstraintThatUsesAFunctionComputedInJava() throws Exception {
      String program =
          """
          typeside Ty = literal {
            external_types Int -> "java.lang.Integer"
            external_parsers Int -> "x => java.lang.Integer.parseInt(x)"
            external_functions plus : Int, Int -> Int = "(x, y) => x + y" }
          schema S = literal : Ty { entities Person attributes age : Person -> Int }
          instance I = literal : S { generators a b : Person equations a.age = 30 b.age = 30 }
          constraints C = literal : S {
            forall p q : Person where plus(p.age, 0) = q.age -> where p = q }
          instance J = chase C I { options use_egglog_for_chase = true egglog_path = "%s" }
          """
              .formatted(inCqlString(stub("computed", "(done)")));
      AqlMultiDriver d =
          new AqlMultiDriver(AqlParserFactory.getParser().parseProgram(program), null);
      d.start();
      String msg = d.env.exn.getMessage();
      assertTrue(msg.contains("A constraint uses plus, which the type side computes in Java"), msg);
      assertFalse(Files.exists(dir.resolve("computed.input")));
    }

    /**
     * A chase that egglog does not finish ends with the definition's timeout, and so does egglog.
     */
    @Test
    void endsEgglogAtTheTimeoutOfTheDefinition() throws Exception {
      String msg = run(silent(), "timeout = 2").env.exn.getMessage();
      assertTrue(msg.startsWith("Timeout after 2 seconds."), msg);
      assertEnds(silentProcess());
    }

    /** Nothing calls {@code entails}: it checks that egglog is there and answers false. */
    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void neverEntails() throws Exception {
      Path quiet = dir.resolve("quiet");
      Files.writeString(quiet, "#!/bin/sh\nexit 0\n");
      assertTrue(quiet.toFile().setExecutable(true));
      AqlMultiDriver d = run(tables(PERSONS, NAMES));
      Constraints c = (Constraints) d.env.defs.eds.get("C");
      assertFalse(EgglogProver.entails(c, c, quiet.toString()));
      String missing = dir.resolve("none").toString();
      RuntimeException e =
          assertThrows(RuntimeException.class, () -> EgglogProver.entails(c, c, missing));
      assertEquals("File does not exist: " + missing, e.getMessage());
      String theory = EgglogProver.toEgglog(c, (Instance) d.env.defs.insts.get("I"));
      assertTrue(theory.startsWith("(sort S0_String)\n"), theory);
      assertTrue(theory.contains("(U1_Person (C1_Person 1))\n"), theory);
      String schema = EgglogProver.toEgglog(c, c);
      assertTrue(schema.contains("(constructor K0_q (S1_Person) S1_Person)\n"), schema);
      assertFalse(schema.contains("(U1_Person (C1_Person "), schema);
    }
  }

  /**
   * The prover and the chase against a real egglog: the executable that the environment variable
   * {@code CQL_EGGLOG} names, which CI builds from egglog 3.0.0. Without the variable these tests
   * are skipped. They compare egglog with CQL's own provers and chase, so they keep their meaning
   * for any egglog release that CQL supports.
   */
  @Nested
  class AgainstEgglog {

    /** a = b, f is an involution, and c is equal to neither a nor b. */
    private static final String INVOLUTION =
        "typeside Ty = literal { types T constants a b c : T functions f : T -> T"
            + " equations a = b forall x. f(f(x)) = x options prover = completion }\n";

    /** The typeside of the chases. */
    private static final String NAMES =
        """
        typeside Ty = literal { types Name constants Alice Bob Sales Ops : Name }
        """;

    /** CQL's own chase, and egglog's. */
    private static final String CHASES =
        """
        instance Native = chase C I
        instance Egg = chase C I { options use_egglog_for_chase = true egglog_path = "%s" }
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
            """
            + CHASES;

    /** Sales has two members and Ops none. */
    private static final String DEPARTMENTS =
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
            """;

    /** Every department needs a member. */
    private static final String WITNESS =
        DEPARTMENTS
            + """
            constraints C = literal : S {
              forall p q : Person where p.name = q.name -> where p = q
              forall x : Dept -> exists m : Person where m.works = x }
            """
            + CHASES;

    /** Only the department named Ops needs a member. */
    private static final String PREMISE =
        DEPARTMENTS
            + """
            constraints C = literal : S {
              forall x : Dept where x.dname = Ops -> exists m : Person where m.works = x }
            """
            + CHASES;

    /**
     * Names that egglog reads as something else, or cannot read: its own sorts and commands,
     * brackets, a comment, numbers, a space, letters outside ASCII, a name on two entities, and a
     * long name.
     */
    private static final String NAMED =
        """
        typeside Ty = literal {
          types "String" "i64"
          constants "rule" "(done)" "1" "true" "a;b" "two words" "naïve" "%s" : "String"
            "union" "-inf" : "i64" }
        schema S = literal : Ty {
          entities "datatype" "Vec"
          foreign_keys "set" : "datatype" -> "Vec"
          attributes "extract" : "datatype" -> "String" "name" : "datatype" -> "String"
            "name" : "Vec" -> "String" "run" : "Vec" -> "i64" }
        instance I = literal : S {
          generators "(a" "b)" "check" "3.14" "NaN" : "datatype" "push" "pop" "x y" : "Vec"
          equations "(a".extract = "rule" "check".extract = "1" "3.14".extract = "true"
            "NaN".extract = "a;b" "(a".name = "two words" "b)".name = "two words"
            "check".name = "naïve" "3.14".name = "%s"
            "(a"."set" = "push" "b)"."set" = "pop" "check"."set" = "push" "3.14"."set" = "x y"
            "NaN"."set" = "x y" "push".name = "rule" "pop".name = "rule" "x y".name = "1"
            "push".run = "union" }
        constraints C = literal : S {
          forall p q : "datatype" where p.name = q.name -> where p = q
          forall v w : "Vec" where v.name = w.name -> where v = w
          forall v : "Vec" where v.run = "-inf" -> exists d : "datatype" where d."set" = v }
        """
                .formatted("long_".repeat(60), "long_".repeat(60))
            + CHASES;

    /** Values of SQL's types, a premise and a conclusion that name a value, and a merge. */
    private static final String SQL =
        """
        schema S = literal : sql {
          entities Person Dept
          foreign_keys works : Person -> Dept
          attributes name : Person -> String age : Person -> Integer dname : Dept -> String }
        instance I = literal : S {
          generators a b c : Person d e : Dept
          equations a.name = "Alice Smith" b.name = "Alice Smith" c.name = "Bob (the builder); x"
            a.age = 30 c.age = 41 a.works = d b.works = d c.works = e
            d.dname = "Sales & Marketing" e.dname = "Ops" }
        constraints C = literal : S {
          forall p q : Person where p.name = q.name -> where p = q
          forall x : Dept where x.dname = "Ops" -> exists m : Person
            where m.works = x m.name = "New hire" }
        """
            + CHASES;

    /**
     * A schema with a cycle and an equation, a labelled null n, two rows that merge, and a
     * constraint whose witness is a value: the department of Carol gets her nickname as its name.
     */
    private static final String NULLS =
        """
        typeside Ty = literal { types Name constants Alice Bob Carol : Name }
        schema S = literal : Ty {
          entities Person Dept
          foreign_keys works : Person -> Dept boss : Dept -> Person
          path_equations Dept.boss.works = Dept
          attributes name : Person -> Name nick : Person -> Name dname : Dept -> Name }
        instance I = literal : S {
          generators a b c : Person d e : Dept n : Name
          equations a.name = Alice b.name = Alice c.name = Carol a.nick = n
            a.works = d b.works = d c.works = e d.boss = a e.boss = c }
        constraints C = literal : S {
          forall p q : Person where p.name = q.name -> where p = q
          forall x : Dept where x.boss.name = Carol -> exists v : Name
            where x.dname = v x.boss.nick = v }
        """
            + CHASES;

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
              AqlParserFactory.getParser().parseProgram(program.formatted(inCqlString(egglog))),
              null);
      d.start();
      return d;
    }

    @SuppressWarnings("unchecked")
    private <X, Y> Instance<String, String, Sym, Fk, Att, String, String, X, Y> instance(
        AqlMultiDriver d, String name) {
      return (Instance<String, String, Sym, Fk, Att, String, String, X, Y>)
          d.env.defs.insts.get(name);
    }

    /**
     * The rows of an entity, sorted, each as its attributes and the rows its foreign keys reach,
     * three foreign keys deep, with a labelled null shown as null. Two instances whose rows differ
     * in these renderings are not isomorphic.
     */
    private <X, Y> List<String> rows(AqlMultiDriver d, String instance, String en) {
      Instance<String, String, Sym, Fk, Att, String, String, X, Y> i = instance(d, instance);
      List<String> rows = new ArrayList<>();
      for (X x : i.algebra().en(en)) {
        rows.add(row(i, en, x, 3));
      }
      Collections.sort(rows);
      return rows;
    }

    private <X, Y> String row(
        Instance<String, String, Sym, Fk, Att, String, String, X, Y> i, String en, X x, int deep) {
      List<String> parts = new ArrayList<>();
      for (Att att : i.schema().attsFrom(en)) {
        Term<String, Void, Sym, Void, Void, Void, Y> v = i.algebra().att(att, x);
        parts.add(att + " = " + (v.sk() != null ? "null" : v));
      }
      for (Fk fk : i.schema().fksFrom(en)) {
        String target = i.schema().fks.get(fk).second;
        String reached = deep == 0 ? "" : row(i, target, i.algebra().fk(fk, x), deep - 1);
        parts.add(fk + " -> (" + reached + ")");
      }
      Collections.sort(parts);
      return String.join(", ", parts);
    }

    /** Asserts that egglog's chase and CQL's own give the same rows in every entity. */
    private void assertSameRows(AqlMultiDriver d) {
      assertNull(d.env.exn);
      for (String en : instance(d, "Native").schema().ens) {
        assertFalse(rows(d, "Native", en).isEmpty(), en);
        assertEquals(rows(d, "Native", en), rows(d, "Egg", en), en);
      }
    }

    /** The answer of the prover; a question that egglog has not answered within 30 s fails. */
    private static <T, C, V> boolean answer(
        EgglogProver<T, C, V> prover, Map<V, T> ctx, KBExp<C, V> lhs, KBExp<C, V> rhs) {
      return assertTimeoutPreemptively(Duration.ofSeconds(30), () -> prover.eq(ctx, lhs, rhs));
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

    /** A variable of a question is a constant of its own, so f(f(v)) = v holds and f(v) = v not. */
    @Test
    void answersAQuestionWithAVariable() throws Exception {
      var completion = typeside(INVOLUTION).semantics();
      var prover = new EgglogProver<>(egglog, kb(INVOLUTION), 8);
      Map<String, Chc<String, Void>> ctx = Map.of("v", Chc.inLeft("T"));
      Term<String, Void, Sym, Void, Void, Void, Void> v = Term.Var("v");
      assertTrue(completion.eq(ctx, f(f(v)), v));
      assertTrue(answer(prover, ctx, kbExp(f(f(v))), kbExp(v)));
      assertFalse(completion.eq(ctx, f(v), v));
      assertFalse(answer(prover, ctx, kbExp(f(v)), kbExp(v)));
      assertFalse(answer(prover, ctx, kbExp(v), c("a")));
      assertTrue(answer(prover, Map.of(), kbExp(f(constant("a"))), kbExp(f(constant("b")))));
    }

    /** egglog declares the sort String itself; CQL's type of that name gets a name of its own. */
    @Test
    void answersWhenATypeIsNamedLikeAnEgglogSort() throws Exception {
      var prover =
          new EgglogProver<>(
              egglog,
              kb(
                  "typeside Ty = literal { types String constants a b c : String"
                      + " equations a = b options prover = completion }\n"),
              8);
      var a = kbExp(constant("a", "String"));
      assertTrue(answer(prover, Map.of(), a, kbExp(constant("b", "String"))));
      assertFalse(answer(prover, Map.of(), a, kbExp(constant("c", "String"))));
    }

    /** A constant that is added later is equal to itself only. */
    @Test
    void answersAQuestionAboutAnAddedConstant() throws Exception {
      var prover = new EgglogProver<>(egglog, kb(INVOLUTION), 8);
      Head<String, Void, Sym, Void, Void, Void, Void> e =
          Head.SymHead(Sym.Sym("e", new Pair<>(List.of(), "T")));
      prover.add(e, Chc.inLeft("T"));
      assertTrue(answer(prover, Map.of(), c("e"), c("e")));
      assertFalse(answer(prover, Map.of(), c("e"), c("a")));
      assertTrue(answer(prover, Map.of(), kbExp(f(f(constant("e")))), c("e")));
    }

    @Test
    void mergesRowsAsTheNativeChaseDoes() throws Exception {
      AqlMultiDriver d = run(MERGE);
      assertSameRows(d);
      assertEquals(List.of("name = Alice", "name = Bob"), rows(d, "Egg", "Person"));
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
     * A witness is added only where the premise of its constraint holds: Ops gets a member, and
     * Sales, which is not named Ops, does not get a third.
     */
    @Test
    void addsAWitnessOnlyWhereThePremiseHolds() throws Exception {
      AqlMultiDriver d = run(PREMISE);
      assertSameRows(d);
      assertEquals(
          List.of(
              "name = Alice, works -> (dname = Sales)",
              "name = Bob, works -> (dname = Sales)",
              "name = null, works -> (dname = Ops)"),
          rows(d, "Egg", "Person"));
      assertEquals(List.of("dname = Ops", "dname = Sales"), rows(d, "Egg", "Dept"));
    }

    @Test
    void chasesNamesThatEgglogCannotRead() throws Exception {
      AqlMultiDriver d = run(NAMED);
      assertSameRows(d);
      assertEquals(4, rows(d, "Egg", "datatype").size());
      assertEquals(2, rows(d, "Egg", "Vec").size());
    }

    /** The functions of SQL's type side stay where a term applies them, so the chase ends. */
    @Test
    void chasesValuesOfTheSqlTypeSide() throws Exception {
      AqlMultiDriver d = run(SQL);
      assertSameRows(d);
      assertEquals(
          List.of(
              "age = 30, name = \"Alice Smith\", works -> (dname = \"Sales & Marketing\")",
              "age = 41, name = \"Bob (the builder); x\", works -> (dname = Ops)",
              "age = null, name = \"New hire\", works -> (dname = Ops)"),
          rows(d, "Egg", "Person"));
    }

    /**
     * For each person: the name, whether the nickname and the name of the person's department are
     * one value, and n where the nickname is the labelled null n of the instance.
     */
    private <X, Y> List<String> nicknames(AqlMultiDriver d, String instance) {
      Instance<String, String, Sym, Fk, Att, String, String, X, Y> i = instance(d, instance);
      List<String> ret = new ArrayList<>();
      for (X x : i.algebra().en("Person")) {
        X dept = i.algebra().fk(Fk.Fk("Person", "works"), x);
        var nick = i.algebra().att(Att.Att("Person", "nick"), x);
        var dname = i.algebra().att(Att.Att("Dept", "dname"), dept);
        String n = String.valueOf(nick.sk()).matches(".*\\bn\\b.*") ? " n" : "";
        ret.add(i.algebra().att(Att.Att("Person", "name"), x) + " " + nick.equals(dname) + n);
      }
      Collections.sort(ret);
      return ret;
    }

    @Test
    void keepsLabelledNullsAndTheValuesThatRowsShare() throws Exception {
      AqlMultiDriver d = run(NULLS);
      assertSameRows(d);
      assertEquals(2, rows(d, "Egg", "Person").size());
      assertEquals(List.of("Alice false n", "Carol true"), nicknames(d, "Native"));
      assertEquals(List.of("Alice false n", "Carol true"), nicknames(d, "Egg"));
    }

    @Test
    void chasesWhenATypeIsNamedLikeAnEgglogSort() throws Exception {
      String program =
          """
          typeside Ty = literal { types String constants Alice Bob : String }
          schema S = literal : Ty { entities Person attributes name : Person -> String }
          instance I = literal : S {
            generators a b c : Person equations a.name = Alice b.name = Bob c.name = Alice }
          constraints C = literal : S {
            forall p q : Person where p.name = q.name -> where p = q }
          """
              + CHASES;
      AqlMultiDriver d = run(program);
      assertSameRows(d);
      assertEquals(List.of("name = Alice", "name = Bob"), rows(d, "Egg", "Person"));
    }

    /** A premise that applies a function of the type side holds of the rows it should. */
    @Test
    void chasesAPremiseThatAppliesAFunctionOfTheTypeSide() throws Exception {
      AqlMultiDriver d =
          run(
              INITIALS
                  + """
                  instance Native = chase C I { options require_consistency = false }
                  instance Egg = chase C I {
                    options use_egglog_for_chase = true egglog_path = "%s" }
                  """);
      assertSameRows(d);
      assertEquals(List.of("name = Alice", "name = Bob"), rows(d, "Egg", "Person"));
    }

    /**
     * A type side that egglog decides and CQL's own provers do not: addition commutes. The chase
     * takes the rows whose values are a + b and b + a for one row. The values of the result have
     * the prover that the option {@code second_prover} names: egglog, where the chase names it, and
     * otherwise one that CQL chooses, which for this type side it cannot.
     */
    @Test
    void chasesOnATypeSideThatOnlyEgglogDecides() throws Exception {
      String program =
          """
          typeside Ty = literal {
            types T constants a b : T functions plus : T, T -> T
            equations forall x y. plus(x, y) = plus(y, x)
            options prover = egglog egglog_path = "%1$s" }
          schema S = literal : Ty {
            entities E attributes v : E -> T options prover = egglog egglog_path = "%1$s" }
          instance I = literal : S {
            generators e1 e2 e3 : E equations e1.v = plus(a, b) e2.v = plus(b, a) e3.v = a
            options prover = egglog second_prover = egglog egglog_path = "%1$s" }
          constraints C = literal : S { forall x y : E where x.v = y.v -> where x = y }
          instance Egg = chase C I {
            options use_egglog_for_chase = true egglog_path = "%1$s" SECOND }
          """;
      AqlMultiDriver d = run(program.replace("SECOND", "second_prover = egglog"));
      assertNull(d.env.exn);
      assertEquals(3, rows(d, "I", "E").size());
      assertEquals(2, rows(d, "Egg", "E").size());
      d = run(program.replace("SECOND", ""));
      String msg = d.env.exn.getMessage();
      assertTrue(msg.contains("Cannot automatically"), msg);
    }
  }
}
