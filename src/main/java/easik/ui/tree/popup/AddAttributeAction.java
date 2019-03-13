package easik.ui.tree.popup;

//~--- non-JDK imports --------------------------------------------------------

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import easik.database.types.EasikType;
import easik.graph.EasikGraphModel;
import easik.model.Model;
import easik.model.attribute.AttributeUI;
import easik.model.attribute.EntityAttribute;
import easik.model.edge.ModelEdge;
import easik.model.ui.ModelFrame;
import easik.model.vertex.ModelVertex;
//~--- JDK imports ------------------------------------------------------------

/**
 * Add attribute popup menu action for the information tree.
 *
 * @author Kevin Green 2006
 * @since 2006-06-14 Kevin Green
 * @version 2006-07-26 Kevin Green
 * @version 06-2014 Federico Mora
 */
public class AddAttributeAction<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>> extends AbstractAction {
	private static final long serialVersionUID = 1807969103366326790L;

	/**  */
	F _theFrame;

	/**
	 * Set up the add attribute menu option.
	 *
	 * @param inFrame
	 */
	public AddAttributeAction(F inFrame) {
		super("Add Attribute");

		_theFrame = inFrame;

		putValue(Action.SHORT_DESCRIPTION, "Add an attribute to the currently selected entity.");
	}

	/**
	 * Inserts an attribute to the currently selected entity (or parent entity
	 * if attribute is selected) in the tree
	 * 
	 * @param e
	 *            The action event
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void actionPerformed(ActionEvent e) {
		// If we're currently synced with a db, give the user the chance to
		// cancel operation
		if (_theFrame.getMModel().isSynced()) {
			int choice = JOptionPane.showConfirmDialog(_theFrame, "Warning: this sketch is currently synced with a db; continue and break synchronization?", "Warning!", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

			if (choice == JOptionPane.CANCEL_OPTION) {
				return;
			}
		}

		// Get currently selected object
		Object[] selected = _theFrame.getMModel().getSelectionCells();
		N curEntity;

		// Check what is currently selected
		if ((selected.length == 1) && (selected[0] instanceof ModelVertex)) {
			// Entity is selected so set it as current entity
			curEntity = (N) selected[0];
		} else {
			JOptionPane.showMessageDialog(_theFrame, "You do not have an entity selected. \nPlease select a single entity and try again.", "No Entity Selected", JOptionPane.ERROR_MESSAGE);

			return;
		}

		AttributeUI<F, GM, M, N, E> myUI = new AttributeUI<>(_theFrame, curEntity);

		if (myUI.isAccepted()) {
			// Get values from dialog
			String newAttName = myUI.getName();
			EasikType newAttType = myUI.getCustomType();

			// Create Entity Attribute
			EntityAttribute<F, GM, M, N, E> newAtt = new EntityAttribute<>(newAttName, newAttType, curEntity);

			// Add attribute to entity
			curEntity.addEntityAttribute(newAtt);

			// TODO
			/*
			 * need to find way to do this now with generics. Want to add to
			 * queryNodes when adding to entityNode
			 * 
			 * for(ViewNode vn :_theFrame.getMModel().getViews()){ HashMap<N,
			 * QueryNode> nodePairs = vn.getMModel().getEntityNodePairs();
			 * //potentially throws QueryException but won't in this case since
			 * we are just adding attributes try {
			 * nodePairs.get(curEntity).processAttributes(); } catch
			 * (QueryException e1) { e1.printStackTrace(); } }
			 */
			_theFrame.getInfoTreeUI().refreshTree(curEntity); // Refresh view of
																// entity
			_theFrame.getMModel().clearSelection();
			_theFrame.getMModel().setDirty();
			_theFrame.getMModel().setSynced(false);
		}
	}
}
