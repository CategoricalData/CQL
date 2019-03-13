package easik.sketch.edge;

//~--- non-JDK imports --------------------------------------------------------

import org.jgraph.graph.AttributeMap;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultPort;
import org.jgraph.graph.GraphConstants;

import easik.model.edge.ModelEdge;
import easik.overview.vertex.ViewNode;
import easik.sketch.Sketch;
import easik.sketch.util.graph.SketchGraphModel;
import easik.sketch.vertex.EntityNode;
import easik.ui.SketchFrame;

/**
 * This is an abstract class to represent an arrow between two nodes. Subclasses
 * of this class represent edges supported in Easik (currently injective,
 * partial, and normal).
 */
public abstract class SketchEdge extends ModelEdge<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> {
	/**
	 *    
	 */
	private static final long serialVersionUID = 5526354785981169589L;

	/**
	 * The entity nodes that acts as this edge's source and target
	 */
	private EntityNode _sourceObj, _targetObj;

	/**
	 * Stores the name of this edge
	 */
	private String _uniqueName;

	/**
	 * Creates a new edge between two entity nodes with the specified name.
	 *
	 * @param source
	 *            The source entity node
	 * @param target
	 *            The target entity node
	 * @param name
	 *            The unique edge identifier
	 * @param cascade
	 *            The Cascade option for this edge (Cascade.CASCADE,
	 *            Cascade.RESTRICT, Cascade.SET_NULL)
	 * @see NormalEdge
	 * @see easik.sketch.edge.InjectiveEdge
	 * @see easik.sketch.edge.PartialEdge
	 */
	protected SketchEdge(final EntityNode source, final EntityNode target, final String name, final Cascade cascade) {
		super(source, target);
		_sourceObj = source;
		_targetObj = target;
		_uniqueName = name;

		setCascading(cascade);
	}

	/**
	 * Getter method for the source EntityNode of this sketch edge
	 * 
	 * @return The source of this edge, defined to be an entityNode.
	 */
	@Override
	public EntityNode getSourceEntity() {
		return _sourceObj;
	}

	/**
	 * Getter method for the target of this sketch edge
	 * 
	 * @return The target of this edge, defined to be an entityNode.
	 */
	@Override
	public EntityNode getTargetEntity() {
		return _targetObj;
	}

	/**
	 * Returns the source port of this edge.
	 *
	 * @see DefaultEdge#getSource()
	 *
	 * @return
	 */
	@Override
	public DefaultPort getSource() {
		return _sourceObj.getPort();
	}

	/**
	 * Returns the target port of this edge.
	 *
	 * @see DefaultEdge#getSource()
	 *
	 * @return
	 */
	@Override
	public DefaultPort getTarget() {
		return _targetObj.getPort();
	}

	/**
	 * toString method returns the common name.
	 * 
	 * @return Name of the edge
	 */
	@Override
	public String toString() {
		return getName();
	}

	/**
	 * Accessor for the unique ID
	 * 
	 * @return The unique ID
	 */
	public String getName() {
		return _uniqueName;
	}

	/**
	 * Sets the name of this edge.
	 * 
	 * @param inName
	 *            The unique name of the edge.
	 */
	public void setName(String inName) {
		final String oldName = getName();

		inName = _sourceObj.getMModel().edgeRenamed(this, oldName, inName);
		for (ViewNode v : _sourceObj.getMModel().getViews()) {
			if (v.getMModel().getEdges().containsKey(oldName)) {
				v.getMModel().getEdges().get(oldName).setName(inName);
			}
		}
		_uniqueName = inName;

		_sourceObj.getMModel().refresh(this);
	}

	/**
	 * Accessor for the injective attribute. For a SketchEdge object, this
	 * always return false, however injective edge subclasses override this.
	 *
	 * @return true if this edge is an injective edge (one-to-one); false
	 *         otherwise
	 */
	@Override
	public boolean isInjective() {
		return false;
	}

	/**
	 * Accessor for determing whether this edge is a partial map. For a
	 * SketchEdge object, this always return false, however partial edge
	 * subclasses may override this.
	 *
	 * @return true if this edge is a partial map edge; false otherwise
	 */
	@Override
	public boolean isPartial() {
		return false;
	}

	/**
	 * Set the cascade mode to the passed-in mode. Note that
	 * <code>Cascade.SET_NULL</code> will be ignored on a non-partial edge type
	 * (if being called via the constructor, this means the edge will end up
	 * with the default, <code>Cascade.RESTRICT</code>).
	 * 
	 * @param mode
	 *            the cascade mode
	 */
	@Override
	public void setCascading(final Cascade mode) {
		if ((mode == Cascade.SET_NULL) && !isPartial()) {
			return;
		}

		_cascadeMode = mode;
	}

	/**
	 * Returns the cascade mode for this edge.
	 * 
	 * @return the cascade
	 */
	@Override
	public Cascade getCascading() {
		return _cascadeMode;
	}

	/**
	 * Override the super getAttributes() to reflect whether or not this edge is
	 * selectable.
	 *
	 * @return
	 */
	@Override
	public AttributeMap getAttributes() {
		final AttributeMap attrs = super.getAttributes();

		// A SketchEdge is selectable unless we're in manipulation mode
		final Sketch sketch = _sourceObj.getMModel();

		GraphConstants.setSelectable(attrs, sketch.getFrame().getMode() != SketchFrame.Mode.MANIPULATE);

		return attrs;
	}

	/**
	 *
	 *
	 * @param pattern
	 *
	 * @return
	 */
	public String getForeignKeyName(final String pattern) {
		return pattern.replaceAll("<target>", getTargetEntity().getName()).replaceAll("<source>", getSourceEntity().getName()).replaceAll("<edge>", getName());
	}
}
