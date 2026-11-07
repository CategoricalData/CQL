package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.InteriorLabel;
import catdata.Pair;
import catdata.Unit;
import catdata.cql.ColimitSchema;
import catdata.cql.Kind;
import catdata.cql.Schema;
import catdata.cql.exp.ColimSchExp.ColimSchExpVar;
import catdata.cql.exp.ColimSchExp.ColimSchExpVisitor;
import catdata.cql.exp.SchExp.SchExpVar;
import catdata.cql.exp.TyExp.TyExpVar;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ColimSchExpModify}, {@code modify C { rename ... remove ... }}, which
 * renames the entities, foreign keys and attributes of a schema colimit, then removes foreign keys
 * and attributes by replacing each with an equal path or term.
 */
class ColimSchExpModifyTest {

  /** m = h(n) and f = g hold in C, so both m and g can be removed. */
  private static final String COLIMIT =
      """
      typeside Ty = literal { types String functions h : String -> String }
      schema S = literal : Ty {
        entities A B
        foreign_keys f g : A -> B
        attributes n m : A -> String
      }
      schema_colimit C = quotient S : Ty {
        path_equations S_A.f = S_A.g
        observation_equations forall x. x.m = h(x.n)
      }
      """;

  private static final String MODIFY =
      """
      schema_colimit M = modify C {
        rename entities S_A -> A2
        rename foreign_keys A2.f -> f2
        rename attributes A2.n -> n2
        remove foreign_keys A2.g -> f2
        remove attributes A2.m -> lambda x : A2. h(x.n2)
        options timeout = 5
      }
      """;

  private static final String PROGRAM = COLIMIT + MODIFY;

  private static ColimSchExpModify exp(String modify) throws Exception {
    return ExpTestHelpers.exp(COLIMIT + modify, "M");
  }

  @Nested
  class Evaluation {

    @Test
    void renamesThenRemoves() throws Exception {
      ColimitSchema<String> m = ExpTestHelpers.eval(PROGRAM, "M", Kind.SCHEMA_COLIMIT);
      Schema<String, String, Sym, Fk, Att> s = m.schemaStr;
      assertEquals(Set.of("A2", "S_B"), s.ens);
      assertEquals(Map.of(Fk.Fk("A2", "f2"), new Pair<>("A2", "S_B")), s.fks);
      assertEquals(Map.of(Att.Att("A2", "n2"), new Pair<>("A2", "String")), s.atts);
      assertTrue(s.eqs.isEmpty(), s.eqs.toString());
    }

    @Test
    void rejectsAForeignKeyTheColimitLacks() throws Exception {
      String msg = ExpTestHelpers.error(PROGRAM.replace("A2.g -> f2", "A2.nope -> f2"));
      assertTrue(msg.contains("Not an fk: (A2, nope) in\n\nentities\n\tA2 S_B"), msg);
    }

    @Test
    void rejectsAnAttributeTheColimitLacks() throws Exception {
      String msg = ExpTestHelpers.error(PROGRAM.replace("A2.m -> lambda", "A2.nope -> lambda"));
      assertTrue(msg.contains("Not an attribute: (A2, nope) in\n\nentities\n\tA2 S_B"), msg);
    }

    @Test
    void rejectsAVariableOfAnotherEntity() throws Exception {
      String msg = ExpTestHelpers.error(PROGRAM.replace("lambda x : A2", "lambda x : S_B"));
      assertTrue(
          msg.endsWith(
              "In processing (A2, m) -> lambda x.x.n2.h,  given type is S_B but expected A2"),
          msg);
    }

    /** eval0 reads allow_java_eqs_unsafe from the options, which the typing never lets through. */
    @Test
    void allowsNoOptionOfItsOwn() throws Exception {
      String program = PROGRAM.replace("timeout = 5", "allow_java_eqs_unsafe = true");
      RuntimeException e =
          assertThrows(RuntimeException.class, () -> ExpTestHelpers.typing(program));
      assertTrue(
          e.getMessage().contains("Option not allowed: allow_java_eqs_unsafe."), e.getMessage());
      assertEquals(ExpTestHelpers.BASE_OPTIONS, exp(MODIFY).allowedOptions());
    }
  }

  @Nested
  class Expression {

