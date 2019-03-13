package easik.sketch.vertex;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.undo.AbstractUndoableEdit;

import easik.Easik;
import easik.model.constraint.EqualizerConstraint;
import easik.model.constraint.LimitConstraint;
import easik.model.constraint.ModelConstraint;
import easik.model.constraint.ProductConstraint;
import easik.model.constraint.PullbackConstraint;
import easik.model.constraint.SumConstraint;
import easik.model.keys.UniqueIndexable;
import easik.model.keys.UniqueKey;
import easik.model.path.ModelPath;
import easik.model.vertex.ModelVertex;
import easik.sketch.Sketch;
import easik.sketch.edge.SketchEdge;
import easik.sketch.util.graph.SketchGraphModel;
import easik.ui.SketchFrame;
import easik.xml.xsd.nodes.constraints.XSDKey;
import easik.xml.xsd.nodes.elements.XSDElement;
import easik.xml.xsd.nodes.types.XSDType;

/**
 * An entity node represents a table in a db. It has a name, attributes, and
 * unique keys. This class keeps track of all these elements.
 */
public class EntityNode extends ModelVertex<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> {
	/**
	 * F *
	 */
	private static final long serialVersionUID = -284442112529134937L;

	/**  */
	private XSDElement xsdElement;

	/**  */
	private XSDType xsdType;

	/**
	 * Creates a new entity node with the name provided at position (0,0).
	 *
	 * @param nodeName
	 *            The name of the new node
	 * @param inSketch
	 *            the sketch
	 */
	public EntityNode(final String nodeName, final Sketch inSketch) {
		this(nodeName, 0, 0, inSketch);
	}

	/**
	 * Creates a new enity node with the name provided. Stores visual
	 * representation information.
	 *
	 * @param nodeName
	 *            Name of the new node
	 * @param x
	 *            X Coordinate of the new node
	 * @param y
	 *            Y Coordinate of the new node
	 * @param inSketch
	 *            the sketch
	 */
	public EntityNode(final String nodeName, final int x, final int y, final Sketch inSketch) {
		super(nodeName, x, y, inSketch);

		_treeNode = new DefaultMutableTreeNode(this);

		_treeNode.add(_attribNode);
		_treeNode.add(_keyNode);
	}

	/**
	 * Changes the name of the EntityNode. This also updates the name in the
	 * EntityNode's SketchFrame, to maintain consistency. Note that if you
	 * attempt to rename an EntityNode to one that already exists in the same
	 * frame, a trailing number will be added to make the name unique. This
	 * means, however, that the name might not end up being what you set--if
	 * that's a problem, make sure the new name isn't taken yet yourself, via
	 * SketchFrame.getEntity().
	 *
	 * @param newName
	 *            the new name to attempt to set. The actual name set may have a
	 *            trailing number appended/incremented.
	 * @see easik.ui.SketchFrame
	 */
	@Override
	public void setName(String newName) {
		final String oldName = getName();

		// The name might have to change, if there are duplicates. nodeRenamed
		// tells us the final name to use:
		newName = getMModel().nodeRenamed(this, oldName, newName);

		super.setName(newName);
	}

