package easik.ui.menu;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import easik.database.DriverInfo;
import easik.database.base.PersistenceDriver;
import easik.sketch.Sketch;
import easik.ui.EasikFrame;
import easik.ui.SketchFrame;
import easik.ui.menu.popup.DatabaseOptions;
import easik.ui.menu.popup.ExportOptions;

/**
 * This class exports the current sketch to a DBMS that the user supplies a
 * connection to.
 *
 * @see ExportFileAction (much like this class, but for file exporting)
 * @author Christian Fiddick
 * @date Summer 2012
 */
public class ExportDatabaseAction extends AbstractAction {
	/**  */
	private static final long serialVersionUID = -6371251432168103506L;

	/**  */
	protected Sketch _theSketch;

	/**
	 * Create a new ExportXSDAction. Adds a new menu item to the File menu.
	 *
	 * @param inFrame  The Frame
	 * @param inSketch The Sketch
	 */
	public ExportDatabaseAction(final EasikFrame inFrame, final Sketch inSketch) {
		super(((inFrame instanceof SketchFrame) ? "" : "Export to ") + "DBMS...");

		_theSketch = inSketch;

		putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_D));

		String[] ds = DriverInfo.availableDatabaseDrivers();
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

		putValue(Action.SHORT_DESCRIPTION, "Export to a supported database " + available);
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
	 * Exports the current sketch as an SQL db. Displays a message if an error
	 * occurred.
	 * 
	 * @param e The action event
	 */
	@Override
	public void actionPerformed(final ActionEvent e) {
		try {
			if (_theSketch.hasDatabase() && _theSketch.getDatabase().hasActiveDriver()) {
				_theSketch.getDatabase().cleanDatabaseDriver();
			}

			String type = _theSketch.getDatabaseType(); // start by getting the
														// database type
			String api = DriverInfo.getApi(type); // JDBC or XML:DB?

			if (DriverInfo.XMLDB.equals(api)) {
				throw new PersistenceDriver.LoadException("No XML:DB export capability");
			} else if (DriverInfo.JDBC.equals(api)) {
				// load options
				final ExportOptions expOpts = new ExportOptions(type, _theSketch.getFrame());

				if (!expOpts.isAccepted()) {
					return;
				}

				final DatabaseOptions dbopts = new DatabaseOptions(type, _theSketch.getFrame());

				if (!dbopts.isAccepted()) {
					return;
				}

				// set the database access object up for database exporting
				if (!_theSketch.getDatabase().setDatabaseExport(type, dbopts.getParams())) {
					return;
				}

				if (!_theSketch.getDatabase().hasActiveDriver()) { // should
																	// have been
																	// loaded by
																	// now
					throw new PersistenceDriver.LoadException("Unable to make connection");
				}

				if (!_theSketch.getDatabase().exportToNative(expOpts.getParams())) {
					return;
				}

				JOptionPane.showMessageDialog(null, "SQL exported successfully. Entering data manipulation mode.",
						"Export successful.", JOptionPane.INFORMATION_MESSAGE);

				// we don't have to worry about enableDataManip() failing... if
				// we were able to export, we know it will work
				_theSketch.getFrame().enableDataManip(true);
			} else {
			} // won't happen if DriverInfo is configured properly
		} catch (PersistenceDriver.LoadException le) {
			_theSketch.getDatabase().cleanDatabaseDriver();
			JOptionPane.showMessageDialog(null, "An error occurred while exporting to database. " + le.getMessage(),
					"Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * At the time this item is added to a menu, the sketch on which it will act may
	 * not be known. This method sets the sketch to be exported.
	 *
	 * @param sketch The sketch that is set to be exported.
	 */
	public void setSketch(final Sketch sketch) {
		_theSketch = sketch;
	}
}
