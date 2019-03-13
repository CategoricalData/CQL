package easik.database;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.io.File;
import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.util.Map;

import javax.swing.JOptionPane;

import easik.database.api.jdbc.JDBCDriver;
import easik.database.base.PersistenceDriver;
import easik.sketch.Sketch;
import easik.ui.datamanip.UpdateMonitor;

/**
 * <p>
 * This class encapsulates database functionality in terms of loading the
 * 'drivers'. Each Sketch object will have their own Database object. This class
 * is in charge of associating database types (as requested by a user) with an
 * API, and taking the necessary steps to ensure that the standard Easik
 * exporting can take place.
 * </p>
 *
 * <p>
 * The API maintenance overhead may seem unnecessary (and it is right now since
 * JDBC is the only outgoing connection type) but it will be useful if different
 * types of connections need to be made. For instance, by distinguishing between
 * outgoing connection types it is possible to make connections to non-JDBC
 * supported (i.e non-relational) databases.
 * </p>
 *
 * <p>
 * This class expects options to be passed to it from whatever source that may
 * be. The driver also expects the caller to 'set it up' for the appropriate
 * type of exporting (to file or to a native database). Either way, a database
 * name needs to be supplied (which dictates the language used) and from this we
 * will attempt to find the right API (@see DriverInfo).
 * </p>
 *
 * <p>
 * 'Database' is not meant to imply server exporting, but rather any exporting
 * that is in a database-specific language. So exportToFile functionality is in
 * here because the file will be in a specific language.
 * </p>
 *
 * <p>
 * No exceptions should leave this class... Show them to the user and then
 * decide what to do with them from there.
 * </p>
 *
 * @author Christian Fiddick
 * @version Summer 2012, Easik 2.2
 */
public class Database {
	/**  */
	private String activeApi; // The currently loaded API

	/**  */
	private PersistenceDriver activeDriver; // The specific database driver

	/**  */
	private String dbName; // The currently loaded database driver type

	/**  */
	private final Sketch sketch; // The sketch using the driver

	/**
	 * Not much to the constructor. Just associate the object with a Sketch.
	 * 
	 * @param sketch
	 *            Client Sketch
	 */
	public Database(Sketch sketch) {
		this.sketch = sketch;
		activeApi = null;
		dbName = null;
	}

	/**
	 * Accesor method for DbName
	 * 
	 * @return dbName
	 */
	public String getDbName() {
		return dbName;
	}

	// --------------------------------------------------------------------------------------------
	// Text exporting - just set the driver up for text exporting
	// (setTextExport) and
	// do the exporting (exportToFile)
	// --------------------------------------------------------------------------------------------

	/**
	 * Wrapper to set a driver up to be used for text exporting.
	 * 
	 * @param type
	 *            The database name
	 * @param opts
	 *            Export options
	 * @return True if successful, false otherwise
	 */
	public boolean setTextExport(String type, final Map<String, ?> opts) {
		return setDb(type) && load(opts);
	}

	/**
	 * Wrapper for exporting to a file.
	 * 
	 * @param selected
	 *            File to export to
	 * @param opts
	 *            Export options
	 * @return True if successful, false otherwise
	 */
	public boolean exportToFile(File selected, Map<String, ?> opts) {
		if (hasActiveDriver()) {
			try {
				activeDriver.getSketchExporter(sketch, opts).exportToFile(selected);
			} catch (Exception e) {
				error("Failed to export to file", e);

				return false;
			}
		} else {
			error("No driver is loaded", null);

			return false;
		}

		return true;
	}

	// --------------------------------------------------------------------------------------------
	// Database exporting - just call setDatabaseExport and then exportToNative
	// --------------------------------------------------------------------------------------------

	/**
	 * Wrapper to set up a driver to be used for server exporting.
	 * 
	 * @param type
	 *            The database name
	 * @param paramMap
	 *            Export options
	 * @return True if successful, false otherwise
	 */
	public boolean setDatabaseExport(final String type, final Map<String, String> paramMap) {
		if (!setDb(type)) {
			return false;
		}

		if (!load(paramMap)) {
			return false;
		}

		if (sketch.setConnectionParams(paramMap)) {
			// updated the connection parameters, so store them with the Sketch
			sketch.setDirty();
		}

		return true;
	}

	/**
	 * Sets the parameters for a database to be loaded (but doesn't actually
	 * load the database).
	 * 
	 * @param dbn
	 *            Name of the database to be loaded
	 * @return True if successful, false otherwise
	 */
	private boolean setDb(String dbn) {
		if (hasActiveDriver()) { // Start fresh
			cleanDatabaseDriver();
		}

		dbName = dbn;

		try {
			activeApi = DriverInfo.getApi(dbName);
		} catch (Exception e) {
			error("No API found for " + dbName, e);
			cleanDatabaseDriver();

			return false;
		}

		return true;
	}

