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
import catdata.cql.Instance;
import catdata.cql.Kind;
import catdata.cql.exp.InstExp.InstExpVar;
import catdata.cql.exp.InstExp.InstExpVisitor;
import catdata.cql.exp.MapExp.MapExpVar;
import catdata.cql.exp.SchExp.SchExpVar;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for {@link InstExpSigmaChase}, {@code sigma_chase F I}, the left Kan extension of I
 * along F computed by a chase, sequential or parallel, rather than by the algebra of {@code sigma}.
 */
class InstExpSigmaChaseTest {

  /** Both rows of A point at the one row of B, and only a1 has a value. */
  private static final String PROGRAM =
      """
      typeside Ty = literal { types String constants x : String }
      schema S = literal : Ty {
        entities A B
        foreign_keys f : A -> B
        attributes n : A -> String
      }
      schema T = literal : Ty {
        entities X Y
        foreign_keys g : X -> Y
        attributes p : X -> String
      }
      mapping F = literal : S -> T {
        entity A -> X
        foreign_keys f -> g
        attributes n -> p
        entity B -> Y
      }
      instance I = literal : S { generators a1 a2 : A b : B equations a1.f = b a2.f = b a1.n = x }
      instance L = sigma F I
      """;

  private static String program(String options) {
    return PROGRAM + "instance K = sigma_chase F I " + options + "\n";
  }

  private static Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?> instance(
      String program, String name) throws Exception {
    return ExpTestHelpers.eval(program, name, Kind.INSTANCE);
  }

  private static InstExpSigmaChase<?, ?, ?, ?> exp(
      String f, String i, Map<String, String> options) {
    return new InstExpSigmaChase<>(new MapExpVar(f), new InstExpVar(i), options);
  }

  private final InstExpSigmaChase<?, ?, ?, ?> exp = exp("F", "I", Map.of());

  @Nested
  class Evaluation {

    @ParameterizedTest
    @ValueSource(strings = {"", "{ options chase_style = parallel }"})
    void agreesWithSigma(String options) throws Exception {
      Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?> k = instance(program(options), "K");
      Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?> l = instance(program(options), "L");
      assertEquals(Set.of("X", "Y"), k.schema().ens);
      assertEquals(l.algebra().size("X"), k.algebra().size("X"));
      assertEquals(l.algebra().size("Y"), k.algebra().size("Y"));
      assertEquals(2, k.algebra().size("X"));
      assertEquals(1, k.algebra().size("Y"));
    }

    /** The help documents leftkan, which the expression does not accept. */
    @Test
    void rejectsTheDocumentedLeftKanStyle() throws Exception {
      String msg = ExpTestHelpers.error(program("{ options chase_style = leftkan }"));
      assertTrue(msg.contains("Style must be sequential or parallel"), msg);
    }

    /** The sequential chase binds the foreign key of a twice, even along the identity. */
    @Test
    void failsWithTheSequentialStyle() throws Exception {
      String program =
          """
          typeside Ty = literal { types String }
          schema S = literal : Ty { entities A B foreign_keys f : A -> B }
          instance I = literal : S { generators a : A }
          instance K = sigma_chase (identity S) I { options chase_style = sequential }
          """;
      String msg = ExpTestHelpers.error(program);
      assertTrue(msg.contains("Conflict on a.f, was 1 now is 2"), msg);
    }

    /**
     * f is sent to the identity, so a.f = b makes a and b one row, as sigma finds. The default
     * chase keeps them apart when a is declared first, and merges them when b is.
     */
    @ParameterizedTest
    @ValueSource(strings = {"a : A b : B", "b : B a : A"})
    void dependsOnTheOrderOfTheGeneratorsWhenTwoRowsMeet(String generators) throws Exception {
      String program =
          """
          typeside Ty = literal { types String }
          schema S = literal : Ty { entities A B foreign_keys f : A -> B }
          schema T = literal : Ty { entities X }
          mapping F = literal : S -> T { entity B -> X entity A -> X foreign_keys f -> identity }
          instance I = literal : S { generators %s equations a.f = b }
          instance K = sigma_chase F I
          instance L = sigma F I
          """
              .formatted(generators);
      assertEquals(1, instance(program, "L").algebra().size("X"));
      int expected = generators.startsWith("a") ? 2 : 1;
      assertEquals(expected, instance(program, "K").algebra().size("X"));
    }

