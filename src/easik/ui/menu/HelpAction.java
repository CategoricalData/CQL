package easik.ui.menu;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import javax.swing.AbstractAction;
import javax.swing.Action;

/**
 * Menu action for launching help
 *
 * @author Rob Fletcher 2005
 * @author Vera Ranieri 2006
 * @version 2006-07-19 Vera Ranieri
 */
public class HelpAction extends AbstractAction {
  /**
   *
   */
  private static final long serialVersionUID = 1824249541417462390L;

  /**
   * Constructor
   */
  public HelpAction() {
    super("Easik Help");

    putValue(Action.SHORT_DESCRIPTION, "Review the documentation");
  }

  /**
   * Registers when the user selects to view the Help
   *
   * @param e The action event
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    // TODO finish updating the helps... Load plaintext if no browser
    // CF2012 load help through browser now
    // Currently loading a local HTML help document
    // URL help = Easik.class.getResource("help");
    File f;

    try {
      f = new File("docs/external/EASIKDOCUMENTATIONRR.htm");
      java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
      desktop.open(f);
    } catch (Exception e1) {
      error();
    }

  }

  private static void error() {
    java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
    try {
      desktop.browse(new java.net.URI("http://www.mta.ca/~rrosebru/project/Easik/docs/all.html"));
    } catch (URISyntaxException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    // these won't be critical errors so just show a simple message
    // JOptionPane.showMessageDialog(null, "Unable to load help files",
    // "Help Error", JOptionPane.ERROR_MESSAGE);
  }
}
