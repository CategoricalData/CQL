package easik.ui.menu.popup;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import easik.ui.DefineQueryNodeDialog;
import easik.ui.ViewFrame;
import easik.view.View;
import easik.view.util.QueryException;
import easik.view.vertex.QueryNode;

/**
 * Action for defining a query node in a view. Opens a dialog which allows the
 * user to rename the node and define the query that it represents.
 */
public class DefineQueryNodeAction extends AbstractAction {
  /**
   *    
   */
  private static final long serialVersionUID = 8032310216478564817L;

  /**  */
  ViewFrame _theFrame;

  /**
   * Sets up define query node action
   *
   * @param inFrame
   */
  public DefineQueryNodeAction(ViewFrame inFrame) {
    super("Edit view node...");

    _theFrame = inFrame;

    putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_R));
    putValue(Action.SHORT_DESCRIPTION, "Change the name of selection");
  }

  /**
   * Prompts user for name and query values and make appropriate updates on query
   * node.
   *
   * @param ourNode
   */
  public static void updateNode(QueryNode ourNode) {
    boolean hadWhere = !ourNode.getWhere().isEmpty();
    boolean hadEntityNode = !(ourNode.getQueriedEntity() == null);
    ViewFrame ourFrame = ourNode.getMModel().getFrame();
    View ourView = ourFrame.getMModel();
    String originalName = ourNode.getName();
    String originalQuery = ourNode.getQuery();
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
    } else if (!dqnd.getQuery().equals(originalQuery)) {
      String query = dqnd.getQuery();

      // this can very likely throw an exception.
      // Any malformed sql should be rejected here.
      try {
        ourNode.setQuery(query);
      } catch (QueryException e) {
        JOptionPane.showMessageDialog(ourView.getParent(),
            "New Query not set. Not valid query.\n" + e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
      }

      if (hadWhere && ourNode.getWhere().isEmpty()) {
        // remove constraints that this queryNode used to be a part of
        ourView.updateConstraints(ourNode, hadWhere);
      } else if (!hadWhere && !ourNode.getWhere().isEmpty()) {
        ourView.updateConstraints(ourNode, hadWhere);
      }

      if (!hadEntityNode && !(ourNode.getQueriedEntity() == null)) {
        ourView.autoAddExistingEdges();
      }
    }

    ourView.clearSelection();
  }

  /**
   * Fires updateNode(QueryNode ourNode) with the current selection, should its
   * size be equal to 1.
   *
   * @param e The action event
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    Object[] currentSelection = _theFrame.getMModel().getSelectionCells();

    if (currentSelection.length != 1) {
      return;
    }

    QueryNode ourNode = (QueryNode) currentSelection[0];

    updateNode(ourNode);
  }
}
