package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.Pair;
import catdata.Unit;
import catdata.cql.Instance;
import catdata.cql.Kind;
import catdata.cql.Query;
import catdata.cql.exp.QueryExp.QueryExpVar;
import catdata.cql.exp.QueryExp.QueryExpVisitor;
import catdata.cql.exp.SchExp.SchExpVar;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link QueryExpCompose}, {@code [Q1 ; Q2]}, the query that evaluates like Q1
 * followed by Q2, built by substituting the blocks of Q1 into those of Q2.
 */
class QueryExpComposeTest {

  private static final String BASE =
      """
      options allow_aggregation_unsafe = true
      typeside Ty = literal {
        types String Int
        constants x y : String zero : Int
        functions h : String -> String
      }
      schema S = literal : Ty { entities A attributes n : A -> String }
      schema T = literal : Ty { entities B }
      query Id = identity S
      instance I = literal : S { generators a : A equations a.n = y }
      """;

  private static Query<String, String, Sym, Fk, Att, String, Fk, Att> query(String program)
      throws Exception {
    return ExpTestHelpers.eval(BASE + program, "C", Kind.QUERY);
  }

  /** The text of the only attribute of the only block of the composed query. */
  private static String attribute(String program) throws Exception {
    return query(program).atts.get(Att.Att("A", "n")).l.toString();
  }

  private static QueryExpCompose exp(String q1, String q2, List<Pair<String, String>> options) {
    return new QueryExpCompose(new QueryExpVar(q1), new QueryExpVar(q2), options);
  }

  private final QueryExpCompose exp = exp("Q1", "Q2", List.of());

  @Nested
  class Evaluation {

    @Test
    void keepsAConstantOfTheSecondQuery() throws Exception {
      String program =
          """
          query Q2 = literal : S -> S { entity A -> { from a:A attributes n -> x } }
          query C = [Id ; Q2]
          instance K = eval C I
          """;
      assertEquals("x", attribute(program));
      Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?> k =
          ExpTestHelpers.eval(BASE + program, "K", Kind.INSTANCE);
      assertEquals(1, k.algebra().size("A"));
      assertTrue(k.toString().contains(".n = x"), k.toString());
    }

    @Test
    void substitutesTheFirstQueryIntoTheSecond() throws Exception {
      String program =
          """
          query Q1 = literal : S -> S {
            entity A -> { from a:A s:String where a.n = s attributes n -> h(s) }
          }
          query C = [Q1 ; Id]
          """;
      assertTrue(attribute(program).matches("h\\(vv\\d+\\.n\\)"), attribute(program));
    }

    @Test
    void cannotComposeWithAnAggregateInTheSecondQuery() throws Exception {
      String program =
          """
          query Q2 = literal : S -> S {
            entity A -> { from a:A attributes n -> from b:A return b.n aggregate x lambda u v. u }
          }
          query C = [Id ; Q2]
          """;
      assertTrue(ExpTestHelpers.error(BASE + program).contains("Cannot compose with aggregation"));
    }

    @Test
    void cannotComposeWithAnAggregateInTheFirstQuery() throws Exception {
      String program =
          """
          query Q1 = literal : S -> S {
            entity A -> { from a:A attributes n -> from b:A return b.n aggregate x lambda u v. u }
          }
          query C = [Q1 ; Id]
          """;
      assertTrue(ExpTestHelpers.error(BASE + program).contains("Cannot compose with aggregation"));
    }

    @Test
    void rejectsAParameterOfTwoTypes() throws Exception {
      String program =
          """
          query Q1 = literal : S -> S { params p : String entity A -> { from a:A attributes n -> a.n } }
          query Q2 = literal : S -> S { params p : Int entity A -> { from a:A attributes n -> a.n } }
          query C = [Q1 ; Q2]
          """;
      String msg = ExpTestHelpers.error(BASE + program);
      assertTrue(msg.contains("Incompatible parameters: [{p=String}] and [{p=Int}]"), msg);
    }

    /**
     * The composed query has the parameter but not its binding, so it cannot be evaluated, and its
     * attribute reads a fresh variable instead of the parameter.
     */
    @Test
    void losesTheBindingAndTheValueOfAParameter() throws Exception {
      String program =
          """
          query Q1 = literal : S -> S {
            params p : String bindings p = x entity A -> { from a:A attributes n -> p }
          }
          query Q2 = literal : S -> S { entity A -> { from a:A attributes n -> a.n } }
          query C = [Q1 ; Q2]
          """;
      Query<String, String, Sym, Fk, Att, String, Fk, Att> c = query(program);
      assertEquals(Map.of("p", "String"), c.params);
      assertTrue(c.consts.isEmpty());
      assertTrue(attribute(program).matches("vv\\d+"), attribute(program));
      String msg = ExpTestHelpers.error(BASE + program + "instance K = eval C I\n");
      assertTrue(msg.contains("Missing bindings: p"), msg);
    }

    @Test
    void rejectsQueriesThatDoNotMeet() {
      String program = BASE + "query QT = identity T\nquery C = [Id ; QT]\n";
      RuntimeException e =
          assertThrows(RuntimeException.class, () -> ExpTestHelpers.typing(program));
      assertTrue(
          e.getMessage()
              .contains(
                  "Cannot compose: target of first, S is not the same as source of second, T"),
          e.getMessage());
    }

    @Test
    void isTypedFromTheFirstSourceToTheSecondTarget() throws Exception {
      String program = BASE + "query C = [Id ; Id]\n";
      QueryExpCompose e = ExpTestHelpers.exp(program, "C");
      assertEquals(
          new Pair<>(new SchExpVar("S"), new SchExpVar("S")),
          e.type(ExpTestHelpers.typing(program)));
      AqlEnv env = ExpTestHelpers.run(program).env;
      assertThrows(IgnoreException.class, () -> e.eval0(env, true));
    }
  }

  @Nested
  class Expression {

    @Test
    void printsBothQueries() {
      assertEquals("[Q1 ; Q2]", exp.toString());
    }

    @Test
    void dependsOnBothQueries() {
      assertEquals(
          Set.of(new Pair<>("Q1", Kind.QUERY), new Pair<>("Q2", Kind.QUERY)),
          Set.copyOf(exp.deps()));
      assertEquals(
          Map.of("timeout", "5"), exp("Q1", "Q2", List.of(new Pair<>("timeout", "5"))).options());
      List<Exp<?>> seen = new ArrayList<>();
      exp.map(seen::add);
      assertEquals(List.of(exp, new QueryExpVar("Q1"), new QueryExpVar("Q2")), seen);
    }

    @Test
    void equalityComparesBothQueriesAndTheOptions() {
      assertEquals(exp, exp);
      assertEquals(exp, exp("Q1", "Q2", List.of()));
      assertEquals(exp.hashCode(), exp("Q1", "Q2", List.of()).hashCode());
      assertNotEquals(exp, exp("Q3", "Q2", List.of()));
      assertNotEquals(exp, exp("Q1", "Q3", List.of()));
      assertNotEquals(exp, exp("Q1", "Q2", List.of(new Pair<>("timeout", "5"))));
      assertNotEquals(exp, null);
      assertNotEquals(exp, "[Q1 ; Q2]");
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsAVisitor() {
      QueryExpVisitor<String, Unit, RuntimeException> v = mock(QueryExpVisitor.class);
      when(v.visit(Unit.unit, exp)).thenReturn("visited");
      assertEquals("visited", exp.accept(Unit.unit, v));
    }
  }
}
