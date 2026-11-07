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
import catdata.cql.Schema;
import catdata.cql.exp.SchExp.SchExpVisitor;
import catdata.cql.exp.TyExp.TyExpVar;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SchExpMsCatalog}, the schema {@code ms_catalog T d} of the {@code
 * INFORMATION_SCHEMA} views of SQL Server, whose columns are all typed by {@code d}. Its text is
 * {@link MsSqlCatalog#cql_schema}.
 */
class SchExpMsCatalogTest {

  private static final String PROGRAM =
      """
      typeside Ty = literal { types Other Text }
      schema S = ms_catalog Ty Text
      """;

  private static SchExpMsCatalog exp() throws Exception {
    return ExpTestHelpers.exp(PROGRAM, "S");
  }

  @Test
  void declaresTheViewsOfTheCatalog() throws Exception {
    Schema<String, String, Sym, Fk, Att> s = ExpTestHelpers.eval(PROGRAM, "S", Kind.SCHEMA);
    assertEquals(20, s.ens.size());
    assertTrue(
        s.ens.containsAll(Set.of("TABLES", "COLUMNS", "TABLE_CONSTRAINTS")), s.ens.toString());
    assertTrue(s.fks.isEmpty());
    Set<String> tables = new TreeSet<>();
    s.atts.keySet().stream().filter(a -> a.en.equals("TABLES")).forEach(a -> tables.add(a.str));
    assertEquals(Set.of("TABLE_CATALOG", "TABLE_SCHEMA", "TABLE_NAME", "TABLE_TYPE"), tables);
  }

  @Test
  void typesEveryColumnByTheGivenType() throws Exception {
    Schema<String, String, Sym, Fk, Att> s = ExpTestHelpers.eval(PROGRAM, "S", Kind.SCHEMA);
    Set<String> types = new TreeSet<>();
    s.atts.values().forEach(p -> types.add(p.second));
    assertEquals(Set.of("Text"), types);
  }

  @Test
  void rejectsATypeTheTypesideLacks() throws Exception {
    String msg =
        ExpTestHelpers.error(
            """
            typeside Ty = literal { types Other }
            schema S = ms_catalog Ty Missing
            """);
    assertTrue(msg.contains("Missing"), msg);
  }

  @Test
  void printsAsItsKeyword() throws Exception {
    assertEquals("ms_catalog Ty Text", exp().toString());
  }

  @Test
  void dependsOnTheTypesideOnly() throws Exception {
    assertEquals(List.of(new Pair<>("Ty", Kind.TYPESIDE)), new ArrayList<>(exp().deps()));
  }

  @Test
  void hasNoOptionsAndAllowsOnlyTheCommonOnes() throws Exception {
    assertTrue(exp().options().isEmpty());
    assertEquals(ExpTestHelpers.BASE_OPTIONS, exp().allowedOptions());
  }

  @Test
  void isTypedByItsTypeside() throws Exception {
    AqlTyping g = ExpTestHelpers.typing(PROGRAM);
    assertEquals(new TyExpVar("Ty"), exp().type(g));
    assertEquals(new TyExpVar("Ty"), g.defs.schs.get("S"));
  }

  @Test
  void resolvesToItself() throws Exception {
    SchExpMsCatalog e = exp();
    assertSame(e, e.resolve(new AqlTyping(), null));
  }

  /** Like ms_query, the typeside itself is not visited. */
  @Test
  void mapSkipsTheTypesideItself() throws Exception {
    List<Exp<?>> seen = new ArrayList<>();
    SchExpMsCatalog e = exp();
    e.map(seen::add);
    assertEquals(List.of(e), seen);
  }

  @Test
  void equalityComparesTheTypesideAndTheType() {
    SchExpMsCatalog a = new SchExpMsCatalog(new TyExpVar("Ty"), "Text");
    assertEquals(a, a);
    assertEquals(a, new SchExpMsCatalog(new TyExpVar("Ty"), "Text"));
    assertEquals(a.hashCode(), new SchExpMsCatalog(new TyExpVar("Ty"), "Text").hashCode());
    assertNotEquals(a, new SchExpMsCatalog(new TyExpVar("Ty"), "Other"));
    assertNotEquals(a, new SchExpMsCatalog(new TyExpVar("U"), "Text"));
    assertNotEquals(a, null);
    assertNotEquals(a, "ms_catalog Ty Text");
  }

  @Test
  void equalityHandlesMissingParts() {
    SchExpMsCatalog none = new SchExpMsCatalog(null, null);
    SchExpMsCatalog full = new SchExpMsCatalog(new TyExpVar("Ty"), "Text");
    assertEquals(none, new SchExpMsCatalog(null, null));
    assertEquals(none.hashCode(), new SchExpMsCatalog(null, null).hashCode());
    assertNotEquals(none, full);
    assertNotEquals(full, none);
    assertNotEquals(new SchExpMsCatalog(null, "Text"), full);
    assertNotEquals(full, new SchExpMsCatalog(new TyExpVar("Ty"), null));
  }

  @Test
  @SuppressWarnings("unchecked")
  void acceptsAVisitor() throws Exception {
    SchExpMsCatalog e = exp();
    SchExpVisitor<String, Unit, RuntimeException> v = mock(SchExpVisitor.class);
    when(v.visit(Unit.unit, e)).thenReturn("visited");
    assertEquals("visited", e.accept(Unit.unit, v));
  }

  @Test
  void coacceptBuildsTheSqlInstance() throws Exception {
    SchExp e = exp().coaccept(Unit.unit, new TypingInversion(), new AqlTyping());
    assertEquals(new SchExpMsCatalog(new TyExpSql(), "Other"), e);
  }
}
