package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import catdata.cql.Term;

class SaturatedInstanceTest {

  @Nested
  class Constructor {

    @Test
    void constructorSetsSchema() {
      var inst = FdmTestHelpers.singleElementInstance();
      assertEquals(FdmTestHelpers.singleEntitySchema().ens, inst.schema().ens);
    }

    @Test
    void constructorSetsConsistencyFlags() {
      var schema = FdmTestHelpers.singleEntitySchema();
      Map<String, Collection<String>> ensMap = new HashMap<>();
      ensMap.put("E", Collections.singletonList("e1"));
      var alg = new ImportAlgebra<>(schema,
          en -> ensMap.get(en), new HashMap<>(),
          (en, x) -> Collections.emptyMap(),
          (en, x) -> Collections.emptyMap(),
          (en, x) -> x, (ty, y) -> y,
          true, Collections.emptyList());

      var sat = new SaturatedInstance<>(alg, alg, true, true, false, null);
      assertTrue(sat.requireConsistency());
      assertTrue(sat.allowUnsafeJava());
    }
  }

  @Nested
  class GensMethod {

    @Test
    @SuppressWarnings("unchecked")
    void gensContainsAlgebraElements() {
      var inst = (SaturatedInstance<String, String, String, String, String, String, String, String, String>)
          FdmTestHelpers.singleElementInstance();
      assertEquals("E", inst.gens().get("e1"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void gensEntrySetIteratesOverElements() {
      var inst = (SaturatedInstance<String, String, String, String, String, String, String, String, String>)
          FdmTestHelpers.singleElementInstance();
      List<String> elements = new ArrayList<>();
      inst.gens().entrySet((x, en) -> elements.add(x));
      assertTrue(elements.contains("e1"));
    }
  }

  @Nested
  class AlgebraMethod {

    @Test
    void algebraGenReturnsItself() {
      var inst = FdmTestHelpers.singleElementInstance();
      assertEquals("e1", inst.algebra().gen("e1"));
    }

    @Test
    void algebraReprReturnsTermGen() {
      var inst = FdmTestHelpers.singleElementInstance();
      assertEquals(Term.Gen("e1"), inst.algebra().repr("E", "e1"));
    }

    @Test
    void algebraEnReturnsElements() {
      var inst = FdmTestHelpers.singleElementInstance();
      List<String> elements = new ArrayList<>();
      inst.algebra().en("E").forEach(elements::add);
      assertEquals(List.of("e1"), elements);
    }

    @Test
    void algebraSizeReturnsCount() {
      var inst = FdmTestHelpers.singleElementInstance();
      assertEquals(1, inst.algebra().size("E"));
    }

    @Test
    void algebraSkReturnsTermSk() {
      var inst = FdmTestHelpers.singleElementInstance();
      assertEquals(Term.Sk("s1"), inst.algebra().sk("s1"));
    }
  }

  @Nested
  class DPMethod {

    @Test
    void dpToStringProver() {
      var inst = FdmTestHelpers.singleElementInstance();
      assertTrue(inst.dp().toStringProver().contains("Saturated"));
    }
  }
}
