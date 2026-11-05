package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.Pair;
import catdata.Unit;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.Instance;
import catdata.cql.Kind;
import catdata.cql.exp.InstExp.InstExpVisitor;
import catdata.cql.exp.TyExp.TyExpVar;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link InstExpCsv}, {@code import_csv "dir" : Ty}, which reads each CSV file of a
 * directory as an entity whose attributes are the columns of the header, typed String on an SQL
 * typeside, or Dom when nulls are imported as Skolem constants.
 */
class InstExpCsvTest {

  @TempDir Path dir;

  /** A.csv has a row with a value in each column and a row whose m is empty. */
  @BeforeEach
  void writeA() throws IOException {
    write("A.csv", "n,m\nx,1\ny,\n");
  }

  private void write(String file, String text) throws IOException {
    Files.writeString(dir.resolve(file), text, StandardCharsets.UTF_8);
  }

  /** A program importing dir onto the SQL typeside, with the given text after the directory. */
  private String program(String rest) {
    return "typeside Ty = sql\ninstance I = import_csv \"" + dir + "\" : Ty " + rest + "\n";
  }

  private static String instance(String program) throws Exception {
    Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?> i =
        ExpTestHelpers.eval(program, "I", Kind.INSTANCE);
    return i.toString();
  }

  /** The equations that A.csv gives on the SQL typeside, the empty field being null. */
  private static final List<String> A = List.of("A_0.n = x", "A_0.m = 1", "A_1.n = y", "A_1.m = ");

  private static void assertImportsA(String text) {
    for (String eq : A) {
      assertTrue(text.contains(eq), text);
    }
  }

  @Nested
  class Evaluation {

    @Test
    void readsEachColumnAsAStringAttribute() throws Exception {
      Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?> i =
          ExpTestHelpers.eval(program(""), "I", Kind.INSTANCE);
      assertEquals(Set.of("A"), i.schema().ens);
      assertEquals(
          Map.of(
              Att.Att("A", "n"), new Pair<>("A", "String"),
              Att.Att("A", "m"), new Pair<>("A", "String")),
          i.schema().atts);
      assertEquals(2, i.algebra().size("A"));
      assertImportsA(i.toString());
    }

    @Test
    void readsOnlyTheFilesWithTheExtension() throws Exception {
      write("B.txt", "k\nz\n");
      assertFalse(instance(program("")).contains("B_0"));
      String txt = instance(program("{ options csv_file_extension = \"txt\" }"));
      assertTrue(txt.contains("B_0.k = z"), txt);
      assertFalse(txt.contains("A_0"), txt);
    }

    /**
     * Reading the directory writes an empty block over the block of each entity, so neither the
     * column mapping nor the options of the block take effect.
     */
    @Test
    void discardsTheBlockOfAnEntity() throws Exception {
      String program = program("{ A -> { n -> \"m\" options csv_import_prefix = \"p_\" } }");
      AqlMultiDriver d = ExpTestHelpers.run(program);
      assertImportsA(
          ExpTestHelpers.<Instance<?, ?, ?, ?, ?, ?, ?, ?, ?>>value(d, "I", Kind.INSTANCE)
              .toString());
      InstExpCsv e = (InstExpCsv) d.env.prog.exps.get("I");
      assertEquals(Map.of("A", new Pair<>(List.of(), List.of())), e.map);
    }

    @Test
    void failsOnABlockForAnEntityWithoutAFile() throws Exception {
      String msg = ExpTestHelpers.error(program("{ B -> { } }"));
      assertTrue(msg.contains("On entity B, "), msg);
      assertTrue(msg.contains("\"java.util.List.size()\""), msg);
    }

    /** The prefix is added to the column read from the header, which then is not in the header. */
    @Test
    void failsOnEveryColumnWithAPrefix() throws Exception {
      String msg = ExpTestHelpers.error(program("{ options csv_import_prefix = \"p_\" }"));
      assertTrue(
          msg.contains(
              "On entity A, No column n in file for A nor explicit mapping for n given. Tried p_n"
                  + " and options are [m, n]"),
          msg);
    }

