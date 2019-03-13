package easik.xml.xsd.nodes.types;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.util.ArrayList;
import java.util.List;

import easik.Easik;
import easik.model.attribute.EntityAttribute;
import easik.sketch.Sketch;
import easik.sketch.edge.SketchEdge;
import easik.sketch.util.graph.SketchGraphModel;
import easik.sketch.vertex.EntityNode;
import easik.ui.SketchFrame;
import easik.xml.xsd.nodes.elements.XSDAbstractCompositor;
import easik.xml.xsd.nodes.elements.XSDAbstractElement;
import easik.xml.xsd.nodes.elements.XSDAttribute;
import easik.xml.xsd.nodes.elements.XSDElement;
import easik.xml.xsd.nodes.elements.XSDSequenceCompositor;

/**
 * Complex type
 *
 * @author brett.giles@drogar.com Date: Aug 14, 2009 Time: 1:55:00 PM
 * @version $$Id$$
 */
public class XSDComplexType extends XSDType {
	/**  */
	private XSDAbstractCompositor contents;

	/**  */
	private boolean referenceable;

	/**
	 *
	 *
	 * @param name
	 */
	public XSDComplexType(final String name) {
		this(name, false, new XSDSequenceCompositor());
	}

	/**
	 *
	 *
	 * @param node
	 * @param parent
	 */
	public XSDComplexType(final EntityNode node, final XSDComplexType parent) {
		super(node.getName() + "Type");

		setTagName("complexType");
		setParent(parent);

		referenceable = true;

		final List<EntityAttribute<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge>> entityAttributes = node.getEntityAttributes();

		contents = new XSDSequenceCompositor(new ArrayList<XSDAbstractElement>(entityAttributes.size()));

		final String idName = Easik.getInstance().getSettings().getProperty("xml_id_name");
		final boolean idIsAttribute = Boolean.valueOf(Easik.getInstance().getSettings().getProperty("xml_id_is_attribute"));

		if (idIsAttribute) {
			contents.addSubElement(new XSDAttribute(idName, XSDBaseType.xsInt));
		} else {
			contents.addSubElement(0, new XSDElement(idName, false, XSDBaseType.xsInt));
		}

		for (final EntityAttribute<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> attr : entityAttributes) {
			contents.addSubElement(new XSDElement(attr.getName(), attr.getType().getXMLSchemaType()));
		}
	}

	/**
	 *
	 *
	 * @param name
	 * @param nillable
	 * @param contents
	 */
	public XSDComplexType(final String name, final boolean nillable, final XSDAbstractCompositor contents) {
		super(name, nillable);

		this.contents = contents;

		setTagName("complexType");

		referenceable = null != name;
	}

	/**
	 *
	 *
	 * @param element
	 */
	public void addAtom(final XSDAbstractElement element) {
		if (null == element) {
			return;
		}

		contents.addSubElement(element);
	}

	/**
	 *
	 *
	 * @return
	 */
	@Override
	public String getBody() {
		return contents.toString();
	}

	/**
	 *
	 *
	 * @return
	 */
	@Override
	public boolean isReferencable() {
		return referenceable;
	}
}
