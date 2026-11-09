package catdata.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.Pair;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SqlSchema}, read from the metadata of an H2 memory database with a table T
 * whose key is an auto-incremented id, and a table U without a key.
 */
class SqlSchemaTest {

  private Connection conn;

  @BeforeEach
  void open() throws Exception {
    conn = DriverManager.getConnection("jdbc:h2:mem:sqlschematest;DB_CLOSE_DELAY=-1");
    try (Statement s = conn.createStatement()) {
      s.execute("create table T (id int auto_increment primary key, n varchar(10))");
      s.execute("create table U (a int, b int)");
    }
  }

  @AfterEach
  void close() throws Exception {
    try (Statement s = conn.createStatement()) {
      s.execute("shutdown");
    }
    conn.close();
  }

  private SqlSchema schema() throws Exception {
    return new SqlSchema(conn.getMetaData(), "\"");
  }

  private static final Pair<String, String> T = new Pair<>("PUBLIC", "T");
  private static final Pair<String, String> U = new Pair<>("PUBLIC", "U");

  @Test
  void readsTheTablesColumnsAndKeys() throws Exception {
    var s = schema();
    assertTrue(s.isTable(T));
    assertTrue(s.isTable(U));
    assertFalse(s.isTable(new Pair<>("PUBLIC", "V")));
    SqlTable t = s.getTable(T);
    assertEquals(
        Set.of("ID", "N"), t.columns.stream().map(c -> c.name).collect(Collectors.toSet()));
    assertEquals(Set.of(t.getColumn("ID")), t.pk);
    assertTrue(t.getColumn("ID").autoInc);
    assertFalse(t.getColumn("N").autoInc);
    SqlTable u = s.getTable(U);
    assertEquals(Set.copyOf(u.columns), u.pk);
    assertEquals(4, s.cols().stream().filter(c -> "PUBLIC".equals(c.table.name.first)).count());
    assertTrue(
        s.tables.stream().anyMatch(x -> "INFORMATION_SCHEMA".equals(x.name.first)),
        "the system tables of H2 are kept; the jdbc import skips them");
    assertTrue(s.fks.isEmpty());
    assertTrue(s.fksTo(T).isEmpty());
  }

  @Test
  void findsAColumnByItsTableAndName() throws Exception {
    var s = schema();
    String key = T + "N";
    assertTrue(s.isColumn(key));
    assertEquals("N", s.getColumn(key).name);
    assertFalse(s.isColumn("N"));
    RuntimeException e = assertThrows(RuntimeException.class, () -> s.getColumn("N"));
    assertEquals("Not a column: N", e.getMessage());
    e = assertThrows(RuntimeException.class, () -> s.getTable(new Pair<>("PUBLIC", "V")));
    assertEquals("Not a table: (PUBLIC, V)", e.getMessage());
    assertFalse(s.isForeignKey("F"));
    e = assertThrows(RuntimeException.class, () -> s.getForeignKey("F"));
    assertEquals("Not a foreign key: F", e.getMessage());
  }

  /** The table name is a schema and table pair, printed as such between the quotes. */
  @Test
  void printsEachTableWithItsKey() throws Exception {
    String ddl = schema().toString();
    assertTrue(ddl.contains("CREATE TABLE \"(PUBLIC, T)\"(\n  "), ddl);
    assertTrue(ddl.contains("PRIMARY KEY (\"ID\")"), ddl);
  }

  /** A table is in that form when its key is one auto-incremented column; U has two. */
  @Test
  void isInCanonicalFormOnlyWithASingleAutoIncrementedKey() throws Exception {
    var s = schema();
    assertTrue(s.getTable(T).isCnf());
    assertEquals("ID", s.getTable(T).getCnfId().name);
    assertFalse(s.getTable(U).isCnf());
    assertFalse(s.isCnf());
  }

  /**
   * The reader of foreign keys is commented out, so these tests add by hand a key from the column A
   * of U to the key of T.
   */
  @Nested
  class WithAForeignKey {

    private SqlForeignKey key(SqlSchema s, String name, Pair<String, String> source) {
      SqlForeignKey fk = new SqlForeignKey();
      fk.name = name;
      fk.source = s.getTable(source);
      fk.target = s.getTable(T);
      fk.map.put(s.getTable(T).getColumn("ID"), fk.source.columns.get(0));
      s.fks.add(fk);
      return fk;
    }

    @Test
    void findsTheKeyByItsNameAndItsTarget() throws Exception {
      var s = schema();
      SqlForeignKey fk = key(s, "u_t", U);
      assertTrue(s.isForeignKey("u_t"));
      assertEquals(fk, s.getForeignKey("u_t"));
      assertEquals(Set.of(fk), s.fksTo(T));
      assertSame(s.fksTo(T), s.fksTo(T), "the keys into a table are computed once");
      assertTrue(s.fksTo(U).isEmpty());
    }

    @Test
    void printsTheKeyAsAConstraintOfItsSource() throws Exception {
      var s = schema();
      key(s, "u_t", U);
      String ddl = s.toString();
      assertTrue(
          ddl.contains(
              "CONSTRAINT \"u_t\" FOREIGN KEY (\"A\") REFERENCES \"(PUBLIC, T)\" (\"ID\")"),
          ddl);
    }

    /** Keys are equal when their names and sources are, so two keys from T and U share a name. */
    @Test
    void refusesTwoKeysWithOneName() throws Exception {
      var s = schema();
      key(s, "same", U);
      key(s, "same", T);
      RuntimeException e = assertThrows(RuntimeException.class, () -> s.isForeignKey("same"));
      assertEquals("Report to Ryan: non-unique FK name", e.getMessage());
    }
  }
}