    /** The entity is removed from a column that starts with it, which then is not in the header. */
    @Test
    void failsOnEveryColumnThatStartsWithTheEntity() throws Exception {
      write("A.csv", "m,A_n\n1,x\n");
      String msg = ExpTestHelpers.error(program("{ options csv_prepend_entity = true }"));
      assertTrue(
          msg.contains("No column A_n in file for A nor explicit mapping for A_n given. Tried n"),
          msg);
      assertTrue(instance(program("")).contains("A_0.A_n = x"));
    }

    @Test
    void rejectsADuplicatedColumn() throws Exception {
      write("A.csv", "n,n\nx,y\n");
      assertTrue(ExpTestHelpers.error(program("")).contains("Duplicated field in csv import"));
    }

    @Test
    void rejectsAnEmptyFile() throws Exception {
      write("A.csv", "");
      String msg = ExpTestHelpers.error(program(""));
      assertTrue(msg.contains("On entity A, No header in CSV file for A"), msg);
    }

    @Test
    void rejectsARowShorterThanTheHeader() throws Exception {
      write("A.csv", "n,m\nx\n");
      String msg = ExpTestHelpers.error(program(""));
      assertTrue(msg.contains("Cannot get index 1 from [x]"), msg);
    }

    @Test
    void rejectsAFile() throws Exception {
      String program =
          "typeside Ty = sql\ninstance I = import_csv \"" + dir.resolve("A.csv") + "\" : Ty\n";
      assertTrue(ExpTestHelpers.error(program).contains("Expects folder of CSV files."));
    }

    @Test
    void rejectsAnUnreadableFile() throws Exception {
      Path a = dir.resolve("A.csv");
      assumeTrue(a.toFile().setReadable(false) && !Files.isReadable(a), "running as root");
      String msg = ExpTestHelpers.error(program(""));
      assertTrue(msg.contains("java.io.FileNotFoundException"), msg);
    }

    @Test
    void rejectsATypesideWithoutTheSqlFunctions() throws Exception {
      String program = program("").replace("typeside Ty = sql", "typeside Ty = literal { }");
      assertTrue(
          ExpTestHelpers.error(program).contains("CSV import must be onto sql typeside."), program);
    }

    @Test
    void isNotEvaluatedAtCompileTime() throws Exception {
      AqlEnv env = ExpTestHelpers.run(program("")).env;
      InstExpCsv e = ExpTestHelpers.exp(program(""), "I");
      assertThrows(IgnoreException.class, () -> e.eval0(env, true));
    }
  }

  @Nested
  class SkolemNulls {

    /** Dom is the only type, and its parser rejects the value bad. */
    private String program(String options) {
      return "options csv_import_skolem_nulls = true "
          + options
          + "\ntypeside Ty = literal { external_types Dom -> \"java.lang.String\""
          + " external_parsers Dom -> \"x => { if (x == 'bad') { throw 'bad value'; } return x; }\" }"
          + "\ninstance I = import_csv \""
          + dir
          + "\" : Ty\n";
    }

    @Test
    void importsANullAsAFreshSkolemConstant() throws Exception {
      Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?> i =
          ExpTestHelpers.eval(program(""), "I", Kind.INSTANCE);
      assertEquals(
          Map.of(
              Att.Att("A", "n"), new Pair<>("A", "Dom"),
              Att.Att("A", "m"), new Pair<>("A", "Dom")),
          i.schema().atts);
      String text = i.toString();
      assertTrue(text.contains("A_0.m = \"1\"@Dom"), text);
      assertTrue(text.matches("(?s).*A_1\\.m = \"\\?\\d+\".*"), text);
    }

