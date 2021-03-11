package easik.ui.menu.popup;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import easik.ui.ViewFrame;
import easik.view.vertex.QueryNode;

/**
 * Action used by the Delete menu option in the view frame.
 *
 * @author Rob Fletcher 2005
 */
public class DeleteFromViewAction extends AbstractAction {
  /**
   *    
   */
  private static final long serialVersionUID = -1243136319713620214L;

  /** The view frame in which the deleting occurs */
  ViewFrame _theFrame;

  /**
   * Sets up the delete action.
   *
   * @param inFrame
   */
  public DeleteFromViewAction(ViewFrame inFrame) {
    super("Delete...");

    _theFrame = inFrame;

    putValue(Action.SHORT_DESCRIPTION, "Delete selection");
  }

  /**
   * When the action is performed, selection is deleted if possible. Error is
   * displayed if no graph item is selected.
   *
   * @param e The action event
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    int op = JOptionPane.showConfirmDialog(_theFrame, "Are you sure you want to delete the selected items?",
        "Confirm Deletion", JOptionPane.YES_NO_OPTION);

    if (op == JOptionPane.YES_OPTION) {
      Object[] currentSelection = _theFrame.getMModel().getSelectionCells();

      if (currentSelection.length == 0) {
        JOptionPane.showMessageDialog(_theFrame, "Operation must be performed with something selected", "Error",
            JOptionPane.ERROR_MESSAGE);
      } else {
        for (Object o : currentSelection) {
          if (o instanceof QueryNode) {
            _theFrame.getMModel().removeNode((QueryNode) o);
          }
        }

        _theFrame.getMModel().setDirty();
      }

      // Clear selection after things have been deleted
      _theFrame.getMModel().clearSelection();
    }
  }
}
