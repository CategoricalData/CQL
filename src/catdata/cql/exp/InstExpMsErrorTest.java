package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.Pair;
import catdata.Unit;
import catdata.cql.Kind;
import catdata.cql.exp.InstExp.InstExpVisitor;
import catdata.cql.exp.TyExp.TyExpVar;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link InstExpMsError}, {@code instance I = ms_error d T url}, which imports the
 * English messages of {@code sys.messages} from a SQL Server onto the schema {@code ms_error}. The
 * query is T-SQL, which H2 does not run even in its SQL Server mode, so the tests see the query in
 * the import's error.
 */
class InstExpMsErrorTest {

  private static String program(String url) {
    return """
        typeside Ty = sql
        instance I = ms_error Varchar Ty "%s"
        """
        .formatted(url);
  }

  private final InstExpMsError exp = new InstExpMsError("Varchar", "url", new TyExpVar("Ty"));

  @Test
  void importsTheEnglishMessagesOfSysMessages() throws Exception {
    String url =
        ExpTestHelpers.h2(
                "create schema sys",
                "create table sys.messages(message_id int, language_id int, severity int,"
                    + " is_event_logged int, text varchar(99))")
            .replace(";DB_CLOSE_DELAY=-1", ";MODE=MSSQLServer;DB_CLOSE_DELAY=-1");
    String msg = ExpTestHelpers.error(program(url));
    assertTrue(msg.contains("On entity Error, SQL error"), msg);
    assertTrue(msg.contains("FROM sys.messages"), msg);
    assertTrue(msg.contains("WHERE language_id = 1033"), msg);
    assertTrue(msg.contains("Column \"LOGGED\" not found"), msg);
  }

  @Test
  void isTypedByTheMsErrorSchema() throws Exception {
    AqlTyping g = ExpTestHelpers.typing(program("url"));
    SchExpMsError s = new SchExpMsError("Varchar", "url", new TyExpVar("Ty"));
    assertEquals(s, g.defs.insts.get("I"));
    assertEquals(s, exp.type(g));
  }

  @Test
  void printsAsItsKeyword() {
    assertEquals("ms_error Varchar Ty url", exp.toString());
  }

  @Test
  void dependsOnTheTypesideOnly() {
    assertEquals(List.of(new Pair<>("Ty", Kind.TYPESIDE)), new ArrayList<>(exp.deps()));
    assertTrue(exp.direct(new AqlTyping()).isEmpty());
  }

  @Test
  void hasNoOptionsAndAllowsOnlyTheCommonOnes() {
    assertTrue(exp.options().isEmpty());
    assertEquals(ExpTestHelpers.BASE_OPTIONS, exp.allowedOptions());
  }

  @Test
  void mapsOverItsTypeside() {
    List<Exp<?>> seen = new ArrayList<>();
    exp.map(seen::add);
    assertEquals(List.of(exp, new TyExpVar("Ty")), seen);
  }

  @Test
  void equalityComparesAllThreeParts() {
    assertEquals(exp, exp);
    assertEquals(exp, new InstExpMsError("Varchar", "url", new TyExpVar("Ty")));
    assertEquals(
        exp.hashCode(), new InstExpMsError("Varchar", "url", new TyExpVar("Ty")).hashCode());
    assertNotEquals(exp, new InstExpMsError("Text", "url", new TyExpVar("Ty")));
    assertNotEquals(exp, new InstExpMsError("Varchar", "url2", new TyExpVar("Ty")));
    assertNotEquals(exp, new InstExpMsError("Varchar", "url", new TyExpVar("U")));
    assertNotEquals(exp, null);
    assertNotEquals(exp, "ms_error");
  }

  @Test
  void equalityHandlesMissingParts() {
    InstExpMsError none = new InstExpMsError(null, null, null);
    assertEquals(none, new InstExpMsError(null, null, null));
    assertEquals(none.hashCode(), new InstExpMsError(null, null, null).hashCode());
    assertNotEquals(none, exp);
    assertNotEquals(exp, none);
    assertNotEquals(new InstExpMsError("Varchar", null, null), exp);
    assertNotEquals(new InstExpMsError("Varchar", "url", null), exp);
    assertNotEquals(exp, new InstExpMsError("Varchar", "url", null));
  }

  @Test
  @SuppressWarnings("unchecked")
  void acceptsAVisitor() {
    InstExpVisitor<String, Unit, RuntimeException> v = mock(InstExpVisitor.class);
    when(v.visit(Unit.unit, exp)).thenReturn("visited");
    assertEquals("visited", exp.accept(Unit.unit, v));
  }
}
