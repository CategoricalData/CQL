package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.Chc;
import catdata.Pair;
import catdata.Unit;
import catdata.cql.AqlOptions;
import catdata.cql.Instance;
import catdata.cql.Kind;
import catdata.cql.Query;
import catdata.cql.Term;
import catdata.cql.exp.MapExp.MapExpVar;
import catdata.cql.exp.QueryExp.QueryExpVisitor;
import catdata.cql.exp.SchExp.SchExpVar;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link QueryExpDeltaCoEval}, {@code toCoQuery F}, the query whose coevaluation is
 * the delta of F: each target entity gets a block made from the delta of its representable
 * instance, and each target attribute is the shortest term over that block equal to it, or a fresh
 * variable when no term is.
 */
class QueryExpDeltaCoEvalTest {

  /** r has no preimage under F, so the X block reads it from a fresh variable. */
  private static final String PROGRAM =
      """
      typeside Ty = literal { types String constants x : String functions h : String -> String }
      schema C = literal : Ty {
        entities A B
        foreign_keys f : A -> B
        attributes n : A -> String m : B -> String
      }
      schema D = literal : Ty {
        entities X Y
        foreign_keys g : X -> Y
        attributes p : X -> String q : Y -> String r : X -> String
      }
      mapping F = literal : C -> D {
        entity A -> X
        foreign_keys f -> g
        attributes n -> p
        entity B -> Y
        attributes m -> q
      }
      query Q = toCoQuery F
      instance J = literal : D { generators a : X equations a.p = x a.g.q = h(x) }
      instance K = coeval Q J
      instance L = delta F J
      """;

  /** The same program with n mapped to h(p), so p itself has no term over the X block. */
  private static final String FUNCTION =
      PROGRAM.replace("attributes n -> p", "attributes n -> lambda z. h(z.p)");

  private static Query<String, String, Sym, Fk, Att, String, Fk, Att> query(String program)
      throws Exception {
    return ExpTestHelpers.eval(program, "Q", Kind.QUERY);
  }

  private static Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?> instance(
      String program, String name) throws Exception {
    return ExpTestHelpers.eval(program, name, Kind.INSTANCE);
  }

  /** The value of attribute n on the only row of A. */
  @SuppressWarnings("unchecked")
  private static String n(Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?> i) {
    Instance<String, String, Sym, Fk, Att, ?, ?, Object, ?> j =
        (Instance<String, String, Sym, Fk, Att, ?, ?, Object, ?>) i;
    Object row = j.algebra().en("A").iterator().next();
    return j.algebra().att(Att.Att("A", "n"), row).toString();
  }

  @Nested
  class Evaluation {

    @Test
    void makesOneBlockPerTargetEntity() throws Exception {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> q = query(PROGRAM);
      assertEquals(Set.of("X", "Y"), q.ens.keySet());
      assertEquals(Map.of("gen1000", "A"), q.ens.get("X").gens);
      assertEquals(Map.of("gen1000", "B"), q.ens.get("Y").gens);
      assertTrue(q.ens.get("Y").sks.isEmpty());
      assertEquals(1, q.ens.get("X").sks.size());
      assertTrue(q.ens.get("X").sks.containsValue("String"));
    }

    @Test
    void readsEachAttributeFromItsPreimageOrAFreshVariable() throws Exception {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> q = query(PROGRAM);
      Term<String, String, Sym, Fk, Att, String, String> gen = Term.Gen("gen1000");
      assertEquals(Chc.inLeft(Term.Att(Att.Att("A", "n"), gen)), q.atts.get(Att.Att("X", "p")));
      assertEquals(Chc.inLeft(Term.Att(Att.Att("B", "m"), gen)), q.atts.get(Att.Att("Y", "q")));
      String sk = q.ens.get("X").sks.keySet().iterator().next();
      assertEquals(Chc.inLeft(Term.Sk(sk)), q.atts.get(Att.Att("X", "r")));
    }

    @Test
    void mapsEachForeignKeyThroughItsPreimage() throws Exception {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> q = query(PROGRAM);
      assertEquals(
          Term.Fk(Fk.Fk("A", "f"), Term.Gen("gen1000")),
          q.fks.get(Fk.Fk("X", "g")).gens().apply("gen1000", "B"));
    }

