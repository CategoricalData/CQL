package catdata.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link DAG}, a directed acyclic graph that refuses an edge closing a cycle. */
class DAGTest {

  @Test
  void refusesAnEdgeThatClosesACycle() {
    var d = new DAG<String>();
    assertTrue(d.addEdge("a", "b"));
    assertTrue(d.addEdge("b", "c"));
    assertFalse(d.addEdge("c", "a"));
    assertTrue(d.hasPath("a", "c"));
    assertEquals(Set.of("b"), d.getChildren("a"));
    assertEquals(Set.of("c"), d.getSinks());
    d.addVertex("e");
    assertEquals(Set.of("c", "e"), d.getSinks());
  }

  @Test
  void removesAVertexWithItsEdges() {
    var d = new DAG<String>();
    d.addEdge("a", "b");
    d.addEdge("b", "c");
    d.removeVertex("b");
    assertFalse(d.hasPath("a", "c"));
    assertEquals(Set.of("a", "c"), d.vertices());
  }

  @Test
  void comparesByItsEdges() {
    var d = new DAG<String>();
    d.addEdge("a", "b");
    var e = new DAG<String>();
    e.addEdge("a", "b");
    assertEquals(d, d);
    assertEquals(d, e);
    assertEquals(d.hashCode(), e.hashCode());
    assertNotEquals(d, null);
    assertNotEquals(d, "dag");
    e.addEdge("b", "c");
    assertNotEquals(d, e);
  }
}
