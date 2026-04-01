package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import catdata.Pair;
import catdata.cql.Algebra;
import catdata.cql.DP;
import catdata.cql.Schema;
import catdata.cql.Term;

class LiteralInstanceTest {

  @Nested
  class Constructor {

    @Test
    void constructorSetsSchema() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var alg = createEmptyAlgebra(schema);
      var dp = createTrueDP();
      var inst = new LiteralInstance<>(schema,
          Collections.emptyMap(), Collections.emptyMap(),
          Collections.emptyList(), dp, alg, false, false);
      assertSame(schema, inst.schema());
    }

    @Test
    void constructorSetsFlags() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var alg = createEmptyAlgebra(schema);
      var dp = createTrueDP();
      var inst = new LiteralInstance<>(schema,
          Collections.emptyMap(), Collections.emptyMap(),
          Collections.emptyList(), dp, alg, true, true);
      assertTrue(inst.requireConsistency());
      assertTrue(inst.allowUnsafeJava());
    }
  }

  @Nested
  class Delegation {

    @Test
    void gensReturnsProvidedMap() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var alg = createEmptyAlgebra(schema);
      var dp = createTrueDP();
      Map<String, String> gens = new HashMap<>();
      gens.put("g1", "E");
      var inst = new LiteralInstance<>(schema, gens, Collections.emptyMap(),
          Collections.emptyList(), dp, alg, false, false);
      assertEquals("E", inst.gens().get("g1"));
    }

    @Test
    void dpReturnsProvided() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var alg = createEmptyAlgebra(schema);
      var dp = createTrueDP();
      var inst = new LiteralInstance<>(schema,
          Collections.emptyMap(), Collections.emptyMap(),
          Collections.emptyList(), dp, alg, false, false);
      assertSame(dp, inst.dp());
    }

    @Test
    void algebraReturnsProvided() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var alg = createEmptyAlgebra(schema);
      var dp = createTrueDP();
      var inst = new LiteralInstance<>(schema,
          Collections.emptyMap(), Collections.emptyMap(),
          Collections.emptyList(), dp, alg, false, false);
      assertSame(alg, inst.algebra());
    }
  }

  @Nested
  class EqsMethod {

    @Test
    void eqsIteratesOverEquations() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var alg = createEmptyAlgebra(schema);
      var dp = createTrueDP();
      Map<String, String> gens = new HashMap<>();
      gens.put("g1", "E");
      var eq = new Pair<>(Term.<String, String, String, String, String, String, String>Gen("g1"),
          Term.<String, String, String, String, String, String, String>Gen("g1"));
      var inst = new LiteralInstance<>(schema,
          gens, Collections.emptyMap(),
          Collections.singletonList(eq), dp, alg, false, false);
      List<Pair<?, ?>> collected = new ArrayList<>();
      inst.eqs((l, r) -> collected.add(new Pair<>(l, r)));
      assertEquals(1, collected.size());
    }
  }

  // Helper methods

  private static DP<String, String, String, String, String, String, String> createTrueDP() {
    return new DP<>() {
      @Override
      public String toStringProver() { return "test-dp"; }
      @Override
      public boolean eq(Map<String, catdata.Chc<String, String>> ctx,
          Term<String, String, String, String, String, String, String> lhs,
          Term<String, String, String, String, String, String, String> rhs) {
        return lhs.equals(rhs);
      }
    };
  }

  private static Algebra<String, String, String, String, String, String, String, String, String> createEmptyAlgebra(
      Schema<String, String, String, String, String> schema) {
    Map<String, Collection<String>> ensMap = new HashMap<>();
    for (String en : schema.ens) {
      ensMap.put(en, Collections.emptyList());
    }
    Map<String, Collection<String>> tysMap = new HashMap<>();
    for (String ty : schema.typeSide.tys) {
      tysMap.put(ty, Collections.emptyList());
    }
    return new ImportAlgebra<>(schema,
        en -> ensMap.get(en), tysMap,
        (en, x) -> Collections.emptyMap(),
        (en, x) -> Collections.emptyMap(),
        (en, x) -> x, (ty, y) -> y,
        true, Collections.emptyList());
  }
}
