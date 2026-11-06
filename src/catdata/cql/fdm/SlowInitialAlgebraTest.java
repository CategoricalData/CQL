package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.cql.AqlOptions;
import catdata.cql.DP;
import catdata.cql.Frozen;
import catdata.cql.Query;
import catdata.cql.Schema;
import catdata.cql.exp.AqlMultiDriver;
import catdata.cql.exp.AqlParserFactory;
import catdata.cql.exp.Att;
import catdata.cql.exp.Fk;
import catdata.cql.exp.Sym;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SlowInitialAlgebraTest {

  private static Iterator<Integer> freshIterator() {
    return new Iterator<>() {
      int i = 0;

      @Override
      public boolean hasNext() {
        return true;
      }

      @Override
      public Integer next() {
        return i++;
      }
    };
  }

  private static <Gen, Sk>
      SlowInitialAlgebra<String, String, String, String, String, Gen, Sk, Integer> create(
          Schema<String, String, String, String, String> schema,
          Map<Gen, String> gens,
          Map<Sk, String> sks) {
    DP<String, String, String, String, String, Gen, Sk> dp = schema.dp();
    return new SlowInitialAlgebra<>(
        u -> dp,
        schema,
        gens,
        sks,
        Collections.emptyList(),
        freshIterator(),
        g -> g.toString(),
        s -> s.toString(),
        AqlOptions.initialOptions);
  }

  @Nested
  class Constructor {

    @Test
    void constructorWithEmptyGenerators() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var alg = create(schema, Collections.emptyMap(), Collections.emptyMap());
      assertNotNull(alg);
    }

    @Test
    void schemaReturnsProvided() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var alg = create(schema, Collections.emptyMap(), Collections.emptyMap());
      assertEquals(schema, alg.schema());
    }

    @Test
    void emptyGeneratorsProducesEmptyEntity() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var alg = create(schema, Collections.emptyMap(), Collections.emptyMap());
      assertTrue(alg.en("E").isEmpty());
    }
  }

  @Nested
  class WithGenerators {

    @Test
    void singleGeneratorProducesOneElement() {
      var schema = FdmTestHelpers.singleEntitySchema();
      Map<String, String> gens = Collections.singletonMap("g1", "E");
      var alg = create(schema, gens, Collections.emptyMap());
      assertEquals(1, alg.en("E").size());
    }

    @Test
    void hasNullsReturnsTrueWhenNoSkolems() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var alg = create(schema, Collections.emptyMap(), Collections.emptyMap());
      assertTrue(alg.hasNulls());
    }
  }

  /**
   * The frozen instances of a query are built on this algebra. R binds a free String v and requires
   * f(a.n) = x, which the typeside does not imply; P binds a row alone.
   */
  @Nested
  class OfAQuery {

    private static final String PROGRAM =
        """
        typeside Ty = literal { types String constants x : String functions f : String -> String
          equations forall v. f(f(v)) = f(v) options prover = completion }
        schema S = literal : Ty { entities A attributes n : A -> String }
        schema T = literal : Ty { entities R P attributes r : R -> String }
        query Q = literal : S -> T {
          entity R -> { from a:A v:String where f(a.n) = x attributes r -> v }
          entity P -> { from a:A }
        }
        """;

    @SuppressWarnings("unchecked")
    private static Frozen<String, String, Sym, Fk, Att> frozen(String en) throws Exception {
      AqlMultiDriver d =
          new AqlMultiDriver(AqlParserFactory.getParser().parseProgram(PROGRAM), null);
      d.start();
      var q = (Query<String, String, Sym, Fk, Att, String, Fk, Att>) d.env.defs.qs.get("Q");
      return q.ens.get(en);
    }

    @Test
    void reportsATypeAlgebraWithAnEquationOfItsOwn() throws Exception {
      var r = frozen("R").algebra();
      assertFalse(r.hasFreeTypeAlgebra());
      assertFalse(r.hasFreeTypeAlgebra());
      assertTrue(r.hasFreeTypeAlgebraOnJava());
      assertTrue(r.talgToString().contains("f(\"inr (id0, n)\") = x"), r.talgToString());
      assertEquals("\"inl v\"", r.sk("v").toString());
    }

    @Test
    void reportsAFreeTypeAlgebraWithoutEquations() throws Exception {
      var p = frozen("P").algebra();
      assertTrue(p.hasFreeTypeAlgebra());
      assertTrue(p.hasFreeTypeAlgebraOnJava());
    }

    @Test
    void isItsOwnDecisionProcedure() throws Exception {
      var alg = (SlowInitialAlgebra<?, ?, ?, ?, ?, ?, ?, ?>) frozen("P").algebra();
      assertSame(alg, alg.dp());
      assertTrue(
          alg.toStringProver().startsWith("Definitional simplification"), alg.toStringProver());
    }
  }
}
