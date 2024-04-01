package easik.model.states;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.util.ArrayList;

import easik.graph.EasikGraphModel;
import easik.model.Model;
import easik.model.edge.ModelEdge;
import easik.model.ui.ModelFrame;
import easik.model.vertex.ModelVertex;

/**
 * The GetPathState is a state which can be used to retrieve a single injective
 * edge or a path beginning with an injective edge from the user.
 */
public class GetInjectivePathState<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>>
    extends GetPathState<F, GM, M, N, E> {
  /**  */
  @SuppressWarnings("unused")
  private boolean _edgeOnly;

  /**
   * Constructs a new GetInjectivePathState that can begin on any injective edge.
   *
   * @param next     Boolean determining whether the user should be allowed to
   *                 select next on this round. True if next could be selected,
   *                 false otherwise.
   * @param finish   Boolean determining whether the user should be allowed to
   *                 select finish on this round. True if finish could be
   *                 selected, false otherwise.
   * @param edgeOnly Boolean determining whether the user should be selecting just
   *                 a single, injective edge (true), or any path beginning with
   *                 an injective edge (false).
   * @param inSketch The sketch
   */
  public GetInjectivePathState(boolean next, boolean finish, boolean edgeOnly, M inModel) {
    this(next, finish, edgeOnly, inModel, null, null);
  }

  /**
   * Constructs a new GetInjectivePathState that can begin on any injective edge
   * out of the desired source node, and must end at the specified target node.
   *
   * @param next       Boolean determining whether the user should be allowed to
   *                   select next on this round. True if next could be selected,
   *                   false otherwise.
   * @param finish     Boolean determining whether the user should be allowed to
   *                   select finish on this round. True if finish could be
   *                   selected, false otherwise.
   * @param edgeOnly   Boolean determining whether the user should be selecting
   *                   just a single, injective edge (true), or any path beginning
   *                   with an injective edge (false).
   * @param sourceNode EntityNode specifying the node at which the path must start
   *                   (null if it can start on any injective edge).
   * @param targetNode EntityNode specifying the node at which the path must end
   *                   before the user can click next or finish.
   * @param inSketch   The sketch
   */
  public GetInjectivePathState(boolean next, boolean finish, boolean edgeOnly, M inModel, N sourceNode,
      N targetNode) {
    super(next, finish, inModel, sourceNode, targetNode);

    _edgeOnly = edgeOnly;
  }

  /**
   * Returns the edges that are to be selectable. Only edges extending from the
   * current path are selectable; if there is no path, then any edge qualifies.
   *
   * @return
   */
  @Override
  public Object[] getSelectables() {
    ArrayList<Object> selectable = new ArrayList<>();
    /*
     * Since we are allowing nodes and edges to be created as we go everything will
     * be selectable
     * 
     */

    for (Object cell : _ourModel.getRoots()) {
      selectable.add(cell);
    }

    return selectable.toArray();

  }
}
