package easik.model.states;

//~--- non-JDK imports --------------------------------------------------------

import easik.graph.EasikGraphModel;
import easik.model.Model;
import easik.model.edge.ModelEdge;
import easik.model.ui.ModelFrame;
import easik.model.vertex.ModelVertex;
import easik.sketch.Sketch;

/**
 * This state is used when adding elements to the graph so that the selection
 * algorithm does not insert them in weird places.
 *
 * @author Kevin Green 2006
 * @since 2006-07-13 Kevin Green
 * @version 2006-07-13 Kevin Green
 * @version 2014-06 Federico Mora
 */
public class LoadingState<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>>
		extends ModelState<F, GM, M, N, E> {
	/**
	 *
	 *
	 * @param inSketch
	 */
	public LoadingState(M model) {
		super(model);
	}

	/**
	 * There is nowhere to go from basic editing (state-wise) so when it is pushed
	 * on, it disables Next and Cancel.
	 */
	@Override
	public void pushedOn() {
		// views won't use these buttons
		if (_ourModel instanceof Sketch) {
			this.setCancelButton(false);
			this.setNextButton(false);
		}
	}

	/**
	 * Empty method, as this should never be popped off.
	 */
	@Override
	public void poppedOff() {
		_ourModel.clearSelection();
	}

	/**
	 * Used to identify this class.
	 *
	 * @return String literal "Basic Editing"
	 */
	@Override
	public String toString() {
		return "Loading";
	}
}
