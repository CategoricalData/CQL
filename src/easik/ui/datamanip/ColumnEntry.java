package easik.ui.datamanip;

//~--- non-JDK imports --------------------------------------------------------

import easik.database.types.EasikType;

/**
 * Simple wrapper class to hold information entered to a column. Contains the
 * column's name, a String representation of the input, and the column's type.
 */
public class ColumnEntry {
  /** The column's name where this entry exists */
  private String columnName;

  /** The EasikType found in this column */
  private EasikType type;

  /** The column's value */
  private String value;

  /**
   *
   *
   * @param inName
   * @param inValue
   * @param inType
   */
  public ColumnEntry(String inName, String inValue, EasikType inType) {
    columnName = inName;
    value = inValue;
    type = inType;
  }

  /**
   *
   *
   * @return
   */
  public String getColumnName() {
    return columnName;
  }

  /**
   *
   *
   * @return
   */
  public String getValue() {
    return value;
  }

  /**
   *
   *
   * @return
   */
  public EasikType getType() {
    return type;
  }
}
