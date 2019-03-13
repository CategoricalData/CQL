package easik.view.edge;

//~--- non-JDK imports --------------------------------------------------------

import easik.sketch.edge.SketchEdge;
import easik.view.vertex.QueryNode;

/**
 * A subclass of ViewDefinitionEdge that identifies an edge as an injective
 * edge.
 * 
 * @see View_Edge
 * 
 * 
 * @author Sarah van der Laan 2013
 * 
 *         *Based off of InjectiveEdge.java
 */
public class InjectiveViewEdge extends View_Edge {
	/**
	 *    
	 */
	private static final long serialVersionUID = 1993197478910641975L;

	/**
	 * Creates a new injective edge between two query nodes with the specified
	 * name and RESTRICT cascading.
	 *
	 *
	 * @param a
	 * @param b
	 * @param name
	 *            The unique edge identifier
	 * @see SketchEdge( easik.sketch.vertex.EntityNode,
	 *      easik.sketch.vertex.EntityNode, String)
	 * @see easik.sketch.edge.PartialEdge( easik.sketch.vertex.EntityNode,
	 *      easik.sketch.vertex.EntityNode, String)
	 */
	public InjectiveViewEdge(QueryNode a, QueryNode b, String name) {
		this(a, b, name, Cascade.RESTRICT);
	}

	/**
	 * Creates a new injective edge between two entity nodes with the specified
	 * name and cascading option.
	 *
	 *
	 * @param a
	 * @param b
	 * @param name
	 *            The unique edge identifier
	 * @param cascade
	 *            The cascade value, such as SketchEdge.Cascade.RESTRICT
	 * @see SketchEdge.Cascade
	 * @see SketchEdge( easik.sketch.vertex.EntityNode,
	 *      easik.sketch.vertex.EntityNode, String)
	 * @see easik.sketch.edge.PartialEdge( easik.sketch.vertex.EntityNode,
	 *      easik.sketch.vertex.EntityNode, String)
	 */
	public InjectiveViewEdge(QueryNode a, QueryNode b, String name, Cascade cascade) {
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
