package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.Chc;
import catdata.LineException;
import catdata.Pair;
import catdata.Unit;
import catdata.cql.Constraints;
import catdata.cql.ED;
import catdata.cql.Kind;
import catdata.cql.exp.EdsExp.EdsExpVisitor;
import catdata.cql.exp.InstExp.InstExpVar;
import catdata.cql.exp.SchExp.SchExpVar;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link EdsExpFromMsCatalog}, {@code constraints C = from_ms_catalog I : S}, which
 * reads the primary keys, unique keys and foreign keys of a SQL Server catalog, imported as the
 * instance {@code I}, as constraints on the schema {@code S}. The catalog is read by entity and
 * attribute name: {@code TABLE_CONSTRAINTS}, {@code KEY_COLUMN_USAGE} and {@code
 * REFERENTIAL_CONSTRAINTS}, as {@code INFORMATION_SCHEMA} names them.
 */
class EdsExpFromMsCatalogTest {

  /** The three catalog views the expression reads, imported from a database. */
  private static String catalog(String url, String from, String where) {
    return """
        typeside Ty = sql
        instance I = import_jdbc "%1$s" : Ty {
          COLUMNS -> "select * from %2$sCOLUMNS %3$s"
          TABLE_CONSTRAINTS -> "select * from %2$sTABLE_CONSTRAINTS %3$s"
          KEY_COLUMN_USAGE -> "select * from %2$sKEY_COLUMN_USAGE %3$s"
          REFERENTIAL_CONSTRAINTS -> "select * from %2$sREFERENTIAL_CONSTRAINTS %4$s"
        }
        """
        .formatted(url, from, where, where.replace("TABLE_SCHEMA", "CONSTRAINT_SCHEMA"));
  }

  /** A database whose own catalog has a primary key, a unique key and a foreign key. */
  private static String h2Catalog() throws Exception {
    String url =
        ExpTestHelpers.h2(
            "create table P(id int primary key, code varchar(5) not null unique, note varchar(9))",
            "create table C(cid int primary key, pid int references P(id), v int check (v > 0))");
    return catalog(url, "INFORMATION_SCHEMA.", "where TABLE_SCHEMA = 'PUBLIC'")
        + """
        schema S = literal : Ty {
          entities PUBLIC_P PUBLIC_C
          attributes
            ID : PUBLIC_P -> Integer  CODE : PUBLIC_P -> Varchar  NOTE : PUBLIC_P -> Varchar
            CID : PUBLIC_C -> Integer  PID : PUBLIC_C -> Integer  V : PUBLIC_C -> Integer
        }
        constraints Cs = from_ms_catalog I : S
        """;
  }

  /**
   * A catalog written by hand: tables named after the views, with the rows given. Each row of
   * {@code keys} is {@code constraint, table, column, position}, in the schema {@code dbo}.
   */
  private static String handCatalog(
      List<String> constraints, List<String> keys, List<String> references) throws Exception {
    List<String> sql = new ArrayList<>();
    sql.add("create table COLUMNS(TABLE_SCHEMA varchar, TABLE_NAME varchar, COLUMN_NAME varchar)");
    sql.add(
        "create table TABLE_CONSTRAINTS(TABLE_SCHEMA varchar, TABLE_NAME varchar,"
            + " CONSTRAINT_NAME varchar, CONSTRAINT_TYPE varchar)");
    sql.add(
        "create table KEY_COLUMN_USAGE(TABLE_SCHEMA varchar, TABLE_NAME varchar,"
            + " CONSTRAINT_NAME varchar, COLUMN_NAME varchar, ORDINAL_POSITION int)");
    sql.add(
        "create table REFERENTIAL_CONSTRAINTS(CONSTRAINT_SCHEMA varchar, CONSTRAINT_NAME varchar,"
            + " UNIQUE_CONSTRAINT_SCHEMA varchar, UNIQUE_CONSTRAINT_NAME varchar)");
    for (String c : constraints) {
      sql.add("insert into TABLE_CONSTRAINTS values (" + c + ")");
    }
    for (String k : keys) {
      sql.add("insert into KEY_COLUMN_USAGE values (" + k + ")");
    }
    for (String r : references) {
      sql.add("insert into REFERENTIAL_CONSTRAINTS values (" + r + ")");
    }
    return catalog(ExpTestHelpers.h2(sql.toArray(new String[0])), "", "");
  }

