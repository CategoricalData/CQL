package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.cql.AqlOptions;
import catdata.cql.Kind;
import catdata.cql.Query;
import catdata.cql.exp.AqlMultiDriver;
import catdata.cql.exp.AqlParserFactory;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CoEvalTransformTest {

  @Nested
  class Constructor {

    @Test
    void constructorSetsSrcAndDst() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var idTransform = new IdentityTransform<>(inst, Optional.empty());
      var coeval =
          new CoEvalTransform<>(
              query, idTransform, AqlOptions.initialOptions, AqlOptions.initialOptions);
      assertNotNull(coeval.src());
      assertNotNull(coeval.dst());
    }
  }

  @Nested
  class TransformBehavior {

    @Test
    void srcIsCoEvalInstance() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var idTransform = new IdentityTransform<>(inst, Optional.empty());
      var coeval =
          new CoEvalTransform<>(
              query, idTransform, AqlOptions.initialOptions, AqlOptions.initialOptions);
      assertTrue(coeval.src() instanceof CoEvalInstance);
    }

    @Test
    void gensIsNotNull() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var idTransform = new IdentityTransform<>(inst, Optional.empty());
      var coeval =
          new CoEvalTransform<>(
              query, idTransform, AqlOptions.initialOptions, AqlOptions.initialOptions);
      assertNotNull(coeval.gens());
    }

    @Test
    void sksIsNotNull() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var query = Query.id(AqlOptions.initialOptions, schema, schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var idTransform = new IdentityTransform<>(inst, Optional.empty());
      var coeval =
          new CoEvalTransform<>(
              query, idTransform, AqlOptions.initialOptions, AqlOptions.initialOptions);
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

  /** {@code coeval Q t} for a transform t whose source has the labelled null s. */
  @Nested
  class OfAProgram {

    @Test
    void sendsTheNullsOfTheSourceToTheirImages() throws Exception {
      AqlMultiDriver d =
          run(
              """
              typeside Ty = literal { types String constants x : String }
              schema S = literal : Ty { entities A attributes n : A -> String }
              schema T = literal : Ty { entities X attributes m : X -> String }
              query Q = literal : S -> T { entity X -> { from a:A attributes m -> a.n } }
              instance J1 = literal : T { generators x1 : X s : String equations x1.m = s }
              instance J2 = literal : T { generators y1 : X equations y1.m = x }
              transform t = literal : J1 -> J2 { generators x1 -> y1 s -> x }
              transform u = coeval Q t
              """);
      assertEquals("", error(d));
      String u = d.env.defs.get("u", Kind.TRANSFORM).toString();
      assertTrue(u.contains("\"inr inl s\" -> x"), u);
    }
  }
}
