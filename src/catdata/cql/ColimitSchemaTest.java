package catdata.cql;

import static catdata.cql.CqlTestHelpers.VARCHAR;
import static catdata.cql.CqlTestHelpers.forall;
import static catdata.cql.CqlTestHelpers.ops;
import static catdata.cql.CqlTestHelpers.sqlSchema;
import static catdata.cql.CqlTestHelpers.sqlTypeSide;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import catdata.Chc;
import catdata.Pair;
import catdata.Quad;
import catdata.Triple;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.exp.Att;
import catdata.cql.exp.Fk;
import catdata.cql.exp.RawTerm;
import catdata.cql.exp.Sym;
import catdata.graph.DMG;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ColimitSchemaTest {

  private static final String HR = "hr";
  private static final String CRM = "crm";

  private static final String PERSON = "Person";
  private static final String OFFICE = "Office";
  private static final Fk WORKS_AT = Fk.Fk(PERSON, "worksAt");
  private static final Fk BACKUP_AT = Fk.Fk(PERSON, "backupAt");
  private static final Att NAME = Att.Att(PERSON, "name");
  private static final Att ADDR = Att.Att(OFFICE, "addr");

  private static final String CLIENT = "Client";
  private static final String SITE = "Site";
  private static final Fk LOCATED_IN = Fk.Fk(CLIENT, "locatedIn");
  private static final Att FULL_NAME = Att.Att(CLIENT, "fullName");
  private static final Att STREET = Att.Att(SITE, "street");

  /** In a colimit that merges Person with Client, the merged entity is named Person. */
  private static final Fk LOCATED_IN_P = Fk.Fk(PERSON, "locatedIn");

  private static final Att FULL_NAME_P = Att.Att(PERSON, "fullName");

  private static final class Fixtures {
    /** hr: Person -worksAt-> Office, with name on Person and addr on Office. */
    private static final Schema<String, String, Sym, Fk, Att> HR_SCH =
        sqlSchema(
            List.of(PERSON, OFFICE),
            Map.of(NAME, PERSON, ADDR, OFFICE),
            Map.of(WORKS_AT, new Pair<>(PERSON, OFFICE)));

    /** hr with a second foreign key backupAt and forall p:Person. p.worksAt = p.backupAt. */
    private static final Schema<String, String, Sym, Fk, Att> HR_EQ_SCH =
        sqlSchema(
            List.of(PERSON, OFFICE),
            Map.of(NAME, PERSON, ADDR, OFFICE),
            Map.of(WORKS_AT, new Pair<>(PERSON, OFFICE), BACKUP_AT, new Pair<>(PERSON, OFFICE)),
            List.of(
                forall("p", PERSON, Term.Fk(WORKS_AT, Term.Var("p")), Term.Fk(BACKUP_AT, Term.Var("p")))));

    /** crm: Client -locatedIn-> Site, with fullName on Client and street on Site. */
    private static final Schema<String, String, Sym, Fk, Att> CRM_SCH =
        sqlSchema(
            List.of(CLIENT, SITE),
            Map.of(FULL_NAME, CLIENT, STREET, SITE),
            Map.of(LOCATED_IN, new Pair<>(CLIENT, SITE)));
  }

  private static Map<String, Schema<String, String, Sym, Fk, Att>> hrAndCrm() {
    return Map.of(HR, Fixtures.HR_SCH, CRM, Fixtures.CRM_SCH);
  }

  private static Schema<String, String, Sym, Fk, Att> entities(String... ens) {
    return sqlSchema(List.of(ens), Map.of(), Map.of());
  }

  private static Quad<String, String, String, String> same(
      String n1, String e1, String n2, String e2) {
    return new Quad<>(n1, e1, n2, e2);
  }

  /** Person = Client and Office = Site. */
  private static Set<Quad<String, String, String, String>> merge() {
    return Set.of(same(HR, PERSON, CRM, CLIENT), same(HR, OFFICE, CRM, SITE));
  }

  private static AqlOptions with(AqlOption o, Object value) {
    return new AqlOptions(ops(), o, value);
  }

  private static AqlOptions leftBias() {
    return new AqlOptions(with(AqlOption.left_bias, true), AqlOption.simplify_names, false);
  }

  private static ColimitSchema<String> quotient(
      List<String> order,
      Map<String, Schema<String, String, Sym, Fk, Att>> nodes,
      Set<Quad<String, String, String, String>> eqEn,
      Set<Quad<String, String, RawTerm, RawTerm>> eqTerms,
      Set<Pair<List<String>, List<String>>> eqTerms2,
      AqlOptions options) {
    return new ColimitSchema<>(order, sqlTypeSide(), nodes, eqEn, eqTerms, eqTerms2, options);
  }

  /** hr + crm, nothing equated. */
  private static ColimitSchema<String> coproduct() {
    return quotient(List.of(HR, CRM), hrAndCrm(), Set.of(), new HashSet<>(), Set.of(), ops());
  }

  /** hr + crm with Person = Client and Office = Site. */
  private static ColimitSchema<String> merged(AqlOptions options) {
    return quotient(List.of(HR, CRM), hrAndCrm(), merge(), new HashSet<>(), Set.of(), options);
  }

  /** merged() plus the path equation Person.worksAt = Person.locatedIn. */
  private static ColimitSchema<String> mergedWithPath() {
    return quotient(
        List.of(HR, CRM),
        hrAndCrm(),
        merge(),
        new HashSet<>(),
        Set.of(new Pair<>(List.of(PERSON, "worksAt"), List.of(PERSON, "locatedIn"))),
        ops());
  }

  private static RawTerm app(String f, RawTerm arg) {
    return new RawTerm(f, List.of(arg));
  }

  /** merged() plus the observation equation forall p:Person. p.name = p.fullName. */
  private static ColimitSchema<String> mergedWithName() {
    Set<Quad<String, String, RawTerm, RawTerm>> eqTerms = new HashSet<>();
    eqTerms.add(
        new Quad<>("p", PERSON, app("name", new RawTerm("p")), app("fullName", new RawTerm("p"))));
    return quotient(List.of(HR, CRM), hrAndCrm(), merge(), eqTerms, Set.of(), ops());
  }

  private static Term<String, String, Sym, Fk, Att, Void, Void> v(String name) {
    return Term.Var(name);
  }

  private static Term<String, String, Sym, Fk, Att, Void, Void> fk(
      Fk f, Term<String, String, Sym, Fk, Att, Void, Void> t) {
    return Term.Fk(f, t);
  }

  private static Term<String, String, Sym, Fk, Att, Void, Void> att(
      Att a, Term<String, String, Sym, Fk, Att, Void, Void> t) {
    return Term.Att(a, t);
  }

  private static boolean proves(
      Schema<String, String, Sym, Fk, Att> s,
      String var,
      String en,
      Term<String, String, Sym, Fk, Att, Void, Void> l,
      Term<String, String, Sym, Fk, Att, Void, Void> r) {
    return s.dp.eq(Map.of(var, Chc.inRight(en)), l, r);
  }

  @Nested
  class Coproducts {

    @Test
    void prefixesEveryUnmergedEntityWithItsSchema() {
      // With simplify_names on (the default), an entity that is not merged is named node_entity.
      assertEquals(
          Set.of("hr_Person", "hr_Office", "crm_Client", "crm_Site"),
          new HashSet<>(coproduct().schemaStr.ens));
    }

    @Test
    void keepsTheForeignKeyAndAttributeNames() {
      Schema<String, String, Sym, Fk, Att> s = coproduct().schemaStr;
      assertEquals(new Pair<>("hr_Person", "hr_Office"), s.fks.get(Fk.Fk("hr_Person", "worksAt")));
      assertEquals(new Pair<>("hr_Person", VARCHAR), s.atts.get(Att.Att("hr_Person", "name")));
      assertEquals(2, s.fks.size());
      assertTrue(s.eqs.isEmpty());
      assertEquals(4, s.atts.size());
    }

    @Test
    void injectsEachSchema() {
      Mapping<String, String, Sym, Fk, Att, String, Fk, Att> hr = coproduct().mappingsStr.get(HR);
      assertEquals(Map.of(PERSON, "hr_Person", OFFICE, "hr_Office"), hr.ens);
      assertEquals(
          new Pair<>("hr_Person", List.of(Fk.Fk("hr_Person", "worksAt"))), hr.fks.get(WORKS_AT));
      assertEquals(
          new Triple<>("v", "hr_Person", att(Att.Att("hr_Person", "name"), v("v"))),
          hr.atts.get(NAME));
    }

    @Test
    void keepsTheNodesInTheGivenOrder() {
      ColimitSchema<String> c =
          quotient(List.of(CRM, HR), hrAndCrm(), Set.of(), new HashSet<>(), Set.of(), ops());
      assertEquals(List.of(CRM, HR), List.copyOf(c.nodes.keySet()));
      assertSame(sqlTypeSide(), c.ty);
    }

    @Test
    void unmergedEntitiesCanReceiveTheSameName() {
      // Node A_B with entity C and node A with entity B_C are both named A_B_C, so the two
      // entities, which nothing equates, become one.
      ColimitSchema<String> c =
          quotient(
              List.of("A_B", "A"),
              Map.of("A_B", entities("C"), "A", entities("B_C")),
              Set.of(),
              new HashSet<>(),
              Set.of(),
              ops());
      assertEquals(Set.of("A_B_C"), new HashSet<>(c.schemaStr.ens));
      assertEquals("A_B_C", c.mappingsStr.get("A_B").ens.get("C"));
      assertEquals("A_B_C", c.mappingsStr.get("A").ens.get("B_C"));
    }
  }

  @Nested
  class Quotients {

    @Test
    void namesAMergedEntityAfterItsFirstMember() {
      assertEquals(Set.of(PERSON, OFFICE), new HashSet<>(merged(ops()).schemaStr.ens));
    }

    @Test
    void keepsTheForeignKeysAndAttributesOfBothSchemas() {
      Schema<String, String, Sym, Fk, Att> s = merged(ops()).schemaStr;
      assertEquals(new Pair<>(PERSON, OFFICE), s.fks.get(WORKS_AT));
      assertEquals(new Pair<>(PERSON, OFFICE), s.fks.get(LOCATED_IN_P));
      assertEquals(
          Set.of(NAME, FULL_NAME_P, ADDR, Att.Att(OFFICE, "street")), new HashSet<>(s.atts.keySet()));
    }

    @Test
    void sendsEquatedEntitiesToTheSamePlace() {
      ColimitSchema<String> c = merged(ops());
      assertEquals(c.mappingsStr.get(HR).ens.get(PERSON), c.mappingsStr.get(CRM).ens.get(CLIENT));
      assertEquals(c.mappingsStr.get(HR).ens.get(OFFICE), c.mappingsStr.get(CRM).ens.get(SITE));
      assertEquals(
          new Pair<>(PERSON, List.of(LOCATED_IN_P)), c.mappingsStr.get(CRM).fks.get(LOCATED_IN));
    }

    @Test
    void namesEverythingAfterItsSchemaWithoutSimplifyNames() {
      ColimitSchema<String> c = merged(with(AqlOption.simplify_names, false));
      assertEquals(Set.of("hr_Person", "hr_Office"), new HashSet<>(c.schemaStr.ens));
      assertTrue(c.schemaStr.fks.containsKey(Fk.Fk("hr_Person", "hr_Person_worksAt")));
      assertTrue(c.schemaStr.fks.containsKey(Fk.Fk("hr_Person", "crm_Client_locatedIn")));
    }

    @Test
    void addsPathEquations() {
      Schema<String, String, Sym, Fk, Att> s = mergedWithPath().schemaStr;
      assertTrue(
          s.eqs.contains(
              new Triple<>(
                  new Pair<>("_v0", PERSON), fk(WORKS_AT, v("_v0")), fk(LOCATED_IN_P, v("_v0")))),
          s.eqs.toString());
      assertTrue(proves(s, "p", PERSON, fk(WORKS_AT, v("p")), fk(LOCATED_IN_P, v("p"))));
    }

    @Test
    void addsThePathEquationsToTheCallersTermEquations() {
      // quotient turns each path equation into a term equation and adds it to the eqTerms set it
      // was given, so the caller's set changes.
      Set<Quad<String, String, RawTerm, RawTerm>> eqTerms = new HashSet<>();
      quotient(
          List.of(HR, CRM),
          hrAndCrm(),
          merge(),
          eqTerms,
          Set.of(new Pair<>(List.of(PERSON, "worksAt"), List.of(PERSON, "locatedIn"))),
          ops());
      assertEquals(1, eqTerms.size());
    }

    @Test
    void addsObservationEquations() {
      Schema<String, String, Sym, Fk, Att> s = mergedWithName().schemaStr;
      assertTrue(proves(s, "p", PERSON, att(NAME, v("p")), att(FULL_NAME_P, v("p"))));
      assertFalse(proves(merged(ops()).schemaStr, "p", PERSON, att(NAME, v("p")), att(FULL_NAME_P, v("p"))));
    }

    @Test
    void twoMergedClassesCanReceiveTheSameName() {
      // {A.E, B.F} and {B.E, C.H} are different classes, but each is named after its first member,
      // and both first members are called E.
      ColimitSchema<String> c =
          quotient(
              List.of("A", "B", "C"),
              Map.of("A", entities("E"), "B", entities("F", "E"), "C", entities("H")),
              Set.of(same("A", "E", "B", "F"), same("B", "E", "C", "H")),
              new HashSet<>(),
              Set.of(),
              ops());
      assertEquals(Set.of("E"), new HashSet<>(c.schemaStr.ens));
      assertEquals("E", c.mappingsStr.get("C").ens.get("H"));
      assertEquals("E", c.mappingsStr.get("A").ens.get("E"));
    }
  }

  @Nested
  class QuotientErrors {

    private String failure(
        Set<Quad<String, String, String, String>> eqEn, Set<Pair<List<String>, List<String>>> eqTerms2) {
      return assertThrows(
              RuntimeException.class,
              () -> quotient(List.of(HR, CRM), hrAndCrm(), eqEn, new HashSet<>(), eqTerms2, ops()))
          .getMessage();
    }

    @Test
    void rejectsAnUnknownFirstSchema() {
      assertEquals("Not a schema: ghost", failure(Set.of(same("ghost", PERSON, CRM, CLIENT)), Set.of()));
    }

    @Test
    void namesTheWrongSchemaWhenTheSecondIsUnknown() {
      // The check is on s.third, but the message prints s.first.
      assertEquals("Not a schema: hr", failure(Set.of(same(HR, PERSON, "ghost", CLIENT)), Set.of()));
    }

    @Test
    void rejectsUnknownEntities() {
      assertEquals(
          "Not an entity in hr, Ghost", failure(Set.of(same(HR, "Ghost", CRM, CLIENT)), Set.of()));
      assertEquals(
          "Not an entity in crm, Ghost", failure(Set.of(same(HR, PERSON, CRM, "Ghost")), Set.of()));
    }

    @Test
    void rejectsAPathThatDoesNotStartAtAnEntity() {
      String msg = failure(merge(), Set.of(new Pair<>(List.of("Ghost"), List.of(PERSON))));
      assertTrue(msg.startsWith("Not an entity: Ghost. Paths must start with entities."), msg);
      msg = failure(merge(), Set.of(new Pair<>(List.of(PERSON), List.of("Ghost"))));
      assertTrue(msg.startsWith("Not an entity: Ghost. Paths must start with entities."), msg);
    }

    @Test
    void rejectsPathsFromDifferentEntities() {
      assertEquals(
          "Not equal before renaming: Person and Office",
          failure(merge(), Set.of(new Pair<>(List.of(PERSON), List.of(OFFICE)))));
    }

    @Test
    void rejectsATermEquationOverATypeSortedVariable() {
      Set<Quad<String, String, RawTerm, RawTerm>> eqTerms = new HashSet<>();
      eqTerms.add(new Quad<>("x", null, new RawTerm("x"), new RawTerm("k", VARCHAR)));
      RuntimeException e =
          assertThrows(
              RuntimeException.class,
              () -> quotient(List.of(HR, CRM), hrAndCrm(), merge(), eqTerms, Set.of(), ops()));
      assertTrue(e.getMessage().endsWith("variable x has type Varchar which is not an entity"), e.getMessage());
    }

    @Test
    void rejectsSimplifyNamesTogetherWithLeftBias() {
      RuntimeException e =
          assertThrows(RuntimeException.class, () -> merged(with(AqlOption.left_bias, true)));
      assertEquals("simplify_names and left_bias cannot both be true", e.getMessage());
    }
  }

  /**
   * With left_bias, the first of exactly two schemas wins every naming conflict (the javadoc of
   * Renamer2).
   */
  @Nested
  class LeftBias {

    @Test
    void namesAMergedEntityAfterTheFirstSchema() {
      ColimitSchema<String> c = merged(leftBias());
      assertEquals(Set.of(PERSON, OFFICE), new HashSet<>(c.schemaStr.ens));
      assertEquals(PERSON, c.mappingsStr.get(CRM).ens.get(CLIENT));
      assertTrue(c.schemaStr.fks.containsKey(LOCATED_IN_P));
      assertTrue(c.schemaStr.atts.containsKey(FULL_NAME_P));
    }

    @Test
    void prefixesAnUnmergedEntityOfTheSecondSchemaThatClashes() {
      ColimitSchema<String> c =
          quotient(
              List.of(HR, CRM),
              Map.of(HR, entities(PERSON), CRM, entities(PERSON)),
              Set.of(),
              new HashSet<>(),
              Set.of(),
              leftBias());
      assertEquals(PERSON, c.mappingsStr.get(HR).ens.get(PERSON));
      assertEquals("crm_Person", c.mappingsStr.get(CRM).ens.get(PERSON));
    }

    @Test
    void keepsTheNameOfAnUnmergedEntityOfTheSecondSchemaThatDoesNotClash() {
      ColimitSchema<String> c =
          quotient(
              List.of(HR, CRM),
              Map.of(HR, entities(PERSON), CRM, entities(CLIENT)),
              Set.of(),
              new HashSet<>(),
              Set.of(),
              leftBias());
      assertEquals(Set.of(PERSON, CLIENT), new HashSet<>(c.schemaStr.ens));
    }

    @Test
    void carriesTheEquationsOfTheSchemas() {
      ColimitSchema<String> c =
          quotient(
              List.of(HR, CRM),
              Map.of(HR, Fixtures.HR_EQ_SCH, CRM, Fixtures.CRM_SCH),
              merge(),
              new HashSet<>(),
              Set.of(),
              leftBias());
      assertTrue(proves(c.schemaStr, "p", PERSON, fk(WORKS_AT, v("p")), fk(BACKUP_AT, v("p"))));
    }

    @Test
    void requiresExactlyTwoSchemas() {
      RuntimeException e =
          assertThrows(
              RuntimeException.class,
              () ->
                  quotient(
                      List.of(HR, CRM, "x"),
                      Map.of(HR, Fixtures.HR_SCH, CRM, Fixtures.CRM_SCH, "x", entities("Thing")),
                      Set.of(),
                      new HashSet<>(),
                      Set.of(),
                      leftBias()));
      assertEquals("left_bias requires exactly two schemas as input.", e.getMessage());
    }

    @Test
    void rejectsAmbiguousNames() {
      // hr's X keeps its name, crm's X becomes crm_X, which is also the name of another hr entity.
      RuntimeException e =
          assertThrows(
              RuntimeException.class,
              () ->
                  quotient(
                      List.of(HR, CRM),
                      Map.of(HR, entities("X", "crm_X"), CRM, entities("X")),
                      Set.of(),
                      new HashSet<>(),
                      Set.of(),
                      leftBias()));
      assertTrue(e.getMessage().startsWith("Ambiguous renaming: "), e.getMessage());
    }

    @Test
    void rejectsTwoEntitiesOfTheFirstSchemaInOneClass() {
      RuntimeException e =
          assertThrows(
              RuntimeException.class,
              () ->
                  quotient(
                      List.of(HR, CRM),
                      Map.of(HR, entities("A", "B"), CRM, entities("Z")),
                      Set.of(same(HR, "A", CRM, "Z"), same(HR, "B", CRM, "Z")),
                      new HashSet<>(),
                      Set.of(),
                      leftBias()));
      assertTrue(e.getMessage().startsWith("Conflict-A on e1 = "), e.getMessage());
    }

    /** zeta: Person -link-> Office, ab: Client -link-> Site, with zeta first. */
    private ColimitSchema<String> clashingForeignKeys() {
      Fk zetaLink = Fk.Fk(PERSON, "link");
      Fk abLink = Fk.Fk(CLIENT, "link");
      Att zetaName = Att.Att(PERSON, "nm");
      Att abName = Att.Att(CLIENT, "nm");
      return quotient(
          List.of("zeta", "ab"),
          Map.of(
              "zeta",
              sqlSchema(
                  List.of(PERSON, OFFICE), Map.of(zetaName, PERSON), Map.of(zetaLink, new Pair<>(PERSON, OFFICE))),
              "ab",
              sqlSchema(
                  List.of(CLIENT, SITE), Map.of(abName, CLIENT), Map.of(abLink, new Pair<>(CLIENT, SITE)))),
          Set.of(same("zeta", PERSON, "ab", CLIENT), same("zeta", OFFICE, "ab", SITE)),
          new HashSet<>(),
          Set.of(),
          leftBias());
    }

    @Test
    void aClashingForeignKeyOfTheFirstSchemaLosesItsName() {
      // The comparator tests l twice instead of l and r, so the branches that put the first
      // schema's foreign keys first are never taken, and Util.ToStringComparator decides: shorter
      // strings first, so "(ab, link)" before "(zeta, link)". ab's link is named first and keeps
      // the name, and zeta's link, from the first schema, is renamed with ab's prefix.
      ColimitSchema<String> c = clashingForeignKeys();
      assertEquals(
          new Pair<>(PERSON, List.of(Fk.Fk(PERSON, "link"))),
          c.mappingsStr.get("ab").fks.get(Fk.Fk(CLIENT, "link")));
      assertEquals(
          new Pair<>(PERSON, List.of(Fk.Fk(PERSON, "ab_link"))),
          c.mappingsStr.get("zeta").fks.get(Fk.Fk(PERSON, "link")));
    }

    @Test
    void aClashingAttributeOfTheFirstSchemaKeepsItsName() {
      // The attributes are never sorted (the second sort is applied to the foreign keys again), so
      // they are named in the collage's insertion order, which makeCoprodSchema makes node order.
      // The first schema therefore wins for attributes, by insertion order rather than by the sort.
      ColimitSchema<String> c = clashingForeignKeys();
      assertEquals(
          Att.Att(PERSON, "nm"), c.mappingsStr.get("zeta").atts.get(Att.Att(PERSON, "nm")).third.att());
      assertEquals(
          Att.Att(PERSON, "ab_nm"), c.mappingsStr.get("ab").atts.get(Att.Att(CLIENT, "nm")).third.att());
    }
  }

  /** The colimit of a diagram S -f-> T is T, up to the names. */
  @Nested
  class Diagrams {

    private final Fk wi = Fk.Fk("Emp", "wi");
    private final Fk memberOf = Fk.Fk(PERSON, "memberOf");
    private final Att ename = Att.Att("Emp", "ename");
    private final Att pname = Att.Att(PERSON, "pname");

    private final Schema<String, String, Sym, Fk, Att> s =
        sqlSchema(List.of("Emp", "Dept"), Map.of(ename, "Emp"), Map.of(wi, new Pair<>("Emp", "Dept")));
    private final Schema<String, String, Sym, Fk, Att> t =
        sqlSchema(List.of(PERSON, "Team"), Map.of(pname, PERSON), Map.of(memberOf, new Pair<>(PERSON, "Team")));

    private ColimitSchema<String> colimit() {
      Mapping<String, String, Sym, Fk, Att, String, Fk, Att> f =
          new Mapping<>(
              Map.of("Emp", PERSON, "Dept", "Team"),
              Map.of(ename, new Triple<>("v", PERSON, att(pname, v("v")))),
              Map.of(wi, new Pair<>(PERSON, List.of(memberOf))),
              s,
              t,
              false);
      DMG<String, String> shape = new DMG<>(List.of("S", "T"), Map.of("f", new Pair<>("S", "T")));
      return new ColimitSchema<>(
          List.of("S", "T"), shape, sqlTypeSide(), Map.of("S", s, "T", t), Map.of("f", f), ops());
    }

    @Test
    void mergesEachEntityWithItsImage() {
      ColimitSchema<String> c = colimit();
      assertEquals(Set.of("Emp", "Dept"), new HashSet<>(c.schemaStr.ens));
      assertEquals("Emp", c.mappingsStr.get("T").ens.get(PERSON));
      assertEquals("Dept", c.mappingsStr.get("T").ens.get("Team"));
    }

    @Test
    void equatesEachForeignKeyAndAttributeWithItsImage() {
      Schema<String, String, Sym, Fk, Att> col = colimit().schemaStr;
      Fk cwi = Fk.Fk("Emp", "wi");
      Fk cmemberOf = Fk.Fk("Emp", "memberOf");
      assertTrue(proves(col, "v", "Emp", fk(cwi, v("v")), fk(cmemberOf, v("v"))));
      assertTrue(
          proves(col, "v", "Emp", att(Att.Att("Emp", "ename"), v("v")), att(Att.Att("Emp", "pname"), v("v"))));
    }

    @Test
    void rejectsANodeWithoutASchema() {
      DMG<String, String> shape = new DMG<>(List.of("S", "T", "U"), Map.of());
      RuntimeException e =
          assertThrows(
              RuntimeException.class,
              () ->
                  new ColimitSchema<>(
                      List.of("S", "T"), shape, sqlTypeSide(), Map.of("S", s, "T", t), Map.of(), ops()));
      assertEquals("No schema for node U", e.getMessage());
    }
  }

  @Nested
  class Renaming {

    @Test
    void renamesAnEntityEverywhere() {
      var r = merged(ops()).renameEntity(PERSON, "Human", false);
      Schema<String, String, Sym, Fk, Att> s = r.first.schemaStr;
      assertEquals(Set.of("Human", OFFICE), new HashSet<>(s.ens));
      assertEquals(new Pair<>("Human", OFFICE), s.fks.get(Fk.Fk("Human", "worksAt")));
      assertEquals(new Pair<>("Human", VARCHAR), s.atts.get(Att.Att("Human", "name")));
      assertEquals("Human", r.first.mappingsStr.get(CRM).ens.get(CLIENT));
      assertEquals("Human", r.second.ens.get(PERSON));
      assertEquals(PERSON, r.third.ens.get("Human"));
    }

    @Test
    void decidesEquationsThroughTheOriginalSchema() {
      var r = mergedWithPath().renameEntity(PERSON, "Human", false);
      Schema<String, String, Sym, Fk, Att> s = r.first.schemaStr;
      assertTrue(s.dp.toStringProver().startsWith("rename entity of "), s.dp.toStringProver());
      assertTrue(
          proves(s, "h", "Human", fk(Fk.Fk("Human", "worksAt"), v("h")), fk(Fk.Fk("Human", "locatedIn"), v("h"))));
    }

    @Test
    void rejectsAnEntityRenameThatDoesNotApply() {
      ColimitSchema<String> c = merged(ops());
      String msg = assertThrows(RuntimeException.class, () -> c.renameEntity("Ghost", "X", false)).getMessage();
      assertTrue(msg.startsWith("Ghost is not an entity in \n"), msg);
      msg = assertThrows(RuntimeException.class, () -> c.renameEntity(PERSON, OFFICE, false)).getMessage();
      assertTrue(msg.startsWith("Office is already an entity in \n"), msg);
    }

    @Test
    void renamesAForeignKey() {
      Fk employedAt = Fk.Fk(PERSON, "employedAt");
      var r = mergedWithPath().renameFk(WORKS_AT, employedAt, false);
      Schema<String, String, Sym, Fk, Att> s = r.first.schemaStr;
      assertFalse(s.fks.containsKey(WORKS_AT));
      assertEquals(new Pair<>(PERSON, OFFICE), s.fks.get(employedAt));
      assertEquals(new Pair<>(PERSON, List.of(employedAt)), r.first.mappingsStr.get(HR).fks.get(WORKS_AT));
      assertTrue(s.dp.toStringProver().startsWith("rename foreign key of "), s.dp.toStringProver());
      assertTrue(proves(s, "p", PERSON, fk(employedAt, v("p")), fk(LOCATED_IN_P, v("p"))));
    }

    @Test
    void rejectsAForeignKeyRenameThatDoesNotApply() {
      ColimitSchema<String> c = merged(ops());
      String msg =
          assertThrows(RuntimeException.class, () -> c.renameFk(Fk.Fk(PERSON, "ghost"), WORKS_AT, false))
              .getMessage();
      assertTrue(msg.startsWith("Person.ghost is not a foreign_key in \n"), msg);
      msg = assertThrows(RuntimeException.class, () -> c.renameFk(WORKS_AT, LOCATED_IN_P, false)).getMessage();
      assertTrue(msg.startsWith("locatedIn is already a foreign_key in \n"), msg);
    }

    @Test
    void renamesAnAttribute() {
      Att label = Att.Att(PERSON, "label");
      var r = mergedWithName().renameAtt(NAME, label, false);
      Schema<String, String, Sym, Fk, Att> s = r.first.schemaStr;
      assertFalse(s.atts.containsKey(NAME));
      assertEquals(att(label, v("v")), r.first.mappingsStr.get(HR).atts.get(NAME).third);
      assertTrue(s.dp.toStringProver().startsWith("rename attribute of "), s.dp.toStringProver());
      assertTrue(proves(s, "p", PERSON, att(label, v("p")), att(FULL_NAME_P, v("p"))));
    }

    @Test
    void rejectsAnAttributeRenameThatDoesNotApply() {
      ColimitSchema<String> c = merged(ops());
      String msg =
          assertThrows(RuntimeException.class, () -> c.renameAtt(Att.Att(PERSON, "ghost"), NAME, false))
              .getMessage();
      assertTrue(msg.startsWith("ghost is not an attribute of Person in \n"), msg);
      msg = assertThrows(RuntimeException.class, () -> c.renameAtt(NAME, FULL_NAME_P, false)).getMessage();
      assertTrue(msg.startsWith("fullName is already an attribute in \n"), msg);
    }
  }

  @Nested
  class Removal {

    @Test
    void replacesAForeignKeyByAProvablyEqualPath() {
      var r = mergedWithPath().removeFk(LOCATED_IN_P, List.of(WORKS_AT), false);
      Schema<String, String, Sym, Fk, Att> s = r.first.schemaStr;
      assertFalse(s.fks.containsKey(LOCATED_IN_P));
      assertTrue(s.eqs.isEmpty(), "worksAt = locatedIn becomes worksAt = worksAt and is dropped");
      assertEquals(new Pair<>(PERSON, List.of(WORKS_AT)), r.first.mappingsStr.get(CRM).fks.get(LOCATED_IN));
      assertTrue(s.dp.toStringProver().startsWith("remove foreign key of "), s.dp.toStringProver());
    }

    @Test
    void rejectsAForeignKeyRemovalThatDoesNotApply() {
      ColimitSchema<String> c = mergedWithPath();
      assertTrue(
          assertThrows(RuntimeException.class, () -> c.removeFk(Fk.Fk(PERSON, "ghost"), List.of(), false))
              .getMessage()
              .startsWith("ghost is not a foreign_key in \n"));
      assertEquals(
          "Cannot replace worksAt with worksAt because that path contains worksAt",
          assertThrows(RuntimeException.class, () -> c.removeFk(WORKS_AT, List.of(WORKS_AT), false))
              .getMessage());
      assertEquals(
          "The term v has type Person and not Office as expected.",
          assertThrows(RuntimeException.class, () -> c.removeFk(WORKS_AT, List.of(), false)).getMessage());
      ColimitSchema<String> unrelated = merged(ops());
      assertEquals(
          "The term v.worksAt is not provably equal to v.locatedIn",
          assertThrows(
                  RuntimeException.class,
                  () -> unrelated.removeFk(LOCATED_IN_P, List.of(WORKS_AT), false))
              .getMessage());
    }

    @Test
    void replacesAnAttributeByAProvablyEqualTerm() {
      var r = mergedWithName().removeAtt(FULL_NAME_P, "v", att(NAME, v("v")), false);
      Schema<String, String, Sym, Fk, Att> s = r.first.schemaStr;
      assertFalse(s.atts.containsKey(FULL_NAME_P));
      assertEquals(
          new Triple<>("v", PERSON, att(NAME, v("v"))), r.first.mappingsStr.get(CRM).atts.get(FULL_NAME));
      assertTrue(s.dp.toStringProver().startsWith("remove attribute of "), s.dp.toStringProver());
    }

    @Test
    void rejectsAnAttributeRemovalThatDoesNotApply() {
      ColimitSchema<String> c = mergedWithName();
      assertTrue(
          assertThrows(
                  RuntimeException.class,
                  () -> c.removeAtt(Att.Att(PERSON, "ghost"), "v", att(NAME, v("v")), false))
              .getMessage()
              .startsWith("ghost is not an attribute in \n"));
      assertEquals(
          "The term v.worksAt has type Office and not Varchar as expected.",
          assertThrows(
                  RuntimeException.class, () -> c.removeAtt(FULL_NAME_P, "v", fk(WORKS_AT, v("v")), false))
              .getMessage());
      assertEquals(
          "Cannot replace fullName with v.fullName because that term contains fullName",
          assertThrows(
                  RuntimeException.class, () -> c.removeAtt(FULL_NAME_P, "v", att(FULL_NAME_P, v("v")), false))
              .getMessage());
      ColimitSchema<String> unrelated = merged(ops());
      assertEquals(
          "The term v.name is not provably equal to v.fullName",
          assertThrows(
                  RuntimeException.class,
                  () -> unrelated.removeAtt(FULL_NAME_P, "v", att(NAME, v("v")), false))
              .getMessage());
    }
  }

  @Nested
  class Wrapping {

    @Test
    void rejectsAMappingFromAnotherSchema() {
      ColimitSchema<String> c = merged(ops());
      var id = Mapping.id(Fixtures.HR_SCH);
      String msg = assertThrows(RuntimeException.class, () -> c.wrap(id, id)).getMessage();
      assertTrue(msg.startsWith("Source of "), msg);
    }

    @Test
    void rejectsMappingsThatAreNotInverse() {
      // Swapping worksAt and locatedIn is a mapping from the colimit to itself, but without an
      // equation between them it is not the identity up to provable equality.
      ColimitSchema<String> c = merged(ops());
      Schema<String, String, Sym, Fk, Att> s = c.schemaStr;
      var id = Mapping.id(s);
      Map<Fk, Pair<String, List<Fk>>> swapped = new LinkedHashMap<>();
      swapped.put(WORKS_AT, new Pair<>(PERSON, List.of(LOCATED_IN_P)));
      swapped.put(LOCATED_IN_P, new Pair<>(PERSON, List.of(WORKS_AT)));
      var swap = new Mapping<>(id.ens, id.atts, swapped, s, s, true);
      String msg = assertThrows(RuntimeException.class, () -> c.wrap(id, swap)).getMessage();
      assertTrue(
          msg.endsWith(", which is not provably equal to itself,  when composing (toUser ; fromUser)"), msg);
    }

    @Test
    void rejectsAttributeMappingsThatAreNotInverse() {
      ColimitSchema<String> c = merged(ops());
      Schema<String, String, Sym, Fk, Att> s = c.schemaStr;
      var id = Mapping.id(s);
      Map<Att, Triple<String, String, Term<String, String, Sym, Fk, Att, Void, Void>>> swapped =
          new LinkedHashMap<>(id.atts);
      swapped.put(NAME, new Triple<>("v", PERSON, att(FULL_NAME_P, v("v"))));
      swapped.put(FULL_NAME_P, new Triple<>("v", PERSON, att(NAME, v("v"))));
      var swap = new Mapping<>(id.ens, swapped, id.fks, s, s, true);
      String msg = assertThrows(RuntimeException.class, () -> c.wrap(id, swap)).getMessage();
      assertTrue(msg.contains(", which is not provably equal to itself, "), msg);
    }
  }

  /**
   * The pseudo-colimit keeps the entities apart and joins each equated pair by a foreign key k and
   * its inverse k_inv. fromPsuedo maps it onto the real colimit, where k becomes the identity.
   */
  @Nested
  class PseudoColimits {

    private Map<String, Schema<String, String, Sym, Fk, Att>> ordered() {
      Map<String, Schema<String, String, Sym, Fk, Att>> m = new LinkedHashMap<>();
      m.put(HR, Fixtures.HR_SCH);
      m.put(CRM, Fixtures.CRM_SCH);
      return m;
    }

    private ColimitSchema<String> pseudo(Map<String, Quad<String, String, String, String>> eqEn) {
      return new ColimitSchema<>(sqlTypeSide(), ordered(), eqEn, new HashSet<>(), ops());
    }

    @Test
    void keepsEveryEntityApart() {
      ColimitSchema<String> c = pseudo(Map.of("same", same(HR, PERSON, CRM, CLIENT)));
      assertEquals(
          Set.of("hr_Person", "hr_Office", "crm_Client", "crm_Site"), new HashSet<>(c.schemaStr.ens));
      assertEquals("hr_Person", c.mappingsStr.get(HR).ens.get(PERSON));
    }

    @Test
    void joinsAnEquatedPairByAnIsomorphism() {
      Schema<String, String, Sym, Fk, Att> s = pseudo(Map.of("same", same(HR, PERSON, CRM, CLIENT))).schemaStr;
      Fk there = Fk.Fk("hr_Person", "same");
      Fk back = Fk.Fk("crm_Client", "same_inv");
      assertEquals(new Pair<>("hr_Person", "crm_Client"), s.fks.get(there));
      assertEquals(new Pair<>("crm_Client", "hr_Person"), s.fks.get(back));
      assertTrue(proves(s, "v", "hr_Person", fk(back, fk(there, v("v"))), v("v")));
      assertTrue(proves(s, "v", "crm_Client", fk(there, fk(back, v("v"))), v("v")));
    }

    @Test
    void carriesTheEquationsOfEachSchema() {
      Map<String, Schema<String, String, Sym, Fk, Att>> nodes = new LinkedHashMap<>();
      nodes.put(HR, Fixtures.HR_EQ_SCH);
      nodes.put(CRM, Fixtures.CRM_SCH);
      Schema<String, String, Sym, Fk, Att> s =
          new ColimitSchema<>(
                  sqlTypeSide(), nodes, Map.of("same", same(HR, PERSON, CRM, CLIENT)), new HashSet<>(), ops())
              .schemaStr;
      assertTrue(
          proves(
              s,
              "p",
              "hr_Person",
              fk(Fk.Fk("hr_Person", "worksAt"), v("p")),
              fk(Fk.Fk("hr_Person", "backupAt"), v("p"))));
    }

    @Test
    void mapsOntoTheRealColimit() {
      ColimitSchema<String> c = pseudo(Map.of("same", same(HR, PERSON, CRM, CLIENT)));
      assertSame(c.schemaStr, c.fromPsuedo.src);
      assertEquals(c.fromPsuedo.ens.get("hr_Person"), c.fromPsuedo.ens.get("crm_Client"));
      assertTrue(c.fromPsuedo.fks.get(Fk.Fk("hr_Person", "same")).second.isEmpty());
      assertTrue(c.fromPsuedo.fks.get(Fk.Fk("crm_Client", "same_inv")).second.isEmpty());
    }

    private Set<Object> constants(Schema<String, String, Sym, Fk, Att> s) {
      Set<Object> ret = new HashSet<>();
      for (var eq : s.eqs) {
        for (var side : List.of(eq.second, eq.third)) {
          if (side.obj() != null) {
            ret.add(side.obj());
          }
        }
      }
      return ret;
    }

    @Test
    void carriesAConstantIntoTheRealColimitAsItsJavaToString() {
      // The real colimit's equations are rebuilt by printing terms with toRaw and parsing them
      // again, and toRaw prints the SqlTypeSide constant Optional.of("k") as "Optional[k]".
      Set<Quad<String, Pair<String, String>, RawTerm, RawTerm>> eqTerms = new HashSet<>();
      eqTerms.add(
          new Quad<>("p", new Pair<>(HR, PERSON), app("name", new RawTerm("p")), new RawTerm("k", VARCHAR)));
      ColimitSchema<String> c =
          new ColimitSchema<>(
              sqlTypeSide(), ordered(), Map.of("same", same(HR, PERSON, CRM, CLIENT)), eqTerms, ops());
      assertEquals(Set.of(Optional.of("k")), constants(c.schemaStr));
      assertEquals(Set.of(Optional.of("Optional[k]")), constants(c.fromPsuedo.dst));
    }

    @Test
    void rejectsUnknownSchemasAndEntities() {
      assertEquals(
          "Not a schema: ghost available: hr,crm",
          assertThrows(RuntimeException.class, () -> pseudo(Map.of("k", same("ghost", PERSON, CRM, CLIENT))))
              .getMessage());
      assertEquals(
          "Not a schema: hr available: hr,crm",
          assertThrows(RuntimeException.class, () -> pseudo(Map.of("k", same(HR, PERSON, "ghost", CLIENT))))
              .getMessage(),
          "the check is on the second schema but the message names the first");
      assertEquals(
          "Not an entity in hr, Ghost",
          assertThrows(RuntimeException.class, () -> pseudo(Map.of("k", same(HR, "Ghost", CRM, CLIENT))))
              .getMessage());
      assertEquals(
          "Not an entity in crm, Ghost",
          assertThrows(RuntimeException.class, () -> pseudo(Map.of("k", same(HR, PERSON, CRM, "Ghost"))))
              .getMessage());
    }
  }

  @Nested
  class ToRaw {

    private final ColimitSchema<String> c = coproduct();

    @Test
    void writesVariablesAttributesAndForeignKeys() {
      assertEquals("v", c.toRaw(v("v"), Set.of()).toString());
      assertEquals("v.worksAt.name", c.toRaw(att(NAME, fk(WORKS_AT, v("v"))), Set.of()).toString());
    }

    @Test
    void skipsTheGivenForeignKeys() {
      assertEquals("v.name", c.toRaw(att(NAME, fk(WORKS_AT, v("v"))), Set.of("worksAt")).toString());
    }

    @Test
    void writesFunctionSymbols() {
      Sym isNull = Sym.Sym("isNull", new Pair<>(List.of(VARCHAR), CqlTestHelpers.BOOLEAN));
      RawTerm r = c.toRaw(Term.Sym(isNull, List.of(att(NAME, v("v")))), Set.of());
      assertEquals("isNull", r.head);
      assertEquals("v.name", r.args.get(0).toString());
    }

    @Test
    void writesAConstantByItsJavaToString() {
      // SqlTypeSide constants are Optionals, so the raw term is the string "Optional[k]", not "k".
      RawTerm r = c.toRaw(Term.Obj(Optional.of("k"), VARCHAR), Set.of());
      assertEquals("Optional[k]", r.head);
      assertEquals(VARCHAR, r.annotation);
    }
  }

  @Nested
  class EqualsAndHashCode {

    @Test
    void comparesOnlyTheNodesAndTheTypeside() {
      // The colimit schema, the mappings and the equations are not compared, so the coproduct and
      // the quotient of the same two schemas are equal.
      ColimitSchema<String> a = coproduct();
      ColimitSchema<String> b = merged(ops());
      assertNotEquals(new HashSet<>(a.schemaStr.ens), new HashSet<>(b.schemaStr.ens));
      assertEquals(a, b);
      assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void aRenamedColimitEqualsTheOriginal() {
      ColimitSchema<String> c = merged(ops());
      assertEquals(c, c.renameEntity(PERSON, "Human", false).first);
    }

    @Test
    void isReflexiveAndRejectsNullAndForeignTypes() {
      ColimitSchema<String> c = coproduct();
      assertTrue(c.equals(c));
      assertFalse(c.equals(null));
      assertFalse(c.equals("not a colimit"));
    }

    @Test
    void distinguishesOnTheNodes() {
      ColimitSchema<String> hrOnly =
          quotient(List.of(HR), Map.of(HR, Fixtures.HR_SCH), Set.of(), new HashSet<>(), Set.of(), ops());
      assertNotEquals(coproduct(), hrOnly);
    }
  }

  @Nested
  class Accessors {

    @Test
    void delegatesToTheColimitSchema() {
      ColimitSchema<String> c = merged(ops());
      assertEquals(c.schemaStr.size(), c.size());
      assertEquals(c.schemaStr.toString(), c.toString());
      assertEquals(Kind.SCHEMA_COLIMIT, c.kind());
    }
  }
}
