package easik.ui.menu.popup;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import easik.overview.Overview;
import easik.overview.vertex.SketchNode;

/**
 * Action for setting the frame associated with a selected sketch node visible.
 * Sketch will be opened for editing.
 */
public class OpenSketchAction extends AbstractAction {
	/**
	 *    
	 */
	private static final long serialVersionUID = -7397698347384102279L;

	/**  */
	private static final String LABEL = "Edit sketch...";

	/** The overview in which the action occurs */
	private Overview _theOverview;

	/**
	 *
	 *
	 * @param inOverview
	 */
	public OpenSketchAction(Overview inOverview) {
		this(inOverview, false);
	}

	/**
	 *
	 *
	 * @param inOverview
	 * @param bold
	 */
	public OpenSketchAction(Overview inOverview, boolean bold) {
		super(bold ? "<html><b>" + LABEL + "</b></html>" : LABEL);

		_theOverview = inOverview;

		putValue(Action.SHORT_DESCRIPTION, "Open selected sketch for editing.");
	}

	/**
	 * Enable editing on selected sketch.
	 * 
	 * @param e The action event
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		Object[] selection = _theOverview.getSelectionCells();

		for (Object sel : selection) {
			if (sel instanceof SketchNode) {
				((SketchNode) sel).getFrame().enableSketchEdit();
			}
		}

		_theOverview.clearSelection();
	}
}
