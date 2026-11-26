package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.Pair;
import catdata.Unit;
import catdata.cql.Algebra;
import catdata.cql.Instance;
import catdata.cql.Kind;
import catdata.cql.Query;
import catdata.cql.exp.InstExp.InstExpVar;
import catdata.cql.exp.QueryExp.QueryExpVisitor;
import catdata.cql.exp.SchExp.SchExpVar;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link QueryExpForeignKeys}, {@code foreign_keys S { ... }}, the query that gives
 * a schema of attributes only the foreign keys its columns stand for.
 */
class QueryExpForeignKeysTest {

  /** Five tables as an import gives them: a record names its place and its habitat by code. */
  private static final String SOURCES =
      """
      schema S = literal : sql {
        entities Unit Habitat Record Observed Label
        attributes
          u_code : Unit -> String
          b_code b_name : Habitat -> String
          c_unit c_habitat c_class : Record -> String
          o_unit o_habitat o_label : Observed -> String
          l_text : Label -> String
      }
      instance I = literal : S {
        generators u1 u2 : Unit  b1 b2 : Habitat  r1 r2 r3 : Record  o1 o2 o3 : Observed
          l1 l2 : Label
        multi_equations
          u_code -> {u1 "A", u2 "B"}
          b_code -> {b1 "1", b2 "2"}
          b_name -> {b1 "oak", b2 "urban"}
          c_unit -> {r1 "A", r2 "A", r3 "B"}
          c_habitat -> {r1 "1", r2 "2", r3 "2"}
          c_class -> {r1 "x", r2 "y", r3 "z"}
          o_unit -> {o1 "A", o2 "B", o3 "C"}
          o_habitat -> {o1 "1", o2 "2", o3 "1"}
          o_label -> {o1 "bird", o2 "bat", o3 "bird"}
          l_text -> {l1 "bird", l2 "bat"}
      }
      """;

  private static final String PROGRAM =
      SOURCES
          + """
          query Q = foreign_keys S {
            rec_unit    : Record -> Unit     where c_unit = u_code
            rec_habitat : Record -> Habitat  where c_habitat = b_code
            seen_rec    : Observed -> Record where o_unit = c_unit  o_habitat = c_habitat
            seen_label  : Observed -> Label  where o_label = l_text
          }
          instance J = eval Q I
          """;

  private static QueryExpForeignKeys.Key key(String fk, String from, String to, String... on) {
    List<Pair<String, String>> l = new ArrayList<>();
    for (int i = 0; i < on.length; i += 2) {
      l.add(new Pair<>(on[i], on[i + 1]));
    }
    return new QueryExpForeignKeys.Key(fk, from, to, l);
  }

  private static QueryExpForeignKeys exp(
      String sch, List<Pair<String, String>> options, QueryExpForeignKeys.Key... keys) {
    return new QueryExpForeignKeys(new SchExpVar(sch), List.of(keys), options);
  }

  private final QueryExpForeignKeys exp =
      exp("S", List.of(), key("f", "A", "B", "a", "b"), key("g", "B", "C", "b1", "c1", "b2", "c2"));

  /** Whether every row agrees, on two columns, with the row its foreign key reaches. */
  private static <X, Y> boolean agree(
      Algebra<String, String, Sym, Fk, Att, ?, ?, X, Y> alg, Fk fk, String to, String a, String b) {
    for (X x : alg.en(fk.en)) {
      if (!alg.att(Att.Att(fk.en, a), x).equals(alg.att(Att.Att(to, b), alg.fk(fk, x)))) {
        return false;
      }
    }
    return true;
  }

  /** The rows of an entity, each as the values of the given attributes, in that order. */
  private static <X, Y> Set<List<String>> rows(
      Algebra<String, String, Sym, Fk, Att, ?, ?, X, Y> alg, String en, String... atts) {
    Set<List<String>> rows = new HashSet<>();
    for (X x : alg.en(en)) {
      List<String> row = new ArrayList<>(atts.length);
      for (String att : atts) {
        row.add(alg.att(Att.Att(en, att), x).toString());
      }
      rows.add(row);
    }
    return rows;
  }

  @Nested
  class Evaluation {

