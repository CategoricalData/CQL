package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import catdata.cql.Term;

class LiteralTransformTest {

  @Nested
  class Constructor {

    @Test
    void constructorSetsFields() {
      var inst = FdmTestHelpers.singleElementInstance();
      var lt = new LiteralTransform<>(
          (gen, en) -> Term.Gen(gen),
          (sk, ty) -> Term.Sk(sk),
          inst, inst, true);
      assertSame(inst, lt.src());
      assertSame(inst, lt.dst());
    }

    @Test
    void gensReturnsMappingFunction() {
      var inst = FdmTestHelpers.singleElementInstance();
      var lt = new LiteralTransform<>(
          (gen, en) -> Term.Gen(gen),
          (sk, ty) -> Term.Sk(sk),
          inst, inst, true);
      assertEquals(Term.Gen("e1"), lt.gens().apply("e1", "E"));
    }

    @Test
    void sksReturnsMappingFunction() {
      var inst = FdmTestHelpers.singleElementInstance();
      var lt = new LiteralTransform<>(
          (gen, en) -> Term.Gen(gen),
          (sk, ty) -> Term.Sk(sk),
          inst, inst, true);
      assertEquals(Term.Sk("s1"), lt.sks().apply("s1", "String"));
    }
  }
}
