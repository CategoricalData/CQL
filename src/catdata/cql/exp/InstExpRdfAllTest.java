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
import catdata.cql.Instance;
import catdata.cql.Kind;
import catdata.cql.exp.InstExp.InstExpVisitor;
import catdata.cql.exp.TyExp.TyExpVar;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for {@link InstExpRdfAll}, {@code import_rdf_all "file" : rdf}, which reads every
 * triple of an RDF file, optionally closed under a Jena reasoner, as a row of the entity R with its
 * subject, predicate and object.
 */
class InstExpRdfAllTest {

  @TempDir Path dir;

  private Path file;

  /** A literal object, a blank node object and a triple about that blank node. */
  @BeforeEach
  void writeTriples() throws Exception {
    file = dir.resolve("data.nt");
    Files.writeString(
        file,
        """
        <http://x/a> <http://x/p> "lit" .
        <http://x/a> <http://x/q> _:b1 .
        _:b1 <http://x/p> <http://x/c> .
        """);
  }

  private String program(String block) {
    return "instance I = import_rdf_all \"" + file + "\" : rdf " + block + "\n";
  }

  private static Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?> eval(String program)
      throws Exception {
    return ExpTestHelpers.eval(program, "I", Kind.INSTANCE);
  }

  @Nested
  class Evaluation {

    @Test
    void readsATripleAsARow() throws Exception {
      Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?> i = eval(program(""));
      assertEquals(Set.of("R"), i.schema().ens);
      assertEquals(3, i.algebra().size("R"));
      assertEquals(1, i.algebra().talg().sks.size());
      String text = i.toString();
      assertTrue(text.contains(".object = lit@Dom"), text);
      assertTrue(text.contains(".subject = \"_:"), text);
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          "getOWLReasoner",
          "getTransitiveReasoner",
          "getRDFSReasoner",
          "getRDFSSimpleReasoner",
          "getOWLMiniReasoner",
          "getOWLMicroReasoner"
        })
    void closesTheTriplesUnderAReasoner(String reasoner) throws Exception {
      Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?> i =
          eval(program("{ options jena_reasoner = " + reasoner + " }"));
      assertTrue(i.algebra().size("R") >= 3, Integer.toString(i.algebra().size("R")));
    }

    @Test
    void rejectsAnUnknownReasoner() throws Exception {
      String msg = ExpTestHelpers.error(program("{ options jena_reasoner = nope }"));
      assertTrue(msg.contains("Not a jena reasoner: nope."), msg);
    }

    @Test
    void needsTheRdfTypeside() throws Exception {
      String program =
          "typeside Ty = literal { types String }\n" + program("").replace(": rdf", ": Ty");
      assertTrue(ExpTestHelpers.error(program).contains("Import not onto rdf typeside"));
    }

    @Test
    void isNotEvaluatedAtCompileTime() throws Exception {
      AqlEnv env = ExpTestHelpers.run(program("")).env;
      InstExpRdfAll e = ExpTestHelpers.exp(program(""), "I");
      assertThrows(IgnoreException.class, () -> e.eval0(env, true));
    }

    @Test
    void makesTheTripleSchema() throws Exception {
      AqlEnv env = ExpTestHelpers.run(program("")).env;
      InstExpRdfAll e = ExpTestHelpers.exp(program(""), "I");
      assertEquals(Set.of("R"), e.makeSchema(env, null).ens);
    }
  }

  @Nested
  class Expression {

    private final InstExpRdfAll exp = new InstExpRdfAll("f", List.of(), new TyExpRdf());

    @Test
    void printsTheFileTheTypesideAndTheOptions() {
      assertEquals("import_rdf_all \"f\" : rdf ", exp.toString());
      assertEquals(
          "import_rdf_all \"f\" : rdf  {\n\t\n\toptions\n\t\tjena_reasoner = getOWLReasoner}",
          new InstExpRdfAll(
                  "f", List.of(new Pair<>("jena_reasoner", "getOWLReasoner")), new TyExpRdf())
              .toString());
    }

    @Test
    void dependsOnItsTypeside() throws Exception {
      InstExpRdfAll e = new InstExpRdfAll("f", List.of(), new TyExpVar("Ty"));
      assertEquals(List.of(new Pair<>("Ty", Kind.TYPESIDE)), new ArrayList<>(e.deps()));
      assertTrue(exp.direct(null).isEmpty());
      assertTrue(exp.options().isEmpty());
      assertTrue(exp.allowedOptions().contains(AqlOption.jena_reasoner));
      List<Exp<?>> seen = new ArrayList<>();
      exp.map(seen::add);
      assertEquals(exp, seen.get(0));
    }

    @Test
    void equalityComparesEveryPart() {
      assertEquals(exp, exp);
      assertEquals(exp, new InstExpRdfAll("f", List.of(), new TyExpRdf()));
      assertEquals(exp.hashCode(), new InstExpRdfAll("f", List.of(), new TyExpRdf()).hashCode());
      assertNotEquals(exp, new InstExpRdfAll("g", List.of(), new TyExpRdf()));
      assertNotEquals(
          exp, new InstExpRdfAll("f", List.of(new Pair<>("jena_reasoner", "")), new TyExpRdf()));
      assertNotEquals(exp, new InstExpRdfAll("f", List.of(), new TyExpVar("Ty")));
      assertNotEquals(exp, null);
      assertNotEquals(exp, "import_rdf_all");
      assertEquals(Map.of(), exp.options());
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsAVisitor() {
      InstExpVisitor<String, Unit, RuntimeException> v = mock(InstExpVisitor.class);
      when(v.visit(Unit.unit, exp)).thenReturn("visited");
      assertEquals("visited", exp.accept(Unit.unit, v));
    }
  }
}
