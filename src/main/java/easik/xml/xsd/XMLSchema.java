package easik.xml.xsd;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import easik.EasikTools;
import easik.xml.Prettify;
import easik.xml.xsd.nodes.XSDBaseNode;
import easik.xml.xsd.nodes.types.XSDType;

/**
 * Class to hold the invormation for building an XML Schema. Contains sets of
 * {@link easik.xml.xsd.nodes.types.XSDType} and
 * {@link easik.xml.xsd.nodes.XSDBaseNode} items.
 * <p/>
 * Note this is not a full implementation of the model of W3C XMLSchemas, rather
 * it is purpose built for producing a schema from the constraint diagram.
 *
 * @author brett.giles@drogar.com Date: Aug 13, 2009 Time: 1:02:36 PM
 * @version $$Id$$
 */
public class XMLSchema {
	/**  */
	private static XMLNameSpace xsNS;

	static {
		try {
			xsNS = new XMLNameSpace("xs", new URI("http://www.w3.org/2001/XMLSchema"));
		} catch (URISyntaxException e) {
			xsNS = new XMLNameSpace("xs", null);
		}
	}

	/**  */
	private boolean elementFormDefaultUnQualified = true;

	/**  */
	private boolean attributeFormDefaultUnQualified = true;

	/**  */
	private final Set<XSDBaseNode> elements;

	/**  */
	private XMLNameSpace targetNS;

	/**  */
	private final Set<XSDType> types;

	/**
	 *
	 *
	 * @param targetNS
	 */
	public XMLSchema(final XMLNameSpace targetNS) {
		this.targetNS = targetNS;
		types = new HashSet<>(1);
		elements = new HashSet<>(1);
	}

	/**
	 *
	 *
	 * @return
	 */
	public static XMLNameSpace getXSDNameSpace() {
		return xsNS;
	}

	/**
	 *
	 *
	 * @return
	 */
	public static XMLNameSpace getXsNS() {
		return xsNS;
	}

	/**
	 *
	 *
	 * @return
	 */
	public static String getXsNSPrefix() {
		return xsNS.getNs() + ':';
	}

	/**
	 *
	 *
	 * @param elt
	 */
	public void addElement(final XSDBaseNode elt) {
		elements.add(elt);
	}

	/**
	 *
	 *
	 * @param typ
	 */
	public void addType(final XSDType typ) {
		types.add(typ);
	}

	/**
	 *
	 *
	 * @return
	 */
	public boolean isElementFormDefaultUnQualified() {
		return elementFormDefaultUnQualified;
	}

	/**
	 *
	 *
	 * @param elementFormDefaultUnQualified
	 */
	public void setElementFormDefaultUnQualified(final boolean elementFormDefaultUnQualified) {
		this.elementFormDefaultUnQualified = elementFormDefaultUnQualified;
	}

	/**
	 *
	 *
	 * @return
	 */
	public boolean isAttributeFormDefaultUnQualified() {
		return attributeFormDefaultUnQualified;
	}

	/**
	 *
	 *
	 * @param attributeFormDefaultUnQualified
	 */
	public void setAttributeFormDefaultUnQualified(final boolean attributeFormDefaultUnQualified) {
		this.attributeFormDefaultUnQualified = attributeFormDefaultUnQualified;
	}

	/**
	 *
	 *
	 * @return
	 */
	public XMLNameSpace getTargetNS() {
		return targetNS;
	}

	/**
	 *
	 *
	 * @param targetNS
	 */
	public void setTargetNS(final XMLNameSpace targetNS) {
		this.targetNS = targetNS;
	}

	/**
	 * XML string of the Schema.
	 *
	 * @see Prettify
	 * @return the prettied string
	 */
	@Override
	public String toString() {
		final StringBuilder ret = new StringBuilder("<?xml version='1.0' ?>");
		final String lineSep = EasikTools.systemLineSeparator();

		ret.append(lineSep).append('<').append(getXsNSPrefix()).append("schema ").append(xsNS.toString())
				.append(attributeFormDefaultUnQualified ? " attributeFormDefault=\"unqualified\" "
						: " attributeFormDefault=\"qualified\" ")
				.append(elementFormDefaultUnQualified ? " elementFormDefault=\"unqualified\" "
						: " elementFormDefault=\"qualified\" ")
				.append(targetNS.prettyString("       ", lineSep)).append(" >").append(lineSep);

		for (final XSDType t : types) {
			ret.append(t.toString()).append(lineSep);
		}

		for (final XSDBaseNode e : elements) {
			ret.append(e.toString()).append(lineSep);
		}

		ret.append("</").append(getXsNSPrefix()).append("schema>").append(lineSep);

		return new Prettify(ret).toString();
	}
}
