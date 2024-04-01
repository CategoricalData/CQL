package easik.ui.menu;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import easik.graph.EasikGraphModel;
import easik.model.Model;
import easik.model.edge.ModelEdge;
import easik.model.states.AddProductConstraintState;
import easik.model.ui.ModelFrame;
import easik.model.vertex.ModelVertex;

/**
 * Action for the menu option used to create product constraints.
 *
 * @author Rob Fletcher 2005
 * @author Kevin Green 2006
 * @version 2006-06-13 Kevin Green
 */
public class AddProductConstraintAction<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>>
    extends AbstractAction {
  /**
   *    
   */
  private static final long serialVersionUID = 3741937032290547834L;

  /**  */
  F _theFrame;

  /**
   * Create the action and set the name and description.
   *
   * @param _theFrame2
   */
  public AddProductConstraintAction(F _theFrame2) {
    super();

    _theFrame = _theFrame2;

    putValue(Action.NAME, "Add a Product Constraint");
    putValue(Action.SHORT_DESCRIPTION, "Create a product constraint from a set of paths");
  }

  /**
   * When the action is performed, attempt to create a product constraint after
   * validating the selection.
   *
   * @param e The action event
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    /*
     * sketches can now be empty since we can add as we go if
     * (!_theFrame.getMModel().getEntities().isEmpty()) {
     * _theFrame.getMModel().getStateManager().pushState(new
     * AddProductConstraintState(_theFrame.getMModel())); } else {
     * JOptionPane.showMessageDialog(null, "Sketch cannot be empty.", "Error",
     * JOptionPane.ERROR_MESSAGE); }
     */
    _theFrame.getMModel().getStateManager().pushState(new AddProductConstraintState<>(_theFrame.getMModel()));

  }
}
