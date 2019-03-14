package easik.model.states;

//~--- non-JDK imports --------------------------------------------------------

import easik.graph.EasikGraphModel;
import easik.model.Model;
import easik.model.edge.ModelEdge;
import easik.model.ui.ModelFrame;
import easik.model.vertex.ModelVertex;

/**
 * The basic editing state is the lowest state of the state manager. It can not
 * be popped off. Basic editing allows access to the basic tools of building
 * sketches.
 *
 * @author Rob Fletcher 2005
 * @author Kevin Green 2006
 * @version 2006-07-13 Kevin Green
 * @version 06-2014 Federico Mora
 */
public class BasicEditingState<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>>
		extends ModelState<F, GM, M, N, E> {
	/**
	 *
	 *
	 * @param inSketch
	 */
	public BasicEditingState(M inModel) {
		super(inModel);
	}

	/**
	 * There is nowhere to go from basic editing (state-wise) so when it is pushed
	 * on, it disables Next and Cancel.
	 */
	@Override
	public void pushedOn() {
		this.setCancelButton(false);
		this.setNextButton(false);
	}

	/**
	 * Empty method, as this should never be popped off.
	 */
	@Override
	public void poppedOff() {
	}

	/**
	 * When we're in basic editing state, we want to be able to add constraints.
	 */
	@Override
	public void gotFocus() {
		_ourModel.getFrame().enableAddConstraintItems(true);
	}

	/**
	 * Used to identify this class.
	 *
	 * @return String literal "Basic Editing"
	 */
	@Override
	public String toString() {
		return "Basic Editing";
	}

	/**
	 * Update the selection so that the only selectable items will be those within
	 * reach of the existing edges.
	 */
	@Override
	public void selectionUpdated() {
		setNextButton(false);
		setFinishButton(false);
		_ourModel.getGraphLayoutCache().reload();
	}
}
