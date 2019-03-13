package easik.ui.menu;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import easik.ui.ApplicationFrame;
import easik.ui.JUtils;

/**
 * The menu action for when 'new sketch' is selected
 *
 * @author Rob Fletcher 2005
 */
public class FileNewOverviewAction extends AbstractAction {
	/**
	 *    
	 */
	private static final long serialVersionUID = 2798045026550109857L;

	/**  */
	ApplicationFrame _theFrame;

	/**
	 * Create a new sketch action option.
	 *
	 * @param inFrame
	 */
	public FileNewOverviewAction(ApplicationFrame inFrame) {
		super("New Overview");

		_theFrame = inFrame;

		putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_N));
		putValue(Action.SHORT_DESCRIPTION, "Clears the current overview and starts a new, empty overview");
	}

	/**
	 * Create a new sketch. Prompts user for confirmation if current sketch is
	 * unsaved.
	 *
	 * @param e
	 *            The Action Event
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		boolean proceed = true;

		if (_theFrame.getOverview().getDirty()) {
			proceed = JUtils.confirmLoss(_theFrame);
		}

		if (proceed) {
			_theFrame.getOverview().initializeOverview();
			_theFrame.setTreeName("");
			_theFrame.getOverview().setDirty(false);
		}
	}
}
