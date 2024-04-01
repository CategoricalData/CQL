package easik.sketch.edge;

//~--- non-JDK imports --------------------------------------------------------

import easik.model.keys.UniqueIndexable;
import easik.sketch.vertex.EntityNode;

/**
 * A subclass of SketchEdge that identifies an edge as a normal (i.e.
 * non-injective, non-partial) edge.
 *
 * @see easik.sketch.edge.SketchEdge
 */
public class NormalEdge extends SketchEdge implements UniqueIndexable {
  /**
   *    
   */
  private static final long serialVersionUID = 6454905968154847820L;

  /**
   * Creates a new normal (i.e. non-injective, non-partial) edge between two
   * entity nodes with the specified name and RESTRICT cascading.
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
  public NormalEdge(EntityNode a, EntityNode b, String name) {
    this(a, b, name, Cascade.RESTRICT);
  }

  /**
   * Creates a new normal (i.e. non-injective, non-partial) edge between two
   * entity nodes with the specified name and cascading option.
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
  public NormalEdge(EntityNode a, EntityNode b, String name, Cascade cascade) {
    super(a, b, name, cascade);
  }
}
