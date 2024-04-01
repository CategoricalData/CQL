package easik;

//~--- JDK imports ------------------------------------------------------------

import java.text.SimpleDateFormat;

/**
 * This class contains constants/objects used in various places in EASIK
 */
public class EasikConstants {
  /** The Easik homepage, displayed in various places. */
  public static final String EASIK_URL = "http://mathcs.mta.ca/research/rosebrugh/Easik/";

  /**
   * The name of the OS Easik is running on. Just an alias for
   * System.getProperty("os.name").
   */
  public static final String OS_NAME = System.getProperty("os.name");

  /** Will be true if running Windows */
  public static final boolean RUNNING_ON_WINDOWS = OS_NAME.contains("Windows");

  /** Will be true if running Mac OS X */
  public static final boolean RUNNING_ON_MAC = !RUNNING_ON_WINDOWS && OS_NAME.contains("Mac OS X");

  /** Will be true if running Linux */
  public static final boolean RUNNING_ON_LINUX = !RUNNING_ON_WINDOWS && !RUNNING_ON_MAC && OS_NAME.contains("Linux");

  /**
   * DATE_SHORT is a SimpleDateFormat object that formats dates into strings such
   * as: "8-Jul-2008"
   */
  public static final SimpleDateFormat DATE_SHORT = new SimpleDateFormat("d-MMM-yyyy");

  /**
   * DATE_LONG is a SimpleDateFormat object that formats dates into strings such
   * as: "Tuesday, July 8, 2008"
   */
  public static final SimpleDateFormat DATE_LONG = new SimpleDateFormat("EEEE, MMMM d, yyyy");

  /**
   * DATETIME_SHORT is a SimpleDateFormat object that formats dates into strings
   * such as: "8-Jul-2008, 2:37 PM"
   */
  public static final SimpleDateFormat DATETIME_SHORT = new SimpleDateFormat("d-MMM-yyyy, h:mm a");

  // Formatting objects for formatting Dates in various places in Easik.

  /**
   * DATETIME_LONG is a SimpleDateFormat object that formats dates into strings
   * such as: "Tuesday, July 8, 2008, 2:37:15 PM"
   */
  public static final SimpleDateFormat DATETIME_LONG = new SimpleDateFormat("EEEE, MMMM d, yyyy, h:mm:ss a");

  /**
   * TIME_LONG is a SimpleDateFormat object that formats dates into strings such
   * as: "2:37:15 PM"
   */
  public static final SimpleDateFormat TIME_LONG = new SimpleDateFormat("h:mm:ss a");

  /**
   * TIME_SHORT is a SimpleDateFormat object that formats dates into strings such
   * as: "2:37 PM"
   */
  public static final SimpleDateFormat TIME_SHORT = new SimpleDateFormat("h:mm a");

  /** A Date formatter for the standard XML timestamp format */
  public static final SimpleDateFormat XML_DATETIME = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
}
