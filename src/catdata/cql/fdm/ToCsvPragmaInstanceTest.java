package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import catdata.cql.AqlOptions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ToCsvPragmaInstanceTest {

  @Nested
  class GetFormatMethod {

    @Test
    void getFormatReturnsNonNull() {
      var format = ToCsvPragmaInstance.getFormat(AqlOptions.initialOptions);
      assertNotNull(format);
    }
  }
}
