package easik.ui.menu.popup;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import easik.model.states.LoadingState;
import easik.sketch.Sketch;
import easik.sketch.vertex.EntityNode;
import easik.ui.SketchFrame;

/**
 * Action for the rename option in the popup menu.
 *
 * @author Rob Fletcher 2005
 * @author Kevin Green 2006
 * @author Jason Rhinelander 2008
 */
public class RenameInSketchAction extends AbstractAction {
	/**
	 *    
	 */
	private static final long serialVersionUID = -7712730231248489294L;

	/**  */
	SketchFrame _theFrame;

	/**
	 * Sets up rename action
	 *
	 * @param inFrame
	 */
	public RenameInSketchAction(SketchFrame inFrame) {
		super("Rename Entity...");

		_theFrame = inFrame;

		putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_R));
		putValue(Action.SHORT_DESCRIPTION, "Change the name of the selected entity");
	}

	/**
	 * Called when clicked upon, will rename an article.
	 *
	 * @param e The action event
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		Sketch _ourSketch = _theFrame.getMModel();

		// If we're currently synced, let user cancel operation.
		if (_ourSketch.isSynced()) {
			if (JOptionPane.showConfirmDialog(_theFrame,
					"Warning: this sketch is currently synced with a db; continue and break synchronization?",
					"Caution!", JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.WARNING_MESSAGE) == JOptionPane.CANCEL_OPTION) {
				return;
			}
		}

		Object[] currentSelection = _ourSketch.getSelectionCells();
		EntityNode nodeToRename = null;

		// If only one entity is selected, then we allow this. We will ignore
		// any
		// non-entities which might be selected
		String originalName = "";

		if ((currentSelection.length == 1) && (currentSelection[0] instanceof EntityNode)) {
			nodeToRename = (EntityNode) currentSelection[0];
			originalName = nodeToRename.getName();
		}

		if (nodeToRename == null) {
			JOptionPane.showMessageDialog(_ourSketch.getParent(),
					"Operation must be performed with a single entity selected", "Error", JOptionPane.ERROR_MESSAGE);
		} else {
			String s = (String) JOptionPane.showInputDialog(_ourSketch.getParent(), "New name:", "Rename entity",
					JOptionPane.QUESTION_MESSAGE, null, null, originalName);

			if (s != null) {
				s = s.trim();

				if (s.equals("")) {
					JOptionPane.showMessageDialog(_ourSketch.getParent(), "Entity name is empty", "Error",
							JOptionPane.ERROR_MESSAGE);
				} else if (_ourSketch.isNameUsed(s) && !nodeToRename.getName().equals(s)) {
					JOptionPane.showMessageDialog(_ourSketch.getParent(), "Entity name is already in use", "Error",
							JOptionPane.ERROR_MESSAGE);
				} else if (s.equals(originalName)) { // no need to do anything
					;
				} else {
					// Push loading state
					_ourSketch.getStateManager().pushState(new LoadingState<>(_ourSketch));
					nodeToRename.setName(s);
					_theFrame.getInfoTreeUI().refreshTree();
					_ourSketch.getGraphLayoutCache().reload();

					// Pop state
					_ourSketch.getStateManager().popState();
					_ourSketch.repaint();
					_ourSketch.setDirty();
					_ourSketch.setSynced(false);
				}
			}

			_ourSketch.clearSelection();
		}
	}
}
