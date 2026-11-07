package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.LineException;
import catdata.Pair;
import catdata.Unit;
import catdata.cql.Algebra;
import catdata.cql.Instance;
import catdata.cql.Kind;
import catdata.cql.Query;
import catdata.cql.Transform;
import catdata.cql.exp.EdsExp.EdsExpVar;
import catdata.cql.exp.QueryExp.QueryExpVar;
import catdata.cql.exp.QueryExp.QueryExpVisitor;
import catdata.cql.exp.SchExp.SchExpVar;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link QueryExpReformulate}, {@code reformulate C Q T n}, which picks the n-th
 * smallest query, among the minimal subqueries of the universal plan {@code Q} under the
 * constraints {@code C}, that uses the fewest entities outside {@code T}. The built-in example
 * {@code Reformulate} is the fixture.
 */
class QueryExpReformulateTest {

  private static String example() throws Exception {
    try (InputStream in =
        QueryExpReformulateTest.class.getResourceAsStream("/examples/cql/Reformulate.cql")) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /** The rows of an instance, each printed as its attribute values in name order. */
  private static Set<String> rows(Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?> i) {
    Set<String> ret = new TreeSet<>();
    for (String en : i.schema().ens) {
      for (Object x : i.algebra().en(en)) {
        StringBuilder sb = new StringBuilder(en);
        Map<String, String> values = new TreeMap<>();
        for (Att a : i.schema().attsFrom(en)) {
          values.put(a.str, rowValue(i, a, x));
        }
        values.forEach((k, v) -> sb.append(' ').append(k).append('=').append(v));
        ret.add(sb.toString());
      }
    }
    return ret;
  }

  @SuppressWarnings("unchecked")
  private static String rowValue(
      Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?> i, Att a, Object x) {
    var alg = (Algebra<String, String, Sym, Fk, Att, Object, Object, Object, Object>) i.algebra();
    return alg.reprT(alg.att(a, x)).toString();
  }

  @Nested
  class Evaluation {

    @Test
    void reformulatesToAQueryWithTheSameAnswer() throws Exception {
      AqlMultiDriver d = ExpTestHelpers.run(example());
      Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?> before =
          ExpTestHelpers.value(d, "QoldResult", Kind.INSTANCE);
      Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?> after =
          ExpTestHelpers.value(d, "QnewResult", Kind.INSTANCE);
      assertEquals(rows(before), rows(after));
      Query<String, String, Sym, Fk, Att, String, Fk, Att> q =
          ExpTestHelpers.value(d, "Qnew", Kind.QUERY);
      Query<String, String, Sym, Fk, Att, String, Fk, Att> old =
          ExpTestHelpers.value(d, "Qold", Kind.QUERY);
      assertTrue(
          q.ens.values().iterator().next().gens.size()
              <= old.ens.values().iterator().next().gens.size());
    }

    @Test
    void rejectsAnIndexPastTheLastCandidate() throws Exception {
      String msg =
          ExpTestHelpers.error(example().replace("(chase C Qold) T 0", "(chase C Qold) T 99"));
      assertTrue(msg.contains("Index: 99, Size: 36"), msg);
    }

    @Test
    void rejectsAnIndexThatIsNotANumber() throws Exception {
      String msg =
          ExpTestHelpers.error(example().replace("(chase C Qold) T 0", "(chase C Qold) T x"));
      assertTrue(msg.contains("For input string: \"x\""), msg);
    }

    @Test
    void rejectsConstraintsOnAnotherSchema() throws Exception {
      String program =
          example()
              .replace(
                  "query Qnew = reformulate C",
                  "constraints C2 = all J T\nquery Qnew = reformulate C2");
      LineException e = assertThrows(LineException.class, () -> ExpTestHelpers.run(program));
      assertTrue(e.getMessage().startsWith("Source of query,\n"), e.getMessage());
      assertTrue(e.getMessage().contains("\nis not that of constraints,\n"), e.getMessage());
    }
  }

  @Nested
  class Expression {

    private final QueryExpReformulate exp =
        new QueryExpReformulate(new QueryExpVar("Q"), new EdsExpVar("C"), new SchExpVar("T"), "0");

    /** The index is not printed, so the printed form does not parse. */
    @Test
    void printsWithoutTheIndex() {
      assertEquals("reformulate C Q T", exp.toString());
    }

    @Test
    void dependsOnItsParts() {
      assertEquals(
          Set.of(
              new Pair<>("Q", Kind.QUERY),
              new Pair<>("C", Kind.CONSTRAINTS),
              new Pair<>("T", Kind.SCHEMA)),
          Set.copyOf(exp.deps()));
      assertTrue(exp.options().isEmpty());
      assertEquals(ExpTestHelpers.BASE_OPTIONS, exp.allowedOptions());
    }

    @Test
    void mapSkipsItsParts() {
      List<Exp<?>> seen = new ArrayList<>();
      exp.map(seen::add);
      assertEquals(List.of(exp), seen);
    }

    @Test
    void equalityComparesEveryPart() {
      assertEquals(exp, exp);
      assertEquals(
          exp,
          new QueryExpReformulate(
              new QueryExpVar("Q"), new EdsExpVar("C"), new SchExpVar("T"), "0"));
      assertEquals(
          exp.hashCode(),
          new QueryExpReformulate(new QueryExpVar("Q"), new EdsExpVar("C"), new SchExpVar("T"), "0")
              .hashCode());
      assertNotEquals(
          exp,
          new QueryExpReformulate(
              new QueryExpVar("R"), new EdsExpVar("C"), new SchExpVar("T"), "0"));
      assertNotEquals(
          exp,
          new QueryExpReformulate(
              new QueryExpVar("Q"), new EdsExpVar("D"), new SchExpVar("T"), "0"));
      assertNotEquals(
          exp,
          new QueryExpReformulate(
              new QueryExpVar("Q"), new EdsExpVar("C"), new SchExpVar("U"), "0"));
      assertNotEquals(
          exp,
          new QueryExpReformulate(
              new QueryExpVar("Q"), new EdsExpVar("C"), new SchExpVar("T"), "1"));
      assertNotEquals(exp, null);
      assertNotEquals(exp, "reformulate");
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsAVisitor() {
      QueryExpVisitor<String, Unit, RuntimeException> v = mock(QueryExpVisitor.class);
      when(v.visit(Unit.unit, exp)).thenReturn("visited");
      assertEquals("visited", exp.accept(Unit.unit, v));
    }
  }

  /**
   * The static searches for a homomorphism, which the containment checks of reformulation use: one
   * between two instances, and one between the single blocks of two queries.
   */
  @Nested
  class Homomorphisms {

    private static final String INSTANCES =
        """
        typeside Ty = literal { types String constants x y : String }
        schema S = literal : Ty { entities A attributes n : A -> String }
        instance One = literal : S { generators a : A }
        instance Two = literal : S { generators b c : A }
        instance Empty = empty : S
        instance X = literal : S { generators a : A equations a.n = x }
        instance Y = literal : S { generators b : A equations b.n = y }
        instance Sk1 = literal : S { generators a : A s : String equations a.n = s }
        instance Sk2 = literal : S { generators b : A t : String equations b.n = t }
        query Q1 = fromInstance Sk1
        query Q2 = fromInstance Sk2
        query QOne = fromInstance One
        query QId = identity S
        """;

    private static AqlEnv env;

    private static AqlEnv env() throws Exception {
      if (env == null) {
        env = ExpTestHelpers.run(INSTANCES).env;
      }
      return env;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Transform<
            String,
            String,
            Sym,
            Fk,
            Att,
            String,
            String,
            String,
            String,
            Object,
            Object,
            Object,
            Object>
        homI(String i1, String i2) throws Exception {
      return QueryExpReformulate.homI(
          (Instance) env().defs.insts.get(i1), (Instance) env().defs.insts.get(i2));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Transform<
            String,
            String,
            Sym,
            Fk,
            Att,
            String,
            String,
            String,
            String,
            Object,
            Object,
            Object,
            Object>
        hom(String q1, String q2) throws Exception {
      return QueryExpReformulate.hom((Query) env().defs.qs.get(q1), (Query) env().defs.qs.get(q2));
    }

    @Test
    void mapsEachGeneratorToOneOfTheSameEntity() throws Exception {
      Transform<
              String,
              String,
              Sym,
              Fk,
              Att,
              String,
              String,
              String,
              String,
              Object,
              Object,
              Object,
              Object>
          h = homI("One", "Two");
      assertTrue(Set.of("b", "c").contains(h.gens().apply("a", "A").toString()));
      assertNotNull(homI("X", "X"));
    }

    @Test
    void mapsEachLabelledNullToOneOfTheSameType() throws Exception {
      Transform<
              String,
              String,
              Sym,
              Fk,
              Att,
              String,
              String,
              String,
              String,
              Object,
              Object,
              Object,
              Object>
          h = homI("Sk1", "Sk2");
      assertEquals("b", h.gens().apply("a", "A").toString());
      assertEquals("t", h.sks().apply("s", "String").toString());
    }

    @Test
    void findsNoneWithoutAGeneratorOrNullToMapTo() throws Exception {
      assertNull(homI("One", "Empty"));
      assertNull(homI("Sk1", "One"));
    }

    @Test
    void findsNoneWhenEveryCandidateBreaksAnEquation() throws Exception {
      assertNull(homI("X", "Y"));
    }

    @Test
    void mapsTheBlockOfOneQueryIntoAnother() throws Exception {
      Transform<
              String,
              String,
              Sym,
              Fk,
              Att,
              String,
              String,
              String,
              String,
              Object,
              Object,
              Object,
              Object>
          h = hom("Q1", "Q2");
      assertEquals("b", h.gens().apply("a", "A").toString());
      assertNull(hom("Q1", "QOne"));
    }

    @Test
    void needsQueriesWithTheSameSingleEntity() {
      assertThrows(RuntimeException.class, () -> hom("Q1", "QId"));
    }
  }
}
