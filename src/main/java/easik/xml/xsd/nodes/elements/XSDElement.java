package easik.xml.xsd.nodes.elements;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.util.ArrayList;
import java.util.List;

import easik.Easik;
import easik.model.keys.UniqueKey;
import easik.sketch.Sketch;
import easik.sketch.edge.SketchEdge;
import easik.sketch.util.graph.SketchGraphModel;
import easik.sketch.vertex.EntityNode;
import easik.ui.SketchFrame;
import easik.xml.xsd.nodes.constraints.XSDAbstractKey;
import easik.xml.xsd.nodes.constraints.XSDKey;
import easik.xml.xsd.nodes.constraints.XSDKeyRef;
import easik.xml.xsd.nodes.constraints.XSDUniqueKey;
import easik.xml.xsd.nodes.types.XSDBaseType;
import easik.xml.xsd.nodes.types.XSDComplexType;
import easik.xml.xsd.nodes.types.XSDType;

/**
 * Representation of an XML Schema element declaration
 * <p/>
 * Elements are rather rich items in XML Schema, possibly reference types or
 * containing types and other elements. Additionally, they may have a variety of
 * constraints (Keys).
 * <p/>
 * This class is, as usual, tuned for easik and as such is somewhat interrelated
 * with easik's entity class
 *
 * @author brett.giles@drogar.com Date: Aug 14, 2009 Time: 1:55:00 PM
 * @version $$Id$$
 * @see EntityNode
 * @see XSDType
 * @see easik.xml.xsd.nodes.elements.XSDAbstractCompositor
 */
public class XSDElement extends XSDAbstractElement {
	/**  */
	private List<XSDAbstractKey> constraints;

	/**  */
	private XSDAbstractCompositor contents;

	/**
	 * Basic constructor, assumes no type, not too useful
	 *
	 * @param name
	 *            Name of the element
	 */
	public XSDElement(final String name) {
		this(name, false, null, null);
	}

	/**
	 * Construct from an EntityNode and parent
	 * <p/>
	 * This assumes the EnityNode has already had its "type" set in XML Schema.
	 * The "type" for this node is then just set to the name of that type
	 *
	 * @param node
	 *            the node.
	 * @param parent
	 *            the parent element
	 */
	public XSDElement(final EntityNode node, final XSDElement parent) {
		super(node.getName(), parent, node.getXsdType());

		setTagName("element");
	}

	/**
	 * Probably minimal useful constructor, sets the name and the type.
	 * <p/>
	 * Can be used with things like base types and restrictions
	 *
	 * @param name
	 *            The name
	 * @param elementType
	 *            The element type
	 */
	public XSDElement(final String name, final XSDType elementType) {
		this(name, false, elementType, null);
	}

	/**
	 * Expands the minimal useful constructor, sets the name and the type and if
	 * it is nillable
	 * <p/>
	 * Can be used with things like base types and restrictions
	 *
	 * @param name
	 *            The name
	 * @param nillable
	 *            Can the element be set to nil (different from empty)
	 * @param elementType
	 *            The element type
	 */
	public XSDElement(final String name, final boolean nillable, final XSDType elementType) {
		this(name, nillable, elementType, null);
	}

	/**
	 * Ful constructor, sets everything except constraints.
	 *
	 * @param name
	 *            The name
	 * @param nillable
	 *            Set to nil or not
	 * @param elementType
	 *            The type
	 * @param contents
	 *            A collection of sub elements.
	 */
	public XSDElement(final String name, final boolean nillable, final XSDType elementType, final XSDAbstractCompositor contents) {
		super(name, nillable, elementType);

		setTagName("element");

		this.contents = contents;
		constraints = new ArrayList<>(1);
	}

	/**
	 * Use an entity node to set the keys, including foreign keyrefs and
	 * uniques.
	 * <p/>
	 * Key is set from the primary key. KeyRefs are set from the outgoing edges.
	 * Uniques are set by Uniques and by noninclusion injective outgoing edges.
	 *
	 * @param node
	 *            we are working with
	 */
	@SuppressWarnings("unused")
	public void setKeys(final EntityNode node) {
		final List<UniqueKey<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge>> uniqueKeyList = node.getUniqueKeys();
		final XSDComplexType myType = (XSDComplexType) getElementType();

		constraints = new ArrayList<XSDAbstractKey>(uniqueKeyList.size() + 3);

		final String idName = Easik.getInstance().getSettings().getProperty("xml_id_name");
		final String keyrefName = Easik.getInstance().getSettings().getProperty("xml_keyref_name");
		final boolean idIsAttribute = Boolean.valueOf(Easik.getInstance().getSettings().getProperty("xml_id_is_attribute"));
		final XSDKey primaryKey = node.createXMLPrimaryKey(this);
		final XSDElement theParent = (XSDElement) getParent();

		theParent.addConstraint(primaryKey);

		for (final SketchEdge edge : node.getOutgoingEdges()) {
			final boolean isInclusion = edge.getTargetEntity().getName().equals(theParent.getName());

			if (edge.isInjective()) {
				if (!isInclusion) {
					constraints.add(new XSDUniqueKey(edge.getForeignKeyName(keyrefName), this, edge.getName()));
				}
			}

			if (!isInclusion) {
				theParent.addConstraint(new XSDKeyRef(this, edge.getTargetEntity().getXMLPrimaryKeyName(), edge.getName()));
				myType.addAtom(new XSDElement(edge.getName(), edge.isPartial(), XSDBaseType.xsInt));
			}
		}

		for (final UniqueKey<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> k : uniqueKeyList) {
			this.addConstraint(new XSDUniqueKey(this, k));
		}
	}

	/**
	 * Get the compositor
	 *
	 * @return Abstract compositor
	 */
	public XSDAbstractCompositor getContents() {
		return contents;
	}

	/**
	 * Set the contents to a new compositor.
	 *
	 * @param contents
	 *            Implementation of compositor.
	 */
	public void setContents(final XSDAbstractCompositor contents) {
		this.contents = contents;
	}

	/**
	 * Add an abstract element or subclass to the contents of this element
	 *
	 * @param content
	 *            Abstract Element
	 */
	public void addAtom(final XSDAbstractElement content) {
		contents.addSubElement(content);
	}

	/**
	 *
	 *
	 * @param constraint
	 */
	public void addConstraint(final XSDAbstractKey constraint) {
		constraints.add(constraint);
	}

	/**
	 * Text of the body of the tag, including embedded anonymous types and
	 * constraints
	 *
	 * @return XML body of this node.
	 */
	@Override
	public String getBody() {
		final StringBuilder ret = new StringBuilder(400);

		if (null != contents) {
			ret.append('<').append(nsPrefix).append(":complexType>").append(lineSep).append(contents.toString()).append(lineSep).append("</").append(nsPrefix).append(":complexType>").append(lineSep);
		} else {
			if (!getElementType().isReferencable()) {
				ret.append(getElementType().toString());
			}
		}

		if ((null != constraints) && (constraints.size() > 0)) {
			for (final XSDAbstractKey uniq : constraints) {
				ret.append(uniq.toString()).append(lineSep);
			}

			final int len = ret.length();

			ret.delete(len - lineSep.length(), len);
		}

		return ret.toString();
	}
}
