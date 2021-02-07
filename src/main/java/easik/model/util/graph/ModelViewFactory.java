package easik.model.util.graph;

//~--- non-JDK imports --------------------------------------------------------

import org.jgraph.graph.DefaultCellViewFactory;
import org.jgraph.graph.EdgeView;
import org.jgraph.graph.VertexView;

import easik.graph.EasikGraphModel;
import easik.model.Model;
import easik.model.edge.ModelEdge;
import easik.model.ui.ModelFrame;
import easik.model.vertex.ModelVertex;

/**
 * Generates JGraph View objects for Easik sketch elements.
 */

/**
 *
 *
 * @version 12/09/12
 * @author Christian Fiddick
 */
public class ModelViewFactory<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>>
		extends DefaultCellViewFactory {
	/**
	 *    
	 */
	private static final long serialVersionUID = 2950493625625444566L;

	/**
	 *
	 */
	public ModelViewFactory() {
		super();
	}

	/**
	 * Creates an EdgeView that represents the passed-in edge.
	 *
	 * @param edge
	 *
	 * @return
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected EdgeView createEdgeView(Object edge) {
		if (edge instanceof ModelEdge) {
			return new ModelEdgeView<>((E) edge);
		}

		return super.createEdgeView(edge);
	}

	/**
	 * Creates a VertexView for viewing the passed-in vertex.
	 *
	 * @param vertex
	 *
	 * @return
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected VertexView createVertexView(Object vertex) {
		return new ModelVertexView<>((N) vertex);
	}
}
