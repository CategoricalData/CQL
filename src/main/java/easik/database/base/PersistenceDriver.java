package easik.database.base;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.util.Map;

import easik.database.types.EasikType;
import easik.model.constraint.EqualizerConstraint;
import easik.model.constraint.LimitConstraint;
import easik.model.constraint.ModelConstraint;
import easik.model.constraint.ProductConstraint;
import easik.model.constraint.PullbackConstraint;
import easik.model.constraint.SumConstraint;
import easik.sketch.Sketch;
import easik.sketch.edge.SketchEdge;
import easik.sketch.util.graph.SketchGraphModel;
import easik.sketch.vertex.EntityNode;
import easik.ui.SketchFrame;

/**
 * The base class of all driver functionality.
 *
 * @author Christian Fiddick
 * @version Summer 2012, Easik 2.2
 */
public abstract class PersistenceDriver extends Options {
	/**
	 * Returns true if the driver supports making a connection and enough
	 * options have been provided to establish a connection.
	 *
	 * @return True if a connection is possible, false otherwise
	 */
	public abstract boolean isConnectable();

	/**
	 * Determines if this driver currently has an active connection.
	 *
	 * @return True if a connection is available, false otherwise
	 */
	public abstract boolean hasConnection();

	/**
	 * Get a connection from this driver. Just return null if not appropriate.
	 *
	 *
	 * @param <T>
	 * @return T Connection
	 * @throws Exception
	 */
	public abstract <T> T getConnection() throws Exception;

	/**
	 * Disconnect from the current connection (if any).
	 *
	 * @throws Exception
	 */
	public abstract void disconnect() throws Exception;

	/**
	 * Override the constraints and allow user updates. This usually amounts to
	 * toggling triggers which is database dependent.
	 *
	 * @param sketch
	 *            Sketch to override triggers for
	 * @throws Exception
	 */
	public abstract void overrideConstraints(Sketch sketch) throws Exception;

	/**
	 * The statement separator for general statements. This is generally only
	 * used when generating multiple SQL statements (e.g. into a file), not when
	 * performing ordinary single db statements.
	 *
	 * @return the string ""
	 */
	public abstract String getStatementSeparator();

	/**
	 * Quotes an identifier (table name, column name, etc.). By default, this
	 * just returns the name trimmed, with spaces changed to underscores,
	 * however many base recognize the "quoteIdentifiers" option, which, when
	 * enabled, allows various other characters including whitespace. See MySQL
	 * subclasses for details. If a driver overrides this method, it *must*
	 * recognize the "quoteIdentifiers" option.
	 *
	 * @param identifier
	 *            the name to quote. identifier.toString() will be the value
	 *            used.
	 * @return the (possibly) quoted, db-safe string
	 */
	public String quoteId(final Object identifier) {
		// By default, we just pass it off to cleanId
		return cleanId(identifier);
	}

	/**
	 * Takes an object, and returns an appropriate db name. By default, this
	 * means trimming the name, changing all sequences of one or more non-word
	 * characters (i.e. anything other than a-z, A-Z, 0-9, and _) to a single
	 * underscore. This name is meant to be used directly when embedding in
	 * another name; if the object is to be used as an identifier in its
	 * entirety, quoteId() should be called instead (which internally calls
	 * this). If a driver supports the quoteIdentifiers option, it should
	 * override this method to strip as little as is required when
	 * quoteIdentifiers is enabled.
	 *
	 * @param identifier
	 *            the identifier to be cleaned up
	 * @return the db-safe identifier string which, if quoteIdentifiers is
	 *         enabled, might only be safe after going through quoteId()
	 * @see #quoteId(Object)
	 */
	@SuppressWarnings("static-method")
	public String cleanId(final Object identifier) {
		if (identifier == null) {
			return null;
		}

		return identifier.toString().trim().replaceAll("\\W+", "_");
	}

	/**
	 * This creates and returns a SketchExporter instance for this driver type.
	 *
	 * @param sketch
	 *            the Sketch being exported
	 * @param exportOptions
	 *            the options for the export, such as db names, create db
	 *            options, etc. See SketchExporter and individual export base
	 *            for supported options.
	 * @return the SketchExporter for this db
	 * @throws LoadException
	 */
	public abstract SketchExporter getSketchExporter(final Sketch sketch, final Map<String, ?> exportOptions) throws LoadException;

	/**
	 * Takes a db EasikType and returns the string representation of that type,
	 * or something as close as possible for this particular db.
	 *
	 * @param type
	 *            the EasikType object desired
	 * @return the string containing the type
	 */
	@SuppressWarnings("static-method")
	public String getTypeString(final EasikType type) {
		return type.toString(); // By default, just do whatever the type
								// implements
	}

	/**
	 * Called from the sketch frame for determining if the insert/delete options
	 * should be enabled in the popup menu.
	 *
	 * @param entity
	 *            the node to check if it is editable
	 * @return true if not constrainted
	 */
	@SuppressWarnings("static-method")
	public boolean editable(final EntityNode entity) {
		for (final ModelConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> c : entity.getConstraints()) {
			// this is the sum in c iff this is the codomain of any path in c
			if ((c instanceof SumConstraint) && (c.getPaths().get(0).getCoDomain() == entity)) {
				return false;
			}

			// this is the product in c iff this is the domain of any path in c
			if ((c instanceof ProductConstraint) && (c.getPaths().get(0).getDomain() == entity)) {
				return false;
			}

			if ((c instanceof PullbackConstraint) && ((PullbackConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge>) c).getSource() == entity) {
				return false;
			}
			if ((c instanceof EqualizerConstraint) && ((EqualizerConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge>) c).getEqualizerEntity() == entity) {
				return false;
			}
			if ((c instanceof LimitConstraint) && c.getRoot() == entity) {
				return false; // TRIANGLES TODO CF2012 is this right?
			}
		}

		return true;
	}

	/**
	 *
	 *
	 * @version 12/09/12
	 * @author Christian Fiddick
	 */
	public static class LoadException extends Exception {
		/**  */
		private static final long serialVersionUID = 1;

		/**
		 *
		 *
		 * @param message
		 */
		public LoadException(final String message) {
			super(message);
		}

		/**
		 *
		 *
		 * @param cause
		 */
		public LoadException(final Throwable cause) {
			this((cause == null) ? "An unknown error occured" : cause.getMessage(), cause);
		}

		/**
		 *
		 *
		 * @param message
		 * @param cause
		 */
		public LoadException(final String message, final Throwable cause) {
			super(message + " (" + cause.getClass().toString() + ')', cause);
		}
	}
}
