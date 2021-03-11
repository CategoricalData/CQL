package easik.ui.menu.popup;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import easik.ui.datamanip.jdbc.DatabaseUtil;
import easik.view.View;
import easik.view.vertex.QueryNode;

/**
 * Action to trigger displaying the contents of the table represented by a
 * selected entity node.
 */
public class ViewQueryAction extends AbstractAction {
  /**
   *    
   */
  private static final long serialVersionUID = -7358840132662817458L;

  /** The view in which we are working */
  private View _ourView;

  /**
   * Prepare the menu option.
   *
   * @param inView
   */
  public ViewQueryAction(View inView) {
    super("View table contents");

    _ourView = inView;

    putValue(Action.SHORT_DESCRIPTION, "Display the result of the query represented by this node.");
  }

  /**
   * @param e The action event
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    Object[] currentSelection = _ourView.getSelectionCells();

    if (currentSelection.length != 1) {
      return;
    }

    DatabaseUtil.displayQueryNode((QueryNode) currentSelection[0], _ourView.getFrame());
  }
}
