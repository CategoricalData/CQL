package easik.ui.menu.popup;

//~--- non-JDK imports --------------------------------------------------------

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import easik.model.edge.ModelEdge.Cascade;
import easik.sketch.Sketch;
import easik.sketch.edge.SketchEdge;
import easik.sketch.vertex.EntityNode;
import easik.ui.datamanip.UpdateMonitor;
//~--- JDK imports ------------------------------------------------------------

/**
 * Action used by the delete row popup menu item.
 */
public class DeleteRowAction extends AbstractAction {
	private static final long serialVersionUID = -9207665018959919191L;

	/** The sketch in which the table from which we are deleting exists. */
	private Sketch _theSketch;

	/**
	 *
	 *
	 * @param inSketch
	 */
	public DeleteRowAction(Sketch inSketch) {
		super("Delete row(s) from table...");

		_theSketch = inSketch;

		putValue(Action.SHORT_DESCRIPTION, "Display data in table to select and remove");
	}

	/**
	 * Create the new entity and set up its name
	 * 
	 * @param e The action event
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		Object[] currentSelection = _theSketch.getSelectionCells();
		Object selected = currentSelection[0];

		if (!(selected instanceof EntityNode)) {
			System.err.println("Action only available on entity nodes: easik.ui.menu.popup.DeleteRowAction");

			return;
		}

		EntityNode table = (EntityNode) selected;
		ArrayList<String> domains = new ArrayList<>();

		// warn user about possible cascades
		for (SketchEdge sk : _theSketch.getEdges().values()) {
			if (sk.getTargetEntity().getName().equals(table.getName()) && sk.getCascading() == Cascade.CASCADE) {
				domains.add(sk.getSourceEntity().getName());
			}
		}

		if (domains.size() > 0) {
			if (JOptionPane.showConfirmDialog(_theSketch,
					"Warning: Rows in this table may have foreign rows in " + domains.toString()
							+ " which will be deleted on cascade",
					"Warning", JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.WARNING_MESSAGE) == JOptionPane.CANCEL_OPTION) {
				return;
			}
		}

		UpdateMonitor um = _theSketch.getDatabase().newUpdateMonitor();

		um.deleteFrom(table);
	}
}
