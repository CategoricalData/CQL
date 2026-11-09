package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.cql.AqlOptions;
import catdata.cql.Kind;
import catdata.cql.Query;
import catdata.cql.exp.AqlMultiDriver;
import catdata.cql.exp.AqlParserFactory;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CoEvalEvalUnitTransformTest {

  @Nested
  class Constructor {

    @Test
    void constructorCreatesTransform() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var t = new CoEvalEvalUnitTransform<>(query, inst, AqlOptions.initialOptions);
      assertNotNull(t.src());
      assertNotNull(t.dst());
    }
  }

  @Nested
  class TransformBehavior {

    @Test
    void srcIsOriginalInstance() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var t = new CoEvalEvalUnitTransform<>(query, inst, AqlOptions.initialOptions);
      assertNotNull(t.src());
    }

    @Test
    void dstIsEvalInstance() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var t = new CoEvalEvalUnitTransform<>(query, inst, AqlOptions.initialOptions);
      assertTrue(t.dst() instanceof EvalInstance);
    }

    @Test
    void gensIsNotNull() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var t = new CoEvalEvalUnitTransform<>(query, inst, AqlOptions.initialOptions);
      assertNotNull(t.gens());
    }

    @Test
    void sksIsNotNull() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var t = new CoEvalEvalUnitTransform<>(query, inst, AqlOptions.initialOptions);
      assertNotNull(t.sks());
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

  /** {@code unit_query Q J} for an instance J over the target of Q, which copies A to X. */
  @Nested
  class OfAProgram {

    private static String program(String generators) {
      return """
          typeside Ty = literal { types String constants x : String }
          schema S = literal : Ty { entities A attributes n : A -> String }
          schema T = literal : Ty { entities X attributes m : X -> String }
          query Q = literal : S -> T { entity X -> { from a:A attributes m -> a.n } }
          instance J = literal : T { generators %s }
          transform v = unit_query Q J
          """
          .formatted(generators);
    }

    @Test
    void sendsEachRowToItsTupleOfTheEvaluation() throws Exception {
      for (String g : List.of("x1 : X equations x1.m = x", "x1 : X")) {
        AqlMultiDriver d = run(program(g));
        assertEquals("", error(d), g);
        String v = d.env.defs.get("v", Kind.TRANSFORM).toString();
        assertTrue(v.contains("x1 -> \"(a=inl 0)\""), v);
      }
    }

    /** The null s is renamed to a null that the evaluation has already defined away. */
    @Test
    void failsOnALabelledNullOfTheInstance() throws Exception {
      String msg = error(run(program("x1 : X s : String equations x1.m = s")));
      assertTrue(msg.contains("the labelled null inl inr inl s has no associated type"), msg);
    }

    /** A from clause that binds a String gives each tuple a value as well as a row. */
    @Test
    void bindsTheValuesOfTheFromClause() throws Exception {
      AqlMultiDriver d =
          run(
              program("x1 : X equations x1.m = x")
                  .replace(
                      "{ from a:A attributes m -> a.n }",
                      "{ from a:A v:String where a.n = v attributes m -> v }"));
      assertEquals("", error(d));
    }
  }
}
