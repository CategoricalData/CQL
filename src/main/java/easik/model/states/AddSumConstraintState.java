package easik.model.states;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.util.ArrayList;

import javax.swing.JOptionPane;

import easik.graph.EasikGraphModel;
import easik.model.Model;
import easik.model.constraint.ModelConstraint;
import easik.model.constraint.SumConstraint;
import easik.model.edge.ModelEdge;
import easik.model.path.ModelPath;
import easik.model.ui.ModelFrame;
import easik.model.vertex.ModelVertex;

/**
 * This state makes a sum constraint. When pushed, it will then push another
 * state to get a path. Once the first path is received, it will push another
 * request for a path. After adding the first path the options to either add
 * another path or finish the constraint are available.
 */
public class AddSumConstraintState<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>>
		extends ModelState<F, GM, M, N, E> implements PathAcceptingState<F, GM, M, N, E> {
	/** Stores whether the user has finished adding paths to this constraint */
	private boolean _finished;

	/** The paths involved in this constraint */
	private ArrayList<ModelPath<F, GM, M, N, E>> _paths;

	/**
	 * If set, we delete this constraint immediately before adding the new one. Used
	 * when adding paths to an existing constraint.
	 */
	private SumConstraint<F, GM, M, N, E> _replace;

	/**
	 * Constructor for creating a new state to collect paths and make a diagram.
	 *
	 * @param inModel The sketch being worked with.
	 */
	public AddSumConstraintState(M inModel) {
		super(inModel);

		_finished = false;
		_paths = new ArrayList<>();
	}

	/**
	 * Constructor for modifying an existing constraint.
	 *
	 * @param inModel  The sketch being worked with
	 * @param existing The existing constraint to have paths added to
	 */
	public AddSumConstraintState(M inModel, SumConstraint<F, GM, M, N, E> existing) {
		super(inModel);

		_finished = false;
		_paths = new ArrayList<>();

		for (ModelPath<F, GM, M, N, E> path : existing.getPaths()) {
			_paths.add(new ModelPath<>(path.getEdges()));
		}

		_replace = existing;
	}

	/**
	 * Called when a new path has been completed by a state above this on the stack.
	 * If it is null, then cancel, otherwise check to see if the user selected the
	 * 'next' or 'finish' option.
	 *
	 * @param inPath The last path in, null if it was a cancellation
	 */
	@Override
	public void passPath(ModelPath<F, GM, M, N, E> inPath) {
		_finished = _ourModel.getStateManager().getFinished();

		// Check if cancelled
		if (inPath == null) {
			_ourModel.getGraphModel().cancelInsignificantUpdate();
			_ourModel.getStateManager().popState();

			return;
		}

		// Check which path is being passed
		if (!_finished) {
			_paths.add(inPath);

			// when we have enough paths, tell the user and leave instructions
			// alone until either finish or cancel
			if (_paths.size() == 1) {
				_ourModel.getFrame()
						.setInstructionText("Select a path beginning with an injective edge with target '"
								+ _paths.get(0).getCoDomain().getName()
								+ "' and press 'Next' to continue, or 'Finish' to add constraint.");
			}

			_ourModel.getStateManager().pushState(
					new GetInjectivePathState<>(true, true, false, _ourModel, null, _paths.get(0).getCoDomain()));
		} else {
			_paths.add(inPath);
			_ourModel.getGraphModel().cancelInsignificantUpdate();
			_ourModel.getGraphModel().beginUpdate();

			if (addDiagram()) {
				// If we're replacing a constraint, remove the old one:
				if (_replace != null) {
					_ourModel.removeConstraint(_replace);
				}
			} else {
				JOptionPane.showMessageDialog(_ourModel.getFrame(),
						"Sum constraint could not be created.\n" + "Please ensure that:\n"
								+ "  1) All edges involved target the same entity\n"
								+ "  2) The first edge in each path is injective\n"
								+ "  3) At least two edges are selected\n" + "  4) All paths are unique",
						"Error", JOptionPane.ERROR_MESSAGE);
			}

			_ourModel.getGraphModel().endUpdate();
			_ourModel.getStateManager().popState();
		}
	}

	/**
	 * Add the diagram to the sketch
	 * 
	 * @return true if the diagram was successfully added, false otherwise
	 */
	@SuppressWarnings("unchecked")
	private boolean addDiagram() {
		if (_ourModel.isSumConstraint(_paths)) {
			ModelConstraint<F, GM, M, N, E> newDiagram = (_replace != null)
					? new SumConstraint<>(_paths, _replace.getX(), _replace.getY(), true, _ourModel)
					: new SumConstraint<>(_paths, _ourModel);

			_ourModel.addNewConstraint(newDiagram);
			_ourModel.setDirty();

			return true;
		}

		return false;
	}

	/**
	 * When this state is pushed on, it sends a message in a popup and then pushes a
	 * path collection state.
	 */
	@Override
	public void pushedOn() {
		_ourModel.clearSelection();
		_ourModel.getStateManager().resetFinished();
		_ourModel.getGraphModel().beginInsignificantUpdate();
		_ourModel.getFrame().setInstructionText((_paths.size() >= 1)
				? "Select a path beginning with an injective edge with target '" + _paths.get(0).getCoDomain().getName()
						+ "' and press 'Next' to continue, or 'Finish' to add constraint."
				: "Select a path beginning with an injective edge and then press 'Next' to add it to the constraint");
		setNextButton(true);
		setFinishButton(_paths.size() >= 1);
		setCancelButton(true);
		_ourModel.getStateManager().pushState(new GetInjectivePathState<>(true, _paths.size() >= 1, false, _ourModel,
				null, (_paths.size() >= 1) ? _paths.get(0).getCoDomain() : null));
	}

	/**
	 * Nothing to do when it gets popped off, as it always pops itself off when
	 * completed.
	 */
	@Override
	public void poppedOff() {
	}

	/**
	 * This state is called "New Sum Constraint".
	 *
	 * @return The string literal described above.
	 */
	@Override
	public String toString() {
		return "New Sum Constraint";
	}
}
