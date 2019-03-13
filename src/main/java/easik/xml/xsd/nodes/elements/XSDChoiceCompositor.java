package easik.xml.xsd.nodes.elements;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;

/**
 * XML Schema choice compositor.
 * <p/>
 * Choice in XML Schema means that each sub element occurs a variable number of
 * times. The default is that it is zero or one. One implying that the others do
 * not occur.
 * <p/>
 * This behaviour is modifiable by using the minOccurs and maxOccurs attributes
 *
 * @author brett.giles@drogar.com Date: Aug 26, 2009 Time: 9:54:11 AM
 * @version $$Id$$
 */
public class XSDChoiceCompositor extends XSDAbstractCompositor {
	/**
	 * Default constructor, no explicit min or max occurs
	 *
	 * @param elements
	 *            The sub elements.
	 */
	public XSDChoiceCompositor(final List<XSDAbstractElement> elements) {
		super("choice", elements);
	}

	/**
	 * Partial constructor including the elements and only the min occurences.
	 *
	 * @param elements
	 *            The elements
	 * @param minOccurs
	 *            The minimum number for each element
	 */
	public XSDChoiceCompositor(final List<XSDAbstractElement> elements, final int minOccurs) {
		super("choice", elements);

		addTagAttribute("minOccurs", String.valueOf(minOccurs));
	}

	/**
	 * Full constructor including the elements and the min/max occurences.
	 *
	 * @param elements
	 *            The elements
	 * @param minOccurs
	 *            The minimum number for each element
	 * @param maxOccurs
	 *            The Max number for each element
	 */
	public XSDChoiceCompositor(final List<XSDAbstractElement> elements, final int minOccurs, final int maxOccurs) {
		super("choice", elements);

		addTagAttribute("minOccurs", String.valueOf(minOccurs));
		addTagAttribute("maxOccurs", String.valueOf(maxOccurs));
	}

	/**
	 * Set the minimum number of occurences
	 *
	 * @param minOccurs
	 *            The min number
	 */
	public void setMinOccurs(final int minOccurs) {
		addTagAttribute("minOccurs", String.valueOf(minOccurs));
	}

	/**
	 * Get the min number of occurences
	 *
	 * @return Min occurences
	 */
	public String getMinOccurs() {
		return getTagAttribute("minOccurs");
	}

	/**
	 * Clear out the minimum occurrences
	 */
	public void clearMinOccurs() {
		removeTagAttribute("minOccurs");
	}

	/**
	 * Set the maximum number of occurences
	 *
	 * @param maxOccurs
	 *            The max number
	 */
	public void setMaxOccurs(final int maxOccurs) {
		addTagAttribute("maxOccurs", String.valueOf(maxOccurs));
	}

	/**
	 * Set the maximum occurrences to the special value of "unbounded"
	 */
	public void setMaxOccursUnbounded() {
		addTagAttribute("maxOccurs", "unbounded");
	}

	/**
	 * Get the maximum occureneces
	 *
	 * @return max occurs
	 */
	public String getMaxOccurs() {
		return getTagAttribute("maxOccurs");
	}

	/**
	 * Clear out the max occurrences
	 */
	public void clearMaxOccurs() {
		removeTagAttribute("maxOccurs");
	}
}
