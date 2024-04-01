package easik.model.constraint;

//~--- non-JDK imports --------------------------------------------------------

import java.awt.Color;
//~--- JDK imports ------------------------------------------------------------
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import org.jgraph.graph.AttributeMap;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.GraphLayoutCache;

import easik.EasikTools;
import easik.graph.EasikGraphModel;
import easik.model.Model;
import easik.model.attribute.EntityAttribute;
import easik.model.edge.GuideEdge;
import easik.model.edge.ModelEdge;
import easik.model.edge.TriangleEdge;
import easik.model.path.ModelPath;
import easik.model.states.LoadingState;
import easik.model.ui.ModelFrame;
import easik.model.vertex.ModelVertex;
import easik.sketch.Sketch;
import easik.sketch.edge.SketchEdge;
import easik.sketch.util.graph.SketchGraphModel;
import easik.sketch.vertex.EntityNode;
import easik.ui.SketchFrame;

/**
 * This class will replace Constraint.java
 * 
 * This is the superclass for all constraints. Constraints get a non-unique
 * label that shows what kind of constraint it is (symbolically) and have a set
 * of paths which it deals with.
 *
 * Note: subclasses must call assignToEntites(..) after edge set has been
 * populated.
 *
 * Based on work by:
 * 
 * @author Rob Fletcher 2005
 * @author Kevin Green 2006
 * @author Vera Ranieri 2006
 * @version 2006-07-26 Kevin Green
 * 
 * @author Federico Mora
 */
