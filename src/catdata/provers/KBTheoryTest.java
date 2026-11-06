package catdata.provers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import catdata.cql.Head;
import catdata.cql.Term;
import catdata.cql.TypeSide;
import catdata.cql.exp.AqlMultiDriver;
import catdata.cql.exp.AqlParserFactory;
import catdata.cql.exp.Sym;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link KBTheory}, the theory that the provers receive, built here from a typeside
 * with the sorts T and U, the constants a b : T and u : U, f : T -> T and g : T, T -> T, and the
 * equations a = b, f(x) = x and g(x, y) = x. Formula names come from a counter shared by every
 * theory, so they are matched by pattern.
 */
class KBTheoryTest {

  private static final String TYPESIDE =
      """
      typeside Ty = literal { types T U constants a b : T u : U
        functions f : T -> T g : T, T -> T
        equations a = b forall x. f(x) = x forall x y. g(x, y) = x
        options prover = completion }
      """;

  private TypeSide<String, Sym> typeside;

  @SuppressWarnings("unchecked")
  private KBTheory<Chc<String, Void>, Head<String, Void, Sym, Void, Void, Void, Void>, String> kb()
      throws Exception {
    AqlMultiDriver d =
        new AqlMultiDriver(AqlParserFactory.getParser().parseProgram(TYPESIDE), null);
    d.start();
    typeside = (TypeSide<String, Sym>) d.env.defs.tys.get("Ty");
    return (KBTheory<Chc<String, Void>, Head<String, Void, Sym, Void, Void, Void, Void>, String>)
        (Object) typeside.collage().toKB();
  }

  private Sym sym(String name) {
    return typeside.syms.keySet().stream()
        .filter(s -> s.toString().equals(name))
        .findFirst()
        .orElseThrow();
  }

  @SuppressWarnings("unchecked")
  private static KBExp<Head<String, Void, Sym, Void, Void, Void, Void>, String> kb(
      Term<String, Void, Sym, Void, Void, Void, Void> t) {
    return (KBExp<Head<String, Void, Sym, Void, Void, Void, Void>, String>) (Object) t;
  }

  private static int count(String text, String regex) {
    Matcher m = Pattern.compile(regex).matcher(text);
    int n = 0;
    while (m.find()) {
      n++;
    }
    return n;
  }

  @Nested
  class Tptp {

    @Test
    void writesAnAxiomPerEquation() throws Exception {
      var kb = kb();
      String fof = kb.tptp();
      assertEquals(3, count(fof, "fof\\(eq\\d+,axiom,\\("), fof);
      assertEquals(2, count(fof, "! \\["), fof);
      assertEquals(1, count(fof, " & "), fof);
      String tff = kb.tptp_typed_nonempty();
      assertEquals(3, count(tff, "tff\\(eq\\d+,axiom,\\("), tff);
      String cnf = kb.tptp_cnf();
      assertTrue(cnf.startsWith("cnf(eq1,axiom,("), cnf);
      assertEquals(3, count(cnf, "cnf\\(eq\\d+,axiom"), cnf);
    }

    @Test
    void writesAConjectureWithItsVariables() throws Exception {
      var kb = kb();
      var a = kb(Term.Sym(sym("a"), List.of()));
      var b = kb(Term.Sym(sym("b"), List.of()));
      String ground = kb.tptp(Map.of(), a, b);
      assertEquals(1, count(ground, "conjecture"), ground);
      assertTrue(ground.trim().endsWith(")."), ground);
      Chc<String, Void> t = Chc.inLeft("T");
      var x = kb(Term.Var("x"));
      String one = kb.tptp(Map.of("x", t), x, x);
      assertTrue(one.contains(") => "), one);
      Map<String, Chc<String, Void>> two = new HashMap<>();
      two.put("x", t);
      two.put("y", t);
      String both = kb.tptp(two, x, kb(Term.Var("y")));
      assertEquals(2, count(both, " & "), both);
    }

    /** Each sort excludes the other, and each symbol states the sorts of its arguments. */
    @Test
    void writesAPreambleOfSortsAndSymbols() throws Exception {
      var kb = kb();
      String preamble = kb.tptp_preamble();
      assertEquals(2, count(preamble, "fof\\(sort\\d+,axiom"), preamble);
      assertEquals(kb.syms.size(), count(preamble, "fof\\(sym\\d+,axiom"), preamble);
      assertTrue(preamble.contains("![X0, X1] : ("), preamble);
      assertTrue(kb.printIso().contains("="), kb.printIso());
    }

