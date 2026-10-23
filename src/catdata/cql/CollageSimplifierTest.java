package catdata.cql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import catdata.Chc;
import catdata.Pair;
import catdata.cql.Collage.CCollage;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Definitional simplification: an equation f(x1, ..., xn) = t, with distinct variables that make
 * up its whole context and no f in t, removes f and replaces it by t everywhere. Terms hash by
 * identity, so the order in which equations are tried changes between runs, and the tests check
 * only what every order gives.
 */
class CollageSimplifierTest {

  private static final String N = "N";
  private static final String E = "E";

  private static Term<String, String, String, String, String, String, String> v(String x) {
    return Term.Var(x);
  }

  private static Term<String, String, String, String, String, String, String> c(String f) {
    return Term.Sym(f, List.of());
  }

  @SafeVarargs
  private static Term<String, String, String, String, String, String, String> app(
      String f, Term<String, String, String, String, String, String, String>... args) {
    return Term.Sym(f, List.of(args));
  }

  /** An equation whose variables all have sort N. */
  private static Eq<String, String, String, String, String, String, String> eq(
      Term<String, String, String, String, String, String, String> l,
      Term<String, String, String, String, String, String, String> r,
      String... vars) {
    Map<String, Chc<String, String>> ctx = new LinkedHashMap<>();
    for (String x : vars) {
      ctx.put(x, Chc.inLeft(N));
    }
    return new Eq<>(ctx, l, r);
  }

  /** A typeside collage over N with the given symbols of the given arities. */
  @SafeVarargs
  private static Collage<String, String, String, String, String, String, String> theory(
      Map<String, Integer> arities,
      Eq<String, String, String, String, String, String, String>... eqs) {
    Collage<String, String, String, String, String, String, String> col = new CCollage<>();
    col.tys().add(N);
    arities.forEach((f, n) -> col.syms().put(f, new Pair<>(Collections.nCopies(n, N), N)));
    col.eqs().addAll(List.of(eqs));
    return col;
  }

  private static CollageSimplifier<String, String, String, String, String, String, String>
      simplify(Collage<String, String, String, String, String, String, String> col) {
    return new CollageSimplifier<>(col);
  }

  @Nested
  class Definitions {

    @Test
    void removesADefinedConstantAndItsEquation() {
      var col = theory(Map.of("a", 0, "b", 0), eq(c("a"), c("b")));
      var s = simplify(col);
      assertEquals(Set.of("b"), s.simplified.syms().keySet());
      assertTrue(s.simplified.eqs().isEmpty());
      assertSame(c("b"), s.simp.apply(c("a")));
      assertSame(app("f", c("b")), s.simp.apply(app("f", c("a"))));
    }

    @Test
    void leavesItsInputAlone() {
      var col = theory(Map.of("a", 0, "b", 0), eq(c("a"), c("b")));
      var s = simplify(col);
      assertSame(col, s.in);
      assertEquals(Set.of("a", "b"), col.syms().keySet());
      assertEquals(1, col.eqs().size());
    }

    /** Whichever equation goes first, both constants end up as c. */
    @Test
    void followsAChainOfDefinitions() {
      var col = theory(Map.of("a", 0, "b", 0, "c", 0), eq(c("a"), c("b")), eq(c("b"), c("c")));
      var s = simplify(col);
      assertEquals(Set.of("c"), s.simplified.syms().keySet());
      assertTrue(s.simplified.eqs().isEmpty());
      assertSame(c("c"), s.simp.apply(c("a")));
      assertSame(c("c"), s.simp.apply(c("b")));
    }

    @Test
    void substitutesTheArgumentsForTheVariables() {
      var col =
          theory(
              Map.of("f", 2, "g", 2, "a", 0, "b", 0),
              eq(app("f", v("x"), v("y")), app("g", v("y"), v("x")), "x", "y"));
      var s = simplify(col);
      assertFalse(s.simplified.syms().containsKey("f"));
      assertSame(app("g", c("b"), c("a")), s.simp.apply(app("f", c("a"), c("b"))));
    }

    @Test
    void readsAnEquationFromRightToLeftToo() {
      Term<String, String, String, String, String, String, String> one = Term.Obj("1", N);
      var col = theory(Map.of("one", 0), eq(one, c("one")));
      var s = simplify(col);
      assertTrue(s.simplified.syms().isEmpty());
      assertSame(one, s.simp.apply(c("one")));
    }

