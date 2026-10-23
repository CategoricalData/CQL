package catdata.cql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** A command's kind and size, on a command that prints a fixed text. */
class PragmaTest {

  @Test
  void isACommandSizedByItsText() {
    Pragma p =
        new Pragma() {
          @Override
          public void execute() {}

          @Override
          public String toString() {
            return "exported 3 rows";
          }
        };
    assertEquals(Kind.PRAGMA, p.kind());
    assertEquals(15, p.size());
  }
}
