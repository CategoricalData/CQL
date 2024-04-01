package easik.ui;

//~--- JDK imports ------------------------------------------------------------

import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * Utility class for various graphical operations used in EASIK UI menus.
 */
public class JUtils {
  /**
   * The maximum number of characters permitted on a line in
   * {@link tooltip(String)}.
   */
  public static final int TIP_WIDTH = 80;

  /**
   * Sets the maximum height of a JComponent to its preferred height, thereby
   * preventing it from being expanded. The component itself is returned, to allow
   * shortcuts such as: xyz.add(JUtils.fixHeight(component)).
   *
   * @param c the component to be height-fixed
   * @return the component
   */
  public static JComponent fixHeight(JComponent c) {
    Dimension max = c.getMaximumSize();

    max.height = c.getPreferredSize().height;

    c.setMaximumSize(max);

    return c;
  }

  /**
   * Sets the maximum width of a JComponent to its preferred width, thereby
   * preventing it from being expanded. The component itself is returned, to allow
   * shortcuts such as: xyz.add(JUtils.fixWidth(component)).
   *
   * @param c the component to be width-fixed
   * @return the component
   */
  public static JComponent fixWidth(JComponent c) {
    Dimension max = c.getMaximumSize();

    max.width = c.getPreferredSize().width;

    c.setMaximumSize(max);

    return c;
  }

  /**
   * Creates a new, fixed-height JTextField with the given initial value.
   *
   * @param initial the initial JTextField value
   * @return JTextField with a fixed height
   * @see fixHeight( javax.swing.JComponent)
   * @see javax.swing.JTextField(String)
   */
  public static JTextField textField(String initial) {
    return (JTextField) fixHeight(new JTextField(initial));
  }

  /**
   * Creates a new, fixed-height JTextField with the given initial value and
   * columns field.
   *
   * @param initial the initial JTextField value
   * @param cols    the number of cols
   * @return JTextField with a fixed height
   * @see fixHeight( javax.swing.JComponent)
   * @see javax.swing.JTextField(String, int)
   */
  public static JTextField textField(String initial, int cols) {
    return (JTextField) fixHeight(new JTextField(initial, cols));
  }

  /**
   * Creates a new, fixed-height JTextField with the given initial columns field.
   *
   * @param cols the number of cols
   * @return JTextField with a fixed height
   * @see fixHeight( javax.swing.JComponent)
   * @see javax.swing.JTextField(int)
   */
  public static JTextField textField(int cols) {
    return (JTextField) fixHeight(new JTextField(cols));
  }

  /**
   * Creates a new JTextArea, sets various useful properties for using it, and
   * sticks it in a JScrollPane (so that it will scroll), which is then returned.
   * The JTextArea is accessible via
   * <code>scrollpane.getViewport().getMModel()</code>, or you can use
   * JUtils.taText(JScrollPane) to get the text.
   *
   * @param initial the initial text
   * @param row
   * @param cols    the number of cols (@see javax.swing.JTextArea)
   * @return a JScrollPane containing the created JTextArea
   */
  public static JScrollPane textArea(String initial, int row, int cols) {
    JTextArea ta = new JTextArea(initial, row, cols);

    ta.setLineWrap(true);
    ta.setWrapStyleWord(true);
    ta.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

    return new JScrollPane(ta);
  }

  /**
   * Creates a new JTextArea with an empty initial string, and default
   * rows/columns
   *
   * @return
   */
  public static JScrollPane textArea() {
    return textArea("");
  }

  /**
   * Creates a new JTextArea with the specified initial string, and default
   * rows/columns
   *
   * @param initial
   *
   * @return
   */
  public static JScrollPane textArea(String initial) {
    return textArea(initial, 0, 0);
  }

  /**
   * Returns the text value of a scrollable JTextArea created by textArea().
   *
   * @param jsp a JScrollPane created by one of the JUtils.textArea() methods
   * @return the string of the JTextArea
   */
  public static String taText(JScrollPane jsp) {
    return ((JTextArea) jsp.getViewport().getView()).getText();
  }

  /**
   * Takes a JComponent and makes it borderless, then returns the JComponent.
   *
   * @param c
   *
   * @return
   */
  public static JComponent borderless(JComponent c) {
    c.setBorder(BorderFactory.createEmptyBorder());

    return c;
  }

  /**
   * Takes a string meant for a tooltip and turns it into an HTML tooltip, adding
   * \n's, and then replacing all \n's (added and pre-existing) to
   * <code>&lt;br&gt;</code> tags to break up long lines.
   *
   * @param tip
   *
   * @return
   */
  public static String tooltip(String tip) {
    if (tip.length() <= TIP_WIDTH) {
      return tip.replaceFirst("^(?!(?i:<html>))", "<html>"); // Add <html>
                                  // if it
                                  // isn't
                                  // already
                                  // there
    }

    StringBuilder full = new StringBuilder(), line = new StringBuilder();

    for (String word : tip.split("[ \t]+")) {
      if (full.length() == 0) {
        // The very first piece
        full.append("<html>");
      }

      if (line.length() == 0) {
        // The first piece on this line
      }

      // Handle a \n or <br> in the string by throwing everything in the
      // word up to
      // the newline into full, then just using the last bit for word
      String p[] = word.split("\n|<[bB][rR]>");

      if (p.length > 1) {
        if (line.length() > 0) {
          full.append(line); // Dump line
          full.append(" ");

          line = new StringBuilder(); // and clear it
        }

        // Dump all the pieces except the last
        for (int i = 0; i <= p.length - 2; i++) {
          full.append(p[i]);
          full.append("\n");
        }

        // Now just treat the last piece as the word:
        word = p[p.length - 1];
      }

      int gLength = line.length();

      if (gLength > 0) {
        if (gLength + word.length() + 1 > TIP_WIDTH) { // + 1 for the
                                // space to be
                                // added
          full.append(line);
          full.append("\n");

          line = new StringBuilder();
        } else {
          // Not the first piece, so add a space.
          line.append(" ");
        }
      }

      line.append(word);
    }

    full.append(line);

    return full.toString().replace("\n", "<br>").replaceAll("(?:<br>)+$", "");
  }

  /**
   * Shows a confirmation dialog, warning the user that their changes will be lost
   * if they continue. Returns true if the user confirms that losing their changes
   * is okay, false if they cancel.
   *
   * @param frame
   *
   * @return
   */
  public static boolean confirmLoss(JFrame frame) {
    return JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(frame,
        "Warning: unsaved changes will be lost; continue anyway?", "Caution!", JOptionPane.OK_CANCEL_OPTION,
        JOptionPane.WARNING_MESSAGE);
  }
}
