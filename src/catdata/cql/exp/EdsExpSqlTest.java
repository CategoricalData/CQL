package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.Pair;
import catdata.Unit;
import catdata.cql.Constraints;
import catdata.cql.Kind;
import catdata.cql.exp.EdsExp.EdsExpVisitor;
import catdata.cql.exp.SchExp.SchExpVar;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link EdsExpSql}, {@code constraints C = sql S}, the constraints that make the
 * comparison symbols of the SQL typeside behave as an equality on the values of S.
 */
class EdsExpSqlTest {

  private static final String SQL =
      """
      typeside Ty = sql
      schema S = literal : Ty { entities A attributes n : A -> Varchar }
      constraints C = sql S
      """;

  private final EdsExpSql exp = new EdsExpSql(new SchExpVar("S"));

  @Nested
  class Evaluation {

    @Test
    void makesConstraintsOnTheSchema() throws Exception {
      Constraints c = ExpTestHelpers.eval(SQL, "C", Kind.CONSTRAINTS);
      assertFalse(c.eds.isEmpty());
      assertEquals(Set.of("A"), c.schema.ens);
      assertTrue(c.toString().contains("isFalse((x eq x)) = false"), c.toString());
    }

    /** Off the SQL typeside the constraints name symbols that the typeside lacks. */
    @Test
    void failsWhenUsedOffTheSqlTypeside() throws Exception {
      String program =
          """
          typeside Ty = literal { types String }
          schema S = literal : Ty { entities A attributes n : A -> String }
          constraints C = sql S
          instance I = literal : S { generators a : A }
          instance J = chase C I
          """;
      Constraints c =
          ExpTestHelpers.eval(
              program.replace("instance J = chase C I\n", ""), "C", Kind.CONSTRAINTS);
      assertFalse(c.eds.isEmpty());
      String msg = ExpTestHelpers.error(program);
      assertTrue(msg.contains("isFalse is not a typeside symbol"), msg);
    }
  }

  @Nested
  class Expression {

    @Test
    void printsItsSchema() {
      assertEquals("sql S", exp.toString());
      assertFalse(exp.isVar());
    }

    @Test
    void isTypedByItsSchema() throws Exception {
      assertEquals(new SchExpVar("S"), exp.type(ExpTestHelpers.typing(SQL)));
      assertEquals(List.of(new Pair<>("S", Kind.SCHEMA)), new ArrayList<>(exp.deps()));
      assertTrue(exp.options().isEmpty());
      assertEquals(ExpTestHelpers.BASE_OPTIONS, exp.allowedOptions());
      List<Exp<?>> seen = new ArrayList<>();
      exp.map(seen::add);
      assertEquals(List.of(exp), seen);
    }

    @Test
    void equalityComparesTheSchema() {
      assertEquals(exp, exp);
      assertEquals(exp, new EdsExpSql(new SchExpVar("S")));
      assertEquals(exp.hashCode(), new EdsExpSql(new SchExpVar("S")).hashCode());
      assertNotEquals(exp, new EdsExpSql(new SchExpVar("T")));
      EdsExpSql none = new EdsExpSql(null);
      assertEquals(none, new EdsExpSql(null));
      assertEquals(none.hashCode(), new EdsExpSql(null).hashCode());
      assertNotEquals(none, exp);
      assertNotEquals(exp, none);
      assertNotEquals(exp, null);
      assertNotEquals(exp, "sql");
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsAVisitor() {
      EdsExpVisitor<String, Unit, RuntimeException> v = mock(EdsExpVisitor.class);
      when(v.visit(Unit.unit, exp)).thenReturn("visited");
      assertEquals("visited", exp.accept(Unit.unit, v));
    }
  }
}
