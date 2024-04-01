package easik.ui.menu;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import easik.Easik;
import easik.graph.EasikGraphModel;
import easik.model.Model;
import easik.model.edge.ModelEdge;
import easik.model.ui.ModelFrame;
import easik.model.vertex.ModelVertex;
import easik.ui.FileChooser;
import easik.ui.FileFilter;

/**
 * Menu action to export the current M as an image.
 */
public class ExportImageAction<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>>
    extends AbstractAction {
  /**
   *    
   */
  private static final long serialVersionUID = -6561061653242962570L;

  /**  */
  M _theModel;

  /**
   * Creates a new ExportImageAction. Adds a new menu option to the File menu.
   *
   * @param inFrame
   */
  public ExportImageAction(F inFrame) {
    super("image...");

    if (inFrame != null) {
      _theModel = inFrame.getMModel();
      putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_I));
    }

    putValue(Action.SHORT_DESCRIPTION, "Export an image of the current M.");
  }

  /**
   * Creates a new image of the current M and prompts the user for a place to save
   * it. Displays a message if an error occurred.
   *
   * @param e The action event
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    if (_theModel.getEntities().isEmpty()) {
      showError("Unable to export: the M is empty!", "Error");

      return;
    }

    FileFilter imageFilter = new FileFilter("Image files (*.png; *.jpg; *.gif; *.bmp)", "png", "jpg", "jpeg", "gif",
        "bmp");
    File selFile = FileChooser.saveFile("Export image", imageFilter, "png", _theModel.getName());

    if (selFile == null) {
      return; // Cancelled
    }

    java.awt.Color background = Easik.getInstance().getSettings().getColor("edit_canvas_background");
    BufferedImage snapshot = _theModel.getImage(background, 5);

    if (snapshot == null) {
      showError("Unable to export: failed to obtain a snapshot of the Model!", "Error");

      return;
    }

    String format = "png";
    Pattern suffix = Pattern.compile("\\.(?i:(png|bmp|jpg|jpeg|gif))$");
    Matcher m = suffix.matcher(selFile.getName());

    if (m.find()) {
      format = m.group(1).toLowerCase();
    }

    try {
      ImageIO.write(snapshot, format, selFile);
    } catch (IOException ioe) {
      showError("Exporting image failed: " + ioe.getMessage(), "Error");

      return;
    }

    JOptionPane.showMessageDialog(_theModel.getFrame(), "Created " + selFile.getName() + " successfully",
        "Image Exported", JOptionPane.INFORMATION_MESSAGE);
  }

  /**
   *
   *
   * @param message
   * @param title
   */
  private void showError(String message, String title) {
    JOptionPane.showMessageDialog(_theModel.getFrame(), message, title, JOptionPane.ERROR_MESSAGE);
  }
}
