package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import catdata.cql.AqlOptions;

class SigmaDeltaCounitTransformTest {

  @Nested
  class Constructor {

    @Test
    void constructorCreatesTransform() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var mapping = FdmTestHelpers.identityMapping(schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var t = new SigmaDeltaCounitTransform<>(mapping, inst, AqlOptions.initialOptions);
      assertNotNull(t.src());
      assertNotNull(t.dst());
    }
  }

  @Nested
  class TransformBehavior {

    @Test
    void dstIsOriginalInstance() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var mapping = FdmTestHelpers.identityMapping(schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var t = new SigmaDeltaCounitTransform<>(mapping, inst, AqlOptions.initialOptions);
      assertSame(inst, t.dst());
    }

    @Test
    void gensIsNotNull() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var mapping = FdmTestHelpers.identityMapping(schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var t = new SigmaDeltaCounitTransform<>(mapping, inst, AqlOptions.initialOptions);
      assertNotNull(t.gens());
    }

    @Test
    void sksIsNotNull() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var mapping = FdmTestHelpers.identityMapping(schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var t = new SigmaDeltaCounitTransform<>(mapping, inst, AqlOptions.initialOptions);
      assertNotNull(t.sks());
    }
  }
}
