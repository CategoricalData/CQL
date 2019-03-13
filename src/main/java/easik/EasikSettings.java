
/**
 * Properties subclass that loads properties from a .easik.properties file in
 * the user's "HOME" directory.  Properties does most of the work here, but we
 * provide some utility functions to make accessing and converting values
 * slightly easier.
 */
package easik;

//~--- JDK imports ------------------------------------------------------------

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 *
 * @version 12/09/12
 * @author Christian Fiddick
 */
public class EasikSettings extends Properties {
	/**
	 *    
	 */
	private static final long serialVersionUID = -4640076620437956007L;

	/**
	 * The maximum number of files that will be stored in the recent files list.
	 */
	public static final int MAX_NUM_FILES = 8;

	/**  */
	private static final Pattern intPattern = Pattern.compile("^(-?\\d+)");

	/**
	 * Constructs a new EasikSettings object. It loads default.properties as a
	 * set of defaults, then also loads .easik.properties from the user's home
	 * directory.
	 */
	public EasikSettings() {
		try {
			defaults = new Properties();

			defaults.load(this.getClass().getResourceAsStream("/default.properties"));
		} catch (IOException ioe) {
			// Unable to read the file; ignore it.
			defaults = null;
		}

		try {
			load(new FileInputStream(propertiesFile()));
		} catch (IOException ioe) {
			// Cannot read file (most likely it doesn't exist, which is fine).
		}
	}

	/**
	 * Overrides the default keys() method to sort the keys; this is primarily
	 * so that the output file is nicer.
	 *
	 * @return
	 */
	@Override
	public synchronized Enumeration<Object> keys() {
		final List<Object> keys = Collections.list(super.keys());

		Collections.sort(keys, new Comparator<>() {
			@Override
			public int compare(final Object aObj, final Object bObj) {
				final String a = (String) aObj;
				final String b = (String) bObj;

				// Sort things into nicer groups: first all the colors, then
				// the widths, then overview/sketch/view attributes, then
				// finally everything else; within any of these groups, sort
				// by normal String ordering.
				int aPos = 999, bPos = 999;

				if (a.startsWith("color_")) {
					aPos = 10;
				} else if (a.matches("^(?:overview|sketch|view)_(?:display|divider).*")) {
					aPos = 30;
				} else if (a.endsWith("_width")) {
					aPos = 20;
				}

				if (b.startsWith("color_")) {
					bPos = 10;
				} else if (b.matches("^(?:overview|sketch|view)_(?:display|divider).*")) {
					bPos = 30;
				} else if (b.endsWith("_width")) {
					bPos = 20;
				}

				if (aPos < bPos) {
					return -1;
				} else if (aPos > bPos) {
					return 1;
				} else {
					return a.compareTo(b);
				}
			}
		});

		return Collections.enumeration(keys);
	}

	/**
	 * Splits the value on | characters (optionally surrounded by whitespace)
	 * and returns a List of String values.
	 *
	 * @param key
	 *            the property name
	 * @return a Set of Strings
	 * @see #getPropertySet(String)
	 * @see #setProperty(String, java.util.Collection<String>)
	 */
	public List<String> getPropertyList(final String key) {
		final List<String> result = new LinkedList<>();
		final String value = getProperty(key);

		if (value != null) {
			result.addAll(Arrays.asList(value.split("\\s*\\|\\s*")));
		}

		return result;
	}

	/**
	 * Splits the value on | characters (optionally surrounded by whitespace)
	 * and returns a Set of String values. You'd use this instead of getList
	 * when you don't want duplicate values. The set will contain entries in the
	 * same order specified in the properties file.
	 *
	 * @param key
	 *            the property name
	 * @return a Set of Strings
	 * @see #getPropertyList(String)
	 * @see #setProperty(String, java.util.Collection<String>)
	 */
	public Set<String> getPropertySet(final String key) {
		final Set<String> result = new LinkedHashSet<>(10);
		final String value = getProperty(key);

		if (value != null) {
			result.addAll(Arrays.asList(value.split("\\s*\\|\\s*")));
		}

		return result;
	}

