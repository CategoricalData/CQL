package easik.sketch.edge;

//~--- non-JDK imports --------------------------------------------------------

import easik.sketch.vertex.EntityNode;

/**
 * A subclass of SketchEdge that identifies an edge as an injective edge.
 * 
 * @see SketchEdge
 */
public class InjectiveEdge extends SketchEdge {
	/**
	 *    
	 */
	private static final long serialVersionUID = 1993197478910641975L;

	/**
	 * Creates a new injective edge between two entity nodes with the specified name
	 * and RESTRICT cascading.
	 *
	 *
	 * @param a
	 * @param b
	 * @param name The unique edge identifier
	 * @see SketchEdge( easik.sketch.vertex.EntityNode,
	 *      easik.sketch.vertex.EntityNode, String)
	 * @see easik.sketch.edge.PartialEdge( easik.sketch.vertex.EntityNode,
	 *      easik.sketch.vertex.EntityNode, String)
	 */
	public InjectiveEdge(EntityNode a, EntityNode b, String name) {
		this(a, b, name, Cascade.RESTRICT);
	}

	/**
	 * Creates a new injective edge between two entity nodes with the specified name
	 * and cascading option.
	 *
	 *
	 * @param a
	 * @param b
	 * @param name    The unique edge identifier
	 * @param cascade The cascade value, such as SketchEdge.Cascade.RESTRICT
	 * @see SketchEdge.Cascade
	 * @see SketchEdge( easik.sketch.vertex.EntityNode,
	 *      easik.sketch.vertex.EntityNode, String)
	 * @see easik.sketch.edge.PartialEdge( easik.sketch.vertex.EntityNode,
	 *      easik.sketch.vertex.EntityNode, String)
	 */
	public InjectiveEdge(EntityNode a, EntityNode b, String name, Cascade cascade) {
		super(a, b, name, cascade);
	}

	/**
	 *
	 *
	 * @return
	 */
	@Override
	public boolean isInjective() {
		return true;
	}
}
