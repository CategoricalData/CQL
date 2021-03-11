package easik.ui.tree.popup;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;

import easik.graph.EasikGraphModel;
import easik.model.Model;
import easik.model.edge.ModelEdge;
import easik.model.keys.UniqueKey;
import easik.model.keys.UniqueKeyUI;
import easik.model.ui.ModelFrame;
import easik.model.vertex.ModelVertex;

/**
 * Edit unique key popup menu action for the information tree.
 *
 * @author Kevin Green 2006
 * @since 2006-06-14 Kevin Green
 * @version 2006-07-26 Kevin Green
 */
public class EditUniqueKeyAction<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>>
    extends AbstractAction {
  /**
   *    
   */
  private static final long serialVersionUID = -8440088078131311968L;

  /**  */
  F _theFrame;

  /**
   * Set up the edit unique key menu option.
   *
   * @param _theFrame2
   */
  public EditUniqueKeyAction(F _theFrame2) {
    super("Edit Unique Key");

    _theFrame = _theFrame2;

    putValue(Action.SHORT_DESCRIPTION, "Edits the currently selected unique key.");
  }

  /**
   * Brings up a dialog to edit the currently selected unique key
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
    N curEntity;
    UniqueKey<F, GM, M, N, E> myKey;

    // Check what is currently selected
    if (curSelected instanceof UniqueKey) {
      // Entity is selected so set it as current entity
      myKey = (UniqueKey<F, GM, M, N, E>) curSelected;
      // we can cast it because we will only edit in sketches
      curEntity = myKey.getEntity();
    } else {
      JOptionPane.showMessageDialog(_theFrame,
          "You do not have an entity selected. \nPlease select an entity and try again.",
          "No Entity Selected", JOptionPane.ERROR_MESSAGE);

      return; // Jump out of function
    }

    UniqueKeyUI<F, GM, M, N, E> myUI = new UniqueKeyUI<>(_theFrame, curEntity, myKey);

    if (myUI.showDialog()) {
      // Get values from dialog
      myKey.setElements(myUI.getSelectedElements());
      myKey.setKeyName(myUI.getKeyName());

      // Refresh tree
      _theFrame.getInfoTreeUI().refreshTree(curSelected); // Refresh view
                                // of key

      Object[] myCell = new Object[] { curEntity };

      _theFrame.getMModel().getGraphLayoutCache().hideCells(myCell, true);
      _theFrame.getMModel().getGraphLayoutCache().showCells(myCell, true);
      _theFrame.getMModel().repaint();
      _theFrame.getMModel().setDirty();
      _theFrame.getMModel().setSynced(false);
    }
  }
}
