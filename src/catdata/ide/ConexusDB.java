package catdata.ide;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import catdata.Triple;
import catdata.cql.gui.CqlCodeEditor;
import catdata.ide.Olog.OlogEntity;
import catdata.ide.Olog.OlogMappingName;
import catdata.ide.Olog.OlogName;
import catdata.ide.Olog.OlogPresentation;
import catdata.ide.OlogMapping.OlogColimitPresentation;
import catdata.ide.OlogMapping.OlogMappingPresentation;

public class ConexusDB {

	public ConexusDB() {

	}

	JPanel right;

	OlogStore theStore;

	DefaultListModel<OlogName> left1 = new DefaultListModel<>();
	DefaultListModel<OlogMappingName> left2 = new DefaultListModel<>();

	String file;

	boolean dirty;

	public void updateUI() {
		left1.clear();
		left2.clear();
		for (var s : theStore.listOlogs()) {
			left1.addElement(s);
		}
		for (var s : theStore.listMappings()) {
			left2.addElement(s);
		}
		String title = "ConexusDB";
		if (theStore.dirty())
			title = "* " + title;
		if (theStore.file() != null)
			title = title + " " + theStore.file();
	}

	static FileDialog saveDialog;

	private static FileDialog getSaveDialog() {
		if (saveDialog != null) {
			saveDialog.setFile("*.json");
			return saveDialog;
		}
		saveDialog = new FileDialog((Dialog) null, "Save", FileDialog.SAVE);
		saveDialog.setFile("*.json");

		saveDialog.setMultipleMode(false);
		return saveDialog;
	}

	static FileDialog openDialog;

	private static FileDialog getOpenDialog() {
		if (openDialog != null) {
			openDialog.setFile("*.json");
			return openDialog;
		}
		openDialog = new FileDialog((Dialog) null, "Open", FileDialog.LOAD);
		openDialog.setFile("*.json");

		openDialog.setMultipleMode(false);
		return openDialog;
	}

