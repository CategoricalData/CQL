package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.Pair;
import catdata.Unit;
import catdata.cql.Constraints;
import catdata.cql.ED;
import catdata.cql.Kind;
import catdata.cql.exp.EdsExp.EdsExpVisitor;
import catdata.cql.exp.SchExp.SchExpVar;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link EdsExpFromOracle}, {@code constraints C = from_oracle url : S}, which reads
 * the foreign, primary and unique keys of the current Oracle user from {@code USER_CONSTRAINTS} and
 * {@code USER_CONS_COLUMNS}. The tests give an H2 database tables with those names.
 */
class EdsExpFromOracleTest {

  /**
   * A database with the two dictionary views. Each constraint is {@code owner, table, name, type,
   * r_owner, r_name}, and each column {@code owner, table, constraint, column, position}.
   */
  private static String dictionary(List<String> constraints, List<String> columns)
      throws Exception {
    List<String> sql = new ArrayList<>();
    sql.add(
        "create table USER_CONSTRAINTS(OWNER varchar, TABLE_NAME varchar, CONSTRAINT_NAME varchar,"
            + " CONSTRAINT_TYPE varchar, R_OWNER varchar, R_CONSTRAINT_NAME varchar)");
    sql.add(
        "create table USER_CONS_COLUMNS(OWNER varchar, TABLE_NAME varchar, CONSTRAINT_NAME"
            + " varchar, COLUMN_NAME varchar, POSITION int)");
    constraints.forEach(c -> sql.add("insert into USER_CONSTRAINTS values (" + c + ")"));
    columns.forEach(c -> sql.add("insert into USER_CONS_COLUMNS values (" + c + ")"));
    return ExpTestHelpers.h2(sql.toArray(new String[0]));
  }

  private static String program(String url) {
    return """
        typeside Ty = sql
        schema S = literal : Ty {
          entities "U.P" "U.C"
          attributes
            ID ID2 : "U.P" -> Integer  CODE : "U.P" -> Varchar
            PID PID2 : "U.C" -> Integer
        }
        constraints Cs = from_oracle "%s" : S
        """
        .formatted(url);
  }

  private static final List<String> KEYS =
      List.of(
          "'U', 'P', 'PK_P', 'P', null, null",
          "'U', 'P', 'UQ_P', 'U', null, null",
          "'U', 'C', 'FK_C', 'R', 'U', 'PK_P'");

  private static final List<String> COLUMNS =
      List.of(
          "'U', 'P', 'PK_P', 'ID2', 2",
          "'U', 'P', 'PK_P', 'ID', 1",
          "'U', 'P', 'UQ_P', 'CODE', 1",
          "'U', 'C', 'FK_C', 'PID', 1",
          "'U', 'C', 'FK_C', 'PID2', 2");

  private static Set<String> equations(Set<? extends Pair<?, ?>> eqs) {
    Set<String> ret = new TreeSet<>();
    eqs.forEach(p -> ret.add(p.first + " = " + p.second));
    return ret;
  }

  @Nested
  class Evaluation {

    @Test
    void readsForeignPrimaryAndUniqueKeys() throws Exception {
      Constraints c =
          ExpTestHelpers.eval(program(dictionary(KEYS, COLUMNS)), "Cs", Kind.CONSTRAINTS);
      // the foreign key is exists unique, which Constraints splits in two
      assertEquals(4, c.eds.size(), c.toString());
      ED fk = c.eds.get(0);
      assertEquals(Set.of("s"), fk.As.keySet());
      assertEquals(Set.of("t"), fk.Es.keySet());
      assertEquals(Set.of("s.PID = t.ID", "s.PID2 = t.ID2"), equations(fk.Ewh));
      Set<String> keys = new TreeSet<>();
      for (ED ed : c.eds.subList(2, 4)) {
        assertEquals(Set.of("x = y"), equations(ed.Ewh));
        keys.add(equations(ed.Awh).toString());
      }
      assertEquals(Set.of("[x.CODE = y.CODE]", "[x.ID = y.ID, x.ID2 = y.ID2]"), keys);
    }

