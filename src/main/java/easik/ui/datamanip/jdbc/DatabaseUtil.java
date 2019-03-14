package easik.ui.datamanip.jdbc;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.Window;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import easik.database.api.jdbc.JDBCDriver;
import easik.database.base.ColumnNaming;
import easik.database.base.PersistenceDriver;
import easik.sketch.vertex.EntityNode;
import easik.ui.datamanip.SelectDataDialog;
import easik.view.vertex.QueryNode;

/**
 *
 *
 * @version 12/09/12
 * @author Christian Fiddick
 */
public abstract class DatabaseUtil {
	/**
	 * Pops up a dialog for the user to select records from the table represented by
	 * the given entity node. The primary IDs of the records are returned.
	 * 
	 * @param frame the JFrame to attach the dialog to
	 * @param table The entity node representing the table from which we wish to
	 *              select the records
	 * @return An array of primary IDs representing the records we selected
	 */
	public static int[] selectRowPKs(final JFrame frame, final EntityNode table) {
		@SuppressWarnings("unused")
		final JDBCDriver dbd;
		final JTable tableData;

		try {
			dbd = table.getMModel().getDatabase().getJDBCDriver();
			tableData = getTable(table);
		} catch (PersistenceDriver.LoadException e) {
			System.err.println("Error in DatabaseUtil: " + e.getMessage());

			return null;
		} catch (SQLException e) {
			System.err.println("Error in DatabaseUtil: " + e.getMessage());

			return null;
		}

		final SelectDataDialog sdd = new SelectDataDialog(frame, table.getName(), tableData);

		if (sdd.isAccepted()) {
			return sdd.getSelectedPKs();
		}

		return new int[0];
	}

	/**
	 * Pops up a dialog for the user to select a single record from the table
	 * represented by the given entity node. The primary ID of the selected record
	 * is returned.
	 * 
	 * @param win   the JFrame or JDialog to attach the dialog to
	 * @param table The entity node representing the table from which we wish to
	 *              select the record
	 * @return The primary ID representing the record we selected
	 */
	public static int selectRowPK(final Window win, final EntityNode table) {
		@SuppressWarnings("unused")
		final PersistenceDriver dbd;
		final JTable tableData;

		try {
			dbd = table.getMModel().getDatabase().getJDBCDriver();
			tableData = getTable(table);

			tableData.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		} catch (PersistenceDriver.LoadException e) {
			System.err.println("Error in DatabaseUtil: " + e.getMessage());

			// 0 is an invalid PK
			return 0;
		} catch (SQLException e) {
			System.err.println("Error in DatabaseUtil: " + e.getMessage());

			// 0 is an invalid PK
			return 0;
		}

		final SelectDataDialog sdd = (win instanceof JDialog)
				? new SelectDataDialog((JDialog) win, table.getName(), tableData)
				: new SelectDataDialog((JFrame) win, table.getName(), tableData);

		if (sdd.isAccepted()) {
			// recall that we set our table for singleton selection
			final int[] selected = sdd.getSelectedPKs();

			return selected[0];
		}

		// 0 is an invalid PK
		return 0;
	}

	/**
	 * Returns an array of the primary keys found in a given table. An array of size
	 * 0 is returned in the event of a db access error.
	 * 
	 * @param table The entity node representing the table from which we wish to get
	 *              the primary IDs
	 * @return The primary IDs of all records found in the given table
	 */
	public static int[] getPKs(final EntityNode table) {
		final JDBCDriver dbd;
		final ColumnNaming cn;
		final ResultSet result;

		try {
			dbd = table.getMModel().getDatabase().getJDBCDriver();
			cn = new ColumnNaming(dbd);
			result = dbd.executeQuery("SELECT * FROM " + table.getName());

			result.last(); // move to last row to get row count

			final int[] pKs = new int[result.getRow()];

			// move back to first row
			result.beforeFirst();
			result.next();

			final String pIdName = cn.tablePK(table);

			for (int i = 0; i < pKs.length; i++) {
				pKs[i] = result.getInt(pIdName);

				result.next();
			}

			return pKs;
		} catch (SQLException e) {
			System.err.println("Error in DatabaseUtil: " + e.getMessage());

			return new int[0];
		}
	}

	/**
	 * Gets the contents of a given table, and returns them as a JTable. The first
	 * column is the record's primary key, and the remaining columns are layed out
	 * in the order EntityNode.getEntityAttributes() is stored.
	 * 
	 * @param table The EntityNode representing the table we wish to view
	 * @return The contents of the given table wrapped as a JTable
	 *
	 * @throws              PersistenceDriver.LoadException
	 * @throws SQLException
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static JTable getTable(final EntityNode table) throws SQLException, PersistenceDriver.LoadException {
		final JDBCDriver dbd;
		@SuppressWarnings("unused")
		final ColumnNaming cn;
		final Vector<Object[]> data;
		final String[] columnNames;
		final int cols;

		// Get column names of "shadow edges". We will ignore these columns.
		/**
		 * Removing all traces of shadow edges
		 * 
		 * final Collection<SketchEdge> shadowEdges = new
		 * HashSet<SketchEdge>(table.getShadowEdges()); final Collection<String>
		 * shadowColumns = new HashSet<String>(shadowEdges.size());
		 * 
		 * for (final SketchEdge edge : shadowEdges) {
		 * shadowColumns.add(edge.getName()); }
		 */

