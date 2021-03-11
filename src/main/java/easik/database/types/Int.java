package easik.database.types;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import easik.xml.xsd.nodes.types.XSDBaseType;
import easik.xml.xsd.nodes.types.XSDType;

/**
 * A class representing an INTEGER SQL data type. An INTEGER can contain values
 * from -2^31 to 2^31-1. See also SmallInt and BigInt. This class should really
 * be named Integer, as that is the standard type, but that interferes with
 * java's built-in Integer class.
 */
public class Int extends EasikType {
  /**
   *
   *
   * @return
   */
  @Override
  public String toString() {
    return "INTEGER";
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
    return input.matches("^[-+]?\\d{1,10}$");
  }

  /**
   *
   *
   * @return
   */
  @Override
  public int getSqlType() {
    return Types.INTEGER;
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
    ps.setInt(col, Integer.parseInt(value));
  }

  /**
   *
   *
   * @return
   */
  @Override
  public XSDType getXMLSchemaType() {
    return XSDBaseType.xsInt;
  }
}