	/**
	 * Returns a Set of SketchEdges of the outgoing edges of this EntityNode.
	 * Note that edges for constraints are <b>not</b> included.
	 *
	 * @return Set of outgoing edges from this EntityNode
	 */
	@Override
	public Set<SketchEdge> getOutgoingEdges() {
		@SuppressWarnings("rawtypes")
		final List outEdges = _theModel.getGraphLayoutCache().getOutgoingEdges(this, Collections.emptySet(), false, true);
		final Set<SketchEdge> outgoing = new LinkedHashSet<>(outEdges.size());

		for (final Object out : outEdges) {
			if (out instanceof SketchEdge) {
				outgoing.add((SketchEdge) out);
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
	@Override
	public Set<UniqueIndexable> getIndexableEdges() {
		final Set<SketchEdge> edges = getOutgoingEdges();
		final Set<UniqueIndexable> attribs = new LinkedHashSet<>(edges.size());

		for (final SketchEdge edge : edges) {
			if (edge instanceof UniqueIndexable) {
				attribs.add((UniqueIndexable) edge);
			}
		}

		return attribs;
	}

	/**
	 * Add a constraint to the set of constraints of which this node is a part
	 *
	 * @param con
	 *            the constraint
	 */
	@Override
	public void addConstraint(final ModelConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> con) {
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
	 * Gets the set of constraints of which this node is a part
	 *
	 * @return the constraints as an unmodifiable set
	 */
	@Override
	public Set<ModelConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge>> getConstraints() {
		return Collections.unmodifiableSet(_constraints);
	}

	/**
	 * Returns the edges that might need to be "shadowed". This is used by
	 * various constraints where we need to do automatic path insertion. For
	 * example, sum constraints, where we have something like A -&gt; B -&gt; C,
	 * where A -&gt; B is the sum, but because B is auto-created by the
	 * insertion into A, we have to specify the B -&gt; C foreign key when
	 * inserting into A.
	 *
	 * @return LinkedList of SketchEdges that are not-null (in other words,
	 *         non-partial edges)
	 */
	public Collection<SketchEdge> getNonPartialEdges() {
		final Collection<SketchEdge> edges = new LinkedList<>();

		for (final SketchEdge e : getOutgoingEdges()) {
			if (!e.isPartial()) {
				edges.add(e);
			}
		}

		return edges;
	}

	/**
	 * This method returns the edges that will be "shadowed" in this entity for
	 * allowing various types of constraints. The problem arises when we have
	 * something like: A -&gt; B -&gt; C, where A is the summand of B, but B has
	 * to be specified. In this case, the B to C edge will be returned as a
	 * "shadow" edge. We handle this for other constraint types, too. For a
	 * good, working, technical example, see the shadowEdges.easik sample
	 * sketch.
	 *
	 * @return a set of edges that will be shadowed by this entity node.
	 * 
	 *         Removing shadow edges completely. Started by Sarah Van der Laan
	 *         continued by Federico Mora because a partial solution is worse
	 *         than all or nothing
	 * 
	 *         public LinkedHashSet<SketchEdge> getShadowEdges() { return
	 *         getShadowEdges(new LinkedHashSet<EntityNode>(5), new
	 *         LinkedHashSet<SketchEdge>(5)); }
	 */

	// Package-only implementation of the above that breaks recursion by
	// ignoring
	// shadowed nodes that we already know about.

	/**
	 *
	 *
	 * @param ignore
	 * @param constraintEdges
	 *
	 * @return
	 */
	LinkedHashSet<SketchEdge> getShadowEdges(final Collection<EntityNode> ignore, final LinkedHashSet<SketchEdge> constraintEdges) {
		// These are the other entity node that we (potentially) need to shadow:
		final Collection<EntityNode> shadow = new LinkedHashSet<>(10);

		CONSTRAINT: for (final ModelConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> c : getMModel().getConstraints().values()) {
			if (c instanceof SumConstraint) {
				final SumConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> s = (SumConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge>) c;

				for (final ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> path : s.getPaths()) {
					// If this entity is the domain of a sum constraint path,
					// we need to include every entity along that path (except
					// for this entity, of course):
					if (path.getDomain() == this) {
						shadow.addAll(path.getEntities());
						constraintEdges.addAll(path.getEdges());

						continue CONSTRAINT;
					}
				}
			} else if (c instanceof ProductConstraint) {
				final ProductConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> p = (ProductConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge>) c;

				for (final ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> path : p.getPaths()) {
					// If this entity is the codomain of a product constraint
					// path,
					// we need to include every entity, excluding the
					// codomains), along each product path
					if (path.getCoDomain() == this) {
						for (final ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> prodPath : p.getPaths()) {
							// But we ignore all of the product path edges,
							// since they will be automatically generated:
							constraintEdges.addAll(prodPath.getEdges());

							final Deque<EntityNode> pathNodes = new LinkedList<>();

							pathNodes.addAll(prodPath.getEntities());
							pathNodes.removeLast();
							shadow.addAll(pathNodes);
						}

						continue CONSTRAINT;
					}
				}
			} else if (c instanceof EqualizerConstraint) {
				final EqualizerConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> e = (EqualizerConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge>) c;

				// If this entity is the domain of an equalizer, we need to
				// shadow the projection entity.
				// We actually shadow every node along the projection path,
				// which will handle multi-edge
				// projection paths, though EASIK doesn't actually support those
				// (primarily because deletion
				// is needed; an injective *path* is fine for creation, but
				// cannot be cleanly deleted--an
				// injective *edge* doesn't cause that problem).
				if (e.getSourceEntity() == this) {
					final ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> projection = e.getProjection();

					shadow.addAll(projection.getEntities());

					// Ignore the projection edge itself:
					constraintEdges.addAll(projection.getEdges());

					continue CONSTRAINT;
				}
			} else if (c instanceof PullbackConstraint) {
				final PullbackConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> pb = (PullbackConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge>) c;

				// A pullback is a little trickier than the above: any insertion
				// into one
				// of the pullback path domains (i.e. projection codomains)
				// means we have
				// to shadow all the entities along *both* projection paths.
				// WPBEDIT CF2012
				for (int i = 0; i < pb.getWidth(); i++) {
					ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> proj = pb.getProjectionPath(i);

					if (this == proj.getCoDomain()) {
						for (int j = 0; j < pb.getWidth(); j++) {
							proj = pb.getProjectionPath(j);

							Deque<EntityNode> projNodes = new LinkedList<>(proj.getEntities());

							projNodes.removeLast();
							shadow.addAll(projNodes);
							constraintEdges.addAll(proj.getEdges());
						}

						continue CONSTRAINT;
					}
				}
			} else if (c instanceof LimitConstraint) {
				// TRIANGLES TODO CF2012 incomplete
			}
		}

		final LinkedHashSet<SketchEdge> shadowEdges = new LinkedHashSet<>(20);

		// All of the ignore entities, plus everything we just found should be
		// ignored by any recursion:
		final Collection<EntityNode> toIgnore = new LinkedHashSet<>(3);

		toIgnore.add(this);
		toIgnore.addAll(ignore);
		toIgnore.addAll(shadow);

		for (final EntityNode node : shadow) {
			// If it's ourself (which obviously doesn't need to be shadowed), or
			// a node
			// that a call up the stack is already shadowing the node, ignore
			// it:
			if ((node == this) || ignore.contains(node)) {
				continue;
			}

			// Otherwise, shadow its non-partial edges, and all of its shadow
			// edges:
			shadowEdges.addAll(node.getShadowEdges(toIgnore, constraintEdges));
			shadowEdges.addAll(node.getNonPartialEdges());
			shadowEdges.removeAll(constraintEdges); // Remove edges already
													// involved in the sum
		}

		return shadowEdges;
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
	@Override
	public UniqueKey<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> uniqueKeyOn(final Collection<UniqueIndexable> inElems) {
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
	@Override
	public UniqueKey<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> uniqueKeyOn(final UniqueIndexable... inElems) {
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
	@Override
	public UniqueKey<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> uniqueKeyOn(final Set<UniqueIndexable> inElemSet) {
		for (final UniqueKey<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> q : getUniqueKeys()) {
			if (inElemSet.containsAll(q.getElements())) {
				return q;
			}
		}

		return null;
	}

	/**
	 *
	 *
	 * @param pattern
	 *
	 * @return
	 */
	public String getPrimaryKeyName(final String pattern) {
		return pattern.replaceAll("<table>", getName());
	}

	/**
	 *
	 *
	 * @param appliesTo
	 *
	 * @return
	 */
	public XSDKey createXMLPrimaryKey(final XSDElement appliesTo) {
		final String idName = Easik.getInstance().getSettings().getProperty("xml_id_name");
		final boolean idIsAttribute = Boolean.valueOf(Easik.getInstance().getSettings().getProperty("xml_id_is_attribute"));

		if (idIsAttribute) {
			return new XSDKey(getXMLPrimaryKeyName(), appliesTo, '@' + idName);
		} 
			return new XSDKey(getXMLPrimaryKeyName(), appliesTo, idName);
		
	}

	/**
	 *
	 *
	 * @return
	 */
	public String getXMLPrimaryKeyName() {
		return getName() + "_PrimaryKey";
	}

	/**
	 *
	 *
	 * @return
	 */
	public XSDType getXsdType() {
		return xsdType;
	}

	/**
	 *
	 *
	 * @param xsdType
	 */
	public void setXsdType(final XSDType xsdType) {
		this.xsdType = xsdType;
	}

	/**
	 *
	 *
	 * @return
	 */
	public XSDElement getXsdElement() {
		return xsdElement;
	}

	/**
	 *
	 *
	 * @param xsdElement
	 */
	public void setXsdElement(final XSDElement xsdElement) {
		this.xsdElement = xsdElement;
	}

}

// WPBEDIT CF2012 EDIT
