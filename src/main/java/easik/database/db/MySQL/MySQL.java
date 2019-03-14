package easik.database.db.MySQL;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import easik.database.api.jdbc.JDBCDriver;
import easik.database.types.Blob;
import easik.database.types.EasikType;
import easik.database.types.Text;
import easik.database.types.Timestamp;
import easik.sketch.Sketch;

/**
 * EASIK MySQL db driver. Options supported by this driver are as follows. Note
 * that almost all of these options are optional, however getting a Connection
 * object (from Database()) will fail if certain options aren't provided. This
 * is used, for example, when a MySQL driver is needed but a connection is not:
 * for example, when producing an SQL dump file. Note that all of the options
 * marked as required for Database() are also required when exporting to a db
 * (as the MySQL calls Database()).
 *
 * <ul>
 * <li><code>db</code> &mdash; required; the name of the db to use <i>[required
 * for Database() and getSketchExporter()]</i></li>
 * <li><code>hostname</code> &mdash; the hostname of the server to connect to
 * <i>[optional; defaults to localhost]</i></li>
 * <li><code>port</code> &mdash; the port of the server to connect to
 * <i>[optional; if not specified the default will be used]</i></li>
 * <li><code>username</code> &mdash; the username to use when connecting
 * <i>[required for Database()]</i></li>
 * <li><code>password</code> &mdash; the password to use when connecting
 * <i>[required for Database()]</i></li>
 * <li><code>connectProperties</code> &mdash; can be set to a
 * {@link java.util.Properties} object containing MySQL driver-specific options
 * <i>[optional; only has an effect when calling Database()]</i><code></li>
 *      <li><code>quoteIdentifiers</code> &mdash; if set to the string
 * <code>"true"</code>, all identifiers (tables, columns, etc.) will be quoted.
 * This means column and table names may contain any characters (including
 * spaces). Note that certain reserved names (e.g. "Table"&mdash;see the list
 * below) will always be quoted. Also note that if quoteIdentifiers is *not*
 * enabled, sequences of characters other than a-z, A-Z, 0-9, and _ will be
 * converted to underscores.</li>
 * </ul>
 *
 */
public class MySQL extends JDBCDriver {
	/**
	 * Reserved identifier names in MySQL. If we encounter a table/column using one
	 * of these names, we force it to be quoted.
	 */
	public static final Set<String> RESERVED_KEYWORDS = new HashSet<>(
			Arrays.asList(("ADD ALL ALTER ANALYZE AND AS ASC ASENSITIVE " + "BEFORE BETWEEN BIGINT BINARY BLOB BOTH BY "
					+ "CALL CASCADE CASE CHANGE CHAR CHARACTER CHECK COLLATE COLUMN CONDITION CONSTRAINT "
					+ "CONTINUE CONVERT CREATE CROSS CURRENT_DATE CURRENT_TIME CURRENT_TIMESTAMP CURRENT_USER CURSOR "
					+ "DATABASE DATABASES DAY_HOUR DAY_MICROSECOND DAY_MINUTE DAY_SECOND DEC DECIMAL DECLARE DEFAULT "
					+ "DELAYED DELETE DESC DESCRIBE DETERMINISTIC " + "DISTINCT DISTINCTROW DIV DOUBLE DROP DUAL "
					+ "EACH ELSE ELSEIF ENCLOSED ESCAPED EXISTS EXIT EXPLAIN "
					+ "FALSE FETCH FLOAT FLOAT4 FLOAT8 FOR FORCE FOREIGN FROM FULLTEXT "
					+ "GRANT GROUP HAVING     HIGH_PRIORITY HOUR_MICROSECOND HOUR_MINUTE HOUR_SECOND "
					+ "IF IGNORE IN INDEX INFILE INNER INOUT INSENSITIVE INSERT "
					+ "INT INT1 INT2 INT3 INT4 INT8 INTEGER INTERVAL INTO IS ITERATE " + "JOIN KEY KEYS KILL "
					+ "LEADING LEAVE LEFT LIKE LIMIT LINES LOAD LOCALTIME     LOCALTIMESTAMP "
					+ "LOCK LONG LONGBLOB LONGTEXT LOOP LOW_PRIORITY "
					+ "MATCH MEDIUMBLOB MEDIUMINT MEDIUMTEXT MIDDLEINT MINUTE_MICROSECOND MINUTE_SECOND MOD MODIFIES "
					+ "NATURAL NOT NO_WRITE_TO_BINLOG NULL NUMERIC "
					+ "ON OPTIMIZE OPTION OPTIONALLY OR ORDER OUT OUTER OUTFILE " + "PRECISION PRIMARY PROCEDURE PURGE "
					+ "READ READS REAL REFERENCES REGEXP RELEASE RENAME REPEAT REPLACE REQUIRE "
					+ "RESTRICT RETURN REVOKE RIGHT RLIKE "
					+ "SCHEMA SCHEMAS SECOND_MICROSECOND SELECT SENSITIVE SEPARATOR SET SHOW SMALLINT SONAME "
					+ "SPATIAL SPECIFIC SQL SQLEXCEPTION SQLSTATE SQLWARNING SQL_BIG_RESULT SQL_CALC_FOUND_ROWS "
					+ "SQL_SMALL_RESULT SSL STARTING STRAIGHT_JOIN "
					+ "TABLE TERMINATED THEN TINYBLOB TINYINT TINYTEXT TO TRAILING TRIGGER TRUE "
					+ "UNDO UNION UNIQUE UNLOCK UNSIGNED UPDATE USAGE USE USING UTC_DATE UTC_TIME UTC_TIMESTAMP "
					+ "VALUES VARBINARY VARCHAR VARCHARACTER VARYING " + "WHEN WHERE WHILE WITH WRITE "
					+ "XOR YEAR_MONTH ZEROFILL ").split("\\s+")));

