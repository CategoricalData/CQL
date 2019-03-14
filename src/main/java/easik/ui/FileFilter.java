package easik.ui;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Pattern;

/**
 * Quick FileFilter class for handling the very common case of filter on file
 * extension. This is typically used to match by extension, but can also be used
 * to match filenames by a regular expression.
 *
 * Because this inherits from java.io.FilenameFilter, this object can be used
 * with java.io.File.list() and java.awt.FileDialog.
 */
public class FileFilter extends javax.swing.filechooser.FileFilter implements FilenameFilter {
	/**
	 * Returns a filter for EASIK files (.easik).
	 */
	public static final FileFilter EASIK = new FileFilter("EASIK Files", "easik");

	/**
	 * Returns a filter for EASIK sketch files (.sketch and .xml [for old versions
	 * of Easik]).
	 */
	public static final FileFilter EASIK_SKETCH = new FileFilter("EASIK Files", "sketch", "xml");

	/**  */
	private String description;

	/**  */
	private Pattern filenamePattern;

	/**
	 * Constructs a new FileFilter using the passed in regular expression Pattern as
	 * the filename filter. Example usage:
	 * <code>FileFilter ff = new FileFilter("Image files", Pattern.compile("^.*\\.(?:gif|jpe?g|png)$", Pattern.CASE_INSENSITIVE))</code>
	 *
	 * @param pattern a Pattern object representing the regex, typical created with
	 *                Pattern.compile(...)
	 * @param desc    the file type description that *may* be shown to the user
	 * @see java.util.regex.Pattern
	 */
	public FileFilter(final String desc, final Pattern pattern) {
		filenamePattern = pattern;
		description = desc;
	}

	/**
	 * Creates a new FileFilter that matches files ending with the provided
	 * extension(s). Note that the match performed will be case-insensitive. For
	 * example:
	 * <code>FileFilter ff = new FileFilter("Image files", "png", "bmp", "jpg", "gif");</code>
	 *
	 * @param desc       the file type description that *may* be shown to the user
	 * @param extensions the filename extensions to match (any number of extensions
	 *                   can be specified)
	 */
	public FileFilter(final String desc, final String... extensions) {
		final StringBuilder extPat = new StringBuilder("");

		for (String ext : extensions) {
			if (extPat.length() > 0) {
				extPat.append('|');
			}

			if (".".equals(ext.substring(1, 2))) // If the user passed in
													// ".blah", remove the ".":
			{
				ext = ext.substring(1);
			}

			extPat.append(Pattern.quote(ext));
		}

		// noinspection HardcodedFileSeparator
		filenamePattern = Pattern.compile("\\.(?:" + extPat + ")$");
		description = desc;
	}

	/**
	 * Returns true if the filename is one we want to include, false otherwise. Used
	 * automatically when used with an AWT FileDialog.
	 *
	 * @param dir      The directory containing the file being considered
	 * @param filename The filename being considered
	 * @return true if the file is of an acceptable type
	 */
	@Override
	public boolean accept(final File dir, final String filename) {
		if (new File(dir, filename).isDirectory() || filenamePattern.matcher(filename).find()) {
			return true;
		}

		return false;
	}

	/**
	 * Returns true if the filename is one we want to include, false otherwise. Used
	 * automatically when used with an swing JFileChooser.
	 *
	 * @param f The file being considered
	 * @return true if the file is of an acceptable type
	 */
	@Override
	public boolean accept(final File f) {
		return accept(f.getParentFile(), f.getName());
	}

	/**
	 *
	 *
	 * @return
	 */
	@Override
	public String getDescription() {
		return description;
	}
}
