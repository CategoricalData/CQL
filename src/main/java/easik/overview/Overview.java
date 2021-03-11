package easik.overview;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.jgraph.JGraph;
import org.jgraph.event.GraphSelectionEvent;
import org.jgraph.event.GraphSelectionListener;
import org.jgraph.graph.AttributeMap;
import org.jgraph.graph.DefaultCellViewFactory;
import org.jgraph.graph.DefaultGraphModel;
import org.jgraph.graph.GraphCell;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.GraphLayoutCache;

import easik.DocumentInfo;
import easik.Easik;
import easik.EasikTools;
import easik.overview.edge.ViewDefinitionEdge;
import easik.overview.util.OverviewFileIO;
import easik.overview.util.graph.OverviewGraphModel;
import easik.overview.vertex.OverviewVertex;
import easik.overview.vertex.SketchNode;
import easik.overview.vertex.ViewNode;
import easik.sketch.Sketch;
import easik.ui.ApplicationFrame;
import easik.ui.GraphUI;
import easik.ui.SketchFrame;
import easik.ui.ViewFrame;
import easik.ui.tree.OverviewInfoTreeUI;
import easik.view.View;

/**
 * An Overview represents a collection of EA Sketch diagrams used to represent
 * databases. This Object also extends the JGraph swing component, allowing it
 * to be added directly to our application's GUI.
 *
 * As of now, we have no way of represecting relationships between sketches.
 */
public class Overview extends JGraph {
  /**
   *    
   */
  private static final long serialVersionUID = 8958091082779615522L;

  /** The current file, initialized to <b>null</b> */
  private File _currentFile = null;

  /**
   * Records whether the sketch has been modified since the last save. Initialized
   * to <b>false</b>
   */
  private boolean _dirty = false;

  /** The current ApplicationFrame */
  private ApplicationFrame _appFrame;

  /** The current DocumentInfo */
  private DocumentInfo _docInfo;

  /** A hash map of all sketch nodes, indexed by their name */
  private HashMap<String, SketchNode> _sketchNodes;

  /** A hash map of all view edges, indexed by their label */
  private HashMap<String, ViewDefinitionEdge> _viewEdges;

  /** A hash map of all view nodes, indexed by their name */
  private HashMap<String, ViewNode> _viewNodes;

  /**
   * The default constructor sets all the visual settings for the JGraph, as well
   * as initialising the sketch to be empty. It also adds appropriate listeners
   * for all of the actions we are concerned with.
   *
   * @param inFrame The application frame of the sketch
   */
  public Overview(ApplicationFrame inFrame) {
    super();

    setBackground(Easik.getInstance().getSettings().getColor("overview_canvas_background"));

    _appFrame = inFrame;

    setAntiAliased(true);
    setDisconnectable(false);
    setConnectable(false);
    setEditable(false);
    setSizeable(false);
    getGraphLayoutCache().setAutoSizeOnValueChange(true);
    addGraphSelectionListener(new GraphSelectionListener() {
      @Override
      public void valueChanged(GraphSelectionEvent e) {
        Overview.this.getGraphLayoutCache().reload();
      }
    });

    // Set up mouse listener to watch for double clicks
    // - Double clicks make a sketch visible
    this.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        boolean doubleClick = ((e.getClickCount() > 1) && (e.getButton() == MouseEvent.BUTTON1));

        if (doubleClick) {
          Object[] currSelection = Overview.this.getSelectionCells();

          // double click on sketch, so make visible
          if (currSelection.length == 1) {
            if (currSelection[0] instanceof SketchNode) {
              SketchFrame f = ((SketchNode) currSelection[0]).getFrame();
              if (f.getMModel().isSynced()) {
                f.enableDataManip(true);
              } else {
                f.enableSketchEdit();
              }
            } else if (currSelection[0] instanceof ViewNode) {
              // enableDataManip so that you can't just open the
              // view without
              // verifying connection
              SketchFrame f = ((ViewNode) currSelection[0]).getMModel().getSketch().getFrame();
              if (f.getMModel().isSynced()) {
                f.enableDataManip(false);
              }
              ((ViewNode) currSelection[0]).getFrame().setVisible(true);
            }
          }
        }
      }

