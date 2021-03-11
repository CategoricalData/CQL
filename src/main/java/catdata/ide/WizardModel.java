package catdata.ide;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.event.ChangeListener;

import catdata.ide.Wizard.WizardModelChangeEvent;

/**
 * A data model for a Wizard. The model is responsible for maintaining both the
 * data and the state of the wizard as it works and the pages it uses.
 * 
 * A properly implemented model must give each of it's possible states a String
 * identifier and a WizardPage to be displayed while it is in that state.
 *
 * @param <T> the type of the result value
 */
public abstract class WizardModel<T> {

  private final List<ChangeListener> listeners = new LinkedList<>();

  public abstract boolean isComplete();

  public abstract T complete() throws IllegalStateException;

  protected abstract Map<String, ? extends JComponent> getAllPages();

  protected abstract void forward0();

  public String forward() {
    String oldstate = getState();

    forward0();

    String newstate = getState();

    for (ChangeListener l : listeners) {
      l.stateChanged(new WizardModelChangeEvent<>(this, newstate, oldstate));
    }

    return newstate;
  }

  protected abstract boolean canGoForward();

  protected abstract void back0();

  public String back() {
    String oldstate = getState();

    back0();

    String newstate = getState();

    for (ChangeListener l : listeners) {
      l.stateChanged(new WizardModelChangeEvent<>(this, newstate, oldstate));
    }

    return newstate;
  }

  protected abstract boolean canGoBack();

  protected abstract String getState();

  public void registerModelListener(ChangeListener l) {
    listeners.add(l);
  }

  public void unregisterModelListener(ChangeListener l) {
    listeners.remove(l);
  }
}
