package easik.ui.datamanip.jdbc;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;
import java.sql.SQLException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import easik.database.api.jdbc.JDBCDriver;
import easik.sketch.Sketch;
import easik.sketch.vertex.EntityNode;
import easik.ui.datamanip.FreeQueryDialog;

/**
 * Action for executing a free-form SQL delete query on a selected table. Sets
 * some default text to minimize typing required by user.
 */
public class ExecPreparedDeleteAction extends AbstractAction {
  /**
   *    
   */
  private static final long serialVersionUID = 3462671277149235877L;

  /** The sketch which contains the valid tables we can query */
  private Sketch _theSketch;

  /**
   * Sets up action for executing a free-form query.
   * 
   * @param inSketch The sketch which contains the valid tables to query.
   */
  public ExecPreparedDeleteAction(Sketch inSketch) {
    super("Delete row(s) via query...");

    _theSketch = inSketch;

    putValue(Action.SHORT_DESCRIPTION, "Execute a free form DELETE query.");
  }

  /**
   * Create the new entity and set up its name
   * 
   * @param e The action event
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    Object[] currentSelection = _theSketch.getSelectionCells();

    if (!(currentSelection[0] instanceof EntityNode)) {
      return;
    }

    EntityNode table = (EntityNode) currentSelection[0];
    JDBCDriver dbd = null;

    dbd = _theSketch.getDatabase().getJDBCDriver();

    if (dbd == null) {
      return; // The user hit Cancel
    }

    FreeQueryDialog afqd;
    String text = "DELETE FROM " + table.getName() + "\n WHERE()";

    while (true) {
      afqd = new FreeQueryDialog(_theSketch.getFrame(), text);

      if (!afqd.isAccepted()) {
        return;
      }

      try {
        String input = afqd.getInput();

        dbd.executeUpdate(input);

        return;
      } catch (SQLException ex) {
        JOptionPane.showMessageDialog(null, ex.getMessage());

        text = afqd.getInput();

        continue;
      }
    }
  }
}
