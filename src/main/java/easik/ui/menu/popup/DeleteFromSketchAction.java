package easik.ui.menu.popup;

//~--- non-JDK imports --------------------------------------------------------

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import easik.model.constraint.ModelConstraint;
import easik.overview.vertex.ViewNode;
import easik.sketch.Sketch;
import easik.sketch.edge.SketchEdge;
import easik.sketch.util.graph.SketchGraphModel;
import easik.sketch.vertex.EntityNode;
import easik.ui.SketchFrame;
//~--- JDK imports ------------------------------------------------------------

/**
 * Action used to delete a node(s) from a sketch.
 */
public class DeleteFromSketchAction extends AbstractAction {
	/**
	 *    
	 */
	private static final long serialVersionUID = -2082503996552182674L;

	/** The sketch frame in which the deleting occurs */
	SketchFrame _theFrame;

	/**
	 * The delete button gets initialized
	 *
	 * @param inFrame
	 */
	public DeleteFromSketchAction(SketchFrame inFrame) {
		super("Delete");

		_theFrame = inFrame;

		putValue(Action.SHORT_DESCRIPTION, "Delete selection.");
	}

	/**
	 * When the action is performed, selection is deleted if possible. Error is
	 * displayed if no graph item is selected.
	 *
	 * @param e The action event
	 */
	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void actionPerformed(ActionEvent e) {
		Sketch _ourSketch = _theFrame.getMModel();

		// The confirm delete message. If we're currently synced with a db, add
		// that to the message;
		String confirm = _ourSketch.isSynced()
				? "Warning: this sketch is currently synced with a db; delete and break synchronization?"
				: "Are you sure you want to delete selected item(s)?";

		if (JOptionPane.showConfirmDialog(_theFrame, confirm, "Warning!", JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.WARNING_MESSAGE) == JOptionPane.CANCEL_OPTION) {
			return;
		}

		Object[] currentSelection = _ourSketch.getSelectionCells();

		if (currentSelection.length == 0) {
			JOptionPane.showMessageDialog(_theFrame, "Operation must be performed with something selected", "Error",
					JOptionPane.ERROR_MESSAGE);
		} else {
			_ourSketch.getGraphModel().beginUpdate();

			// Delete constraints, edges, and entities in that order,
			// to avoid deletion of an entity causing deletion of edges or
			// constraints
			// and thereby causing a problem trying to remove something that is
			// already removed.
			// First, delete any constraints:
			for (Object o : currentSelection) {
				if (o instanceof ModelConstraint) {
					_ourSketch.removeConstraint(
							(ModelConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge>) o);
					for (ViewNode v : _ourSketch.getViews()) {
						if (v.getMModel().getConstraints().containsKey(
								((ModelConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge>) o)
										.getID())) {
							v.getMModel().removeConstraint(((ModelConstraint) o));
						}
					}
				}
			}

			// THEN any edges:
			for (Object o : currentSelection) {
				if (o instanceof SketchEdge) {
					for (ViewNode v : _theFrame.getMModel().getViews()) {
						if (v.getMModel().getEdges().containsKey(((SketchEdge) o).getName())) {
							// put up a warning cause this exists in a View
							if (JOptionPane.showConfirmDialog(_theFrame,
									"SketchEdge " + ((SketchEdge) o).getName()
											+ " exists in a View. Continue and delete in view as well?",
									"Warning!", JOptionPane.OK_CANCEL_OPTION,
									JOptionPane.WARNING_MESSAGE) == JOptionPane.CANCEL_OPTION) {
								return;
							}
							// if we want to proceed and delete it...
							v.getMModel().removeEdge(v.getMModel().getEdges().get(((SketchEdge) o).getName()));
						}
					}
					_ourSketch.removeEdge((SketchEdge) o);
				}
			}

			// Then finally, any entities.
			for (Object o : currentSelection) {
				if (o instanceof EntityNode) {
					for (ViewNode v : _theFrame.getMModel().getViews()) {
						if (v.getMModel().getEntityNodePairs().containsKey((o))) {
							// put up a warning cause this exists in a View
							if (JOptionPane.showConfirmDialog(_theFrame,
									"EntityNode " + ((EntityNode) o).getName()
											+ " is being queried by a View. Continue and delete in view as well?",
									"Warning!", JOptionPane.OK_CANCEL_OPTION,
									JOptionPane.WARNING_MESSAGE) == JOptionPane.CANCEL_OPTION) {
								return;
							}
							// if we want to proceed and delete it...
							v.getMModel().removeNode(v.getMModel().getEntityNodePairs().get(o));
						}
					}
					_ourSketch.removeNode((EntityNode) o);
				}
			}

			_ourSketch.setDirty();
			_ourSketch.getGraphModel().endUpdate();
			_ourSketch.setSynced(false);
		}

		// Clear selection after things have been deleted
		_ourSketch.clearSelection();
	}
}
