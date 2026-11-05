package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.Pair;
import catdata.Unit;
import catdata.cql.Kind;
import catdata.cql.Schema;
import catdata.cql.exp.SchExp.SchExpVar;
import catdata.cql.exp.SchExp.SchExpVisitor;
import catdata.cql.exp.TyExp.TyExpVar;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SchExpDiff}, {@code except S T}, the schema S without the entities, foreign
 * keys and attributes of T.
 */
class SchExpDiffTest {

  private static final String BASE =
      """
      typeside Ty = literal { types String }
      typeside Ty2 = literal { types Int }
      schema S = literal : Ty {
        entities A B
        foreign_keys f : A -> B
        attributes n : A -> String m : B -> String
      }
      schema T = literal : Ty { entities B attributes m : B -> String }
      schema C = literal : Ty { entities C }
      schema S2 = literal : Ty { entities A B attributes n : A -> String m : B -> String }
      schema E = literal : Ty { entities A B foreign_keys f g : A -> B path_equations A.f = A.g }
      schema U = literal : Ty2 { entities B }
      """;

  private static Schema<String, String, Sym, Fk, Att> except(String s, String t) throws Exception {
    return ExpTestHelpers.eval(BASE + "schema X = except " + s + " " + t + "\n", "X", Kind.SCHEMA);
  }

  private final SchExpDiff exp = new SchExpDiff(new SchExpVar("S"), new SchExpVar("T"));

  @Nested
  class Evaluation {

    @Test
    void keepsWhatTheSecondSchemaLacks() throws Exception {
      Schema<String, String, Sym, Fk, Att> x = except("S", "C");
      assertEquals(Set.of("A", "B"), x.ens);
      assertEquals(Map.of(Fk.Fk("A", "f"), new Pair<>("A", "B")), x.fks);
      assertEquals(2, x.atts.size());
    }

    @Test
    void removesTheEntitiesOfTheSecondSchemaWithTheirAttributes() throws Exception {
      Schema<String, String, Sym, Fk, Att> x = except("S2", "T");
      assertEquals(Set.of("A"), x.ens);
      assertEquals(Map.of(Att.Att("A", "n"), new Pair<>("A", "String")), x.atts);
    }

    /** f still points at B, which is removed, and the schema fails to build. */
    @Test
    void failsOnAForeignKeyIntoARemovedEntity() throws Exception {
      String msg = ExpTestHelpers.error(BASE + "schema X = except S T\n");
      assertTrue(msg.contains("NullPointerException"), msg);
    }

    @Test
    void rejectsEquations() throws Exception {
      String msg = ExpTestHelpers.error(BASE + "schema X = except E C\n");
      assertTrue(msg.contains("Equations not support with schema except"), msg);
    }

    @Test
    void rejectsSchemasOnDifferentTypesides() {
      String program = BASE + "schema X = except S U\n";
      RuntimeException e =
          assertThrows(RuntimeException.class, () -> ExpTestHelpers.typing(program));
      assertTrue(e.getMessage().contains("Unequal type sides: except S U"), e.getMessage());
    }
  }

  @Nested
  class Expression {

    @Test
    void printsBothSchemas() {
      assertEquals("except S T", exp.toString());
    }

    @Test
    void isTypedByTheTypesideOfBoth() throws Exception {
      assertEquals(new TyExpVar("Ty"), exp.type(ExpTestHelpers.typing(BASE)));
      assertSame(exp, exp.resolve(new AqlTyping(), null));
      assertEquals(
          Set.of(new Pair<>("S", Kind.SCHEMA), new Pair<>("T", Kind.SCHEMA)),
          Set.copyOf(exp.deps()));
      assertTrue(exp.options().isEmpty());
      List<Exp<?>> seen = new ArrayList<>();
      exp.map(seen::add);
      assertEquals(List.of(exp), seen);
    }

    @Test
    void equalityComparesBothSchemas() {
      assertEquals(exp, exp);
      assertEquals(exp, new SchExpDiff(new SchExpVar("S"), new SchExpVar("T")));
      assertEquals(
          exp.hashCode(), new SchExpDiff(new SchExpVar("S"), new SchExpVar("T")).hashCode());
      assertNotEquals(exp, new SchExpDiff(new SchExpVar("T"), new SchExpVar("T")));
      assertNotEquals(exp, new SchExpDiff(new SchExpVar("S"), new SchExpVar("S")));
      SchExpDiff none = new SchExpDiff(null, null);
      assertEquals(none, new SchExpDiff(null, null));
      assertEquals(none.hashCode(), new SchExpDiff(null, null).hashCode());
      assertNotEquals(none, exp);
      assertNotEquals(exp, none);
      assertNotEquals(new SchExpDiff(new SchExpVar("S"), null), exp);
      assertNotEquals(exp, null);
      assertNotEquals(exp, "except");
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsAVisitor() {
      SchExpVisitor<String, Unit, RuntimeException> v = mock(SchExpVisitor.class);
      when(v.visit(Unit.unit, exp)).thenReturn("visited");
      assertEquals("visited", exp.accept(Unit.unit, v));
    }
  }
}
