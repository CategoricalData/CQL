package easik.xml.xsd;

//~--- non-JDK imports --------------------------------------------------------

import easik.xml.xsd.nodes.XSDBaseNode;

/**
 * General XML Schema tag - primarily used for facets.
 *
 * @author gilesb
 * @since Date: 12-Sep-2009 Time: 11:08:17 AM
 */
public class XSDGeneralTag extends XSDBaseNode {
	/**
	 * Create with a tagname only
	 *
	 * @param tagName The tag name
	 */
	public XSDGeneralTag(final String tagName) {
		super(null);

		setTagName(tagName);
	}

	/**
	 * Create with a tagname and one attribute
	 *
	 * @param tagName The tag name
	 * @param attr    The attribute name
	 * @param value   the attribute value
	 */
	public XSDGeneralTag(final String tagName, final String attr, final String value) {
		this(tagName);

		addTagAttribute(attr, value);
	}

	/**
	 * Body of the tag, called by toString()
	 *
	 * @return null - no body.
	 */
	@Override
	public String getBody() {
		return null;
	}
}
