package easik.database.types;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import easik.xml.xsd.nodes.types.XSDBaseType;
import easik.xml.xsd.nodes.types.XSDType;

/**
 * A class representing a BIGINT SQL data type. A BIGINT can contain values from
 * -2^63 to 2^63-1.
 */
public class BigInt extends EasikType {
	/**
	 *
	 *
	 * @return
	 */
	@Override
	public String toString() {
		return "BIGINT";
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
		return input.matches("^[-+]?\\d{1,19}$");
	}

	/**
	 *
	 *
	 * @return
	 */
	@Override
	public int getSqlType() {
		return Types.BIGINT;
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
		ps.setLong(col, Long.parseLong(value));
	}

	/**
	 *
	 *
	 * @return
	 */
	@Override
	public XSDType getXMLSchemaType() {
		return XSDBaseType.xsLong;
	}
}