    @Test
    void joinsEachRowWithTheRowsItsColumnsName() throws Exception {
      Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?> j =
          ExpTestHelpers.eval(PROGRAM, "J", Kind.INSTANCE);
      Algebra<String, String, Sym, Fk, Att, ?, ?, ?, ?> alg = j.algebra();
      assertEquals(2, alg.size("Unit"));
      assertEquals(2, alg.size("Habitat"));
      assertEquals(3, alg.size("Record"));
      assertEquals(2, alg.size("Label"));
      assertTrue(agree(alg, Fk.Fk("Record", "rec_unit"), "Unit", "c_unit", "u_code"));
      assertTrue(agree(alg, Fk.Fk("Record", "rec_habitat"), "Habitat", "c_habitat", "b_code"));
      assertTrue(agree(alg, Fk.Fk("Observed", "seen_rec"), "Record", "o_unit", "c_unit"));
      assertTrue(agree(alg, Fk.Fk("Observed", "seen_rec"), "Record", "o_habitat", "c_habitat"));
      assertTrue(agree(alg, Fk.Fk("Observed", "seen_label"), "Label", "o_label", "l_text"));
    }

    @Test
    void leavesOutARowWithNoRowToReach() throws Exception {
      Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?> j =
          ExpTestHelpers.eval(PROGRAM, "J", Kind.INSTANCE);
      // o3 names unit C, which no record has
      assertEquals(
          Set.of(List.of("A", "bird"), List.of("B", "bat")),
          rows(j.algebra(), "Observed", "o_unit", "o_label"));
    }

    @Test
    void givesTwoRowsWhenTheColumnsOnTheRightAreNotAKey() throws Exception {
      String program =
          PROGRAM
              .replace("u1 u2 : Unit", "u1 u2 u3 : Unit")
              .replace(
                  "u_code -> {u1 \"A\", u2 \"B\"}", "u_code -> {u1 \"A\", u2 \"B\", u3 \"A\"}");
      Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?> j =
          ExpTestHelpers.eval(program, "J", Kind.INSTANCE);
      // r1 and r2 name unit A, which two rows of Unit now have
      assertEquals(5, j.algebra().size("Record"));
    }

    @Test
    void joinsEveryRowAForeignKeyReachesInTurn() throws Exception {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> q =
          ExpTestHelpers.eval(PROGRAM, "Q", Kind.QUERY);
      Map<String, String> observed = q.ens.get("Observed").gens;
      assertEquals(5, observed.size());
      assertEquals(
          Set.of("Observed", "Record", "Unit", "Habitat", "Label"), Set.copyOf(observed.values()));
      Map<String, String> record = q.ens.get("Record").gens;
      assertEquals(3, record.size());
      assertEquals(1, q.ens.get("Unit").gens.size());
      for (String v : record.keySet()) {
        String w =
            q.fks.get(Fk.Fk("Observed", "seen_rec")).gens().apply(v, record.get(v)).toString();
        assertEquals(record.get(v), observed.get(w), v + " -> " + w);
      }
    }

    @Test
    void keepsEveryEntityAndAttributeAndAddsTheForeignKeys() throws Exception {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> q =
          ExpTestHelpers.eval(PROGRAM, "Q", Kind.QUERY);
      assertEquals(Set.copyOf(q.src.ens), Set.copyOf(q.dst.ens));
      assertEquals(q.src.atts, q.dst.atts);
      assertEquals(
          Map.of(
              Fk.Fk("Record", "rec_unit"), new Pair<>("Record", "Unit"),
              Fk.Fk("Record", "rec_habitat"), new Pair<>("Record", "Habitat"),
              Fk.Fk("Observed", "seen_rec"), new Pair<>("Observed", "Record"),
              Fk.Fk("Observed", "seen_label"), new Pair<>("Observed", "Label")),
          q.dst.fks);
    }

