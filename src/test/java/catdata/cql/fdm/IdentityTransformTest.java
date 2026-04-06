package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import catdata.cql.Term;

class IdentityTransformTest {

  @Nested
  class Constructor {

    @Test
    void srcAndDstAreSameWhenOptionalEmpty() {
      var inst = FdmTestHelpers.singleElementInstance();
      var id = new IdentityTransform<>(inst, Optional.empty());
      assertSame(id.src(), id.dst());
    }

    @Test
    void srcAndDstDifferWhenOptionalPresent() {
      var inst = FdmTestHelpers.singleElementInstance();
      var inst2 = FdmTestHelpers.singleElementInstance();
      var id = new IdentityTransform<>(inst, Optional.of(inst2));
      assertSame(inst, id.src());
      assertSame(inst2, id.dst());
    }

    @Test
    void constructorRejectsNull() {
      assertThrows(RuntimeException.class,
          () -> new IdentityTransform<>(null, Optional.empty()));
    }
  }

  @Nested
  class GensAndSks {

    @Test
    void gensMapsToTermGen() {
      var inst = FdmTestHelpers.singleElementInstance();
      var id = new IdentityTransform<>(inst, Optional.empty());
      var result = id.gens().apply("e1", "E");
      assertEquals(Term.Gen("e1"), result);
    }

    @Test
    void sksMapsToTermSk() {
      var inst = FdmTestHelpers.singleElementInstance();
      var id = new IdentityTransform<>(inst, Optional.empty());
      var result = id.sks().apply("sk1", "String");
      assertEquals(Term.Sk("sk1"), result);
    }
  }
}
