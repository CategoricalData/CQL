package easik.ui.menu.popup;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import easik.sketch.Sketch;

/**
 * Action for clearing our SQL server connection.
 */
public class DisconnectAction extends AbstractAction {
  /**
   *    
   */
  private static final long serialVersionUID = -5234642592012382419L;

  /** The sketch containing the connection we wish to clear */
  private Sketch _theSketch;

  /**
   * Prepare the menu option, as well as pass a reference to the last clicked
   * point, which is used when positioning the new entity.
   *
   * @param inSketch
   */
  public DisconnectAction(Sketch inSketch) {
    super("Disconnect");

    _theSketch = inSketch;

    putValue(Action.SHORT_DESCRIPTION, "Disable Current Connection");
  }

  /**
   * Clears the sketch's driver, and hides the datamanip frame
   * 
   * @param e The action event
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    _theSketch.getDatabase().cleanDatabaseDriver();
    _theSketch.getFrame().setVisible(false);
  }
}
