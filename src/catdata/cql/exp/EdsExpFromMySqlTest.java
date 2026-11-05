package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.Pair;
import catdata.Unit;
import catdata.cql.Kind;
import catdata.cql.exp.EdsExp.EdsExpVisitor;
import catdata.cql.exp.SchExp.SchExpVar;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link EdsExpFromMySql}, {@code constraints C = from_mysql url : S}, which reads
 * the foreign, primary and unique keys of the current MySQL database. It loads {@code
 * com.mysql.jdbc.Driver} first, and CQL ships only the H2 driver, so the tests see that failure.
 */
class EdsExpFromMySqlTest {

  private static final String PROGRAM =
      """
      typeside Ty = sql
      schema S = literal : Ty { entities P }
      constraints Cs = from_mysql "jdbc:mysql://localhost/db" : S
      """;

  private final EdsExpFromMySql exp = new EdsExpFromMySql("url", new SchExpVar("S"));

  @Test
  void needsTheMySqlDriver() throws Exception {
    String msg = ExpTestHelpers.error(PROGRAM);
    assertTrue(msg.contains("java.lang.ClassNotFoundException: com.mysql.jdbc.Driver"), msg);
  }

  @Test
  void printsAsItsKeyword() {
    assertEquals("from_mysql url : S", exp.toString());
  }

  @Test
  void dependsOnTheSchema() {
    assertEquals(List.of(new Pair<>("S", Kind.SCHEMA)), new ArrayList<>(exp.deps()));
  }

  @Test
  void hasNoOptionsAndAllowsOnlyTheCommonOnes() {
    assertTrue(exp.options().isEmpty());
    assertEquals(ExpTestHelpers.BASE_OPTIONS, exp.allowedOptions());
  }

  @Test
  void isTypedByItsSchema() throws Exception {
    AqlTyping g = ExpTestHelpers.typing(PROGRAM);
    assertEquals(new SchExpVar("S"), g.defs.eds.get("Cs"));
    assertEquals(new SchExpVar("S"), exp.type(g));
  }

  @Test
  void mapSkipsTheSchemaItself() {
    List<Exp<?>> seen = new ArrayList<>();
    exp.map(seen::add);
    assertEquals(List.of(exp), seen);
  }

  @Test
  void equalityComparesTheUrlAndTheSchema() {
    assertEquals(exp, exp);
    assertEquals(exp, new EdsExpFromMySql("url", new SchExpVar("S")));
    assertEquals(exp.hashCode(), new EdsExpFromMySql("url", new SchExpVar("S")).hashCode());
    assertNotEquals(exp, new EdsExpFromMySql("url2", new SchExpVar("S")));
    assertNotEquals(exp, new EdsExpFromMySql("url", new SchExpVar("T")));
    assertNotEquals(exp, null);
    assertNotEquals(exp, "from_mysql url : S");
  }

  @Test
  @SuppressWarnings("unchecked")
  void acceptsAVisitor() {
    EdsExpVisitor<String, Unit, RuntimeException> v = mock(EdsExpVisitor.class);
    when(v.visit(Unit.unit, exp)).thenReturn("visited");
    assertEquals("visited", exp.accept(Unit.unit, v));
  }
}
