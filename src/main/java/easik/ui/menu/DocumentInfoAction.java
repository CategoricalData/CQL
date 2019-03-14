package easik.ui.menu;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import easik.ui.DocInfoUI;
import easik.ui.EasikFrame;
import easik.ui.SketchFrame;
import easik.ui.ViewFrame;

/**
 * Menu action for displaying document info for sketch frames and view frames
 *
 * @author Kevin Green 2006
 * @since 20006-06-22 Kevin Green
 * @version 2006-06-22 Kevin Green
 */
public class DocumentInfoAction extends AbstractAction {
	/**
	 *    
	 */
	private static final long serialVersionUID = -5048109389079364995L;

	/**  */
	private EasikFrame _theFrame;

	/**
	 * Set up the document menu option.
	 *
	 * @param inFrame
	 */
	private DocumentInfoAction(EasikFrame inFrame) {
		super("Document Information");

		_theFrame = inFrame;

		putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_I));
		putValue(Action.SHORT_DESCRIPTION, "Information about the current sketch");
	}

	/**
	 *
	 *
	 * @param inFrame
	 */
	public DocumentInfoAction(SketchFrame inFrame) {
		this((EasikFrame) inFrame);
	}

	/**
	 *
	 *
	 * @param inFrame
	 */
	public DocumentInfoAction(ViewFrame inFrame) {
		this((EasikFrame) inFrame);
	}

	/**
	 * Brings up a dialog to set the document header information.
	 * 
	 * @param e The action event
	 */
	@SuppressWarnings("unused")
	@Override
	public void actionPerformed(ActionEvent e) {
		// System.out.println("Set document header info pressed!");
		new DocInfoUI(_theFrame);
	}
}
