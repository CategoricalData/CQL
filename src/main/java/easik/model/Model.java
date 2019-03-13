/**
 * 
 */
package easik.model;

import java.awt.Color;
import java.awt.Image;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.swing.undo.AbstractUndoableEdit;

import org.jgraph.JGraph;
import org.jgraph.event.GraphSelectionEvent;
import org.jgraph.event.GraphSelectionListener;
import org.jgraph.graph.DefaultGraphSelectionModel;
import org.jgraph.graph.GraphCell;

import easik.DocumentInfo;
import easik.Easik;
import easik.EasikTools;
import easik.graph.EasikGraphModel;
import easik.model.constraint.ModelConstraint;
import easik.model.edge.GuideEdge;
import easik.model.edge.ModelEdge;
import easik.model.path.ModelPath;
import easik.model.states.LoadingState;
import easik.model.ui.ModelFrame;
import easik.model.vertex.ModelVertex;
import easik.overview.Overview;
import easik.overview.vertex.ViewNode;
import easik.ui.GraphUI;

/**
 * This class will be extended by View and Sketch. It will provide methods used
 * to allow generics. For example getFrame() which is needed by ModelState so
 * that states can work with Views and Sketches.
 * 
 * @author Federico Mora
 *
 */
public abstract class Model<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>> extends JGraph {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4706192530164748256L;

	/** The current ApplicationFrame */
	protected F _Frame;

	/** The current DocumentInfo */
	protected DocumentInfo _docInfo;

	/** The State Manager monitoring this sketch */
	protected ModelStateManager<F, GM, M, N, E> _stateManager;

	/** The overview in which this sketch exists */
	protected Overview _theOverview;

	/** Linked List of all constraints */
	protected HashMap<Integer, ModelConstraint<F, GM, M, N, E>> _constraints;

	/** Hash Map of all edges, indexed by their label */
	protected LinkedHashMap<String, E> _edges;
	/** Hash Map of all entity nodes, indexed by their name */
	protected LinkedHashMap<String, N> _nodes;

	/** Keeps our model on hand */
	protected GM model;

	/** keeps track of what constraint ID to assign */
	protected int constraintID = 0;

	/**  */
	private List<ModelPath<F, GM, M, N, E>> fromSource; // used by
														// asPullbackConstraint
														// and
														// getProjectionPathFor

	/** True if we want to display warnings */
	private boolean _warnings = true;

	/**
	 * The default constructor sets all the visual settings for the JGraph, as
	 * well as initialising the sketch to be empty. It also adds appropriate
	 * listeners for all of the actions we are concerned with.
	 * 
	 * @param inFrame
	 *            The application frame of the sketch
	 * @param inOverview
	 *            the overview
	 */
	public Model(final F inFrame, final Overview inOverview) {
		super();

		setBackground(Easik.getInstance().getSettings().getColor("edit_canvas_background"));

		_Frame = inFrame;
		_theOverview = inOverview;

		setAntiAliased(true);
		setDisconnectable(false);
		setConnectable(false);
		setEditable(false);
		setSizeable(false);

		_docInfo = new DocumentInfo(_Frame);
		_Frame.setTitle("EASIK - Untitled");
		_constraints = new HashMap<>();
		initialiseModel();

		getGraphLayoutCache().setAutoSizeOnValueChange(true);
		setSelectionModel(new DefaultGraphSelectionModel(this) {
			/**
			*            
			*/
			private static final long serialVersionUID = -2426168338927400953L;

			@Override
			public Object[] getSelectables() {
				return Model.this.getStateManager().getSelectables();
			}
		});

		addGraphSelectionListener(new GraphSelectionListener() {
			@Override
			public void valueChanged(final GraphSelectionEvent gsEvent) {
				Model.this.getStateManager().selectionUpdated();
			}
		});

		updateUI();
	}

	/**
	 * When we initialise the model, we flush out all the data concerning the
	 * model itself.
	 *
	 * This methods serves as a "new model" function.
	 */
	protected abstract void initialiseModel();

