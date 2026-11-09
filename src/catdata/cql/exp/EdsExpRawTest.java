package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.Chc;
import catdata.Pair;
import catdata.Unit;
import catdata.cql.AqlOptions;
import catdata.cql.Constraints;
import catdata.cql.Kind;
import catdata.cql.exp.EdsExp.EdsExpVar;
import catdata.cql.exp.EdsExp.EdsExpVisitor;
import catdata.cql.exp.EdsExpRaw.EdExpRaw;
import catdata.cql.exp.SchExp.SchExpVar;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link EdsExpRaw}, the constraints literal {@code literal : S { imports ... forall
 * ... where ... -> exists ... where ... }}, and its inner {@link EdExpRaw}, one constraint.
 */
class EdsExpRawTest {

  private static final String SCHEMA =
      """
      typeside Ty = literal { types String }
      schema S = literal : Ty { entities A B foreign_keys f : A -> B attributes n : A -> String }
      """;

  private static final String ED = "forall a:A s:String where a.n = s -> exists b:B where a.f = b";

  private static String program(String body) {
    return SCHEMA + "constraints C = literal : S { " + body + " }\n";
  }

  private static EdsExpRaw exp(String body) throws Exception {
    return ExpTestHelpers.exp(program(body), "C");
  }

  private static EdExpRaw ed(String body) throws Exception {
    return (EdExpRaw) exp(body).raw().get("constraints").get(0).s;
  }

  @Nested
  class Evaluation {

    @Test
    void evaluatesEachConstraint() throws Exception {
      Constraints c = ExpTestHelpers.eval(program(ED), "C", Kind.CONSTRAINTS);
      assertEquals(1, c.eds.size());
      assertEquals(Chc.inRight("A"), c.eds.get(0).As.get("a"));
      assertEquals(Chc.inLeft("String"), c.eds.get(0).As.get("s"));
      assertEquals(Chc.inRight("B"), c.eds.get(0).Es.get("b"));
    }

    @Test
    void addsTheConstraintsOfItsImports() throws Exception {
      String program =
          SCHEMA
              + "constraints C0 = literal : S { "
              + ED
              + " }\nconstraints C = literal : S { imports C0 forall a:A -> where a = a }\n";
      Constraints c = ExpTestHelpers.eval(program, "C", Kind.CONSTRAINTS);
      assertEquals(2, c.eds.size());
    }

    @Test
    void rejectsAVariableOfAnUnknownSort() throws Exception {
      String msg = ExpTestHelpers.error(program("forall a:Nope -> where a = a"));
      assertTrue(
          msg.contains("The sort for a, namely Nope, is not declared as an entity.  Available: "),
          msg);
    }

    @Test
    void buildsAnEmptyLiteralWithoutOptions() {
      EdsExpRaw e = new EdsExpRaw(new SchExpVar("S"), List.of(), List.of(), Unit.unit);
      assertTrue(e.options().isEmpty());
      assertEquals("literal : S {}", e.toString());
    }
  }

  @Nested
  class Expression {

    @Test
    void printsItsImportsConstraintsAndOptions() throws Exception {
      String s =
          ExpTestHelpers.<EdsExpRaw>exp(
                  SCHEMA
                      + "constraints C0 = literal : S { }\n"
                      + "constraints C = literal : S { imports C0 "
                      + ED.replace("exists", "exists unique")
                      + " options timeout = 5 }",
                  "C")
              .toString();
      assertTrue(s.startsWith("literal : S {\n\timports\n\t\tC0\n"), s);
      assertTrue(s.contains("\tforall\n\t\ta:A\n\t\ts:String\n"), s);
      assertTrue(s.contains("\texists unique\n\t\tb:B\n"), s);
      assertTrue(s.endsWith(" options timeout = 5}"), s);
    }

    @Test
    void dependsOnItsSchemaAndImports() throws Exception {
      EdsExpRaw e =
          ExpTestHelpers.exp(
              SCHEMA
                  + "constraints C0 = literal : S { }\nconstraints C = literal : S { imports C0 }",
              "C");
      assertEquals(
          Set.of(new Pair<>("S", Kind.SCHEMA), new Pair<>("C0", Kind.CONSTRAINTS)),
          Set.copyOf(e.deps()));
      assertEquals(List.of(new EdsExpVar("C0")), new ArrayList<>(e.imports()));
      AqlTyping g =
          ExpTestHelpers.typing(
              SCHEMA
                  + "constraints C0 = literal : S { }\nconstraints C = literal : S { imports C0 }");
      assertEquals(new SchExpVar("S"), e.type(g));
    }

