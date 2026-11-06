package catdata.provers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.Pair;
import catdata.cql.Head;
import catdata.cql.Term;
import catdata.cql.exp.Sym;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the default methods of {@link KBExp}, on CQL terms over the constants c : T and d
 * : U, f : T -> T and g : T, T -> T.
 */
class KBExpTest {

  private static final Sym C = Sym.Sym("c", new Pair<>(List.of(), "T"));
  private static final Sym D = Sym.Sym("d", new Pair<>(List.of(), "U"));
  private static final Sym F = Sym.Sym("f", new Pair<>(List.of("T"), "T"));
  private static final Sym G = Sym.Sym("g", new Pair<>(List.of("T", "T"), "T"));

  private static Map<Head<String, Void, Sym, Void, Void, Void, Void>, Pair<List<String>, String>>
      syms() {
    Map<Head<String, Void, Sym, Void, Void, Void, Void>, Pair<List<String>, String>> m =
        new HashMap<>();
    m.put(Head.SymHead(C), new Pair<>(List.of(), "T"));
    m.put(Head.SymHead(D), new Pair<>(List.of(), "U"));
    m.put(Head.SymHead(F), new Pair<>(List.of("T"), "T"));
    m.put(Head.SymHead(G), new Pair<>(List.of("T", "T"), "T"));
    return m;
  }

  @SuppressWarnings("unchecked")
  private static KBExp<Head<String, Void, Sym, Void, Void, Void, Void>, String> kb(
      Term<String, Void, Sym, Void, Void, Void, Void> t) {
    return (KBExp<Head<String, Void, Sym, Void, Void, Void, Void>, String>) (Object) t;
  }

  private static Term<String, Void, Sym, Void, Void, Void, Void> app(
      Sym s, List<Term<String, Void, Sym, Void, Void, Void, Void>> args) {
    return Term.Sym(s, args);
  }

  private static final Term<String, Void, Sym, Void, Void, Void, Void> X = Term.Var("x");

  private static final Term<String, Void, Sym, Void, Void, Void, Void> CC = app(C, List.of());

  @Test
  void findsAVariableOrASubterm() {
    var t = kb(app(G, List.of(app(F, List.of(X)), CC)));
    assertTrue(t.occurs("x"));
    assertFalse(t.occurs("y"));
    assertFalse(kb(CC).occurs("x"));
    assertTrue(t.hasAsSubterm(kb(app(F, List.of(X)))));
    assertTrue(t.hasAsSubterm(kb(CC)));
    assertFalse(t.hasAsSubterm(kb(app(F, List.of(CC)))));
  }

  @Test
  void refusesASymbolUsedWithTwoArities() {
    var t = kb(app(F, List.of(app(F, List.of(CC, CC)))));
    RuntimeException e = assertThrows(RuntimeException.class, t::symbols);
    assertEquals("Symbol f used with arity 2 and also 1", e.getMessage());
  }

  @Test
  void typesATermOrSaysWhyNot() {
    assertEquals("T", kb(app(F, List.of(X))).type(syms(), Map.of("x", "T")));
    Sym h = Sym.Sym("h", new Pair<>(List.of(), "T"));
    RuntimeException e =
        assertThrows(RuntimeException.class, () -> kb(app(h, List.of())).type(syms(), Map.of()));
    assertTrue(e.getMessage().startsWith("Missing symbol h in ctx "), e.getMessage());
    e =
        assertThrows(
            RuntimeException.class,
            () -> kb(app(F, List.of(app(D, List.of())))).type(syms(), Map.of()));
    assertTrue(e.getMessage().startsWith("Argument d "), e.getMessage());
    assertTrue(
        e.getMessage().contains("expected at sort T but actually at sort U"), e.getMessage());
  }

  /** Each subterm is recorded under its sort, with the terms it is an argument of. */
  @Test
  void collectsTheSubtermsOfATerm() {
    Map<
            String,
            Map<
                KBExp<Head<String, Void, Sym, Void, Void, Void, Void>, String>,
                Set<KBExp<Head<String, Void, Sym, Void, Void, Void, Void>, String>>>>
        pred = new HashMap<>();
    pred.put("T", new HashMap<>());
    var fx = kb(app(F, List.of(X)));
    assertTrue(fx.allSubExps0(syms(), "T", pred));
    assertEquals(Set.of(fx, kb(X)), pred.get("T").keySet());
    assertEquals(Set.of(fx), pred.get("T").get(kb(X)));
    assertFalse(fx.allSubExps0(syms(), "T", pred));
    assertFalse(kb(X).allSubExps0(syms(), "T", pred));
    RuntimeException e =
        assertThrows(RuntimeException.class, () -> kb(CC).allSubExps0(syms(), "U", pred));
    assertTrue(e.getMessage().startsWith("Anomaly: U not in "), e.getMessage());
  }

  @Test
  void printsForEgglog() {
    assertEquals("(f x)", kb(app(F, List.of(X))).toEgglog());
    assertEquals("x", kb(X).toEgglog());
  }
}
