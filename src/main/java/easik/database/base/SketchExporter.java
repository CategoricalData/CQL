package easik.database.base;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import easik.EasikConstants;
import easik.EasikTools;
import easik.model.keys.UniqueIndexable;
import easik.sketch.Sketch;
import easik.sketch.edge.InjectiveEdge;
import easik.sketch.edge.SketchEdge;
import easik.sketch.vertex.EntityNode;

/**
 * <p>
 * Base class for exporting a sketch to a file, string, XML or db connection.
 * SketchExporter objects are not created directly, but rather by first getting
 * an API layer object and calling getSketchExporter(Sketch, options) on it.
 * </p>
 *
 * <p>
 * In addition to per-driver options, the following options are recognized by
 * all drivers (though for some drivers, may have no effect).
 * <p/>
 * <ul>
 * <li><code>bigKeys</code> &mdash; if set to the string <code>"true"</code>,
 * the generated tables will use BIGINTs (or equivelant) instead of INTEGERs for
 * primary keys and foreign key columns. Use this if your table could
 * conceivably contain more than 2 billion rows in its lifetime.</li>
 * </ul>
 *
 * @since 2009-08-24 Changed to abstract base clase while a significant part of
 *        the DB exporting functionality moved to a sub-class
 *        easik.database.export.DBSketchExporter.
 *
 * @author Christian Fiddick (I didn't do much here)
 * @version Summer 2012, Easik 2.2
 */
public abstract class SketchExporter extends Options {
	/**  */
	protected Pattern startOfLine = Pattern.compile("^", Pattern.MULTILINE);

	// Records unique keys that get auto-created to enforce constraints during
	// export

	/**  */
	protected UniqueHM addedUnique = new UniqueHM();

	/**
	 * The end-of-statement string, by default a ";". This will be set to the empty
	 * string when exporting directly to a db since queries are sent individually.
	 * Since ";" is only required in TEXT and FILE mode, not in DATABASE mode,
	 * <code>$</code> should always be appended to statements requiring a
	 * terminator.
	 */
	protected String $;

	/**  */
	protected ColumnNaming colNaming;

	/**
	 * The db driver. We need this even when not connecting to a db for handling
	 * quoting, identifier cleaning, and column naming (via ColumnNaming).
	 */
	protected PersistenceDriver dbd;

	/**  */
	protected String lineSep;

	/**
	 * The current export mode.
	 */
	protected Mode mode;

	/**  */
	protected Sketch sketch;

	/**
	 * The available export modes. Mode.TEXT is for output that will be returned as
	 * a single string; Mode.FILE is for writing to a file; Mode.DATABASE is for an
	 * export being sent directly to a db. Currently, the main different between
	 * them is that comments are suppressed from a db export.
	 */
	protected enum Mode {
		TEXT, FILE, DATABASE
	}

	/**
	 * Protected constructor to be used by subclasses to set common fields.
	 *
	 * @param sk         The sketch to export
	 * @param db         The db driver
	 * @param exportOpts options
	 */
	protected SketchExporter(final Sketch sk, final PersistenceDriver db, final Map<String, ?> exportOpts) {
		dbd = db;
		sketch = sk;
		options = new HashMap<>(exportOpts);
		$ = dbd.getStatementSeparator();
		colNaming = new ColumnNaming(db);
		lineSep = EasikTools.systemLineSeparator();
	}

	/**
	 * Returns the sketch being exported.
	 *
	 * @return the exported sketch
	 */
	public Sketch getMModel() {
		return sketch;
	}

	/**
	 * Returns the initialization string, which is a comment containing the export
	 * date and Easik name and URL.
	 *
	 * @return a comment
	 */
	protected List<String> initialize() {
		final String now = new SimpleDateFormat("d MMMM yyyy, h:mm:ss a z").format(new Date());

		return comment("Created " + now + lineSep + "Created using EASIK, " + EasikConstants.EASIK_URL);
	}

	/**
	 * Takes a string, returns either a single-element list containing the string
	 * formatted as a comment, or, if comments are suppressed (for instance, for a
	 * db export), an empty list.
	 *
	 * @param text to commentize
	 * @return the comments as a list of strings.
	 */
	public abstract List<String> comment(final String text);