    @Test
    void rewritesTheOtherEquations() {
      var col =
          theory(
              Map.of("f", 1, "s", 1, "a", 0),
              eq(app("f", v("x")), app("s", v("x")), "x"),
              eq(app("s", app("f", c("a"))), c("a")));
      var s = simplify(col);
      var expected = eq(app("s", app("s", c("a"))), c("a"));
      assertEquals(Set.of(expected), Set.copyOf(s.simplified.eqs()));
    }

    /** a = f(b) and b = g(a): once one is removed, the other is defined by itself. */
    @Test
    void removesOnlyOneOfTwoMutualDefinitions() {
      var col =
          theory(
              Map.of("a", 0, "b", 0, "f", 1, "g", 1),
              eq(c("a"), app("f", c("b"))),
              eq(c("b"), app("g", c("a"))));
      var s = simplify(col);
      assertEquals(1, s.simplified.eqs().size());
      Set<String> left = s.simplified.syms().keySet();
      assertTrue(left.contains("a") ^ left.contains("b"), left.toString());
    }
  }

  @Nested
  class NotDefinitions {

    private void assertKept(Collage<String, String, String, String, String, String, String> col) {
      var s = simplify(col);
      assertEquals(col.syms().keySet(), s.simplified.syms().keySet());
      assertEquals(Set.copyOf(col.eqs()), Set.copyOf(s.simplified.eqs()));
      assertSame(app("f", v("x")), s.simp.apply(app("f", v("x"))));
    }

    @Test
    void keepsARecursiveEquation() {
      assertKept(theory(Map.of("s", 1, "f", 1), eq(app("s", app("s", v("x"))), v("x"), "x")));
      var f = app("f", v("x"));
      assertKept(theory(Map.of("f", 1, "g", 1), eq(f, app("g", f), "x")));
    }

    @Test
    void keepsARepeatedVariable() {
      assertKept(theory(Map.of("f", 1, "m", 2), eq(app("m", v("x"), v("x")), v("x"), "x")));
    }

    /** forall x, y. f(x) = y says that N has one element; it defines nothing. */
    @Test
    void keepsAVariableOutsideTheArguments() {
      assertKept(theory(Map.of("f", 1), eq(app("f", v("x")), v("y"), "x", "y")));
    }

    @Test
    void dropsAnEquationOfEqualSides() {
      var col = theory(Map.of("f", 1), eq(app("f", v("x")), app("f", v("x")), "x"));
      var s = simplify(col);
      assertTrue(s.simplified.eqs().isEmpty());
      assertEquals(Set.of("f"), s.simplified.syms().keySet());
    }
  }

  @Nested
  class Paths {

    /** forall x:E. x.f = x.g removes the foreign key f. */
    @Test
    void removesADefinedForeignKey() {
      Collage<String, String, String, String, String, String, String> col = new CCollage<>();
      col.getEns().add(E);
      col.fks().put("f", new Pair<>(E, E));
      col.fks().put("g", new Pair<>(E, E));
      Term<String, String, String, String, String, String, String> x = v("x");
      col.eqs().add(new Eq<>(Map.of("x", Chc.inRight(E)), Term.Fk("f", x), Term.Fk("g", x)));
      var s = simplify(col);
      assertEquals(Set.of("g"), s.simplified.fks().keySet());
      Term<String, String, String, String, String, String, String> e = Term.Gen("e");
      assertSame(Term.Fk("g", Term.Fk("g", e)), s.simp.apply(Term.Fk("f", Term.Fk("f", e))));
    }

    @Test
    void removesADefinedGenerator() {
      Collage<String, String, String, String, String, String, String> col = new CCollage<>();
      col.getEns().add(E);
      col.gens().put("e1", E);
      col.gens().put("e2", E);
      col.eqs().add(new Eq<>(null, Term.Gen("e1"), Term.Gen("e2")));
      var s = simplify(col);
      assertEquals(Set.of("e2"), s.simplified.gens().keySet());
      assertSame(Term.Gen("e2"), s.simp.apply(Term.Gen("e1")));
    }
  }
}
