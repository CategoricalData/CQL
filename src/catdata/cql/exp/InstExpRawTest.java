package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.Pair;
import catdata.Unit;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.Instance;
import catdata.cql.Kind;
import catdata.cql.exp.InstExp.InstExpVar;
import catdata.cql.exp.InstExp.InstExpVisitor;
import catdata.cql.exp.SchExp.SchExpVar;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link InstExpRaw}, the instance literal {@code literal : S { imports ...
 * generators ... equations ... }}, read as a presentation, or with {@code interpret_as_algebra} as
 * the rows themselves.
 */
class InstExpRawTest {

  private static final String SCHEMA =
      """
      typeside Ty = literal { types String constants x y : String }
      schema S = literal : Ty {
        entities A C
        foreign_keys f : A -> C
        attributes n : A -> String
      }
      """;

  private static String instance(String body) {
    return SCHEMA + "instance I = literal : S { " + body + " }\n";
  }

  private static Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?> eval(String body)
      throws Exception {
    return ExpTestHelpers.eval(instance(body), "I", Kind.INSTANCE);
  }

  private static final String ALGEBRA = "options interpret_as_algebra = true";

  @Nested
  class Presentation {

    @Test
    void takesTheGeneratorsAndEquationsOfItsImports() throws Exception {
      String program =
          SCHEMA
              + "instance J = literal : S { generators a : A equations a.n = x }\n"
              + "instance I = literal : S { imports J generators b : A equations b.n = a.n }\n";
      Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?> i =
          ExpTestHelpers.eval(program, "I", Kind.INSTANCE);
      assertEquals(2, i.algebra().size("A"));
      // a.f and b.f are unset, so each is a row of C of its own
      assertEquals(2, i.algebra().size("C"));
    }

    @Test
    void acceptsAGeneratorOfAType() throws Exception {
      Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?> i =
          eval("generators a : A s : String equations a.n = s");
      assertEquals(1, i.algebra().size("A"));
      assertEquals(1, i.sks().size());
    }

    @Test
    void rejectsAGeneratorOfAnUnknownSort() throws Exception {
      String msg = ExpTestHelpers.error(instance("generators a : Nope"));
      assertTrue(
          msg.contains("The sort for a, namely Nope, is not declared as a type or entity"), msg);
    }

    @Test
    void rejectsAnEquationItCannotType() throws Exception {
      String msg = ExpTestHelpers.error(instance("generators a : A equations a.n = a.f"));
      assertTrue(msg.contains("In equation a.n = a.f, "), msg);
    }
  }

  @Nested
  class Algebra {

    @Test
    void readsTheGeneratorsAsTheRows() throws Exception {
      Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?> i =
          eval("generators a b : A c : C equations a.f = c c = b.f a.n = x y = b.n " + ALGEBRA);
      assertEquals(2, i.algebra().size("A"));
      assertEquals(1, i.algebra().size("C"));
    }

    @Test
    void makesAnUnsetAttributeALabelledNull() throws Exception {
      Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?> i =
          eval("generators a : A c : C equations a.f = c " + ALGEBRA);
      assertEquals(1, i.algebra().talg().sks.size());
    }

    @Test
    void readsAConstantAsAnAttributeValue() throws Exception {
      Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?> i =
          eval("generators a b : A c : C equations a.f = c b.f = c a.n = x y = b.n " + ALGEBRA);
      assertEquals(0, i.algebra().talg().sks.size());
    }

    @Test
    void rejectsAGeneratorOfAType() throws Exception {
      String msg = ExpTestHelpers.error(instance("generators s : String " + ALGEBRA));
      assertTrue(msg.contains("Cannot have generating labelled nulls with import_as_theory"), msg);
    }

    @Test
    void rejectsAnEquationThatIsNotARow() throws Exception {
      String msg =
          ExpTestHelpers.error(
              instance("generators a b : A c : C equations a.f = b.f a.f = c b.f = c " + ALGEBRA));
      assertTrue(
          msg.contains(
              "interpret_as_algebra not compatible with equation a.f = b.f; each equation must be of"
                  + " the form gen.fk=gen or gen.att=javaobject"),
          msg);
    }

