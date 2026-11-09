package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.Pair;
import catdata.Unit;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.Kind;
import catdata.cql.Mapping;
import catdata.cql.exp.MapExp.MapExpVar;
import catdata.cql.exp.MapExp.MapExpVisitor;
import catdata.cql.exp.SchExp.SchExpVar;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link MapExpRaw}, the mapping literal {@code literal : S -> T { entity A -> B
 * foreign_keys f -> g attributes n -> lambda x. x.m }}.
 */
class MapExpRawTest {

  private static final String SCHEMAS =
      """
      typeside Ty = literal { types String Int }
      schema S = literal : Ty {
        entities A C
        foreign_keys f : A -> C
        attributes n : A -> String
      }
      schema T = literal : Ty {
        entities B D E
        foreign_keys g : B -> D h : D -> E
        attributes m : B -> String k : B -> Int
      }
      """;

  private static final String BODY =
      "entity A -> B foreign_keys f -> g.h attributes n -> lambda x. x.m entity C -> E";

  private static String mapping(String body) {
    return SCHEMAS + "mapping F = literal : S -> T { " + body + " }\n";
  }

  private static Mapping<String, String, Sym, Fk, Att, String, Fk, Att> eval(String body)
      throws Exception {
    return ExpTestHelpers.eval(mapping(body), "F", Kind.MAPPING);
  }

  @Nested
  class Evaluation {

    @Test
    void mapsEntitiesForeignKeysAndAttributes() throws Exception {
      var f = eval(BODY);
      assertEquals("B", f.ens.get("A"));
      assertEquals("E", f.ens.get("C"));
      assertEquals(2, f.fks.get(Fk.Fk("A", "f")).second.size());
      assertEquals("x.m", f.atts.get(Att.Att("A", "n")).third.toString());
    }

    @Test
    void takesTheMappingsOfItsImports() throws Exception {
      String program =
          SCHEMAS
              + "mapping F0 = literal : S -> T { "
              + BODY
              + " }\nmapping F = literal : S -> T { imports F0 }\n";
      Mapping<String, String, Sym, Fk, Att, String, Fk, Att> f =
          ExpTestHelpers.eval(program, "F", Kind.MAPPING);
      assertEquals("B", f.ens.get("A"));
    }

    @Test
    void acceptsAVariableOfTheRightEntity() throws Exception {
      var f = eval(BODY.replace("lambda x.", "lambda x:B."));
      assertEquals("B", f.atts.get(Att.Att("A", "n")).second);
    }

    /**
     * The error for an entity outside the schemas is meant to name the entity, but finding its
     * position fails first, since the labels are kept under each entity's name, not "entities".
     */
    @Test
    void reportsAnEntityOutsideTheSchemasByThePositionLookup() throws Exception {
      assertTrue(
          ExpTestHelpers.error(mapping(BODY.replace("entity C -> E", "entity C -> Z")))
              .contains("For section entities, cannot find (C, Z)"));
      assertTrue(
          ExpTestHelpers.error(mapping(BODY + " entity Z -> E"))
              .contains("For section entities, cannot find (Z, E)"));
    }

    @Test
    void rejectsForeignKeysNotInTheSchemas() throws Exception {
      assertTrue(
          ExpTestHelpers.error(mapping(BODY.replace("f -> g.h", "f -> g.h zz -> g")))
              .contains("Not a foreign key in source: A.zz"));
      assertTrue(
          ExpTestHelpers.error(mapping(BODY.replace("f -> g.h", "f -> g.zz")))
              .contains(
                  "In foreign key mapping (A, f) -> g.zz, No target foreign key zz starting at D"));
    }

    @Test
    void rejectsAttributesItCannotMap() throws Exception {
      assertTrue(
          ExpTestHelpers.error(mapping(BODY + " attributes zz -> lambda x. x.m"))
              .contains("is not a source attribute."));
      assertTrue(
          ExpTestHelpers.error(mapping(BODY.replace("lambda x.", "lambda x:D.")))
              .contains("the given source entity for the variable, D, is not B as expected."));
      assertTrue(
          ExpTestHelpers.error(mapping(BODY.replace("lambda x. x.m", "lambda x. x.k")))
              .contains("in mapping for (A, n), "));
    }

    @Test
    void readsAnAttributeAsOneOfItsBlocksEntity() throws Exception {
      String msg = ExpTestHelpers.error(mapping("entity C -> E attributes n -> lambda x. x.m"));
      assertTrue(msg.contains("in mapping for (C, n), (C, n) is not a source attribute."), msg);
    }

