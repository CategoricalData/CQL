package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ToJdbcPragmaTransformTest {

  @Test
  void classCanBeLoaded() throws ClassNotFoundException {
    Class<?> clazz = Class.forName("catdata.cql.fdm.ToJdbcPragmaTransform");
    assertEquals("ToJdbcPragmaTransform", clazz.getSimpleName());
  }

  @Test
  void classExtendsPragma() {
    assertTrue(catdata.cql.Pragma.class.isAssignableFrom(ToJdbcPragmaTransform.class));
  }
}
