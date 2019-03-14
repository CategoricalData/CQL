package easik.view.vertex;

//~--- non-JDK imports --------------------------------------------------------

import java.util.ArrayList;
import java.util.List;

//~--- JDK imports ------------------------------------------------------------
import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.undo.AbstractUndoableEdit;

import easik.database.types.EasikType;
import easik.model.attribute.EntityAttribute;
import easik.model.vertex.ModelVertex;
import easik.sketch.Sketch;
import easik.sketch.edge.SketchEdge;
import easik.sketch.util.graph.SketchGraphModel;
import easik.sketch.vertex.EntityNode;
import easik.ui.SketchFrame;
import easik.ui.ViewFrame;
import easik.view.View;
import easik.view.edge.View_Edge;
import easik.view.util.QueryException;
import easik.view.util.graph.ViewGraphModel;

/**
 * A query node represents a query on a db.
 *
 * @author updated by Sarah van der Laan 2013
 * @author updated by Federico Mora 2014
 */
public class QueryNode extends ModelVertex<ViewFrame, ViewGraphModel, View, QueryNode, View_Edge> {
	private static final long serialVersionUID = -5432610819775420277L;

	/**
	 * The Columns being selected this node queries Federico Mora 2014
	 */
	private String _queriedColumns;

	/**
	 * The statement following the where clause Federico Mora 2014
	 */
	private String _whereStatement;

	/**
	 * The EntityNode this node queries Federico Mora 2014
	 */
	private EntityNode _queriedEntityNode;

	/**
	 * Is the queryNode updateable? Federico Mora 2014
	 */
	private boolean updateable = true;

	/**
	 * An optional update to the view Sarah van der Laan 2013
	 */
	private String _update;

	/**
	 * Node representing the attributes for the information tree Federico Mora 2014
	 */
	@SuppressWarnings("hiding")
	private DefaultMutableTreeNode _attribNode = new DefaultMutableTreeNode("Attributes");

	/** The sketch this view is on */
	private Sketch _ourSketch;

	/**
	 * Creates a new query node with the name provided.
	 *
	 * @param nodeName The name of the new node
	 * @param inView   The frame containing this node
	 * @param inQuery  The query represented by this node
	 * @throws QueryException
	 */
	public QueryNode(String nodeName, View inView, String inQuery) throws QueryException {
		this(nodeName, 0, 0, inView, inQuery);

	}

	/**
	 * Creates a new query node with the name provided. Stores visual representation
	 * information.
	 *
	 * @param nodeName Name of the new node
	 * @param x        X Coordinate of the new node
	 * @param y        Y Coordinate of the new node
	 * @param inView   The frame in which this node exists
	 * @param inQuery  The query represented by this node
	 * @throws QueryException
	 */
	public QueryNode(String nodeName, int x, int y, View inView, String inQuery) throws QueryException {
		super(nodeName, x, y, inView);
		_ourSketch = inView.getSketch();

		processQuery(inQuery);
		_treeNode = new DefaultMutableTreeNode(this);

		_treeNode.add(_attribNode);

	}

	/**
	 *
	 *
	 * @return
	 */
	@Override
	public DefaultMutableTreeNode getAttributeNode() {
		return _attribNode;
	}

