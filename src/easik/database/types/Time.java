package easik.database.types;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import easik.xml.xsd.nodes.types.XSDBaseType;
import easik.xml.xsd.nodes.types.XSDType;

/**
 * A class representing a TIME SQL data type. A time value stores a time of day
 * (such as 15:07:12) without an associated date. See also DATE and TIMESTAMP.
 */
public class Time extends EasikType {
  /**
   *
   *
   * @return
   */
  @Override
  public String toString() {
    return "TIME";
  }

  /**
   *
   *
   * @param input
   *
   * @return
   */
  @Override
  public boolean verifyInput(final String input) {
    return true;
  }

  /**
   *
   *
   * @return
   */
  @Override
  public int getSqlType() {
    return Types.TIME;
  }

  /**
   *
   *
   * @param ps
   * @param col
   * @param value
   *
   * @throws SQLException
   */
  @Override
  public void bindValue(final PreparedStatement ps, final int col, final String value) throws SQLException {
    ps.setTime(col, new java.sql.Time(Long.parseLong(value)));
  }

  /**
   *
   *
   * @return
   */
  @Override
  public XSDType getXMLSchemaType() {
    return XSDBaseType.xsTime;
  }
}
