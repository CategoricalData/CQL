package easik.view.edge;

/**
*
*
* @version        Summer 2013
* @author         Sarah van der Laan
*/

//~--- non-JDK imports --------------------------------------------------------

import easik.model.keys.UniqueIndexable;
import easik.view.vertex.QueryNode;

/**
 * A subclass of SketchEdge that identifies an edge as a normal (i.e.
 * non-injective, non-partial) edge.
 *
 * @see easik.sketch.edge.SketchEdge
 * 
 *      Based off of NormalEdge.java
 */

public class NormalViewEdge extends View_Edge implements UniqueIndexable {

  private static final long serialVersionUID = 6454905968154847820L;

  /**
   * Creates a new normal (i.e. non-injective, non-partial) edge between two query
   * nodes with the specified name and RESTRICT cascading.
   *
   *
   * @param a
   * @param b
   * @param name The unique edge identifier
   * @see easik.sketch.edge.SketchEdge( easik.sketch.vertex.EntityNode,
   *      easik.sketch.vertex.EntityNode, String)
   * @see easik.sketch.edge.PartialEdge( easik.sketch.vertex.EntityNode,
   *      easik.sketch.vertex.EntityNode, String)
   */
  public NormalViewEdge(QueryNode a, QueryNode b, String name) {
    this(a, b, name, Cascade.RESTRICT);
  }

  /**
   * Creates a new normal (i.e. non-injective, non-partial) edge between two query
   * nodes with the specified name and cascading option.
   *
   *
   * @param a
   * @param b
   * @param name    The unique edge identifier
   * @param cascade The cascade value, such as SketchEdge.Cascade.RESTRICT
   * @see easik.sketch.edge.SketchEdge.Cascade
   * @see easik.sketch.edge.SketchEdge( easik.sketch.vertex.EntityNode,
   *      easik.sketch.vertex.EntityNode, String)
   * @see easik.sketch.edge.PartialEdge( easik.sketch.vertex.EntityNode,
   *      easik.sketch.vertex.EntityNode, String)
   */
  public NormalViewEdge(QueryNode a, QueryNode b, String name, Cascade cascade) {
    super(a, b, name, cascade);
  }
}
