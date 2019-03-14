package easik.ui.tree.popup;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;

import easik.graph.EasikGraphModel;
import easik.model.Model;
import easik.model.edge.ModelEdge;
import easik.model.keys.UniqueKey;
import easik.model.ui.ModelFrame;
import easik.model.vertex.ModelVertex;

/**
 * Delete unique key popup menu action for the information tree.
 *
 * @author Kevin Green 2006
 * @since 2006-06-14 Kevin Green
 * @version 2006-07-26 Kevin Green
 */
public class DeleteUniqueKeyAction<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>>
		extends AbstractAction {
	/**
	 *    
	 */
	private static final long serialVersionUID = 5014977747317916921L;

	/**  */
	F _theFrame;

	/**
	 * Set up the add attribute menu option.
	 *
	 * @param _theFrame2
	 */
	public DeleteUniqueKeyAction(F _theFrame2) {
		super("Delete Unique Key");

		_theFrame = _theFrame2;

		putValue(Action.SHORT_DESCRIPTION, "Deletes the currently selected unique key.");
	}

	/**
	 * Deletes the currently selected unique key
	 * 
	 * @param e The action event
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		// If there is nothing seleceted then just do nothing
		if (_theFrame.getInfoTreeUI().getInfoTree().isSelectionEmpty()) {
			return;
		}

		// If we're currently synced with a db, give the user the chance to
		// cancel operation
		if (_theFrame.getMModel().isSynced()) {
			int choice = JOptionPane.showConfirmDialog(_theFrame,
					"Warning: this sketch is currently synced with a db; continue and break synchronization?",
					"Warning!", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

			if (choice == JOptionPane.CANCEL_OPTION) {
				return;
			}
		}

		// Get currently selected object
		DefaultMutableTreeNode curSelected = (DefaultMutableTreeNode) _theFrame.getInfoTreeUI().getInfoTree()
				.getSelectionPath().getLastPathComponent();

		// Selection is a unique key
		if (curSelected instanceof UniqueKey) {
			@SuppressWarnings("unchecked")
			UniqueKey<F, GM, M, N, E> curKey = (UniqueKey<F, GM, M, N, E>) curSelected;
			// cast because we only have uniquekeys in sketches right now so
			// this is not generic
			N parentEntity = curKey.getEntity();

			// Show a confirmation dialog box for the deletion
			if (JOptionPane.showConfirmDialog(_theFrame,
					"Are you sure you want to delete the '" + curKey.toString() + "' unique key from the '"
							+ parentEntity + "' entity?",
					"Confirm Delete", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
				// Delete the unique key from entity
				parentEntity.removeUniqueKey(curKey);
				_theFrame.getInfoTreeUI().refreshTree(parentEntity); // Refresh
																		// view
																		// of
																		// entity
				_theFrame.getMModel().setDirty();
				_theFrame.getMModel().setSynced(false);
			}
		}

		// Selection is not an attribute
		else {
			JOptionPane.showMessageDialog(_theFrame,
					"You don't have a unique key selected. \nPlease select a unique key and try again.",
					"No Attribute Selected", JOptionPane.ERROR_MESSAGE);

			return;
		}
	}
}
