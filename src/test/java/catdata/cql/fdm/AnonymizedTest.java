package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AnonymizedTest {

  @Nested
  class Constructor {

    @Test
    void constructorSetsSchema() {
      var inst = FdmTestHelpers.instanceWithAtt();
      var anon = new Anonymized<>(inst);
      assertSame(inst.schema(), anon.schema());
    }
  }

  @Nested
  class InstanceBehavior {

    @Test
    void gensDelegates() {
      var inst = FdmTestHelpers.instanceWithAtt();
      var anon = new Anonymized<>(inst);
      assertNotNull(anon.gens());
    }

    @Test
    void sksDelegates() {
      var inst = FdmTestHelpers.instanceWithAtt();
      var anon = new Anonymized<>(inst);
      assertNotNull(anon.sks());
    }

    @Test
    void algebraIsNotNull() {
      var inst = FdmTestHelpers.instanceWithAtt();
      var anon = new Anonymized<>(inst);
      assertNotNull(anon.algebra());
    }

    @Test
    void dpIsNotNull() {
      var inst = FdmTestHelpers.instanceWithAtt();
      var anon = new Anonymized<>(inst);
      assertNotNull(anon.dp());
    }

    @Test
    void requireConsistencyDelegates() {
      var inst = FdmTestHelpers.instanceWithAtt();
      var anon = new Anonymized<>(inst);
      assertEquals(inst.requireConsistency(), anon.requireConsistency());
    }

    @Test
    void allowUnsafeJavaDelegates() {
      var inst = FdmTestHelpers.instanceWithAtt();
      var anon = new Anonymized<>(inst);
      assertEquals(inst.allowUnsafeJava(), anon.allowUnsafeJava());
    }

    @Test
    void algebraPreservesEntitySize() {
      var inst = FdmTestHelpers.instanceWithAtt();
      var anon = new Anonymized<>(inst);
      assertEquals(inst.algebra().size("E"), anon.algebra().size("E"));
    }
  }
}
