package easik.graph;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import org.jgraph.graph.AbstractCellView;
import org.jgraph.graph.CellView;
import org.jgraph.graph.DefaultGraphModel;
import org.jgraph.graph.EdgeView;
import org.jgraph.graph.GraphLayoutCache;

/**
 * A routing algorithm that routes multiple edges in parallel, but separated. We
 * extend JGraph's ParallelEdgeRouter to also handle separating parallel loops.
 */
public class EdgeRouter extends org.jgraph.util.ParallelEdgeRouter {
	/**
	 *    
	 */
	private static final long serialVersionUID = -3809619596358539665L;

	/**
	 * Singleton to reach parallel edge router
	 */
	@SuppressWarnings("hiding")
	protected static final EdgeRouter sharedInstance = new EdgeRouter();

	/**
	 * Getter for singleton managing parallel edges
	 *
	 * @return ParallelEdgeRouter for parallel edges
	 */
	public static EdgeRouter getSharedInstance() {
		return sharedInstance;
	}

	/**
	 * Calculates intermediate points for multiple loops. This is the same
	 * algorithm used by DefaultEdge.LoopRouting, but we scale the loop box for
	 * parallel self-referencing edges.
	 *
	 * @param cache
	 * @param edge
	 *
	 * @return
	 */
	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List routeLoop(final GraphLayoutCache cache, final EdgeView edge) {
		final CellView sourceNode = edge.getSource();
		final List newPoints = new ArrayList();

		newPoints.add(sourceNode);

		final CellView sourceParent = (sourceNode != null) ? sourceNode.getParentView() : edge.getSourceParentView();
		if (sourceNode == null) {
			throw new RuntimeException("Internal EASIK error, please report");
		}
		final Object[] edges = DefaultGraphModel.getEdgesBetween(cache.getModel(), sourceNode.getCell(), sourceNode.getCell(), true);
		int position = 0;

		if (edges != null) {
			for (int i = 0; i < edges.length; i++) {
				if (edges[i] == edge.getCell()) {
					position = i;

					break;
				}
			}
		}

		if (sourceParent != null) {
			final Point2D from = AbstractCellView.getCenterPoint(sourceParent);
			final Rectangle2D rect = sourceParent.getBounds();
			final double posWidthFactor = 1.25 + 0.75 * position;
			final double posHeightFactor = 1.5 + position;
			final double width = rect.getWidth();
			final double height2 = rect.getHeight() / 2;
			double loopWidth = Math.min(20, Math.max(10, width / 8));
			double loopHeight = Math.min(30, Math.max(12, Math.max(loopWidth + 4, height2 / 2)));

			loopWidth *= posWidthFactor;
			loopHeight *= posHeightFactor;

			newPoints.add(edge.getAttributes().createPoint(from.getX() - loopWidth, from.getY() - height2 - loopHeight * 1.0));

			final double midpointY = from.getY() - height2 - 1.5 * loopHeight;

			newPoints.add(edge.getAttributes().createPoint(from.getX(), midpointY));
			newPoints.add(edge.getAttributes().createPoint(from.getX() + loopWidth, from.getY() - height2 - loopHeight * 1.0));
			newPoints.add(edge.getTarget());

			return newPoints;
		}

		return null;
	}
}
