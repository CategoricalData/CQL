package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ComposeTransformTest {

  @Nested
  class Constructor {

    @Test
    void compositionSetsSrcAndDst() {
      var inst1 = FdmTestHelpers.singleElementInstance();
      var inst2 = FdmTestHelpers.singleElementInstance();
      var inst3 = FdmTestHelpers.singleElementInstance();
      var t1 = new IdentityTransform<>(inst1, Optional.of(inst2));
      var t2 = new IdentityTransform<>(inst2, Optional.of(inst3));
      var composed = new ComposeTransform<>(t1, t2);
      assertSame(inst1, composed.src());
      assertSame(inst3, composed.dst());
    }
  }

  @Nested
  class GensAndSks {

    @Test
    void gensComposesTransforms() {
      var inst = FdmTestHelpers.singleElementInstance();
      var t1 = new IdentityTransform<>(inst, Optional.empty());
      var t2 = new IdentityTransform<>(inst, Optional.empty());
      var composed = new ComposeTransform<>(t1, t2);
      var result = composed.gens().apply("e1", "E");
      assertNotNull(result);
    }

    @Test
    void sksComposesTransforms() {
      var inst = FdmTestHelpers.singleElementInstance();
      var t1 = new IdentityTransform<>(inst, Optional.empty());
      var t2 = new IdentityTransform<>(inst, Optional.empty());
      var composed = new ComposeTransform<>(t1, t2);
      assertNotNull(composed.sks());
    }
  }
}
