package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import catdata.InteriorLabel;
import catdata.ParseException;
import catdata.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SchExpRaw}, the schema literal. A schema literal declares its entities,
 * foreign keys and attributes either in sections ({@code entities}, {@code foreign_keys}, {@code
 * attributes}) or per entity ({@code entity Patient attributes name : String}). The IDE outline is
 * built from {@link SchExpRaw#raw()}, so both forms must be indexed there (issue #113).
 */
class SchExpRawTest {

  private static final String ENTITIES = "entities";
  private static final String FOREIGN_KEYS = "foreign keys";
  private static final String ATTRIBUTES = "attributes";

  private static final String TYPESIDE = "typeside Ty = literal { types String }\n";

  /** Patient and Visit, declared in the sectioned form. */
  private static final String SECTIONED =
      TYPESIDE
          + """
          schema S = literal : Ty {
            entities
              Patient Visit
            foreign_keys
              patient : Visit -> Patient
            attributes
              name dob : Patient -> String
              visit_date : Visit -> String
          }
          """;

  /** The same schema as {@link #SECTIONED}, declared per entity. */
  private static final String PER_ENTITY =
      TYPESIDE
          + """
          schema S = literal : Ty {
            entity Patient
            attributes name dob : String

            entity Visit
            foreign_keys patient : Patient
            attributes visit_date : String
          }
          """;

  /**
   * Both forms in one literal. The grammar takes the per-entity declarations first, while the
   * sectioned ones come first in the merged lists.
   */
  private static final String MIXED =
      TYPESIDE
          + """
          schema S = literal : Ty {
            entity Patient
            attributes name : String

            entities
              Visit
            foreign_keys
              patient : Visit -> Patient
          }
          """;

  private static SchExpRaw parse(String program) throws ParseException {
    return (SchExpRaw) new CombinatorParser().parseProgram(program).exps.get("S");
  }

  /** The outline text of every label in a section, in order. */
  private static List<String> labels(SchExpRaw schema, String section) {
    List<String> ret = new ArrayList<>();
    for (InteriorLabel<Object> label : schema.raw().get(section)) {
      ret.add(label.toString());
    }
    return ret;
  }

  /** The source position of the label whose outline text is {@code text}. */
  private static int locOf(SchExpRaw schema, String section, String text) {
    for (InteriorLabel<Object> label : schema.raw().get(section)) {
      if (label.toString().equals(text)) {
        return label.loc;
      }
    }
    throw new AssertionError("No label " + text + " in section " + section);
  }

  @Nested
  class SectionedForm {

    @Test
    void indexesEntities() throws ParseException {
      assertEquals(List.of("Patient", "Visit"), labels(parse(SECTIONED), ENTITIES));
    }

    @Test
    void indexesForeignKeys() throws ParseException {
      assertEquals(List.of("patient : Visit -> Patient"), labels(parse(SECTIONED), FOREIGN_KEYS));
    }

    @Test
    void indexesAttributes() throws ParseException {
      assertEquals(
          List.of(
              "name : Patient -> String", "dob : Patient -> String", "visit_date : Visit -> String"),
          labels(parse(SECTIONED), ATTRIBUTES));
    }
  }

  @Nested
  class PerEntityForm {

    @Test
    void indexesEntities() throws ParseException {
      assertEquals(List.of("Patient", "Visit"), labels(parse(PER_ENTITY), ENTITIES));
    }

    @Test
    void indexesForeignKeysWithTheEnclosingEntityAsSource() throws ParseException {
      assertEquals(List.of("patient : Visit -> Patient"), labels(parse(PER_ENTITY), FOREIGN_KEYS));
    }

    @Test
    void indexesAttributesWithTheEnclosingEntityAsSource() throws ParseException {
      assertEquals(
          List.of(
              "name : Patient -> String", "dob : Patient -> String", "visit_date : Visit -> String"),
          labels(parse(PER_ENTITY), ATTRIBUTES));
    }

    @Test
    void labelsPointAtTheDeclaredNames() throws ParseException {
      SchExpRaw schema = parse(PER_ENTITY);

      assertEquals(
          PER_ENTITY.indexOf("entity Visit") + "entity ".length(),
          locOf(schema, ENTITIES, "Visit"));
      assertEquals(
          PER_ENTITY.indexOf("foreign_keys patient") + "foreign_keys ".length(),
          locOf(schema, FOREIGN_KEYS, "patient : Visit -> Patient"));
      assertEquals(
          PER_ENTITY.indexOf("dob : String"), locOf(schema, ATTRIBUTES, "dob : Patient -> String"));
    }

    @Test
    void findLocatesAnEntity() throws ParseException {
      assertEquals(
          PER_ENTITY.indexOf("entity Patient") + "entity ".length(),
          parse(PER_ENTITY).find(ENTITIES, "Patient"));
    }

    @Test
    void matchesTheSectionedForm() throws ParseException {
      SchExpRaw perEntity = parse(PER_ENTITY);
      SchExpRaw sectioned = parse(SECTIONED);

      assertEquals(sectioned.ens, perEntity.ens);
      assertEquals(sectioned.fks, perEntity.fks);
      assertEquals(sectioned.atts, perEntity.atts);
      for (String section : List.of(ENTITIES, FOREIGN_KEYS, ATTRIBUTES)) {
        assertEquals(labels(sectioned, section), labels(perEntity, section), section);
      }
    }
  }

  @Nested
  class MixedForm {

    @Test
    void indexesBothFormsWithTheSectionedOnesFirst() throws ParseException {
      SchExpRaw schema = parse(MIXED);

      assertEquals(List.of("Visit", "Patient"), labels(schema, ENTITIES));
      assertEquals(List.of("patient : Visit -> Patient"), labels(schema, FOREIGN_KEYS));
      assertEquals(List.of("name : Patient -> String"), labels(schema, ATTRIBUTES));
    }

    @Test
    void keepsEveryDeclarationInTheSchema() throws ParseException {
      SchExpRaw schema = parse(MIXED);

      assertEquals(Set.of("Patient", "Visit"), schema.ens);
      assertEquals(Set.of(new Pair<>("patient", new Pair<>("Visit", "Patient"))), schema.fks);
      assertEquals(Set.of(new Pair<>("name", new Pair<>("Patient", "String"))), schema.atts);
    }
  }
}
