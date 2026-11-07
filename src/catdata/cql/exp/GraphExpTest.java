package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.Pair;
import catdata.Unit;
import catdata.cql.Graph;
import catdata.cql.Kind;
import catdata.cql.exp.GraphExp.GraphExpLiteral;
import catdata.cql.exp.GraphExp.GraphExpRaw;
import catdata.cql.exp.GraphExp.GraphExpVar;
import catdata.cql.exp.GraphExp.GraphExpVisitor;
import catdata.graph.DMG;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link GraphExp}: the graph literal {@code literal { imports ... nodes ... edges
 * ... }}, a graph variable, and a graph given as a value.
 */
class GraphExpTest {

  private static final String PROGRAM =
      """
      graph G0 = literal { nodes a b edges f : a -> b }
      graph G = literal { imports G0 nodes c edges g : b -> c }
      graph H = G
      """;

  private static Graph<String, String> graph(String name) throws Exception {
    return ExpTestHelpers.eval(PROGRAM, name, Kind.GRAPH);
  }

  @Nested
  class Literal {

    @Test
    void addsTheNodesAndEdgesOfItsImports() throws Exception {
      Graph<String, String> g = graph("G");
      assertEquals(Set.of("a", "b", "c"), g.dmg.nodes);
      assertEquals(Map.of("f", new Pair<>("a", "b"), "g", new Pair<>("b", "c")), g.dmg.edges);
    }

    @Test
    void resolvesAVariable() throws Exception {
      assertEquals(graph("G").dmg.nodes, graph("H").dmg.nodes);
    }

    @Test
    void printsItsNodesAndEdgesButNotItsImports() throws Exception {
      GraphExpRaw g = ExpTestHelpers.exp(PROGRAM, "G");
      assertEquals("literal {\n\tnodes\n\t\tc\n\tedges\n\t\tg: b -> c\n}", g.toString());
    }

    /** With no nodes of its own, a literal prints as empty, whatever it imports. */
    @Test
    void printsAsEmptyWithoutNodesOfItsOwn() throws Exception {
      GraphExpRaw g =
          ExpTestHelpers.exp(
              "graph G0 = literal { nodes a }\ngraph G = literal { imports G0 }", "G");
      assertEquals("literal {}", g.toString());
    }

    @Test
    void indexesItsNodesAndEdgesForTheOutline() throws Exception {
      GraphExpRaw g = ExpTestHelpers.exp(PROGRAM, "G");
      assertTrue(g.raw().keySet().containsAll(Set.of("nodes", "edges")), g.raw().toString());
      assertTrue(g.options().isEmpty());
    }

    @Test
    void dependsOnItsImports() throws Exception {
      GraphExpRaw g = ExpTestHelpers.exp(PROGRAM, "G");
      assertEquals(Set.of(new Pair<>("G0", Kind.GRAPH)), Set.copyOf(g.deps()));
      assertEquals(Kind.GRAPH, g.kind());
      assertEquals(Unit.unit, g.type(new AqlTyping()));
      assertEquals(ExpTestHelpers.BASE_OPTIONS, g.allowedOptions());
      List<Exp<?>> seen = new ArrayList<>();
      g.map(seen::add);
      assertEquals(List.of(g), seen);
      assertEquals(new GraphExpVar("v"), g.Var("v"));
    }

    @Test
    void equalityComparesNodesEdgesAndImports() throws Exception {
      GraphExpRaw g = ExpTestHelpers.exp(PROGRAM, "G");
      assertEquals(g, g);
      assertEquals(g, ExpTestHelpers.exp(PROGRAM, "G"));
      assertEquals(g.hashCode(), ExpTestHelpers.<GraphExpRaw>exp(PROGRAM, "G").hashCode());
      assertNotEquals(g, ExpTestHelpers.exp(PROGRAM.replace("nodes c", "nodes d"), "G"));
      assertNotEquals(g, ExpTestHelpers.exp(PROGRAM.replace("g : b -> c", "g : c -> b"), "G"));
      assertNotEquals(g, ExpTestHelpers.exp(PROGRAM.replace("imports G0 ", ""), "G"));
      assertNotEquals(g, null);
      assertNotEquals(g, "literal");
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsAVisitor() throws Exception {
      GraphExpRaw g = ExpTestHelpers.exp(PROGRAM, "G");
      GraphExpVisitor<String, Unit, RuntimeException> v = mock(GraphExpVisitor.class);
      when(v.visit(Unit.unit, g)).thenReturn("visited");
      assertEquals("visited", g.accept(Unit.unit, v));
    }
  }

  @Nested
  class Value {

    private final DMG<String, String> dmg =
        new DMG<>(Set.of("a", "b"), Map.of("f", new Pair<>("a", "b")));

    private final GraphExpLiteral exp = new GraphExpLiteral(dmg);

    @Test
    void resolvesToItsGraph() {
      assertEquals(dmg, exp.resolve(null).dmg);
      assertEquals(dmg.toString(), exp.toString());
    }

    @Test
    void hasNoDependenciesOrOptions() {
      assertTrue(exp.deps().isEmpty());
      assertTrue(exp.options().isEmpty());
      assertEquals(ExpTestHelpers.BASE_OPTIONS, exp.allowedOptions());
      List<Exp<?>> seen = new ArrayList<>();
      exp.map(seen::add);
      assertEquals(List.of(exp), seen);
    }

    @Test
    void equalityComparesTheGraph() {
      assertEquals(exp, exp);
      assertEquals(exp, new GraphExpLiteral(dmg));
      assertEquals(exp.hashCode(), new GraphExpLiteral(dmg).hashCode());
      assertNotEquals(exp, new GraphExpLiteral(new DMG<>(Set.of("a"), Map.of())));
      assertNotEquals(exp, new GraphExpLiteral(null));
      assertNotEquals(new GraphExpLiteral(null), exp);
      assertEquals(new GraphExpLiteral(null), new GraphExpLiteral(null));
      assertNotEquals(exp, null);
      assertNotEquals(exp, "graph");
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsAVisitor() {
      GraphExpVisitor<String, Unit, RuntimeException> v = mock(GraphExpVisitor.class);
      when(v.visit(Unit.unit, exp)).thenReturn("visited");
      assertEquals("visited", exp.accept(Unit.unit, v));
    }
  }

  @Nested
  class Variable {

    private final GraphExpVar exp = new GraphExpVar("G");

    @Test
    void isAVariableThatDependsOnItsGraph() {
      assertTrue(exp.isVar());
      assertEquals(List.of(new Pair<>("G", Kind.GRAPH)), new ArrayList<>(exp.deps()));
      assertTrue(exp.options().isEmpty());
      assertEquals("G", exp.toString());
      List<Exp<?>> seen = new ArrayList<>();
      exp.map(seen::add);
      assertEquals(List.of(exp), seen);
      assertEquals(ExpTestHelpers.BASE_OPTIONS, exp.allowedOptions());
    }

    @Test
    void equalityComparesTheName() {
      assertEquals(exp, exp);
      assertEquals(exp, new GraphExpVar("G"));
      assertEquals(exp.hashCode(), new GraphExpVar("G").hashCode());
      assertNotEquals(exp, new GraphExpVar("H"));
      assertNotEquals(exp, null);
      assertNotEquals(exp, "G");
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsAVisitor() {
      GraphExpVisitor<String, Unit, RuntimeException> v = mock(GraphExpVisitor.class);
      when(v.visit(Unit.unit, exp)).thenReturn("visited");
      assertEquals("visited", exp.accept(Unit.unit, v));
    }
  }
}
