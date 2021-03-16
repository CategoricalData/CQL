package catdata.aql;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;

import catdata.Program;
import catdata.Util;
import catdata.aql.exp.AqlEnv;
import catdata.aql.exp.AqlMultiDriver;
import catdata.aql.exp.AqlParserFactory;
import catdata.aql.exp.Exp;
import catdata.ide.CodeTextPanel;
import catdata.ide.Example;
import catdata.ide.Examples;
import catdata.ide.Language;
import gnu.trove.map.hash.THashMap;

public class AqlTester {

  static String message = "This self-test will run all the built-in CQL examples and check for exceptions.  This test cannot be interupted.  This window will disappear for a while. Continue?";

  public static void doSelfTestGui() {
    deleteFilesCreatedDuring(() -> {
      int c = JOptionPane.showConfirmDialog(null, message, "Run Self-Test?", JOptionPane.YES_NO_OPTION);
      if (c != JOptionPane.YES_OPTION) {
        return null;
      }
      Map<String, Throwable> result = getTestResult();
      if (result.isEmpty()) {
        JOptionPane.showMessageDialog(null, "OK: Tests Passed");
        return null;
      }
      JTabbedPane t = new JTabbedPane();
      for (String k : result.keySet()) {
        t.addTab(k, new CodeTextPanel("Error", result.get(k).getMessage()));
      }
      JOptionPane.showMessageDialog(null, t);
      return null; // ignored
    });
  }

  /**
   * Return a map of test name to error on that test, empty if all tests are successful.
   */
  public static Map<String, Throwable> doSelfTestSilent() {
    return deleteFilesCreatedDuring(() -> {
      // silence output
      PrintStream out = System.out, err = System.err;
      PrintStream nul = new PrintStream(new OutputStream() {
        @Override
        public void write(int arg0) throws IOException {
        }
      });
      try {
        System.setOut(nul);
        System.setErr(nul);

        // run tests
        return getTestResult();
      } finally {
        // restore output
        System.setOut(out);
        System.setErr(nul);
      }
    });
  }

  private static <A> A deleteFilesCreatedDuring(java.util.function.Supplier<A> closure) {
    try {
      // record current files, to later delete created ones
      Set<Path> files = walkPwd();

      A ret = closure.get();

      Set<Path> createdDirs = new java.util.HashSet<>();
      // delete created non-directory files
      for (Path f : Util.diff(walkPwd(), files)) {
        if (Files.isDirectory(f)) {
          createdDirs.add(f);
        } else {
          Files.delete(f);
        }
      }
      // delete now-empty created directories
      for (Path f : createdDirs) {
        Files.delete(f);
      }

      return ret;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static Set<Path> walkPwd() throws IOException {
    return Files.walk(Paths.get("")).collect(java.util.stream.Collectors.toSet());
  }

  private static Map<String, Throwable> getTestResult() {
    Map<String, String> progs = new THashMap<>();
    for (Example e : Examples.getExamples(Language.CQL)) {
      if (e.getName().equals("TutorialTSP") || e.getName().equals("QuickSQL")
          || e.getName().equals("Stdlib") || e.getName().equals("Imports")) {
        continue;
      }
      progs.put(e.getName(), e.getText());
    }
    Map<String, Throwable> result = new THashMap<>();
    for (String k : Util.alphabetical(progs.keySet())) {

      try {
        System.out.println(k);
        Program<Exp<?>> prog = AqlParserFactory.getParser().parseProgram(progs.get(k));
        AqlMultiDriver driver = new AqlMultiDriver(prog, null);
        driver.start(); // blocks
        AqlEnv env = driver.env;
        if (env.exn != null) {
          result.put(k, env.exn);
        }
        // Thread.sleep(3000);
      } catch (Throwable ex) {
        ex.printStackTrace();
        result.put(k, ex);
      }
    }
    return result;
  }

}
