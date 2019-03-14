package easik.model.util.graph;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.geom.Point2D;

import org.jgraph.graph.EdgeView;

import easik.graph.EasikGraphModel;
import easik.model.Model;
import easik.model.edge.ModelEdge;
import easik.model.ui.ModelFrame;
import easik.model.vertex.ModelVertex;

/**
 * A view class for the entity edges. We only use this to override the label
 * vector for self-referencing loops: the default (average all the points) is
 * completely useless. Instead, we change it to go to the middle point, instead
 * of to the average point.
 */
public class ModelEdgeView<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>>
		extends EdgeView {
	/**
	 *    
	 */
	private static final long serialVersionUID = -5637564708747928527L;

	/**
	 *
	 *
	 * @param node
	 */
	public ModelEdgeView(E node) {
		super(node);
	}

	/**
	 *
	 *
	 * @return
	 */
	@Override
	public Point2D getLabelVector() {
		if ((labelVector == null) && isLoop() && (getPointCount() == 5)) {
			@SuppressWarnings("unused")
			Point2D p0 = getPoint(0), p2 = getPoint(2), p4 = getPoint(4);

			return (labelVector = new Point2D.Double(p2.getX() - p0.getX(), (p2.getY() - p0.getY() + 10) * 2));
		}

		return super.getLabelVector();
	}
}
