package easik.ui;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.SwingUtilities;

import org.jgraph.JGraph;
import org.jgraph.plaf.basic.BasicGraphUI;

/**
 * A subclass of BasicGraphUI which we are overloading to allow for better
 * control of the UI. For now we will simply make it ignore all but left clicks,
 * allowing the right clicks to be used for popup menus
 *
 * @author Rob Fletcher 2005
 */
public class GraphUI extends BasicGraphUI {
  private static final long serialVersionUID = -101070984660815029L;

  /**
   * Accessor for the JGraph
   * 
   * @return the JGraph
   */
  public JGraph getGraph() {
    return graph;
  }

  /**
   * Overloaded method used to create a new mouse listener
   * 
   * @return New mouse listener
   */
  @Override
  protected MouseListener createMouseListener() {
    return new MouseHandler();
  }

  /**
   * A subclass of Mouse Handler which will ignore all but the left click.
   *
   * @author Rob Fletcher
   */
  class MouseHandler extends BasicGraphUI.MouseHandler {
    private static final long serialVersionUID = 8279681235456030395L;

    /**
     * Mousepressed function examines to see if the mouse event is a left click, and
     * only then will it perform it's superclass's duties.
     *
     * @param e Mouse event
     */
    @Override
    public void mousePressed(MouseEvent e) {
      if (SwingUtilities.isLeftMouseButton(e)) {
        super.mousePressed(e);
      }
    }

    /**
     * Overwritten JGraph method to remove creation of edge handles on right click
     *
     * @param e Mouse event
     */
    @Override
    public void mouseDragged(MouseEvent e) {
      autoscroll(graph, e.getPoint());

      if (graph.isEnabled()) {
        if ((handler != null) && (handler == marquee)) {
          marquee.mouseDragged(e);
        } else if ((handler == null) && !isEditing(graph) && (focus != null)) {
          if (!graph.isCellSelected(focus.getCell())) {
            selectCellForEvent(focus.getCell(), e);

            cell = null;
          }

          handler = handle;
        }

        if ((handle != null) && (handler == handle)) {
          handle.mouseDragged(e);
        }
      }
    }
  }
}
