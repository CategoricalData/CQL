package easik.ui.menu;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import easik.graph.EasikGraphModel;
import easik.model.Model;
import easik.model.edge.ModelEdge;
import easik.model.states.AddCommutativeDiagramState;
import easik.model.ui.ModelFrame;
import easik.model.vertex.ModelVertex;

/**
 * Menu action for adding commutative diagrams.
 *
 * @author Rob Fletcher 2005
 * @author Kevin Green 2006
 * @version 2006-05-23 Kevin Green
 */
public class AddCommutativeDiagramAction<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>> extends AbstractAction {
	/*
	 * The Sketch Frame to which this state is referring
	 */

	/**
	 *    
	 */
	private static final long serialVersionUID = 9059956219227674248L;

	/**  */
	private F _theFrame;

	/**
	 * Create the new action and set the name and description
	 *
	 * @param _theFrame2
	 */
	public AddCommutativeDiagramAction(F _theFrame2) {
		super();

		_theFrame = _theFrame2;

		putValue(Action.NAME, "Add a Commutative Diagram");
		putValue(Action.SHORT_DESCRIPTION, "Add a commutative diagram to sketch");
	}

	/**
	 * When action is performed, attempt to create a new commutative diagram.
	 *
	 * @param e
	 *            The action event
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		/*
		 * sketches can now be empty since we can add as we go if
		 * (!_theFrame.getMModel().getEntities().isEmpty()) {
		 * _theFrame.getMModel().getStateManager().pushState(new
		 * AddCommutativeDiagramState(_theFrame.getMModel())); } else {
		 * JOptionPane.showMessageDialog(null, "Sketch cannot be empty.",
		 * "Error", JOptionPane.ERROR_MESSAGE); }
		 */

		_theFrame.getMModel().getStateManager().pushState(new AddCommutativeDiagramState<>(_theFrame.getMModel()));
	}
}