    @Test
    void followsAPathOfForeignKeysAfterADelta() throws Exception {
      String program =
          PROGRAM
              + """
              schema Small = literal : sql {
                entities Seen
                attributes name hab : Seen -> String
              }
              mapping F = literal : Small -> schemaOf J {
                entity Seen -> Observed
                attributes
                  name -> lambda x. x.seen_label.l_text
                  hab -> lambda x. x.seen_rec.rec_habitat.b_name
              }
              instance K = delta F J
              """;
      Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?> k =
          ExpTestHelpers.eval(program, "K", Kind.INSTANCE);
      assertEquals(
          Set.of(List.of("bird", "oak"), List.of("bat", "urban")),
          rows(k.algebra(), "Seen", "name", "hab"));
    }

    @Test
    void rejectsACycle() throws Exception {
      String program =
          SOURCES
              + """
              query Q = foreign_keys S {
                rec_unit : Record -> Unit where c_unit = u_code
                unit_rec : Unit -> Record where u_code = c_unit
              }
              """;
      String e = ExpTestHelpers.error(program);
      assertTrue(e.contains("form a cycle, Record -> Unit -> Record."), e);
    }

    @Test
    void rejectsAForeignKeyToItsOwnEntity() throws Exception {
      String program =
          SOURCES + "query Q = foreign_keys S { prev : Record -> Record where c_unit = c_class }\n";
      String e = ExpTestHelpers.error(program);
      assertTrue(e.contains("form a cycle, Record -> Record."), e);
    }

    @Test
    void rejectsWhatIsNotInTheSchema() throws Exception {
      String e =
          ExpTestHelpers.error(
              SOURCES + "query Q = foreign_keys S { f : Record -> Place where c_unit = u_code }\n");
      assertTrue(e.contains("in f, Place is not an entity of S."), e);
      e =
          ExpTestHelpers.error(
              SOURCES + "query Q = foreign_keys S { f : Record -> Unit where c_none = u_code }\n");
      assertTrue(e.contains("in f, c_none is not an attribute of Record."), e);
      e =
          ExpTestHelpers.error(
              SOURCES + "query Q = foreign_keys S { f : Record -> Unit where c_unit = u_none }\n");
      assertTrue(e.contains("in f, u_none is not an attribute of Unit."), e);
    }

    @Test
    void rejectsColumnsOfDifferentTypes() throws Exception {
      String program =
          """
          schema S = literal : sql {
            entities A B
            attributes a : A -> String  b : B -> Integer
          }
          query Q = foreign_keys S { f : A -> B where a = b }
          """;
      String e = ExpTestHelpers.error(program);
      assertTrue(e.contains("in f, A.a is of type String and B.b of type Integer."), e);
    }

    @Test
    void rejectsASchemaThatIsNotOfAttributesOnly() throws Exception {
      String e =
          ExpTestHelpers.error(
              """
              schema S = literal : sql {
                entities A B
                foreign_keys g : A -> B
                attributes a : A -> String  b : B -> String
              }
              query Q = foreign_keys S { f : A -> B where a = b }
              """);
      assertTrue(e.contains("S has foreign keys already (g : A -> B)."), e);
      e =
          ExpTestHelpers.error(
              """
              schema S = literal : sql {
                entities A B
                attributes a : A -> String  b : B -> String
                observation_equations forall x:A. x.a = "z"
              }
              query Q = foreign_keys S { f : A -> B where a = b }
              """);
      assertTrue(e.contains("S has equations."), e);
    }

    @Test
    void rejectsAForeignKeyDeclaredTwiceForOneEntityWhenTyped() {
      String program =
          SOURCES
              + """
              query Q = foreign_keys S {
                f : Record -> Unit where c_unit = u_code
                f : Record -> Habitat where c_habitat = b_code
              }
              """;
      RuntimeException e =
          assertThrows(RuntimeException.class, () -> ExpTestHelpers.typing(program));
      assertTrue(
          e.getMessage().contains("foreign_keys: f is declared twice for Record."), e::getMessage);
    }

    @Test
    void rejectsADeclarationWithNoColumns() {
      QueryExpForeignKeys q =
          new QueryExpForeignKeys(
              new SchExpVar("S"),
              List.of(new QueryExpForeignKeys.Key("f", "A", "B", List.of())),
              List.of());
      RuntimeException e = assertThrows(RuntimeException.class, q::cod);
      assertEquals("foreign_keys: f names no columns.", e.getMessage());
    }

