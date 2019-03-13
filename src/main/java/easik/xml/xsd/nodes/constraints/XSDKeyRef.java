package easik.xml.xsd.nodes.constraints;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.util.List;

import easik.xml.xsd.nodes.elements.XSDElement;

/**
 * Key reference tag in XML Schema
 * 
 * @author gilesb
 * @since Date: 19-Aug-2009 Time: 6:39:20 PM
 */
public class XSDKeyRef extends XSDAbstractKey {
	/**
	 *
	 *
	 * @param name
	 * @param appliesTo
	 * @param fields
	 */
	public XSDKeyRef(final String name, final XSDElement appliesTo, final List<String> fields) {
		super(name, appliesTo, fields);

		setTagName("keyref");
	}

	/**
	 *
	 *
	 * @param name
	 * @param appliesTo
	 * @param fields
	 */
	public XSDKeyRef(final String name, final XSDElement appliesTo, final String... fields) {
		super(name, appliesTo, fields);

		setTagName("keyref");
	}

	/**
	 *
	 *
	 * @param appliesTo
	 * @param refersTo
	 * @param fields
	 */
	public XSDKeyRef(final XSDElement appliesTo, final String refersTo, final String... fields) {
		this(appliesTo.getName() + "_ReferTo_" + refersTo, appliesTo, fields);

		addTagAttribute("refer", refersTo);
	}
}
