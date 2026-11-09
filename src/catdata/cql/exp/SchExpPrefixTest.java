package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.Chc;
import catdata.Pair;
import catdata.Unit;
import catdata.cql.Kind;
import catdata.cql.Schema;
import catdata.cql.Term;
import catdata.cql.exp.SchExp.SchExpCoVisitor;
import catdata.cql.exp.SchExp.SchExpVar;
import catdata.cql.exp.SchExp.SchExpVisitor;
import catdata.cql.exp.TyExp.TyExpVar;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SchExpPrefix}, {@code prefix S "p"}, the copy of S whose entities have the
 * prefix p.
 */
class SchExpPrefixTest {

  private static final String PROGRAM =
      """
      typeside Ty = literal { types String constants x : String }
      schema S = literal : Ty {
        entities A B
        foreign_keys f g : A -> B
        path_equations A.f = A.g
        attributes n : A -> String
        observation_equations forall a. a.n = x
      }
      schema T = literal : Ty { entities A }
      schema P = prefix S "p"
      """;

  private static Schema<String, String, Sym, Fk, Att> prefixed() throws Exception {
    return ExpTestHelpers.eval(PROGRAM, "P", Kind.SCHEMA);
  }

  private final SchExpPrefix exp = new SchExpPrefix(new SchExpVar("S"), "p");

  @Nested
  class Evaluation {

    @Test
    void prefixesEveryEntity() throws Exception {
      Schema<String, String, Sym, Fk, Att> p = prefixed();
      assertEquals(Set.of("pA", "pB"), p.ens);
      assertEquals(
          Map.of(
              Fk.Fk("pA", "f"), new Pair<>("pA", "pB"), Fk.Fk("pA", "g"), new Pair<>("pA", "pB")),
          p.fks);
      assertEquals(Map.of(Att.Att("pA", "n"), new Pair<>("pA", "String")), p.atts);
      assertEquals(2, p.eqs.size());
      assertTrue(p.eqs.stream().allMatch(eq -> eq.first.second.equals("pA")));
      assertEquals("Prefix renaming", p.dp().toStringProver());
    }

    @Test
    void decidesAnEquationWithoutSymbolsOfTheSchema() throws Exception {
      Term<String, String, Sym, Fk, Att, Object, Object> v = Term.Var("v");
      assertTrue(prefixed().dp().eq(null, v, v));
    }

    /**
     * The decision procedure looks the prefixed foreign key up among the unprefixed ones, and adds
     * the prefix to the context it was given instead of to its own copy.
     */
    @Test
    void failsOnAForeignKeyAndPrefixesTheContextAgain() throws Exception {
      Map<String, Chc<String, String>> ctx = new HashMap<>(Map.of("v", Chc.inRight("pA")));
      Term<String, String, Sym, Fk, Att, Object, Object> f =
          Term.Fk(Fk.Fk("pA", "f"), Term.Var("v"));
      Term<String, String, Sym, Fk, Att, Object, Object> g =
          Term.Fk(Fk.Fk("pA", "g"), Term.Var("v"));
      Schema<String, String, Sym, Fk, Att> p = prefixed();
      assertThrows(NullPointerException.class, () -> p.dp().eq(ctx, f, g));
      assertEquals(Map.of("v", Chc.inRight("ppA")), ctx);
    }

    /** Checking the path equation of S in P writes to the immutable context of the check. */
    @Test
    void cannotBeTheTargetOfAMappingThatChecksAnEquation() throws Exception {
      String program =
          PROGRAM
              + "mapping M = literal : S -> P { entity A -> pA foreign_keys f -> f g -> g"
              + " attributes n -> n entity B -> pB }\n";
      String msg = ExpTestHelpers.error(program);
      assertTrue(msg.contains("UnsupportedOperationException"), msg);
    }
  }

  @Nested
  class Expression {

    @Test
    void printsTheSchemaAndThePrefix() {
      assertEquals("prefix S p", exp.toString());
    }

    @Test
    void isTypedAndDependsAsItsSchema() throws Exception {
      assertEquals(new TyExpVar("Ty"), exp.type(ExpTestHelpers.typing(PROGRAM)));
      assertEquals(List.of(new Pair<>("S", Kind.SCHEMA)), new ArrayList<>(exp.deps()));
      assertSame(exp, exp.resolve(new AqlTyping(), null));
      assertTrue(exp.options().isEmpty());
      assertEquals(ExpTestHelpers.BASE_OPTIONS, exp.allowedOptions());
      List<Exp<?>> seen = new ArrayList<>();
      exp.map(seen::add);
      assertEquals(List.of(exp), seen);
    }

    /** equals compares the prefixes twice and never the schemas, which hashCode does compare. */
    @Test
    void equalsAnyPrefixWithTheSamePrefix() {
      SchExpPrefix other = new SchExpPrefix(new SchExpVar("T"), "p");
      assertEquals(exp, other);
      assertNotEquals(exp.hashCode(), other.hashCode());
      assertEquals(exp, new SchExpPrefix(new SchExpVar("S"), "p"));
      assertEquals(exp.hashCode(), new SchExpPrefix(new SchExpVar("S"), "p").hashCode());
      assertNotEquals(exp, new SchExpPrefix(new SchExpVar("S"), "q"));
      SchExpPrefix none = new SchExpPrefix(null, null);
      assertEquals(none, new SchExpPrefix(null, null));
      assertEquals(none.hashCode(), new SchExpPrefix(null, null).hashCode());
      assertNotEquals(none, exp);
      assertNotEquals(exp, none);
      assertNotEquals(exp, null);
      assertNotEquals(exp, "prefix");
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsAVisitorAndACoVisitor() {
      SchExpVisitor<String, Unit, RuntimeException> v = mock(SchExpVisitor.class);
      when(v.visit(Unit.unit, exp)).thenReturn("visited");
      assertEquals("visited", exp.accept(Unit.unit, v));
      SchExpCoVisitor<Unit, Unit, RuntimeException> cv = mock(SchExpCoVisitor.class);
      when(cv.visitSchExpPrefix(Unit.unit, Unit.unit)).thenReturn(exp);
      assertEquals(exp, exp.coaccept(Unit.unit, cv, Unit.unit));
    }
  }
}
