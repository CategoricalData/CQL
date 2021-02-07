package easik.ui.menu.popup;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import easik.sketch.Sketch;
import easik.sketch.vertex.EntityNode;
import easik.ui.ViewFrame;
import easik.ui.datamanip.UpdateMonitor;
import easik.view.View;
import easik.view.vertex.QueryNode;

/**
 * 
 * @author Sarah van der Laan 2013
 *
 */

public class ViewDeleteAction extends AbstractAction {

	private static final long serialVersionUID = -7358840132662817458L;

	/** The view in which we are working */
	private View _ourView;

	/** The sketch in which the current view belongs to */
	private Sketch _ourSketch;

	/** The view frame */
	@SuppressWarnings("unused")
	private ViewFrame _ourViewFrame;

	/**
	 * Prepare the menu option.
	 *
	 * @param inView
	 */
	public ViewDeleteAction(View inView) {
		// should it say this??
		super("Delete row from view table...");

		_ourView = inView;
		_ourSketch = _ourView.getSketch();
		_ourViewFrame = _ourView.getFrame();

		putValue(Action.SHORT_DESCRIPTION, "Delete a row to the view table referenced by this query node.");
	}

	/**
	 * @param e The action event
	 * @author Sarah van der Laan
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		Object[] currentSelection = _ourView.getSelectionCells();

		QueryNode currNode = (QueryNode) currentSelection[0];

		String queryString = currNode.getQuery();
		String entityNodeName = null;

		// find corresponding entity node name
		String[] tokens = queryString.split("\\s+");
		for (int i = 0; i < tokens.length; i++) {
			if (tokens[i].equalsIgnoreCase("from")) {
				entityNodeName = tokens[i + 1];
			}
		}

		EntityNode _ourEntityNode = null;
		// set corresponding node in order to use
		for (EntityNode node : _ourSketch.getEntities()) {
			if (node.getName().equalsIgnoreCase(entityNodeName)) {
				_ourEntityNode = node;

			}
		}

		if (!_ourSketch.hasDatabase()) {
			JOptionPane.showMessageDialog(null, "Not currently connected to a database.");
			return;
		}

		UpdateMonitor um = _ourSketch.getDatabase().newUpdateMonitor();

		if (um == null) {
			JOptionPane.showMessageDialog(null, "Could not perform update: problem accessing db driver");

			return;
		}

		if (_ourEntityNode != null)
			um.deleteFrom(_ourEntityNode);

	}
}
