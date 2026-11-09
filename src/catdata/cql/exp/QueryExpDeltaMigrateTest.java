package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.LineException;
import catdata.Pair;
import catdata.Unit;
import catdata.cql.AqlOptions;
import catdata.cql.Instance;
import catdata.cql.Kind;
import catdata.cql.Mapping;
import catdata.cql.Query;
import catdata.cql.exp.MapExp.MapExpVar;
import catdata.cql.exp.QueryExp.QueryExpVar;
import catdata.cql.exp.QueryExp.QueryExpVisitor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link QueryExpDeltaMigrate}, {@code delta F Q}, the query that first migrates
 * along {@code delta F}, then runs {@code Q}.
 */
class QueryExpDeltaMigrateTest {

  private static final String PROGRAM =
      """
      typeside Ty = literal { types String constants x y : String }
      schema S = literal : Ty { entities A attributes n : A -> String }
      schema S2 = literal : Ty { entities A1 A2 attributes n1 : A1 -> String n2 : A2 -> String }
      mapping F = literal : S2 -> S {
        entity A1 -> A attributes n1 -> n
        entity A2 -> A attributes n2 -> n
      }
      schema T = literal : Ty { entities B attributes m : B -> String }
      query Q = literal : S -> T { entity B -> { from a:A attributes m -> a.n } }
      query DQ = delta F Q
      """;

  private static Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?> eval(String q2)
      throws Exception {
    return ExpTestHelpers.eval(
        PROGRAM
            + "instance J = literal : S2 { generators p : A1 q : A2 equations p.n1 = x q.n2 = "
            + q2
            + " }\ninstance K = eval DQ J\n",
        "K",
        Kind.INSTANCE);
  }

  @Nested
  class Evaluation {

    @Test
    void pullsEachBlockBackAlongTheMapping() throws Exception {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> dq =
          ExpTestHelpers.eval(PROGRAM, "DQ", Kind.QUERY);
      var block = dq.ens.get("B");
      assertEquals(Set.of("A1", "A2"), Set.copyOf(block.gens.values()));
      assertEquals(1, block.eqs.size());
    }

    @Test
    void joinsTheCopiesOfAnEntityOnTheirAttributes() throws Exception {
      assertEquals(1, eval("x").algebra().size("B"));
      assertEquals(0, eval("y").algebra().size("B"));
    }

    @Test
    void doesNotSupportTargetForeignKeys() throws Exception {
      String program =
          PROGRAM
              .replace(
                  "schema T = literal : Ty { entities B attributes m : B -> String }",
                  "schema T = literal : Ty { entities B C foreign_keys g : B -> C attributes m : B -> String }")
              .replace(
                  "{ entity B -> { from a:A attributes m -> a.n } }",
                  "{ entity C -> { from c:A } entity B -> { from a:A attributes m -> a.n foreign_keys g -> { c -> a } } }");
      assertTrue(ExpTestHelpers.error(program).contains("todo"));
    }

    @Test
    void rejectsAMappingIntoAnotherSchema() {
      String program =
          PROGRAM.replace(
              "query DQ = delta F Q", "query Q2 = literal : T -> T { }\nquery DQ = delta F Q2");
      LineException e = assertThrows(LineException.class, () -> ExpTestHelpers.run(program));
      assertTrue(
          e.getMessage()
              .startsWith("Target of mapping:\n\nS\n\nnot equal to source of query:\n\nT"),
          e.getMessage());
    }

    @Test
    void turnsAMappingIntoAQuery() throws Exception {
      Mapping<String, String, Sym, Fk, Att, String, Fk, Att> f =
          ExpTestHelpers.eval(PROGRAM, "F", Kind.MAPPING);
      Query<String, String, Sym, Fk, Att, String, Fk, Att> q =
          QueryExpDeltaMigrate.extracted(new AqlOptions(Map.of(), AqlOptions.initialOptions), f);
      assertEquals(Set.of("A1", "A2"), q.dst.ens);
      assertEquals(Set.of("A"), Set.copyOf(q.ens.get("A1").gens.values()));
    }
  }

  @Nested
  class Expression {

    private final QueryExpDeltaMigrate exp =
        new QueryExpDeltaMigrate(new MapExpVar("F"), new QueryExpVar("Q"));

    @Test
    void isTypedFromTheSourceOfTheMappingToTheTargetOfTheQuery() throws Exception {
      AqlTyping g = ExpTestHelpers.typing(PROGRAM);
      assertEquals(new Pair<>(new SchExp.SchExpVar("S2"), new SchExp.SchExpVar("T")), exp.type(g));
    }

    @Test
    void printsAsItsKeyword() {
      assertEquals("delta F Q", exp.toString());
    }

    @Test
    void dependsOnTheMappingAndTheQuery() {
      assertEquals(
          Set.of(new Pair<>("F", Kind.MAPPING), new Pair<>("Q", Kind.QUERY)),
          Set.copyOf(exp.deps()));
      assertTrue(exp.options().isEmpty());
      assertEquals(ExpTestHelpers.BASE_OPTIONS, exp.allowedOptions());
      List<Exp<?>> seen = new ArrayList<>();
      exp.map(seen::add);
      assertEquals(List.of(exp), seen);
    }

    /** equals tests for QueryExpDeltaEval, so two equal migrations are never equal. */
    @Test
    void isEqualOnlyToItself() {
      assertEquals(exp, exp);
      assertNotEquals(exp, new QueryExpDeltaMigrate(new MapExpVar("F"), new QueryExpVar("Q")));
      assertEquals(
          exp.hashCode(),
          new QueryExpDeltaMigrate(new MapExpVar("F"), new QueryExpVar("Q")).hashCode());
      assertNotEquals(exp, null);
      assertThrows(
          ClassCastException.class,
          () -> exp.equals(new QueryExpDeltaEval(new MapExpVar("F"), List.of())));
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
