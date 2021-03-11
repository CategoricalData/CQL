package easik.ui.menu;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.Action;

import easik.overview.vertex.SketchNode;
import easik.sketch.Sketch;
import easik.ui.ApplicationFrame;
import easik.ui.EasikFrame;
import easik.ui.FileChooser;
import easik.ui.FileFilter;
import easik.ui.SketchFrame;

/**
 * Menu action for exporting the sketch as XML
 * 
 * @author Rob Fletcher 2005
 * @author Kevin Green 2006
 * @version 2006-07-13 Kevin Green
 */
public class SketchExportAction extends AbstractAction {
  /**
   *    
   */
  private static final long serialVersionUID = 6718927075215733294L;

  /**  */
  EasikFrame _theFrame;

  /**
   * Create a new save as menu action.
   * 
   * @param inFrame the frame
   */
  public SketchExportAction(final EasikFrame inFrame) {
    super((inFrame instanceof SketchFrame) ? "Sketch XML..." : "Export sketch to XML...");

    _theFrame = inFrame;

    putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_A));
    putValue(Action.SHORT_DESCRIPTION, "Export current sketch as XML document");
  }

  /**
   * Display a dialog prompting the user for the name under which to save the
   * current sketch.
   *
   * @param e The action event
   */
  @Override
  public void actionPerformed(final ActionEvent e) {
    final Sketch sketch;

    if (_theFrame instanceof SketchFrame) {
      sketch = ((SketchFrame) _theFrame).getMModel();
    } else {
      final Object[] currentSelection = ((ApplicationFrame) _theFrame).getOverview().getSelectionCells();

      if (!((currentSelection.length == 1) && (currentSelection[0] instanceof SketchNode))) {
        System.err.println("Sketch export via overview popup should only be enabled when "
            + "selection size is 1, and it is a sketch node");

        return;
      }

      sketch = ((SketchNode) currentSelection[0]).getFrame().getMModel();
    }

    final File selFile = FileChooser.saveFile("Save EASIK Sketch", FileFilter.EASIK_SKETCH, "sketch");

    if (selFile != null) {
      sketch.getDocInfo().updateModificationDate();
      sketch.saveToXML(selFile);
    }
  }
}
