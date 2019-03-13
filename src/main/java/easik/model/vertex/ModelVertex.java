/**
 * 
 */
package easik.model.vertex;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.undo.AbstractUndoableEdit;

import org.jgraph.graph.AttributeMap;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.DefaultPort;
import org.jgraph.graph.GraphConstants;

import easik.graph.EasikGraphModel;
import easik.model.Model;
import easik.model.attribute.EntityAttribute;
import easik.model.constraint.ModelConstraint;
import easik.model.edge.ModelEdge;
import easik.model.keys.UniqueIndexable;
import easik.model.keys.UniqueKey;
import easik.model.ui.ModelFrame;

/**
 * Will be extended by ViewVertex and SketchVertex Contains methods used for
 * generics. For example getName() which allows ModelPath to work with either a
 * ViewVertex or a SketchVertex and call these methods
 * 
 * @author Federico Mora
 *
 */

public abstract class ModelVertex<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>> extends DefaultGraphCell {
	/**
	 * 
	 */
	private static final long serialVersionUID = 4320693946567830608L;

	/** The port of this node that edges connect to */
	protected DefaultPort _port = new DefaultPort();

	/**
	 * The name of this node
	 */
	protected String _name;

	/**
	 * The x and y coordinates we wish this vertex to be initially rendered at
	 */
	protected int _initX, _initY;

	/** The sketch to which this node belongs */
	protected M _theModel;

	/**
	 * List of all unique keys for this entity
	 */
	protected List<UniqueKey<F, GM, M, N, E>> _uniqueKeys = new ArrayList<>(3);

	/**
	 * Node representing the key for the information tree
	 */
	protected DefaultMutableTreeNode _keyNode = new DefaultMutableTreeNode("Keys");

	/**
	 * The set of constraints this Node is currently in
	 */
	protected Set<ModelConstraint<F, GM, M, N, E>> _constraints = new LinkedHashSet<>(10);

	/**
	 * List of all attributes for this entity
	 */
	protected List<EntityAttribute<F, GM, M, N, E>> _entityAttributes = new ArrayList<>(20);

	/**
	 * List hidden attributes for this entity. Used for keeping track of what
	 * rows are part of constraints
	 */
	protected List<EntityAttribute<F, GM, M, N, E>> _hiddenAttributes = new ArrayList<>(20);

	/**
	 * Node representing this object in the information tree
	 */
	protected DefaultMutableTreeNode _treeNode;

	/**  */
	protected ArrayList<N> depend = new ArrayList<>();

	/**
	 * Node representing the attributes for the information tree
	 */
	protected DefaultMutableTreeNode _attribNode = new DefaultMutableTreeNode("Attributes");

	/**
	 * Create a blank sketch vertex, no name, and located at (0, 0)
	 *
	 * @param inSketch
	 *            the Sketch the vertex is being added to
	 */
	public ModelVertex(M inModel) {
		this("", 0, 0, inModel);
	}

	/**
	 * Create a vertex with basic attributes
	 * 
	 * @param name
	 *            Label
	 * @param x
	 *            X coordinate
	 * @param y
	 *            Y coordinate
	 * @param inSketch
	 *            the sketch the vertex is being added to
	 */
	public ModelVertex(String name, int x, int y, M inModel) {
		_initX = x;
		_initY = y;
		_name = name;
		_theModel = inModel;

		add(_port);
		setUserObject(this);
	}

	/**
	 * Accessor for the name field
	 * 
	 * @return the name
	 */
	public String getName() {
		return _name;
	}

	/**
	 * To string method returns the name
	 * 
	 * @return Current name
	 */
	@Override
	public String toString() {
		return getName();
	}

	/**
	 * Mutator for the name field
	 * 
	 * @param name
	 *            The new name
	 */
	public void setName(String name) {
		_name = name;

		_theModel.refresh(this);
	}

	/**
	 * 
	 */
	public void addDepend(N n) {
		depend.add(n);
	}

	/**
	 * 
	 */
	public void removeDepend(N n) {
		depend.remove(n);
	}

	/**
	 * 
	 */
	public ArrayList<N> getDepend() {
		return depend;
	}

	/**
	 * Accessor for the X coordinate
	 * 
	 * @return The x coordinate
	 */
	public int getX() {
		Rectangle2D bounds = GraphConstants.getBounds(getAttributes());

		if (bounds != null) {
			return (int) bounds.getX();
		} 
			return _initX;
		
	}

