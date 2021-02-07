package easik.ui.tree.popup;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;

import easik.overview.vertex.OverviewVertex;
import easik.overview.vertex.SketchNode;
import easik.overview.vertex.ViewNode;
import easik.ui.ApplicationFrame;

/**
 * Action for the rename entity option in the popup menu.
 *
 * @author Kevin Green 2006
 * @version 2006-07-31 Kevin Green
 */
public class RenameInOverviewFromTreeAction extends AbstractAction {
	/**
	 *    
	 */
	private static final long serialVersionUID = 5008245517707229733L;

	/**  */
	ApplicationFrame _theFrame;

	/**
	 * Sets up rename entity action
	 *
	 * @param inFrame
	 * @param label
	 */
	public RenameInOverviewFromTreeAction(ApplicationFrame inFrame, String label) {
		super(label);

		_theFrame = inFrame;

		putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_R));
		putValue(Action.SHORT_DESCRIPTION, "Change the name of selection");
	}

	/**
	 * Called when clicked upon, will rename an article.
	 *
	 * @param e The action event
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		// If there is nothing seleceted then just do nothing
		if (_theFrame.getInfoTreeUI().getInfoTree().isSelectionEmpty()) {
			System.err.println("'OK'");

			return;
		}

		// Get currently selected object
		DefaultMutableTreeNode curSelected = (DefaultMutableTreeNode) _theFrame.getInfoTreeUI().getInfoTree()
				.getSelectionPath().getLastPathComponent();
		OverviewVertex nodeToRename;
		String originalName = "";

		// Check what is currently selected
		if (curSelected.getUserObject() instanceof SketchNode) {
			nodeToRename = (SketchNode) curSelected.getUserObject();
		} else if (curSelected.getUserObject() instanceof ViewNode) {
			nodeToRename = (ViewNode) curSelected.getUserObject();
		} else {
			return;
		}

		originalName = nodeToRename.getName();

		String s = (String) JOptionPane.showInputDialog(_theFrame, "New name:", "Rename", JOptionPane.QUESTION_MESSAGE,
				null, null, originalName);

		if (s != null) {
			s = s.trim();

			if (s.equals("")) {
				JOptionPane.showMessageDialog(_theFrame, "Entity name is empty", "Error", JOptionPane.ERROR_MESSAGE);
			} else if (_theFrame.getOverview().isNameUsed(s)) {
				JOptionPane.showMessageDialog(_theFrame, "Entity name is already in use", "Error",
						JOptionPane.ERROR_MESSAGE);
			} else {
				nodeToRename.setName(s);
				_theFrame.getInfoTreeUI().refreshTree();
				_theFrame.getOverview().getGraphLayoutCache().reload();
				_theFrame.getOverview().repaint();

				if (nodeToRename instanceof SketchNode) {
					((SketchNode) nodeToRename).getFrame().getMModel().setDirty();
				} else if (nodeToRename instanceof ViewNode) {
					((ViewNode) nodeToRename).getFrame().getMModel().setDirty();
				}
			}
		}

		_theFrame.getOverview().clearSelection();
	}
}
