package catdata.cql.exp;

import catdata.Program;
import catdata.Util;
import catdata.cql.AqlTester;
import catdata.cql.Instance;
import catdata.cql.Pragma;
import catdata.ide.Example;
import catdata.ide.Examples;
import catdata.ide.Language;
import java.io.File;
import java.util.Collection;

/**
 * Generates a standalone, self-contained <code>examples/</code> website from the CQL self-tester.
 *
 * <p>For every built-in CQL example that the self-test runs (see {@link AqlTester#NON_RUNNABLE} for
 * the ones it skips), this writes one HTML page showing the example's source and its executed
 * output (instances rendered as tables, command results). An <code>index.html</code> links them
 * together. Because the pages are produced by parsing and running the examples on every build, they
 * are always in sync with the language — this is the auto-generated counterpart to {@code
 * AqlHelp}'s manual, but free of the help frameset so the top-level website can link straight into
 * any page.
 *
 * <p>Run via the Gradle <code>generateExamples</code> task, which supplies headless mode and the
 * {@code --add-opens} flag (the examples may touch AWT through {@code AqlMultiDriver}).
 */
public final class AqlExamples {

  private AqlExamples() {}

  /** Self-contained page styling; inlined so every page renders standalone with no external CSS. */
  static final String CSS =
      "<style>"
          + "body{font-family:sans-serif;margin:2em;line-height:1.4;color:#222}"
          + "h1{border-bottom:1px solid #ccc;padding-bottom:.3em}"
          + "pre{background:#f5f5f5;border:1px solid #ddd;padding:1em;overflow:auto;"
          + "white-space:pre-wrap}"
          + "table{border-collapse:collapse}"
          + "a{color:#0645ad;text-decoration:none}a:hover{text-decoration:underline}"
          + "</style>";

  /**
   * The CQL examples to document: every built-in example the self-test runs, i.e. all of them minus
   * {@link AqlTester#NON_RUNNABLE}.
   */
  public static Collection<Example> examplesToDocument() {
    java.util.List<Example> out = new java.util.ArrayList<>();
    for (Example e : Examples.getExamples(Language.CQL)) {
      if (!AqlTester.NON_RUNNABLE.contains(e.getName())) {
        out.add(e);
      }
    }
    return out;
  }

  /** Builds the {@code index.html} listing every documented example alphabetically. */
  public static String renderIndex(Collection<Example> examples) {
    StringBuilder sb = new StringBuilder();
    sb.append("<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>CQL Examples</title>")
        .append(CSS)
        .append("</head><body>");
    sb.append("<h1>CQL Examples</h1>");
    sb.append(
        "<p>These examples are generated automatically from the CQL self-tester. Each one is"
            + " parsed and executed on every build, so the source and output shown are always"
            + " current. Source: <a href=\"https://github.com/CategoricalData/CQL\""
            + " target=\"_blank\">CategoricalData/CQL</a>.</p>");
    sb.append("<ul>");
    for (Example ex : Util.alphabetical(examples)) {
      String name = ex.getName().trim();
      sb.append("<li><a href=\"").append(name).append(".html\">").append(name).append("</a></li>");
    }
    sb.append("</ul></body></html>");
    return sb.toString();
  }

  /**
   * Builds the page for a single example: its source, and (when {@code run}) its executed output.
   * Splitting {@code run} out keeps the page renderable without executing the program, which the
   * unit tests rely on.
   */
  public static String renderExamplePage(Example ex, boolean run) {
    String name = ex.getName().trim();
    StringBuilder sb = new StringBuilder();
    sb.append("<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>")
        .append(name)
        .append(" — CQL Example</title>")
        .append(CSS)
        .append("</head><body>");
    sb.append("<p><a href=\"index.html\">← All examples</a></p>");
    sb.append("<h1>example ").append(name).append("</h1>");
    sb.append("<pre>\n").append(AqlInACan.strip(ex.getText().trim())).append("\n</pre>");
    if (run) {
      sb.append(renderExecutedOutput(ex));
    }
    sb.append("</body></html>");
    return sb.toString();
  }

  /**
   * Parses and runs the example, rendering its resulting instances (as HTML tables) and commands.
   * Any parse/run failure is rendered inline rather than thrown, so one bad example cannot abort
   * generation of the whole site.
   */
  @SuppressWarnings("unchecked")
  static String renderExecutedOutput(Example ex) {
    StringBuilder sb = new StringBuilder();
    try {
      Program<Exp<?>> prog = AqlParserFactory.getParser().parseProgram(ex.getText());
      AqlMultiDriver dr = new AqlMultiDriver(prog, null);
      dr.start(); // blocks until finished
      if (dr.env.exn != null) {
        return errorSection(dr.env.exn.getMessage());
      }
      for (String k : Util.alphabetical(dr.env.defs.insts.keySet())) {
        Instance<String, String, Sym, Fk, Att, String, String, Object, Object> i =
            (Instance<String, String, Sym, Fk, Att, String, String, Object, Object>)
                dr.env.defs.insts.get(k);
        sb.append("<hr/><h3>instance ").append(k).append("</h3>");
        sb.append(AqlInACan.toHtml(dr.env, i));
      }
      for (String k : Util.alphabetical(dr.env.defs.ps.keySet())) {
        Pragma p = dr.env.defs.ps.get(k);
        sb.append("<hr/><h3>command ").append(k).append("</h3><pre>");
        sb.append(AqlInACan.strip(p.toString())).append("</pre>");
      }
    } catch (Exception e) {
      return errorSection(e.getMessage());
    }
    return sb.toString();
  }

  private static String errorSection(String message) {
    return "<hr/><h3>error</h3><pre>" + AqlInACan.strip(String.valueOf(message)) + "</pre>";
  }

  /** Writes {@code index.html} plus one page per example into {@code dir}. */
  public static void generate(File dir, boolean run) throws java.io.IOException {
    if (!dir.exists()) {
      dir.mkdir();
    }
    Collection<Example> examples = examplesToDocument();
    for (Example ex : examples) {
      System.out.println(ex.getName());
      Util.writeFile(
          renderExamplePage(ex, run),
          new File(dir, ex.getName().trim() + ".html").getAbsolutePath());
    }
    Util.writeFile(renderIndex(examples), new File(dir, "index.html").getAbsolutePath());
  }

  /**
   * Generates the manual into a temp dir (wrapped so examples that write files on disk are cleaned
   * up), then copies it in as the top-level {@code examples/} directory. Exits with status 1 if
   * either step fails.
   */
  public static void main(String[] args) {
    try {
      File tmp = java.nio.file.Files.createTempDirectory("aqlexamples").toFile();
      AqlTester.deleteFilesCreatedDuring(
          () -> {
            try {
              generate(tmp, true);
            } catch (java.io.IOException e) {
              throw new RuntimeException(e);
            }
            return null; // not used
          });
      AqlTester.replaceDirectory(tmp.toPath(), new File("examples").toPath());
    } catch (java.io.IOException | RuntimeException e) {
      // exit here too, so the non-daemon threads cannot keep a failed run alive
      e.printStackTrace();
      System.exit(1);
    }
    System.exit(0); // slay daemons (AWT/netty non-daemon threads otherwise hang the JVM)
  }
}
