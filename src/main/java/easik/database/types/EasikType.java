package easik.database.types;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import easik.xml.xsd.nodes.types.XSDType;

/**
 * The super-class of all Easik types. Every attribute must be a EasikType
 * subclass: db base then map these types into the closest supported SQL data
 * type.
 *
 * To work properly, every type that takes arguments (for example, a size)
 * should have at least two constructors: one that provides a "natural"
 * interface (so that calls such as <code>new Varchar(25)</code> work as
 * expected), and a constructor that takes a
 * <code>Map&lt;String, String&gt;</code> as returned by attributes() that
 * reconstructs the object with the same parameters. If attributes() returns an
 * empty Map, the 0-argument constructor will be used by newType().
 *
 * If a new EasikType is implemented, the addition must be reflected in the
 * following locations: - easik.db.TypesUtil.getSupportedTypes() -
 * easik.db.Database.getDataAsString() - easik.sketch.util.TypeFactory.getType()
 * -- This was supposed to not need updating, but as type toString methods give
 * information about a types "attributes" (e.g. VarChar(255)), we cannot simply
 * equate a string with a EasikType's toString method because VarChar(255) !=
 * VarChar(254) - easik.db.columnValue.getColumnValue() -- This wraps our value
 * represented as closely as possible by a java type with our EasikType object
 */
public abstract class EasikType {
	/**
	 * Returns a string representation of this data type. This should generally be
	 * the SQL standard version of the datatype, which base are free to use if they
	 * support it. Once created, new EasikType subclasses should *not* change their
	 * toString() output without very good reason as it may break base that are
	 * depending on the existing string value.
	 *
	 * @return
	 */
	@Override
	public abstract String toString();

	/**
	 * Verifies a value to see if it conforms to the values available for this type.
	 * The value is typically something user-provided; this method can be used to
	 * check whether such a value is (likely) valid for the type represented by this
	 * object. This checking doesn't have to be *perfect*, just approximate. For
	 * example, BIGINT allows input consisting of any number up to 19 digits, even
	 * though values greater than approximately 9.2e18 aren't actually valid. In
	 * other words, this checking is meant to be a quick check, not an exhaustive
	 * one.
	 *
	 * @param input the value to check
	 * @return true if the value is valid for this type, false otherwise
	 */
	public abstract boolean verifyInput(String input);

	/**
	 * Returns the attributes of a type, for types that have attributes (for
	 * example, VARCHAR has a size attribute). The default is to return an empty
	 * set. A type class name and attribute set can be passed into newType() to
	 * recreate the type.
	 * 
	 * @return a map of the attributes for the type
	 */
	@SuppressWarnings("static-method")
	public Map<String, String> attributes() {
		return Collections.emptyMap();
	}

	/**
	 * Takes a type name and an attribute Map (as returned by attributes()) and
	 * constructs and returns a new EasikType object of that class.
	 *
	 * @param className the fully-qualified class name of the type, such as
	 *                  "easik.databse.types.Integer". The class must exist and must
	 *                  be a subclass of EasikType.
	 * @param attrs     the attributes of the type object, as returned by
	 *                  attributes().
	 * @return the new object of type <code>typeClass</code>, which must be a
	 *         EasikType subclass
	 * @throws ClassNotFoundException if <code><i>(typeClass)</i></code> does not
	 *                                exist, is not a EasikType subclass, does not
	 *                                have an appropriate constructor, or has a
	 *                                constructor that throws an exception.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static EasikType newType(final String className, final Map<String, String> attrs)
			throws ClassNotFoundException {
		if (!className.matches("^(?!(?i:" + EasikType.class.getName() + "))(?:\\w+\\.)+\\w+$")) // Only
																								// allow
																								// package.Class
																								// names,
																								// but
																								// don't
																								// allow
																								// this
																								// EasikType
																								// class
		{
			throw new ClassNotFoundException('\'' + className + "' is not a valid EasikType class name");
		}

		final Class typeClass = EasikType.class.getClassLoader().loadClass(className);

		if (!EasikType.class.isAssignableFrom(typeClass)) // the object must be
															// a EasikType
															// subclass
		{
			throw new ClassNotFoundException("Class found, but is not a EasikType subclass");
		}

		final EasikType t;

		try {
			if (attrs.isEmpty()) {
				t = (EasikType) typeClass.getConstructor().newInstance();
			} else {
				t = (EasikType) typeClass.getConstructor(Map.class).newInstance(attrs);
			}
		} catch (Exception e) {
			throw new ClassNotFoundException("Class found, but no suitable constructor exists", e);
		}

		return t;
	}

	/**
	 * Takes an SQL signature of a type name (such as "VARCHAR(255)") and maps it
	 * into a EasikType object. If the signature can't be identified as a known
	 * Easik db type, it'll be mapped into a Custom type object based on the
	 * passed-in signature.
	 *
	 * @param sig the SQL signature of the type, such as "INTEGER" or "DECIMAL(10,
	 *            2)"
	 * @return a EasikType object representing the signature as best as possible, or
	 *         a Custom type object if the type cannot be identified.
	 */
	public static EasikType typeFromSignature(final String sig) {
		final Pattern types = Pattern.compile(

				// $1 $2 $3 $4 $5 $6 $7
				"^(?:(varchar2?)\\s*\\(\\s*(\\d+)\\s*\\)|(char)\\s*\\(\\s*(\\d+)\\s*\\)|(int(?:eger)?)|(smallint(?:eger)?)|(bigint(?:eger)?)|"
						+

						// $8 $9 $10
						"(date)|(datetime|timestamp)|(time)|" +

						// $11 $12 $13
						"((?:decimal|numeric)\\s*\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\))|" +

						// $14 $15 $16 $17 $18
						"((?:big)?text|clob)|(blob)|(double(?:\\s*precision)?)|(float)|(bool(?:ean)?))$",
				Pattern.CASE_INSENSITIVE);
		final Matcher m = types.matcher(sig);

		if (m.matches()) {
			if (m.group(1) != null) {
				return new Varchar(Integer.parseInt(m.group(2)));
			} else if (m.group(3) != null) {
				return new Char(Integer.parseInt(m.group(4)));
			} else if (m.group(5) != null) {
				return new Int();
			} else if (m.group(6) != null) {
				return new SmallInt();
			} else if (m.group(7) != null) {
				return new BigInt();
			} else if (m.group(8) != null) {
				return new Date();
			} else if (m.group(9) != null) {
				return new Timestamp();
			} else if (m.group(10) != null) {
				return new Time();
			} else if (m.group(11) != null) {
				return new Decimal(Integer.parseInt(m.group(12)), Integer.parseInt(m.group(13)));
			} else if (m.group(14) != null) {
				return new Text();
			} else if (m.group(15) != null) {
				return new Blob();
			} else if (m.group(16) != null) {
				return new DoublePrecision();
			} else if (m.group(17) != null) {
				return new Float();
			} else // (m.group(18) != null)
			{
				return new Boolean();
			}
		}

		return new Custom(sig);
	}

	/**
	 * Returns the java.sql.Types type closest this type.
	 *
	 * @return java sql type
	 */
	public abstract int getSqlType();

	/**
	 *
	 *
	 * @return
	 */
	public abstract XSDType getXMLSchemaType();

	/**
	 *
	 *
	 * @param ps
	 * @param col
	 * @param value
	 *
	 * @throws SQLException
	 */
	@SuppressWarnings("static-method")
	public void bindValue(final PreparedStatement ps, final int col, final String value) throws SQLException {
		ps.setString(col, value);
	}
}
