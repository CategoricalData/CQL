package easik.model.edge;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.Color;

import easik.graph.EasikGraphModel;
import easik.model.Model;
import easik.model.constraint.ModelConstraint;
import easik.model.ui.ModelFrame;
import easik.model.vertex.ModelVertex;

/**
 * This edge is really just a virtual edge, and is only used in drawing the
 * limit constraint. This is entirely for display purposes.
 *
 * @author Christian Fiddick
 * @date Summer 2012
 */
public class TriangleEdge<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>> extends GuideEdge<F, GM, M, N, E> {
	/**  */
	private static final long serialVersionUID = 5105470457416734030L;

	/**  */
	private Color color; // color of edge

	/**  */
	private int lineBegin; // constant for line beginning style, i.e
							// GraphConstants.ARROW_CIRCLE

	/**  */
	private int lineEnd; // constant for line ending style, i.e
							// GraphConstants.ARROW_CIRCLE

	/**  */
	private float width; // width of edge

	/**
	 * Creates a guide edge between a constraint vertex and a SketchNode. The
	 * edge will default to highlighted if the node is the domain or codomain of
	 * a path of the constraint, non-highlighted otherwise.
	 *
	 * @param con
	 *            the Constraint object
	 * @param node
	 *            the EntityNode
	 * @param color
	 *            color of edge
	 * @param width
	 *            width of edge
	 * @param lineEnd
	 *            constant for line ending style, i.e
	 *            GraphConstants.ARROW_CIRCLE
	 * @param lineBegin
	 *            constant for line beginning style, i.e
	 *            GraphConstants.ARROW_CIRCLE
	 */
	public TriangleEdge(ModelConstraint<F, GM, M, N, E> con, N node, Color color, float width, int lineEnd, int lineBegin) {
		super(con, node);

		this.color = color;
		this.width = width;
		this.lineEnd = lineEnd;
		this.lineBegin = lineBegin;
	}

	/**
	 * Color getter.
	 * 
	 * @return Color of edge
	 */
	public Color getColor() {
		return color;
	}

	/**
	 * Width getter.
	 * 
	 * @return Width of edge
	 */
	public float getWidth() {
		return width;
	}

	/**
	 * Line end style getter.
	 * 
	 * @return Line end style of edge
	 */
	public int getLineEnd() {
		return lineEnd;
	}

	/**
	 * Line begin style getter.
	 * 
	 * @return Line begin style of edge
	 */
	public int getLineBegin() {
		return lineBegin;
	}
}

// CF2012 TRIANGLES NEW
