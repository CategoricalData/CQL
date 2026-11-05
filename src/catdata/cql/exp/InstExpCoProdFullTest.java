package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.Pair;
import catdata.ParseException;
import catdata.Unit;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.Instance;
import catdata.cql.Kind;
import catdata.cql.exp.InstExp.InstExpVar;
import catdata.cql.exp.InstExp.InstExpVisitor;
import catdata.cql.exp.SchExp.SchExpVar;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link InstExpCoProdFull}, {@code coproduct I + J : S}, the disjoint union of
 * instances of S, each generator tagged with the name of its instance. Instances with free type
 * algebras are combined directly; otherwise the union is presented again as a literal.
 */
class InstExpCoProdFullTest {

  /** h(s) = h(t) is an equation of the type algebra of I that the typeside does not have. */
  private static final String PROGRAM =
      """
      options require_consistency = false
      typeside Ty = literal { types String constants x : String functions h : String -> String }
      schema S = literal : Ty { entities A attributes n : A -> String }
      schema T = literal : Ty { entities A }
      instance I = literal : S { generators a : A s t : String equations a.n = s h(s) = h(t) }
      instance J = literal : S { generators b : A equations b.n = x }
      instance J2 = literal : S { generators c : A }
      instance E = empty : T
      """;

  private static Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?> coproduct(String is)
      throws Exception {
    return ExpTestHelpers.eval(
        PROGRAM + "instance K = coproduct " + is + " : S\n", "K", Kind.INSTANCE);
  }

  private static InstExpCoProdFull<?, ?, ?, ?> exp(List<String> is, String sch) {
    return new InstExpCoProdFull<>(is, new SchExpVar(sch), List.of());
  }

  private final InstExpCoProdFull<?, ?, ?, ?> exp = exp(List.of("I", "J"), "S");

  @Nested
  class Evaluation {

    @Test
    void tagsTheRowsOfFreeInstancesWithTheirInstance() throws Exception {
      Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?> k = coproduct("J + J2");
      assertEquals(2, k.algebra().size("A"));
      assertEquals(2, k.gens().size());
      assertTrue(k.toString().contains("\"(J, b)\".n = x"), k.toString());
      assertTrue(k.toString().contains("\"(J2, c)\":A"), k.toString());
    }

    @Test
    void presentsInstancesWithEquationsOfValuesAsALiteral() throws Exception {
      Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?> k = coproduct("I + J");
      assertEquals(2, k.algebra().size("A"));
      assertEquals(2, k.sks().size());
      String text = k.toString();
      assertTrue(text.contains("h(\"(I, s)\") = h(\"(I, t)\")"), text);
      assertTrue(text.contains("\"(I, a)\".n = \"(I, s)\""), text);
    }

    /** The typing does not compare the schemas, so the mismatch is found when evaluating. */
    @Test
    void rejectsAnInstanceOfAnotherSchema() throws Exception {
      String program = PROGRAM + "instance K = coproduct J + E : S\n";
      ExpTestHelpers.typing(program);
      assertTrue(ExpTestHelpers.error(program).contains("E not on given schema "));
    }

    @Test
    void rejectsAnInstanceNamedTwice() {
      ParseException e =
          assertThrows(
              ParseException.class,
              () -> ExpTestHelpers.parse(PROGRAM + "instance K = coproduct J + J : S\n"));
      assertTrue(e.getMessage().contains("Duplicate name in J, J"), e.getMessage());
    }

    @Test
    void isNotEvaluatedAtCompileTime() throws Exception {
      AqlEnv env = ExpTestHelpers.run(PROGRAM).env;
      assertThrows(IgnoreException.class, () -> exp.eval0(env, true));
    }
  }

  @Nested
  class Expression {

    @Test
    void printsTheInstancesAndTheSchema() {
      assertEquals("coproduct I + J : S", exp.toString());
    }

    @Test
    void dependsOnTheSchemaAndTheInstances() throws Exception {
      assertEquals(
          Set.of(
              new Pair<>("S", Kind.SCHEMA),
              new Pair<>("I", Kind.INSTANCE),
              new Pair<>("J", Kind.INSTANCE)),
          Set.copyOf(exp.deps()));
      assertEquals(Set.of(new InstExpVar("I"), new InstExpVar("J")), Set.copyOf(exp.direct(null)));
      assertEquals(new SchExpVar("S"), exp.type(ExpTestHelpers.typing(PROGRAM)));
      assertTrue(exp.options().isEmpty());
      assertTrue(exp.allowedOptions().contains(AqlOption.require_consistency));
      List<Exp<?>> seen = new ArrayList<>();
      exp.map(seen::add);
      assertEquals(List.of(exp, new SchExpVar("S")), seen);
    }

    @Test
    void equalityComparesEveryPart() {
      assertEquals(exp, exp);
      assertEquals(exp, exp(List.of("I", "J"), "S"));
      assertEquals(exp.hashCode(), exp(List.of("I", "J"), "S").hashCode());
      assertNotEquals(exp, exp(List.of("J", "I"), "S"));
      assertNotEquals(exp, exp(List.of("I", "J"), "T"));
      assertNotEquals(
          exp,
          new InstExpCoProdFull<>(
              List.of("I", "J"), new SchExpVar("S"), List.of(new Pair<>("timeout", "5"))));
      InstExpCoProdFull<?, ?, ?, ?> none =
          new InstExpCoProdFull<>(List.of("I", "J"), null, List.of());
      assertEquals(none, new InstExpCoProdFull<>(List.of("I", "J"), null, List.of()));
      assertEquals(
          none.hashCode(), new InstExpCoProdFull<>(List.of("I", "J"), null, List.of()).hashCode());
      assertNotEquals(none, exp);
      assertNotEquals(exp, none);
      assertNotEquals(exp, null);
      assertNotEquals(exp, "coproduct");
      assertEquals(Map.of(), none.options());
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