    @Test
    void reportsAValueTheParserRejects() throws Exception {
      write("A.csv", "n\nbad\n");
      String msg = ExpTestHelpers.error(program(""));
      assertTrue(
          msg.contains(
              "On att A.n, error while importing bad of class class java.lang.String.  Consider"
                  + " option import_null_on_err_unsafe."),
          msg);
    }

    /** The rejected value becomes an empty Optional, which is not a value of Dom. */
    @Test
    void failsOnARejectedValueWithNullOnError() throws Exception {
      write("A.csv", "n\nbad\n");
      String msg = ExpTestHelpers.error(program("import_null_on_err_unsafe = true"));
      assertTrue(
          msg.contains(
              "primitive Optional.empty is given type Dom but is not an instance of class"),
          msg);
    }

    @Test
    void rejectsATypesideWithoutDom() throws Exception {
      String program = program("").replace("Dom", "Str");
      assertTrue(
          ExpTestHelpers.error(program)
              .contains("CSV import with skolem must be onto rdf typeside."));
    }
  }

  @Nested
  class Expression {

    private InstExpCsv exp(String rest) throws Exception {
      return ExpTestHelpers.exp(program(rest).replace(dir.toString(), "d"), "I");
    }

    @Test
    void printsTheDirectoryAndTheBlocks() throws Exception {
      assertEquals("import_csv \"d\" : Ty", exp("").toString());
      assertEquals(
          "import_csv \"d\" : Ty {\n\tA -> {n -> m options csv_import_prefix = p_ }\n}",
          exp("{ A -> { n -> \"m\" options csv_import_prefix = \"p_\" } }").toString());
      assertEquals(
          "import_csv \"d\" : Ty {\n\tA -> {n -> m }\n}",
          exp("{ A -> { n -> \"m\" } }").toString());
    }

    @Test
    void isTypedByItsOwnSchema() throws Exception {
      InstExpCsv e = exp("{ options csv_file_extension = \"txt\" }");
      AqlTyping g = ExpTestHelpers.typing(program(""));
      assertEquals(new SchExpInst<>(e), e.type(g));
      assertEquals(List.of(new Pair<>("Ty", Kind.TYPESIDE)), new ArrayList<>(e.deps()));
      assertTrue(e.direct(g).isEmpty());
      assertEquals(Map.of("csv_file_extension", "txt"), e.options());
      assertTrue(e.allowedOptions().contains(AqlOption.csv_import_prefix));
      assertTrue(e.raw().get("imports").isEmpty());
      List<Exp<?>> seen = new ArrayList<>();
      e.map(seen::add);
      assertEquals(List.of(e, new TyExpVar("Ty")), seen);
    }

    @Test
    void indexesTheBlockOfEachEntity() throws Exception {
      assertEquals(
          "A -> ([(n, m)], [])",
          exp("{ A -> { n -> \"m\" } }").raw().get("imports").get(0).toString());
    }

    @Test
    void equalityComparesEveryPart() throws Exception {
      InstExpCsv e = exp("");
      assertEquals(e, e);
      assertEquals(e, exp(""));
      assertEquals(e.hashCode(), exp("").hashCode());
      assertNotEquals(e, ExpTestHelpers.exp(program(""), "I"));
      assertNotEquals(e, exp("{ A -> { } }"));
      assertNotEquals(e, exp("{ options csv_file_extension = \"txt\" }"));
      assertNotEquals(
          e,
          ExpTestHelpers.exp(program("").replace(dir.toString(), "d").replace(": Ty", ": U"), "I"));
      assertNotEquals(e, null);
      assertNotEquals(e, "import_csv");
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsAVisitor() throws Exception {
      InstExpCsv e = exp("");
      InstExpVisitor<String, Unit, RuntimeException> v = mock(InstExpVisitor.class);
      when(v.visit(Unit.unit, e)).thenReturn("visited");
      assertEquals("visited", e.accept(Unit.unit, v));
    }
  }
}
