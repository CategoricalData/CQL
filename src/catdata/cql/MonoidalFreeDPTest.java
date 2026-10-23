package catdata.cql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import catdata.Chc;
import catdata.Pair;
import catdata.cql.Collage.CCollage;
import catdata.cql.exp.Sym;
import catdata.provers.KBExp;
import catdata.provers.KBTheory;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** The prover for theories whose equations use unary symbols only, on zero, s and plus over N. */
class MonoidalFreeDPTest {

  private static final String N = "N";
  private static final Sym ZERO = Sym.Sym("zero", new Pair<>(List.of(), N));
  private static final Sym S = Sym.Sym("s", new Pair<>(List.of(N), N));
  private static final Sym PLUS = Sym.Sym("plus", new Pair<>(List.of(N, N), N));
  private static final Sym ONE = Sym.Sym("one", new Pair<>(List.of(), N));

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

  private static KBExp<Head<String, Void, Sym, Void, Void, Void, Void>, String> kb(
      Term<String, Void, Sym, Void, Void, Void, Void> t) {
    return t.toKB();
  }

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
  private static Collage<String, Void, Sym, Void, Void, Void, Void> collage(
      Eq<String, Void, Sym, Void, Void, Void, Void>... eqs) {
    Collage<String, Void, Sym, Void, Void, Void, Void> col = new CCollage<>();
    col.tys().add(N);
    for (Sym f : List.of(ZERO, S, PLUS)) {
      col.syms().put(f, f.ty);
    }
    col.eqs().addAll(List.of(eqs));
    return col;
  }

  /** s(s(x)) = x. */
  private static MonoidalFreeDP<String, Void, Sym, Void, Void, Void, Void> involution() {
    return new MonoidalFreeDP<>(collage(eq(s(s(v("x"))), v("x"), "x")).toKB());
  }

  @Nested
  class Admission {

    @Test
    void acceptsTermsOfUnarySymbols() {
      assertTrue(MonoidalFreeDP.ok(v("x")));
      assertTrue(MonoidalFreeDP.ok(zero()));
      assertTrue(MonoidalFreeDP.ok(s(s(v("x")))));
      assertTrue(MonoidalFreeDP.ok(Term.Obj(Optional.of(1), N)));
      assertFalse(MonoidalFreeDP.ok(plus(v("x"), v("x"))));
      assertTrue(MonoidalFreeDP.ok(kb(s(s(v("x"))))));
      assertFalse(MonoidalFreeDP.ok(kb(plus(v("x"), v("x")))));
    }

    /** A foreign key or an attribute is as good as its argument. */
    @Test
    void acceptsAPathWhenItsArgumentIs() {
      Term<String, String, String, String, String, String, String> g = Term.Gen("g");
      assertTrue(MonoidalFreeDP.ok(Term.Fk("f", Term.Fk("f", g))));
      assertTrue(MonoidalFreeDP.ok(Term.Att("a", g)));
      Term<String, String, String, String, String, String, String> pair =
          Term.Sym("pair", List.of(g, g));
      assertFalse(MonoidalFreeDP.ok(Term.Att("a", pair)));
      assertTrue(MonoidalFreeDP.ok(kb(zero())));
    }

    @Test
    void acceptsTheoriesOfUnaryEquationsInOneVariable() {
      assertTrue(MonoidalFreeDP.ok(collage(eq(s(s(v("x"))), v("x"), "x"))));
      assertFalse(MonoidalFreeDP.ok(collage(eq(plus(v("x"), zero()), v("x"), "x"))));
      assertFalse(MonoidalFreeDP.ok(collage(eq(s(v("x")), s(v("y")), "x", "y"))));
      assertTrue(MonoidalFreeDP.ok(collage(eq(s(s(v("x"))), v("x"), "x")).toKB()));
      assertFalse(MonoidalFreeDP.ok(collage(eq(s(v("x")), s(v("y")), "x", "y")).toKB()));
    }

