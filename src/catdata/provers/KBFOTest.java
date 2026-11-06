package catdata.provers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.Pair;
import catdata.cql.Head;
import catdata.cql.Term;
import catdata.cql.exp.Sym;
import catdata.provers.KBFO.AndOr;
import catdata.provers.KBFO.Bind;
import catdata.provers.KBFO.Eq;
import catdata.provers.KBFO.Implies;
import catdata.provers.KBFO.KBFOVisitor;
import catdata.provers.KBFO.Not;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link KBFO}, first-order formulas over {@link KBExp}, which nothing in CQL
 * constructs. The terms are CQL terms over a constant c and a function f, both of sort T.
 */
class KBFOTest {

  private static final Sym F = Sym.Sym("f", new Pair<>(List.of("T"), "T"));
  private static final Sym C = Sym.Sym("c", new Pair<>(List.of(), "T"));

  private static Map<Head<String, Void, Sym, Void, Void, Void, Void>, Pair<List<String>, String>>
      syms() {
    Map<Head<String, Void, Sym, Void, Void, Void, Void>, Pair<List<String>, String>> m =
        new HashMap<>();
    m.put(Head.SymHead(F), new Pair<>(List.of("T"), "T"));
    m.put(Head.SymHead(C), new Pair<>(List.of(), "T"));
    return m;
  }

  @SuppressWarnings("unchecked")
  private static KBExp<Head<String, Void, Sym, Void, Void, Void, Void>, String> kb(
      Term<String, Void, Sym, Void, Void, Void, Void> t) {
    return (KBExp<Head<String, Void, Sym, Void, Void, Void, Void>, String>) (Object) t;
  }

  private static final Term<String, Void, Sym, Void, Void, Void, Void> X = Term.Var("x");
  private static final Term<String, Void, Sym, Void, Void, Void, Void> CC = Term.Sym(C, List.of());

  private static Term<String, Void, Sym, Void, Void, Void, Void> f(
      Term<String, Void, Sym, Void, Void, Void, Void> t) {
    return Term.Sym(F, List.of(t));
  }

  private static Eq<String, Head<String, Void, Sym, Void, Void, Void, Void>, String> eq(
      Term<String, Void, Sym, Void, Void, Void, Void> l,
      Term<String, Void, Sym, Void, Void, Void, Void> r) {
    return new Eq<>(kb(l), kb(r));
  }

  /** f(x) = c. */
  private static Eq<String, Head<String, Void, Sym, Void, Void, Void, Void>, String> fxIsC() {
    return eq(f(X), CC);
  }

  private static Bind<String, Head<String, Void, Sym, Void, Void, Void, Void>, String> forallX(
      String sort, KBFO<String, Head<String, Void, Sym, Void, Void, Void, Void>, String> body) {
    return new Bind<>(true, new ArrayList<>(List.of(new Pair<>("x", sort))), body);
  }

  /** The sort that {@link KBFO#typeInf(KBExp, Map, Map)} infers for a term. */
  private static String typeInf(
      KBExp<Head<String, Void, Sym, Void, Void, Void, Void>, String> t,
      Map<Head<String, Void, Sym, Void, Void, Void, Void>, Pair<List<String>, String>> syms,
      Map<String, String> cur) {
    return KBFO.<Head<String, Void, Sym, Void, Void, Void, Void>, String, String>typeInf(
        t, syms, cur);
  }

  @Nested
  class Formulas {

    @Test
    void printEachConnective() {
      var eq = fxIsC();
      assertEquals("f(x) = c", eq.toString());
      assertEquals("not f(x) = c", new Not<>(eq).toString());
      assertEquals("f(x) = c -> f(x) = c", new Implies<>(eq, eq).toString());
      assertEquals("true", new AndOr<>(true, List.of()).toString());
      assertEquals("false", new AndOr<>(false, List.of()).toString());
      assertEquals("(f(x) = c or f(x) = c)", new AndOr<>(false, List.of(eq, eq)).toString());
      assertEquals("forall x:T . f(x) = c", forallX("T", eq).toString());
      assertEquals(
          "exists x:T . f(x) = c", new Bind<>(false, List.of(new Pair<>("x", "T")), eq).toString());
    }