	/**
	 * Called internally by Node when a node is renamed, to keep the entity node
	 * map consistent. Should not be called directly; instead call
	 * sketch.getEntity("currentname").setName("newname").
	 *
	 * @see easik.sketch.vertex.EntityNode#setName(String)
	 * @param node
	 *            the EntityNode being renamed
	 * @param oldName
	 *            the old name of the node
	 * @param newName
	 *            the candidate new name
	 * @return a string containing the final new node name, for EntityNode to
	 *         use.
	 */
	public String nodeRenamed(final N node, final String oldName, String newName) {
		// If the name already exists, we have to rename it
		while (_nodes.containsKey(newName)) {
			newName = EasikTools.incrementName(newName);
		}

		_nodes.put(newName, _nodes.remove(oldName));

		return newName;
	}

	/**
	 * Overridden version of JGraph.getSelectionCells() that always returns an
	 * empty array (instead of null) for an empty selection.
	 *
	 * @return
	 */
	@Override
	public Object[] getSelectionCells() {
		Object[] ret = super.getSelectionCells();

		if (ret == null) {
			ret = new Object[0];
		}

		return ret;
	}

	/**
	 *
	 *
	 * @return
	 */
	public ModelStateManager<F, GM, M, N, E> getStateManager() {
		return _stateManager;
	}

	/**
	 * Refreshes the specified nodes/edges in the sketch GUI. If called with no
	 * arguments, all items are refreshed.
	 *
	 * @param cells
	 *            the cells.
	 */
	public void refresh(final GraphCell... cells) {
		setBackground(Easik.getInstance().getSettings().getColor("edit_canvas_background"));

		final Object[] toRefresh = (cells.length > 0) ? cells : getRoots();

		model.cellsChanged(toRefresh);

		if (cells.length == 0) {
			super.refresh();
		}

	}

	/**
	 * Overrides superclass's refresh() method to go through the above
	 */
	@Override
	public void refresh() {
		refresh(new GraphCell[0]);
	}

	/**
	 * Returns a collection of all of the edges in the model
	 *
	 * @return Collection of all the edges.
	 */
	public Map<String, E> getEdges() {
		return Collections.unmodifiableMap(_edges);
	}

	/**
	 * Should not be called directly; instead just call edge.setName("newname").
	 *
	 * @see easik.sketch.edge.SketchEdge#setName(String)
	 * @param edge
	 *            the edge being renamed
	 * @param oldName
	 *            the old name of the edge
	 * @param newName
	 *            the candidate new name
	 * @return a string containing the final new edge name, for SketchEdge to
	 *         use.
	 */
	public abstract String edgeRenamed(final E edge, final String oldName, String newName);

	/**
	 * Removes an entity, and also cascades to remove all the arrows involved
	 * with it.
	 *
	 * @param toRemove
	 *            The entity about to be removed
	 */
	public abstract void removeNode(final N toRemove);

	/**
	 * Returns an edge based on its name.
	 *
	 * @param name
	 *            name of the edge desired
	 * @return the edge with this name.
	 */
	public E getEdge(final String name) {
		return _edges.get(name);
	}

	/**
	 * Accessor for a single entity in the sketch. Takes the entity name.
	 *
	 * @param entName
	 *            the entity name
	 * @return Node associated with that name, or null if the entity does not
	 *         exist
	 */
	public N getEntity(final String entName) {
		return _nodes.get(entName);
	}

	/**
	 * Accessor for the entities in the sketch
	 * 
	 * @return Collection of the entities
	 */
	public Collection<N> getEntities() {
		return Collections.unmodifiableCollection(_nodes.values());
	}

	/**
	 * Gets the HashMap of constraints indexed by their ID
	 *
	 * @return _constraints.
	 */
	public HashMap<Integer, ModelConstraint<F, GM, M, N, E>> getConstraints() {
		return _constraints;
	}

	/**
	 * Since this is a Swing component, this method is overloading a method of
	 * JGraph to adjust the look and feel. The feel we are changing is ignoring
	 * all but left clicks, allowing for right-click functionality not affecting
	 * the selections.
	 */
	@Override
	public void updateUI() {
		setUI(new GraphUI());
		invalidate();
	}

