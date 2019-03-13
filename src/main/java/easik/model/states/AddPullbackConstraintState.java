package easik.model.states;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.util.ArrayList;

import javax.swing.JOptionPane;

import easik.graph.EasikGraphModel;
import easik.model.Model;
import easik.model.constraint.ConstraintException;
import easik.model.constraint.PullbackConstraint;
import easik.model.edge.ModelEdge;
import easik.model.path.ModelPath;
import easik.model.ui.ModelFrame;
import easik.model.vertex.ModelVertex;

/**
 * This state makes a pullback constraint. When pushed, it will then push
 * another state to get a path. Once the first path is received, it will push
 * another request for a path. This repeated until all paths have been collected
 * and then the constraint is created (as long as the paths are valid).
 *
 * @author Kevin Green 2006
 * @author Vera Ranieri 2006
 * @since 2006-05-23 Kevin Green
 * @version 2006-08-15 Kevin Green
 * @version Christian Fiddick Summer 2012
 * @version 06-2014 Federico Mora
 */
public class AddPullbackConstraintState<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>> extends ModelState<F, GM, M, N, E> implements PathAcceptingState<F, GM, M, N, E> {
	/** Stores whether the user has finished adding paths to this constraint */
	@SuppressWarnings("unused")
	private boolean _finished;

	/** The paths involved in this constraint */
	private ArrayList<ModelPath<F, GM, M, N, E>> _paths;

	/** The current path number being selected */
	private int _round;

	/** The width of the constraint, so we expect 2*width paths */
	private int width;

	/**
	 * Constructor for creating a new state to collect paths and make a diagram.
	 * 
	 * @param inModel
	 *            The sketch being worked with.
	 * @param width
	 *            Number of paths to target node
	 */
	public AddPullbackConstraintState(M inModel, int width) {
		super(inModel);

		_finished = false;
		_paths = new ArrayList<>();
		_round = 0;

		if (width < 2) // error, but just correct and proceed
		{
			this.width = 2;
		} else {
			this.width = width;
		}
	}

