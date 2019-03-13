package easik.xml;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.util.Stack;

import easik.EasikTools;

/**
 * Hacky class to indent the XML Schema output by Easik
 *
 * This has probably been written a few thousand times already, but what the
 * heck.
 *
 * Simple algorithm:
 * <ul>
 * <li>Loop through the characters, use a stack to push/pop the start/end
 * tags</li>
 * <li>insert some spaces after lineendings, with the number depending on the
 * size of the stack.</li>
 * <li>Handle a few special cases along the way.</li>
 * </ul>
 *
 * @author brett.giles@drogar.com Date: Aug 26, 2009 Time: 9:54:11 AM
 * @version $$Id$$
 */
public class Prettify {
	/**  */
	private StringBuilder prettied;

	/**  */
	private Stack<String> tags;

	/**
	 *
	 *
	 * @param toBePrettied
	 */
	public Prettify(final CharSequence toBePrettied) {
		final String SPACES = "                                                                                        ";
		final String lineSep = EasikTools.systemLineSeparator();
		final int len = toBePrettied.length();

		prettied = new StringBuilder(len + 120);
		tags = new Stack<>();

		for (int i = 0; i < len; i++) {
			prettied.append(toBePrettied.charAt(i));

			if (i + 2 < len) {
				if ("/>".equals(toBePrettied.subSequence(i, i + 2))) {
					if (!tags.isEmpty()) {
						tags.pop();
					}
				}
			}

			if (i + lineSep.length() < len) // noinspection
											// AssignmentToForLoopParameter
			{
				if (lineSep.equals(toBePrettied.subSequence(i, i + lineSep.length()))) {
					prettied.append(toBePrettied.subSequence(i + 1, i + lineSep.length()));

					i += lineSep.length() - 1;

					try {
						final int closeTagAdd = adjustSpace(toBePrettied, i);

						prettied.append(SPACES, 0, closeTagAdd + 2 * (tags.size() - 1));
					} catch (StringIndexOutOfBoundsException e) {
						// Ignore, don't add space at the end.
					}
				}
			}
		}
	}

	/**
	 *
	 *
	 * @param tbp
	 * @param i
	 *
	 * @return
	 *
	 * @throws StringIndexOutOfBoundsException
	 */
	private int adjustSpace(final CharSequence tbp, final int i) throws StringIndexOutOfBoundsException {
		if (tbp.charAt(i + 1) != '<') {
			return 0; // Skip documentation
		}

		int loc = i + 2; // Past the '<'

		switch (tbp.charAt(loc)) {
		case '?':
			break; // Just process command

		case '-':
			break; // just process comment

		case '/':
			if (tags.empty()) {
				return 0; // Nothing to pop
			}

			final String tos = tags.peek();
			final CharSequence endTag = tbp.subSequence(loc + 1, loc + 1 + tos.length());

			if (tos.equals(endTag)) {
				tags.pop();

				return 2;
			}

			break;

		default:
			final StringBuilder tag = new StringBuilder(20);

			while ((tbp.charAt(loc) != ' ') && (tbp.charAt(loc) != '>') && (tbp.charAt(loc) != '/') && (tbp.charAt(loc) != '\t')) {
				tag.append(tbp.charAt(loc));

				loc++;
			}

			if (tags.empty()) {
				tags.push(tag.toString());
			} else {
				final String stag = tags.peek();

				if (!tag.toString().equals(stag)) { // Note negation
					tags.push(tag.toString());
				}
			}

			break;
		}

		return 0;
	}

	/**
	 *
	 *
	 * @return
	 */
	@Override
	public String toString() {
		return prettied.toString();
	}
}
