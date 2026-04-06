package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class InitialInstanceTest {

  @Nested
  class Constructor {

    @Test
    void constructorSetsSchema() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var init = new InitialInstance<>(schema);
      assertSame(schema, init.schema());
    }
  }

  @Nested
  class EmptyBehavior {

    @Test
    void gensIsEmpty() {
      var init = new InitialInstance<>(FdmTestHelpers.singleEntitySchema());
      assertEquals(0, init.gens().size());
    }

    @Test
    void sksIsEmpty() {
      var init = new InitialInstance<>(FdmTestHelpers.singleEntitySchema());
      assertEquals(0, init.sks().size());
    }

    @Test
    void algebraSizeIsZero() {
      var init = new InitialInstance<>(FdmTestHelpers.singleEntitySchema());
      assertEquals(0, init.algebra().size("E"));
    }

    @Test
    void algebraEnIsEmpty() {
      var init = new InitialInstance<>(FdmTestHelpers.singleEntitySchema());
      assertFalse(init.algebra().en("E").iterator().hasNext());
    }

    @Test
    void algebraHasFreeTypeAlgebra() {
      var init = new InitialInstance<>(FdmTestHelpers.singleEntitySchema());
      assertTrue(init.algebra().hasFreeTypeAlgebra());
    }

    @Test
    void algebraHasNoNulls() {
      var init = new InitialInstance<>(FdmTestHelpers.singleEntitySchema());
      assertFalse(init.algebra().hasNulls());
    }

    @Test
    void requireConsistencyIsTrue() {
      var init = new InitialInstance<>(FdmTestHelpers.singleEntitySchema());
      assertTrue(init.requireConsistency());
    }

    @Test
    void allowUnsafeJavaIsFalse() {
      var init = new InitialInstance<>(FdmTestHelpers.singleEntitySchema());
      assertFalse(init.allowUnsafeJava());
    }

    @Test
    void dpDelegatesFromSchema() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var init = new InitialInstance<>(schema);
      assertNotNull(init.dp());
    }
  }

  @Nested
  class OnEmptySchema {

    @Test
    void initialInstanceOnEmptySchema() {
      var init = new InitialInstance<>(FdmTestHelpers.emptySchema());
      assertEquals(0, init.gens().size());
      assertEquals(0, init.sks().size());
    }
  }
}
