package easik.model.constraint;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.util.ArrayList;
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
 * Class representing a pullback constraint. This constraint was generalized to
 * allow for more than 2 paths to the target.
 *
 * To aid in visualizing the procedure, we describe paths as follows: _ P _ / \
 * i1 ... in \ T where "P" is the pullback, ij is the jth intermediate node, and
 * T is the target node.
 *
 * @author Kevin Green 2006
 * @author Vera Ranieri 2006
 * @author Jason Rhinelander 2008
 * @version Christian Fiddick Summer 2012
 * @version 06-2014 Federico Mora
 */
public class PullbackConstraint<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>>
    extends ModelConstraint<F, GM, M, N, E> {
  /**  */
  private static final long serialVersionUID = -377563390225014414L;

  /**
   * The 'width' of the constraint (the number of paths from the source to the
   * target)
   */
  private final int width; // calculated as size of path array / 2

  /**
   * Takes a List of paths, makes them a pullback. The paths can be specified in
   * any order, as long as they can be arranged into a pullback. If the paths are
   * invalid (that is, they cannot form a pullback), a ConstraintException is
   * thrown.
   *
   * @param inPaths A List of ModelPath<F,GM,M,N,E>s
   * @param inModel the M this constraint belongs to
   * @throws ConstraintException if the paths do not form a valid pullback
   *                             constraint
   */
  public PullbackConstraint(List<ModelPath<F, GM, M, N, E>> inPaths, M inModel) throws ConstraintException {
    super(inModel);

    width = inPaths.size() / 2;

    setName("PB");

    _isVisible = true;
    _paths = (ArrayList<ModelPath<F, GM, M, N, E>>) inModel.asPullbackConstraint(inPaths);

    if (_paths == null) // This method rearranges _paths
    {
      throw new ConstraintException(
          "Unable to create pullback constraint: specified paths do not form a valid pullback constraint");
    }

    addEdges();
    assignToEntities();
  }

  /**
   * Takes an ArrayList of paths, makes them a pullback.
   *
   * @param paths     An ArrayList of ModelPath<F,GM,M,N,E>s
   * @param x         X coordinate of visual aid
   * @param y         Y coordinate of visual aid
   * @param isVisible If the constraint is visible in the graph or not
   * @param inModel   the M this constraint belongs to
   *
   * @throws ConstraintException
   */
  public PullbackConstraint(ArrayList<ModelPath<F, GM, M, N, E>> paths, int x, int y, boolean isVisible, M inModel)
      throws ConstraintException {
    super("PB", x, y, isVisible, inModel);

    width = paths.size() / 2;
    _paths = (ArrayList<ModelPath<F, GM, M, N, E>>) inModel.asPullbackConstraint(paths);

    if (_paths == null) // This method rearranges _paths
    {
      throw new ConstraintException(
          "Unable to create pullback constraint: specified paths do not form a valid pullback constraint");
    }
    // add pullback to dependent node list of side nodes
    for (int i = 0; i < width; i++) {
      ModelPath<F, GM, M, N, E> p = getTargetPath(i);
      p.getDomain().addDepend(getSource());
    }
    addEdges();
    assignToEntities();
  }

  /**
   * Constructor that accepts ID. Used by views to match with corresponding sketch
   * constraints.
   * 
   * @param inPaths
   * @param inModel
   * @param id
   * @throws ConstraintException
   */
  public PullbackConstraint(ArrayList<ModelPath<F, GM, M, N, E>> inPaths, M inModel, int id)
      throws ConstraintException {
    super(inModel, id);

    width = inPaths.size() / 2;

    setName("PB");

    _isVisible = true;
    _paths = (ArrayList<ModelPath<F, GM, M, N, E>>) inModel.asPullbackConstraint(inPaths);

    if (_paths == null) // This method rearranges _paths
    {
      throw new ConstraintException(
          "Unable to create pullback constraint: specified paths do not form a valid pullback constraint");
    }

    addEdges();
    assignToEntities();
  }

  /**
   * The width of the pullback constraint getter.
   * 
   * @return Number of paths from source to target
   */
  public int getWidth() {
    return width;
  }

  /**
   * Returns the "source" node of the pullback--that is, the pullback node itself.
   * 
   * @return N the pullback node
   */
  public N getSource() {
    return _paths.get(0).getDomain();
  }

  /**
   * Returns the "target" node of the pullback (that is, the codomain of both
   * pullback paths).
   * 
   * @return N the pullback target node
   */
  public N getTarget() {
    return _paths.get(_paths.size() - 1).getCoDomain();
  }

  /**
   * Returns the projection path from the pullback to the ith source node.
   *
   * @param i
   * @return The ith projection path if it exists, null otherwise
   */
  public ModelPath<F, GM, M, N, E> getProjectionPath(int i) {
    if (!validPathNumber(i)) {
      return null;
    }

    return _paths.get(i * 2);
  }

  /**
   * Returns the target path from the ith source node to the target node.
   *
   * @param i
   * @return The ith target path if it exists, null otherwise
   */
  public ModelPath<F, GM, M, N, E> getTargetPath(int i) {
    if (!validPathNumber(i)) {
      return null;
    }

    return _paths.get(i * 2 + 1);
  }

  /**
   * Check if the ith path exists.
   * 
   * @param i Path to check validity of
   * @return True if i is positive and less than width
   */
  public boolean validPathNumber(int i) {
    return (i >= 0) && (i < width);
  }

  /**
   * Returns one of the <b>full</b> paths from pullback to target. This is the
   * combination of *one* of the pair of paths from the pullback node to target
   * node.
   *
   * The returned path is equivalent to combining the paths returned by
   * getProjectionPath(i) and getTargetPath(i).
   *
   * @param i Path number to get
   * @return ModelPath<F,GM,M,N,E> created from the combination of paths from
   *         pullback to target nodes
   */
  public ModelPath<F, GM, M, N, E> getFullPath(int i) {
    if (!validPathNumber(i)) {
      return null;
    }

    LinkedList<E> edges = new LinkedList<>();

    edges.addAll(getProjectionPath(i).getEdges());
    edges.addAll(getTargetPath(i).getEdges());

    return new ModelPath<>(edges);
  }

  @Override
  public void addEntityAttribute(EntityAttribute<F, GM, M, N, E> inAtt) {
    // does nothing
  }

  @Override
  /**
   * @return Array list of all projection paths
   */
  public ArrayList<ModelPath<F, GM, M, N, E>> getProjectionPaths() {
    ArrayList<ModelPath<F, GM, M, N, E>> pp = new ArrayList<>();
    for (int i = 0; i < width; i++) {
      pp.add(getProjectionPath(i));
    }
    return pp;
  }
}
