package easik.ui.tree.popup;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;

import easik.database.types.EasikType;
import easik.graph.EasikGraphModel;
import easik.model.Model;
import easik.model.attribute.AttributeUI;
import easik.model.attribute.EntityAttribute;
import easik.model.edge.ModelEdge;
import easik.model.ui.ModelFrame;
import easik.model.vertex.ModelVertex;

/**
 * Edit attribute popup menu action for the information tree.
 *
 * @author Kevin Green 2006
 * @since 2006-06-14 Kevin Green
 * @version 2006-07-26 Kevin Green
 */
public class EditAttributeAction<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>>
		extends AbstractAction {
	/**
	 *    
	 */
	private static final long serialVersionUID = 3410021724162068140L;

	/**  */
	F _theFrame;

	/**
	 * Set up the edit attribute menu option.
	 *
	 * @param _theFrame2
	 */
	public EditAttributeAction(F _theFrame2) {
		super("Edit Attribute");

		_theFrame = _theFrame2;

		putValue(Action.SHORT_DESCRIPTION, "Edits the currently selected attribute.");
	}

	/**
	 * Brings up a dialog to edit the currently selected attribute
	 * 
	 * @param e The action event
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		// If there is nothing seleceted then just do nothing
		if (_theFrame.getInfoTreeUI().getInfoTree().isSelectionEmpty()) {
			return;
		}

		// If we're currently synced with a db, give the user the chance to
		// cancel operation
		if (_theFrame.getMModel().isSynced()) {
			int choice = JOptionPane.showConfirmDialog(_theFrame,
					"Warning: this sketch is currently synced with a db; continue and break synchronization?",
					"Warning!", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

			if (choice == JOptionPane.CANCEL_OPTION) {
				return;
			}
		}

		// Get currently selected object
		DefaultMutableTreeNode curSelected = (DefaultMutableTreeNode) _theFrame.getInfoTreeUI().getInfoTree()
				.getSelectionPath().getLastPathComponent();

		// Selection is an attribute
		if (curSelected instanceof EntityAttribute) {
			@SuppressWarnings("unchecked")
			EntityAttribute<F, GM, M, N, E> curAttribute = (EntityAttribute<F, GM, M, N, E>) curSelected;
			// we can cast it because we will only edit in sketches
			N parentEntity = curAttribute.getEntity();
			AttributeUI<F, GM, M, N, E> myUI = new AttributeUI<>(_theFrame, parentEntity, curAttribute);

			if (myUI.isAccepted()) {
				// Get values from dialog
				@SuppressWarnings("unused")
				String newAttName = myUI.getName();
				@SuppressWarnings("unused")
				EasikType newAttType = myUI.getCustomType();

				curAttribute.setName(myUI.getName());
				curAttribute.setType(myUI.getCustomType());
				_theFrame.getInfoTreeUI().refreshTree(parentEntity);

				Object[] myCell = new Object[] { parentEntity };

				_theFrame.getMModel().getGraphLayoutCache().hideCells(myCell, true);
				_theFrame.getMModel().getGraphLayoutCache().showCells(myCell, true);
				_theFrame.getMModel().repaint();
				_theFrame.getMModel().setDirty();
				_theFrame.getMModel().setSynced(false);
			}
		}

		// Selection is not an attribute
		else {
			JOptionPane.showMessageDialog(_theFrame,
					"You don't have an attribute selected. \nPlease select an attribute and try again.",
					"No Attribute Selected", JOptionPane.ERROR_MESSAGE);
		}
	}
}