      @Override
      public void mousePressed(MouseEvent e) {
        mouseClicked(e);
      }
    });
    initializeOverview();
    updateUI();
  }

  /**
   * Refreshes the specified nodes/edges in the overview GUI. If called with no
   * arguments, all items are refreshed.
   *
   * @param cells
   */
  public void refresh(GraphCell... cells) {
    setBackground(Easik.getInstance().getSettings().getColor("overview_canvas_background"));

    Object[] toRefresh = (cells.length > 0) ? cells : getRoots();

    ((DefaultGraphModel) getModel()).cellsChanged(toRefresh);
    _appFrame.updateTitle();

    if (cells.length == 0) {
      super.refresh();
    }
  }

  /**
   * Overridden refresh method to force parameterless calls to go through
   * refresh(GraphCell...cells)
   */
  @Override
  public void refresh() {
    this.refresh(new GraphCell[0]);
  }

  /**
   * Refreshes the overview GUI as well as the GUIs of all Sketches and Views.
   */
  public void refreshAll() {
    for (SketchNode node : _sketchNodes.values()) {
      Sketch s = node.getFrame().getMModel();

      s.refresh();
      s.updateThumb();
    }

    for (ViewNode node : _viewNodes.values()) {
      View v = node.getMModel();

      v.refresh();
      v.updateThumb();
    }

    refresh();
  }

  /**
   * Gets the outgoing edge from the node representing the given view.
   * 
   * @param view The view
   * @return The edge leaving the given view.
   */
  public ViewDefinitionEdge getViewEdge(View view) {
    ViewNode vn = view.getFrame().getNode();

    for (ViewDefinitionEdge currEdge : _viewEdges.values()) {
      if (currEdge.getSourceNode() == vn) {
        return currEdge;
      }
    }

    return null;
  }

  /**
   * Gets a view edge with a given label.
   * 
   * @param inLabel The label of the edge we want.
   * @return The edge who's label matches inLabel.
   */
  public ViewDefinitionEdge getViewEdge(String inLabel) {
    return _viewEdges.get(inLabel);
  }

  /**
   * Accessor for the sketches in the overview
   * 
   * @return Collection of the sketches
   */
  public Collection<SketchNode> getSketches() {
    return Collections.unmodifiableCollection(_sketchNodes.values());
  }

  /**
   * Accessor for the views in the overview
   * 
   * @return Collection of the views
   */
  public Collection<ViewNode> getViews() {
    return Collections.unmodifiableCollection(_viewNodes.values());
  }

  /**
   * Accessor for the view edges in the overview
   * 
   * @return Collection of view edges
   */
  public Collection<ViewDefinitionEdge> getViewEdges() {
    return Collections.unmodifiableCollection(_viewEdges.values());
  }

  /**
   * Removes a sketch from both the tree representation, and the graphical.
   *
   * @param toRemove The entity about to be removed
   */
  public void removeSketch(SketchNode toRemove) {
    ArrayList<ViewNode> removeViews = new ArrayList<>(toRemove.getFrame().getMModel().getViews());

    for (ViewNode v : removeViews) {
      removeView(v);
    }

    // dispose of the sketch's frame
    toRemove.getFrame().dispose();
    _sketchNodes.remove(toRemove.toString());
    getGraphLayoutCache().remove(new Object[] { toRemove });
    _appFrame.getInfoTreeUI().removeSketch(toRemove);
  }

  /**
   * Removes a view edge from the overview. This does not automatically delete the
   * source view from the graph.
   * 
   * @param toRemove The edge to remove.
   */
  private void removeViewEdge(ViewDefinitionEdge toRemove) {
    _viewEdges.remove(toRemove.getName());
    getGraphLayoutCache().remove(new Object[] { toRemove });
  }

  /**
   * Remove view and it's view edge from the overview.
   *
   * @param remove
   */
  public void removeView(ViewNode remove) {
    // remove from sketch
    View toRemove = remove.getFrame().getMModel();

    toRemove.getSketch().removeView(toRemove.getName());

    for (ViewDefinitionEdge edge : _viewEdges.values()) {
      if (edge.getSourceNode().getName().equals(toRemove.getName())) {
        removeViewEdge(edge);

        break;
      }
    }

    // remove from overview graph
    _viewNodes.remove(toRemove.getName());
    getGraphLayoutCache().remove(new Object[] { remove });

    // remove from info tree
    _appFrame.getInfoTreeUI().removeView(toRemove.getFrame().getNode());

    // dispose of the frame
    remove.getFrame().dispose();
  }

  /**
   * Get the working file for this sketch.
   * 
   * @return The file last saved using this sketch
   */
  public File getFile() {
    return _currentFile;
  }

  /**
   * This assigns a file to the current overview.
   *
   * @param inFile File to be assigned.
   */
  public void setFile(File inFile) {
    _currentFile = inFile;
  }

  /**
   * Since this is a Swing component, this method is overloading a method of
   * JGraph to adjust the look and feel. The feel we are changing is ignoring all
   * but left clicks, allowing for right-click functionality not affecting the
   * selections.
   */
  @Override
  public void updateUI() {
    this.setUI(new GraphUI());
    this.invalidate();
  }

  /**
   * Determines whether the sketch has been modified since the last save.
   *
   * @return The dirtiness, true means dirty.
   */
  public boolean getDirty() {
    return _dirty;
  }

  /**
   * Used to mark a sketch as dirty or not. Since it's only marked as non-dirty
   * when saving, we mark all the current node/view positions if setting
   * non-dirty.
   *
   * @param inDirty NEw dirtiness.
   */
  public void setDirty(boolean inDirty) {
    _dirty = inDirty;

    if (_dirty) {
      getDocInfo().updateModificationDate();
    }

    if (!_dirty) {
      for (SketchNode n : _sketchNodes.values()) {
        n.savePosition();
      }

      for (ViewNode v : _viewNodes.values()) {
        v.savePosition();
      }
    }

    _appFrame.setDirty(_dirty);
  }

  /**
   * Checks to see if any of the sketch nodes or view nodes have moved and, if so,
   * updates them and sets the overview to dirty. If the overview is already
   * dirty, we don't have to do anything at all.
   */
  public void checkDirty() {
    if (_dirty) {
      return;
    }

    for (SketchNode n : _sketchNodes.values()) {
      Rectangle2D newBounds = GraphConstants.getBounds(n.getAttributes());

      if ((int) newBounds.getX() != n.getLastKnownX()) {
        setDirty(true);

        return;
      }
    }

    for (ViewNode v : _viewNodes.values()) {
      Rectangle2D newBounds = GraphConstants.getBounds(v.getAttributes());

      if ((int) newBounds.getX() != v.getLastKnownX()) {
        setDirty(true);

        return;
      }
    }
  }

  /**
   * Returns the parental application frame to whoever asks for it.
   * 
   * @return The current application frame
   */
  public ApplicationFrame getFrame() {
    return _appFrame;
  }

  /**
   * Gets the document information
   * 
   * @return The document information
   */
  public DocumentInfo getDocInfo() {
    return _docInfo;
  }

  /**
   * When we initialise the overview, we flush out all the data concerning the
   * sketch itself.
   *
   * This methods serves as a "new overview" function.
   */
  public void initializeOverview() {
    clearSelection();

    if (_sketchNodes != null) {
      for (SketchNode node : _sketchNodes.values()) {
        node.getFrame().dispose();
      }
    }

    if (_viewNodes != null) {
      for (ViewNode node : _viewNodes.values()) {
        node.getFrame().dispose();
      }
    }

    setFile(null);

    _sketchNodes = new HashMap<>();
    _viewNodes = new HashMap<>();
    _viewEdges = new HashMap<>();
    _docInfo = new DocumentInfo(_appFrame);

    if (_appFrame.getInfoTreeUI() != null) {
      _appFrame.setInfoTreeUI(new OverviewInfoTreeUI(_appFrame));
      _appFrame.getInfoTreeUI().refreshTree();
    }

    OverviewGraphModel model = new OverviewGraphModel(this);
    GraphLayoutCache glc = new GraphLayoutCache(model, new DefaultCellViewFactory());

    setModel(model);
    setGraphLayoutCache(glc);
  }

  /**
   * Used to initialise a new sketch based on provided data (usually from the
   * Sketch loading methods).
   *
   * @param sketchNodes         A Map of all of the sketches in the overview
   * @param viewNodes
   * @param viewDefinitionEdges
   * @param docInfo             The document information to be stored along with
   *                            this overview
   */
  public void initializeFromData(Map<String, SketchNode> sketchNodes, Map<String, ViewNode> viewNodes,
      Map<String, ViewDefinitionEdge> viewDefinitionEdges, DocumentInfo docInfo) {
    initializeOverview();

    _sketchNodes = new HashMap<>();
    _viewNodes = new HashMap<>();
    _viewEdges = new HashMap<>();
    _docInfo = docInfo;

    for (SketchNode node : sketchNodes.values()) {
      if (node != null) {
        addVertex(node);
      }
    }

    for (ViewNode node : viewNodes.values()) {
      if (node != null) {
        addVertex(node);
      }
    }

    for (ViewDefinitionEdge edge : viewDefinitionEdges.values()) {
      if (edge != null) {
        addViewEdge(edge);
      }
    }

    refresh();
  }

  /**
   * Called internally by SketchNode when a sketch is renamed, to keep the sketch
   * node map consistent. Should not be called directly; instead call
   * overview.getSketch("currentname").setName("newname").
   *
   * @see easik.overview.SketchNode.setName(String)
   * @param node    the SketchNode being renamed
   * @param oldName the old name of the node
   * @param newName the candidate new name
   * @return a string containing the final new node name, for EntityNode to use.
   */
  public String sketchRenamed(SketchNode node, String oldName, String newName) {
    // If the name already exists, we have to rename it
    while (_sketchNodes.containsKey(newName)) {
      newName = EasikTools.incrementName(newName);
    }

    if (_sketchNodes.containsKey(oldName)) {
      _sketchNodes.put(newName, _sketchNodes.remove(oldName));
    }

    return newName;
  }

  /**
   * Called internally by ViewNode when a view is renamed, to keep the view node
   * map consistent. Should not be called directly; instead call
   * overview.getView("currentname").setName("newname").
   *
   * @see easik.overview.vertex.ViewNode.setName(String)
   * @param node    the ViewNode being renamed
   * @param oldName the old name of the node
   * @param newName the candidate new name
   * @return a string containing the final new node name, for ViewNode to use.
   */
  public String viewRenamed(ViewNode node, String oldName, String newName) {
    // If the name already exists, we have to rename it
    while (_viewNodes.containsKey(newName)) {
      newName = EasikTools.incrementName(newName);
    }

    _viewNodes.put(newName, _viewNodes.remove(oldName));

    return newName;
  }

  /**
   * Called internally by ViewDefinitionEdge when an edge is renamed, to keep the
   * edge map consistent. Should not be called directly; instead just call
   * edge.setName("newname").
   *
   * @see easik.sketch.edge.SketchEdge.setName(String)
   * @param edge    the edge being renamed
   * @param oldName the old name of the edge
   * @param newName the candidate new name
   * @return a string containing the final new edge name, for SketchEdge to use.
   */
  public String viewEdgeRenamed(ViewDefinitionEdge edge, String oldName, String newName) {
    // If the name already exists, we have to rename it
    while (_viewEdges.containsKey(newName)) {
      newName = EasikTools.incrementName(newName);
    }

    _viewEdges.put(newName, _viewEdges.remove(oldName));

    return newName;
  }

  // TODO: For loading and saving, have it return its success or an error
  // message

  /**
   * Saves the overview as an XML file.
   *
   * @param outputFile The file to be written to
   */
  public void saveToXML(File outputFile) {
    OverviewFileIO.overviewToXML(outputFile, this);
  }

  /**
   * Requests that an XML file be loaded into the overview. Note that this only
   * loads the data, it doesn't do the other operations required for properly
   * loading an overview XML file.
   *
   * @param inputFile The file from which the data will be drawn.
   * @see openOverview( java.io.File) for complete file loading
   */
  public void loadFromXML(File inputFile) {
    if (!OverviewFileIO.graphicalOverviewFromXML(inputFile, this)) {
      System.err.println("Error loading overview from XML...");
    }
  }

  /**
   * Loads an XML overview file into the overview, sets up the window, marks the
   * window clean, and adds the file to the recent files list.
   *
   * @param inputFile The file from which the data will be drawn.
   */
  public void openOverview(File inputFile) {
    loadFromXML(inputFile);
    setFile(inputFile);
    setDirty(false);
    refresh();
    _appFrame.addRecentFile(inputFile);
  }

  /**
   * Add a new sketch at point (x,y). Returns the new SketchNode.
   *
   * @param name The name of the new sketch being added
   * @param x    X Coordinate of new sketch
   * @param y    Y Coordinate of new sketch
   * @return the created SketchNode
   */
  public SketchNode addNewSketch(String name, double x, double y) {
    SketchFrame newFrame = new SketchFrame(this);
    SketchNode newNode = new SketchNode(name, (int) x, (int) y, newFrame);

    addVertex(newNode);

    return newNode;
  }

  /**
   * Add a view to a given sketch node
   *
   * @param sketch
   * @param viewName The name of the new view. Assumes no naming naming conflict
   * @return The new view node
   */
  public ViewNode addNewView(SketchNode sketch, String viewName) {
    Point newP = getNewViewPosition(sketch.getX(), sketch.getY(), 10);
    ViewFrame newFrame = new ViewFrame(this, sketch.getFrame().getMModel());
    ViewNode newNode = new ViewNode(viewName, (int) newP.getX(), (int) newP.getY(), newFrame);

    sketch.getFrame().getMModel().addView(newNode);

    // Add our ViewNode
    addVertex(newNode);

    // Add edge to out connected sketch
    addViewEdge(new ViewDefinitionEdge(newNode, sketch, getNewEdgeName()));

    return newNode;
  }

  /**
   * Add one one of our verticies to the overview
   *
   * @param theNode The node to be added
   */
  public void addVertex(OverviewVertex theNode) {
    // The next call will fire a rendering. At this point, the model adapter
    // does not know
    // where it should place the node, and picks a default value. This will
    // cause an update
    // in our Node's x and y coordinates, making it forget where it was
    // initialized.
    // We store it's initializeded position so as not to lose them in the
    // first rendering.
    int initX = theNode.getX();
    int initY = theNode.getY();

    // Make sure the name is unique; increment it if not.
    if (isNameUsed(theNode.getName())) {
      theNode.setName(getNewName(theNode.getName()));
    }

    // Add our sketch to the graph
    getGraphLayoutCache().insert(theNode);

    // Add our vertex to the appropriate map
    if (theNode instanceof SketchNode) {
      _sketchNodes.put(theNode.toString(), (SketchNode) theNode);
      _appFrame.getInfoTreeUI().addSketch((SketchNode) theNode);
    } else if (theNode instanceof ViewNode) {
      _viewNodes.put(theNode.toString(), (ViewNode) theNode);
      _appFrame.getInfoTreeUI().addView((ViewNode) theNode);
    }

    // Set the on-screen position of our sketch to the attributes of the
    // sketch
    AttributeMap nAttribs = theNode.getAttributes();

    GraphConstants.setAutoSize(nAttribs, true);
    GraphConstants.setBounds(nAttribs, new Rectangle2D.Double(initX, initY, 0, 0));

    // Reload the graph to reflect the new changes
    refresh(theNode);
  }

  /**
   * Adds a view edge to the overview.
   *
   * @param edge
   */
  public void addViewEdge(ViewDefinitionEdge edge) {
    getGraphLayoutCache().insert(edge);
    _viewEdges.put(edge.getName(), edge);
    refresh(edge);
  }

  /**
   * Returns the next available 'NewSketch' name, so we don't get duplicates.
   * 
   * @return The next new sketch name.
   */
  public String getNewSketchName() {
    return getNewName("NewSketch0");
  }

  /**
   * Returns the next available 'NewView' name, so we don't get duplicates.
   * 
   * @return The next new view name.
   */
  public String getNewViewName() {
    return getNewName("NewView0");
  }

  /**
   * Returns the next available edge name, so we don't get duplicates.
   * 
   * @return The next net edge name.
   */
  public String getNewEdgeName() {
    return getNewEdgeName("ve_0");
  }

  /**
   * Takes an edge name and makes it unique by append a number if needed.
   * 
   * @param tryName the first name to try
   * @return the next available new name
   */
  public String getNewEdgeName(String tryName) {
    while (isEdgeNameUsed(tryName)) {
      tryName = EasikTools.incrementName(tryName);
    }

    return tryName;
  }

  /**
   * Takes a name, and makes it unique. If it is already used, appends a number to
   * make it unique.
   *
   * @param tryName the first name to try
   * @return the next available new name
   */
  public String getNewName(String tryName) {
    while (isNameUsed(tryName)) {
      tryName = EasikTools.incrementName(tryName);
    }

    return tryName;
  }

  /**
   * Checks to see if a name is in use so that we will not have several instances
   * at once.
   *
   * @param inName The desired new name to check against
   * @return Is it used or not.
   */
  public boolean isNameUsed(String inName) {
    if (_sketchNodes.keySet().contains(inName) || _viewNodes.keySet().contains(inName)) {
      return true;
    }
    return false;

  }

  /**
   * Checks to see if an edge name is in use so that we will not have duplicates.
   *
   * @param inName The desired new name to check against
   * @return Is it used or not.
   */
  public boolean isEdgeNameUsed(String inName) {
    if (_viewEdges.keySet().contains(inName)) {
      return true;
    }
    return false;

  }

  /**
   * Tries random positions to find location for a new sketch node to be placed in
   * an effort to avoid hidden nodes. Looks for any location on the canvas. Gives
   * up afer a specified number of tries, accepting the random position.
   * 
   * @param tries The number of random tries to get a new location before giving
   *              up.
   * @return The point deemed acceptable for placement of a new node.
   */
  public Point getNewSketchPosition(int tries) {
    Random r = new Random();
    int w = getWidth() - 120;
    int h = getHeight() - 40;
    Point p = new Point(r.nextInt(w), r.nextInt(h));

    for (int i = 0; i < tries; i++) {
      if (getFirstCellForLocation(p.getX(), p.getY()) == null) {
        return p;
      }

      p = new Point(r.nextInt(w), r.nextInt(h));
    }

    return p;
  }

  /**
   * Tries random positions to find location for a new view node to be placed in
   * an effort to avoid hidden nodes. Looks for a point on a circe around a
   * specified point. Gives up afer a specified number of tries, accepting the
   * random position.
   * 
   * @param x     The X coordinate circle's centre on whose circumference we are
   *              placing the new node.
   * @param y     The Y coordinate circle's centre on whose circumference we are
   *              placing the new node.
   * @param tries The number of random tries to get a new location before giving
   *              up.
   * @return The point deemed acceptable for placement of a new node.
   */
  public Point getNewViewPosition(int x, int y, int tries) {
    Random random = new Random();

    // make sure we have tries
    tries = (tries <= 0) ? 10 : tries;

    // Radius and angle
    int r = 145;
    int theta;

    // Java compiler thinks it is smart and wants these initialized here.
    int newX = 0;
    int newY = 0;

    for (int i = 0; i < tries; i++) {
      theta = (int) (random.nextDouble() * 2 * Math.PI);

      // These can't be negative
      newX = (int) (x + r * Math.sin(theta));
      newY = (int) (y - r * Math.cos(theta));

      if (getFirstCellForLocation((newX > 0) ? newX : 0, (newY > 0) ? newY : 0) == null) {
        return new Point((newX > 0) ? newX : 0, (newY > 0) ? newY : 0);
      }
    }

    return new Point((newX > 0) ? newX : 0, (newY > 0) ? newY : 0);
  }
}
