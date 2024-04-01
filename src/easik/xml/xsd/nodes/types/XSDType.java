package easik.xml.xsd.nodes.types;

//~--- non-JDK imports --------------------------------------------------------

import easik.xml.xsd.XMLNameSpace;
import easik.xml.xsd.nodes.XSDBaseNode;

/**
 * Abstract base for the "type" elements in XML Schema
 *
 * @author brett.giles@drogar.com Date: Aug 13, 2009 Time: 1:11:13 PM
 * @version $$Id$$
 */
public abstract class XSDType extends XSDBaseNode {
  /**  */
  private XMLNameSpace ns;

  /**
   *
   *
   * @param name
   */
  public XSDType(final String name) {
    this(null, name);
  }

  /**
   *
   *
   * @param name
   * @param nillable
   */
  public XSDType(final String name, final Boolean nillable) {
    super(name, false);

    setNillable(nillable);
  }

  /**
   *
   *
   * @param ns
   * @param name
   */
  public XSDType(final XMLNameSpace ns, final String name) {
    super(name, false);

    this.ns = ns;
  }

  /**
   * Unqualified (no namespace) name
   * 
   * @return Name
   */
  protected String getTypeName() {
    return getName();
  }

  /**
   * Qualified name, with namespace ':' in front.
   *
   * Qualified names may be needed in the XML Schema in references to types,
   * depending on whether the schema requires qualified elements or attributes.
   *
   * @see easik.xml.xsd.XMLSchema
   * @return Qualified name
   */
  public String getQualifiedName() {
    return (null != ns) ? ns.getNs() + ':' + getName() : getName();
  }

  /**
   *
   *
   * @return
   */
  public XMLNameSpace getNs() {
    return ns;
  }

  /**
   *
   *
   * @return
   */
  public abstract boolean isReferencable();
}
