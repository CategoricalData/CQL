package easik.model.states;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.util.ArrayList;

import javax.swing.JOptionPane;

import easik.graph.EasikGraphModel;
import easik.model.Model;
import easik.model.constraint.ConstraintException;
import easik.model.constraint.EqualizerConstraint;
import easik.model.edge.ModelEdge;
import easik.model.path.ModelPath;
import easik.model.ui.ModelFrame;
import easik.model.vertex.ModelVertex;

/**
 * This state makes an equalizer constraint. When pushed, it will then push
 * another state to get a path. Once the first path is received, it will push
 * another request for a path. This repeated until three paths have been
 * collected and then the constraint is created (as long as the paths are
 * valid).
 */
public class AddEqualizerConstraintState<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>>
    extends ModelState<F, GM, M, N, E> implements PathAcceptingState<F, GM, M, N, E> {
  /** Stores whether the user has finished adding paths to this constraint */
  private boolean _finished;

  /** The paths involved in this constraint */
  private ArrayList<ModelPath<F, GM, M, N, E>> _paths;

  /** The current path number being selected */
  private int _round;

  /**
   * Constructor for creating a new state to collect paths and make a diagram.
   *
   * @param inModel The sketch being worked with.
   */
  public AddEqualizerConstraintState(M inModel) {
    super(inModel);

    _finished = false;
    _paths = new ArrayList<>();
    _round = 0;
  }

  /**
   * Called when a new path has been completed by a state above this on the stack.
   * If it is null, then cancel, otherwise add it to the collection. When we get
   * four paths, then we're finished (if they share a domain and codomain).
   *
   * @param inPath The last path in, null if it was a cancellation
   */
  @Override
  public void passPath(ModelPath<F, GM, M, N, E> inPath) {
    // the button state for the next "get path" (compiler complained when
    // not initialized)
    _finished = _ourModel.getStateManager().getFinished();

    // set on invalid path selection
    String error = null;

    // Check if cancelled
    if (inPath == null) {
      _ourModel.getGraphModel().cancelInsignificantUpdate();
      _ourModel.getStateManager().popState();

      return;
    }

    _round++;

    if (_round == 1) {

      if (inPath.getFirstEdge().isInjective()) {
        _paths.add(inPath);
        _ourModel.getFrame().setInstructionText("Select a fully defined path with source node '"
            + inPath.getCoDomain().getName() + "', and press 'Next'");
        _ourModel.getStateManager().pushState(
            new GetFullyDefinedPathState<>(true, false, _ourModel, inPath.getCoDomain(), null, true));
      } else {
        error = "Invalid equalizer projection: selected path was not injective.";
      }
    } else if (_round == 2) {
      if (!(inPath.getDomain() == _paths.get(0).getCoDomain())) {
        error = "Invalid path selection: Selected path domain did not equal '"
            + _paths.get(0).getCoDomain().getName() + "'.";
      } else {
        _paths.add(inPath);

        N domain = _paths.get(1).getDomain(), codomain = _paths.get(1).getCoDomain();

        _ourModel.getFrame()
            .setInstructionText("Select another fully defined path from '" + domain.getName() + "' to '"
                + codomain.getName()
                + "', then press 'Next' to add more paths, or 'Finish' to create the equalizer.");
        _ourModel.getStateManager()
            .pushState(new GetFullyDefinedPathState<>(true, true, _ourModel, domain, codomain, true));
      }
    } else if (_round >= 3) {
      ArrayList<ModelPath<F, GM, M, N, E>> testConstraint = new ArrayList<>(_paths);

      testConstraint.add(inPath);

      if (!_ourModel.isEqualizerConstraint(testConstraint)) {
        if (inPath.getDomain() != _paths.get(1).getDomain()
            && inPath.getCoDomain() != _paths.get(1).getCoDomain()) {
          error = "Invalid path selection: Path does not go from '" + _paths.get(1).getDomain().getName()
              + "' to '" + _paths.get(1).getCoDomain().getName() + "' or is not unique.";
        } else {
          error = "Paths must be fully defined.";
        }
      } else {
        _paths.add(inPath);

        N domain = _paths.get(1).getDomain(), codomain = _paths.get(1).getCoDomain();

        if (!_finished) {
          _ourModel.getFrame()
              .setInstructionText("Select another fully defined path from '" + domain.getName() + "' to '"
                  + codomain.getName()
                  + "', then press 'Next' to add more paths, or 'Finish' to create the equalizer.");
          _ourModel.getStateManager()
              .pushState(new GetFullyDefinedPathState<>(true, true, _ourModel, domain, codomain, true));
        }
      }
    }

    if (_finished) {
      System.err.println("finished");
      _ourModel.getGraphModel().cancelInsignificantUpdate();
      addDiagram();
      _ourModel.getStateManager().popState();
    }

    // if error, tell user and pop this state.
    if (error != null) {
      JOptionPane.showMessageDialog(_ourModel.getParent(), error + " Equalizer constraint not created.", "Error",
          JOptionPane.ERROR_MESSAGE);
      _ourModel.getGraphModel().cancelInsignificantUpdate();
      _ourModel.getStateManager().popState();
    }

    return;
  }

  /**
   * Add the diagram to the sketch
   *
   * @return true if the constraint was successfully added to the sketch, false
   *         otherwise
   */
  @SuppressWarnings("unchecked")
  private boolean addDiagram() {
    if (_ourModel.isEqualizerConstraint(_paths)) {
      try {
        EqualizerConstraint<F, GM, M, N, E> newDiagram = new EqualizerConstraint<>(_paths, _ourModel);

        _ourModel.addNewConstraint(newDiagram);
        _ourModel.setDirty();

        return true;
      } catch (ConstraintException ce) {
        System.err.println("Caught constraint exception");
      }
    }
    System.err.println("Didn't even try");
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
        .setInstructionText("Select an injective edge for the equalizer projection and click 'Next'.");
    _ourModel.getFrame().getNextButton().setEnabled(true);
    _ourModel.getFrame().getCancelButton().setEnabled(true);
    _ourModel.getStateManager().pushState(new GetInjectivePathState<>(true, false, true, _ourModel));
  }

  /**
   * Nothing to do when it gets popped off, as it always pops itself off when
   * completed.
   */
  @Override
  public void poppedOff() {
  }

  /**
   * This state is called "New Equalizer Constraint"
   *
   * @return The string literal described above.
   */
  @Override
  public String toString() {
    return "New Equalizer Constraint";
  }
}
