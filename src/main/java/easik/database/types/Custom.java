package easik.database.types;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collections;
import java.util.Map;

import easik.xml.xsd.nodes.types.XSDBaseType;
import easik.xml.xsd.nodes.types.XSDType;

/**
 * A class representing a user-provided SQL data type. No checking or automatic
 * db type conversion will be performed on this type--users specifying a custom
 * type are expected to provide a valid custom type.
 */
public class Custom extends EasikType {
  /**  */
  private String custom;

  /**
   * Recreates the object from the attributes returned by attributes().
   *
   * @param attr custom attribute
   */
  public Custom(final Map<String, String> attr) {
    this(attr.get("custom"));
  }

  /**
   *
   *
   * @param type
   */
  public Custom(final String type) {
    custom = type;
  }

  /**
   *
   *
   * @return
   */
  @Override
  public String toString() {
    return custom;
  }

  /**
   * Returns the custom SQL type signature this Custom object was created with.
   * 
   * @return custom string
   */
  public String getCustom() {
    return custom;
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
   * Returns the attributes of this object. For a Custom type, this is just the
   * entire type signature as a "custom" attribute key.
   *
   * @return
   */
  @Override
  public Map<String, String> attributes() {
    return Collections.singletonMap("custom", custom);
  }

  /**
   *
   *
   * @return
   */
  @Override
  public int getSqlType() {
    return Types.OTHER;
  } // FIXME??

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
    ps.setString(col, value);
  }

  /**
   *
   *
   * @return
   */
  @Override
  public XSDType getXMLSchemaType() {
    return XSDBaseType.xsString; // @todo - perhaps inherit from string???
  }
}
