package easik.ui.menu;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import easik.Easik;
import easik.ui.ApplicationFrame;
import easik.ui.JUtils;

/**
 * Menu action for retrieving a previously viewed file quickly and easily.
 *
 * @author Kevin Green 2006
 * @author Vera Ranieri 2006
 * @since 2006-07-13 Kevin Green
 * @version 2006-08-03 Vera Ranieri
 */
public class RecentFileAction extends AbstractAction {
	/**
	 *    
	 */
	private static final long serialVersionUID = -3503315249095269643L;

	/** String representation of the file */
	String _file;

	/** The frame action occurs in */
	ApplicationFrame _theFrame;

	/**
	 * Constructor accepting one file
	 * 
	 * @param file
	 *            The file to be opened
	 * @param inFrame
	 */
	public RecentFileAction(String file, ApplicationFrame inFrame) {
		super(file);

		_theFrame = inFrame;

		putValue(Action.SHORT_DESCRIPTION, "Open the recent file");

		_file = file;
	}

	/**
	 * Records if the file has been chosen to be opened
	 * 
	 * @param e
	 *            The action event
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		if (_theFrame.getOverview().getDirty()) {
			if (!JUtils.confirmLoss(_theFrame)) {
				return;
			}
		}

		_theFrame.getOverview().initializeOverview();
		_theFrame.setTreeName("");
		_theFrame.getOverview().setDirty(false);

		File selFile = new File(_file);

		if (selFile.exists()) {
			_theFrame.getOverview().loadFromXML(selFile);
			_theFrame.getOverview().setFile(selFile);
			Easik.getInstance().getSettings().setProperty("folder_last", selFile.getAbsolutePath());
			_theFrame.getOverview().setDirty(false);
			_theFrame.addRecentFile(selFile);
		} else {
			JOptionPane.showMessageDialog(_theFrame, "'" + _file + "' does not exist.", "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
}
