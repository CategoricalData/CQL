package easik.xml.xsd.nodes.constraints;

import easik.xml.xsd.nodes.XSDBaseNode;
import easik.xml.xsd.nodes.elements.XSDAbstractElement;

/**
 * Abstract base of XPATH element functions
 * 
 * @author gilesb
 * @since Date: 12-Sep-2009 Time: 10:25:49 AM
 */
public abstract class XSDAbstractXPath extends XSDBaseNode {
	/**
	 * Create with no name.
	 * 
	 * @param path
	 *            The XPATH element
	 */
	public XSDAbstractXPath(final String path) {
		super(null);

		addTagAttribute("xpath", path);
	}

	/**
	 * Create with no name.
	 * 
	 * @param elt
	 *            The element to get the PATH
	 */
	public XSDAbstractXPath(final XSDAbstractElement elt) {
		super(null);

		addTagAttribute("xpath", elt.getXPath());
	}

	/**
	 *
	 *
	 * @param path
	 */
	protected void setXpath(final String path) {
		if ((path != null && path.length() != 0)) {
			addTagAttribute("xpath", path);
		}
	}

	/**
	 * Body of the tag, called by toString()
	 *
	 * For selectors, no body - only the attributes are important.
	 *
	 * @return A string containing the body.
	 */
	@Override
	public String getBody() {
		return "";
	}
}
