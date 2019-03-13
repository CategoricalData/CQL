package easik.ui.menu.popup;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import easik.sketch.Sketch;
import easik.sketch.edge.InjectiveEdge;
import easik.sketch.edge.NormalEdge;
import easik.sketch.edge.PartialEdge;
import easik.sketch.edge.SketchEdge;
import easik.sketch.vertex.EntityNode;
import easik.ui.SketchFrame;
import easik.ui.menu.popup.EdgeOptions.Edge;

/**
 * This class contains the action for the click on the popup menu to create an
 * new edge in a sketch: it can either be instantiated to create a NormalEdge,
 * InjectiveEdge, PartialEdge, or a self-referencing partial edge
 * (<code>Edge.NORMAL</code>, <code>Edge.INJECTIVE</code>,
 * <code>Edge.PARTIAL</code>, and <code>Edge.SELF</code>, respectively).
 */
public class NewSketchEdgeAction extends AbstractAction {
	/**
	 *    
	 */
	private static final long serialVersionUID = 6386511727462714952L;

	/** The current edge type being added */
	private Edge _edgeType;

	/**
	 * The sketch frame in wich the new edge will be displayed.
	 */
	private SketchFrame _theFrame;

	/**
	 * Prepare the menu option for a new edge
	 *
	 * @param inFrame
	 * @param type
	 */
	public NewSketchEdgeAction(SketchFrame inFrame, Edge type) {
		super("Add " + ((type == Edge.INJECTIVE) ? "injective " : (type == Edge.PARTIAL) ? "partial " : (type == Edge.SELF) ? "self-referencing " : "") + "edge...");

		_theFrame = inFrame;
		_edgeType = type;

		putValue(Action.SHORT_DESCRIPTION, (type == Edge.SELF) ? "Adds a self-referencing partial map edge from the selected node back to itself" : (type == Edge.PARTIAL) ? "Connect the two selected nodes with a partial map edge" : (type == Edge.INJECTIVE) ? "Connect the two selected nodes with an injective edge" : "Connect the two selected nodes with an edge");
	}

	/**
	 * The action for creating a new edge. Make sure the selection is alright,
	 * and then create the edge.
	 * 
	 * @param e
	 *            The action event
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		Sketch _ourSketch = _theFrame.getMModel();

		// If we're currently synced with a db, give the user the chance to
		// cancel operation
		if (_ourSketch.isSynced()) {
			if (JOptionPane.showConfirmDialog(_theFrame, "Warning: this sketch is currently synced with a db; continue and break synchronization?", "Warning!", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.CANCEL_OPTION) {
				return;
			}
		}

		Object[] currentSelection = _ourSketch.getSelectionCells();
		EntityNode node[] = new EntityNode[(_edgeType == Edge.SELF) ? 1 : 2];

		if (currentSelection.length == node.length) {
			for (int i = 0; i < node.length; i++) {
				node[i] = null;

				if (currentSelection[i] instanceof EntityNode) {
					node[i] = (EntityNode) currentSelection[i];
				}
			}
		}

		if ((_edgeType == Edge.SELF) && (node[0] == null)) {
			JOptionPane.showMessageDialog(_theFrame, "Operation must be performed with one entity selected", "Error", JOptionPane.ERROR_MESSAGE);
		} else if ((_edgeType != Edge.SELF) && ((node[0] == null) || (node[1] == null))) {
			JOptionPane.showMessageDialog(_theFrame, "Operation must be performed with two entities selected", "Error", JOptionPane.ERROR_MESSAGE);
		} else {
			EdgeOptions opts = new EdgeOptions(_theFrame, _edgeType, node[0], (_edgeType == Edge.SELF) ? null : node[1]);

			if (opts.isAccepted()) {
				SketchEdge edge;

				if (_edgeType == Edge.SELF) {
					edge = new PartialEdge(node[0], node[0], opts.getName(), opts.getCascadeMode());
				} else {
					// Swap the node positions if the user reversed them in the
					// UI:
					if (opts.isReversed()) {
						node = new EntityNode[] { node[1], node[0] };
					}

					if (_edgeType == Edge.PARTIAL) {
						edge = new PartialEdge(node[0], node[1], opts.getName(), opts.getCascadeMode());
					} else if (_edgeType == Edge.INJECTIVE) {
						edge = new InjectiveEdge(node[0], node[1], opts.getName(), opts.getCascadeMode());
					} else {
						edge = new NormalEdge(node[0], node[1], opts.getName(), opts.getCascadeMode());
					}
				}

				_ourSketch.addEdge(edge);
				_ourSketch.setDirty();
				_ourSketch.setSynced(false);
			}
		}
	}
}
