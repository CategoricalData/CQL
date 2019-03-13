package easik.model.constraint;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import easik.graph.EasikGraphModel;
import easik.model.Model;
import easik.model.attribute.EntityAttribute;
import easik.model.edge.ModelEdge;
import easik.model.path.ModelPath;
import easik.model.ui.ModelFrame;
import easik.model.vertex.ModelVertex;

/**
 * Class representing an equalizer constraint. This requires 3 paths: one from
 * the equalizer node to the start node of the equalizer paths, which must be a
 * single, injective edge, and two from the start node to the same target node,
 * along different paths.
 *
 * @author Jason Rhinelander 2008
 * @version 06-2014 Federico Mora
 */

/*
 * To aid in visualizing the procedure, we describe paths as equalizer and
 * left/right; those are obviously arbitrary designations, but are based on this
 * sort of visual representation: E | A / \ B C \ / D
 *
 * where E->A is the equalizer path, A->B->D is the "left" path, and A->C->D is
 * the "right" path.
 */
public class EqualizerConstraint<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>> extends ModelConstraint<F, GM, M, N, E> {
	/**
	 *    
	 */
	private static final long serialVersionUID = 5446022931662474776L;

	/**
	 * Takes a List of paths, makes them an equalizer. The paths can be
	 * specified in any order, as long as they can be arranged into an
	 * equalizer. If the paths are invalid (that is, they cannot form an
	 * equalizer), a ConstraintException is thrown.
	 *
	 * @param inPaths
	 *            A List of ModelPath<F,GM,M,N,E>s
	 * @param inModel
	 *            the M this constraint belongs to
	 *
	 * @throws ConstraintException
	 */
	public EqualizerConstraint(List<ModelPath<F, GM, M, N, E>> inPaths, M inModel) throws ConstraintException {
		super(inModel);

		setName("EQ");

		_isVisible = true;

		initPaths(inPaths);
	}

	/**
	 * Takes an ArrayList of paths, with coordinates and a visible boolean,
	 * makes them an equalizer.
	 *
	 * @param paths
	 *            An ArrayList of ModelPath<F,GM,M,N,E>s
	 * @param x
	 *            X coordinate of visual aid
	 * @param y
	 *            Y coordinate of visual aid
	 * @param isVisible
	 *            If the constraint is visible in the graph or not
	 * @param inModel
	 *            the sketch
	 *
	 * @throws ConstraintException
	 */
	public EqualizerConstraint(List<ModelPath<F, GM, M, N, E>> paths, int x, int y, boolean isVisible, M inModel) throws ConstraintException {
		super("EQ", x, y, isVisible, inModel);

		initPaths(paths);
		// assign equalizer as dependent of source
		getSourceEntity().addDepend(getEqualizerEntity());
	}

	/**
	 * Constructor that accepts ID. Used by views to match with corresponding
	 * sketch constraints.
	 * 
	 * @param inPaths
	 * @param inModel
	 * @param id
	 * @throws ConstraintException
	 */
	public EqualizerConstraint(ArrayList<ModelPath<F, GM, M, N, E>> inPaths, M inModel, int id) throws ConstraintException {
		super(inModel, id);

		setName("EQ");

		_isVisible = true;

		initPaths(inPaths);
	}

	/**
	 * Common path verification shared by constructors
	 *
	 * @param inPaths
	 *
	 * @throws ConstraintException
	 */
	private void initPaths(List<ModelPath<F, GM, M, N, E>> inPaths) throws ConstraintException {
		_paths = new ArrayList<>(inPaths);

		if (!_theModel.isEqualizerConstraint(_paths)) // This method rearranges
														// _paths
		{
			throw new ConstraintException("Unable to create equalizer constraint: specified paths do not form a valid equalizer constraint");
		}

		addEdges();
		assignToEntities();
	}

	/**
	 * Returns the equalizer node.
	 *
	 * @return N the equalizer node
	 */
	public N getEqualizerEntity() {
		return _paths.get(0).getDomain();
	}

	/**
	 * Returns the "source" node of the equalizer--that is, the domain of both
	 * equalizer paths.
	 *
	 * @return N the source node
	 */
	public N getSourceEntity() {
		return _paths.get(1).getDomain();
	}

	/**
	 * Returns the "target" node of the equalizer (that is, the codomain of both
	 * equalizer paths).
	 *
	 * @return N the equalizer target node
	 */
	public N getTargetEntity() {
		return _paths.get(1).getCoDomain();
	}

	/**
	 * Returns the path containing the injective edge from the equalizer node to
	 * the source node.
	 *
	 * @return
	 */
	public ModelPath<F, GM, M, N, E> getProjection() {
		return _paths.get(0);
	}

	/**
	 * Returns the path containing the injective edge from the equalizer node to
	 * the source node.
	 *
	 * @return
	 */
	@Override
	public ArrayList<ModelPath<F, GM, M, N, E>> getProjectionPaths() {
		ArrayList<ModelPath<F, GM, M, N, E>> pp = new ArrayList<>();
		pp.add(_paths.get(0));
		return pp;
	}

	/**
	 * Returns the paths from the source node to target node.
	 *
	 * @return
	 */
	public List<ModelPath<F, GM, M, N, E>> getEqualizerPaths() {
		LinkedList<ModelPath<F, GM, M, N, E>> eqPaths = new LinkedList<>(_paths);

		eqPaths.removeFirst();

		return Collections.unmodifiableList(eqPaths);
	}

	@Override
	public void addEntityAttribute(EntityAttribute<F, GM, M, N, E> inAtt) {
		// does nothing
	}
}
