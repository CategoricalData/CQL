package easik.database.types;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import easik.xml.xsd.nodes.types.XSDBaseType;
import easik.xml.xsd.nodes.types.XSDType;

/**
 * A class representing a DOUBLE PRECISION SQL data type. This is a fairly
 * common type typically with a range of about 1e-307 to 1e+308, and a precision
 * of at least 15 digits. See also FLOAT, for less precise floating point
 * numbers, and DECIMAL for fixed-precision floating point values. In standard
 * SQL, this is essentially a FLOAT(53).
 */
public class DoublePrecision extends EasikType {
	/**
	 *
	 *
	 * @return
	 */
	@Override
	public String toString() {
		return "DOUBLE PRECISION";
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
		return input.matches("^([+−]?)(?=\\d|\\.\\d)\\d*(\\.\\d*)?([Ee]([+−]?\\d+))?$");
	}

	/**
	 *
	 *
	 * @return
	 */
	@Override
	public int getSqlType() {
		return Types.DOUBLE;
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
	 *
	 *
	 * @return
	 */
	@Override
	public XSDType getXMLSchemaType() {
		return XSDBaseType.xsDouble;
	}
}
