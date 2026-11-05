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
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SchExpMsErrorShallow}, {@code schema S = ms_error_shallow d : T}, which has
 * one entity per SQL Server error message, read from the strings of {@link MsSqlError}, each with
 * four attributes typed by {@code d}.
 */
class SchExpMsErrorShallowTest {

  private static final String PROGRAM =
      """
      typeside Ty = literal { types Text }
      schema S = ms_error_shallow Text : Ty
      """;

  private static SchExpMsErrorShallow exp() throws Exception {
    return ExpTestHelpers.exp(PROGRAM, "S");
  }

  @Test
  void declaresAnEntityPerErrorMessage() throws Exception {
    Schema<String, String, Sym, Fk, Att> s = ExpTestHelpers.eval(PROGRAM, "S", Kind.SCHEMA);
    assertEquals(14070, s.ens.size());
    assertEquals(4 * 14070, s.atts.size());
    Map<String, String> atts = new TreeMap<>();
    s.atts.forEach(
        (a, p) -> {
          if (a.en.equals("Error21")) {
            atts.put(a.str, p.second);
          }
        });
    assertEquals(
        Map.of("cause_json", "Text", "cause_text", "Text", "level", "Text", "row_id", "Text"),
        atts);
    Set<String> types = new TreeSet<>();
    s.atts.values().forEach(p -> types.add(p.second));
    assertEquals(Set.of("Text"), types);
  }

  /** The JDBC string is never set, so it prints as null, which does not parse. */
  @Test
  void printsANullJdbcString() throws Exception {
    assertEquals("ms_error_shallow Text null : Ty", exp().toString());
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
    SchExpMsErrorShallow e = exp();
    assertSame(e, e.resolve(new AqlTyping(), null));
  }

  @Test
  void mapSkipsTheTypesideItself() throws Exception {
    List<Exp<?>> seen = new ArrayList<>();
    SchExpMsErrorShallow e = exp();
    e.map(seen::add);
    assertEquals(List.of(e), seen);
  }

  @Test
  void equalityComparesTheTypeAndTheTypeside() {
    SchExpMsErrorShallow a = new SchExpMsErrorShallow("Text", new TyExpVar("Ty"));
    assertEquals(a, a);
    assertEquals(a, new SchExpMsErrorShallow("Text", new TyExpVar("Ty")));
    assertEquals(a.hashCode(), new SchExpMsErrorShallow("Text", new TyExpVar("Ty")).hashCode());
    assertNotEquals(a, new SchExpMsErrorShallow("Other", new TyExpVar("Ty")));
    assertNotEquals(a, new SchExpMsErrorShallow("Text", new TyExpVar("U")));
    assertNotEquals(a, null);
    assertNotEquals(a, "ms_error_shallow");
  }

  @Test
  void equalityHandlesMissingParts() {
    SchExpMsErrorShallow none = new SchExpMsErrorShallow(null, null);
    SchExpMsErrorShallow full = new SchExpMsErrorShallow("Text", new TyExpVar("Ty"));
    assertEquals(none, new SchExpMsErrorShallow(null, null));
    assertEquals(none.hashCode(), new SchExpMsErrorShallow(null, null).hashCode());
    assertNotEquals(none, full);
    assertNotEquals(full, none);
    assertNotEquals(new SchExpMsErrorShallow("Text", null), full);
    assertNotEquals(full, new SchExpMsErrorShallow("Text", null));
  }

  @Test
  @SuppressWarnings("unchecked")
  void acceptsAVisitor() throws Exception {
    SchExpMsErrorShallow e = exp();
    SchExpVisitor<String, Unit, RuntimeException> v = mock(SchExpVisitor.class);
    when(v.visit(Unit.unit, e)).thenReturn("visited");
    assertEquals("visited", e.accept(Unit.unit, v));
  }

  /** Coaccept calls the visitor's method for ms_error, not ms_error_shallow. */
  @Test
  void coacceptBuildsAnMsErrorSchema() throws Exception {
    SchExp e = exp().coaccept(Unit.unit, new TypingInversion(), new AqlTyping());
    assertEquals(new SchExpMsError("Other", "jdbc_string", new TyExpSql()), e);
  }
}
