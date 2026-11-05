package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.Pair;
import catdata.Unit;
import catdata.cql.Kind;
import catdata.cql.exp.TyExp.TyExpCoVisitor;
import catdata.cql.exp.TyExp.TyExpVar;
import catdata.cql.exp.TyExp.TyExpVisitor;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TyExpSqlNull}, {@code typeside T = sqlNull Ty}, which adds SQL's
 * three-valued nulls to a typeside. The construction fails on every typeside we tried; these tests
 * pin that and cover the expression.
 */
class TyExpSqlNullTest {

  private final TyExpSqlNull exp = new TyExpSqlNull(new TyExpVar("Ty"));

  @Nested
  class Evaluation {

    @Test
    void refusesATypesideWithJavaTypes() throws Exception {
      String msg = ExpTestHelpers.error("typeside Ty = sql\ntypeside T = sqlNull Ty\n");
      assertTrue(msg.contains("Cannot nullify java typesides"), msg);
    }

    /** The constant named in the message, true or false, depends on the order of a hash map. */
    @Test
    void failsOnATypesideWithoutBooleans() throws Exception {
      String msg =
          ExpTestHelpers.error("typeside Ty = literal { types String }\ntypeside T = sqlNull Ty\n");
      assertTrue(msg.contains(", not a declared type: boolean"), msg);
    }
  }

  @Nested
  class Expression {

    @Test
    void printsItsParent() {
      assertEquals("sqlNull Ty", exp.toString());
      assertEquals(Unit.unit, exp.type(new AqlTyping()));
    }

    @Test
    void dependsOnItsParent() {
      assertEquals(List.of(new Pair<>("Ty", Kind.TYPESIDE)), new ArrayList<>(exp.deps()));
      assertTrue(exp.options().isEmpty());
      assertEquals(ExpTestHelpers.BASE_OPTIONS, exp.allowedOptions());
      List<Exp<?>> seen = new ArrayList<>();
      exp.map(seen::add);
      assertEquals(List.of(exp, new TyExpVar("Ty")), seen);
    }

    @Test
    void equalityComparesTheParent() {
      assertEquals(exp, exp);
      assertEquals(exp, new TyExpSqlNull(new TyExpVar("Ty")));
      assertEquals(exp.hashCode(), new TyExpSqlNull(new TyExpVar("Ty")).hashCode());
      assertNotEquals(exp, new TyExpSqlNull(new TyExpVar("Ty2")));
      TyExpSqlNull none = new TyExpSqlNull(null);
      assertEquals(none, new TyExpSqlNull(null));
      assertEquals(none.hashCode(), new TyExpSqlNull(null).hashCode());
      assertNotEquals(none, exp);
      assertNotEquals(exp, none);
      assertNotEquals(exp, null);
      assertNotEquals(exp, "sqlNull");
    }

    /** The co-visitor treats the expression as the sql typeside. */
    @Test
    @SuppressWarnings("unchecked")
    void acceptsAVisitorAndACoVisitor() {
      TyExpVisitor<String, Unit, RuntimeException> v = mock(TyExpVisitor.class);
      when(v.visit(Unit.unit, exp)).thenReturn("visited");
      assertEquals("visited", exp.accept(Unit.unit, v));
      TyExpCoVisitor<Unit, Unit, RuntimeException> cv = mock(TyExpCoVisitor.class);
      TyExpSql sql = new TyExpSql();
      when(cv.visitTyExpSql(Unit.unit, Unit.unit)).thenReturn(sql);
      assertEquals(sql, exp.coaccept(Unit.unit, cv, Unit.unit));
    }
  }
}
