package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SkolemInstanceTest {

  @Nested
  class Constructor {

    @Test
    void constructorSetsSourceInstance() {
      var inst = FdmTestHelpers.singleElementInstance();
      var skolem = new SkolemInstance<>(inst);
      assertSame(inst, skolem.I);
    }
  }

  @Nested
  class InstanceBehavior {

    @Test
    void schemaDelegates() {
      var inst = FdmTestHelpers.singleElementInstance();
      var skolem = new SkolemInstance<>(inst);
      assertSame(inst.schema(), skolem.schema());
    }

    @Test
    void gensDelegates() {
      var inst = FdmTestHelpers.singleElementInstance();
      var skolem = new SkolemInstance<>(inst);
      assertNotNull(skolem.gens());
    }

    @Test
    void requireConsistencyIsFalse() {
      var inst = FdmTestHelpers.singleElementInstance();
      var skolem = new SkolemInstance<>(inst);
      assertFalse(skolem.requireConsistency());
    }

    @Test
    void allowUnsafeJavaIsTrue() {
      var inst = FdmTestHelpers.singleElementInstance();
      var skolem = new SkolemInstance<>(inst);
      assertTrue(skolem.allowUnsafeJava());
    }

    @Test
    void dpToStringProver() {
      var inst = FdmTestHelpers.singleElementInstance();
      var skolem = new SkolemInstance<>(inst);
      assertEquals("skolem", skolem.dp().toStringProver());
    }

    @Test
    void transIsNotNull() {
      var inst = FdmTestHelpers.singleElementInstance();
      var skolem = new SkolemInstance<>(inst);
      assertNotNull(skolem.trans);
    }
  }

  @Nested
  class SksMethod {

    @Test
    void sksContainsKeyReturnsTrue() {
      var inst = FdmTestHelpers.singleElementInstance();
      var skolem = new SkolemInstance<>(inst);
      // sks.containsKey always returns true
      assertTrue(skolem.sks().containsKey(new catdata.Pair<>("anything", "any")));
    }

    @Test
    void sksSizeForNoAtts() {
      var inst = FdmTestHelpers.singleElementInstance();
      var skolem = new SkolemInstance<>(inst);
      // schema has no atts, so sks size is 0
      assertEquals(0, skolem.sks().size());
    }
  }
}
