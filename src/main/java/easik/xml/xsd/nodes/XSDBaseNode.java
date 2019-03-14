package easik.xml.xsd.nodes;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import easik.EasikTools;
import easik.xml.xsd.XMLSchema;

/**
 * Class to serve as a base node for XML Schema document elements.
 * <p/>
 * Basic abstract class that contains common points of XML Schema elements.
 * These are:
 * <ul>
 * <li>Tagname - Standard xml thing</li>
 * <li>tag attributes - Map of these, again, standard xml thing</li>
 * <li>A parent - to assist with XPATH</li>
 * <li>An Annotation, which although acceptable in XML Schema is normally a
 * subelement only</li>
 * </ul>
 * <p/>
 * Additionally, there are a variety of common functional attributes included
 * here, including managing the tag attribute list, handling of some items
 * (name, nillable) as tag attributes, and "toString" which requires the sub
 * class to define body.
 *
 * @author brett.giles@drogar.com Date: Aug 13, 2009 Time: 1:12:04 PM
 * @version $$Id$$
 * @see XSDAnnotation
 * @see easik.xml.xsd.XMLSchema
 * @see easik.xml.xsd.XMLNameSpace
 */
public abstract class XSDBaseNode {
	/**
	 * Make line seperator available as a class element and in all children
	 */
	public static final String lineSep = EasikTools.systemLineSeparator();

	/**
	 * Make the XMLSchema namespace available as a class element and in all children
	 */
	public static final String nsPrefix = XMLSchema.getXSDNameSpace().getNs();

	/**  */
	private XSDAnnotation annotation;

	/**  */
	private XSDBaseNode parent;

	/**  */
	private Map<String, String> tagAttributeList;

	/**  */
	private String tagName;

	/**
	 * Create with only a name
	 * <p/>
	 * Name will be a tag attribute.
	 *
	 * @param name The name
	 */
	public XSDBaseNode(final String name) {
		this(name, false, null);
	}

	/**
	 * Create with a name and whether the tag is nillable or not
	 * <p/>
	 * Name and nillable will be tag attributes.
	 *
	 * @param name     The name
	 * @param nillable can it use the nillable attribute
	 */
	protected XSDBaseNode(final String name, final boolean nillable) {
		this(name, nillable, null);
	}

	/**
	 * Create with a name and a parent
	 * <p/>
	 * Name will be tag attributes.
	 *
	 * @param name   The name
	 * @param parent The container element/node
	 */
	protected XSDBaseNode(final String name, final XSDBaseNode parent) {
		this(name, false, parent);
	}

	/**
	 * Create with a name and whether the tag is nillable or not and a parent
	 * <p/>
	 * Name and nillable will be tag attributes.
	 *
	 * @param name     The name
	 * @param nillable can it use the nillable attribute
	 * @param parent   The container element/node
	 */
	protected XSDBaseNode(final String name, final boolean nillable, final XSDBaseNode parent) {
		addTagAttribute("name", name);

		this.parent = parent;

		if (nillable) {
			addTagAttribute("nillable", "true");
		}
	}

	/**
	 * return the name
	 *
	 * @return name of the node
	 */
	public String getName() {
		return getTagAttribute("name");
	}

	/**
	 * Set a name
	 *
	 * @param name the name
	 */
	protected void setName(final String name) {
		addTagAttribute("name", name);
	}

	/**
	 * Is this nillable
	 *
	 * @return true or false
	 */
	public boolean isNillable() {
		return Boolean.valueOf(getTagAttribute("nillable"));
	}

	/**
	 * Set nillability
	 *
	 * @param nillable true or false
	 */
	public void setNillable(final boolean nillable) {
		if (nillable) {
			addTagAttribute("nillable", "true");
		} else {
			removeTagAttribute("nillable");
		}
	}

	/**
	 * Create a standard tag opening
	 * <p/>
	 * consists of the tag name prefixed by the namespace and includes the tag
	 * attributes. If selfClose is true, close at the same time.
	 *
	 * @param selfClose True if there are no sub elements
	 * @return string with tag display at start.
	 */
	public String getStartTag(final boolean selfClose) {
		return '<' + nsPrefix + ':' + tagName + tagAttributeListToString() + (selfClose ? "/>" : '>' + lineSep);
	}

	/**
	 * Create the standard tag closing.
	 *
	 * @return String with closing
	 */
	public String getEndTag() {
		return "</" + nsPrefix + ':' + tagName + '>';
	}

	/**
	 * Body of the tag, called by toString()
	 *
	 * @return A string containing the body.
	 */
	public abstract String getBody();

