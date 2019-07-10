package catdata.ide;

import java.awt.Desktop;
import java.awt.HeadlessException;
import java.awt.MenuBar;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import catdata.DeadLockDetector;
import catdata.Pair;
import catdata.Util;
import catdata.aql.exp.AqlParserFactory;
import catdata.ide.IdeOptions.IdeOption;

/**
 * 
 * @author ryan
 * 
 *         Program entry point.
 */
public class IDE {

	public static void main(String... args) {
		final Options options = new Options();

		/*
		 * options.addOption(Option.builder("p") .longOpt("aqlparser") .required(false)
		 * .desc("aql parser engine") .hasArg() .build());
		 */

		options.addOption(Option.builder("i").longOpt("input").required(false).desc("input file").hasArgs().build());

		final CommandLineParser cmdlineParser = new DefaultParser();
		final HelpFormatter formatter = new HelpFormatter();

		CommandLine tempCmdLine = null;
		try {
			tempCmdLine = cmdlineParser.parse(options, args);
		} catch (ParseException e) {
			System.out.println(e.getMessage());
			formatter.printHelp("utility-name", options);
			System.exit(1);
		}
		final CommandLine cmdLine = tempCmdLine;

		String aqlParser = "combinator"; // cmdLine.getOptionValue("aqlparser","combinator");
		switch (aqlParser.toLowerCase()) {
		case "combinator":
			// System.out.println("combinator parser used");
			AqlParserFactory.mode = AqlParserFactory.Mode.COMBINATOR;
			break;
		case "antlr4":
			// System.out.println("antlr4 parser used");
			AqlParserFactory.mode = AqlParserFactory.Mode.ANTLR4;
			break;
		default:
			// System.out.println("default combinator parser used");
			AqlParserFactory.mode = AqlParserFactory.Mode.COMBINATOR;
			break;
		}

		System.setProperty("apple.eawt.quitStrategy", "CLOSE_ALL_WINDOWS");
		// apple.awt.application.name
		Toolkit.getDefaultToolkit().setDynamicLayout(true);

		SwingUtilities.invokeLater(() -> {
			try {
				DefunctGlobalOptions.load();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			try {

				UIManager.setLookAndFeel(IdeOptions.theCurrentOptions.getString(IdeOption.LOOK_AND_FEEL));

				JFrame f = new JFrame("Categorical Query Language IDE");

				Pair<JPanel, MenuBar> gui = GUI.makeGUI(f);

				f.setContentPane(gui.first);
				f.setMenuBar(gui.second);
				f.pack();
				f.setSize(1024, 640);
				f.setLocationRelativeTo(null);
				f.setVisible(true);

				f.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
				f.addWindowListener(new WindowAdapter() {
					@Override
					public void windowClosing(WindowEvent windowEvent) {
						GUI.exitAction();
					}
				});
				
						
				//Runtime.getRuntime().

				String[] inputFilePath = cmdLine.getOptionValues("input");
				if (inputFilePath == null) {
					GUI.newAction(null, "", Language.getDefault());
				} else if (inputFilePath.length == 0) {
					GUI.newAction(null, "", Language.getDefault());
				} else {
					File[] fs = new File[inputFilePath.length];
					int i = 0;
					for (String s : inputFilePath) {
						fs[i++] = new File(s);
					}
					GUI.openAction(fs);
				}

				((CodeEditor<?, ?, ?>) GUI.editors.getComponentAt(0)).topArea.requestFocusInWindow();

				// Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());

				DeadLockDetector.makeDeadLockDetector();
		
			
	
				
			} catch (HeadlessException | ClassNotFoundException | IllegalAccessException | InstantiationException
					| UnsupportedLookAndFeelException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(null, "Unrecoverable error, restart IDE: " + e.getMessage());
			}
		});
		
		new Thread(() -> {
			File jf = new File("cql.jar");
			if (jf.exists()) {
				long current = jf.lastModified();
				//System.out.println("cql.jar file modification date: " + new Date(current).toLocaleString());
				try {
					URL u = new URL("https://www.categoricaldata.net/version.php");
					URLConnection c = u.openConnection();
					c.connect();
					String l = Util.readFile(new InputStreamReader(c.getInputStream()));
					long newest = Long.parseLong(l.toString().trim() + "000");
					//System.out.println("Newest cql.jar version: " + new Date(newest).toLocaleString());
					if (newest > current) {
						int x = JOptionPane.showConfirmDialog(null, "New Version Available - Download and Exit?", "Update?", JOptionPane.YES_NO_OPTION);
						if (JOptionPane.YES_OPTION == x) {
							Desktop.getDesktop().browse(new URI("http://categoricaldata.net/download"));
							System.exit(0);
						}
					}
				} catch (Exception ex) { 
					ex.printStackTrace();
				}
			} 
			
			}).start();
	}

	/*
	 * public static final class ExceptionHandler implements
	 * Thread.UncaughtExceptionHandler {
	 * 
	 * @Override public void uncaughtException(Thread aThread, Throwable aThrowable)
	 * { }
	 * 
	 * 
	 * private String getStackTrace(Throwable aThrowable) { final Writer result =
	 * new StringWriter(); final PrintWriter printWriter = new PrintWriter(result);
	 * aThrowable.printStackTrace(printWriter); return result.toString(); } }
	 */

}
