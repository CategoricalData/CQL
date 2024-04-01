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
 * This class represents the sum colimit constraint. It contains a list of paths
 * sharing a target.
 *
 * @author Kevin Green 2006
 * @author Vera Ranieri 2006
 * @version 2006-07-13 Vera Ranieri
 * @version 06-2014 Federico Mora
 */
public class SumConstraint<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>>
    extends ModelConstraint<F, GM, M, N, E> {
  /**
   *    
   */
  private static final long serialVersionUID = 4812008431345659670L;

  /**
   * Default Constructor takes a List of ModelPath<EntityNode, E>s
   *
   * @param inPaths A List of ModelPath<EntityNode, E>s
   * @param inModel
   */
  public SumConstraint(List<ModelPath<F, GM, M, N, E>> inPaths, M inModel) {
    super(inModel);

    _paths = new ArrayList<>(inPaths);
    _edges = new ArrayList<>();

    // Fill in _edges array
    for (ModelPath<F, GM, M, N, E> p : inPaths) {
      LinkedList<E> curPathEdges = p.getEdges();

      for (E e : curPathEdges) {
        if (!_edges.contains(e)) {
          _edges.add(e);
        }
      }
    }

    _isVisible = true;

    setName("+");
    assignToEntities();
  }

  /**
   * Constructor for specifying position on graph
   *
   * @param inPaths   A List of ModelPath<F,GM,M,N,E>s
   * @param x         X coordinate of visual on graph
   * @param y         Y coordinate of visual on graph
   * @param isVisible If the constraint is visible in the graph or not
   * @param inModel
   */
  public SumConstraint(List<ModelPath<F, GM, M, N, E>> inPaths, int x, int y, boolean isVisible, M inModel) {
    super("+", x, y, isVisible, inModel);

    _paths = new ArrayList<>(inPaths);
    _edges = new ArrayList<>();

    // Fill in _edges array
    for (ModelPath<F, GM, M, N, E> p : inPaths) {
      LinkedList<E> curPathEdges = p.getEdges();

      p.getDomain().addDepend(p.getCoDomain());

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
   */
  public SumConstraint(ArrayList<ModelPath<F, GM, M, N, E>> inPaths, M inModel, int id) {
    super(inModel, id);

    _paths = new ArrayList<>(inPaths);
    _edges = new ArrayList<>();

    // Fill in _edges array
    for (ModelPath<F, GM, M, N, E> p : inPaths) {
      LinkedList<E> curPathEdges = p.getEdges();

      for (E e : curPathEdges) {
        if (!_edges.contains(e)) {
          _edges.add(e);
        }
      }
    }

    _isVisible = true;

    setName("+");
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
   * There are no projection paths in Sums, BUT, for the purpose of creating the
   * belongs to constraint column in each intermediate table we will return all
   * paths.
   * 
   * This is used in the method assignToEntitties in Model
   */
  public ArrayList<ModelPath<F, GM, M, N, E>> getProjectionPaths() {
    return _paths;
  }
}