    @Test
    void rejectsATypeTheTargetTypesideLacks() throws Exception {
      String program =
          SCHEMAS
              + """
              typeside Ty2 = literal { types Int }
              schema T2 = literal : Ty2 { entities B attributes k : B -> Int }
              mapping F = literal : S -> T2 { entity A -> B attributes n -> lambda x. x.k entity C -> B }
              """;
      String msg = ExpTestHelpers.error(program);
      assertTrue(msg.contains("type String does not exist in target typeside."), msg);
    }
  }

  @Nested
  class Expression {

    @Test
    void printsItsSections() throws Exception {
      MapExpRaw f = ExpTestHelpers.exp(mapping(BODY), "F");
      String s = f.toString();
      assertTrue(s.startsWith("literal : S -> T {\n"), s);
      assertTrue(s.contains("A -> B"), s);
      assertTrue(s.contains("f -> g.h"), s);
    }

    @Test
    void printsItsImports() throws Exception {
      MapExpRaw f =
          ExpTestHelpers.exp(
              SCHEMAS
                  + "mapping F0 = literal : S -> T { }\nmapping F = literal : S -> T { imports F0 }\n",
              "F");
      assertTrue(f.toString().contains("\timports\n\t\tF0"), f.toString());
      assertEquals(List.of(new MapExpVar("F0")), new ArrayList<>(f.imports()));
    }

    @Test
    void indexesEachEntityForTheOutline() throws Exception {
      MapExpRaw f = ExpTestHelpers.exp(mapping(BODY), "F");
      assertTrue(f.raw().keySet().containsAll(Set.of("A", "C")), f.raw().keySet().toString());
      assertEquals(3, f.raw().get("A").size());
    }

    @Test
    void dependsOnBothSchemasAndItsImports() throws Exception {
      MapExpRaw f =
          ExpTestHelpers.exp(
              SCHEMAS
                  + "mapping F0 = literal : S -> T { }\nmapping F = literal : S -> T { imports F0 }\n",
              "F");
      assertEquals(
          Set.of(
              new Pair<>("S", Kind.SCHEMA),
              new Pair<>("T", Kind.SCHEMA),
              new Pair<>("F0", Kind.MAPPING)),
          Set.copyOf(f.deps()));
      assertTrue(f.allowedOptions().contains(AqlOption.dont_validate_unsafe));
    }

    @Test
    void isTypedByItsSchemas() throws Exception {
      MapExpRaw f = ExpTestHelpers.exp(mapping(BODY), "F");
      AqlTyping g = ExpTestHelpers.typing(mapping(BODY));
      assertEquals(new Pair<>(new SchExpVar("S"), new SchExpVar("T")), f.type(g));
      List<Exp<?>> seen = new ArrayList<>();
      f.map(seen::add);
      assertEquals(List.of(f, new SchExpVar("S"), new SchExpVar("T")), seen);
    }

    @Test
    void equalityComparesEveryPart() throws Exception {
      MapExpRaw f = ExpTestHelpers.exp(mapping(BODY), "F");
      assertEquals(f, f);
      assertEquals(f, ExpTestHelpers.exp(mapping(BODY), "F"));
      assertEquals(f.hashCode(), ExpTestHelpers.<MapExpRaw>exp(mapping(BODY), "F").hashCode());
      assertNotEquals(
          f, ExpTestHelpers.exp(mapping(BODY.replace("entity C -> E", "entity C -> D")), "F"));
      assertNotEquals(f, ExpTestHelpers.exp(mapping(BODY.replace("g.h", "g")), "F"));
      assertNotEquals(f, ExpTestHelpers.exp(mapping(BODY.replace("x.m", "x.g.h.m")), "F"));
      assertNotEquals(
          f, ExpTestHelpers.exp(mapping(BODY + " options dont_validate_unsafe = true"), "F"));
      assertNotEquals(
          f,
          ExpTestHelpers.exp(mapping(BODY).replace("literal : S -> T", "literal : T -> T"), "F"));
      assertNotEquals(
          f,
          ExpTestHelpers.exp(mapping(BODY).replace("literal : S -> T", "literal : S -> S"), "F"));
      assertNotEquals(f, null);
      assertNotEquals(f, "literal");
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsAVisitor() throws Exception {
      MapExpRaw f = ExpTestHelpers.exp(mapping(BODY), "F");
      MapExpVisitor<String, Unit, RuntimeException> v = mock(MapExpVisitor.class);
      when(v.visit(Unit.unit, f)).thenReturn("visited");
      assertEquals("visited", f.accept(Unit.unit, v));
    }

    @Test
    void coacceptsTheTypingInversion() throws Exception {
      MapExpRaw f = ExpTestHelpers.exp(mapping(BODY), "F");
      assertTrue(f.coaccept(Unit.unit, new TypingInversion(), new AqlTyping()) instanceof MapExp);
    }
  }
}
