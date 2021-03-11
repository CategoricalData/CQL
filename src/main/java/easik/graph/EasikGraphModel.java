package easik.graph;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.Color;

import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoableEdit;

import org.jgraph.graph.AttributeMap;
import org.jgraph.graph.DefaultGraphModel;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.GraphUndoManager;

import easik.Easik;

/**
 *
 *
 * @version 12/09/12
 * @author Christian Fiddick
 */
public abstract class EasikGraphModel extends DefaultGraphModel {
  // Handles insignificant edits (see beginInsignificantEdit()):

  /**
   *    
   */
  private static final long serialVersionUID = 3359687236243390701L;

  /**  */
  private int insignificantLevel = 0;

  /** The manager in charge of keeping track of undoable edits */
  private GraphUndoManager undoManager = new GraphUndoManager();

  /**  */
  private CompoundEdit insignificantEdit;

  /**
   *
   */
  public EasikGraphModel() {
    super();

    undoManager.setLimit(100); // FIXME: more? Make it a setting?
    addUndoableEditListener(undoManager);
  }

  /**
   * Returns the common attribute mapping for edges. Used by
   * normalEdgeAttributes() et al. to establish the common attributes for an edge.
   * 
   * @return The attribute mapping
   */
  @SuppressWarnings("static-access")
  public static AttributeMap commonEdgeAttributes() {
    AttributeMap map = new AttributeMap();

    GraphConstants.setLabelAlongEdge(map, true);
    GraphConstants.setBendable(map, false);
    GraphConstants.setMoveable(map, false);
    GraphConstants.setConnectable(map, false);
    GraphConstants.setDisconnectable(map, false);

    EdgeRouter er = EdgeRouter.getSharedInstance();

    er.setEdgeSeparation(20.0);
    er.setEdgeDeparture(50.0);
    GraphConstants.setRouting(map, er);
    GraphConstants.setLineStyle(map, GraphConstants.STYLE_SPLINE);

    return map;
  }

  /**
   * Alias for calling getColor() on the EasikSettings instance
   *
   * @param name the color property name to pass to
   *             {@link EasikSettings.getColor(String)}
   *
   * @return
   */
  protected static Color getColor(String name) {
    return Easik.getInstance().getSettings().getColor(name);
  }

  /**
   * Alias for calling getFloat() on the EasikSettings instance.
   *
   * @param name         the property name to pass to
   *                     {@link EasikSettings.getFloat(String, float)};
   *                     <code>"_width"</code> will be appended to the property
   *                     name.
   * @param defaultValue the default value to pass to
   *                     {@link EasikSettings.getFloat(String, float)}
   *
   * @return
   */
  protected static float getWidth(String name, float defaultValue) {
    return Easik.getInstance().getSettings().getFloat(name + "_width", defaultValue);
  }

  /**
   *
   *
   * @param name
   * @param defaultValue
   *
   * @return
   */
  protected static float getWidth(String name, int defaultValue) {
    return getWidth(name, (float) defaultValue);
  }

  /**
   *
   *
   * @param name
   * @param defaultValue
   *
   * @return
   */
  protected static float getWidth(String name, double defaultValue) {
    return getWidth(name, (float) defaultValue);
  }

  /**
   * Alias for calling getInt() on the EasikSettings instance.
   *
   * @param name         the property name to pass to
   *                     {@link EasikSettings.getInt(String, int)};
   *                     <code>"_width"</code> will be appended to the property
   *                     name.
   * @param defaultValue the default value to pass to
   *                     {@link EasikSettings.getInt(String, int)}
   *
   * @return
   */
  protected static int getIntWidth(String name, int defaultValue) {
    return Easik.getInstance().getSettings().getInt(name + "_width", defaultValue);
  }

  /**
   * Clears the current graph selection prior to an undo/redo. Must be implemented
   * by subclasses.
   */
  public abstract void clearSelection();

  /**
   * Sets the current graph to "dirty". Must be implemented. Will be called when
   * an undo/redo occur.
   */
  public abstract void setDirty();

  /**
   * Performs the most recently made edit's undo method.
   */
  public void undo() {
    clearSelection();
    undoManager.undo();
  }

