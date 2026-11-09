package catdata.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.Pair;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SqlInstance} and {@link SqlForeignKey}. An instance reads every table of a
 * schema with {@code SELECT * FROM}, naming the table by the printed pair of its schema and name,
 * so the tables read here are the H2 tables named {@code (PUBLIC, P)}, a parent with the key ID,
 * {@code (PUBLIC, C)}, a child whose column PID refers to a parent, {@code (PUBLIC, D)}, two rows
 * with the same K, and {@code (PUBLIC, E)}, which is empty.
 */
class SqlInstanceTest {

  private static final SqlType INTEGER = SqlType.resolve(String.valueOf(Types.INTEGER));
  private static final SqlType VARCHAR = SqlType.resolve(String.valueOf(Types.VARCHAR));

  private Connection conn;
  private SqlTable p;
  private SqlTable c;
  private SqlTable d;
  private SqlTable e;

  @BeforeEach
  void open() throws Exception {
    conn = DriverManager.getConnection("jdbc:h2:mem:sqlinstancetest;DB_CLOSE_DELAY=-1");
    try (Statement s = conn.createStatement()) {
      s.execute("create table T (ID int primary key)");
      s.execute("create table \"(PUBLIC, P)\" (ID int primary key, N varchar(10))");
      s.execute("insert into \"(PUBLIC, P)\" values (1, 'a'), (2, null)");
      s.execute("create table \"(PUBLIC, C)\" (CID int primary key, PID int)");
      s.execute("insert into \"(PUBLIC, C)\" values (10, 1), (11, 3)");
      s.execute("create table \"(PUBLIC, D)\" (K int, V varchar(10))");
      s.execute("insert into \"(PUBLIC, D)\" values (1, 'x'), (1, 'y')");
      s.execute("create table \"(PUBLIC, E)\" (Z int)");
    }
    p = table("P", "ID", INTEGER, "N", VARCHAR);
    c = table("C", "CID", INTEGER, "PID", INTEGER);
    d = table("D", "K", INTEGER, "V", VARCHAR);
    e = table("E", "Z", INTEGER);
  }

  @AfterEach
  void close() throws Exception {
    try (Statement s = conn.createStatement()) {
      s.execute("shutdown");
    }
    conn.close();
  }

  /** A table of the schema PUBLIC whose key is its first column. */
  private static SqlTable table(String name, Object... columns) {
    SqlTable t = new SqlTable();
    t.name = new Pair<>("PUBLIC", name);
    for (int i = 0; i < columns.length; i += 2) {
      t.columns.add(new SqlColumn(t, (String) columns[i], (SqlType) columns[i + 1]));
    }
    t.pk.add(t.columns.get(0));
    return t;
  }

  /** The schema of the database, with its tables replaced by the given ones. */
  private SqlSchema schema(SqlTable... tables) throws Exception {
    SqlSchema s = new SqlSchema(conn.getMetaData(), "\"");
    s.tables.clear();
    s.tables.addAll(Set.of(tables));
    return s;
  }

  private SqlInstance instance() throws Exception {
    return new SqlInstance(schema(p, c, d, e), conn, false, true, "\"");
  }

  private static Map<SqlColumn, Optional<Object>> row(SqlTable t, Object... values) {
    Map<SqlColumn, Optional<Object>> row = new HashMap<>();
    for (int i = 0; i < values.length; i++) {
      row.put(t.columns.get(i), Optional.ofNullable(values[i]));
    }
    return row;
  }

  /** The foreign key from PID of C to the key of the given table. */
  private SqlForeignKey fk(SqlTable target) {
    SqlForeignKey fk = new SqlForeignKey();
    fk.name = "c_" + target.name.second;
    fk.source = c;
    fk.target = target;
    fk.map.put(target.columns.get(0), c.getColumn("PID"));
    return fk;
  }

  @Nested
  class Reading {

    @Test
    void rejectsAMissingSchemaOrConnection() throws Exception {
      var s = schema(p);
      RuntimeException ex =
          assertThrows(RuntimeException.class, () -> new SqlInstance(null, conn, false, true, ""));
      assertEquals("Null schema given to SqlInstance", ex.getMessage());
      ex = assertThrows(RuntimeException.class, () -> new SqlInstance(s, null, false, true, ""));
      assertEquals("Null JDBC connection given to SqlInstance", ex.getMessage());
    }

    @Test
    void namesATableOfTheDatabaseByThePrintedPair() throws Exception {
      SqlSchema s = new SqlSchema(conn.getMetaData(), "\"");
      s.tables.removeIf(t -> !t.name.equals(new Pair<>("PUBLIC", "T")));
      SQLException ex =
          assertThrows(SQLException.class, () -> new SqlInstance(s, conn, false, true, "\""));
      assertTrue(ex.getMessage().contains("Table \"(PUBLIC, T)\" not found"), ex.getMessage());
    }

