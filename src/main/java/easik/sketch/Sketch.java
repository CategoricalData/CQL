package easik.sketch;

//~--- non-JDK imports --------------------------------------------------------

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.undo.AbstractUndoableEdit;

import org.jgraph.graph.AttributeMap;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.GraphLayoutCache;

import easik.DocumentInfo;
import easik.Easik;
import easik.EasikTools;
import easik.database.Database;
import easik.database.DriverInfo;
import easik.database.api.jdbc.JDBCDriver;
import easik.model.Model;
import easik.model.ModelStateManager;
import easik.model.constraint.ModelConstraint;
import easik.model.edge.ModelEdge.Cascade;
import easik.model.keys.UniqueIndexable;
import easik.model.keys.UniqueKey;
import easik.model.path.ModelPath;
import easik.model.states.LoadingState;
import easik.model.util.graph.KosarajuSCC;
import easik.model.util.graph.ModelViewFactory;
import easik.overview.Overview;
import easik.overview.vertex.ViewNode;
import easik.sketch.edge.SketchEdge;
import easik.sketch.util.SketchFileIO;
import easik.sketch.util.graph.SketchGraphModel;
import easik.sketch.vertex.EntityNode;
import easik.ui.SketchFrame;
import easik.ui.datamanip.UpdateMonitor;
import easik.ui.menu.popup.EditSketchEdgeAction;
import easik.ui.menu.popup.RenameInSketchAction;
import easik.ui.menu.popup.ViewDataAction;
import easik.ui.tree.ModelInfoTreeUI;
import easik.view.vertex.QueryNode;

//~--- JDK imports ------------------------------------------------------------
/**
 * A Sketch represents an EA Sketch diagram used to represent a db, this object
 * also extends the JGraph swing component, allowing it to be added directly to
 * the application's GUI.
 *
 * When done with the current sketch, instead of creating a new one, the sketch
 * should simply be reinitialised, and it will become ready. Since a sketch is
 * also a Swing component it can get hairy creating a new one and changing all
 * the references. This might have become easier since the singletons were
 * introduced, so feel free to try to change that.
 *
 * @author Rob Fletcher 2005
 * @author Kevin Green 2006
 * @author Vera Ranieri 2006
 * @author Christian Fiddick 2012
 * @version 2014/7 Federico Mora
 */
public class Sketch extends Model<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> {
  /**
   *    
   */
  private static final long serialVersionUID = 8593634890916321234L;

  /**
   * The connection parameters stored with the sketch, reflecting the last
   * settings used with this sketch.
   */
  private HashMap<String, String> _connParams;

  /** The default cascade modes for new edges of this sketch */
  private Cascade _defaultCascade, _defaultPartialCascade;

  /**
   * Flag set if this sketch has been exported. If set, should lock editing
   */
  private boolean _syncLock;

  /**
   * An update monitor that can be used when performing operations on this sketch
   */
  @SuppressWarnings("unused")
  private UpdateMonitor _updateMonitor;

  /** Map of views on this sketch indexed by name */
  private HashMap<String, ViewNode> _views;

  /** The current connection to a server */
  private Database database; // DBCON CF2012

  /** True if there is a cycle in graph */
  private boolean _hasCycle = false;

  private String strongConnected = "";

  private String pPaths = "";