	/**
	 * Accessor for the Y Coordinate
	 * 
	 * @return The Y Coordinate
	 */
	public int getY() {
		Rectangle2D bounds = GraphConstants.getBounds(getAttributes());

		if (bounds != null) {
			return (int) bounds.getY();
		} 
			return _initY;
		
	}

	/**
	 *
	 *
	 * @return
	 */
	public int getLastKnownX() {
		return _initX;
	}

	/**
	 *
	 *
	 * @return
	 */
	public int getLastKnownY() {
		return _initY;
	}

	/**
	*
	*/
	public void savePosition() {
		_initX = getX();
		_initY = getY();
	}

	/**
	 * add to the list of hidden attributes. Used for keeping track of
	 * intermediate nodes' rows in constraints.
	 *
	 * @param inAtt
	 *            The
	 *            EntityAttribute<SketchFrame,SketchGraphModel,Sketch,EntityNode,SketchEdge>
	 *            to add to this EntityNode.
	 */
	public void addHiddenEntityAttribute(final EntityAttribute<F, GM, M, N, E> inAtt) {
		for (EntityAttribute<F, GM, M, N, E> ea : _hiddenAttributes) {
			if (inAtt.getName().equals(ea.getName())) {
				return;
			}
		}
		_hiddenAttributes.add(inAtt);
	}

	/**
	 * Creates a new
	 * EntityAttribute<SketchFrame,SketchGraphModel,Sketch,EntityNode,SketchEdge>
	 * and add its to the list of attributes
	 *
	 * @param inAtt
	 *            The
	 *            EntityAttribute<SketchFrame,SketchGraphModel,Sketch,EntityNode,SketchEdge>
	 *            to add to this EntityNode.
	 */
	public void addEntityAttribute(final EntityAttribute<F, GM, M, N, E> inAtt) {
		for (EntityAttribute<F, GM, M, N, E> ea : _entityAttributes) {
			if (inAtt.getName().equals(ea.getName())) {
				return;
			}
		}
		_entityAttributes.add(inAtt);
		getAttributeNode().add(inAtt);
		getMModel().refresh();
		getMModel().getFrame().getInfoTreeUI().refreshTree(this);
		getMModel().getGraphModel().postEdit(new AbstractUndoableEdit() {
			/**
			*            
			*/
			private static final long serialVersionUID = -8359488490185936367L;

			@Override
			public void undo() {
				super.undo();
				_entityAttributes.remove(inAtt);
				getAttributeNode().remove(inAtt);
				getMModel().refresh();
				getMModel().getFrame().getInfoTreeUI().refreshTree(ModelVertex.this);
			}

			@Override
			public void redo() {
				super.redo();
				_entityAttributes.add(inAtt);
				getAttributeNode().add(inAtt);
				getMModel().refresh();
				getMModel().getFrame().getInfoTreeUI().refreshTree(ModelVertex.this);
			}
		});
	}

	/**
	 * Removes an attribute from the list
	 *
	 * @param curAttribute
	 *            The attribute to be removed
	 */
	public void removeEntityAttribute(final EntityAttribute<F, GM, M, N, E> curAttribute) {
		final GM model = getMModel().getGraphModel();

		model.beginUpdate();

		final int attPos = _entityAttributes.indexOf(curAttribute);
		final int nodePos = getAttributeNode().getIndex(curAttribute);

		_entityAttributes.remove(curAttribute);
		getAttributeNode().remove(curAttribute);
		getMModel().refresh();
		getMModel().getFrame().getInfoTreeUI().refreshTree(this);
		getMModel().getGraphModel().postEdit(new AbstractUndoableEdit() {
			/**
			*            
			*/
			private static final long serialVersionUID = -3013106807521458129L;

			@Override
			public void undo() {
				super.undo();
				_entityAttributes.add(attPos, curAttribute);
				getAttributeNode().insert(curAttribute, nodePos);
				getMModel().refresh();
				getMModel().getFrame().getInfoTreeUI().refreshTree(ModelVertex.this);
			}

			@Override
			public void redo() {
				super.redo();
				_entityAttributes.remove(curAttribute);
				getAttributeNode().remove(curAttribute);
				getMModel().refresh();
				getMModel().getFrame().getInfoTreeUI().refreshTree(ModelVertex.this);
			}
		});

		// Remove references to the attribute from unique keys
		for (final UniqueKey<F, GM, M, N, E> curKey : _uniqueKeys) {
			curKey.removeElement(curAttribute);
		}

		// Fix up any empty/duplicate unique keys resulting from the attribute
		// removal
		this.cleanup();
		model.endUpdate();
	}

