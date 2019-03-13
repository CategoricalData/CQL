package easik.ui.menu;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import easik.ui.ApplicationFrame;
import easik.ui.DocInfoUI;

/**
 * Menu action for accessing an overview's document information
 */
public class OverviewDocumentInfoAction extends AbstractAction {
	/**
	 *    
	 */
	private static final long serialVersionUID = 9208223372442279352L;

	/**  */
	private ApplicationFrame _theFrame;

	/**
	 * Set up the document menu option.
	 *
	 * @param inFrame
	 */
	public OverviewDocumentInfoAction(ApplicationFrame inFrame) {
		super("Document Information");

		_theFrame = inFrame;

		putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_I));
		putValue(Action.SHORT_DESCRIPTION, "Information about the current sketches");
	}

	/**
	 * Brings up a dialog to set the overview's header information.
	 * 
	 * @param e
	 *            The action event
	 */
	@SuppressWarnings("unused")
	@Override
	public void actionPerformed(ActionEvent e) {
		new DocInfoUI(_theFrame);
	}
}
