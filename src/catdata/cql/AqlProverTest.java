package catdata.cql;

import static catdata.cql.CqlTestHelpers.ops;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import catdata.Chc;
import catdata.Pair;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.AqlProver.ProverName;
import catdata.cql.Collage.CCollage;
import catdata.cql.exp.Sym;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/** AqlProver over a typeside of one type N with zero : N, s : N -> N and plus : N, N -> N. */
class AqlProverTest {

  private static final String N = "N";
  private static final Sym ZERO = Sym.Sym("zero", new Pair<>(List.of(), N));
  private static final Sym S = Sym.Sym("s", new Pair<>(List.of(N), N));
  private static final Sym PLUS = Sym.Sym("plus", new Pair<>(List.of(N, N), N));

  private static Term<String, Void, Sym, Void, Void, Void, Void> v(String x) {
    return Term.Var(x);
  }

  private static Term<String, Void, Sym, Void, Void, Void, Void> zero() {
    return Term.Sym(ZERO, List.of());
  }

  private static Term<String, Void, Sym, Void, Void, Void, Void> s(
      Term<String, Void, Sym, Void, Void, Void, Void> t) {
    return Term.Sym(S, List.of(t));
  }

  private static Term<String, Void, Sym, Void, Void, Void, Void> plus(
      Term<String, Void, Sym, Void, Void, Void, Void> a,
      Term<String, Void, Sym, Void, Void, Void, Void> b) {
    return Term.Sym(PLUS, List.of(a, b));
  }

  /** An equation whose variables, in order, are all of sort N. */
  private static Eq<String, Void, Sym, Void, Void, Void, Void> eq(
      Term<String, Void, Sym, Void, Void, Void, Void> l,
      Term<String, Void, Sym, Void, Void, Void, Void> r,
      String... vars) {
    Map<String, Chc<String, Void>> ctx = new LinkedHashMap<>();
    for (String x : vars) {
      ctx.put(x, Chc.inLeft(N));
    }
    return new Eq<>(ctx, l, r);
  }

  @SafeVarargs
  private static DP<String, Void, Sym, Void, Void, Void, Void> prover(
      AqlOptions options, Eq<String, Void, Sym, Void, Void, Void, Void>... eqs) {
    Collage<String, Void, Sym, Void, Void, Void, Void> col = new CCollage<>();
    col.tys().add(N);
    for (Sym f : List.of(ZERO, S, PLUS)) {
      col.syms().put(f, f.ty);
    }
    col.eqs().addAll(List.of(eqs));
    AqlJs<String, Sym> js = new AqlJs<>(options, col.syms(), Map.of(), Map.of(), Map.of());
    return AqlProver.createTypeSide(options, col, js);
  }

  private static AqlOptions with(ProverName p) {
    return new AqlOptions(ops(), AqlOption.prover, p);
  }

  private static AqlOptions precedence(String p) {
    return new AqlOptions(with(ProverName.completion), AqlOption.completion_precedence, p);
  }

  private static AqlOptions nonTerminating(AqlOptions o) {
    return new AqlOptions(o, AqlOption.program_allow_nontermination_unsafe, true);
  }

  private static String message(Executable e) {
    return assertThrows(RuntimeException.class, e).getMessage();
  }

  private static String name(DP<String, Void, Sym, Void, Void, Void, Void> dp) {
    String s = dp.toStringProver();
    assertEquals(s, dp.toString());
    return s;
  }

  @Nested
  class Automatic {

    @Test
    void choosesFreeWithoutEquations() {
      assertTrue(name(prover(ops())).contains("FreeProver"));
    }

    @Test
    void choosesCongruenceForGroundEquations() {
      var dp = prover(ops(), eq(plus(zero(), zero()), zero()));
      assertTrue(name(dp).contains("CongruenceProverUniform"));
      assertTrue(dp.eq(null, plus(plus(zero(), zero()), zero()), zero()));
    }

    @Test
    void choosesMonoidalForUnaryEquations() {
      var dp = prover(ops(), eq(s(s(v("x"))), v("x"), "x"));
      assertTrue(name(dp).contains("MonoidalFreeDP"));
      assertTrue(dp.eq(null, s(s(s(zero()))), s(zero())));
    }

    @Test
    void choosesProgramForASizeReducingSystem() {
      var dp = prover(ops(), eq(plus(zero(), v("x")), v("x"), "x"));
      assertTrue(name(dp).contains("ProgramProver"));
      assertTrue(dp.eq(null, plus(zero(), s(zero())), s(zero())));
    }

    /** x = plus(zero, x) is turned into plus(zero, x) -> x. */
    @Test
    void reorientsAnEquationWhoseLeftSideIsSmaller() {
      var dp = prover(ops(), eq(v("x"), plus(zero(), v("x")), "x"));
      assertTrue(name(dp).contains("ProgramProver"));
      assertTrue(dp.eq(null, plus(zero(), zero()), zero()));
    }

    @Test
    void choosesProgramWithoutReorientingWhenNonTerminationIsAllowed() {
      var dp = prover(nonTerminating(ops()), eq(plus(zero(), v("x")), v("x"), "x"));
      assertTrue(name(dp).contains("ProgramProver"));
    }