  private static final String SCHEMA =
      """
      schema S = literal : Ty {
        entities P C
        attributes
          x y : P -> Integer
          a b : C -> Integer
      }
      constraints Cs = from_ms_catalog I : S
      """;

  /** The constraints a program binds to Cs. */
  private static Constraints constraints(String program) throws Exception {
    return ExpTestHelpers.eval(program, "Cs", Kind.CONSTRAINTS);
  }

  /** The constraints that have an exists part, which are the foreign keys. */
  private static List<ED> foreignKeys(Constraints c) {
    return c.eds.stream().filter(ed -> !ed.Es.isEmpty()).toList();
  }

  /** The equations of an ED's exists part, printed. */
  private static Set<String> ewh(ED ed) {
    Set<String> ret = new TreeSet<>();
    ed.Ewh.forEach(p -> ret.add(p.first + " = " + p.second));
    return ret;
  }

  @Nested
  class Evaluation {

    @Test
    void readsTheKeysOfADatabaseCatalog() throws Exception {
      Constraints c = constraints(h2Catalog());
      assertEquals(4, c.eds.size(), c.toString());
      List<ED> fks = foreignKeys(c);
      assertEquals(1, fks.size());
      ED fk = fks.get(0);
      assertEquals(Set.of("s"), fk.As.keySet());
      assertEquals(Set.of("t"), fk.Es.keySet());
      assertEquals(Set.of("s.PID = t.ID"), ewh(fk));
      Set<String> keys = new TreeSet<>();
      for (ED ed : c.eds) {
        if (ed.Es.isEmpty()) {
          assertEquals(Set.of("x = y"), ewh(ed));
          ed.Awh.forEach(p -> keys.add(p.first.toString()));
        }
      }
      assertEquals(Set.of("x.CID", "x.CODE", "x.ID"), keys);
    }

    /**
     * Without sql_constraints_simple, the keys compare with eq and a primary key column must not be
     * null, but the NOT NULL of CODE is not imported.
     */
    @Test
    void importsNotNullOnlyForPrimaryKeyColumns() throws Exception {
      Constraints c = constraints("options sql_constraints_simple = false\n" + h2Catalog());
      assertEquals(6, c.eds.size(), c.toString());
      Set<String> notNull = new TreeSet<>();
      for (ED ed : c.eds) {
        for (String eq : ewh(ed)) {
          if (eq.startsWith("isNull(")) {
            notNull.add(eq);
          }
        }
      }
      assertEquals(Set.of("isNull(v.CID) = false", "isNull(v.ID) = false"), notNull);
      ED fk = foreignKeys(c).get(0);
      assertEquals(Set.of("(s.PID eq t.ID) = true"), ewh(fk));
    }

    @Test
    void namesTablesOfTheSchemaDboWithoutAPrefix() throws Exception {
      Constraints c =
          constraints(
              handCatalog(
                      List.of("'dbo', 'P', 'PK', 'PRIMARY KEY'"),
                      List.of("'dbo', 'P', 'PK', 'x', 1"),
                      List.of())
                  + SCHEMA);
      assertEquals(1, c.eds.size());
      assertEquals(Map.of("x", Chc.inRight("P"), "y", Chc.inRight("P")), c.eds.get(0).As);
    }

    @Test
    void matchesTheColumnsOfACompositeForeignKeyByPosition() throws Exception {
      Constraints c =
          constraints(
              handCatalog(
                      List.of("'dbo', 'P', 'PK', 'PRIMARY KEY'", "'dbo', 'C', 'FK', 'FOREIGN KEY'"),
                      List.of(
                          "'dbo', 'P', 'PK', 'y', 2",
                          "'dbo', 'P', 'PK', 'x', 1",
                          "'dbo', 'C', 'FK', 'a', 1",
                          "'dbo', 'C', 'FK', 'b', 2"),
                      List.of("'dbo', 'FK', 'dbo', 'PK'"))
                  + SCHEMA);
      assertEquals(Set.of("s.a = t.x", "s.b = t.y"), ewh(foreignKeys(c).get(0)));
    }

    @Test
    void ignoresCheckConstraints() throws Exception {
      Constraints c =
          constraints(
              handCatalog(
                      List.of("'dbo', 'C', 'CK', 'check'"),
                      List.of("'dbo', 'C', 'CK', 'a', 1"),
                      List.of())
                  + SCHEMA);
      assertTrue(c.eds.isEmpty(), c.toString());
    }

