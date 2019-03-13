package easik.database.types;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import easik.xml.xsd.nodes.types.XSDBaseType;
import easik.xml.xsd.nodes.types.XSDType;

/**
 * A class representing a DATE SQL data type. A DATE value stores a DATE (such
 * (such as '2008-06-12') without a time. See also TIME and TIMESTAMP.
 */
public class Date extends EasikType {
	/**
	 *
	 *
	 * @return
	 */
	@Override
	public String toString() {
		return "DATE";
	}

	// Input will be in the form of selecting from a popup callender

	/**
	 *
	 *
	 * @param input
	 *
	 * @return
	 */
	@Override
	public boolean verifyInput(final String input) {
		return true;
	}

	/**
	 *
	 *
	 * @return
	 */
	@Override
	public int getSqlType() {
		return Types.DATE;
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
		ps.setDate(col, new java.sql.Date(Long.parseLong(value)));
	}

	/**
	 *
	 *
	 * @return
	 */
	@Override
	public XSDType getXMLSchemaType() {
		return XSDBaseType.xsDate;
	}
}
