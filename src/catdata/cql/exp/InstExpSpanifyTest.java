package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.Pair;
import catdata.Unit;
import catdata.cql.Instance;
import catdata.cql.Kind;
import catdata.cql.Term;
import catdata.cql.exp.InstExp.InstExpVar;
import catdata.cql.exp.InstExp.InstExpVisitor;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link InstExpSpanify}, {@code spanify I}, which turns an instance of the RDF
 * schema, one row per triple, into an instance with one entity per predicate whose rows are the
 * triples of that predicate, each with its subject and object.
 */
class InstExpSpanifyTest {

  /** Two triples for f and one for g, whose object is unknown. */
  private static final String PROGRAM =
      """
      instance I = literal : rdf : rdf {
        generators r1 r2 r3 : R
        equations
          r1.subject = "a1" r1.predicate = "f" r1.object = "b1"
          r2.subject = "a2" r2.predicate = "f" r2.object = "b2"
          r3.subject = "a1" r3.predicate = "g"
      }
      instance K = spanify I
      """;

  private static Instance<String, String, Sym, Fk, Att, Object, Object, Object, Object> k;

  @BeforeAll
  static void spanify() throws Exception {
    k = ExpTestHelpers.eval(PROGRAM, "K", Kind.INSTANCE);
  }

  /** The value of the attribute of the entity on the row. */
  private static String value(String en, String att, Object row) {
    return k.algebra().att(Att.Att(en, att), row).toString();
  }

  @Nested
  class Evaluation {

    @Test
    void makesAnEntityPerPredicate() {
      assertEquals(Set.of("f", "g"), k.schema().ens);
      assertEquals(
          Map.of(
              Att.Att("f", "subject"), new Pair<>("f", "Dom"),
              Att.Att("f", "object"), new Pair<>("f", "Dom"),
              Att.Att("g", "subject"), new Pair<>("g", "Dom"),
              Att.Att("g", "object"), new Pair<>("g", "Dom")),
          k.schema().atts);
      assertEquals(2, k.algebra().size("f"));
      assertEquals(1, k.algebra().size("g"));
      assertEquals(3, k.gens().size());
    }

    @Test
    void keepsTheSubjectAndObjectOfEachTriple() throws Exception {
      Set<String> f = new TreeSet<>();
      for (Object row : k.algebra().en("f")) {
        f.add(value("f", "subject", row) + " " + value("f", "object", row));
      }
      assertEquals(Set.of("a1@Dom b1@Dom", "a2@Dom b2@Dom"), f);
      Object g = k.algebra().en("g").iterator().next();
      assertEquals("a1@Dom", value("g", "subject", g));
      Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?> i =
          ExpTestHelpers.eval(PROGRAM, "I", Kind.INSTANCE);
      assertEquals(i.algebra().hasNulls(), k.algebra().hasNulls());
    }

    @Test
    void namesEachRowByItsPredicate() {
      Object g = k.algebra().en("g").iterator().next();
      assertEquals("g", k.gens().get(g));
      assertTrue(k.gens().containsKey(g));
      List<String> seen = new ArrayList<>();
      k.gens().entrySet((row, en) -> seen.add(en));
      assertEquals(List.of("f", "f", "g"), seen.stream().sorted().toList());
      assertThrows(RuntimeException.class, () -> k.gens().remove(g));
      assertThrows(RuntimeException.class, () -> k.gens().put(g, "f"));
    }

    @Test
    void wrapsTheAlgebraAndProverOfTheInstance() {
      Object g = k.algebra().en("g").iterator().next();
      assertEquals(g, k.algebra().gen(g));
      assertEquals(Term.Gen(g), k.algebra().repr("g", g));
      assertThrows(RuntimeException.class, () -> k.algebra().fk(Fk.Fk("g", "h"), g));
      assertTrue(k.algebra().toStringProver().startsWith("Spanify algebra wrapper of "));
      assertTrue(k.dp().toStringProver().startsWith("Spanify wrapper of "));
      assertEquals(k.algebra().printX("g", g), k.algebra().printX("R", g));
      assertEquals(k.schema(), k.algebra().schema());
      Object unknown = k.algebra().att(Att.Att("g", "object"), g).sk();
      assertNotNull(k.algebra().printY("Dom", unknown));
    }

    /**
     * The wrapper answers whether the instance requires consistency, so it allows unsafe Java
     * equations for an instance that forbids them.
     */
    @Test
    void allowsUnsafeJavaWhenTheInstanceRequiresConsistency() throws Exception {
      String program =
          PROGRAM.replace(
              "r3.predicate = \"g\"\n",
              "r3.predicate = \"g\"\n  options allow_java_eqs_unsafe = false\n");
      Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?> i =
          ExpTestHelpers.eval(program, "I", Kind.INSTANCE);
      Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?> spanned =
          ExpTestHelpers.eval(program, "K", Kind.INSTANCE);
      assertFalse(i.allowUnsafeJava());
      assertTrue(i.requireConsistency());
      assertTrue(spanned.allowUnsafeJava());
      assertTrue(spanned.requireConsistency());
    }

