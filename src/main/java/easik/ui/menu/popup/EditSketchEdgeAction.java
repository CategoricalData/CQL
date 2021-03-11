package easik.ui.menu.popup;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import easik.sketch.Sketch;
import easik.sketch.edge.SketchEdge;
import easik.ui.SketchFrame;

/**
 * Action for editing (renaming and setting cascade mode) a sketch edge.
 */
public class EditSketchEdgeAction extends AbstractAction {
  /**
   *    
   */
  private static final long serialVersionUID = 7737541465791199661L;

  /**  */
  private SketchFrame _theFrame;

  /**
   * Sets up rename action
   *
   * @param inFrame
   */
  public EditSketchEdgeAction(SketchFrame inFrame) {
    super("Edit edge ...");

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
    Sketch _ourSketch = _theFrame.getMModel();
    Object[] currentSelection = _ourSketch.getSelectionCells();

    // If only one edge is selected, then we allow this. We will ignore any
    // non-edges which might be selected
    if ((currentSelection.length == 1) && (currentSelection[0] instanceof SketchEdge)) {
      // If we're currently synced with a db, give the user the chance to
      // cancel operation
      if (_ourSketch.isSynced()) {
        if (JOptionPane.showConfirmDialog(_theFrame,
            "Warning: this sketch is currently synced with a db; delete and break synchronization?",
            "Warning!", JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE) == JOptionPane.CANCEL_OPTION) {
          return;
        }
      }

      SketchEdge edge = (SketchEdge) currentSelection[0];
      EdgeOptions opts = new EdgeOptions(_theFrame, edge);

      if (opts.isAccepted()) {
        if (!opts.getName().equals(edge.getName())) {
          edge.setName(opts.getName());
        }

        edge.setCascading(opts.getCascadeMode());
        _theFrame.getInfoTreeUI().refreshTree();
        _ourSketch.getGraphLayoutCache().reload();
        _ourSketch.clearSelection();
        _ourSketch.repaint();
        _ourSketch.setDirty();
        _ourSketch.setSynced(false);
      }
    }
  }
}
