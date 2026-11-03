package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import catdata.cql.AqlTester;
import catdata.ide.Example;
import catdata.ide.Examples;
import catdata.ide.Language;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Unit tests for the standalone examples-site generator {@link AqlExamples}. */
class AqlExamplesTest {

  /** A minimal in-memory {@link Example} so rendering can be tested without disk or execution. */
  private static Example example(String name, String text) {
    return new Example() {
      @Override
      public String getName() {
        return name;
      }

      @Override
      public String getText() {
        return text;
      }
    };
  }

  @Test
  void renderIndexListsEveryExampleAlphabetically() {
    List<Example> examples = new ArrayList<>();
    examples.add(example("Bravo", "schema B = literal : ty {}"));
    examples.add(example("Alpha", "schema A = literal : ty {}"));

    String html = AqlExamples.renderIndex(examples);

    assertTrue(html.contains("href=\"Alpha.html\""), "index should link to Alpha");
    assertTrue(html.contains("href=\"Bravo.html\""), "index should link to Bravo");
    assertTrue(
        html.indexOf("Alpha.html") < html.indexOf("Bravo.html"),
        "examples should be listed alphabetically");
  }

  @Test
  void renderExamplePageShowsStrippedSourceAndTitle() {
    // The '<' must be HTML-escaped by AqlInACan.strip so it renders literally in the <pre> block.
    Example ex = example("Demo", "-- a < b\nschema S = literal : ty {}");

    String html = AqlExamples.renderExamplePage(ex, false);

    assertTrue(html.contains("<h1>example Demo</h1>"), "page should carry the example title");
    assertTrue(html.contains("a &lt; b"), "source angle brackets should be escaped");
    assertTrue(html.contains("href=\"index.html\""), "page should link back to the index");
  }

  @Test
  void examplesToDocumentExcludesNonRunnableExamples() {
    Collection<Example> docs = AqlExamples.examplesToDocument();

    assertFalse(docs.isEmpty(), "there should be runnable examples to document");
    for (Example ex : docs) {
      assertFalse(
          AqlTester.NON_RUNNABLE.contains(ex.getName()),
          "skipped example should not be documented: " + ex.getName());
    }
  }

  @Test
  @Tag("integration")
  void runningARealExampleRendersItsExecutedOutput() {
    Example employees = findExample("Employees");
    String html = AqlExamples.renderExamplePage(employees, true);
    assertTrue(
        html.contains("<h3>instance "), "executed output should include at least one instance");
  }

  private static Example findExample(String name) {
    for (Example e : Examples.getExamples(Language.CQL)) {
      if (e.getName().equals(name)) {
        return e;
      }
    }
    fail("expected built-in example not found: " + name);
    throw new AssertionError("unreachable");
  }
}
