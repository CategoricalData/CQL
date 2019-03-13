package easik.ui.menu.popup;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import easik.sketch.Sketch;
import easik.sketch.vertex.EntityNode;
import easik.ui.datamanip.UpdateMonitor;

/**
 * Action for adding a row to a table in the popup menu
 */
public class AddRowAction extends AbstractAction {
	/**
	 *    
	 */
	private static final long serialVersionUID = 9210894323277802220L;

	/** The sketch in which any table we wish to add data exists. */
	private Sketch _theSketch;

	/**
	 * Prepare the menu option.
	 *
	 * @param inSketch
	 *            The sketch in which any table we wish to add to exists.
	 */
	public AddRowAction(Sketch inSketch) {
		super("Add row to table...");

		_theSketch = inSketch;

		putValue(Action.SHORT_DESCRIPTION, "Add a row into selected table");
	}

	/**
	 * Checks that our sketch has a db driver, and sends the selected entity
	 * node off to the sketch's update monitor.
	 * 
	 * @param e
	 *            The action event
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		Object[] currentSelection = _theSketch.getSelectionCells();

		if (!(currentSelection[0] instanceof EntityNode) || !_theSketch.hasDatabase()) {
			return;
		}

		UpdateMonitor um = _theSketch.getDatabase().newUpdateMonitor();

		um.insert((EntityNode) currentSelection[0]);
	}
}
