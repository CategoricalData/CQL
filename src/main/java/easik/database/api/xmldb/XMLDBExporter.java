package easik.database.api.xmldb;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.util.Collections;
import java.util.List;
import java.util.Map;

import easik.database.base.SketchExporter;
import easik.model.constraint.CommutativeDiagram;
import easik.model.constraint.EqualizerConstraint;
import easik.model.constraint.LimitConstraint;
import easik.model.constraint.ProductConstraint;
import easik.model.constraint.PullbackConstraint;
import easik.model.constraint.SumConstraint;
import easik.sketch.Sketch;
import easik.sketch.edge.SketchEdge;
import easik.sketch.util.graph.SketchGraphModel;
import easik.sketch.vertex.EntityNode;
import easik.ui.SketchFrame;

/**
 * This class is the XML:DB specific sketch exporter that should be extended by
 * the specific database exporter (that uses XML:DB).
 *
 * @author Christian Fiddick
 * @version Summer 2012, Easik 2.2
 */
public abstract class XMLDBExporter extends SketchExporter {
	/**
	 *
	 *
	 * @param sk
	 * @param db
	 * @param exportOpts
	 */
	protected XMLDBExporter(final Sketch sk, XMLDBDriver db, final Map<String, ?> exportOpts) {
		super(sk, db, exportOpts);
	}

	/**
	 * Takes a string, returns either a single-element list containing the
	 * string formatted as a comment.
	 *
	 * @param text
	 *            to commentize
	 * @return the comments as a list of strings.
	 */
	@Override
	public List<String> comment(final String text) {
		final List<String> ret;

		if (mode == Mode.DATABASE) {
			ret = Collections.emptyList();
		} else {
			ret = Collections.singletonList("<!--" + text + "-->");
		}

		return ret;
	}

	/**
	 * Returns a list of queries to run to create db constraints enforcing the
	 * passed-in commutative diagram.
	 *
	 * @param constraint
	 *            the commutative diagram
	 * @param id
	 *            a unique ID (typically a number) that can be used in
	 *            constraint names
	 * @return list of queries to create db enforcement of the constraint
	 */
	protected abstract List<String> createConstraint(CommutativeDiagram<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> constraint, String id);

	/**
	 * Returns a list of queries to run to create db constraints enforcing the
	 * passed-in product constraint.
	 *
	 * @param constraint
	 *            the product constraint
	 * @param id
	 *            a unique ID (typically a number) that can be used in
	 *            constraint names
	 * @return list of queries to create db enforcement of the constraint
	 */
	protected abstract List<String> createConstraint(ProductConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> constraint, String id);

	/**
	 * Returns a list of queries to run to create db constraints enforcing the
	 * passed-in equalizer constraint.
	 *
	 * @param constraint
	 *            the equalizer constraint
	 * @param id
	 *            a unique ID (typically a number) that can be used in
	 *            constraint names
	 * @return list of queries to create db enforcement of the constraint
	 */
	protected abstract List<String> createConstraint(EqualizerConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> constraint, String id);

	/**
	 * Returns a list of queries to run to create db constraints enforcing the
	 * passed-in pullback.
	 *
	 * @param constraint
	 *            the pullback constraint
	 * @param id
	 *            a unique ID (typically a number) that can be used in
	 *            constraint names
	 * @return list of queries to create db enforcement of the constraint
	 */
	protected abstract List<String> createConstraint(PullbackConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> constraint, String id);

	/**
	 * Returns a list of queries to run to create db constraints enforcing the
	 * passed-in sum constraint.
	 *
	 * @param constraint
	 *            the sum constraint
	 * @param id
	 *            a unique ID (typically a number) that can be used in
	 *            constraint names
	 * @return list of queries to create db enforcement of the constraint
	 */
	protected abstract List<String> createConstraint(SumConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> constraint, String id);

	/**
	 * Returns a list of queries to run to create db constraints enforcing the
	 * passed-in FLC constraint.
	 *
	 * @param constraint
	 *            the FLC constraint
	 * @param id
	 *            a unique ID (typically a number) that can be used in
	 *            constraint names
	 * @return list of queries to create db enforcement of the constraint
	 */
	protected abstract List<String> createConstraint(LimitConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> constraint, String id);
}
