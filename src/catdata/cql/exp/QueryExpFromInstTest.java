package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.Pair;
import catdata.Unit;
import catdata.cql.Instance;
import catdata.cql.Kind;
import catdata.cql.Query;
import catdata.cql.exp.InstExp.InstExpVar;
import catdata.cql.exp.QueryExp.QueryExpVisitor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link QueryExpFromInst}, {@code fromInstance I}, the query into the unit schema
 * whose only block is the canonical query of I: its generators as the from clause and its equations
 * as the where clause, so that evaluating it counts the matches of I.
 */
class QueryExpFromInstTest {

  private static final String PROGRAM =
      """
      typeside Ty = literal { types String constants x y : String }
      schema S = literal : Ty { entities A attributes n : A -> String }
      instance I = literal : S { generators a : A s : String equations a.n = x a.n = s }
      query Q = fromInstance I
      instance J = literal : S { generators p q r : A equations p.n = x q.n = x r.n = y }
      instance K = eval Q J
      """;

  private final QueryExpFromInst exp = new QueryExpFromInst(new InstExpVar("I"));

  @Test
  void makesTheCanonicalQueryOfTheInstance() throws Exception {
    Query<String, String, Sym, Fk, Att, String, Fk, Att> q =
        ExpTestHelpers.eval(PROGRAM, "Q", Kind.QUERY);
    assertEquals(Set.of(""), q.dst.ens);
    var block = q.ens.get("");
    assertEquals(Map.of("a", "A"), block.gens);
    assertEquals(Map.of("s", "String"), block.sks);
    assertEquals(2, block.eqs.size());
  }

  /** s is bound to x by the equations, so each of the two rows with n = x is one match. */
  @Test
  void countsTheMatchesOfTheInstance() throws Exception {
    Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?> k =
        ExpTestHelpers.eval(PROGRAM, "K", Kind.INSTANCE);
    assertEquals(2, k.algebra().size(""));
  }

  @Test
  void isTypedFromTheSchemaOfTheInstanceToTheUnitSchema() throws Exception {
    AqlTyping g = ExpTestHelpers.typing(PROGRAM);
    Pair<SchExp, SchExp> t = exp.type(g);
    assertEquals(new SchExp.SchExpVar("S"), t.first);
    assertTrue(t.second.toString().contains("unit"), t.second.toString());
  }

  @Test
  void printsAsItsKeyword() {
    assertEquals("fromInstance I", exp.toString());
  }

  @Test
  void dependsOnTheInstance() {
    assertEquals(List.of(new Pair<>("I", Kind.INSTANCE)), new ArrayList<>(exp.deps()));
    assertTrue(exp.options().isEmpty());
    assertEquals(ExpTestHelpers.BASE_OPTIONS, exp.allowedOptions());
    List<Exp<?>> seen = new ArrayList<>();
    exp.map(seen::add);
    assertEquals(exp, seen.get(0));
  }

  @Test
  void equalityComparesTheInstance() {
    assertEquals(exp, exp);
    assertEquals(exp, new QueryExpFromInst(new InstExpVar("I")));
    assertEquals(exp.hashCode(), new QueryExpFromInst(new InstExpVar("I")).hashCode());
    assertNotEquals(exp, new QueryExpFromInst(new InstExpVar("J")));
    assertNotEquals(exp, null);
    assertNotEquals(exp, "fromInstance I");
  }

  @Test
  @SuppressWarnings("unchecked")
  void acceptsAVisitor() {
    QueryExpVisitor<String, Unit, RuntimeException> v = mock(QueryExpVisitor.class);
    when(v.visit(Unit.unit, exp)).thenReturn("visited");
    assertEquals("visited", exp.accept(Unit.unit, v));
  }
}
