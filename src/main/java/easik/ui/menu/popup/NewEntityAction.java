package easik.ui.menu.popup;

//~--- non-JDK imports --------------------------------------------------------

import java.awt.Point;
//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import easik.graph.EasikGraphModel;
import easik.model.Model;
import easik.model.edge.ModelEdge;
import easik.model.ui.ModelFrame;
import easik.model.vertex.ModelVertex;

/**
 * Action for the new entity option in the popup menu.
 *
 * @author Rob Fletcher 2005
 */
public class NewEntityAction<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>>
		extends AbstractAction {
	/**
	 *    
	 */
	private static final long serialVersionUID = -123110465158533398L;

	/** The point at which to place the new entity */
	Point _newPoint;

	/** The frame in which the new entity will be displayed. */
	F _theFrame;

	/**
	 * Prepare the menu option, as well as pass a reference to the last clicked
	 * point, which is used when positioning the new entity.
	 *
	 * @param inPoint    The sketch's last-rightclicked-position
	 * @param _theFrame2
	 */
	public NewEntityAction(Point inPoint, F _theFrame2) {
		super("Add entity...");

		_theFrame = _theFrame2;

		putValue(Action.SHORT_DESCRIPTION, "Add a new entity to the sketch");

		_newPoint = inPoint;
	}

	/**
	 * Create the new entity and set up its name
	 * 
	 * @param e The action event
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		M _ourModel = _theFrame.getMModel();

		// If we're currently synced with a db, give the user the chance to
		// cancel operation
		if (_ourModel.isSynced()) {
			int choice = JOptionPane.showConfirmDialog(_theFrame,
					"Warning: this sketch is currently synced with a db; continue and break synchronization?",
					"Warning!", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

			if (choice == JOptionPane.CANCEL_OPTION) {
				return;
			}
		}

		String newName = _ourModel.getNewName();

		newName = (String) JOptionPane.showInputDialog(_theFrame, "Name for new entity:", "Get name",
				JOptionPane.QUESTION_MESSAGE, null, null, newName);

		if ((newName == null) || (newName.trim() == null)) {
			return;
		}

		newName = newName.trim();

		while (newName.equals("") || _theFrame.getMModel().isNameUsed(newName)) {
			JOptionPane.showMessageDialog(_theFrame, "Error while naming entity.\n"
					+ "Please ensure that entity name is:\n" + "1) Not blank\n" + "2) Not already in use", "Error",
					JOptionPane.ERROR_MESSAGE);

			newName = (String) JOptionPane.showInputDialog(_theFrame, "Name for new entity:", "Get name",
					JOptionPane.QUESTION_MESSAGE, null, null, newName);

			if (newName == null) {
				return;
			}

			newName = newName.trim();
		}

		_ourModel.getFrame().getInfoTreeUI().storeExpansion();

		Point p = (_newPoint != null) ? _newPoint : _ourModel.getNewPosition(10);

		_ourModel.addNewNode(newName, p.getX(), p.getY());
		_ourModel.setDirty();
		_ourModel.setSynced(false);
		_ourModel.getFrame().getInfoTreeUI().revertExpansion();
	}
}
