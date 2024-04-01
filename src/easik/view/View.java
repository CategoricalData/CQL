package easik.view;

//~--- non-JDK imports --------------------------------------------------------

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.undo.AbstractUndoableEdit;

import org.jgraph.graph.AttributeMap;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.GraphLayoutCache;

import easik.DocumentInfo;
import easik.model.Model;
import easik.model.ModelStateManager;
import easik.model.constraint.CommutativeDiagram;
import easik.model.constraint.ConstraintException;
import easik.model.constraint.EqualizerConstraint;
import easik.model.constraint.ModelConstraint;
import easik.model.constraint.ProductConstraint;
import easik.model.constraint.PullbackConstraint;
import easik.model.constraint.SumConstraint;
import easik.model.edge.ModelEdge.Cascade;
import easik.model.path.ModelPath;
import easik.model.states.LoadingState;
import easik.model.util.graph.ModelViewFactory;
import easik.overview.Overview;
import easik.overview.vertex.ViewNode;
import easik.sketch.Sketch;
import easik.sketch.edge.SketchEdge;
import easik.sketch.util.graph.SketchGraphModel;
import easik.sketch.vertex.EntityNode;
import easik.ui.SketchFrame;
import easik.ui.ViewFrame;
import easik.ui.menu.popup.DefineQueryNodeAction;
import easik.ui.tree.ModelInfoTreeUI;
import easik.view.edge.InjectiveViewEdge;
import easik.view.edge.NormalViewEdge;
import easik.view.edge.PartialViewEdge;
import easik.view.edge.View_Edge;
import easik.view.util.QueryException;
import easik.view.util.graph.ViewGraphModel;
import easik.view.vertex.QueryNode;
//~--- JDK imports ------------------------------------------------------------

/**
 *
 *
 * @version 12/09/12
 * @author Christian Fiddick
 * @author Updated by Federico Mora 5/2014
 */
public class View extends Model<ViewFrame, ViewGraphModel, View, QueryNode, View_Edge> {
  private static final long serialVersionUID = -8903773663603521530L;

  /**
   * Records whether the view has been modified since the last save. Initialized
   * to <b>false</b>
   */
  private boolean _dirty = false;

  /** The sketch this view is on */
  private Sketch _ourSketch;

