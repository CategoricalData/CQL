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
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SchExpMsQuery}, the schema {@code ms_query T d} of the abstract syntax
 * trees of SQL Server queries. Its body is the 6729 strings {@code MsSqlQuery.str0} to {@code
 * str6728}: the first declares the entities, and each of the others one attribute, typed by the
 * type {@code d}.
 */
class SchExpMsQueryTest {

  private static final String PROGRAM =
      """
      typeside Ty = literal { types String Text }
      schema S = ms_query Ty Text
      """;

  private static SchExpMsQuery exp() throws Exception {
    return ExpTestHelpers.exp(PROGRAM, "S");
  }

  @Test
  void sizeCountsTheStringFields() {
    List<String> names = new ArrayList<>();
    for (Field f : MsSqlQuery.class.getFields()) {
      if (Modifier.isStatic(f.getModifiers()) && f.getType().equals(String.class)) {
        names.add(f.getName());
      }
    }
    assertEquals(MsSqlQuery.size, names.size());
    for (int i = 0; i < MsSqlQuery.size; i++) {
      assertTrue(names.contains("str" + i), "str" + i);
    }
  }

  @Test
  void declaresEveryEntityAndAttributeOfTheStrings() throws Exception {
    Schema<String, String, Sym, Fk, Att> s = ExpTestHelpers.eval(PROGRAM, "S", Kind.SCHEMA);
    assertEquals(871, s.ens.size());
    assertEquals(MsSqlQuery.size - 1, s.atts.size());
    assertTrue(s.fks.isEmpty());
    assertTrue(s.eqs.isEmpty());
    Set<String> parts = new TreeSet<>();
    s.atts.keySet().stream().filter(a -> a.en.equals("TSqlScript")).forEach(a -> parts.add(a.str));
    assertEquals(
        Set.of("Batches", "fragment_text", "parent", "query_id", "query_seqno", "row_id"), parts);
  }

  @Test
  void typesEveryAttributeByTheGivenType() throws Exception {
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
            typeside Ty = literal { types String }
            schema S = ms_query Ty Missing
            """);
    assertTrue(msg.contains("Missing"), msg);
  }

  @Test
  void printsAsItsKeyword() throws Exception {
    assertEquals("ms_query Ty Text", exp().toString());
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
    SchExpMsQuery e = exp();
    assertSame(e, e.resolve(new AqlTyping(), null));
  }

  /**
   * MapSubExps passes the function to the sub-expressions of the typeside instead of to the
   * typeside itself, so a variable typeside is never visited.
   */
  @Test
  void mapSkipsTheTypesideItself() throws Exception {
    List<Exp<?>> seen = new ArrayList<>();
    SchExpMsQuery e = exp();
    e.map(seen::add);
    assertEquals(List.of(e), seen);
  }

  @Test
  void equalityComparesTheTypesideAndTheType() {
    SchExpMsQuery a = new SchExpMsQuery(new TyExpVar("Ty"), "Text");
    assertEquals(a, a);
    assertEquals(a, new SchExpMsQuery(new TyExpVar("Ty"), "Text"));
    assertEquals(a.hashCode(), new SchExpMsQuery(new TyExpVar("Ty"), "Text").hashCode());
    assertNotEquals(a, new SchExpMsQuery(new TyExpVar("Ty"), "String"));
    assertNotEquals(a, new SchExpMsQuery(new TyExpVar("Other"), "Text"));
    assertNotEquals(a, null);
    assertNotEquals(a, "ms_query Ty Text");
  }

  @Test
  void equalityHandlesMissingParts() {
    SchExpMsQuery none = new SchExpMsQuery(null, null);
    SchExpMsQuery full = new SchExpMsQuery(new TyExpVar("Ty"), "Text");
    assertEquals(none, new SchExpMsQuery(null, null));
    assertEquals(none.hashCode(), new SchExpMsQuery(null, null).hashCode());
    assertNotEquals(none, full);
    assertNotEquals(full, none);
    assertNotEquals(new SchExpMsQuery(null, "Text"), full);
    assertNotEquals(full, new SchExpMsQuery(new TyExpVar("Ty"), null));
  }

  @Test
  @SuppressWarnings("unchecked")
  void acceptsAVisitor() throws Exception {
    SchExpMsQuery e = exp();
    SchExpVisitor<String, Unit, RuntimeException> v = mock(SchExpVisitor.class);
    when(v.visit(Unit.unit, e)).thenReturn("visited");
    assertEquals("visited", e.accept(Unit.unit, v));
  }

  @Test
  void coacceptBuildsTheSqlInstance() throws Exception {
    SchExp e = exp().coaccept(Unit.unit, new TypingInversion(), new AqlTyping());
    assertEquals(new SchExpMsQuery(new TyExpSql(), "Other"), e);
  }
}
