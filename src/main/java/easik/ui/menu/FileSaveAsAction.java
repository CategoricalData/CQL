package easik.ui.menu;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.Action;

import easik.overview.Overview;
import easik.ui.ApplicationFrame;
import easik.ui.FileChooser;
import easik.ui.FileFilter;

/**
 * Menu action for exporting the overview as XML
 * 
 * @author Rob Fletcher 2005
 * @author Kevin Green 2006
 * @version 2006-07-13 Kevin Green
 */
public class FileSaveAsAction extends AbstractAction {
	/**
	 *    
	 */
	private static final long serialVersionUID = -8535341492857194552L;

	/**
	 * The frame containing the overview that will be saved
	 */
	ApplicationFrame _theFrame;

	/**
	 * Create a new save as menu action.
	 *
	 * @param inFrame
	 */
	public FileSaveAsAction(ApplicationFrame inFrame) {
		super("Save Overview As...");

		_theFrame = inFrame;

		putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_A));
		putValue(Action.SHORT_DESCRIPTION, "Export current overview as XML document");
	}

	/**
	 * Display a dialog prompting the user for the name under which to save the
	 * current overview.
	 *
	 * @param e
	 *            The action event
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		saveFileAs(_theFrame);
	}

	/**
	 * Prompts for a filename and saves the overview as that name. Returns true
	 * if the file was successfully saved, false if not (i.e. because the user
	 * cancelled).
	 *
	 * @param inFrame
	 *
	 * @return
	 */
	public static boolean saveFileAs(ApplicationFrame inFrame) {
		@SuppressWarnings("unused")
		Overview overview = inFrame.getOverview();
		File selFile = FileChooser.saveFile("Save EASIK Sketch", FileFilter.EASIK, "easik");

		if (selFile != null) {
			return FileSaveAction.saveFile(inFrame, selFile);
		}

		return false;
	}
}
