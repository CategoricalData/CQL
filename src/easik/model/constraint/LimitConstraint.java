package easik.model.constraint;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.util.ArrayList;
import java.util.LinkedList;

import easik.graph.EasikGraphModel;
import easik.model.Model;
import easik.model.attribute.EntityAttribute;
import easik.model.edge.ModelEdge;
import easik.model.path.ModelPath;
import easik.model.ui.ModelFrame;
import easik.model.vertex.ModelVertex;

/*
*            A
*           /|\
*          / v \
*         /  L  \
*        /  / \  \
*        v  v  v  v
*        B -----> C
 */

/**
 *
 */
public class LimitConstraint<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>>
    extends ModelConstraint<F, GM, M, N, E> {
  private static final long serialVersionUID = 8645375610612714804L;
  private Cone<F, GM, M, N, E> cone;
  private Cone<F, GM, M, N, E> limitCone1, limitCone2;

  public LimitConstraint(M inSketch, Cone<F, GM, M, N, E> cone, Cone<F, GM, M, N, E> limitCone1,
      Cone<F, GM, M, N, E> limitCone2) {
    super(inSketch);

    setName("LC");

    _isVisible = true;
    this.cone = cone;
    this.limitCone1 = limitCone1;
    this.limitCone2 = limitCone2;

    setEdgesAndPaths();
  }

  private void setEdgesAndPaths() {
    ArrayList<ModelPath<F, GM, M, N, E>> paths = new ArrayList<>();

    paths.add(cone.AB);
    paths.add(cone.BC);
    paths.add(cone.AC);
    paths.add(limitCone1.AB);
    paths.add(limitCone1.BC);
    paths.add(limitCone1.AC);
    paths.add(limitCone2.AB);
    paths.add(limitCone2.BC);
    paths.add(limitCone2.AC);

    _paths = paths;

    ArrayList<E> edges = new ArrayList<>();

    for (ModelPath<F, GM, M, N, E> p : paths) {
      LinkedList<E> curPathEdges = p.getEdges();

      for (E e : curPathEdges) {
        if (!edges.contains(e)) {
          edges.add(e);
        }
      }
    }

    _edges = edges;
  }

  public LimitConstraint(String name, int x, int y, boolean isVisible, M inModel,
      ArrayList<ModelPath<F, GM, M, N, E>> constraintPaths) {
    super(name, x, y, isVisible, inModel);

    // assumed order of constraintPaths
    if (constraintPaths.size() != 9) {
      throw new RuntimeException("Bad array size"); // TODO
    }

    cone = new Cone<>(constraintPaths.get(0), constraintPaths.get(1), constraintPaths.get(2));
    limitCone1 = new Cone<>(constraintPaths.get(3), constraintPaths.get(4), constraintPaths.get(5));
    limitCone2 = new Cone<>(constraintPaths.get(6), constraintPaths.get(7), constraintPaths.get(8));

    setEdgesAndPaths();
  }

  public N getLimitNode() {
    return limitCone1.getB();
  }

  public boolean isLimitValid() {
    if (limitCone1.getB() != limitCone2.getB()) {
      return false;
    }

    if ((cone.getA() != limitCone1.getA()) || (cone.getA() != limitCone2.getA())) {
      return false;
    }

    if ((cone.getB() != limitCone1.getC()) || (cone.getC() != limitCone2.getC())) {
      return false;
    }

    return true;
  }

  public Cone<F, GM, M, N, E> getCone() {
    return cone;
  }

  public Cone<F, GM, M, N, E> getLimitCone1() {
    return limitCone1;
  }

  public Cone<F, GM, M, N, E> getLimitCone2() {
    return limitCone2;
  }

  @Override
  public void addEntityAttribute(EntityAttribute<F, GM, M, N, E> inAtt) {
    // does nothing
  }

  @Override
  public ArrayList<ModelPath<F, GM, M, N, E>> getProjectionPaths() {
    // TODO Auto-generated method stub
    return null;
  }
}
