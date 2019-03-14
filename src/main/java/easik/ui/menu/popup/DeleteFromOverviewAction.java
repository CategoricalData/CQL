package easik.ui.menu.popup;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import easik.overview.Overview;
import easik.overview.vertex.SketchNode;
import easik.overview.vertex.ViewNode;

/**
 * Action used to delete a node(s) from an overview.
 *
 * @author Rob Fletcher 2005
 */
public class DeleteFromOverviewAction extends AbstractAction {
	/**
	 *    
	 */
	private static final long serialVersionUID = 3820762854249160779L;

	/** The overview in which the delete action occurs */
	private Overview _theOverview;

	/**
	 * The delete button gets initialized
	 *
	 * @param inOverview
	 */
	public DeleteFromOverviewAction(Overview inOverview) {
		super("Delete");

		_theOverview = inOverview;

		putValue(Action.SHORT_DESCRIPTION, "Delete selection.");
	}

	/**
	 * When the action is performed, selection is deleted if possible. Error is
	 * displayed if no graph item is selected.
	 *
	 * @param e The action event
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		Object[] currentSelection = _theOverview.getSelectionCells();

		if (currentSelection.length != 1) {
			return;
		}

		Object cell = currentSelection[0];
		String confirm = "Are you sure you want to delete selected item?";

		if (cell instanceof SketchNode) {
			if (((SketchNode) cell).getMModel().isSynced()) {
				confirm = "Warning: this sketch is currently synced with a db; delete and break synchronization?";
			}

			if (JOptionPane.showConfirmDialog(_theOverview.getFrame(), confirm, "Warning!",
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.CANCEL_OPTION) {
				return;
			}

			_theOverview.removeSketch((SketchNode) cell);
		} else if (cell instanceof ViewNode) {
			if (((ViewNode) cell).getMModel().getSketch().isSynced()) {
				confirm = "Warning: this view is of a sketch that is currently synced with a db; delete and break synchronization?";
			}

			if (JOptionPane.showConfirmDialog(_theOverview.getFrame(), confirm, "Warning!",
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.CANCEL_OPTION) {
				return;
			}

			_theOverview.removeView((ViewNode) cell);
		}

		_theOverview.setDirty(true);
		_theOverview.clearSelection();
	}
}
