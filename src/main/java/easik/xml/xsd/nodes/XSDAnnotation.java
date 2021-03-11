package easik.xml.xsd.nodes;

//~--- non-JDK imports --------------------------------------------------------

import easik.EasikTools;
import easik.xml.xsd.XMLSchema;

/**
 * XML Schema Annotation
 *
 * Annotations are used to inject human readable (sub node of "documentation")
 * or computer readable (sub node of "appInfo") documentation into an XML Schema
 * element.
 *
 * User: gilesb Date: 24-Aug-2009 Time: 11:51:16 AM To change this template use
 * File | Settings | File Templates.
 */
public class XSDAnnotation {
  /**  */
  private static final String lineSep = EasikTools.systemLineSeparator();

  /**  */
  private static final String nsPrefix = XMLSchema.getXSDNameSpace().getNs();

  /**  */
  private String appInfo;

  /**  */
  private String documentation;

  /**
   *
   *
   * @param documentation
   */
  public XSDAnnotation(final String documentation) {
    this(documentation, null);
  }

  /**
   *
   *
   * @param documentation
   * @param appInfo
   */
  public XSDAnnotation(final String documentation, final String appInfo) {
    this.documentation = documentation;
    this.appInfo = appInfo;
  }

  /**
   *
   *
   * @return
   */
  public String getDocumentation() {
    return documentation;
  }

  /**
   *
   *
   * @param documentation
   */
  public void setDocumentation(final String documentation) {
    this.documentation = documentation;
  }

  /**
   *
   *
   * @return
   */
  public String getAppInfo() {
    return appInfo;
  }

  /**
   *
   *
   * @param appInfo
   */
  public void setAppInfo(final String appInfo) {
    this.appInfo = appInfo;
  }

  /**
   * Add more annotations to this annotations. Merge if any existing.
   * 
   * @param other Other annotation
   */
  public void append(final XSDAnnotation other) {
    documentation = documentation + lineSep + other.getDocumentation();

    final String otherAppInf = other.getAppInfo();

    if (null != otherAppInf) {
      if (null != appInfo) {
        appInfo = appInfo + lineSep + otherAppInf;
      } else {
        appInfo = otherAppInf;
      }
    }
  }

  /**
   * Create XML of the annotation
   *
   * @return XML String of the annotation.
   */
  @Override
  public String toString() {
    final StringBuilder ret = new StringBuilder(50 + documentation.length());

    ret.append('<').append(nsPrefix).append(":annotation>").append(lineSep);

    if (documentation.length() > 0) {
      ret.append('<').append(nsPrefix).append(":documentation>").append(lineSep).append(documentation)
          .append(lineSep).append("</").append(nsPrefix).append(":documentation>").append(lineSep);
    }

    if ((null != appInfo) && (appInfo.length() > 0)) {
      ret.append('<').append(nsPrefix).append(":appinfo>").append(lineSep).append(appInfo).append("</")
          .append(nsPrefix).append(":appinfo>").append(lineSep);
    }

    ret.append("</").append(nsPrefix).append(":annotation>");

    return ret.toString();
  }
}
