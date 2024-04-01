package catdata.ide;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map.Entry;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * @param <T> the type of the result the wizard will generate
 */
class Wizard<T> implements ActionListener, ChangeListener {
  private final WizardModel<T> model;
  private final JDialog dialog = new JDialog();
  private final JPanel contentPanel = new JPanel();
  private final CardLayout layout;
  private final JButton nextButton;
  private final JButton backButton;
  private final Consumer<T> completionCallback;
  private boolean started = false;

  public Wizard(WizardModel<T> model, Consumer<T> completionCallback) {
    dialog.setBounds(100, 100, 1000, 600);
    dialog.getContentPane().setLayout(new BorderLayout());
    layout = new CardLayout();
    contentPanel.setLayout(layout);
    contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
    this.model = model;
    model.registerModelListener(this);
    dialog.getContentPane().add(contentPanel, BorderLayout.CENTER);
    JPanel buttonPane = new JPanel();
    buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
    dialog.getContentPane().add(buttonPane, BorderLayout.SOUTH);

    JButton cancelButton = new JButton("Cancel");
    cancelButton.setActionCommand("Cancel");
    buttonPane.add(cancelButton);
    cancelButton.addActionListener(this);

    backButton = new JButton("Back");
    backButton.setActionCommand("Back");
    backButton.setEnabled(false);
    buttonPane.add(backButton);
    dialog.getRootPane().setDefaultButton(backButton);
    backButton.addActionListener(this);

    nextButton = new JButton("Next");
    nextButton.setActionCommand("Next");
    buttonPane.add(nextButton);
    dialog.getRootPane().setDefaultButton(nextButton);
    nextButton.addActionListener(this);

    // dialog.setModal(true);
    dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

    this.completionCallback = completionCallback;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    switch (e.getActionCommand()) {
    case "Next":
      model.forward();
      break;
    case "Cancel":
      dialog.dispose();
      break;
    case "Back":
      model.back();
      break;
    case "Finish":
      complete();
      break;
    default:
      throw new RuntimeException();
    }
  }

  /**
   * Starts the wizard and displays it.
   * 
   * A wizard may only be started once.
   */
  public void startWizard() {
    if (started) {
      throw new IllegalStateException("Tried to start a wizard that had already started.");
    }
    for (Entry<String, ? extends JComponent> entry : model.getAllPages().entrySet()) {
      layout.addLayoutComponent(entry.getValue(), entry.getKey());
      contentPanel.add(entry.getValue());
    }
    layout.show(contentPanel, model.getState());
    dialog.setVisible(true);
    started = true;
  }

  /**
   * Returns whether or not this wizard can be completed.
   * 
   * @return true when completable
   */
  private boolean isComplete() {
    return model.isComplete();
  }

  /**
   * Finish the wizard. The callback method is applied and the JDialog is
   * disposed.
   */
  private void complete() {
    if (!isComplete()) {
      throw new IllegalStateException("Attempted to complete an unfinished model.");
    }
    completionCallback.accept(model.complete());
    dialog.dispose();
  }

  @SuppressWarnings("rawtypes")
  @Override
  public void stateChanged(ChangeEvent e) {
    if (e instanceof WizardModelChangeEvent) {
      nextButton.setEnabled(model.canGoForward());
      backButton.setEnabled(model.canGoBack());

      if (isComplete()) {
        nextButton.setText("Finish");
      } else {
        nextButton.setText("Next");
      }
      nextButton.setActionCommand(nextButton.getText());
      layout.show(contentPanel, ((WizardModelChangeEvent) e).newState);
    }

  }

  @SuppressWarnings("serial")
  public static class WizardModelChangeEvent<T> extends ChangeEvent {
    public final String newState;
    public final String oldState;

    public WizardModelChangeEvent(WizardModel<T> source, String newState, String oldState) {
      super(source);
      this.newState = newState;
      this.oldState = oldState;
    }
  }

}