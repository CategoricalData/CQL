package easik.ui.menu;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import easik.graph.EasikGraphModel;
import easik.model.Model;
import easik.model.edge.ModelEdge;
import easik.model.states.AddLimitConstraintState;
import easik.model.ui.ModelFrame;
import easik.model.vertex.ModelVertex;

/**
 * A limit constraint menu option and action.
 */
public class AddLimitConstraintAction<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>>
    extends AbstractAction {
  /**  */
  private static final long serialVersionUID = -9089239559578339118L;

  /**  */
  private F _theFrame;

  /**
   * Creates and initializes the menu option.
   *
   * @param _theFrame2
   */
  public AddLimitConstraintAction(F _theFrame2) {
    super();

    _theFrame = _theFrame2;

    putValue(Action.NAME, "Add a Limit Constraint");
    putValue(Action.SHORT_DESCRIPTION, "Create a limit constraint");
  }

  /**
   * Creates the limit constraint if the selection is appropriate.
   * 
   * @param e The action event
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    /*
     * sketches can now be empty since we can add as we go if
     * (!_theFrame.getMModel().getEntities().isEmpty()) {
     * _theFrame.getMModel().getStateManager().pushState(new
     * AddLimitConstraintState(_theFrame.getMModel())); } else {
     * JOptionPane.showMessageDialog(null, "Sketch cannot be empty.", "Error",
     * JOptionPane.ERROR_MESSAGE); }
     */
    _theFrame.getMModel().getStateManager().pushState(new AddLimitConstraintState<>(_theFrame.getMModel()));
  }
}
