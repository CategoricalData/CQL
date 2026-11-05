package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.Unit;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit tests for the CQL HTML manual generator {@link AqlHelp}. */
class AqlHelpTest {

  /** Matches the target of every {@code href="..."} / {@code src="..."} attribute. */
  private static final Pattern LINK = Pattern.compile("(?:href|src)=\"([^\"]+)\"");

  /**
   * Regenerating the manual must not leave any local {@code href}/{@code src} pointing at a page
   * that was never written — those are the 404s tracked in issue #110. External links ({@code
   * http...}), parent-relative assets ({@code ../css/...}), empty targets, and pure anchors are out
   * of scope and skipped.
   */
  @Test
  @Tag("integration")
  void generatedManualHasNoBrokenLocalLinks(@TempDir Path dir) throws IOException {
    // run = false: render the pages without executing examples, which is all link-checking needs.
    AqlHelp.help(dir.toFile(), false);

    List<String> generated = new ArrayList<>();
    for (File f : dir.toFile().listFiles()) {
      generated.add(f.getName());
    }

    List<String> broken = new ArrayList<>();
    for (File f : dir.toFile().listFiles()) {
      if (!f.getName().endsWith(".html")) {
        continue;
      }
      String html = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
      Matcher m = LINK.matcher(html);
      while (m.find()) {
        String target = m.group(1);
        if (target.isEmpty()
            || target.startsWith("http")
            || target.startsWith("..")
            || target.startsWith("#")) {
          continue;
        }
        String base = target.split("#")[0].split("\\?")[0];
        if (!generated.contains(base)) {
          broken.add(f.getName() + " -> " + target);
        }
      }
    }

    assertTrue(broken.isEmpty(), "broken local links in generated manual: " + broken);
  }

  @Test
  @Tag("integration")
  void createsAMissingDirectory(@TempDir Path dir) {
    File manual = new File(dir.toFile(), "manual");
    AqlHelp.help(manual, false);
    assertTrue(new File(manual, "index.html").isFile());
  }

  @Test
  void failsWhenThePagesCannotBeWritten(@TempDir Path dir) throws IOException {
    File file = Files.writeString(dir.resolve("manual"), "").toFile();
    assertThrows(RuntimeException.class, () -> AqlHelp.help(file, false));
  }

  /** The report prints every sample expression of the typing inversion, one per paragraph. */
  @Test
  void reportsEverySampleExpression() {
    long samples =
        new TypingInversion()
            .covisit(Unit.unit, x -> new AqlTyping()).values().stream()
                .flatMap(Set::stream)
                .filter(p -> p.second != null)
                .count();
    String report = new AqlHelp().report();
    assertEquals(samples, report.split("----------------------------------\n", -1).length - 1);
    assertTrue(report.endsWith("\n\n"), report);
  }
}
