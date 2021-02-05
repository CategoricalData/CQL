package easik.ui.menu.popup;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import easik.overview.Overview;
import easik.overview.vertex.SketchNode;

/**
 * Action for the new view option in the popup menu.
 */
public class NewViewAction extends AbstractAction {
	/**
	 *    
	 */
	private static final long serialVersionUID = -9168776264827045572L;

	/** The overview in which the new view will be placed */
	private Overview _theOverview;

	/**
	 * Prepare the menu option.
	 *
	 * @param inOverview
	 */
	public NewViewAction(Overview inOverview) {
		super("Add view...");

		_theOverview = inOverview;

		putValue(Action.SHORT_DESCRIPTION, "Add a new view on selected sketch");
	}

	/**
	 * Get new name and send off to overview for creation. The frame where this
	 * action becomes available to the user is responsible for ensuring that the
	 * current selection is valid for creating the view (i.e. That it consists of
	 * exactly one sketch node.)
	 * 
	 * @param e The action event
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		String newName = _theOverview.getNewViewName();

		newName = (String) JOptionPane.showInputDialog(_theOverview.getFrame(), "Name for new view:", "Get name",
				JOptionPane.QUESTION_MESSAGE, null, null, newName);

		if (newName == null) {
			return;
		}

		newName = newName.trim();

		while (newName.equals("") || _theOverview.isNameUsed(newName)) {
			JOptionPane.showMessageDialog(_theOverview.getFrame(),
					"Error while naming view.\n" + "Please ensure that view name is not blank and not already in use\n",
					"Error", JOptionPane.ERROR_MESSAGE);

			newName = (String) JOptionPane.showInputDialog(_theOverview.getFrame(), "Name for new view:", "Get name",
					JOptionPane.QUESTION_MESSAGE, null, null, newName);

			if (newName == null) {
				return;
			}

			newName = newName.trim();
		}

		// get selected node and add view
		Object[] selection = _theOverview.getSelectionCells();

		_theOverview.addNewView((SketchNode) selection[0], newName);
		_theOverview.setDirty(true);
	}
}
