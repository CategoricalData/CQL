package easik.ui.menu.popup;

//~--- non-JDK imports --------------------------------------------------------
//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import easik.ui.ViewFrame;
import easik.view.View;
import easik.view.edge.View_Edge;

/**
 * Action for editing (renaming and setting cascade mode) a View edge.
 */
public class EditViewEdgeAction extends AbstractAction {
  /**
   *    
   */
  private static final long serialVersionUID = 7737541465791199661L;

  /**  */
  private ViewFrame _theFrame;

  /**
   * Sets up rename action
   *
   * @param inFrame
   */
  @SuppressWarnings("deprecation")
  public EditViewEdgeAction(ViewFrame inFrame) {
    super("Rename ...");

    _theFrame = inFrame;

    putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_R));
    putValue(Action.SHORT_DESCRIPTION, "Edit the name and cascade option of the selected edge");
  }

  /**
   * Called when clicked upon, will popup the edge UI and, if accepted, update the
   * name and cascade option.
   *
   * @param e The action event
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    View _ourView = _theFrame.getMModel();
    Object[] currentSelection = _ourView.getSelectionCells();

    // If only one edge is selected, then we allow this. We will ignore any
    // non-edges which might be selected
    if ((currentSelection.length == 1) && (currentSelection[0] instanceof View_Edge)) {
      // If we're currently synced with a db, give the user the chance to
      // cancel operation
      if (_ourView.getSketch().isSynced()) {
        if (JOptionPane.showConfirmDialog(_theFrame,
            "Warning: this sketch is currently synced with a db; delete and break synchronization?",
            "Warning!", JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE) == JOptionPane.CANCEL_OPTION) {
          return;
        }
      }

      View_Edge edge = (View_Edge) currentSelection[0];
      ViewEdgeOptions opts = new ViewEdgeOptions(_theFrame, edge);

      if (opts.isAccepted()) {
        if (!opts.getName().equals(edge.getName())) {
          edge.setName(opts.getName());
        }

        // edge.setCascading(opts.getCascadeMode());
        _theFrame.getInfoTreeUI().refreshTree();
        _ourView.getGraphLayoutCache().reload();
        _ourView.clearSelection();
        _ourView.repaint();
        _ourView.setDirty();
        // _ourView.getMModel().setSynced(false);
      }
    }
  }
}
