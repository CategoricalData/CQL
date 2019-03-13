package easik.database.api.jdbc;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import easik.database.base.PersistenceDriver;
import easik.database.base.SketchExporter;
import easik.model.constraint.CommutativeDiagram;
import easik.model.constraint.EqualizerConstraint;
import easik.model.constraint.LimitConstraint;
import easik.model.constraint.ModelConstraint;
import easik.model.constraint.ProductConstraint;
import easik.model.constraint.PullbackConstraint;
import easik.model.constraint.SumConstraint;
import easik.model.path.ModelPath;
import easik.overview.vertex.ViewNode;
import easik.sketch.Sketch;
import easik.sketch.edge.SketchEdge;
import easik.sketch.util.graph.SketchGraphModel;
import easik.sketch.vertex.EntityNode;
import easik.ui.SketchFrame;
import easik.view.vertex.QueryNode;

/**
 * This class is the JDBC specific sketch exporter that should be extended by
 * the specific database exporter (that uses JDBC). This will usually be any
 * driver that exports SQL.
 *
 * @author Christian Fiddick
 * @version Summer 2012, Easik 2.2
 */
public abstract class JDBCExporter extends SketchExporter {
	/**  */
	protected JDBCDriver dbDriver;

	/**  */
	protected boolean hadAutoCommit;

	/**
	 *
	 *
	 * @param sk
	 * @param db
	 * @param exportOpts
	 *
	 * @throws PersistenceDriver.LoadException
	 */
	protected JDBCExporter(final Sketch sk, final JDBCDriver db, final Map<String, ?> exportOpts) throws PersistenceDriver.LoadException {
		super(sk, db, exportOpts);

		if (!db.hasOption("database")) {
			throw new PersistenceDriver.LoadException("SQL export driver requires \"db\" parameter!");
		}

		dbDriver = db;
	}

	/**
	 * Takes a string, returns either a single-element list containing the
	 * string formatted as a comment, or, if comments are suppressed (for
	 * instance, for a db export), an empty list. By default, commenting
	 * involves adding "-- " to the beginning of each line, but individual
	 * drivers may do something else.
	 *
	 * @param text
	 *            to commentize
	 * @return the comments as a list of strings.
	 */
	@Override
	public List<String> comment(final String text) {
		final List<String> ret;

		if (mode == Mode.DATABASE) {
			ret = Collections.emptyList();
		} else {
			ret = Collections.singletonList(startOfLine.matcher(text).replaceAll("-- "));
		}

		return ret;
	}

	/**
	 * Exports the sketch directly to a database.
	 *
	 *
	 * @throws PersistenceDriver.LoadException
	 */
	@Override
	public void exportToNative() throws PersistenceDriver.LoadException {
		mode = Mode.DATABASE;
		$ = ""; // We don't want statement terminators

		Connection conn = null;

		try {
			if (optionEnabled("createDatabase")) {
				dbDriver.createDatabase(optionEnabled("dropDatabase"));
			}

			if (optionEnabled("dropSchema")) {
				dbDriver.dropAndRecreateSchema();
			}

			conn = dbDriver.getConnection();

			preInit(conn);

			for (final String statement : initialize()) {
				// Some things in initialize may be allowed to fail; if we get
				// an exception, pass it to ignoreError,
				// and if it returns true, just keep going.
				try {
					dbDriver.executeUpdate(statement);
				} catch (SQLException e) {
					if (!ignoreError(e)) {
						throw e;
					}
				}
			}

			postInit(conn);

			for (final String statement : createTables()) {
				dbDriver.executeUpdate(statement);
			}

			for (final String statement : createConstraints()) {
				dbDriver.executeUpdate(statement);
			}

			for (final String statement : createExtras(true)) {
				dbDriver.executeUpdate(statement);
			}

			for (final String statement : createViews()) {
				dbDriver.executeUpdate(statement);
			}
			finished(conn);
		} catch (SQLException sqle) {
			System.err.println("Caught exception: " + sqle.getMessage());

			// Catch the exception and rethrow it, but first call aborted() to
			// abort the current transaction.
			// aborted() might throw another SQLException; if so we just ignore
			// it.
			if (conn != null) {
				try {
					aborted(conn);
				} catch (SQLException e2) {
				}
			}
			throw new PersistenceDriver.LoadException(sqle.getMessage());
		}
	}