  /**
   * The default constructor sets all the visual settings for the JGraph, as well
   * as Initializing the view to be empty. It also adds appropriate listeners for
   * all of the actions we are concerned with.
   *
   * @param inFrame    The view frame of the sketch
   * @param inSketch   The sketch this view is on
   * @param inOverview The overview in which this
   */
  public View(ViewFrame inFrame, Sketch inSketch, Overview inOverview) {
    super(inFrame, inOverview);

    _ourSketch = inSketch;

    _stateManager = new ModelStateManager<>(this);

    // Set up mouse listener to watch for double clicks
    // - Double clicks edit a query node
    this.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if ((_ourSketch.getFrame().getMode() == SketchFrame.Mode.EDIT) && (e.getClickCount() == 2)) {
          Object[] currSelection = getSelectionCells();

          if ((currSelection.length == 1) && (currSelection[0] instanceof QueryNode)) {
            DefineQueryNodeAction.updateNode((QueryNode) currSelection[0]);
          }
        }
      }
    });
  }

  /**
   * When we initialize the sketch, we flush out all the data concerning the
   * sketch itself. Even the modelAdapter is reinitialized.
   *
   * This methods serves as a "new sketch" function.
   */
  @Override
  public void initialiseModel() {
    clearSelection();

    model = new ViewGraphModel(this);

    final GraphLayoutCache glc = new GraphLayoutCache(model,
        new ModelViewFactory<ViewFrame, ViewGraphModel, View, QueryNode, View_Edge>());

    setModel(model);
    setGraphLayoutCache(glc);

    _nodes = new LinkedHashMap<>();
    _edges = new LinkedHashMap<>();

    if (_Frame.getInfoTreeUI() != null) {
      _Frame.setInfoTreeUI(new ModelInfoTreeUI<>(_Frame)); // Wipe
                                  // Tree
      _Frame.getInfoTreeUI().refreshTree();
    }

    _docInfo.reset();

    model.discardUndo();
  }

  /**
   * When we initialize a new sketch, we need to clear the selection buffer just
   * in case something is still selected. Or else it will remain selected because
   * there will be no events removing it.
   */
  public void newView() {
    initialiseModel();
  }

  /**
   * Used to initialize a new view based on provided data (usually from the Sketch
   * loading methods).
   *
   * @param queryNodes A map of all of the query nodes in the view
   * @param head       The header created from the loaded XML file.
   */
  public void initializeFromData(final Map<String, QueryNode> queryNodes, final DocumentInfo head,
      final Map<String, View_Edge> edges) {
    initialiseModel();

    _nodes = new LinkedHashMap<>();
    _docInfo = head;

    _Frame.setTreeName(_docInfo.getName());

    // Add the entities
    addEntity(queryNodes.values());

    // Add the entities
    addEdge(edges.values());

    /**
     * not initializing ViewCOnstraints from data at the moment for (final
     * ModelConstraint c : _constraints.values()) { addConstraint(c); }
     */

    refresh();
    model.discardUndo();
  }

  /**
   * Used to mark a view as dirty or not. Since it's only marked as non-dirty when
   * saving, we mark all the current queryNode positions if setting non-dirty.
   *
   * @param inDirty NEw dirtiness.
   */
  public void setDirty(boolean inDirty) {
    _dirty = inDirty;

    if (_dirty) {
      getDocInfo().updateModificationDate();
    }

    if (!_dirty) {
      for (QueryNode n : _nodes.values()) {
        n.savePosition();
      }
    }

    _Frame.setDirty(_dirty);
  }

  /**
   * Checks to see if any of the query nodes have moved and, if so, updates them
   * and sets the view to dirty. If the view is already dirty, we don't have to do
   * anything at all.
   */
  public void checkDirty() {
    if (_dirty) {
      return;
    }

    for (QueryNode n : _nodes.values()) {
      if (n == null) {
        // not sure why this would ever be null
      } else {
        Rectangle2D newBounds = GraphConstants.getBounds(n.getAttributes());
        if ((int) newBounds.getX() != n.getLastKnownX()) {
          setDirty(true);
          return;
        }
      }
    }

  }

  /**
   * Removes an entity, and also cascades to remove all the arrows involved with
   * it.
   *
   * @param toRemove The entity about to be removed
   */
  @Override
  public void removeNode(final QueryNode toRemove) {
    // So we don't get a ConcurrentModificationException
    final ArrayList<View_Edge> removeEdges = new ArrayList<>();

    for (final View_Edge edge : _edges.values()) {
      if (edge.getSourceQueryNode().equals(toRemove) || edge.getTargetQueryNode().equals(toRemove)) {
        // add the edge to a list of edges to remove
        removeEdges.add(edge);
      }
    }

    model.beginUpdate();

    // Remove the edges
    for (final View_Edge e : removeEdges) {
      removeEdge(e);
    }

    // remove the constraints this queryNode is a part of
    for (ModelConstraint<ViewFrame, ViewGraphModel, View, QueryNode, View_Edge> c : toRemove.getConstraints()) {
      if (_constraints.containsKey(c.getID())) {
        this.removeConstraint(c);
      }
    }

    _nodes.remove(toRemove.toString());
    getGraphLayoutCache().remove(new Object[] { toRemove });
    // Remove Entity from tree
    _Frame.getInfoTreeUI().removeNode(toRemove);
    model.endUpdate();
  }

  public void removeEdge(final View_Edge toRemove) {
    model.beginUpdate();
    for (final ModelConstraint<ViewFrame, ViewGraphModel, View, QueryNode, View_Edge> c : _constraints.values()) {
      if (c.hasEdge(toRemove)) {
        removeConstraint(c);
      }
    }
    _edges.remove(toRemove.getName());
    getGraphLayoutCache().remove(new Object[] { toRemove });
    model.endUpdate();
  }

  /**
   * Add a new, empty entity at point X, Y
   *
   * @param name The name of the new entity being added
   * @param x    X Coordinate of new entity
   * @param y    Y Coordinate of new entity
   */
  @Override
  public void addNewNode(final String name, final double x, final double y) {
    final QueryNode newEntity;

    // this won't throw exception because query is blank
    try {
      newEntity = new QueryNode(name, (int) x, (int) y, this, "");
      addEntity(newEntity);
    } catch (QueryException e) {
      e.printStackTrace();
    }
  }

  /**
   * Add one or more entities, or an array of entities, to the graph, dealing with
   * all of the dependencies.
   *
   * @param theEntities one or more QueryNodes (or an array of querynodes) to be
   *                    added
   */
  public void addEntity(final QueryNode... theEntities) {
    addEntity(Arrays.asList(theEntities));
  }

  /**
   * Adds a collection (set, list, etc.) of QueryNodes to the graph.
   *
   * @param theEntities the collection of entities to be added.
   */
  public void addEntity(final Collection<QueryNode> theEntities) {
    // Push loading state
    _stateManager.pushState(new LoadingState<>(this));

    final GraphLayoutCache glc = getGraphLayoutCache();

    model.beginUpdate();

    for (final QueryNode node : theEntities) {

      // Set the on-screen position of our entity to the attributes of the
      // entity
      final AttributeMap nAttribs = node.getAttributes();

      GraphConstants.setAutoSize(nAttribs, true);
      GraphConstants.setBounds(nAttribs, new Rectangle2D.Double(node.getX(), node.getY(), 0, 0));

      if (_nodes.containsKey(node.getName())) {
        node.setName(node.getName());
      }

      // Add our entity to the graph
      glc.insert(node);

      // Add our entity to our table of entities
      _nodes.put(node.toString(), node);

      // Add Entity to tree
      _Frame.getInfoTreeUI().addNode(node);
    }

    model.postEdit(new AbstractUndoableEdit() {
      /**
       *            
       */
      private static final long serialVersionUID = -74767611415529681L;

      @Override
      public void undo() {
        super.undo();

        for (final QueryNode node : theEntities) {
          _nodes.remove(node.toString());
        }
      }

      @Override
      public void redo() {
        super.redo();

        for (final QueryNode node : theEntities) {
          _nodes.put(node.toString(), node);
        }
      }
    });
    model.endUpdate();

    autoAddExistingEdges();
    // Reload the graph to respect the new changes
    glc.reload();

    // Pop state
    _stateManager.popState();
  }

  /**
   * Checks to see if a name is in use, so that we will not have several instances
   * at once.
   *
   * @param inName The desired new name to check against
   * @return Is it used or not.
   */
  @Override
  public boolean isNameUsed(String inName) {
    Set<String> keys = _nodes.keySet();

    for (String name : keys) {
      if (name.equalsIgnoreCase(inName)) {
        return true;
      }
    }

    for (EntityNode en : _ourSketch.getEntities()) {
      if (en.getName().equalsIgnoreCase(inName)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Gets the sketch to which this view is associated.
   * 
   * @return The sketch to which this view is associated.
   */
  public Sketch getSketch() {
    return _ourSketch;
  }

  /**
   * Called internally by View_Edge when an edge is renamed, to keep the edge map
   * consistent. Should not be called directly; instead just call
   * edge.setName("newname").
   *
   * @see easik.sketch.edge.SketchEdge#setName(String)
   * @param edge    the edge being renamed
   * @param oldName the old name of the edge
   * @param newName the candidate new name
   * @return a string containing the final new edge name, for SketchEdge to use.
   */
  @Override
  public String edgeRenamed(final View_Edge edge, final String oldName, String newName) {
    _edges.put(newName, _edges.remove(oldName));

    return newName;
  }

  /**
   * 
   * Adds one or more view edges (or an array of edges) to the view.
   *
   * @param inEdges one or more ViewEdges (or an array of viewEdges) to be added
   * 
   * 
   * @author Sarah van der Laan 2013
   */
  public void addEdge(View_Edge... inEdges) {
    addEdge(Arrays.asList(inEdges));

  }

  /**
   * Adds a collection (set, list, etc.) of edges to the view.
   *
   * @param inEdges Collection of ViewEdges to add
   * 
   * @author Sarah van der Laan 2013
   */
  public void addEdge(Collection<View_Edge> inEdges) {

    for (View_Edge edge : inEdges) {

      // Add our edge to the graph
      getGraphLayoutCache().insert(edge);

      // System.out.println("SOURCE: " +
      // edge.getSourceQueryNode().getName());
      // System.out.println("TARGET: " +
      // edge.getTargetQueryNode().getName());

      _edges.put(edge.getName(), edge);
    }

    addConstraints();
    refresh();
    _theOverview.refresh();

  }

  /**
   * Call this method when a new QueryNode or Edge is created to automatically add
   * whatever existing edges It has in the underlying sketch with other existing
   * QueryNodes.
   * 
   *
   * @author Federico Mora
   */
  public void autoAddExistingEdges() {
    Collection<SketchEdge> sketchEdges = _ourSketch.getEdges().values();
    HashMap<EntityNode, QueryNode> nodeMatches = getEntityNodePairs();

    for (SketchEdge se : sketchEdges) {
      if (nodeMatches.containsKey(se.getTargetEntity()) && nodeMatches.containsKey(se.getSourceEntity())
          && !_edges.containsKey(se.getName())) {

        View_Edge vEdge;
        // need to move down??
        if (se.isPartial()) {
          vEdge = new PartialViewEdge(nodeMatches.get(se.getSourceEntity()),
              nodeMatches.get(se.getTargetEntity()), se.getName());

        } else if (se.isInjective()) {
          // System.out.println("Edge is injective");
          // **NEED TO FIGURE OUT CASCADING
          vEdge = new InjectiveViewEdge(nodeMatches.get(se.getSourceEntity()),
              nodeMatches.get(se.getTargetEntity()), se.getName(), Cascade.RESTRICT);

        } else {
          vEdge = new NormalViewEdge(nodeMatches.get(se.getSourceEntity()),
              nodeMatches.get(se.getTargetEntity()), se.getName());
        }

        this.addEdge(vEdge);

      }
    }

  }

  /**
   * Adds constraints to the view frame is all elements of Sketch constraints are
   * being queried in view
   *
   * @author Federico Mora
   */
  @SuppressWarnings("unchecked")
  private void addConstraints() {
    ArrayList<ModelPath<ViewFrame, ViewGraphModel, View, QueryNode, View_Edge>> inPaths = new ArrayList<>();
    ArrayList<View_Edge> vEdges = new ArrayList<>();
    HashMap<EntityNode, QueryNode> nodeMatches = getEntityNodePairs();

    for (ModelConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> c : _ourSketch
        .getConstraints().values()) {
      ifblock: if (_constraints.containsKey(c.getID())) {
        // already represented so lets get out
      } else {
        // looking to see if we have all edges needed for each
        // constraint
        for (ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> skPath : c.getPaths()) {
          vEdges.clear();
          for (SketchEdge skEdge : skPath.getEdges()) {
            if (!_edges.containsKey(skEdge.getName())) {
              break ifblock;
            } else if (!nodeMatches.get(skEdge.getTargetEntity()).getWhere().isEmpty()
                || !nodeMatches.get(skEdge.getSourceEntity()).getWhere().isEmpty()) {
              // if the nodes being queried have where statements
              // then just exit
              break ifblock;
            } else {
              vEdges.add(_edges.get(skEdge.getName()));
            }
          }
          inPaths.add(new ModelPath<>(vEdges));

        }
        // add Constraint to View
        // if we survived to here it means elements are present to add
        // constraint
        ModelConstraint<ViewFrame, ViewGraphModel, View, QueryNode, View_Edge> newDiagram = null;
        if (c.getType().equals("commutativediagram")) {
          newDiagram = new CommutativeDiagram<>(inPaths, this, c.getID());
        } else if (c.getType().equals("sumconstraint")) {
          newDiagram = new SumConstraint<>(inPaths, this, c.getID());
        } else if (c.getType().equals("pullbackconstraint")) {
          try {
            newDiagram = new PullbackConstraint<>(inPaths, this, c.getID());
          } catch (ConstraintException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        } else if (c.getType().equals("equalizerconstraint")) {
          try {
            newDiagram = new EqualizerConstraint<>(inPaths, this, c.getID());
          } catch (ConstraintException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        } else if (c.getType().equals("productconstraint")) {
          try {
            newDiagram = new ProductConstraint<>(inPaths, this, c.getID());
          } catch (ConstraintException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        }

        this.addNewConstraint(newDiagram);
        this.setDirty();
      }
    }

  }

  /**
   * Returns a hash map of paired QueryNodes and the EntityNodes they represent
   *
   * @return HashMap<EntityNode, QueryNode>
   * @author Federico Mora
   */
  public HashMap<EntityNode, QueryNode> getEntityNodePairs() {
    HashMap<EntityNode, QueryNode> eNodes = new HashMap<>();
    for (QueryNode qn : _nodes.values()) {
      if (qn != null) {
        eNodes.put(qn.getQueriedEntity(), qn);
      }
    }
    return eNodes;
  }

  /**
   * Updates the view by taking away constraints if a node has been updated to
   * include a where statement. Or if it had a where statement and it was updated
   * to not having a where statement
   * 
   * if it had a where statement then it must not have one now, if it didn't then
   * now it must.
   * 
   * @author Federico Mora
   */
  public void updateConstraints(QueryNode ourNode, boolean hadWhere) {
    if (hadWhere) {
      this.addConstraints();
    } else {
      for (ModelConstraint<ViewFrame, ViewGraphModel, View, QueryNode, View_Edge> vc : ourNode.getConstraints()) {
        this.removeConstraint(vc);
      }
    }

  }

  @Override
  public boolean isSynced() {
    return _ourSketch.isSynced();
  }

  @Override
  public void setSynced(boolean state) {
    // is not used in views but need to guarantee that it is there
    // for generic puproses
  }

  @Override
  public Collection<ViewNode> getViews() {
    // we are a view so return null
    return null;
  }
}
