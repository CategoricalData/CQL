package easik.model.states;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.util.ArrayList;

import javax.swing.JOptionPane;

import easik.graph.EasikGraphModel;
import easik.model.Model;
import easik.model.constraint.ModelConstraint;
import easik.model.constraint.ProductConstraint;
import easik.model.edge.ModelEdge;
import easik.model.path.ModelPath;
import easik.model.ui.ModelFrame;
import easik.model.vertex.ModelVertex;

/**
 * This state makes a product constraint. When pushed, it will then push another
 * state to get a path. Once the first path is received, it will push another
 * request for a path. After adding the first path the options to either add
 * another path or finish the constraint are available.
 *
 * @author Kevin Green 2006
 * @author Vera Ranieri 2006
 * @since 2006-05-23 Kevin Green
 * @version 2006-08-15 Kevin Green
 */
public class AddProductConstraintState<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>>
		extends ModelState<F, GM, M, N, E> implements PathAcceptingState<F, GM, M, N, E> {
	/**
	 * Stores whether the selection of paths has finished for this constraint
	 */
	private boolean _finished;

	/**
	 * The paths involved in this constraint
	 */
	private ArrayList<ModelPath<F, GM, M, N, E>> _paths;

	/**
	 * If set, we delete this constraint immediately before adding the new one. Used
	 * when adding paths to an existing constraint.
	 */
	private ProductConstraint<F, GM, M, N, E> _replace;

	/**
	 * Constructor for creating a new state to collect paths and make a diagram.
	 *
	 * @param inModel The sketch being worked with.
	 */
	public AddProductConstraintState(M inModel) {
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
	public AddProductConstraintState(M inModel, ProductConstraint<F, GM, M, N, E> existing) {
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

		// if we have a duplicate path, let user try again
		else if (_paths.contains(inPath)) {
			JOptionPane.showMessageDialog(_ourModel.getParent(), "Cannot select duplicate path. Try Again!", "Easik",
					JOptionPane.INFORMATION_MESSAGE);
			_ourModel.getStateManager().pushState(new GetFullyDefinedPathState<>(true, _paths.size() > 0, _ourModel,
					_paths.get(0).getDomain(), null, true));

			return;
		}

		// if we are on our 2nd+ path it's invalid, let user retry
		else if ((_paths.size() > 0) && !(inPath.getDomain() == _paths.get(0).getDomain())) {
			JOptionPane.showMessageDialog(_ourModel.getParent(),
					"Must select path starting from '" + _paths.get(0).getDomain() + "'. Try again!", "Easik",
					JOptionPane.INFORMATION_MESSAGE);

			// let user try again
			_ourModel.getStateManager().pushState(new GetFullyDefinedPathState<>(true, _paths.size() > 0, _ourModel,
					_paths.get(0).getDomain(), null, true));

			return;
		}

		// if still not finished, add path and push new state to allow user to
		// select another path
		if (!_finished) {
			// if we got this far, our new path is OK
			_paths.add(inPath);

			// once we have selected our first path, we can update instructions
			// and leave until finished or canceled
			if (_paths.size() == 1) {
				_ourModel.getFrame().setInstructionText("Select a path with domain '" + inPath.getDomain().getName()
						+ "', then press 'Next' for another path or 'Finish' to accept.");
			}

			_ourModel.getStateManager()
					.pushState(new GetFullyDefinedPathState<>(true, true, _ourModel, inPath.getDomain(), null, true));
		}

		// add the path and create constraint
		else {
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
						"Projection edges form product must be fully defined and normal.", "Error",
						JOptionPane.ERROR_MESSAGE);
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
		if (_ourModel.isProductConstraint(_paths)) {
			ModelConstraint<F, GM, M, N, E> newDiagram = (_replace != null)
					? new ProductConstraint<>(_paths, _replace.getX(), _replace.getY(), true, _ourModel)
					: new ProductConstraint<>(_paths, _ourModel);

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
		_ourModel.getFrame()
				.setInstructionText((_paths.size() >= 1)
						? "Select a fully defined normal path with domain '" + _paths.get(0).getDomain().getName()
								+ "', then press 'Next' for another path or 'Finish' to accept."
						: "Select a fully deifned normal path and then press 'Next' to add it to the constraint");
		setNextButton(true);
		setFinishButton(_paths.size() >= 1);
		setCancelButton(true);
		_ourModel.getStateManager().pushState(new GetFullyDefinedPathState<>(true, _paths.size() >= 1, _ourModel,
				((_paths.size() >= 1) ? _paths.get(0).getDomain() : null), null, true));
	}

	/**
	 * Nothing to do when it gets popped off, as it always pops itself off when
	 * completed.
	 */
	@Override
	public void poppedOff() {
	}

	/**
	 * This state is called "New Product Constraint"
	 *
	 * @return The string literal described above.
	 */
	@Override
	public String toString() {
		return "New Product Constraint";
	}
}
