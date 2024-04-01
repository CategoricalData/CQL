package easik.ui;

//~--- JDK imports ------------------------------------------------------------

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;

/**
 * OptionsDialog is a class designed for showing modal dialog boxes to collect
 * information. In particular, the layout of components is all handled
 * automatically. There are two basic display modes: a simple page of options
 * and values, and a tabbed mode (by subclasses implementing the
 * TabbedOptionsDialog interface) where any number of tabs can be used, each
 * providing a list of options just like the tabless interface.
 *
 * The usual implementation is like this:
 *
 * - Create a class that inherits from OptionsDialog, and, optionally,
 * implements the TabbedOptionsDialog and/or ContainedOptions interfaces. - In
 * the constructor(s), take the parent frame/window/dialog that the dialog
 * should attach to (if it is modal, that window/frame/dialog will be
 * inaccessible until the dialog is accepted or dismissed), and any other
 * parameters needed, and call <code>super(frame, title)</code>. - if creating a
 * <b>non-tabbed</b> dialog, create a getOptions() method that returns a list of
 * Option objects. Each Option object has a JLabel and a JComponent to use as
 * the option value. This is usually done via code such as:
 * <code>optionlist.add(new Option(new JLabel("Enter a value:"), new JTextField("initial")))</code>
 * or
 * <code>optionlist.add(new Option(new JLabel("full-width label to display")))</code>
 * or <code>optionlist.add(new Option.Title("Title text"))</code>. - if creating
 * a <b>tabbed</b> dialog, instead implement the getTabs() method, which creates
 * and returns a list of OptionTab objects (which themselves have methods to add
 * options similar to the above). You may also add a getOptions() method: any
 * options returned will be displayed before the tabs. - if you want code to
 * verify the user's input, add a verify() method that returns true if the
 * user's input is good, or provides some feedback and returns false if the
 * dialog shouldn't been closed (i.e. we in effect "cancel" the click of the OK
 * button). - if the class is to be a "contained" object--that is, an object
 * meant to be created with all its own logic and code instead of an object that
 * provides methods for the calling class to call to obtain the
 * results--implement the ContainedOptionsDialog class and add an
 * accepted(boolean) method: that method will be called with the user's choice
 * (true = OK, false = Cancel), and is expected to do any necessary actions for
 * the user's choice. - if you want the options dialog to become visible upon
 * object creation, call the showDialog() method (<b>not</b>
 * <code>setVisible(true)</code>) at the end of the constructor; otherwise have
 * your code call it when you want to show the dialog.
 */

// This GroupLayout is quite similar (but not identical) to that in Java 1.6;
// but since,
// at the time of writing, we don't want to require 1.6, we use this instead.
public abstract class OptionsDialog extends JDialog {
  private static final long serialVersionUID = -87860547361090816L;

  /**
   * Stores whether "OK" was clicked (if false, the dialog is still open, or
   * cancel was clicked, or the dialog was closed without clicking either.
   */
  private boolean _ok = false;

  /**
   * Sets up a new OptionsDialog attached to the specified parent dialog, with the
   * specified title. The dialog will be a modal dialog. The default dialog box
   * size is 300x300; subclasses should call setSize() to set this for themselves.
   *
   * @param parent the parent dialog of the modal dialog
   * @param title  the title of the dialog box
   */
  public OptionsDialog(final JDialog parent, final String title) {
    super(parent, title, true);

    setSize(300, 300);
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
  }

  /**
   * Sets up a new OptionsDialog attached to the specified parent frame, with the
   * specified title. The dialog will be a modal dialog. The default dialog box
   * size is 300x300; subclasses should call setSize() to set this for themselves.
   *
   * @param parent the parent frame of the modal dialog
   * @param title  the title of the dialog box
   */
  public OptionsDialog(final JFrame parent, final String title) {
    super(parent, title, true);

    setSize(300, 300);
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
  }

  /**
   * Returns a list of Option label/component pairs to be displayed in the option
   * dialog. Must be overridden by sub-classes unless they implement
   * TabbedOptionsDialog. For TabbedOptionsDialog objects, if this method returns
   * any options, they will be added to the page *before* the tabs returned by
   * getTabs().
   *
   * @return list of Option objects
   */
  @SuppressWarnings("static-method")
  public List<Option> getOptions() {
    return Collections.emptyList();
  }

