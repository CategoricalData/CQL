package easik.ui.menu.popup;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import easik.sketch.Sketch;

/**
 * The menu action for manipulating a database with constraint override enabled.
 *
 * @author Christian Fiddick
 * @version Summer 2012, Easik 2.2
 */
public class OverrideConstraintAction extends AbstractAction {
	/**  */
	private static final long serialVersionUID = -584978205297659599L;

	/** The sketch we wish to edit */
	private Sketch _theSketch;

	/**
	 *
	 *
	 * @param inSketch
	 */
	public OverrideConstraintAction(Sketch inSketch) {
		super("ModelConstraint Override");

		_theSketch = inSketch;

		putValue(Action.SHORT_DESCRIPTION, "Perform an update with constraints disabled");
	}

	/**
	 *
	 *
	 * @param e
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		_theSketch.getDatabase().overrideConstraints();
	}
}
