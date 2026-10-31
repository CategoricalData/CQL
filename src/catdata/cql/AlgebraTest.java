package catdata.cql;

import static catdata.cql.CqlTestHelpers.VARCHAR;
import static catdata.cql.CqlTestHelpers.ops;
import static catdata.cql.CqlTestHelpers.sqlSchema;
import static catdata.cql.CqlTestHelpers.syntactic;
import static catdata.cql.CqlTestHelpers.typedSqlSchema;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.cql.Algebra.TAlg;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.exp.Att;
import catdata.cql.exp.Fk;
import catdata.cql.exp.Sym;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AlgebraTest {

  /** A type with no java backing. */
  private static final String B = "B";

  /** A type backed by java.lang.Boolean. */
  private static final String BOOL = "Bool";

  private static final String EMP = "Emp";
  private static final String DEPT = "Dept";
  private static final String WORKS_IN = "worksIn";
  private static final String MANAGES = "manages";
  private static final String NAME = "name";
  private static final String FLAG = "flag";

  private static final Fk SQL_WORKS_IN = Fk.Fk(EMP, WORKS_IN);
  private static final Att SQL_NAME = Att.Att(EMP, NAME);
  private static final Att SQL_LABEL = Att.Att(DEPT, "label");

  /** The only entity of {@link #textTable()}, with one attribute of each kind of SQL type. */
  private static final String TABLE = "T";

  private static final Att COL_VARCHAR = Att.Att(TABLE, "a");
  private static final Att COL_LONGVARCHAR = Att.Att(TABLE, "b");
  private static final Att COL_INTEGER = Att.Att(TABLE, "c");
  private static final Att COL_NVARCHAR = Att.Att(TABLE, "d");

  private static int databases = 0;

  /**
   * An algebra read off explicit tables. Rows are their own generators, as in ImportAlgebra, and
   * the counters record how often the carriers and the type algebra are computed.
   */
  private static final class TableAlgebra<S, F, A>
      extends Algebra<String, String, S, F, A, String, String, String, String> {

    private final Schema<String, String, S, F, A> schema;
    private final Map<String, List<String>> rows = new LinkedHashMap<>();
    private final Map<F, Map<String, String>> fkValues = new HashMap<>();
    private final Map<A, Map<String, Term<String, Void, S, Void, Void, Void, String>>> attValues =
        new HashMap<>();
    private final Map<String, Chc<String, Pair<String, A>>> origins = new HashMap<>();
    private final Map<String, Integer> declaredSizes = new HashMap<>();
    private final Map<String, Integer> enCalls = new HashMap<>();
    private TAlg<String, S, String> typeAlgebra =
        new TAlg<>(new LinkedHashMap<>(), new ArrayList<>());
    private int talgBuilds = 0;

    TableAlgebra(Schema<String, String, S, F, A> schema) {
      this.schema = schema;
      for (String en : schema.ens) {
        rows.put(en, new ArrayList<>());
      }
    }

    TableAlgebra<S, F, A> withRows(String en, String... xs) {
      rows.get(en).addAll(List.of(xs));
      return this;
    }

    TableAlgebra<S, F, A> withFk(F fk, String x, String y) {
      fkValues.computeIfAbsent(fk, k -> new HashMap<>()).put(x, y);
      return this;
    }

    TableAlgebra<S, F, A> withAtt(A att, String x, Term<String, Void, S, Void, Void, Void, String> v) {
      attValues.computeIfAbsent(att, k -> new HashMap<>()).put(x, v);
      return this;
    }

    TableAlgebra<S, F, A> withTalg(
        Map<String, String> sks,
        List<Pair<Term<String, Void, S, Void, Void, Void, String>, Term<String, Void, S, Void, Void, Void, String>>>
            eqs) {
      typeAlgebra = new TAlg<>(sks, eqs);
      return this;
    }

    TableAlgebra<S, F, A> withOrigin(String y, Chc<String, Pair<String, A>> origin) {
      origins.put(y, origin);
      return this;
    }

    TableAlgebra<S, F, A> withDeclaredSize(String en, int size) {
      declaredSizes.put(en, size);
      return this;
    }

    int enCalls(String en) {
      return enCalls.getOrDefault(en, 0);
    }

    @Override
    public Schema<String, String, S, F, A> schema() {
      return schema;
    }

    @Override
    public boolean hasNulls() {
      return !typeAlgebra.sks.isEmpty();
    }

    @Override
    public Iterable<String> en(String en) {
      enCalls.merge(en, 1, Integer::sum);
      return rows.get(en);
    }

    @Override
    public String gen(String gen) {
      return gen;
    }

    @Override
    public String fk(F fk, String x) {
      return fkValues.get(fk).get(x);
    }

    @Override
    public Term<String, Void, S, Void, Void, Void, String> att(A att, String x) {
      return attValues.get(att).get(x);
    }

    @Override
    public Term<String, Void, S, Void, Void, Void, String> sk(String sk) {
      return Term.Sk(sk);
    }

    @Override
    public Term<Void, String, Void, F, Void, String, Void> repr(String en, String x) {
      return Term.Gen(x);
    }

    @Override
    public int size(String en) {
      return declaredSizes.getOrDefault(en, rows.get(en).size());
    }

    @Override
    protected TAlg<String, S, String> talg0() {
      talgBuilds++;
      return typeAlgebra;
    }

    @Override
    public Chc<String, Pair<String, A>> reprT_prot(String y) {
      return origins.get(y);
    }

    @Override
    public String toStringProver() {
      return "tables";
    }

    @Override
    public Object printX(String en, String x) {
      return x;
    }

    @Override
    public Object printY(String ty, String y) {
      return y;
    }
  }

  @SafeVarargs
  private static Term<String, Void, String, Void, Void, Void, String> sym(
      String f, Term<String, Void, String, Void, Void, Void, String>... args) {
    return Term.Sym(f, List.of(args));
  }

  private static Term<String, Void, String, Void, Void, Void, String> sk(String y) {
    return Term.Sk(y);
  }

  private static Term<String, Void, String, Void, Void, Void, String> bool(boolean b) {
    return Term.Obj(b, BOOL);
  }

  private static Term<String, Void, String, Void, Void, Void, String> t() {
    return sym("t");
  }

  private static Term<String, Void, String, Void, Void, Void, String> f() {
    return sym("f");
  }

  private static Pair<Term<String, Void, String, Void, Void, Void, String>, Term<String, Void, String, Void, Void, Void, String>>
      eq(
          Term<String, Void, String, Void, Void, Void, String> l,
          Term<String, Void, String, Void, Void, Void, String> r) {
    return new Pair<>(l, r);
  }

  private static Pair<List<String>, String> sort(String result, String... args) {
    return new Pair<>(List.of(args), result);
  }

  private static Triple<Map<String, String>, Term<String, Void, String, Void, Void, Void, Void>, Term<String, Void, String, Void, Void, Void, Void>>
      groundEq(
          Term<String, Void, String, Void, Void, Void, Void> l,
          Term<String, Void, String, Void, Void, Void, Void> r) {
    return new Triple<>(Map.of(), l, r);
  }

  /** B with t, f : B and neg : B -> B, the ground equation neg(t) = f, and no java. */
  private static TypeSide<String, String> pure() {
    Map<String, Pair<List<String>, String>> syms = new LinkedHashMap<>();
    syms.put("t", sort(B));
    syms.put("f", sort(B));
    syms.put("neg", sort(B, B));
    Set<Triple<Map<String, String>, Term<String, Void, String, Void, Void, Void, Void>, Term<String, Void, String, Void, Void, Void, Void>>>
        eqs = new LinkedHashSet<>();
    eqs.add(groundEq(Term.Sym("neg", List.of(Term.Sym("t", List.of()))), Term.Sym("f", List.of())));
    AqlJs<String, String> js = new AqlJs<>(ops(), syms, Map.of(), Map.of(), Map.of());
    return new TypeSide<>(new LinkedHashSet<>(List.of(B)), syms, eqs, js, syntactic(), ops());
  }

  /**
   * {@link #pure()} plus Bool, backed by java.lang.Boolean, with not : Bool -> Bool computed in
   * javascript and the ground equation not(true) = false. Built once per test JVM, since it starts
   * the javascript engine.
   */
  private static TypeSide<String, String> mixed() {
    return MixedHolder.INSTANCE;
  }

  private static final class MixedHolder {
    private static final TypeSide<String, String> INSTANCE = buildMixed();

    private static TypeSide<String, String> buildMixed() {
      Map<String, Pair<List<String>, String>> syms = new LinkedHashMap<>();
      syms.put("t", sort(B));
      syms.put("f", sort(B));
      syms.put("neg", sort(B, B));
      syms.put("not", sort(BOOL, BOOL));
      Set<Triple<Map<String, String>, Term<String, Void, String, Void, Void, Void, Void>, Term<String, Void, String, Void, Void, Void, Void>>>
          eqs = new LinkedHashSet<>();
      eqs.add(
          groundEq(Term.Sym("neg", List.of(Term.Sym("t", List.of()))), Term.Sym("f", List.of())));
      eqs.add(groundEq(Term.Sym("not", List.of(Term.Obj(true, BOOL))), Term.Obj(false, BOOL)));
      AqlOptions options = new AqlOptions(ops(), AqlOption.allow_java_eqs_unsafe, true);
      AqlJs<String, String> js =
          new AqlJs<>(
              options,
              syms,
              Map.of(BOOL, "java.lang.Boolean"),
              Map.of(BOOL, "x => java.lang.Boolean.parseBoolean(x)"),
              Map.of("not", "x => !x"));
      return new TypeSide<>(
          new LinkedHashSet<>(List.of(B, BOOL)), syms, eqs, js, syntactic(), options);
    }
  }

  /** Emp -worksIn, manages-> Dept over ts, with the given attributes and no equations. */
  private static Schema<String, String, String, String, String> schema(
      TypeSide<String, String> ts, Map<String, Pair<String, String>> atts) {
    Map<String, Pair<String, String>> fks = new LinkedHashMap<>();
    fks.put(WORKS_IN, new Pair<>(EMP, DEPT));
    fks.put(MANAGES, new Pair<>(EMP, DEPT));
    return new Schema<>(ts, List.of(EMP, DEPT), atts, fks, List.of(), syntactic(), false);
  }

  /**
   * Over {@link #mixed()}: Emp = {e1, e2, e3} and Dept = {d1, d2}, with
   *
   * <pre>
   *        worksIn  manages  name     flag
   *   e1   d1       d2       t        true
   *   e2   d1       d1       y1       false
   *   e3   d2       d2       neg(t)   true
   * </pre>
   *
   * where y1 : B is a labelled null that came from e2.name. The type algebra has no equations.
   */
  private static TableAlgebra<String, String, String> company() {
    Map<String, Pair<String, String>> atts = new LinkedHashMap<>();
    atts.put(NAME, new Pair<>(EMP, B));
    atts.put(FLAG, new Pair<>(EMP, BOOL));
    return companyRows(new TableAlgebra<>(schema(mixed(), atts)))
        .withAtt(FLAG, "e1", bool(true))
        .withAtt(FLAG, "e2", bool(false))
        .withAtt(FLAG, "e3", bool(true));
  }

  /** {@link #company()} over {@link #pure()}, without the flag attribute. */
  private static TableAlgebra<String, String, String> pureCompany() {
    return companyRows(new TableAlgebra<>(schema(pure(), Map.of(NAME, new Pair<>(EMP, B)))));
  }

  private static TableAlgebra<String, String, String> companyRows(
      TableAlgebra<String, String, String> alg) {
    return alg.withRows(EMP, "e1", "e2", "e3")
        .withRows(DEPT, "d1", "d2")
        .withFk(WORKS_IN, "e1", "d1")
        .withFk(WORKS_IN, "e2", "d1")
        .withFk(WORKS_IN, "e3", "d2")
        .withFk(MANAGES, "e1", "d2")
        .withFk(MANAGES, "e2", "d1")
        .withFk(MANAGES, "e3", "d2")
        .withAtt(NAME, "e1", t())
        .withAtt(NAME, "e2", sk("y1"))
        .withAtt(NAME, "e3", sym("neg", t()))
        .withTalg(new LinkedHashMap<>(Map.of("y1", B)), List.of())
        .withOrigin("y1", Chc.inRight(new Pair<>("e2", NAME)));
  }

  private static Term<String, Void, Sym, Void, Void, Void, String> str(String s) {
    return Term.Obj(Optional.of(s), VARCHAR);
  }

  private static Term<String, Void, Sym, Void, Void, Void, String> sqlSk(String y) {
    return Term.Sk(y);
  }

  /**
   * Over SqlTypeSide: Emp = {e1, e2, e3} with name Ann, Bob, Ann, working in d1, d1, d2, and Dept =
   * {d1, d2} with label R&amp;D and the labelled null y1. The type algebra has no equations.
   */
  private static TableAlgebra<Sym, Fk, Att> sqlCompany() {
    Map<Att, String> atts = new LinkedHashMap<>();
    atts.put(SQL_NAME, EMP);
    atts.put(SQL_LABEL, DEPT);
    Schema<String, String, Sym, Fk, Att> s =
        sqlSchema(List.of(EMP, DEPT), atts, Map.of(SQL_WORKS_IN, new Pair<>(EMP, DEPT)));
    return new TableAlgebra<>(s)
        .withRows(EMP, "e1", "e2", "e3")
        .withRows(DEPT, "d1", "d2")
        .withFk(SQL_WORKS_IN, "e1", "d1")
        .withFk(SQL_WORKS_IN, "e2", "d1")
        .withFk(SQL_WORKS_IN, "e3", "d2")
        .withAtt(SQL_NAME, "e1", str("Ann"))
        .withAtt(SQL_NAME, "e2", str("Bob"))
        .withAtt(SQL_NAME, "e3", str("Ann"))
        .withAtt(SQL_LABEL, "d1", str("R&D"))
        .withAtt(SQL_LABEL, "d2", sqlSk("y1"))
        .withTalg(new LinkedHashMap<>(Map.of("y1", VARCHAR)), List.of());
  }

  /** Over SqlTypeSide: the entity T with a : Varchar, b : Longvarchar, c : Integer, d : Nvarchar. */
  private static TableAlgebra<Sym, Fk, Att> textTable() {
    Map<Att, Pair<String, String>> atts = new LinkedHashMap<>();
    atts.put(COL_VARCHAR, new Pair<>(TABLE, VARCHAR));
    atts.put(COL_LONGVARCHAR, new Pair<>(TABLE, "Longvarchar"));
    atts.put(COL_INTEGER, new Pair<>(TABLE, "Integer"));
    atts.put(COL_NVARCHAR, new Pair<>(TABLE, "Nvarchar"));
    return new TableAlgebra<>(typedSqlSchema(List.of(TABLE), atts, Map.of(), List.of()));
  }

  private static final List<Chc<Fk, Att>> TEXT_HEADER =
      List.of(
          Chc.inRight(COL_VARCHAR),
          Chc.inRight(COL_LONGVARCHAR),
          Chc.inRight(COL_INTEGER),
          Chc.inRight(COL_NVARCHAR));

  /** A fresh in-memory database, dropped when its last connection closes. */
  private static synchronized Connection freshDatabase() throws SQLException {
    return DriverManager.getConnection("jdbc:h2:mem:algebra_test_" + databases++);
  }

  /** Runs a query and renders each row as its columns joined by |. */
  private static List<String> rows(Connection conn, String query) throws SQLException {
    List<String> ret = new ArrayList<>();
    try (Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(query)) {
      int n = rs.getMetaData().getColumnCount();
      while (rs.next()) {
        List<String> cols = new ArrayList<>(n);
        for (int i = 1; i <= n; i++) {
          cols.add(String.valueOf(rs.getObject(i)));
        }
        ret.add(String.join("|", cols));
      }
    }
    return ret;
  }

  /** Drops an in-memory database that DB_CLOSE_DELAY=-1 would otherwise keep alive. */
  private static void shutdown(Connection conn) throws SQLException {
    try (Statement stmt = conn.createStatement()) {
      stmt.execute("SHUTDOWN");
    }
  }

  private static Set<String> indexNames(Connection conn, String table) throws SQLException {
    Set<String> ret = new TreeSet<>();
    try (ResultSet rs = conn.getMetaData().getIndexInfo(null, null, table.toUpperCase(), false, false)) {
      while (rs.next()) {
        ret.add(rs.getString("INDEX_NAME"));
      }
    }
    return ret;
  }

  private static Map<String, List<String>> noIndices() {
    return Map.of(EMP, List.of(), DEPT, List.of());
  }

  private static <X> List<X> list(Iterable<X> it) {
    List<X> ret = new ArrayList<>();
    it.forEach(ret::add);
    return ret;
  }

  @Nested
  class TypeAlgebra {

    @Test
    void printsItsEquations() {
      TAlg<String, String, String> talg =
          new TAlg<>(Map.of("y1", B), List.of(eq(sk("y1"), t()), eq(sym("neg", f()), t())));
      assertEquals("\nEquations\ny1 = t \n neg(f) = t", talg.toString());
    }

    @Test
    void printsNoEquationsAsAnEmptyList() {
      assertEquals("\nEquations\n", new TAlg<>(Map.of(), List.of()).toString());
    }

    @Test
    void keepsTheFirstRenderingAfterTheEquationsChange() {
      List<Pair<Term<String, Void, String, Void, Void, Void, String>, Term<String, Void, String, Void, Void, Void, String>>>
          eqs = new ArrayList<>(List.of(eq(sk("y1"), t())));
      TAlg<String, String, String> talg = new TAlg<>(Map.of("y1", B), eqs);
      String first = talg.toString();
      eqs.add(eq(sk("y1"), f()));
      assertSame(first, talg.toString());
    }

    @Test
    void exposesTheGivenCollectionOfEquations() {
      List<Pair<Term<String, Void, String, Void, Void, Void, String>, Term<String, Void, String, Void, Void, Void, String>>>
          eqs = new ArrayList<>(List.of(eq(sk("y1"), t())));
      TAlg<String, String, String> talg = new TAlg<>(Map.of("y1", B), eqs);
      assertSame(eqs, talg.eqsNoDefns());
      assertEquals(eqs, list(talg.allEqs()));
    }

    @Test
    void reportsTheGivenSizeForAnIterableOfEquations() {
      List<Pair<Term<String, Void, String, Void, Void, Void, String>, Term<String, Void, String, Void, Void, Void, String>>>
          backing = new ArrayList<>(List.of(eq(sk("y1"), t())));
      TAlg<String, String, String> talg = new TAlg<>(Map.of("y1", B), backing::iterator, 7);
      assertEquals(7, talg.eqsNoDefns().size());
      assertEquals(backing, list(talg.eqsNoDefns()));
    }

    @Test
    void readsAnIterableOfEquationsLive() {
      List<Pair<Term<String, Void, String, Void, Void, Void, String>, Term<String, Void, String, Void, Void, Void, String>>>
          backing = new ArrayList<>(List.of(eq(sk("y1"), t())));
      TAlg<String, String, String> talg = new TAlg<>(Map.of("y1", B), backing::iterator, 1);
      backing.add(eq(sk("y1"), f()));
      assertEquals(backing, list(talg.allEqs()));
      assertEquals(1, talg.eqsNoDefns().size());
    }

    @Test
    void allEqsFailsWithoutEquations() {
      TAlg<String, String, String> talg = new TAlg<>(Map.of(), null);
      RuntimeException e = assertThrows(RuntimeException.class, talg::allEqs);
      assertEquals("Anomaly: please report", e.getMessage());
    }

    @Test
    void typesSkolemsAndSymbols() {
      TAlg<String, String, String> talg =
          new TAlg<>(Map.of("y1", B, "y2", BOOL), List.of());
      assertEquals(B, talg.type(mixed(), sk("y1")));
      assertEquals(BOOL, talg.type(mixed(), sym("not", sk("y2"))));
      assertEquals(B, talg.type(mixed(), sym("neg", t())));
    }

    @Test
    void equalsComparesEquationsAndSkolems() {
      TAlg<String, String, String> a = new TAlg<>(Map.of("y1", B), List.of(eq(sk("y1"), t())));
      TAlg<String, String, String> same =
          new TAlg<>(Map.of("y1", B), List.of(eq(sk("y1"), t())));
      assertEquals(a, a);
      assertEquals(a, same);
      assertEquals(a.hashCode(), same.hashCode());
      assertNotEquals(a, new TAlg<>(Map.of("y1", B), List.of(eq(sk("y1"), f()))));
      assertNotEquals(a, new TAlg<>(Map.of("y2", B), List.of(eq(sk("y1"), t()))));
      assertNotEquals(a, null);
      assertNotEquals(a, "y1 = t");
    }

    @Test
    void equalsHandlesMissingFields() {
      List<Pair<Term<String, Void, String, Void, Void, Void, String>, Term<String, Void, String, Void, Void, Void, String>>>
          eqs = List.of(eq(sk("y1"), t()));
      TAlg<String, String, String> noEqs = new TAlg<>(Map.of("y1", B), null);
      TAlg<String, String, String> noSks = new TAlg<>(null, eqs);
      assertEquals(noEqs, new TAlg<>(Map.of("y1", B), null));
      assertEquals(noSks, new TAlg<>(null, eqs));
      assertNotEquals(noEqs, new TAlg<>(Map.of("y1", B), eqs));
      assertNotEquals(new TAlg<>(Map.of("y1", B), eqs), noEqs);
      assertNotEquals(noSks, new TAlg<>(Map.of("y1", B), eqs));
      assertNotEquals(new TAlg<>(Map.of("y1", B), eqs), noSks);
      assertEquals(31 * 31, new TAlg<String, String, String>(null, null).hashCode());
    }

    @Test
    void equalsComparesIterableBackedEquationsByIdentity() {
      List<Pair<Term<String, Void, String, Void, Void, Void, String>, Term<String, Void, String, Void, Void, Void, String>>>
          backing = List.of(eq(sk("y1"), t()));
      TAlg<String, String, String> a = new TAlg<>(Map.of("y1", B), backing::iterator, 1);
      TAlg<String, String, String> b = new TAlg<>(Map.of("y1", B), backing::iterator, 1);
      assertNotEquals(a, b);
    }

    @Test
    void collageCarriesTheTypeSideAndTheSkolems() {
      TypeSide<String, String> ts = mixed();
      TAlg<String, String, String> talg = new TAlg<>(Map.of("y1", B), List.of());
      Collage<String, Void, String, Void, Void, Void, String> col = talg.toCollage(ts, false);
      assertSame(ts.tys, col.tys());
      assertSame(ts.syms, col.syms());
      assertSame(ts.js.java_tys, col.java_tys());
      assertSame(ts.js.java_parsers, col.java_parsers());
      assertSame(ts.js.java_fns, col.java_fns());
      assertSame(talg.sks, col.sks());
      assertTrue(col.getEns().isEmpty());
      assertTrue(col.atts().isEmpty());
      assertTrue(col.fks().isEmpty());
      assertTrue(col.gens().isEmpty());
    }

    @Test
    void collageWithoutTypeSideEquationsHasOnlyTheInstanceEquations() {
      TAlg<String, String, String> talg =
          new TAlg<>(Map.of("y1", B), List.of(eq(sk("y1"), t()), eq(sk("y1"), f())));
      Collection<Eq<String, Void, String, Void, Void, Void, String>> eqs =
          talg.toCollage(mixed(), false).eqs();
      assertEquals(2, eqs.size());
      List<Eq<String, Void, String, Void, Void, Void, String>> l = new ArrayList<>(eqs);
      assertTrue(l.get(0).ctx.isEmpty());
      assertEquals(sk("y1"), l.get(0).lhs);
      assertEquals(t(), l.get(0).rhs);
      assertEquals(f(), l.get(1).rhs);
    }

    @Test
    void collageWithTypeSideEquationsPutsThemFirst() {
      TypeSide<String, String> ts = mixed();
      TAlg<String, String, String> talg = new TAlg<>(Map.of("y1", B), List.of(eq(sk("y1"), t())));
      Collection<Eq<String, Void, String, Void, Void, Void, String>> eqs =
          talg.toCollage(ts, true).eqs();
      assertEquals(1 + ts.eqs.size(), eqs.size());
      List<Pair<Term<String, Void, String, Void, Void, Void, String>, Term<String, Void, String, Void, Void, Void, String>>>
          pairs = new ArrayList<>();
      for (Eq<String, Void, String, Void, Void, Void, String> e : eqs) {
        pairs.add(new Pair<>(e.lhs, e.rhs));
      }
      assertEquals(
          List.of(
              eq(sym("neg", t()), f()),
              eq(sym("not", bool(true)), bool(false)),
              eq(sk("y1"), t())),
          pairs);
    }
  }

  @Nested
  class Freeness {

    @Test
    void isFreeWithoutEquations() {
      TableAlgebra<String, String, String> alg = company();
      assertTrue(alg.hasFreeTypeAlgebra());
      assertTrue(alg.hasFreeTypeAlgebraOnJava());
    }

    @Test
    void isFreeWhenEveryEquationIsAGroundTypeSideEquation() {
      TableAlgebra<String, String, String> alg =
          company().withTalg(Map.of(), List.of(eq(sym("neg", t()), f())));
      assertTrue(alg.hasFreeTypeAlgebra());
    }

    @Test
    void isNotFreeWhenAGroundTypeSideEquationIsReversed() {
      TableAlgebra<String, String, String> alg =
          company().withTalg(Map.of(), List.of(eq(f(), sym("neg", t()))));
      assertFalse(alg.hasFreeTypeAlgebra());
    }

    @Test
    void isNotFreeWithAnInstanceEquation() {
      TableAlgebra<String, String, String> alg =
          company().withTalg(Map.of("y1", B), List.of(eq(sk("y1"), t())));
      assertFalse(alg.hasFreeTypeAlgebra());
    }

    @Test
    void onJavaIgnoresEquationsOfTypesWithoutJava() {
      TableAlgebra<String, String, String> alg =
          company().withTalg(Map.of("y1", B), List.of(eq(sk("y1"), t())));
      assertTrue(alg.hasFreeTypeAlgebraOnJava());
    }

    @Test
    void onJavaAcceptsGroundTypeSideEquationsOfJavaTypes() {
      TableAlgebra<String, String, String> alg =
          company().withTalg(Map.of(), List.of(eq(sym("not", bool(true)), bool(false))));
      assertTrue(alg.hasFreeTypeAlgebraOnJava());
    }

    @Test
    void onJavaRejectsAnInstanceEquationOfAJavaType() {
      TableAlgebra<String, String, String> alg =
          company()
              .withTalg(
                  Map.of("y1", B, "y2", BOOL),
                  List.of(eq(sk("y1"), t()), eq(sk("y2"), bool(true))));
      assertFalse(alg.hasFreeTypeAlgebraOnJava());
    }
  }

  @Nested
  class Carriers {

    @Test
    void sizeAddsTheEntities() {
      TableAlgebra<String, String, String> alg = company();
      assertEquals(5, alg.size());
      assertEquals(3, alg.size(EMP));
    }

    @Test
    void sizeOfBiggestIsTheLargestEntity() {
      assertEquals(3, company().sizeOfBiggest());
      assertEquals(9, company().withDeclaredSize(DEPT, 9).sizeOfBiggest());
    }

    @Test
    void sizeOfBiggestIsZeroWithoutEntities() {
      Schema<String, String, String, String, String> empty =
          new Schema<>(pure(), List.of(), Map.of(), Map.of(), List.of(), syntactic(), false);
      assertEquals(0, new TableAlgebra<>(empty).sizeOfBiggest());
    }

    @Test
    void estimateNullSizeScalesRowsTimesAttributes() {
      assertEquals(3, company().estimateNullSize(0.5f));
      assertEquals(12, company().estimateNullSize(2f));
    }

    @Test
    void estimateNullSizeTruncatesEachEntitySeparately() {
      // Emp: 3 rows x 1 attribute, Dept: 2 rows x 1 attribute, so 0.75 + 0.5 = 1.25 in all.
      assertEquals(0, sqlCompany().estimateNullSize(0.25f));
    }

    @Test
    void allXsListsEveryRowEntityByEntity() {
      assertEquals(List.of("e1", "e2", "e3", "d1", "d2"), list(company().allXs()));
    }

    @Test
    void allXs0PairsEveryRowWithItsEntity() {
      assertEquals(
          List.of(
              new Pair<>(EMP, "e1"),
              new Pair<>(EMP, "e2"),
              new Pair<>(EMP, "e3"),
              new Pair<>(DEPT, "d1"),
              new Pair<>(DEPT, "d2")),
          list(company().allXs0()));
    }

    @Test
    void validateMoreAcceptsMatchingSizes() {
      assertDoesNotThrow(() -> company().validateMore());
    }

    @Test
    void validateMoreRejectsADeclaredSizeThatDisagreesWithTheRows() {
      TableAlgebra<String, String, String> alg = company().withDeclaredSize(EMP, 4);
      RuntimeException e = assertThrows(RuntimeException.class, alg::validateMore);
      assertEquals("On entity Emp, given size is 4 but counted 3: e1, e2, e3", e.getMessage());
    }

    @Test
    void intifyXNumbersTheRowsEntityByEntity() {
      Pair<TObjectIntMap<String>, TIntObjectMap<String>> ids = company().intifyX(10);
      List<String> rows = List.of("e1", "e2", "e3", "d1", "d2");
      for (int i = 0; i < rows.size(); i++) {
        assertEquals(10 + i, ids.first.get(rows.get(i)));
        assertEquals(rows.get(i), ids.second.get(10 + i));
      }
      assertEquals(5, ids.first.size());
      assertEquals(5, ids.second.size());
    }

    @Test
    void intifyYNumbersTheSkolemsOfTheTypeAlgebra() {
      Map<String, String> sks = new LinkedHashMap<>();
      sks.put("y1", B);
      sks.put("y2", BOOL);
      Pair<TObjectIntMap<String>, TIntObjectMap<String>> ids =
          company().withTalg(sks, List.of()).intifyY(3);
      assertEquals(3, ids.first.get("y1"));
      assertEquals(4, ids.first.get("y2"));
      assertEquals("y1", ids.second.get(3));
      assertEquals("y2", ids.second.get(4));
      assertEquals(2, ids.second.size());
    }

    @Test
    void talgIsComputedOnce() {
      TableAlgebra<String, String, String> alg = company();
      TAlg<String, String, String> first = alg.talg();
      assertSame(first, alg.talg());
      assertEquals(1, alg.talgBuilds);
    }

    @Test
    void talgToStringPrintsTheTypeAlgebra() {
      TableAlgebra<String, String, String> alg =
          company().withTalg(Map.of("y1", B), List.of(eq(sk("y1"), t())));
      assertEquals("\nEquations\ny1 = t", alg.talgToString());
    }
  }

  @Nested
  class Printing {

    @Test
    void printsCarriersForeignKeysAttributesAndTheTypeAlgebra() {
      TableAlgebra<String, String, String> alg = company();
      String expected =
          "carriers\n\tEmp -> {e1, e2, e3}\n\tDept -> {d1, d2}"
              + "\n\nforeign keys"
              + "\n\tworksIn -> {(e1, d1), (e2, d1), (e3, d2)}"
              + "\n\tmanages -> {(e1, d2), (e2, d1), (e3, d2)}"
              + "\n\nattributes"
              + "\n\tname -> {(e1, "
              + t()
              + "), (e2, "
              + sk("y1")
              + "), (e3, "
              + sym("neg", t())
              + ")}"
              + "\n\tflag -> {(e1, "
              + bool(true)
              + "), (e2, "
              + bool(false)
              + "), (e3, "
              + bool(true)
              + ")}"
              + "\n\n----- type algebra\n\n"
              + alg.talg();
      assertEquals(expected, alg.toString());
    }

    @Test
    void printsEmptyCarriers() {
      Schema<String, String, String, String, String> s =
          new Schema<>(pure(), List.of(EMP), Map.of(), Map.of(), List.of(), syntactic(), false);
      assertEquals(
          "carriers\n\tEmp -> {}\n\nforeign keys\n\nattributes\n\n----- type algebra\n\n\nEquations\n",
          new TableAlgebra<>(s).toString());
    }
  }

  @Nested
  class Indexing {

    @Test
    void enIndexedFkSelectsTheRowsPointingAtARow() {
      TableAlgebra<String, String, String> alg = company();
      assertEquals(List.of("e1", "e2"), list(alg.en_indexedFk(EMP, WORKS_IN, "d1")));
      assertEquals(List.of("e3"), list(alg.en_indexedFk(EMP, WORKS_IN, "d2")));
    }

    @Test
    void enIndexedFkIsCached() {
      TableAlgebra<String, String, String> alg = company();
      Collection<String> first = alg.en_indexedFk(EMP, WORKS_IN, "d1");
      int calls = alg.enCalls(EMP);
      assertSame(first, alg.en_indexedFk(EMP, WORKS_IN, "d1"));
      assertEquals(calls, alg.enCalls(EMP));
    }

    @Test
    void enIndexedAttSelectsTheRowsWithAValueOnAFreeJavaTypeAlgebra() {
      TableAlgebra<Sym, Fk, Att> alg = sqlCompany();
      assertEquals(List.of("e1", "e3"), list(alg.en_indexedAtt(EMP, SQL_NAME, Optional.of("Ann"))));
      assertEquals(List.of("d1"), list(alg.en_indexedAtt(DEPT, SQL_LABEL, Optional.of("R&D"))));
      assertTrue(alg.en_indexedAtt(EMP, SQL_NAME, Optional.of("Cy")).isEmpty());
    }

    @Test
    void enIndexedAttIsCachedOnAFreeJavaTypeAlgebra() {
      TableAlgebra<Sym, Fk, Att> alg = sqlCompany();
      Collection<String> first = alg.en_indexedAtt(EMP, SQL_NAME, Optional.of("Ann"));
      int calls = alg.enCalls(EMP);
      assertSame(first, alg.en_indexedAtt(EMP, SQL_NAME, Optional.of("Ann")));
      assertEquals(calls, alg.enCalls(EMP));
    }

    @Test
    void enIndexedAttReturnsEveryRowWhenTheTypeAlgebraHasEquations() {
      TableAlgebra<Sym, Fk, Att> alg =
          sqlCompany().withTalg(Map.of("y1", VARCHAR), List.of(new Pair<>(sqlSk("y1"), str("x"))));
      Collection<String> all = alg.en_indexedAtt(EMP, SQL_NAME, Optional.of("Ann"));
      assertEquals(List.of("e1", "e2", "e3"), list(all));
      assertEquals(3, all.size());
    }

    @Test
    void enIndexedAttReturnsEveryRowWhenATypeHasNoJava() {
      TableAlgebra<String, String, String> alg = company();
      Collection<String> all = alg.en_indexedAtt(EMP, FLAG, true);
      assertEquals(List.of("e1", "e2", "e3"), list(all));
      assertEquals(3, all.size());
    }

    @Test
    void enIndexedWithNoConstraintsIsTheCarrier() {
      TableAlgebra<String, String, String> alg = company();
      assertSame(alg.rows.get(EMP), alg.en_indexed(EMP, List.of(), List.of()));
    }

    @Test
    void enIndexedWithOneForeignKeyUsesTheForeignKeyIndex() {
      TableAlgebra<String, String, String> alg = company();
      Collection<String> byFk = alg.en_indexedFk(EMP, MANAGES, "d2");
      assertSame(byFk, alg.en_indexed(EMP, List.of(new Pair<>(MANAGES, "d2")), List.of()));
    }

    @Test
    void enIndexedWithOneAttributeUsesTheAttributeIndex() {
      TableAlgebra<Sym, Fk, Att> alg = sqlCompany();
      Collection<String> byAtt = alg.en_indexedAtt(EMP, SQL_NAME, Optional.of("Bob"));
      assertSame(
          byAtt, alg.en_indexed(EMP, List.of(), List.of(new Pair<>(SQL_NAME, Optional.of("Bob")))));
    }

    @Test
    void enIndexedIntersectsSeveralForeignKeys() {
      TableAlgebra<String, String, String> alg = company();
      List<Pair<String, String>> fks =
          List.of(new Pair<>(WORKS_IN, "d1"), new Pair<>(MANAGES, "d1"));
      assertEquals(List.of("e2"), list(alg.en_indexed(EMP, fks, List.of())));
    }

    @Test
    void enIndexedIntersectsForeignKeysAndAttributes() {
      TableAlgebra<Sym, Fk, Att> alg = sqlCompany();
      List<Pair<Fk, String>> fks = List.of(new Pair<>(SQL_WORKS_IN, "d1"));
      List<Pair<Att, Object>> atts = List.of(new Pair<>(SQL_NAME, Optional.of("Ann")));
      assertEquals(List.of("e1"), list(alg.en_indexed(EMP, fks, atts)));
    }

    @Test
    void enIndexedCachesTheIntersection() {
      TableAlgebra<String, String, String> alg = company();
      List<Pair<String, String>> fks =
          List.of(new Pair<>(WORKS_IN, "d2"), new Pair<>(MANAGES, "d2"));
      Iterable<String> first = alg.en_indexed(EMP, fks, List.of());
      int calls = alg.enCalls(EMP);
      assertSame(first, alg.en_indexed(EMP, new ArrayList<>(fks), List.of()));
      assertEquals(calls, alg.enCalls(EMP));
    }

    @Test
    void fkAsSetPairsEachRowWithItsImage() {
      TableAlgebra<String, String, String> alg = company();
      Set<Pair<String, String>> set = alg.fkAsSet(WORKS_IN);
      assertEquals(
          Set.of(new Pair<>("e1", "d1"), new Pair<>("e2", "d1"), new Pair<>("e3", "d2")), set);
      int calls = alg.enCalls(EMP);
      assertSame(set, alg.fkAsSet(WORKS_IN));
      assertEquals(calls, alg.enCalls(EMP));
    }

    @Test
    void attAsSetPairsEachRowWithItsValue() {
      TableAlgebra<String, String, String> alg = company();
      Set<Pair<String, Term<String, Void, String, Void, Void, Void, String>>> set =
          alg.attAsSet(FLAG);
      assertEquals(
          Set.of(
              new Pair<>("e1", bool(true)),
              new Pair<>("e2", bool(false)),
              new Pair<>("e3", bool(true))),
          set);
      int calls = alg.enCalls(EMP);
      assertSame(set, alg.attAsSet(FLAG));
      assertEquals(calls, alg.enCalls(EMP));
    }
  }

  @Nested
  class Terms {

    @Test
    void nfEvaluatesGeneratorsAndForeignKeys() {
      TableAlgebra<String, String, String> alg = company();
      Term<Void, String, Void, String, Void, String, Void> e1 = Term.Gen("e1");
      assertEquals("e1", alg.nf(e1));
      assertEquals("d2", alg.nf(Term.Fk(MANAGES, e1)));
    }

    @Test
    void nfRejectsAVariable() {
      TableAlgebra<String, String, String> alg = company();
      RuntimeException e = assertThrows(RuntimeException.class, () -> alg.nf(Term.Var("v")));
      assertEquals("Anomaly: please report", e.getMessage());
    }

    @Test
    void intoXEvaluatesGeneratorsAndForeignKeys() {
      TableAlgebra<String, String, String> alg = company();
      Term<String, String, String, String, String, String, String> e3 = Term.Gen("e3");
      assertEquals("e3", alg.intoX(e3));
      assertEquals("d2", alg.intoX(Term.Fk(WORKS_IN, e3)));
    }

    @Test
    void intoXRejectsATermOfAType() {
      TableAlgebra<String, String, String> alg = company();
      RuntimeException e = assertThrows(RuntimeException.class, () -> alg.intoX(Term.Sk("y1")));
      assertEquals("Anomaly: please report", e.getMessage());
    }

    @Test
    void intoYEvaluatesAttributesAndKeepsConstantsSkolemsAndVariables() {
      TableAlgebra<String, String, String> alg = company();
      Term<String, String, String, String, String, String, String> e2 = Term.Gen("e2");
      assertEquals(sk("y1"), alg.intoY(Term.Att(NAME, e2)));
      assertEquals(bool(true), alg.intoY(Term.Obj(true, BOOL)));
      assertEquals(sk("y7"), alg.intoY(Term.Sk("y7")));
      assertEquals(Term.Var("v"), alg.intoY(Term.Var("v")));
    }

    @Test
    void intoYDoesNotReduceJavaSymbols() {
      TableAlgebra<String, String, String> alg = company();
      Term<String, String, String, String, String, String, String> e1 = Term.Gen("e1");
      assertEquals(
          sym("not", bool(true)), alg.intoY(Term.Sym("not", List.of(Term.Att(FLAG, e1)))));
    }

    @Test
    void intoYRejectsATermOfAnEntity() {
      TableAlgebra<String, String, String> alg = company();
      RuntimeException e = assertThrows(RuntimeException.class, () -> alg.intoY(Term.Gen("e1")));
      assertEquals("Anomaly: please report: e1", e.getMessage());
    }

    @Test
    void reprTMapsASkolemBackToItsLabelledNull() {
      TableAlgebra<String, String, String> alg =
          company().withOrigin("y9", Chc.inLeft("s9"));
      assertEquals(Term.Sk("s9"), alg.reprT(sk("y9")));
    }

    @Test
    void reprTMapsASkolemBackToTheAttributeItCameFrom() {
      TableAlgebra<String, String, String> alg = company();
      assertEquals(Term.Att(NAME, Term.Gen("e2")), alg.reprT(sk("y1")));
    }

    @Test
    void reprTReducesJavaSymbols() {
      TableAlgebra<String, String, String> alg = company();
      assertEquals(Term.Obj(false, BOOL), alg.reprT(sym("not", bool(true))));
    }

    @Test
    void reprTRewritesTheArgumentsOfASymbol() {
      TableAlgebra<String, String, String> alg = company();
      assertEquals(
          Term.Sym("neg", List.of(Term.Att(NAME, Term.Gen("e2")))),
          alg.reprT(sym("neg", sk("y1"))));
    }

    @Test
    void reprTLeavesSymbolsAloneWithoutJava() {
      TableAlgebra<String, String, String> alg = pureCompany();
      assertEquals(Term.Sym("neg", List.of(Term.Sym("t", List.of()))), alg.reprT(sym("neg", t())));
    }

    @Test
    void reprTKeepsConstants() {
      assertEquals(Term.Obj(true, BOOL), company().reprT(bool(true)));
    }

    @Test
    void reprTRejectsAVariable() {
      TableAlgebra<String, String, String> alg = company();
      RuntimeException e = assertThrows(RuntimeException.class, () -> alg.reprT(Term.Var("v")));
      assertEquals("Please report, reprT on v", e.getMessage());
    }
  }

  @Nested
  class Records {

    private Connection textDatabase() throws SQLException {
      Connection conn = freshDatabase();
      try (Statement stmt = conn.createStatement()) {
        stmt.execute("create table T(a varchar(32), b longvarchar, c integer, d nvarchar(32))");
      }
      return conn;
    }

    private void store(
        TableAlgebra<Sym, Fk, Att> alg, Connection conn, String x, boolean emitNulls)
        throws Exception {
      alg.storeMyRecord(
          false,
          List.of("?", "?", "?", "?"),
          List.of("a", "b", "c", "d"),
          null,
          conn,
          x,
          TEXT_HEADER,
          TABLE,
          "",
          "",
          false,
          emitNulls,
          att -> jdbcType(alg, att));
    }

    /** The JDBC type of an attribute's CQL type, which is what an export binds these types with. */
    private int jdbcType(TableAlgebra<Sym, Fk, Att> alg, Att att) {
      return SqlTypeSide.getSqlType(alg.schema().atts.get(att).second);
    }

    private TableAlgebra<Sym, Fk, Att> row(
        TableAlgebra<Sym, Fk, Att> alg,
        String x,
        Term<String, Void, Sym, Void, Void, Void, String> a,
        Term<String, Void, Sym, Void, Void, Void, String> b,
        Term<String, Void, Sym, Void, Void, Void, String> c,
        Term<String, Void, Sym, Void, Void, Void, String> d) {
      return alg.withRows(TABLE, x)
          .withAtt(COL_VARCHAR, x, a)
          .withAtt(COL_LONGVARCHAR, x, b)
          .withAtt(COL_INTEGER, x, c)
          .withAtt(COL_NVARCHAR, x, d);
    }

    @Test
    void storesPresentValues() throws Exception {
      TableAlgebra<Sym, Fk, Att> alg =
          row(textTable(), "r1", str("x"), str("y"), Term.Obj(Optional.of(7), "Integer"), str("z"));
      try (Connection conn = textDatabase()) {
        store(alg, conn, "r1", false);
        assertEquals(List.of("x|y|7|z"), rows(conn, "select a, b, c, d from T"));
      }
    }

    @Test
    void rejectsAConstantThatIsNotAnOptional() throws Exception {
      TableAlgebra<Sym, Fk, Att> alg =
          row(
              textTable(),
              "r1",
              Term.Obj("x", VARCHAR),
              str("y"),
              Term.Obj(Optional.of(7), "Integer"),
              str("z"));
      try (Connection conn = textDatabase()) {
        assertThrows(ClassCastException.class, () -> store(alg, conn, "r1", false));
      }
    }

    @Test
    void storesAnEmptyOptionalAsNull() throws Exception {
      Term<String, Void, Sym, Void, Void, Void, String> none = Term.Obj(Optional.empty(), VARCHAR);
      TableAlgebra<Sym, Fk, Att> alg =
          row(textTable(), "r1", none, none, Term.Obj(Optional.empty(), "Integer"), none);
      try (Connection conn = textDatabase()) {
        store(alg, conn, "r1", true);
        assertEquals(List.of("null|null|null|null"), rows(conn, "select a, b, c, d from T"));
      }
    }

    @Test
    void storesLabelledNullsAsNullWithoutEmitNullsExceptOnLongvarcharAndNvarchar()
        throws Exception {
      TableAlgebra<Sym, Fk, Att> alg =
          row(textTable(), "r1", sqlSk("y1"), sqlSk("y2"), sqlSk("y3"), sqlSk("y4"));
      try (Connection conn = textDatabase()) {
        store(alg, conn, "r1", false);
        assertEquals(List.of("null|?0|null|?1"), rows(conn, "select a, b, c, d from T"));
      }
    }

    @Test
    void numbersLabelledNullsWithEmitNullsAcrossRecords() throws Exception {
      TableAlgebra<Sym, Fk, Att> alg = textTable();
      row(alg, "r1", sqlSk("y1"), sqlSk("y2"), sqlSk("y3"), sqlSk("y4"));
      row(alg, "r2", sqlSk("y1"), sqlSk("y5"), sqlSk("y3"), sqlSk("y4"));
      try (Connection conn = textDatabase()) {
        store(alg, conn, "r1", true);
        store(alg, conn, "r2", true);
        assertEquals(
            List.of("?0|?1|null|?2", "?0|?3|null|?2"),
            rows(conn, "select a, b, c, d from T order by b"));
      }
    }

    @Test
    void emitsIdsAndHonorsThePrefixAndTheQuote() throws Exception {
      TableAlgebra<Sym, Fk, Att> alg =
          row(textTable(), "r1", str("x"), str("y"), Term.Obj(Optional.of(7), "Integer"), str("z"));
      try (Connection conn = freshDatabase()) {
        try (Statement stmt = conn.createStatement()) {
          stmt.execute("create table \"p_T\"(\"rowid\" integer, \"a\" varchar(32))");
        }
        alg.storeMyRecord(
            true,
            List.of("?", "?"),
            List.of("\"rowid\"", "\"a\""),
            alg.intifyX(5),
            conn,
            "r1",
            List.of(Chc.inRight(COL_VARCHAR)),
            TABLE,
            "p_",
            "\"",
            false,
            false,
            att -> jdbcType(alg, att));
        assertEquals(List.of("5|x"), rows(conn, "select \"rowid\", \"a\" from \"p_T\""));
      }
    }
  }

  @Nested
  class Sql {

    @Test
    void createAndLoadWritesEveryRowUnderTheTruncatedNames() throws SQLException {
      TableAlgebra<Sym, Fk, Att> alg = sqlCompany();
      Connection conn = alg.createAndLoad(noIndices(), alg.intifyX(10), 32);
      try {
        Schema<String, String, Sym, Fk, Att> s = alg.schema();
        String emp = s.truncate(EMP, true);
        String dept = s.truncate(DEPT, true);
        String worksIn = s.truncate(Chc.inLeft(SQL_WORKS_IN), true);
        String name = s.truncate(Chc.inRight(SQL_NAME), true);
        String label = s.truncate(Chc.inRight(SQL_LABEL), true);
        assertEquals(
            List.of("10|13|Ann", "11|13|Bob", "12|14|Ann"),
            rows(
                conn,
                "select rowid, " + worksIn + ", " + name + " from " + emp + " order by rowid"));
        assertEquals(
            List.of("13|R&D", "14|null"),
            rows(conn, "select rowid, " + label + " from " + dept + " order by rowid"));
      } finally {
        shutdown(conn);
      }
    }

    @Test
    void createAndLoadRunsTheIndexStatements() throws SQLException {
      TableAlgebra<Sym, Fk, Att> alg = sqlCompany();
      Schema<String, String, Sym, Fk, Att> s = alg.schema();
      String emp = s.truncate(EMP, true);
      String index =
          "create index emp_name on " + emp + "(" + s.truncate(Chc.inRight(SQL_NAME), true) + ")";
      Connection conn =
          alg.createAndLoad(Map.of(EMP, List.of(index), DEPT, List.of()), alg.intifyX(0), 32);
      try {
        assertTrue(indexNames(conn, emp).contains("EMP_NAME"), indexNames(conn, emp).toString());
      } finally {
        shutdown(conn);
      }
    }

    @Test
    void createAndLoadFailsWhenAnEntityHasNoIndexList() {
      TableAlgebra<Sym, Fk, Att> alg = sqlCompany();
      Pair<TObjectIntMap<String>, TIntObjectMap<String>> ids = alg.intifyX(0);
      RuntimeException e =
          assertThrows(RuntimeException.class, () -> alg.createAndLoad(Map.of(), ids, 32));
      assertInstanceOf(NullPointerException.class, e.getCause());
    }

    @Test
    void createAndLoadKeepsTheDatabaseAliveAfterItsConnectionCloses() throws SQLException {
      TableAlgebra<Sym, Fk, Att> alg = sqlCompany();
      Connection conn = alg.createAndLoad(noIndices(), alg.intifyX(0), 32);
      String url = conn.getMetaData().getURL();
      conn.close();
      try (Connection again = DriverManager.getConnection(url + ";IFEXISTS=TRUE")) {
        assertEquals(
            List.of("3"),
            rows(again, "select count(*) from " + alg.schema().truncate(EMP, true)));
        shutdown(again);
      }
    }

    @Test
    void addIndicesLoadsOnceAndRunsOnlyNewStatements() throws SQLException {
      TableAlgebra<Sym, Fk, Att> alg = sqlCompany();
      Schema<String, String, Sym, Fk, Att> s = alg.schema();
      String emp = s.truncate(EMP, true);
      String byName =
          "create index emp_name on " + emp + "(" + s.truncate(Chc.inRight(SQL_NAME), true) + ")";
      String byDept =
          "create index emp_dept on "
              + emp
              + "("
              + s.truncate(Chc.inLeft(SQL_WORKS_IN), true)
              + ")";
      Pair<TObjectIntMap<String>, TIntObjectMap<String>> ids = alg.intifyX(0);
      Connection first = alg.addIndices(ids, Map.of(EMP, List.of(byName), DEPT, List.of()), 32);
      try {
        Connection second =
            alg.addIndices(ids, Map.of(EMP, List.of(byName, byDept), DEPT, List.of()), 32);
        assertSame(first, second);
        Set<String> names = indexNames(first, emp);
        assertTrue(names.containsAll(Set.of("EMP_NAME", "EMP_DEPT")), names.toString());
        assertEquals(List.of("3"), rows(first, "select count(*) from " + emp));
      } finally {
        shutdown(first);
      }
    }

    @Test
    void addIndicesWrapsAFailingStatement() throws SQLException {
      TableAlgebra<Sym, Fk, Att> alg = sqlCompany();
      Pair<TObjectIntMap<String>, TIntObjectMap<String>> ids = alg.intifyX(0);
      Connection conn = alg.addIndices(ids, noIndices(), 32);
      try {
        Map<String, List<String>> broken = Map.of(EMP, List.of("create index on nowhere"));
        RuntimeException e =
            assertThrows(RuntimeException.class, () -> alg.addIndices(ids, broken, 32));
        assertInstanceOf(SQLException.class, e.getCause());
      } finally {
        shutdown(conn);
      }
    }

    @Test
    void finalizeClosesTheConnectionOfAddIndices() throws SQLException {
      TableAlgebra<Sym, Fk, Att> alg = sqlCompany();
      Connection conn = alg.addIndices(alg.intifyX(0), noIndices(), 32);
      String url = conn.getMetaData().getURL();
      alg.finalize();
      assertTrue(conn.isClosed());
      try (Connection again = DriverManager.getConnection(url + ";IFEXISTS=TRUE")) {
        shutdown(again);
      }
    }

    @Test
    void finalizeWithoutAConnectionDoesNothing() {
      TableAlgebra<Sym, Fk, Att> alg = sqlCompany();
      assertDoesNotThrow(alg::finalize);
    }

    private final Att number = Att.Att(TABLE, "n");
    private final Att other = Att.Att(TABLE, "o");

    /**
     * Over SqlTypeSide: the entity T with n : Numeric, o : Other and a : Varchar, where n is 9.99,
     * 123.4, a labelled null and absent.
     */
    private TableAlgebra<Sym, Fk, Att> numbers() {
      Map<Att, Pair<String, String>> atts = new LinkedHashMap<>();
      atts.put(number, new Pair<>(TABLE, "Numeric"));
      atts.put(other, new Pair<>(TABLE, "Other"));
      atts.put(COL_VARCHAR, new Pair<>(TABLE, VARCHAR));
      return new TableAlgebra<Sym, Fk, Att>(
              typedSqlSchema(List.of(TABLE), atts, Map.of(), List.of()))
          .withRows(TABLE, "r1", "r2", "r3", "r4")
          .withAtt(number, "r1", Term.Obj(Optional.of(new BigDecimal("9.99")), "Numeric"))
          .withAtt(number, "r2", Term.Obj(Optional.of(new BigDecimal("123.4")), "Numeric"))
          .withAtt(number, "r3", sqlSk("y2"))
          .withAtt(number, "r4", Term.Obj(Optional.empty(), "Numeric"))
          .withAtt(other, "r1", Term.Obj(Optional.of(42), "Other"))
          .withAtt(other, "r2", Term.Obj(Optional.empty(), "Other"))
          .withAtt(other, "r3", Term.Obj(Optional.empty(), "Other"))
          .withAtt(other, "r4", Term.Obj(Optional.empty(), "Other"))
          .withAtt(COL_VARCHAR, "r1", str("x"))
          .withAtt(COL_VARCHAR, "r2", sqlSk("y1"))
          .withAtt(COL_VARCHAR, "r3", str("z"))
          .withAtt(COL_VARCHAR, "r4", str("w"))
          .withTalg(new LinkedHashMap<>(Map.of("y1", VARCHAR, "y2", "Numeric")), List.of());
    }

    @Test
    void sqlColumnTypesSizeNumericColumnsFromTheirPresentValues() {
      Map<Att, Optional<SqlDialect.ColumnType>> types =
          numbers().sqlColumnTypes(SqlDialect.H2, 32);
      assertEquals(
          Optional.of(new SqlDialect.ColumnType("numeric(5,2)", Types.NUMERIC)), types.get(number));
      assertEquals(
          Optional.of(new SqlDialect.ColumnType("varchar(32)", Types.VARCHAR)),
          types.get(COL_VARCHAR));
      assertEquals(Optional.empty(), types.get(other));
    }

    @Test
    void sqlColumnTypesReadValuesOnlyForTypesWhoseColumnsHaveASize() {
      // a constant that is not an optional fails only where the values are read
      TableAlgebra<Sym, Fk, Att> alg = numbers().withAtt(COL_VARCHAR, "r1", Term.Obj("x", VARCHAR));
      assertDoesNotThrow(() -> alg.sqlColumnTypes(SqlDialect.H2, 32));
      alg.withAtt(number, "r1", Term.Obj(new BigDecimal("1"), "Numeric"));
      assertThrows(ClassCastException.class, () -> alg.sqlColumnTypes(SqlDialect.H2, 32));
    }

    @Test
    void createAndLoadKeepsTheScaleOfNumericValuesAndStoresOtherAsJavaObjects()
        throws SQLException {
      TableAlgebra<Sym, Fk, Att> alg = numbers();
      Connection conn = alg.createAndLoad(Map.of(TABLE, List.of()), alg.intifyX(0), 32);
      try {
        Schema<String, String, Sym, Fk, Att> s = alg.schema();
        String n = s.truncate(Chc.inRight(number), true);
        String o = s.truncate(Chc.inRight(other), true);
        // a numeric column without a scale would have rounded 9.99 to 10
        assertEquals(
            List.of("9.99|42", "123.40|null", "null|null", "null|null"),
            rows(conn, "select " + n + ", " + o + " from " + s.truncate(TABLE, true)));
      } finally {
        shutdown(conn);
      }
    }
  }
}