    @Test
    void rejectsAForeignKeyAcrossOwners() throws Exception {
      String msg =
          ExpTestHelpers.error(
              program(
                  dictionary(
                      List.of(
                          "'U', 'P', 'PK_P', 'P', null, null",
                          "'V', 'C', 'FK_C', 'R', 'U', 'PK_P'"),
                      List.of("'U', 'P', 'PK_P', 'ID', 1", "'V', 'C', 'FK_C', 'PID', 1"))));
      assertTrue(msg.contains("Different owner in oracle import- please report"), msg);
    }

    @Test
    void rejectsAForeignKeyFromATableTheSchemaLacks() throws Exception {
      String msg =
          ExpTestHelpers.error(
              program(
                  dictionary(
                      List.of(
                          "'U', 'P', 'PK_P', 'P', null, null",
                          "'U', 'X', 'FK_X', 'R', 'U', 'PK_P'"),
                      List.of("'U', 'P', 'PK_P', 'ID', 1", "'U', 'X', 'FK_X', 'PID', 1"))));
      assertTrue(msg.contains("Not found: U.X in "), msg);
    }

    @Test
    void rejectsAForeignKeyToATableTheSchemaLacks() throws Exception {
      String msg =
          ExpTestHelpers.error(
              program(
                  dictionary(
                      List.of(
                          "'U', 'X', 'PK_X', 'P', null, null",
                          "'U', 'C', 'FK_C', 'R', 'U', 'PK_X'"),
                      List.of("'U', 'X', 'PK_X', 'ID', 1", "'U', 'C', 'FK_C', 'PID', 1"))));
      assertTrue(msg.contains("Not found: U.X in "), msg);
    }

    @Test
    void rejectsAForeignKeyWithTheWrongNumberOfColumns() throws Exception {
      String msg =
          ExpTestHelpers.error(
              program(
                  dictionary(
                      List.of(
                          "'U', 'P', 'PK_P', 'P', null, null",
                          "'U', 'C', 'FK_C', 'R', 'U', 'PK_P'"),
                      List.of(
                          "'U', 'P', 'PK_P', 'ID', 1",
                          "'U', 'C', 'FK_C', 'PID', 1",
                          "'U', 'C', 'FK_C', 'PID2', 2"))));
      assertTrue(msg.contains("Anomaly: please report"), msg);
    }

    @Test
    void reportsADatabaseWithoutTheDictionary() throws Exception {
      String msg = ExpTestHelpers.error(program(ExpTestHelpers.h2()));
      assertTrue(msg.contains("Table \"USER_CONS_COLUMNS\" not found"), msg);
    }
  }

  @Nested
  class Expression {

    private final EdsExpFromOracle exp = new EdsExpFromOracle("url", new SchExpVar("S"));

    @Test
    void printsAsItsKeyword() {
      assertEquals("from_oracle url : S", exp.toString());
    }

    @Test
    void dependsOnTheSchema() {
      assertEquals(List.of(new Pair<>("S", Kind.SCHEMA)), new ArrayList<>(exp.deps()));
    }

    @Test
    void hasNoOptionsAndAllowsOnlyTheCommonOnes() {
      assertTrue(exp.options().isEmpty());
      assertEquals(ExpTestHelpers.BASE_OPTIONS, exp.allowedOptions());
    }

    @Test
    void isTypedByItsSchema() throws Exception {
      AqlTyping g = ExpTestHelpers.typing(program("url"));
      assertEquals(new SchExpVar("S"), g.defs.eds.get("Cs"));
      assertEquals(new SchExpVar("S"), exp.type(g));
    }

    @Test
    void mapSkipsTheSchemaItself() {
      List<Exp<?>> seen = new ArrayList<>();
      exp.map(seen::add);
      assertEquals(List.of(exp), seen);
    }

    @Test
    void equalityComparesTheUrlAndTheSchema() {
      assertEquals(exp, exp);
      assertEquals(exp, new EdsExpFromOracle("url", new SchExpVar("S")));
      assertEquals(exp.hashCode(), new EdsExpFromOracle("url", new SchExpVar("S")).hashCode());
      assertNotEquals(exp, new EdsExpFromOracle("url2", new SchExpVar("S")));
      assertNotEquals(exp, new EdsExpFromOracle("url", new SchExpVar("T")));
      assertNotEquals(exp, null);
      assertNotEquals(exp, "from_oracle url : S");
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
