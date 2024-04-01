package easik.database.types;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import easik.xml.xsd.nodes.types.XSDBaseType;
import easik.xml.xsd.nodes.types.XSDType;

/**
 * A class representing a BOOLEAN SQL data type. A BOOLEAN value contains a
 * true/false value.
 */
public class Boolean extends EasikType {
  /**
   *
   *
   * @return
   */
  @Override
  public String toString() {
    return "BOOLEAN";
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
    return input.matches("^(?i:true|false)$");
  }

  /**
   *
   *
   * @return
   */
  @Override
  public int getSqlType() {
    return Types.BOOLEAN;
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
    ps.setBoolean(col, java.lang.Boolean.parseBoolean(value));
  }

  /**
   *
   *
   * @return
   */
  @Override
  public XSDType getXMLSchemaType() {
    return XSDBaseType.xsBoolean;
  }
}
