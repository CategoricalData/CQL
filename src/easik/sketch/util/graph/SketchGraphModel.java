package easik.sketch.util.graph;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;

import org.jgraph.graph.AttributeMap;
import org.jgraph.graph.GraphCell;
import org.jgraph.graph.GraphConstants;

import easik.graph.EasikGraphModel;
import easik.model.constraint.ModelConstraint;
import easik.model.edge.GuideEdge;
import easik.model.edge.TriangleEdge;
import easik.model.states.GetPathState;
import easik.model.ui.ModelFrame.Mode;
import easik.sketch.Sketch;
import easik.sketch.edge.InjectiveEdge;
import easik.sketch.edge.PartialEdge;
import easik.sketch.edge.SketchEdge;
import easik.sketch.vertex.EntityNode;
import easik.ui.SketchFrame;

/**
 *
 *
 * @version 12/09/12
 * @author Christian Fiddick
 */
public class SketchGraphModel extends EasikGraphModel {
  // The mode prefix for colours/widths from the settings based on the current
  // sketch mode

  /**
   *    
   */
  private static final long serialVersionUID = 8791592790763433223L;

  /**  */
  private String _mode;

  // The Sketch this model is for

  /**  */
  private Sketch _sketch;

  /**
   * Constructs a new SketchGraphModel, which overrides cell attributes to display
   * things appropriately for Easik.
   *
   * @param inSketch
   */
  public SketchGraphModel(Sketch inSketch) {
    super();

    _sketch = inSketch;
  }

  /**
   * Overridden method to get cell attributes; we make sure the appropriate
   * attributes are applied to the Easik objects before returning them.
   *
   * @see DefaultGraphModel.getAttributes(Object)
   *
   * @param o
   *
   * @return
   */
  @Override
  @SuppressWarnings("unchecked")
  public AttributeMap getAttributes(Object o) {
    _mode = (_sketch.getFrame().getMode() == Mode.EDIT) ? "edit_" : "manip_";

    if (o instanceof GraphCell) {
      GraphCell cell = (GraphCell) o;
      AttributeMap attribs = cell.getAttributes();
      AttributeMap easikAttribs = null;

      if (cell instanceof SketchEdge) {
        easikAttribs = (cell instanceof InjectiveEdge) ? injectiveEdgeAttributes()
            : (cell instanceof PartialEdge) ? partialEdgeAttributes() : normalEdgeAttributes();
      } else if (cell instanceof TriangleEdge) {
        easikAttribs = triangleEdgeAttributes(
            (TriangleEdge<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge>) cell);
      } else if (cell instanceof GuideEdge) {
        easikAttribs = ((GuideEdge<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge>) cell)
            .isHighlighted() ? virtualHighlightedEdgeAttributes() : virtualEdgeAttributes();
      } else if (cell instanceof ModelConstraint) {
        easikAttribs = virtualVertexAttributes();
      } else if (cell instanceof EntityNode) {
        easikAttribs = vertexAttributes();
      }

      if (easikAttribs != null) {
        if (_sketch.isCellSelected(cell)) {
          Color selColor;
          float lineWidth;

          if (_sketch.getStateManager().peekState() instanceof GetPathState) {
            selColor = getColor("path_selection");
            lineWidth = getWidth("path_selection", 2);
          } else {
            selColor = getColor("selection");
            lineWidth = getWidth("selection", 3);
          }

          int borderWidth = getIntWidth(
              _mode + ((cell instanceof ModelConstraint) ? "constraint" : "entity") + "_border", 1);

          GraphConstants.setBorder(easikAttribs, BorderFactory.createLineBorder(selColor, borderWidth));
          GraphConstants.setForeground(easikAttribs, selColor);
          GraphConstants.setLineColor(easikAttribs, selColor);
          GraphConstants.setLineWidth(easikAttribs, lineWidth);
        }

        if (attribs == null) {
          cell.setAttributes(easikAttribs);

          attribs = easikAttribs;
        } else {
          attribs.applyMap(easikAttribs);
        }

        return attribs;
      }
    }

    return super.getAttributes(o);
  }

  /**
   * Returns an attribute map for normal (non-injective, non-partial) edges.
   *
   * @return a map of attributes to be used for new normal edges
   */
  private AttributeMap normalEdgeAttributes() {
    AttributeMap map = commonEdgeAttributes();

    GraphConstants.setLineEnd(map, GraphConstants.ARROW_TECHNICAL);
    GraphConstants.setEndFill(map, true);
    GraphConstants.setEndSize(map, 10);

    Color color = getColor(_mode + "edge_normal");

    GraphConstants.setLineColor(map, color);
    GraphConstants.setForeground(map, color);
    GraphConstants.setLineWidth(map, getWidth(_mode + "edge_normal", 1.5));

    return map;
  }

  /**
   * Returns the attribute map for injective edges.
   *
   * @return a map of attributes to be used for new injective edges
   */
  private AttributeMap injectiveEdgeAttributes() {
    AttributeMap map = commonEdgeAttributes();

    GraphConstants.setLineEnd(map, GraphConstants.ARROW_TECHNICAL);
    GraphConstants.setEndFill(map, true);
    GraphConstants.setEndSize(map, 10);
    GraphConstants.setLineBegin(map, GraphConstants.ARROW_DIAMOND);
    GraphConstants.setBeginFill(map, false);
    GraphConstants.setBeginSize(map, 15);

    Color color = getColor(_mode + "edge_injective");

    GraphConstants.setLineColor(map, color);
    GraphConstants.setForeground(map, color);
    GraphConstants.setLineWidth(map, getWidth(_mode + "edge_injective", 1.5));

    return map;
  }

