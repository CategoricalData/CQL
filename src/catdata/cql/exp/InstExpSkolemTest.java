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
import catdata.cql.exp.InstExp.InstExpVar;
import catdata.cql.exp.InstExp.InstExpVisitor;
import catdata.cql.exp.SchExp.SchExpVar;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link InstExpSkolem} and {@link TransExpSkolem}, {@code instance K = skolem I}
 * and {@code transform t = skolem I}: the instance whose SQL nulls are labelled nulls, constrained
 * by {@code isNull}, and the transform from it back to I.
 */
class InstExpSkolemTest {

  /** Every value of n is given, the one of b as a SQL null. */
  private static final String SQL =
      """
      typeside Ty = sql
      schema S = literal : Ty { entities A attributes n : A -> Varchar }
      instance I = literal : S { generators a b : A equations a.n = hello b.n = null }
      instance K = skolem I
      transform t = skolem I
      """;

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static InstExpSkolem<?, ?, ?, ?> inst(String i) {
    return new InstExpSkolem(new InstExpVar(i));
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static TransExpSkolem<?, ?, ?, ?> trans(String i) {
    return new TransExpSkolem(new InstExpVar(i));
  }

  @Nested
  class Evaluation {

    @Test
    void namesEveryValueByALabelledNull() throws Exception {
      Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?> k =
          ExpTestHelpers.eval(SQL, "K", Kind.INSTANCE);
      assertEquals(2, k.algebra().size("A"));
      assertEquals(2, k.sks().size());
      String text = k.toString();
      assertTrue(text.contains("isNull(b.n) = true"), text);
      assertTrue(text.contains("isNull(a.n) = false"), text);
    }

    @Test
    void mapsBackToTheInstance() throws Exception {
      Transform<String, String, Sym, Fk, Att, ?, ?, ?, ?, ?, ?, ?, ?> t =
          ExpTestHelpers.eval(SQL, "t", Kind.TRANSFORM);
      assertEquals(2, t.src().sks().size());
      assertEquals(0, t.dst().sks().size());
    }

    /** A labelled null of I, or a value that is not a SQL value, reaches an anomaly. */
    @Test
    void failsOnAMissingValue() throws Exception {
      String msg = ExpTestHelpers.error(SQL.replace(" b.n = null", ""));
      assertTrue(msg.contains("Anomaly: please report"), msg);
    }

    @Test
    void failsOffTheSqlTypeside() throws Exception {
      String program =
          """
          typeside Ty = literal { types String constants x : String }
          schema S = literal : Ty { entities A attributes n : A -> String }
          instance I = literal : S { generators a : A equations a.n = x }
          instance K = skolem I
          """;
      assertTrue(ExpTestHelpers.error(program).contains("Anomaly: please report"));
    }
  }

  @Nested
  class Expression {

    @Test
    void printsTheInstance() {
      assertEquals("skolem I", inst("I").toString());
      assertEquals("skolem I", trans("I").toString());
    }

    @Test
    void isTypedAsItsInstance() throws Exception {
      AqlTyping g = ExpTestHelpers.typing(SQL);
      assertEquals(new SchExpVar("S"), inst("I").type(g));
      assertEquals(new Pair<>(inst("I"), new InstExpVar("I")), trans("I").type(g));
      assertEquals(Set.of(new InstExpVar("I")), Set.copyOf(inst("I").direct(g)));
    }

    @Test
    void dependsOnTheInstance() {
      for (Exp<?> e : List.<Exp<?>>of(inst("I"), trans("I"))) {
        assertEquals(List.of(new Pair<>("I", Kind.INSTANCE)), new ArrayList<>(e.deps()));
        assertTrue(e.options().isEmpty());
        assertEquals(ExpTestHelpers.BASE_OPTIONS, e.allowedOptions());
        List<Exp<?>> seen = new ArrayList<>();
        e.map(seen::add);
        assertEquals(List.of(e, new InstExpVar("I")), seen);
      }
    }

    @Test
    void equalityComparesTheInstance() {
      assertEquals(inst("I"), inst("I"));
      assertEquals(inst("I").hashCode(), inst("I").hashCode());
      assertNotEquals(inst("I"), inst("J"));
      assertNotEquals(inst("I"), null);
      assertNotEquals(inst("I"), "skolem");
      InstExpSkolem<?, ?, ?, ?> i = inst("I");
      assertEquals(i, i);
      TransExpSkolem<?, ?, ?, ?> t = trans("I");
      assertEquals(t, t);
      assertEquals(t, trans("I"));
      assertEquals(t.hashCode(), trans("I").hashCode());
      assertNotEquals(t, trans("J"));
      assertNotEquals(t, null);
      assertNotEquals(t, "skolem");
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsAVisitor() {
      InstExpSkolem<?, ?, ?, ?> i = inst("I");
      InstExpVisitor<String, Unit, RuntimeException> v = mock(InstExpVisitor.class);
      when(v.visit(Unit.unit, i)).thenReturn("instance");
      assertEquals("instance", i.accept(Unit.unit, v));
      TransExpSkolem<?, ?, ?, ?> t = trans("I");
      TransExpVisitor<String, Unit, RuntimeException> w = mock(TransExpVisitor.class);
      when(w.visit(Unit.unit, t)).thenReturn("transform");
      assertEquals("transform", t.accept(Unit.unit, w));
    }
  }
}
