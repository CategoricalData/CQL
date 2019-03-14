package easik.ui.menu.popup;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import easik.sketch.Sketch;
import easik.sketch.vertex.EntityNode;
import easik.ui.datamanip.SelectDataDialog;
import easik.ui.datamanip.jdbc.DatabaseUtil;

/**
 * Action to trigger displaying the contents of the table represented by a
 * selected entity node.
 *
 * @author Rob Fletcher 2005
 */
public class ViewDataAction extends AbstractAction {
	/**
	 *    
	 */
	private static final long serialVersionUID = 4938382108393122500L;

	/** The overview in which the new sketch will be placed */
	private Sketch _theSketch;

	/** The primary IDs of the selected rows */
	@SuppressWarnings("unused")
	private int[] selected_IDs;

	/** The JTable holding the selected table's data */
	@SuppressWarnings("unused")
	private SelectDataDialog tableData;

	/**
	 * Prepare the menu option.
	 *
	 * @param inSketch
	 */
	public ViewDataAction(Sketch inSketch) {
		super("View table contents...");

		_theSketch = inSketch;

		putValue(Action.SHORT_DESCRIPTION, "View all of the data held in the table represented by the selected node.");
	}

	/**
	 * @param e The action event
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		Object[] currentSelection = _theSketch.getSelectionCells();

		if (!(currentSelection[0] instanceof EntityNode)) {
			return;
		}

		EntityNode node = (EntityNode) currentSelection[0];

		DatabaseUtil.selectRowPKs(node.getMModel().getFrame(), node);
	}
}
