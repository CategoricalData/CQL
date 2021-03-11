package easik.model.util.graph;

//~--- JDK imports ------------------------------------------------------------
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;

//~--- non-JDK imports --------------------------------------------------------
import org.jgraph.JGraph;
import org.jgraph.graph.CellView;
import org.jgraph.graph.CellViewRenderer;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.VertexView;

import easik.Easik;
import easik.EasikSettings;
import easik.graph.EasikGraphModel;
import easik.model.Model;
import easik.model.attribute.EntityAttribute;
import easik.model.constraint.ModelConstraint;
import easik.model.edge.ModelEdge;
import easik.model.keys.UniqueKey;
import easik.model.ui.ModelFrame;
import easik.model.ui.ModelFrame.Mode;
import easik.model.vertex.ModelVertex;

/**
 *
 *
 * @version 12/09/12
 * @author Christian Fiddick
 */
public class ModelVertexRenderer<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>>
    extends JPanel implements CellViewRenderer {
  /**
   *    
   */
  private static final long serialVersionUID = 7888655094910055539L;

  /** The gradient color of the entity */
  transient protected Color gradientColor = null;

  /** The graph */
  transient protected M model = null;

  /** The entity label container, attributes, and unique keys panes */
  transient protected JPanel _entity, _attributes, _uniqueKeys;

  /** The label of the entity */
  transient protected JLabel _entityLabel;

  /** If the entity cell has focus or not */
  transient protected boolean hasFocus;

  /** If the graph is in preview mode */
  transient protected boolean preview;

  /** If the entity cell is selected */
  transient protected boolean selected;

  /** Creates a new circle renderer. */
  public ModelVertexRenderer() {
    super();
  }

  /**
   * Returns the renderer component after initializing it
   * 
   * @param graph   The graph (really a M in disguise)
   * @param view    The cell view
   * @param sel     If the view is selected or not
   * @param focus   If the view has focus or not
   * @param preview If the graph is in preview mode or not
   * @return The renderer component fully initialized
   */
  @Override
  @SuppressWarnings("unchecked")
  public Component getRendererComponent(JGraph graph, CellView view, boolean sel, boolean focus, boolean preview) {
    this.model = (M) graph;
    this.selected = sel;
    this.preview = preview;
    this.hasFocus = focus;

    ModelVertex<F, GM, M, N, E> v = (ModelVertex<F, GM, M, N, E>) view.getCell();

    // If the constraint is hidden, return a completely empty JPanel that
    // doesn't paint anything
    if ((v instanceof ModelConstraint) && !((ModelConstraint<F, GM, M, N, E>) v).isVisible()) {
      return new JPanel() {

        private static final long serialVersionUID = -8516030326162065848L;

        @Override
        public void paint(Graphics g) {
        }
      };
    }

    // if entity moved, set overview as dirty FIXME(if not working properly,
    // may be because of int casting and off by one)
    if ((int) (view.getBounds().getX()) != v.getX() || (int) (view.getBounds().getY()) != v.getY()) {
      model.setDirty();
    }

    // Initialize panel
    removeAll();
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

    // Add in entity/constraint label
    add(_entity = new JPanel(new GridLayout(1, 1)));
    _entity.add(_entityLabel = new JLabel(v.getName(), SwingConstants.CENTER));

    N myNode = (N) v;

    // Add in attributes
    _attributes = new JPanel(new GridLayout(0, 1));

    add(_attributes);

    for (EntityAttribute<F, GM, M, N, E> att : myNode.getEntityAttributes()) {
      _attributes.add(new JLabel(" @ " + att.getName()));
    }

    if ((_attributes.getComponentCount() == 0) || !model.getFrame().getShowAttsVal()) {
      _attributes.setVisible(false);
    }

    // Add in unique keys
    _uniqueKeys = new JPanel(new GridLayout(0, 1));

    add(_uniqueKeys);

    for (UniqueKey<F, GM, M, N, E> key : myNode.getUniqueKeys()) {
      _uniqueKeys.add(new JLabel(" $ " + key.getKeyName()));
    }

    if ((_uniqueKeys.getComponentCount() == 0) || !model.getFrame().getShowAttsVal()) {
      _uniqueKeys.setVisible(false);
    }

    @SuppressWarnings("rawtypes")
    Map attributes = view.getAllAttributes();

    installAttributes(v, attributes);

    // Set desired size
    setSize(getPreferredSize());

    return this;
  }

  /**
   * Takes the graph constants assigned and uses them with this class
   *
   *
   * @param v
   * @param attributes The attributes in use
   */
  protected void installAttributes(ModelVertex<F, GM, M, N, E> v, Map<?, ?> attributes) {
    _entityLabel.setIcon(GraphConstants.getIcon(attributes));
    _entityLabel.setVerticalAlignment(GraphConstants.getVerticalAlignment(attributes));
    _entityLabel.setHorizontalAlignment(GraphConstants.getHorizontalAlignment(attributes));
    _entityLabel.setVerticalTextPosition(GraphConstants.getVerticalTextPosition(attributes));
    _entityLabel.setHorizontalTextPosition(GraphConstants.getHorizontalTextPosition(attributes));
    _entity.setOpaque(true);
    _entityLabel.setOpaque(true);

    String modeTag = (model.getFrame().getMode() == Mode.EDIT) ? "edit_" : "manip_";

    setOpaque(GraphConstants.isOpaque(attributes));
    setBorder(GraphConstants.getBorder(attributes));

    Color bordercolor = GraphConstants.getBorderColor(attributes);
    int borderWidth = Math.max(1, Math.round(GraphConstants.getLineWidth(attributes)));

    if ((getBorder() == null) && (bordercolor != null)) {
      setBorder(BorderFactory.createLineBorder(bordercolor, borderWidth));
    }

    if (bordercolor == null) {
      if (getBorder() instanceof LineBorder) {
        bordercolor = ((LineBorder) getBorder()).getLineColor();
      } else {
        bordercolor = Color.black;
      }
    }

    Color foreground = GraphConstants.getForeground(attributes);

    if (foreground == null) {
      foreground = model.getForeground();
    }

    setForeground(foreground);
    _entityLabel.setForeground(foreground);

    Color background = GraphConstants.getBackground(attributes);

    setBackground((background != null) ? background : model.getBackground());

    EasikSettings s = Easik.getInstance().getSettings();

    if (v != null) { // TODO aql easik
      _attributes.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, bordercolor));
      _uniqueKeys.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, bordercolor));

      Color entityFG = s.getColor(modeTag + "entity_fg", Color.black),
          entityBG = s.getColor(modeTag + "entity_bg", Color.white),
          attribFG = s.getColor(modeTag + "attribute_fg", foreground),
          attribBG = s.getColor(modeTag + "attribute_bg", background);

      _entityLabel.setBackground(entityBG);
      _entityLabel.setForeground(entityFG);
      _attributes.setBackground(attribBG);
      _attributes.setOpaque(true);
      _uniqueKeys.setBackground(attribBG);
      _uniqueKeys.setOpaque(true);

      for (Component c : _attributes.getComponents()) {
        c.setForeground(attribFG);

        if (c instanceof JComponent) {
          ((JComponent) c).setOpaque(false);
        }
      }

      for (Component c : _uniqueKeys.getComponents()) {
        c.setForeground(attribFG);

        if (c instanceof JComponent) {
          ((JComponent) c).setOpaque(false);
        }
      }
    } else { // A constraint
      Color constFG = s.getColor(modeTag + "constraint_fg", Color.black),
          constBG = s.getColor(modeTag + "constraint_bg", Color.white);

      _entityLabel.setBackground(constBG);
      _entityLabel.setForeground(constFG);
    }

    gradientColor = GraphConstants.getGradientColor(attributes);

    setFont(GraphConstants.getFont(attributes));
  }

  /**
   * Draws the object on the screen
   *
   * @param g The graphics to be drawn
   */
  @Override
  public void paint(Graphics g) {
    try {
      if ((gradientColor != null) && !preview) {
        setOpaque(false);

        Graphics2D g2d = (Graphics2D) g;

        g2d.setPaint(new GradientPaint(0, 0, getBackground(), getWidth(), getHeight(), gradientColor, true));
        g2d.fillRect(0, 0, getWidth(), getHeight());
      }

      super.paint(g);
      paintSelectionBorder(g);
    } catch (IllegalArgumentException e) {
      // JDK Bug: Zero length string passed to TextLayout constructor
    }
  }

  /**
   * Provided for subclassers to paint a selection border.
   *
   * @param g Graphic to be drawn
   */
  protected void paintSelectionBorder(Graphics g) {
    ((Graphics2D) g).setStroke(GraphConstants.SELECTION_STROKE);
    g.setColor(model.getLockedHandleColor());

    if (selected) {
      Dimension d = getSize();

      g.drawRect(0, 0, d.width - 1, d.height - 1);
    }
  }

  /**
   * Returns the intersection of the bounding rectangle and the straight line
   * between the source and the specified point p. The specified point is expected
   * not to intersect the bounds.
   *
   * @param view   The view of the cell
   * @param source The source of the line
   * @param p      The inner point which the line should be drawn towards
   * @return The perimeter intersection point
   */
  @SuppressWarnings("static-method")
  public Point2D getPerimeterPoint(VertexView view, Point2D source, Point2D p) {
    Rectangle2D bounds = view.getBounds();
    double x = bounds.getX();
    double y = bounds.getY();
    double width = bounds.getWidth();
    double height = bounds.getHeight();
    double xCenter = x + width / 2;
    double yCenter = y + height / 2;
    double dx = p.getX() - xCenter; // Compute Angle
    double dy = p.getY() - yCenter;
    double alpha = Math.atan2(dy, dx);
    double xout = 0, yout = 0;
    double pi = Math.PI;
    double pi2 = Math.PI / 2.0;
    double beta = pi2 - alpha;
    double t = Math.atan2(height, width);

    if ((alpha < -pi + t) || (alpha > pi - t)) { // Left edge
      xout = x;
      yout = yCenter - width * Math.tan(alpha) / 2;
    } else if (alpha < -t) { // Top Edge
      yout = y;
      xout = xCenter - height * Math.tan(beta) / 2;
    } else if (alpha < t) { // Right Edge
      xout = x + width;
      yout = yCenter + width * Math.tan(alpha) / 2;
    } else { // Bottom Edge
      yout = y + height;
      xout = xCenter + height * Math.tan(beta) / 2;
    }

    return new Point2D.Double(xout, yout);
  }
}
