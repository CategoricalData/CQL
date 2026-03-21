package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import catdata.cql.Instance;

class CoprodInstanceTest {

  private CoprodInstance<String, String, String, String, String, String, String, String, String> createCoprod(
      Map<String, Instance<String, String, String, String, String, String, String, String, String>> insts) {
    var schema = FdmTestHelpers.singleEntitySchema();
    return new CoprodInstance<>(insts, schema, false, false);
  }

  @Nested
  class Constructor {

    @Test
    void emptyMapProducesEmptyInstance() {
      var coprod = createCoprod(Collections.emptyMap());
      assertEquals(0, coprod.algebra().size("E"));
    }

    @Test
    void singleInstanceCoprod() {
      Map<String, Instance<String, String, String, String, String, String, String, String, String>> insts = new HashMap<>();
      insts.put("i1", FdmTestHelpers.singleElementInstance());
      var coprod = createCoprod(insts);
      assertEquals(1, coprod.algebra().size("E"));
    }
  }

  @Nested
  class InstanceBehavior {

    @Test
    void schemaIsProvided() {
      var coprod = createCoprod(Collections.emptyMap());
      assertEquals(FdmTestHelpers.singleEntitySchema().ens, coprod.schema().ens);
    }

    @Test
    void dpIsNotNull() {
      var coprod = createCoprod(Collections.emptyMap());
      assertNotNull(coprod.dp());
    }

    @Test
    void algebraIsNotNull() {
      var coprod = createCoprod(Collections.emptyMap());
      assertNotNull(coprod.algebra());
    }

    @Test
    void requireConsistencyMatchesInput() {
      var coprod = createCoprod(Collections.emptyMap());
      assertFalse(coprod.requireConsistency());
    }

    @Test
    void allowUnsafeJavaMatchesInput() {
      var coprod = createCoprod(Collections.emptyMap());
      assertFalse(coprod.allowUnsafeJava());
    }
  }
}
