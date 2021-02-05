package easik.database.api.jdbc;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Set;

import javax.swing.JOptionPane;

import easik.database.base.PersistenceDriver;
import easik.database.types.EasikType;
import easik.sketch.Sketch;
import easik.sketch.vertex.EntityNode;
import easik.ui.datamanip.ColumnEntry;
import easik.ui.datamanip.FreeQueryDialog;
import easik.ui.datamanip.jdbc.JDBCUpdateMonitor;

/**
 * This class is the JDBC specific driver that should be extended by the
 * specific database driver (that uses JDBC). This will usually be any driver
 * that exports SQL.
 *
 * @author Christian Fiddick
 * @version Summer 2012, Easik 2.2
 */
public abstract class JDBCDriver extends PersistenceDriver {
	/**
	 * Used by SQL drivers to cache their connection.
	 */
	protected Connection sqlConn;

	/**
	 * Determines if this driver currently has an active connection.
	 *
	 * @return True if there is an active connection, false otherwise
	 */
	@Override
	public boolean hasConnection() {
		try {
			return (sqlConn != null) && sqlConn.isValid(1000);
		} catch (SQLException e) {
			return false;
		}
	}

	/**
	 * Attempt to get an outgoing connection.
	 *
	 * @return SQL Connection (JDBC)
	 * @throws LoadException
	 * @throws SQLException
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Connection getConnection() throws LoadException, SQLException {
		if (sqlConn != null) {
			return sqlConn;
		}

		if (!isConnectable()) {
			throw new LoadException(
					"Insufficient driver parameters for establishing a db connection: at least db, username, and password are required");
		}

		final StringBuilder dsn = new StringBuilder("jdbc:mysql://");

		if (optionMatches("hostname", "^[A-Za-z0-9.-]+$")) {
			dsn.append(options.get("hostname").toString());
		} else {
			dsn.append("localhost");
		}

		// In theory, a port name can be used on some system (typically such
		// systems have an /etc/services file),
		// which map a name like 'mysql' to a number like 3306; but we don't
		// allow that here.
		if (optionMatches("port", "^[0-9]+$")) {
			dsn.append(':').append(options.get("port"));
		}

		if (!hasOption("createDatabase")) {
			// noinspection HardcodedFileSeparator
			dsn.append('/').append(options.get("database"));
		}

		final Properties props;

		if (options.containsKey("connectProperties") && (options.get("connectProperties") instanceof Properties)) {
			props = new Properties((Properties) options.get("connectProperties"));
		} else {
			props = new Properties();
		}

		// MySQL has a "false" default for the paranoid option which includes
		// stack trace
		// information in any errors messages, so unless the Database creator
		// explicitly
		// asks for it to be false, default it to true.
		if (!props.containsKey("paranoid")) {
			props.setProperty("paranoid", "true");
		}

		if (options.containsKey("username")) {
			props.setProperty("user", options.get("username").toString());
		}

		if (options.containsKey("password")) {
			props.setProperty("password", options.get("password").toString());
		}

		try {
			// Loading the MySQL JDBCDriver class registers it with the
			// DriverInfo
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException cnfe) {
			throw new LoadException("Unable to load MySQL driver", cnfe);
		}

		sqlConn = DriverManager.getConnection(dsn.toString(), props);

		if (!hasConnection()) {
			throw new PersistenceDriver.LoadException("DriverManager.getConnection failed, unknown cause");
		}

		return sqlConn;
	}

	/**
	 * Close the currently cached connection.
	 *
	 * @throws SQLException
	 */
	@Override
	public void disconnect() throws SQLException {
		if (sqlConn != null) {
			sqlConn.close();

			sqlConn = null;
		}
	}

	/**
	 * The statement separator for SQL statements. This is generally only used when
	 * generating multiple SQL statements (e.g. into a file), not when performing
	 * ordinary single db statements.
	 *
	 * @return the string ";"
	 */
	@Override
	public String getStatementSeparator() {
		return ";";
	}

	/**
	 * Returns true if enough options are specified to establish a connection. At a
	 * minimum, <code>db</code>, <code>username</code>, and <code>password</code>
	 * options are required, and <code>db</code> and <code>username</code> must be
	 * non-empty strings.
	 *
	 * @return
	 */
	@Override
	public boolean isConnectable() {
		// db and username have to be non-empty; password just has to be
		// specified (but can be empty):
		return hasOption("database") && hasOption("username") && options.containsKey("password");
	}

	/**
	 * Executes a given SQL query that produces a ResultSet. Typically a SELECT
	 * statement.
	 *
	 * @param s The statement to execute
	 * @return The ResultSet returned from the query's execution
	 *
	 * @throws SQLException
	 */
	public ResultSet executeQuery(final String s) throws SQLException {
		try {
			return getConnection().createStatement().executeQuery(s);
		} catch (LoadException le) {
			throw new SQLException("Unable to attempt SQL query: " + le.getMessage());
		}
	}

	/**
	 * A simple alias for obj.Database().prepareStatement(...). Takes a query, which
	 * may contain literal value placeholders (such as "SELECT col1, col2 FROM
	 * tablename WHERE col1 = ? AND col = ?"), and executes it safely, quoting
	 * values replacing placeholders as required. When handling user input of any
	 * sort, using executeQuery with setString() etc. calls on the PreparedStatement
	 * object is highly recommended over using executeQuery(). The returned prepared
	 * statement is set to not return auto-generated keys.
	 *
	 * @param sql the sql string to prepare.
	 * @return a prepared sql statement
	 *
	 * @throws SQLException
	 */
	public PreparedStatement prepareStatement(final String sql) throws SQLException {
		try {
			return getConnection().prepareStatement(sql);
		} catch (LoadException le) {
			throw new SQLException("Unable to attempt SQL query: " + le.getMessage()); // ,
																						// le);
		}
	}