	/**
	 * Gets a property, but first converts it to a Color object. Returns null if
	 * the color doesn't exist. The key name should be without the 'color_'
	 * prefix.
	 *
	 * @param keySuffix
	 *            the property name, without the 'color_' prefix
	 * @return a Color object, null if the key doesn't exist
	 */
	public Color getColor(final String keySuffix) {
		final String color = getProperty("color_" + keySuffix);

		if (color == null) {
			return null;
		}

		return Color.decode(color);
	}

	/**
	 * Just like getColor, except it takes a second, default Color value to
	 * return if the color doesn't exist and isn't in the default properties
	 * file.
	 *
	 * @param keySuffix
	 *            the property name, without the 'color_' prefix
	 * @param defColor
	 *            the fallback Color
	 * @return a Color object
	 */
	public Color getColor(final String keySuffix, final Color defColor) {
		final Color color = getColor(keySuffix);

		if (color == null) {
			return defColor;
		}

		return color;
	}

	/**
	 * Returns the string value from the defaults property file.
	 *
	 * @param key
	 *            the property name
	 * @return the property value from the defaults file.
	 */
	public String getDefaultProperty(final String key) {
		return defaults.getProperty(key);
	}

	/**
	 * Returns the Color for a parameter from the default properties file.
	 *
	 * @param keySuffix
	 *            the property name, without the 'color_' prefix
	 * @return a Color object
	 */
	public Color getDefaultColor(final String keySuffix) {
		final String color = defaults.getProperty("color_" + keySuffix);

		if (color == null) {
			return null;
		}

		return Color.decode(color);
	}

	/**
	 * Saves the Color property as an encoded color string. The key name should
	 * be without the 'color_' prefix.
	 *
	 * @param keySuffix
	 *            the property name, without the 'color_' prefix
	 * @param color
	 *            the Color object
	 */
	public void setColor(final String keySuffix, final Color color) {
		if (color.equals(getDefaultColor(keySuffix))) {
			remove("color_" + keySuffix);
		} else {
			Formatter formatter = new Formatter();
			final String colorString = formatter.format("#%06X", color.getRGB() & 0xFFFFFF).toString();

			formatter.close();
			setProperty("color_" + keySuffix, colorString);
		}
	}

	/**
	 * Overriden to clear the property if you try to set it back to its current
	 * value.
	 *
	 * @see java.util.Properties#setProperty(String, String)
	 *
	 * @param key
	 * @param value
	 *
	 * @return
	 */
	@Override
	public synchronized Object setProperty(final String key, final String value) {
		if (value.equals(getDefaultProperty(key))) {
			final String ret = getProperty(key);

			remove(key);

			return ret;
		} 
			return super.setProperty(key, value);
		
	}

	/**
	 * Sets a Collection of String elements as the value, with elements joined
	 * together with ' | '. The converse to getPropertyList and
	 * getPropertySet().
	 *
	 * @param key
	 *            property key
	 * @param values
	 *            the Collection (e.g. List or Set) of property value Strings
	 */
	public void setProperty(final String key, final Collection<String> values) {
		String value = "";

		for (final String v : values) {
			value = value + ("".equals(value) ? "" : " | ") + v;
		}

		setProperty(key, value);
	}

	/**
	 * Sets a boolean value, using the string "true" for a true value, and
	 * "false" for a false value.
	 *
	 * @param key
	 *            the property name to set
	 * @param value
	 *            the boolean value to set
	 */
	public void setBoolean(final String key, final boolean value) {
		setProperty(key, value ? "true" : "false");
	}

