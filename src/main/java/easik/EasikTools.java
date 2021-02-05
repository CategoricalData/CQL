package easik;

//~--- JDK imports ------------------------------------------------------------

import java.awt.Color;
import java.util.Collection;
import java.util.HashMap;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Various useful small utility functions that are used in multiple places
 * within Easik. Commonly-used algorithms in Easik are good candidates for
 * moving to this file.
 */
public class EasikTools {
	/**  */
	private static final Pattern endDigits = Pattern.compile("(\\d+)$");

	/**  */
	private static HashMap<Color, Boolean> usedColors = new HashMap<>(); // colors
																			// being
																			// used
																			// elsewhere
																			// in
																			// Easik

	/**  */
	private static boolean hardToFindUnusedCol = false; // will probably never
														// be true, but just in
														// case we won't waste
														// time searching for
														// one

	/**
	 * Takes a name, such as "name" or "name5", and increments or adds a number as
	 * appropriate. For example, passing "name" would return "name2", while "name0"
	 * would return "name1".
	 * 
	 * @param name input
	 * @return incremented name
	 */
	public static String incrementName(final String name) {
		final Matcher m = endDigits.matcher(name);

		if (m.find()) {
			return m.replaceFirst(String.valueOf((Integer.parseInt(m.group(1)) + 1)));
		} // No digits, so we have something like blah, so change it to
			// blah2

		return name + 2;

	}

	/**
	 * Joins strings together on a delimiter, like Perl's join function or PHP's
	 * implode function.
	 *
	 * @param delimiter the delimiter to join the pieces with
	 * @param pieces    the strings to join
	 * @return the joined string
	 */
	public static String join(final String delimiter, final Object... pieces) {
		final StringBuilder result = new StringBuilder(String.valueOf(pieces[0]));

		for (int i = 1; i < pieces.length; i++) {
			result.append(delimiter).append(String.valueOf(pieces[i]));
		}

		return result.toString();
	}

	/**
	 * Just like join(), but takes any sort of String Collection (LinkedList,
	 * ArrayList, Set, etc .).
	 * 
	 * @param delimiter the delim
	 * @param pieces    the pieces
	 * @return joined string
	 */
	public static String join(final String delimiter, final Collection<?> pieces) {
		return join(delimiter, pieces.toArray());
	}

	/**
	 *
	 *
	 * @return
	 */
	public static String systemLineSeparator() {
		try {
			return System.getProperty("line.separator");
		} catch (Exception e) {
			// Assume /n
			// noinspection HardcodedFileSeparator
			return "\n"; // TODO aql changed from /n by ryan
		}
	}

	/**
	 * TODO CF2012 Just load from preset list Get a (probably) unused random color.
	 * 
	 * @param transparency Transparency of color (0-255)
	 * @return A random, unused color
	 */
	public static Color getUnusedColor(int transparency) {
		final int COLOR_LIM = 200; // this ensures that the color is dark enough
									// to be visible over a white surface
		final int MAX_ATTEMPTS_COLOR = 500;
		Random rgen = new Random();

		// Don't worry about transparency for now, just look for an unused plain
		// RGB color
		Color randomCol = new Color(rgen.nextInt(COLOR_LIM), rgen.nextInt(COLOR_LIM), rgen.nextInt(COLOR_LIM));

		if (!hardToFindUnusedCol) { // don't waste time looping
			int i = 0;

			while (usedColors.containsKey(randomCol) && (i < MAX_ATTEMPTS_COLOR)) {
				i++;

				randomCol = new Color(rgen.nextInt(COLOR_LIM), rgen.nextInt(COLOR_LIM), rgen.nextInt(COLOR_LIM));
			}

			if (i >= MAX_ATTEMPTS_COLOR) {
				hardToFindUnusedCol = true;
			}

			usedColors.put(randomCol, true);
		}

		return new Color(randomCol.getRed(), randomCol.getGreen(), randomCol.getBlue(), transparency);
	}
}