	/**
	 * Adds a unique key to the list
	 *
	 * @param inAtts
	 *            The attributes used in the key
	 * @param inName
	 *            The name of the unique key
	 */
	public void addUniqueKey(final ArrayList<UniqueIndexable> inAtts, final String inName) {
		addUniqueKey(new UniqueKey<>(this, inAtts, inName));
	}

	/**
	 * Adds a unique key to the list
	 *
	 * @param inKey
	 *            The key to be added
	 */
	public void addUniqueKey(final UniqueKey<F, GM, M, N, E> inKey) {
		_uniqueKeys.add(inKey);
		getKeyNode().add(inKey);
		getMModel().refresh();
		getMModel().getFrame().getInfoTreeUI().refreshTree(this);
		getMModel().getGraphModel().postEdit(new AbstractUndoableEdit() {
			/**
			*            
			*/
			private static final long serialVersionUID = 6892030054106524533L;

			@Override
			public void undo() {
				super.undo();
				_uniqueKeys.remove(inKey);
				getKeyNode().remove(inKey);
				getMModel().refresh();
				getMModel().getFrame().getInfoTreeUI().refreshTree(ModelVertex.this);
			}

			@Override
			public void redo() {
				super.redo();
				_uniqueKeys.add(inKey);
				getKeyNode().add(inKey);
				getMModel().refresh();
				getMModel().getFrame().getInfoTreeUI().refreshTree(ModelVertex.this);
			}
		});
	}

	/**
	 *
	 *
	 * @return
	 */
	public DefaultMutableTreeNode getAttributeNode() {
		return _attribNode;
	}

	/**
	 * Add a constraint to the set of constraints of which this node is a part
	 *
	 * @param con
	 *            the constraint
	 */
	public void addConstraint(final ModelConstraint<F, GM, M, N, E> con) {
		final boolean added = _constraints.add(con);

		if (added) {
			getMModel().getGraphModel().postEdit(new AbstractUndoableEdit() {
				/**
				 *                
				 */
				private static final long serialVersionUID = 2332966369924855958L;

				@Override
				public void undo() {
					super.undo();
					_constraints.remove(con);
				}

				@Override
				public void redo() {
					super.redo();
					_constraints.add(con);
				}
			});
		}
	}

	/**
	 * Removes a constraint from the set of constraints this node believes to be
	 * in
	 *
	 * @param con
	 *            the constraint
	 * @return true if removed
	 */
	public boolean removeConstraint(final ModelConstraint<F, GM, M, N, E> con) {
		final boolean removed = _constraints.remove(con);
		final EntityAttribute<F, GM, M, N, E> ea = getHiddenAttribute(con);

		_hiddenAttributes.remove(ea);

		if (removed) {
			getMModel().getGraphModel().postEdit(new AbstractUndoableEdit() {
				/**
				 *                
				 */
				private static final long serialVersionUID = 6399404906957760505L;

				@Override
				public void undo() {
					super.undo();
					_constraints.add(con);
					_hiddenAttributes.add(ea);
				}

				@Override
				public void redo() {
					super.redo();
					_constraints.remove(con);
					_hiddenAttributes.remove(ea);
				}
			});
		}
		return removed;
	}

	/**
	 * Gets a hidden attribute based on a constraint passed in as an argument
	 * 
	 * Federico Mora
	 * 
	 * @param con
	 *            ModelConstraint to match the attribute
	 * @return
	 */
	private EntityAttribute<F, GM, M, N, E> getHiddenAttribute(ModelConstraint<F, GM, M, N, E> con) {
		EntityAttribute<F, GM, M, N, E> ea = null;
		for (EntityAttribute<F, GM, M, N, E> att : _hiddenAttributes) {
			if (att.getName().equals("BC" + con.getID())) {
				ea = att;
			}
		}
		return ea;
	}

	/**
	 * Gets the set of constraints of which this node is a part
	 *
	 * @return the constraints as an unmodifiable set
	 */
	public Set<ModelConstraint<F, GM, M, N, E>> getConstraints() {
		return Collections.unmodifiableSet(_constraints);
	}

	/**
	 * Returns the Sketch this node belongs to.
	 *
	 * @return Sketch
	 */
	public M getMModel() {
		return _theModel;
	}

