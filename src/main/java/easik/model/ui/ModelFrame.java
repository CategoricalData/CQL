/**
 * 
 */
package easik.model.ui;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import easik.graph.EasikGraphModel;
import easik.model.Model;
import easik.model.edge.ModelEdge;
import easik.model.vertex.ModelVertex;
import easik.overview.vertex.OverviewVertex;
import easik.ui.EasikFrame;
import easik.ui.tree.ModelInfoTreeUI;

/**
 * This class will be extended by ViewFrame and SketchFrame. It will provide
 * methods used to allow generics. For example
 * 
 * @author Federico Mora
 *
 */
public abstract class ModelFrame<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>>
    extends EasikFrame {

  /**
   * 
   */
  private static final long serialVersionUID = -6126270587925997545L;

  /** The next/finish/cancel buttons */
  private JButton _nextButton, _finishButton, _cancelButton;

  /** The right, button, and main panels of the frame */
  private JPanel _buttonPane;

  /**  */
  private JLabel _instructionBar;

  /** The SketchNode representing this frame */
  private OverviewVertex _myNode;

  /** The information tree */
  private ModelInfoTreeUI<F, GM, M, N, E> _infoTreeUI;

  /** The frames current mode - sketch editing or data manipulation */
  private Mode _mode;

  /** The current sketch */
  private M _ourModel;

  /**
   * Allows the frame to know which mode it is in. The current modes allow editing
   * a sketch, or manipulating data on a server if a connection exists.
   */
  public enum Mode {
    EDIT, MANIPULATE
  }

  public ModelFrame(String title) {
    super(title);
  }

  /**
   * Sets if the button pane is visible or not
   *
   * @param isVis True if visible, false if hidden.
   */
  public void setButtonPaneVisibility(final boolean isVis) {
    _buttonPane.setVisible(isVis);
  }

  /**
   * Returns the next button
   *
   * @return The next button
   */
  public JButton getNextButton() {
    return _nextButton;
  }

  /**
   * Returns the finish button
   *
   * @return The finish button
   */
  public JButton getFinishButton() {
    return _finishButton;
  }

  /**
   * Returns the cancel button.
   *
   * @return The cancel button
   */
  public JButton getCancelButton() {
    return _cancelButton;
  }

  /**
   * Sets the enableness of the "add ____ constraint" menu items
   * 
   * @param state The state of the add constraint menu items.
   */
  public abstract void enableAddConstraintItems(final boolean state);

  /**
   * Returns the value of the show attributes check box menu item
   *
   * @return The value of the show attributes check box menu item
   */
  public abstract boolean getShowAttsVal();

  /**
   * Assigns then SketchNode representing this frame in the overview
   *
   * @param inNode
   */
  public void assignNode(final OverviewVertex inNode) {
    _myNode = inNode;
  }

  /**
   * Returns the SketchNode representing this frame in the overview
   *
   * @return
   */
  public OverviewVertex getNode() {
    return _myNode;
  }

  /**
   * The instruction bar tells the user what to do when the button pane in
   * enables, as it is not always obvious... This method changes that text.
   *
   * @param inText The new text
   */
  public void setInstructionText(final String inText) {
    _instructionBar.setText(inText);
  }

  /**
   * Returns the InfoTreeUI object
   *
   * @return The InfoTreeUI object
   */
  public ModelInfoTreeUI<F, GM, M, N, E> getInfoTreeUI() {
    return _infoTreeUI;
  }

  /**
   * Returns the frames current edit mode. The edit mode determines what popup
   * options and menu items are available.
   * 
   * @return The frames current edit mode.
   */
  public Mode getMode() {
    return _mode;
  }

  /**
   * Returns the current sketch
   *
   * @return The sketch
   */
  public M getMModel() {
    return _ourModel;
  }
}