  /**
   * Shows the option modal dialog. This method doesn't return until the user
   * accepts, cancels, or otherwise dismisses the dialog box. This method returns
   * true if the user clicks OK (and, if overridden, verify() succeeds).
   *
   * @return true if the user clicked OK and the data was acceptable
   * @see #verify()
   */
  public boolean showDialog() {
    setLocationRelativeTo(getParent());

    if (this instanceof TabbedOptionsDialog) {
      final List<Option> preTabOpts = getOptions();

      if (!preTabOpts.isEmpty()) {
        final JPanel preOpts = optionsPanel(preTabOpts);

        add(new JScrollPane(preOpts), BorderLayout.NORTH);
      }

      final JTabbedPane tabs = new JTabbedPane();
      int selIndex = -1;

      for (final OptionTab tab : ((TabbedOptionsDialog) this).getTabs()) {
        final JPanel options = optionsPanel(tab.getOptions());

        tabs.addTab(tab.getTitle(), tab.getIcon(), new JScrollPane(options), tab.getToolTip());

        if (tab.hasMnemonic()) {
          tabs.setMnemonicAt(tabs.getTabCount() - 1, tab.getMnemonic());
        }

        if ((selIndex == -1) && tab.isInitial()) {
          selIndex = tabs.getTabCount() - 1;
        }
      }

      if (selIndex != -1) {
        tabs.setSelectedIndex(selIndex);
      }

      add(tabs, BorderLayout.CENTER);
    } else {
      final JPanel options = optionsPanel(getOptions());

      add(new JScrollPane(options), BorderLayout.CENTER);
    }

    final JButton ok = new JButton("OK");
    final JButton cancel = new JButton("Cancel");

    ok.setActionCommand("ok");
    cancel.setActionCommand("cancel");

    final ButtonListener bl = new ButtonListener();

    ok.addActionListener(bl);
    cancel.addActionListener(bl);

    final JPanel buttons = new JPanel();

    buttons.add(ok);
    buttons.add(cancel);
    add(buttons, BorderLayout.SOUTH);
    getRootPane().setDefaultButton(ok);

    _ok = false;

    setVisible(true); // blocks until user hits OK (and verify succeeds) or
              // Cancel

    if (this instanceof ContainedOptionsDialog) {
      ((ContainedOptionsDialog) this).accepted(_ok);
    }

    return _ok;
  }

  /**
   * Creates a JPanel with the specified options. Interal use only.
   * 
   * @param opts the options
   * @return a jpanel
   */
  private static JPanel optionsPanel(final List<Option> opts) {
    final JPanel options = new JPanel();
    final GroupLayout gl = new GroupLayout(options);

    options.setLayout(gl);
    gl.setAutoCreateGaps(true);
    gl.setAutoCreateContainerGaps(true);

    final GroupLayout.ParallelGroup labels = gl.createParallelGroup();
    final GroupLayout.ParallelGroup values = gl.createParallelGroup();
    final GroupLayout.ParallelGroup titles = gl.createParallelGroup();
    final GroupLayout.ParallelGroup horiz = gl.createParallelGroup();
    final GroupLayout.SequentialGroup cols = gl.createSequentialGroup();
    final GroupLayout.SequentialGroup rows = gl.createSequentialGroup();

    cols.addGroup(labels);
    cols.addGroup(values);
    horiz.addGroup(cols);
    horiz.addGroup(titles);

    for (final Option o : opts) {
      final JLabel l = o.getLabel();
      final JComponent c = o.getComponent();

      if (c == null) {
        // This is a label-only row, allowed to take up the whole row
        titles.addComponent(l);
        rows.addComponent(l);
      } else {
        if (l.getBorder() == null) {
          l.setBorder(new EmptyBorder(3, 0, 0, 0));
        }

        if (l.getLabelFor() == null) {
          l.setLabelFor(c);
        }

        labels.addComponent(l);
        values.addComponent(c);

        final GroupLayout.ParallelGroup row = gl.createParallelGroup(GroupLayout.Alignment.BASELINE);

        row.addComponent(l);
        row.addComponent(c);
        rows.addGroup(row);
      }
    }

    gl.setHorizontalGroup(horiz);
    gl.setVerticalGroup(rows);

    return options;
  }

  /**
   * Called when the user clicks the OK button. This method should be overridden
   * by subclasses that wish to perform data checks, prompting the user if data is
   * invalid, etc. The default behaviour is to do nothing (i.e. allow anything).
   *
   * @return true if the window should be allowed to be closed, false if the close
   *         should be aborted
   */
  @SuppressWarnings("static-method")
  public boolean verify() {
    return true;
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
    public void actionPerformed(final ActionEvent e) {
      if ("ok".equals(e.getActionCommand())) {
        if (OptionsDialog.this.verify()) {
          _ok = true;

          OptionsDialog.this.dispose();
        }
      } else if ("cancel".equals(e.getActionCommand())) {
        OptionsDialog.this.dispose();
      }
    }
  }
}
