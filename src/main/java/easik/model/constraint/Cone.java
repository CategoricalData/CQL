package easik.model.constraint;

//~--- non-JDK imports --------------------------------------------------------

import easik.graph.EasikGraphModel;
import easik.model.Model;
import easik.model.edge.ModelEdge;
import easik.model.path.ModelPath;
import easik.model.ui.ModelFrame;
import easik.model.vertex.ModelVertex;

/**
 *
 */
public class Cone<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>> {
	/*
	 * A / \ / \ v v B----->C
	 */
	public final ModelPath<F, GM, M, N, E> AB, BC, AC;

	public Cone(ModelPath<F, GM, M, N, E> AB, ModelPath<F, GM, M, N, E> BC, ModelPath<F, GM, M, N, E> AC) {
		if (!validCone(AB, BC, AC)) {
			throw new RuntimeException("Invalid cone");
		}

		this.AB = AB;
		this.BC = BC;
		this.AC = AC;
	}

	public N getA() {
		return AB.getDomain();
	}

	public N getB() {
		return BC.getDomain();
	}

	public N getC() {
		return AC.getCoDomain();
	}

	public boolean validCone(ModelPath<F, GM, M, N, E> AB, ModelPath<F, GM, M, N, E> BC, ModelPath<F, GM, M, N, E> AC) {
		if ((AB.getDomain() != AC.getDomain()) || (AB.getCoDomain() != BC.getDomain())
				|| (AC.getCoDomain() != BC.getCoDomain())) {
			return false;
		}

		return true;
	}
}
