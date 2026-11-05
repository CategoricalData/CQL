package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.Chc;
import catdata.Pair;
import catdata.Quad;
import catdata.Unit;
import catdata.cql.Constraints;
import catdata.cql.ED;
import catdata.cql.Kind;
import catdata.cql.exp.EdsExp.EdsExpVar;
import catdata.cql.exp.EdsExp.EdsExpVisitor;
import catdata.cql.exp.SchExp.SchExpVar;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link EdsExpInfer}, {@code infer S -> T : ST { A.a -> B.b }}, which builds, for
 * each source entity A and target entity B of the mapping, the constraint that every A, chased with
 * S, has a B, chased with T, whose mapped attributes agree.
 */
class EdsExpInferTest {

  private static final String SCHEMA =
      """
      typeside Ty = literal { types String constants x y : String }
      schema ST = literal : Ty { entities A B attributes a1 : A -> String b1 : B -> String }
      constraints S = empty : ST
      constraints T = empty : ST
      """;

  private static String infer(String quads) {
    return SCHEMA + "constraints C = infer S -> T : ST { " + quads + " }\n";
  }

  @Nested
  class Evaluation {

    @Test
    void relatesEachSourceEntityToEachTargetEntity() throws Exception {
      Constraints c = ExpTestHelpers.eval(infer("A.a1 -> B.b1"), "C", Kind.CONSTRAINTS);
      assertEquals(1, c.eds.size());
      ED ed = c.eds.get(0);
      assertEquals(1, ed.As.size());
      assertEquals(1, ed.Es.size());
      assertTrue(ed.Awh.isEmpty());
      assertEquals(1, ed.Ewh.size());
      String s = c.toString();
      // the variables are fresh names from a global counter
      assertTrue(
          Pattern.compile(
                  "forall\n\t\t(v\\d+):A\n->\n\texists\n\t\t(v\\d+):B\n\twhere\n\t\t\\1.a1 = \\2.b1")
              .matcher(s)
              .find(),
          s);
    }

    /**
     * The variables of the constraint are sorted inLeft, which marks a type, although A and B are
     * entities, so a check of the constraint fails before it looks at the data.
     */
    @Test
    void sortsEntityVariablesAsTypes() throws Exception {
      Constraints c = ExpTestHelpers.eval(infer("A.a1 -> B.b1"), "C", Kind.CONSTRAINTS);
      ED ed = c.eds.get(0);
      assertEquals(Chc.inLeft("A"), ed.As.values().iterator().next());
      assertEquals(Chc.inLeft("B"), ed.Es.values().iterator().next());
      String msg =
          ExpTestHelpers.error(
              infer("A.a1 -> B.b1")
                  + """
                  instance I = literal : ST { generators a : A b : B equations a.a1 = x b.b1 = y }
                  command chk = check C I
                  """);
      assertTrue(
          Pattern.compile("On symbol v\\d+, the return Type inl A is not declared.")
              .matcher(msg)
              .find(),
          msg);
    }

    @Test
    void handWrittenConstraintIsChecked() throws Exception {
      String msg =
          ExpTestHelpers.error(
              SCHEMA
                  + """
                  constraints C = literal : ST { forall v0:A -> exists v1:B where v0.a1 = v1.b1 }
                  instance I = literal : ST { generators a : A b : B equations a.a1 = x b.b1 = y }
                  command chk = check C I
                  """);
      assertTrue(msg.contains("Not satisfied."), msg);
    }

    @Test
    void rejectsForeignKeys() throws Exception {
      String msg =
          ExpTestHelpers.error(
              infer("A.a1 -> B.b1")
                  .replace("attributes a1", "foreign_keys f : A -> B attributes a1"));
      assertTrue(msg.contains("Can't have FKs in infer"), msg);
    }

    @Test
    void rejectsNamesTheSchemaLacks() throws Exception {
      assertTrue(ExpTestHelpers.error(infer("Z.a1 -> B.b1")).contains("Not a source entity, Z"));
      assertTrue(ExpTestHelpers.error(infer("A.a1 -> Z.b1")).contains("Not a target entity, Z"));
      assertTrue(
          ExpTestHelpers.error(infer("A.zz -> B.b1")).contains("Not a source attribute, zz"));
      assertTrue(
          ExpTestHelpers.error(infer("A.a1 -> B.zz")).contains("Not a target attribute, zz"));
    }
  }

  @Nested
  class Expression {

    private final List<Quad<String, String, String, String>> quads =
        List.of(new Quad<>("A", "a1", "B", "b1"));

    private final EdsExpInfer exp =
        new EdsExpInfer(new EdsExpVar("S"), new EdsExpVar("T"), new SchExpVar("ST"), quads);

    @Test
    void printsWithoutTheMapping() {
      assertEquals("infer S ->  T : ST", exp.toString());
    }

    @Test
    void dependsOnBothConstraintsAndTheSchema() {
      assertEquals(
          Set.of(
              new Pair<>("S", Kind.CONSTRAINTS),
              new Pair<>("T", Kind.CONSTRAINTS),
              new Pair<>("ST", Kind.SCHEMA)),
          Set.copyOf(exp.deps()));
      assertFalse(exp.isVar());
      assertTrue(exp.options().isEmpty());
      assertEquals(ExpTestHelpers.BASE_OPTIONS, exp.allowedOptions());
    }

    @Test
    void isTypedByTheCombinedSchema() throws Exception {
      AqlTyping g = ExpTestHelpers.typing(infer("A.a1 -> B.b1"));
      assertEquals(new SchExpVar("ST"), exp.type(g));
      assertEquals(new SchExpVar("ST"), g.defs.eds.get("C"));
    }

    @Test
    void mapSkipsItsParts() {
      List<Exp<?>> seen = new ArrayList<>();
      exp.map(seen::add);
      assertEquals(List.of(exp), seen);
    }

    @Test
    void equalityComparesEveryPart() {
      assertEquals(exp, exp);
      assertEquals(
          exp, new EdsExpInfer(new EdsExpVar("S"), new EdsExpVar("T"), new SchExpVar("ST"), quads));
      assertEquals(
          exp.hashCode(),
          new EdsExpInfer(new EdsExpVar("S"), new EdsExpVar("T"), new SchExpVar("ST"), quads)
              .hashCode());
      assertNotEquals(
          exp, new EdsExpInfer(new EdsExpVar("X"), new EdsExpVar("T"), new SchExpVar("ST"), quads));
      assertNotEquals(
          exp, new EdsExpInfer(new EdsExpVar("S"), new EdsExpVar("X"), new SchExpVar("ST"), quads));
      assertNotEquals(
          exp, new EdsExpInfer(new EdsExpVar("S"), new EdsExpVar("T"), new SchExpVar("X"), quads));
      assertNotEquals(
          exp,
          new EdsExpInfer(new EdsExpVar("S"), new EdsExpVar("T"), new SchExpVar("ST"), List.of()));
      assertNotEquals(exp, null);
      assertNotEquals(exp, "infer");
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsAVisitor() {
      EdsExpVisitor<String, Unit, RuntimeException> v = mock(EdsExpVisitor.class);
      when(v.visit(Unit.unit, exp)).thenReturn("visited");
      assertEquals("visited", exp.accept(Unit.unit, v));
    }

    @Test
    void keepsTheMapping() {
      assertEquals(Map.of("A", "a1"), Map.of(exp.maps.get(0).first, exp.maps.get(0).second));
    }
  }
}
