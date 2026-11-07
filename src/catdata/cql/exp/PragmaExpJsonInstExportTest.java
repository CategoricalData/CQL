package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.Pair;
import catdata.Unit;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.Kind;
import catdata.cql.Pragma;
import catdata.cql.exp.InstExp.InstExpVar;
import catdata.cql.exp.PragmaExp.PragmaExpVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link PragmaExpJsonInstExport}, {@code export_json_instance I "file"}, which
 * writes an instance as JSON-LD: a resource per row, a class per entity and a property per
 * attribute.
 */
class PragmaExpJsonInstExportTest {

  @TempDir Path dir;

  private static String program(String typeside, String value, Path file, String block) {
    return """
        typeside Ty = %s
        schema S = literal : Ty { entities A attributes n : A -> %s }
        instance I = literal : S { generators a : A equations a.n = %s }
        command c = export_json_instance I "%s" %s
        """
        .formatted(typeside, typeside.equals("sql") ? "Varchar" : "String", value, file, block);
  }

  private static final String LITERAL = "literal { types String constants x : String }";

  private Pragma export(String program) throws Exception {
    return ExpTestHelpers.eval(program, "c", Kind.PRAGMA);
  }

  @Nested
  class Evaluation {

    /** The result is the number of triples; the text of the file it reads first is overwritten. */
    @Test
    void writesTheValuesOfAnSqlInstance() throws Exception {
      Path file = dir.resolve("out.json");
      Pragma p = export(program("sql", "hello", file, ""));
      assertEquals("Size 6", p.toString());
      String json = Files.readString(file);
      assertTrue(json.contains("cql://entity/A/0"), json);
      assertTrue(json.contains("\"cql://attribute/A/n\": \"hello\""), json);
    }

    /** A value that is a constant of the typeside rather than a Java object is left out. */
    @Test
    void dropsAValueThatIsATypesideConstant() throws Exception {
      Path file = dir.resolve("out.json");
      Pragma p = export(program(LITERAL, "x", file, ""));
      assertEquals("Size 5", p.toString());
      String json = Files.readString(file);
      assertTrue(json.contains("cql://entity/A/0"), json);
      assertFalse(json.contains("\"cql://attribute/A/n\":"), json);
    }

    @Test
    void namesTheTypeOfAnAttributeByItsExternalType() throws Exception {
      Path file = dir.resolve("out.json");
      String block =
          "{ external_types String -> \"http://www.w3.org/2001/XMLSchema#string\" \"x => x\" }";
      export(program(LITERAL, "x", file, block));
      assertTrue(Files.readString(file).contains("http://www.w3.org/2001/XMLSchema#string"));
    }

    /** A file that cannot be written is reported in the result, and the command still succeeds. */
    @Test
    void reportsAFileItCannotWrite() throws Exception {
      Path file = dir.resolve("missing").resolve("out.json");
      Pragma p = export(program("sql", "hello", file, ""));
      assertTrue(p.toString().startsWith(file + "\n\n"), p.toString());
    }

    @Test
    void isNotEvaluatedAtCompileTime() throws Exception {
      String program = program("sql", "hello", dir.resolve("o.json"), "");
      AqlEnv env = ExpTestHelpers.run(program).env;
      PragmaExpJsonInstExport<?, ?> e = ExpTestHelpers.exp(program, "c");
      assertThrows(IgnoreException.class, () -> e.eval0(env, true));
    }
  }

  @Nested
  class Expression {

    @SuppressWarnings({"unchecked", "rawtypes"})
    private PragmaExpJsonInstExport<?, ?> exp(
        String file,
        List<Pair<String, String>> options,
        List<Pair<String, Pair<String, String>>> tys) {
      return new PragmaExpJsonInstExport(new InstExpVar("I"), file, options, tys);
    }

    private final PragmaExpJsonInstExport<?, ?> exp = exp("f", List.of(), null);

    /** The file is not printed, and the options are printed with their separators swapped. */
    @Test
    void printsTheInstanceAndTheOptionsButNotTheFile() {
      assertEquals("export_json_instance I ", exp.toString());
      assertEquals(
          "export_json_instance I  {\n\toptionsstart_ids_at\n\t\t1}",
          exp("f", List.of(new Pair<>("start_ids_at", "1")), null).toString());
    }

    @Test
    void dependsOnTheInstance() {
      assertEquals(List.of(new Pair<>("I", Kind.INSTANCE)), new ArrayList<>(exp.deps()));
      assertTrue(exp.options().isEmpty());
      assertTrue(exp.allowedOptions().contains(AqlOption.start_ids_at));
      List<Exp<?>> seen = new ArrayList<>();
      exp.map(seen::add);
      assertEquals(exp, seen.get(0));
    }

    @Test
    void equalityComparesEveryPart() {
      List<Pair<String, Pair<String, String>>> tys =
          List.of(new Pair<>("String", new Pair<>("t", "x => x")));
      assertEquals(exp, exp);
      assertEquals(exp, exp("f", List.of(), null));
      assertEquals(exp.hashCode(), exp("f", List.of(), null).hashCode());
      assertEquals(exp, exp("f", List.of(), List.of()));
      assertNotEquals(exp, exp("g", List.of(), null));
      assertNotEquals(exp, exp("f", List.of(new Pair<>("start_ids_at", "1")), null));
      assertNotEquals(exp, exp("f", List.of(), tys));
      assertNotEquals(exp, null);
      assertNotEquals(exp, "export_json_instance");
      assertEquals(Map.of(), exp.options());
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsAVisitor() {
      PragmaExpVisitor<String, Unit, RuntimeException> v = mock(PragmaExpVisitor.class);
      when(v.visit(Unit.unit, exp)).thenReturn("visited");
      assertEquals("visited", exp.accept(Unit.unit, v));
    }
  }
}