	/**
	 * Returns the parental sketch frame to whoever asks for it.
	 * 
	 * @return The current sketch frame
	 */
	public F getFrame() {
		return _Frame;
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
	 * Returns the next available constraintID starting at 0.
	 *
	 * @return The next constraintID.
	 */
	public int getConstraintID() {
		return constraintID;
	}

	/**
	 * Increments constraintID
	 */
	public void incConstraintID() {
		constraintID++;
	}

	/**
	 * Getter method for the overview in which this sketch exists.
	 *
	 * @return The overview in which this sketch exists
	 */
	public Overview getOverview() {
		return _theOverview;
	}

	/**
	 * Returns the GraphModel for this Sketch. This is a simple wrapper + cast
	 * around the parent's getMModel() method.
	 *
	 * @return the model
	 */
	@SuppressWarnings("unchecked")
	public GM getGraphModel() {
		return (GM) getModel();
	}

	/**
	 * Add a new constraint to the sketch
	 *
	 * @param c
	 *            The constraint to be added
	 */
	@SuppressWarnings("unchecked")
	public void addNewConstraint(final ModelConstraint<F, GM, M, N, E>... c) {
		addNewConstraint(Arrays.asList(c));
	}

	/**
	 * Add a new, empty entity at point X, Y
	 *
	 * @param name
	 *            The name of the new entity being added
	 * @param x
	 *            X Coordinate of new entity
	 * @param y
	 *            Y Coordinate of new entity
	 */
	public abstract void addNewNode(final String name, final double x, final double y);

	/**
	 * Returns the state of our sync flag. Any time a command would be issued to
	 * edit this sketch, a call should be made to this method. If it returns
	 * true, editing cannot occur safely.
	 * 
	 * @return A flag indicating if this sketch has been synced with a db. If
	 *         true, any changes to the sketch may destroy the link to the db.
	 */
	public abstract boolean isSynced();

	/**
	 * Sets our sync flag flag. This indicates whether or not we are synced with
	 * a db and wish to lock editing.
	 * 
	 * @param state
	 *            The state of our exported flag.
	 */
	public abstract void setSynced(final boolean state);

	/**
	 * Returns the next available name, starting from the passed-in name.
	 *
	 * @param name
	 *            the initial name to try; if it exists, we add/increment a
	 *            number until it's unique
	 * @return the first available unused name
	 */
	public String getNewName(String name) {
		while (isNameUsed(name)) {
			name = EasikTools.incrementName(name);
		}

		return name;
	}

	/**
	 * Returns the next available name, starting from 'NewEntity0', so we don't
	 * get duplicates.
	 *
	 * @return The next new name.
	 */
	public String getNewName() {
		return getNewName("NewEntity0");
	}

	/**
	 * Checks to see if a name is in use, so that we will not have several
	 * instances at once.
	 *
	 * @param inName
	 *            The desired new name to check against
	 * @return Is it used or not.
	 */
	public abstract boolean isNameUsed(final String inName);

	/**
	 * Accessor for the set of views on this sketch.
	 * 
	 * @return Collection of the views on this sketch null if it is a view
	 */
	public abstract Collection<ViewNode> getViews();

	/**
	 *
	 *
	 * @param c
	 */
	public void addNewConstraint(final Collection<ModelConstraint<F, GM, M, N, E>> c) {
		model.beginUpdate();

		for (final ModelConstraint<F, GM, M, N, E> con : c) {
			addConstraint(con);
		}

		model.endUpdate();
		refresh();
		_theOverview.refresh();
	}

	/**
	 * Adds a constraint to the sketch. This will register the constraint in the
	 * constraint list, as well as adding a visual representation of the
	 * constraint to the graph.
	 *
	 * @param c
	 *            The constraint to be added.
	 */
	@SuppressWarnings("unchecked")
	public void addConstraint(final ModelConstraint<F, GM, M, N, E> c) {
		// Push loading state
		_stateManager.pushState(new LoadingState<>((M) this));
		model.beginUpdate();
		_constraints.put(c.getID(), c);
		c.setVisible(c.isVisible());
		_Frame.getInfoTreeUI().addConstraint(c);
		model.postEdit(new AbstractUndoableEdit() {
			/**
			*            
			*/
			private static final long serialVersionUID = -4081680510909421247L;

			@Override
			public void undo() {
				super.undo();
				_constraints.remove(c.getID());
			}

			@Override
			public void redo() {
				super.redo();
				_constraints.put(c.getID(), c);
			}
		});
		model.endUpdate();

		// Pop state
		_stateManager.popState();
	}

	/**
	 * Removes a constraint and guide arrows
	 *
	 * @param toRemove
	 *            The constraint about to be removed
	 */
	public void removeConstraint(final ModelConstraint<F, GM, M, N, E> toRemove) {
		model.beginUpdate();
		getGraphLayoutCache().remove(toRemove.getGuideEdges().toArray(new GuideEdge[toRemove.getGuideEdges().size()]));
		getGraphLayoutCache().remove(new Object[] { toRemove });

		final int position = toRemove.getID();

		_constraints.remove(toRemove.getID());
		for (N n : toRemove.getEntities()) {
			n.removeConstraint(toRemove);
		}
		model.postEdit(new AbstractUndoableEdit() {
			/**
			*            
			*/
			private static final long serialVersionUID = 6431577416127308496L;

			@Override
			public void undo() {
				super.undo();
				_constraints.put(position, toRemove);
				for (N n : toRemove.getEntities()) {
					n.addConstraint(toRemove);
				}
			}

			@Override
			public void redo() {
				super.redo();
				_constraints.remove(position);
				for (N n : toRemove.getEntities()) {
					n.removeConstraint(toRemove);
				}
			}
		});

		// Remove Entity from tree
		_Frame.getInfoTreeUI().removeConstraint(toRemove);
		model.endUpdate();
	}

	/**
	 * Tries random positions to find location for a new node to be placed in an
	 * effort to avoid hidden nodes. Gives up afer a specified number of tries,
	 * accepting the random position.
	 * 
	 * @param tries
	 *            The number of random tries to get a new location before giving
	 *            up.
	 * @return The point deemed acceptable for placement of a new node.
	 */
	public Point getNewPosition(final int tries) {
		final Random r = new Random();
		final int w = getWidth() - 120;
		final int h = getHeight() - 40;
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
	 * Determines if this sketch contains any elements.
	 *
	 * @return True if this sketch contains any elements, and false otherwise
	 */
	public boolean isEmpty() {
		return getRoots().length == 0;
	}

	/**
	 * Takes a snapshot of the current sketch and returns it. We override
	 * JGraph's default getImage method to make sure the sketch is displayable,
	 * setting it up propertly if it isn't.
	 *
	 * @param bg
	 *            background color of the sketch, or null for transparent
	 * @param inset
	 *            the size of an empty border to add around the sketch
	 * @return an Image of the current sketch
	 */
	@Override
	public BufferedImage getImage(final Color bg, final int inset) {
		if (!_Frame.isDisplayable()) {
			// set preferred size to current size, and then pack to make sketch
			// displayable.
			// This is required for JGraph.getImage(..) to work properly
			_Frame.setPreferredSize(_Frame.getSize());
			_Frame.pack();
		}

		return super.getImage(bg, inset);
	}

	/**
	 * Takes a snapshot of the current sketch, sends the image to
	 * scaleImage(..), and sets in the node representing this sketch.
	 */
	public void updateThumb() {
		Image temp = getImage(Easik.getInstance().getSettings().getColor("edit_canvas_background"), 0);

		if (temp != null) {
			temp = scaleThumb((BufferedImage) temp);
		}

		_Frame.getNode().setThumbnail(temp);
	}

	/**
	 * Scales image by factor specified for x and y directions.
	 *
	 * @param inThumb
	 *            The image to be scaled
	 *
	 * @return The scaled image
	 */
	private static Image scaleThumb(final BufferedImage inThumb) {
		final double scaleFactor = Double.parseDouble(Easik.getInstance().getSettings().getProperty("thumb_scale_factor", "0.5"));

		return inThumb.getScaledInstance((int) (inThumb.getWidth() * scaleFactor), (int) (inThumb.getHeight() * scaleFactor), Image.SCALE_SMOOTH);
	}

	/**
	 * Gets the name of this sketch. Simply a shortcut for _docInfo.getName().
	 *
	 * @return
	 */
	@Override
	public String getName() {
		return _docInfo.getName();
	}

	/**
	 * Used to mark a sketch as dirty. When called, we mark the Overview as
	 * dirty, and update the last modification time of the sketch to the current
	 * time.
	 */
	public void setDirty() {
		getOverview().setDirty(true);
		getOverview().refresh(_Frame.getNode());
		getDocInfo().updateModificationDate();
	}

	/**
	 * Method to determine whether a set of paths potentially forming a pullback
	 * constraint could legally form a pullback constraint. Additionally, if the
	 * pullback constraint is valid, this returns the passed-in list in the
	 * appropriate order,
	 *
	 * Note: This method does not currently care about the order that the paths
	 * are selected.
	 *
	 * Modified summer 2012 (CRF): The original (not 'wide') pullback constraint
	 * was hard coded (with 4 nested loops) to arrange the paths in order (i.e.
	 * exponential time). This is infeasible for a generalized pullback. This
	 * method now arranges the edges more efficiently: For each edge in the
	 * constraint, if we store a count of the number of times we see an N (by
	 * calling getDomain() and getCoDomain() on the path) in a hash map. Once
	 * all paths have been considered, the source and target will have each been
	 * encountered n times where n is the width of the constraint. The
	 * intermediate nodes will have been encountered 2 times. So it is possible
	 * to determine the constraints if the width is > 2.If the width = 2, I pass
	 * the paths to be dealt with by the original pullback method. Otherwise,
	 * the source and targets are identified to separate the path list into
	 * target and projection paths. This then lets us arrange the list as a
	 * pullback. _____________________________________________________ | Source
	 * P1 | Target P1 | Source P2 | Target P2 | ...
	 * |___________|___________|___________|___________|_____
	 *
	 * TODO All of this could be simplified by keeping track of which node is
	 * which upon creation...
	 *
	 * @param paths
	 *            The set of potential paths
	 * @return Null if invalid pullback constraint, a valid pullback constraint
	 *         ordering otherwise.
	 */
	public List<ModelPath<F, GM, M, N, E>> asPullbackConstraint(List<ModelPath<F, GM, M, N, E>> paths) {
		if (paths.size() == 4) // pass to original pullback check
		{
			return asPullbackConstraintBaseCase(paths);
		}

		// make sure we have a valid number of paths
		if ((paths.size() < 2) || (paths.size() % 2 != 0)) {
			return null;
		}

		// determine source and target nodes
		HashMap<N, Integer> counts = new HashMap<>();

		for (ModelPath<F, GM, M, N, E> sk : paths) {
			if (sk == null) {
				return null;
			}

			if (!counts.containsKey(sk.getCoDomain())) {
				counts.put(sk.getCoDomain(), new Integer(1));
			} else {
				counts.put(sk.getCoDomain(), counts.get(sk.getCoDomain()) + 1);
			}

			if (!counts.containsKey(sk.getDomain())) {
				counts.put(sk.getDomain(), new Integer(1));
			} else {
				counts.put(sk.getDomain(), counts.get(sk.getDomain()) + 1);
			}
		}

		// the potential source and target nodes, don't know which yet
		N a = null;
		N b = null;
		Set<N> seen = counts.keySet();

		for (N en : seen) {
			if (counts.get(en) > 2) { // a source or target node
				if (a == null) {
					a = en;

					continue;
				} else if (b == null) {
					b = en;

					continue;
				} else { // something went wrong
					return null;
				}
			}
		}

		// now determine source and target from an arbitrary edge (0)
		N source = null;
		N target = null;

		if (paths.get(0).getDomain() == a) {
			source = a;
			target = b;
		} else if (paths.get(0).getDomain() == b) {
			source = b;
			target = a;
		} else {
			if (paths.get(0).getCoDomain() == a) {
				target = a;
				source = b;
			} else if (paths.get(0).getCoDomain() == b) {
				source = a;
				target = b;
			} else { // something went wrong
				return null;
			}
		}

		// now separate the source and target paths knowing the source and
		// target nodes
		fromSource = new ArrayList<>();

		ArrayList<ModelPath<F, GM, M, N, E>> toTarget = new ArrayList<>();

		for (int i = 0; i < paths.size(); i++) {
			if (paths.get(i).getDomain() == source) {
				fromSource.add(paths.get(i));
			} else {
				toTarget.add(paths.get(i));
			}
		}

		// should have two width sized lists
		if ((fromSource.size() != toTarget.size()) || (fromSource.size() != paths.size() / 2)) {
			return null;
		}

		// the ordered paths (will eventually be returned if constraint is
		// valid)
		List<ModelPath<F, GM, M, N, E>> orderedPaths = new ArrayList<>();

		for (ModelPath<F, GM, M, N, E> p : toTarget) {
			ModelPath<F, GM, M, N, E> skp = getProjectionPathFor(p);

			if (skp == null) {
				return null;
			}

			orderedPaths.add(skp);
			orderedPaths.add(p);
		}

		// make sure we could match a target with every source
		if (fromSource.size() != 0) {
			return null;
		}

		// this will probably pass at this point, but it doesn't hurt to check
		if (orderedPaths.size() != paths.size()) {
			return null;
		}

		// now make sure we have a pullback arrangement that makes sense
		for (int i = 0; i < orderedPaths.size(); i++) {
			if (i % 2 == 1) {
				if (orderedPaths.get(i).getCoDomain() != target) {
					return null;
				}
			} else {
				if (orderedPaths.get(i).getDomain() != source) {
					return null;
				}
			}
		}

		// success
		return orderedPaths;
	}

	/**
	 * Find the projection path for a given source path. (The path to the
	 * intermediary node that is the domain of p)
	 * 
	 * @param p
	 *            The source path
	 * @return Corresponding projection path, null if not found
	 */
	private ModelPath<F, GM, M, N, E> getProjectionPathFor(ModelPath<F, GM, M, N, E> p) {
		for (int i = 0; i < fromSource.size(); i++) {
			ModelPath<F, GM, M, N, E> curr = fromSource.get(i);

			if (p.getDomain() == curr.getCoDomain()) { // intermediary nodes
														// match
				fromSource.remove(i); // make future searches faster and check
										// validity later

				return curr; // found it
			}
		}

		return null; // didn't find a valid path, this ruins the whole
						// constraint
	}

	/**
	 * Method to determine whether a set of paths potentially forming a pullback
	 * constraint could legally form a pullback constraint. Additionally, if the
	 * pullback constraint is valid, this rearranges the passed-in list to be in
	 * the appropriate order: path elements 0 and 1 form one path, elements 2
	 * and 3 form the other path. In other words, the domains of elements 0 and
	 * 2 are the same, and the codomains of elements 1 and 3 are the same.
	 *
	 * Note: This method does not currently care about the order that the paths
	 * are selected.
	 *
	 * @param paths
	 *            The set of potential paths
	 * @return Null if invalid pullback constraint, a valid pullback constraint
	 *         ordering otherwise.
	 * @since 2006-05-25 Vera Ranieri
	 */
	public List<ModelPath<F, GM, M, N, E>> asPullbackConstraintBaseCase(List<ModelPath<F, GM, M, N, E>> paths) {
		if (paths.size() != 4) // NOT GENERAL!
		{
			return null;
		}

		for (ModelPath<F, GM, M, N, E> p : paths) {
			if (p == null) {
				return null;
			}

			if (!p.isFullyDefined()) {
				return null;
			}
		}

		// brute force search for a valid configuration
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++) {
				if (j == i) {
					continue;
				}

				for (int k = 0; k < 4; k++) {
					if ((k == i) || (k == j)) {
						continue;
					}

					for (int l = 0; l < 4; l++) {
						if ((l == k) || (l == j) || (l == i)) {
							continue;
						}

						ModelPath<F, GM, M, N, E> pathA = paths.get(i), pathB = paths.get(j), pathC = paths.get(k), pathD = paths.get(l);
						N domA = pathA.getDomain(), domB = pathB.getDomain(), domC = pathC.getDomain(), domD = pathD.getDomain();
						N codoA = pathA.getCoDomain(), codoB = pathB.getCoDomain(), codoC = pathC.getCoDomain(), codoD = pathD.getCoDomain();

						try {
							if ((codoA == domB) && ( // "Left" paths connect
							codoC == domD) && ( // "Right" paths connect
							codoB == codoD) && ( // "Bottom" paths point to same
													// thing
							domA == domC)) { // "Top" paths come from the same
												// thing
								if (!(!pathB.getEdges().getFirst().isInjective() || (pathC.getEdges().getFirst().isInjective() && !pathD.getEdges().getFirst().isInjective()) || pathA.getEdges().getFirst().isInjective())) {
									return null;
								}

								ArrayList<ModelPath<F, GM, M, N, E>> newOrderPaths = new ArrayList<>();

								newOrderPaths.add(pathA);
								newOrderPaths.add(pathB);
								newOrderPaths.add(pathC);
								newOrderPaths.add(pathD);

								return newOrderPaths;
							}
						} catch (Exception e) {
							return null;
						}
					}
				}
			}
		}

		return null;
	}

