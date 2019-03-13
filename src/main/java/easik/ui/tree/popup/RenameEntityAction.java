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
import easik.model.states.LoadingState;
import easik.model.ui.ModelFrame;
import easik.model.vertex.ModelVertex;

/**
 * Action for the rename entity option in the popup menu.
 *
 * @author Kevin Green 2006
 * @version 2006-07-31 Kevin Green
 */
public class RenameEntityAction<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>> extends AbstractAction {
	/**
	 *    
	 */
	private static final long serialVersionUID = -3535374412791180412L;

	/**  */
	F _theFrame;

	/**
	 * Sets up rename entity action
	 *
	 * @param _theFrame2
	 */
	public RenameEntityAction(F _theFrame2) {
		super("Rename Entity");

		_theFrame = _theFrame2;

		putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_R));
		putValue(Action.SHORT_DESCRIPTION, "Change the name of selection");
	}

	/**
	 * Called when clicked upon, will rename an article.
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

		// Get M object
		M _ourModel = _theFrame.getMModel();

		// If we're currently synced with a db, give the user the chance to
		// cancel operation
		if (_ourModel.isSynced()) {
			int choice = JOptionPane.showConfirmDialog(_theFrame, "Warning: this sketch is currently synced with a db; continue and break synchronization?", "Warning!", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

			if (choice == JOptionPane.CANCEL_OPTION) {
				return;
			}
		}

		// Get currently selected object
		DefaultMutableTreeNode curSelected = (DefaultMutableTreeNode) _theFrame.getInfoTreeUI().getInfoTree().getSelectionPath().getLastPathComponent();
		N nodeToRename;
		String originalName = "";

		// Check what is currently selected
		if (curSelected instanceof ModelVertex) {
			nodeToRename = (N) curSelected;
			originalName = nodeToRename.getName();
		} else {
			return;
		}

		String s = (String) JOptionPane.showInputDialog(_ourModel.getParent(), "New name:", "Rename", JOptionPane.QUESTION_MESSAGE, null, null, originalName);

		if (s != null) {
			s = s.trim();

			if (s.equals("")) {
				JOptionPane.showMessageDialog(_theFrame, "Entity name is empty", "Error", JOptionPane.ERROR_MESSAGE);
			} else if (_ourModel.isNameUsed(s)) {
				JOptionPane.showMessageDialog(_theFrame, "Entity name is already in use", "Error", JOptionPane.ERROR_MESSAGE);
			} else {
				// Push loading state
				_ourModel.getStateManager().pushState(new LoadingState<>(_ourModel));
				nodeToRename.setName(s);
				_theFrame.getInfoTreeUI().refreshTree();
				_theFrame.getMModel().getGraphLayoutCache().reload();

				// Pop state
				_ourModel.getStateManager().popState();
				_ourModel.repaint();
				_ourModel.setDirty();
				_ourModel.setSynced(false);
			}
		}

		_ourModel.clearSelection();
	}
}