	/**
	 * Loads the driver if the name and API have been set.
	 * 
	 * @param opts
	 *            Driver options
	 * @return True if successful, false otherwise
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private boolean load(Map<String, ?> opts) {
		if ((activeApi != null) && (dbName != null)) {
			// Attempt to load the specified class
			final Class subclass;

			try {
				// By convention the database driver has the same name as the
				// containing package
				subclass = Database.class.getClassLoader().loadClass("easik.database.db." + dbName + '.' + dbName);
			} catch (ClassNotFoundException cnfe) {
				error("Failed to find the driver", cnfe);

				return false;
			}

			// Check to make sure the driver is valid
			if (activeApi.equals(DriverInfo.JDBC)) {
				if (!JDBCDriver.class.isAssignableFrom(subclass)) {
					error("Driver class '" + subclass.getName() + "' is not a valid JDBC Driver", null);

					return false;
				}
			}

			// Get the constructor that we can call
			Constructor<? extends PersistenceDriver> c;

			try {
				c = subclass.getConstructor(Map.class);
			} catch (NoSuchMethodException nsme) {
				error("Driver can't be instantiated", nsme);

				return false;
			}

			if (c == null) {
				return false;
			}

			try {
				activeDriver = c.newInstance(opts);
			} catch (Exception e) {
				error("Failed to instantiate driver", e);

				return false;
			}

			sketch.getFrame().setConnectionStatus(activeDriver);
		} else {
			error("Don't know what database type (and API) to load", null);

			return false;
		}

		return true;
	}

	/**
	 * Wrapper for exporting to a database server.
	 * 
	 * @param opts
	 *            Export options
	 * @return True if successful, false otherwise
	 */
	public boolean exportToNative(Map<String, ?> opts) {
		if (hasActiveDriver()) {
			try {
				activeDriver.getSketchExporter(sketch, opts).exportToNative();
			} catch (Exception e) {
				error("Failed to export to database", e);
				return false;
			}
		} else {
			error("No driver is loaded", null);

			return false;
		}

		return true;
	}

	/**
	 * Does this database object have an active driver? This does not mean that
	 * the driver works...
	 * 
	 * @return true if a driver is loaded, false otherwise
	 */
	public boolean hasActiveDriver() {
		return (activeDriver != null) && (dbName != null) && (activeApi != null);
	}

	/**
	 * Does this database object have an outgoing connection?
	 * 
	 * @return true if the driver reports a connection
	 */
	public boolean hasConnection() {
		return hasActiveDriver() && activeDriver.hasConnection();
	}

	/**
	 * Remove the active database driver.
	 * 
	 * @return True if successful, false otherwise
	 */
	public boolean cleanDatabaseDriver() {
		try {
			if (hasConnection()) {
				activeDriver.disconnect();
			}
		} catch (Exception e) {
			error("Couldn't disconnect driver", e);

			return false;
		}

		activeDriver = null;

		sketch.getFrame().setConnectionStatus(activeDriver);

		return true;
	}

	/**
	 * Attempt to disable constraints temporarily and allow an update.
	 * 
	 * @return True if successful, false otherwise
	 */
	public boolean overrideConstraints() {
		if (hasActiveDriver()) {
			try {
				activeDriver.overrideConstraints(sketch);
			} catch (Exception e) {
				error("Failed to override constraints", e);

				return false;
			}
		} else {
			error("No driver is loaded", null);

			return false;
		}

		return true;
	}

	/**
	 * Return the currently loaded driver as a JDBC driver.
	 * 
	 * @return currently loaded driver as a JDBC driver
	 */
	public JDBCDriver getJDBCDriver() {
		if (activeDriver instanceof JDBCDriver) {
			return (JDBCDriver) activeDriver;
		} 
			error("The currently loaded driver is not a JDBC based driver, but is trying to be used as such", null);
		

		return null;
	}

	/**
	 * Get an outgoing JDBC connection from the currently loaded JDBC driver.
	 * 
	 * @return outgoing JDBC connection
	 */
	public Connection getJDBCConnection() {
		if (hasActiveDriver()) {
			if (activeDriver instanceof JDBCDriver) {
				try {
					return getJDBCDriver().getConnection();
				} catch (Exception e) {
					error("Couldn't get a JDBC connection", e);
				}
			}
		} else {
			error("No driver is loaded", null);
		}

		return null;
	}

	/**
	 * Get an UpdateMonitor from the active database driver.
	 * 
	 * @return UpdateMonitor for the active database driver
	 */
	public UpdateMonitor newUpdateMonitor() {
		if (hasActiveDriver()) {
			// currently just a JDBC driver will be loaded but this would need
			// updating if XML database exporting were possible
			JDBCDriver driver = getJDBCDriver();

			if (driver != null) {
				return driver.newUpdateMonitor(sketch);
			}
		} else {
			error("No driver is loaded, can't get update monitor", null);
		}

		return null;
	}

	/**
	 * Alert the user about an error (show a dialog and print the stack trace)
	 * 
	 * @param info
	 *            Some background to the Exception
	 * @param e
	 *            The Exception to get the message from
	 */
	public void error(String info, Exception e) {
		String exceptionMessage = "";

		if (e != null) {
			e.printStackTrace();

			exceptionMessage = ": " + e.getMessage();
		}

		JOptionPane.showMessageDialog(sketch.getFrame(), info + exceptionMessage, "Database Error", JOptionPane.ERROR_MESSAGE);
	}
}
