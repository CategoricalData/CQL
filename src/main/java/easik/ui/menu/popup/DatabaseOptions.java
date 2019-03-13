package easik.ui.menu.popup;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import easik.Easik;
import easik.database.base.PersistenceDriver;
import easik.ui.JUtils;
import easik.ui.Option;
import easik.ui.OptionsDialog;
import easik.ui.SketchFrame;

/**
 *
 *
 * @version 12/09/12
 * @author Christian Fiddick
 */
public class DatabaseOptions extends OptionsDialog {
	/**
	 *    
	 */
	private static final long serialVersionUID = -7015982443613547415L;

	/**  */
	private boolean _skipConnSettings = false;

	/** If available, dialog uses this driver's options as field defaults */
	private PersistenceDriver _dbd;

	// The SQL dialect we're using

	/**  */
	private String _dialect;

	/*
	 * Various JThings containing entered information. Note that not all of
	 * these are used for every db type.
	 */

	/**  */
	private JTextField _hostname, _port, _database, _schema, _username;

	/**  */
	private JPasswordField _password;

	/**  */
	private JCheckBox _quoteIdentifiers;

	/** This dialog's parent frame */
	private SketchFrame _theFrame;

	/**
	 * Creates and displays a new modal db options dialog.
	 *
	 * @param dialect
	 *            the SQL driver type, such as "MySQL" or "PostgreSQL"
	 * @param sketchFrame
	 *            the SketchFrame to attach this modal dialog box to
	 */
	public DatabaseOptions(final String dialect, final SketchFrame sketchFrame) {
		super(sketchFrame, "Database connection options");

		_dialect = dialect;
		_theFrame = sketchFrame;

		setSize(425, 350);
		showDialog();
	}

	/**
	 * Creates and displays a new modal db options dialog.
	 *
	 * @param dialect
	 *            the SQL driver type, such as "MySQL" or "PostgreSQL"
	 * @param dbd
	 *            an existing driver whos settings will be used as the defualt
	 *            for the current dialog
	 * @param sketchFrame
	 *            the SketchFrame to attach this modal dialog box to
	 */
	public DatabaseOptions(final String dialect, final PersistenceDriver dbd, final SketchFrame sketchFrame) {
		super(sketchFrame, "Database connection options");

		_dialect = dialect;
		_theFrame = sketchFrame;

		setSize(425, 350);

		_dbd = dbd;

		showDialog();
	}

	/**
	 * Creates and displays a new modal db options dialog, but doesn't ask for
	 * username/password/hostname/port, but only db name (and related). This is
	 * used when getting a driver for a non-connection (for instance, for an SQL
	 * text export).
	 * 
	 * @param dialect
	 *            which slq
	 * @param sketchFrame
	 *            the frame
	 * @param noConnection
	 *            connect or not
	 */
	public DatabaseOptions(final String dialect, final SketchFrame sketchFrame, final boolean noConnection) {
		super(sketchFrame, "Database options");

		_dialect = dialect;
		_theFrame = sketchFrame;

		setSize(425, noConnection ? 250 : 350);

		_skipConnSettings = noConnection;

		showDialog();
	}

	/**
	 * Creates and displays a new modal db options dialog, but doesn't ask for
	 * username/password/hostname/port, but only db name (and related). This is
	 * used when getting a driver for a non-connection (for instance, for an SQL
	 * text export).
	 * 
	 * @param dialect
	 *            the SQL driver type, such as "MySQL" or "PostgreSQL"
	 * @param dbd
	 *            an existing driver whos settings will be used as the defualt
	 *            for the current dialog
	 * @param sketchFrame
	 *            the SketchFrame to attach this modal dialog box to
	 * @param noConnection
	 *            connect or not
	 */
	public DatabaseOptions(final String dialect, final PersistenceDriver dbd, final SketchFrame sketchFrame, final boolean noConnection) {
		super(sketchFrame, "Database options");

		_dialect = dialect;
		_theFrame = sketchFrame;

		setSize(425, noConnection ? 250 : 350);

		_skipConnSettings = noConnection;
		_dbd = dbd;

		showDialog();
	}

