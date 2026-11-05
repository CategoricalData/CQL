package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.LineException;
import catdata.Pair;
import catdata.Unit;
import catdata.cql.Constraints;
import catdata.cql.ED;
import catdata.cql.Kind;
import catdata.cql.exp.EdsExp.EdsExpVisitor;
import catdata.cql.exp.InstExp.InstExpVar;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link EdsExpLearn}, {@code constraints C = learn I J}, which writes one
 * constraint from two instances: a universal variable per row of I, an existential one per row of
 * J, and an equation for each attribute value seen before.
 */
class EdsExpLearnTest {

  private static String program(String i, String j) {
    return """
        typeside Ty = sql
        schema S = literal : Ty { entities A attributes n m : A -> Varchar }
        instance I = literal : S { generators %s }
        instance J = literal : S { generators %s }
        constraints C = learn I J
        """
        .formatted(i, j);
  }

  private static ED learned(String i, String j) throws Exception {
    Constraints c = ExpTestHelpers.eval(program(i, j), "C", Kind.CONSTRAINTS);
    assertEquals(1, c.eds.size());
    return c.eds.get(0);
  }

  @Nested
  class Evaluation {

    @Test
    void makesAVariablePerRow() throws Exception {
      ED ed =
          learned(
              "a b : A equations a.n = x b.n = y a.m = p b.m = q",
              "c : A equations c.n = z c.m = r");
      assertEquals(Set.of("v0", "v1"), ed.As.keySet());
      assertEquals(Set.of("v2"), ed.Es.keySet());
      assertTrue(ed.Awh.isEmpty());
      assertTrue(ed.Ewh.isEmpty());
    }

    /**
     * An unset attribute is a labelled null named by its row and attribute, so the nulls of the
     * first rows of I and J are the same term, and the constraint equates them although the data
     * says nothing about either.
     */
    @Test
    void equatesTheLabelledNullsOfRowsWithTheSameNumber() throws Exception {
      ED ed = learned("a b : A equations a.n = x b.n = y", "c : A equations c.n = z");
      assertTrue(ed.Awh.isEmpty(), ed.Awh.toString());
      assertEquals(1, ed.Ewh.size(), ed.Ewh.toString());
      var eq = ed.Ewh.iterator().next();
      assertTrue(eq.first.toString().endsWith(".m"), eq.toString());
      assertTrue(eq.second.toString().endsWith(".m"), eq.toString());
    }

    @Test
    void equatesRepeatedValues() throws Exception {
      ED ed =
          learned(
              "a b : A equations a.n = x b.n = x a.m = p b.m = q",
              "c : A equations c.n = r c.m = x");
      assertEquals(1, ed.Awh.size(), ed.Awh.toString());
      assertEquals(1, ed.Ewh.size(), ed.Ewh.toString());
    }

    @Test
    void skipsNullsAndPlaceholderValues() throws Exception {
      ED ed =
          learned(
              "a b : A equations a.n = null b.n = null a.m = Y b.m = Y",
              "c : A equations c.n = null c.m = \"00:00.0\"");
      assertTrue(ed.Awh.isEmpty(), ed.Awh.toString());
      assertTrue(ed.Ewh.isEmpty(), ed.Ewh.toString());
    }

    @Test
    void rejectsInstancesOnDifferentSchemas() {
      String program =
          program("a : A", "c : A")
              .replace(
                  "instance J = literal : S",
                  "schema S2 = literal : Ty { entities A }\ninstance J = literal : S2");
      LineException e = assertThrows(LineException.class, () -> ExpTestHelpers.run(program));
      assertTrue(
          e.getMessage().startsWith("In learn I J, mapping source is S but target schema is S2"),
          e.getMessage());
    }
  }

  @Nested
  class Expression {

    private final EdsExpLearn<?, ?, ?, ?> exp = learn("I", "J");

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static EdsExpLearn<?, ?, ?, ?> learn(String i, String j) {
      return new EdsExpLearn(new InstExpVar(i), new InstExpVar(j));
    }

    @Test
    void printsAsItsKeyword() {
      assertEquals("learn I J", exp.toString());
    }

    @Test
    void dependsOnBothInstances() {
      assertEquals(
          Set.of(new Pair<>("I", Kind.INSTANCE), new Pair<>("J", Kind.INSTANCE)),
          Set.copyOf(exp.deps()));
      assertTrue(exp.options().isEmpty());
      assertFalse(exp.isVar());
      assertEquals(ExpTestHelpers.BASE_OPTIONS, exp.allowedOptions());
      List<Exp<?>> seen = new ArrayList<>();
      exp.map(seen::add);
      assertEquals(List.of(exp), seen);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void equalityComparesBothInstances() {
      assertEquals(exp, exp);
      assertEquals(exp, learn("I", "J"));
      assertEquals(exp.hashCode(), learn("I", "J").hashCode());
      assertNotEquals(exp, learn("K", "J"));
      assertNotEquals(exp, learn("I", "K"));
      EdsExpLearn none = new EdsExpLearn(null, null);
      assertEquals(none, new EdsExpLearn(null, null));
      assertEquals(none.hashCode(), new EdsExpLearn(null, null).hashCode());
      assertNotEquals(none, exp);
      assertNotEquals(exp, none);
      assertNotEquals(new EdsExpLearn(new InstExpVar("I"), null), exp);
      assertNotEquals(exp, null);
      assertNotEquals(exp, "learn I J");
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsAVisitor() {
      EdsExpVisitor<String, Unit, RuntimeException> v = mock(EdsExpVisitor.class);
      when(v.visit(Unit.unit, exp)).thenReturn("visited");
      assertEquals("visited", exp.accept(Unit.unit, v));
    }
  }
}
