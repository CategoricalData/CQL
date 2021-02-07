package easik.xml.xsd.nodes.constraints;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import easik.Easik;
import easik.model.keys.UniqueIndexable;
import easik.model.keys.UniqueKey;
import easik.sketch.Sketch;
import easik.sketch.edge.SketchEdge;
import easik.sketch.util.graph.SketchGraphModel;
import easik.sketch.vertex.EntityNode;
import easik.ui.SketchFrame;
import easik.xml.xsd.nodes.elements.XSDElement;

/**
 * Uniqueness constraint in XML Schema
 * 
 * @author gilesb
 * @since Date: 19-Aug-2009 Time: 6:39:20 PM
 */
public class XSDUniqueKey extends XSDAbstractKey {
	/**
	 * Create based on an element and Unique key, but create a mangled name based on
	 * the "appliesTo" xsd element
	 *
	 * Use the unique key to discover the fields this refers to.
	 *
	 * @param appliesTo Which element
	 * @param key       The unique key of the element
	 */
	public XSDUniqueKey(final XSDElement appliesTo,
			final UniqueKey<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> key) {
		super(appliesTo.getName() + '_' + key.getKeyName(), appliesTo, new ArrayList<String>(key.getElements().size()));

		setTagName("unique");

		final String keyrefName = Easik.getInstance().getSettings().getProperty("xml_keyref_name");
		final Set<UniqueIndexable> keyColumns = key.getElements();

		for (final UniqueIndexable keyColumn : keyColumns) {
			if (keyColumn instanceof SketchEdge) {
				fields.add(new XSDXPathField(((SketchEdge) keyColumn).getForeignKeyName(keyrefName)));
			} else { // Probably an attribute
				fields.add(new XSDXPathField(keyColumn.getName()));
			}
		}
	}

	/**
	 *
	 *
	 * @param name
	 * @param appliesTo
	 * @param fields
	 */
	public XSDUniqueKey(final String name, final XSDElement appliesTo, final List<String> fields) {
		super(name, appliesTo, fields);

		setTagName("unique");
	}

	/**
	 *
	 *
	 * @param name
	 * @param appliesTo
	 * @param fields
	 */
	public XSDUniqueKey(final String name, final XSDElement appliesTo, final String... fields) {
		super(name, appliesTo, fields);

		setTagName("unique");
	}
}
