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
import catdata.cql.Instance;
import catdata.cql.Kind;
import catdata.cql.Query;
import catdata.cql.exp.MapExp.MapExpVar;
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
 * Unit tests for {@link QueryExpFromCoSpan}, {@code fromCoSpan F G}, the query from the source of F
 * to the source of G that composes {@code toCoQuery F} with {@code toQuery G}, for two mappings
 * with the same target.
 */
class QueryExpFromCoSpanTest {

  /** With G the identity of D, the query coevaluates to the delta of F. */
  private static final String PROGRAM =
      """
      typeside Ty = literal { types String constants x : String }
      schema C = literal : Ty {
        entities A B
        foreign_keys f : A -> B
        attributes n : A -> String
      }
      schema D = literal : Ty {
        entities X Y
        foreign_keys g : X -> Y
        attributes p : X -> String
      }
      mapping F = literal : C -> D {
        entity A -> X
        foreign_keys f -> g
        attributes n -> p
        entity B -> Y
      }
      query Q = fromCoSpan F (identity D)
      instance J = literal : D { generators a : X equations a.p = x }
      instance K = coeval Q J
      instance L = delta F J
      """;

  private static QueryExpFromCoSpan exp(String f, String g, List<Pair<String, String>> options) {
    return new QueryExpFromCoSpan(new MapExpVar(f), new MapExpVar(g), options);
  }

  private final QueryExpFromCoSpan exp = exp("F", "G", List.of());

  @Nested
  class Evaluation {

    @Test
    void goesFromTheSourceOfTheFirstToTheSourceOfTheSecond() throws Exception {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> q =
          ExpTestHelpers.eval(PROGRAM, "Q", Kind.QUERY);
      assertEquals(Set.of("A", "B"), q.src.ens);
      assertEquals(Set.of("X", "Y"), q.dst.ens);
      AqlTyping g = ExpTestHelpers.typing(PROGRAM);
      QueryExpFromCoSpan e = ExpTestHelpers.exp(PROGRAM, "Q");
      assertEquals(new Pair<>(new SchExpVar("C"), new SchExpVar("D")), e.type(g));
    }

    @Test
    void coevaluatesToTheDeltaOfTheFirstWhenTheSecondIsTheIdentity() throws Exception {
      Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?> k =
          ExpTestHelpers.eval(PROGRAM, "K", Kind.INSTANCE);
      Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?> l =
          ExpTestHelpers.eval(PROGRAM, "L", Kind.INSTANCE);
      assertEquals(l.algebra().size("A"), k.algebra().size("A"));
      assertEquals(l.algebra().size("B"), k.algebra().size("B"));
      assertEquals(1, k.algebra().size("A"));
    }

    @Test
    void rejectsMappingsWithDifferentTargets() throws Exception {
      String program = PROGRAM.replace("fromCoSpan F (identity D)", "fromCoSpan F (identity C)");
      RuntimeException e =
          assertThrows(RuntimeException.class, () -> ExpTestHelpers.typing(program));
      assertTrue(
          e.getMessage()
              .contains(
                  "Cannot co-span: target of first, D is not the same as target of second, C"),
          e.getMessage());
    }
  }

  @Nested
  class Expression {

    @Test
    void printsBothMappings() {
      assertEquals("fromCoSpan F G", exp.toString());
    }

    @Test
    void dependsOnBothMappings() {
      assertEquals(
          Set.of(new Pair<>("F", Kind.MAPPING), new Pair<>("G", Kind.MAPPING)),
          Set.copyOf(exp.deps()));
      assertEquals(
          Map.of("timeout", "5"), exp("F", "G", List.of(new Pair<>("timeout", "5"))).options());
      assertTrue(exp.allowedOptions().contains(AqlOption.query_remove_redundancy));
      assertTrue(exp.allowedOptions().contains(AqlOption.dont_validate_unsafe));
      List<Exp<?>> seen = new ArrayList<>();
      exp.map(seen::add);
      assertEquals(List.of(exp, new MapExpVar("F"), new MapExpVar("G")), seen);
    }

    /**
     * equals accepts only a QueryExpCompose and then casts it to QueryExpFromCoSpan, so an
     * expression equals only itself, and comparing it with a composition throws.
     */
    @Test
    void equalsOnlyItself() {
      assertEquals(exp, exp);
      assertNotEquals(exp, exp("F", "G", List.of()));
      assertEquals(exp.hashCode(), exp("F", "G", List.of()).hashCode());
      assertNotEquals(exp, null);
      QueryExpCompose compose =
          new QueryExpCompose(new QueryExpVar("P"), new QueryExpVar("R"), List.of());
      assertThrows(ClassCastException.class, () -> exp.equals(compose));
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
