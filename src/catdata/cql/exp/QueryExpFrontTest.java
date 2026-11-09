package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.Pair;
import catdata.Unit;
import catdata.cql.Kind;
import catdata.cql.Query;
import catdata.cql.Term;
import catdata.cql.exp.EdsExp.EdsExpVar;
import catdata.cql.exp.QueryExp.QueryExpVisitor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link QueryExpFront}, {@code front C i} and {@code back C i}, the query with one
 * block, {@code Front}, that selects the rows matching the premise of constraint i of C, or its
 * premise and conclusion together, and returns their attributes.
 */
class QueryExpFrontTest {

  private static final String BASE =
      """
      typeside Ty = literal { types String constants x : String }
      schema S = literal : Ty { entities A B attributes n m : A -> String k : B -> String }
      schema F = literal : Ty { entities A B foreign_keys f : A -> B }
      constraints C = literal : S { forall a:A -> exists b:B where b.k = a.n }
      """;

  private static Query<String, String, Sym, Fk, Att, String, Fk, Att> query(String q)
      throws Exception {
    return ExpTestHelpers.eval(BASE + "query Q = " + q + "\n", "Q", Kind.QUERY);
  }

  private static QueryExpFront exp(String c, String i, boolean isBack) {
    return new QueryExpFront(new EdsExpVar(c), i, isBack, List.of());
  }

  private final QueryExpFront exp = exp("C", "0", false);

  @Nested
  class Evaluation {

    @Test
    void selectsTheRowsOfThePremise() throws Exception {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> q = query("front C 0");
      assertEquals(Set.of("Front"), q.ens.keySet());
      assertEquals(Map.of("a", "A"), q.ens.get("Front").gens);
      assertTrue(q.ens.get("Front").eqs.isEmpty());
      assertEquals(
          Set.of("a_n", "a_m"), Set.copyOf(q.dst.atts.keySet().stream().map(a -> a.str).toList()));
    }

    @Test
    void addsTheConclusionAtTheBack() throws Exception {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> q = query("back C 0");
      assertEquals(Map.of("a", "A", "b", "B"), q.ens.get("Front").gens);
      assertEquals(1, q.ens.get("Front").eqs.size());
    }

    /** A constant of the typeside in a where clause reaches the anomaly at the end of conv. */
    @Test
    void cannotTranslateAConstantOfTheTypeside() throws Exception {
      String program =
          BASE.replace(
                  "forall a:A -> exists b:B where b.k = a.n",
                  "forall a:A where a.n = x -> where a.m = a.n")
              + "query Q = front C 0\n";
      assertTrue(ExpTestHelpers.error(program).contains("Anomaly: please report"));
    }

    @Test
    void rejectsAnIndexPastTheLastConstraint() throws Exception {
      String msg = ExpTestHelpers.error(BASE + "query Q = front C 1\n");
      assertTrue(msg.contains("Expected a number between zero and 0"), msg);
    }

    @Test
    void rejectsASchemaWithForeignKeys() throws Exception {
      String program =
          BASE
              + "constraints CF = literal : F { forall a:A -> where a = a }\nquery Q = front CF 0\n";
      assertTrue(
          ExpTestHelpers.error(program).contains("Front/Back cannot be used with foreign keys."));
    }

    @Test
    void reservesTheEntityOption() {
      RuntimeException e =
          assertThrows(
              RuntimeException.class,
              () ->
                  new QueryExpFront(
                      new EdsExpVar("C"),
                      "0",
                      false,
                      List.of(new Pair<>("simple_query_entity", "E"))));
      assertTrue(e.getMessage().startsWith("option reserved on front keyword: "), e.getMessage());
    }
  }

  @Nested
  class Terms {

    @Test
    void translatesVariablesAttributesAndValues() {
      Term<String, String, Sym, Fk, Att, Void, Void> v = Term.Var("v");
      assertEquals("v", QueryExpFront.conv(v).toString());
      assertEquals("v.n", QueryExpFront.conv(Term.Att(Att.Att("A", "n"), v)).toString());
      assertEquals("\"1\"@T", QueryExpFront.conv(Term.Obj(1, "T")).toString());
      assertEquals("\"2\"@T", QueryExpFront.conv(Term.Obj(Optional.of(2), "T")).toString());
      assertEquals("null@T", QueryExpFront.conv(Term.Obj(Optional.empty(), "T")).toString());
    }

    @Test
    void cannotTranslateAForeignKey() {
      Term<String, String, Sym, Fk, Att, Void, Void> t = Term.Fk(Fk.Fk("A", "f"), Term.Var("v"));
      assertThrows(RuntimeException.class, () -> QueryExpFront.conv(t));
    }
  }

  @Nested
  class Expression {

    /** The index is printed before the constraints, the reverse of the order that parses. */
    @Test
    void printsTheIndexBeforeTheConstraints() {
      assertEquals("front 0 C", exp.toString());
      assertEquals("back 0 C", exp("C", "0", true).toString());
    }

    @Test
    void reservesTheEntityName() throws Exception {
      assertEquals(Map.of("simple_query_entity", "Front"), exp.options());
      assertEquals(List.of(new Pair<>("C", Kind.CONSTRAINTS)), new ArrayList<>(exp.deps()));
      List<Exp<?>> seen = new ArrayList<>();
      exp.map(seen::add);
      assertEquals(exp, seen.get(0));
    }

    @Test
    void equalityComparesEveryPart() {
      assertEquals(exp, exp);
      assertEquals(exp, exp("C", "0", false));
      assertEquals(exp.hashCode(), exp("C", "0", false).hashCode());
      assertNotEquals(exp, exp("D", "0", false));
      assertNotEquals(exp, exp("C", "1", false));
      assertNotEquals(exp, exp("C", "0", true));
      assertNotEquals(
          exp,
          new QueryExpFront(new EdsExpVar("C"), "0", false, List.of(new Pair<>("timeout", "5"))));
      assertNotEquals(exp, null);
      assertNotEquals(exp, "front");
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
