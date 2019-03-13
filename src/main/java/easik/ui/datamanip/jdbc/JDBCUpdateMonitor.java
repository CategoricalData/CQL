package easik.ui.datamanip.jdbc;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import easik.EasikTools;
import easik.database.api.jdbc.JDBCDriver;
import easik.database.base.ColumnNaming;
import easik.database.types.EasikType;
import easik.database.types.Int;
import easik.model.attribute.EntityAttribute;
import easik.model.constraint.CommutativeDiagram;
import easik.model.constraint.LimitConstraint;
import easik.model.constraint.ModelConstraint;
import easik.model.constraint.ProductConstraint;
import easik.model.constraint.PullbackConstraint;
import easik.model.constraint.SumConstraint;
import easik.model.path.ModelPath;
import easik.sketch.Sketch;
import easik.sketch.edge.SketchEdge;
import easik.sketch.util.graph.SketchGraphModel;
import easik.sketch.vertex.EntityNode;
import easik.ui.SketchFrame;
import easik.ui.datamanip.ColumnEntry;
import easik.ui.datamanip.RowEntryDialog;
import easik.ui.datamanip.UpdateMonitor;

/**
 * In charge of monitoring update statements sent to server. Ensures constraints
 * are maintained, and appropriate error messages are sent to the user.
 *
 * This is used by the JDBCDrivers
 * 
 * @see easik.database.api.jdbc.JDBCDriver;
 * @see easik.database.db.MySQL.MySQL;
 *
 * @version Christian Fiddick Summer 2012
 */
public class JDBCUpdateMonitor extends UpdateMonitor {
	/** The sketch within which our editing takes place */
	private Sketch _theSketch;

	/** A column naming object for this update monitor */
	private ColumnNaming cn;

	/** The db driver for this update monitor */
	private JDBCDriver dbd;

	/**
	 *
	 *
	 * @param inSketch
	 * @param inDbd
	 */
	public JDBCUpdateMonitor(final Sketch inSketch, final JDBCDriver inDbd) {
		_theSketch = inSketch;
		dbd = inDbd;
		cn = new ColumnNaming(dbd);
	}

	/**
	 * Pops up a dialog for the user to select a record from the given table.
	 * Once selected, the table and primary ID is handed off to
	 * updateRow(EntityNode table, int pk).
	 * 
	 * @param table
	 *            The entity node representing the table we wish to update.
	 * @return Success of update.
	 */
	@Override
	public boolean updateRow(final EntityNode table) {
		final int pk = DatabaseUtil.selectRowPK(table.getMModel().getFrame(), table);

		return (pk > 0) && updateRow(table, pk);
	}

	/**
	 * Pops up a row entry dialog for the given table. The default field values
	 * will be the values stored in the row of the table who's primary ID
	 * matches the one given. Some cases call for use to restrict the update of
	 * foreign keys. Those cases are: a) A pullback b) The summands of a sum
	 * constrain c) The product of a product constraint If the table is in a
	 * constraint and the user wishes to break it the update is aborted leaving
	 * the old values in the record. Note: There are currently no checks to
	 * ensure that pk matches a record in our table.
	 * 
	 * @param table
	 *            The EntityNode representing the table we wish to update.
	 * @param pk
	 *            The primary ID of the row whos data will be used to set
	 *            defaults for our row entry dialog.
	 * @return The Success of the update.
	 */
	@Override
	public boolean updateRow(final EntityNode table, final int pk) {
		final DialogOptions dOpts = getDialogOptions(table);

		try {
			// Remove option to update foreign keys restricted by constraints
			for (final ModelConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> c : table.getConstraints()) {
				for (final SketchEdge se : table.getOutgoingEdges()) {
					if (c.hasEdge(se)) {
						dOpts.fKeys.remove(cn.tableFK(se));
					}
				}
			}

			// Display dialog for user with defaults as old record values
			final RowEntryDialog red = new RowEntryDialog(_theSketch.getFrame(), "Update " + table.getName() + ": " + cn.tablePK(table) + " = " + pk, dOpts.attToType, dOpts.fKeys, DatabaseUtil.getRecord(table, pk));

			if (red.isAccepted()) {
				final Set<ColumnEntry> input = new LinkedHashSet<>(red.getInput());

				// if we do not have any new input, no need to execute an update
				if (input.isEmpty()) {
					return true;
				}

				final StringBuilder sb = new StringBuilder("UPDATE " + table.getName() + " SET ");

				for (final ColumnEntry ce : input) {
					sb.append(dbd.quoteId(ce.getColumnName())).append('=' + "?,");
				}

				// remove last comma
				sb.delete(sb.length() - 1, sb.length());
				sb.append(" WHERE ").append(cn.tablePK(table)).append('=').append(pk);
				dbd.executePreparedUpdate(sb.toString(), input);

				return true;
			}

			return false;
		} catch (SQLException e) {
			JOptionPane.showMessageDialog(table.getMModel().getFrame(), "Could not update record: " + e.getMessage());

			return false;
		}
	}

