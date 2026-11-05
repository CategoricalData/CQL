package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.InteriorLabel;
import catdata.Pair;
import catdata.ParseException;
import catdata.Unit;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.Kind;
import catdata.cql.TypeSide;
import catdata.cql.exp.TyExp.TyExpCoVisitor;
import catdata.cql.exp.TyExp.TyExpVar;
import catdata.cql.exp.TyExp.TyExpVisitor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TyExpRaw}, the typeside literal {@code literal { imports ... types ...
 * constants ... functions ... external_types ... equations ... }}.
 */
class TyExpRawTest {

  private static final String BASE = "typeside Ty0 = literal { types Bool }\n";

  private static final String LITERAL =
      """
      typeside Ty = literal {
        imports Ty0
        types Nat
        constants zero : Nat
        functions succ : Nat -> Nat plus : Nat, Nat -> Nat
        external_types Str -> "java.lang.String"
        external_parsers Str -> "x => x"
        external_functions cat : Str, Str -> Str = "(x, y) => x + y"
        equations
          forall x. plus(x, zero) = x
          forall x:Nat, y. plus(x, succ(y)) = succ(plus(x, y))
        options timeout = 5 prover = completion
      }
      """;

  private static final String PROGRAM = BASE + LITERAL;

  private static TyExpRaw exp(String literal) throws Exception {
    return ExpTestHelpers.exp(BASE + literal, "Ty");
  }

  @Nested
  class Evaluation {

    @Test
    void addsItsDeclarationsToThoseOfItsImports() throws Exception {
      TypeSide<String, Sym> t = ExpTestHelpers.eval(PROGRAM, "Ty", Kind.TYPESIDE);
      assertEquals(Set.of("Bool", "Nat", "Str"), t.tys);
      assertEquals(4, t.syms.size());
      assertEquals(2, t.eqs.size());
      assertEquals(Map.of("Str", "java.lang.String"), t.js.java_tys);
    }

    @Test
    void reportsTheEquationThatFailsToType() throws Exception {
      String msg = ExpTestHelpers.error(PROGRAM.replace("plus(x, zero) = x", "nope(x) = x"));
      assertTrue(msg.contains("In equation x.nope = x, "), msg);
    }

    @Test
    void rejectsAVariableBoundTwice() throws Exception {
      String msg = ExpTestHelpers.error(PROGRAM.replace("forall x. plus", "forall x, x. plus"));
      assertTrue(msg.contains("Duplicate bound variable: x"), msg);
    }
  }

  @Nested
  class Expression {

    /**
     * Each section is printed, but the text does not parse back: the external sections appear under
     * reserved words that the grammar does not accept, and a unary symbol applied to a compound
     * term is printed after it, as {@code plus(x, y).succ}.
     */
    @Test
    void printsEachSectionInAFormThatDoesNotParse() throws Exception {
      String text = exp(LITERAL).toString();
      assertEquals(
          "literal {\n"
              + "\n\timports\n\t\tTy0\n"
              + "\n\ttypes\n\t\tNat\n"
              + "\tconstants\n\t\tzero : Nat\n"
              + "\tfunctions\n\t\tplus : Nat, Nat -> Nat\n\t\tsucc : Nat -> Nat\n"
              + "\tjava_types\n\t\tStr = \"java.lang.String\"\n"
              + "\tjava_constants\n\t\tStr = \"x => x\"\n"
              + "\tjava_functions\n\t\tcat : Str, Str -> Str = \"(x, y) => x + y\"\n"
              + "\tequations\n"
              + "\t\tforall x:Nat, y. plus(x, y.succ) = plus(x, y).succ\n"
              + "\t\tforall x. plus(x, zero) = x\n"
              + "\toptions\n\t\tprover = completion\n\t\ttimeout = 5\n"
              + "}",
          text);
      assertThrows(
          ParseException.class, () -> ExpTestHelpers.parse(BASE + "typeside Ty = " + text));
      assertEquals("literal {\n}", exp("typeside Ty = literal { }").toString());
    }

    @Test
    void dependsOnItsImports() throws Exception {
      TyExpRaw e = exp(LITERAL);
      assertEquals(List.of(new Pair<>("Ty0", Kind.TYPESIDE)), new ArrayList<>(e.deps()));
      assertEquals(List.of(new TyExpVar("Ty0")), new ArrayList<>(e.imports()));
      assertEquals(Map.of("timeout", "5", "prover", "completion"), e.options());
      assertTrue(e.allowedOptions().contains(AqlOption.allow_java_eqs_unsafe));
      List<Exp<?>> seen = new ArrayList<>();
      e.map(seen::add);
      assertEquals(e, seen.get(0));
    }

    @Test
    void indexesEachSectionForTheOutline() throws Exception {
      Map<String, List<InteriorLabel<Object>>> raw = exp(LITERAL).raw();
      assertEquals("Nat", raw.get("types").get(0).toString());
      assertTrue(
          Set.of("zero : Nat", "succ : Nat -> Nat", "plus : Nat,Nat -> Nat")
              .contains(raw.get("functions").get(0).toString()));
      assertEquals(2, raw.get("equations").size());
      assertEquals("Str = java.lang.String", raw.get("java_types").get(0).toString());
      assertEquals("Str = x => x", raw.get("java_constants").get(0).toString());
      assertEquals("cat : Str,Str -> Str", raw.get("java_functions").get(0).toString());
    }

    @Test
    void equalityComparesEveryPart() throws Exception {
      TyExpRaw e = exp(LITERAL);
      assertEquals(e, e);
      assertEquals(e, exp(LITERAL));
      assertEquals(e.hashCode(), exp(LITERAL).hashCode());
      for (Pair<String, String> change :
          List.of(
              new Pair<>("imports Ty0", ""),
              new Pair<>("types Nat", "types Nat Int"),
              new Pair<>("constants zero : Nat", "constants zero one : Nat"),
              new Pair<>("\"java.lang.String\"", "\"java.lang.Object\""),
              new Pair<>("\"x => x\"", "\"y => y\""),
              new Pair<>("\"(x, y) => x + y\"", "\"(x, y) => y + x\""),
              new Pair<>("forall x. plus(x, zero) = x", ""),
              new Pair<>("timeout = 5", "timeout = 6"))) {
        assertNotEquals(e, exp(LITERAL.replace(change.first, change.second)), change.second);
      }
      assertNotEquals(e, null);
      assertNotEquals(e, "literal");
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsAVisitorAndACoVisitor() throws Exception {
      TyExpRaw e = exp(LITERAL);
      TyExpVisitor<String, Unit, RuntimeException> v = mock(TyExpVisitor.class);
      when(v.visit(Unit.unit, e)).thenReturn("visited");
      assertEquals("visited", e.accept(Unit.unit, v));
      TyExpCoVisitor<Unit, Unit, RuntimeException> cv = mock(TyExpCoVisitor.class);
      when(cv.visitTyExpRaw(Unit.unit, Unit.unit)).thenReturn(e);
      assertEquals(e, e.coaccept(Unit.unit, cv, Unit.unit));
    }
  }
}