	/**
	 * Accesses the default folder. This depends on the 'folder_default'
	 * setting: if it's set to "last", we use 'folder_last'; if set to
	 * "specified" we use 'folder_specified'; otherwise, and if either of those
	 * are null/empty, we use the current directory.
	 *
	 * @return the default folder
	 */
	public String getDefaultFolder() {
		final String mode = getProperty("folder_default");
		String dir = null;

		if ((mode != null) && "last".equals(mode)) {
			dir = getProperty("folder_last");
		} else if ((mode != null) && "specified".equals(mode)) {
			dir = getProperty("folder_specified");
		}

		if (dir == null) {
			dir = System.getProperty("user.dir");
		}

		return dir;
	}

	/**
	 * Adds the specified filename to the list of recent files. In addition to
	 * adding the file to the file set, the file will be brought to the
	 * beginning of the set (and removed first, if it already exists), and the
	 * set will be trimmed to only contain 8 elements.
	 * <p/>
	 * You probably want to call updateRecentFilesMenu() on the main
	 * ApplicationFrame to update the recent files list after calling this.
	 *
	 * @param filename
	 *            recent file
	 */
	public void addRecentFile(final String filename) {
		final Deque<String> recent = (LinkedList<String>) getPropertyList("recent_files");

		recent.remove(filename);
		recent.addFirst(filename);

		while (recent.size() > MAX_NUM_FILES) {
			recent.removeLast();
		}

		setProperty("recent_files", recent);
	}

	/**
	 * Adds the specified File object's filename to the list of recent files.
	 *
	 * @param file
	 *            the File object referencing the file to add to the list
	 * @see #addRecentFile(String)
	 */
	public void addRecentFile(final File file) {
		addRecentFile(file.getPath());
	}

	/**
	 * Stores the current properties to .easik-settings in the user's home
	 * directory.
	 */
	public void store() {
		try {
			final FileOutputStream out = new FileOutputStream(propertiesFile());

			store(out, "Easik preferences file.");
			out.close();
		} catch (IOException ioe) {
			// We failed to write the file; there isn't much we can do about it.
		}
	}

	/**
	 *
	 *
	 * @return
	 */
	private static File propertiesFile() {
		final String homedir = System.getProperty("user.home");

		return new File(homedir, ".easik-settings");
	}

	/**
	 * Parses a value as a double and returns it.
	 *
	 * @param id
	 *            the key the access
	 * @param defaultVal
	 *            the value to return if the key does not exist
	 * @return a double
	 */
	public double getDouble(final String id, final double defaultVal) {
		final String d = getProperty(id);

		if (d == null) {
			return defaultVal;
		}

		try {
			return Double.parseDouble(d);
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}

	/**
	 * Parses a value as a float and returns it. This is just a wrapper around
	 * getDouble().
	 *
	 * @param id
	 *            the key the access
	 * @param defaultVal
	 *            the value to return if the key does not exist
	 * @return a float
	 */
	public float getFloat(final String id, final float defaultVal) {
		return (float) getDouble(id, defaultVal);
	}

	/**
	 * Parses a value as a integer and returns it. We actually are slightly more
	 * lenient than Integer.parseInt(): we use any leading digits, so all of
	 * "456", "456.9" and "456abc" would return the integer 456.
	 *
	 * @param id
	 *            the key the access
	 * @param defaultVal
	 *            the value to return if the key does not exist
	 * @return an int
	 */
	public int getInt(final String id, final int defaultVal) {
		final String i = getProperty(id);

		if (i == null) {
			return defaultVal;
		}

		final Matcher m = intPattern.matcher(i);

		if (!m.find()) {
			return defaultVal;
		} 
			return Integer.parseInt(m.group(1));
		
	}

	/**
	 * Parses a value as a boolean and returns it. This will accept "true"
	 * (case-insentive) and "1" as true values, and anything else as false
	 * values.
	 *
	 * @param id
	 *            the key the access
	 * @param defaultVal
	 *            the value to return if the key does not exist
	 * @return a bool
	 */
	public boolean getBoolean(final String id, final boolean defaultVal) {
		final String i = getProperty(id);

		if (i == null) {
			return defaultVal;
		}

		if ("true".equalsIgnoreCase(i) || "1".equals(i)) {
			return true;
		}

		return false;
	}
}