	/**
	 * Removes a unique key from the list
	 *
	 * @param inKey
	 *            The unique key to be removed
	 */
	public void removeUniqueKey(final UniqueKey<F, GM, M, N, E> inKey) {
		final int keyPos = _uniqueKeys.indexOf(inKey);
		final int nodePos = getKeyNode().getIndex(inKey);

		_uniqueKeys.remove(inKey);
		getKeyNode().remove(inKey);
		getMModel().refresh();
		getMModel().getFrame().getInfoTreeUI().refreshTree(this);
		getMModel().getGraphModel().postEdit(new AbstractUndoableEdit() {
			/**
			 *            
			 */
			private static final long serialVersionUID = 2520426386166974414L;

			@Override
			public void undo() {
				super.undo();
				_uniqueKeys.add(keyPos, inKey);
				getKeyNode().insert(inKey, nodePos);
				getMModel().refresh();
				getMModel().getFrame().getInfoTreeUI().refreshTree(ModelVertex.this);
			}

			@Override
			public void redo() {
				super.redo();
				_uniqueKeys.remove(inKey);
				getKeyNode().remove(inKey);
				getMModel().refresh();
				getMModel().getFrame().getInfoTreeUI().refreshTree(ModelVertex.this);
			}
		});
	}

	/**
	 * Returns a List of the unique keys
	 *
	 * @return a List of the unique keys
	 */
	public List<UniqueKey<F, GM, M, N, E>> getUniqueKeys() {
		return Collections.unmodifiableList(_uniqueKeys);
	}

	/**
	 * Returns the tree node used to display unique keys
	 *
	 * @return The tree node used to display unique keys
	 */
	public DefaultMutableTreeNode getKeyNode() {
		return _keyNode;
	}

	/**
	 * Returns the list of EntityAttributes
	 *
	 * @return List of EntityAttributes
	 */
	public List<EntityAttribute<F, GM, M, N, E>> getEntityAttributes() {
		return Collections.unmodifiableList(_entityAttributes);
	}

	/**
	 * Returns the list of hidden EntityAttributes
	 *
	 * @return List of hidden EntityAttributes
	 */
	public List<EntityAttribute<F, GM, M, N, E>> getHiddenEntityAttributes() {
		return Collections.unmodifiableList(_hiddenAttributes);
	}

	/**
	 * Returns a Set of SketchEdges of the outgoing edges of this EntityNode.
	 * Note that edges for constraints are <b>not</b> included.
	 *
	 * @return Set of outgoing edges from this EntityNode
	 */
	@SuppressWarnings("unchecked")
	public Set<E> getOutgoingEdges() {
		@SuppressWarnings("rawtypes")
		final List outEdges = _theModel.getGraphLayoutCache().getOutgoingEdges(this, Collections.emptySet(), false, true);
		final Set<E> outgoing = new LinkedHashSet<>(outEdges.size());

		for (final Object out : outEdges) {
			if (out instanceof ModelEdge) {
				outgoing.add((E) out);
			}
		}

		return outgoing;
	}

	/**
	 * Returns a Set of UniqueIndexable-implementing outgoing edges of this
	 * node.
	 *
	 * @return set of uniques
	 */
	public Set<UniqueIndexable> getIndexableEdges() {
		final Set<E> edges = getOutgoingEdges();
		final Set<UniqueIndexable> attribs = new LinkedHashSet<>(edges.size());

		for (final E edge : edges) {
			if (edge instanceof UniqueIndexable) {
				attribs.add((UniqueIndexable) edge);
			}
		}

		return attribs;
	}

	/**
	 * Check to see if an attribute name is already used in the entity
	 *
	 * @param inNode
	 *            The entity being checked against
	 * @param attName
	 *            The name being checked for duplication
	 * @return true if the name is in use, false otherwise
	 */
	public boolean isAttNameUsed(final String attName) {
		// Loop through all attributes of the entity
		for (final EntityAttribute<F, GM, M, N, E> curAtt : getEntityAttributes()) {
			if (curAtt.getName().toUpperCase().equals(attName.toUpperCase())) {
				return true;
			}
		}

		// No duplicate found
		return false;
	}

