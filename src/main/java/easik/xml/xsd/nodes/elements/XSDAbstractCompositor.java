package easik.xml.xsd.nodes.elements;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.util.List;

import easik.xml.xsd.nodes.XSDBaseNode;

/**
 * Abstract base of the compositor classes
 * <p/>
 * In XML Schema, a "compositor" is a tag that will hold other elements.
 * Elements can be "element", "attribute" or other compositors.
 * <p/>
 * For our implementation, we only create sequence or choice compositors.
 *
 * @author brett.giles@drogar.com Date: Aug 26, 2009 Time: 11:41:28 AM
 * @version $$Id$$
 */
public abstract class XSDAbstractCompositor extends XSDAbstractElement {
	/**  */
	private List<XSDAbstractElement> elements;

	/**
	 * Default constructor, requires a tagname and a list of sub elements.
	 *
	 * @param tagName  The tag name.
	 * @param elements The list of elements
	 */
	public XSDAbstractCompositor(final String tagName, final List<XSDAbstractElement> elements) {
		super(null, false, null);

		this.elements = elements;

		setTagName(tagName);
	}

	/**
	 * Add a sub element to the compositor
	 * <p/>
	 * Note that any subclass of {@link XSDAbstractElement} is allowed.
	 * <p/>
	 * If the element is null, it is not added as a sub element.
	 *
	 * @param elt The element
	 */
	public void addSubElement(final XSDAbstractElement elt) {
		if (null != elt) {
			elements.add(elt);
		}
	}

	/**
	 * Add a sub element to the compositor at specified position.
	 * <p/>
	 * Note that any subclass of {@link XSDAbstractElement} is allowed.
	 * <p/>
	 * If the element is null, it is not added as a sub element.
	 *
	 * @param pos Where to add the element in the list.
	 * @param elt The element
	 */
	public void addSubElement(final int pos, final XSDAbstractElement elt) {
		if (null != elt) {
			elements.add(pos, elt);
		}
	}

	/**
	 *
	 *
	 * @return
	 */
	@Override
	public String toString() {
		return listToString();
	}

	/**
	 *
	 *
	 * @return
	 */
	@Override
	public String getBody() {
		return toString();
	}

	/**
	 *
	 *
	 * @return
	 */
	public boolean isEmpty() {
		return elements.isEmpty();
	}

	/**
	 * Helper method used by sequences to list elements.
	 *
	 * @return XML String of collection
	 */
	protected String listToString() {
		final StringBuilder ret = new StringBuilder(100);

		if ((null != elements) && (elements.size() > 0)) {
			ret.append('<').append(nsPrefix).append(':').append(getTagName()).append(' ')
					.append(tagAttributeListToString()).append('>').append(lineSep);

			for (final XSDBaseNode elt : elements) {
				ret.append(elt.toString()).append(lineSep);
			}

			ret.append("</").append(nsPrefix).append(':').append(getTagName()).append('>');
		}

		return ret.toString();
	}
}
