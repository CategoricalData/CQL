package catdata.cql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * The self-test helpers. deleteFilesCreatedDuring compares the working directory before and after,
 * which is the project directory under Gradle, so these tests work in a fresh directory under
 * build/tmp and remove it afterwards. doSelfTestGui shows dialogs and is not tested;
 * doSelfTestSilent runs every example and is exercised by CqlExamplesTest.
 */
class AqlTesterTest {

  /** Fixed, so that the hash order of the paths below it is the same on every run. */
  private final Path base = Path.of("build", "tmp", "aqltester");

  @BeforeEach
  void makeBase() throws IOException {
    removeBase();
    Files.createDirectories(base);
  }

  @AfterEach
  void removeBase() throws IOException {
    if (!Files.exists(base)) {
      return;
    }
    try (Stream<Path> s = Files.walk(base)) {
      s.forEach(p -> p.toFile().setWritable(true));
    }
    try (Stream<Path> s = Files.walk(base)) {
      s.sorted(Comparator.reverseOrder())
          .forEach(
              p -> {
                try {
                  Files.delete(p);
                } catch (IOException e) {
                  throw new UncheckedIOException(e);
                }
              });
    }
  }

  private static void write(Path p) {
    try {
      Files.writeString(p, "x");
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static void mkdir(Path p) {
    try {
      Files.createDirectory(p);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Test
  void isAClassOfStaticMethods() {
    assertEquals(AqlTester.class, new AqlTester().getClass());
  }

  @Test
  void skipsTheExamplesThatAreNotStandalonePrograms() {
    assertEquals(Set.of("TutorialTSP", "QuickSQL", "Stdlib", "Imports"), AqlTester.NON_RUNNABLE);
  }

  @Nested
  class DeleteFilesCreatedDuring {

    @Test
    void deletesNewFilesAndADirectoryAndKeepsTheOldOnes() throws IOException {
      Path old = base.resolve("old.txt");
      write(old);
      Path dir = base.resolve("new");
      Path file = dir.resolve("f.txt");
      String r =
          AqlTester.deleteFilesCreatedDuring(
              () -> {
                write(base.resolve("top.txt"));
                mkdir(dir);
                write(file);
                return "done";
              });
      assertEquals("done", r);
      assertTrue(Files.exists(old));
      assertFalse(Files.exists(base.resolve("top.txt")));
      assertFalse(Files.exists(dir));
    }

    /**
     * Issue 65: new directories are deleted in hash order, so a parent can come before its child.
     * With 20 pairs at fixed paths, some parent always does.
     */
    @Test
    void failsOnNestedNewDirectories() {
      RuntimeException e =
          assertThrows(
              RuntimeException.class,
              () ->
                  AqlTester.deleteFilesCreatedDuring(
                      () -> {
                        for (int k = 0; k < 20; k++) {
                          Path outer = base.resolve("outer" + k);
                          mkdir(outer);
                          mkdir(outer.resolve("inner"));
                        }
                        return null;
                      }));
      assertInstanceOf(DirectoryNotEmptyException.class, e.getCause());
    }

    @Test
    void wrapsAFileItCannotDelete() {
      Path locked = base.resolve("locked");
      RuntimeException e =
          assertThrows(
              RuntimeException.class,
              () ->
                  AqlTester.deleteFilesCreatedDuring(
                      () -> {
                        mkdir(locked);
                        write(locked.resolve("f.txt"));
                        locked.toFile().setWritable(false);
                        assumeFalse(Files.isWritable(locked), "running with rights to ignore it");
                        return null;
                      }));
      assertInstanceOf(IOException.class, e.getCause());
    }
  }
}
