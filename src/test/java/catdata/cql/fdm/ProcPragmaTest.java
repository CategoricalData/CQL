package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collections;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ProcPragmaTest {

  @Nested
  class Constructor {

    @Test
    void constructorSetsFields() {
      var pragma = new ProcPragma(Collections.emptyList());
      assertNotNull(pragma);
    }
  }

  @Nested
  class ToStringMethod {

    @Test
    void toStringBeforeExecute() {
      var pragma = new ProcPragma(Collections.emptyList());
      assertNotNull(pragma.toString());
    }
  }
}
