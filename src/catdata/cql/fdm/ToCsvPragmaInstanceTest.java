package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.cql.AqlOptions;
import catdata.cql.Instance;
import catdata.cql.exp.AqlMultiDriver;
import catdata.cql.exp.AqlParserFactory;
import catdata.cql.exp.Att;
import catdata.cql.exp.Fk;
import catdata.cql.exp.Sym;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ToCsvPragmaInstanceTest {

  @TempDir Path dir;

  /** A has two rows, one with a value and one whose n is a labelled null; B has none. */
  private static final String PROGRAM =
      """
      typeside Ty = sql
      schema S = literal : Ty { entities A B attributes n : A -> Varchar }
      instance I = literal : S { generators a b : A equations a.n = hello }
      """;

  @SuppressWarnings("unchecked")
  private static Instance<String, String, Sym, Fk, Att, String, String, Object, Object> instance()
      throws Exception {
    AqlMultiDriver d = new AqlMultiDriver(AqlParserFactory.getParser().parseProgram(PROGRAM), null);
    d.start();
    return (Instance<String, String, Sym, Fk, Att, String, String, Object, Object>)
        d.env.defs.insts.get("I");
  }

  private ToCsvPragmaInstance<String, String, Sym, Fk, Att, String, String, Object, Object> export(
      Path target, Map<String, String> options) throws Exception {
    return new ToCsvPragmaInstance<>(
        instance(), target.toString(), new AqlOptions(options, AqlOptions.initialOptions));
  }

  private static String read(Path file) throws Exception {
    return Files.readString(file, StandardCharsets.UTF_8);
  }

  @Nested
  class GetFormatMethod {

    @Test
    void getFormatReturnsNonNull() {
      var format = ToCsvPragmaInstance.getFormat(AqlOptions.initialOptions);
      assertNotNull(format);
    }
  }

  @Nested
  class Execute {

    @Test
    void writesAFileWithAHeaderForEachEntityWithRows() throws Exception {
      var p = export(dir, Map.of());
      p.execute();
      String a = read(dir.resolve("A.csv"));
      assertTrue(a.startsWith("id,n"), a);
      assertTrue(a.contains(",hello"), a);
      assertEquals(3, a.lines().count());
      assertEquals("Export to " + dir + "/.", p.toString());
    }

    /** The help promises one file per entity, but an entity without rows gets none. */
    @Test
    void writesNoFileForAnEntityWithoutRows() throws Exception {
      export(dir, Map.of()).execute();
      assertFalse(Files.exists(dir.resolve("B.csv")));
    }

    /** Every file already in the target directory is deleted first. */
    @Test
    void deletesTheOtherFilesOfTheDirectory() throws Exception {
      Files.writeString(dir.resolve("keep.txt"), "important");
      export(dir, Map.of()).execute();
      assertFalse(Files.exists(dir.resolve("keep.txt")));
    }

    /** A subdirectory stops the export, after some of the files have been deleted. */
    @Test
    void failsOnADirectoryWithASubdirectory() throws Exception {
      Files.createDirectory(dir.resolve("sub"));
      var p = export(dir, Map.of());
      RuntimeException e = assertThrows(RuntimeException.class, p::execute);
      assertTrue(e.getMessage().startsWith("Cannot delete directory: "), e.getMessage());
      assertFalse(Files.exists(dir.resolve("A.csv")));
    }

    @Test
    void createsAMissingDirectoryAndReplacesAFile() throws Exception {
      Path missing = dir.resolve("new");
      export(missing, Map.of()).execute();
      assertTrue(Files.exists(missing.resolve("A.csv")));
      Path file = Files.writeString(dir.resolve("plain"), "x");
      export(file, Map.of()).execute();
      assertTrue(Files.isDirectory(file));
      assertTrue(Files.exists(file.resolve("A.csv")));
    }

    @Test
    void appliesTheOptionsOfTheFile() throws Exception {
      Map<String, String> options = new HashMap<>();
      options.put("emit_ids", "false");
      options.put("csv_file_extension", "txt");
      options.put("csv_utf8_bom", "true");
      options.put("emit_nulls", "true");
      options.put("csv_row_sort_order", "A.n");
      export(dir, options).execute();
      String a = read(dir.resolve("A.txt"));
      assertTrue(a.startsWith("﻿n"), a);
      assertTrue(a.contains("?0"), a);
      assertTrue(a.contains("hello"), a);
    }
  }
}
