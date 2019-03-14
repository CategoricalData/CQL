package easik.ui.menu;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import easik.graph.EasikGraphModel;
import easik.model.Model;
import easik.model.edge.ModelEdge;
import easik.model.states.AddSumConstraintState;
import easik.model.ui.ModelFrame;
import easik.model.vertex.ModelVertex;

/**
 * The menu action which creates simple sum constraints.
 *
 * @author Rob Fletcher 2005
 * @author Kevin Green 2006
 * @version 2006-05-23 Kevin Green
 */
public class AddSumConstraintAction<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>>
		extends AbstractAction {
	/**
	 *    
	 */
	private static final long serialVersionUID = 1564034330451404278L;

	/**  */
	F _theFrame;

	/**
	 * Creates and initializes the menu option.
	 *
	 *
	 * @param _theFrame2
	 */
	public AddSumConstraintAction(F _theFrame2) {
		super();

		_theFrame = _theFrame2;

		putValue(Action.NAME, "Add a Sum Constraint");
		putValue(Action.SHORT_DESCRIPTION, "Assign a sum constraint to a set of injective paths");
	}

	/**
	 * Creates a sum constraint if the selection allows that.
	 *
	 * @param e The action event
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		/*
		 * sketches can now be empty since we can add as we go if
		 * (!_theFrame.getMModel().getEntities().isEmpty()) {
		 * _theFrame.getMModel().getStateManager().pushState(new
		 * AddSumConstraintState(_theFrame.getMModel())); } else {
		 * JOptionPane.showMessageDialog(null, "Sketch cannot be empty.", "Error",
		 * JOptionPane.ERROR_MESSAGE); }
		 */

		_theFrame.getMModel().getStateManager().pushState(new AddSumConstraintState<>(_theFrame.getMModel()));

	}
}
