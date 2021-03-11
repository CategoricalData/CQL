package easik.ui.datamanip;

//~--- non-JDK imports --------------------------------------------------------

import easik.sketch.vertex.EntityNode;

/**
 * In charge of monitoring update statements sent to server. Ensures constraints
 * are maintained, and appropriate error messages are sent to the user.
 *
 * API specific update monitors extend this.
 * 
 * @see easik.ui.datamanip.jdbc.JDBCUpdateMonitor;
 *
 * @version Christian Fiddick Summer 2012
 */
public abstract class UpdateMonitor {
  /**
   *
   *
   * @param table
   *
   * @return
   */
  public abstract boolean updateRow(final EntityNode table);

  /**
   *
   *
   * @param table
   * @param pk
   *
   * @return
   */
  public abstract boolean updateRow(final EntityNode table, final int pk);

  /**
   *
   *
   * @param table
   *
   * @return
   */
  public abstract boolean deleteFrom(final EntityNode table);

  /**
   *
   *
   * @param table
   *
   * @return
   */
  public abstract boolean insert(final EntityNode table);

}