	/**
	 * Some things in initialize may be allowed to fail; if we get an exception,
	 * it gets passed here, and if this returns true, we ignore that exception
	 * and keep going.
	 *
	 * @param e
	 *            the SQLException that occurred
	 * @return true, if the SQL exception should be ignored, false if it is a
	 *         proper error.
	 */
	protected static boolean ignoreError(final SQLException e) {
		return false;
	}

	/**
	 * Writes the db export SQL script to a file.
	 *
	 * @param outFile
	 *            the file to write to
	 *
	 * @throws IOException
	 */
	@Override
	public void exportToFile(final File outFile) throws IOException {
		final PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(outFile)));

		mode = Mode.FILE;
		$ = dbd.getStatementSeparator();

		// Writes the initial comments, db name, and so on
		for (final String p : initialize()) {
			out.println(p);
		}

		// Now write the table (include foreign key references)
		for (final String t : createTables()) {
			out.println(t);
		}

		// Generate any necessary constraints
		for (final String c : createConstraints()) {
			out.println(c);
		}

		// Generate any additional extras needed by the db
		for (final String x : createExtras(true)) {
			out.println(x);
		}

		// Generare all views on this sketch
		for (final String v : createViews()) {
			out.println(v);
		}

		out.close();
	}

	/**
	 * Returns the SQL export as a string.
	 *
	 * @return sql export
	 */
	@Override
	public String exportToString() {
		mode = Mode.TEXT;
		$ = dbd.getStatementSeparator();

		final StringBuilder sql = new StringBuilder("");

		for (final String p : initialize()) {
			sql.append(p).append(lineSep);
		}

		for (final String t : createTables()) {
			sql.append(t).append(lineSep);
		}

		for (final String c : createConstraints()) {
			sql.append(c).append(lineSep);
		}

		for (final String x : createExtras()) {
			sql.append(x).append(lineSep);
		}

		for (final String v : createViews()) {
			sql.append(v).append(lineSep);
		}

		return sql.toString();
	}

	/**
	 * Called immediately before initialize() when doing a db export. This turns
	 * on auto-commit (it'll be turned off after initialization, in postInit(),
	 * and saved the old auto-commit value to be restored after the export is
	 * complete.
	 *
	 * @param conn
	 *            The connection
	 *
	 * @throws SQLException
	 */
	protected void preInit(final Connection conn) throws SQLException {
		hadAutoCommit = conn.getAutoCommit();

		conn.setAutoCommit(true);
	}

	/**
	 * Called immediately after initialize() when doing a db export. The default
	 * operation turns off auto-commit mode (i.e. it begins a transaction).
	 *
	 * @param conn
	 *            The connection
	 *
	 * @throws SQLException
	 */
	protected static void postInit(final Connection conn) throws SQLException {
		conn.setAutoCommit(false);
	}

	/**
	 * Called upon successful completion of the creation queries. By default the
	 * transaction is committed, the original auto-commit setting of the
	 * connection is restored and we set a flag in our sketch indicating that
	 * we've exported to a db.
	 *
	 * @param conn
	 *            The connection
	 *
	 * @throws SQLException
	 */
	protected void finished(final Connection conn) throws SQLException {
		conn.commit();
		conn.setAutoCommit(hadAutoCommit);
		sketch.setSynced(true);
		sketch.setDirty();
	}

	/**
	 * Called if an SQLException occurs during db creation. The default
	 * operation is to rollback the current transaction, then restore the
	 * original auto-commit setting.
	 *
	 * @param conn
	 *            The connection
	 *
	 * @throws SQLException
	 */
	protected void aborted(final Connection conn) throws SQLException {
		conn.rollback();
		conn.setAutoCommit(hadAutoCommit);
		sketch.getDatabase().cleanDatabaseDriver();
	}

	/**
	 * Returns the list of queries that creates the needed tables, including
	 * foreign key references, but not including data constraints. The default
	 * operation is to figure out an appropriate creation order and then call
	 * createTable(EntityNode, boolean) for each table.
	 *
	 * @return list of queries to run to create the tables
	 */
	protected List<String> createTables() {
		final List<String> sql = new LinkedList<>();

		// Get the entities of the sketch; as we add them to the output string,
		// we remove them
		// from here and add to 'created'. When entities is empty, we're done.
		final Set<EntityNode> entities = new HashSet<>(sketch.getEntities());

		// As we create entities, we add them to this HashSet
		final Set<EntityNode> created = new HashSet<>(entities.size());

		createTables(sql, entities, created);

		return sql;
	}

	/**
	 * Recursive method to create table definitions. Each iteration exhaustively
	 * determines and generates all unconnected EntityNodes, then, if any
	 * EntityNodes remain, arbitrarily removes one (deferring creation of its
	 * references), and recurses, until all tables have been created.
	 *
	 * @param tables
	 *            the sql for the tables
	 * @param entities
	 *            sketch entities to create
	 * @param created
	 *            sketch entites already created
	 */
	private void createTables(final List<String> tables, final Set<EntityNode> entities, final Set<EntityNode> created) {
		NON_EMPTY: while (!entities.isEmpty()) {
			// Start out looking for entities with no edges to so-far uncreated
			// entities
			ENTITIES: for (final EntityNode n : entities) {
				for (final SketchEdge e : n.getOutgoingEdges()) {
					final EntityNode target = e.getTargetEntity();

					// if an EntityNode target isn't already created, skip this
					// vertex (for now)
					if (!created.contains(target)) {
						continue ENTITIES;
					}
				}

				// Also check shadow edges:
				/**
				 * Removing all traces of shadow edges
				 *
				 * for (final SketchEdge e : n.getShadowEdges()) { final
				 * EntityNode target = e.getTargetEntity();
				 * 
				 * // if an EntityNode target isn't already created, skip this
				 * vertex (for now) if (!created.contains(target)) { continue
				 * ENTITIES; } }
				 */

				// If we got here, all the entity's targets exist, so we can go
				// ahead
				// with the table creation.
				tables.addAll(createTable(n, true));
				entities.remove(n);
				created.add(n);

				continue NON_EMPTY; // Restart the non-empty loop, since we've
									// changed entities.
			}

			// If we get here, entities still exist, but they all point to
			// entities that
			// don't yet exist--this can happen, for instance, with: A -> B and
			// B -> A,
			// or (even simpler), a self-reference (A -> A).
			break;
		}

		if (!entities.isEmpty()) {
			final EntityNode someNode = entities.iterator().next();

			tables.addAll(createTable(someNode, false));
			entities.remove(someNode);
			created.add(someNode);

			// Recurse, creating the rest of the tables
			createTables(tables, entities, created);

			// When the remaining tables are created, we are able to create the
			// reference:
			tables.addAll(createReferences(someNode));
		}
	}

	/**
	 * Returns the table creation statement. If includeReferences is true, the
	 * table output should include reference (foreign key) information;
	 * otherwise, this information should be skipped (generally in such a case,
	 * createReferences will be called later to access that information.
	 *
	 * @param table
	 *            the EntityNode of the table to create
	 * @param includeReferences
	 *            a boolean indicating whether or not the generated table scheme
	 *            may include references.
	 * @return a string of SQL that generates the table
	 */
	protected abstract List<String> createTable(EntityNode table, boolean includeReferences);

	/**
	 * Returns a string creating this table's references to other tables.
	 * Normally this method is only called when reference loops exist (since at
	 * least one of the references must be deferred), however implementing
	 * classes are, of course, free to call this method from createTable (if
	 * inline references aren't used/supported, for instance).
	 *
	 * @param table
	 *            the EntityNode of the table whose outgoing references are to
	 *            be created
	 * @return a string of SQL that generates the references
	 */
	protected abstract List<String> createReferences(EntityNode table);

	/**
	 * Generates db constraints from the sketch constraints. This method is not
	 * generally overridden by drivers: the individual createConstraint(...)
	 * methods are called for each existing constraint.
	 *
	 * @return list of queries to create the constraints.
	 */
	protected List<String> createConstraints() {
		final List<String> constraintSQL = new LinkedList<>();
		final List<ModelConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge>> constraints = new ArrayList<>(sketch.getConstraints().values());
		int id = 0;

		for (final ModelConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> c : constraints) {
			id++;

			if (c instanceof CommutativeDiagram) {
				constraintSQL.addAll(createConstraint((CommutativeDiagram<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge>) c, String.valueOf(id)));
			} else if (c instanceof ProductConstraint) {
				constraintSQL.addAll(createConstraint((ProductConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge>) c, String.valueOf(id)));
			} else if (c instanceof PullbackConstraint) {
				constraintSQL.addAll(createConstraint((PullbackConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge>) c, String.valueOf(id)));
			} else if (c instanceof EqualizerConstraint) {
				constraintSQL.addAll(createConstraint((EqualizerConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge>) c, String.valueOf(id)));
			} else if (c instanceof SumConstraint) {
				constraintSQL.addAll(createConstraint((SumConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge>) c, String.valueOf(id)));
			} else if (c instanceof LimitConstraint) {
				constraintSQL.addAll(createConstraint((LimitConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge>) c, String.valueOf(id)));
			} else {
				System.err.println("Unknown constraint type encountered: " + c.getClass());
			}
		}

		return constraintSQL;
	}

	/**
	 * Returns a list of queries to run to create db constraints enforcing the
	 * passed-in commutative diagram.
	 *
	 * @param constraint
	 *            the commutative diagram
	 * @param id
	 *            a unique ID (typically a number) that can be used in
	 *            constraint names
	 * @return list of queries to create db enforcement of the constraint
	 */
	protected abstract List<String> createConstraint(CommutativeDiagram<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> constraint, String id);

	/**
	 * Returns a list of queries to run to create db constraints enforcing the
	 * passed-in product constraint.
	 *
	 * @param constraint
	 *            the product constraint
	 * @param id
	 *            a unique ID (typically a number) that can be used in
	 *            constraint names
	 * @return list of queries to create db enforcement of the constraint
	 */
	protected abstract List<String> createConstraint(ProductConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> constraint, String id);

	/**
	 * Returns a list of queries to run to create db constraints enforcing the
	 * passed-in equalizer constraint.
	 *
	 * @param constraint
	 *            the equalizer constraint
	 * @param id
	 *            a unique ID (typically a number) that can be used in
	 *            constraint names
	 * @return list of queries to create db enforcement of the constraint
	 */
	protected abstract List<String> createConstraint(EqualizerConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> constraint, String id);

	/**
	 * Returns a list of queries to run to create db constraints enforcing the
	 * passed-in pullback.
	 *
	 * @param constraint
	 *            the pullback constraint
	 * @param id
	 *            a unique ID (typically a number) that can be used in
	 *            constraint names
	 * @return list of queries to create db enforcement of the constraint
	 */
	protected abstract List<String> createConstraint(PullbackConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> constraint, String id);

	/**
	 * Returns a list of queries to run to create db constraints enforcing the
	 * passed-in sum constraint.
	 *
	 * @param constraint
	 *            the sum constraint
	 * @param id
	 *            a unique ID (typically a number) that can be used in
	 *            constraint names
	 * @return list of queries to create db enforcement of the constraint
	 */
	protected abstract List<String> createConstraint(SumConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> constraint, String id);

	/**
	 * Returns a list of queries to run to create db constraints enforcing the
	 * passed-in FLC constraint.
	 *
	 * @param constraint
	 *            the FLC constraint
	 * @param id
	 *            a unique ID (typically a number) that can be used in
	 *            constraint names
	 * @return list of queries to create db enforcement of the constraint
	 */
	protected abstract List<String> createConstraint(LimitConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> constraint, String id);

	/**
	 * Used for databases to create any required extra output. This might
	 * includes some sort of "fail" procedure, for example. The default
	 * implementation adds nothing.
	 *
	 * @return a String containing the extra SQL
	 */
	protected List<String> createExtras() {
		return createExtras(false); // Trigger toggle unsupported by default
	}

	/**
	 * Used for databases to create any required extra output. This might
	 * includes some sort of "fail" procedure, for example. The default
	 * implementation adds nothing. You should specify whether trigger toggling
	 * is supported on the target database, as this will affect what extra SQL
	 * is added.
	 *
	 * @param toggleTriggers
	 *            True to enable trigger toggling, false otherwise.
	 * @return a String containing the extra SQL
	 */
	protected abstract List<String> createExtras(boolean toggleTriggers);

	/**
	 * Generates views on the sketch.
	 *
	 * @return list of queries to create our views
	 */
	protected List<String> createViews() {
		final List<String> viewSQL = new LinkedList<>();
		final Set<ViewNode> views = new HashSet<>(sketch.getViews());

		for (final ViewNode node : views) {
			viewSQL.addAll(createView(node));
		}

		return viewSQL;
	}

	/**
	 * Returns the list view creation statements that will create our view.
	 *
	 * @param node
	 *            The view node containing the query nodes making up our view
	 * @return A list of of SQL Strings that will generate the view.
	 */
	protected abstract List<String> createView(ViewNode node);

	/**
	 * Returns an SQL query string that will generate the view represended by a
	 * given query node.
	 *
	 * @param node
	 *            The query node containing the SELECT statements that will
	 *            generate the view.
	 * @return And SQL query string that will generate the view represented by
	 *         our node.
	 */
	protected abstract String createView(QueryNode node);

	/**
	 * Constructs a join clause for this path. For instance, if the path is a
	 * -&gt; b -&gt; c -&gt; d, this would return
	 * <code>a JOIN b ON b.a_id = a.id JOIN c ON c.b_id = b.id JOIN d ON d.c_id = c.id</code>
	 * suitable for use in a select query.
	 *
	 * @param inEdges
	 *            the path for which the selection is being made
	 * @param includeLast
	 *            true if the last path target should be included. If false, the
	 *            last path will be omitted.
	 * @param initialOn
	 *            if non-null, "ON " + initialOn will be added after the path
	 *            source table name
	 * @return the string, suitable for use in a SELECT query.
	 */
	protected String joinPath(final List<SketchEdge> inEdges, final boolean includeLast, final String initialOn) {
		final LinkedList<SketchEdge> edges = new LinkedList<>(inEdges);
		final StringBuilder joinClause = new StringBuilder(quoteId(edges.get(0).getSourceEntity().getName()));

		if (initialOn != null) {
			joinClause.append(" ON ").append(initialOn);
		}

		if (!includeLast && !edges.isEmpty()) {
			edges.removeLast();
		}

		for (final SketchEdge e : edges) {
			final EntityNode target = e.getTargetEntity();

			joinClause.append(" JOIN ").append(quoteId(target)).append(" ON ").append(qualifiedFK(e)).append(" = ").append(qualifiedPK(target));
		}

		return joinClause.toString();
	}

	/**
	 * Constructs a special join clause for this path to be used in an update
	 * statement for constraint consistency in projections.
	 * 
	 * @param p
	 *            the path for which the selection is being made
	 * @return the string, suitable for use in a SELECT query.
	 * 
	 * @author Federico Mora
	 */
	protected String leftJoinPath(final ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> p) {
		final LinkedList<SketchEdge> edges = p.getEdges();
		final StringBuilder joinClause = new StringBuilder();

		Iterator<SketchEdge> iter = edges.descendingIterator();
		iter.next();

		int i = 0;
		while (iter.hasNext()) {
			final SketchEdge e = iter.next();
			final EntityNode source = e.getSourceEntity();
			final EntityNode target = e.getTargetEntity();
			if (i == 0) {
				i++;
				joinClause.append(quoteId(target) + " LEFT JOIN ").append(quoteId(source)).append(" ON ").append(qualifiedPK(target)).append(" = ").append(qualifiedFK(e));
			} else {
				joinClause.append(" LEFT JOIN ").append(quoteId(source)).append(" ON ").append(qualifiedPK(target)).append(" = ").append(qualifiedFK(e));
			}
		}

		return joinClause.toString();
	}

	/**
	 * Shortcut for calling <code>joinPath(edges, wantLast, null)</code>.
	 *
	 * @param edges
	 *            the edges
	 * @param includeLast
	 *            Include the last or not
	 * @return a string of the join path
	 * @see #joinPath(java.util.List< easik.sketch.edge.SketchEdge>, boolean,
	 *      String)
	 */
	protected String joinPath(final List<SketchEdge> edges, final boolean includeLast) {
		return joinPath(edges, includeLast, null);
	}

	/**
	 * Shortcut method for calling
	 * <code>joinPath(path.getEdges(), wantLast)</code>
	 *
	 * @param path
	 *            the edges
	 * @param includeLast
	 *            Include the last or not
	 * @return a string of the join path
	 * @see #joinPath(java.util.List< easik.sketch.edge.SketchEdge>, boolean,
	 *      String)
	 */
	protected String joinPath(final ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> path, final boolean includeLast) {
		return joinPath(path.getEdges(), includeLast, null);
	}

	/**
	 * Shortcut method for calling <code>joinPath(path.getEdges(), true);</code>
	 *
	 * @param edges
	 *            the edges
	 * @return a string of the join path
	 * @see #joinPath(java.util.List< easik.sketch.edge.SketchEdge>, boolean,
	 *      String)
	 */
	protected String joinPath(final List<SketchEdge> edges) {
		return joinPath(edges, true, null);
	}

	/**
	 * Shortcut method for calling <code>joinPath(path, true);</code>.
	 *
	 * @param path
	 *            the edges
	 * @return a string of the join path
	 * @see #joinPath(java.util.List< easik.sketch.edge.SketchEdge>, boolean,
	 *      String)
	 */
	protected String joinPath(final ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> path) {
		return joinPath(path.getEdges(), true, null);
	}
}
