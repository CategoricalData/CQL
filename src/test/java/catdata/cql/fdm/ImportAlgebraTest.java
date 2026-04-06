package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import catdata.cql.Term;

class ImportAlgebraTest {

  private ImportAlgebra<String, String, String, String, String, String, String> emptyAlgebra() {
    var schema = FdmTestHelpers.singleEntitySchema();
    Map<String, Collection<String>> ensMap = new HashMap<>();
    ensMap.put("E", Collections.emptyList());
    Map<String, Collection<String>> tysMap = new HashMap<>();
    return new ImportAlgebra<>(schema,
        en -> ensMap.get(en), tysMap,
        (en, x) -> Collections.emptyMap(),
        (en, x) -> Collections.emptyMap(),
        (en, x) -> x, (ty, y) -> y,
        true, Collections.emptyList());
  }

  private ImportAlgebra<String, String, String, String, String, String, String> singleElementAlgebra() {
    var schema = FdmTestHelpers.singleEntitySchema();
    Map<String, Collection<String>> ensMap = new HashMap<>();
    ensMap.put("E", Collections.singletonList("e1"));
    Map<String, Collection<String>> tysMap = new HashMap<>();
    return new ImportAlgebra<>(schema,
        en -> ensMap.get(en), tysMap,
        (en, x) -> Collections.emptyMap(),
        (en, x) -> Collections.emptyMap(),
        (en, x) -> x, (ty, y) -> y,
        true, Collections.emptyList());
  }

  @Nested
  class SchemaMethod {

    @Test
    void schemaReturnsProvidedSchema() {
      var schema = FdmTestHelpers.singleEntitySchema();
      Map<String, Collection<String>> ensMap = new HashMap<>();
      ensMap.put("E", Collections.emptyList());
      var alg = new ImportAlgebra<>(schema,
          en -> ensMap.get(en), new HashMap<>(),
          (en, x) -> Collections.emptyMap(),
          (en, x) -> Collections.emptyMap(),
          (en, x) -> x, (ty, y) -> y,
          true, Collections.emptyList());
      assertSame(schema, alg.schema());
    }
  }

  @Nested
  class EnMethod {

    @Test
    void enReturnsElementsForEntity() {
      var alg = singleElementAlgebra();
      var elements = alg.en("E");
      assertEquals(1, elements.size());
      assertTrue(elements.contains("e1"));
    }

    @Test
    void enReturnsEmptyForNoElements() {
      var alg = emptyAlgebra();
      var elements = alg.en("E");
      assertTrue(elements.isEmpty());
    }
  }

  @Nested
  class GenMethod {

    @Test
    void genReturnsItself() {
      var alg = singleElementAlgebra();
      assertEquals("e1", alg.gen("e1"));
    }
  }

  @Nested
  class SkMethod {

    @Test
    void skReturnsTermSk() {
      var alg = emptyAlgebra();
      assertEquals(Term.Sk("s1"), alg.sk("s1"));
    }
  }

  @Nested
  class ReprMethod {

    @Test
    void reprReturnsTermGen() {
      var alg = singleElementAlgebra();
      assertEquals(Term.Gen("e1"), alg.repr("E", "e1"));
    }
  }

  @Nested
  class SizeMethod {

    @Test
    void sizeReturnsElementCount() {
      assertEquals(0, emptyAlgebra().size("E"));
      assertEquals(1, singleElementAlgebra().size("E"));
    }
  }

  @Nested
  class NullsAndTypeAlgebra {

    @Test
    void hasFreeTypeAlgebraWhenNoEqs() {
      assertTrue(emptyAlgebra().hasFreeTypeAlgebra());
    }

    @Test
    void hasNullsWhenSksEmpty() {
      assertTrue(emptyAlgebra().hasNulls());
    }
  }

  @Nested
  class PrintMethods {

    @Test
    void printXDelegatesToFunction() {
      var alg = singleElementAlgebra();
      assertEquals("e1", alg.printX("E", "e1"));
    }

    @Test
    void printYDelegatesToFunction() {
      var alg = emptyAlgebra();
      assertEquals("y1", alg.printY("String", "y1"));
    }
  }

  @Nested
  class ToStringProver {

    @Test
    void toStringProverReturnsExpected() {
      assertEquals("Import algebra prover", emptyAlgebra().toStringProver());
    }
  }

  @Nested
  class ConstructorValidation {

    @Test
    void constructorRejectsNullSchema() {
      assertThrows(RuntimeException.class, () ->
          new ImportAlgebra<>(null,
              en -> Collections.emptyList(), new HashMap<>(),
              (en, x) -> Collections.emptyMap(),
              (en, x) -> Collections.emptyMap(),
              (en, x) -> x, (ty, y) -> y,
              true, Collections.emptyList()));
    }
  }
}
