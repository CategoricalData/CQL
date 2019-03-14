package easik.model.util.graph;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.geom.Point2D;

import org.jgraph.graph.EdgeView;
import org.jgraph.graph.VertexView;

import easik.graph.EasikGraphModel;
import easik.model.Model;
import easik.model.edge.ModelEdge;
import easik.model.ui.ModelFrame;
import easik.model.vertex.ModelVertex;

/**
 * A view class for SketchVertexes (EntityNodes and Constraints). We override
 * this to use our own Vertex renderer, since the default JGraph one is based on
 * a JLabel, which is inappropriate for Easik.
 *
 * @see org.jgraph.graph.VertexView
 */
public class ModelVertexView<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>>
		extends VertexView {
	/**
	 *    
	 */
	private static final long serialVersionUID = -5757046501364392384L;

	/** The renderer for this view */
	@SuppressWarnings("rawtypes")
	public static transient ModelVertexRenderer _renderer = new ModelVertexRenderer();

	/**
	 * Sets the cell that this view will be used to render.
	 *
	 * @param vertex The SketchVertex to be rendered by this view.
	 */
	public ModelVertexView(ModelVertex<F, GM, M, N, E> vertex) {
		super(vertex);
	}

	/**
	 * Returns our made-for-Easik vertex renderer
	 *
	 * @return
	 */
	@Override
	public ModelVertexRenderer<F, GM, M, N, E> getRenderer() {
		return _renderer;
	}

	/**
	 * Returns a perimeter point for edges to use.
	 *
	 * @param edge   The edge
	 * @param source The source of the edge
	 * @param p      The desitination of the edge
	 * @return The perimeter intersection point
	 */
	@Override
	public Point2D getPerimeterPoint(EdgeView edge, Point2D source, Point2D p) {
		return getRenderer().getPerimeterPoint(this, source, p);
	}
}
