package easik.sketch.edge;

//~--- non-JDK imports --------------------------------------------------------

import easik.model.keys.UniqueIndexable;
import easik.sketch.vertex.EntityNode;

/**
 * A subclass of SketchEdge that identifies an edge as a partial map edge.
 * Partial map edges are like normal edges, but they are permitted to not
 * reference a foreign element: in exported SQL, this means the foreign key is
 * permitted to contain NULL.
 *
 * @see SketchEdge
 */
public class PartialEdge extends SketchEdge implements UniqueIndexable {
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
	public PartialEdge(EntityNode a, EntityNode b, String name) {
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
	public PartialEdge(EntityNode a, EntityNode b, String name, Cascade cascade) {
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
