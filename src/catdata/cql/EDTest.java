package catdata.cql;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.Chc;
import catdata.Pair;
import catdata.cql.Collage.CCollage;
import catdata.cql.exp.Att;
import catdata.cql.exp.Fk;
import catdata.cql.exp.Sym;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class EDTest {

  private static final String EN = "Employee";
  private static final String DEPT = "Department";
  private static final String TY = "Varchar";

  private static AqlOptions ops() {
    return AqlOptions.initialOptions;
  }

  /**
   * A two-entity acyclic schema over {@link SqlTypeSide}: Employee.worksIn : Employee ->
   * Department, with a Varchar attribute on each. Built once and shared; constructing a typeside
   * spins up the JS engine and a prover, which is slow.
   */
  private static Schema<String, String, Sym, Fk, Att> schema() {
    return SchemaHolder.INSTANCE;
  }

  private static final class SchemaHolder {
    private static final Schema<String, String, Sym, Fk, Att> INSTANCE = build();

    private static Schema<String, String, Sym, Fk, Att> build() {
      TypeSide<String, Sym> ts = SqlTypeSide.SqlTypeSide(ops());
      Collage<String, String, Sym, Fk, Att, Void, Void> col = new CCollage<>();
      col.tys().addAll(ts.tys);
      col.syms().putAll(ts.syms);
      col.java_tys().putAll(ts.js.java_tys);
      col.getEns().add(EN);
      col.getEns().add(DEPT);
      col.atts().put(Att.Att(EN, "name"), new Pair<>(EN, TY));
      col.atts().put(Att.Att(DEPT, "label"), new Pair<>(DEPT, TY));
      col.fks().put(Fk.Fk(EN, "worksIn"), new Pair<>(EN, DEPT));
      return new Schema<>(ts, col, ops());
    }
  }

  private static Chc<String, String> dept() {
    return Chc.inRight(DEPT);
  }

  /** An entity-sorted variable, i.e. one ranging over rows rather than over type values. */
  private static Chc<String, String> en() {
    return Chc.inRight(EN);
  }

  /** A type-sorted variable, i.e. one ranging over the typeside. */
  private static Chc<String, String> ty() {
    return Chc.inLeft(TY);
  }

  private static Term<String, String, Sym, Fk, Att, Void, Void> v(String name) {
    return Term.Var(name);
  }

  private static Map<String, Chc<String, String>> ctx(String var, Chc<String, String> sort) {
    Map<String, Chc<String, String>> m = new HashMap<>();
    m.put(var, sort);
    return m;
  }

  @SafeVarargs
  private static Set<
          Pair<
              Term<String, String, Sym, Fk, Att, Void, Void>,
              Term<String, String, Sym, Fk, Att, Void, Void>>>
      eqs(
          Pair<
                  Term<String, String, Sym, Fk, Att, Void, Void>,
                  Term<String, String, Sym, Fk, Att, Void, Void>>...
              ps) {
    Set<
            Pair<
                Term<String, String, Sym, Fk, Att, Void, Void>,
                Term<String, String, Sym, Fk, Att, Void, Void>>>
        s = new HashSet<>();
    Collections.addAll(s, ps);
    return s;
  }

  /** The trivial ED: no hypotheses, no conclusion. */
  private static ED empty() {
    return new ED(
        Collections.emptyMap(),
        Collections.emptyMap(),
        Collections.emptySet(),
        Collections.emptySet(),
        false,
        ops());
  }

  /** forall a:Employee -> exists e:Employee. */
  private static ED forallExists() {
    return new ED(ctx("a", en()), ctx("e", en()), eqs(), eqs(), false, ops());
  }

  @Nested
  class Construction {

    @Test
    void copiesItsInputsDefensively() {
      Map<String, Chc<String, String>> as = ctx("a", en());
      Map<String, Chc<String, String>> es = ctx("e", en());
      Set<
              Pair<
                  Term<String, String, Sym, Fk, Att, Void, Void>,
                  Term<String, String, Sym, Fk, Att, Void, Void>>>
          awh = eqs(new Pair<>(v("a"), v("a")));
      Set<
              Pair<
                  Term<String, String, Sym, Fk, Att, Void, Void>,
                  Term<String, String, Sym, Fk, Att, Void, Void>>>
          ewh = eqs(new Pair<>(v("e"), v("e")));

      ED ed = new ED(as, es, awh, ewh, false, ops());

      as.put("intruder", en());
      es.put("intruder", en());
      awh.clear();
      ewh.clear();

      assertEquals(1, ed.As.size(), "As must not alias the caller's map");
      assertEquals(1, ed.Es.size(), "Es must not alias the caller's map");
      assertEquals(1, ed.Awh.size(), "Awh must not alias the caller's set");
      assertEquals(1, ed.Ewh.size(), "Ewh must not alias the caller's set");
    }

    @Test
    void retainsTheDeclaredContexts() {
      ED ed = forallExists();
      assertEquals(en(), ed.As.get("a"));
      assertEquals(en(), ed.Es.get("e"));
      assertTrue(!ed.isUnique);
    }

    @Test
    void uniqueWithNoExistentialsIsAnAnomaly() {
      // "exists unique" with an empty exists clause is not expressible; Util.anomaly rejects it.
      RuntimeException e =
          assertThrows(
              RuntimeException.class,
              () ->
                  new ED(
                      ctx("a", en()),
                      Collections.emptyMap(),
                      Collections.emptySet(),
                      Collections.emptySet(),
                      true,
                      ops()));
      assertTrue(e.getMessage().contains("Anomaly"));
    }

    @Test
    void uniqueWithExistentialsIsAccepted() {
      ED ed = new ED(ctx("a", en()), ctx("e", en()), eqs(), eqs(), true, ops());
      assertTrue(ed.isUnique);
    }

    @Test
    void acceptsTypeSortedVariables() {
      ED ed = new ED(ctx("x", ty()), ctx("y", ty()), eqs(), eqs(), false, ops());
      assertTrue(ed.As.get("x").left, "type-sorted variables land on the left of the Chc");
      assertTrue(ed.Es.get("y").left);
    }
  }

  @Nested
  class ToString {

    @Test
    void theTrivialEdIsJustTheImplicationArrow() {
      assertEquals("->\n", empty().toString());
    }

    @Test
    void rendersTheForallClause() {
      ED ed = new ED(ctx("a", en()), Collections.emptyMap(), eqs(), eqs(), false, ops());
      String s = ed.toString();
      assertTrue(s.startsWith("\tforall"), s);
      assertTrue(s.contains("a:" + EN), s);
      assertTrue(s.contains("->"), s);
    }

    @Test
    void rendersTheExistsClause() {
      String s = forallExists().toString();
      assertTrue(s.contains("\texists"), s);
      assertTrue(s.contains("e:" + EN), s);
    }

    @Test
    void marksUniqueExistentials() {
      ED plain = new ED(ctx("a", en()), ctx("e", en()), eqs(), eqs(), false, ops());
      ED uniq = new ED(ctx("a", en()), ctx("e", en()), eqs(), eqs(), true, ops());
      assertTrue(!plain.toString().contains("exists unique"), plain.toString());
      assertTrue(uniq.toString().contains("exists unique"), uniq.toString());
    }

    @Test
    void rendersBothWhereClauses() {
      ED ed =
          new ED(
              ctx("a", en()),
              ctx("e", en()),
              eqs(new Pair<>(v("a"), v("a"))),
              eqs(new Pair<>(v("e"), v("e"))),
              false,
              ops());
      String s = ed.toString();
      assertEquals(2, s.split("\twhere", -1).length - 1, "one where per clause: " + s);
      assertTrue(s.contains("a = a"), s);
      assertTrue(s.contains("e = e"), s);
    }

    @Test
    void putsTheForallClauseBeforeTheArrowAndTheExistsClauseAfter() {
      ED ed =
          new ED(
              ctx("a", en()),
              ctx("e", en()),
              eqs(new Pair<>(v("a"), v("a"))),
              eqs(new Pair<>(v("e"), v("e"))),
              false,
              ops());
      String s = ed.toString();
      int arrow = s.indexOf("->");
      assertTrue(s.indexOf("\tforall") < arrow, s);
      assertTrue(s.indexOf("\texists") > arrow, s);
      assertTrue(s.indexOf("a = a") < arrow, "the forall where-clause precedes the arrow: " + s);
      assertTrue(s.indexOf("e = e") > arrow, "the exists where-clause follows the arrow: " + s);
    }

    @Test
    void omitsEmptyClauses() {
      String s = forallExists().toString();
      assertTrue(!s.contains("\twhere"), "no where clause when both are empty: " + s);
    }
  }

  @Nested
  class EqualsAndHashCode {

    @Test
    void isReflexive() {
      ED ed = forallExists();
      assertEquals(ed, ed);
    }

    @Test
    void isSymmetricOnEqualValues() {
      ED a = forallExists();
      ED b = forallExists();
      assertEquals(a, b);
      assertEquals(b, a);
      assertEquals(a.hashCode(), b.hashCode(), "equal EDs must agree on hashCode");
    }

    @Test
    void rejectsNullAndForeignTypes() {
      ED ed = forallExists();
      assertNotEquals(null, ed);
      assertNotEquals(ed, new Object());
      assertNotEquals("not an ED", ed);
    }

    @Test
    void distinguishesOnTheForallContext() {
      ED a = new ED(ctx("a", en()), ctx("e", en()), eqs(), eqs(), false, ops());
      ED b = new ED(ctx("b", en()), ctx("e", en()), eqs(), eqs(), false, ops());
      assertNotEquals(a, b);
    }

    @Test
    void distinguishesOnTheExistsContext() {
      ED a = new ED(ctx("a", en()), ctx("e", en()), eqs(), eqs(), false, ops());
      ED b = new ED(ctx("a", en()), ctx("f", en()), eqs(), eqs(), false, ops());
      assertNotEquals(a, b);
    }

    @Test
    void distinguishesOnTheForallWhereClause() {
      ED a =
          new ED(
              ctx("a", en()), ctx("e", en()), eqs(new Pair<>(v("a"), v("a"))), eqs(), false, ops());
      ED b = new ED(ctx("a", en()), ctx("e", en()), eqs(), eqs(), false, ops());
      assertNotEquals(a, b);
    }

    @Test
    void distinguishesOnTheExistsWhereClause() {
      ED a =
          new ED(
              ctx("a", en()), ctx("e", en()), eqs(), eqs(new Pair<>(v("e"), v("e"))), false, ops());
      ED b = new ED(ctx("a", en()), ctx("e", en()), eqs(), eqs(), false, ops());
      assertNotEquals(a, b);
    }

    @Test
    void distinguishesOnUniqueness() {
      ED a = new ED(ctx("a", en()), ctx("e", en()), eqs(), eqs(), false, ops());
      ED b = new ED(ctx("a", en()), ctx("e", en()), eqs(), eqs(), true, ops());
      assertNotEquals(a, b);
      assertNotEquals(a.hashCode(), b.hashCode(), "isUnique participates in hashCode");
    }

    @Test
    void ignoresOptions() {
      // options is deliberately absent from both equals and hashCode: two EDs asserting the same
      // sentence are the same constraint regardless of the prover settings used to check it.
      ED a = new ED(ctx("a", en()), ctx("e", en()), eqs(), eqs(), false, AqlOptions.initialOptions);
      ED b =
          new ED(
              ctx("a", en()),
              ctx("e", en()),
              eqs(),
              eqs(),
              false,
              new AqlOptions(Collections.emptyMap(), AqlOptions.initialOptions));
      assertEquals(a, b);
      assertEquals(a.hashCode(), b.hashCode());
    }
  }

  @Nested
  class Unfreeze {

    private final ED ed = empty();

    @Test
    void turnsGeneratorsIntoPrefixedVariables() {
      assertEquals(v("Ag"), ed.unfreeze("A", Term.Gen("g")));
    }

    @Test
    void turnsSkolemsIntoPrefixedVariables() {
      assertEquals(v("Es"), ed.unfreeze("E", Term.Sk("s")));
    }

    @Test
    void distinguishesGeneratorsAndSkolemsOnlyByName() {
      // Both collapse to variables, so a gen and an sk sharing a name unfreeze identically.
      assertEquals(ed.unfreeze("A", Term.Gen("x")), ed.unfreeze("A", Term.Sk("x")));
    }

    @Test
    void recursesThroughForeignKeys() {
      Fk fk = Fk.Fk(EN, "manager");
      Term<String, String, Sym, Fk, Att, String, String> t = Term.Fk(fk, Term.Gen("g"));
      assertEquals(Term.Fk(fk, v("Ag")), ed.unfreeze("A", t));
    }

    @Test
    void recursesThroughAttributes() {
      Att att = Att.Att(EN, "name");
      Term<String, String, Sym, Fk, Att, String, String> t = Term.Att(att, Term.Gen("g"));
      assertEquals(Term.Att(att, v("Ag")), ed.unfreeze("A", t));
    }

    @Test
    void recursesThroughNestedForeignKeys() {
      Fk fk = Fk.Fk(EN, "manager");
      Term<String, String, Sym, Fk, Att, String, String> t =
          Term.Fk(fk, Term.Fk(fk, Term.Gen("g")));
      assertEquals(Term.Fk(fk, Term.Fk(fk, v("Ag"))), ed.unfreeze("A", t));
    }

    @Test
    void rejectsTermsThatAreAlreadyVariables() {
      // unfreeze is the inverse of freeze, so its input must not contain variables already.
      Term<String, String, Sym, Fk, Att, String, String> t = Term.Var("x");
      RuntimeException e = assertThrows(RuntimeException.class, () -> ed.unfreeze("A", t));
      assertTrue(e.getMessage().contains("Anomaly"), e.getMessage());
    }

    @Test
    void appliesThePrefixVerbatim() {
      assertEquals(v("g"), ed.unfreeze("", Term.Gen("g")));
      assertEquals(v("prefix_g"), ed.unfreeze("prefix_", Term.Gen("g")));
    }
  }

  @Nested
  class Constants {

    @Test
    void theUnitForeignKeyPointsFromBackToFront() {
      assertEquals("front", ED.FRONT);
      assertEquals("back", ED.BACK);
      assertEquals(ED.BACK, ED.UNIT.en, "unit is declared on the back entity");
      assertEquals("unit", ED.UNIT.str);
    }
  }

  /**
   * Tests for the methods that need a real schema to interpret the ED against. The schema is
   * deliberately acyclic: a cyclic one with generators and no equations trips the divergence check
   * in {@link catdata.cql.fdm.InitialAlgebra}.
   */
  @Nested
  class WithSchema {

    private final Schema<String, String, Sym, Fk, Att> sch = schema();
    private final Att name = Att.Att(EN, "name");
    private final Att label = Att.Att(DEPT, "label");

    /** forall a:Employee -> exists e:Department. */
    private ED simple() {
      return new ED(ctx("a", en()), ctx("e", dept()), eqs(), eqs(), false, ops());
    }

    @Test
    void validateAcceptsAWellTypedEd() {
      assertDoesNotThrow(() -> simple().validate(sch));
    }

    @Test
    void validateRejectsAVariableMissingFromTheForallContext() {
      ED ed =
          new ED(
              ctx("a", en()),
              ctx("e", dept()),
              eqs(new Pair<>(v("undeclared"), v("undeclared"))),
              eqs(),
              false,
              ops());
      RuntimeException e = assertThrows(RuntimeException.class, () -> ed.validate(sch));
      assertTrue(e.getMessage().contains("is not a variable in type context"), e.getMessage());
    }

    @Test
    void validateIgnoresTheExistsWhereClause() {
      // validate only types Awh; an unbound variable in Ewh is not caught here.
      ED ed =
          new ED(
              ctx("a", en()),
              ctx("e", dept()),
              eqs(),
              eqs(new Pair<>(v("undeclared"), v("undeclared"))),
              false,
              ops());
      assertDoesNotThrow(() -> ed.validate(sch));
    }

    @Test
    void frontHasAGeneratorPerUniversalVariable() {
      assertEquals(1, simple().front(sch).gens().size());
    }

    @Test
    void backExtendsFrontWithTheExistentialVariables() {
      ED ed = simple();
      assertEquals(1, ed.front(sch).gens().size());
      assertEquals(2, ed.back(sch).gens().size(), "back carries both the forall and exists gens");
    }

    @Test
    void frontQAgreesWithFrontOnGenerators() {
      ED ed = simple();
      assertEquals(ed.front(sch).gens().size(), ed.frontQ(sch).gens().size());
    }

    @Test
    void typeSortedVariablesBecomeSkolemsNotGenerators() {
      ED ed = new ED(ctx("x", ty()), ctx("e", dept()), eqs(), eqs(), false, ops());
      assertEquals(0, ed.front(sch).gens().size(), "a type-sorted forall variable is not a gen");
      assertEquals(1, ed.front(sch).sks().size(), "it is a skolem instead");
    }

    @Test
    void asTransformGoesFromFrontToBack() {
      ED ed = simple();
      var t = ed.asTransform(sch);
      assertEquals(ed.front(sch).gens().size(), t.src().gens().size());
      assertEquals(ed.back(sch).gens().size(), t.dst().gens().size());
    }

    @Test
    void getQIsCachedPerSchema() {
      ED ed = simple();
      assertSame(ed.getQ(sch), ed.getQ(sch), "getQ memoizes on the schema");
    }

    @Test
    void toSqlEmitsAnInsertPerExistentialEntity() {
      ED ed =
          new ED(
              ctx("a", en()),
              ctx("e", dept()),
              eqs(),
              eqs(new Pair<>(Term.Att(label, v("e")), Term.Att(name, v("a")))),
              false,
              ops());
      String sql = ed.toSql(sch);
      assertTrue(sql.contains("INSERT INTO " + DEPT), sql);
      assertTrue(sql.contains("SELECT a.name AS [label]"), sql);
      assertTrue(sql.contains("FROM Employee AS [a]"), sql);
    }

    @Test
    void toSqlSkipsTypeSortedExistentials() {
      // Only entity-sorted existentials become tables to insert into.
      ED ed = new ED(ctx("a", en()), ctx("x", ty()), eqs(), eqs(), false, ops());
      assertEquals("", ed.toSql(sch));
    }

    @Test
    void toSqlFillsUnconstrainedAttributesWithNull() {
      String sql = simple().toSql(sch);
      assertTrue(
          sql.contains("NULL AS [label]"), "label is unconstrained, so it inserts NULL: " + sql);
    }

    @Test
    void toSqlEmitsAWhereClauseForTheForallEquations() {
      ED ed =
          new ED(
              ctx("a", en()),
              ctx("e", dept()),
              eqs(new Pair<>(Term.Att(name, v("a")), Term.Att(name, v("a")))),
              eqs(),
              false,
              ops());
      assertTrue(ed.toSql(sch).contains("WHERE"), ed.toSql(sch));
    }

    @Test
    void tptpXSortedRendersAUniversalOverAnExistential() {
      String tptp = simple().tptpXSorted(sch);
      assertTrue(tptp.startsWith("(! ["), "opens with a universal quantifier: " + tptp);
      assertTrue(tptp.contains("? ["), "contains an existential quantifier: " + tptp);
      assertTrue(tptp.contains("=>"), "the ED is an implication: " + tptp);
    }

    @Test
    void tptpXSortedUsesTrueForAnEmptyConclusion() {
      ED ed = new ED(ctx("a", en()), Collections.emptyMap(), eqs(), eqs(), false, ops());
      assertTrue(ed.tptpXSorted(sch).contains("$true"), ed.tptpXSorted(sch));
    }

    @Test
    void tptpXSortedRejectsUniqueEds() {
      // tptpXSorted has no encoding for "exists unique" and bails out.
      ED ed = new ED(ctx("a", en()), ctx("e", dept()), eqs(), eqs(), true, ops());
      RuntimeException e = assertThrows(RuntimeException.class, () -> ed.tptpXSorted(sch));
      assertTrue(e.getMessage().contains("Anomaly"), e.getMessage());
    }

    @Test
    void sigmaRenamesEntitiesAlongTheMapping() {
      // sigma with no mapping applied is exercised by the examples; here we check that an ED
      // over entity-sorted variables keeps its type-sorted ones untouched.
      ED ed = new ED(ctx("x", ty()), ctx("e", dept()), eqs(), eqs(), false, ops());
      assertTrue(ed.As.get("x").left, "type-sorted variables are not remapped by sigma");
    }
  }
}
