package easik.ui.menu.popup;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import easik.sketch.Sketch;
import easik.sketch.vertex.EntityNode;
import easik.ui.datamanip.UpdateMonitor;

/**
 * Action for updating a record in a table represented by an entity node.
 */
public class UpdateRowAction extends AbstractAction {
  /**
   *    
   */
  private static final long serialVersionUID = -7693290290528204946L;

  /** The sketch in which valid entity nodes exist */
  private Sketch _theSketch;

  /**
   * Popup menu action for updating values in a given row of a table.
   *
   * @param inSketch
   */
  public UpdateRowAction(Sketch inSketch) {
    super("Select row and update...");

    _theSketch = inSketch;

    putValue(Action.SHORT_DESCRIPTION, "Select a record from table and set new column values.");
  }

  /**
   * Triggers JDBCUpdateMonitor to update a row in the selected table
   * 
   * @param e The action event
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    Object[] currentSelection = _theSketch.getSelectionCells();

    if (!(currentSelection[0] instanceof EntityNode)) {
      return;
    }

    EntityNode node = (EntityNode) currentSelection[0];
    UpdateMonitor um = _theSketch.getDatabase().newUpdateMonitor();

    if (um == null) {
      JOptionPane.showMessageDialog(null, "Could not perform update: problem accessing db driver");

      return;
    }

    um.updateRow(node);
  }
}
