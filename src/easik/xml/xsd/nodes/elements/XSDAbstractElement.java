package easik.xml.xsd.nodes.elements;

//~--- non-JDK imports --------------------------------------------------------

import easik.xml.xsd.nodes.XSDBaseNode;
import easik.xml.xsd.nodes.types.XSDType;

/**
 * Abstract the additional functions of an element or attribute.
 *
 * Elements all have an XML Schema type which is added hear. When the type is
 * named (e.g., is referencable within the schema), we add the name of the type
 * as an attribute to the node.
 *
 * @author brett.giles@drogar.com Date: Aug 26, 2009 Time: 9:54:11 AM
 * @version $$Id$$
 */
public abstract class XSDAbstractElement extends XSDBaseNode {
  /**  */
  private XSDType elementType;

  /**
   *
   *
   * @param name
   * @param nillable
   * @param elementType
   */
  public XSDAbstractElement(final String name, final boolean nillable, final XSDType elementType) {
    super(name, nillable);

    setElementType(elementType);
  }

  /**
   *
   *
   * @param name
   * @param parent
   * @param elementType
   */
  public XSDAbstractElement(final String name, final XSDBaseNode parent, final XSDType elementType) {
    super(name, parent);

    setElementType(elementType);
  }

  /**
   *
   *
   * @return
   */
  public XSDType getElementType() {
    return elementType;
  }

  /**
   *
   *
   * @param elementType
   */
  public void setElementType(final XSDType elementType) {
    this.elementType = elementType;

    try {
      if (elementType.isReferencable()) {
        addTagAttribute("type", elementType.getQualifiedName());
      }
    } catch (NullPointerException e) {
      // Ignore
    }
  }
}
