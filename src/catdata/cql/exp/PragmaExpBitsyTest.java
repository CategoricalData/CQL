package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.Pair;
import catdata.Unit;
import catdata.cql.AqlOptions;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.Pragma;
import catdata.cql.exp.PragmaExp.PragmaExpVisitor;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.tinkerpop.gremlin.server.GremlinServer;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PragmaExpBitsy}, {@code command c = spawn_bitsy}, which starts a Gremlin
 * server over a Bitsy graph stored in {@code bitsy_db_path}. The pragma keeps no handle to the
 * server, so the tests stop it through the pragma's field.
 */
class PragmaExpBitsyTest {

  private static GremlinServer server(Pragma p) throws ReflectiveOperationException {
    Field f = p.getClass().getDeclaredField("srv");
    f.setAccessible(true);
    return (GremlinServer) f.get(p);
  }

  private static Pragma spawn(int port, Path db) throws Exception {
    PragmaExpBitsy e =
        new PragmaExpBitsy(
            List.of(
                new Pair<>("tinkerpop_port", Integer.toString(port)),
                new Pair<>("bitsy_db_path", db.toString())));
    Pragma p = e.eval0(new AqlEnv(ExpTestHelpers.parse("")), false);
    p.execute();
    return p;
  }

  @Test
  void startsAServerOverTheGivenFolder() throws Exception {
    int port = TinkerpopTestHelpers.freePort();
    Path db = Files.createTempDirectory("bitsy_test");
    Pragma p = spawn(port, db);
    try {
      assertEquals(
          "Bitsy Tinkerpop-Gremlin-Groovy Server spawned: GremlinServer localhost:" + port + "\n\n",
          p.toString());
      String before = p.toString();
      p.execute();
      assertEquals(before, p.toString());
      assertTrue(new File(db.toFile(), "txA.txt").exists());
    } finally {
      server(p).stop().join();
    }
  }

  @Test
  void createsAMissingFolder() throws Exception {
    Path db = Files.createTempDirectory("bitsy_test").resolve("new");
    Pragma p = spawn(TinkerpopTestHelpers.freePort(), db);
    try {
      assertTrue(db.toFile().isDirectory());
    } finally {
      server(p).stop().join();
    }
  }

  @Test
  void reportsAPathThatIsAFile() throws Exception {
    Path file = Files.createTempFile("bitsy_test", ".txt");
    Pragma p = spawn(TinkerpopTestHelpers.freePort(), file);
    assertEquals("Bitsy path exists but is not a folder: " + file, p.toString());
  }

  /** The default path is the working directory, so a temporary folder is never used. */
  @Test
  void defaultsToTheWorkingDirectory() {
    assertEquals(
        ".",
        new AqlOptions(Map.of(), AqlOptions.initialOptions).getOrDefault(AqlOption.bitsy_db_path));
  }

  @Test
  void isSkippedAtCompileTime() throws Exception {
    PragmaExpBitsy e = new PragmaExpBitsy(List.of());
    AqlEnv env = new AqlEnv(ExpTestHelpers.parse(""));
    assertThrows(IgnoreException.class, () -> e.eval0(env, true));
  }

  /** The separators are swapped, so the printed options do not parse. */
  @Test
  void printsItsOptions() {
    assertEquals("spawn_bitsy {}", new PragmaExpBitsy(List.of()).toString());
    assertEquals(
        "spawn_bitsy {\n\toptionstinkerpop_port\n\t\t1}",
        new PragmaExpBitsy(List.of(new Pair<>("tinkerpop_port", "1"))).toString());
  }

  @Test
  void escapesForEcmaScript() {
    assertEquals("a\\'b", PragmaExpBitsy.maybeQuote("a'b"));
  }

  @Test
  void hasNoDependenciesAndAllowsTheBitsyOptions() {
    PragmaExpBitsy e = new PragmaExpBitsy(List.of(new Pair<>("tinkerpop_port", "1")));
    assertTrue(e.deps().isEmpty());
    assertEquals(Map.of("tinkerpop_port", "1"), e.options());
    assertTrue(
        e.allowedOptions()
            .containsAll(
                Set.of(
                    AqlOption.tinkerpop_host,
                    AqlOption.tinkerpop_port,
                    AqlOption.tinkerpop_graph_name,
                    AqlOption.bitsy_db_path)));
    List<Exp<?>> seen = new ArrayList<>();
    e.map(seen::add);
    assertEquals(List.of(e), seen);
  }

  @Test
  void equalityComparesTheOptions() {
    PragmaExpBitsy a = new PragmaExpBitsy(List.of());
    assertEquals(a, a);
    assertEquals(a, new PragmaExpBitsy(List.of()));
    assertEquals(a.hashCode(), new PragmaExpBitsy(List.of()).hashCode());
    assertNotEquals(a, new PragmaExpBitsy(List.of(new Pair<>("tinkerpop_port", "1"))));
    assertNotEquals(a, null);
    assertNotEquals(a, "spawn_bitsy");
  }

  @Test
  @SuppressWarnings("unchecked")
  void acceptsAVisitor() {
    PragmaExpBitsy e = new PragmaExpBitsy(List.of());
    PragmaExpVisitor<String, Unit, RuntimeException> v = mock(PragmaExpVisitor.class);
    when(v.visit(Unit.unit, e)).thenReturn("visited");
    assertEquals("visited", e.accept(Unit.unit, v));
  }
}
