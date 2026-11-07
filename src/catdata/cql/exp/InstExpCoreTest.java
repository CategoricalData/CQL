package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.Pair;
import catdata.Unit;
import catdata.cql.Instance;
import catdata.cql.Kind;
import catdata.cql.exp.InstExp.InstExpVar;
import catdata.cql.exp.InstExp.InstExpVisitor;
import catdata.cql.exp.SchExp.SchExpVar;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link InstExpCore}, {@code instance K = core I}, the smallest sub-instance of I
 * that I maps into. The search for that map sends each labelled null of I to a labelled null, never
 * to a value.
 */
class InstExpCoreTest {

  private static final String SCHEMA =
      """
      typeside Ty = literal { types String constants x y : String }
      schema S = literal : Ty { entities A attributes n : A -> String }
      """;

  private static Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?> core(String equations)
      throws Exception {
    String program =
        SCHEMA
            + "instance I = literal : S { generators a b : A s t : String equations "
            + equations
            + " }\ninstance K = core I\n";
    return ExpTestHelpers.eval(program, "K", Kind.INSTANCE);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static InstExpCore<?, ?, ?, ?> exp(String i) {
    return new InstExpCore(new InstExpVar(i));
  }

  @Nested
  class Evaluation {

    @Test
    void foldsARowWhoseValueIsAnotherLabelledNull() throws Exception {
      Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?> k = core("a.n = s b.n = t");
      assertEquals(1, k.algebra().size("A"));
      assertTrue(k.dp().toStringProver().startsWith("Sub-instance wrapper of "));
    }

    /** b could map onto a by sending s to x, but the search never sends a null to a value. */
    @Test
    void keepsARowWhoseNullCouldBeAValue() throws Exception {
      assertEquals(2, core("a.n = x b.n = s").algebra().size("A"));
    }

    @Test
    void keepsRowsWithDifferentValues() throws Exception {
      assertEquals(2, core("a.n = x b.n = y").algebra().size("A"));
    }

    @Test
    void isNotEvaluatedAtCompileTime() throws Exception {
      String program = SCHEMA + "instance I = literal : S { generators a : A }\n";
      AqlEnv env = ExpTestHelpers.run(program).env;
      assertThrows(IgnoreException.class, () -> exp("I").eval0(env, true));
    }
  }

  @Nested
  class Expression {

    @Test
    void printsItsInstance() {
      assertEquals("core I", exp("I").toString());
    }

    @Test
    void isTypedAndDependsAsItsInstance() throws Exception {
      String program = SCHEMA + "instance I = literal : S { generators a : A }\n";
      InstExpCore<?, ?, ?, ?> e = exp("I");
      AqlTyping g = ExpTestHelpers.typing(program);
      assertEquals(new SchExpVar("S"), e.type(g));
      assertEquals(Set.of(new InstExpVar("I")), Set.copyOf(e.direct(g)));
      assertEquals(List.of(new Pair<>("I", Kind.INSTANCE)), new ArrayList<>(e.deps()));
      assertTrue(e.options().isEmpty());
      assertEquals(ExpTestHelpers.BASE_OPTIONS, e.allowedOptions());
      List<Exp<?>> seen = new ArrayList<>();
      e.map(seen::add);
      assertEquals(List.of(e, new InstExpVar("I")), seen);
    }

    @Test
    void equalityComparesTheInstance() {
      InstExpCore<?, ?, ?, ?> e = exp("I");
      assertEquals(e, e);
      assertEquals(e, exp("I"));
      assertEquals(e.hashCode(), exp("I").hashCode());
      assertNotEquals(e, exp("J"));
      assertNotEquals(e, null);
      assertNotEquals(e, "core");
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsAVisitor() {
      InstExpCore<?, ?, ?, ?> e = exp("I");
      InstExpVisitor<String, Unit, RuntimeException> v = mock(InstExpVisitor.class);
      when(v.visit(Unit.unit, e)).thenReturn("visited");
      assertEquals("visited", e.accept(Unit.unit, v));
    }
  }
}
