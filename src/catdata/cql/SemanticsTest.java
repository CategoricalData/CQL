package catdata.cql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The interface of every definition's value; its one default is the viewer's sample. */
class SemanticsTest {

  @Test
  void aValueHasNoSampleUnlessItDefinesOne() {
    Semantics s =
        new Semantics() {
          @Override
          public Kind kind() {
            return Kind.COMMENT;
          }

          @Override
          public int size() {
            return 0;
          }
        };
    assertNull(s.sample(3));
    assertEquals(Kind.COMMENT, s.kind());
  }

  @Test
  void anInstanceDefinesASample() {
    var i =
        CqlTestHelpers.literal(
            CqlTestHelpers.sqlSchema(List.of("E"), Map.of(), Map.of()),
            Map.of("e", "E"),
            List.of());
    assertEquals("E (1 rows)\n\nID: 0", i.sample(1));
  }
}
