package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.Pair;
import catdata.Unit;
import catdata.cql.AqlOptions;
import catdata.cql.Constraints;
import catdata.cql.Kind;
import catdata.cql.Schema;
import catdata.cql.TypeSide;
import catdata.cql.exp.EdsExp.EdsExpVisitor;
import catdata.cql.exp.TyExp.TyExpVar;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link EdsExpSqlNull}, {@code constraints C = sqlNull T}, the reflexivity,
 * symmetry and substitution constraints of the equality of each type of the three-valued typeside
 * {@code sqlNull T}.
 */
class EdsExpSqlNullTest {

  private final EdsExpSqlNull exp = new EdsExpSqlNull(new TyExpVar("Ty"));

  /** The typeside sqlNull T cannot be built, so neither can these constraints. */
  @Test
  void failsLikeItsTypeside() throws Exception {
    String msg =
        ExpTestHelpers.error(
            "typeside Ty = literal { types String }\nconstraints C = sqlNull Ty\n");
    assertTrue(msg.contains("not a declared type: boolean"), msg);
  }

  @Test
  void makesThreeConstraintsPerTypeButBool() throws Exception {
    TypeSide<String, Sym> ts =
        ExpTestHelpers.eval("typeside Ty = literal { types Bool Int Str }", "Ty", Kind.TYPESIDE);
    Schema<String, String, Sym, Fk, Att> t = Schema.terminal(ts);
    Constraints c = EdsExpSqlNull.makeEds(t, new AqlOptions(Map.of(), AqlOptions.initialOptions));
    assertEquals(6, c.eds.size());
    assertTrue(c.toString().contains("eq_Int"), c.toString());
    assertFalse(c.toString().contains("eq_Bool"), c.toString());
  }

  @Test
  void isTypedByTheEmptySchemaOfTheNullableTypeside() {
    assertEquals(new SchExpEmpty(new TyExpSqlNull(new TyExpVar("Ty"))), exp.type(new AqlTyping()));
  }

  @Test
  void printsAsItsKeyword() {
    assertEquals("sqlNull Ty", exp.toString());
  }

  @Test
  void dependsOnItsTypeside() {
    assertEquals(List.of(new Pair<>("Ty", Kind.TYPESIDE)), new ArrayList<>(exp.deps()));
    assertTrue(exp.options().isEmpty());
    assertFalse(exp.isVar());
    assertEquals(ExpTestHelpers.BASE_OPTIONS, exp.allowedOptions());
  }

  @Test
  void mapsOverItsTypeside() {
    List<Exp<?>> seen = new ArrayList<>();
    exp.map(seen::add);
    assertEquals(List.of(exp, new TyExpVar("Ty")), seen);
  }

  @Test
  void equalityComparesTheTypeside() {
    assertEquals(exp, exp);
    assertEquals(exp, new EdsExpSqlNull(new TyExpVar("Ty")));
    assertEquals(exp.hashCode(), new EdsExpSqlNull(new TyExpVar("Ty")).hashCode());
    assertNotEquals(exp, new EdsExpSqlNull(new TyExpVar("U")));
    assertEquals(new EdsExpSqlNull(null), new EdsExpSqlNull(null));
    assertEquals(new EdsExpSqlNull(null).hashCode(), new EdsExpSqlNull(null).hashCode());
    assertNotEquals(new EdsExpSqlNull(null), exp);
    assertNotEquals(exp, new EdsExpSqlNull(null));
    assertNotEquals(exp, null);
    assertNotEquals(exp, "sqlNull Ty");
  }

  @Test
  @SuppressWarnings("unchecked")
  void acceptsAVisitor() {
    EdsExpVisitor<String, Unit, RuntimeException> v = mock(EdsExpVisitor.class);
    when(v.visit(Unit.unit, exp)).thenReturn("visited");
    assertEquals("visited", exp.accept(Unit.unit, v));
  }
}
