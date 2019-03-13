package easik.model.states;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.util.LinkedList;

import javax.swing.JOptionPane;

import easik.graph.EasikGraphModel;
import easik.model.Model;
import easik.model.constraint.CommutativeDiagram;
import easik.model.constraint.ModelConstraint;
import easik.model.edge.ModelEdge;
import easik.model.path.ModelPath;
import easik.model.ui.ModelFrame;
import easik.model.vertex.ModelVertex;

/**
 * This state makes a commutative diagram. When pushed, it will then push
 * another state to get a path. Once the first path is received, it will push
 * another request for a path. After adding the first path the options to either
 * add another path or finish the constraint are available.
 */
public class AddCommutativeDiagramState<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>> extends ModelState<F, GM, M, N, E> implements PathAcceptingState<F, GM, M, N, E> {
	/**
	 * Boolean to ascertain whether the selection of paths for this constraint
	 * has been finished
	 */
	private boolean _finished;

	/**
	 * The paths involved in this constraint
	 */
	private LinkedList<ModelPath<F, GM, M, N, E>> _paths;

	/**
	 * If set, we delete this CD immediately before adding the new one. Used
	 * when adding paths to an existing CD.
	 */
	private CommutativeDiagram<F, GM, M, N, E> _replace;

	/**
	 * Constructor for creating a new state to collect paths and make a diagram.
	 *
	 * @param inModel
	 *            The sketch being worked with.
	 */
	public AddCommutativeDiagramState(M inModel) {
		super(inModel);

		_finished = false;
		_paths = new LinkedList<>();
	}

	/**
	 * Constructor for modifying an existing commutative diagram.
	 *
	 * @param inModel
	 *            The sketch being worked with
	 * @param existing
	 *            The existing constraint to have paths added to
	 */
	public AddCommutativeDiagramState(M inModel, CommutativeDiagram<F, GM, M, N, E> existing) {
		super(inModel);

		_finished = false;
		_paths = new LinkedList<>();

		for (ModelPath<F, GM, M, N, E> path : existing.getPaths()) {
			_paths.add(new ModelPath<>(path.getEdges()));
		}

		_replace = existing;
	}

	/**
	 * Called when a new path has been completed by a state above this on the
	 * stack. If it is null, then cancel, otherwise add it to the collection.
	 * When two paths are in the collection, finish (if they share a domain and
	 * codomain).
	 *
	 * @param inPath
	 *            The last path in, null if it was a cancellation
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

			if ((_paths.size() > 1) && !_ourModel.isCommutativeDiagram(_paths)) {
				JOptionPane.showMessageDialog(_ourModel.getParent(), "Invalid or duplicated path selected.\nCommutative Diagram not created.", "Error", JOptionPane.ERROR_MESSAGE);
				_ourModel.getGraphModel().cancelInsignificantUpdate();
				_ourModel.getStateManager().popState();

				return;
			}

			N domain = _paths.getFirst().getDomain(), codomain = _paths.getFirst().getCoDomain();

			_ourModel.getFrame().setInstructionText("Select a path from '" + domain.getName() + "' to '" + codomain.getName() + "' and click 'Next' to continue, or 'Finish' to add constraint.");
			_ourModel.getStateManager().pushState(new GetPathState<>(true, true, _ourModel, domain, codomain));
		} else {
			_paths.add(inPath);
			_ourModel.getGraphModel().cancelInsignificantUpdate();
			_ourModel.getGraphModel().beginUpdate();

			if (addDiagram()) {
				// If we're replacing a CD, remove the old one:
				if (_replace != null) {
					_ourModel.removeConstraint(_replace);
				}
			} else {
				JOptionPane.showMessageDialog(_ourModel.getParent(), "Invalid path selection.\nCommutative Diagram not created.", "Error", JOptionPane.ERROR_MESSAGE);
			}

			_ourModel.getGraphModel().endUpdate();
			_ourModel.getStateManager().popState();
		}
	}

	/**
	 * Add the diagram to the sketch
	 *
	 * @return true if the diagram was successfully added to the sketch, false
	 *         otherwise
	 */
	@SuppressWarnings("unchecked")
	private boolean addDiagram() {
		if (_ourModel.isCommutativeDiagram(_paths)) {
			ModelConstraint<F, GM, M, N, E> newDiagram = (_replace != null) ? new CommutativeDiagram<>(_paths, _replace.getX(), _replace.getY(), true, _ourModel) : new CommutativeDiagram<>(_paths, _ourModel);

			_ourModel.addNewConstraint(newDiagram);
			_ourModel.setDirty();

			return true;
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
		_ourModel.getFrame().setInstructionText((_paths.size() >= 1) ? "Select a path from '" + _paths.getFirst().getDomain().getName() + "' to '" + _paths.getFirst().getCoDomain().getName() + "' and click 'Next' to continue, or 'Finish' to add constraint." : "Select the first path and press 'Next'.");
		setNextButton(true);
		setFinishButton(_paths.size() >= 1);
		setCancelButton(true);

		if (_paths.size() >= 1) {
			// We're adding a path to the CD, so we have a required source and
			// target
			N domain = _paths.getFirst().getDomain(), codomain = _paths.getFirst().getCoDomain();

			_ourModel.getStateManager().pushState(new GetPathState<>(true, true, _ourModel, domain, codomain));
		} else {
			_ourModel.getStateManager().pushState(new GetPathState<>(true, false, _ourModel));
		}
	}

	/**
	 * Nothing to do when it gets popped off, as it always pops itself off when
	 * completed.
	 */
	@Override
	public void poppedOff() {
	}

	/**
	 * This state is called "New Commutative diagram"
	 *
	 * @return The string literal described above.
	 */
	@Override
	public String toString() {
		return "New Commutative Diagram";
	}
}
