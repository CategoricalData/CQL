package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import catdata.cql.AqlOptions;

class ToCsvPragmaTransformTest {

  @Nested
  class GetFormat {

    @Test
    void getFormatReturnsNonNull() {
      var format = ToCsvPragmaInstance.getFormat(AqlOptions.initialOptions);
      assertNotNull(format);
    }

    @Test
    void getFormatHasDelimiterString() {
      var format = ToCsvPragmaInstance.getFormat(AqlOptions.initialOptions);
      assertNotNull(format.getDelimiterString());
    }

    @Test
    void getFormatHasQuoteChar() {
      var format = ToCsvPragmaInstance.getFormat(AqlOptions.initialOptions);
      assertNotNull(format.getQuoteCharacter());
    }
  }
}
