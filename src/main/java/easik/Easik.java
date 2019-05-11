package easik;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.io.File;

import javax.swing.SwingUtilities;

import easik.ui.ApplicationFrame;

/**
 * Easik is the starting point of the application. It creates an instance of the
 * Application frame and then allows for user interaction.
 */
public class Easik {
	/** The current version of EASIK. */
	public static final String VERSION = "3.0 -> CQL";

	/**
	 * The current svn revision of Easik.java. This is mainly for internal use,
	 * though it also shows up as a build number in "About Easik".
	 */
	public static final int REVISION = 0;

	/** The instance of this sketch */
	private static Easik _instance;

	/**
	 * The current settings of this application, as defined in the
	 * ~/.easik.properties file
	 */
	private EasikSettings _settings;

	/** The current frame */
	private ApplicationFrame _theFrame;

	/**
	 * Creates an instance of the application frame and then sets it to be visible.
	 * <p>
	 * Creating an instance of this class will start running an instance of the
	 * application.
	 */
	private Easik() {
		_instance = this;
		_settings = new EasikSettings();
		_theFrame = new ApplicationFrame();

		_theFrame.setVisible(true);
	}

	/**
	 * Entry point of the Application. Creates an instance of the application.
	 *
	 * @param args Program parameters; currently ignored
	 */
	public static void main(final String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				createAndShowGUI(args);
			}
		});
	}

	/**
	 *
	 *
	 * @param args
	 */
	public static void createAndShowGUI(final String[] args) {
		// On a mac, set the application to use the normal menus at the top of
		// the screen
		// This has to be *first*, before other AWT/Swing items load up
		// if (EasikConstants.RUNNING_ON_MAC) {
		// System.setProperty("apple.laf.useScreenMenuBar", "true");
		// }
		// this seems to throw lots of null pointer exceptions - ryan TODO aql easik

		final Easik e = Easik.getInstance();
		e._theFrame.toFront();
		// If a filename is specified, try to load it as an overview.
		if (args.length > 0) {
			final File f = new File(args[0]);

			e.getFrame().getOverview().openOverview(f);
		} else { // Otherwise open the most recently used overview if possible
			/*
			 * for (final String file :
			 * getInstance().getSettings().getPropertySet("recent_files")) { File f = new
			 * File(file);
			 * 
			 * if (f.exists() && f.length() != 0) {
			 * getInstance().getFrame().getOverview().openOverview(f);
			 * 
			 * break; } }
			 */
		}
	}

	/**
	 * Singleton method to get the one instance
	 *
	 * @return The one instance.
	 */
	@SuppressWarnings("unused")
	public static Easik getInstance() {
		if (_instance == null) {
			new Easik();
		}
		return _instance;
	}

	/**
	 * Returns the current application frame
	 *
	 * @return The frame
	 */
	public ApplicationFrame getFrame() {
		return _theFrame;
	}

	/**
	 * Returns the EasikSettings containing the current settings
	 *
	 * @return The easik settings
	 * @since 2008-05-08
	 */
	public EasikSettings getSettings() {
		return _settings;
	}

	public static void clear() {
		_instance = null;
	}
}