    /** No term over the X block equals p, so p is a fresh variable that the where clause binds. */
    @Test
    void constrainsAFreshVariableByTheEquationsOfTheDelta() throws Exception {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> q = query(FUNCTION);
      assertEquals(2, q.ens.get("X").sks.size());
      assertEquals(1, q.ens.get("X").eqs.size());
      String eq = q.ens.get("X").eqs.iterator().next().toString();
      assertTrue(eq.matches("\\(gen1000\\.n, h\\(sk\\d\\)\\)"), eq);
    }

    @Test
    void coevaluatesToTheDeltaOfTheMapping() throws Exception {
      for (String program : List.of(PROGRAM, FUNCTION)) {
        Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?> k = instance(program, "K");
        Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?> l = instance(program, "L");
        assertEquals(l.algebra().size("A"), k.algebra().size("A"));
        assertEquals(l.algebra().size("B"), k.algebra().size("B"));
        assertEquals(n(l), n(k));
      }
      assertEquals("x", n(instance(PROGRAM, "K")));
      assertEquals("h(x)", n(instance(FUNCTION, "K")));
    }

    /** The option is parsed as a boolean but read as an integer, so no value is accepted. */
    @Test
    void rejectsEveryValueOfTheMaximumTermSize() throws Exception {
      String option = "query Q = toCoQuery F { options toCoQuery_max_term_size = ";
      String integer =
          ExpTestHelpers.error(PROGRAM.replace("query Q = toCoQuery F", option + "1 }"));
      assertTrue(integer.contains("neither true nor false: 1"), integer);
      String bool =
          ExpTestHelpers.error(PROGRAM.replace("query Q = toCoQuery F", option + "true }"));
      assertTrue(
          bool.contains("java.lang.Boolean cannot be cast to class java.lang.Integer"), bool);
    }

    @Test
    void isNotEvaluatedAtCompileTime() throws Exception {
      AqlEnv env = ExpTestHelpers.run(PROGRAM).env;
      QueryExpDeltaCoEval e = ExpTestHelpers.exp(PROGRAM, "Q");
      assertThrows(IgnoreException.class, () -> e.eval0(env, true));
    }
  }

  @Nested
  class Expression {

    private QueryExpDeltaCoEval exp(String mapping, List<Pair<String, String>> options) {
      return new QueryExpDeltaCoEval(new MapExpVar(mapping), options);
    }

    private final QueryExpDeltaCoEval exp = exp("F", List.of());

    @Test
    void printsAsItsKeyword() {
      assertEquals("toCoQuery F", exp.toString());
    }

    @Test
    void dependsOnItsMapping() throws Exception {
      assertEquals(List.of(new Pair<>("F", Kind.MAPPING)), new ArrayList<>(exp.deps()));
      assertEquals(Map.of("timeout", "5"), exp("F", List.of(new Pair<>("timeout", "5"))).options());
      assertTrue(exp.allowedOptions().contains(AqlOptions.AqlOption.toCoQuery_max_term_size));
      assertTrue(exp.allowedOptions().containsAll(AqlOptions.proverOptionNames()));
      List<Exp<?>> seen = new ArrayList<>();
      exp.map(seen::add);
      assertEquals(List.of(exp, new MapExpVar("F")), seen);
      AqlTyping g = ExpTestHelpers.typing(PROGRAM);
      assertEquals(new Pair<>(new SchExpVar("C"), new SchExpVar("D")), exp.type(g));
    }

    @Test
    void equalityComparesTheMappingAndTheOptions() {
      assertEquals(exp, exp);
      assertEquals(exp, exp("F", List.of()));
      assertEquals(exp.hashCode(), exp("F", List.of()).hashCode());
      assertNotEquals(exp, exp("G", List.of()));
      assertNotEquals(exp, exp("F", List.of(new Pair<>("timeout", "5"))));
      QueryExpDeltaCoEval none = new QueryExpDeltaCoEval(null, List.of());
      assertEquals(none, new QueryExpDeltaCoEval(null, List.of()));
      assertEquals(none.hashCode(), new QueryExpDeltaCoEval(null, List.of()).hashCode());
      assertNotEquals(none, exp);
      assertNotEquals(exp, none);
      assertNotEquals(exp, null);
      assertNotEquals(exp, "toCoQuery F");
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