  /**
   * Performs the redo method on the most recently undone edit.
   */
  public void redo() {
    clearSelection();
    undoManager.redo();
  }

  /**
   * Discards all edits in the undo manager.
   *
   * @see javax.swing.undo.UndoManager.discardAllEdits()
   */
  public void discardUndo() {
    undoManager.discardAllEdits();
  }

  /**
   * @return True if there exists an edit that can be undone, false otherwise.
   */
  public boolean canUndo() {
    return undoManager.canUndo();
  }

  /**
   * @return True if there exists an edit that can be redone, false otherwise.
   */
  public boolean canRedo() {
    return undoManager.canRedo();
  }

  /**
   * Begins a new compount edit. Each UndoableEdit then needs to be passed to
   * postEdit(), then finally endUpdate() called to signify the end of the change.
   * This properly handles nested updates, so
   * begin-add1-begin-add2-add3-end-add4-end gets added as a CompoundEdit with
   * three edits: add1, a CompountEdit of add2 and add3, and add4. What this means
   * is that code can safely call postEdit() with an UndoableEdit as needed: if an
   * update is in progress, it will be added to the in-progress compound edit,
   * otherwise it will be treated as its own edit. Any code that needs to call
   * other methods should do a beginUpdate(), then add its own UndoableEdits, make
   * the method calls, then finally call endUpdate()--no matter what happens in
   * such a case, everything from beginUpdate() to endUpdate() will be considered
   * a single edit.
   *
   * Note that if the automaton is currently in an insignificant edit mode, any
   * edit that occur will also be insignificant.
   */
  @Override
  public synchronized void beginUpdate() {
    if (insignificantLevel > 0) {
      insignificantLevel++;
    } else {
      super.beginUpdate();
    }
  }

  /**
   * Determines if EASIK currently in an insignificant update.
   * 
   * @return True if in an insignificant update, false otherwise.
   */
  public boolean inInsignificantUpdate() {
    return insignificantLevel > 0;
  }

  /**
   * Similar to beginUpdate(), but this beings an *insignificant* edit--that is,
   * an edit that won't be considered a "step" in the undo/redo list, but will be
   * undone/redone when moving between significant steps.
   */
  public synchronized void beginInsignificantUpdate() {
    if (insignificantLevel == 0) {
      insignificantEdit = new CompoundEdit() {
        /**
         *                
         */
        private static final long serialVersionUID = -7589808295352003936L;

        @Override
        public boolean isSignificant() {
          return false;
        }
      };

      insignificantLevel++;
    }
  }

  /**
   * Ends an insignificant edit, and posts it to the underlying UndoManager.
   */
  public synchronized void endInsignificantUpdate() {
    insignificantLevel--;

    if (insignificantLevel == 0) {
      insignificantEdit.end();
      postEdit(insignificantEdit);

      insignificantEdit = null;
    }

    if (insignificantLevel < 0) {
      insignificantLevel = 0;
    }
  }

  /**
   * Ends an insignificant edit and does *not* post it to the underlying
   * UndoManager. The change will not show up in the undo history.
   */
  public synchronized void cancelInsignificantUpdate() {
    insignificantLevel--;

    if (insignificantLevel == 0) {
      insignificantEdit.die();

      insignificantEdit = null;
    } else if (insignificantLevel < 0) {
      insignificantLevel = 0;
    }
  }

  /**
   * Adds an edit to the compound edit in progress, if beginUpdate() or
   * beginInsignificantEdit() have been called, otherwise posts the edit directly
   * to the undo manager.
   *
   * @see beginUpdate()
   *
   * @param e
   */
  @Override
  public synchronized void postEdit(UndoableEdit e) {
    if (insignificantLevel > 0) {
      insignificantEdit.addEdit(e);
    } else {
      super.postEdit(e);
    }
  }

  /**
   * Finishes a compound edit and sends it to the undo manager, assuming the
   * current update isn't part of a nested update.
   *
   * @see beginUpdate()
   */
  @Override
  public synchronized void endUpdate() {
    if (insignificantLevel > 0) {
      endInsignificantUpdate();
    } else {
      super.endUpdate();
    }
  }
}
