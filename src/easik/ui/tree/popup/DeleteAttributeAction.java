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
import easik.model.attribute.EntityAttribute;
import easik.model.edge.ModelEdge;
import easik.model.ui.ModelFrame;
import easik.model.vertex.ModelVertex;
import easik.sketch.Sketch;
import easik.sketch.edge.SketchEdge;
import easik.sketch.util.graph.SketchGraphModel;
import easik.sketch.vertex.EntityNode;
import easik.ui.SketchFrame;

/**
 * Delete attribute popup menu action for the information tree.
 *
 * @author Kevin Green 2006
 * @since 2006-06-14 Kevin Green
 * @version 2006-07-26 Kevin Green
 */
public class DeleteAttributeAction<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>>
    extends AbstractAction {
  /**
   *    
   */
  private static final long serialVersionUID = 1585029351562358106L;

  /**  */
  F _theFrame;

  /**
   * Set up the delete attribute menu option.
   *
   * @param _theFrame2
   */
  public DeleteAttributeAction(F _theFrame2) {
    super("Delete Attribute");

    _theFrame = _theFrame2;

    putValue(Action.SHORT_DESCRIPTION, "Deletes the currently selected attribute.");
  }

  /**
   * Deletes the currently selected attribute
   * 
   * @param e The action event
   */
  @Override
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

    // Selection is an attribute
    if (curSelected instanceof EntityAttribute) {
      @SuppressWarnings("unchecked")
      EntityAttribute<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> curAttribute = (EntityAttribute<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge>) curSelected;
      EntityNode parentEntity = curAttribute.getEntity();

      // Show a confirmation dialog box for the deletion
      if (JOptionPane.showConfirmDialog(_theFrame,
          "Are you sure you want to delete the '" + curAttribute.getName() + "' attribute from the '"
              + parentEntity + "' entity?",
          "Confirm Delete", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
        // Delete the attribute from entity
        parentEntity.removeEntityAttribute(curAttribute);
        _theFrame.getMModel().setDirty();
        _theFrame.getMModel().setSynced(false);
      }
    }

    // Selection is not an attribute
    else {
      JOptionPane.showMessageDialog(_theFrame,
          "You don't have an attribute selected. \nPlease select an attribute and try again.",
          "No Attribute Selected", JOptionPane.ERROR_MESSAGE);

      return;
    }

    _theFrame.getInfoTreeUI().revertExpansion();
  }
}