	/**
	 * Processes the inQuery, assigning to instance variables
	 *
	 * @param String inQuery of the new node
	 * @author Federico Mora
	 * @throws QueryException
	 */
	private void processQuery(String inQuery) throws QueryException {
		if (inQuery.isEmpty()) {
			return;
		}

		boolean warnings = _ourSketch.useWarnings();
		String oldEntityNodeName = "";

		if (_queriedEntityNode != null) {
			oldEntityNodeName = _queriedEntityNode.getName();
		}

		String entityNodeName = null;
		String errMess = "";
		String tempWhere = "";
		ArrayList<String> tempQColumns = new ArrayList<>();
		EntityNode tempNode = null;
		boolean update = true;
		int fromToken = 0;

		String[] tokens = inQuery.split("[,\\s]+");

		if (!tokens[0].toUpperCase().equals("SELECT")) {
			// tell user has to start with Select
			throw new QueryException("Query must start with select");
		}

		for (int i = 0; i < tokens.length; i++) {
			if (tokens[i].equalsIgnoreCase("from")) {
				fromToken = i;
				entityNodeName = tokens[i + 1];
			}
		}

		// get everything between Select and from
		for (int i = 1; i < fromToken; i++) {
			tempQColumns.add(tokens[i]);
		}

		if (!tempQColumns.contains("*") && tempQColumns.size() == 1) {
			// warn user that this may not be updatable
			update = false;
			errMess += "View will not be updatebale if not Select * \n";
		}

		// get everything after the table being queried
		for (int i = fromToken + 2; i < tokens.length; i++) {
			tempWhere += tokens[i] + " ";
		}

		if (tempWhere.toUpperCase().startsWith("WHERE")) {
			// warn user against where clause
			update = false;
			errMess += "Views with WHERE queries not updatebale \n";
		} else if (!tempWhere.isEmpty()) {
			// error, can't query from more than one table
			throw new QueryException("Cannot query from more than one table");
		}

		boolean found = false;

		// set corresponding node in order to use
		for (EntityNode sketchNode : _ourSketch.getEntities()) {
			if (sketchNode.getName().equals(entityNodeName)) {
				// second part of if is so that when updating query it doesn't
				// throw exception
				// because it would think it was already being queried even
				// though it is the
				// same node.
				if (_theModel.getEntityNodePairs().containsKey(sketchNode)
						&& !sketchNode.getName().equals(oldEntityNodeName)) {
					// this node is already being queried. Not allowed.
					throw new QueryException("Entity Node is already being queried.");
				}
				tempNode = sketchNode;
				found = true;
			}
		}

		if (!found) {
			throw new QueryException("Entity node being queried does not exist");
		}

		if (!errMess.isEmpty() && warnings) {
			JOptionPane.showMessageDialog(_theModel,
					this.getMModel().getName() + ", due to node " + this.getName() + ": " + errMess, "Warning",
					JOptionPane.ERROR_MESSAGE);
		}

		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String s : tempQColumns) {
			if (!first) {
				sb.append(", ");
			}
			sb.append(s);
			first = false;
		}
		_queriedColumns = sb.toString();
		_whereStatement = tempWhere;
		_queriedEntityNode = tempNode;
		updateable = update;
		processAttributes();

	}

	/**
	 * Processes queried columns. A helper method to processQuery but can also be
	 * called publicly. It is called in AdAttributeAction.java so that when
	 * attributes are added to entities, their corresponding queryNodes are updated.
	 *
	 * @author Federico Mora
	 * @throws QueryException
	 */
	public void processAttributes() throws QueryException {
		List<EntityAttribute<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge>> entityAtts = _queriedEntityNode
				.getEntityAttributes();
		boolean alreadyAdded = false;

		ArrayList<String> tempQColumns = new ArrayList<>();
		for (String s : _queriedColumns.split("[,\\s]+")) {
			tempQColumns.add(s);
		}

		if (tempQColumns.contains("*") && tempQColumns.size() == 1) {
			for (EntityAttribute<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> ea : entityAtts) {
				alreadyAdded = false;
				for (EntityAttribute<ViewFrame, ViewGraphModel, View, QueryNode, View_Edge> vea : _entityAttributes) {
					if (vea.getName().equals(ea.getName())) {
						alreadyAdded = true;
					}
				}
				if (!alreadyAdded) {
					this.addEntityAttribute(ea.getName(), ea.getType());
				}
			}
		} else {
			// check if it exists
			boolean exists = false;
			for (String name : tempQColumns) {
				exists = false;
				for (EntityAttribute<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> ea : entityAtts) {
					if (ea.getName().equals(name)) {
						exists = true;
					}
				}
				if (!exists) {
					throw new QueryException("Column " + name + " doesn't exist!");
				}
			}
			// for every attribute that exists and is being queried add it
			// remove every other attribute
			for (EntityAttribute<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> ea : entityAtts) {
				if (tempQColumns.contains(ea.getName())) {
					alreadyAdded = false;
					for (EntityAttribute<ViewFrame, ViewGraphModel, View, QueryNode, View_Edge> vea : _entityAttributes) {
						if (vea.getName().equals(ea.getName())) {
							alreadyAdded = true;
						}
					}
					if (!alreadyAdded) {
						this.addEntityAttribute(ea.getName(), ea.getType());
					}
				} else {
					ArrayList<EntityAttribute<ViewFrame, ViewGraphModel, View, QueryNode, View_Edge>> tempEAs = new ArrayList<>(
							_entityAttributes);
					for (EntityAttribute<ViewFrame, ViewGraphModel, View, QueryNode, View_Edge> vea : tempEAs) {
						if (vea.getName().equals(ea.getName())) {
							this.removeEntityAttribute(vea);
						}
					}
				}
			}
		}

	}

	/**
	 * Creates a new
	 * EntityAttribute<ViewFrame,ViewGraphModel,View,QueryNode,View_Edge> and add
	 * its to the list of attributes
	 *
	 * @param inName          Name of attribute
	 * @param inAttributeType SQL EasikType of the attribute
	 */
	public void addEntityAttribute(final String inName, final EasikType inAttributeType) {
		addEntityAttribute(new EntityAttribute<>(inName, inAttributeType, this));
	}

	/**
	 * Creates a new
	 * EntityAttribute<ViewFrame,ViewGraphModel,View,QueryNode,View_Edge> and add
	 * its to the list of attributes
	 *
	 * @param inAtt The
	 *              EntityAttribute<ViewFrame,ViewGraphModel,View,QueryNode,View_Edge>
	 *              to add to this EntityNode.
	 */
	@Override
	public void addEntityAttribute(final EntityAttribute<ViewFrame, ViewGraphModel, View, QueryNode, View_Edge> inAtt) {
		_entityAttributes.add(inAtt);
		getAttributeNode().add(inAtt);
		_theModel.refresh();
		_theModel.getFrame().getInfoTreeUI().refreshTree(this);
		_theModel.getGraphModel().postEdit(new AbstractUndoableEdit() {
			/**
			 *            
			 */
			private static final long serialVersionUID = -8359488490185936367L;

			@Override
			public void undo() {
				super.undo();
				_entityAttributes.remove(inAtt);
				getAttributeNode().remove(inAtt);
				_theModel.refresh();
				_theModel.getFrame().getInfoTreeUI().refreshTree(QueryNode.this);
			}

			@Override
			public void redo() {
				super.redo();
				_entityAttributes.add(inAtt);
				getAttributeNode().add(inAtt);
				_theModel.refresh();
				_theModel.getFrame().getInfoTreeUI().refreshTree(QueryNode.this);
			}
		});
	}

	/**
	 * gets the query represented by this node. Note: There is no SQL syntax
	 * checking that takes place.
	 *
	 * @return String Query
	 */
	public String getQuery() {
		// if it hasn't been created just return an empty query
		if (_queriedColumns == null) {
			return "";
		}
		return "Select " + this.getSelect() + " From " + this.getQueriedEntity().getName() + " " + this.getWhere();
	}

	/**
	 * Sets the query represented by this node. Note: There is no SQL syntax
	 * checking that takes place.
	 *
	 * @param inQuery
	 * @throws QueryException
	 */
	public void setQuery(String inQuery) throws QueryException {
		processQuery(inQuery);
		_theModel.getGraphLayoutCache().reload();
		_theModel.repaint();
		_theModel.setDirty();
	}

	/**
	 * Gets the query represented by this node.
	 *
	 * @return
	 */
	public String getSelect() {
		return _queriedColumns;
	}

	/**
	 * Returns the query represented by this node. Note: There is no SQL syntax
	 * checking that takes place.
	 *
	 * @param inQuery
	 */
	public String getWhere() {
		if (_whereStatement == null) {
			return "";
		}
		return _whereStatement;
	}

	/**
	 * @author Sarah van der Laan 2013
	 * @param inUpdate
	 */
	public void setUpdate(String inUpdate) {
		_update = inUpdate;
	}

	/**
	 * Gets the update (if there is one) of the node
	 * 
	 * @author Sarah van der Laan 2013
	 *
	 * @return
	 */
	public String getUpdate() {
		return _update;
	}

	/**
	 * Changes the name of the QueryNode. This also updates the name in the
	 * QueryNode's ViewFrame, to maintain consistency. Note that if you attempt to
	 * rename a QueryNode to one that already exists in the same frame, a trailing
	 * number will be added to make the name unique. This means, however, that the
	 * name might not end up being what you set--if that's a problem, make sure the
	 * new name isn't taken yet yourself, via ViewFrame.getEntity().
	 *
	 * @param newName the new name to attempt to set. The actual name set may have a
	 *                trailing number appended/incremented.
	 */
	@Override
	public void setName(String newName) {
		// String oldName = getName();
		// The name might have to change, if there are duplicates. nodeRenamed
		// tells us the final name to use:
		// newName = getMModel().nodeRenamed(this, oldName, newName);

		super.setName(newName);
	}

	/**
	 * Sets the tree node used to display entity
	 *
	 * @param inNode The tree node used to display entity
	 */
	public void setNode(DefaultMutableTreeNode inNode) {
		_treeNode = inNode;
	}

	/**
	 * Returns the tree node used to display entity
	 *
	 * @return The tree node used to display entity
	 */
	public DefaultMutableTreeNode getNode() {
		return _treeNode;
	}

	/**
	 * Returns the EntityNode that is being queried
	 *
	 * @return return the EntityNode that is being queried
	 * @author Federico Mora
	 */
	public EntityNode getQueriedEntity() {
		return _queriedEntityNode;
	}

	/**
	 * Returns the state of updateability
	 *
	 * @return return updateable, which is true if we can update the queryNode
	 * @author Federico Mora
	 */
	public boolean isUpdateable() {
		return updateable && _ourSketch.editable(this.getQueriedEntity());
	}

	/**
	 * Sets updateable to true or false
	 * 
	 * @param inUpdateable, true if we can update
	 * @author Federico Mora public void setUpdateable(boolean inUpdateable) {
	 *         updateable = inUpdateable; }
	 */

	/**
	 * Removes an attribute from the list
	 *
	 * @param inAttribute The attribute to be removed
	 * @author Federico Mora
	 */
	@Override
	public void removeEntityAttribute(
			final EntityAttribute<ViewFrame, ViewGraphModel, View, QueryNode, View_Edge> inAttribute) {
		final SketchGraphModel model = _ourSketch.getGraphModel();

		model.beginUpdate();

		final int attPos = _entityAttributes.indexOf(inAttribute);
		final int nodePos = getAttributeNode().getIndex(inAttribute);

		_entityAttributes.remove(inAttribute);
		getAttributeNode().remove(inAttribute);
		_ourSketch.refresh();
		_theModel.refresh();
		_theModel.getFrame().getInfoTreeUI().refreshTree(this);
		_theModel.getGraphModel().postEdit(new AbstractUndoableEdit() {
			/**
			 *            
			 */
			private static final long serialVersionUID = -3013106807521458129L;

			@Override
			public void undo() {
				super.undo();
				_entityAttributes.add(attPos, inAttribute);
				getAttributeNode().insert(inAttribute, nodePos);
				_ourSketch.refresh();
				_theModel.refresh();
				_theModel.getFrame().getInfoTreeUI().refreshTree(QueryNode.this);
			}

			@Override
			public void redo() {
				super.redo();
				_entityAttributes.remove(inAttribute);
				getAttributeNode().remove(inAttribute);
				_theModel.refresh();
				_theModel.refresh();
				_theModel.getFrame().getInfoTreeUI().refreshTree(QueryNode.this);
			}
		});

		/**
		 * might be needed if we unique keys
		 * 
		 * // Remove references to the attribute from unique keys for (final UniqueKey
		 * curKey : _uniqueKeys) { curKey.removeElement(inAttribute); }
		 * 
		 * // Fix up any empty/duplicate unique keys resulting from the attribute
		 * removal UniqueKey.cleanup(this);
		 */
		model.endUpdate();
	}

}
