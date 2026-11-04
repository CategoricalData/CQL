package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.cql.AqlOptions;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DistinctTransformTest {

  @Nested
  class Constructor {

    @Test
    void constructorCreatesSrcAndDst() {
      var inst = FdmTestHelpers.singleElementInstance();
      var idTransform = new IdentityTransform<>(inst, Optional.empty());
      var dt =
          new DistinctTransform<>(
              idTransform, AqlOptions.initialOptions, AqlOptions.initialOptions);
      assertTrue(dt.src() instanceof DistinctInstance);
      assertTrue(dt.dst() instanceof DistinctInstance);
    }
  }

  @Nested
  class TransformBehavior {

    @Test
    void gensDelegatesToWrapped() {
      var inst = FdmTestHelpers.singleElementInstance();
      var idTransform = new IdentityTransform<>(inst, Optional.empty());
      var dt =
          new DistinctTransform<>(
              idTransform, AqlOptions.initialOptions, AqlOptions.initialOptions);
      assertNotNull(dt.gens());
    }

    @Test
    void sksDelegatesToWrapped() {
      var inst = FdmTestHelpers.singleElementInstance();
      var idTransform = new IdentityTransform<>(inst, Optional.empty());
      var dt =
          new DistinctTransform<>(
              idTransform, AqlOptions.initialOptions, AqlOptions.initialOptions);
      assertNotNull(dt.sks());
    }
  }
}
