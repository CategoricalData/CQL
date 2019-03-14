package easik.ui.menu;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.Action;

import easik.ui.ApplicationFrame;
import easik.ui.FileChooser;
import easik.ui.FileFilter;
import easik.ui.JUtils;

/**
 * Menu action for opening and loading an overview
 */
public class FileOpenAction extends AbstractAction {
	/**
	 *    
	 */
	private static final long serialVersionUID = 3455997192523676199L;

	/**
	 * The frame the overview is to be placed in
	 */
	ApplicationFrame _theFrame;

	/**
	 * Create the menu option for loading an overview
	 *
	 * @param inFrame
	 */
	public FileOpenAction(ApplicationFrame inFrame) {
		super("Open Overview...");

		_theFrame = inFrame;

		putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_O));
		putValue(Action.SHORT_DESCRIPTION, "Import an existing Easik document");
	}

	/**
	 * Opens a new dialog box to choose the file to load. Prompts user for
	 * confirmation if the current sketch is unsaved.
	 *
	 * @param e The action event
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		boolean proceed = true;

		if (_theFrame.getOverview().getDirty()) {
			proceed = JUtils.confirmLoss(_theFrame);
		}

		if (proceed) {
			File selFile = FileChooser.loadFile("Open EASIK Overview", FileFilter.EASIK);

			if (selFile != null) {
				_theFrame.getOverview().openOverview(selFile);
			}
		}
	}
}