  /**
   * Returns the attribute map for partial edges.
   *
   * @return a map of attributes to be used for partial edges
   */
  private AttributeMap partialEdgeAttributes() {
    AttributeMap map = commonEdgeAttributes();

    GraphConstants.setLineEnd(map, GraphConstants.ARROW_TECHNICAL);
    GraphConstants.setEndFill(map, true);
    GraphConstants.setEndSize(map, 10);
    GraphConstants.setLineBegin(map, GraphConstants.ARROW_LINE);
    GraphConstants.setBeginFill(map, true);
    GraphConstants.setBeginSize(map, 15);

    Color color = getColor(_mode + "edge_partial");

    GraphConstants.setLineColor(map, color);
    GraphConstants.setForeground(map, color);
    GraphConstants.setLineWidth(map, getWidth(_mode + "edge_partial", 1.5));

    return map;
  }

  /**
   * Returns the attribute map for virtual edges.
   *
   * @return a map of attributes to be used for new virtual edges
   */
  private AttributeMap virtualEdgeAttributes() {
    AttributeMap map = commonEdgeAttributes();

    GraphConstants.setLineEnd(map, GraphConstants.ARROW_NONE);
    GraphConstants.setLineBegin(map, GraphConstants.ARROW_NONE);
    GraphConstants.setDashPattern(map, new float[] { 5.0f, 3.0f });

    Color color = getColor(_mode + "edge_virtual");

    GraphConstants.setLineColor(map, color);
    GraphConstants.setForeground(map, color);
    GraphConstants.setLineWidth(map, getWidth(_mode + "edge_virtual", 1.5));

    return map;
  }

  /**
   * TRIANGLES CF2012 Set the attributes for display of a triangle edge.
   * 
   * @param edge the edge to read attributes from
   * @return a map of attributes to be used for new triangle edges
   */
  private static AttributeMap triangleEdgeAttributes(
      TriangleEdge<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> edge) {
    AttributeMap map = commonEdgeAttributes();

    GraphConstants.setLineEnd(map, edge.getLineEnd());
    GraphConstants.setLineBegin(map, edge.getLineBegin());
    GraphConstants.setLineWidth(map, edge.getWidth());
    GraphConstants.setLineColor(map, edge.getColor());
    GraphConstants.setForeground(map, edge.getColor());
    GraphConstants.setDashPattern(map, new float[] { edge.getWidth(), 3 });

    return map;
  }

  /**
   * Returns the attribute map for highlighted virtual edges.
   *
   * @return a map of attributes to be used for highlighted virtual edges
   */
  private AttributeMap virtualHighlightedEdgeAttributes() {
    AttributeMap map = virtualEdgeAttributes();
    Color color = getColor(_mode + "edge_virtual_highlighted");

    GraphConstants.setLineColor(map, color);
    GraphConstants.setForeground(map, color);
    GraphConstants.setLineWidth(map, getWidth(_mode + "edge_virtual_highlighted", 1.5));

    return map;
  }

  /**
   * Returns the attribute map for vertices (i.e. EntityNodes).
   *
   * @return a map of attributes to be used for new vertices
   */
  private AttributeMap vertexAttributes() {
    AttributeMap map = new AttributeMap();

    GraphConstants.setAutoSize(map, true);
    GraphConstants.setInset(map, 5);
    GraphConstants.setBorder(map, BorderFactory.createLineBorder(getColor(_mode + "entity_border"),
        getIntWidth(_mode + "entity_border", 1)));
    GraphConstants.setBackground(map, getColor(_mode + "entity_bg"));
    GraphConstants.setForeground(map, getColor(_mode + "entity_fg"));
    GraphConstants.setFont(map, GraphConstants.DEFAULTFONT.deriveFont(Font.BOLD, 12));
    GraphConstants.setOpaque(map, true);

    return map;
  }

  /**
   * Returns the attribute map for virtual vertices (i.e. constaint nodes).
   *
   * @return a map of attributes to be used for new virtual vertices
   */
  private AttributeMap virtualVertexAttributes() {
    AttributeMap map = new AttributeMap();

    GraphConstants.setAutoSize(map, true);
    GraphConstants.setInset(map, 5);
    GraphConstants.setBorder(map, BorderFactory.createLineBorder(getColor(_mode + "constraint_border"),
        getIntWidth(_mode + "constraint_border", 1)));
    GraphConstants.setBackground(map, getColor(_mode + "constraint_bg"));
    GraphConstants.setForeground(map, getColor(_mode + "constraint_fg"));
    GraphConstants.setFont(map, GraphConstants.DEFAULTFONT.deriveFont(Font.BOLD, 12));
    GraphConstants.setOpaque(map, true);

    return map;
  }

  /**
   *
   */
  @Override
  public void clearSelection() {
    _sketch.clearSelection();
  }

  /**
   *
   */
  @Override
  public void setDirty() {
    _sketch.setDirty();
  }
}
