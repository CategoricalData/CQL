package easik.xml.xsd.nodes.constraints;

//~--- non-JDK imports --------------------------------------------------------

import easik.xml.xsd.nodes.elements.XSDAbstractElement;

/**
 * Implement the functionality for XPATH field references
 * 
 * @author gilesb
 * @since 12-Sep-2009 Time: 10:31:30 AM
 */
public class XSDXPathField extends XSDAbstractXPath {
	/**
	 *
	 *
	 * @param path
	 */
	public XSDXPathField(final String path) {
		super(path);

		setTagName("field");
	}

	/**
	 * Create with no name.
	 *
	 * @param elt
	 *            The element to get the PATH
	 */
	public XSDXPathField(final XSDAbstractElement elt) {
		super(elt);

		setTagName("field");
	}
}