    @Test
    void rejectsAnInstanceOffTheDomainOfTheMapping() {
      String program = PROGRAM + "instance J = empty : T\ninstance K = sigma_chase F J\n";
      RuntimeException e =
          assertThrows(RuntimeException.class, () -> ExpTestHelpers.typing(program));
      assertTrue(
          e.getMessage()
              .contains(
                  "Type error: In sigma_chase F J  domain of mapping is S but instance has schema T"),
          e.getMessage());
    }

    @Test
    void isNotEvaluatedAtCompileTime() throws Exception {
      AqlEnv env = ExpTestHelpers.run(PROGRAM).env;
      assertThrows(IgnoreException.class, () -> exp.eval0(env, true));
    }
  }

  @Nested
  class Expression {

    /** A space is printed after the instance whether or not there are options. */
    @Test
    void printsTheMappingTheInstanceAndTheOptions() {
      assertEquals("sigma_chase F I ", exp.toString());
      assertEquals(
          "sigma_chase F I  {\nchase_style = sequential\n}",
          exp("F", "I", Map.of("chase_style", "sequential")).toString());
    }

    @Test
    void isTypedByTheCodomainOfTheMapping() throws Exception {
      assertEquals(new SchExpVar("T"), exp.type(ExpTestHelpers.typing(PROGRAM)));
      assertEquals(
          Set.of(new Pair<>("F", Kind.MAPPING), new Pair<>("I", Kind.INSTANCE)),
          Set.copyOf(exp.deps()));
      assertEquals(Set.of(new InstExpVar("I")), Set.copyOf(exp.direct(null)));
      assertTrue(exp.options().isEmpty());
      assertTrue(exp.allowedOptions().contains(AqlOption.chase_style));
      List<Exp<?>> seen = new ArrayList<>();
      exp.map(seen::add);
      assertEquals(List.of(exp, new MapExpVar("F"), new InstExpVar("I")), seen);
    }

    @Test
    void equalityComparesEveryPart() {
      assertEquals(exp, exp);
      assertEquals(exp, exp("F", "I", Map.of()));
      assertEquals(exp.hashCode(), exp("F", "I", Map.of()).hashCode());
      assertNotEquals(exp, exp("G", "I", Map.of()));
      assertNotEquals(exp, exp("F", "J", Map.of()));
      assertNotEquals(exp, exp("F", "I", Map.of("chase_style", "sequential")));
      InstExpSigmaChase<?, ?, ?, ?> none = new InstExpSigmaChase<>(null, null, null);
      assertEquals(none, new InstExpSigmaChase<>(null, null, null));
      assertEquals(none.hashCode(), new InstExpSigmaChase<>(null, null, null).hashCode());
      assertNotEquals(none, exp);
      assertNotEquals(exp, none);
      assertNotEquals(new InstExpSigmaChase<>(new MapExpVar("F"), null, null), exp);
      assertNotEquals(new InstExpSigmaChase<>(new MapExpVar("F"), new InstExpVar("I"), null), exp);
      assertNotEquals(exp, new InstExpSigmaChase<>(new MapExpVar("F"), new InstExpVar("I"), null));
      assertNotEquals(exp, null);
      assertNotEquals(exp, "sigma_chase");
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsAVisitor() {
      InstExpVisitor<String, Unit, RuntimeException> v = mock(InstExpVisitor.class);
      when(v.visit(Unit.unit, exp)).thenReturn("visited");
      assertEquals("visited", exp.accept(Unit.unit, v));
    }
  }
}