public abstract class ModelConstraint<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>>
    extends ModelVertex<F, GM, M, N, E> {
  /**  */
  private static final long serialVersionUID = 4073411262247365979L;

  /** Stores whether the constraint is visible or not */
  protected boolean _isVisible = true;

  /** Stores whether or not the visuals are part of the graph */
  protected boolean _edgesVisualized = false, _nodeVisualized = false;

  /** The edge involved in the constraint */
  protected ArrayList<E> _edges;

  /** The paths involved in the constraint */
  protected ArrayList<ModelPath<F, GM, M, N, E>> _paths;

  /** The node representing this constraint in the info tree */
  @SuppressWarnings("hiding")
  protected DefaultMutableTreeNode _treeNode;

  /** The Guide Edges of the constraint */
  protected ArrayList<GuideEdge<F, GM, M, N, E>> _visuals;

  protected int ID;

  /**
   * Default constructor needed for subclasses
   *
   * @param inModel
   */
  public ModelConstraint(M inModel) {
    super(inModel);

    ID = _theModel.getConstraintID();
    _theModel.incConstraintID();
    _visuals = new ArrayList<>();
  }

  /**
   * Creates a very simple constraint node.
   *
   * @param name      The label on the node
   * @param x         x coordinate
   * @param y         y coordinate
   * @param isVisible If the constraint is visible in the graph or not
   * @param inModel   the sketch this constraint belongs to
   */
  public ModelConstraint(String name, int x, int y, boolean isVisible, M inModel) {
    super(name, x, y, inModel);

    ID = _theModel.getConstraintID();
    _theModel.incConstraintID();
    _isVisible = isVisible;
    _visuals = new ArrayList<>();
  }

  /**
   * used by views to match their constraints to sketch constraints Id is the
   * mechanism used.
   * 
   * @author Federico Mora
   */
  public ModelConstraint(M inModel, int id2) {
    super(inModel);

    ID = id2;
    _theModel.incConstraintID();
    _visuals = new ArrayList<>();
  }

  /**
   * Returns if the constraint is visible or not (True = visible)
   *
   * @return If the constraint is visible or not
   */
  public boolean isVisible() {
    return _isVisible;
  }

  /**
   * Returns an ID which is sued to match cConstraints to ViewConstraints
   *
   * @return the ID int of the constraint
   */
  public int getID() {
    return ID;
  }

  /**
   * Sets if the constraint should be visible or not
   *
   * @param inIsVisible If the constraint should be visible or not.
   */
  public void setVisible(boolean inIsVisible) {
    if (inIsVisible != _isVisible) {
      _isVisible = inIsVisible;

      _theModel.setDirty();
    }

    // GuideEdge<F, GM, M, N, E>[] visuals = _visuals.toArray(new GuideEdge[0]);
    @SuppressWarnings("unused")
    GraphLayoutCache glc = _theModel.getGraphLayoutCache();

    if (_isVisible && !_edgesVisualized) {
      addVisualsToModel();
    } else if (!_isVisible && _edgesVisualized) {
      removeVisualsFromModel();
    }

    _theModel.clearSelection();
  }

  /**
   * Adds the visual aids to the sketch
   */
  private void addVisualsToModel() {
    // Push loading state
    _theModel.getStateManager().pushState(new LoadingState<>(_theModel));
    _theModel.getGraphModel().beginInsignificantUpdate();
    _visuals.clear();

    // Get Nodes involved in constraint
    LinkedHashSet<N> entitiesInvolved = new LinkedHashSet<>();

    for (E edge : _edges) {
      entitiesInvolved.add(edge.getSourceEntity());
      entitiesInvolved.add(edge.getTargetEntity());
    }

    int avgX = 0, avgY = 0;

    if ("limitconstraint".equals(getType())) { // TRIANGLES CF2012
                          // The limit ('triangle')
                          // constraint is drawn
                          // slightly differently
      for (N node : entitiesInvolved) {
        avgX += node.getX();
        avgY += node.getY();
      }

      LimitConstraint<F, GM, M, N, E> thisConstraintAsLC = ((LimitConstraint<F, GM, M, N, E>) this);
      N constraintRoot = thisConstraintAsLC.getLimitNode();
      final float BIG_EDGE_WIDTH = (float) 3.0;
      final float LITTLE_EDGE_WIDTH = (float) 2.0;
      final int TRANSPARENCY = 230;
      Color col = EasikTools.getUnusedColor(TRANSPARENCY); // a hopefully
                                  // unique
                                  // color,
                                  // 230 is
                                  // transparency

      _visuals.add(new TriangleEdge<>(this, constraintRoot, col, BIG_EDGE_WIDTH, GraphConstants.ARROW_NONE,
          GraphConstants.ARROW_NONE));
      _visuals.add(new TriangleEdge<>(this, thisConstraintAsLC.getCone().getA(), col, LITTLE_EDGE_WIDTH,
          GraphConstants.ARROW_NONE, GraphConstants.ARROW_NONE));
      _visuals.add(new TriangleEdge<>(this, thisConstraintAsLC.getCone().getB(), col, LITTLE_EDGE_WIDTH,
          GraphConstants.ARROW_NONE, GraphConstants.ARROW_NONE));
      _visuals.add(new TriangleEdge<>(this, thisConstraintAsLC.getCone().getC(), col, LITTLE_EDGE_WIDTH,
          GraphConstants.ARROW_NONE, GraphConstants.ARROW_NONE));
    } else {
      // Place this object at the average position of all the involved
      // entities, and draw
      // edges going to each entity involved
      for (N node : entitiesInvolved) {
        GuideEdge<F, GM, M, N, E> myEdge = new GuideEdge<>(this, node);

        _visuals.add(myEdge);

        avgX += node.getX();
        avgY += node.getY();
      }
    }

    // If only one entity is involved add some vertical space
    if (entitiesInvolved.size() == 1) {
      avgY += 20;
    }

    int posX = getX();
    int posY = getY();

    // If visual does not already have a position then position it
    if ((posX == 0) && (posY == 0)) {
      posX = avgX / entitiesInvolved.size();
      posY = avgY / entitiesInvolved.size();
    }

    // Set the on-screen position of our entity to the attributes of the
    // entity
    AttributeMap attribs = getAttributes();

    GraphConstants.setAutoSize(attribs, true);
    GraphConstants.setBounds(attribs, new Rectangle2D.Double(posX, posY, 0, 0));
    GraphConstants.setSelectable(attribs, false);

    // Actually add them to the sketch display:
    GraphLayoutCache glc = _theModel.getGraphLayoutCache();

    if (!_nodeVisualized) {
      glc.insert(this);

      _nodeVisualized = true;
    }

    glc.insert(_visuals.toArray(new GuideEdge[0]));

    _edgesVisualized = true;

    _theModel.getGraphModel().endInsignificantUpdate();
    _theModel.refresh();

    // Pop state
    _theModel.getStateManager().popState();
  }

  /**
   * Removes the visuals from the sketch (when hiding)
   */
  private void removeVisualsFromModel() {
    // Push loading state
    _theModel.getStateManager().pushState(new LoadingState<>(_theModel));
    _theModel.getGraphModel().beginInsignificantUpdate();

    GraphLayoutCache glc = _theModel.getGraphLayoutCache();

    glc.remove(_visuals.toArray(new GuideEdge[0]));
    _visuals.clear();

    _edgesVisualized = false;

    GraphConstants.setSelectable(getAttributes(), false);
    _theModel.refresh();
    _theModel.getGraphModel().cancelInsignificantUpdate();

    // Pop state
    _theModel.getStateManager().popState();
  }

  /**
   * Tests to see if an entity is either the domain or co-domain of any of the
   * paths involved in the constraint
   *
   * @param inEntity The entity to be tested
   * @return True if it is either domain or co-domain of a path involved in the
   *         constraint, false otherwise.
   */
  public boolean isDomainOrCoDomain(N inEntity) {
    @SuppressWarnings("unused")
    int size = _paths.size();

    for (ModelPath<F, GM, M, N, E> path : _paths) {
      if ((path.getDomain() == inEntity) || (path.getCoDomain() == inEntity)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Adds all edges participating in this constraint to an arraylist of edges.
   */
  protected void addEdges() {
    _edges = new ArrayList<>();

    // Fill in _edges array
    for (ModelPath<F, GM, M, N, E> p : _paths) {
      List<E> curPathEdges = p.getEdges();

      for (E e : curPathEdges) {
        if (!_edges.contains(e)) {
          _edges.add(e);
        }
      }
    }
  }

  /**
   * Finds all of the entities involved in this constraint and makes a call to
   * addConstraint(this) on each one. Duplicates will be found but Java's
   * Set.add(..) method takes care of that.
   * 
   * This method is only currently used for Sketches since we currently do not
   * allow users to create new constraints in Views.
   */
  protected void assignToEntities() {
    ArrayList<ModelPath<F, GM, M, N, E>> pp = getProjectionPaths();
    for (ModelPath<F, GM, M, N, E> p : this.getPaths()) {
      N dom = p.getDomain();
      N codom = p.getCoDomain();

      for (E edge : p.getEdges()) {
        N source = edge.getSourceEntity();
        N target = edge.getTargetEntity();

        if (pp != null && pp.contains(p)) {
          String name = "BC" + this.getID();

          if (source != dom) {
            // Create Entity Attribute
            EntityAttribute<F, GM, M, N, E> newAtt = new EntityAttribute<>(name,
                new easik.database.types.Boolean(), source);
            source.addHiddenEntityAttribute(newAtt);
          }
          if (target != codom) {
            // Create Entity Attribute
            EntityAttribute<F, GM, M, N, E> newAtt = new EntityAttribute<>(name,
                new easik.database.types.Boolean(), target);
            target.addHiddenEntityAttribute(newAtt);
          }
        }

        source.addConstraint(this);
        target.addConstraint(this);
      }
    }
  }

  /**
   * 
   * @return ArrayList of projection paths. Null if constraint does not have
   *         projection paths.
   */
  public abstract ArrayList<ModelPath<F, GM, M, N, E>> getProjectionPaths();

  /**
   * Returns all Nodes in the constraint.
   * 
   * @return
   */
  public ArrayList<N> getEntities() {
    ArrayList<N> nodes = new ArrayList<>();
    for (ModelPath<F, GM, M, N, E> p : _paths) {
      for (N n : p.getEntities()) {
        if (!nodes.contains(n)) {
          nodes.add(n);
        }
      }
    }
    return nodes;
  }

  /**
   * Accessor for the constraint label
   *
   * @return The label
   */
  @Override
  public String toString() {
    return getName();
  }

  /**
   * Checks to see if the constraint contains a path that uses an edge
   *
   * @param inEdge The edge to check against.
   * @return True if constraint contains inEdge, false otherwise.
   */
  public boolean hasEdge(E inEdge) {
    return _edges.contains(inEdge);
  }

  /**
   * Returns the edges dealt with in the constraint.
   *
   * @return The edges
   */
  public List<E> getEdges() {
    return Collections.unmodifiableList(_edges);
  }

  /**
   * Returns the guide edges used by the constaint.
   *
   * @return the GuideEdges
   */
  public List<GuideEdge<F, GM, M, N, E>> getGuideEdges() {
    return Collections.unmodifiableList(_visuals);
  }

  /**
   * Returns the set of the paths involved in the constraint
   *
   * @return The array of sketch paths
   */
  public List<ModelPath<F, GM, M, N, E>> getPaths() {
    return Collections.unmodifiableList(_paths);
  }

  /**
   * Returns a string corresponding to the constraint type. (used in XML
   * generation).
   *
   * @return A string of the type of constraint
   */
  public String getType() {
    if (this instanceof SumConstraint) {
      return "sumconstraint";
    } else if (this instanceof PullbackConstraint) {
      return "pullbackconstraint";
    } else if (this instanceof EqualizerConstraint) {
      return "equalizerconstraint";
    } else if (this instanceof ProductConstraint) {
      return "productconstraint";
    } else if (this instanceof CommutativeDiagram) {
      return "commutativediagram";
    } else if (this instanceof LimitConstraint) {
      return "limitconstraint";
    } else {
      return "unknown";
    }
  }

  /**
   *
   *
   * @return
   */
  @Override
  public DefaultMutableTreeNode getTreeNode() {
    if (_treeNode == null) {
      _treeNode = new DefaultMutableTreeNode(this);
    }

    return _treeNode;
  }

  /**
   * Sets all constraints to visible/invisible in the sketch.
   *
   * @param hashMap The list of constraints.
   * @param show    true if constraints are to be set to visible, false otherwise
   * @since 2006-05-29 Vera Ranieri
   */
  public static void setAllConstraintsVisible(
      HashMap<Integer, ModelConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge>> hashMap,
      boolean show) {
    for (ModelConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> c : hashMap.values()) {
      c.setVisible(show);
    }
  }

  /**
   * Determines the entities involved in a constraint and lists them for an error
   * message.
   * 
   * @param paths The paths forming the constraint
   * @return A string of the domains and codomains of the constraint, formatted
   *         for an error message
   * @since 2006-08-04 Vera Ranieri
   */
  public static String getTablesInvolvedForError(
      ArrayList<ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge>> paths) {
    if (paths == null) {
      return null;
    }
    ArrayList<String> domains = new ArrayList<>();
    ArrayList<String> codomains = new ArrayList<>();
    String domain, codomain;

    for (ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> p : paths) {
      domain = p.getDomain().getName();
      codomain = p.getCoDomain().getName();

      if (!domains.contains(domain)) {
        domains.add(domain);
      }

      if (!codomains.contains(codomain)) {
        codomains.add(codomain);
      }
    }

    String result = "With domains: ";

    for (String d : domains) {
      result += d + ", ";
    }

    result = result.substring(0, result.length() - 2) + "\n and \n Codomains: ";

    for (String c : codomains) {
      result += c + ", ";
    }

    result = result.substring(0, result.length() - 2);

    return result;
  }

  /**
   * Overrides superclass getAttributes to override selectable status.
   *
   * @return
   */
  @Override
  public AttributeMap getAttributes() {
    AttributeMap attrs = super.getAttributes();

    // Selectable as long as it is visible:
    GraphConstants.setSelectable(attrs, isVisible());

    return attrs;
  }
}