    @Test
    void readsEachRowWithAbsentValuesForNulls() throws Exception {
      var i = instance();
      assertEquals(Set.of(row(p, 1, "a"), row(p, 2, null)), i.get(p));
      assertTrue(i.get(e).isEmpty());
      RuntimeException ex =
          assertThrows(RuntimeException.class, () -> i.get(table("X", "Y", INTEGER)));
      assertEquals("Not a table: (PUBLIC, X)", ex.getMessage());
    }

    @Test
    void readsAColumnItCannotFindAsNullOnlyWhenAsked() throws Exception {
      p.columns.add(new SqlColumn(p, "NOPE", INTEGER));
      var s = schema(p);
      assertThrows(SQLException.class, () -> new SqlInstance(s, conn, false, true, "\""));
      var i = new SqlInstance(s, conn, true, true, "\"");
      for (Map<SqlColumn, Optional<Object>> r : i.get(p)) {
        assertEquals(Optional.empty(), r.get(p.getColumn("NOPE")));
      }
    }

    /** The rows of a table are a set, so two equal rows count once even without DISTINCT. */
    @Test
    void keepsEqualRowsOnce() throws Exception {
      try (Statement s = conn.createStatement()) {
        s.execute("insert into \"(PUBLIC, E)\" values (5), (5)");
      }
      var i = new SqlInstance(schema(e), conn, false, false, "\"");
      assertEquals(Set.of(row(e, 5)), i.get(e));
    }

    @Test
    void printsTheRowsAsInsertStatementsAndSkipsEmptyTables() throws Exception {
      String s = instance().toString();
      assertTrue(s.contains("INSERT INTO \"(PUBLIC, P)\" VALUES\n  ("), s);
      assertTrue(s.contains("('1', 'a')"), s);
      assertTrue(s.contains("('2', NULL)"), s);
      assertTrue(s.contains("INSERT INTO \"(PUBLIC, C)\" VALUES\n  ("), s);
      assertTrue(s.contains("('10', '1')"), s);
      assertFalse(s.contains("(PUBLIC, E)"), s);
    }
  }

  @Nested
  class ForeignKeys {

    @Test
    void followsAKeyToTheRowItNames() throws Exception {
      var i = instance();
      SqlForeignKey fk = fk(p);
      fk.validate();
      assertEquals(row(p, 1, "a"), i.follow(row(c, 10, 1), fk));
      RuntimeException ex = assertThrows(RuntimeException.class, () -> i.follow(row(c, 11, 3), fk));
      assertTrue(ex.getMessage().startsWith("Could not resolve: c_P in "), ex.getMessage());
    }

    @Test
    void refusesATargetThatTwoRowsShare() throws Exception {
      var i = instance();
      RuntimeException ex =
          assertThrows(RuntimeException.class, () -> i.follow(row(c, 10, 1), fk(d)));
      assertTrue(
          ex.getMessage().startsWith("Target is not actually a key: c_D in "), ex.getMessage());
    }

    @Test
    void validatesItsEndsAndColumns() {
      SqlForeignKey fk = fk(p);
      fk.source = null;
      assertNull(assertThrows(RuntimeException.class, fk::validate).getMessage());
      fk.source = c;
      fk.target = null;
      assertNull(assertThrows(RuntimeException.class, fk::validate).getMessage());
      fk.target = p;
      fk.name = null;
      assertNull(assertThrows(RuntimeException.class, fk::validate).getMessage());
      fk.name = "c_P";
      fk.target = c;
      String msg = assertThrows(RuntimeException.class, fk::validate).getMessage();
      assertTrue(msg.endsWith(" is not the primary key of (PUBLIC, C)"), msg);
      fk.target = p;
      fk.map.put(p.getColumn("ID"), p.getColumn("N"));
      assertEquals(
          "N is not a column in (PUBLIC, C)",
          assertThrows(RuntimeException.class, fk::validate).getMessage());
      SqlColumn text = new SqlColumn(c, "TXT", VARCHAR);
      c.columns.add(text);
      fk.map.put(p.getColumn("ID"), text);
      assertEquals(
          "Types do not match for TXT and ID",
          assertThrows(RuntimeException.class, fk::validate).getMessage());
    }

    @Test
    void isNamedAndComparedByItsNameAndSource() {
      SqlForeignKey a = fk(p);
      SqlForeignKey b = fk(d);
      b.name = a.name;
      assertEquals("c_P", a.toString());
      assertEquals(a, b);
      assertEquals(a.hashCode(), b.hashCode());
      assertEquals(a, a);
      assertNotEquals(a, null);
      assertNotEquals(a, "c_P");
      b.source = p;
      assertNotEquals(a, b);
      b.name = "other";
      assertNotEquals(a, b);
      SqlForeignKey empty = new SqlForeignKey();
      assertEquals(empty, new SqlForeignKey());
      assertEquals(empty.hashCode(), new SqlForeignKey().hashCode());
      assertNotEquals(empty, a);
      assertNotEquals(a, empty);
    }
  }
}