	/**
	 * Creates a new MySQL db driver object. Should not be called directly.
	 *
	 * @param opts The options for a MySQL db
	 */
	public MySQL(final Map<String, ?> opts) {
		options = new HashMap<>(opts);
	}

	/**
	 * Creates a db corresponding to the current set of options.
	 * 
	 * @param drop Indicates if the db should be droped before creation, should a
	 *             naming conflict arise.
	 * @return Success of creation.
	 *
	 * @throws LoadException
	 * @throws SQLException
	 */
	@Override
	public boolean createDatabase(final boolean drop) throws LoadException, SQLException {
		options.put("createDatabase", "true");

		final Connection conn = getConnection();

		if (conn == null) {
			return false;
		}

		if (drop) {
			conn.createStatement().executeUpdate("DROP DATABASE IF EXISTS " + options.get("database"));
		}

		conn.createStatement().executeUpdate("CREATE DATABASE " + options.get("database"));
		sqlConn.setCatalog((String) getOption("database"));
		options.remove("createDatabase"); // okay?

		return true;
	}

	/**
	 * Creates a new SketchExporter object for this MySQL driver. The db parameter
	 * must have been specified when this db driver was created.
	 *
	 * @param sketch
	 * @param exportOpts
	 *
	 * @return
	 *
	 * @throws LoadException
	 */
	@Override
	public MySQLExporter getSketchExporter(final Sketch sketch, final Map<String, ?> exportOpts) throws LoadException {
		if (!hasOption("database")) {
			throw new LoadException("MySQL MySQL requires \"db\" parameter!");
		}

		return new MySQLExporter(sketch, this, exportOpts);
	}

	/**
	 * MySQL-specific identifier cleaning. This varies depending on
	 * quoteIdentifiers: if enabled, we trim trailing spaces, and convert /, \, and
	 * . to underscores, and double-up ` characters. Otherwise, we fall back to
	 * SketchExporters cleaning.
	 *
	 * This still isn't perfect, however: MySQL's retarded design means that, prior
	 * to MySQL 5.1.6, databases and tables, even when quoted, must not contain
	 * anything not representable on disk (because of the terrible design decision
	 * that db and table names should be mapped directly to filenames). In effect,
	 * this means a MySQL server running on a Windows system, for example, has
	 * entirely different naming constraints than a MySQL server running on a unix
	 * system. We actually ignore this constraint, which means if we have
	 * entities/attributes with ".", "/", or "\" characters (or various characters
	 * not allowed in Windows filenames), the SQL won't work on MySQL servers older
	 * than 5.1.6.
	 *
	 * @param id
	 *
	 * @return
	 */
	@Override
	public String cleanId(final Object id) {
		if (optionEnabled("quoteIdentifiers")) {
			return id.toString().replaceAll("`", "``");
		}
		return super.cleanId(id);

	}

	/**
	 * MySQL-specific identifier quoting. If the "quoteIdentifiers" option is
	 * enabled, or the identifier is a reserved keyword, we quote identifiers using
	 * the MySQL-specific <code>`</code> character (as opposed to the SQL-standard
	 * <code>"</code> character).
	 *
	 * @param id
	 *
	 * @return
	 */
	@Override
	public String quoteId(final Object id) {
		final String cleaned = cleanId(id);

		if (optionEnabled("quoteIdentifiers") || RESERVED_KEYWORDS.contains(cleaned.toUpperCase())) {
			return '`' + cleaned + '`';
		}
		return cleaned;

	}

	/**
	 * Takes an EasikType and returns the string representation of that type, or
	 * something as close as possible for this particular db.
	 *
	 * @param type the EasikType object desired
	 * @return the string containing the type
	 */
	@Override
	public String getTypeString(final EasikType type) {
		if (type instanceof easik.database.types.Float) {
			return "REAL"; // MySQL treats FLOAT as DOUBLE PRECISION
		} else if (type instanceof Text) {
			return "LONGTEXT"; // MySQL provides TINYTEXT, TEXT, MEDIUMTEXT, and
								// LONGTEXT for no good reason. Just use
								// LONGTEXT.
		} else if (type instanceof Blob) {
			return "LONGBLOB"; // Likewise, MySQL pointlessly has 4 BLOB types.
		} else if (type instanceof Timestamp) {
			return "DATETIME"; // MySQL TIMESTAMPs are horrid auto-updating
								// things
		} else {
			return type.toString();
		}
	}

	/**
	 * Toggle the triggers on the server side.
	 *
	 * @param on True for constraints off, false for on
	 * @return True if toggle successful, false otherwise
	 */
	@Override
	public boolean toggleConstraint(boolean on) {
		try {
			String update = "SET @DISABLE_TRIGGER=";

			if (on) {
				update += "1";
			} else {
				update += "NULL";
			}

			executeUpdate(update);
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return true;
	}
}
