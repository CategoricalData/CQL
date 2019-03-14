package easik.ui.tree.popup;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;

import easik.graph.EasikGraphModel;
import easik.model.Model;
import easik.model.edge.ModelEdge;
import easik.model.ui.ModelFrame;
import easik.model.vertex.ModelVertex;

/**
 * Action for the delete entity option in the popup menu.
 *
 * @author Kevin Green 2006
 * @since 2006-08-02 Kevin Green
 * @version 2006-08-02 Kevin Green
 */
public class DeleteEntityAction<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>>
		extends AbstractAction {
	/**
	 *    
	 */
	private static final long serialVersionUID = -3794000562274937471L;

	/**  */
	F _theFrame;

	/**
	 * Sets up delete entity action
	 *
	 * @param _theFrame2
	 */
	public DeleteEntityAction(F _theFrame2) {
		super("Delete Entity");

		_theFrame = _theFrame2;

		putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_D));
		putValue(Action.SHORT_DESCRIPTION, "Deletes the currently selected entity");
	}

	/**
	 * Called when clicked upon, will delete entity.
	 *
	 * @param e The action event
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void actionPerformed(ActionEvent e) {
		// If there is nothing seleceted then just do nothing
		if (_theFrame.getInfoTreeUI().getInfoTree().isSelectionEmpty()) {
			return;
		}

		// Get M object
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

		// Get currently selected object
		DefaultMutableTreeNode curSelected = (DefaultMutableTreeNode) _theFrame.getInfoTreeUI().getInfoTree()
				.getSelectionPath().getLastPathComponent();

		if (curSelected instanceof ModelVertex) {
			_theFrame.getMModel().removeNode((N) curSelected);
			_theFrame.getMModel().setDirty();
			_theFrame.getMModel().setSynced(false);
		}
	}
}
