package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.InteriorLabel;
import catdata.Pair;
import catdata.Unit;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.Instance;
import catdata.cql.Kind;
import catdata.cql.exp.GraphExp.GraphExpVar;
import catdata.cql.exp.InstExp.InstExpVar;
import catdata.cql.exp.InstExp.InstExpVisitor;
import catdata.cql.exp.SchExp.SchExpVar;
import catdata.cql.exp.TransExp.TransExpVar;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link InstExpColim}, {@code colimit G S { nodes ... edges ... }}, the colimit of
 * a diagram of instances of S shaped like the graph G: an instance for each node and a transform
 * for each edge.
 */
class InstExpColimTest {

  /** The colimit of t : I -> J glues a of I to b of J. */
  private static final String BASE =
      """
      typeside Ty = literal { types String constants x y : String }
      schema S = literal : Ty { entities A attributes n : A -> String }
      schema T = literal : Ty { entities A }
      instance I = literal : S { generators a : A equations a.n = x }
      instance J = literal : S { generators b c : A equations b.n = x c.n = y }
      transform t = literal : I -> J { generators a -> b }
      graph G = literal { nodes u v edges e : u -> v }
      """;

  private static final String COLIMIT =
      "instance K = colimit G S { nodes u -> I v -> J edges e -> t }";

  private static final String PROGRAM = BASE + COLIMIT + "\n";

  /** The program with static typing on and the given colimit. */
  private static String statically(String colimit) {
    return "options static_typing = true\n" + BASE + colimit + "\n";
  }

  private static InstExpColim<?, ?, ?, ?> exp(String colimit) throws Exception {
    return ExpTestHelpers.exp(BASE + colimit, "K");
  }

  @Nested
  class Evaluation {

    @Test
    void gluesTheInstancesAlongTheTransforms() throws Exception {
      Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?> k =
          ExpTestHelpers.eval(PROGRAM, "K", Kind.INSTANCE);
      assertEquals(3, k.gens().size());
      assertEquals(2, k.algebra().size("A"));
    }

    @Test
    void rejectsANodeOnAnotherSchema() throws Exception {
      String program = PROGRAM.replace("v -> J", "v -> (empty : T)");
      RuntimeException e =
          assertThrows(RuntimeException.class, () -> ExpTestHelpers.typing(program));
      assertTrue(
          e.getMessage().contains("The instance for v has schema T, not S as expected"),
          e.getMessage());
    }

    @Test
    void failsOnAnEdgeWithoutATransform() throws Exception {
      String msg = ExpTestHelpers.error(PROGRAM.replace(" edges e -> t", ""));
      assertTrue(msg.contains("\"catdata.cql.Transform.src()\""), msg);
    }

    @Test
    void failsOnANodeWithoutAnInstance() throws Exception {
      String msg = ExpTestHelpers.error(PROGRAM.replace(" v -> J", ""));
      assertTrue(msg.contains("the entity for generator (v, b) is not defined."), msg);
    }

    @Test
    void isNotEvaluatedAtCompileTime() throws Exception {
      AqlEnv env = ExpTestHelpers.run(PROGRAM).env;
      InstExpColim<?, ?, ?, ?> e = exp(COLIMIT);
      assertThrows(IgnoreException.class, () -> e.eval0(env, true));
    }
  }

  /** With static_typing, set for the whole program, each transform is checked against its edge. */
  @Nested
  class StaticTyping {

    private String typingError(String colimit) {
      RuntimeException e =
          assertThrows(RuntimeException.class, () -> ExpTestHelpers.typing(statically(colimit)));
      return e.getMessage();
    }

    @Test
    void acceptsTransformsBetweenTheInstancesOfTheirNodes() throws Exception {
      AqlTyping g = ExpTestHelpers.typing(statically(COLIMIT));
      assertEquals(new SchExpVar("S"), exp(COLIMIT).type(g));
    }

    @Test
    void rejectsATransformFromAnotherInstance() {
      assertTrue(
          typingError(COLIMIT.replace("u -> I", "u -> J"))
              .contains("On e, its source is I but should be J"));
    }

