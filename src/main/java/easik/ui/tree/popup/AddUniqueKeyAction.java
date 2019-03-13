package easik.ui.tree.popup;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import easik.graph.EasikGraphModel;
import easik.model.Model;
import easik.model.edge.ModelEdge;
import easik.model.keys.UniqueKey;
import easik.model.keys.UniqueKeyUI;
import easik.model.ui.ModelFrame;
import easik.model.vertex.ModelVertex;

/**
 * Add unique key popup menu action for the information tree.
 *
 * @author Kevin Green 2006
 * @since 2006-06-14 Kevin Green
 * @version 2006-07-26 Kevin Green
 */
public class AddUniqueKeyAction<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>> extends AbstractAction {
	/**
	 *    
	 */
	private static final long serialVersionUID = 5691593062367015368L;

	/**  */
	private F _theFrame;

	/**
	 * Set up the add unqiue key menu option.
	 *
	 * @param _theFrame2
	 */
	public AddUniqueKeyAction(F _theFrame2) {
		super("Add Unique Key");

		_theFrame = _theFrame2;

		putValue(Action.SHORT_DESCRIPTION, "Add a unique key to the currently selected entity.");
	}

	/**
	 * Inserts a unique key to the currently selected entity
	 * 
	 * @param e
	 *            The action event
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void actionPerformed(ActionEvent e) {
		// If there is nothing selected then just do nothing
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
		N curEntity;

		// Check what is currently selected
		if (curSelected instanceof ModelVertex) {
			// Entity is selected so set it as current entity
			curEntity = (N) curSelected;
		} else {
			JOptionPane.showMessageDialog(_theFrame, "You have not selected an entity to add the unique key to.\nPlease select an entity and try again.", "No entity selected", JOptionPane.ERROR_MESSAGE);

			return;
		}

		// If the entity does not contain any attributes
		if (curEntity.getEntityAttributes().isEmpty() && curEntity.getIndexableEdges().isEmpty()) {
			// Pop up error dialog
			JOptionPane.showMessageDialog(_theFrame, "The selected entity has no attributes/edges.\nA unique key can only be added to an entity with\nattributes or outgoing, non-injective edges.", "Entity has no attributes/edges", JOptionPane.ERROR_MESSAGE);

			return;
		}

		UniqueKeyUI<F, GM, M, N, E> myUI = new UniqueKeyUI<>(_theFrame, curEntity);

		if (myUI.showDialog()) {
			UniqueKey<F, GM, M, N, E> newKey = new UniqueKey<>(curEntity, myUI.getSelectedElements(), myUI.getKeyName());

			// Create new unique key
			curEntity.addUniqueKey(newKey);
			_theFrame.getInfoTreeUI().refreshTree(curEntity); // Refresh view of
																// entity keys
			_theFrame.getMModel().clearSelection();
			_theFrame.getMModel().setDirty();
			_theFrame.getMModel().setSynced(false);
		}
	}
}