	/**
	 * Static method to determine whether a set of paths could theoretically
	 * designate a sum constraint.
	 * 
	 * @param paths
	 *            The paths to check for a sum constraint formation.
	 * @return True if forms a sum constraint, false otherwise.
	 * @since 2006-05-29 Vera Ranieri
	 */
	public boolean isSumConstraint(List<ModelPath<F, GM, M, N, E>> paths) {
		// We must have a bunch of injective arrows aimed at the same spot
		N sharedTarget = null;
		int edgesInvolved = 0;
		ModelPath<F, GM, M, N, E> currentPath;
		E edge;
		int size = paths.size();

		// Search through the selection...
		for (int i = 0; i < size; i++) {
			currentPath = paths.get(i);

			// First target we come across becomes our 'sharedTarget'
			if (sharedTarget == null) {
				sharedTarget = currentPath.getCoDomain();
			} else if (sharedTarget != currentPath.getCoDomain()) {
				// If we encounter a source which is not the sharedSource, fail
				return false;
			}

			// Make sure the first edge in the path is injective
			if (currentPath.getEdges().size() != 0) {
				edge = currentPath.getEdges().get(0);

				if (!edge.isInjective()) {
					return false;
				}

				for (int j = i + 1; j < size; j++) {
					if (ModelPath.pathsAreEqual(currentPath, paths.get(j))) {
						return false;
					}
				}
			}

			// Otherwise, we're fine, add the edge
			edgesInvolved++;
		}

		return edgesInvolved >= 2;
	}

