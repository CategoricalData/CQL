/**
 * 
 */
package easik.model.states;

import easik.graph.EasikGraphModel;
import easik.model.Model;
import easik.model.edge.ModelEdge;
import easik.model.ui.ModelFrame;
import easik.model.vertex.ModelVertex;
import easik.sketch.Sketch;
import easik.view.View;

/**
 * This will replace ModelState and will also be used in Views (no existing
 * States for Views at time of creation).
 * 
 * This is the base state. It contains stubs and abstract methods for allowing a
 * class to be treated like a state, and to take part in the State Manager
 * 
 * @author Federico Mora
 * 
 *         based on work by Rob Fletcher 2005 Vera Ranieri 2006 Kevin Green 2006
 *         2006-08-02 Kevin Green
 *
 */
public abstract class ModelState<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>> {
  /**
   * Records state of the next button
   */
  private boolean _nextOn = false;

  /**
   * Records the state of the finish button
   */
  private boolean _finishOn = false;

  /**
   * Records the state of the cancel button
   */
  private boolean _cancelOn = false;

  /**
   * The current model
   */
  protected M _ourModel;

  /**
   *
   *
   * @param inSketch
   */
  public ModelState(M inModel) {
    _ourModel = inModel;
  }

  /**
   * Hook to record when selection has been updated
   */
  public void selectionUpdated() {
  }

  ;

  /**
   * Hook to record when state has been pushed on
   *
   */
  public abstract void pushedOn();

  /**
   * Hook to record when state has been popped off
   *
   */
  public abstract void poppedOff();

  /**
   * @return The string representation of this state
   */
  @Override
  public abstract String toString();

  /**
   * Returns selectable cells. The default is to return all graph cells.
   *
   * @return
   */
  public Object[] getSelectables() {
    return _ourModel.getRoots();
  }

  /**
   * When this state gets the top spot again, this gets executed. Restores buttons
   * and labels. If overridden, be sure to at call super.gotfocus() first.
   */
  public void gotFocus() {
    // views don't add constraints
    if (_ourModel instanceof Sketch) {
      setCancelButton(_cancelOn);
      setNextButton(_nextOn);
      setFinishButton(_finishOn);
    }
  }

  /**
   * Optional hook for when the cancel button is clicked
   *
   */
  public void cancelClicked() {
  }

  /**
   * Optional hook for when the next button is clicked
   *
   */
  public void nextClicked() {
  }

  /**
   * Optional hook for when the finish button is clicked.
   */
  public void finishClicked() {
  }

  /**
   * Use this function to toggle the usability of the cancel button. It remembers
   * the state in a separate boolean so it can be restored.
   *
   * @param inButton Status of the button
   */
  protected void setCancelButton(boolean inButton) {
    if (_ourModel instanceof View) {
      // views don't have cancel buttons or add constraint and all that
      return;
    }
    _cancelOn = inButton;

    _ourModel.getFrame().getCancelButton().setEnabled(inButton);

    if (_cancelOn || _nextOn || _finishOn) {
      _ourModel.getFrame().setButtonPaneVisibility(true);
    } else {
      _ourModel.getFrame().setButtonPaneVisibility(false);
    }
  }

  /**
   * Use this function to toggle the usability of the ok button. It remembers the
   * state in a separate boolean so it can be restored.
   *
   * @param inButton Status of the button
   */
  protected void setNextButton(boolean inButton) {
    if (_ourModel instanceof View) {
      // views don't have cancel buttons or add constraint and all that
      return;
    }
    _nextOn = inButton;

    _ourModel.getFrame().getNextButton().setEnabled(inButton);

    if (_cancelOn || _nextOn || _finishOn) {
      _ourModel.getFrame().setButtonPaneVisibility(true);
    } else {
      _ourModel.getFrame().setButtonPaneVisibility(false);
    }
  }

  /**
   * Use this function to toggle the usability of the finish button. It remembers
   * the state in a separate boolean so it can be restored.
   *
   * @param inButton Status of the button
   */
  protected void setFinishButton(boolean inButton) {
    if (_ourModel instanceof View) {
      // views don't have cancel buttons or add constraint and all that
      return;
    }
    _finishOn = inButton;

    _ourModel.getFrame().getFinishButton().setEnabled(inButton);

    if (_cancelOn || _nextOn || _finishOn) {
      _ourModel.getFrame().setButtonPaneVisibility(true);
    } else {
      _ourModel.getFrame().setButtonPaneVisibility(false);
    }
  }
}
