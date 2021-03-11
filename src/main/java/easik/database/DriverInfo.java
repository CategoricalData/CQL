package easik.database;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.util.ArrayList;

import easik.database.base.PersistenceDriver;

/**
 * <p>
 * This is a Driver Manager class, mostly hard coded. It is used to keep track
 * of the installed drivers, not use them. There really is not much to this
 * class.
 * </p>
 *
 * <p>
 * This class may seem like overkill but it will be useful if different APIs
 * need to be used.
 * </p>
 *
 * @author Christian Fiddick
 * @version Summer 2012, Easik 2.2
 */
public class DriverInfo {
  // available APIs

  /**  */
  public static final String JDBC = "jdbc";

  /**  */
  public static final String XMLDB = "xmldb";

  /**  */
  private static final String[] availableApis = { JDBC, XMLDB };

  // available database-specific drivers, reference the API array

  /**  */
  private static final Driver[] drivers = { new Driver("MySQL", JDBC), new Driver("XSD", XMLDB) // not
                                                  // exporting
                                                  // to
                                                  // a
                                                  // server,
                                                  // don't
                                                  // need
                                                  // an
                                                  // API
  };

  /**
   * Get a String array of the available database drivers.
   * 
   * @return String array of the available database drivers
   */
  public static String[] availableDatabaseDrivers() {
    ArrayList<String> dbDrivers = new ArrayList<>();

    for (Driver d : drivers) {
      for (String api : availableApis) {
        if (api.equals(d.api)) {
          dbDrivers.add(d.driverName);

          break;
        }
      }
    }

    String[] returnMe = new String[dbDrivers.size()];

    return dbDrivers.toArray(returnMe);
  }

  /**
   * Return String array of file drivers (API irrelevant).
   * 
   * @return String array of database drivers
   */
  public static String[] availableFileDrivers() {
    ArrayList<String> fDrivers = new ArrayList<>();

    for (Driver d : drivers) {
      fDrivers.add(d.driverName);
    }

    String[] returnMe = new String[fDrivers.size()];

    return fDrivers.toArray(returnMe);
  }

  /**
   * Return the appropriate API.
   * 
   * @param database Name of database
   * @return API to use (null if database unknown)
   *
   * @throws PersistenceDriver.LoadException
   */
  public static String getApi(String database) throws PersistenceDriver.LoadException {
    if (database == null) {
      return null;
    }

    String possibleApi = null;

    for (Driver d : drivers) {
      if (d.driverName.equals(database)) {
        possibleApi = d.api;

        if (possibleApi == null) {
          return null; // no server export, so null is okay
        }

        break;
      }
    }

    // check that we support the API
    for (String api : availableApis) {
      if (api.equals(possibleApi)) {
        return possibleApi;
      }
    }

    // means the database is not supported if this is reached
    throw new PersistenceDriver.LoadException("No API found for the database '" + database + "'");
  }

  /* Installed driver - just a name-API pair */

  /**
   *
   *
   * @version 12/09/12
   * @author Christian Fiddick
   */
  private static class Driver {
    /**  */
    public final String api;

    /**  */
    public final String driverName;

    /**
     *
     *
     * @param driverName
     * @param api
     */
    public Driver(String driverName, String api) {
      this.api = api;
      this.driverName = driverName;
    }
  }
}
