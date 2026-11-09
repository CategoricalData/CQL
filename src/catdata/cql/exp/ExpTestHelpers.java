package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.ParseException;
import catdata.Program;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.Kind;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** Shared helpers for the tests of {@code catdata.cql.exp}. */
final class ExpTestHelpers {

  private ExpTestHelpers() {}

  /** The options every expression allows, whatever its kind ({@link Exp#allowedOptions()}). */
  static final Set<AqlOption> BASE_OPTIONS =
      Set.of(
          AqlOption.always_reload,
          AqlOption.timeout,
          AqlOption.num_threads,
          AqlOption.talg_reduction);

  /** Parses a program with the parser the IDE uses. */
  static Program<Exp<?>> parse(String program) throws ParseException {
    return AqlParserFactory.getParser().parseProgram(program);
  }

  /** The expression a program binds to a name, as parsed. */
  @SuppressWarnings("unchecked")
  static <E extends Exp<?>> E exp(String program, String name) throws ParseException {
    return (E) parse(program).exps.get(name);
  }

  /** How long a run goes before its watchdog records the threads, before the default timeout. */
  private static final long WATCHDOG_MILLIS = 20_000;

  /** The threads that the watchdog of {@link #run} recorded, for the runs still going after it. */
  private static final Map<AqlMultiDriver, String> SLOW_RUNS =
      Collections.synchronizedMap(new WeakHashMap<>());

  /**
   * Runs a program as the IDE's Run does. A run still going after 20 s gets its threads recorded,
   * because once the 30 s timeout fires it interrupts the thread it stops, which then leaves the
   * call where it was stuck.
   */
  static AqlMultiDriver run(String program) throws ParseException {
    AqlMultiDriver d = new AqlMultiDriver(parse(program), null);
    Thread watchdog =
        new Thread(
            () -> {
              try {
                Thread.sleep(WATCHDOG_MILLIS);
              } catch (InterruptedException e) {
                return;
              }
              SLOW_RUNS.put(d, threads());
            },
            "test-watchdog");
    watchdog.setDaemon(true);
    watchdog.start();
    try {
      d.start();
    } finally {
      watchdog.interrupt();
    }
    return d;
  }

  /** The value a run bound to a name, after asserting the run had no errors. */
  @SuppressWarnings("unchecked")
  static <X> X value(AqlMultiDriver d, String name, Kind kind) {
    assertTrue(d.exn.isEmpty(), () -> d.exn + threadsOfATimeout(d));
    return (X) d.env.defs.get(name, kind);
  }

  /**
   * After a run that timed out, the threads that its watchdog recorded 20 s into the run, or the
   * threads now if it recorded none, so that the failure shows where the run was stuck. Empty after
   * any other run.
   */
  private static String threadsOfATimeout(AqlMultiDriver d) {
    if (!d.exn.toString().contains("Timeout after")) {
      return "";
    }
    String recorded = SLOW_RUNS.get(d);
    return recorded != null
        ? "\nThreads 20 s into the run:\n" + recorded
        : "\nThreads at the timeout:\n" + threads();
  }

  /** The stacks of the threads that run CQL, TinkerPop or Netty code, other than this one. */
  private static String threads() {
    StringBuilder sb = new StringBuilder();
    for (ThreadInfo t : ManagementFactory.getThreadMXBean().dumpAllThreads(true, true)) {
      if (t.getThreadId() == Thread.currentThread().getId()) {
        continue;
      }
      StackTraceElement[] stack = t.getStackTrace();
      boolean relevant =
          Arrays.stream(stack)
              .map(StackTraceElement::getClassName)
              .anyMatch(
                  c -> c.startsWith("catdata.") || c.contains("tinkerpop") || c.contains("netty"));
      if (!relevant) {
        continue;
      }
      sb.append('"').append(t.getThreadName()).append("\" ").append(t.getThreadState());
      if (t.getLockName() != null) {
        sb.append(" on ").append(t.getLockName());
      }
      if (t.getLockOwnerName() != null) {
        sb.append(" owned by ").append(t.getLockOwnerName());
      }
      sb.append('\n');
      for (int i = 0; i < Math.min(stack.length, 40); i++) {
        sb.append("    at ").append(stack[i]).append('\n');
      }
    }
    return sb.toString();
  }

  /** Runs a program and returns the value bound to a name, asserting the run had no errors. */
  static <X> X eval(String program, String name, Kind kind) throws ParseException {
    return value(run(program), name, kind);
  }

  /**
   * The message of the error the IDE reports for a run: the driver's {@code env.exn}, which skips
   * the interruptions of the definitions the driver stopped after the first failure.
   */
  static String error(AqlMultiDriver d) {
    assertNotNull(d.env.exn, "the run succeeded");
    return d.env.exn.getMessage();
  }

  /** Runs a program and returns the message of its only error. */
  static String error(String program) throws ParseException {
    return error(run(program));
  }

  private static int databases = 0;

  /**
   * A fresh in-memory H2 database with the given statements run. It lives until the JVM exits, so a
   * program can connect to its URL after the setup connection closes.
   */
  static synchronized String h2(String... statements) throws SQLException {
    String url = "jdbc:h2:mem:exp_test_" + databases++ + ";DB_CLOSE_DELAY=-1";
    try (Connection conn = DriverManager.getConnection(url);
        Statement stmt = conn.createStatement()) {
      for (String s : statements) {
        stmt.execute(s);
      }
    }
    return url;
  }

  /** The typing of a program, as the IDE computes it before a run. */
  static AqlTyping typing(String program) throws ParseException {
    return new AqlTyping(parse(program), false);
  }
}
