package easik.xml.xsd.nodes.elements;

//~--- JDK imports ------------------------------------------------------------

import java.util.ArrayList;
import java.util.List;

/**
 * XML Schema sequence compositor.
 *
 * Sequence in XML Schema means that each sub element occurs one or more times
 * in the order listed.
 *
 * @author brett.giles@drogar.com Date: Aug 26, 2009 Time: 9:54:11 AM
 * @version $$Id$$
 */
public class XSDSequenceCompositor extends XSDAbstractCompositor {
  /**
   *
   */
  public XSDSequenceCompositor() {
    super("sequence", new ArrayList<XSDAbstractElement>(1));
  }

  /**
   *
   *
   * @param elements
   */
  public XSDSequenceCompositor(final List<XSDAbstractElement> elements) {
    super("sequence", elements);
  }
}
