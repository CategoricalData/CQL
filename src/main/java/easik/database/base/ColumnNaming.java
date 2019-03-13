package easik.database.base;

//~--- non-JDK imports --------------------------------------------------------

import easik.sketch.edge.SketchEdge;
import easik.sketch.vertex.EntityNode;

/**
 * Class to handle column naming for EASIK db modules. This module is used for
 * generating primary key names and foreign key names. There are a few different
 * ways EASIK-created columns can be named: this module is the authoritative
 * source for going from options to actual names. The following options
 * (provided to the Database.loadDriver() method's options) affect the generated
 * column names:
 *
 * <ul>
 * <li><code>pkFormat</code>: the naming style of primary keys. This value may
 * contain <code>&lt;table&gt;</code>, which will be replaced with the table
 * (entity) name. For EASIK v1-style naming (EntityName_id), you would use
 * <code>&lt;table&gt;_id</code></li>
 * <li><code>fkFormat</code>: the naming style of primary keys. This value may
 * contain the following keys, between <code>&lt;</code> and <code>&gt;</code>,
 * which will be replaced with the values indicated below. All other characters
 * will be interpreted literally. Note that non-alphanumeric and non-underscore
 * values will be removed by most drivers if SQL quoting is not enabled.
 * Supported tags:
 * <ul>
 * <li><code>&lt;target&gt;</code> &mdash; will be substituted with the name of
 * the target table.</li>
 * <li><code>&lt;source&gt;</code> &mdash; will be substituted with the name of
 * the source table.</li>
 * <li><code>&lt;edge&gt;</code> &mdash; will be substituted with the label of
 * the edge. Values <b>not</b> containing this tag (which is guaranteed to be
 * unique for multiple edges between tables) will fail on any sketch that
 * contains multiple parallel edges.</li>
 * </ul>
 * </li>
 * </ul>
 */
public class ColumnNaming {
	// The db driver, for driver-specific operations (i.e. quoting and
	// identifier cleaning)

	/**  */
	private PersistenceDriver dbd;

	// The formats used for primary and foreign keys.

	/**  */
	private String pkFormat, fkFormat;

	/**
	 * Creates a new ColumnNaming object for the specified driver and options.
	 *
	 * @param dbd
	 *            the Database object for this column naming object.
	 */
	public ColumnNaming(final PersistenceDriver dbd) {
		pkFormat = dbd.getOptionString("pkFormat");

		if (pkFormat == null) {
			pkFormat = "<table>_id"; // Fall back to this mainly for historic
										// reasons: Easik v1 always used this
										// format
		}

		fkFormat = dbd.getOptionString("fkFormat");

		if (fkFormat == null) {
			fkFormat = "<target>_<edge>"; // Completely *unlike* Easik v1: the
											// v1 "<target>_id" doesn't allow
											// multiple edges
		}

		this.dbd = dbd;
	}

	/**
	 * Takes an EntityNode and returns the name of the primary key column for
	 * that table/node. The name will <b>not</b> be quoted; quoteId() should be
	 * called on the resulting value before using it in the db.
	 *
	 * @param table
	 *            the table whose primary key is desired
	 * @return the primary key column name
	 * @see #qualifiedPK(easik.sketch.vertex.EntityNode)
	 */
	public String tablePK(final EntityNode table) {
		return table.getPrimaryKeyName(pkFormat);
	}

	/**
	 * Takes two EntityNodes and returns the name of the foreign key column in
	 * the source table pointing to the target table. The name will <b>not</b>
	 * be quoted; quoteId() should be called on the resulting value before using
	 * it in the db.
	 *
	 * @param edge
	 *            the edge between the tables being referenced
	 * @return the foreign key column name (i.e. of the column in the edge's
	 *         source node)
	 * @see #qualifiedFK(easik.sketch.edge.SketchEdge)
	 * @see easik.sketch.edge.SketchEdge#getForeignKeyName(String)
	 */
	public String tableFK(final SketchEdge edge) {
		return edge.getForeignKeyName(fkFormat);
	}

	/**
	 * Takes an EntityNode, and returns the quoted, fully-qualified name of the
	 * primary key column. Fully-qualified typically means
	 * <code>tablename.columnname</code> (though, if identifier quoting occurs,
	 * it might be quoted, such as <code>"tablename"."columnname"</code>). In
	 * constrast to the result of tablePK(), this value should not be passed to
	 * quoteId().
	 *
	 * @param table
	 *            the table having the desired primary key
	 * @return the quoted, qualified column reference string
	 */
	public String qualifiedPK(final EntityNode table) {
		return dbd.quoteId(table) + '.' + dbd.quoteId(tablePK(table));
	}

	/**
	 * Takes two EntityNodes, and returns the quoted, fully-qualified name of
	 * the foreign key column connecting them. Fully-qualified typically means
	 * <code>tablename.columnname</code> (though, if identifier quoting occurs,
	 * it might be quoted, such as <code>"tablename"."columnname"</code>). In
	 * contrast to tableFK(), this value should not be passed to quoteId.
	 *
	 * @param edge
	 *            the edge between the source table and target table
	 * @return the quoted, qualified column reference string
	 */
	public String qualifiedFK(final SketchEdge edge) {
		return dbd.quoteId(edge.getSourceEntity().getName()) + '.' + dbd.quoteId(tableFK(edge));
	}
}