  /**
   * The default constructor sets all the visual settings for the JGraph, as well
   * as initialising the sketch to be empty. It also adds appropriate listeners
   * for all of the actions we are concerned with.
   *
   * @param inFrame    The application frame of the sketch
   * @param inOverview the overview
   */
  public Sketch(final SketchFrame inFrame, final Overview inOverview) {
    super(inFrame, inOverview);

    _stateManager = new ModelStateManager<>(this);
    _syncLock = false;

    // Set up mouse listener to watch for double clicks
    // - Double clicks edit component
    addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(final MouseEvent e) {
        // Ignore it if we're not in the basic editing state:
        /*
         * we don't want to ignore it any more since we want to allow the creation of
         * edges and nodes as we go in the constraint creation. For ease of use
         * 
         * if (!(Sketch.this.getStateManager().peekState() instanceof
         * BasicEditingState)) { return; }
         */

        final boolean doubleClick = ((e.getClickCount() > 1) && (e.getButton() == MouseEvent.BUTTON1));
        final Object[] selection = getSelectionCells();

        if ((_Frame.getMode() == SketchFrame.Mode.EDIT) && doubleClick && (selection.length > 0)) {
          if (selection[0] instanceof EntityNode) {
            // hack to get rename action
            final JMenuItem renameAction = new JMenuItem(new RenameInSketchAction(_Frame));

            renameAction.doClick();
          } else if (selection[0] instanceof SketchEdge) {
            // hack to get edit edge action
            final JMenuItem editEdgeAction = new JMenuItem(new EditSketchEdgeAction(_Frame));

            editEdgeAction.doClick();
          }
        } else if ((_Frame.getMode() == SketchFrame.Mode.MANIPULATE) && doubleClick
            && (selection.length == 1)) {
          if (selection[0] instanceof EntityNode) {
            // hack to get rename action
            final JMenuItem seeContentsAction = new JMenuItem(new ViewDataAction(_Frame.getMModel()));

            seeContentsAction.doClick();
          }
        }
      }

      @Override
      public void mousePressed(final MouseEvent e) {
        mouseClicked(e);
      }
    });

    final String cascade = Easik.getInstance().getSettings().getProperty("sql_cascade", "restrict");

    _defaultCascade = cascade.equals("cascade") ? Cascade.CASCADE : Cascade.RESTRICT;

    final String pCascade = Easik.getInstance().getSettings().getProperty("sql_cascade_partial", "set_null");

    _defaultPartialCascade = pCascade.equals("cascade") ? Cascade.CASCADE
        : pCascade.equals("restrict") ? Cascade.RESTRICT : Cascade.SET_NULL;

  }

  /**
   * When we initialise the sketch, we flush out all the data concerning the
   * sketch itself.
   *
   * This methods serves as a "new sketch" function.
   */
  @Override
  public void initialiseModel() {
    clearSelection();

    model = new SketchGraphModel(this);

    final GraphLayoutCache glc = new GraphLayoutCache(model,
        new ModelViewFactory<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge>());

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

    _views = new HashMap<>();
    _connParams = new HashMap<>();

    model.discardUndo();
  }

  /**
   * Used to initialise a new sketch based on provided data (usually from the
   * Sketch loading methods).
   *
   * @param syncLock    ?
   * @param entityNodes A map of all of the entities in the sketch
   * @param edges       A map containing all of the edges in the sketch
   * @param linkedList  A collection of the constraints of the sketch
   * @param head        The header created from the loaded XML file.
   * @param connParams  connection params
   */
  public String initializeFromData(final boolean syncLock, final Map<String, EntityNode> entityNodes,
      final Map<String, SketchEdge> edges,
      final LinkedList<ModelConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge>> linkedList,
      final DocumentInfo head, final Map<String, String> connParams) {
    initialiseModel();

    _syncLock = syncLock;

    _Frame.setMode(syncLock ? SketchFrame.Mode.MANIPULATE : SketchFrame.Mode.EDIT);

    _constraints = new HashMap<>();
    _docInfo = head;

    _Frame.setTreeName(_docInfo.getName());

    // Add the entities, edges, and constraints
    // want to gather warnings and then display them all together so
    // temporarily turn off warnings
    // Warnings should always be true for now since we haven't added the
    // option to disable them yet
    boolean tempWarnings = this.useWarnings();
    this.setWarnings(false);
    addEntity(entityNodes.values());
    String warning = addEdge(edges.values());
    this.setWarnings(tempWarnings);

    for (final ModelConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> c : linkedList) {
      addConstraint(c);
    }

    setConnectionParams(connParams);
    refresh();

    model.discardUndo();

    // if there is something to warn about and we had previously set
    // warnigns to true
    if (warning != null && tempWarnings) {
      return warning;
    }
    return null;

  }

  /**
   * Accessor for the set of views on this sketch.
   * 
   * @return Collection of the views on this sketch
   */
  @Override
  public Collection<ViewNode> getViews() {
    return Collections.unmodifiableCollection(_views.values());
  }

  /**
   * Removes a view with a given name from our set of views.
   * 
   * @param name The name of the view.
   */
  public void removeView(final String name) {
    _views.remove(name);
  }

  /**
   * Adds a view to our set of views
   * 
   * @param view The view to add to our set
   */
  public void addView(final ViewNode view) {
    _views.put(view.getName(), view);
  }

  /**
   * Called internally by SketchEdge when an edge is renamed, to keep the edge map
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
  public String edgeRenamed(final SketchEdge edge, final String oldName, String newName) {
    // If the name already exists, we have to rename it
    while (_edges.containsKey(newName)) {
      newName = EasikTools.incrementName(newName);
    }

    _edges.put(newName, _edges.remove(oldName));

    return newName;
  }

  /**
   * Requests that an XML file be loaded into the sketch.
   *
   * @param inputFile The file from which the data will be drawn.
   * @return The success of the load
   */
  public boolean loadFromXML(final File inputFile) {
    if (!SketchFileIO.graphicalSketchFromXML(inputFile, this)) {
      _theOverview.removeSketch(_Frame.getNode());

      return false;
    }
    return true;

  }

  /**
   * Removes an entity, and also cascades to remove all the arrows involved with
   * it.
   *
   * @param toRemove The entity about to be removed
   */
  @Override
  public void removeNode(final EntityNode toRemove) {
    // So we don't get a ConcurrentModificationException
    final ArrayList<SketchEdge> removeEdges = new ArrayList<>();

    for (final SketchEdge edge : _edges.values()) {
      if (edge.getSourceEntity().equals(toRemove) || edge.getTargetEntity().equals(toRemove)) {
        // add the edge to a list of edges to remove
        removeEdges.add(edge);
      }
    }

    model.beginUpdate();

    // Remove the edges
    for (final SketchEdge e : removeEdges) {
      removeEdge(e);
    }

    _nodes.remove(toRemove.toString());
    getGraphLayoutCache().remove(new Object[] { toRemove });

    model.postEdit(new AbstractUndoableEdit() {
      /**
       *            
       */
      private static final long serialVersionUID = 4458021761030500709L;

      @Override
      public void undo() {
        super.undo();
        _nodes.put(toRemove.toString(), toRemove);
      }

      @Override
      public void redo() {
        super.redo();
        _nodes.remove(toRemove.toString());
      }
    });

    // Remove Entity from tree
    _Frame.getInfoTreeUI().removeNode(toRemove);
    model.endUpdate();
  }

  /**
   * Removes an edge, also cascades to remove all constraints using it.
   *
   * @param toRemove The edge about to be removed
   */
  public void removeEdge(final SketchEdge toRemove) {
    model.beginUpdate();
    // Check for constraints that need these edges
    for (final ModelConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> c : _constraints
        .values()) {
      if (c.hasEdge(toRemove)) {
        removeConstraint(c);
      }
    }

    if (toRemove instanceof UniqueIndexable) {
      final EntityNode source = toRemove.getSourceEntity();
      boolean needCleanup = false;

      for (final UniqueKey<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> q : source
          .getUniqueKeys()) {
        if (q.removeElement((UniqueIndexable) toRemove)) {
          needCleanup = true;
        }
      }

      if (needCleanup) {
        source.cleanup();
      }

      // FIXME: undo: unique key cleanup stuff
    }

    _edges.remove(toRemove.getName());
    toRemove.getSourceEntity().removeDepend(toRemove.getTargetEntity());

    model.postEdit(new AbstractUndoableEdit() {
      /**
       *            
       */
      private static final long serialVersionUID = 711022413236293938L;

      @Override
      public void undo() {
        super.undo();
        _edges.put(toRemove.getName(), toRemove);
        toRemove.getSourceEntity().addDepend(toRemove.getTargetEntity());
      }

      @Override
      public void redo() {
        super.redo();
        _edges.remove(toRemove.getName());
        toRemove.getSourceEntity().removeDepend(toRemove.getTargetEntity());
      }
    });
    getGraphLayoutCache().remove(new Object[] { toRemove });
    model.endUpdate();
    KosarajuSCC s = new KosarajuSCC(this);
    strongConnected = s.getSCC();

  }

  /**
   * Saves the existing sketch as an XML file.
   *
   * @param outputFile The file to be written to
   */
  public void saveToXML(final File outputFile) {
    SketchFileIO.sketchToXML(outputFile, this);
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
    final EntityNode newEntity;

    newEntity = new EntityNode(name, (int) x, (int) y, this);

    addEntity(newEntity);
  }

  /**
   * Add one or more entities, or an array of entities, to the graph, dealing with
   * all of the dependencies.
   *
   * @param theEntities one or more EntityNodes (or an array of entitynodes) to be
   *                    added
   */
  public void addEntity(final EntityNode... theEntities) {
    addEntity(Arrays.asList(theEntities));
  }

  /**
   * Adds a collection (set, list, etc.) of EntityNodes to the graph.
   *
   * @param theEntities the collection of entities to be added.
   */
  public void addEntity(final Collection<EntityNode> theEntities) {
    // Push loading state
    _stateManager.pushState(new LoadingState<>(this));

    final GraphLayoutCache glc = getGraphLayoutCache();

    model.beginUpdate();

    for (final EntityNode node : theEntities) {
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
      _nodes.put(node.getName(), node);

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

        for (final EntityNode node : theEntities) {
          _nodes.remove(node.getName());
        }
      }

      @Override
      public void redo() {
        super.redo();

        for (final EntityNode node : theEntities) {
          _nodes.put(node.getName(), node);
        }
      }
    });
    model.endUpdate();

    // Reload the graph to respect the new changes
    glc.reload();

    // Pop state
    _stateManager.popState();
  }

  /**
   * Adds one or more edges (or an array of edges) to the sketch.
   *
   * @param inEdges one or more SketchEdges (or an array of sketchedges) to be
   *                added
   */
  public String addEdge(final SketchEdge... inEdges) {
    String warning = addEdge(Arrays.asList(inEdges));
    for (ViewNode vn : _Frame.getMModel().getViews()) {
      vn.getMModel().autoAddExistingEdges();
    }
    return warning;
  }

  /**
   * Adds a collection (set, list, etc.) of edges to the sketch.
   *
   * @param inEdges Collection of SketchEdges to add
   */
  public String addEdge(final Collection<SketchEdge> inEdges) {
    // Push loading state
    _stateManager.pushState(new LoadingState<>(this));

    final GraphLayoutCache glc = getGraphLayoutCache();

    model.beginUpdate();

    for (final SketchEdge edge : inEdges) {
      if (_edges.containsKey(edge.getName())) {
        edge.setName(edge.getName());
      }
      // Add our entity to the graph
      glc.insert(edge);
      _edges.put(edge.getName(), edge);
      edge.getSourceEntity().addDepend(edge.getTargetEntity());
    }

    String warning = "";
    String prevCycle = strongConnected;
    KosarajuSCC s = new KosarajuSCC(this);
    strongConnected = s.getSCC();

    // if there is a cycle, we want to use warnigns, and we have not warned
    // about this cycle previously
    if (setHasCycle(!strongConnected.isEmpty()) && !prevCycle.equals(strongConnected)) {
      warning += this.getName() + " contains a strongly connected component" + "\n        " + strongConnected
          + "\n";
    }

    // if there is a mismatched path pair in the sketch
    String temp = pPaths;
    pPaths = this.multPathWarning();
    if (!pPaths.isEmpty() && !temp.equals(pPaths)) {
      warning += pPaths;
    }

    if (!warning.isEmpty() && useWarnings()) {
      JOptionPane.showMessageDialog(this, warning, "Warning", JOptionPane.ERROR_MESSAGE);
    } else if (!useWarnings()) {
      return warning;
    }

    model.postEdit(new AbstractUndoableEdit() {
      /**
       *            
       */
      private static final long serialVersionUID = -6523342649950083978L;

      @Override
      public void undo() {
        super.undo();

        for (final SketchEdge edge : inEdges) {
          _edges.remove(edge.getName());
        }
      }

      @Override
      public void redo() {
        super.redo();

        for (final SketchEdge edge : inEdges) {
          _edges.put(edge.getName(), edge);
          edge.getSourceEntity().addDepend(edge.getTargetEntity());
        }
      }
    });
    model.endUpdate();

    // Pop state
    _stateManager.popState();
    refresh();
    _theOverview.refresh();

    return null;
  }

  /**
   * Method that checks if there are paths with the same domain and codomain and
   * then sees if they mismatch. We only care about when one of the paths is all
   * cascade and the other path contains an edge which is not cascade. In this
   * case we will warn the user.
   * 
   * @return String warning
   * @author Federico Mora
   */
  private String multPathWarning() {
    boolean cascade = false;
    boolean other = false;
    String warning = "";
    // want to look at paths from every node to every other node
    for (EntityNode s : _nodes.values()) {
      for (EntityNode t : _nodes.values()) {
        if (s != t) {
          cascade = false;
          other = false;

          // System.out.println("Finding paths from " + s.getName() +
          // " to " + t.getName());
          ArrayList<SketchEdge> startEdges = new ArrayList<>();
          ArrayList<SketchEdge> endEdges = new ArrayList<>();

          for (SketchEdge edge : _edges.values()) {
            if (edge.getSourceEntity() == s) {
              startEdges.add(edge);
            }
            if (edge.getTargetEntity() == t) {
              endEdges.add(edge);
            }
          }
          // if the start node doesn't break off into multiple paths
          // then we don't care
          // if the end node doesn't join multiple paths then we don't
          // care
          if (startEdges.size() < 2 || endEdges.size() < 2) {
            // System.out.println("skipping " + s.getName() + " to "
            // + t.getName());
            continue;
          }

          ArrayList<ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge>> paths = new ArrayList<>();
          ArrayList<LinkedList<SketchEdge>> temp = buildpath(s, t);

          for (LinkedList<SketchEdge> q : temp) {
            if (q.peekLast().getTargetEntity() != t) {
              q.clear();
            } else {
              paths.add(new ModelPath<>(q));
            }
          }

          if (paths.size() > 1) {
            for (ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> p : paths) {
              if (p.isCompositeCascade()) {
                cascade = true;
              } else {
                other = true;
              }
            }
            if (cascade && other) {
              warning += this.getName() + " contains multiple paths from " + s.getName() + " to "
                  + t.getName() + "\n";
              for (ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> p : paths) {
                if (p.isCompositeCascade()) {
                  warning += "        " + p.toString() + " is Aggregate Cascade\n";
                } else {
                  warning += "        " + p.toString() + " is Aggregate Other\n";
                }

              }
            }
          }

        }
      }
    }
    return warning;
  }

  /**
   * Recursive method that does most of the work for multPathWarning
   * 
   * @return ArrayList used to create path
   * @author Federico Mora
   */
  @SuppressWarnings("unchecked")
  private static ArrayList<LinkedList<SketchEdge>> buildpath(EntityNode start, EntityNode end) {
    ArrayList<SketchEdge> visited = new ArrayList<>();
    ArrayList<LinkedList<SketchEdge>> paths = new ArrayList<>();

    // BFS uses LinkedList data structure
    LinkedList<EntityNode> LinkedList = new LinkedList<>();
    LinkedList.add(start);
    // path.add(start);
    // visited.add(start);
    while (!LinkedList.isEmpty()) {
      EntityNode node = LinkedList.remove();
      for (SketchEdge ed : node.getOutgoingEdges()) {
        if (!visited.contains(ed)) {
          visited.add(ed);

          // find a LinkedList to add the edge to
          boolean queueExists = false;
          LinkedList<SketchEdge> newPath = null;
          for (LinkedList<SketchEdge> q : paths) {
            if (q.peekLast().getTargetEntity() == ed.getSourceEntity()) {
              newPath = (java.util.LinkedList<SketchEdge>) q.clone();
              newPath.add(ed);
              queueExists = true;
              continue;
            }
          }

          // else make a new LinkedList
          if (!queueExists) {
            LinkedList<SketchEdge> p = new LinkedList<>();
            p.add(ed);
            paths.add(p);
          } else {
            paths.add(newPath);
          }

          if (ed.getTargetEntity() == end) {
            // return paths;
          } else {
            LinkedList.add(ed.getTargetEntity());
          }
        }
      }
    }
    return paths;
  }

  /**
   * Checks to see if a name is in use, so that we will not have several instances
   * at once.
   *
   * @param inName The desired new name to check against
   * @return Is it used or not.
   */
  @Override
  public boolean isNameUsed(final String inName) {
    final Set<String> keys = _nodes.keySet();

    for (final String name : keys) {
      if (name.equalsIgnoreCase(inName)) {
        return true;
      }
    }

    for (final ViewNode vn : _views.values()) {
      for (final QueryNode qn : vn.getMModel().getEntities()) {
        if (qn.getName().equalsIgnoreCase(inName)) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Returns the first available unique edge identifier
   * 
   * @param isInjective true if the edge is injective, false otherwise
   * @return The next id
   */
  public String getNewEdgeName(final boolean isInjective) {
    int currentID = 0;
    boolean foundOne = false;
    final String prefix = isInjective ? "isA_" : "f";

    while (!foundOne) {
      currentID++;

      if (!isEdgeNameUsed(prefix + currentID)) {
        foundOne = true;
      }
    }

    return prefix + currentID;
  }

  /**
   * Checks to see if a name is in use, so that we will not have several instances
   * at once. For edges.
   *
   * @param inName The desired new edge name to check against
   * @return Is it used or not.
   */
  public boolean isEdgeNameUsed(final String inName) {
    final Set<String> edges = _edges.keySet();

    for (final String e : edges) {
      if (e.equalsIgnoreCase(inName)) {
        return true;
      }
    }

    return false;
  }

  // There used to be a more database functionality in here, not any more
  // See database.Database

  /**
   * Editable check wrapper.
   * 
   * @see easik.database.base.PersistenceDriver#editable(easik.sketch.vertex.EntityNode)
   *
   * @param entity
   *
   * @return
   */
  public boolean editable(final EntityNode entity) {
    if (getDatabase().hasActiveDriver()) {
      JDBCDriver d = getDatabase().getJDBCDriver();

      if (d != null) {
        return d.editable(entity);
      }
    }

    return false;
  }

  /**
   * Check if a database object exists for the Sketch.
   * 
   * @return True if database is not null
   */
  public boolean hasDatabase() {
    return database != null;
  }

  /**
   * Initialize a simple database that can be used to establish outgoing
   * connections.
   */
  public void setDatabase() {
    database = new Database(this);
  }

  /**
   * Database getter that will call setDatabase if necessary.
   * 
   * @return A database object (never null because this creates it)
   */
  public Database getDatabase() {
    if (!hasDatabase()) {
      setDatabase();
    }

    return database;
  }

  /**
   * Let the user choose from the appropriate set of installed server exporting
   * database drivers.
   * 
   * @return Chosen database driver
   */
  public String getDatabaseType() {
    return getDBType(DriverInfo.availableDatabaseDrivers(), "sql_driver");
  }

  /**
   * Let the user choose from the appropriate set of installed file exporting
   * database drivers.
   * 
   * @return Chosen file driver
   */
  public String getFileDatabaseType() {
    return getDBType(DriverInfo.availableFileDrivers(), "sql_driver");
  }

  /**
   * Pop up a dialog box for the user to choose database drivers from.
   * 
   * @param activeTypes     The database drivers to choose from
   * @param defaultProperty Name of default property to fall back to
   * @return The selected database driver type (MySQL, PostgreSQL, etc.)
   */
  public String getDBType(final String[] activeTypes, String defaultProperty) {
    int defaultIndex = 0;
    String defType = _connParams.get("driver");

    if (defType == null) {
      defType = Easik.getInstance().getSettings().getProperty(defaultProperty);
    }

    if (defType != null) {
      for (int i = 0; i < activeTypes.length; i++) {
        if (defType.equals(activeTypes[i])) {
          defaultIndex = i;

          break;
        }
      }
    }

    return (String) JOptionPane.showInputDialog(getFrame(), "Please select the database type to use",
        "Choose database type", JOptionPane.INFORMATION_MESSAGE, null, activeTypes, activeTypes[defaultIndex]);
  }

  /**
   * Stores the map of db connection options reflecting the last connection used
   * for this sketch. We don't allow "password" to be stored.
   *
   * @param params Map of connection parameters to store
   * @return true if the connection parameters are changed.
   * @see #getConnectionParams()
   */
  public boolean setConnectionParams(final Map<String, String> params) {
    boolean changed = false;

    for (final String k : params.keySet()) {
      if (setConnectionParam(k, params.get(k))) {
        changed = true;
      }
    }

    return changed;
  }

  /**
   * Clears the current map of db connection options.
   */
  public void clearConnectionParams() {
    _connParams.clear();
  }

  /**
   * Sets a single connection parameter in the current db connection options
   * hashmap. Returns true if the value is new (that is, it didn't exist, or it
   * was set to something else).
   *
   * @param key   the connection parameter name
   * @param value the connection parameter value
   * @return true if the connection params are now different as a result of this
   *         change
   * @see #setConnectionParams(java.util.Map)
   */
  public boolean setConnectionParam(final String key, final String value) {
    if (!key.equals("password")) {
      final String old = _connParams.get(key);

      _connParams.put(key, value);

      if ((old == null) || !old.equals(value)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Returns a map of saved db connection options reflecting the last connection
   * used for this sketch (saved by setConnectionParams()).
   *
   * @return Map of String, String pairs
   * @see #setConnectionParams(java.util.Map)
   */
  public Map<String, String> getConnectionParams() {
    return Collections.unmodifiableMap(_connParams);
  }

  /**
   * Sets the default cascade mode for new non-partial edges of this sketch.
   *
   * @param newDefault the new Cascade mode, e.g. <code>Cascade.RESTRICT</code>.
   *                   <code>Cascade.SET_NULL</code> is not permitted: if passed,
   *                   it will be ignored.
   */
  public void setDefaultCascading(final Cascade newDefault) {
    if (newDefault == Cascade.SET_NULL) {
      return; // Impossible choice.
    }

    _defaultCascade = newDefault;
  }

  /**
   * Returns the current default cascade mode for new non-partial edges of the
   * sketch.
   *
   * @return the Cascade value for new non-partial edges
   */
  public Cascade getDefaultCascading() {
    return _defaultCascade;
  }

  /**
   * Sets the default cascade mode for new partial edges of this sketch.
   *
   * @param newDefault the new Cascade mode, e.g. <code>Cascade.SET_NULL</code>.
   */
  public void setDefaultPartialCascading(final Cascade newDefault) {
    _defaultPartialCascade = newDefault;
  }

  /**
   * Returns the current default cascade mode for new non-partial edges of the
   * sketch.
   *
   * @return the Cascade value for new partial edges
   */
  public Cascade getDefaultPartialCascading() {
    return _defaultPartialCascade;
  }

  /**
   * Sets our sync flag flag. This indicates whether or not we are synced with a
   * db and wish to lock editing.
   * 
   * @param state The state of our exported flag.
   */
  @Override
  public void setSynced(final boolean state) {
    if (!state) {
      database.cleanDatabaseDriver();
    }

    _syncLock = state;
  }

  /**
   * Returns the state of our sync flag. Any time a command would be issued to
   * edit this sketch, a call should be made to this method. If it returns true,
   * editing cannot occur safely.
   * 
   * @return A flag indicating if this sketch has been synced with a db. If true,
   *         any changes to the sketch may destroy the link to the db.
   */
  @Override
  public boolean isSynced() {
    return _syncLock;
  }

  public boolean getHasCycle() {
    return _hasCycle;
  }

  public boolean setHasCycle(boolean _hasCycle) {
    this._hasCycle = _hasCycle;
    return _hasCycle;
  }
}

// DBCON CF2012 EDIT