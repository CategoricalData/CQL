package catdata.cql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import catdata.Chc;
import catdata.Triple;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** An equation with a context of sorted variables. */
class EqTest {

  private static final Chc<String, String> N = Chc.inLeft("N");

  private static Term<String, String, String, String, String, String, String> v(String x) {
    return Term.Var(x);
  }

  private static Term<String, String, String, String, String, String, String> c(String f) {
    return Term.Sym(f, List.of());
  }

  private static Eq<String, String, String, String, String, String, String> eq(
      Map<String, Chc<String, String>> ctx,
      Term<String, String, String, String, String, String, String> l,
      Term<String, String, String, String, String, String, String> r) {
    return new Eq<>(ctx, l, r);
  }

  @Nested
  class Context {

    @Test
    void anAbsentOrEmptyContextIsTheEmptyMap() {
      assertSame(Collections.emptyMap(), eq(null, c("a"), c("b")).ctx);
      assertSame(Collections.emptyMap(), eq(new HashMap<>(), c("a"), c("b")).ctx);
    }

    @Test
    void keepsTheGivenMap() {
      Map<String, Chc<String, String>> ctx = Map.of("x", N);
      assertSame(ctx, eq(ctx, v("x"), c("a")).ctx);
    }

    /** The hash is computed once, from a map the caller can still change. */
    @Test
    void hashesTheContextAsItWasAtConstruction() {
      Map<String, Chc<String, String>> ctx = new LinkedHashMap<>();
      ctx.put("x", N);
      var e = eq(ctx, v("x"), c("a"));
      int before = e.hashCode();
      var same = eq(Map.of("x", N), v("x"), c("a"));
      assertEquals(same, e);
      ctx.put("y", N);
      assertEquals(before, e.hashCode());
      assertNotEquals(same, e);
    }
  }

  @Nested
  class Equality {

    @Test
    void comparesTheContextAndBothSides() {
      var e = eq(Map.of("x", N), v("x"), c("a"));
      assertEquals(e, eq(Map.of("x", N), v("x"), c("a")));
      assertEquals(e.hashCode(), eq(Map.of("x", N), v("x"), c("a")).hashCode());
      assertNotEquals(e, eq(Map.of("x", N), v("x"), c("b")));
      assertNotEquals(e, eq(Map.of("x", N), c("a"), v("x")));
      assertTrue(e.equals(e));
      assertFalse(e.equals(null));
      assertFalse(e.equals("x = a"));
    }

    /** "Aa" and "BB" hash alike, so the contexts are told apart by equals, not by the hash. */
    @Test
    void comparesContextsWithTheSameHash() {
      assertEquals("Aa".hashCode(), "BB".hashCode());
      var a = eq(Map.of("Aa", N), c("a"), c("b"));
      var b = eq(Map.of("BB", N), c("a"), c("b"));
      assertEquals(a.hashCode(), b.hashCode());
      assertNotEquals(a, b);
    }

    @Test
    void allowsMissingSides() {
      assertEquals(eq(null, null, null), eq(null, null, null));
      assertNotEquals(eq(null, null, null), eq(null, c("a"), null));
    }
  }

  @Nested
  class Printing {

    @Test
    void writesTheContextBeforeTheEquation() {
      assertEquals("a = b", eq(null, c("a"), c("b")).toString());
      Map<String, Chc<String, String>> ctx = new LinkedHashMap<>();
      ctx.put("x", N);
      ctx.put("e", Chc.inRight("E"));
      assertEquals("x:inl N, e:inr E. x = a", eq(ctx, v("x"), c("a")).toString());
    }

    @Test
    void toTripleKeepsTheContextAndSides() {
      var e = eq(Map.of("x", N), v("x"), c("a"));
      assertEquals(new Triple<>(Map.of("x", N), v("x"), c("a")), e.toTriple());
    }
  }
}
