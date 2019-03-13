package easik.model.states;

//~--- non-JDK imports --------------------------------------------------------

import easik.graph.EasikGraphModel;
import easik.model.Model;
import easik.model.constraint.Cone;
import easik.model.constraint.LimitConstraint;
import easik.model.edge.ModelEdge;
import easik.model.path.ModelPath;
import easik.model.ui.ModelFrame;
import easik.model.vertex.ModelVertex;

/**
 *
 */
public class AddLimitConstraintState<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>> extends ModelState<F, GM, M, N, E> implements PathAcceptingState<F, GM, M, N, E> {
	private Cone<F, GM, M, N, E> cone, limitCone1, limitCone2;
	private ModelPath<F, GM, M, N, E> AB, BC, AC;
	private String currCone = "cone";

	public AddLimitConstraintState(M inModel) {
		super(inModel);
	}

	// @Override for PathAcceptingState interface, not for M State
	@Override
	public void passPath(ModelPath<F, GM, M, N, E> path) {
		if (path == null) { // User has clicked cancel button
			_ourModel.getGraphModel().cancelInsignificantUpdate();
			_ourModel.getStateManager().popState();

			return;
		}

		if (AB == null) {
			AB = path;

			_ourModel.getFrame().setInstructionText("Select path BC of " + currCone); // TODO
																						// user
																						// doesn't
																						// know
																						// what
																						// BC
																						// is
			_ourModel.getStateManager().pushState(new GetPathState<>(true, false, _ourModel));
		} else if (BC == null) {
			BC = path;

			_ourModel.getFrame().setInstructionText("Select path AC of " + currCone); // TODO
																						// user
																						// doesn't
																						// know
																						// what
																						// AC
																						// is
			_ourModel.getStateManager().pushState(new GetPathState<>(false, true, _ourModel));
		} else if (AC == null) {
			AC = path;

			if (cone == null) {
				_ourModel.getFrame().setInstructionText("Select path AB of " + currCone); // TODO
																							// user
																							// doesn't
																							// know
																							// what
																							// AB
																							// is
				_ourModel.getStateManager().pushState(new GetPathState<>(true, false, _ourModel));

				currCone = "limitCone1";
				cone = new Cone<>(AB, BC, AC);
			} else if (limitCone1 == null) {
				_ourModel.getFrame().setInstructionText("Select path AB of " + currCone); // TODO
																							// user
																							// doesn't
																							// know
																							// what
																							// AB
																							// is
				_ourModel.getStateManager().pushState(new GetPathState<>(true, false, _ourModel));

				currCone = "limitCone2";
				limitCone1 = new Cone<>(AB, BC, AC);
			} else if (limitCone2 == null) {
				limitCone2 = new Cone<>(AB, BC, AC);

				_ourModel.addConstraint(new LimitConstraint<>(_ourModel, cone, limitCone1, limitCone2));
				_ourModel.getGraphModel().cancelInsignificantUpdate();
				_ourModel.getGraphModel().beginUpdate();
				_ourModel.getGraphModel().endUpdate();
				_ourModel.getStateManager().popState();
			}

			AB = null;
			BC = null;
			AC = null;
		}
	}

	@Override
	public void pushedOn() {
		currCone = "cone";

		_ourModel.clearSelection();
		_ourModel.getStateManager().resetFinished();
		_ourModel.getGraphModel().beginInsignificantUpdate();
		_ourModel.getFrame().setInstructionText("Select path AB of " + currCone); // TODO
																					// user
																					// doesn't
																					// know
																					// what
																					// AB
																					// is
		_ourModel.getFrame().getNextButton().setEnabled(true);
		_ourModel.getFrame().getCancelButton().setEnabled(true);
		_ourModel.getStateManager().pushState(new GetPathState<>(true, false, _ourModel));
	}

	@Override
	public void poppedOff() {
	}

	@Override
	public String toString() {
		return "Add limit constraint state";
	}
}