    @Test
    void skipsAForeignKeyWithoutColumns() throws Exception {
      Constraints c =
          constraints(
              handCatalog(
                      List.of("'dbo', 'P', 'PK', 'unique'", "'dbo', 'C', 'FK', 'FOREIGN KEY'"),
                      List.of("'dbo', 'P', 'PK', 'x', 1"),
                      List.of("'dbo', 'FK', 'dbo', 'PK'"))
                  + SCHEMA);
      assertEquals(1, c.eds.size(), c.toString());
      assertTrue(foreignKeys(c).isEmpty());
    }

    @Test
    void rejectsAnUnknownConstraintType() throws Exception {
      String msg =
          ExpTestHelpers.error(
              handCatalog(List.of("'dbo', 'P', 'D', 'DEFAULT'"), List.of(), List.of()) + SCHEMA);
      assertTrue(msg.contains("Anomaly: please report"), msg);
    }

    @Test
    void rejectsAReferenceFromAnUnknownConstraint() throws Exception {
      String msg =
          ExpTestHelpers.error(
              handCatalog(
                      List.of("'dbo', 'P', 'PK', 'PRIMARY KEY'"),
                      List.of("'dbo', 'P', 'PK', 'x', 1"),
                      List.of("'dbo', 'FK', 'dbo', 'PK'"))
                  + SCHEMA);
      assertTrue(msg.contains("Anomaly: please report"), msg);
    }

    @Test
    void rejectsAReferenceToAnUnknownConstraint() throws Exception {
      String msg =
          ExpTestHelpers.error(
              handCatalog(
                      List.of("'dbo', 'C', 'FK', 'FOREIGN KEY'"),
                      List.of("'dbo', 'C', 'FK', 'a', 1"),
                      List.of("'dbo', 'FK', 'dbo', 'PK'"))
                  + SCHEMA);
      assertTrue(msg.contains("Anomaly: please report"), msg);
    }

    @Test
    void rejectsAKeyOnAColumnTheSchemaLacks() throws Exception {
      String msg =
          ExpTestHelpers.error(
              handCatalog(
                      List.of("'dbo', 'P', 'PK', 'PRIMARY KEY'"),
                      List.of("'dbo', 'P', 'PK', 'z', 1"),
                      List.of())
                  + SCHEMA);
      assertTrue(msg.contains("Cannot find P z in "), msg);
    }

    /** The catalog must have a COLUMNS entity, although only its size is read. */
    @Test
    void needsAColumnsEntity() throws Exception {
      String url = ExpTestHelpers.h2("create table P(id int primary key)");
      String program =
          catalog(url, "INFORMATION_SCHEMA.", "where TABLE_SCHEMA = 'PUBLIC'")
                  .replaceAll("(?m)^ *COLUMNS -> .*\\n", "")
              + SCHEMA;
      assertFalse(program.contains("COLUMNS ->"), program);
      String msg = ExpTestHelpers.error(program);
      assertTrue(
          msg.contains("Cannot invoke \"java.util.Map.keySet()\" because the return value of"),
          msg);
    }

    /** The rows of the catalog must be strings, which a literal instance's are not. */
    @Test
    void rejectsACatalogWhoseRowsAreNotStrings() throws Exception {
      String msg =
          ExpTestHelpers.error(
              """
              typeside Ty = sql
              schema MsCat = ms_catalog Ty Varchar
              instance I = literal : MsCat {
                generators c : TABLE_CONSTRAINTS
                equations c.TABLE_NAME = P c.TABLE_SCHEMA = dbo c.CONSTRAINT_NAME = PK
                  c.CONSTRAINT_TYPE = UNIQUE
              }
              """
                  + SCHEMA);
      assertTrue(msg.contains("java.lang.Integer cannot be cast to class java.lang.String"), msg);
    }

    /** The expression reads sql_constraints_simple but does not allow it. */
    @Test
    void doesNotAllowTheOptionsItReads() throws Exception {
      String program =
          handCatalog(List.of(), List.of(), List.of())
              + SCHEMA.replace(
                  "from_ms_catalog I : S",
                  "from_ms_catalog I : S { options sql_constraints_simple = false }");
      LineException e = assertThrows(LineException.class, () -> ExpTestHelpers.run(program));
      assertTrue(
          e.getMessage().startsWith("Option not allowed: sql_constraints_simple."), e.getMessage());
    }
  }

