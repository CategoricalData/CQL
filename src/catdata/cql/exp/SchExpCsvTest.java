package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.LineException;
import catdata.Pair;
import catdata.Unit;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.Kind;
import catdata.cql.Schema;
import catdata.cql.exp.SchExp.SchExpVisitor;
import catdata.cql.exp.TyExp.TyExpVar;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SchExpCsv}, {@code schema S = import_csv "folder" : T}, which declares an
 * entity per file of the folder and a String attribute per column of its header.
 */
class SchExpCsvTest {

  /** A fresh folder with the given files, each a name and its contents. */
  private static Path folder(String... files) throws Exception {
    Path dir = Files.createTempDirectory("schexpcsv");
    for (int i = 0; i < files.length; i += 2) {
      Files.writeString(dir.resolve(files[i]), files[i + 1]);
    }
    return dir;
  }

  private static String program(Object dir, String options) {
    return "typeside Ty = sql\nschema S = import_csv \"" + dir + "\" : Ty " + options + "\n";
  }

  private static Schema<String, String, Sym, Fk, Att> schema(Object dir, String options)
      throws Exception {
    return ExpTestHelpers.eval(program(dir, options), "S", Kind.SCHEMA);
  }

  /** The attributes of a schema, as entity.attribute to type. */
  private static Map<String, String> atts(Schema<String, String, Sym, Fk, Att> s) {
    Map<String, String> ret = new TreeMap<>();
    s.atts.forEach((a, p) -> ret.put(a.en + "." + a.str, p.second));
    return ret;
  }

  @Nested
  class Evaluation {

    @Test
    void declaresAnEntityPerFileAndAnAttributePerColumn() throws Exception {
      var s = schema(folder("People.csv", "id,name\n1,a\n", "Dept.csv", "code\n"), "");
      assertEquals(Set.of("People", "Dept"), s.ens);
      assertEquals(
          Map.of("Dept.code", "String", "People.id", "String", "People.name", "String"), atts(s));
      assertTrue(s.fks.isEmpty());
    }

    @Test
    void readsTheOptions() throws Exception {
      var s =
          ExpTestHelpers.<Schema<String, String, Sym, Fk, Att>>eval(
              "options csv_prepend_entity = true\n"
                  + program(
                      folder("People.txt", "id;name\n"),
                      "{ options csv_field_delim_char = \";\" csv_file_extension = \"txt\" }"),
              "S",
              Kind.SCHEMA);
      assertEquals(Set.of("People"), s.ens);
      assertEquals(Map.of("People.People_id", "String", "People.People_name", "String"), atts(s));
    }

    /** csv_prepend_entity is read but not allowed in the expression's own options. */
    @Test
    void doesNotAllowPrependingTheEntityLocally() throws Exception {
      String program =
          program(folder("People.csv", "id\n"), "{ options csv_prepend_entity = true }");
      LineException e = assertThrows(LineException.class, () -> ExpTestHelpers.run(program));
      assertTrue(
          e.getMessage().startsWith("Option not allowed: csv_prepend_entity."), e.getMessage());
    }

    @Test
    void cleansColumnNames() throws Exception {
      var s = schema(folder("People.csv", "﻿id, a\tb ,,name\n"), "");
      assertEquals(
          Map.of("People.id", "String", "People.a b", "String", "People.name", "String"), atts(s));
    }

    @Test
    void skipsDsStore() throws Exception {
      var s = schema(folder("People.csv", "id\n", ".DS_Store", "binary"), "");
      assertEquals(Set.of("People"), s.ens);
    }

    /** The extension is only stripped, never filtered on, so any other file is an entity too. */
    @Test
    void makesAnEntityOfAFileWithAnotherExtension() throws Exception {
      var s = schema(folder("People.csv", "id\n", "README.txt", "notes\n"), "");
      assertEquals(Set.of("People", "README.txt"), s.ens);
      assertEquals("String", atts(s).get("README.txt.notes"));
    }

    /** Hidden files are tested by their full path, so they are read like any other file. */
    @Test
    void readsAHiddenFile() throws Exception {
      Path dir = folder("People.csv", "id\n", ".gitkeep", "");
      String msg = ExpTestHelpers.error(program(dir, ""));
      assertTrue(msg.contains("No header in: " + dir.resolve(".gitkeep")), msg);
    }

    /** Every path below a folder given as ./ or ../ starts with a dot, so every file is skipped. */
    @Test
    void readsNothingFromARelativePathStartingWithADot() throws Exception {
      Path dir = folder("People.csv", "id\n");
      Path relative = Path.of("").toAbsolutePath().relativize(dir);
      assertTrue(relative.toString().startsWith(".."), relative.toString());
      assertTrue(schema(relative, "").ens.isEmpty());
    }

