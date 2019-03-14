package easik.ui.menu.popup;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import easik.overview.Overview;
import easik.overview.vertex.ViewNode;

/**
 * Action for setting the frame associated with a view node visible.
 */
public class OpenViewAction extends AbstractAction {
	/**
	 *    
	 */
	private static final long serialVersionUID = -7259816059777287059L;

	/** The overview in which the action occurs. */
	private Overview _theOverview;

	/**
	 *
	 *
	 * @param inOverview
	 */
	public OpenViewAction(Overview inOverview) {
		super("Open view...");

		_theOverview = inOverview;
	}

	/**
	 * Set the view frame associated with the selected view node as visible.
	 * 
	 * @param e The action event
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		Object[] selection = _theOverview.getSelectionCells();

		for (Object sel : selection) {
			if (sel instanceof ViewNode) {
				((ViewNode) sel).getFrame().setVisible(true);
			}
		}

		_theOverview.clearSelection();
	}
}