  @Nested
  class Expression {

    private final EdsExpFromMsCatalog exp =
        new EdsExpFromMsCatalog(new InstExpVar("I"), new SchExpVar("S"), List.of());

    @Test
    void rejectsAMissingInstanceOrSchema() {
      SchExp s = new SchExpVar("S");
      InstExp<?, ?, ?, ?> i = new InstExpVar("I");
      List<Pair<String, String>> none = List.of();
      assertThrows(RuntimeException.class, () -> new EdsExpFromMsCatalog(null, s, none));
      assertThrows(RuntimeException.class, () -> new EdsExpFromMsCatalog(i, null, none));
    }

    @Test
    void printsAsItsKeyword() {
      assertEquals("from_ms_catalog I : S", exp.toString());
    }

    @Test
    void dependsOnTheInstanceAndTheSchema() {
      assertEquals(
          Set.of(new Pair<>("I", Kind.INSTANCE), new Pair<>("S", Kind.SCHEMA)),
          Set.copyOf(exp.deps()));
    }

    @Test
    void keepsItsOptionsButAllowsOnlyTheCommonOnes() {
      EdsExpFromMsCatalog e =
          new EdsExpFromMsCatalog(
              new InstExpVar("I"), new SchExpVar("S"), List.of(new Pair<>("timeout", "5")));
      assertEquals(Map.of("timeout", "5"), e.options());
      assertEquals(ExpTestHelpers.BASE_OPTIONS, e.allowedOptions());
    }

    @Test
    void isTypedByItsSchema() throws Exception {
      AqlTyping g = ExpTestHelpers.typing(handCatalog(List.of(), List.of(), List.of()) + SCHEMA);
      assertEquals(new SchExpVar("S"), g.defs.eds.get("Cs"));
      assertEquals(new SchExpVar("S"), exp.type(g));
    }

    @Test
    void mapsOverTheSubExpressionsOfItsParts() {
      List<Exp<?>> seen = new ArrayList<>();
      exp.map(seen::add);
      assertEquals(List.of(exp), seen);
    }

    @Test
    void equalityComparesTheInstanceTheSchemaAndTheOptions() {
      List<Pair<String, String>> ops = List.of(new Pair<>("timeout", "5"));
      EdsExpFromMsCatalog same =
          new EdsExpFromMsCatalog(new InstExpVar("I"), new SchExpVar("S"), List.of());
      assertEquals(exp, exp);
      assertEquals(exp, same);
      assertEquals(exp.hashCode(), same.hashCode());
      assertNotEquals(
          exp, new EdsExpFromMsCatalog(new InstExpVar("J"), new SchExpVar("S"), List.of()));
      assertNotEquals(
          exp, new EdsExpFromMsCatalog(new InstExpVar("I"), new SchExpVar("T"), List.of()));
      assertNotEquals(exp, new EdsExpFromMsCatalog(new InstExpVar("I"), new SchExpVar("S"), ops));
      assertNotEquals(exp, null);
      assertNotEquals(exp, "from_ms_catalog I : S");
    }

    @Test
    void equalityHandlesMissingParts() {
      EdsExpFromMsCatalog none =
          new EdsExpFromMsCatalog(new InstExpVar("I"), new SchExpVar("S"), List.of());
      none.I = null;
      none.S = null;
      none.options = null;
      EdsExpFromMsCatalog other =
          new EdsExpFromMsCatalog(new InstExpVar("I"), new SchExpVar("S"), List.of());
      other.I = null;
      other.S = null;
      other.options = null;
      assertEquals(none, other);
      assertEquals(none.hashCode(), other.hashCode());
      assertNotEquals(none, exp);
      assertNotEquals(exp, none);
      other.I = exp.I;
      assertNotEquals(none, other);
      other.I = null;
      other.options = Collections.emptyMap();
      assertNotEquals(none, other);
      assertNotEquals(other, exp);
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsAVisitor() {
      EdsExpVisitor<String, Unit, RuntimeException> v = mock(EdsExpVisitor.class);
      when(v.visit(Unit.unit, exp)).thenReturn("visited");
      assertEquals("visited", exp.accept(Unit.unit, v));
    }
  }
}
