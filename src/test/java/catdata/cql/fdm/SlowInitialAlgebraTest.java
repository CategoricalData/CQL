package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import catdata.cql.AqlOptions;
import catdata.cql.DP;
import catdata.cql.Schema;

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

  private static <Gen, Sk> SlowInitialAlgebra<String, String, String, String, String, Gen, Sk, Integer> create(
      Schema<String, String, String, String, String> schema,
      Map<Gen, String> gens, Map<Sk, String> sks) {
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
}
