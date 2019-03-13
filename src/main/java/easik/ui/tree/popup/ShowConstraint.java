package easik.ui.tree.popup;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;

import easik.model.constraint.ModelConstraint;
import easik.sketch.Sketch;
import easik.sketch.edge.SketchEdge;
import easik.sketch.util.graph.SketchGraphModel;
import easik.sketch.vertex.EntityNode;
import easik.ui.SketchFrame;

/**
 * Show constraint menu action for the information tree.
 *
 * @author Kevin Green 2006
 * @since 2006-07-13 Kevin Green
 * @version 2006-07-13 Kevin Green
 */
public class ShowConstraint extends AbstractAction {
	/**
	 *    
	 */
	private static final long serialVersionUID = -9046311697804858962L;

	/**  */
	SketchFrame _theFrame;

	/**
	 * Set up the edit attribute menu option.
	 *
	 * @param inFrame
	 */
	public ShowConstraint(SketchFrame inFrame) {
		super("Show constraint");

		_theFrame = inFrame;

		putValue(Action.SHORT_DESCRIPTION, "Makes the selected contraint visible.");
	}

	/**
	 * Makes the selected constraint visible
	 * 
	 * @param e
	 *            The action event
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void actionPerformed(ActionEvent e) {
		// If there is nothing seleceted then just do nothing
		if (_theFrame.getInfoTreeUI().getInfoTree().isSelectionEmpty()) {
			return;
		}

		// Get currently selected object
		DefaultMutableTreeNode curSelected = (DefaultMutableTreeNode) _theFrame.getInfoTreeUI().getInfoTree().getSelectionPath().getLastPathComponent();

		// Selection is a constraint
		if (curSelected instanceof ModelConstraint) {
			((ModelConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge>) curSelected).setVisible(true);
		}

		// Selection is not a constraint
		else {
			JOptionPane.showMessageDialog(_theFrame, "You don't have a constraint selected. \nPlease select a constraint and try again.", "No ModelConstraint Selected", JOptionPane.ERROR_MESSAGE);
		}
	}
}
