package easik.ui.tree.popup;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;

import easik.overview.vertex.SketchNode;
import easik.overview.vertex.ViewNode;
import easik.ui.ApplicationFrame;

/**
 * Action for the delete entity option in the popup menu.
 *
 * @author Kevin Green 2006
 * @since 2006-08-02 Kevin Green
 * @version 2006-08-02 Kevin Green
 */
public class DeleteFromOverviewFromTreeAction extends AbstractAction {
	/**
	 *    
	 */
	private static final long serialVersionUID = -4161499877634434395L;

	/**  */
	ApplicationFrame _theFrame;

	/**
	 * Sets up delete entity action
	 *
	 * @param inFrame
	 * @param label
	 */
	public DeleteFromOverviewFromTreeAction(ApplicationFrame inFrame, String label) {
		super(label);

		_theFrame = inFrame;

		putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_D));
		putValue(Action.SHORT_DESCRIPTION, "Deletes the currently selected entity");
	}

	/**
	 * Called when clicked upon, will delete entity.
	 *
	 * @param e The action event
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		// If there is nothing seleceted then just do nothing
		if (_theFrame.getInfoTreeUI().getInfoTree().isSelectionEmpty()) {
			return;
		}

		int op = JOptionPane.showConfirmDialog(_theFrame, "Are you sure you want to delete the selected items?",
				"Confirm Deletion", JOptionPane.YES_NO_OPTION);

		if (op == JOptionPane.YES_OPTION) {
			// Get currently selected object
			DefaultMutableTreeNode curSelected = (DefaultMutableTreeNode) _theFrame.getInfoTreeUI().getInfoTree()
					.getSelectionPath().getLastPathComponent();

			if (curSelected.getUserObject() instanceof SketchNode) {
				_theFrame.getOverview().removeSketch((SketchNode) curSelected.getUserObject());
				_theFrame.getOverview().setDirty(true);
			} else {
				_theFrame.getOverview().removeView((ViewNode) curSelected.getUserObject());
				_theFrame.getOverview().setDirty(true);
			}
		}
	}
}