	/**
	 * Trys to delete from a given table. If the action will break a constraint,
	 * it is aborted and the user is notified.
	 * 
	 * @param table
	 *            The table from which we will attempt the delete
	 * @return The success of the delete
	 */
	@Override
	public boolean deleteFrom(final EntityNode table) {
		final int[] selectedPKs = DatabaseUtil.selectRowPKs(table.getMModel().getFrame(), table);

		// if there was a selection
		if (selectedPKs.length > 0) {
			final String PKcolumn = cn.tablePK(table);
			final StringBuilder sb = new StringBuilder("DELETE FROM " + dbd.quoteId(table.getName()) + " WHERE ");

			// populate input set for prepared statement while adding column
			// names
			final Set<ColumnEntry> input = new LinkedHashSet<>(selectedPKs.length);

			for (final int pk : selectedPKs) {
				for (EntityAttribute<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> ea : table.getHiddenEntityAttributes()) {
					try {
						ResultSet result = dbd.executeQuery("SELECT * FROM " + table.getName() + " Where id=" + pk + " AND " + ea.getName() + "= 1");
						// since the result can at maximum be 1 because pks are
						// unique we only need to check if there is something in
						// the result
						// we do this by .isBeforeFirst()
						if (result.isBeforeFirst()) {
							JOptionPane.showMessageDialog(null, "Unable to execute DELETE: Deleting row will cause constraint inconsistency");
							return false;
						}
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				sb.append(PKcolumn).append("=? OR ");
				input.add(new ColumnEntry(PKcolumn, Integer.toString(pk), new Int()));
			}

			// remove last ',OR '
			sb.delete(sb.length() - 4, sb.length());

			try {
				dbd.executePreparedUpdate(sb.toString(), input);
			} catch (SQLException e) {
				JOptionPane.showMessageDialog(null, "Unable to execute DELETE: " + e.getMessage());
			}
		}

		return true;
	}

	/**
	 * Determines if insertion into a given table requires special handling due
	 * to constraints it may be in. As of now, special cases that may result
	 * from being in multiple constraints are not supported.
	 * 
	 * @param table
	 *            The table into which we wish to insert data
	 * @return Success of the insertion
	 */
	@Override
	public boolean insert(final EntityNode table) {
		final DialogOptions dOpts = getDialogOptions(table);
		final String lineSep = EasikTools.systemLineSeparator();

		// a set of column-value pairs of which we wish to force a specific
		// value, leaving the user out
		final Set<ColumnEntry> forced = new HashSet<>(10);

		// FIXME: This is pretty unpredicatable when there is more than one
		// contstraint. Tighten up?
		for (final ModelConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> c : table.getConstraints()) {
			if (c instanceof SumConstraint) {
				// if table is in the sum's domain, we let out trigger take care
				// of its foreign key, so remove it from the dialog's selection
				for (final ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> sp : c.getPaths()) {
					if (sp.getDomain() == table) {
						// we force the value 0 to avoid out driver to kick back
						// an error for having a null fKey
						final String columnName = cn.tableFK(sp.getFirstEdge());

						dOpts.fKeys.remove(columnName);
						forced.add(new ColumnEntry(columnName, "0", new Int()));

						break;
					}
				}
			}

			if (c instanceof CommutativeDiagram) {
				// if in the domain of a CD, we must make sure that our paths
				// commute
				if (c.getPaths().get(0).getDomain() == table) {
					JOptionPane.showMessageDialog(null, "Be sure that the following paths commute:" + lineSep + EasikTools.join(lineSep, c.getPaths()), "Commutative diagram", JOptionPane.INFORMATION_MESSAGE);

					try {
						return promptAndInsert(table, dOpts, forced);
					} catch (SQLException e) {
						/*
						 * if(e instanceof com.mysql.jdbc.exceptions.jdbc4.
						 * MySQLIntegrityConstraintViolationException){
						 * //injective property violated
						 * JOptionPane.showMessageDialog(null, e.getMessage(),
						 * "Injective property violation",
						 * JOptionPane.ERROR_MESSAGE); }else{
						 */
						JOptionPane.showMessageDialog(null, "Not all of the following paths commute -- insert aborted!" + lineSep + EasikTools.join(lineSep, c.getPaths()), "Commutative diagram failure", JOptionPane.ERROR_MESSAGE);
						// }
					}
				}
			}

			if (c instanceof PullbackConstraint) {
				// if we're not inserting to the target (or source, but that is
				// disabled at the popup menu level), there is a chance our
				// insert will pull a record back into the source. If this
				// happens, we want to let the user update the new record
				if (((PullbackConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge>) c).getTarget() != table) {
					final EntityNode pullback = ((PullbackConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge>) c).getSource();

					try {
						// get row count pre-insert
						ResultSet result = dbd.executeQuery("SELECT COUNT(*) FROM " + pullback.getName() + " X");

						result.next();

						final int preRowCount = result.getInt(1);

						if (!promptAndInsert(table, dOpts)) {
							return false;
						}

						// get row count post-insert
						result = dbd.executeQuery("SELECT COUNT(*) FROM " + pullback.getName() + " X");

						result.next();

						final int postRowCount = result.getInt(1);

						// WPBEDIT CF2012
						// if our pullback has more rows after INSERT, update
						// new row (the one with the highest primary ID)
						if (postRowCount > preRowCount) {
							result = dbd.executeQuery("SELECT MAX(" + cn.tablePK(pullback) + ") FROM " + pullback.getName() + " X");

							result.next();

							final int pk = result.getInt(1);

							if (JOptionPane.showConfirmDialog(null, "New record in pullback table '" + pullback.getName() + "'. Enter column data?", "Insert column data?", JOptionPane.YES_NO_OPTION) == 0) {
								updateRow(pullback, pk);
							}
						}

						return true;
					} catch (SQLException e) {
						JOptionPane.showMessageDialog(null, "Could not execute update. MYSQL Error output:\n" + e.getMessage());
					}
				}
			}

			if (c instanceof ProductConstraint) {
				// if we're inserting into a factor, we want to allow the user
				// to set values for each resulting record automatically
				// inserting into the product.
				for (final ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> sp : c.getPaths()) {
					if (sp.getCoDomain() == table) {
						final EntityNode product = sp.getDomain();

						try {
							if (!promptAndInsert(table, dOpts)) {
								return false;
							}

							// get the new records from the product. They are
							// any record who's fk to our INSERT factor matches
							// the primary id of the last insert
							ResultSet result = dbd.executeQuery("SELECT MAX(" + cn.tablePK(table) + ") FROM " + table.getName() + " X");

							result.next();

							final int newPK = result.getInt(1);

							result = dbd.executeQuery("SELECT * FROM " + product.getName() + " WHERE " + cn.tableFK(sp.getFirstEdge()) + " = " + newPK);

							// get count of new rows as result of INSERT
							result.last();

							final int newRows = result.getRow();

							result.beforeFirst();

							if ((newRows > 0) && (JOptionPane.showConfirmDialog(null, newRows + " new rows in product table '" + product.getName() + "'. Insert column data?", "Insert column data?", JOptionPane.YES_NO_OPTION) == 0)) {
								while (result.next()) {
									updateRow(product, result.getInt(1));
								}
							}
						} catch (SQLException e) {
							JOptionPane.showMessageDialog(null, e.getMessage());
						}

						return true;
					}
				}
			}

			if (c instanceof LimitConstraint) {
				// TRIANGLES TODO CF2012 Incomplete
			}
		}

		try {
			return promptAndInsert(table, dOpts, forced);
		} catch (SQLException e) {
			JOptionPane.showMessageDialog(null, "Could not execute update. MYSQL Error output:\n" + e.getMessage());
			System.err.println(e.getMessage());
			return false;
		}
	}

	/**
	 * Gets the options needed for open a dialog for row insertion: map of
	 * attribute names to their type, and a map of foreign key names to a JTable
	 * of data they point to.
	 * 
	 * @param table
	 *            The node representing the table who's information we want
	 * @return Wrapper class holding the maps needed for insertion dialog
	 */
	private DialogOptions getDialogOptions(final EntityNode table) {
		@SuppressWarnings("unused")
		final HashSet<ModelConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge>> constraints = new HashSet<ModelConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge>>(table.getConstraints());
		final HashMap<String, EasikType> attToType = new HashMap<>(25);
		final LinkedHashMap<String, EntityNode> fKeys = new LinkedHashMap<>(10);

		// find attributes, and map to their EasikType
		for (final EntityAttribute<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> ea : table.getEntityAttributes()) {
			attToType.put(ea.getName(), ea.getType());
		}

		// find all foreign keys, and add to foreign key set
		for (final SketchEdge ske : table.getOutgoingEdges()) {
			fKeys.put(cn.tableFK(ske), ske.getTargetEntity());
		}

		// find shadow foreign keys (used by sum constraints to solve parent
		// insertion issues)
		/**
		 * REMOVED because involves shadow edges for (final SketchEdge
		 * shadowEdge : table.getShadowEdges()) {
		 * fKeys.put(cn.tableFK(shadowEdge), shadowEdge.getTargetEntity()); }
		 */

		return new DialogOptions(attToType, fKeys);
	}

	/**
	 * Initializes a data insertion dialog and waits for the user to insert and
	 * accept the data. Once Accepted, forms an input statement and a map of
	 * input to its type which is handed to Database.executePreparedUpdate(..).
	 *
	 * @param table
	 *            the EntityNode
	 * @param dOpts
	 *            A DialogOptions object containing maps as specified in
	 *            easik.ui.datamanip.RowEntryDialog
	 * @param forced
	 *            A set of ColumnEntry objects that will be used in the
	 *            generated INSERT statement which the user has no control over
	 *            (i.e. the columns hadn't not shown up in the entry dialog, but
	 *            we still wish to include them).
	 *
	 *            No column name in this set should match one in dOpts. Should
	 *            this happen, is is undetermined which which of the two entries
	 *            will appear first in the input, and therefore the result after
	 *            INSERT is undetermined.
	 * @return Success of insert
	 *
	 * @throws SQLException
	 */
	private boolean promptAndInsert(final EntityNode table, final DialogOptions dOpts, final Set<ColumnEntry> forced) throws SQLException {
		final String tableName = table.getName();
		final RowEntryDialog red = new RowEntryDialog(table.getMModel().getFrame(), "Add row to table: " + tableName, dOpts.attToType, dOpts.fKeys);

		if (!red.isAccepted()) {
			return false;
		}

		final Set<ColumnEntry> input = new LinkedHashSet<>(red.getInput());

		input.addAll(forced);

		final StringBuilder sb = new StringBuilder("INSERT INTO " + dbd.quoteId(tableName) + ' ');
		final Collection<String> colNames = new LinkedList<>();
		final Collection<String> values = new LinkedList<>();

		for (final ColumnEntry entry : input) {
			colNames.add(dbd.quoteId(entry.getColumnName()));
			values.add("?");
		}

		if (colNames.isEmpty()) {
			sb.append(dbd.emptyInsertClause());
		} else {
			sb.append('(').append(EasikTools.join(", ", colNames)).append(") VALUES (").append(EasikTools.join(", ", values)).append(')');
		}

		dbd.executePreparedUpdate(sb.toString(), input);

		return true;
	}

	/**
	 * Calls promptAndInsert(EntityNode table, DialogOptions dOpts, Set
	 * &lt;ColumnEntry&gt; forced) with an empty set of forced values.
	 * 
	 * @param table
	 *            the table
	 * @param dOpts
	 *            the dialog options
	 * @return dialog result
	 *
	 * @throws SQLException
	 */
	private boolean promptAndInsert(final EntityNode table, final DialogOptions dOpts) throws SQLException {
		return promptAndInsert(table, dOpts, new HashSet<ColumnEntry>(0));
	}

	/**
	 *
	 *
	 * @version 12/09/12
	 * @author Christian Fiddick
	 */
	private class DialogOptions {
		/**  */
		private HashMap<String, EasikType> attToType;

		/**  */
		private LinkedHashMap<String, EntityNode> fKeys;

		/**
		 *
		 *
		 * @param inAttToType
		 * @param inFKeys
		 */
		private DialogOptions(final Map<String, EasikType> inAttToType, final Map<String, EntityNode> inFKeys) {
			attToType = new HashMap<>(inAttToType);
			fKeys = new LinkedHashMap<>(inFKeys);
		}
	}
}
