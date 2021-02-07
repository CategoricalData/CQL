package easik.xml.xsd.nodes.constraints;

//~--- non-JDK imports --------------------------------------------------------

import easik.xml.xsd.nodes.elements.XSDAbstractElement;

/**
 * Implement the functionality for XPATH selectors
 * 
 * @author gilesb
 * @since 12-Sep-2009 Time: 10:31:30 AM
 */
public class XSDXPathSelector extends XSDAbstractXPath {
	/**
	 *
	 *
	 * @param path
	 */
	public XSDXPathSelector(final String path) {
		super(path);

		setTagName("selector");
	}

	/**
	 * Create with no name.
	 *
	 * @param elt The element to get the PATH
	 */
	public XSDXPathSelector(final XSDAbstractElement elt) {
		super(elt);

		setTagName("selector");
	}
}
