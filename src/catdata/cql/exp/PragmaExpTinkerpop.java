package catdata.cql.exp;

import catdata.Pair;
import catdata.Unit;
import catdata.Util;
import catdata.cql.AqlOptions;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.Kind;
import catdata.cql.Pragma;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.driver.ser.GraphBinaryMessageSerializerV1;
import org.apache.tinkerpop.gremlin.groovy.engine.GremlinExecutor;
import org.apache.tinkerpop.gremlin.jsr223.ConcurrentBindings;
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerIoRegistryV1d0;

public final class PragmaExpTinkerpop extends PragmaExp {
  private final List<String> jss;

  private final Map<String, String> options;

  @Override
  public Map<String, String> options() {
    return options;
  }

  @Override
  public Unit type(AqlTyping G) {
    return Unit.unit;
  }

  public PragmaExpTinkerpop(List<String> jss, List<Pair<String, String>> options) {
    this.options = Util.toMapSafely(options);
    this.jss = jss;
  }

  public <R, P, E extends Exception> R accept(P params, PragmaExpVisitor<R, P, E> v) throws E {
    return v.visit(params, this);
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    result = prime * result + ((options == null) ? 0 : options.hashCode());
    result = prime * result + ((jss == null) ? 0 : jss.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    PragmaExpTinkerpop other = (PragmaExpTinkerpop) obj;

    if (options == null) {
      if (other.options != null) return false;
    } else if (!options.equals(other.options)) return false;
    if (jss == null) {
      if (other.jss != null) return false;
    } else if (!jss.equals(other.jss)) return false;
    return true;
  }

  @Override
  public Pragma eval0(AqlEnv env, boolean isC) {
    if (isC) {
      throw new IgnoreException();
    }
    return new Pragma() {
      String str;

      @Override
      public void execute() {
        if (str != null) {
          return;
        }
        str = "";
        AqlOptions ops = new AqlOptions(options, env.defaults);

        try {
          str += execGremlin(ops, jss, results -> format(results, jss));
          str += "\n";

        } catch (Exception ex) {
          ex.printStackTrace();
          str += "\n\n" + ex.getMessage();
        }
      }

      @Override
      public String toString() {
        return str;
      }
    };
  }

  /**
   * The threads of the pool that each {@link #execGremlin} call gives its {@code GremlinExecutor}.
   * Each evaluation schedules its timeout on that pool and cancels it from another task once the
   * evaluation ends. An executor that owns its pool shuts it down on close and waits for it, and a
   * shutdown that runs before the cancel keeps the timeout queued, so the close waits the whole
   * timeout, 30 s by default. A pool the call owns is not shut down by the executor, so the call
   * drops the queued timeout with {@code shutdownNow}. The threads are daemons, so a pool never
   * keeps the JVM running.
   */
  private static final ThreadFactory EXECUTOR_THREADS =
      new ThreadFactory() {
        private final AtomicInteger count = new AtomicInteger();

        @Override
        public Thread newThread(Runnable r) {
          Thread t = new Thread(r, "cql-gremlin-executor-" + count.incrementAndGet());
          t.setDaemon(true);
          return t;
        }
      };

  /** Reads the results of Gremlin scripts while the connection that produced them is open. */
  @FunctionalInterface
  public interface GremlinReader<X> {
    X read(List<Object> results) throws Exception;
  }

  /**
   * Runs the scripts on the graph that the options name and hands their results to the reader. A
   * result can be a traversal that still reads from the server, so the connection stays open until
   * the reader returns. Then the remote traversal source is closed, and the driver's cluster is
   * closed, which stops its threads. The cluster is configured in memory, not from a file, because
   * Tinkerpop never closes a cluster file it reads, which Windows then cannot delete.
   */
  public static <X> X execGremlin(AqlOptions ops, List<String> jss, GremlinReader<X> reader)
      throws Exception {
    String host = (String) ops.getOrDefault(AqlOption.tinkerpop_host);
    Integer port = (Integer) ops.getOrDefault(AqlOption.tinkerpop_port);
    long timeout = 1000 * (Long) ops.getOrDefault(AqlOption.timeout);
    String gname = (String) ops.getOrDefault(AqlOption.tinkerpop_graph_name);
    GraphBinaryMessageSerializerV1 serializer = new GraphBinaryMessageSerializerV1();
    serializer.configure(
        Map.of("ioRegistries", List.of(TinkerIoRegistryV1d0.class.getName())), null);
    Cluster cluster = Cluster.build(host).port(port).serializer(serializer).create();
    try (GraphTraversalSource source =
        AnonymousTraversalSource.traversal()
            .withRemote(DriverRemoteConnection.using(cluster, gname))) {
      ConcurrentBindings b = new ConcurrentBindings();
      b.putIfAbsent(gname, source);

      List<Object> ret = new ArrayList<>(jss.size());
      ScheduledThreadPoolExecutor pool = new ScheduledThreadPoolExecutor(4, EXECUTOR_THREADS);
      try (GremlinExecutor conn =
          GremlinExecutor.build()
              .evaluationTimeout(timeout)
              .globalBindings(b)
              .executorService(pool)
              .scheduledExecutorService(pool)
              .create()) {
        for (String s : jss) {
          CompletableFuture<Object> evalResult = conn.eval(s);
          Object actualResult = evalResult.get();
          ret.add(actualResult);
        }
      } finally {
        pool.shutdownNow();
      }
      return reader.read(ret);
    } finally {
      cluster.close();
    }
  }

  public static String maybeQuote(String s) {
    return StringEscapeUtils.escapeEcmaScript(s);
  }

  public static String format(List<Object> str0, List<String> jss) throws Exception {
    String str = "";
    for (int i = 0; i < str0.size(); i++) {
      String s = jss.get(i);
      Object o = str0.get(i);
      str += "Exec " + s + ":\n";

      if (o instanceof Iterable) {
        str += "Java class: Iterable\n";
        Iterable<Object> it = (Iterable<Object>) o;
        str += Util.sep(it, "\n");
      } else if (o.getClass().isArray()) {
        str += "Java class: Array\n";
        Object[] oo = (Object[]) o;
        str += Arrays.deepToString(oo);
      } else if (o instanceof GraphTraversal<?, ?>) {
        str += "Java class: GraphTraversal\n";
        GraphTraversal<?, ?> t = (GraphTraversal<?, ?>) o;
        for (Object x : t.toList()) {
          str += (x + "  [ " + x.getClass() + "]\n");
        }
        t.close();
      } else {
        str += "Java class: " + o.getClass() + "\n";
        str += o;
      }
    }
    return str;
  }

  @Override
  protected void allowedOptions(Set<AqlOption> set) {
    set.add(AqlOption.tinkerpop_host);
    set.add(AqlOption.tinkerpop_port);
    set.add(AqlOption.tinkerpop_graph_name);
  }

  @Override
  public String toString() {
    final StringBuilder sb =
        new StringBuilder()
            .append("exec_tinkerpop {")
            .append(Util.sep(jss.stream().map(Util::quote).collect(Collectors.toList()), ""));

    if (!options.isEmpty()) {
      sb.append("\n\toptions").append(Util.sep(options, "\n\t\t", " = "));
    }
    return sb.append("}").toString();
  }

  @Override
  public Collection<Pair<String, Kind>> deps() {
    return Collections.emptyList();
  }

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {}
}
