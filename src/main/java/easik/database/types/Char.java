package easik.database.types;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collections;
import java.util.Map;

import easik.xml.xsd.nodes.types.FacetEnum;
import easik.xml.xsd.nodes.types.XSDBaseType;
import easik.xml.xsd.nodes.types.XSDRestriction;
import easik.xml.xsd.nodes.types.XSDSimpleType;
import easik.xml.xsd.nodes.types.XSDType;

/**
 * A class representing a CHAR(n) SQL data type. A CHAR differs from a VARCHAR
 * primarily in that, in theory, a CHAR will always occupy the full size (n),
 * while a VARCHAR will only use as much storage space as needed to store the
 * inserted values. Note that some databases (such as MySQL) will silently
 * convert CHAR columns to VARCHAR columns if the table contains any other
 * variable-size data type. Also note that the maximum size of the CHAR type is
 * db-dependent; some databases in particular will not allow more than 255.
 */
public class Char extends EasikType {
	/**  */
	private int size;

	/**
	 * Creates a new Char object. The n specifies the required size of this
	 * char. Where possible, the db should try to use a CHAR(n), but db
	 * limitations may convert this into other text types.
	 *
	 * @param n
	 *            size
	 */
	public Char(final int n) {
		size = n;
	}

	/**
	 * Recreates the object from the attributes returned by attributes().
	 *
	 * @param attr
	 *            size attribute
	 */
	public Char(final Map<String, String> attr) {
		this(Integer.parseInt(attr.get("size")));
	}

	/**
	 *
	 *
	 * @return
	 */
	public int getSize() {
		return size;
	}

	/**
	 *
	 *
	 * @return
	 */
	@Override
	public String toString() {
		return "CHAR(" + size + ')';
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
		return input.length() <= size;
	}

	/**
	 * Returns the attributes of this object
	 *
	 * @return
	 */
	@Override
	public Map<String, String> attributes() {
		return Collections.singletonMap("size", String.valueOf(size));
	}

	/**
	 *
	 *
	 * @return
	 */
	@Override
	public int getSqlType() {
		return Types.CHAR;
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
		ps.setString(col, value);
	}

	/**
	 * Not just a basic type, requires a restriction.
	 *
	 * @return the xml schema type.
	 */
	@Override
	public XSDType getXMLSchemaType() {
		return new XSDSimpleType(new XSDRestriction("char" + size, XSDBaseType.xsString, FacetEnum.LENGTH, String.valueOf(size)));
	}
}
