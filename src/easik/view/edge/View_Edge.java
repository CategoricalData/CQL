package easik.view.edge;

/**
*
*
* @version        Summer 2013
* @author         Sarah van der Laan
*/

//~--- non-JDK imports --------------------------------------------------------

import org.jgraph.graph.AttributeMap;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultPort;
import org.jgraph.graph.GraphConstants;

import easik.model.edge.ModelEdge;
import easik.sketch.edge.NormalEdge;
import easik.ui.ViewFrame;
import easik.view.View;
import easik.view.util.graph.ViewGraphModel;
import easik.view.vertex.QueryNode;

/**
 * This is an abstract class to represent an arrow between two query nodes.
 * 
 * Based off of SketchEdge.java
 * 
 * @author Sarah van der Laan
 */

public abstract class View_Edge extends ModelEdge<ViewFrame, ViewGraphModel, View, QueryNode, View_Edge> {

  /**
   *    
   */
  private static final long serialVersionUID = 5526354785981169589L;

  /**
   * The query nodes that acts as this edge's source and target
   */
  private QueryNode _sourceObj, _targetObj;

  /**
   * Stores the name of this edge
   */
  private String _uniqueName;

  /**
   * Creates a new edge between two entity nodes with the specified name.
   *
   * @param source  The source entity node
   * @param target  The target entity node
   * @param name    The unique edge identifier
   * @param cascade The Cascade option for this edge (Cascade.CASCADE,
   *                Cascade.RESTRICT, Cascade.SET_NULL)
   * @see NormalEdge
   * @see easik.sketch.edge.InjectiveEdge
   * @see easik.sketch.edge.PartialEdge
   */
  protected View_Edge(final QueryNode source, final QueryNode target, final String name, final Cascade cascade) {
    super(source, target);
    _sourceObj = source;
    _targetObj = target;
    _uniqueName = name;

    setCascading(cascade);
  }

  /**
   * Getter method for the source QueryNode of this sketch edge
   * 
   * @return The source of this edge, defined to be an queryNode.
   */
  public QueryNode getSourceQueryNode() {
    return _sourceObj;
  }

  /**
   * Getter method for the target of this sketch edge
   * 
   * @return The target of this edge, defined to be an entityNode.
   */
  public QueryNode getTargetQueryNode() {
    return _targetObj;
  }

  /**
   * Returns the source port of this edge.
   *
   * @see DefaultEdge#getSource()
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
   * @see DefaultEdge#getSource()
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
    return getName();
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
   *
   *
   **/
  public void setName(String inName) {
    final String oldName = getName();

    inName = _sourceObj.getMModel().edgeRenamed(this, oldName, inName);
    _uniqueName = inName;

    _sourceObj.getMModel().refresh(this);
  }

  /**
   * Accessor for the injective attribute. For a SketchEdge object, this always
   * return false, however injective edge subclasses override this.
   *
   * @return true if this edge is an injective edge (one-to-one); false otherwise
   */
  @Override
  public boolean isInjective() {
    return false;
  }

  /**
   * Override the super getAttributes() to reflect whether or not this edge is
   * selectable.
   *
   * @return
   * 
   *     NEED TO WORK ON THIS METHOD
   * 
   */

  @Override
  public AttributeMap getAttributes() {
    final AttributeMap attrs = super.getAttributes();

    // A SketchEdge is selectable unless we're in manipulation mode
    // final View view = _sourceObj.getMModel();

    GraphConstants.setSelectable(attrs, false);

    return attrs;
  }

  /**
   *
   *
   * @param pattern
   *
   * @return
   */
  public String getForeignKeyName(final String pattern) {
    return pattern.replaceAll("<target>", getTargetQueryNode().getName())
        .replaceAll("<source>", getSourceQueryNode().getName()).replaceAll("<edge>", getName());
  }
}
