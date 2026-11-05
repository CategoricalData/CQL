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
 * Unit tests for {@link QueryExpRext}, {@code rext Q1 Q2}, the right Kan extension of Q2 along Q1
 * for two queries with the same source: a block for each entity of the target of Q2, made by
 * evaluating Q1 on the block of Q2.
 */
class QueryExpRextTest {

  private static final String PROGRAM =
      """
      typeside Ty = literal { types String constants x : String }
      schema S = literal : Ty {
        entities A B
        foreign_keys f : A -> B
        attributes n : A -> String
      }
      schema T = literal : Ty { entities C }
      query Id = identity S
      query R = rext Id Id
      instance I = literal : S { generators a : A b : B equations a.f = b a.n = x }
      instance J = eval R I
      """;

  private static QueryExpRext exp(String q1, String q2, List<Pair<String, String>> options) {
    return new QueryExpRext(new QueryExpVar(q1), new QueryExpVar(q2), options);
  }

  private final QueryExpRext exp = exp("Q1", "Q2", List.of());

  @Nested
  class Evaluation {

    /** Variables are named after a counter shared by every rext, so they are matched by pattern. */
    @Test
    void extendsTheIdentityAlongItselfToTheIdentity() throws Exception {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> r =
          ExpTestHelpers.eval(PROGRAM, "R", Kind.QUERY);
      assertEquals(Set.of("A", "B"), r.ens.keySet());
      assertTrue(r.ens.get("A").gens.containsValue("A"));
      String n = r.atts.get(Att.Att("A", "n")).l.toString();
      assertTrue(n.matches("gen\\d+\\.n"), n);
      String b = r.ens.get("B").gens.keySet().iterator().next();
      assertTrue(r.fks.get(Fk.Fk("A", "f")).gens().apply(b, "B").toString().matches("gen\\d+\\.f"));
    }

    @Test
    void evaluatesLikeTheIdentity() throws Exception {
      Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?> j =
          ExpTestHelpers.eval(PROGRAM, "J", Kind.INSTANCE);
      assertEquals(1, j.algebra().size("A"));
      assertEquals(1, j.algebra().size("B"));
      assertTrue(j.toString().contains(".n = x"), j.toString());
    }

    @Test
    void rejectsQueriesWithDifferentSources() {
      String program = PROGRAM + "query QT = identity T\nquery R2 = rext Id QT\n";
      RuntimeException e =
          assertThrows(RuntimeException.class, () -> ExpTestHelpers.typing(program));
      assertTrue(
          e.getMessage()
              .contains("Cannot Rext: source of first, S is not the same as source of second, T"),
          e.getMessage());
    }

    @Test
    void isTypedFromTheTargetOfTheFirstToTheTargetOfTheSecond() throws Exception {
      QueryExpRext e = ExpTestHelpers.exp(PROGRAM, "R");
      assertEquals(
          new Pair<>(new SchExpVar("S"), new SchExpVar("S")),
          e.type(ExpTestHelpers.typing(PROGRAM)));
      AqlEnv env = ExpTestHelpers.run(PROGRAM).env;
      assertThrows(IgnoreException.class, () -> e.eval0(env, true));
    }
  }

  @Nested
  class Expression {

    @Test
    void printsBothQueries() {
      assertEquals("rext Q1 Q2", exp.toString());
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
      assertNotEquals(exp, "rext");
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
