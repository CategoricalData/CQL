package catdata.cql.exp;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.tinkerpop.gremlin.server.GremlinServer;
import org.apache.tinkerpop.gremlin.server.Settings;

/**
 * A Gremlin server over an in-memory TinkerGraph, for the tests of the TinkerPop expressions. It
 * speaks the serializer the client of {@link PragmaExpTinkerpop#execGremlin} asks for, and binds
 * the traversal source {@code g}, the default of {@code tinkerpop_graph_name}.
 */
final class TinkerpopTestHelpers implements AutoCloseable {

  final int port;

  private final GremlinServer server;

  private TinkerpopTestHelpers(int port, GremlinServer server) {
    this.port = port;
    this.server = server;
  }

  /** A free local port. */
  static int freePort() throws IOException {
    try (ServerSocket s = new ServerSocket(0)) {
      return s.getLocalPort();
    }
  }

  /** Starts a server on a free port. */
  static TinkerpopTestHelpers start() throws Exception {
    int port = freePort();
    Path dir = Files.createTempDirectory("tinkerpop_test");
    Path graph = dir.resolve("graph.properties");
    Files.writeString(
        graph, "gremlin.graph=org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph\n");
    Path init = dir.resolve("init.groovy");
    Files.writeString(
        init, "def globals = [:]\nglobals << [g : traversal().withEmbedded(graph)]\n");
    String yaml =
        """
        host: localhost
        port: %d
        evaluationTimeout: 30000
        graphs: { graph: %s }
        scriptEngines: {
          gremlin-groovy: {
            plugins: { org.apache.tinkerpop.gremlin.server.jsr223.GremlinServerGremlinPlugin: {},
                       org.apache.tinkerpop.gremlin.tinkergraph.jsr223.TinkerGraphGremlinPlugin: {},
                       org.apache.tinkerpop.gremlin.jsr223.ScriptFileGremlinPlugin: {files: [%s]}}}}
        serializers:
          - { className: org.apache.tinkerpop.gremlin.driver.ser.GraphBinaryMessageSerializerV1,
              config: { ioRegistries: [org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerIoRegistryV1d0] }}
        """
            .formatted(port, graph, init);
    Path settings = dir.resolve("server.yaml");
    Files.writeString(settings, yaml);
    GremlinServer server = new GremlinServer(Settings.read(settings.toString()));
    server.start().join();
    return new TinkerpopTestHelpers(port, server);
  }

  /**
   * The program-level options that point a program at this server, on one thread, so that its
   * commands run in the order they are written.
   */
  String options() {
    return "options tinkerpop_port = " + port + " num_threads = 1\n";
  }

  @Override
  public void close() {
    server.stop().join();
  }
}