    @Test
    void rejectsAFolderThatCannotBeRead() throws Exception {
      Path dir = folder("People.csv", "id\n");
      assertTrue(
          ExpTestHelpers.error(program(dir.resolve("missing"), ""))
              .contains("Does not exist: " + dir.resolve("missing")));
      assertTrue(
          ExpTestHelpers.error(program(dir.resolve("People.csv"), ""))
              .contains("Not a directory: " + dir.resolve("People.csv")));
      Files.createDirectory(dir.resolve("sub"));
      String msg = ExpTestHelpers.error(program(dir, ""));
      assertTrue(msg.contains("sub"), msg);
    }

    @Test
    void rejectsAFileWithoutAHeader() throws Exception {
      Path dir = folder("Empty.csv", "");
      assertTrue(
          ExpTestHelpers.error(program(dir, ""))
              .contains("No header in: " + dir.resolve("Empty.csv")));
    }

    @Test
    void needsTheSqlTypeside() throws Exception {
      String msg =
          ExpTestHelpers.error(
              program(folder("People.csv", "id\n"), "")
                  .replace("typeside Ty = sql", "typeside Ty = literal { types String }"));
      assertTrue(msg.contains("Not on SQL typeside"), msg);
    }
  }

  @Nested
  class Expression {

    private final SchExpCsv exp = new SchExpCsv("dir", List.of(), new TyExpVar("Ty"));

    @Test
    void printsTheFolderAndTypeside() throws Exception {
      SchExpCsv e = ExpTestHelpers.exp(program("dir", ""), "S");
      assertEquals(exp, e);
      assertTrue(exp.toString().contains("dir"), exp.toString());
    }

    @Test
    void dependsOnItsTypeside() {
      assertEquals(List.of(new Pair<>("Ty", Kind.TYPESIDE)), new ArrayList<>(exp.deps()));
      assertTrue(exp.options().isEmpty());
      assertTrue(
          exp.allowedOptions()
              .containsAll(
                  Set.of(
                      AqlOption.csv_field_delim_char,
                      AqlOption.csv_escape_char,
                      AqlOption.csv_quote_char,
                      AqlOption.csv_import_prefix,
                      AqlOption.csv_file_extension)));
    }

    @Test
    void isTypedByItsTypeside() throws Exception {
      AqlTyping g = ExpTestHelpers.typing(program("dir", ""));
      assertEquals(new TyExpVar("Ty"), exp.type(g));
      assertSame(exp, exp.resolve(g, null));
    }

    @Test
    void mapsOverItsTypeside() {
      List<Exp<?>> seen = new ArrayList<>();
      exp.map(seen::add);
      assertEquals(List.of(exp, new TyExpVar("Ty")), seen);
    }

    @Test
    void equalityComparesEveryPart() {
      assertEquals(exp, exp);
      assertEquals(exp, new SchExpCsv("dir", List.of(), new TyExpVar("Ty")));
      assertEquals(exp.hashCode(), new SchExpCsv("dir", List.of(), new TyExpVar("Ty")).hashCode());
      assertNotEquals(exp, new SchExpCsv("dir2", List.of(), new TyExpVar("Ty")));
      assertNotEquals(exp, new SchExpCsv("dir", List.of(), new TyExpVar("U")));
      assertNotEquals(
          exp, new SchExpCsv("dir", List.of(new Pair<>("timeout", "1")), new TyExpVar("Ty")));
      SchExpCsv none = new SchExpCsv(null, List.of(), null);
      assertEquals(none, new SchExpCsv(null, List.of(), null));
      assertEquals(none.hashCode(), new SchExpCsv(null, List.of(), null).hashCode());
      assertNotEquals(none, exp);
      assertNotEquals(exp, none);
      assertNotEquals(new SchExpCsv("dir", List.of(), null), exp);
      assertNotEquals(exp, null);
      assertNotEquals(exp, "import_csv");
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsAVisitor() {
      SchExpVisitor<String, Unit, RuntimeException> v = mock(SchExpVisitor.class);
      when(v.visit(Unit.unit, exp)).thenReturn("visited");
      assertEquals("visited", exp.accept(Unit.unit, v));
    }

    @Test
    void coacceptsTheTypingInversion() {
      assertTrue(
          exp.coaccept(Unit.unit, new TypingInversion(), new AqlTyping()) instanceof SchExpCsv);
    }
  }
}
