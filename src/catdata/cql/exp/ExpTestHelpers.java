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
import java.util.Set;

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

  /** Runs a program as the IDE's Run does. */
  static AqlMultiDriver run(String program) throws ParseException {
    AqlMultiDriver d = new AqlMultiDriver(parse(program), null);
    d.start();
    return d;
  }

  /** The value a run bound to a name, after asserting the run had no errors. */
  @SuppressWarnings("unchecked")
  static <X> X value(AqlMultiDriver d, String name, Kind kind) {
    assertTrue(d.exn.isEmpty(), () -> d.exn + threadsAtTimeout(d));
    return (X) d.env.defs.get(name, kind);
  }

  /**
   * After a run that timed out, the stacks of the threads that run CQL, TinkerPop or Netty code, so
   * that the failure shows where the run was stuck. Empty after any other run.
   */
  private static String threadsAtTimeout(AqlMultiDriver d) {
    if (!d.exn.toString().contains("Timeout after")) {
      return "";
    }
    StringBuilder sb = new StringBuilder("\nThreads at the timeout:\n");
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
