package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import catdata.cql.AqlOptions;

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
