package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.LineException;
import catdata.LocStr;
import catdata.Pair;
import catdata.Unit;
import catdata.cql.Kind;
import catdata.cql.exp.InstExp.InstExpVar;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TransExpImport} and its two forms, {@code transform t = import_jdbc url : I
 * -> J { A -> "query" }} ({@link TransExpJdbc}) and {@code transform t = import_csv : I -> J { A ->
 * "file" }} ({@link TransExpCsv}). Both are documented, but both now fail once the checks before
 * the import pass.
 */
class TransExpImportTest {

  private static final String INSTANCES =
      """
      typeside Ty = literal { types String }
      schema S = literal : Ty { entities A attributes n : A -> String }
      instance I = literal : S { generators a : A }
      instance J = literal : S { generators b : A }
      """;

  private static final List<Pair<LocStr, String>> MAP =
      List.of(new Pair<>(new LocStr(0, "A"), "q"));

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static TransExpJdbc<?, ?, ?, ?> jdbc(String url, String i, String j) {
    return new TransExpJdbc(url, new InstExpVar(i), new InstExpVar(j), MAP, List.of());
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static TransExpCsv<?, ?, ?, ?> csv(String i, String j) {
    return new TransExpCsv(new InstExpVar(i), new InstExpVar(j), MAP, List.of());
  }

  @Nested
  class Evaluation {

    @Test
    void jdbcImportIsRemoved() throws Exception {
      String msg =
          ExpTestHelpers.error(
              INSTANCES + "transform t = import_jdbc \"jdbc:h2:mem:x\" : I -> J { A -> \"q\" }\n");
      assertTrue(msg.contains("JDBC import for transforms deprecated and removed."), msg);
    }

    @Test
    void csvImportIsRemoved() throws Exception {
      String msg =
          ExpTestHelpers.error(INSTANCES + "transform t = import_csv : I -> J { A -> \"f\" }\n");
      assertTrue(msg.contains("Transform import deprecated."), msg);
    }

    @Test
    void rejectsAnImportForAnEntityTheSchemaLacks() throws Exception {
      String msg =
          ExpTestHelpers.error(INSTANCES + "transform t = import_csv : I -> J { Z -> \"f\" }\n");
      assertTrue(
          msg.contains("there is an import for Z, which is not an entity in the schema "), msg);
    }

    @Test
    void rejectsASourceWithLabelledNulls() throws Exception {
      String program =
          INSTANCES.replace("generators a : A", "generators a : A s : String")
              + "transform t = import_csv : I -> J { A -> \"f\" }\n";
      assertTrue(ExpTestHelpers.error(program).contains("Import of nulls not supported."));
    }

    @Test
    void rejectsInstancesOnDifferentSchemas() {
      String program =
          INSTANCES
              + "schema S2 = literal : Ty { entities A }\ninstance K = literal : S2 { }\n"
              + "transform t = import_csv : I -> K { A -> \"f\" }\n";
      LineException e = assertThrows(LineException.class, () -> ExpTestHelpers.run(program));
      assertTrue(
          e.getMessage().startsWith("Source instance of transform has schema\nS"), e.getMessage());
    }

    @Test
    void isSkippedAtCompileTime() throws Exception {
      AqlMultiDriver d = ExpTestHelpers.run(INSTANCES);
      TransExpCsv<?, ?, ?, ?> e = csv("I", "J");
      assertThrows(IgnoreException.class, () -> e.eval0(d.env, true));
    }
  }

  @Nested
  class Expression {

    @Test
    void printsItsForm() {
      assertEquals("import_jdbc  \"url\" : I -> J {\n\tA -> q}", jdbc("url", "I", "J").toString());
      assertEquals("import_csv I -> J {\n\tA -> q}", csv("I", "J").toString());
    }

    @Test
    void dependsOnBothInstances() throws Exception {
      assertEquals(
          Set.of(new Pair<>("I", Kind.INSTANCE), new Pair<>("J", Kind.INSTANCE)),
          Set.copyOf(csv("I", "J").deps()));
      AqlTyping g = ExpTestHelpers.typing(INSTANCES);
      assertEquals(new Pair<>(new InstExpVar("I"), new InstExpVar("J")), csv("I", "J").type(g));
      List<Exp<?>> seen = new ArrayList<>();
      TransExpCsv<?, ?, ?, ?> e = csv("I", "J");
      e.map(seen::add);
      assertEquals(e, seen.get(0));
    }

    @Test
    void keepsTheTwoFormsApart() {
      assertNotEquals(csv("I", "J"), jdbc("url", "I", "J"));
      assertNotEquals(jdbc("url", "I", "J"), csv("I", "J"));
      assertNotEquals(jdbc("url", "I", "J"), jdbc("other", "I", "J"));
      assertEquals(jdbc("url", "I", "J"), jdbc("url", "I", "J"));
      assertEquals(jdbc("url", "I", "J").hashCode(), jdbc("url", "I", "J").hashCode());
      assertEquals(csv("I", "J"), csv("I", "J"));
      assertEquals(csv("I", "J").hashCode(), csv("I", "J").hashCode());
      assertNotEquals(csv("I", "J"), csv("I", "K"));
      assertNotEquals(csv("I", "J"), csv("K", "J"));
      assertNotEquals(csv("I", "J"), null);
      assertNotEquals(csv("I", "J"), "import_csv");
    }

    @Test
    @SuppressWarnings("unchecked")
    void bothAcceptAVisitor() {
      TransExpVisitor<String, Unit, RuntimeException> v = mock(TransExpVisitor.class);
      TransExpCsv<?, ?, ?, ?> c = csv("I", "J");
      TransExpJdbc<?, ?, ?, ?> j = jdbc("url", "I", "J");
      when(v.visit(Unit.unit, c)).thenReturn("csv");
      when(v.visit(Unit.unit, j)).thenReturn("jdbc");
      assertEquals("csv", c.accept(Unit.unit, v));
      assertEquals("jdbc", j.accept(Unit.unit, v));
    }
  }
}