    @Test
    void convertsATermWithTheGivenConstructors() throws Exception {
      var kb = kb();
      var t = kb(Term.Sym(sym("g"), List.of(Term.Var("x"), Term.Sym(sym("a"), List.of()))));
      String s =
          kb.convert(
              t,
              v -> "?" + v,
              (c, args) -> {
                List<String> l = new ArrayList<>();
                args.forEachRemaining(l::add);
                return c + l;
              });
      assertTrue(s.matches("\\S+\\[\\?\\S+, \\S+\\[]]"), s);
      assertEquals(Chc.inLeft("T"), kb.type(Map.of("x", Chc.inLeft("T")), t));
    }
  }

  @Nested
  class Structure {

    @Test
    void copiesAndExtendsATheory() throws Exception {
      var kb = kb();
      var copy = new KBTheory<>(kb);
      assertEquals(Set.copyOf(kb.tys), Set.copyOf(copy.tys));
      assertEquals(kb.syms, copy.syms);
      assertSame(kb.eqs, copy.eqs);
      copy.add(kb);
      assertEquals(6, Iterables.size(copy.eqs));
      assertTrue(copy.toString().startsWith("KBTheory [tys="), copy.toString());
      kb.validate();
    }

    @Test
    void findsTheInhabitedSorts() throws Exception {
      assertEquals(Set.of(Chc.inLeft("T"), Chc.inLeft("U")), kb().inhabGen());
    }
  }

  /** Theories written out by hand, over the sorts T and U, to break one rule each. */
  @Nested
  class Validation {

    private final Head<String, Void, Sym, Void, Void, Void, Void> c =
        Head.SymHead(Sym.Sym("c", new Pair<>(List.of(), "T")));

    private final Head<String, Void, Sym, Void, Void, Void, Void> d =
        Head.SymHead(Sym.Sym("d", new Pair<>(List.of(), "U")));

    private final Head<String, Void, Sym, Void, Void, Void, Void> h =
        Head.SymHead(Sym.Sym("h", new Pair<>(List.of("V"), "T")));

    @SuppressWarnings("unchecked")
    private KBTheory<String, Head<String, Void, Sym, Void, Void, Void, Void>, String> theory(
        Map<Head<String, Void, Sym, Void, Void, Void, Void>, Pair<List<String>, String>> syms,
        List<
                Triple<
                    Map<String, String>,
                    KBExp<Head<String, Void, Sym, Void, Void, Void, Void>, String>,
                    KBExp<Head<String, Void, Sym, Void, Void, Void, Void>, String>>>
            eqs) {
      return new KBTheory<>(KBExpFactoryNewImpl.factory, List.of("T", "U"), syms, eqs);
    }

    private KBExp<Head<String, Void, Sym, Void, Void, Void, Void>, String> app(
        Head<String, Void, Sym, Void, Void, Void, Void> head) {
      return kb(Term.Head(head, List.of()));
    }

    private void fails(KBTheory<?, ?, ?> kb, String start) {
      RuntimeException e = assertThrows(RuntimeException.class, kb::validate);
      assertTrue(e.getMessage().startsWith(start), e.getMessage());
    }

    @Test
    void rejectsASymbolOfAnUndeclaredSort() {
      fails(
          theory(Map.of(c, new Pair<>(List.of(), "V")), List.of()),
          "On symbol c, the return Type V");
      fails(
          theory(Map.of(h, new Pair<>(List.of("V"), "T")), List.of()),
          "On symbol h, the argument Type V is not declared.");
    }

    @Test
    void rejectsAnEquationOverAnUndeclaredSortOrAcrossSorts() {
      var syms =
          Util.<Head<String, Void, Sym, Void, Void, Void, Void>, Pair<List<String>, String>>mk();
      syms.put(c, new Pair<>(List.of(), "T"));
      syms.put(d, new Pair<>(List.of(), "U"));
      fails(theory(syms, List.of(new Triple<>(Map.of("v", "V"), app(c), app(c)))), "In equation ");
      fails(theory(syms, List.of(new Triple<>(Map.of(), app(c), app(d)))), "In equation ");
      RuntimeException e =
          assertThrows(
              RuntimeException.class,
              theory(syms, List.of(new Triple<>(Map.of(), app(c), app(d))))::validate);
      assertTrue(e.getMessage().endsWith("lhs type is T but rhs type is U"), e.getMessage());
    }
  }
}