	/**
	 * Writes the export script to a file.
	 *
	 * @param outFile the file to write to
	 *
	 * @throws IOException
	 */
	public abstract void exportToFile(final File outFile) throws IOException;

	/**
	 * Returns the export as a string.
	 *
	 * @return sql export
	 */
	public abstract String exportToString();

	/**
	 * Exports the db directly to native storage.
	 * <p/>
	 * For example, db for a db driver. The db driver this export object was created
	 * from must support Database().
	 *
	 * @throws PersistenceDriver.LoadException if the db driver cannot be loaded
	 */
	public abstract void exportToNative() throws PersistenceDriver.LoadException;

	/*
	 * These are convenience wrappers around the same methods in
	 * easik.db.ColumnNaming. See that class for documentation.
	 */

	/**
	 *
	 *
	 * @param table
	 *
	 * @return
	 */
	public String tablePK(final EntityNode table) {
		return colNaming.tablePK(table);
	}

	/**
	 *
	 *
	 * @param table
	 *
	 * @return
	 */
	public String qualifiedPK(final EntityNode table) {
		return colNaming.qualifiedPK(table);
	}

	/**
	 *
	 *
	 * @param edge
	 *
	 * @return
	 */
	public String tableFK(final SketchEdge edge) {
		return colNaming.tableFK(edge);
	}

	/**
	 *
	 *
	 * @param edge
	 *
	 * @return
	 */
	public String qualifiedFK(final SketchEdge edge) {
		return colNaming.qualifiedFK(edge);
	}

	/*
	 * Simple convenience wrappers around the same methods in the Database
	 */

	/**
	 *
	 *
	 * @param identifier
	 *
	 * @return
	 */
	public String cleanId(final Object identifier) {
		return dbd.cleanId(identifier);
	}

	/**
	 *
	 *
	 * @param identifier
	 *
	 * @return
	 */
	public String quoteId(final Object identifier) {
		return dbd.quoteId(identifier);
	}

	/**
	 *
	 *
	 * @version 12/09/12
	 * @author Christian Fiddick
	 */
	protected class UniqueHM extends HashMap<EntityNode, LinkedList<HashSet<SketchEdge>>> {
		/**  */
		private static final int DEFAULT_COLLECTION_INITIAL_CAPACITY = 20;

		/**  */
		private static final long serialVersionUID = 1;

		/**
		 *
		 *
		 * @return
		 */
		@Override
		public Object clone() {
			return super.clone();
		}

		/*
		 * Returns true if the EntityNode plus uniques auto-generated by the export
		 * already enforce uniqueness on the provided edges
		 */

		/**
		 *
		 *
		 * @param node
		 * @param edges
		 *
		 * @return
		 */
		@SuppressWarnings("unlikely-arg-type")
		public boolean isUnique(final EntityNode node, final SketchEdge... edges) {
			// If any of the edges is an injective edge, it's unique (so
			// anything containing it is, too)
			for (final SketchEdge e : edges) {
				if (e instanceof InjectiveEdge) {
					return true;
				}
			}

			// All SketchEdges other than InjectiveEdges are unique-indexable:
			final Set<UniqueIndexable> test = new HashSet<>(DEFAULT_COLLECTION_INITIAL_CAPACITY);

			for (final SketchEdge e : edges) {
				if (e instanceof UniqueIndexable) {
					test.add((UniqueIndexable) e);
				}
			}

			// If the node has a unique key enforcing uniqueness, we're good:
			if (node.uniqueKeyOn(test) != null) {
				return true;
			}

			// Otherwise go through all our added sets, and return true is any
			// of them
			// is a subset of test
			final Iterable<HashSet<SketchEdge>> nodeUniques = get(node);

			if (nodeUniques != null) {
				for (final Set<SketchEdge> unique : nodeUniques) {
					if (test.containsAll(unique)) {
						return true;
					}
				}
			}

			return false;
		}

		/*
		 * Adds the specified SketchEdges as a new unique set associated with the
		 * EntityNode
		 */

		/**
		 *
		 *
		 * @param node
		 * @param edges
		 */
		public void add(final EntityNode node, final SketchEdge... edges) {
			Collection<HashSet<SketchEdge>> nodeUniques = get(node);

			if (nodeUniques == null) {
				nodeUniques = new LinkedList<>();
			}

			nodeUniques.add(new HashSet<>(Arrays.asList(edges)));
		}
	}
}
