package easik.model;

//~--- non-JDK imports --------------------------------------------------------
//~--- JDK imports ------------------------------------------------------------
import java.util.LinkedList;

import easik.graph.EasikGraphModel;
import easik.model.edge.ModelEdge;
import easik.model.states.BasicEditingState;
import easik.model.states.ModelState;
import easik.model.ui.ModelFrame;
import easik.model.vertex.ModelVertex;

/**
 * Replaced SketchTateManager and will now work with Views as well.
 * 
 * The state manager is a class that keeps track of the current state of the
 * program. Different states are used in order to interpret more complicated
 * inputs. An example of this would be the process needed to create a
 * commutative diagram. First there are several clicks required to select each
 * path. Paths can listen to what's going on with the UI, and can change and
 * collect information that way.
 *
 * The state manager works like a stack. New states are pushed on top and then
 * the state manager forwards any important messages up to the active state.
 *
 * @author Federico Mora
 * 
 */
public class ModelStateManager<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>> {
  /**
   * The stack.
   */
  private LinkedList<ModelState<F, GM, M, N, E>> _states = new LinkedList<>();

  /**
   * Boolean to record whether the user has pressed the <it>Finish</it> button.
   */
  private boolean _finished;

  /**
   * The Sketch this state manager is monitoring
   */
  private M _ourModel;

  /**
   * Initialize the state manager
   *
   * @param inSketch
   */
  public ModelStateManager(M inModel) {
    _ourModel = inModel;
    _finished = false;
  }

  /**
   *
   */
  public void initialize() {
    pushState(new BasicEditingState<>(_ourModel));
  }

  /**
   * Hook for informing the current active state that the selection has been
   * updated
   */
  public void selectionUpdated() {
    peekState().selectionUpdated();
  }

  /**
   * Hook for getting the currently-selectable graph elements.
   *
   * @return
   */
  public Object[] getSelectables() {
    return peekState().getSelectables();
  }

  /**
   * Non destructively take a look at the top item on the stack.
   * 
   * @return The top state
   */
  public ModelState<F, GM, M, N, E> peekState() {
    return _states.getFirst();
  }

  /**
   * Pop the state off of the top of the stack. After the state has been popped
   * the 'popped' hook is called.
   * 
   * @return The popped state
   */
  public ModelState<F, GM, M, N, E> popState() {
    ModelState<F, GM, M, N, E> toReturn = _states.removeFirst();

    // Inform the state it has been popped
    toReturn.poppedOff();

    // Inform the new top state that it is in the spotlight
    peekState().gotFocus();

    return toReturn;
  }

  /**
   * Pushes a state onto the stack, calls the 'pushed on' hook after being added.
   * 
   * @param toPush The new state being pushed on
   */
  public void pushState(ModelState<F, GM, M, N, E> toPush) {
    _states.addFirst(toPush);
    toPush.gotFocus();
    toPush.pushedOn();
  }

  /**
   * Informs the top element that the next button has been clicked
   *
   */
  public void nextClicked() {
    peekState().nextClicked();
  }

  /**
   * Informs the top element that the cancel button has been clicked.
   *
   */
  public void cancelClicked() {
    peekState().cancelClicked();
  }

  /**
   * Informs the top element that the finish button has been clicked.
   */
  public void finishClicked() {
    _finished = true;

    peekState().finishClicked();
  }

  /**
   * Gets the value currently stored in <it>finished</it>
   * 
   * @return True if the user has selected finished, false otherwise.
   * 
   */
  public boolean getFinished() {
    return _finished;
  }

  /**
   * Resets the value of <it>finished</it> to false
   * 
   */
  public void resetFinished() {
    _finished = false;
  }
}
