package easik.model.constraint;

//~--- non-JDK imports --------------------------------------------------------
//~--- JDK imports ------------------------------------------------------------
import java.util.ArrayList;
import java.util.List;

import easik.graph.EasikGraphModel;
import easik.model.Model;
import easik.model.attribute.EntityAttribute;
import easik.model.edge.ModelEdge;
import easik.model.path.ModelPath;
import easik.model.ui.ModelFrame;
import easik.model.vertex.ModelVertex;

/**
 * The commutative diagram is a constraint made up of a list of lists of sketch
 * edges. These lists share a domain and codomain.
 * 
 * @version 06-2014 Federico Mora
 */
public class CommutativeDiagram<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>> extends ModelConstraint<F, GM, M, N, E> {
	/**
	 *    
	 */
	private static final long serialVersionUID = 6526668464897655851L;

	/**
	 * Creates a commutative diagram.
	 *
	 * @param inPaths
	 *            List of paths to be used for the commutative diagram
	 * @param inModel
	 *            the sketch to which this CD belongs
	 */
	public CommutativeDiagram(List<ModelPath<F, GM, M, N, E>> inPaths, M inModel) {
		super(inModel);

		_paths = new ArrayList<>(inPaths);

		addEdges();

		_isVisible = true;

		setName("CD");
		assignToEntities();
	}

	/**
	 * Creates a commutative diagram from a List of paths. Also sets details of
	 * the visual representation.
	 *
	 * @param inPaths
	 *            List of paths to be used for the commutative diagram
	 * @param x
	 *            x coordinate
	 * @param y
	 *            y coordinate
	 * @param isVisible
	 *            If true the constraint will be shown in the graph, otherwise
	 *            it will not be visible.
	 * @param inModel
	 *            the sketch to which this CD belongs
	 */
	public CommutativeDiagram(List<ModelPath<F, GM, M, N, E>> inPaths, int x, int y, boolean isVisible, M inModel) {
		super("CD", x, y, isVisible, inModel);

		_paths = new ArrayList<>(inPaths);

		addEdges();
		assignToEntities();
	}

	/**
	 * Constructor that accepts ID. Used by views to match with corresponding
	 * sketch constraints.
	 * 
	 * @param inPaths
	 * @param inModel
	 * @param id
	 */
	public CommutativeDiagram(ArrayList<ModelPath<F, GM, M, N, E>> inPaths, M inModel, int id) {
		super(inModel, id);

		_paths = new ArrayList<>(inPaths);

		addEdges();

		_isVisible = true;

		setName("CD");
		assignToEntities();
	}

	/**
	 * Sets the path array, updates edge list, and updates display. This method
	 * should not be called without first checking whether the new paths are
	 * valid, using CommutativeDiagram.isCommutativeDiagram(paths)!
	 *
	 * @param inPaths
	 *            The new array of paths
	 */
	public void setPaths(List<ModelPath<F, GM, M, N, E>> inPaths) {
		_paths = new ArrayList<>(inPaths);

		addEdges();
		setVisible(!isVisible());
		setVisible(!isVisible());
	}

	@Override
	public void addEntityAttribute(EntityAttribute<F, GM, M, N, E> inAtt) {
		// does nothing
	}

	@Override
	/**
	 * Commutative diagrams don't have projection paths so return null
	 */
	public ArrayList<ModelPath<F, GM, M, N, E>> getProjectionPaths() {
		return null;
	}
}
