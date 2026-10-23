package catdata.cql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** The keyword of each kind, as error messages and the IDE print it. */
class KindTest {

  @ParameterizedTest
  @CsvSource({
    "CONSTRAINTS, constraints",
    "TYPESIDE, typeside",
    "SCHEMA, schema",
    "INSTANCE, instance",
    "MAPPING, mapping",
    "TRANSFORM, transform",
    "QUERY, query",
    "PRAGMA, command",
    "GRAPH, graph",
    "COMMENT, comment",
    "SCHEMA_COLIMIT, schema_colimit",
    "THEORY_MORPHISM, theory_morphism",
    "APG_typeside, apg_typeside",
    "APG_instance, apg_instance",
    "APG_morphism, apg_morphism",
    "APG_mapping, apg_mapping",
    "APG_schema, apg_schema"
  })
  void printsItsKeyword(Kind k, String keyword) {
    assertEquals(keyword, k.toString());
  }

  @Test
  void hasAKeywordForEveryKind() {
    assertEquals(17, Kind.values().length);
  }
}
