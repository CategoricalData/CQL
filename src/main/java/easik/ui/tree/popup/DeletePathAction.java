package easik.ui.tree.popup;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;

import easik.graph.EasikGraphModel;
import easik.model.Model;
import easik.model.constraint.CommutativeDiagram;
import easik.model.constraint.ModelConstraint;
import easik.model.constraint.ProductConstraint;
import easik.model.constraint.SumConstraint;
import easik.model.edge.ModelEdge;
import easik.model.path.ModelPath;
import easik.model.ui.ModelFrame;
import easik.model.vertex.ModelVertex;

/**
 * Show constraint menu action for the information tree.
 *
 * @author Kevin Green 2006
 * @since 2006-08-17 Kevin Green
 * @version 2006-08-22 Kevin Green
 */
public class DeletePathAction<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>>
    extends AbstractAction {
  /**
   *    
   */
  private static final long serialVersionUID = 8996892768450901519L;

  /**  */
  F _theFrame;

  /**
   * Set up the remove path menu option.
   *
   * @param _theFrame2
   */
  public DeletePathAction(F _theFrame2) {
    super("Remove Path from ModelConstraint");

    _theFrame = _theFrame2;

    putValue(Action.SHORT_DESCRIPTION, "Removes the currently selected path from the constraint.");
  }

  /**
   * Tests if the removal is valid and then removes the path from the constraint
   *
   * @param e The action event
   */
  @Override
  @SuppressWarnings("unchecked")
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

    // Selection is a constraint
    if (curSelected instanceof ModelPath) {
      ModelConstraint<F, GM, M, N, E> curConstraint = (ModelConstraint<F, GM, M, N, E>) curSelected.getParent();
      ArrayList<ModelPath<F, GM, M, N, E>> tempPaths = new ArrayList<>();

      tempPaths.remove(curSelected);

      boolean valid = false;

      if (curConstraint instanceof SumConstraint) {
        valid = _theFrame.getMModel().isSumConstraint(tempPaths);

        // Replace previous path array list
        if (valid) {
          ((SumConstraint<F, GM, M, N, E>) curConstraint).setPaths(tempPaths);
        }
      } else if (curConstraint instanceof ProductConstraint) {
        valid = _theFrame.getMModel().isProductConstraint(tempPaths);

        // Replace previous path array list
        if (valid) {
          ((ProductConstraint<F, GM, M, N, E>) curConstraint).setPaths(tempPaths);
        }
      } else if (curConstraint instanceof CommutativeDiagram) {
        valid = _theFrame.getMModel().isCommutativeDiagram(tempPaths);

        // Replace previous path array list
        if (valid) {
          ((CommutativeDiagram<F, GM, M, N, E>) curConstraint).setPaths(tempPaths);
        }
      } else {
        JOptionPane.showMessageDialog(_theFrame,
            "You don't have a path selected that can be removed. \nPlease select another path and try again.",
            "No ModelConstraint Selected", JOptionPane.ERROR_MESSAGE);

        return;
      }

      if (valid) {
        ModelConstraint<F, GM, M, N, E> myConst = curConstraint;

        // Remove old tree node
        myConst.removeFromParent();
        _theFrame.getInfoTreeUI().addConstraint(myConst);

        // Referesh Tree
        _theFrame.getInfoTreeUI().refreshTree(myConst);
        _theFrame.getMModel().setDirty();
        _theFrame.getMModel().setSynced(false);
      } else {
        JOptionPane.showMessageDialog(_theFrame,
            "Revoming this path would make the constraint invalid.\nPath was not removed",
            "Path Not Removed", JOptionPane.ERROR_MESSAGE);
      }
    }

    // Selection is not a constraint
    else {
      JOptionPane.showMessageDialog(_theFrame,
          "You don't have a path selected. \nPlease select a path and try again.",
          "No ModelConstraint Selected", JOptionPane.ERROR_MESSAGE);
    }
  }
}
