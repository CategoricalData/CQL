package easik.ui.menu;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import easik.ui.JUtils;
import easik.ui.ViewFrame;
import easik.view.View;

/**
 * The menu action for when 'new sketch' is selected
 *
 * @author Rob Fletcher 2005
 */
public class FileClearViewAction extends AbstractAction {
	/**
	 *    
	 */
	private static final long serialVersionUID = 4733890144255143420L;

	/**  */
	ViewFrame _theFrame;

	/**
	 * Create a new sketch action option.
	 *
	 * @param inFrame
	 */
	public FileClearViewAction(ViewFrame inFrame) {
		super("Clear view");

		_theFrame = inFrame;

		putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_N));
		putValue(Action.SHORT_DESCRIPTION, "Start new sketch");
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

		if (!_theFrame.getMModel().isEmpty()) {
			proceed = JUtils.confirmLoss(_theFrame);

			// we removed something from the current view, changing the
			// overview, so set it to dirty
			if (proceed) {
				_theFrame.getOverview().setDirty(true);
			}
		}

		if (proceed) {
			View theView = _theFrame.getMModel();

			theView.newView();
			theView.updateThumb();
			_theFrame.getNode().setName(theView.getOverview().getNewViewName());
			theView.getOverview().refresh();
		}
	}
}