    /** Each check of an equation also prints both sides to the standard output. */
    @Test
    void decidesAnEquationThroughTheInstanceAndPrintsIt() {
      Object g = k.algebra().en("g").iterator().next();
      Term<String, String, Sym, Fk, Att, Object, Object> subject =
          Term.Att(Att.Att("g", "subject"), Term.Gen(g));
      Term<String, String, Sym, Fk, Att, Object, Object> a1 = Term.Obj("a1", "Dom");
      Object object = k.algebra().att(Att.Att("g", "object"), g).sk();
      Term<String, String, Sym, Fk, Att, Object, Object> unknown = Term.Sk(object);
      PrintStream out = System.out;
      ByteArrayOutputStream buf = new ByteArrayOutputStream();
      System.setOut(new PrintStream(buf, true, StandardCharsets.UTF_8));
      try {
        assertTrue(k.dp().eq(Map.of(), subject, a1));
        assertFalse(k.dp().eq(Map.of(), unknown, a1));
      } finally {
        System.setOut(out);
      }
      assertTrue(buf.toString(StandardCharsets.UTF_8).startsWith("? "), buf.toString());
    }

    @Test
    void rejectsAPredicateThatIsNotAValue() throws Exception {
      String program = PROGRAM.replace(" r3.predicate = \"g\"", "");
      String msg = ExpTestHelpers.error(program);
      assertTrue(msg.contains("Encountered predicate that is not IRI: "), msg);
    }

    @Test
    void rejectsAnInstanceOffTheRdfTypeside() throws Exception {
      String program =
          """
          typeside Ty = literal { types String }
          schema S = literal : Ty { entities R }
          instance I = literal : S { generators r : R }
          instance K = spanify I
          """;
      assertTrue(ExpTestHelpers.error(program).contains("Not on RDF typeside: I"));
    }

    @Test
    void isNotEvaluatedAtCompileTime() throws Exception {
      AqlEnv env = ExpTestHelpers.run(PROGRAM).env;
      InstExpSpanify<?, ?> e = ExpTestHelpers.exp(PROGRAM, "K");
      assertThrows(IgnoreException.class, () -> e.eval0(env, true));
    }
  }

  @Nested
  class Expression {

    @SuppressWarnings({"unchecked", "rawtypes"})
    private InstExpSpanify<?, ?> exp(String i, List<Pair<String, String>> options) {
      return new InstExpSpanify(new InstExpVar(i), options);
    }

    private final InstExpSpanify<?, ?> exp = exp("I", List.of());

    @Test
    void printsTheInstanceAndTheOptions() {
      assertEquals("spanify I", exp.toString());
      assertEquals(
          "spanify I {\n\t\n\toptions\n\t\ttimeout = 5}",
          exp("I", List.of(new Pair<>("timeout", "5"))).toString());
    }

    @Test
    void isTypedByItsOwnSchema() throws Exception {
      AqlTyping g = ExpTestHelpers.typing(PROGRAM);
      assertEquals(new SchExpInst<>(exp), exp.type(g));
      assertEquals(List.of(new Pair<>("I", Kind.INSTANCE)), new ArrayList<>(exp.deps()));
      assertEquals(Set.of(new InstExpVar("I")), Set.copyOf(exp.direct(g)));
      assertTrue(exp.options().isEmpty());
      assertEquals(ExpTestHelpers.BASE_OPTIONS, exp.allowedOptions());
      List<Exp<?>> seen = new ArrayList<>();
      exp.map(seen::add);
      assertEquals(List.of(exp), seen);
    }

    @Test
    void makesTheSchemaOfItsValue() throws Exception {
      AqlEnv env = ExpTestHelpers.run(PROGRAM).env;
      InstExpSpanify<?, ?> e = ExpTestHelpers.exp(PROGRAM, "K");
      assertEquals(Set.of("f", "g"), e.makeSchema(env, null).ens);
    }

    @Test
    void equalityComparesTheInstanceAndTheOptions() {
      assertEquals(exp, exp);
      assertEquals(exp, exp("I", List.of()));
      assertEquals(exp.hashCode(), exp("I", List.of()).hashCode());
      assertNotEquals(exp, exp("J", List.of()));
      assertNotEquals(exp, exp("I", List.of(new Pair<>("timeout", "5"))));
      InstExpSpanify<?, ?> none = new InstExpSpanify<>(null, List.of());
      assertEquals(none, new InstExpSpanify<>(null, List.of()));
      assertEquals(none.hashCode(), new InstExpSpanify<>(null, List.of()).hashCode());
      assertNotEquals(none, exp);
      assertNotEquals(exp, none);
      assertNotEquals(exp, null);
      assertNotEquals(exp, "spanify");
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