    @Test
    void printsEachClause() throws Exception {
      assertEquals(
          """
          modify C {
          \trename entities
          \t\tS_A -> A2
          \trename foreign_keys
          \t\tA2.f -> f2
          \trename attributes
          \t\tA2.n -> n2
          \tremove foreign_keys
          \t\tA2.g -> f2
          \tremove attributes
          \t\tm -> lambda x. x.n2.h
          \toptions
          \t\ttimeout = 5}""",
          exp(MODIFY).toString());
    }

    /**
     * The remove attributes clause is printed when there are foreign keys to remove rather than
     * attributes, and without the entity of the attribute.
     */
    @Test
    void printsTheRemovedAttributesOnlyWithRemovedForeignKeys() throws Exception {
      assertEquals(
          "modify C {}",
          exp("schema_colimit M = modify C { remove attributes A2.m -> lambda x. h(x.n2) }")
              .toString());
      assertEquals(
          "modify C {\n\tremove foreign_keys\n\t\tA2.g -> f2\n\tremove attributes}",
          exp("schema_colimit M = modify C { remove foreign_keys A2.g -> f2 }").toString());
    }

    @Test
    void indexesEachClauseForTheOutline() throws Exception {
      Map<String, List<InteriorLabel<Object>>> raw = exp(MODIFY).raw();
      assertEquals(
          Set.of("rename_entities", "rename_fks", "rename_atts", "remove_fks", "remove_atts"),
          raw.keySet());
      assertEquals("S_A -> A2", raw.get("rename_entities").get(0).toString());
      assertEquals("f -> f2", raw.get("rename_fks").get(0).toString());
      assertEquals("n -> n2", raw.get("rename_atts").get(0).toString());
      assertEquals("g -> f2", raw.get("remove_fks").get(0).toString());
      assertEquals("m -> \\x. x.n2.h", raw.get("remove_atts").get(0).toString());
    }

    @Test
    void isTypedAndDependsAsItsColimit() throws Exception {
      ColimSchExpModify e = exp(MODIFY);
      AqlTyping g = ExpTestHelpers.typing(PROGRAM);
      assertEquals(Set.of("S"), e.type(g));
      assertEquals(new TyExpVar("Ty"), e.typeOf(g));
      assertEquals(new SchExpVar("S"), e.getNode("S", g));
      assertEquals(List.of(new Pair<>("C", Kind.SCHEMA_COLIMIT)), new ArrayList<>(e.deps()));
      assertEquals(Map.of("timeout", "5"), e.options());
      assertEquals(
          Set.of(new Pair<>(new SchExpColim(new ColimSchExpVar("C")), new SchExpColim(e))),
          e.gotos(e));
      List<Exp<?>> seen = new ArrayList<>();
      e.map(seen::add);
      assertEquals(List.of(e, new ColimSchExpVar("C")), seen);
    }

    @Test
    void equalityComparesEveryClause() throws Exception {
      ColimSchExpModify e = exp(MODIFY);
      assertEquals(e, e);
      assertEquals(e, exp(MODIFY));
      assertEquals(e.hashCode(), exp(MODIFY).hashCode());
      for (Pair<String, String> change :
          List.of(
              new Pair<>("S_A -> A2", "S_A -> A3"),
              new Pair<>("A2.f -> f2", "A2.f -> f3"),
              new Pair<>("A2.n -> n2", "A2.n -> n3"),
              new Pair<>("A2.g -> f2", "A2.g -> f3"),
              new Pair<>("lambda x : A2. h(x.n2)", "lambda y : A2. h(y.n2)"),
              new Pair<>("timeout = 5", "timeout = 6"),
              new Pair<>("modify C", "modify D"))) {
        assertNotEquals(e, exp(MODIFY.replace(change.first, change.second)), change.second);
      }
      assertNotEquals(e, null);
      assertNotEquals(e, "modify");
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsAVisitor() throws Exception {
      ColimSchExpModify e = exp(MODIFY);
      ColimSchExpVisitor<String, Unit, RuntimeException> v = mock(ColimSchExpVisitor.class);
      when(v.visit(Unit.unit, e)).thenReturn("visited");
      assertEquals("visited", e.accept(Unit.unit, v));
    }
  }
}
