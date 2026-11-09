package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * Unit tests for {@link PragmaExpRdfDirectExport}, {@code command c = export_rdf_direct_xml I
 * "file"}, which writes the direct RDF mapping of an instance as RDF/XML.
 */
class PragmaExpRdfDirectExportTest {

  @TempDir Path dir;

  private static String program(Path file) {
    return """
        typeside Ty = rdf
        schema S = rdf : Ty
        instance I = literal : S {
          generators r : R
          equations r.subject = "http://x/a" r.predicate = "http://x/p" r.object = "http://x/o"
        }
        command c = export_rdf_direct_xml I "%s"
        """
        .formatted(file);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static PragmaExpRdfDirectExport<?, ?> exp(String file, List<Pair<String, String>> ops) {
    return new PragmaExpRdfDirectExport(new InstExpVar("I"), file, ops, null);
  }

  @Nested
  class Evaluation {

    /** The output is the size of the model; the file's text it reads first is overwritten. */
    @Test
    void writesTheInstanceAsRdfXml() throws Exception {
      Path file = dir.resolve("out.xml");
      Pragma p = ExpTestHelpers.eval(program(file), "c", Kind.PRAGMA);
      assertTrue(p.toString().startsWith("Size "), p.toString());
      String xml = Files.readString(file);
      assertTrue(xml.contains("rdf:RDF"), xml);
    }

    @Test
    void needsTheTripleSchema() throws Exception {
      Path file = dir.resolve("out.xml");
      String program =
          """
          typeside Ty = literal { types String }
          schema S = literal : Ty { entities A }
          instance I = literal : S { generators a : A }
          command c = export_rdf_direct_xml I "%s"
          """
              .formatted(file);
      Pragma p = ExpTestHelpers.eval(program, "c", Kind.PRAGMA);
      assertTrue(p.toString().contains("Not on triple schema"), p.toString());
    }

    @Test
    void reportsAFileItCannotWrite() throws Exception {
      Path file = Path.of("/nonexistent/dir/out.xml");
      Pragma p = ExpTestHelpers.eval(program(file), "c", Kind.PRAGMA);
      assertTrue(p.toString().startsWith(file + "\n\n"), p.toString());
    }

    @Test
    void isSkippedAtCompileTime() throws Exception {
      AqlEnv env = new AqlEnv(ExpTestHelpers.parse(""));
      PragmaExpRdfDirectExport<?, ?> e = exp("f", List.of());
      assertThrows(IgnoreException.class, () -> e.eval0(env, true));
    }
  }

  @Nested
  class Expression {

    @Test
    void printsItsInstanceFileAndOptions() {
      assertEquals("export_rdf_direct_xml I f ", exp("f", List.of()).toString());
      assertEquals(
          "export_rdf_direct_xml I f  {\n\toptionsstart_ids_at\n\t\t1}",
          exp("f", List.of(new Pair<>("start_ids_at", "1"))).toString());
    }

    @Test
    void dependsOnTheInstance() throws Exception {
      PragmaExpRdfDirectExport<?, ?> e = exp("f", List.of(new Pair<>("start_ids_at", "1")));
      assertEquals(List.of(new Pair<>("I", Kind.INSTANCE)), new ArrayList<>(e.deps()));
      assertEquals(Map.of("start_ids_at", "1"), e.options());
      assertTrue(e.allowedOptions().contains(AqlOption.start_ids_at));
      List<Exp<?>> seen = new ArrayList<>();
      e.map(seen::add);
      assertEquals(List.of(e, new InstExpVar("I")), seen);
      AqlTyping g = ExpTestHelpers.typing(program(Path.of("f")).replace("command c", "command d"));
      assertEquals(Unit.unit, e.type(g));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void equalityComparesEveryPart() {
      assertEquals(exp("f", List.of()), exp("f", List.of()));
      assertEquals(exp("f", List.of()).hashCode(), exp("f", List.of()).hashCode());
      assertNotEquals(exp("f", List.of()), exp("g", List.of()));
      assertNotEquals(exp("f", List.of()), exp("f", List.of(new Pair<>("start_ids_at", "1"))));
      assertNotEquals(
          exp("f", List.of()),
          new PragmaExpRdfDirectExport(new InstExpVar("J"), "f", List.of(), null));
      assertNotEquals(
          exp("f", List.of()),
          new PragmaExpRdfDirectExport(
              new InstExpVar("I"), "f", List.of(), List.of(new Pair<>("T", new Pair<>("c", "f")))));
      PragmaExpRdfDirectExport none = new PragmaExpRdfDirectExport(null, null, List.of(), null);
      assertEquals(none, new PragmaExpRdfDirectExport(null, null, List.of(), null));
      assertEquals(
          none.hashCode(), new PragmaExpRdfDirectExport(null, null, List.of(), null).hashCode());
      assertNotEquals(none, exp("f", List.of()));
      assertNotEquals(exp("f", List.of()), none);
      assertNotEquals(
          new PragmaExpRdfDirectExport(new InstExpVar("I"), null, List.of(), null),
          exp("f", List.of()));
      assertNotEquals(exp("f", List.of()), null);
      assertNotEquals(exp("f", List.of()), "export");
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsAVisitor() {
      PragmaExpRdfDirectExport<?, ?> e = exp("f", List.of());
      PragmaExpVisitor<String, Unit, RuntimeException> v = mock(PragmaExpVisitor.class);
      when(v.visit(Unit.unit, e)).thenReturn("visited");
      assertEquals("visited", e.accept(Unit.unit, v));
    }
  }
}
