package easik.ui;

//~--- non-JDK imports --------------------------------------------------------

import java.awt.Color;
//~--- JDK imports ------------------------------------------------------------
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;

import easik.Easik;
import easik.EasikSettings;
import easik.overview.Overview;

/**
 * Simple class that inherits from JFrame; Easik's ApplicationFrame,
 * SketchFrame, and DatatypesController all inherit from this. In the future,
 * this might be used to provide some common methods between the two.
 */
public abstract class EasikFrame extends JFrame {
  private static final long serialVersionUID = 5529843989982833701L;

  /** An instance of the current Easik settings */
  protected EasikSettings _settings;

  /** The tree name */
  protected JLabel _treeName;

  /**
   *
   *
   * @param title
   */
  public EasikFrame(String title) {
    super(title);

    getContentPane().setBackground(Color.white);

    _settings = Easik.getInstance().getSettings();

    this.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent evt) {
        closeWindow();
      }
    });
    this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
  }

  /**
   *
   */
  public abstract void closeWindow();

  /**
   * Sets the String to be displayed above the information tree.
   *
   * @param name The name of the tree
   */
  public void setTreeName(String name) {
    _treeName.setText(name);
  }

  /**
   * Returns the Overview, regardless of what type of EasikFrame instance this is.
   *
   * @return
   */
  public abstract Overview getOverview();

  /**
   * Adds the specified JMenuItem to the menu, optionally setting up a key
   * accelerator.
   *
   * @param menu      the menu to which the item should be added
   * @param item      the item to be added
   * @param keyCode   the key code (e.g. KeyEvent.VK_J) to bind to the menu item.
   *                  null to not bind a accelerator.
   * @param extraMask an extra key mask to apply, in addition to the default mask.
   *                  Optional.
   */
  public static void addMenuItem(JMenu menu, JMenuItem item, Integer keyCode, int extraMask) {
    menu.add(item);

    if (keyCode != null) {
      item.setAccelerator(KeyStroke.getKeyStroke(keyCode.intValue(),
          Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | extraMask));
    }
  }

  /**
   *
   *
   * @param menu
   * @param item
   * @param keyCode
   */
  public static void addMenuItem(JMenu menu, JMenuItem item, Integer keyCode) {
    addMenuItem(menu, item, keyCode, 0);
  }

  /**
   * Adds the specified AbstractAction as a JMenuItem to the menu, optionally
   * setting up a key accelerator.
   *
   * @param menu      the menu to which the item should be added
   * @param action    the AbstractAction of the item to be added
   * @param keyCode   the key code (e.g. KeyEvent.VK_J) to bind to the menu item.
   *                  null to not bind a accelerator.
   * @param extraMask an extra key mask to apply, in addition to the default mask.
   *                  Optional.
   */
  public static void addMenuItem(JMenu menu, AbstractAction action, Integer keyCode, int extraMask) {
    addMenuItem(menu, new JMenuItem(action), keyCode, extraMask);
  }

  /**
   *
   *
   * @param menu
   * @param action
   * @param keyCode
   */
  public static void addMenuItem(JMenu menu, AbstractAction action, Integer keyCode) {
    addMenuItem(menu, action, keyCode, 0);
  }
}
