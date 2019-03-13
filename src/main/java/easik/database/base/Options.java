package easik.database.base;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;

/**
 * Simple class that gives an inheriting class a protected
 * <code>Map&lt;String, ?&gt;</code> field named options, and public, read-only
 * methods to query that option map for values. Subclasses should set the
 * options field in their constructor, before attempting to call any methods.
 */
public abstract class Options {
	/**  */
	protected Map<String, Object> options;

	/**
	 * Returns the value of an option.
	 *
	 * @param key
	 *            - key of option
	 * @return the value of the key
	 */
	public Object getOption(final String key) {
		return options.get(key);
	}

	/**
	 * Returns the String value of an option, or null if the option doesn't
	 * exist.
	 * 
	 * @param key
	 *            - key of option
	 * @return the value of the key
	 */
	public String getOptionString(final String key) {
		final Object v = options.get(key);

		if (v == null) {
			return null;
		}

		return v.toString();
	}

	/**
	 * Returns true if the option with key specified by <code>key</code> is
	 * stringwise-equal to the value specified by <code>value</code>. Returns
	 * false if <code>key</code> is not set, is null, or is not equal.
	 *
	 * @param key
	 *            the key to look up
	 * @param value
	 *            the value to test it against
	 * @return boolean indicating that the key is present and equal
	 */
	public boolean optionEquals(final String key, final String value) {
		final Object v = options.get(key);

		return (v != null) && v.toString().equals(value);
	}

	/**
	 * Returns true if the option exists and is equal to <code>"true"</code>.
	 * This is a shortcut for calling <code>optionEquals(key, "true")</code>.
	 *
	 * @param key
	 *            the key to look up
	 * @return boolean indicating that the key is present and is "true"
	 */
	public boolean optionEnabled(final String key) {
		return optionEquals(key, "true");
	}

	/**
	 * Returns true if the option exists, is non-null, and its string value
	 * matches the provided pattern.
	 *
	 * @param key
	 *            the key name
	 * @param pattern
	 *            the regular expression
	 * @return true if the key exists and matches the pattern
	 * @see String#matches(String)
	 */
	public boolean optionMatches(final String key, final String pattern) {
		final Object v = options.get(key);

		return (v != null) && v.toString().matches(pattern);
	}

	/**
	 * Returns true if the option exists, is not null, and has a positive,
	 * non-zero string length.
	 *
	 * @param key
	 *            the key name
	 * @return true if the key exists, is not null, and is not empty
	 */
	public boolean hasOption(final String key) {
		final Object v = options.get(key);

		return (v != null) && (v.toString().length() > 0);
	}
}