    @Test
    void compareByTheirParts() {
      var eq = fxIsC();
      List<KBFO<String, Head<String, Void, Sym, Void, Void, Void, Void>, String>> all =
          List.of(
              eq,
              new Not<>(eq),
              new Implies<>(eq, eq),
              new AndOr<>(true, List.of(eq)),
              forallX("T", eq));
      List<KBFO<String, Head<String, Void, Sym, Void, Void, Void, Void>, String>> again =
          List.of(
              fxIsC(),
              new Not<>(fxIsC()),
              new Implies<>(fxIsC(), fxIsC()),
              new AndOr<>(true, List.of(fxIsC())),
              forallX("T", fxIsC()));
      for (int i = 0; i < all.size(); i++) {
        var a = all.get(i);
        assertEquals(a, a);
        assertEquals(a, again.get(i));
        assertEquals(a.hashCode(), again.get(i).hashCode());
        assertNotEquals(a, null);
        assertNotEquals(a, "formula");
        assertNotEquals(a, all.get((i + 1) % all.size()));
      }
      assertNotEquals(new AndOr<>(true, List.of(eq)), new AndOr<>(false, List.of(eq)));
      assertNotEquals(forallX("T", eq), new Bind<>(false, List.of(new Pair<>("x", "T")), eq));
      assertNotEquals(eq(CC, CC), eq);
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptAVisitor() {
      var eq = fxIsC();
      var not = new Not<>(eq);
      var implies = new Implies<>(eq, eq);
      var and = new AndOr<>(true, List.of(eq));
      var all = forallX("T", eq);
      KBFOVisitor<String, Head<String, Void, Sym, Void, Void, Void, Void>, String, String, Integer>
          v = mock(KBFOVisitor.class);
      when(v.visit(0, eq)).thenReturn("eq");
      when(v.visit(0, not)).thenReturn("not");
      when(v.visit(0, implies)).thenReturn("implies");
      when(v.visit(0, and)).thenReturn("and");
      when(v.visit(0, all)).thenReturn("bind");
      assertEquals(
          List.of("eq", "not", "implies", "and", "bind"),
          List.of(
              eq.accept(0, v),
              not.accept(0, v),
              implies.accept(0, v),
              and.accept(0, v),
              all.accept(0, v)));
    }
  }

  @Nested
  class Typing {

    @Test
    void typesAFormulaWhoseVariablesAreInScope() {
      Map<String, String> cur = new HashMap<>(Map.of("x", "T"));
      var eq = fxIsC();
      eq.type(syms(), cur);
      new Not<>(eq).type(syms(), cur);
      new Implies<>(eq, eq).type(syms(), cur);
      new AndOr<>(true, List.of(eq)).type(syms(), cur);
    }

    /**
     * The bound variable is added to a copy of the scope, but the body is typed in the original.
     */
    @Test
    void losesTheVariableItBinds() {
      RuntimeException e =
          assertThrows(
              RuntimeException.class, () -> forallX("T", fxIsC()).type(syms(), new HashMap<>()));
      assertTrue(
          e.getMessage().contains("expected at sort T but actually at sort null"), e.getMessage());
    }

    @Test
    void refusesAVariableWithoutASortOrBoundTwice() {
      RuntimeException e =
          assertThrows(
              RuntimeException.class, () -> forallX(null, fxIsC()).type(syms(), new HashMap<>()));
      assertEquals("No sort for variable x", e.getMessage());
      e =
          assertThrows(
              RuntimeException.class,
              () -> forallX("T", fxIsC()).type(syms(), new HashMap<>(Map.of("x", "T"))));
      assertEquals("Shadowed variable: x", e.getMessage());
      e =
          assertThrows(
              RuntimeException.class,
              () -> forallX("T", fxIsC()).typeInf(syms(), new HashMap<>(Map.of("x", "T"))));
      assertEquals("Shadowed variable: x", e.getMessage());
    }

