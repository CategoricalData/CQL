package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.Pair;
import catdata.Unit;
import catdata.cql.Instance;
import catdata.cql.Kind;
import catdata.cql.Term;
import catdata.cql.Transform;
import catdata.cql.exp.InstExp.InstExpVar;
import catdata.cql.exp.InstExp.InstExpVisitor;
import catdata.cql.exp.SchExp.SchExpVar;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link InstExpSkolem} and {@link TransExpSkolem}, {@code instance K = skolem I}
 * and {@code transform t = skolem I}: the instance whose SQL nulls are labelled nulls, constrained
 * by {@code isNull}, and the transform from it back to I.
 */
class InstExpSkolemTest {

  /** Every value of n is given, the one of b as a SQL null. */
  private static final String SQL =
      """
      typeside Ty = sql
      schema S = literal : Ty { entities A attributes n : A -> Varchar }
      instance I = literal : S { generators a b : A equations a.n = hello b.n = null }
      """;

  /**
   * K and t are in programs of their own. In one program the driver evaluates them in parallel, and
   * the two can deadlock: each validates a transform out of I, and they take the lock of I and the
   * lock of a map of I in opposite orders.
   */
  private static final String SQL_K = SQL + "instance K = skolem I\n";

  private static final String SQL_T = SQL + "transform t = skolem I\n";

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static InstExpSkolem<?, ?, ?, ?> inst(String i) {
    return new InstExpSkolem(new InstExpVar(i));
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static TransExpSkolem<?, ?, ?, ?> trans(String i) {
    return new TransExpSkolem(new InstExpVar(i));
  }

  @Nested
  class Evaluation {

    @Test
    void namesEveryValueByALabelledNull() throws Exception {
      Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?> k =
          ExpTestHelpers.eval(SQL_K, "K", Kind.INSTANCE);
      assertEquals(2, k.algebra().size("A"));
      assertEquals(2, k.sks().size());
      String text = k.toString();
      assertTrue(text.contains("isNull(b.n) = true"), text);
      assertTrue(text.contains("isNull(a.n) = false"), text);
    }

    @Test
    void mapsBackToTheInstance() throws Exception {
      Transform<String, String, Sym, Fk, Att, ?, ?, ?, ?, ?, ?, ?, ?> t =
          ExpTestHelpers.eval(SQL_T, "t", Kind.TRANSFORM);
      assertEquals(2, t.src().sks().size());
      assertEquals(0, t.dst().sks().size());
    }

    /** A labelled null of I, or a value that is not a SQL value, reaches an anomaly. */
    @Test
    void failsOnAMissingValue() throws Exception {
      String msg = ExpTestHelpers.error(SQL_K.replace(" b.n = null", ""));
      assertTrue(msg.contains("Anomaly: please report"), msg);
    }

    @Test
    void failsOffTheSqlTypeside() throws Exception {
      String program =
          """
          typeside Ty = literal { types String constants x : String }
          schema S = literal : Ty { entities A attributes n : A -> String }
          instance I = literal : S { generators a : A equations a.n = x }
          instance K = skolem I
          """;
      assertTrue(ExpTestHelpers.error(program).contains("Anomaly: please report"));
    }
  }

  /**
   * K is {@code skolem I} for I imported from an H2 table with SQL nulls, the way the Warehouse
   * examples build their instances from Oracle and SQL Server. An import names its rows from a
   * counter that all imports in the JVM share, so these tests identify a row by its values.
   */
  @Nested
  class OfAnImportedTable {

    private static final String CLIENT = "PUBLIC.CLIENT";

    /** ID is a key of the table. */
    private static final String KEY =
        "constraints C = literal : S {"
            + " forall x y : \"PUBLIC.CLIENT\" where x.ID = y.ID -> where x = y }\n";

    private static final String ANN = "COUNTRY = CO, ID = 1, NAME = Ann";
    private static final String BO = "COUNTRY = null, ID = 2, NAME = Bo";
    private static final String CY = "COUNTRY = null, ID = null, NAME = Cy";
    private static final String US = "COUNTRY = US, ID = null, NAME = null";

    private String url;

    @BeforeEach
    void createTheTable() throws SQLException {
      url =
          ExpTestHelpers.h2(
              "create table CLIENT(ID int, NAME varchar(10), COUNTRY varchar(10))",
              "insert into CLIENT values (1, 'Ann', 'CO'), (2, 'Bo', null),"
                  + " (null, 'Cy', null), (null, null, 'US')");
    }

    /** The import of the table, followed by the rest of a program. */
    private String program(String rest) {
      return """
          schema S = import_jdbc_all "%1$s"
          instance I = import_jdbc_direct "%1$s" "ROW_NUMBER() OVER ()" : S
          """
              .formatted(url)
          + rest;
    }

    /**
     * A row by its values, sorted by column. A labelled null shows as null, after an assertion that
     * it is the labelled null of that row and column.
     */
    private static <X, Y> String row(Instance<String, String, Sym, Fk, Att, ?, ?, X, Y> k, X x) {
      List<String> values = new ArrayList<>();
      for (Att att : k.schema().attsFrom(CLIENT)) {
        Term<String, Void, Sym, Void, Void, Void, Y> v = k.algebra().att(att, x);
        if (v.sk() != null) {
          assertEquals(new Pair<>(x, att), v.sk());
          values.add(att + " = null");
        } else {
          values.add(att + " = " + v);
        }
      }
      Collections.sort(values);
      return String.join(", ", values);
    }

    /** The rows of K, sorted. */
    private static <X, Y> List<String> rows(Instance<String, String, Sym, Fk, Att, ?, ?, X, Y> k) {
      List<String> rows = new ArrayList<>();
      for (X x : k.algebra().en(CLIENT)) {
        rows.add(row(k, x));
      }
      Collections.sort(rows);
      return rows;
    }

    /**
     * For each row of K, the lines that name one of its labelled nulls, sorted, each with the
     * labelled null written as its column.
     */
    private static <X, Y> Map<String, List<String>> byRow(
        Instance<String, String, Sym, Fk, Att, ?, ?, X, Y> k, Collection<String> lines) {
      Map<String, List<String>> byRow = new TreeMap<>();
      for (X x : k.algebra().en(CLIENT)) {
        List<String> mine = new ArrayList<>();
        for (Att att : k.schema().attsFrom(CLIENT)) {
          String sk = Term.Sk(new Pair<>(x, att)).toString();
          for (String line : lines) {
            if (line.contains(sk)) {
              mine.add(line.replace(sk, att.toString()));
            }
          }
        }
        Collections.sort(mine);
        byRow.put(row(k, x), mine);
      }
      return byRow;
    }

    /**
     * Each labelled null of the source of t, with the term of I that t maps it to, and the row of
     * that term written as x.
     */
    private static <Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> List<String> images(
        Transform<String, String, Sym, Fk, Att, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> t) {
      List<String> images = new ArrayList<>();
      t.src()
          .sks()
          .entrySet(
              (sk, ty) -> {
                var image = t.sks().apply(sk, ty);
                images.add(
                    Term.Sk(sk) + " -> " + image.toString().replace(image.arg.toString(), "x"));
              });
      return images;
    }

    /** Every value of I is a labelled null of K, equal to the value when it is no SQL null. */
    @Test
    void keepsEveryValueAndGivesEverySqlNullALabelledNullOfItsOwn() throws Exception {
      Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?> k =
          ExpTestHelpers.eval(program("instance K = skolem I\n"), "K", Kind.INSTANCE);
      assertEquals(List.of(ANN, US, BO, CY), rows(k));
      assertEquals(12, k.sks().size());
    }

    /** Each value has an isNull equation, and each value that is no SQL null an equation too. */
    @Test
    void recordsWhichValuesWereNull() throws Exception {
      Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?> k =
          ExpTestHelpers.eval(program("instance K = skolem I\n"), "K", Kind.INSTANCE);
      List<String> eqs = new ArrayList<>();
      for (var eq : k.algebra().talg().eqsNoDefns()) {
        eqs.add(eq.first + " = " + eq.second);
      }
      assertEquals(19, eqs.size());
      assertEquals(
          Map.of(
              ANN,
              List.of(
                  "COUNTRY = CO",
                  "ID = 1",
                  "NAME = Ann",
                  "isNull(COUNTRY) = false",
                  "isNull(ID) = false",
                  "isNull(NAME) = false"),
              BO,
              List.of(
                  "ID = 2",
                  "NAME = Bo",
                  "isNull(COUNTRY) = true",
                  "isNull(ID) = false",
                  "isNull(NAME) = false"),
              CY,
              List.of(
                  "NAME = Cy",
                  "isNull(COUNTRY) = true",
                  "isNull(ID) = true",
                  "isNull(NAME) = false"),
              US,
              List.of(
                  "COUNTRY = US",
                  "isNull(COUNTRY) = false",
                  "isNull(ID) = true",
                  "isNull(NAME) = true")),
          byRow(k, eqs));
    }

    /**
     * In I the two SQL null IDs are one value, so the key fails. In K they are two labelled nulls,
     * so the key holds. This is why the Warehouse examples skolemize what they import.
     */
    @Test
    void satisfiesAKeyThatTheImportViolates() throws Exception {
      AqlMultiDriver d =
          ExpTestHelpers.run(program("instance K = skolem I\n" + KEY + "command ok = check C K\n"));
      assertTrue(d.exn.isEmpty(), d.exn::toString);
      String msg = ExpTestHelpers.error(program(KEY + "command ok = check C I\n"));
      assertTrue(msg.startsWith("Not satisfied."), msg);
      assertTrue(msg.contains("[ID:, NAME:Cy, COUNTRY:]"), msg);
      assertTrue(msg.contains("[ID:, NAME:, COUNTRY:US]"), msg);
    }

    /** Every labelled null maps to the value of its row and column in I, SQL null or not. */
    @Test
    void mapsEveryLabelledNullToItsRowAndColumn() throws Exception {
      Transform<String, String, Sym, Fk, Att, ?, ?, ?, ?, ?, ?, ?, ?> t =
          ExpTestHelpers.eval(program("transform t = skolem I\n"), "t", Kind.TRANSFORM);
      List<String> each = List.of("COUNTRY -> x.COUNTRY", "ID -> x.ID", "NAME -> x.NAME");
      assertEquals(Map.of(ANN, each, BO, each, CY, each, US, each), byRow(t.src(), images(t)));
    }
  }

  @Nested
  class Expression {

    @Test
    void printsTheInstance() {
      assertEquals("skolem I", inst("I").toString());
      assertEquals("skolem I", trans("I").toString());
    }

    @Test
    void isTypedAsItsInstance() throws Exception {
      AqlTyping g = ExpTestHelpers.typing(SQL);
      assertEquals(new SchExpVar("S"), inst("I").type(g));
      assertEquals(new Pair<>(inst("I"), new InstExpVar("I")), trans("I").type(g));
      assertEquals(Set.of(new InstExpVar("I")), Set.copyOf(inst("I").direct(g)));
    }

    @Test
    void dependsOnTheInstance() {
      for (Exp<?> e : List.<Exp<?>>of(inst("I"), trans("I"))) {
        assertEquals(List.of(new Pair<>("I", Kind.INSTANCE)), new ArrayList<>(e.deps()));
        assertTrue(e.options().isEmpty());
        assertEquals(ExpTestHelpers.BASE_OPTIONS, e.allowedOptions());
        List<Exp<?>> seen = new ArrayList<>();
        e.map(seen::add);
        assertEquals(List.of(e, new InstExpVar("I")), seen);
      }
    }

    @Test
    void equalityComparesTheInstance() {
      assertEquals(inst("I"), inst("I"));
      assertEquals(inst("I").hashCode(), inst("I").hashCode());
      assertNotEquals(inst("I"), inst("J"));
      assertNotEquals(inst("I"), null);
      assertNotEquals(inst("I"), "skolem");
      InstExpSkolem<?, ?, ?, ?> i = inst("I");
      assertEquals(i, i);
      TransExpSkolem<?, ?, ?, ?> t = trans("I");
      assertEquals(t, t);
      assertEquals(t, trans("I"));
      assertEquals(t.hashCode(), trans("I").hashCode());
      assertNotEquals(t, trans("J"));
      assertNotEquals(t, null);
      assertNotEquals(t, "skolem");
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsAVisitor() {
      InstExpSkolem<?, ?, ?, ?> i = inst("I");
      InstExpVisitor<String, Unit, RuntimeException> v = mock(InstExpVisitor.class);
      when(v.visit(Unit.unit, i)).thenReturn("instance");
      assertEquals("instance", i.accept(Unit.unit, v));
      TransExpSkolem<?, ?, ?, ?> t = trans("I");
      TransExpVisitor<String, Unit, RuntimeException> w = mock(TransExpVisitor.class);
      when(w.visit(Unit.unit, t)).thenReturn("transform");
      assertEquals("transform", t.accept(Unit.unit, w));
    }
  }
}