	/**
	 *
	 *
	 * @return
	 */
	@Override
	public List<Option> getOptions() {
		final List<Option> opts = new LinkedList<>();
		final Map<String, String> saved = _theFrame.getMModel().getConnectionParams();

		if (!_skipConnSettings) {
			final String username = saved.get("username");
			String hostname = saved.get("hostname");
			final String port = saved.get("port");

			if (hostname == null) {
				hostname = "localhost"; // A reasonable hostname default
			}

			opts.add(new Option(new JLabel("Username"), _username = JUtils.textField(username)));
			opts.add(new Option(new JLabel("Password"), _password = (JPasswordField) JUtils.fixHeight(new JPasswordField())));
			opts.add(new Option(new JLabel("Database hostname"), _hostname = JUtils.textField(hostname)));
			opts.add(new Option(new JLabel("Database port"), _port = JUtils.textField(port)));
		}

		String defaultDBName = saved.get("database");

		if (defaultDBName == null) {
			defaultDBName = _theFrame.getMModel().getDocInfo().getName().replaceAll("\\W+", "_").replaceFirst("^_+", "").replaceFirst("_+$", "");
		}

		opts.add(new Option(new JLabel("Database name"), _database = JUtils.textField(defaultDBName)));

		if ("PostgreSQL".equals(_dialect)) {
			String schema = saved.get("schema");

			if (schema == null) {
				schema = "";
			}

			opts.add(new Option(new JLabel("Schema name"), _schema = JUtils.textField(schema)));
		}

		final boolean quoting;
		String quoteSetting = saved.get("quoteIdentifiers");

		if (quoteSetting == null) {
			quoteSetting = Easik.getInstance().getSettings().getProperty("sql_quoting");
		}

		quoting = (quoteSetting != null) && "true".equals(quoteSetting);
		_quoteIdentifiers = new JCheckBox("Quote table and column names");

		_quoteIdentifiers.setSelected(quoting);
		opts.add(new Option(new JLabel("Identifier quoting"), _quoteIdentifiers));

		// FIXME -- we should add some help (mouseover? help button?) to
		// describe these in more detail
		if (_dbd != null) {
			// If we have an existing connection, reset everything to its
			// current settings:
			if (_dbd.hasOption("hostname")) {
				_hostname.setText((String) _dbd.getOption("hostname"));
			}

			if (_dbd.hasOption("port")) {
				_port.setText((String) _dbd.getOption("port"));
			}

			if (_dbd.hasOption("username")) {
				_username.setText((String) _dbd.getOption("username"));
			}

			if (_dbd.hasOption("database")) {
				_database.setText((String) _dbd.getOption("database"));
			}

			if ("PostgreSQL".equals(_dialect) && _dbd.hasOption("schema")) {
				_schema.setText((String) _dbd.getOption("schema"));
			}

			if (_dbd.hasOption("quoteIdentifiers")) {
				_quoteIdentifiers.setSelected(Boolean.parseBoolean((String) _dbd.getOption("quoteIdentifiers")));
			}
		}

		return opts;
	}

	/**
	 *
	 *
	 * @return
	 */
	public Map<String, String> getParams() {
		final Map<String, String> options = new HashMap<>(9);

		if (!_skipConnSettings) {
			options.put("type", _dialect);
			options.put("hostname", _hostname.getText());
			options.put("port", _port.getText());
			options.put("username", _username.getText());
			options.put("password", new String(_password.getPassword()));
		}

		options.put("database", _database.getText());

		if ("PostgreSQL".equals(_dialect)) {
			options.put("schema", _schema.getText());
		}

		options.put("quoteIdentifiers", _quoteIdentifiers.isSelected() ? "true" : "false");

		// We don't prompt for the pk/fk settings currently, but include them
		// anyway.
		final String pkFormat = Easik.getInstance().getSettings().getProperty("sql_pk_columns");
		final String fkFormat = Easik.getInstance().getSettings().getProperty("sql_fk_columns");

		if (pkFormat != null) {
			options.put("pkFormat", pkFormat);
		}

		if (fkFormat != null) {
			options.put("fkFormat", fkFormat);
		}

		return options;
	}

	/**
	 *
	 *
	 * @return
	 */
	@Override
	public boolean verify() {
		// FIXME -- make sure they entered at least a db name
		// Perhaps some other data sanity checks?
		// - numeric port
		// - hostname consisting only of letters, numbers, and .'s.
		return true;
	}
}
