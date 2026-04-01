package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ToExcelPragmaInstanceTest {

  @Test
  void classCanBeLoaded() throws ClassNotFoundException {
    Class<?> clazz = Class.forName("catdata.cql.fdm.ToExcelPragmaInstance");
    assertEquals("ToExcelPragmaInstance", clazz.getSimpleName());
  }

  @Test
  void classExtendsPragma() {
    assertTrue(catdata.cql.Pragma.class.isAssignableFrom(ToExcelPragmaInstance.class));
  }
}
