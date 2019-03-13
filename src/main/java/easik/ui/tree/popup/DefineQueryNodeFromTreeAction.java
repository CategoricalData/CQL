package easik.ui.tree.popup;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;

import easik.ui.DefineQueryNodeDialog;
import easik.ui.ViewFrame;
import easik.view.View;
import easik.view.util.QueryException;
import easik.view.vertex.QueryNode;

/**
 * Action for defining a query node in a view. Opens a dialog which allows the
 * user to rename the node and define the query that it represents.
 */
public class DefineQueryNodeFromTreeAction extends AbstractAction {
	/**
	 *    
	 */
	private static final long serialVersionUID = -912422631395543380L;

	/**  */
	ViewFrame _theFrame;

	/**
	 * Sets up define query node action
	 *
	 * @param inFrame
	 */
	public DefineQueryNodeFromTreeAction(ViewFrame inFrame) {
		super("Define query node...");

		_theFrame = inFrame;

		putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_R));
		putValue(Action.SHORT_DESCRIPTION, "Change the name of selection");
	}

	/**
	 * Prompts user for name and query values and make appropriate updates on
	 * query node.
	 *
	 * @param ourNode
	 */
	public static void updateNode(QueryNode ourNode) {
		ViewFrame ourFrame = ourNode.getMModel().getFrame();
		View ourView = ourFrame.getMModel();
		String originalName = ourNode.getName();
		DefineQueryNodeDialog dqnd = new DefineQueryNodeDialog(ourFrame, "Define Query Node", ourNode);

		if (!dqnd.isAccepted()) {
			return;
		}

		String errorMess = null;
		String name = dqnd.getName();

		if (name.equals("")) {
			errorMess = "Blank name field: did not update.";
		} else if (ourView.isNameUsed(name) && !originalName.equals(name)) {
			errorMess = "Name already in use: not update.";
		}

		if (errorMess != null) {
			JOptionPane.showMessageDialog(ourView.getParent(), errorMess, "Error", JOptionPane.ERROR_MESSAGE);
		} else if (!name.equals(originalName)) {
			ourNode.setName(name);
			ourFrame.getInfoTreeUI().refreshTree();
			ourView.getGraphLayoutCache().reload();
			ourView.repaint();
			ourView.setDirty();
		}

		String query = dqnd.getQuery();

		try {
			ourNode.setQuery(query);
		} catch (QueryException e) {
			JOptionPane.showMessageDialog(ourView.getParent(), "New Query not set. Not valid query.", "Error", JOptionPane.ERROR_MESSAGE);
		}

		ourView.clearSelection();
	}

	/**
	 * Fires updateNode(QueryNode ourNode) with the current selection, should
	 * its size be equal to 1.
	 *
	 * @param e
	 *            The action event
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		Object[] currentSelection = _theFrame.getMModel().getSelectionCells();

		if (currentSelection.length != 1) {
			return;
		}

		DefaultMutableTreeNode selected = (DefaultMutableTreeNode) _theFrame.getInfoTreeUI().getInfoTree().getSelectionPath().getLastPathComponent();

		if (selected.getUserObject() instanceof QueryNode) {
			updateNode((QueryNode) selected.getUserObject());
		}
	}
}
