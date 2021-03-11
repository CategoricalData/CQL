package easik.ui.menu;

//~--- non-JDK imports --------------------------------------------------------

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

import easik.graph.EasikGraphModel;
import easik.model.Model;
import easik.model.edge.ModelEdge;
import easik.model.states.AddPullbackConstraintState;
import easik.model.ui.ModelFrame;
import easik.model.vertex.ModelVertex;
import easik.ui.JUtils;

/**
 * Add a simple pullback constraint menu option and action.
 *
 * @author Rob Fletcher 2005
 * @author Kevin Green 2006
 * @author Vera Ranieri 2006
 * @version 2006-05-25 Vera Ranieri
 * @version Christian Fiddick Summer 2012
 */
public class AddPullbackConstraintAction<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>>
    extends AbstractAction {
  /**  */
  private static final long serialVersionUID = 2034451266127954820L;

  /**  */
  private F _theFrame;

  /**
   * Creates and initializes the menu option.
   *
   * @param _theFrame2
   */
  public AddPullbackConstraintAction(F _theFrame2) {
    super();

    _theFrame = _theFrame2;

    putValue(Action.NAME, "Add a Pullback Constraint");
    putValue(Action.SHORT_DESCRIPTION, "Create a pullback constraint from a set of paths");
  }

  /**
   * Creates the pullback if the selection is appropriate.
   * 
   * @param e The action event
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    /*
     * sketches can now be empty since we can add as we go if
     * (!_theFrame.getMModel().getEntities().isEmpty()) { // get the user defined
     * pullback width first PBWidthDialog pbwd = new PBWidthDialog(_theFrame);
     * 
     * if (!pbwd.isAccepted()) { return; }
     * 
     * int w = pbwd.getInput();
     * 
     * _theFrame.getMModel().getStateManager().pushState(new
     * AddPullbackConstraintState(_theFrame.getMModel(), w)); } else {
     * JOptionPane.showMessageDialog(null, "Sketch cannot be empty.", "Error",
     * JOptionPane.ERROR_MESSAGE); }
     */

    // get the user defined pullback width first
    PBWidthDialog pbwd = new PBWidthDialog(_theFrame);

    if (!pbwd.isAccepted()) {
      return;
    }

    int w = pbwd.getInput();

    _theFrame.getMModel().getStateManager().pushState(new AddPullbackConstraintState<>(_theFrame.getMModel(), w));

  }
}

/** Dialog to get user defined pullback width */
class PBWidthDialog extends JDialog {
  /**
   *    
   */
  private static final long serialVersionUID = 4169036480943896851L;

  /**  */
  private final int DEFAULT_WIDTH = 2;

  /** Flag indicating the state of the OK button */
  private boolean _ok = false;

  /** This dialog's parent frame */
  @SuppressWarnings("unused")
  private JFrame _parent;

  /** Get the width from this */
  private JTextField inBox;

  /**
   * Sets up text area with a default statement fragment for the user to complete.
   *
   * @param parent The parent frame of the modal dialog
   */
  public PBWidthDialog(JFrame parent) {
    super(parent, "Pullback Width", true);

    _parent = parent;
    inBox = JUtils.textField("" + DEFAULT_WIDTH);

    inBox.setPreferredSize(new Dimension(180, 60));
    setResizable(false);
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);

    JButton ok = new JButton("OK"), cancel = new JButton("Cancel");

    ok.setActionCommand("ok");
    cancel.setActionCommand("cancel");

    ButtonListener bl = new ButtonListener();

    ok.addActionListener(bl);
    cancel.addActionListener(bl);

    JPanel buttons = new JPanel();

    buttons.add(ok);
    buttons.add(cancel);
    add(inBox, BorderLayout.CENTER);
    add(buttons, BorderLayout.SOUTH);

    _ok = false;

    // get a good size such that no scrolling is needed
    Dimension size = getPreferredSize();

    setSize(size);
    setVisible(true);
  }

  /**
   * Returns true if the user accepted the options dialog (that is, clicked the OK
   * button).
   *
   * @return true if the user clicked OK (and the fields verified successfully),
   *         false if the user cancelled or closed the dialog.
   */
  public boolean isAccepted() {
    return _ok;
  }

  /**
   * Get the width as an int.
   * 
   * @return Parsed width if possible, DEFAULT_WIDTH otherwise
   */
  public int getInput() {
    try {
      int val = Integer.parseInt(inBox.getText());

      if (val < 2) {
        return DEFAULT_WIDTH;
      }

      return val;
    } catch (NumberFormatException e) {
      return DEFAULT_WIDTH;
    }
  }

  /**
   *
   *
   * @version 12/09/12
   * @author Christian Fiddick
   */
  private class ButtonListener implements ActionListener {
    // Fired when the user clicks OK or Cancel

    /**
     *
     *
     * @param e
     */
    @Override
    public void actionPerformed(ActionEvent e) {
      if (e.getActionCommand().equals("ok")) {
        _ok = true;

        PBWidthDialog.this.dispose();
      } else if (e.getActionCommand().equals("cancel")) {
        PBWidthDialog.this.dispose();
      }
    }
  }
}
