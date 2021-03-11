package easik.view.edge;

//~--- non-JDK imports --------------------------------------------------------

import easik.model.keys.UniqueIndexable;
import easik.sketch.edge.SketchEdge;
import easik.view.vertex.QueryNode;

/**
 * A subclass of ViewDefinitionEdge that identifies an edge as a partial map
 * edge. Partial map edges are like normal edges, but they are permitted to not
 * reference a foreign element: in exported SQL, this means the foreign key is
 * permitted to contain NULL.
 *
 * @see View_Edge
 * 
 * 
 * @author Sarah van der Laan 2013
 * 
 *         **Based off of PartialEdge.java
 */
public class PartialViewEdge extends View_Edge implements UniqueIndexable {
  /**
   *    
   */
  private static final long serialVersionUID = 1796525114292808241L;

  /**
   * Creates a new partial map edge between two entity nodes with the specified
   * name and SET_NULL cascading.
   *
   *
   * @param a
   * @param b
   * @param name The unique edge identifier
   * @see SketchEdge.Cascade
   * @see SketchEdge( easik.sketch.vertex.EntityNode,
   *      easik.sketch.vertex.EntityNode, String)
   * @see InjectiveEdge( easik.sketch.vertex.EntityNode,
   *      easik.sketch.vertex.EntityNode, String)
   */
  public PartialViewEdge(QueryNode a, QueryNode b, String name) {
    this(a, b, name, Cascade.SET_NULL);
  }

  /**
   * Creates a new partial map edge between two entity nodes with the specified
   * name and cascading option.
   *
   *
   * @param a
   * @param b
   * @param name    The unique edge identifier
   * @param cascade The cascading option, such as
   *                <code>SketchEdge.Cascade.SET_NULL</code>
   * @see SketchEdge.Cascade
   * @see SketchEdge( easik.sketch.vertex.EntityNode,
   *      easik.sketch.vertex.EntityNode, String)
   * @see InjectiveEdge( easik.sketch.vertex.EntityNode,
   *      easik.sketch.vertex.EntityNode, String)
   */
  public PartialViewEdge(QueryNode a, QueryNode b, String name, Cascade cascade) {
    super(a, b, name, cascade);
  }

  /**
   *
   *
   * @return
   */
  @Override
  public boolean isPartial() {
    return true;
  }
}
