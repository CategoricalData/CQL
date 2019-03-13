package easik.ui.menu;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import easik.ui.ApplicationFrame;
import easik.ui.EasikFrame;

/**
 * Menu action for quitting a frame.
 *
 * @author Rob Fletcher 2005
 * @author Kevin Green 2006
 * @version 2006-06-23 Kevin Green
 */
public class FileQuitAction extends AbstractAction {
	/**
	 *    
	 */
	private static final long serialVersionUID = 7822463022862314508L;

	/**  */
	EasikFrame _theFrame;

	/**
	 * Set up the quit menu option.
	 *
	 * @param inFrame
	 */
	public FileQuitAction(EasikFrame inFrame) {
		super((inFrame instanceof ApplicationFrame) ? "Quit" : "Close");

		_theFrame = inFrame;

		putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_Q));
		putValue(Action.SHORT_DESCRIPTION, (inFrame instanceof ApplicationFrame) ? "Quit the EASIK Application" : "Close this EASIK Sketch Window");
	}

	/**
	 * Quits the application, warns if the sketch is dirty. \\FIXME no it
	 * doesn't...
	 * 
	 * @param e
	 *            The action event
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		_theFrame.closeWindow();
	}
}
