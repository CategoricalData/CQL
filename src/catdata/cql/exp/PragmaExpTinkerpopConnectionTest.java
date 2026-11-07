package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import catdata.cql.AqlOptions;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests that {@link PragmaExpTinkerpop#execGremlin}, which every TinkerPop instance and command
 * goes through, releases what each call opens: the driver's cluster with its threads, and the two
 * configuration files it writes to the temporary directory.
 */
class PragmaExpTinkerpopConnectionTest {

  private static TinkerpopTestHelpers server;

  @BeforeAll
  static void startServer() throws Exception {
    server = TinkerpopTestHelpers.start();
  }

  @AfterAll
  static void stopServer() {
    server.close();
  }

  private static AqlOptions options() {
    return new AqlOptions(
        Map.of("tinkerpop_port", Integer.toString(server.port)), AqlOptions.initialOptions);
  }

  /** The live threads of Gremlin drivers. */
  private static long driverThreads() {
    return Thread.getAllStackTraces().keySet().stream()
        .filter(Thread::isAlive)
        .filter(t -> t.getName().startsWith("gremlin-driver-"))
        .count();
  }

  /**
   * The driver threads once they settle, waiting up to 30 s for them to fall to the given number: a
   * cluster shuts its event loops down as it closes, and they stop after a short quiet period.
   */
  private static long driverThreadsOnceAtMost(long expected) throws InterruptedException {
    long deadline = System.nanoTime() + 30_000_000_000L;
    while (driverThreads() > expected && System.nanoTime() < deadline) {
      Thread.sleep(100);
    }
    return driverThreads();
  }

  /** The configuration files that calls left in the temporary directory. */
  private static Set<String> configFiles() throws IOException {
    try (Stream<Path> files = Files.list(Path.of(System.getProperty("java.io.tmpdir")))) {
      return files
          .map(p -> p.getFileName().toString())
          .filter(n -> n.matches("tempfile0.*\\.yaml|tempfile1.*\\.properties"))
          .collect(Collectors.toSet());
    }
  }

  /** A traversal result still reads from the server, so the reader runs before the close. */
  @Test
  void readsATraversalBeforeItClosesTheConnection() throws Exception {
    List<?> values =
        PragmaExpTinkerpop.execGremlin(
            options(),
            List.of("g.inject(1, 2, 3)"),
            results -> ((GraphTraversal<?, ?>) results.get(0)).toList());
    assertEquals(List.of(1, 2, 3), values);
  }

  /** Each script reads from the server first, since the driver connects only when a script does. */
  @Test
  void releasesTheClusterAndTheFilesOfEachCall() throws Exception {
    long threads = driverThreads();
    Set<String> files = configFiles();
    for (int i = 0; i < 5; i++) {
      assertEquals(
          "[2]",
          PragmaExpTinkerpop.execGremlin(options(), List.of("g.inject(2).next()"), List::toString));
    }
    assertEquals(threads, driverThreadsOnceAtMost(threads));
    assertEquals(files, configFiles());
  }

  @Test
  void releasesTheClusterAndTheFilesWhenAScriptFails() throws Exception {
    long threads = driverThreads();
    Set<String> files = configFiles();
    assertThrows(
        ExecutionException.class,
        () ->
            PragmaExpTinkerpop.execGremlin(
                options(), List.of("g.inject(1).next(); nope()"), List::toString));
    assertEquals(threads, driverThreadsOnceAtMost(threads));
    assertEquals(files, configFiles());
  }
}