		final ResultSet result;

		dbd = table.getMModel().getDatabase().getJDBCDriver();
		cn = new ColumnNaming(dbd);

		final String mess = "SELECT * FROM " + dbd.quoteId(table.getName());

		result = dbd.executeQuery(mess);

		final ResultSetMetaData rsmd = result.getMetaData();

		// build 2D array of data
		cols = rsmd.getColumnCount(); // - shadowEdges.size();
		data = new Vector();
		columnNames = new String[cols];

		int currRow, i;

		// Populate array of column names
		for (i = 0; i < cols; i++) {
			final String columnName = rsmd.getColumnName(i + 1);

			// if (!(shadowColumns.contains(columnName)))
			// {
			columnNames[i] = columnName;
			// }
		}

		while (result.next()) {
			currRow = result.getRow();

			// if we have no rows, break
			if (currRow == 0) {
				break;
			}

			final Object[] row = new Object[cols];

			for (i = 0; i < cols; i++) {
				row[i] = result.getString(columnNames[i]);
			}

			data.add(row);
		}

		final Object[][] dataObj = data.toArray(new Object[0][0]);

		return new JTable(dataObj, columnNames);
	}

	/**
	 * Returns a Map of column names to their value for a given table/primary key
	 * pair. If no record exists in the table with the given primary key, we return
	 * an empty Map. Can throw a Database.LoadException or SQLException in the event
	 * that a db driver can not be acquired.
	 * 
	 * @param table The table from which we get the requested row.
	 * @param pk    The primary id of the row which we want
	 * @return A mapping of column names to their corresponding value for the given
	 *         table/id pair. If no such row exists, returns an empty Map.
	 */
	public static Map<String, String> getRecord(final EntityNode table, final int pk) {
		final JDBCDriver dbd;
		final ColumnNaming cn;
		final Map<String, String> data = new HashMap<>(20);
		final ResultSet result;

		try {
			dbd = table.getMModel().getDatabase().getJDBCDriver();
			cn = new ColumnNaming(dbd);

			final String select = "SELECT * FROM " + dbd.quoteId(table.getName()) + " WHERE " + cn.tablePK(table)
					+ " = " + pk;

			result = dbd.executeQuery(select);

			final ResultSetMetaData rsmd = result.getMetaData();

			result.next();

			for (int i = 1; i <= rsmd.getColumnCount(); i++) {
				data.put(dbd.quoteId(rsmd.getColumnName(i)), result.getString(i));
			}
		} catch (SQLException e) {
			JOptionPane.showMessageDialog(table.getMModel().getFrame(), "Could not get record: " + e.getMessage());
		}

		return data;
	}

	/**
	 *
	 *
	 * @param node
	 * @param parent
	 */
	@SuppressWarnings({ "rawtypes", "unchecked", "unused" })
	public static void displayQueryNode(final QueryNode node, final JFrame parent) {
		final JDBCDriver dbd;
		final ResultSet result;
		final ResultSetMetaData rsmd;
		final Vector<Object[]> data;
		final JTable asTable;
		final String[] columnNames;
		final int cols;

		try {
			dbd = node.getMModel().getSketch().getDatabase().getJDBCDriver();
			result = dbd.executeQuery(node.getQuery());
			rsmd = result.getMetaData();
			cols = rsmd.getColumnCount();

			// build 2D array of data
			data = new Vector();
			columnNames = new String[cols];

			int currRow, i;

			// Populate array of column names
			for (i = 0; i < cols; i++) {
				columnNames[i] = rsmd.getColumnName(i + 1);
			}

			while (result.next()) {
				currRow = result.getRow();

				// if we have no rows, break
				if (currRow == 0) {
					break;
				}

				final Object[] row = new Object[cols];

				for (i = 0; i < cols; i++) {
					row[i] = result.getString(columnNames[i]);
				}

				data.add(row);
			}

			final Object[][] dataObj = data.toArray(new Object[0][0]);

			asTable = new JTable(dataObj, columnNames);

			new SelectDataDialog(parent, node.getName(), asTable);
		} catch (SQLException e) {
			JOptionPane.showMessageDialog(node.getMModel().getSketch().getFrame(),
					"Could not get record: " + e.getMessage());
		}
	}

	/**
	 * 
	 * PROBABLY DO NOT NEED
	 * 
	 * @author Sarah van der Laan
	 * 
	 * @param node
	 * @param parent
	 * 
	 *               public static void addToQueryNode(final QueryNode node, final
	 *               JFrame parent) { final JDBCDriver dbd; final ResultSet result;
	 *               final ResultSetMetaData rsmd; final Vector<Object[]> data;
	 *               final JTable asTable; final String[] columnNames; final int
	 *               cols;
	 * 
	 *               try { dbd =
	 *               node.getMModel().getMModel().getDatabase().getJDBCDriver();
	 *               result = dbd.executeQuery(node.getQuery()); rsmd =
	 *               result.getMetaData(); cols = rsmd.getColumnCount();
	 * 
	 * 
	 *               } catch (SQLException e) {
	 *               JOptionPane.showMessageDialog(node.getMModel().getMModel().getFrame(),
	 *               "Could not get record: " + e.getMessage()); } }
	 */
}
