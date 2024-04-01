package easik.database.db.XSD;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.util.HashMap;
import java.util.Map;

import easik.database.api.xmldb.XMLDBDriver;
import easik.database.types.Blob;
import easik.database.types.EasikType;
import easik.database.types.Text;
import easik.database.types.Timestamp;
import easik.sketch.Sketch;

/**
 * This is meant to be a driver for use in exporting XSD to an XML database, but
 * this is really only a stub right now (with a few implemented methods).
 *
 * @author Christian Fiddick
 * @version Summer 2012, Easik 2.2
 */
public class XSD extends XMLDBDriver {
  /**
   * Creates a new XSD database driver object. Should not be called directly.
   *
   * @param opts The options for a MySQL database for compatibility
   *
   * @throws LoadException
   */
  public XSD(final Map<String, ?> opts) throws LoadException {
    if (null == opts) {
      options = new HashMap<>(10);
    } else {
      options = new HashMap<>(opts);
    }
  }

  /**
   * Creates a new SketchExporter object for this XML:DB driver. The db parameter
   * must have been specified when this db driver was created.
   *
   * @param sketch
   * @param exportOpts
   *
   * @return
   *
   * @throws LoadException
   */
  @Override
  public XSDExporter getSketchExporter(final Sketch sketch, final Map<String, ?> exportOpts) throws LoadException {
    return new XSDExporter(sketch, this, exportOpts);
  }

  /**
   * Takes an object, and returns an appropriate db name.
   *
   * @param id the identifier to be cleaned up
   * @return the db-safe identifier string which, if quoteIdentifiers is enabled,
   *         might only be safe after going through quoteId()
   */
  @Override
  public String cleanId(final Object id) {
    if (optionEnabled("quoteIdentifiers")) {
      return id.toString().replaceAll("`", "``");
    }
    return super.cleanId(id);

  }

  /**
   * Quotes an identifier (table name, column name, etc.).
   *
   * @param id the name to quote. identifier.toString() will be the value used.
   * @return the (possibly) quoted, db-safe string
   */
  @Override
  public String quoteId(final Object id) {
    final String cleaned = cleanId(id);

    if (optionEnabled("quoteIdentifiers")) {
      return '`' + cleaned + '`';
    }
    return cleaned;

  }

  /**
   * Takes an EasikType and returns the string representation of that type, or
   * something as close as possible for this particular db.
   *
   * @param type the EasikType object desired
   * @return the string containing the type
   */
  @Override
  public String getTypeString(final EasikType type) {
    if (type instanceof easik.database.types.Float) {
      return "REAL";
    } else if (type instanceof Text) {
      return "LONGTEXT";
    } else if (type instanceof Blob) {
      return "LONGBLOB";
    } else if (type instanceof Timestamp) {
      return "DATETIME";
    } else {
      return type.toString();
    }
  }

  // Below are some connectivity stubs that don't do anything since no
  // XML:DB's are currently supported

  /**
   *
   *
   * @return
   */
  @Override
  public boolean isConnectable() {
    return false;
  }

  /**
   *
   *
   * @return
   */
  @Override
  public boolean hasConnection() {
    return false;
  }

  /**
   *
   *
   * @param <T>
   *
   * @return
   *
   * @throws Exception
   */
  @Override
  public <T> T getConnection() throws Exception {
    return null;
  }

  /**
   *
   *
   * @throws Exception
   */
  @Override
  public void disconnect() throws Exception {
  }

  /**
   *
   *
   * @param sketch
   *
   * @throws Exception
   */
  @Override
  public void overrideConstraints(Sketch sketch) throws Exception {
  }

  /**
   *
   *
   * @return
   */
  @Override
  public String getStatementSeparator() {
    return null;
  }
}
