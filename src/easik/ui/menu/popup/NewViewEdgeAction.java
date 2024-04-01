package easik.ui.menu.popup;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import easik.model.edge.ModelEdge.Cascade;
import easik.sketch.Sketch;
import easik.sketch.edge.SketchEdge;
import easik.sketch.vertex.EntityNode;
import easik.ui.ViewFrame;
import easik.ui.menu.popup.EdgeOptions.Edge;
import easik.view.View;
import easik.view.edge.InjectiveViewEdge;
import easik.view.edge.NormalViewEdge;
import easik.view.edge.PartialViewEdge;
import easik.view.edge.View_Edge;
import easik.view.vertex.QueryNode;

/**
 * This class contains the action for the click on the popup menu to create an
 * new edge in a sketch: it can either be instantiated to create a NormalEdge,
 * InjectiveEdge, PartialEdge, or a self-referencing partial edge
 * (<code>Edge.NORMAL</code>, <code>Edge.INJECTIVE</code>,
 * <code>Edge.PARTIAL</code>, and <code>Edge.SELF</code>, respectively).
 */
public class NewViewEdgeAction extends AbstractAction {
  /**
   *    
   */
  private static final long serialVersionUID = 6386511727462714952L;
  /**
   * The sketch frame in which the new edge will be displayed.
   */
  private ViewFrame _theFrame;

  @SuppressWarnings("unused")
  private Edge type;

  /**
   * Prepare the menu option for a new edge
   *
   * @param inFrame
   * @param type
   */
  public NewViewEdgeAction(ViewFrame inFrame) {

    super("Add edge from original sketch...");

    _theFrame = inFrame;

    /**
     * putValue(AbstractAction.SHORT_DESCRIPTION, (type == Edge.SELF) ? "Adds a
     * self-referencing partial map edge from the selected node back to itself" :
     * (type == Edge.PARTIAL) ? "Connect the two selected nodes with a partial map
     * edge" : (type == Edge.INJECTIVE) ? "Connect the two selected nodes with an
     * injective edge" : "Connect the two selected nodes with an edge");
     */

    putValue(Action.SHORT_DESCRIPTION, "Adds an edge which was present in original sketch - if applicable");
  }

  /**
   * The action for creating a new edge. Make sure the selection is alright, and
   * then create the edge.
   * 
   * @param e The action event
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    View _ourView = _theFrame.getMModel();
    Sketch _ourSketch = _ourView.getSketch();
    boolean foundEdge = false, forward = false, reverse = false;

    // If we're currently synced with a db, give the user the chance to
    // cancel operation
    if (_ourSketch.isSynced()) {
      if (JOptionPane.showConfirmDialog(_theFrame,
          "Warning: this sketch is currently synced with a db; continue and break synchronization?",
          "Warning!", JOptionPane.OK_CANCEL_OPTION,
          JOptionPane.WARNING_MESSAGE) == JOptionPane.CANCEL_OPTION) {
        return;
      }
    }

    Object[] currentSelection = _ourView.getSelectionCells();

    QueryNode currNode = (QueryNode) currentSelection[0];

    String queryString = currNode.getQuery();
    String entityNodeName = null;

    // find corresponding entity node name
    String[] tokens = queryString.split("\\s+");
    for (int i = 0; i < tokens.length; i++) {
      if (tokens[i].equalsIgnoreCase("from")) {
        entityNodeName = tokens[i + 1];
      }
    }

    EntityNode[] node = new EntityNode[2];
    // set corresponding node in order to use
    for (EntityNode sketchNode : _ourSketch.getEntities()) {
      if (sketchNode.getName().equalsIgnoreCase(entityNodeName)) {
        node[0] = sketchNode;

      }
    } // end getting first node

    if (currentSelection.length > 1) {
      currNode = (QueryNode) currentSelection[1];

      queryString = currNode.getQuery();
      entityNodeName = null;

      // find corresponding entity node name
      tokens = queryString.split("\\s+");
      for (int i = 0; i < tokens.length; i++) {
        if (tokens[i].equalsIgnoreCase("from")) {
          entityNodeName = tokens[i + 1];
        }
      }

      // set corresponding node in order to use
      for (EntityNode sketchNode : _ourSketch.getEntities()) {
        if (sketchNode.getName().equalsIgnoreCase(entityNodeName)) {
          node[1] = sketchNode;

        }
      }

      for (SketchEdge edge : _ourSketch.getEdges().values()) {
        View_Edge vEdge;
        forward = (edge.getTargetEntity().equals(node[0]) && edge.getSourceEntity().equals(node[1]));
        reverse = (edge.getTargetEntity().equals(node[1]) && edge.getSourceEntity().equals(node[0]));
        if (forward || reverse) {
          // System.out.println("This edge exists");
          foundEdge = true;

          // need to move down??
          if (edge.isPartial()) {

            // ***NEED TO FIGURE OUT CASCADING
            vEdge = new PartialViewEdge((QueryNode) currentSelection[0], (QueryNode) currentSelection[0],
                edge.getName());

          } else if (edge.isInjective()) {
            // System.out.println("Edge is injective");
            // **NEED TO FIGURE OUT CASCADING
            if (forward) {
              vEdge = new InjectiveViewEdge((QueryNode) currentSelection[1],
                  (QueryNode) currentSelection[0], edge.getName(), Cascade.RESTRICT);
            } else {
              vEdge = new InjectiveViewEdge((QueryNode) currentSelection[0],
                  (QueryNode) currentSelection[1], edge.getName(), Cascade.RESTRICT);
            }
            // System.out.println(vEdge.getName());
          } else {
            if (forward) {
              vEdge = new NormalViewEdge((QueryNode) currentSelection[1], (QueryNode) currentSelection[0],
                  edge.getName());
            } else {
              vEdge = new NormalViewEdge((QueryNode) currentSelection[0], (QueryNode) currentSelection[1],
                  edge.getName());
            }

          }

          _ourView.addEdge(vEdge);

        }

      }
      if (!foundEdge) {
        // System.out.println("This edge does not exist");
      }
    } // end checking if 2 nodes

    // edge that goes into itself
    else {
      /*
       * for(SketchEdge edge: node[0].getOutgoingEdges()) {
       * 
       * }
       */
    }

  }
}
