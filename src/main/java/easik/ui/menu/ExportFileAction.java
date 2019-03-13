package easik.ui.menu;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import easik.database.DriverInfo;
import easik.database.base.PersistenceDriver;
import easik.sketch.Sketch;
import easik.ui.EasikFrame;
import easik.ui.FileChooser;
import easik.ui.FileFilter;
import easik.ui.SketchFrame;
import easik.ui.menu.popup.DatabaseOptions;
import easik.ui.menu.popup.ExportOptions;
import easik.ui.menu.popup.XSDWriteOptions;

/**
 * This class exports the current sketch as a file (in specified language). The
 * language is specific to the DBMS the user must select.
 *
 * @see easik.ui.menu.ExportDatabaseAction (much like this class, but for
 *      database exporting)
 * @author Christian Fiddick
 * @date Summer 2012
 */
public class ExportFileAction extends AbstractAction {
	/**  */
	private static final long serialVersionUID = -6341431602813419967L;

	/**  */
	protected Sketch _theSketch;

	/**
	 * Create a new ExportFileAction. Adds a new menu item to the File menu.
	 *
	 * @param inFrame
	 *            The Frame
	 * @param inSketch
	 *            The Sketch
	 */
	@SuppressWarnings("deprecation")
	public ExportFileAction(final EasikFrame inFrame, final Sketch inSketch) {
		super(((inFrame instanceof SketchFrame) ? "" : "Export to ") + "File...");

		_theSketch = inSketch;

		putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_F));

		String[] ds = DriverInfo.availableFileDrivers();
		String available = "";
		int i = 0;

		for (String d : ds) {
			if (i == 0) {
				available += "(";
			}

			available += d;

			if (i == ds.length - 1) {
				available += ")";
			} else if (i == ds.length - 2) {
				available += ", or ";
			} else {
				available += ", ";
			}

			i++;
		}

		putValue(Action.SHORT_DESCRIPTION, "Export sketch to a file " + available);
	}

	/**
	 *
	 *
	 * @return
	 *
	 * @throws CloneNotSupportedException
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	/**
	 * Exports the current sketch as a file (in specified language). Displays a
	 * message if an error occurred.
	 * 
	 * @param e
	 *            The action event
	 */
	@Override
	public void actionPerformed(final ActionEvent e) {
		try {
			String type = _theSketch.getFileDatabaseType(); // start by getting
															// the database type
			String api = DriverInfo.getApi(type); // JDBC or XML:DB?

			if (DriverInfo.XMLDB.equals(api)) {
				// load options
				final XSDWriteOptions xsdOpts = new XSDWriteOptions(_theSketch.getFrame());

				if (!xsdOpts.isAccepted()) {
					return;
				}

				final DatabaseOptions dbopts = new DatabaseOptions(type, _theSketch.getFrame(), true);

				if (!dbopts.isAccepted()) {
					return;
				}

				// set up the database access object for text exporting
				if (!_theSketch.getDatabase().setTextExport(type, dbopts.getParams())) {
					return;
				}

				if (_theSketch.setConnectionParams(dbopts.getParams())) {
					_theSketch.setDirty();
				}

				if (!_theSketch.getDatabase().hasActiveDriver()) { // should
																	// have been
																	// set up by
																	// now
					return;
				}

				final File selected = FileChooser.saveFile("Select XML/XSD output file", new FileFilter("XML Schema definitions", "xsd"), "xsd");

				if (selected == null) {
					return;
				}

				// now export
				if (!_theSketch.getDatabase().exportToFile(selected, xsdOpts.getParams())) {
					return;
				}

				JOptionPane.showMessageDialog(null, "XML/XSD exported successfully.", "Export successful", JOptionPane.INFORMATION_MESSAGE);
			} else if (DriverInfo.JDBC.equals(api)) {
				// load options
				final ExportOptions expOpts = new ExportOptions(type, _theSketch.getFrame());

				if (!expOpts.isAccepted()) {
					return;
				}

				final DatabaseOptions dbopts = new DatabaseOptions(type, _theSketch.getFrame(), true);

				if (!dbopts.isAccepted()) {
					return;
				}

				// set up the database access object for text exporting
				if (!_theSketch.getDatabase().setTextExport(type, dbopts.getParams())) {
					return;
				}

				if (_theSketch.setConnectionParams(dbopts.getParams())) {
					_theSketch.setDirty();
				}

				if (!_theSketch.getDatabase().hasActiveDriver()) { // should
																	// have been
																	// set up by
																	// now
					return;
				}

				final File selected = FileChooser.saveFile("Select SQL output file", new FileFilter("SQL scripts", "sql", "mysql", "pgsql"), "sql");

				if (selected == null) {
					return;
				}

				if (!_theSketch.getDatabase().exportToFile(selected, expOpts.getParams())) {
					return;
				}

				JOptionPane.showMessageDialog(null, "SQL exported successfully.", "Export successful", JOptionPane.INFORMATION_MESSAGE);
			} else {
			} // won't happen if DriverInfo is configured properly
		} catch (PersistenceDriver.LoadException le) {
			_theSketch.getDatabase().cleanDatabaseDriver();
			JOptionPane.showMessageDialog(null, "An error occurred while exporting to file. " + le.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
}
