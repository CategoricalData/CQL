package catdata.cql;

import static catdata.cql.CqlTestHelpers.VARCHAR;
import static catdata.cql.CqlTestHelpers.forall;
import static catdata.cql.CqlTestHelpers.ops;
import static catdata.cql.CqlTestHelpers.sqlSchema;
import static catdata.cql.CqlTestHelpers.sqlTypeSide;
import static catdata.cql.CqlTestHelpers.syntactic;
import static catdata.cql.CqlTestHelpers.typedSqlSchema;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.cql.exp.Att;
import catdata.cql.exp.Fk;
import catdata.cql.exp.Sym;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SchemaTest {

  private static final String EMP = "Employee";
  private static final String DEPT = "Department";
  private static final String CO = "Company";
  private static final Fk WORKS_IN = Fk.Fk(EMP, "worksIn");
  private static final Fk MANAGES = Fk.Fk(EMP, "manages");
  private static final Fk PART_OF = Fk.Fk(DEPT, "partOf");
  private static final Att NAME = Att.Att(EMP, "name");
  private static final Att LABEL = Att.Att(DEPT, "label");

  private static final String NODE = "Node";
  private static final Fk PARENT = Fk.Fk(NODE, "parent");

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

  private static Term<String, String, Sym, Fk, Att, Void, Void> str(String s) {
    return Term.Obj(Optional.of(s), VARCHAR);
  }

  private static Triple<Pair<String, String>, Term<String, String, Sym, Fk, Att, Void, Void>, Term<String, String, Sym, Fk, Att, Void, Void>>
      eq(
          String var,
          String en,
          Term<String, String, Sym, Fk, Att, Void, Void> l,
          Term<String, String, Sym, Fk, Att, Void, Void> r) {
    return new Triple<>(new Pair<>(var, en), l, r);
  }

  /** forall e:Employee. e.worksIn.partOf = e.manages.partOf */
  private static final Triple<Pair<String, String>, Term<String, String, Sym, Fk, Att, Void, Void>, Term<String, String, Sym, Fk, Att, Void, Void>>
      SAME_COMPANY =
          eq("e", EMP, fk(PART_OF, fk(WORKS_IN, v("e"))), fk(PART_OF, fk(MANAGES, v("e"))));

  /** forall e:Employee. e.name = e.worksIn.label */
  private static final Triple<Pair<String, String>, Term<String, String, Sym, Fk, Att, Void, Void>, Term<String, String, Sym, Fk, Att, Void, Void>>
      NAMED_AFTER_DEPT = eq("e", EMP, att(NAME, v("e")), att(LABEL, fk(WORKS_IN, v("e"))));

  private static final Map<Att, String> ORG_ATTS = Map.of(NAME, EMP, LABEL, DEPT);
  private static final Map<Fk, Pair<String, String>> ORG_FKS =
      Map.of(
          WORKS_IN, new Pair<>(EMP, DEPT),
          MANAGES, new Pair<>(EMP, DEPT),
          PART_OF, new Pair<>(DEPT, CO));

  /**
   * Employee -worksIn, manages-> Department -partOf-> Company, with name on Employee and label on
   * Department, and no equations. Built fresh on every call, since truncate numbers names per
   * schema instance.
   */
  private static Schema<String, String, Sym, Fk, Att> plain() {
    return sqlSchema(List.of(EMP, DEPT, CO), ORG_ATTS, ORG_FKS);
  }

  /** plain() with the equations SAME_COMPANY and NAMED_AFTER_DEPT. */
  private static Schema<String, String, Sym, Fk, Att> org() {
    return sqlSchema(
        List.of(EMP, DEPT, CO),
        ORG_ATTS,
        ORG_FKS,
        List.of(
            forall(
                "e",
                EMP,
                Term.Fk(PART_OF, Term.Fk(WORKS_IN, Term.Var("e"))),
                Term.Fk(PART_OF, Term.Fk(MANAGES, Term.Var("e")))),
            forall(
                "e",
                EMP,
                Term.Att(NAME, Term.Var("e")),
                Term.Att(LABEL, Term.Fk(WORKS_IN, Term.Var("e"))))));
  }

  /**
   * A schema over SqlTypeSide built with the seven-argument constructor and syntactic equality, so
   * that no prover is involved and the collections are exactly the ones given.
   */
  private static Schema<String, String, Sym, Fk, Att> raw(
      Collection<String> ens,
      Map<Att, Pair<String, String>> atts,
      Map<Fk, Pair<String, String>> fks,
      Collection<Triple<Pair<String, String>, Term<String, String, Sym, Fk, Att, Void, Void>, Term<String, String, Sym, Fk, Att, Void, Void>>>
          eqs,
      boolean checkJava) {
    return new Schema<>(sqlTypeSide(), ens, atts, fks, eqs, syntactic(), checkJava);
  }

  private static Map<Att, Pair<String, String>> varchars(Map<Att, String> atts) {
    return atts.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> new Pair<>(e.getValue(), VARCHAR)));
  }

  @Nested
  class Construction {

    @Test
    void keepsTheEntitiesAttributesAndForeignKeysOfTheCollage() {
      Schema<String, String, Sym, Fk, Att> s = org();
      assertEquals(Set.of(EMP, DEPT, CO), new HashSet<>(s.ens));
      assertEquals(varchars(ORG_ATTS), s.atts);
      assertEquals(ORG_FKS, s.fks);
      assertSame(sqlTypeSide(), s.typeSide);
    }

    @Test
    void turnsEachCollageEquationIntoATriple() {
      assertEquals(Set.of(SAME_COMPANY, NAMED_AFTER_DEPT), new HashSet<>(org().eqs));
    }

    @Test
    void indexesAttributesAndForeignKeysByEntity() {
      Schema<String, String, Sym, Fk, Att> s = org();
      assertEquals(List.of(NAME), new ArrayList<>(s.attsFrom(EMP)));
      assertEquals(Set.of(WORKS_IN, MANAGES), new HashSet<>(s.fksFrom(EMP)));
      assertEquals(Set.of(WORKS_IN, MANAGES), new HashSet<>(s.fksTo(DEPT)));
      assertEquals(List.of(PART_OF), new ArrayList<>(s.fksTo(CO)));
      assertTrue(s.fksFrom(CO).isEmpty());
      assertTrue(s.attsFrom(CO).isEmpty());
    }

    @Test
    void sizeCountsEntitiesAttributesForeignKeysAndEquations() {
      assertEquals(3 + 2 + 3 + 2, org().size());
    }

    @Test
    void isASchema() {
      assertEquals(Kind.SCHEMA, org().kind());
    }

    @Test
    void theUnitSchemaHasOneEntityNamedByTheEmptyString() {
      Schema<String, String, Sym, Fk, Att> u = Schema.unit(sqlTypeSide());
      assertEquals(List.of(""), new ArrayList<>(u.ens));
      assertTrue(u.atts.isEmpty());
      assertTrue(u.fks.isEmpty());
      assertTrue(u.eqs.isEmpty());
      assertSame(sqlTypeSide().semantics(), u.dp);
    }

    @Test
    void theTerminalSchemaHasNoEntities() {
      Schema<String, String, Sym, Fk, Att> t = Schema.terminal(sqlTypeSide());
      assertTrue(t.ens.isEmpty());
      assertEquals(0, t.size());
    }

    @Test
    void discretizingKeepsOnlyTheGivenEntities() {
      Schema<String, String, Sym, Fk, Att> s = org();
      var d = s.discretize(Set.of(EMP));
      assertEquals(Set.of(EMP), new HashSet<>(d.ens));
      assertTrue(d.atts.isEmpty());
      assertTrue(d.fks.isEmpty());
      assertTrue(d.eqs.isEmpty());
      assertSame(s.typeSide, d.typeSide);
      assertSame(s.dp, d.dp);
    }
  }

  @Nested
  class Validation {

    private final List<String> ens = List.of(EMP, DEPT, CO);

    @Test
    void acceptsAWellFormedSchema() {
      assertDoesNotThrow(() -> org().validate(false));
    }

    @Test
    void rejectsAnAttributeOfAnUndeclaredType() {
      RuntimeException e =
          assertThrows(
              RuntimeException.class,
              () -> raw(ens, Map.of(NAME, new Pair<>(EMP, "Ghost")), Map.of(), List.of(), false));
      assertEquals("On attribute name, the target type Ghost is not declared.", e.getMessage());
    }

    @Test
    void anAttributeOnAnUndeclaredEntityFailsBeforeValidation() {
      // The constructor indexes attributes by entity before it validates, so the lookup of an
      // undeclared entity returns null and validate's "source entity" message is never reached.
      assertThrows(
          NullPointerException.class,
          () -> raw(ens, Map.of(NAME, new Pair<>("Ghost", VARCHAR)), Map.of(), List.of(), false));
    }

    @Test
    void aForeignKeyIntoAnUndeclaredEntityFailsBeforeValidation() {
      assertThrows(
          NullPointerException.class,
          () -> raw(ens, Map.of(), Map.of(WORKS_IN, new Pair<>(EMP, "Ghost")), List.of(), false));
    }

    @Test
    void aForeignKeyFromAnUndeclaredEntityFailsBeforeValidation() {
      assertThrows(
          NullPointerException.class,
          () -> raw(ens, Map.of(), Map.of(WORKS_IN, new Pair<>("Ghost", DEPT)), List.of(), false));
    }

    @Test
    void rejectsAnEquationOverAnUndeclaredEntity() {
      RuntimeException e =
          assertThrows(
              RuntimeException.class,
              () -> raw(ens, Map.of(), Map.of(), List.of(eq("x", "Ghost", v("x"), v("x"))), false));
      assertEquals(
          "In schema equation forall x:Ghost, x = x, context sort Ghost is not a declared entity.",
          e.getMessage());
    }

    @Test
    void rejectsAnEquationWhoseSidesHaveDifferentSorts() {
      RuntimeException e =
          assertThrows(
              RuntimeException.class,
              () ->
                  raw(
                      ens,
                      varchars(ORG_ATTS),
                      Map.of(),
                      List.of(eq("e", EMP, att(NAME, v("e")), v("e"))),
                      false));
      assertEquals(
          "In schema equation forall e:Employee, e.name = e : lhs sort is Varchar but rhs sort is"
              + " Employee",
          e.getMessage());
    }

    @Test
    void rejectsAnIllTypedEquation() {
      // label is an attribute of Department, not of Employee.
      RuntimeException e =
          assertThrows(
              RuntimeException.class,
              () ->
                  raw(
                      ens,
                      varchars(ORG_ATTS),
                      Map.of(),
                      List.of(eq("e", EMP, att(LABEL, v("e")), att(LABEL, v("e")))),
                      false));
      assertTrue(
          e.getMessage().startsWith("In schema equation forall e:Employee, e.label = e.label : "),
          e.getMessage());
    }

    @Test
    void checkJavaDoesNotCheckSchemaEquations() {
      // The java checks in validate are commented out, so an equation of java type is accepted
      // even with checkJava on. TypeSide rejects the same thing (TypeSideTest.JavaValidation).
      assertDoesNotThrow(
          () ->
              raw(
                  ens,
                  varchars(ORG_ATTS),
                  Map.of(),
                  List.of(eq("e", EMP, att(NAME, v("e")), str("k"))),
                  true));
    }
  }

  @Nested
  class Typing {

    @Test
    void typesAPathByItsTargetEntity() {
      assertEquals(
          Chc.inRight(CO), plain().type(new Pair<>("e", EMP), fk(PART_OF, fk(WORKS_IN, v("e")))));
    }

    @Test
    void typesAnAttributeByItsType() {
      assertEquals(
          Chc.inLeft(VARCHAR),
          plain().type(new Pair<>("e", EMP), att(LABEL, fk(WORKS_IN, v("e")))));
    }
  }

  @Nested
  class Acyclicity {

    @Test
    void reportsNothingForAnAcyclicSchema() {
      assertNull(plain().acyclic());
    }

    @Test
    void reportsASelfLoop() {
      Schema<String, String, Sym, Fk, Att> tree =
          sqlSchema(List.of(NODE), Map.of(), Map.of(PARENT, new Pair<>(NODE, NODE)));
      assertTrue(
          tree.acyclic().startsWith("Adding dependency on parent causes circularity"),
          tree.acyclic());
    }

    @Test
    void reportsALongerCycle() {
      Fk there = Fk.Fk("A", "there");
      Fk back = Fk.Fk("B", "back");
      Schema<String, String, Sym, Fk, Att> cycle =
          sqlSchema(
              List.of("A", "B"),
              Map.of(),
              Map.of(there, new Pair<>("A", "B"), back, new Pair<>("B", "A")));
      assertTrue(cycle.acyclic().contains(" causes circularity "), cycle.acyclic());
    }
  }

  /** Paths are lists of foreign keys in the order they are applied. */
  @Nested
  class Paths {

    private final List<Fk> empty = List.of();
    private final List<Fk> worksIn = List.of(WORKS_IN);
    private final List<Fk> manages = List.of(MANAGES);
    private final List<Fk> worksInPartOf = List.of(WORKS_IN, PART_OF);
    private final List<Fk> managesPartOf = List.of(MANAGES, PART_OF);

    @Test
    void foldAppliesTheForeignKeysInOrder() {
      assertEquals(fk(PART_OF, fk(WORKS_IN, v("e"))), Schema.fold(worksInPartOf, v("e")));
    }

    @Test
    void prodExtendsAPathByEachForeignKeyOutOfItsEnd() {
      Schema<String, String, Sym, Fk, Att> s = plain();
      assertEquals(Set.of(worksIn, manages), new HashSet<>(s.prod(EMP, empty)));
      assertEquals(List.of(worksInPartOf), s.prod(EMP, worksIn));
      assertTrue(s.prod(CO, empty).isEmpty());
    }

    @Test
    void pathsFromListsEveryPathIncludingTheEmptyOne() {
      Schema<String, String, Sym, Fk, Att> s = plain();
      assertEquals(
          Set.of(empty, worksIn, manages, worksInPartOf, managesPartOf), s.pathsFrom(EMP));
      assertEquals(Set.of(empty), s.pathsFrom(CO));
    }

    @Test
    void maxPathsFromKeepsThePathsThatAreNotProperPrefixes() {
      Schema<String, String, Sym, Fk, Att> s = plain();
      assertEquals(Set.of(worksInPartOf, managesPartOf), s.maxPathsFrom(s.pathsFrom(EMP)));
    }

    @Test
    void elimMaxRemovesTheProperPrefixesOfOnePath() {
      Schema<String, String, Sym, Fk, Att> s = plain();
      assertEquals(
          Set.of(manages, worksInPartOf, managesPartOf),
          s.elimMax(worksInPartOf, s.pathsFrom(EMP)));
    }
  }

  @Nested
  class Truncation {

    @Test
    void keepsTheNamesWhenNotTruncating() {
      Schema<String, String, Sym, Fk, Att> s = plain();
      assertEquals("worksIn", s.truncate(Chc.inLeft(WORKS_IN), false));
      assertEquals("name", s.truncate(Chc.inRight(NAME), false));
      assertEquals(EMP, s.truncate(EMP, false));
    }

    @Test
    void numbersEachDistinctNameOncePerSchema() {
      Schema<String, String, Sym, Fk, Att> s = plain();
      assertEquals("s0", s.truncate(EMP, true));
      assertEquals("s1", s.truncate(Chc.inLeft(WORKS_IN), true));
      assertEquals("s0", s.truncate(EMP, true));
      assertEquals("s0", plain().truncate(DEPT, true), "a new schema starts again at s0");
    }
  }

  @Nested
  class Sql {

    @Test
    void createsOneTablePerEntityInAlphabeticalOrder() {
      var sql = plain().toSQL("p_", "int", "id", false, 255, "`", false, false);
      assertEquals(List.of(CO, DEPT, EMP), new ArrayList<>(sql.keySet()));
      assertEquals(
          List.of(
              "drop table if exists p_Department;",
              "create table `p_Department`(`id` int primary key, `partOf` int not null , `label`"
                  + " varchar(255))"),
          sql.get(DEPT).second);
      assertEquals(List.of(Chc.inLeft(PART_OF), Chc.inRight(LABEL)), sql.get(DEPT).first);
      assertTrue(sql.get(DEPT).third.isEmpty(), "foreign key constraints are not generated");
    }

    @Test
    void listsEveryForeignKeyAndAttributeOfAnEntity() {
      String create = plain().toSQL("", "int", "id", false, 255, "", false, false).get(EMP).second.get(1);
      assertTrue(create.startsWith("create table Employee(id int primary key, "), create);
      assertTrue(create.contains("worksIn int not null "), create);
      assertTrue(create.contains("manages int not null "), create);
      assertTrue(create.endsWith("name varchar(255))"), create);
    }

    @Test
    void hiveHasNoIdColumnAndNoNotNull() {
      assertEquals(
          "create table Department(partOf int, label varchar(255))",
          plain().toSQL("", "int", "id", false, 255, "", false, true).get(DEPT).second.get(1));
    }

    @Test
    void noIdColumnWithoutAnIdColumnName() {
      assertEquals(
          "create table Department(partOf int not null , label varchar(255))",
          plain().toSQL("", "int", null, false, 255, "", false, false).get(DEPT).second.get(1));
    }

    @Test
    void oracleMapsNumericToNumber() {
      Att budget = Att.Att(DEPT, "budget");
      Schema<String, String, Sym, Fk, Att> s =
          typedSqlSchema(List.of(DEPT), Map.of(budget, new Pair<>(DEPT, "Numeric")), Map.of(), List.of());
      assertEquals(
          "create table Department(budget Number)",
          s.toSQL("", "int", null, false, 255, "", true, false).get(DEPT).second.get(1));
      assertEquals(
          "create table Department(budget Numeric)",
          s.toSQL("", "int", null, false, 255, "", false, false).get(DEPT).second.get(1));
    }

    @Test
    void truncatingRenamesTheCreatedTableButNotTheDroppedOne() {
      // create uses truncate(en1, truncate), drop uses en1 itself.
      Schema<String, String, Sym, Fk, Att> s = plain();
      List<String> q = s.toSQL("", "int", "id", true, 255, "", false, false).get(DEPT).second;
      assertEquals("drop table if exists Department;", q.get(0));
      assertTrue(q.get(1).startsWith("create table " + s.truncate(DEPT, true) + "("), q.get(1));
    }

    @Test
    void indexesTheRequestedColumnsUnderTheirTruncatedNames() {
      Att bio = Att.Att(EMP, "bio");
      Schema<String, String, Sym, Fk, Att> s =
          typedSqlSchema(
              List.of(EMP, DEPT),
              Map.of(NAME, new Pair<>(EMP, VARCHAR), bio, new Pair<>(EMP, "Text")),
              Map.of(WORKS_IN, new Pair<>(EMP, DEPT)),
              List.of());
      Map<String, List<String>> idx =
          s.toSQL_srcIdxs(new Pair<>(List.of(WORKS_IN), List.of(NAME, bio)));
      String table = s.truncate(EMP, true);
      // bio is a Text column, which cannot be indexed, so it is skipped.
      assertEquals(
          List.of(
              "create index idx0 on " + table + "(" + s.truncate(Chc.inLeft(WORKS_IN), true) + ")",
              "create index idx1 on " + table + "(" + s.truncate(Chc.inRight(NAME), true) + ")"),
          idx.get(EMP));
      assertTrue(idx.get(DEPT).isEmpty());
    }

    @Test
    void quickSqlDeclaresEveryAttributeAsVarchar() {
      Att headcount = Att.Att(DEPT, "headcount");
      Schema<String, String, Sym, Fk, Att> s =
          typedSqlSchema(
              List.of(DEPT, EMP),
              Map.of(headcount, new Pair<>(DEPT, "Integer"), NAME, new Pair<>(EMP, VARCHAR)),
              Map.of(),
              List.of());
      assertEquals(
          "CREATE TABLE Department([headcount] Varchar);\nCREATE TABLE Employee([name] Varchar);\n",
          s.toSqlQuick());
    }
  }

  @Nested
  class CheckerTptp {

    @Test
    void declaresEachEntityAsATypeAndAPredicateOfTheSameName() {
      Schema<String, String, Sym, Fk, Att> s =
          sqlSchema(List.of(EMP), Map.of(NAME, EMP), Map.of());
      String e = Schema.conv(EMP);
      String a = Schema.conv0(NAME);
      assertEquals(
          Set.of(
              "tff(" + e + "_entity, type, " + e + ": $tType).",
              "tff(" + e + "_pred, type, " + e + ": " + e + " > $o).",
              "tff(" + a + "_att, type, " + a + ": " + e + " > Varchar)."),
          Arrays.stream(s.toCheckerTpTp().split("\n")).collect(Collectors.toSet()));
    }

    @Test
    void rejectsForeignKeysAndEquations() {
      RuntimeException e = assertThrows(RuntimeException.class, () -> plain().toCheckerTpTp());
      assertEquals("Anomaly: please report", e.getMessage());
    }

    @Test
    void conversionNamesAreStableAndDistinct() {
      // The names come from static counters shared by every schema, so only their shape is fixed.
      assertEquals(Schema.conv(EMP), Schema.conv(EMP));
      assertNotEquals(Schema.conv(EMP), Schema.conv(DEPT));
      assertTrue(Schema.conv(EMP).startsWith("en"), Schema.conv(EMP));
      assertTrue(Schema.conv0(NAME).startsWith("att"), Schema.conv0(NAME));
    }
  }

  @Nested
  class ToString {

    @Test
    void listsEachSection() {
      String s = org().toString();
      assertTrue(s.startsWith("entities\n\tCompany Department Employee\nforeign_keys\n\t"), s);
      assertTrue(s.contains("worksIn : Employee -> Department"), s);
      assertTrue(s.contains("partOf : Department -> Company"), s);
      assertTrue(
          s.contains("\npath_equations\n\tEmployee.worksIn.partOf = Employee.manages.partOf"), s);
      assertTrue(s.contains("\nattributes\n\t"), s);
      assertTrue(s.contains("name : Employee -> Varchar"), s);
      assertTrue(
          s.endsWith("\nobservation_equations\n\tforall e:Employee. e.name = e.worksIn.label"), s);
    }

    @Test
    void putsTheSectionsInOrder() {
      String s = org().toString();
      int fks = s.indexOf("foreign_keys");
      int paths = s.indexOf("path_equations");
      int atts = s.indexOf("\nattributes");
      int obs = s.indexOf("observation_equations");
      assertTrue(0 < fks && fks < paths && paths < atts && atts < obs, s);
    }

    @Test
    void quotesNamesThatAreNotAlphanumeric() {
      String s = sqlSchema(List.of("Big Co", "9lives"), Map.of(), Map.of()).toString();
      assertEquals("entities\n\t\"9lives\" \"Big Co\"", s);
    }

    @Test
    void anEmptySchemaPrintsNothing() {
      assertEquals("", Schema.terminal(sqlTypeSide()).toString());
    }

    @Test
    void isComputedOnce() {
      Schema<String, String, Sym, Fk, Att> s = org();
      assertSame(s.toString(), s.toString());
    }
  }

  /** The collage view of a schema, over a small typeside so that both have one equation. */
  @Nested
  class Collages {

    private static final String STR = "Str";

    /** Str with concat, empty and forall x. concat(x, empty) = x; C -g-> C with c : C -> Str. */
    private Schema<String, String, String, String, String> small() {
      Map<String, Pair<List<String>, String>> syms =
          Map.of("concat", new Pair<>(List.of(STR, STR), STR), "empty", new Pair<>(List.of(), STR));
      Term<String, Void, String, Void, Void, Void, Void> x = Term.Var("x");
      Term<String, Void, String, Void, Void, Void, Void> concat =
          Term.Sym("concat", List.of(x, Term.Sym("empty", List.of())));
      TypeSide<String, String> ts =
          new TypeSide<>(
              Set.of(STR),
              syms,
              Set.of(new Triple<>(Map.of("x", STR), concat, x)),
              new AqlJs<>(ops(), syms, Map.of(STR, "java.lang.String"), Map.of(STR, "x => x"), Map.of()),
              syntactic(),
              ops());
      Term<String, String, String, String, String, Void, Void> y = Term.Var("y");
      Term<String, String, String, String, String, Void, Void> ygc = Term.Att("c", Term.Fk("g", y));
      Term<String, String, String, String, String, Void, Void> ycK =
          Term.Sym("concat", List.of(Term.Att("c", y), Term.Obj("k", STR)));
      return new Schema<>(
          ts,
          Set.of("C"),
          Map.of("c", new Pair<>("C", STR)),
          Map.of("g", new Pair<>("C", "C")),
          Set.of(new Triple<>(new Pair<>("y", "C"), ygc, ycK)),
          syntactic(),
          false);
    }

    @Test
    void sharesTheSchemasComponents() {
      Schema<String, String, Sym, Fk, Att> s = org();
      Collage<String, String, Sym, Fk, Att, Void, Void> col = s.collage();
      assertSame(s.typeSide.tys, col.tys());
      assertSame(s.typeSide.syms, col.syms());
      assertSame(s.typeSide.js.java_tys, col.java_tys());
      assertSame(s.typeSide.js.java_parsers, col.java_parsers());
      assertSame(s.typeSide.js.java_fns, col.java_fns());
      assertSame(s.ens, col.getEns());
      assertSame(s.atts, col.atts());
      assertSame(s.fks, col.fks());
      assertTrue(col.gens().isEmpty());
      assertTrue(col.sks().isEmpty());
    }

    @Test
    void iteratesTheSchemaEquationsAndThenTheTypesideOnes() {
      Collage<String, String, String, String, String, Void, Void> col = small().collage();
      List<Map<String, Chc<String, String>>> contexts = new ArrayList<>();
      for (Eq<String, String, String, String, String, Void, Void> e : col.eqs()) {
        contexts.add(e.ctx);
      }
      assertEquals(List.of(Map.of("y", Chc.inRight("C")), Map.of("x", Chc.inLeft(STR))), contexts);
    }

    @Test
    void sizeOfItsEquationsCountsOnlyTheSchemaOnes() {
      // size() returns the number of schema equations, while the iterator also yields the
      // typeside's, so the collection reports 1 element and iterates 2.
      Collage<String, String, String, String, String, Void, Void> col = small().collage();
      Collection<Eq<String, String, String, String, String, Void, Void>> eqs = col.eqs();
      assertEquals(1, eqs.size());
      assertEquals(2, new ArrayList<>(eqs).size());
    }

    @Test
    void exportsToCoq() {
      List<String> lines =
          Arrays.stream(small().toCoq().split("\n")).filter(l -> !l.isEmpty()).collect(Collectors.toList());
      assertEquals(
          Set.of(
              "Parameter Str : Type.",
              "Parameter concat : Str * Str -> Str.",
              "Parameter empty :  Str.",
              "Axiom ax0 : forall (x : Str),  concat(x, empty) = x.",
              "Parameter k : Str.",
              "Parameter C : Set.",
              "Parameter C_g : C -> C.",
              "Parameter C_c : C -> Str.",
              "Axiom ax1 : forall (y : C),  C_c(C_g(y)) = concat(C_c(y), k)."),
          new HashSet<>(lines));
      assertEquals(9, lines.size(), "every declaration appears once");
    }

    @Test
    void exportsAnOverloadedSymbolOncePerOverload() {
      // Issue 12 of the open-issues log applies to Schema.toCoq as well.
      long plus =
          Arrays.stream(plain().toCoq().split("\n")).filter(l -> l.startsWith("Parameter plus :")).count();
      assertEquals(9, plus);
    }
  }

  @Nested
  class EqualsAndHashCode {

    @Test
    void equalSchemasFromCollagesHaveDifferentHashCodes() {
      // The collage constructor stores its equations in Collections.synchronizedCollection, whose
      // hashCode is Object's. equals compares the equations element by element, so the two
      // schemas are equal, but hashCode hashes the wrapper and tells them apart.
      Schema<String, String, Sym, Fk, Att> a = plain();
      Schema<String, String, Sym, Fk, Att> b = plain();
      assertEquals(a, b);
      assertNotEquals(a.hashCode(), b.hashCode());
      assertEquals(2, new HashSet<>(List.of(a, b)).size(), "a hash set keeps both copies");
    }

    @Test
    void equalSchemasWithSetsOfEquationsAgreeOnHashCode() {
      Schema<String, String, Sym, Fk, Att> a =
          raw(List.of(EMP, DEPT, CO), varchars(ORG_ATTS), ORG_FKS, Set.of(SAME_COMPANY), false);
      Schema<String, String, Sym, Fk, Att> b =
          raw(List.of(EMP, DEPT, CO), varchars(ORG_ATTS), ORG_FKS, Set.of(SAME_COMPANY), false);
      assertEquals(a, b);
      assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void theOrderOfTheEquationsMatters() {
      // collectionEq walks both collections in step, so the same two equations listed in a
      // different order compare unequal.
      Schema<String, String, Sym, Fk, Att> a =
          raw(
              List.of(EMP, DEPT, CO),
              varchars(ORG_ATTS),
              ORG_FKS,
              List.of(SAME_COMPANY, NAMED_AFTER_DEPT),
              false);
      Schema<String, String, Sym, Fk, Att> b =
          raw(
              List.of(EMP, DEPT, CO),
              varchars(ORG_ATTS),
              ORG_FKS,
              List.of(NAMED_AFTER_DEPT, SAME_COMPANY),
              false);
      assertNotEquals(a, b);
    }

    @Test
    void isReflexiveAndRejectsNull() {
      Schema<String, String, Sym, Fk, Att> s = plain();
      assertTrue(s.equals(s));
      assertFalse(s.equals(null));
    }

    @Test
    void throwsOnAForeignType() {
      Schema<String, String, Sym, Fk, Att> s = plain();
      assertThrows(ClassCastException.class, () -> s.equals("not a schema"));
    }

    @Test
    void distinguishesOnEveryComponent() {
      List<String> ens = List.of(EMP, DEPT, CO);
      Schema<String, String, Sym, Fk, Att> base =
          raw(ens, varchars(ORG_ATTS), ORG_FKS, Set.of(), false);
      List<Schema<String, String, Sym, Fk, Att>> others =
          List.of(
              raw(List.of(EMP, DEPT, CO, "Extra"), varchars(ORG_ATTS), ORG_FKS, Set.of(), false),
              raw(ens, varchars(ORG_ATTS), Map.of(WORKS_IN, new Pair<>(EMP, DEPT)), Set.of(), false),
              raw(ens, varchars(Map.of(NAME, EMP)), ORG_FKS, Set.of(), false),
              raw(ens, varchars(ORG_ATTS), ORG_FKS, Set.of(SAME_COMPANY), false));
      for (Schema<String, String, Sym, Fk, Att> other : others) {
        assertNotEquals(base, other);
      }
    }

    @Test
    void distinguishesOnTheTypeSide() {
      TypeSide<String, Sym> initial = TypeSide.initial(ops());
      Schema<String, String, Sym, Fk, Att> overSql = raw(List.of(EMP), Map.of(), Map.of(), Set.of(), false);
      Schema<String, String, Sym, Fk, Att> overInitial =
          new Schema<>(initial, List.of(EMP), Map.of(), Map.of(), Set.of(), syntactic(), false);
      assertNotEquals(overSql, overInitial);
    }
  }
}