	/**
	 * Method to determine whether a given set of paths could legally create a
	 * product constraint.
	 *
	 * @param paths
	 *            The paths potentially involved in a product constraint
	 * @return True if a legal path configuration, false otherwise
	 * @since 2006-05-25 Vera Ranieri
	 */
	public boolean isProductConstraint(List<ModelPath<F, GM, M, N, E>> paths) {
		// We must have a bunch of injective arrows aimed at the same spot
		N sharedSource = null;
		int pathsInvolved = 0;

		// Search through the selection...
		for (ModelPath<F, GM, M, N, E> currentPath : paths) {
			// First source we come across becomes our 'sharedSource'
			if (sharedSource == null) {
				sharedSource = currentPath.getDomain();
			} else if (sharedSource != currentPath.getDomain()) {
				// If we encounter a source which is not the sharedSource, fail
				return false;
			}

			if (sharedSource.equals(currentPath.getCoDomain())) {
				// In a loop
				return false;
			}

			if (!currentPath.isFullyNormal()) {
				return false;
			}

			// make sure path is not restrict. Since must be fully defined this
			// means it must be cascade
			if (!currentPath.isCompositeCascade()) {
				return false;
			}

			// Otherwise, we're fine, add the edge
			pathsInvolved++;
		}

		return pathsInvolved >= 2;
	}

