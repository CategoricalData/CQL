/**
 * 
 */
package easik.model.edge;

import org.jgraph.graph.DefaultEdge;

import easik.graph.EasikGraphModel;
import easik.model.Model;
import easik.model.ui.ModelFrame;
import easik.model.vertex.ModelVertex;
import easik.sketch.edge.NormalEdge;

/**
 * Will be extended by View_Edge and SketchEdge
 * 
 * Contains methods used for generics. For example isInjective() (among others)
 * which allows ModelPath to work with either a View_Edge or a SketchEdge and
 * call these methods
 * 
 * @author Federico Mora
 *
 */
public abstract class ModelEdge<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>> extends DefaultEdge {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3634146223766365754L;

	/**  */
	protected Cascade _cascadeMode = Cascade.RESTRICT;

	/**
	 * The entity nodes that acts as this edge's source and target
	 */
	private N _sourceObj, _targetObj;

	/**
	 * The available cascade modes: CASCADE, RESTRICT, and SET_NULL.
	 *
	 * CASCADE does cascading deletion on updates and deletions (actually, in
	 * Easik, since foreign keys always point to primary keys, update cascading
	 * won't actually happen).
	 *
	 * RESTRICT prevents updates or deletions that would violate a foreign key
	 * constraint.
	 *
	 * SET_NULL sets referencing foreign keys to NULL when a row is deleted.
	 * This only works for partial map edges.
	 *
	 * @see easik.sketch.edge.SketchEdge#setCascading(easik.sketch.edge.SketchEdge.Cascade)
	 * @see easik.sketch.edge.SketchEdge#getCascading()
	 */
	public enum Cascade {
		CASCADE, RESTRICT, SET_NULL
	}

	;

	/**
	 * Creates a new edge between two entity nodes with the specified name.
	 *
	 * @param source
	 *            The source entity N
	 * @param target
	 *            The target entity N
	 * @param name
	 *            The unique edge identifier
	 * @param cascade
	 *            The Cascade option for this edge (Cascade.CASCADE,
	 *            Cascade.RESTRICT, Cascade.SET_NULL)
	 * @see NormalEdge
	 * @see easik.sketch.edge.InjectiveEdge
	 * @see easik.sketch.edge.PartialEdge
	 */
	protected ModelEdge(final N source, final N target) {
		_sourceObj = source;
		_targetObj = target;
	}

	/**
	 * Getter method for the source EntityNode of this sketch edge
	 * 
	 * @return The source of this edge, defined to be an entityNode.
	 */
	public N getSourceEntity() {
		return _sourceObj;
	}

	/**
	 * Getter method for the target of this sketch edge
	 * 
	 * @return The target of this edge, defined to be an entityNode.
	 */
	public N getTargetEntity() {
		return _targetObj;
	}

	/**
	 * Accessor for the injective attribute. Must be extended below.
	 */
	public abstract boolean isInjective();

	/**
	 * Accessor for determing whether this edge is a partial map. For a
	 * SketchEdge object, this always return false, however partial edge
	 * subclasses may override this.
	 *
	 * @return true if this edge is a partial map edge; false otherwise
	 */
	@SuppressWarnings("static-method")
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
	public Cascade getCascading() {
		return _cascadeMode;
	}

}