	/**
	 * XML of the node and its contained elements.
	 * <p/>
	 * Builds the start of the tag, adds in the body, and any annotations, then
	 * closes the tag.
	 *
	 * @return String with XML of node
	 */
	@Override
	public String toString() {
		final StringBuilder ret = new StringBuilder(50);
		final String body = getBody();
		final boolean selfClosed = (body == null || body.length() == 0) && (null == annotation);

		ret.append(getStartTag(selfClosed));

		if (selfClosed) {
			return ret.toString();
		}

		if (null != annotation) {
			ret.append(annotation.toString()).append(lineSep);
		}

		if ((body != null && body.length() != 0)) {
			ret.append(body).append(lineSep);
		}

		ret.append(getEndTag());

		return ret.toString();
	}

	/**
	 * Get the parent (containing Element)
	 *
	 * @return parent
	 */
	public XSDBaseNode getParent() {
		return parent;
	}

	/**
	 * Set the parent node (element)
	 *
	 * @param parent the parent node
	 */
	public void setParent(final XSDBaseNode parent) {
		this.parent = parent;
	}

	/**
	 * Gets an XPATH selector for the node using the name attribute and parents
	 * xpaths.
	 *
	 * @return XPath selector
	 */
	public String getXPath() {
		final String name = getTagAttribute("name");

		if (null != parent) {
			return parent.getXPath() + '/' + name;
		}
		return name;

	}

	/**
	 * Get any annotation associated with this node
	 *
	 * @return The annotation
	 */
	public XSDAnnotation getAnnotation() {
		return annotation;
	}

	/**
	 * (re)Set the initial annotation
	 *
	 * @param annotation The annotation
	 */
	public void setAnnotation(final XSDAnnotation annotation) {
		this.annotation = annotation;
	}

	/**
	 * Add more annotations to the node.
	 * <p/>
	 * Note this works whether or not an annotation has already been added. If there
	 * is not a current annotation, the nodes annotation is set to the parameter. If
	 * there is an existing annotation, the parameter is merged with the existing
	 * one.
	 *
	 * @param xannotation The annotation.
	 */
	public void addAnnotation(final XSDAnnotation xannotation) {
		if (null == this.annotation) {
			this.annotation = xannotation;
		} else {
			this.annotation.append(annotation);
		}
	}

	/**
	 * Get the tag name.
	 *
	 * @return Tag name, without any namespace prefix.
	 */
	public String getTagName() {
		return tagName;
	}

	/**
	 * Set the tag name
	 * <p/>
	 * Note this should be without a namespace prefix, but that is not checked by
	 * the method.
	 *
	 * @param tagName The non-namespace prefixed tag name.
	 * @todo Check for ':' and disallow in a friendly way.
	 */
	public void setTagName(final String tagName) {
		this.tagName = tagName;
	}

	/**
	 * Manage the attribute list - remove an attribute.
	 *
	 * @param key of the attribute to remove.
	 */
	public void removeTagAttribute(final String key) {
		if (null != tagAttributeList) {
			tagAttributeList.remove(key);
		}
	}

	/**
	 * Manage the attribute list, add an attribute
	 *
	 * @param key   Of the attribute
	 * @param value Of the attribute.
	 */
	public void addTagAttribute(final String key, final String value) {
		if (null == tagAttributeList) {
			tagAttributeList = new HashMap<>(3);
		}

		if (null == value) {
			tagAttributeList.remove(key);
		} else {
			tagAttributeList.put(key, value);
		}
	}

	/**
	 * Return the entire attribute list as an unmodifiable map
	 *
	 * @return map of attributes
	 */
	public Map<String, String> getTagAttributeList() {
		return Collections.unmodifiableMap(tagAttributeList);
	}

	/**
	 * Manage the attribute list, get value for a particular key
	 *
	 * @param key of the desired attribute
	 * @return null if not found, otherwise the value.
	 */
	public String getTagAttribute(final String key) {
		if ((tagAttributeList == null) || tagAttributeList.isEmpty()) {
			return null;
		}

		return tagAttributeList.get(key);
	}

	/**
	 * Return a string representation of the attribute list, suitable for use in XML
	 * <p/>
	 * Creates a string of the form:
	 * 
	 * <pre>
	 *   k1="v1" k2="v2"
	 * </pre>
	 * 
	 * for all the key/value pairs in the attributes.
	 *
	 * @return String of attributes.
	 */
	public String tagAttributeListToString() {
		if ((tagAttributeList == null) || tagAttributeList.isEmpty()) {
			return "";
		}

		final StringBuilder ret = new StringBuilder(15 * tagAttributeList.size());

		ret.append(' ');

		for (final Map.Entry<String, String> kvpair : tagAttributeList.entrySet()) {
			ret.append(kvpair.getKey()).append("=\"").append(kvpair.getValue()).append("\" ");
		}

		return ret.toString();
	}
}
