package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.cql.AqlOptions;
import catdata.cql.Instance;
import catdata.cql.Query;
import catdata.cql.exp.AqlMultiDriver;
import catdata.cql.exp.AqlParserFactory;
import catdata.cql.exp.Att;
import catdata.cql.exp.Fk;
import catdata.cql.exp.Sym;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EvalAlgebraTest {

  @Nested
  class ViaEvalInstance {

    @Test
    void algebraSchemaMatchesQuery() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var eval = new EvalInstance<>(query, inst, AqlOptions.initialOptions);
      assertEquals(schema.ens, eval.algebra().schema().ens);
    }

    @Test
    void algebraEnReturnsElements() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var eval = new EvalInstance<>(query, inst, AqlOptions.initialOptions);
      assertTrue(eval.algebra().en("E").iterator().hasNext());
    }

    @Test
    void algebraSizeMatchesInput() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var eval = new EvalInstance<>(query, inst, AqlOptions.initialOptions);
      assertEquals(1, eval.algebra().size("E"));
    }

    @Test
    void algebraToStringProverNotNull() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var eval = new EvalInstance<>(query, inst, AqlOptions.initialOptions);
      assertNotNull(eval.algebra().toStringProver());
    }
  }

  private static AqlMultiDriver run(String program) throws Exception {
    AqlMultiDriver d = new AqlMultiDriver(AqlParserFactory.getParser().parseProgram(program), null);
    d.start();
    return d;
  }

  @SuppressWarnings("unchecked")
  private static Instance<String, String, Sym, Fk, Att, Object, Object, Object, Object> instance(
      AqlMultiDriver d, String name) {
    return (Instance<String, String, Sym, Fk, Att, Object, Object, Object, Object>)
        d.env.defs.insts.get(name);
  }

  private static String error(AqlMultiDriver d) {
    return d.env.exn == null ? "" : d.env.exn.getMessage();
  }

  /**
   * {@code eval_use_sql_above = 0} sends every evaluation of an instance over the {@code sql}
   * typeside through H2. C joins A and B, D copies A, and E has no from clause.
   */
  @Nested
  class OverSql {

    private static final String PROGRAM =
        """
        typeside Ty = sql
        schema S = literal : Ty { entities A B foreign_keys f : A -> B
          attributes n : A -> Varchar m : B -> Varchar }
        instance I = literal : S { generators a1 a2 a3 : A b1 b2 : B
          equations a1.f = b1 a2.f = b1 a3.f = b2 a1.n = x a2.n = y a3.n = z b1.m = p b2.m = q }
        schema R = literal : Ty { entities C D E attributes c : C -> Varchar }
        query Q = literal : S -> R {
          entity C -> { from a:A b:B where a.f = b attributes c -> b.m }
          entity D -> { from a:A }
          entity E -> { }
        }
        instance J = eval Q I
        """;

    private static String withOptions(String options) {
      return PROGRAM + "instance K = eval Q I { options " + options + " }\n";
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          "eval_use_sql_above = 0",
          "eval_use_sql_above = 0 eval_sql_persistent_indices = true"
        })
    void findsTheRowsThatTheEvaluationWithoutSqlFinds(String options) throws Exception {
      AqlMultiDriver d = run(withOptions(options));
      assertEquals("", error(d));
      var j = instance(d, "J").algebra();
      var k = instance(d, "K").algebra();
      for (String en : List.of("C", "D", "E")) {
        assertEquals(j.size(en), k.size(en), en);
      }
      assertEquals(List.of(3, 3, 1), List.of(k.size("C"), k.size("D"), k.size("E")));
      Att c = Att.Att("C", "c");
      List<String> values = new ArrayList<>();
      k.en("C").forEach(x -> values.add(k.att(c, x).toString()));
      values.sort(null);
      assertEquals(List.of("p", "p", "q"), values);
    }

    /** The help says that {@code eval_use_indices} also applies to the SQL path. */
    @Test
    void failsWithoutIndices() throws Exception {
      String msg = error(run(withOptions("eval_use_sql_above = 0 eval_use_indices = false")));
      assertTrue(
          msg.contains("because the return value of \"java.util.Map.get(Object)\" is null"), msg);
    }

    @Test
    void reportsTheErrorOfTheDatabase() throws Exception {
      String msg =
          error(
              run(
                  """
                  typeside Ty = sql
                  schema S = literal : Ty { entities Emp attributes ok : Emp -> Boolean }
                  schema T = literal : Ty { entities R }
                  instance I = literal : S { generators e : Emp equations e.ok = true }
                  query Q = literal : S -> T { entity R -> { from e:Emp where e.ok = true } }
                  instance K = eval Q I { options eval_use_sql_above = 0 }
                  """));
      assertTrue(msg.contains("org.h2.jdbc.JdbcSQLSyntaxErrorException"), msg);
    }

    /**
     * With a labelled null the type algebra has a variable, so the evaluation stays off SQL unless
     * {@code eval_approx_sql_unsafe} is set; only then does binding a type in a from clause fail.
     */
    @Test
    void goesThroughSqlOnALabelledNullOnlyWhenAllowed() throws Exception {
      String program =
          """
          options active_domain = true
          typeside Ty = sql
          schema S = literal : Ty { entities A attributes n : A -> Varchar }
          schema T = literal : Ty { entities R }
          instance I = literal : S { generators a1 a2 : A equations a1.n = x }
          query Q = literal : S -> T { entity R -> { from a:A v:Varchar } }
          instance K = eval Q I { options eval_use_sql_above = 0
          """;
      assertEquals("", error(run(program + "}\n")));
      String msg = error(run(program + "eval_approx_sql_unsafe = true }\n"));
      assertTrue(msg.contains("FROM clauses can't bind types in SQL.  Given: "), msg);
    }
  }

  /**
   * R pairs each row of A with each Color of the instance, red, green and the null of a2, and S
   * copies A. {@code printX} prints every column with the entity of the last column of the row.
   */
  @Nested
  class Printing {

    private static final String PROGRAM =
        """
        typeside Ty = literal { types Color constants red green : Color }
        schema S = literal : Ty { entities A attributes n : A -> Color }
        schema T = literal : Ty { entities R S attributes r : R -> Color }
        instance I = literal : S { generators a1 a2 : A equations a1.n = red }
        query Q = literal : S -> T {
          entity R -> { from a:A c:Color attributes r -> c }
          entity S -> { from a:A }
        }
        instance K = eval Q I
        """;

    @Test
    void printsARowOfOneEntityByTheRowsOfTheInstance() throws Exception {
      var k = instance(run(PROGRAM), "K").algebra();
      List<String> rows = new ArrayList<>();
      k.en("S").forEach(x -> rows.add(k.printX("S", x).toString()));
      rows.sort(null);
      assertEquals(List.of("(a=a1)", "(a=a2)"), rows);
    }

    /** The last column is c, a Color, so the entity found for a is null. */
    @Test
    void failsToPrintARowThatPairsARowWithAValue() throws Exception {
      var k = instance(run(PROGRAM), "K").algebra();
      assertEquals(6, k.size("R"));
      Att r = Att.Att("R", "r");
      List<String> values = new ArrayList<>();
      for (Object x : k.en("R")) {
        values.add(k.att(r, x).toString());
        RuntimeException e = assertThrows(RuntimeException.class, () -> k.printX("R", x));
        assertEquals("Anomaly: please report", e.getMessage());
      }
      assertTrue(values.containsAll(List.of("red", "green")), values.toString());
    }

    /** Over a {@code sigma_chase} result, the row 2 of B is printed as a row of A. */
    @Test
    void failsToPrintARowOfTwoEntities() throws Exception {
      AqlMultiDriver d =
          run(
              """
              typeside Ty = literal { types String }
              schema S = literal : Ty { entities A B }
              instance I0 = literal : S { generators a1 a2 : A b1 : B }
              instance I = sigma_chase (identity S) I0
              schema T = literal : Ty { entities R }
              query Q = literal : S -> T { entity R -> { from a:A b:B } }
              instance K = eval Q I
              """);
      var k = instance(d, "K").algebra();
      assertEquals(2, k.size("R"));
      for (Object x : k.en("R")) {
        RuntimeException e = assertThrows(RuntimeException.class, () -> k.printX("R", x));
        assertTrue(e.getMessage().startsWith("Can't get int 2 at entity A"), e.getMessage());
      }
    }

    @Test
    void delegatesItsTypeAlgebraToTheInstance() throws Exception {
      AqlMultiDriver d = run(PROGRAM);
      var i = instance(d, "I").algebra();
      var k = instance(d, "K").algebra();
      Object y = k.talg().sks.keySet().iterator().next();
      assertEquals(i.printY("Color", y), k.printY("Color", y));
      assertEquals(i.talgToString(), k.talgToString());
      assertEquals(i.hasNulls(), k.hasNulls());
      assertEquals(i.hasFreeTypeAlgebraOnJava(), k.hasFreeTypeAlgebraOnJava());
    }

    @Test
    void rejectsAnInstanceOfAnotherSchema() throws Exception {
      AqlMultiDriver d =
          run(
              """
              typeside Ty = sql
              schema S = literal : Ty { entities A }
              schema S2 = literal : Ty { entities A2 }
              query Q = identity S
              instance I = literal : S2 { generators a : A2 }
              """);
      @SuppressWarnings("unchecked")
      Query<String, String, Sym, Fk, Att, String, Fk, Att> q =
          (Query<String, String, Sym, Fk, Att, String, Fk, Att>) d.env.defs.qs.get("Q");
      var i = instance(d, "I");
      RuntimeException e =
          assertThrows(
              RuntimeException.class, () -> new EvalAlgebra<>(q, i, AqlOptions.initialOptions));
      assertEquals("Anomaly: please report", e.getMessage());
    }
  }
}
