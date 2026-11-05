package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.Pair;
import catdata.Unit;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.Kind;
import catdata.cql.Transform;
import catdata.cql.exp.InstExp.InstExpVar;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TransExpRaw}, the transform literal {@code literal : I -> J { generators
 * ... }}, which sends each generator and labelled null of I to a term of J.
 */
class TransExpRawTest {

  private static final String BASE =
      """
      typeside Ty = literal { types String constants x : String }
      schema S = literal : Ty { entities A attributes n : A -> String }
      schema T = literal : Ty { entities A }
      instance I = literal : S { generators a : A s : String equations a.n = s }
      instance J = literal : S { generators b : A equations b.n = x }
      transform t0 = literal : I -> J { generators a -> b s -> x }
      """;

  private static final String LITERAL =
      "transform t = literal : I -> J { generators a -> b s -> x }";

  private static final String PROGRAM = BASE + LITERAL + "\n";

  private static TransExpRaw exp(String literal) throws Exception {
    return ExpTestHelpers.exp(BASE + literal, "t");
  }

  @Nested
  class Evaluation {

    @Test
    void sendsEachGeneratorAndLabelledNullToATerm() throws Exception {
      Transform<String, String, Sym, Fk, Att, String, String, String, String, ?, ?, ?, ?> t =
          ExpTestHelpers.eval(PROGRAM, "t", Kind.TRANSFORM);
      assertEquals("b", t.gens().apply("a", "A").toString());
      assertEquals("x", t.sks().apply("s", "String").toString());
    }

    /** The grammar accepts imports, but evaluation fails whenever there is one. */
    @Test
    void cannotImportATransform() throws Exception {
      String msg = ExpTestHelpers.error(BASE + "transform t = literal : I -> J { imports t0 }\n");
      assertTrue(msg.contains("Anomaly: please report"), msg);
    }

    @Test
    void rejectsANameThatIsNotInTheSource() throws Exception {
      String msg = ExpTestHelpers.error(PROGRAM.replace("s -> x }", "s -> x z -> b }"));
      assertTrue(
          msg.contains("In transform for z, z is not a source generator/labelled null"), msg);
    }

    @Test
    void rejectsInstancesOfDifferentSchemas() {
      String program = BASE + "instance K = empty : T\ntransform t = literal : I -> K { }\n";
      RuntimeException e =
          assertThrows(RuntimeException.class, () -> ExpTestHelpers.typing(program));
      assertTrue(e.getMessage().contains("Mismatched schemas:\n\nS\n\nand\n\nT"), e.getMessage());
    }

    @Test
    void isTypedByItsInstances() throws Exception {
      AqlTyping g = ExpTestHelpers.typing(PROGRAM);
      assertEquals(new Pair<>(new InstExpVar("I"), new InstExpVar("J")), exp(LITERAL).type(g));
    }
  }

  @Nested
  class Expression {

    private static final String FULL =
        "transform t = literal : I -> J { imports t0 generators a -> b s -> x"
            + " options dont_validate_unsafe = true }";

    @Test
    void printsItsImportsGeneratorsAndOptions() throws Exception {
      assertEquals(
          "literal : I -> J {\n\timports\n\t\tt0\n\n\tgenerators\n\t\ta -> b\n\t\ts -> x\n"
              + "\n\toptions\n\t\tdont_validate_unsafe = true\n}",
          exp(FULL).toString());
      assertEquals("literal : I -> J {}", exp("transform t = literal : I -> J { }").toString());
    }

    @Test
    void dependsOnItsInstancesAndImports() throws Exception {
      TransExpRaw e = exp(FULL);
      assertEquals(
          Set.of(
              new Pair<>("I", Kind.INSTANCE),
              new Pair<>("J", Kind.INSTANCE),
              new Pair<>("t0", Kind.TRANSFORM)),
          Set.copyOf(e.deps()));
      assertEquals(Map.of("dont_validate_unsafe", "true"), e.options());
      assertTrue(e.allowedOptions().contains(AqlOption.dont_validate_unsafe));
      List<Exp<?>> seen = new ArrayList<>();
      e.map(seen::add);
      assertEquals(List.of(e, new InstExpVar("I"), new InstExpVar("J")), seen);
    }

    @Test
    void indexesItsGeneratorsForTheOutline() throws Exception {
      assertEquals("a -> b", exp(LITERAL).raw().get("generators").get(0).toString());
    }

    @Test
    void equalityComparesEveryPart() throws Exception {
      TransExpRaw e = exp(FULL);
      assertEquals(e, e);
      assertEquals(e, exp(FULL));
      assertEquals(e.hashCode(), exp(FULL).hashCode());
      for (Pair<String, String> change :
          List.of(
              new Pair<>(": I ->", ": J ->"),
              new Pair<>("-> J {", "-> I {"),
              new Pair<>("imports t0", "imports t1"),
              new Pair<>("a -> b", "a -> a"),
              new Pair<>("= true", "= false"))) {
        assertNotEquals(e, exp(FULL.replace(change.first, change.second)), change.second);
      }
      assertNotEquals(e, null);
      assertNotEquals(e, "literal");
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsAVisitor() throws Exception {
      TransExpRaw e = exp(LITERAL);
      TransExpVisitor<String, Unit, RuntimeException> v = mock(TransExpVisitor.class);
      when(v.visit(Unit.unit, e)).thenReturn("visited");
      assertEquals("visited", e.accept(Unit.unit, v));
    }
  }
}
