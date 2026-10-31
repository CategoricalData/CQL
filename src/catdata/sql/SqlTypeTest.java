package catdata.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Types;
import org.junit.jupiter.api.Test;

class SqlTypeTest {

  @Test
  void namesAJdbcTypeAndKeepsItsCode() {
    SqlType t = SqlType.resolve(String.valueOf(Types.INTEGER));
    assertEquals("INTEGER", t.name);
    assertEquals(Types.INTEGER, t.jdbcType);
  }

  @Test
  void keepsTheCodeOfAVendorTypeItNamesVarchar() {
    // -155 is SQL Server's datetimeoffset
    SqlType t = SqlType.resolve("-155");
    assertEquals("VARCHAR", t.name);
    assertEquals(-155, t.jdbcType);
  }
}