	public Triple<JPanel, OlogPresentation, JTextField> makeOlogUI() {
		JPanel ret = new JPanel(new BorderLayout());

		JPanel top = new JPanel(new GridLayout(2, 7));
		top.add(new JLabel("Name: ", JLabel.RIGHT));
		JTextField f = new JTextField();
		top.add(f);
		JButton addEn = new JButton("Add Entity");
		JButton delEn = new JButton("Remove Entity");
		JButton addFk = new JButton("Add Arrow");
		JButton delFk = new JButton("Remove Arrow");
		JButton addAtt = new JButton("Add Attribute");
		JButton delAtt = new JButton("Remove Attribute");
		JButton addGen = new JButton("Add Row");
		JButton delGen = new JButton("Remove Row");
		JButton addSk = new JButton("Add Null");
		JButton delSk = new JButton("Remove Null");
		JButton addRule = new JButton("Add Rule");
		JButton delRule = new JButton("Remove Rule");

		OlogPresentation p = new OlogPresentation(new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>(),
				new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>());
		var jsp = new RSyntaxTextArea(p.toString());
		jsp.setPreferredSize(new Dimension(800,400));

		addEn.addActionListener((ActionEvent e) -> {
			JTextField a = new JTextField();

			Object[] message = { "Name: ", a };
			int option = JOptionPane.showConfirmDialog(null, message, "Add Entity", JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.PLAIN_MESSAGE, null);
			if (option == JOptionPane.OK_OPTION) {
				try {
					p.objects().add(new OlogEntity(new OlogName(f.getText()), a.getText()));
					jsp.setText(p.toString());
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		delEn.addActionListener((ActionEvent e) -> {
			JTextField a = new JTextField();

			Object[] message = { "Name: ", a };
			int option = JOptionPane.showConfirmDialog(null, message, "Remove Entity ", JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.PLAIN_MESSAGE, null);
			if (option == JOptionPane.OK_OPTION) {
				try {
					p.objects().remove(new OlogEntity(new OlogName(f.getText()), a.getText()));
					jsp.setText(p.toString());
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		top.add(addEn);
		top.add(delEn);
		top.add(addFk);
		top.add(delFk);
		top.add(addAtt);
		top.add(delAtt);
		top.add(addGen);
		top.add(delGen);
		top.add(addSk);
		top.add(delSk);
		top.add(addRule);
		top.add(delRule);

		right = new JPanel(new GridLayout(1, 1));

		ret.add(top, BorderLayout.NORTH);
		ret.add(jsp, BorderLayout.CENTER);

		return new Triple<>(ret, p, f);
	}

	public Triple<JPanel, OlogColimitPresentation, JTextField> makeComposeUI() {
		JPanel ret = new JPanel(new BorderLayout());

		JPanel top = new JPanel(new GridLayout(2, 5));
		top.add(new JLabel("Name: ", JLabel.RIGHT));
		JTextField f = new JTextField();
		top.add(f);
		JButton addEn = new JButton("Add Node Mapping");
		JButton delEn = new JButton("Remove Node Mapping");
		JButton addFk = new JButton("Add Arrow Mapping");
		JButton delFk = new JButton("Remove Arrow Mapping");

		OlogColimitPresentation p = new OlogColimitPresentation(new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>());
		var jsp = new RSyntaxTextArea(p.toString());
		jsp.setPreferredSize(new Dimension(800,400));

		addEn.addActionListener((ActionEvent e) -> {
			JTextField a = new JTextField();
			JTextField b = new JTextField();
			
			Object[] message = { "Node Name: ", a, "Olog Name: ", b };
			int option = JOptionPane.showConfirmDialog(null, message, "Add Node Mapping", JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.PLAIN_MESSAGE, null);
			if (option == JOptionPane.OK_OPTION) {
				try {
					p.m1().put(a.getText(), new OlogName(b.getText()));
					jsp.setText(p.toString());
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		delEn.addActionListener((ActionEvent e) -> {
			JTextField a = new JTextField();

			Object[] message = { "Source Entity: ", a };
			int option = JOptionPane.showConfirmDialog(null, message, "Remove Entity Mapping", JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.PLAIN_MESSAGE, null);
			if (option == JOptionPane.OK_OPTION) {
				try {
					p.m1().remove(a.getText());
					jsp.setText(p.toString());
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		top.add(addEn);
		top.add(delEn);
		top.add(addFk);
		top.add(delFk);
		right = new JPanel(new GridLayout(1, 1));

		ret.add(top, BorderLayout.NORTH);
		ret.add(jsp, BorderLayout.CENTER);

		return new Triple<>(ret, p, f);
	}
	
	public Triple<JPanel, OlogMappingPresentation, JTextField> makeMappingUI(OlogName src, OlogName dst) {
		JPanel ret = new JPanel(new BorderLayout());

		JPanel top = new JPanel(new GridLayout(2, 6));
		top.add(new JLabel("Name: ", JLabel.RIGHT));
		JTextField f = new JTextField();
		top.add(f);
		JButton addEn = new JButton("Add Entity Mapping");
		JButton delEn = new JButton("Remove Entity Mapping");
		JButton addFk = new JButton("Add Arrow Mapping");
		JButton delFk = new JButton("Remove Arrow Mapping");
		JButton addAtt = new JButton("Add Attribute Mapping");
		JButton delAtt = new JButton("Remove Attribute Mapping");
		JButton addGen = new JButton("Add Row Mapping");
		JButton delGen = new JButton("Remove Row Mapping");
		JButton addSk = new JButton("Add Null Mapping");
		JButton delSk = new JButton("Remove Null Mapping");

		OlogMappingPresentation p = new OlogMappingPresentation(new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(),
				new HashMap<>());
		var jsp = new RSyntaxTextArea(p.toString());
		jsp.setPreferredSize(new Dimension(800,400));

		addEn.addActionListener((ActionEvent e) -> {
			JTextField a = new JTextField();
			JTextField b = new JTextField();
			
			Object[] message = { "Source Entity: ", a, "Target Entity: ", b };
			int option = JOptionPane.showConfirmDialog(null, message, "Add Entity Mapping", JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.PLAIN_MESSAGE, null);
			if (option == JOptionPane.OK_OPTION) {
				try {
					p.objectMapping().put(new OlogEntity(src, a.getText()), new OlogEntity(dst, b.getText()));
					jsp.setText(p.toString());
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		delEn.addActionListener((ActionEvent e) -> {
			JTextField a = new JTextField();

			Object[] message = { "Source Entity: ", a };
			int option = JOptionPane.showConfirmDialog(null, message, "Remove Entity Mapping", JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.PLAIN_MESSAGE, null);
			if (option == JOptionPane.OK_OPTION) {
				try {
					p.objectMapping().remove(new OlogEntity(src, a.getText()));
					jsp.setText(p.toString());
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		top.add(addEn);
		top.add(delEn);
		top.add(addFk);
		top.add(delFk);
		top.add(addAtt);
		top.add(delAtt);
		top.add(addGen);
		top.add(delGen);
		top.add(addSk);
		top.add(delSk);

		right = new JPanel(new GridLayout(1, 1));

		ret.add(top, BorderLayout.NORTH);
		ret.add(jsp, BorderLayout.CENTER);

		return new Triple<>(ret, p, f);
	}

	
	public JPanel makeUI() {
		theStore = new OlogStoreImpl();
		JPanel ret = new JPanel(new BorderLayout());

		JPanel top = new JPanel(new GridLayout(2, 6));

		JButton newButton = new JButton("Clear DB");
		newButton.addActionListener((ActionEvent e) -> {
			theStore.clear();
			updateUI();
		});

		JButton loadButton = new JButton("Load DB");
		JButton saveButton = new JButton("Save DB");
		JButton saveAsButton = new JButton("Save DB As");

		loadButton.addActionListener((ActionEvent e) -> {
			FileDialog jfc = getOpenDialog();
			jfc.setVisible(true);
			String f = jfc.getFile();
			if (f == null) {
				return;
			}
			theStore.open(f);
		});

		ActionListener xx = (ActionEvent e) -> {
			FileDialog jfc = getSaveDialog();
			jfc.setVisible(true);

			String f = jfc.getFile();
			if (f == null) {
				return;
			}
			String d = jfc.getDirectory();
			if (d == null) {
				throw new RuntimeException("Could not save file");
			}
			if (!f.endsWith(".json")) {
				f = f + ".json";
			}
			File file = new File(d, f);
			theStore.saveAs(file.getAbsolutePath());
		};
		saveAsButton.addActionListener(xx);
		saveButton.addActionListener((ActionEvent e) -> {
			if (theStore.file() == null)
				xx.actionPerformed(e);
			else
				theStore.save();
		});

		JButton ologLit = new JButton("Add Literal Olog");
		ologLit.addActionListener((ActionEvent e) -> {
			var message = makeOlogUI();
			int option = JOptionPane.showConfirmDialog(null, message.first, "Add Literal Olog",
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null);
			if (option == JOptionPane.OK_OPTION) {
				try {
					theStore.addLiteralOlog(new OlogName(message.third.getText()), message.second);
					updateUI();
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		JButton ologComp = new JButton("Add Composed Olog");
		ologComp.addActionListener((ActionEvent e) -> {
			var message = makeComposeUI();
			int option = JOptionPane.showConfirmDialog(null, message.first, "Add Literal Olog",
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null);
			if (option == JOptionPane.OK_OPTION) {
				try {
					theStore.addComposedOlog(new OlogName(message.third.getText()), message.second);
					updateUI();
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		JButton deleteOlog = new JButton("Delete Olog");
		deleteOlog.addActionListener((ActionEvent e) -> {
			JTextField a = new JTextField();
			Object[] message = { "Name:", a };
			int option = JOptionPane.showConfirmDialog(null, message, "Delete Olog", JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.PLAIN_MESSAGE, null);
			if (option == JOptionPane.OK_OPTION) {
				try {
					theStore.deleteOlog(new OlogName(a.getText()));
					updateUI();
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		JButton mapLit = new JButton("Add Literal Mapping");
		mapLit.addActionListener((ActionEvent e) -> {
			JTextField c = new JTextField();
			JTextField a = new JTextField();
			Object[] message = { "Source:", c, "Target:", a };
			int option = JOptionPane.showConfirmDialog(null, message, "Add Literal Mapping",
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null);
			if (option == JOptionPane.OK_OPTION) {
				try {
					
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
			} else {
				return;
			}
			
			var message2 = makeMappingUI(new OlogName(c.getText()), new OlogName(a.getText()));
			option = JOptionPane.showConfirmDialog(null, message2.first, "Add Literal Mapping",
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null);
			if (option == JOptionPane.OK_OPTION) {
				try {
					theStore.addLiteralMapping(new OlogMappingName(message2.third.getText()), message2.second);
					updateUI();
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		JButton mapComp = new JButton("Add Composed Mapping");
		mapComp.addActionListener((ActionEvent e) -> {
			JTextField c = new JTextField();
			JTextField a = new JTextField();
			Object[] message = { "Name:", c, "Comma separated list:", a };
			int option = JOptionPane.showConfirmDialog(null, message, "Add Composed Mapping",
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null);
			if (option == JOptionPane.OK_OPTION) {
				try {
					theStore.addComposedMapping(new OlogMappingName(c.getText()), Arrays.asList(a.getText().split(",")).stream().map((x)->new OlogMappingName(x)).collect(Collectors.toList())); 
					updateUI();
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		JButton deleteMapping = new JButton("Delete Mapping");
		deleteMapping.addActionListener((ActionEvent e) -> {
			JTextField a = new JTextField();
			Object[] message = { "Name:", a };
			int option = JOptionPane.showConfirmDialog(null, message, "Delete Mapping", JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.PLAIN_MESSAGE, null);
			if (option == JOptionPane.OK_OPTION) {
				try {
					theStore.deleteMapping(new OlogMappingName(a.getText()));
					updateUI();
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		top.add(newButton);
		top.add(loadButton);
		top.add(saveButton);
		top.add(saveAsButton);

		top.add(new JLabel("Prover path:", JLabel.RIGHT));
		top.add(new JTextField("/usr/opt/eprover"));
		top.add(ologLit);
		top.add(ologComp);
		top.add(deleteOlog);
		top.add(mapLit);
		top.add(mapComp);
		top.add(deleteMapping);

		var left1x = new JList<>(left1);
		var left2x = new JList<>(left2);

		left1x.setBorder(BorderFactory.createTitledBorder("Ologs"));
		left2x.setBorder(BorderFactory.createTitledBorder("Mappings"));

		right = new JPanel(new GridLayout(1, 1));

		JSplitPane jsp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

		jsp.setResizeWeight(.33d);

		JSplitPane jsp2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		jsp2.add(left1x, JSplitPane.TOP);
		jsp2.add(left2x, JSplitPane.BOTTOM);
		jsp2.setResizeWeight(.5d);
		jsp2.setBorder(BorderFactory.createEmptyBorder());
		
		jsp.add(jsp2, JSplitPane.LEFT);
		jsp.add(right, JSplitPane.RIGHT);

		ret.add(top, BorderLayout.NORTH);
		ret.add(jsp, BorderLayout.CENTER);
		return ret;
	}

	public static void show() {
		JFrame f = new JFrame("ConexusDB");
		var x = new ConexusDB();
		f.add(x.makeUI());
		f.setSize(1360, 768);
		f.setLocationRelativeTo(null);
		f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		// todo: warn on dirty close
		f.setVisible(true);
	}

}
