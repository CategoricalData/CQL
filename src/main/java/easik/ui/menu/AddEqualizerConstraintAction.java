package easik.ui.menu;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import easik.model.states.AddEqualizerConstraintState;
import easik.ui.SketchFrame;

/**
 * Add a simple equalizer constraint menu option and action.
 */
public class AddEqualizerConstraintAction extends AbstractAction {
	/**
	 *    
	 */
	private static final long serialVersionUID = -7369258944023998601L;

	/**  */
	SketchFrame _theFrame;

	/**
	 * Creates and initializes the menu option.
	 *
	 * @param inFrame
	 */
	public AddEqualizerConstraintAction(SketchFrame inFrame) {
		super();

		_theFrame = inFrame;

		putValue(Action.NAME, "Add an Equalizer Constraint");
		putValue(Action.SHORT_DESCRIPTION, "Create a equalizer constraint from a set of paths");
	}

	/**
	 * Creates the equalizer if the selection is appropriate.
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
		 * AddEqualizerConstraintState(_theFrame.getMModel())); } else {
		 * JOptionPane.showMessageDialog(null, "Sketch cannot be empty.",
		 * "Error", JOptionPane.ERROR_MESSAGE); }
		 */

		_theFrame.getMModel().getStateManager().pushState(new AddEqualizerConstraintState<>(_theFrame.getMModel()));
	}
}
