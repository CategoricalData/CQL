/**
 * 
 */
package easik.model.path;

//-------JDK imports-----------
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

//------NON JDK imports----------
import easik.EasikTools;
import easik.graph.EasikGraphModel;
import easik.model.Model;
import easik.model.edge.ModelEdge;
import easik.model.edge.ModelEdge.Cascade;
import easik.model.ui.ModelFrame;
import easik.model.vertex.ModelVertex;
import easik.sketch.edge.InjectiveEdge;
import easik.sketch.edge.NormalEdge;

/**
 * This class will replace ModelPath<SketchFrame,
 * SketchGraphModel,Sketch,EntityNode, SketchEdge> and ModelPath<ViewFrame,
 * ViewGraphModel,View,QueryNode, View_Edge> by using generics.
 * 
 * This class is used to track path data used for constraints
 * 
 * @author Federico Mora
 *
 */
public class ModelPath<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>>
		extends DefaultMutableTreeNode {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8153519722775614255L;

	/**
	 * The unique id of this path
	 */
	private String _id = null;

	/**
	 * The codomain of this path
	 */
	private N _coDomain;

	/**
	 * The domain of this path
	 */
	private N _domain;

	/**
	 * The list of edges comprising this path
	 */
	private LinkedList<E> _edges;

	/**
	 * Constructor takes one entity, creates a 0-length path.
	 *
	 * @param inEntity An entity
	 */
	public ModelPath(N inEntity) {
		_domain = inEntity;
		_coDomain = inEntity;
		_edges = new LinkedList<>();
	}

	/**
	 * Constructor takes a List of one or more edges that form a path.
	 *
	 * @param inPath A List of edges
	 */
	public ModelPath(List<E> inPath) {
		_edges = new LinkedList<>(inPath);
		_domain = _edges.getFirst().getSourceEntity();
		_coDomain = _edges.getLast().getTargetEntity();
	}

	/**
	 * Generates a new ID based on the edges in the path
	 *
	 */
	private void generateID() {
		if (_id == null) {
			if (_edges.size() == 0) {
				_id = _domain.getName() + "_identity";
			} else {
				_id = EasikTools.join(";", _edges);
			}
		}
	}

	/**
	 * Overloaded method returns the _id
	 * 
	 * @return The id of this path
	 */
	@Override
	public String toString() {
		return getId();
	}

	/**
	 * Returns the ID of the path
	 *
	 * @return The ID of the path
	 */
	public String getId() {
		generateID();

		return _id;
	}

	/**
	 * Returns the list of edges
	 *
	 * @return The list of edges
	 */
	public LinkedList<E> getEdges() {
		return _edges;
	}

	/**
	 * Returns the first edge of this path
	 *
	 * @return
	 */
	public E getFirstEdge() {
		return _edges.getFirst();
	}

	/**
	 * Returns the last edge of this path
	 *
	 * @return
	 */
	public E getLastEdge() {
		return _edges.getLast();
	}

	/**
	 * Returns the domain of the path
	 *
	 * @return The domain of the path
	 */
	public N getDomain() {
		return _domain;
	}

	/**
	 * Returns the codomain of the path
	 *
	 * @return The codomain of the path
	 */
	public N getCoDomain() {
		return _coDomain;
	}

	/**
	 * Returns the second-last EntityNode in the path. In other words, this gives
	 * the source (domain) of the last edge that makes up the path.
	 *
	 * @return the second-last EntityNode along the path
	 */
	public N getLastDomain() {
		return _edges.getLast().getSourceEntity();
	}

	/**
	 * Returns the second EntityNode in the path. In other words, this gives the
	 * target (codomain) of the first edge that makes up the path.
	 *
	 * @return
	 */
	public N getFirstCoDomain() {
		return _edges.getFirst().getTargetEntity();
	}

	/**
	 * Returns the entities along this path, in order from domain to codomain.
	 *
	 * @return List of EntityNodes
	 */
	public List<N> getEntities() {
		LinkedList<N> nodes = new LinkedList<>();

		if (!_edges.isEmpty()) {
			nodes.add(_domain);

			for (E edge : _edges) {
				nodes.add(edge.getTargetEntity());
			}
		}

		return Collections.unmodifiableList(nodes);
	}

	/**
	 * Returns true if this path is injective, that is, if the first edge of this
	 * path is injective.
	 *
	 * @return
	 */
	public boolean isInjective() {
		return _edges.getFirst().isInjective();
	}

	/**
	 * Method to determine whether two paths are equal, based on the edges that
	 * comprise the path.
	 *
	 * @param a The first path
	 * @param b The second path
	 * @return True if the paths are equal, false otherwise
	 */
	public static boolean pathsAreEqual(ModelPath<?, ?, ?, ?, ?> a, ModelPath<?, ?, ?, ?, ?> b) {
		if (a.getEdges().size() != b.getEdges().size()) {
			return false; // Unequal length paths are obviously not equal
		}

		@SuppressWarnings("rawtypes")
		Iterator pathA = a.getEdges().iterator(), pathB = b.getEdges().iterator();

		while (pathA.hasNext()) {
			if (pathA.next() != pathB.next()) {
				return false; // We found a different edge
			}
		}

		// We got through all the edges with no differences: the paths are
		// equal.
		return true;
	}

	/**
	 * Returns true if this path is all delete on cascade, false otherwise
	 *
	 * @return boolean
	 * @author Federico Mora
	 */
	public boolean isCompositeCascade() {
		boolean cascade = true;

		for (ModelEdge<F, GM, M, N, E> ed : getEdges()) {
			boolean edgeCascade;
			if (ed.getCascading() == Cascade.CASCADE) {
				edgeCascade = true;
			} else {
				edgeCascade = false;
			}
			cascade = cascade && edgeCascade;
		}

		return cascade;
	}

	public boolean isFullyNormal() {
		for (ModelEdge<F, GM, M, N, E> ed : getEdges()) {
			// normal edge in this case only works for Sketches not views
			// this is OK at this point but will be bad if we decide to use this
			// for views
			if (!(ed instanceof NormalEdge)) {
				return false;
			}
		}
		return true;
	}

	public boolean isFullyDefined() {
		for (ModelEdge<F, GM, M, N, E> ed : getEdges()) {
			// normal edge and injective edge in this case only work for
			// Sketches not views
			// this is OK at this point but will be bad if we decide to use this
			// for views
			if (!(ed instanceof NormalEdge || ed instanceof easik.view.edge.NormalViewEdge)
					&& !(ed instanceof InjectiveEdge || ed instanceof easik.view.edge.InjectiveViewEdge)) {
				return false;
			}
		}
		return true;
	}

}
