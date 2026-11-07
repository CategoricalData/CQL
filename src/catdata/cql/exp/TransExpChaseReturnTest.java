package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.Pair;
import catdata.Unit;
import catdata.cql.Instance;
import catdata.cql.Kind;
import catdata.cql.Transform;
import catdata.cql.exp.EdsExp.EdsExpVar;
import catdata.cql.exp.InstExp.InstExpVar;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TransExpChaseReturn}, {@code chase_return C I}, the transform from I into
 * the result of chasing I with the constraints C.
 */
class TransExpChaseReturnTest {

  /** Every row of A needs a row of B with the same value, so the chase adds two rows to B. */
  private static final String PROGRAM =
      """
      typeside Ty = literal { types String constants x y : String }
      schema S = literal : Ty {
        entities A B
        attributes n : A -> String m : B -> String
      }
      constraints C = literal : S { forall a:A -> exists b:B where b.m = a.n }
      instance I = literal : S { generators a1 a2 : A equations a1.n = x a2.n = y }
      transform t = chase_return C I
      instance J = chase C I
      """;

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static TransExpChaseReturn<?, ?, ?, ?, ?> exp(String c, String i) {
    return new TransExpChaseReturn(new EdsExpVar(c), new InstExpVar(i));
  }

  private final TransExpChaseReturn<?, ?, ?, ?, ?> exp = exp("C", "I");

  @Nested
  class Evaluation {

    @Test
    void mapsTheInstanceIntoItsChase() throws Exception {
      Transform<String, String, Sym, Fk, Att, ?, ?, ?, ?, ?, ?, ?, ?> t =
          ExpTestHelpers.eval(PROGRAM, "t", Kind.TRANSFORM);
      Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?> j =
          ExpTestHelpers.eval(PROGRAM, "J", Kind.INSTANCE);
      assertEquals(2, t.src().algebra().size("A"));
      assertEquals(0, t.src().algebra().size("B"));
      assertEquals(2, t.dst().algebra().size("A"));
      assertEquals(2, t.dst().algebra().size("B"));
      assertEquals(j.algebra().size("B"), t.dst().algebra().size("B"));
    }

    @Test
    void isTypedFromTheInstanceToItsChase() throws Exception {
      AqlTyping g = ExpTestHelpers.typing(PROGRAM);
      assertEquals(
          new Pair<>(
              new InstExpVar("I"),
              new InstExpChase<>(new EdsExpVar("C"), new InstExpVar("I"), List.of())),
          exp.type(g));
    }
  }

  @Nested
  class Expression {

    @Test
    void printsTheConstraintsAndTheInstance() {
      assertEquals("chase_return C I", exp.toString());
    }

    @Test
    void dependsOnTheConstraintsAndTheInstance() {
      assertEquals(
          Set.of(new Pair<>("C", Kind.CONSTRAINTS), new Pair<>("I", Kind.INSTANCE)),
          Set.copyOf(exp.deps()));
      assertTrue(exp.options().isEmpty());
      assertEquals(ExpTestHelpers.BASE_OPTIONS, exp.allowedOptions());
      List<Exp<?>> seen = new ArrayList<>();
      exp.map(seen::add);
      assertEquals(List.of(exp), seen);
    }

    @Test
    void equalityComparesTheConstraintsAndTheInstance() {
      assertEquals(exp, exp);
      assertEquals(exp, exp("C", "I"));
      assertEquals(exp.hashCode(), exp("C", "I").hashCode());
      assertNotEquals(exp, exp("D", "I"));
      assertNotEquals(exp, exp("C", "J"));
      TransExpChaseReturn<?, ?, ?, ?, ?> none = new TransExpChaseReturn<>(null, null);
      assertEquals(none, new TransExpChaseReturn<>(null, null));
      assertEquals(none.hashCode(), new TransExpChaseReturn<>(null, null).hashCode());
      assertNotEquals(none, exp);
      assertNotEquals(exp, none);
      assertNotEquals(new TransExpChaseReturn<>(new EdsExpVar("C"), null), exp);
      assertNotEquals(exp, null);
      assertNotEquals(exp, "chase_return");
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsAVisitor() {
      TransExpVisitor<String, Unit, RuntimeException> v = mock(TransExpVisitor.class);
      when(v.visit(Unit.unit, exp)).thenReturn("visited");
      assertEquals("visited", exp.accept(Unit.unit, v));
    }
  }
}