    @Test
    void mapsOverItsSchemaAndConstraints() throws Exception {
      EdsExpRaw e = exp(ED);
      List<Exp<?>> seen = new ArrayList<>();
      e.map(seen::add);
      assertEquals(List.of(e, new SchExpVar("S"), ed(ED)), seen);
    }

    @Test
    void equalityComparesEveryPart() throws Exception {
      EdsExpRaw e = exp(ED);
      assertEquals(e, e);
      assertEquals(e, exp(ED));
      assertEquals(e.hashCode(), exp(ED).hashCode());
      assertNotEquals(e, exp(ED.replace("a.f = b", "b = a.f")));
      assertNotEquals(e, exp(ED + " options timeout = 5"));
      assertNotEquals(
          e,
          ExpTestHelpers.exp(
              SCHEMA
                  + "schema S2 = literal : Ty { imports S }\nconstraints C = literal : S2 { "
                  + ED
                  + " }",
              "C"));
      assertNotEquals(
          e,
          ExpTestHelpers.exp(
              SCHEMA
                  + "constraints C0 = literal : S { }\nconstraints C = literal : S { imports C0 "
                  + ED
                  + " }",
              "C"));
      assertNotEquals(e, null);
      assertNotEquals(e, "literal");
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsAVisitor() throws Exception {
      EdsExpRaw e = exp(ED);
      EdsExpVisitor<String, Unit, RuntimeException> v = mock(EdsExpVisitor.class);
      when(v.visit(Unit.unit, e)).thenReturn("visited");
      assertEquals("visited", e.accept(Unit.unit, v));
    }
  }

  @Nested
  class OneConstraint {

    @Test
    void indexesItsClausesForTheOutline() throws Exception {
      EdExpRaw e = ed(ED);
      assertEquals(Set.of("forall", "where", "exists", "where "), e.raw().keySet());
      assertEquals("a : A", e.raw().get("forall").get(0).toString());
      assertEquals(
          Set.of("forall", "where", "exists unique", "where "),
          ed(ED.replace("exists", "exists unique")).raw().keySet());
    }

    @Test
    void isAnInertExpression() throws Exception {
      EdExpRaw e = ed(ED);
      assertEquals(Optional.of(Chc.inRight(Unit.unit)), e.type(new AqlTyping()));
      assertNull(e.options());
      assertNull(e.kind());
      assertNull(e.eval0(null, false));
      assertNull(e.deps());
      assertNull(e.Var("v"));
      assertTrue(e.allowedOptions().containsAll(AqlOptions.proverOptionNames()));
      List<Exp<?>> seen = new ArrayList<>();
      e.map(seen::add);
      assertEquals(List.of(e), seen);
    }

    @Test
    void printsAConstraintWithoutWhereClauses() throws Exception {
      assertEquals(
          "\tforall\n\t\ta:A\n->\n\texists\n\t\tb:B\n", ed("forall a:A -> exists b:B").toString());
      assertEquals(
          "->\n", new EdExpRaw(List.of(), List.of(), List.of(), List.of(), false, null).toString());
    }

    @Test
    void equalityComparesEveryClause() throws Exception {
      EdExpRaw e = ed(ED);
      assertEquals(e, e);
      assertEquals(e, ed(ED));
      assertEquals(e.hashCode(), ed(ED).hashCode());
      assertNotEquals(e, ed(ED.replace("s:String", "t:String").replace("= s", "= t")));
      assertNotEquals(e, ed(ED.replace("where a.n = s ", "")));
      assertNotEquals(e, ed(ED.replace("b:B", "c:B").replace("= b", "= c")));
      assertNotEquals(e, ed(ED.replace(" where a.f = b", "")));
      assertNotEquals(e, ed(ED.replace("exists", "exists unique")));
      assertNotEquals(e, null);
      assertNotEquals(e, "forall");
    }
  }
}
