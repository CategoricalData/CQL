
/**
 *
 */
package easik.ui.menu;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import easik.Easik;

/**
 * Menu action for showing about dialog box
 *
 * @author Vera Ranieri 2006
 * @author Kevin Green 2006
 * @since 2006-07-19 Vera Ranieri
 * @version 2006-08-04 Kevin Green
 */
public class AboutAction extends AbstractAction {
  /**
   *    
   */
  private static final long serialVersionUID = 2628665718783464938L;

  /**
   * Create the new action and set the name and description
   */
  public AboutAction() {
    super("About Easik");

    putValue(Action.SHORT_DESCRIPTION, "About Easik");
  }

  /**
   * When action is performed, show the about dialog box.
   *
   * @param e The action event
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    showAboutDialog();
  }

  /**
   *
   */
  public static void showAboutDialog() {
    ImageIcon myIcon = new ImageIcon(Easik.getInstance().getFrame().getIconImage());

    myIcon.setDescription("Easik");
    JOptionPane.showMessageDialog(null,
        "EASIK - Entity Attribute Sketch Implementation Kit\n" + "Version " + Easik.VERSION + " (build r"
            + Easik.REVISION + ")\n" + "Developed 2005-2008 at Mount Allison University\n\n"
            + "R. Rosebrugh\n" + "Rob Fletcher (2005)\n" + "Vera Ranieri (2006)\n" + "Kevin Green (2006)\n"
            + "Jason Rhinelander (2008)\n" + "Andrew Wood (2008)\n",
        "Easik", JOptionPane.INFORMATION_MESSAGE, myIcon);
  }
}
