package easik.model.states;

//~--- non-JDK imports --------------------------------------------------------

import easik.graph.EasikGraphModel;
import easik.model.Model;
import easik.model.edge.ModelEdge;
import easik.model.path.ModelPath;
import easik.model.ui.ModelFrame;
import easik.model.vertex.ModelVertex;

/**
 * States implementing this interface can accept paths. Use this interface
 * whenever a state will be sent paths.
 *
 * @author Rob Fletcher 2005
 * @author Kevin Green 2006
 * @version 2006-06-13 Kevin Green
 * @version 06-2014 Federico Mora
 */
public interface PathAcceptingState<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>> {
	/**
	 * Hook used by states to transmit information about a path.
	 *
	 * @param path A ModelPath<SketchFrame, SketchGraphModel,Sketch,EntityNode,
	 *             SketchEdge> element.
	 */
	public void passPath(ModelPath<F, GM, M, N, E> path);
}