	/**
	 * Returns if the name of a key is already used
	 *
	 * @param inNode
	 *            The entity being checked against
	 * @param keyName
	 *            The name of the key
	 * @return true if the key name is already used, false otherwise
	 */
	public boolean isKeyNameUsed(final String keyName) {
		for (final UniqueKey<F, GM, M, N, E> q : getUniqueKeys()) {
			if (q.getKeyName().toUpperCase().equals(keyName.toUpperCase())) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Finds any empty or duplicate unique keys of the passed-in EntityNode and
	 * returns a list of them. This is used by cleanup(EntityNode) to remove
	 * problem keys after removing an attribute or edge.
	 *
	 * @param inNode
	 *            the node to check
	 * @return List of UniqueKey<F,GM,M,N,E> objects that should be removed to
	 *         clean up the table
	 */
	public List<UniqueKey<F, GM, M, N, E>> findBadKeys() {
		final Collection<Set<UniqueIndexable>> seen = new HashSet<>(5);
		final List<UniqueKey<F, GM, M, N, E>> badKeys = new LinkedList<>();

		for (final UniqueKey<F, GM, M, N, E> q : getUniqueKeys()) {
			final Set<UniqueIndexable> qElems = q.getElements();

			if (qElems.isEmpty()) // The attribute set is now empty
			{
				badKeys.add(q);
			} else if (seen.contains(qElems)) // We've already encountered this
												// set of attributes
			{
				badKeys.add(q);
			} else // Otherwise it's good, but remember that we've seen it
					// already
			{
				seen.add(qElems);
			}
		}

		return badKeys;
	}

	/**
	 * Tests to see if there are any empty or duplicate unique keys in the
	 * passed-in EntityNode, and if so, removes them. This is typically called
	 * when removing an attribute to fix up the EntityNode, since the attribute
	 * will have also been removed from the existing unique keys.
	 *
	 * @param inNode
	 *            The node whose unique keys should be cleaned
	 * @return true if any unique keys were removed
	 */
	public boolean cleanup() {
		final List<UniqueKey<F, GM, M, N, E>> badKeys = findBadKeys();

		if (badKeys.isEmpty()) {
			return false;
		}

		for (final UniqueKey<F, GM, M, N, E> remove : badKeys) {
			removeUniqueKey(remove);
		}

		return true;
	}

	/**
	 * Returns the tree node used to display this EntityNode
	 *
	 * @return the treenode
	 */
	public DefaultMutableTreeNode getTreeNode() {
		return _treeNode;
	}

	/**
	 * Returns the DefaultPort object that edges pointing to this vertex should
	 * be attached to.
	 *
	 * @return
	 */
	public DefaultPort getPort() {
		return _port;
	}

	/**
	 * Overrides superclass getAttributes to override selectable status.
	 *
	 * @return
	 */
	@Override
	public AttributeMap getAttributes() {
		AttributeMap attrs = super.getAttributes();

		// Always selectable:
		GraphConstants.setSelectable(attrs, true);

		return attrs;
	}

	/**
	 * Determines whether a set of attributes would enforce new uniqueness on
	 * this entity, or whether uniqueness of those attributes is already
	 * enforced by an existing unique key. Essentially, this boils down to two
	 * things: there can be no existing unique key that is a duplicate of the
	 * specified elements, and there can be no unique key that is a subset of
	 * the specified elements. The latter is because if a unique key on (A, B)
	 * already exists, then (A, B, C) will be already guaranteed to be unique
	 * because the (A, B) part has to be unique.
	 *
	 * @param inElems
	 *            The list/set/collection/array of attributes/edges to check
	 * @return the unique key that already exists that enforces the uniqueness,
	 *         or null if the specified items would form new uniqueness.
	 */
	public UniqueKey<F, GM, M, N, E> uniqueKeyOn(final Collection<UniqueIndexable> inElems) {
		return uniqueKeyOn(new HashSet<>(inElems));
	}

	/**
	 * Array or multi-value version of uniqueKeyOn(...)
	 *
	 * @param inElems
	 *            The list/set/collection/array of attributes/edges to check
	 * @return the unique key that already exists that enforces the uniqueness,
	 *         or null if the specified items would form new uniqueness.
	 */
	public UniqueKey<F, GM, M, N, E> uniqueKeyOn(final UniqueIndexable... inElems) {
		return uniqueKeyOn(Arrays.asList(inElems));
	}

	/**
	 * Version of uniqueKeyOn() that takes a set of UniqueIndexable edges.
	 *
	 * @param inElemSet
	 *            The list/set/collection/array of attributes/edges to check
	 * @return the unique key that already exists that enforces the uniqueness,
	 *         or null if the specified items would form new uniqueness.
	 */
	public UniqueKey<F, GM, M, N, E> uniqueKeyOn(final Set<UniqueIndexable> inElemSet) {
		for (final UniqueKey<F, GM, M, N, E> q : getUniqueKeys()) {
			if (inElemSet.containsAll(q.getElements())) {
				return q;
			}
		}

		return null;
	}

}
