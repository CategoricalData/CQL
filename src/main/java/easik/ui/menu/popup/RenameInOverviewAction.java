package easik.ui.menu.popup;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import easik.overview.Overview;
import easik.overview.vertex.OverviewVertex;
import easik.overview.vertex.SketchNode;
import easik.overview.vertex.ViewNode;

/**
 * Action for renaming a vertex in an overview
 *
 * @author Rob Fletcher 2005
 * @author Kevin Green 2006
 * @version 2006-07-26 Kevin Green
 */
public class RenameInOverviewAction extends AbstractAction {
	/**
	 *    
	 */
	private static final long serialVersionUID = 261018313044637670L;

	/** The overview in which the vertex gets renamed */
	private Overview _theOverview;

	/**
	 * Sets up rename action
	 *
	 * @param inOverview
	 */
	public RenameInOverviewAction(Overview inOverview) {
		super("Rename ...");

		_theOverview = inOverview;

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
	public void actionPerformed(ActionEvent e) {
		Object[] currentSelection = _theOverview.getSelectionCells();

		if (!((currentSelection.length == 1) && (currentSelection[0] instanceof OverviewVertex))) {
			return;
		}

		OverviewVertex nodeToRename = (OverviewVertex) currentSelection[0];
		String originalName = nodeToRename.getName();

		if (nodeToRename instanceof SketchNode) {
			if (((SketchNode) nodeToRename).getMModel().isSynced() && (JOptionPane.showConfirmDialog(_theOverview.getFrame(), "Warning: this sketch is currently synced with a db; continue and break synchronization?", "Warning!", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.CANCEL_OPTION)) {
				return;
			}
		} else if (nodeToRename instanceof ViewNode) {
			if (((ViewNode) nodeToRename).getMModel().getSketch().isSynced() && (JOptionPane.showConfirmDialog(_theOverview.getFrame(), "Warning: this view is of a sketch that is currently synced with a db; continue and break synchronization?", "Warning!", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.CANCEL_OPTION)) {
				return;
			}
		} else {
			return;
		}

		String s = (String) JOptionPane.showInputDialog(_theOverview.getParent(), "New name:", "Rename", JOptionPane.QUESTION_MESSAGE, null, null, originalName);

		if (s != null) {
			s = s.trim();

			if (s.equals("")) {
				JOptionPane.showMessageDialog(_theOverview.getParent(), "Sketch name is empty", "Error", JOptionPane.ERROR_MESSAGE);

				return;
			}

			if (nodeToRename instanceof SketchNode) {
				if (_theOverview.isNameUsed(s) && !nodeToRename.getName().equals(s)) {
					JOptionPane.showMessageDialog(_theOverview.getParent(), "Sketch name is already in use", "Error", JOptionPane.ERROR_MESSAGE);

					return;
				}
			} else if (nodeToRename instanceof ViewNode) {
				if (_theOverview.isNameUsed(s) && !nodeToRename.getName().equals(s)) {
					JOptionPane.showMessageDialog(_theOverview.getParent(), "View name is already in use", "Error", JOptionPane.ERROR_MESSAGE);

					return;
				}
			}

			if (!s.equals(originalName)) {
				nodeToRename.setName(s);
				_theOverview.getFrame().getInfoTreeUI().storeExpansion();
				_theOverview.getFrame().getInfoTreeUI().refreshTree();
				_theOverview.getGraphLayoutCache().reload();
				_theOverview.getFrame().getInfoTreeUI().revertExpansion();

				if (nodeToRename instanceof SketchNode) {
					((SketchNode) nodeToRename).getFrame().getMModel().setDirty();
					((SketchNode) nodeToRename).getFrame().getMModel().setSynced(false);
				} else if (nodeToRename instanceof ViewNode) {
					((ViewNode) nodeToRename).getFrame().getMModel().setDirty();
					((ViewNode) nodeToRename).getFrame().getMModel().getSketch().setSynced(false);
				} else {
					_theOverview.setDirty(true);
				}

				_theOverview.repaint();
			}
		}

		_theOverview.clearSelection();
	}
}
