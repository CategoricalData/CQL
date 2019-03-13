package easik.ui.tree.popup;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import easik.graph.EasikGraphModel;
import easik.model.Model;
import easik.model.constraint.CommutativeDiagram;
import easik.model.constraint.ProductConstraint;
import easik.model.constraint.SumConstraint;
import easik.model.edge.ModelEdge;
import easik.model.states.AddCommutativeDiagramState;
import easik.model.states.AddProductConstraintState;
import easik.model.states.AddSumConstraintState;
import easik.model.ui.ModelFrame;
import easik.model.vertex.ModelVertex;

/**
 * Show constraint menu action for the information tree.
 *
 * @author Kevin Green 2006
 * @since 2006-08-17 Kevin Green
 * @version 2006-08-17 Kevin Green
 */
public class AddPathAction<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>> extends AbstractAction {
	/**
	 *    
	 */
	private static final long serialVersionUID = 3328275613576541107L;

	/**  */
	F _theFrame;

	/**
	 * Set up the add path menu option.
	 *
	 * @param _theFrame2
	 */
	public AddPathAction(F _theFrame2) {
		super("Add Path(s) to Constraint");

		_theFrame = _theFrame2;

		putValue(Action.SHORT_DESCRIPTION, "Adds a path to the currently selected constraint.");
	}

	/**
	 * Pushes the add path state
	 *
	 * @param e
	 *            The action event
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void actionPerformed(ActionEvent e) {
		// If there is nothing seleceted or too many things selected then just
		// do nothing
		if (_theFrame.getMModel().getSelectionCells().length != 1) {
			return;
		}

		// If we're currently synced with a db, give the user the chance to
		// cancel operation
		if (_theFrame.getMModel().isSynced()) {
			int choice = JOptionPane.showConfirmDialog(_theFrame, "Warning: this sketch is currently synced with a db; continue and break synchronization?", "Warning!", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

			if (choice == JOptionPane.CANCEL_OPTION) {
				return;
			}
		}

		// Get currently selected object
		Object curSelected = _theFrame.getMModel().getSelectionCells()[0];

		// Selection is a constraint that can have a path added
		if (curSelected instanceof SumConstraint) {
			_theFrame.getMModel().getStateManager().pushState(new AddSumConstraintState<>(_theFrame.getMModel(), (SumConstraint<F, GM, M, N, E>) curSelected));
		} else if (curSelected instanceof CommutativeDiagram) {
			_theFrame.getMModel().getStateManager().pushState(new AddCommutativeDiagramState<>(_theFrame.getMModel(), (CommutativeDiagram<F, GM, M, N, E>) curSelected));
		} else if (curSelected instanceof ProductConstraint) {
			_theFrame.getMModel().getStateManager().pushState(new AddProductConstraintState<>(_theFrame.getMModel(), (ProductConstraint<F, GM, M, N, E>) curSelected));
		}

		// Selection is not a valid constraint type
		else {
			JOptionPane.showMessageDialog(_theFrame, "You don't have a sum, product, or commutative diagram constraint selected.\nPlease select a sum, product, or commutative diagram constraint and try again.", "No Constraint Selected", JOptionPane.ERROR_MESSAGE);
		}
	}
}
