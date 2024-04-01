package easik.ui.menu.popup;

//~--- non-JDK imports --------------------------------------------------------

import java.awt.Point;
//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.Action;

import easik.overview.Overview;
import easik.overview.vertex.SketchNode;
import easik.sketch.Sketch;
import easik.ui.FileChooser;
import easik.ui.FileFilter;
import easik.ui.SketchFrame;

/**
 * Action for creating a new sketch from an imported sketch in the popup menu.
 */
public class ImportSketchAction extends AbstractAction {
  /**
   *    
   */
  private static final long serialVersionUID = 4928252862188214825L;

  /** The point at which to place the imported sketch */
  Point _newPoint;

  /** The overview into which the new sketch will be imported */
  private Overview _theOverview;

  /**
   * Prepare the menu option, as well as pass a reference to the last clicked
   * point, which is used when positioning the new entity.
   *
   * @param inPoint    The sketch's last-rightclicked-position
   * @param inOverview
   */
  public ImportSketchAction(Point inPoint, Overview inOverview) {
    super("Import sketch...");

    _theOverview = inOverview;

    putValue(Action.SHORT_DESCRIPTION,
        "Add a new sketch to the document imported from a previously saved sketch export.");

    _newPoint = inPoint;
  }

  /**
   * Create the new sketch.
   * 
   * @param e The action event
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    File selFile = FileChooser.loadFile("Import EASIK Sketch", FileFilter.EASIK_SKETCH);

    if (selFile != null) {
      actionPerformed0(selFile);
    }
  }

  public void actionPerformed0(File selFile) {
    Point p = (_newPoint != null) ? _newPoint : _theOverview.getNewSketchPosition(10);

    _theOverview.getFrame().getInfoTreeUI().storeExpansion();

    SketchNode newNode = _theOverview.addNewSketch(_theOverview.getNewName("ImportedSketch0"), p.getX(), p.getY());

    _theOverview.getFrame().getInfoTreeUI().storeExpansion();

    SketchFrame newFrame = newNode.getFrame();
    Sketch sk = newFrame.getMModel();

    if (!sk.loadFromXML(selFile)) {
      return;
    }

    newNode.updateName();
    sk.updateThumb();
    _theOverview.getFrame().getInfoTreeUI().revertExpansion();
    _theOverview.setDirty(true);
    _theOverview.refresh(); // Updates gui
  }
}
