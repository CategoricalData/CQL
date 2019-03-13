package easik.database.types;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import easik.xml.xsd.nodes.types.XSDBaseType;
import easik.xml.xsd.nodes.types.XSDType;

/**
 * A class representing a TIMESTAMP SQL data type. A TIMESTAMP value stores a
 * date and time (such as '2008-06-12 18:58:32'). Note that, some databases
 * (MySQL in particular) have a bizarre and standard-incompatible version of
 * TIMESTAMP, but have the alternative DATETIME type that will be used instead.
 * See TimestampWithTimeZone for an alternative that includes a timezone with
 * the date/time.
 */
public class Timestamp extends EasikType {
	/**
	 *
	 *
	 * @return
	 */
	@Override
	public String toString() {
		return "TIMESTAMP";
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
		return true;
	}

	/**
	 *
	 *
	 * @return
	 */
	@Override
	public int getSqlType() {
		return Types.TIMESTAMP;
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
		ps.setTimestamp(col, new java.sql.Timestamp(Long.parseLong(value)));
	}

	/**
	 *
	 *
	 * @return
	 */
	@Override
	public XSDType getXMLSchemaType() {
		return XSDBaseType.xsDateTime;
	}
}