    @Test
    void checksThatEveryForeignKeyIsSet() throws Exception {
      String msg = ExpTestHelpers.error(instance("generators a : A c : C " + ALGEBRA));
      assertTrue(
          msg.contains(
              "Incomplete import: no value for foreign key f specified for  ID a in entity A"),
          msg);
    }

    /** Without the check, the unset foreign key fails later, with a message that names nothing. */
    @Test
    void failsLaterWithoutTheClosureCheck() throws Exception {
      String msg =
          ExpTestHelpers.error(
              instance(
                  "generators a : A c : C options interpret_as_algebra = true"
                      + " import_dont_check_closure_unsafe = true"));
      assertTrue(msg.contains("|| null null null null null null"), msg);
    }
  }

  @Nested
  class Expression {

    private InstExpRaw exp(String body) throws Exception {
      return ExpTestHelpers.exp(instance(body), "I");
    }

    @Test
    void printsItsGeneratorsAndEquations() throws Exception {
      String s = exp("generators a : A equations a.n = x").toString();
      assertTrue(s.startsWith("literal : S {"), s);
      assertTrue(s.contains("a : A"), s);
      assertTrue(s.contains("a.n = x"), s);
    }

    @Test
    void indexesItsGeneratorsAndEquationsForTheOutline() throws Exception {
      InstExpRaw e = exp("generators a : A equations a.n = x");
      assertTrue(e.raw().keySet().containsAll(Set.of("generators", "equations")));
      assertEquals("a : A", e.raw().get("generators").get(0).toString());
    }

    @Test
    void dependsOnItsSchemaAndImports() throws Exception {
      InstExpRaw e =
          ExpTestHelpers.exp(
              SCHEMA + "instance J = literal : S { }\ninstance I = literal : S { imports J }\n",
              "I");
      assertEquals(
          Set.of(new Pair<>("S", Kind.SCHEMA), new Pair<>("J", Kind.INSTANCE)),
          Set.copyOf(e.deps()));
      assertEquals(List.of(new InstExpVar("J")), new ArrayList<>(e.imports()));
      assertTrue(
          e.allowedOptions()
              .containsAll(
                  Set.of(
                      AqlOption.interpret_as_algebra,
                      AqlOption.import_dont_check_closure_unsafe,
                      AqlOption.diverge_limit)));
    }

    @Test
    void isTypedByItsSchema() throws Exception {
      AqlTyping g = ExpTestHelpers.typing(instance("generators a : A"));
      assertEquals(new SchExpVar("S"), exp("generators a : A").type(g));
      List<Exp<?>> seen = new ArrayList<>();
      InstExpRaw e = exp("generators a : A");
      e.map(seen::add);
      assertEquals(List.of(e, new SchExpVar("S")), seen);
    }

    @Test
    void equalityComparesEveryPart() throws Exception {
      InstExpRaw e = exp("generators a : A equations a.n = x");
      assertEquals(e, e);
      assertEquals(e, exp("generators a : A equations a.n = x"));
      assertEquals(e.hashCode(), exp("generators a : A equations a.n = x").hashCode());
      assertNotEquals(e, exp("generators b : A equations b.n = x"));
      assertNotEquals(e, exp("generators a : A equations a.n = y"));
      assertNotEquals(e, exp("generators a : A equations a.n = x " + ALGEBRA));
      assertNotEquals(
          e,
          ExpTestHelpers.exp(
              SCHEMA
                  + "schema S2 = literal : Ty { entities A attributes n : A -> String }\n"
                  + "instance I = literal : S2 { generators a : A equations a.n = x }",
              "I"));
      assertNotEquals(e, null);
      assertNotEquals(e, "literal");
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsAVisitor() throws Exception {
      InstExpRaw e = exp("generators a : A");
      InstExpVisitor<String, Unit, RuntimeException> v = mock(InstExpVisitor.class);
      when(v.visit(Unit.unit, e)).thenReturn("visited");
      assertEquals("visited", e.accept(Unit.unit, v));
    }
  }
}
