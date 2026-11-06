package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.cql.AqlOptions;
import catdata.cql.Transform;
import catdata.cql.exp.AqlMultiDriver;
import catdata.cql.exp.AqlParserFactory;
import catdata.cql.exp.Att;
import catdata.cql.exp.Fk;
import catdata.cql.exp.Sym;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ToCsvPragmaTransformTest {

  @Nested
  class GetFormat {

    @Test
    void getFormatReturnsNonNull() {
      var format = ToCsvPragmaInstance.getFormat(AqlOptions.initialOptions);
      assertNotNull(format);
    }

    @Test
    void getFormatHasDelimiterString() {
      var format = ToCsvPragmaInstance.getFormat(AqlOptions.initialOptions);
      assertNotNull(format.getDelimiterString());
    }

    @Test
    void getFormatHasQuoteChar() {
      var format = ToCsvPragmaInstance.getFormat(AqlOptions.initialOptions);
      assertNotNull(format.getQuoteCharacter());
    }
  }

  /** Exports of the identity transform of an instance with the rows a1 and a2. */
  @Nested
  class Execute {

    @TempDir Path dir;

    @SuppressWarnings("unchecked")
    private ToCsvPragmaTransform<
            String,
            String,
            Sym,
            Fk,
            Att,
            String,
            String,
            String,
            String,
            Object,
            Object,
            Object,
            Object>
        export(Path target) throws Exception {
      AqlMultiDriver d =
          new AqlMultiDriver(
              AqlParserFactory.getParser()
                  .parseProgram(
                      """
                      typeside Ty = literal { types String }
                      schema S = literal : Ty { entities A }
                      instance I = literal : S { generators a1 a2 : A }
                      transform t = identity I
                      """),
              null);
      d.start();
      var t =
          (Transform<
                  String,
                  String,
                  Sym,
                  Fk,
                  Att,
                  String,
                  String,
                  String,
                  String,
                  Object,
                  Object,
                  Object,
                  Object>)
              d.env.defs.trans.get("t");
      return new ToCsvPragmaTransform<>(
          t, target.toString(), AqlOptions.initialOptions, AqlOptions.initialOptions);
    }

    @Test
    void replacesAnExistingFileWithARowPerPair() throws Exception {
      Path file = Files.writeString(dir.resolve("t.csv"), "old");
      var p = export(file);
      p.execute();
      assertEquals(List.of("0,0", "1,1"), Files.readAllLines(file));
      assertEquals("Exported to " + file + ".", p.toString());
    }

    @Test
    void refusesADirectory() throws Exception {
      var p = export(dir);
      RuntimeException e = assertThrows(RuntimeException.class, p::execute);
      assertEquals("Cannot delete directory: " + dir, e.getMessage());
    }

    @Test
    void failsInAMissingDirectory() throws Exception {
      var p = export(dir.resolve("missing").resolve("t.csv"));
      RuntimeException e = assertThrows(RuntimeException.class, p::execute);
      assertInstanceOf(IOException.class, e.getCause());
      assertTrue(Files.notExists(dir.resolve("missing")));
    }
  }
}
