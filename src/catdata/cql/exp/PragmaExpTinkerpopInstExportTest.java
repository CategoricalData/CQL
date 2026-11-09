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
import catdata.cql.Pragma;
import catdata.cql.exp.InstExp.InstExpVar;
import catdata.cql.exp.PragmaExp.PragmaExpVisitor;
import java.util.ArrayList;
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
 * Unit tests for {@link PragmaExpTinkerpopInstExport}, {@code export_tinkerpop_instance I}, which
 * writes one vertex per row of {@code I}, labelled by its entity, with a property per attribute,
 * and one edge per foreign key.
 */
class PragmaExpTinkerpopInstExportTest {

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

  private static String program(String name) {
    return server.options()
        + """
        typeside Ty = sql
        schema S = literal : Ty {
          entities Emp Dept
          foreign_keys worksIn : Emp -> Dept
          attributes name : Emp -> Varchar  title : Dept -> Varchar
        }
        instance J = literal : S {
          generators e : Emp d : Dept
          equations e.worksIn = d e.name = %s
        }
        command x = export_tinkerpop_instance J
        instance I = import_tinkerpop_all
        """
            .formatted(name);
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
  void writesAVertexPerRowAndAnEdgePerForeignKey() throws Exception {
    AqlMultiDriver d = ExpTestHelpers.run(program("ann"));
    Instance<String, String, Sym, Fk, Att, Object, Object, Object, Object> i =
        ExpTestHelpers.value(d, "I", Kind.INSTANCE);
    assertEquals(Set.of("Dept", "Emp"), values(i, "Vertex", "label"));
    assertEquals(Set.of("worksIn"), values(i, "Edge", "label"));
    // the unset title of d is a labelled null, which is left out
    assertEquals(Set.of("name"), values(i, "HasVertexProperty", "key"));
    assertEquals(Set.of("ann"), values(i, "HasVertexProperty", "value"));
    Pragma p = ExpTestHelpers.value(d, "x", Kind.PRAGMA);
    assertTrue(
        p.toString()
            .startsWith("g.addV('Emp').property('name','ann').as('0').addV('Dept').as('1')"),
        p.toString());
    assertTrue(p.toString().contains(".addE('worksIn').from('0').to('1')"), p.toString());
  }

  /** Values are not escaped, so a quote ends the Groovy string. */
  @Test
  void cannotExportAValueWithAQuote() throws Exception {
    AqlMultiDriver d = ExpTestHelpers.run(program("\"O'Brien\""));
    Pragma p = ExpTestHelpers.value(d, "x", Kind.PRAGMA);
    assertTrue(p.toString().startsWith("g.addV('Emp').property('name','O'Brien')"), p.toString());
    Instance<String, String, Sym, Fk, Att, Object, Object, Object, Object> i =
        ExpTestHelpers.value(d, "I", Kind.INSTANCE);
    assertEquals(0, i.algebra().size("Vertex"));
  }

  @Test
  void isSkippedAtCompileTime() throws Exception {
    PragmaExpTinkerpopInstExport<?, ?, ?, ?> e =
        new PragmaExpTinkerpopInstExport<>(new InstExpVar("J"), List.of());
    AqlEnv env = new AqlEnv(ExpTestHelpers.parse(""));
    assertThrows(IgnoreException.class, () -> e.eval0(env, true));
  }

  /** The keyword is misspelled in the printed form. */
  @Test
  void printsAMisspelledKeyword() {
    assertEquals(
        "export_tinkerop_instance J {}",
        new PragmaExpTinkerpopInstExport<>(new InstExpVar("J"), List.of()).toString());
  }

  @Test
  void dependsOnTheInstanceAndAllowsTheTinkerpopOptions() {
    PragmaExpTinkerpopInstExport<?, ?, ?, ?> e =
        new PragmaExpTinkerpopInstExport<>(
            new InstExpVar("J"), List.of(new Pair<>("tinkerpop_port", "1")));
    assertEquals(List.of(new Pair<>("J", Kind.INSTANCE)), new ArrayList<>(e.deps()));
    assertEquals(Map.of("tinkerpop_port", "1"), e.options());
    assertTrue(
        e.allowedOptions()
            .containsAll(
                Set.of(
                    AqlOption.tinkerpop_host,
                    AqlOption.tinkerpop_port,
                    AqlOption.tinkerpop_graph_name)));
    List<Exp<?>> seen = new ArrayList<>();
    e.map(seen::add);
    // The instance itself is not visited
    assertEquals(List.of(e), seen);
  }

  @Test
  void equalityComparesTheInstanceAndOptions() {
    PragmaExpTinkerpopInstExport<?, ?, ?, ?> a =
        new PragmaExpTinkerpopInstExport<>(new InstExpVar("J"), List.of());
    assertEquals(a, a);
    assertEquals(a, new PragmaExpTinkerpopInstExport<>(new InstExpVar("J"), List.of()));
    assertEquals(
        a.hashCode(),
        new PragmaExpTinkerpopInstExport<>(new InstExpVar("J"), List.of()).hashCode());
    assertNotEquals(a, new PragmaExpTinkerpopInstExport<>(new InstExpVar("K"), List.of()));
    assertNotEquals(
        a,
        new PragmaExpTinkerpopInstExport<>(
            new InstExpVar("J"), List.of(new Pair<>("tinkerpop_port", "1"))));
    assertNotEquals(a, null);
    assertNotEquals(a, "export_tinkerpop_instance J");
  }

  @Test
  @SuppressWarnings("unchecked")
  void acceptsAVisitor() {
    PragmaExpTinkerpopInstExport<?, ?, ?, ?> e =
        new PragmaExpTinkerpopInstExport<>(new InstExpVar("J"), List.of());
    PragmaExpVisitor<String, Unit, RuntimeException> v = mock(PragmaExpVisitor.class);
    when(v.visit(Unit.unit, e)).thenReturn("visited");
    assertEquals("visited", e.accept(Unit.unit, v));
  }
}
