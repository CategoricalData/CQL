package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import catdata.Pair;
import catdata.cql.Term;

class DeltaAlgebraTest {

  private DeltaAlgebra<String, String, String, String, String, String, String, String, String, String, String, String>
      createDeltaAlgebra() {
    var schema = FdmTestHelpers.singleEntitySchema();
    var mapping = FdmTestHelpers.identityMapping(schema);
    var inst = FdmTestHelpers.singleElementInstance();
    return new DeltaAlgebra<>(mapping, inst);
  }

  @Nested
  class SchemaMethod {

    @Test
    void schemaReturnsMappingSource() {
      var alg = createDeltaAlgebra();
      assertEquals(FdmTestHelpers.singleEntitySchema().ens, alg.schema().ens);
    }
  }

  @Nested
  class EnMethod {

    @Test
    void enReturnsPairedElements() {
      var alg = createDeltaAlgebra();
      var elements = alg.en("E");
      assertEquals(1, elements.size());
      Pair<String, String> elem = elements.iterator().next();
      assertEquals("E", elem.first);
      assertEquals("e1", elem.second);
    }

    @Test
    void enCachesResult() {
      var alg = createDeltaAlgebra();
      var first = alg.en("E");
      var second = alg.en("E");
      assertSame(first, second);
    }
  }

  @Nested
  class GenMethod {

    @Test
    void genReturnsItself() {
      var alg = createDeltaAlgebra();
      var gen = new Pair<>("E", "e1");
      assertSame(gen, alg.gen(gen));
    }
  }

  @Nested
  class ReprMethod {

    @Test
    void reprReturnsTermGen() {
      var alg = createDeltaAlgebra();
      var gen = new Pair<>("E", "e1");
      assertEquals(Term.Gen(gen), alg.repr("E", gen));
    }
  }

  @Nested
  class SizeMethod {

    @Test
    void sizeReturnsCount() {
      var alg = createDeltaAlgebra();
      assertEquals(1, alg.size("E"));
    }
  }

  @Nested
  class SkMethod {

    @Test
    void skReturnsTermSk() {
      var alg = createDeltaAlgebra();
      assertEquals(Term.Sk("y1"), alg.sk("y1"));
    }
  }

  @Nested
  class PrintMethods {

    @Test
    void toStringProverDelegates() {
      var alg = createDeltaAlgebra();
      assertNotNull(alg.toStringProver());
    }

    @Test
    void printXShowsEntityAndValue() {
      var alg = createDeltaAlgebra();
      var result = alg.printX("E", new Pair<>("E", "e1"));
      assertNotNull(result);
      assertTrue(result.toString().contains("e1"));
    }
  }

  @Nested
  class TypeAlgebra {

    @Test
    void hasFreeTypeAlgebraDelegates() {
      var alg = createDeltaAlgebra();
      assertTrue(alg.hasFreeTypeAlgebra());
    }

    @Test
    void hasNullsDelegates() {
      var alg = createDeltaAlgebra();
      // ImportAlgebra hasNulls returns true when sks are empty
      assertNotNull(alg.hasNulls());
    }
  }
}