	/**
	 * Helper method to determine if the ith source path should be injective.
	 * 
	 * @param i
	 *            Path from source to intermediary node to check
	 * @return True if path should be injective, false otherwise
	 */
	private boolean shouldBeInjective(int i) {
		if (width != 2) {
			return false; // Dr. Rosebrugh proved this... I think.
		}

		for (int k = 0; k < width; k++) {
			if (k == i - width) {
				continue;
			}

			if (_paths.get(k).isInjective()) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Called when a new path has been completed by a state above this on the
	 * stack. If it is null, then cancel, otherwise add it to the collection.
	 * When we get width*2 paths, then we're finished (if they share a domain
	 * and codomain).
	 *
	 * @param inPath
	 *            The last path in, null if it was a cancellation
	 */
	@Override
	public void passPath(ModelPath<F, GM, M, N, E> inPath) {
		_round++;

		// the button state for the next "get path"
		boolean next = _round < width * 2 - 1;
		boolean finished = _round == width * 2 - 1;

		// set on invalid path selection
		String error = null;

		// check if cancelled
		if (inPath == null) {
			_ourModel.getGraphModel().cancelInsignificantUpdate();
			_ourModel.getStateManager().popState();

			return;
		}

		N codomain = inPath.getCoDomain();

		if (_round <= width) { // selecting target paths
			if (_round == 1) {
				_paths.add(inPath);
				_ourModel.getFrame().setInstructionText("Select a fully defined path " + (_round + 1) + " to pullback codomain ('" + codomain.getName() + "'), and press 'Next'.");
				_ourModel.getStateManager().pushState(new GetFullyDefinedPathState<>(next, finished, _ourModel, null, codomain, true));
			} else {
				if (codomain != _paths.get(0).getCoDomain()) {
					error = "Invalid path selection: Selected codomain did not equal '" + _paths.get(0).getCoDomain().getName() + "'.";
				} else {
					_paths.add(inPath);

					if (_round == width) {
						boolean injective = shouldBeInjective(_round);
						N target = _paths.get(0).getDomain();

						_ourModel.getFrame().setInstructionText("Select " + (injective ? "an injective" : "a") + " pullback projection with target node '" + target.getName() + "', and press 'Next'");
						_ourModel.getStateManager().pushState(injective ? new GetInjectivePathState<>(next, finished, false, _ourModel, null, target) : new GetFullyDefinedPathState<>(next, finished, _ourModel, null, target, true));
					} else {
						_ourModel.getFrame().setInstructionText("Select path " + (_round + 1) + " to pullback codomain ('" + codomain.getName() + "'), and press 'Next'.");
						_ourModel.getStateManager().pushState(new GetFullyDefinedPathState<>(next, finished, _ourModel, null, codomain, true));
					}
				}
			}
		} else { // selecting source paths
			if (_round == width * 2) {
				_paths.add(inPath);

				if (addDiagram()) {
					JOptionPane.showMessageDialog(_ourModel.getParent(), "Created a Pullback Constraint", "EASIK", JOptionPane.INFORMATION_MESSAGE);
					_ourModel.getStateManager().popState();

					return;
				} 
					if (codomain != _paths.get((_round - width) - 1).getDomain()) {
						error = "Invalid path selection: Selected codomain did not equal '" + _paths.get(width).getCoDomain() + "'.";
					} 
						error = "Paths must be fully defined.";
					

				
			} else {
				if (codomain != _paths.get((_round - width) - 1).getDomain()) {
					error = "Invalid path selection: Selected codomain did not equal '" + _paths.get((_round - width) - 1).getDomain() + "'.";
				} else {
					_paths.add(inPath);

					boolean injective = shouldBeInjective(_round);
					N source = inPath.getDomain();
					N target = _paths.get(_round - width).getDomain();
					String button = (_round == width * 2 - 1) ? "Finish" : "Next";

					_ourModel.getFrame().setInstructionText("Select " + (injective ? "an injective" : "a") + " pullback projection from '" + source.getName() + "' to '" + target.getName() + "', and press '" + button + '\'');
					_ourModel.getStateManager().pushState(injective ? new GetInjectivePathState<>(next, finished, false, _ourModel, source, target) : new GetFullyDefinedPathState<>(next, finished, _ourModel, source, target, true));
				}
			}
		}

		// if error, tell user and pop this state.
		if (error != null) {
			JOptionPane.showMessageDialog(_ourModel.getParent(), error + " Pullback constraint not created.", "Error", JOptionPane.ERROR_MESSAGE);
			_ourModel.getGraphModel().cancelInsignificantUpdate();
			_ourModel.getStateManager().popState();
		}
	}

	/**
	 * Add the diagram to the sketch
	 *
	 * @return true if the constraint was successfully added to the sketch,
	 *         false otherwise
	 */
	@SuppressWarnings("unchecked")
	private boolean addDiagram() {
		ArrayList<ModelPath<F, GM, M, N, E>> tmpSkp = (ArrayList<ModelPath<F, GM, M, N, E>>) _ourModel.asPullbackConstraint(_paths);

		if (tmpSkp != null) {
			_paths = tmpSkp;

			_ourModel.getGraphModel().cancelInsignificantUpdate();

			try {
				PullbackConstraint<F, GM, M, N, E> newDiagram = new PullbackConstraint<>(_paths, _ourModel);

				_ourModel.addNewConstraint(newDiagram);
				_ourModel.setDirty();

				return true;
			} catch (ConstraintException ce) {
			}
		}

		return false;
	}

	/**
	 * When this state is pushed on, it sends a message in a popup and then
	 * pushes a path collection state.
	 */
	@Override
	public void pushedOn() {
		_ourModel.clearSelection();
		_ourModel.getStateManager().resetFinished();
		_ourModel.getGraphModel().beginInsignificantUpdate();
		_ourModel.getFrame().setInstructionText("Select first fully defined path to the pullback codomain and click 'Next'.");
		_ourModel.getFrame().getNextButton().setEnabled(true);
		_ourModel.getFrame().getCancelButton().setEnabled(true);
		_ourModel.getStateManager().pushState(new GetFullyDefinedPathState<>(true, false, _ourModel, true));
	}

	/**
	 * Nothing to do when it gets popped off, as it always pops itself off when
	 * completed.
	 */
	@Override
	public void poppedOff() {
	}

	/**
	 * This state is called "New Pullback Constraint"
	 *
	 * @return The string literal described above.
	 */
	@Override
	public String toString() {
		return "New Pullback Constraint";
	}
}

// WPBEDIT CF2012 EDIT
