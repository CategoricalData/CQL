package easik.ui.menu.popup;

//~--- non-JDK imports --------------------------------------------------------

import java.awt.Point;
//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import easik.overview.Overview;

/**
 * Action for the new sketch option in the popup menu.
 *
 * @author Rob Fletcher 2005
 */
public class NewSketchAction extends AbstractAction {
	/**
	 *    
	 */
	private static final long serialVersionUID = -5398330495995427306L;

	/** The point on the overview at which to place the new sketch */
	private Point _newPoint;

	/** The overview in which the new sketch will be placed */
	private Overview _theOverview;

	/**
	 * Prepare the menu option, as well as pass a reference to the last clicked
	 * point, which is used when positioning the new sketch. If set to null, picks a
	 * random point to position sketch node.
	 *
	 * @param inPoint    The sketch's last-rightclicked-position
	 * @param inOverview
	 */
	public NewSketchAction(Point inPoint, Overview inOverview) {
		super("Add sketch...");

		_theOverview = inOverview;

		putValue(Action.SHORT_DESCRIPTION, "Add a new sketch to the overview");

		_newPoint = inPoint;
	}

	/**
	 * Create the new sketch and set up its name
	 * 
	 * @param e The action event
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		String newName = _theOverview.getNewSketchName();

		newName = (String) JOptionPane.showInputDialog(_theOverview.getFrame(), "Name for new sketch:", "Get name",
				JOptionPane.QUESTION_MESSAGE, null, null, newName);

		if (newName == null) {
			return;
		}

		newName = newName.trim();

		while (newName.equals("") || _theOverview.isNameUsed(newName)) {
			JOptionPane.showMessageDialog(_theOverview.getFrame(),
					"Error while naming sketch.\n"
							+ "Please ensure that sketch name is not blank and not already in use\n",
					"Error", JOptionPane.ERROR_MESSAGE);

			newName = (String) JOptionPane.showInputDialog(_theOverview.getFrame(), "Name for new sketch:", "Get name",
					JOptionPane.QUESTION_MESSAGE, null, null, newName);

			if (newName == null) {
				return;
			}

			newName = newName.trim();
		}

		_theOverview.getFrame().getInfoTreeUI().storeExpansion();

		Point p = (_newPoint != null) ? _newPoint : _theOverview.getNewSketchPosition(10);

		_theOverview.addNewSketch(newName, p.getX(), p.getY());
		_theOverview.setDirty(true);
		_theOverview.getFrame().getInfoTreeUI().revertExpansion();
	}
}
