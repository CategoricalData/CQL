package easik.overview.edge;

//~--- non-JDK imports --------------------------------------------------------

import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultPort;

import easik.overview.vertex.SketchNode;
import easik.overview.vertex.ViewNode;

/**
 * This is a class to represent an edge between a view and a sketch.
 */
public class ViewDefinitionEdge extends DefaultEdge {
  /**
   *    
   */
  private static final long serialVersionUID = -3676434961662947442L;

  /** The view node that acts as this edge's source */
  private ViewNode _sourceObj;

  /** The sketch node that acts as this edge's target */
  private SketchNode _targetObj;

  /** Stores the name of this edge */
  private String _uniqueName;

  /**
   * Creates a new edge between a view node and a sketch node with the specified
   * name.
   *
   * @param a    The source view node
   * @param b    The target sketch node
   * @param name The unique edge identifier
   */
  public ViewDefinitionEdge(ViewNode a, SketchNode b, String name) {
    _sourceObj = a;
    _targetObj = b;
    _uniqueName = name;
  }

  /**
   * Getter method for the source EntityNode of this sketch edge
   * 
   * @return The source of this edge, defined to be an entityNode.
   */
  public ViewNode getSourceNode() {
    return _sourceObj;
  }

  /**
   * Getter method for the target of this sketch edge
   * 
   * @return The target of this edge, defined to be an entityNode.
   */
  public SketchNode getTargetNode() {
    return _targetObj;
  }

  /**
   * Returns the source port of this edge.
   *
   * @see DefaultEdge.getSource()
   *
   * @return
   */
  @Override
  public DefaultPort getSource() {
    return _sourceObj.getPort();
  }

  /**
   * Returns the target port of this edge.
   *
   * @see DefaultEdge.getSource()
   *
   * @return
   */
  @Override
  public DefaultPort getTarget() {
    return _targetObj.getPort();
  }

  /**
   * toString method returns the common name.
   * 
   * @return Name of the edge
   */
  @Override
  public String toString() {
    return _uniqueName;
  }

  /**
   * Accessor for the unique ID
   * 
   * @return The unique ID
   */
  public String getName() {
    return _uniqueName;
  }

  /**
   * Sets the name of this edge.
   * 
   * @param inName The unique name of the edge.
   */
  public void setName(String inName) {
    String oldName = getName();

    inName = _sourceObj.getOverview().viewEdgeRenamed(this, oldName, inName);
    _uniqueName = inName;
  }
}