	/**
	 * Executes a given SQL updating statement. Which may be an INSERT, UPDATE, or
	 * DELETE statement.
	 *
	 * @author Sarah van der Laan
	 * @param s The SQL statement to execute
	 *
	 * @throws SQLException
	 */
	public void executeUpdate(final String s) throws SQLException {
		try {
			getConnection().createStatement().executeUpdate(s);
		} catch (LoadException le) {
			throw new SQLException("Unable to execute SQL statement: " + le.getMessage()); // ,
																							// le);
		}
	}

	/**
	 * Executes a given SQL updating statement. Which may be an INSERT, UPDATE, or
	 * DELETE statement. The statement must be in the form expected by
	 * java.sql.PreparedStatement
	 *
	 * @param sql   The SQL statement to execute
	 * @param input The set of ColumnEntry objects from which our values are
	 *              retrieved. Note: This works but relys on 'input' being iterated
	 *              over in the same order that it was when 'sql' was generated.
	 *
	 * @throws SQLException
	 */
	public void executePreparedUpdate(final String sql, final Set<ColumnEntry> input) throws SQLException {
		final PreparedStatement ps = prepareStatement(sql);
		int col = 0;

		for (final ColumnEntry entry : input) {
			final EasikType type = entry.getType();
			@SuppressWarnings("unused")
			final String value = entry.getValue();

			col++;

			// if value is NULL, assign and continue
			if ("".equals(entry.getValue())) {
				ps.setNull(col, type.getSqlType());

				continue;
			}

			// bind appropriate type to the prepared statement
			type.bindValue(ps, col, entry.getValue());
		}

		ps.execute();
	}

	/**
	 * Returns the part of statement for an insertion with no columns, such as:
	 * <code>() VALUES ()</code> (to be used after
	 * <code>INSERT INTO tablename </code>). By default, we return "() VALUES ()",
	 * but some databases (e.g. postgresql) need something else (e.g. "DEFAULT
	 * VALUES");
	 *
	 * @return String to add when inserting without specifying any columns/values.
	 */
	@SuppressWarnings("static-method")
	public String emptyInsertClause() {
		return "() VALUES ()";
	}

	/**
	 * Creates a db corresponding to the current set of options, with the ability to
	 * drop the db before crating.
	 *
	 * @param drop Indicates if the db should be droped before creation, should a
	 *             naming conflict arise.
	 * @return The success of the db creation. (Always false)
	 *
	 * @throws LoadException
	 * @throws SQLException
	 */
	@SuppressWarnings("static-method")
	public boolean createDatabase(final boolean drop) throws LoadException, SQLException {
		return false;
	}

	/**
	 * Drops a recreates a schema corresponding to the current set of options, if
	 * the driver supports schemas.
	 *
	 * @return Success of operation(Always false)
	 *
	 * @throws LoadException
	 * @throws SQLException
	 */
	@SuppressWarnings("static-method")
	public boolean dropAndRecreateSchema() throws LoadException, SQLException {
		return false;
	}

	/**
	 * Get an update monitor for this JDBC driver.
	 * 
	 * @param sk Requesting sketch.
	 * @return a JDBC update monitor
	 */
	public JDBCUpdateMonitor newUpdateMonitor(Sketch sk) {
		return new JDBCUpdateMonitor(sk, this);
	}

	/**
	 * Toggle constraints for this connection.
	 *
	 *
	 * @param on
	 * @return Success of operation
	 *
	 * @throws Exception
	 */
	public abstract boolean toggleConstraint(boolean on) throws Exception;

	/**
	 * Toggle constraints and let the user modify the table.
	 *
	 * @param sketch Sketch to override triggers for
	 * @throws Exception
	 */
	@Override
	public void overrideConstraints(Sketch sketch) throws Exception {
		// Get update type (in case they need to be treated separately)
		final String INSERT = "Insert";
		final String DELETE = "Delete";
		String[] options = { INSERT, DELETE };
		String edit = (String) JOptionPane.showInputDialog(sketch.getFrame(), "Select constraint override update",
				"What would you like to do?", JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);

		if (edit == null) {
			return;
		}

		// Get which table to update
		ArrayList<EntityNode> ens = new ArrayList<>(sketch.getEntities());

		options = new String[ens.size()];

		for (int i = 0; i < options.length; i++) {
			options[i] = ens.get(i).getName();
		}

		String table = (String) JOptionPane.showInputDialog(sketch.getFrame(), "Select table to modify",
				"Which table to modify?", JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);

		if (table == null) {
			return;
		}

		FreeQueryDialog afqd;
		String text;

		if (INSERT.equals(edit)) {
			text = "INSERT INTO " + table + "() VALUES()";
		} else {
			text = "DELETE FROM " + table + "\n WHERE()";
		}

		afqd = new FreeQueryDialog(sketch.getFrame(), text);

		if (!afqd.isAccepted()) {
			return;
		}

		try {
			toggleConstraint(true); // Disable constraint
			executeUpdate(afqd.getInput());
		} finally {
			toggleConstraint(false); // Enable constraints
		}
	}
}
