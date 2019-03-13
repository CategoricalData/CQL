package easik.database.types;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import easik.xml.xsd.nodes.types.XSDBaseType;
import easik.xml.xsd.nodes.types.XSDType;

/**
 * A class representing a FLOAT (also known as REAL) SQL data type. A FLOAT
 * typically has a range of about 1e-37 to 1e+37, and a precision of at least 6
 * digits. See also DOUBLE PRECISION, for more precise floating point numbers,
 * and DECIMAL for fixed-precision floating point values. In standard SQL, this
 * is essentially a FLOAT(24).
 */
public class Float extends EasikType {
	/**
	 *
	 *
	 * @return
	 */
	@Override
	public String toString() {
		return "FLOAT";
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
		return Types.FLOAT;
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
		ps.setFloat(col, java.lang.Float.parseFloat(value));
	}

	/**
	 *
	 *
	 * @return
	 */
	@Override
	public XSDType getXMLSchemaType() {
		return XSDBaseType.xsFloat;
	}
}
