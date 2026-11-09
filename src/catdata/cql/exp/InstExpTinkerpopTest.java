package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.Pair;
import catdata.Unit;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.Instance;
import catdata.cql.Kind;
import catdata.cql.exp.InstExp.InstExpVisitor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link InstExpTinkerpop}, {@code import_tinkerpop_all}, which imports every
 * vertex, edge and property of a TinkerPop graph onto the schema {@code tinkerpop}. The tests run a
 * Gremlin server over a TinkerGraph and load it with {@code exec_tinkerpop}.
 */
class InstExpTinkerpopTest {

  private static TinkerpopTestHelpers server;

  @BeforeAll
  static void startServer() throws Exception {
    server = TinkerpopTestHelpers.start();
  }

  @AfterAll
  static void stopServer() {
    server.close();
  }

  @BeforeEach
  void clearGraph() throws Exception {
    ExpTestHelpers.run(
        server.options() + "command c = exec_tinkerpop { \"g.V().drop().iterate()\" }");
  }

  /** Loads the graph ann -knows-> bob, then imports it. */
  private static Instance<String, String, Sym, Fk, Att, Object, Object, Object, Object> imported()
      throws Exception {
    AqlMultiDriver d =
        ExpTestHelpers.run(
            server.options()
                + """
                command load = exec_tinkerpop {
                  "g.addV('person').property('name','ann').as('a').addV('person').property('name','bob').addE('knows').from('a').property('since', 2020).iterate()"
                }
                instance I = import_tinkerpop_all
                """);
    return ExpTestHelpers.value(d, "I", Kind.INSTANCE);
  }

  private static Set<String> values(
      Instance<String, String, Sym, Fk, Att, Object, Object, Object, Object> i,
      String en,
      String att) {
    Set<String> ret = new TreeSet<>();
    for (Object x : i.algebra().en(en)) {
      Object o = i.algebra().att(Att.Att(en, att), x).obj();
      ret.add(String.valueOf(o instanceof Optional<?> p ? p.orElse(null) : o));
    }
    return ret;
  }

  @Test
  void importsVerticesEdgesAndProperties() throws Exception {
    var i = imported();
    assertEquals(2, i.algebra().size("Vertex"));
    assertEquals(1, i.algebra().size("Edge"));
    assertEquals(Set.of("person"), values(i, "Vertex", "label"));
    assertEquals(Set.of("knows"), values(i, "Edge", "label"));
    assertEquals(Set.of("ann", "bob"), values(i, "HasVertexProperty", "value"));
    assertEquals(Set.of("name"), values(i, "HasVertexProperty", "key"));
    assertEquals(Set.of("2020"), values(i, "HasEdgeProperty", "value"));
    assertEquals(Set.of("since"), values(i, "HasEdgeProperty", "key"));
  }

  @Test
  void pointsEachEdgeAtItsVertices() throws Exception {
    var i = imported();
    Object e = i.algebra().en("Edge").iterator().next();
    Object out = i.algebra().fk(Fk.Fk("Edge", "out"), e);
    Object in = i.algebra().fk(Fk.Fk("Edge", "in"), e);
    Map<Object, String> names = new HashMap<>();
    for (Object p : i.algebra().en("HasVertexProperty")) {
      Object v = i.algebra().fk(Fk.Fk("HasVertexProperty", "vertex"), p);
      names.put(v, String.valueOf(i.algebra().att(Att.Att("HasVertexProperty", "value"), p).obj()));
    }
    assertEquals("ann", names.get(out));
    assertEquals("bob", names.get(in));
  }

  @Test
  void isSkippedAtCompileTime() throws Exception {
    InstExpTinkerpop e = new InstExpTinkerpop(List.of());
    AqlEnv env = new AqlEnv(ExpTestHelpers.parse(""));
    assertThrows(IgnoreException.class, () -> e.eval0(env, true));
  }

  @Test
  void reportsAServerItCannotReach() throws Exception {
    String msg =
        ExpTestHelpers.error(
            "options tinkerpop_port = "
                + TinkerpopTestHelpers.freePort()
                + "\n"
                + "instance I = import_tinkerpop_all");
    assertTrue(msg.contains("Connection refused") || msg.contains("connect"), msg);
  }

  @Test
  void printsAsItsKeyword() {
    assertEquals("import_tinkerpop_all", new InstExpTinkerpop(List.of()).toString());
    assertEquals(
        "import_tinkerpop_all  {\n\t\n\toptions\n\t\ttinkerpop_port = 1}",
        new InstExpTinkerpop(List.of(new Pair<>("tinkerpop_port", "1"))).toString());
  }

  @Test
  void dependsOnNothing() {
    InstExpTinkerpop e = new InstExpTinkerpop(List.of());
    assertTrue(e.deps().isEmpty());
    assertTrue(e.direct(new AqlTyping()).isEmpty());
  }

  @Test
  void allowsTheTinkerpopOptions() {
    InstExpTinkerpop e = new InstExpTinkerpop(List.of(new Pair<>("tinkerpop_port", "1")));
    assertEquals(Map.of("tinkerpop_port", "1"), e.options());
    assertTrue(
        e.allowedOptions()
            .containsAll(
                Set.of(
                    AqlOption.tinkerpop_host,
                    AqlOption.tinkerpop_port,
                    AqlOption.tinkerpop_graph_name)));
  }

  @Test
  void isTypedByTheTinkerpopSchema() {
    assertEquals(new SchExpTinkerpop(), new InstExpTinkerpop(List.of()).type(new AqlTyping()));
  }

  @Test
  void equalityComparesTheOptions() {
    InstExpTinkerpop a = new InstExpTinkerpop(List.of());
    assertEquals(a, a);
    assertEquals(a, new InstExpTinkerpop(List.of()));
    assertEquals(a.hashCode(), new InstExpTinkerpop(List.of()).hashCode());
    assertNotEquals(a, new InstExpTinkerpop(List.of(new Pair<>("tinkerpop_port", "1"))));
    assertNotEquals(a, null);
    assertNotEquals(a, "import_tinkerpop_all");
  }

  @Test
  void mapsOverNothing() {
    InstExpTinkerpop e = new InstExpTinkerpop(List.of());
    List<Exp<?>> seen = new ArrayList<>();
    e.map(seen::add);
    assertEquals(List.of(e), seen);
  }

  @Test
  @SuppressWarnings("unchecked")
  void acceptsAVisitor() {
    InstExpTinkerpop e = new InstExpTinkerpop(List.of());
    InstExpVisitor<String, Unit, RuntimeException> v = mock(InstExpVisitor.class);
    when(v.visit(Unit.unit, e)).thenReturn("visited");
    assertSame("visited", e.accept(Unit.unit, v));
  }
}
