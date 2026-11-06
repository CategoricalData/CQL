package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

class CoEvalInstanceTest {

  @Nested
  class Constructor {

    @Test
    void constructorSetsSchema() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var coeval = new CoEvalInstance<>(query, inst, AqlOptions.initialOptions);
      assertEquals(schema.ens, coeval.schema().ens);
    }
  }

  @Nested
  class InstanceBehavior {

    @Test
    void algebraIsNotNull() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var coeval = new CoEvalInstance<>(query, inst, AqlOptions.initialOptions);
      assertNotNull(coeval.algebra());
    }

    @Test
    void dpIsNotNull() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var coeval = new CoEvalInstance<>(query, inst, AqlOptions.initialOptions);
      assertNotNull(coeval.dp());
    }

    @Test
    void gensIsNotNull() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var coeval = new CoEvalInstance<>(query, inst, AqlOptions.initialOptions);
      assertNotNull(coeval.gens());
    }

    @Test
    void sksIsNotNull() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var coeval = new CoEvalInstance<>(query, inst, AqlOptions.initialOptions);
      assertNotNull(coeval.sks());
    }
  }

  private static AqlMultiDriver run(String program) throws Exception {
    AqlMultiDriver d = new AqlMultiDriver(AqlParserFactory.getParser().parseProgram(program), null);
    d.start();
    return d;
  }

  private static String error(AqlMultiDriver d) {
    return d.env.exn == null ? "" : d.env.exn.getMessage();
  }

  /** K is {@code coeval Q J}; J has the row x1 with m = x and the row x2 with m = s, a null. */
  @Nested
  class OfAProgram {

    private static final String PROGRAM =
        """
        typeside Ty = literal { types String constants x : String }
        schema S = literal : Ty { entities A B foreign_keys f : A -> B attributes n : A -> String }
        schema T = literal : Ty { entities X attributes m : X -> String }
        query Q = literal : S -> T { entity X -> { from a:A attributes m -> a.n } }
        instance J = literal : T { generators x1 x2 : X s : String equations x1.m = x x2.m = s }
        instance K = coeval Q J
        """;

    @SuppressWarnings("unchecked")
    private Instance<String, String, Sym, Fk, Att, Object, Object, Object, Object> k()
        throws Exception {
      AqlMultiDriver d = run(PROGRAM);
      assertEquals("", error(d));
      return (Instance<String, String, Sym, Fk, Att, Object, Object, Object, Object>)
          d.env.defs.insts.get("K");
    }

    @Test
    void printsEachRowByTheRowOfTheInstanceItComesFrom() throws Exception {
      var alg = k().algebra();
      List<String> rows = new ArrayList<>();
      alg.en("A").forEach(a -> rows.add(alg.printX("A", a).toString()));
      rows.sort(null);
      assertEquals(List.of("[a=x1]", "[a=x2]"), rows);
      List<String> printed = new ArrayList<>();
      for (Object y : alg.talg().sks.keySet()) {
        printed.add(alg.printY("String", y).toString());
      }
      assertEquals(alg.talg().sks.size(), printed.size());
    }

    /** Each row of A has its own f, since nothing in J equates them. */
    @Test
    void takesItsJavaFlagFromTheInstanceAndAddsTheForeignKeys() throws Exception {
      AqlMultiDriver d = run(PROGRAM);
      var j = d.env.defs.insts.get("J");
      var k = d.env.defs.insts.get("K");
      assertEquals(j.allowUnsafeJava(), k.allowUnsafeJava());
      assertEquals(2, k.algebra().size("B"));
    }

    @Test
    void refusesAnAggregate() throws Exception {
      String msg =
          error(
              run(
                  "options allow_aggregation_unsafe = true\n"
                      + """
                      typeside Ty = sql
                      schema S = literal : Ty { entities A B foreign_keys f : A -> B
                        attributes n : A -> Integer }
                      schema T = literal : Ty { entities Y attributes total : Y -> Integer }
                      query Q = literal : S -> T { entity Y -> { from b:B attributes total ->
                        from a:A where a.f = b return a.n aggregate 0 lambda x y. "+"(x, y) } }
                      instance J = literal : T { generators y : Y }
                      instance K = coeval Q J
                      """));
      assertTrue(msg.contains("Cannot co-eval with aggregation"), msg);
    }
  }
}
