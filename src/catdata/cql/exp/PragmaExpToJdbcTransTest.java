package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.Pair;
import catdata.Unit;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.Kind;
import catdata.cql.exp.PragmaExp.PragmaExpVisitor;
import catdata.cql.exp.TransExp.TransExpVar;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PragmaExpToJdbcTrans}, {@code export_jdbc_transform t "url" "prefix"},
 * which the help describes as writing a two-column table per entity, but whose command now always
 * fails.
 */
class PragmaExpToJdbcTransTest {

  private static final String PROGRAM =
      """
      typeside Ty = sql
      schema S = literal : Ty { entities A }
      instance I = literal : S { generators a : A }
      transform t = identity I
      command E = export_jdbc_transform t "" "pre_"
      """;

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static PragmaExpToJdbcTrans<?, ?, ?, ?, ?, ?, ?, ?> exp(
      String url, List<Pair<String, String>> options1, List<Pair<String, String>> options2) {
    return new PragmaExpToJdbcTrans(new TransExpVar("t"), url, "pre_", options1, options2);
  }

  private final PragmaExpToJdbcTrans<?, ?, ?, ?, ?, ?, ?, ?> exp = exp("u", List.of(), List.of());

  @Nested
  class Evaluation {

    @Test
    void failsWhenRun() throws Exception {
      String msg = ExpTestHelpers.error(PROGRAM);
      assertTrue(msg.contains("JDBC export for transforms deprecated"), msg);
    }

    @Test
    void isNotEvaluatedAtCompileTime() throws Exception {
      AqlEnv env = ExpTestHelpers.run(PROGRAM).env;
      assertThrows(IgnoreException.class, () -> exp.eval0(env, true));
    }
  }

  @Nested
  class Expression {

    /** The options are printed with their key and value separators swapped. */
    @Test
    void printsTheTransformTheUrlThePrefixAndTheOptions() {
      assertEquals("export_jdbc_transform t  \"u\" \"pre_\"", exp.toString());
      List<Pair<String, String>> one = List.of(new Pair<>("start_ids_at", "1"));
      assertEquals(
          "export_jdbc_transform t  \"u\" \"pre_\" {\n\toptionsstart_ids_at\n\t\t1\n}{\n\toptions"
              + "start_ids_at\n\t\t1\n}",
          exp("u", one, one).toString());
    }

    @Test
    void dependsOnTheTransform() throws Exception {
      assertEquals(List.of(new Pair<>("t", Kind.TRANSFORM)), new ArrayList<>(exp.deps()));
      assertEquals(Unit.unit, exp.type(ExpTestHelpers.typing(PROGRAM)));
      assertEquals(
          Map.of("start_ids_at", "1"),
          exp("u", List.of(new Pair<>("start_ids_at", "1")), List.of()).options());
      assertTrue(exp.allowedOptions().contains(AqlOption.start_ids_at));
      List<Exp<?>> seen = new ArrayList<>();
      exp.map(seen::add);
      assertEquals(List.of(exp, new TransExpVar("t")), seen);
    }

    @Test
    void equalityComparesEveryPart() {
      List<Pair<String, String>> one = List.of(new Pair<>("start_ids_at", "1"));
      assertEquals(exp, exp);
      assertEquals(exp, exp("u", List.of(), List.of()));
      assertEquals(exp.hashCode(), exp("u", List.of(), List.of()).hashCode());
      assertNotEquals(exp, exp("v", List.of(), List.of()));
      assertNotEquals(exp, exp("u", one, List.of()));
      assertNotEquals(exp, exp("u", List.of(), one));
      assertNotEquals(exp, null);
      assertNotEquals(exp, "export_jdbc_transform");
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsAVisitor() {
      PragmaExpVisitor<String, Unit, RuntimeException> v = mock(PragmaExpVisitor.class);
      when(v.visit(Unit.unit, exp)).thenReturn("visited");
      assertEquals("visited", exp.accept(Unit.unit, v));
    }
  }
}
