package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import catdata.LocStr;
import catdata.Pair;
import catdata.cql.Instance;
import catdata.cql.Kind;
import catdata.cql.Schema;
import catdata.cql.exp.SchExp.SchExpVar;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class InstExpJdbcTest {

  private static int databases = 0;

  /** A fresh in-memory H2 database, kept until the JVM exits, with the given statements run. */
  private static synchronized String database(String... statements) throws SQLException {
    String url = "jdbc:h2:mem:inst_exp_jdbc_test_" + databases++ + ";DB_CLOSE_DELAY=-1";
    try (Connection conn = DriverManager.getConnection(url);
        Statement stmt = conn.createStatement()) {
      for (String s : statements) {
        stmt.execute(s);
      }
    }
    return url;
  }

  /** Runs a program as the IDE does. */
  private static AqlMultiDriver run(String program) throws Exception {
    AqlMultiDriver d = new AqlMultiDriver(AqlParserFactory.getParser().parseProgram(program), null);
    d.start();
    return d;
  }

  @SuppressWarnings("unchecked")
  private static Instance<String, String, Sym, Fk, Att, String, String, String, String> instance(
      AqlMultiDriver d, String name) {
    assertTrue(d.exn.isEmpty(), d.exn.toString());
    return (Instance<String, String, Sym, Fk, Att, String, String, String, String>)
        d.env.defs.get(name, Kind.INSTANCE);
  }

  /** The type of each attribute of a schema, by name. */
  private static Map<String, String> types(Schema<String, String, Sym, Fk, Att> s) {
    Map<String, String> ret = new HashMap<>();
    s.atts.forEach((att, p) -> ret.put(att.str, p.second));
    return ret;
  }

  /** The value of an attribute in the only row of an entity. */
  private static Object value(
      Instance<String, String, Sym, Fk, Att, String, String, String, String> i, String att) {
    Att a = Att.Att("T", att);
    String x = i.algebra().en("T").iterator().next();
    return ((Optional<?>) i.algebra().att(a, x).obj()).orElse(null);
  }

  private static String error(AqlMultiDriver d) {
    assertEquals(1, d.exn.size(), d.exn.toString());
    return d.exn.get(0).getMessage();
  }

  @Nested
  class ImportJdbc {

    @Test
    void typesEachColumnByItsJdbcTypeRatherThanItsName() throws Exception {
      // H2 names a varchar CHARACTER VARYING, which used to import as String
      String url =
          database(
              "create table T(i int, v varchar(10), b boolean, n numeric(5,2), u uuid, j json)",
              "insert into T values (7, 'x', true, 9.99, random_uuid(), json '{}')");
      Instance<String, String, Sym, Fk, Att, String, String, String, String> i =
          instance(
              run("instance I = import_jdbc \"" + url + "\" : sql { T -> \"select * from T\" }"),
              "I");
      Map<String, String> expected = new HashMap<>();
      expected.put("I", "Integer");
      expected.put("V", "Varchar");
      expected.put("B", "Boolean");
      expected.put("N", "Numeric");
      expected.put("U", "Binary"); // H2 reports a uuid as BINARY
      expected.put("J", "Other");
      assertEquals(expected, types(i.schema()));
      assertEquals(7, value(i, "I"));
      assertEquals(new BigDecimal("9.99"), value(i, "N"));
    }

    @Test
    void readsLargeObjectsIntoMemory() throws Exception {
      // the driver's Clob and Blob are invalid once the import closes its connection
      String url =
          database("create table T(c clob, b blob)", "insert into T values ('text', X'0102')");
      Instance<String, String, Sym, Fk, Att, String, String, String, String> i =
          instance(
              run("instance I = import_jdbc \"" + url + "\" : sql { T -> \"select * from T\" }"),
              "I");
      assertEquals(Map.of("C", "Clob", "B", "Blob"), types(i.schema()));
      assertEquals("text", value(i, "C"));
      assertArrayEquals(new byte[] {1, 2}, (byte[]) value(i, "B"));
    }

    @Test
    void givesAColumnTypeTheTypesideLacksTheTypeOther() throws Exception {
      String url = database("create table T(i int)", "insert into T values (7)");
      Instance<String, String, Sym, Fk, Att, String, String, String, String> i =
          instance(
              run(
                  "typeside Ty = literal { external_types Other -> \"java.util.Optional\" "
                      + "external_parsers Other -> \"x => java.util.Optional.of(x)\" }\n"
                      + "instance I = import_jdbc \""
                      + url
                      + "\" : Ty { T -> \"select * from T\" }"),
              "I");
      assertEquals(Map.of("I", "Other"), types(i.schema()));
    }
  }

  @Nested
  class ImportJdbcDirect {

    @Test
    void typesTheInstanceByTheSchemaItIsDeclaredOn() throws Exception {
      // without quotes H2 upper cases the column names, which still match the attributes
      String url =
          database("create table T(id int, name varchar(10))", "insert into T values (7, 'x')");
      AqlMultiDriver d =
          run(
              "schema S = literal : sql { entities T attributes id : T -> Integer name : T -> Other }\n"
                  + "instance I = import_jdbc_direct \""
                  + url
                  + "\" \"ROW_NUMBER() OVER ()\" : S { options jdbc_quote_char = \"\" }");
      Instance<String, String, Sym, Fk, Att, String, String, String, String> i = instance(d, "I");
      // the row number column is no attribute, and name keeps the type Other it is declared with
      assertEquals(d.env.defs.get("S", Kind.SCHEMA), i.schema());
      assertEquals(7, value(i, "id"));
      assertEquals("x", value(i, "name"));
    }

    @Test
    void rejectsAColumnThatMatchesTwoAttributesUpToCase() throws Exception {
      String url = database("create table T(AB int)", "insert into T values (1)");
      AqlMultiDriver d =
          run(
              "schema S = literal : sql { entities T attributes aB : T -> Integer Ab : T -> Integer }\n"
                  + "instance I = import_jdbc_direct \""
                  + url
                  + "\" \"ROW_NUMBER() OVER ()\" : S { options jdbc_quote_char = \"\" }");
      String e = error(d);
      assertTrue(e.contains("Column AB matches the attributes "), e);
      assertTrue(e.contains(" of T up to case"), e);
    }

    @Test
    void rejectsAColumnThatIsNoAttributeOfTheTargetSchema() throws Exception {
      String url = database("create table T(id int)", "insert into T values (1)");
      AqlMultiDriver d = run("schema S = literal : sql { entities T attributes id : T -> Integer }");
      @SuppressWarnings("unchecked")
      Schema<String, String, Sym, Fk, Att> s =
          (Schema<String, String, Sym, Fk, Att>) d.env.defs.get("S", Kind.SCHEMA);
      InstExpJdbc e =
          new InstExpJdbc(
              new TyExpSch(new SchExpVar("S")),
              List.of(),
              url,
              List.of(new Pair<>(new LocStr(0, "T"), "select id, 2 as other from T")),
              s);
      RuntimeException ex = assertThrows(RuntimeException.class, () -> e.eval(d.env, false));
      assertTrue(ex.getMessage().contains("Column OTHER is not an attribute of T"), ex.getMessage());
    }

    @Test
    void comparesTheTargetSchema() throws Exception {
      List<Pair<LocStr, String>> map = List.of(new Pair<>(new LocStr(0, "T"), "select 1"));
      TyExp ty = new TyExpSql();
      InstExpJdbc plain = new InstExpJdbc(ty, List.of(), "", map);
      assertEquals(plain, new InstExpJdbc(ty, List.of(), "", map, null));
      assertEquals(plain.hashCode(), new InstExpJdbc(ty, List.of(), "", map, null).hashCode());
      AqlMultiDriver d = run("schema S = literal : sql { entities T }");
      @SuppressWarnings("unchecked")
      Schema<String, String, Sym, Fk, Att> s =
          (Schema<String, String, Sym, Fk, Att>) d.env.defs.get("S", Kind.SCHEMA);
      assertNotEquals(plain, new InstExpJdbc(ty, List.of(), "", map, s));
    }
  }

  @Nested
  class Read {

    private final ResultSet rs = mock(ResultSet.class);

    @Test
    void readsAClobIntoAString() throws SQLException {
      Clob c = mock(Clob.class);
      when(c.length()).thenReturn(4L);
      when(c.getSubString(1, 4)).thenReturn("text");
      when(rs.getObject(1)).thenReturn(c);
      assertEquals("text", InstExpJdbc.read(rs, 1));
    }

    @Test
    void readsABlobIntoBytes() throws SQLException {
      Blob b = mock(Blob.class);
      byte[] bytes = {1, 2};
      when(b.length()).thenReturn(2L);
      when(b.getBytes(1, 2)).thenReturn(bytes);
      when(rs.getObject(1)).thenReturn(b);
      assertSame(bytes, InstExpJdbc.read(rs, 1));
    }

    @Test
    void readsAnSqlxmlIntoAString() throws SQLException {
      // PostgreSQL returns an xml column as an SQLXML
      SQLXML x = mock(SQLXML.class);
      when(x.getString()).thenReturn("<a/>");
      when(rs.getObject(1)).thenReturn(x);
      assertEquals("<a/>", InstExpJdbc.read(rs, 1));
    }

    @Test
    void keepsAnyOtherValue() throws SQLException {
      when(rs.getObject(1)).thenReturn(7);
      assertEquals(7, InstExpJdbc.read(rs, 1));
    }
  }
}
