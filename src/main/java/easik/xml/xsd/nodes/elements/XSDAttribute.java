package easik.xml.xsd.nodes.elements;

//~--- non-JDK imports --------------------------------------------------------

import easik.xml.xsd.nodes.XSDBaseNode;
import easik.xml.xsd.nodes.types.XSDType;

/**
 * Items specific to an attribute definition in XML Schema, tuned for EASIK
 *
 * The only time we have attributes in the definitions created by EASIK is for
 * the key value, which is an option. Default is the key is an element as well.
 *
 * The only unique item an an attribute, for easik, is that its tagname is
 * "attribute".
 *
 *
 * @author brett.giles@drogar.com Date: Aug 26, 2009 Time: 9:54:11 AM
 * @version $$Id$$
 */
public class XSDAttribute extends XSDAbstractElement {
	/**
	 *
	 *
	 * @param name
	 * @param elementType
	 */
	public XSDAttribute(final String name, final XSDType elementType) {
		this(name, null, elementType);
	}

	/**
	 *
	 *
	 * @param name
	 * @param parent
	 * @param elementType
	 */
	public XSDAttribute(final String name, final XSDBaseNode parent, final XSDType elementType) {
		super(name, parent, elementType);

		setTagName("attribute");
	}

	/**
	 *
	 *
	 * @return
	 */
	@Override
	public String getBody() {
		if (getElementType().isReferencable()) {
			return lineSep;
		} 
			return getElementType().toString() + lineSep;
		
	}
}