    @Test
    void rejectsATransformIntoAnotherInstance() {
      String colimit = "transform t2 = identity I\n" + COLIMIT.replace("e -> t", "e -> t2");
      assertTrue(typingError(colimit).contains("On e, its target is I but should be J"));
    }

    @Test
    void failsOnAnEdgeWhoseNodeHasNoInstance() {
      assertTrue(typingError(COLIMIT.replace(" v -> J", "")).contains("\"reqdDst\" is null"));
    }

    @Test
    void failsOnAnEdgeWithoutATransform() {
      assertTrue(
          typingError(COLIMIT.replace(" edges e -> t", ""))
              .contains("\"catdata.cql.exp.TransExp.type(catdata.cql.exp.AqlTyping)\""));
    }

    @Test
    void cannotBeSetOnTheColimitItself() {
      String program = PROGRAM.replace("e -> t }", "e -> t options static_typing = true }");
      RuntimeException e =
          assertThrows(RuntimeException.class, () -> ExpTestHelpers.typing(program));
      assertTrue(e.getMessage().contains("Option not allowed: static_typing."), e.getMessage());
    }
  }

  @Nested
  class Expression {

    @Test
    void printsTheShapeTheSchemaAndTheDiagram() throws Exception {
      assertEquals(
          "colimit G S {\n\tnodes\n\t\tu -> I\n\t\tv -> J\n\tedges\n\t\te -> t}",
          exp(COLIMIT).toString());
      assertEquals("colimit G S {}", exp("instance K = colimit G S { }").toString());
    }

    @Test
    void dependsOnEveryPart() throws Exception {
      InstExpColim<?, ?, ?, ?> e = exp(COLIMIT);
      assertEquals(
          Set.of(
              new Pair<>("G", Kind.GRAPH),
              new Pair<>("S", Kind.SCHEMA),
              new Pair<>("I", Kind.INSTANCE),
              new Pair<>("J", Kind.INSTANCE),
              new Pair<>("t", Kind.TRANSFORM)),
          Set.copyOf(e.deps()));
      assertEquals(Set.of(new InstExpVar("I"), new InstExpVar("J")), Set.copyOf(e.direct(null)));
      assertTrue(e.options().isEmpty());
      assertTrue(e.allowedOptions().contains(AqlOption.require_consistency));
      assertTrue(e.allowedOptions().contains(AqlOption.allow_java_eqs_unsafe));
      List<Exp<?>> seen = new ArrayList<>();
      e.map(seen::add);
      assertEquals(
          List.of(
              e,
              new SchExpVar("S"),
              new GraphExpVar("G"),
              new InstExpVar("I"),
              new InstExpVar("J"),
              new TransExpVar("t")),
          seen);
    }

    @Test
    void indexesTheNodesAndEdgesForTheOutline() throws Exception {
      Map<String, List<InteriorLabel<Object>>> raw = exp(COLIMIT).raw();
      assertEquals(Set.of("nodes", "edges"), raw.keySet());
      assertEquals("u -> I", raw.get("nodes").get(0).toString());
      assertEquals("e -> t", raw.get("edges").get(0).toString());
    }

    @Test
    void equalityComparesEveryPart() throws Exception {
      InstExpColim<?, ?, ?, ?> e = exp(COLIMIT);
      assertEquals(e, e);
      assertEquals(e, exp(COLIMIT));
      assertEquals(e.hashCode(), exp(COLIMIT).hashCode());
      for (Pair<String, String> change :
          List.of(
              new Pair<>("colimit G", "colimit H"),
              new Pair<>("G S", "G T"),
              new Pair<>("u -> I", "u -> J"),
              new Pair<>("e -> t", "e -> s"),
              new Pair<>("e -> t }", "e -> t options timeout = 5 }"))) {
        assertNotEquals(e, exp(COLIMIT.replace(change.first, change.second)), change.second);
      }
      assertNotEquals(e, null);
      assertNotEquals(e, "colimit");
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsAVisitor() throws Exception {
      InstExpColim<?, ?, ?, ?> e = exp(COLIMIT);
      InstExpVisitor<String, Unit, RuntimeException> v = mock(InstExpVisitor.class);
      when(v.visit(Unit.unit, e)).thenReturn("visited");
      assertEquals("visited", e.accept(Unit.unit, v));
    }
  }
}
