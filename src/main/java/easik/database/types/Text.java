package easik.database.types;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import easik.xml.xsd.nodes.types.XSDBaseType;
import easik.xml.xsd.nodes.types.XSDType;

/**
 * A class representing a TEXT SQL data type. A TEXT can store an unlimited
 * amount of text data (though some base impose a limit--for example, the
 * maximum size under MySQL is 4GB). This type will be mapped to the largest
 * text type supported by the db (under MySQL, BIGTEXT; under most other
 * databases, TEXT).
 */
public class Text extends EasikType {
	/**
	 *
	 *
	 * @return
	 */
	@Override
	public String toString() {
		return "TEXT";
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
		return Types.CLOB;
	} // FIXME??

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
	 *
	 *
	 * @return
	 */
	@Override
	public XSDType getXMLSchemaType() {
		return XSDBaseType.xsString;
	}
}