    /**
     * Issue 66: the term check stops at a unary symbol, the theory check does not, so AqlProver's
     * automatic choice, which uses the first, picks this prover and its constructor refuses.
     */
    @Test
    void lookUnderAUnarySymbolOnlyInTheTheoryCheck() {
      Term<String, Void, Sym, Void, Void, Void, Void> t = s(plus(v("x"), v("x")));
      assertTrue(MonoidalFreeDP.ok(t));
      assertFalse(MonoidalFreeDP.ok(kb(t)));
      var col = collage(eq(t, v("x"), "x"));
      assertTrue(MonoidalFreeDP.ok(col));
      assertFalse(MonoidalFreeDP.ok(col.toKB()));
      var th = col.toKB();
      String m = assertThrows(RuntimeException.class, () -> new MonoidalFreeDP<>(th)).getMessage();
      assertTrue(m.startsWith("Not monoidal: "), m);
    }
  }

  @Nested
  class Deciding {

    @Test
    void decidesTheUnaryEquations() {
      var dp = involution();
      assertTrue(dp.eq(Map.of(), kb(s(s(s(zero())))), kb(s(zero()))));
      assertFalse(dp.eq(Map.of(), kb(s(zero())), kb(zero())));
    }

    @Test
    void comparesBinarySymbolsArgumentByArgument() {
      var dp = involution();
      assertTrue(dp.eq(Map.of(), kb(plus(s(s(zero())), zero())), kb(plus(zero(), zero()))));
      assertFalse(dp.eq(Map.of(), kb(plus(s(zero()), zero())), kb(plus(zero(), zero()))));
      Sym times = Sym.Sym("times", new Pair<>(List.of(N, N), N));
      assertFalse(
          dp.eq(Map.of(), kb(Term.Sym(times, List.of(zero(), zero()))), kb(plus(zero(), zero()))));
    }

    /** Issue 66: s(s(x)) = x proves s(s(plus(zero, zero))) = plus(zero, zero). */
    @Test
    void answersFalseWhenTheAritiesDiffer() {
      var dp = involution();
      assertFalse(dp.eq(Map.of(), kb(s(s(plus(zero(), zero())))), kb(plus(zero(), zero()))));
    }

    /** Issue 66: the unary prover underneath knows no plus. */
    @Test
    void failsOnAUnarySymbolOverABinaryOne() {
      var dp = involution();
      var p = plus(zero(), zero());
      assertThrows(NullPointerException.class, () -> dp.eq(Map.of(), kb(s(s(p))), kb(s(p))));
    }

    @Test
    void rejectsAContextOfTwoVariables() {
      var dp = involution();
      Map<String, Chc<String, Void>> two = Map.of("x", Chc.inLeft(N), "y", Chc.inLeft(N));
      assertThrows(RuntimeException.class, () -> dp.eq(two, kb(v("x")), kb(v("y"))));
    }
  }

  @Nested
  class Signature {

    @Test
    void takesNewConstants() {
      var dp = involution();
      Head<String, Void, Sym, Void, Void, Void, Void> one = Head.SymHead(ONE);
      dp.add(one, Chc.inLeft(N));
      assertEquals(new Pair<>(List.of(), Chc.inLeft(N)), dp.kb.syms.get(one));
      Term<String, Void, Sym, Void, Void, Void, Void> t = Term.Sym(ONE, List.of());
      assertTrue(dp.eq(Map.of(), kb(s(s(t))), kb(t)));
    }

    @Test
    void describesItselfByTheUnaryProver() {
      var dp = involution();
      assertFalse(dp.supportsTrivialityCheck());
      assertFalse(dp.toString().isEmpty());
      KBTheory<Chc<String, Void>, Head<String, Void, Sym, Void, Void, Void, Void>, String> th =
          collage(eq(s(s(v("x"))), v("x"), "x")).toKB();
      assertTrue(th.syms.containsKey(Head.SymHead(PLUS)));
    }
  }
}
