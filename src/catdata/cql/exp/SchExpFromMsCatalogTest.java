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
import catdata.cql.exp.InstExp.InstExpVar;
import catdata.cql.exp.SchExp.SchExpVisitor;
import catdata.cql.exp.TyExp.TyExpVar;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SchExpFromMsCatalog}, {@code schema S = from_ms_catalog I}, which is
 * documented to build the schema of the tables of a SQL Server catalog imported as {@code I}.
 */
class SchExpFromMsCatalogTest {

  private static final String CATALOG =
      """
      typeside Ty = sql
      schema MsCat = ms_catalog Ty Varchar
      instance I = empty : MsCat
      schema S = from_ms_catalog I
      """;

  private final SchExpFromMsCatalog exp = new SchExpFromMsCatalog(new InstExpVar("I"), List.of());

  /**
   * Evaluation builds {@code from_ms_catalog I : null}, whose constructor rejects the missing
   * schema, so no catalog ever gives a schema.
   */
  @Test
  void neverEvaluates() throws Exception {
    String msg = ExpTestHelpers.error(CATALOG);
    assertTrue(msg.contains("Anomaly: please report"), msg);
  }

  @Test
  void isTypedByTheTypesideOfTheCatalog() throws Exception {
    AqlTyping g = ExpTestHelpers.typing(CATALOG);
    assertEquals(new TyExpVar("Ty"), g.defs.schs.get("S"));
    assertEquals(new TyExpVar("Ty"), exp.type(g));
  }

  @Test
  void printsAsItsKeyword() {
    assertEquals("from_ms_catalog I", exp.toString());
  }

  @Test
  void dependsOnTheCatalog() {
    assertEquals(List.of(new Pair<>("I", Kind.INSTANCE)), new ArrayList<>(exp.deps()));
  }

  @Test
  void keepsItsOptionsButAllowsOnlyTheCommonOnes() {
    SchExpFromMsCatalog e =
        new SchExpFromMsCatalog(new InstExpVar("I"), List.of(new Pair<>("timeout", "5")));
    assertEquals(Map.of("timeout", "5"), e.options());
    assertEquals(ExpTestHelpers.BASE_OPTIONS, e.allowedOptions());
  }

  @Test
  void resolvesToItself() {
    assertSame(exp, exp.resolve(new AqlTyping(), null));
  }

  @Test
  void mapSkipsTheCatalogItself() {
    List<Exp<?>> seen = new ArrayList<>();
    exp.map(seen::add);
    assertEquals(List.of(exp), seen);
  }

  @Test
  void equalityComparesTheCatalogAndTheOptions() {
    SchExpFromMsCatalog same = new SchExpFromMsCatalog(new InstExpVar("I"), List.of());
    assertEquals(exp, exp);
    assertEquals(exp, same);
    assertEquals(exp.hashCode(), same.hashCode());
    assertNotEquals(exp, new SchExpFromMsCatalog(new InstExpVar("J"), List.of()));
    assertNotEquals(
        exp, new SchExpFromMsCatalog(new InstExpVar("I"), List.of(new Pair<>("timeout", "5"))));
    assertNotEquals(exp, null);
    assertNotEquals(exp, "from_ms_catalog I");
  }

  @Test
  void equalityHandlesMissingParts() {
    SchExpFromMsCatalog none = new SchExpFromMsCatalog(null, List.of());
    none.options = null;
    SchExpFromMsCatalog other = new SchExpFromMsCatalog(null, List.of());
    other.options = null;
    assertEquals(none, other);
    assertEquals(none.hashCode(), other.hashCode());
    assertNotEquals(none, exp);
    assertNotEquals(exp, none);
    other.options = Map.of();
    assertNotEquals(none, other);
    assertNotEquals(other, none);
  }

  @Test
  @SuppressWarnings("unchecked")
  void acceptsAVisitor() {
    SchExpVisitor<String, Unit, RuntimeException> v = mock(SchExpVisitor.class);
    when(v.visit(Unit.unit, exp)).thenReturn("visited");
    assertEquals("visited", exp.accept(Unit.unit, v));
  }

  @Test
  void coacceptBuildsACatalogVariable() {
    AqlTyping g = new AqlTyping();
    SchExp e = exp.coaccept(Unit.unit, new TypingInversion(), g);
    assertEquals(new SchExpFromMsCatalog(new InstExpVar("i"), List.of()), e);
    assertEquals(new SchExpMsQuery(new TyExpSql(), "Other"), g.defs.insts.get("i"));
  }
}
