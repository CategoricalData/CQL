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
 * Represents the product constraint. Contains a list of paths which all share a
 * source.
 *
 * @author Kevin Green 2006
 * @author Vera Ranieri 2006
 * @version 2006-05-25 Vera Ranieri
 * @version 06-2014 Federico Mora
 */
public class ProductConstraint<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>>
    extends ModelConstraint<F, GM, M, N, E> {
  /**
   *    
   */
  private static final long serialVersionUID = -8016594066808044537L;

  /**
   * Product constraint constructor. Accepts a List that can have any number of
   * paths. Gives default coordinates.
   *
   * @param inPaths A List of ModelPath<F,GM,M,N,E>s
   * @param inModel
   */
  public ProductConstraint(List<ModelPath<F, GM, M, N, E>> inPaths, M inModel) {
    super(inModel);

    setName("x");

    _isVisible = true;
    _paths = new ArrayList<>(inPaths);

    addEdges();
    assignToEntities();
  }

  /**
   * Product constraint constructor. Accepts coordinates in constructor to allow
   * for user-defined visual placement.
   *
   * @param inPaths   A List of ModelPath<F,GM,M,N,E>s
   * @param x         X coordinate of visual on graph
   * @param y         Y coordinate of visual on graph
   * @param isVisible If the constraint is visible in the graph or not
   * @param inModel
   */
  public ProductConstraint(List<ModelPath<F, GM, M, N, E>> inPaths, int x, int y, boolean isVisible, M inModel) {
    super("x", x, y, isVisible, inModel);

    _paths = new ArrayList<>(inPaths);

    _edges = new ArrayList<>();

    // Fill in _edges array
    for (ModelPath<F, GM, M, N, E> p : inPaths) {
      // add to list of depends
      LinkedList<E> curPathEdges = p.getEdges();
      p.getCoDomain().addDepend(p.getDomain());

      for (E e : curPathEdges) {
        if (!_edges.contains(e)) {
          _edges.add(e);
        }
      }
    }
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
  public ProductConstraint(ArrayList<ModelPath<F, GM, M, N, E>> inPaths, M inModel, int id)
      throws ConstraintException {
    super(inModel, id);

    setName("x");

    _isVisible = true;
    _paths = new ArrayList<>(inPaths);

    addEdges();
    assignToEntities();
  }

  /**
   * Sets the path array, updates edge list, and updates display
   *
   * @param inPaths The new array of paths
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
   * All paths are projection paths, so return paths.
   */
  public ArrayList<ModelPath<F, GM, M, N, E>> getProjectionPaths() {
    return _paths;
  }
}
