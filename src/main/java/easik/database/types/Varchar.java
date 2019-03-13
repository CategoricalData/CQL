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
 * A class representing a VARCHAR(n) SQL data type.
 */
public class Varchar extends EasikType {
	/**  */
	private int size;

	/**
	 * Creates a new Varchar object. The n specifies the required size of this
	 * varchar. Note that some databases may not support size values larger than
	 * 255.
	 *
	 * @param n
	 *            the size
	 */
	public Varchar(final int n) {
		size = n;
	}
	
	public Varchar() {
		this(255);
	}

	/**
	 * Recreates the object from the attributes returned by attributes().
	 *
	 * @param attr
	 *            the size attribute in a map
	 */
	public Varchar(final Map<String, String> attr) {
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
		return "VARCHAR(" + size + ')';
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
		return input.matches("^.{0," + size + "}$");
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
		return Types.VARCHAR;
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
		return new XSDSimpleType(new XSDRestriction("varchar" + size, XSDBaseType.xsString, FacetEnum.MAXLENGTH, String.valueOf(size)));
	}
}