    @Test
    void letsTwoEntitiesNameAForeignKeyAlike() throws Exception {
      String program =
          SOURCES
              + """
              query Q = foreign_keys S {
                to : Observed -> Record where o_unit = c_unit  o_habitat = c_habitat
                to : Record -> Habitat where c_habitat = b_code
              }
              instance J = eval Q I
              schema Small = literal : sql { entities Seen attributes hab : Seen -> String }
              mapping F = literal : Small -> schemaOf J {
                entity Seen -> Observed
                attributes hab -> lambda x. x.to.to.b_name
              }
              instance K = delta F J
              """;
      Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?> k =
          ExpTestHelpers.eval(program, "K", Kind.INSTANCE);
      assertEquals(Set.of(List.of("oak"), List.of("urban")), rows(k.algebra(), "Seen", "hab"));
    }

    @Test
    void rejectsAForeignKeyNamedLikeAnAttributeOfItsEntity() throws Exception {
      String e =
          ExpTestHelpers.error(
              SOURCES
                  + "query Q = foreign_keys S { c_unit : Record -> Unit where c_unit = u_code }\n");
      assertTrue(e.contains("foreign_keys: c_unit is an attribute of Record already."), e);
    }

    @Test
    void isTypedFromTheSchemaToTheSchemaWithTheForeignKeys() throws Exception {
      QueryExpForeignKeys q = ExpTestHelpers.exp(PROGRAM, "Q");
      AqlTyping g = ExpTestHelpers.typing(PROGRAM);
      assertEquals(new Pair<>(new SchExpVar("S"), q.cod()), q.type(g));
      assertTrue(g.eq(q.cod(), new SchExpInst<>(new InstExpVar("J"))));
    }
  }

  @Nested
  class Expression {

    @Test
    void printsAsItIsWritten() throws Exception {
      assertEquals(
          "foreign_keys S {\n\tf : A -> B where a = b\n\tg : B -> C where b1 = c1 b2 = c2\n}",
          exp.toString());
      QueryExpForeignKeys withOptions =
          exp("S", List.of(new Pair<>("timeout", "5")), key("f", "A", "B", "a", "b"));
      assertEquals(
          "foreign_keys S {\n\tf : A -> B where a = b\n\toptions\n\t\ttimeout = 5\n}",
          withOptions.toString());
      for (QueryExpForeignKeys e : List.of(exp, withOptions)) {
        assertEquals(e, ExpTestHelpers.exp("query Q = " + e, "Q"));
      }
    }

    @Test
    void dependsOnTheSchema() {
      assertEquals(Set.of(new Pair<>("S", Kind.SCHEMA)), Set.copyOf(exp.deps()));
      assertEquals(Map.of(), exp.options());
      assertEquals(
          Map.of("timeout", "5"),
          exp("S", List.of(new Pair<>("timeout", "5")), key("f", "A", "B", "a", "b")).options());
      List<Exp<?>> seen = new ArrayList<>();
      exp.map(seen::add);
      assertEquals(List.of(exp, new SchExpVar("S")), seen);
    }

    @Test
    void equalityComparesTheSchemaTheForeignKeysAndTheOptions() {
      QueryExpForeignKeys.Key f = key("f", "A", "B", "a", "b");
      QueryExpForeignKeys.Key g = key("g", "B", "C", "b1", "c1", "b2", "c2");
      assertEquals(exp, exp);
      assertEquals(exp, exp("S", List.of(), f, g));
      assertEquals(exp.hashCode(), exp("S", List.of(), f, g).hashCode());
      assertNotEquals(exp, exp("T", List.of(), f, g));
      assertNotEquals(exp, exp("S", List.of(), f));
      assertNotEquals(exp, exp("S", List.of(), g, f));
      assertNotEquals(exp, exp("S", List.of(), f, key("g", "B", "C", "b2", "c2", "b1", "c1")));
      assertNotEquals(exp, exp("S", List.of(), f, key("g", "B", "D", "b1", "c1", "b2", "c2")));
      assertNotEquals(exp, exp("S", List.of(new Pair<>("timeout", "5")), f, g));
      assertNotEquals(exp, null);
      assertNotEquals(exp, "foreign_keys");
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
