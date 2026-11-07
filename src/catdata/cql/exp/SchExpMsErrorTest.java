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
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SchExpMsError}, {@code schema S = ms_error d url : T}, the schema of the
 * table of SQL Server error messages, with one entity {@code Error} whose attributes are typed by
 * {@code d}. The JDBC string is kept but not used.
 */
class SchExpMsErrorTest {

  private static final String PROGRAM =
      """
      typeside Ty = literal { types Text }
      schema S = ms_error Text "jdbc:none" : Ty
      """;

  private static SchExpMsError exp() throws Exception {
    return ExpTestHelpers.exp(PROGRAM, "S");
  }

  @Test
  void declaresTheErrorEntity() throws Exception {
    Schema<String, String, Sym, Fk, Att> s = ExpTestHelpers.eval(PROGRAM, "S", Kind.SCHEMA);
    assertEquals(List.of("Error"), List.copyOf(s.ens));
    Map<String, String> atts = new TreeMap<>();
    s.atts.forEach((a, p) -> atts.put(a.str, p.second));
    assertEquals(
        Map.of("logged", "Text", "message_id", "Text", "severity", "Text", "text", "Text"), atts);
  }

  @Test
  void printsAsItsKeyword() throws Exception {
    assertEquals("ms_error Text jdbc:none : Ty", exp().toString());
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
  }

  @Test
  void resolvesToItself() throws Exception {
    SchExpMsError e = exp();
    assertSame(e, e.resolve(new AqlTyping(), null));
  }

  @Test
  void mapSkipsTheTypesideItself() throws Exception {
    List<Exp<?>> seen = new ArrayList<>();
    SchExpMsError e = exp();
    e.map(seen::add);
    assertEquals(List.of(e), seen);
  }

  @Test
  void equalityComparesAllThreeParts() {
    SchExpMsError a = new SchExpMsError("Text", "url", new TyExpVar("Ty"));
    assertEquals(a, a);
    assertEquals(a, new SchExpMsError("Text", "url", new TyExpVar("Ty")));
    assertEquals(a.hashCode(), new SchExpMsError("Text", "url", new TyExpVar("Ty")).hashCode());
    assertNotEquals(a, new SchExpMsError("Other", "url", new TyExpVar("Ty")));
    assertNotEquals(a, new SchExpMsError("Text", "url2", new TyExpVar("Ty")));
    assertNotEquals(a, new SchExpMsError("Text", "url", new TyExpVar("U")));
    assertNotEquals(a, null);
    assertNotEquals(a, "ms_error");
  }

  @Test
  void equalityHandlesMissingParts() {
    SchExpMsError none = new SchExpMsError(null, null, null);
    SchExpMsError full = new SchExpMsError("Text", "url", new TyExpVar("Ty"));
    assertEquals(none, new SchExpMsError(null, null, null));
    assertEquals(none.hashCode(), new SchExpMsError(null, null, null).hashCode());
    assertNotEquals(none, full);
    assertNotEquals(full, none);
    assertNotEquals(new SchExpMsError("Text", null, null), full);
    assertNotEquals(new SchExpMsError("Text", "url", null), full);
    assertNotEquals(full, new SchExpMsError("Text", "url", null));
  }

  @Test
  @SuppressWarnings("unchecked")
  void acceptsAVisitor() throws Exception {
    SchExpMsError e = exp();
    SchExpVisitor<String, Unit, RuntimeException> v = mock(SchExpVisitor.class);
    when(v.visit(Unit.unit, e)).thenReturn("visited");
    assertEquals("visited", e.accept(Unit.unit, v));
  }

  @Test
  void coacceptBuildsTheSqlInstance() throws Exception {
    SchExp e = exp().coaccept(Unit.unit, new TypingInversion(), new AqlTyping());
    assertEquals(new SchExpMsError("Other", "jdbc_string", new TyExpSql()), e);
  }
}
