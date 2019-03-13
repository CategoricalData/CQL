package easik.ui.menu;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.Action;

import easik.Easik;
import easik.overview.Overview;
import easik.ui.ApplicationFrame;

/**
 * Menu action for saving the overview without first prompting for a file if
 * possible.
 *
 * @author Rob Fletcher 2005
 * @author Vera Ranieri 2006
 * @author Kevin Green 2006
 * @version 2006-08-03 Kevin Green
 */
public class FileSaveAction extends AbstractAction {
	/**
	 *    
	 */
	private static final long serialVersionUID = 1339949107545376577L;

	/**
	 * The frame containing the overview that will be saved
	 */
	ApplicationFrame _theFrame;

	/**
	 * Create a new action for saving the overview or saving as if there is not
	 * already a file.
	 *
	 * @param inFrame
	 */
	public FileSaveAction(ApplicationFrame inFrame) {
		super("Save Overview");

		_theFrame = inFrame;

		// FIXME -- what does this do?
		putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_S));
		putValue(Action.SHORT_DESCRIPTION, "Export current overview as XML document");
	}

	/**
	 * Check to see if the overview already has a file name and if it doesn't
	 * prompt for one. Then save with that file name.
	 * 
	 * @param e
	 *            The action event
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		saveFile(_theFrame);
	}

	/**
	 * Saves the file with the current name, if it has one. Otherwise, invokes
	 * the saveFileAs to prompt for a name. Returns true if the file was
	 * successfully saved.
	 *
	 * @param inFrame
	 *
	 * @return
	 */
	public static boolean saveFile(ApplicationFrame inFrame) {
		File selFile = inFrame.getOverview().getFile();

		if (selFile == null) {
			return FileSaveAsAction.saveFileAs(inFrame);
		} 
			return saveFile(inFrame, selFile);
		
	}

	/**
	 *
	 *
	 * @param inFrame
	 * @param selFile
	 *
	 * @return
	 */
	public static boolean saveFile(ApplicationFrame inFrame, File selFile) {
		Overview overview = inFrame.getOverview();

		overview.getDocInfo().updateModificationDate();
		overview.saveToXML(selFile);
		overview.setFile(selFile);
		overview.setDirty(false);
		Easik.getInstance().getSettings().addRecentFile(selFile);
		inFrame.updateRecentFilesMenu();

		return true;
	}
	
	public static void saveFileAql(Overview overview, File selFile) {
		overview.getDocInfo().updateModificationDate();
		overview.saveToXML(selFile);
		overview.setFile(selFile);
	}
}
