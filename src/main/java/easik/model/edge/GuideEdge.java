package easik.model.edge;

//~--- non-JDK imports --------------------------------------------------------

import org.jgraph.graph.AttributeMap;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.GraphConstants;

import easik.graph.EasikGraphModel;
import easik.model.Model;
import easik.model.constraint.ModelConstraint;
import easik.model.ui.ModelFrame;
import easik.model.vertex.ModelVertex;

/**
 * The GuideEdge is a directed edge which is used only as a visual aid to the
 * graph drawer. It is rendered differently than the traditional edge, and
 * allows constraints to show which entities it applies to. Technically it
 * should point to arrows, not nodes, but that might be a bit tricky.
 *
 * @author Rob Fletcher 2005
 * @author Kevin Green 2006
 * @version 2006-07-06 Kevin Green
 * @version 06-2014 Federico Mora
 */
public class GuideEdge<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>> extends DefaultEdge {
	/**
	 *    
	 */
	private static final long serialVersionUID = -9189681231391848983L;

	/**
	 * Stores whether this edge is highlighted
	 */
	private boolean _isHighlighted;

	/**
	 * Creates a guide edge between a constraint vertex and a SketchNode. The
	 * edge will default to highlighted if the node is the domain or codomain of
	 * a path of the constraint, non-highlighted otherwise.
	 *
	 * @param con
	 *            the Constraint object
	 * @param node
	 *            the EntityNode
	 */
	public GuideEdge(ModelConstraint<F, GM, M, N, E> con, N node) {
		_isHighlighted = con.isDomainOrCoDomain(node);

		setSource(con.getPort());
		setTarget(node.getPort());
	}

	/**
	 * Gets whether the guide edge is highlighted or not
	 * 
	 * @return true if the edge is highlighted, false otherwise
	 */
	public boolean isHighlighted() {
		return _isHighlighted;
	}

	/**
	 * Sets the edge as highlighted or not
	 * 
	 * @param isHighlighted
	 *            true if the edge is highlighted, false if not.
	 */
	public void setHighlighted(boolean isHighlighted) {
		_isHighlighted = isHighlighted;
	}

	/**
	 *
	 *
	 * @return
	 */
	@Override
	public String toString() {
		return "";
	}

	/**
	 *
	 *
	 * @return
	 */
	@Override
	public AttributeMap getAttributes() {
		AttributeMap attrs = super.getAttributes();

		// A GuideEdge is never selectable
		GraphConstants.setSelectable(attrs, false);

		return attrs;
	}
}