	/**
	 * Method to determine whether a set of paths potentially forming a
	 * equalizer constraint could legally form a equalizer constraint. The first
	 * path must be the equalizer projection, and must consist of a single,
	 * injective edge. The remaining paths (of which there must be at least 2)
	 * must start at the project path's codomain, must all have the same
	 * codomain, and must be distinct paths from one another.
	 *
	 * @param paths
	 *            The set of potential paths
	 * @return True if a legal equalizer configuration, false otherwise.
	 */
	public boolean isEqualizerConstraint(ArrayList<ModelPath<F, GM, M, N, E>> paths) {
		// There has to be at least 3 paths: the projection, and 2 or more
		// equalizer paths.
		if (paths.size() < 3) {
			return false;
		}

		// Null paths are not permitted, nor are empty paths:
		for (ModelPath<F, GM, M, N, E> p : paths) {
			if (p == null) {
				return false;
			}

			if (p.getEdges().isEmpty()) {
				return false;
			}

			// has to be fully defined
			if (!p.isFullyDefined()) {
				return false;
			}
		}

		// Make sure the the first path is a valid projection path: it must
		// start with an injective edge.
		if (!paths.get(0).isInjective()) {
			return false;
		}

		N domain = paths.get(0).getCoDomain();
		N codomain = paths.get(1).getCoDomain();

		// and its domain must be the codomain of all the other edges. Also make
		// sure that all
		// the other paths agree on codomain:
		for (int i = 1; i < paths.size(); i++) {
			ModelPath<F, GM, M, N, E> p = paths.get(i);

			if ((p.getDomain() != domain) || (p.getCoDomain() != codomain)) {
				return false;
			}

			// We also need to make sure that there are no equal paths:
			for (int j = i + 1; j < paths.size(); j++) {
				if (ModelPath.pathsAreEqual(p, paths.get(j))) {
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * Checks whether the current selected paths form a commutative diagram.
	 * This is determined by checking whether the paths participating in the
	 * commutative diagram have the same domain and codomain, and also by making
	 * sure that each path is different.
	 *
	 * @param inPaths
	 *            The list of paths participating in the commutative diagram
	 * @return true if it forms a valid commutative diagram, false otherwise
	 */
	public boolean isCommutativeDiagram(List<ModelPath<F, GM, M, N, E>> inPaths) {
		if (inPaths.size() < 2) {
			return false; // Not a CD if it's a single path
		}

		ArrayList<ModelPath<F, GM, M, N, E>> paths = new ArrayList<>(inPaths);
		ModelPath<F, GM, M, N, E> firstPath = paths.get(0);

		for (int i = 0; i < paths.size(); i++) {
			ModelPath<F, GM, M, N, E> path = paths.get(i);

			// Check that the domain and codomain are equal to the first path
			// (and therefore
			// all equal to each other).
			if (i > 0) // Don't both checking the first one, obviously it is
						// equal to itself
			{
				if (!firstPath.getDomain().equals(path.getDomain()) || !firstPath.getCoDomain().equals(path.getCoDomain())) {
					return false;
				}
			}

			// Now look at every following path, and make sure none is the same
			// as this path:
			for (int j = i + 1; j < paths.size(); j++) {
				ModelPath<F, GM, M, N, E> otherPath = paths.get(j);

				// We can skip the check if that path sizes aren't the same:
				if (path.getEdges().size() == otherPath.getEdges().size()) {
					boolean different = false;
					int size = path.getEdges().size();

					for (int k = 0; k < size; k++) {
						if (!path.getEdges().get(k).equals(otherPath.getEdges().get(k))) {
							// We found a difference, so we can abort this loop
							different = true;

							break;
						}
					}

					if (!different) {
						return false; // Identical paths; not a valid CD.
					}
				}
			}
		}

		// We got through all the checks, so this must be a valid CD.
		return true;
	}

	public boolean useWarnings() {
		return _warnings;
	}

	public void setWarnings(boolean _warnings) {
		this._warnings = _warnings;
	}

}
