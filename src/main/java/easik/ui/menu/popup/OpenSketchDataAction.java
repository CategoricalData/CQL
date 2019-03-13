package easik.ui.menu.popup;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import easik.overview.Overview;
import easik.overview.vertex.SketchNode;

/**
 * Action for setting the frame associated with a sketch node visible. Will be
 * opened for data manipulation.
 */
public class OpenSketchDataAction extends AbstractAction {
	/**
	 *    
	 */
	private static final long serialVersionUID = 4296132622980211628L;

	/**  */
	private static final String LABEL = "Manipulate db...";

	/** The overview in which the action occurs. */
	private Overview _theOverview;

	/**
	 *
	 *
	 * @param inOverview
	 */
	public OpenSketchDataAction(Overview inOverview) {
		this(inOverview, false);
	}

	/**
	 *
	 *
	 * @param inOverview
	 * @param bold
	 */
	public OpenSketchDataAction(Overview inOverview, boolean bold) {
		super(bold ? "<html><b>" + LABEL + "</b></html>" : LABEL);

		_theOverview = inOverview;

		putValue(Action.SHORT_DESCRIPTION, "Open selected sketch for data manipulation.");
	}

	/**
	 * Enable editing on selected sketch.
	 * 
	 * @param e
	 *            The action event
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		Object[] selection = _theOverview.getSelectionCells();

		for (Object sel : selection) {
			if (sel instanceof SketchNode) {
				((SketchNode) sel).getFrame().enableDataManip(true);
			}
		}

		_theOverview.clearSelection();
	}
}
