package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ProcPragmaTest {

  /**
   * The output of each command is read on two threads that may still be appending when execute
   * returns, and the executor may close the streams before they are read. The result is read once
   * both streams of every command have reported, or after five seconds; a reading that collides
   * with an append is retried.
   */
  private static String result(ProcPragma pragma, List<String> cmds) throws InterruptedException {
    String s = "";
    for (int i = 0; i < 500; i++) {
      try {
        s = pragma.toString();
      } catch (ConcurrentModificationException e) {
        continue;
      }
      String done = s;
      if (cmds.stream()
          .allMatch(c -> done.contains(c + " stdout: ") && done.contains(c + " stderr: "))) {
        return s;
      }
      Thread.sleep(10);
    }
    return s;
  }

  @Nested
  class Constructor {

    @Test
    void constructorSetsFields() {
      var pragma = new ProcPragma(Collections.emptyList());
      assertNotNull(pragma);
    }
  }

  @Nested
  class ToStringMethod {

    @Test
    void toStringBeforeExecute() {
      var pragma = new ProcPragma(Collections.emptyList());
      assertNotNull(pragma.toString());
    }

    @Test
    void isEmptyWithoutCommands() {
      var pragma = new ProcPragma(Collections.emptyList());
      pragma.execute();
      assertEquals("", pragma.toString());
    }
  }

  @Nested
  class Execute {

    /**
     * Whether the text of the output survives depends on the timing, so only the headers are
     * checked.
     */
    @Test
    void reportsBothStreamsOfEachCommandInOrder() throws Exception {
      List<String> cmds = List.of("echo one", "echo two");
      var pragma = new ProcPragma(cmds);
      pragma.execute();
      String s = result(pragma, cmds);
      for (String c : cmds) {
        assertTrue(s.contains(c + " stdout: "), s);
        assertTrue(s.contains(c + " stderr: "), s);
      }
    }

    @Test
    void failsOnACommandThatExitsWithAnError() {
      var pragma = new ProcPragma(List.of("false"));
      RuntimeException e = assertThrows(RuntimeException.class, pragma::execute);
      assertTrue(e.getMessage().contains("Process exited with an error: 1"), e.getMessage());
    }

    @Test
    void failsOnACommandThatDoesNotExist() {
      var pragma = new ProcPragma(List.of("no-such-command-for-cql-tests"));
      assertThrows(RuntimeException.class, pragma::execute);
    }
  }
}