    @Test
    void findsNoProverForCommutativity() {
      var comm = eq(plus(v("x"), v("y")), plus(v("y"), v("x")), "x", "y");
      String m = message(() -> prover(ops(), comm));
      assertTrue(m.startsWith("Cannot automatically chose prover"), m);
    }
  }

  @Nested
  class Explicit {

    @Test
    void programRejectsAnEquationOfEqualSides() {
      String m =
          message(
              () ->
                  prover(
                      with(ProverName.program),
                      eq(plus(v("x"), v("y")), plus(v("y"), v("x")), "x", "y")));
      assertTrue(m.startsWith("Cannot orient "), m);
      assertTrue(
          m.endsWith(
              "Possible solution: add options program_allow_nontermination_unsafe=true, or"
                  + " prover=completion"),
          m);
    }

    /**
     * Issue 62: plus(x, s(s(s(y)))) is bigger than plus(x, plus(x, x)), but x occurs in it once
     * against three times. numOccs counts each variable once, so the rule is accepted, and
     * rewriting plus(t, t) for t = s(s(s(zero))) never ends. The test does not run that rewriting.
     */
    @Test
    void programAcceptsARuleThatDuplicatesAVariable() {
      var dup = eq(plus(v("x"), s(s(s(v("y"))))), plus(v("x"), plus(v("x"), v("x"))), "x", "y");
      assertTrue(name(prover(with(ProverName.program), dup)).contains("ProgramProver"));
      var auto = prover(ops(), dup);
      assertTrue(name(auto).contains("ProgramProver"));
    }

    @Test
    void completionTakesAPrecedence() {
      AqlOptions o = precedence("zero s plus");
      var dp = prover(o, eq(plus(zero(), v("x")), v("x"), "x"));
      assertTrue(name(dp).contains("CompletionProver"));
      assertTrue(dp.eq(null, plus(zero(), zero()), zero()));
    }

    /** A name that is not a symbol is dropped, and the error names the symbols left out. */
    @Test
    void completionDropsANameItCannotRead() {
      AqlOptions o = precedence("zero nosuch");
      String m = message(() -> prover(o, eq(plus(zero(), v("x")), v("x"), "x")));
      assertTrue(
          m.startsWith("Incorrect precedence. Symbols in signature but not precedence: "), m);
      assertTrue(m.contains("plus") && m.contains("s"), m);
      assertFalse(m.contains("nosuch"), m);
    }

    @Test
    void congruenceAndMonoidalCanBeChosen() {
      var ground = eq(s(zero()), zero());
      assertTrue(name(prover(with(ProverName.congruence), ground)).contains("Congruence"));
      var unary = eq(s(v("x")), v("x"), "x");
      assertTrue(name(prover(with(ProverName.monoidal), unary)).contains("Monoidal"));
    }

    @Test
    void freeRejectsEquations() {
      assertEquals(
          "not an empty theory, as required by free proving strategy",
          message(() -> prover(with(ProverName.free), eq(s(zero()), zero()))));
    }

    @Test
    void failDecidesOnlySyntacticEquality() {
      var dp = prover(with(ProverName.fail), eq(s(zero()), zero()));
      assertTrue(name(dp).contains("FailProver"));
      assertTrue(dp.eq(null, zero(), zero()));
      assertThrows(RuntimeException.class, () -> dp.eq(null, s(zero()), zero()));
    }

    /** E and Vampire look for their executables when asked, here by the triviality check. */
    @Test
    void externalProversNeedTheirExecutables() {
      String e = message(() -> prover(with(ProverName.e), eq(s(zero()), zero())));
      assertTrue(e.startsWith("Error in triviality check: File does not exist: "), e);
      assertTrue(e.endsWith("Consider setting the option triviality_check_best_effort = false"), e);
      String v = message(() -> prover(with(ProverName.vampire), eq(s(zero()), zero())));
      assertTrue(v.startsWith("Error in triviality check: File does not exist: "), v);
      String g = message(() -> prover(with(ProverName.egglog), eq(s(zero()), zero())));
      assertEquals("File does not exist: /usr/local/bin/egglog", g);
    }

    @Test
    void withoutTheTrivialityCheckEFailsOnlyWhenAsked() {
      AqlOptions o =
          new AqlOptions(with(ProverName.e), AqlOption.triviality_check_best_effort, false);
      var dp = prover(o, eq(s(zero()), zero()));
      assertTrue(name(dp).contains("EProver"));
      assertTrue(message(() -> dp.eq(null, s(s(zero())), zero())).contains("File does not exist"));
    }
  }

  /**
   * The triviality check asks the prover whether two variables are equal, which only E and Vampire,
   * not installed here, can answer yes; so "Trivial sorts detected" is out of reach.
   */
  @Nested
  class Triviality {

    /** plus(x, y) = x and plus(x, y) = y prove x = y, so zero = s(zero) holds. */
    @Test
    void completionMissesThatTheTheoryEquatesEverything() {
      AqlOptions o = precedence("zero s plus");
      var dp =
          prover(
              o,
              eq(plus(v("x"), v("y")), v("x"), "x", "y"),
              eq(plus(v("x"), v("y")), v("y"), "x", "y"));
      assertFalse(dp.eq(null, zero(), s(zero())));
      assertTrue(dp.eq(null, plus(zero(), s(zero())), zero()));
    }
  }
}
