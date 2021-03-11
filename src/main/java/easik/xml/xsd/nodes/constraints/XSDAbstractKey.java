package easik.xml.xsd.nodes.constraints;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import easik.xml.xsd.nodes.XSDBaseNode;
import easik.xml.xsd.nodes.elements.XSDElement;

/**
 * Base for XML Schema key and keyref tags
 * <p/>
 * Keys in XML Schema can either function as:
 * <ul>
 * <li>A primary key</li>
 * <li>A uniqueness constraint</li>
 * <li>A reference to another (i.e. foreign key)</li>
 * </ul>
 * <p/>
 * In all cases, the tag requires a name, the element it applies to and the
 * fields that comprise the key. These are managed in this abstract class.
 *
 * @author gilesb
 * @since Date: 21-Aug-2009 Time: 5:48:09 PM
 */
public abstract class XSDAbstractKey extends XSDBaseNode {
  /**
   * The element this key applies to. Will be used to create the XPATH expression
   * for the key
   */
  protected XSDXPathSelector appliesTo;

  /**
   * Which fields comprise the key. Multiple fields are allowed.
   */
  protected List<XSDXPathField> fields;

  /**
   * Full constructor
   *
   * @param name      Key name
   * @param appliesTo Which element
   * @param fields    List of fields
   */
  public XSDAbstractKey(final String name, final XSDElement appliesTo, final List<String> fields) {
    super(name);

    this.appliesTo = new XSDXPathSelector(appliesTo);
    this.fields = new ArrayList<>(fields.size());

    for (final String field : fields) {
      this.fields.add(new XSDXPathField(field));
    }
  }

  /**
   * Full constructor with variable argument for fields
   *
   * @param name      The name
   * @param appliesTo Which element
   * @param fields    list of fields as variable number of arguments
   */
  public XSDAbstractKey(final String name, final XSDElement appliesTo, final String... fields) {
    this(name, appliesTo, Arrays.asList(fields));
  }

  /**
   * Prepare xml representation of the body of the key.
   *
   * @return the body...
   */
  @Override
  public String getBody() {
    final StringBuilder ret = new StringBuilder(200);

    ret.append(appliesTo.toString()).append(lineSep);

    if ((fields != null) && (fields.size() > 0)) {
      for (final XSDXPathField pathField : fields) {
        ret.append(pathField.toString()).append(lineSep);
      }

      final int len = ret.length();

      ret.delete(len - lineSep.length(), len);
    }

    return ret.toString();
  }
}
