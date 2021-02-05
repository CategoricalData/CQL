package easik.xml.xsd.nodes.constraints;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.util.List;

import easik.xml.xsd.nodes.elements.XSDElement;

/**
 * XMLDBDriver Primary key tag
 * 
 * @author gilesb
 * @since Date: 19-Aug-2009 Time: 6:39:20 PM
 */
public class XSDKey extends XSDAbstractKey {
	/**
	 *
	 *
	 * @param name
	 * @param appliesTo
	 * @param fields
	 */
	public XSDKey(final String name, final XSDElement appliesTo, final List<String> fields) {
		super(name, appliesTo, fields);

		setTagName("key");
	}

	/**
	 *
	 *
	 * @param name
	 * @param appliesTo
	 * @param fields
	 */
	public XSDKey(final String name, final XSDElement appliesTo, final String... fields) {
		super(name, appliesTo, fields);

		setTagName("key");
	}
}
