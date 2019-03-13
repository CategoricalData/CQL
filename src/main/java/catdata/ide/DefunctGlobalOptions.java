package catdata.ide;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import catdata.Pair;
import catdata.Unit;
import catdata.Util;

/**
 * 
 * @author ryan
 * 
 *         Contains global constants for debugging.
 */
public class DefunctGlobalOptions implements Serializable {

	private static final long serialVersionUID = 1L;
	
	public static DefunctGlobalOptions debug = new DefunctGlobalOptions();
	
	public static void clear() {
		debug = new DefunctGlobalOptions();
	}

	private static int selected_tab = 0;

	//public final GeneralOptions general = new GeneralOptions();
	//public final FqlOptions fql = new FqlOptions();
	//public final FqlppOptions fqlpp = new FqlppOptions();
	//public final XOptions fpql = new XOptions();
	//public OplOptions opl = new OplOptions(); 
	//private final AqlOptionsDefunct aql = new AqlOptionsDefunct();
	
	private Options[] options() {
		return new Options[] { /*general,*/  /*fpql, opl */ /*,aql*/ };
	}
	
	{
		for (Options option : options()) {
			Options.biggestSize = Integer.max(Options.biggestSize, option.size());
		}
	}
	
	private static void save() {
		try {
			FileOutputStream fileOut = new FileOutputStream("cdide.ser");
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(debug);
			out.close();
			fileOut.close();
		} catch (IOException i) {
			i.printStackTrace();
			JOptionPane.showMessageDialog(null, i.getLocalizedMessage());
		}
	}
	
	private static void delete() {
		File f = new File("cdide.ser");
		if (!f.exists()) {
			return;
		}
		if (!f.delete()) {
			throw new RuntimeException("Could not delete cdide.ser");
		}
	}

	public static void load() {
		try {
			if (!new File("cdide.ser").exists()) {
				return;
			}
			FileInputStream fileIn = new FileInputStream("cdide.ser");
			ObjectInputStream in = new ObjectInputStream(fileIn);
			DefunctGlobalOptions e = (DefunctGlobalOptions) in.readObject();
			in.close();
			fileIn.close();

			if (e != null) {
				debug = e;
			} else {
				throw new RuntimeException("Cannot restore options, file corrupt");
			}
		} catch (IOException | ClassNotFoundException | RuntimeException i) {
			i.printStackTrace();
			JOptionPane.showMessageDialog(null, "Cannot restore options - deleting");
			delete();
		}
		
	}

				
	public static void showOptions() {
		JTabbedPane jtb = new JTabbedPane();

		List<Function<Unit, Unit>> callbacks = new LinkedList<>();
		for (Options option : debug.options()) {
			Pair<JComponent, Function<Unit, Unit>> x = option.display();
			jtb.add(option.getName(), x.first);
			callbacks.add(x.second);
		}
		jtb.setSelectedIndex(selected_tab);
		
		JOptionPane pane = new JOptionPane(new JScrollPane(jtb), JOptionPane.PLAIN_MESSAGE,
				JOptionPane.OK_CANCEL_OPTION, null,
				new String[] { "OK", "Cancel", "Reset", "Save", "Load", "Delete" }, "OK");
		
		JDialog dialog = pane.createDialog(null, "Options");
		dialog.setModal(false);
		dialog.setResizable(true);
		dialog.addWindowListener(new WindowAdapter() {

			@Override
			public void windowDeactivated(WindowEvent e) {
				Object ret = pane.getValue();
				
				selected_tab = jtb.getSelectedIndex();

				if (ret == "OK") {
					for (Function<Unit, Unit> callback : callbacks) {
						callback.apply(Unit.unit);
					}
				} else if (ret == "Reset") {
					debug = new DefunctGlobalOptions();
					showOptions();
				} else if (ret == "Save") { // save
					for (Function<Unit, Unit> callback : callbacks) {
						callback.apply(Unit.unit);
					}
					save();
					showOptions();
				} else if (ret == "Load") { // load
					load();
					showOptions();
				} else if (ret == "Delete") {
					delete();
					showOptions();
				} 
			}
			
		
		});
		
		dialog.pack();
		dialog.setVisible(true);
	
	}
	 

	public static void showAbout() {
		JOptionPane.showMessageDialog(null, new CodeTextPanel("", about()), "About",
				JOptionPane.PLAIN_MESSAGE, null);
	}

	private static final String aboutErr = "No cdide.properties found.  If you are building from source, make sure cdide.properties is on the classpath.";
	
	private static String aboutString = null;
	private static String about() {
		if (aboutString != null) {
			return aboutString;
		}
		try (InputStream in = Object.class.getResourceAsStream("/cdide.properties")) {
			Properties prop = new Properties();
			prop.load(in);

            aboutString = in == null ? aboutErr : Util.sep(prop, ": ", "\n\n");
		} catch (IOException e) {
			e.printStackTrace();
			aboutString = aboutErr;
		}	
		return aboutString;
	}


}
