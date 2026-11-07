package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.Pair;
import catdata.Unit;
import catdata.cql.Kind;
import catdata.cql.Mapping;
import catdata.cql.Schema;
import catdata.cql.exp.ColimSchExp.ColimSchExpVar;
import catdata.cql.exp.MapExp.MapExpVisitor;
import catdata.cql.exp.SchExp.SchExpCoVisitor;
import catdata.cql.exp.SchExp.SchExpVisitor;
import catdata.cql.exp.TyExp.TyExpVar;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link MapExpPseudo}, {@code mapping M = getPseudo C}, the mapping from a
 * pseudo-quotient schema colimit to the schema in which each isomorphism is an identity, and for
 * {@link SchExpPseudo}, the expression its typing gives as the target.
 */
class MapExpPseudoTest {

  private static final String PROGRAM =
      """
      typeside Ty = literal { types String }
      schema S = literal : Ty { entities A attributes n : A -> String }
      schema T = literal : Ty { entities B attributes m : B -> String }
      schema_colimit C = pseudo_quotient S + T : Ty {
        entity_isomorphisms i : S.A -> T.B
        equations forall x:S.A, x.n = x.i.m
      }
      schema_colimit Q = quotient S + T : Ty { entity_equations S.A = T.B }
      mapping M = getPseudo C
      """;

  private final MapExpPseudo exp = new MapExpPseudo(new ColimSchExpVar("C"));

  private final SchExpPseudo sch = new SchExpPseudo(new ColimSchExpVar("C"));

  @Nested
  class Evaluation {

    @Test
    void collapsesEachIsomorphism() throws Exception {
      Mapping<String, String, Sym, Fk, Att, String, Fk, Att> m =
          ExpTestHelpers.eval(PROGRAM, "M", Kind.MAPPING);
      assertEquals(Set.of("S_A", "T_B"), m.src.ens);
      assertEquals(Set.of("A"), m.dst.ens);
      assertEquals("A", m.ens.get("T_B"));
    }

    @Test
    void rejectsAColimitThatIsNotAPseudoQuotient() throws Exception {
      String msg = ExpTestHelpers.error(PROGRAM + "mapping N = getPseudo Q\n");
      assertTrue(msg.contains("Not a pseudo colimit: "), msg);
    }

    /** The target in the typing evaluates to the colimit itself, not to the target of M. */
    @Test
    void typesTheTargetAsASchemaThatIsNotTheTarget() throws Exception {
      AqlEnv env = ExpTestHelpers.run(PROGRAM).env;
      Pair<SchExp, SchExp> type = exp.type(ExpTestHelpers.typing(PROGRAM));
      assertEquals(new SchExpColim(new ColimSchExpVar("C")), type.first);
      assertEquals(sch, type.second);
      Schema<String, String, Sym, Fk, Att> target = sch.eval(env, false);
      Mapping<String, String, Sym, Fk, Att, String, Fk, Att> m =
          ExpTestHelpers.value(ExpTestHelpers.run(PROGRAM), "M", Kind.MAPPING);
      assertEquals(Set.of("S_A", "T_B"), target.ens);
      assertNotEquals(m.dst.ens, target.ens);
      assertEquals(new TyExpVar("Ty"), sch.type(ExpTestHelpers.typing(PROGRAM)));
    }
  }

  @Nested
  class Expression {

    @Test
    void printsTheColimit() {
      assertEquals("getPseudo C", exp.toString());
      assertEquals("getPseudo C", sch.toString());
    }

    @Test
    void dependsOnTheColimit() {
      for (Exp<?> e : List.<Exp<?>>of(exp, sch)) {
        assertEquals(List.of(new Pair<>("C", Kind.SCHEMA_COLIMIT)), new ArrayList<>(e.deps()));
        assertTrue(e.options().isEmpty());
        assertEquals(ExpTestHelpers.BASE_OPTIONS, e.allowedOptions());
        List<Exp<?>> seen = new ArrayList<>();
        e.map(seen::add);
        assertEquals(List.of(e, new ColimSchExpVar("C")), seen);
      }
      assertSame(sch, sch.resolve(new AqlTyping(), null));
    }

    @Test
    void equalityComparesTheColimit() {
      assertEquals(exp, exp);
      assertEquals(exp, new MapExpPseudo(new ColimSchExpVar("C")));
      assertEquals(exp.hashCode(), new MapExpPseudo(new ColimSchExpVar("C")).hashCode());
      assertNotEquals(exp, new MapExpPseudo(new ColimSchExpVar("D")));
      MapExpPseudo none = new MapExpPseudo(null);
      assertEquals(none, new MapExpPseudo(null));
      assertEquals(none.hashCode(), new MapExpPseudo(null).hashCode());
      assertNotEquals(none, exp);
      assertNotEquals(exp, none);
      assertNotEquals(exp, null);
      assertNotEquals(exp, "getPseudo");
      assertEquals(sch, sch);
      assertEquals(sch, new SchExpPseudo(new ColimSchExpVar("C")));
      assertEquals(sch.hashCode(), new SchExpPseudo(new ColimSchExpVar("C")).hashCode());
      assertNotEquals(sch, new SchExpPseudo(new ColimSchExpVar("D")));
      assertNotEquals(sch, null);
      assertNotEquals(sch, "getPseudo");
    }

    /** The co-visitor treats the expression as getSchema C. */
    @Test
    @SuppressWarnings("unchecked")
    void acceptsVisitors() {
      MapExpVisitor<String, Unit, RuntimeException> v = mock(MapExpVisitor.class);
      when(v.visit(Unit.unit, exp)).thenReturn("mapping");
      assertEquals("mapping", exp.accept(Unit.unit, v));
      SchExpVisitor<String, Unit, RuntimeException> w = mock(SchExpVisitor.class);
      when(w.visit(Unit.unit, sch)).thenReturn("schema");
      assertEquals("schema", sch.accept(Unit.unit, w));
      SchExpCoVisitor<Unit, Unit, RuntimeException> cv = mock(SchExpCoVisitor.class);
      SchExpColim colim = new SchExpColim(new ColimSchExpVar("C"));
      when(cv.visitSchExpColim(Unit.unit, Unit.unit)).thenReturn(colim);
      assertEquals(colim, sch.coaccept(Unit.unit, cv, Unit.unit));
    }
  }
}
