package easik.database.types;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import easik.xml.xsd.nodes.types.FacetEnum;
import easik.xml.xsd.nodes.types.XSDBaseType;
import easik.xml.xsd.nodes.types.XSDRestriction;
import easik.xml.xsd.nodes.types.XSDSimpleType;
import easik.xml.xsd.nodes.types.XSDType;

/**
 * A class representing a DECIMAL (also known as NUMERIC) SQL data type. A
 * DECIMAL requires two arguments: the precision and scale. The precision
 * indicates the maximum total number of significant digits of the number, and
 * the scale indicates the maximum total number of significant digits after the
 * decimal point. For example, the value 123.45678 has precision 8, and scale 5.
 * This type is often used for monetary values, where floating point numbers are
 * not appropriate. Note, however, that DECIMAL types are significantly slower
 * and occupy significantly more space than FLOAT or DOUBLE PRECISION types.
 */
public class Decimal extends EasikType {
  /**  */
  private int precision, scale;

  /**
   * Recreates the object from the attributes returned by attributes().
   *
   * @param attr the attributes
   */
  public Decimal(final Map<String, String> attr) {
    this(Integer.parseInt(attr.get("precision")), Integer.parseInt(attr.get("scale")));
  }

  /**
   *
   *
   * @param p
   * @param s
   */
  public Decimal(final int p, final int s) {
    precision = p;
    scale = s;
  }

  /**
   *
   *
   * @return
   */
  public int getPrecision() {
    return precision;
  }

  /**
   *
   *
   * @return
   */
  public int getScale() {
    return scale;
  }

  /**
   *
   *
   * @return
   */
  @Override
  public String toString() {
    return "DECIMAL(" + precision + ',' + scale + ')';
  }

  /**
   *
   *
   * @param input
   *
   * @return
   */
  @Override
  public boolean verifyInput(final String input) {
    final int decimal = input.indexOf('.');

    if (decimal >= 0) {
      if (decimal > precision - scale) {
        return input.matches("^[-+]?\\d{0," + (precision - 1) + "}.\\d{1," + (precision - decimal) + "}$");
      }
      return input.matches("^[-+]?\\d{0," + (precision - 1) + "}.\\d{1," + scale + "}$");

    }
    return input.matches("^[-+]?\\d{1," + precision + "}$");

  }

  /**
   * Returns the attributes of this object
   *
   * @return
   */
  @Override
  public Map<String, String> attributes() {
    final Map<String, String> attr = new HashMap<>(2);

    attr.put("precision", String.valueOf(precision));
    attr.put("scale", String.valueOf(scale));

    return attr;
  }

  /**
   *
   *
   * @return
   */
  @Override
  public int getSqlType() {
    return Types.DECIMAL;
  }

  /**
   *
   *
   * @param ps
   * @param col
   * @param value
   *
   * @throws SQLException
   */
  @Override
  public void bindValue(final PreparedStatement ps, final int col, final String value) throws SQLException {
    ps.setDouble(col, Double.parseDouble(value));
  }

  /**
   * Not just a basic type, requires a restriction.
   *
   * @return the xml schema type.
   */
  @Override
  public XSDType getXMLSchemaType() {
    final XSDRestriction dectype = new XSDRestriction("dec" + precision + '_' + scale, XSDBaseType.xsDecimal,
        FacetEnum.TOTALDIGITS, String.valueOf(precision));

    dectype.addFacet(FacetEnum.FRACTIONDIGITS, String.valueOf(scale));

    return new XSDSimpleType(dectype);
  }
}
