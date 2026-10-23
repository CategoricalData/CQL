package catdata.cql;

import static catdata.cql.CqlTestHelpers.VARCHAR;
import static catdata.cql.CqlTestHelpers.literal;
import static catdata.cql.CqlTestHelpers.ops;
import static catdata.cql.CqlTestHelpers.sqlSchema;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import catdata.Chc;
import catdata.Pair;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.Collage.CCollage;
import catdata.cql.exp.Att;
import catdata.cql.exp.Fk;
import catdata.cql.exp.Sym;
import catdata.cql.fdm.IdentityTransform;
import catdata.cql.fdm.LiteralInstance;
import catdata.cql.fdm.Row;
import catdata.provers.KBTheory;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ConstraintsTest {

  private static final String E = "E";
  private static final String D = "D";
  private static final String COLOR = "Color";

  private static final Att K = Att.Att(E, "k");
  private static final Att V = Att.Att(E, "v");
  private static final Att N = Att.Att(E, "n");
  private static final Att A = Att.Att(E, "a");
  private static final Fk F = Fk.Fk(E, "f");

  private static final Sym RED = Sym.Sym("red", new Pair<>(List.of(), COLOR));
  private static final Sym GREEN = Sym.Sym("green", new Pair<>(List.of(), COLOR));

  /** k v : E -> Varchar. */
  private static final Schema<String, String, Sym, Fk, Att> KEYED =
      sqlSchema(List.of(E), Map.of(K, E, V, E), Map.of());

  /** f : E -> D and n : E -> Varchar. */
  private static final Schema<String, String, Sym, Fk, Att> DEPTS =
      sqlSchema(List.of(D, E), Map.of(N, E), Map.of(F, new Pair<>(E, D)));

  // Terms of EDs, whose variables are the ED's.

  private static Term<String, String, Sym, Fk, Att, Void, Void> v(String x) {
    return Term.Var(x);
  }

  private static Term<String, String, Sym, Fk, Att, Void, Void> att(
      Att a, Term<String, String, Sym, Fk, Att, Void, Void> t) {
    return Term.Att(a, t);
  }

  private static Term<String, String, Sym, Fk, Att, Void, Void> fk(
      Fk f, Term<String, String, Sym, Fk, Att, Void, Void> t) {
    return Term.Fk(f, t);
  }

  private static Pair<
          Term<String, String, Sym, Fk, Att, Void, Void>,
          Term<String, String, Sym, Fk, Att, Void, Void>>
      is(
          Term<String, String, Sym, Fk, Att, Void, Void> l,
          Term<String, String, Sym, Fk, Att, Void, Void> r) {
    return new Pair<>(l, r);
  }

  private static Map<String, Chc<String, String>> rows(String en, String... vars) {
    Map<String, Chc<String, String>> m = new LinkedHashMap<>();
    for (String x : vars) {
      m.put(x, Chc.inRight(en));
    }
    return m;
  }

  // Terms of instances, whose generators are the instance's.

  private static Term<String, String, Sym, Fk, Att, String, String> gen(String g) {
    return Term.Gen(g);
  }

  private static Term<String, String, Sym, Fk, Att, String, String> iatt(Att a, String g) {
    return Term.Att(a, gen(g));
  }

  private static Term<String, String, Sym, Fk, Att, String, String> str(String s) {
    return Term.Obj(Optional.of(s), VARCHAR);
  }

  private static Pair<
          Term<String, String, Sym, Fk, Att, String, String>,
          Term<String, String, Sym, Fk, Att, String, String>>
      eq(
          Term<String, String, Sym, Fk, Att, String, String> l,
          Term<String, String, Sym, Fk, Att, String, String> r) {
    return new Pair<>(l, r);
  }

  /** forall x y : E where x.k = y.k -> where x = y. */
  private static ED key() {
    return new ED(
        rows(E, "x", "y"),
        Map.of(),
        Set.of(is(att(K, v("x")), att(K, v("y")))),
        Set.of(is(v("x"), v("y"))),
        false,
        ops());
  }

  /** forall x y : E where x.n = y.n -> where x = y, over {@link #DEPTS}. */
  private static ED nKey() {
    return new ED(
        rows(E, "x", "y"),
        Map.of(),
        Set.of(is(att(N, v("x")), att(N, v("y")))),
        Set.of(is(v("x"), v("y"))),
        false,
        ops());
  }

  /** forall x : E -> exists y : E where y.k = x.v, over {@link #KEYED}. */
  private static ED kv() {
    return new ED(
        rows(E, "x"),
        rows(E, "y"),
        Set.of(),
        Set.of(is(att(K, v("y")), att(V, v("x")))),
        false,
        ops());
  }

  /** forall d : D -> exists [unique] e : E where e.f = d. */
  private static ED everyD(boolean unique) {
    return new ED(
        rows(D, "d"), rows(E, "e"), Set.of(), Set.of(is(fk(F, v("e")), v("d"))), unique, ops());
  }

  private static Constraints of(Schema<String, String, Sym, Fk, Att> s, ED... eds) {
    return new Constraints(s, List.of(eds), ops());
  }

  /** e1.k = e2.k = a, e3.k = b, e1.v = p. */
  private static LiteralInstance<
          String, String, Sym, Fk, Att, String, String, Integer, Chc<String, Pair<Integer, Att>>>
      keyed() {
    Map<String, String> gens = new LinkedHashMap<>();
    gens.put("e1", E);
    gens.put("e2", E);
    gens.put("e3", E);
    return literal(
        KEYED,
        gens,
        List.of(
            eq(iatt(K, "e1"), str("a")),
            eq(iatt(K, "e2"), str("a")),
            eq(iatt(K, "e3"), str("b")),
            eq(iatt(V, "e1"), str("p"))));
  }

  /** e1.k = a, e2.k = b. */
  private static LiteralInstance<
          String, String, Sym, Fk, Att, String, String, Integer, Chc<String, Pair<Integer, Att>>>
      keyedOk() {
    return literal(
        KEYED,
        Map.of("e1", E, "e2", E),
        List.of(eq(iatt(K, "e1"), str("a")), eq(iatt(K, "e2"), str("b"))));
  }

  /** d1 d2 : D and e1 e2 : E with e1.f = e2.f = d1 and e1.n = x; nothing points at d2. */
  private static LiteralInstance<
          String, String, Sym, Fk, Att, String, String, Integer, Chc<String, Pair<Integer, Att>>>
      depts() {
    Map<String, String> gens = new LinkedHashMap<>();
    gens.put("d1", D);
    gens.put("d2", D);
    gens.put("e1", E);
    gens.put("e2", E);
    return literal(
        DEPTS,
        gens,
        List.of(
            eq(Term.Fk(F, gen("e1")), gen("d1")),
            eq(Term.Fk(F, gen("e2")), gen("d1")),
            eq(iatt(N, "e1"), str("x"))));
  }

  /** Color with red and green, and a : E -> Color; e1.a = red and e2.a is unknown. */
  private static LiteralInstance<
          String, String, Sym, Fk, Att, String, String, Integer, Chc<String, Pair<Integer, Att>>>
      colored() {
    Map<Sym, Pair<List<String>, String>> syms = new LinkedHashMap<>();
    syms.put(RED, RED.ty);
    syms.put(GREEN, GREEN.ty);
    TypeSide<String, Sym> ts =
        new TypeSide<>(
            new HashSet<>(Set.of(COLOR)),
            syms,
            new HashSet<>(),
            Map.of(),
            Map.of(),
            Map.of(),
            ops());
    Collage<String, String, Sym, Fk, Att, Void, Void> col = new CCollage<>();
    col.tys().addAll(ts.tys);
    col.syms().putAll(ts.syms);
    col.getEns().add(E);
    col.atts().put(A, new Pair<>(E, COLOR));
    return literal(
        new Schema<>(ts, col, ops()),
        Map.of("e1", E, "e2", E),
        List.of(eq(iatt(A, "e1"), Term.Sym(RED, List.of()))));
  }

  /** forall c : Color -> exists x : E where x.a = c. */
  private static ED everyColorUsed(AqlOptions options) {
    return new ED(
        Map.of("c", Chc.inLeft(COLOR)),
        rows(E, "x"),
        Set.of(),
        Set.of(is(att(A, v("x")), v("c"))),
        false,
        options);
  }

  @Nested
  class Construction {

    @Test
    void keepsAnEdWithoutUniqueness() {
      Constraints c = of(KEYED, key());
      assertEquals(List.of(key()), c.eds);
      assertSame(KEYED, c.schema);
      assertEquals(1, c.size());
      assertEquals(Kind.CONSTRAINTS, c.kind());
      assertEquals(key().toString(), c.toString());
    }

    /** exists unique e becomes exists e, plus: two such e are equal. */
    @Test
    void splitsAUniqueEdIntoExistenceAndUniqueness() {
      Constraints c = of(DEPTS, everyD(true));
      assertEquals(2, c.eds.size());
      assertEquals(everyD(false), c.eds.get(0));
      ED uniqueness = c.eds.get(1);
      Map<String, Chc<String, String>> as = rows(D, "d");
      as.putAll(rows(E, "e", "e_des_0"));
      assertEquals(as, uniqueness.As);
      assertTrue(uniqueness.Es.isEmpty());
      assertEquals(
          Set.of(is(fk(F, v("e")), v("d")), is(fk(F, v("e_des_0")), v("d"))), uniqueness.Awh);
      assertEquals(Set.of(is(v("e"), v("e_des_0"))), uniqueness.Ewh);
      assertFalse(uniqueness.isUnique);
    }

    @Test
    void separatesEdsByABlankLine() {
      Constraints c = of(DEPTS, everyD(true));
      assertEquals(c.eds.get(0) + "\n\n" + c.eds.get(1), c.toString());
    }

    @Test
    void canHaveNoEds() {
      Constraints c = new Constraints(ops(), KEYED);
      assertTrue(c.eds.isEmpty());
      assertSame(KEYED, c.schema);
      assertEquals("", c.toString());
    }

    @Test
    void comparesTheEdsInOrderAndTheSchema() {
      assertEquals(of(KEYED, key(), everyD(false)), of(KEYED, key(), everyD(false)));
      assertEquals(of(KEYED, key()).hashCode(), of(KEYED, key()).hashCode());
      assertNotEquals(of(KEYED, key(), everyD(false)), of(KEYED, everyD(false), key()));
      assertNotEquals(of(KEYED, key()), of(DEPTS, key()));
      Constraints c = of(KEYED, key());
      assertTrue(c.equals(c));
      assertFalse(c.equals(null));
      assertFalse(c.equals(key()));
    }
  }

  /** What {@code check C I} reports. */
  @Nested
  class Triggers {

    @Test
    void findEveryPairThatBreaksAKey() {
      var i = keyed();
      Collection<
              Pair<
                  Integer,
                  Row<
                      String,
                      Chc<Integer, Term<String, String, Sym, Fk, Att, String, String>>,
                      Chc<String, String>>>>
          t = of(KEYED, key()).triggers(i, ops());
      Integer e1 = i.algebra().gen("e1");
      Integer e2 = i.algebra().gen("e2");
      Set<List<Integer>> found = new HashSet<>();
      for (var p : t) {
        assertEquals(0, p.first);
        found.add(List.of(p.second.get("x").l, p.second.get("y").l));
      }
      assertEquals(Set.of(List.of(e1, e2), List.of(e2, e1)), found);
      assertEquals(2, t.size());
    }

    @Test
    void areNoneOnAnInstanceThatSatisfiesTheEds() {
      assertTrue(of(KEYED, key()).triggers(keyedOk(), ops()).isEmpty());
    }

    @Test
    void numberTheirEdAndFindAMissingWitness() {
      var i = depts();
      var t = of(DEPTS, nKey(), everyD(false)).triggers(i, ops());
      assertEquals(1, t.size());
      var p = t.iterator().next();
      assertEquals(1, p.first);
      assertEquals(i.algebra().gen("d2"), p.second.get("d").l);
    }

    @Test
    void skipAnEdWhoseFrontRangesOverAnEmptyEntity() {
      var empty = literal(DEPTS, Map.of(), List.of());
      assertTrue(of(DEPTS, everyD(false)).triggers(empty, ops()).isEmpty());
    }

    /** A value-sorted variable needs a typeside whose values can be enumerated. */
    @Test
    void compareValuesWithTheDecisionProcedure() {
      var i = colored();
      AqlOptions keep = new AqlOptions(ops(), AqlOption.query_remove_redundancy, false);
      var t =
          new Constraints(i.schema(), List.of(everyColorUsed(keep)), keep).triggers(i, keep);
      assertEquals(1, t.size(), "" + t);
      assertEquals(Term.Sym(GREEN, List.of()), t.iterator().next().second.get("c").r);
    }

    /**
     * Issue 54: with query_remove_redundancy (the default), the back block drops c, which x.a
     * defines, but the unit transform still maps the front's c to it.
     */
    @Test
    void failForAValueVariableOnlyTheConclusionConstrains() {
      var i = colored();
      Constraints c = new Constraints(i.schema(), List.of(everyColorUsed(ops())), ops());
      String m = assertThrows(RuntimeException.class, () -> c.triggers(i, ops())).getMessage();
      assertTrue(
          m.startsWith(
              "In transform for foreign key unit, In c, the labelled null c has no associated"
                  + " type."),
          m);
    }
  }

  /** What {@code chase C I} evaluates to. */
  @Nested
  class Chase {

    @Test
    void mergesTheRowsThatShareAKey() {
      var i = keyed();
      Constraints c = of(KEYED, key());
      var r = c.chase(i, ops());
      assertEquals(2, r.first.algebra().size(E));
      assertTrue(c.triggers(r.first, ops()).isEmpty());
      Object e1 = r.second.repr(E, i.algebra().gen("e1"));
      Object e2 = r.second.repr(E, i.algebra().gen("e2"));
      Object e3 = r.second.repr(E, i.algebra().gen("e3"));
      assertEquals(e1, e2);
      assertNotEquals(e1, e3);
    }

    @Test
    void addsTheMissingWitnessAndMergesTheExtraOnes() {
      var i = depts();
      Constraints c = of(DEPTS, everyD(true));
      var r = c.chase(i, ops());
      assertEquals(2, r.first.algebra().size(D));
      assertEquals(2, r.first.algebra().size(E));
      assertTrue(c.triggers(r.first, ops()).isEmpty());
    }

    /** The front's labelled null c is sent to the row's value, green. */
    @Test
    void addsARowForAValueWithoutOne() {
      var i = colored();
      AqlOptions keep = new AqlOptions(ops(), AqlOption.query_remove_redundancy, false);
      Constraints c = new Constraints(i.schema(), List.of(everyColorUsed(keep)), keep);
      var r = c.chase(i, keep);
      assertEquals(3, r.first.algebra().size(E));
      assertTrue(c.triggers(r.first, keep).isEmpty());
    }

    @Test
    void returnsASatisfyingInstanceUnchanged() {
      var i = keyedOk();
      Constraints c = of(KEYED, key());
      assertNull(c.step(i, ops()));
      var r = c.chase(i, ops());
      assertSame(i, r.first);
      assertSame(i, r.second.src());
      assertSame(i, r.second.dst());
    }
  }

  @Nested
  class Pushout {

    /** Issue 1: Instance.equals reads the collage before anything has built it. */
    @Test
    void comparesTheSourcesThroughInstanceEquals() {
      var a = keyed();
      var b = keyedOk();
      var j = new IdentityTransform<>(a, Optional.empty());
      var k = new IdentityTransform<>(b, Optional.empty());
      assertThrows(NullPointerException.class, () -> Constraints.pushout(j, k, ops()));
      a.hashCode();
      b.hashCode();
      String m =
          assertThrows(RuntimeException.class, () -> Constraints.pushout(j, k, ops()))
              .getMessage();
      assertTrue(m.startsWith("Source of \n"), m);
    }
  }

  @Nested
  class Export {

    @Test
    void tptpWritesOneFormulaPerEdAfterAnOptionalPreamble() {
      Constraints c = of(KEYED, key(), kv());
      KBTheory<Chc<String, String>, Head<String, String, Sym, Fk, Att, Object, Object>, String>
          kb = KEYED.collage().toKB();
      String with = c.tptp("axiom", true, kb);
      assertTrue(with.startsWith("fof(sort"), with);
      String without = c.tptp("axiom", false, kb);
      assertFalse(without.contains("fof(sort"), without);
      assertEquals(2, with.lines().filter(l -> l.startsWith("fof(eq")).count(), with);
      assertEquals(2, without.lines().filter(l -> l.startsWith("fof(eq")).count(), without);
      assertTrue(without.lines().allMatch(l -> l.contains(",axiom,(")), without);
    }

    /** Both numberings are static counters, so the names depend on earlier calls. */
    @Test
    void tptpNamesKeepCountingAcrossCalls() {
      Constraints c = of(KEYED, key());
      KBTheory<Chc<String, String>, Head<String, String, Sym, Fk, Att, Object, Object>, String>
          kb = KEYED.collage().toKB();
      assertNotEquals(c.tptp("axiom", false, kb), c.tptp("axiom", false, kb));
      String first = c.tptpSorted("axiom", KEYED);
      String second = c.tptpSorted("axiom", KEYED);
      assertTrue(first.startsWith("tff(ed") && first.endsWith(")).\n"), first);
      assertNotEquals(first, second);
      assertEquals(first.substring(first.indexOf(',')), second.substring(second.indexOf(',')));
    }

    @Test
    void asTransformsGivesOneTransformPerEd() {
      Constraints c = of(DEPTS, nKey(), everyD(false));
      assertEquals(2, c.asTransforms(DEPTS).size());
    }

    @Test
    void toSqlWritesOneBlockPerEd() {
      Constraints c = of(DEPTS, everyD(false));
      String sql = c.toSql(DEPTS);
      assertEquals(c.eds.get(0).toSql(DEPTS) + "\n", sql);
      assertEquals("", new Constraints(ops(), DEPTS).toSql(DEPTS));
    }
  }
}