    @Test
    void refusesAnEquationBetweenSorts() {
      var syms = syms();
      Sym d = Sym.Sym("d", new Pair<>(List.of(), "U"));
      syms.put(Head.SymHead(d), new Pair<>(List.of(), "U"));
      var eq = eq(CC, Term.Sym(d, List.of()));
      RuntimeException e =
          assertThrows(RuntimeException.class, () -> eq.type(syms, new HashMap<>()));
      assertEquals("Equality not at same sort, lhs is T and  rhs is U", e.getMessage());
      e = assertThrows(RuntimeException.class, () -> eq.typeInf(syms, new HashMap<>()));
      assertEquals("Equality not at same sort, lhs is T and  rhs is U", e.getMessage());
    }

    /** x = c gives x the sort of c, on either side. */
    @Test
    void infersTheSortOfAVariableFromTheOtherSide() {
      Map<String, String> cur = new HashMap<>();
      eq(CC, X).typeInf(syms(), cur);
      assertEquals(Map.of("x", "T"), cur);
      Map<String, String> cur2 = new HashMap<>();
      eq(X, CC).typeInf(syms(), cur2);
      assertEquals(Map.of("x", "T"), cur2);
      Map<String, String> cur3 = new HashMap<>();
      var eq = fxIsC();
      new AndOr<>(true, List.of(eq)).typeInf(syms(), cur3);
      new Not<>(eq).typeInf(syms(), cur3);
      new Implies<>(eq, eq).typeInf(syms(), cur3);
      assertEquals(Map.of("x", "T"), cur3);
    }

    @Test
    void reportsATermThatCannotBeTyped() {
      Map<String, String> cur = new HashMap<>(Map.of("x", "U"));
      RuntimeException e =
          assertThrows(RuntimeException.class, () -> typeInf(kb(f(X)), syms(), cur));
      assertEquals("Variable x requires two different sorts, T and U", e.getMessage());
      Sym g = Sym.Sym("g", new Pair<>(List.of(), "T"));
      e =
          assertThrows(
              RuntimeException.class,
              () -> typeInf(kb(Term.Sym(g, List.of())), syms(), new HashMap<>()));
      assertTrue(e.getMessage().startsWith("On g, missing symbol g in Map "), e.getMessage());
      e =
          assertThrows(
              RuntimeException.class,
              () -> typeInf(kb(Term.Sym(C, List.of(CC))), syms(), new HashMap<>()));
      assertEquals("On c(c), too many arguments: 0 >= 0", e.getMessage());
      var syms = syms();
      Sym d = Sym.Sym("d", new Pair<>(List.of(), "U"));
      syms.put(Head.SymHead(d), new Pair<>(List.of(), "U"));
      e =
          assertThrows(
              RuntimeException.class,
              () -> typeInf(kb(f(Term.Sym(d, List.of()))), syms, new HashMap<>()));
      assertEquals("Argument d expected at sort T but actually at sort U", e.getMessage());
    }

    /**
     * The body is typed, not inferred, and outside the binder's scope, so a variable without a sort
     * is never given one: c = c leaves it unsorted, and x = x fails on the null sort of x.
     */
    @Test
    void leavesAnUnsortedVariableUnsorted() {
      RuntimeException e =
          assertThrows(
              RuntimeException.class,
              () -> forallX(null, eq(CC, CC)).typeInf(syms(), new HashMap<>()));
      assertEquals("Variable with unconstrainted sort: x", e.getMessage());
      assertThrows(
          NullPointerException.class,
          () -> forallX(null, eq(X, X)).typeInf(syms(), new HashMap<>()));
    }
  }
}
