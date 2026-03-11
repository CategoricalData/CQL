package catdata.cql;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import catdata.Program;
import catdata.cql.exp.AqlParserFactory;
import catdata.cql.exp.Exp;
import catdata.ide.Example;
import catdata.ide.Examples;
import catdata.ide.Language;

/**
 * JUnit 5 wrapper around the existing CQL self-test.
 * Runs all built-in CQL examples and verifies they execute without exceptions.
 */
class CqlExamplesTest {

    /** Names to skip (require external resources or are too slow). */
    private static final Set<String> SKIP = Set.of(
        "TutorialTSP", "QuickSQL", "Stdlib", "Imports"
    );

    @Test
    @Tag("integration")
    void allBuiltInExamplesShouldPass() {
        Map<String, Throwable> failures = AqlTester.deleteFilesCreatedDuring(() -> {
            return AqlTester.doSelfTestSilent();
        });

        if (!failures.isEmpty()) {
            StringBuilder sb = new StringBuilder("CQL example failures:\n");
            for (Map.Entry<String, Throwable> entry : failures.entrySet()) {
                sb.append("  - ").append(entry.getKey())
                  .append(": ").append(entry.getValue().getMessage())
                  .append("\n");
            }
            fail(sb.toString());
        }
    }

    @Test
    void parserShouldLoadAllExamples() {
        for (Example e : Examples.getExamples(Language.CQL)) {
            if (SKIP.contains(e.getName())) continue;

            try {
                Program<Exp<?>> prog = AqlParserFactory.getParser().parseProgram(e.getText());
                assertTrue(prog.exps.size() > 0,
                    "Example '" + e.getName() + "' should have at least one expression");
            } catch (Exception ex) {
                fail("Failed to parse example '" + e.getName() + "': " + ex.getMessage());
            }
        }
    }
}
