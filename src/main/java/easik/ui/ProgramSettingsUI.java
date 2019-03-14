package easik.ui;

//~--- non-JDK imports --------------------------------------------------------

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
//~--- JDK imports ------------------------------------------------------------
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import easik.Easik;
import easik.EasikSettings;
import easik.database.DriverInfo;

/**
 * This dialog is used to set various program options
 *
 * @author Kevin Green 2006
 * @since 2006-08-01 Kevin Green
 * @version 2006-08-04 Kevin Green
 */
public class ProgramSettingsUI extends OptionsDialog implements TabbedOptionsDialog, ContainedOptionsDialog {
	// The color chooser object used for selecting colors (using a single object
	// lets it keep its color history)
	private static final long serialVersionUID = -7760321916297350077L;

	/**  */
	private static JColorChooser colorChooser = new JColorChooser();

	// keep track of colorSetting buttons

	/**  */
	private LinkedList<ColorEditButton> _colorButtons = new LinkedList<>();

	// width sliders

	/**  */
	private LinkedHashMap<String, JSlider> _widthSliders = new LinkedHashMap<>();

	// Radio buttons for Last Used Folder, Running Directory, and Specific
	// Folder

	/**  */
	private JRadioButton _folderLast, _folderRunning, _folderSpecific;

	// The button to set the specific folder

	/**  */
	private JButton _folderSpecificButton;

	// The field showing the specific folder

	/**  */
	private JLabel _folderSpecificLoc;

	// The show all attributes and unique keys load value check box

	/**  */
	private JCheckBox _showAttsAndUniqueKeys;

	/**  */
	// private JCheckBox _showWarnings;

	/**  */
	@SuppressWarnings("rawtypes")
	private JComboBox _sqlDriver, _sqlCascading, _sqlCascadingPartial;

	// Custom PK naming text field:

	/**  */
	private JTextField _sqlPKCustom, _sqlFKCustom;

	// Radio buttons for sql settings

	/**  */
	private JRadioButton _sqlPKid, _sqlPKTable_id, _sqlPKCustomButton, _sqlFKTargetTable, _sqlFKTargetTable_id,
			_sqlFKEdges, _sqlFKTargetEdge, _sqlFKCustomButton;

	// SQL quoting checkbox and cascading delete checkbox

	/**  */
	private JCheckBox _sqlQuoting;

	// The thumbnail scale slider

	/**  */
	private JSlider thumbScaleSlider;

	/**
	 * Default Constructor
	 *
	 * @param parent
	 */
	public ProgramSettingsUI(JFrame parent) {
		super(parent, "EASIK Preferences");

		setSize(500, 500);
	}

	/**
	 *
	 *
	 * @param accepted
	 */
	@Override
	public void accepted(boolean accepted) {
		if (!accepted) {
			return;
		}

		// The user hit OK, so save the various settings
		EasikSettings s = Easik.getInstance().getSettings();

		s.setProperty("attrib_display", _showAttsAndUniqueKeys.isSelected() ? "show" : "hide");
		// s.setProperty("warning_display", _showWarnings.isSelected()
		// ? "show"
		// : "hide");
		s.setProperty("folder_specified", _folderSpecificLoc.getText());
		s.setProperty("folder_default",
				_folderSpecific.isSelected() ? "specified" : _folderRunning.isSelected() ? "pwd" : "last");
		s.setProperty("thumb_scale_factor", (thumbScaleSlider.getValue() / 100.0) + "");

		for (ColorEditButton c : _colorButtons) {
			c.saveColor();
		}

		for (String setting : _widthSliders.keySet()) {
			JSlider slider = _widthSliders.get(setting);

			s.setProperty(setting + "_width", (slider.getValue() / 10.0) + "");
		}

		s.setProperty("sql_driver", "" + _sqlDriver.getSelectedItem());
		s.setProperty("sql_pk_columns",
				_sqlPKid.isSelected() ? "id" : _sqlPKTable_id.isSelected() ? "<table>_id" : _sqlPKCustom.getText());
		s.setProperty("sql_fk_columns",
				_sqlFKEdges.isSelected() ? "<edge>"
						: _sqlFKTargetEdge.isSelected() ? "<target>_<edge>"
								: _sqlFKTargetTable.isSelected() ? "<target>" : "<target>_id" // _sqlFKTargetTable_id.isSelected()
		);
		s.setProperty("sql_quoting", _sqlQuoting.isSelected() ? "true" : "false");
		s.setProperty("sql_cascade", (_sqlCascading.getSelectedIndex() == 0) ? "restrict" : "cascade");

		int parIndex = _sqlCascadingPartial.getSelectedIndex();

		s.setProperty("sql_cascade_partial", (parIndex == 0) ? "set_null" : (parIndex == 1) ? "restrict" : "cascade");
		Easik.getInstance().getFrame().getOverview().refreshAll();
	}

	/**
	 *
	 *
	 * @return
	 */
	@Override
	public boolean verify() {
		boolean ret = true;

		// Check to make sure the custom PK is something valid
		if (_sqlPKCustomButton.isSelected() && !_sqlPKCustom.getText().matches("^(?:[^<]++|<table>)+$")) {
			JOptionPane.showMessageDialog(this,
					"Invalid custom primary key value entered.\n\nThe custom primary key value must be non-empty, and may contain the <table> tag, which will be replaced with the current table name.",
					"Invalid custom primary key", JOptionPane.ERROR_MESSAGE);

			ret = false;
		}

		// Check to make sure the custom FK is something valid
		if (_sqlFKCustomButton.isSelected()
				&& !_sqlFKCustom.getText().matches("^(?:[^<]++|<(?:source|target|edge)>)+$")) {
			JOptionPane.showMessageDialog(this,
					"Invalid custom foreign key value entered.\n\nThe custom foreign key value must be non-empty, and may contain the <source>, <target>, and <edge> tags, which will be replaced with the source table name, target table name, and edge name, respectively.",
					"Invalid custom foreign key", JOptionPane.ERROR_MESSAGE);

			ret = false;
		}

		return ret;
	}

	/**
	 * Makes the dialog
	 *
	 * @return
	 */
	@Override
	public List<OptionTab> getTabs() {
		LinkedList<OptionTab> tabs = new LinkedList<>();
		OptionTab general = new OptionTab("General", "General EASIK application settings");
		OptionTab colours = new OptionTab("Colours", "User interface and sketch colour settings");
		OptionTab sql = new OptionTab("SQL Defaults", "General SQL-server related default settings");

		general.setMnemonic(KeyEvent.VK_G);
		colours.setMnemonic(KeyEvent.VK_C);
		sql.setMnemonic(KeyEvent.VK_S);
		addGeneralOptions(general);
		addColorOptions(colours);
		addSQLOptions(sql);
		tabs.add(general);
		tabs.add(colours);
		tabs.add(sql);

		return tabs;
	}

	/**
	 * Adds the options for the General tab to the passed-in OptionTab
	 *
	 * @param general
	 */
	protected void addGeneralOptions(OptionTab general) {
		EasikSettings s = Easik.getInstance().getSettings();

		general.addOption(new JLabel("Default folder"), folderPanel(s));

		JPanel settings = new JPanel();

		settings.setLayout(new BoxLayout(settings, BoxLayout.Y_AXIS));

		_showAttsAndUniqueKeys = new JCheckBox("Show attributes & unique keys",
				s.getProperty("attrib_display", "show").equals("show"));

		// _showWarnings = new JCheckBox("Show warnings",
		// s.getProperty("warning_display", "show").equals("show"));

		settings.add(_showAttsAndUniqueKeys);
		// settings.add(_showWarnings);
		general.addOption(new JLabel("Startup settings"), settings);

		float currentScale = s.getFloat("thumb_scale_factor", 0.25f);

		thumbScaleSlider = new JSlider(1, 100, (int) (100 * currentScale));

		Dimension d = thumbScaleSlider.getPreferredSize();

		thumbScaleSlider.setPreferredSize(new Dimension(200, d.height));

		final JLabel thumbScaleIndicator = new JLabel((currentScale + "").replaceAll("\\.?0+$", ""));

		thumbScaleSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider s = (JSlider) e.getSource();
				@SuppressWarnings("unused")
				JPanel p = (JPanel) s.getParent();

				thumbScaleIndicator.setText(((s.getValue() / 100.0) + "").replaceAll("\\.?0+$", ""));
			}
		});

		String scaleTT = JUtils.tooltip(
				"The scale affects how large the thumbnail of sketches and views shows in the overview will be.  A value of 0.25 (the default) means 1/4 size; 0.5 would mean 1/2 size.");
		JLabel scaleLabel = new JLabel("Thumbnail scale");

		scaleLabel.setToolTipText(scaleTT);
		thumbScaleSlider.setToolTipText(scaleTT);
		general.addOption(new Option(scaleLabel, thumbScaleSlider, thumbScaleIndicator));
	}

	/**
	 *
	 *
	 * @param s
	 *
	 * @return
	 */
	protected JPanel folderPanel(EasikSettings s) {
		JPanel folderPane = new JPanel();

		_folderLast = new JRadioButton("Last used folder");
		_folderRunning = new JRadioButton("Running directory");
		_folderSpecific = new JRadioButton("Specific folder:");

		Dimension size = _folderSpecific.getMaximumSize();

		size.width = 125;

		_folderSpecific.setMaximumSize(size);

		String folderDefault = s.getProperty("folder_default", "pwd");

		if (folderDefault.equals("specified")) {
			_folderSpecific.setSelected(true);
		} else if (folderDefault.equals("pwd")) {
			_folderRunning.setSelected(true);
		} else // "last"
		{
			_folderLast.setSelected(true);
		}

		ButtonGroup folder = new ButtonGroup();

		folder.add(_folderLast);
		folder.add(_folderRunning);
		folder.add(_folderSpecific);

		_folderSpecificLoc = new JLabel(s.getProperty("folder_specified", ""));
		size = _folderSpecificLoc.getMaximumSize();
		size.width = 350;

		_folderSpecificLoc.setMaximumSize(size);

		size = _folderSpecificLoc.getPreferredSize();
		size.width = 350;

		_folderSpecificLoc.setPreferredSize(size);

		_folderSpecificButton = new JButton("Browse...");

		_folderSpecificButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e1) {
				JRadioButton wasSelected = _folderLast.isSelected() ? _folderLast
						: _folderRunning.isSelected() ? _folderRunning : _folderSpecific;

				// Check the radio button, if it isn't already:
				_folderSpecific.setSelected(true);

				File selFile = FileChooser.directory("Select folder");

				if (selFile != null) {
					_folderSpecificLoc.setText(selFile.getAbsolutePath());

					Dimension size = _folderSpecificLoc.getMaximumSize();

					size.width = 350;

					_folderSpecificLoc.setMaximumSize(size);

					size = _folderSpecificLoc.getPreferredSize();
					size.width = 350;

					_folderSpecificLoc.setPreferredSize(size);
				} else {
					// The user hit cancel, so restore the previous selection:
					wasSelected.setSelected(true);
				}
			}
		});
		folderPane.setLayout(new BoxLayout(folderPane, BoxLayout.Y_AXIS));
		_folderLast.setAlignmentX(Component.LEFT_ALIGNMENT);
		_folderRunning.setAlignmentX(Component.LEFT_ALIGNMENT);
		_folderSpecific.setAlignmentX(Component.LEFT_ALIGNMENT);
		folderPane.add(_folderLast);
		folderPane.add(_folderRunning);

		JPanel specificPanel = new JPanel();

		specificPanel.setLayout(new BoxLayout(specificPanel, BoxLayout.X_AXIS));
		specificPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		specificPanel.add(_folderSpecific);
		specificPanel.add(_folderSpecificButton);
		folderPane.add(specificPanel);
		_folderSpecificLoc.setBorder(new EmptyBorder(0, new JRadioButton().getPreferredSize().width, 0, 0));
		folderPane.add(_folderSpecificLoc);

		return folderPane;
	}

	/**
	 * Adds settings to the general colour tab pane
	 *
	 * @param colors
	 */
	protected void addColorOptions(OptionTab colors) {
		EasikSettings s = Easik.getInstance().getSettings();

		for (ColorField f : colorFields()) {
			String name = f.name, setting = f.key;

			if (setting == null) { // A title
				colors.addOption(new Option.Title(name));

				continue;
			}

			ColorEditButton c = new ColorEditButton(setting);

			_colorButtons.add(c);

			// If it's one of these, add a width slider:
			if (setting.matches("^(?:manip_|edit_)?edge_.*|.*_border$|^(?:path_)?selection$")) {
				float currentValue = s.getFloat(setting + "_width", 10.0f);
				JSlider widthSlider = new JSlider(10, 40, (int) (10 * currentValue));

				if (setting.matches(".*_border$")) { // Borders need an integer
														// width
					widthSlider.setMinorTickSpacing(10);
					widthSlider.setSnapToTicks(true);
					widthSlider.setPaintTicks(true);
				}

				_widthSliders.put(setting, widthSlider);

				Dimension d = widthSlider.getPreferredSize();

				widthSlider.setPreferredSize(new Dimension(120, d.height));

				final JLabel width = new JLabel(
						(currentValue + "").replaceAll("\\.?0+$", "") + " pixel" + ((currentValue == 1.0) ? "" : "s"));

				widthSlider.addChangeListener(new ChangeListener() {
					@Override
					public void stateChanged(ChangeEvent e) {
						JSlider s = (JSlider) e.getSource();
						@SuppressWarnings("unused")
						JPanel p = (JPanel) s.getParent();

						width.setText(((s.getValue() / 10.0) + "").replaceAll("\\.?0+$", "") + " pixel"
								+ ((s.getValue() == 10.0) ? "" : "s"));
					}
				});
				colors.addOption(new Option(name, c, widthSlider, width));
			} else {
				colors.addOption(new JLabel(name), c);
			}
		}
	}

	/**
	 *
	 *
	 * @param sql
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void addSQLOptions(OptionTab sql) {
		EasikSettings s = Easik.getInstance().getSettings();
		String[] drivers = DriverInfo.availableDatabaseDrivers();

		_sqlDriver = new JComboBox(drivers);

		_sqlDriver.setSelectedItem(s.getProperty("sql_driver", drivers[0]));
		sql.addOption(new JLabel("Default SQL driver"), _sqlDriver);

		_sqlPKid = new JRadioButton("Use \"id\"");
		_sqlPKTable_id = new JRadioButton("Use \"<tablename>_id\"");
		_sqlPKCustomButton = new JRadioButton("Use custom name:");

		ButtonGroup pk = new ButtonGroup();

		pk.add(_sqlPKid);
		pk.add(_sqlPKTable_id);
		pk.add(_sqlPKCustomButton);

		String pkColumns = s.getProperty("sql_pk_columns", "<table>_id");

		if (pkColumns.equals("id")) {
			_sqlPKid.setSelected(true);
		} else if (pkColumns.equals("<table>_id")) {
			_sqlPKTable_id.setSelected(true);
		} else {
			_sqlPKCustomButton.setSelected(true);
		}

		_sqlPKCustom = JUtils.textField(pkColumns);

		JPanel customPK = new JPanel();

		customPK.setLayout(new BoxLayout(customPK, BoxLayout.X_AXIS));
		customPK.add(_sqlPKCustomButton);
		customPK.add(_sqlPKCustom);
		_sqlPKid.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				_sqlPKCustom.setText("id");
			}
		});
		_sqlPKTable_id.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				_sqlPKCustom.setText("<table>_id");
			}
		});

		JPanel pkSettings = new JPanel();

		pkSettings.setLayout(new BoxLayout(pkSettings, BoxLayout.Y_AXIS));
		pkSettings.add(_sqlPKid);
		pkSettings.add(_sqlPKTable_id);
		pkSettings.add(customPK);
		pkSettings.setAlignmentX(Component.LEFT_ALIGNMENT);
		_sqlPKid.setAlignmentX(Component.LEFT_ALIGNMENT);
		_sqlPKTable_id.setAlignmentX(Component.LEFT_ALIGNMENT);
		customPK.setAlignmentX(Component.LEFT_ALIGNMENT);
		sql.addOption(new JLabel("Primary key column names"), pkSettings);

		_sqlFKTargetTable = new JRadioButton("Use \"<target name>\"");
		_sqlFKTargetTable_id = new JRadioButton("Use \"<target name>_id\"");
		_sqlFKEdges = new JRadioButton("Use edge labels");
		_sqlFKTargetEdge = new JRadioButton("Use \"<target name>_<edge label>\"");
		_sqlFKCustomButton = new JRadioButton("Use custom name:");

		ButtonGroup fk = new ButtonGroup();

		fk.add(_sqlFKTargetTable);
		fk.add(_sqlFKTargetTable_id);
		fk.add(_sqlFKEdges);
		fk.add(_sqlFKTargetEdge);
		fk.add(_sqlFKCustomButton);

		String fkColumns = s.getProperty("sql_fk_columns", "<target>_<edge>");

		if (fkColumns.equals("<edge>")) {
			_sqlFKEdges.setSelected(true);
		} else if (fkColumns.equals("<target>")) {
			_sqlFKTargetTable.setSelected(true);
		} else if (fkColumns.equals("<target>_id")) {
			_sqlFKTargetTable_id.setSelected(true);
		} else if (fkColumns.equals("<target>_<edge>")) {
			_sqlFKTargetEdge.setSelected(true);
		} else {
			_sqlFKCustomButton.setSelected(true);
		}

		_sqlFKCustom = JUtils.textField(fkColumns);

		JPanel customFK = new JPanel();

		customFK.setLayout(new BoxLayout(customFK, BoxLayout.X_AXIS));
		customFK.add(_sqlFKCustomButton);
		customFK.add(_sqlFKCustom);
		_sqlFKEdges.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				_sqlFKCustom.setText("<edge>");
			}
		});
		_sqlFKTargetTable.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				_sqlFKCustom.setText("<target>");
			}
		});
		_sqlFKTargetTable_id.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				_sqlFKCustom.setText("<target>_id");
			}
		});
		_sqlFKTargetEdge.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				_sqlFKCustom.setText("<target>_<edge>");
			}
		});

		JPanel fkSettings = new JPanel();

		fkSettings.setLayout(new BoxLayout(fkSettings, BoxLayout.Y_AXIS));
		fkSettings.add(_sqlFKTargetTable);
		fkSettings.add(_sqlFKTargetTable_id);
		fkSettings.add(_sqlFKEdges);
		fkSettings.add(_sqlFKTargetEdge);
		fkSettings.add(customFK);
		fkSettings.setAlignmentX(Component.LEFT_ALIGNMENT);
		_sqlFKTargetTable.setAlignmentX(Component.LEFT_ALIGNMENT);
		_sqlFKTargetTable_id.setAlignmentX(Component.LEFT_ALIGNMENT);
		_sqlFKEdges.setAlignmentX(Component.LEFT_ALIGNMENT);
		_sqlFKTargetEdge.setAlignmentX(Component.LEFT_ALIGNMENT);
		customFK.setAlignmentX(Component.LEFT_ALIGNMENT);
		sql.addOption(new JLabel("Foreign key column names"), fkSettings);

		_sqlQuoting = new JCheckBox("Quote SQL identifiers");

		_sqlQuoting.setSelected(s.getProperty("sql_quoting", "false").equals("true"));

		JLabel quoteLabel = new JLabel("Identifier quoting");
		String quoteTT = JUtils.tooltip(
				"This option makes EASIK quote identifiers, which allows various non-alphanumeric characters (such as spaces and punctuation) in table and column names.\n\nNote also that quoted identifiers are, for many databases, case-sensitive.");

		quoteLabel.setToolTipText(quoteTT);
		_sqlQuoting.setToolTipText(quoteTT);
		sql.addOption(quoteLabel, _sqlQuoting);

		_sqlCascading = new JComboBox(new String[] { "Restrict deletions", "Cascade deletions" });

		_sqlCascading.setSelectedIndex(s.getProperty("sql_cascade", "restrict").equals("cascade") ? 1 : 0);

		_sqlCascadingPartial = new JComboBox(new String[] { "Set null", "Restrict deletions", "Cascade deletions" });

		String partialCascading = s.getProperty("sql_cascade_partial", "set_null");

		_sqlCascadingPartial
				.setSelectedIndex(partialCascading.equals("cascade") ? 2 : partialCascading.equals("restrict") ? 1 : 0);

		JLabel cascadeLabel = new JLabel("Edge cascading");
		String cascadeTT = JUtils.tooltip(
				"This option affects how EASIK creates edges when exporting to a db.\n\n\"Cascade deletions\" cause deletions in one table to trigger deletions of any rows in other tables that point to the row(s) being deleted.\n\n\"Restrict deletions\" causes attempted deletions of referenced rows to fail.\n\nThis option will be used by default for normal and injective edges of new sketches.  To change the default for new edges of an existing sketch, change the setting in the sketch's Document Information menu.");

		cascadeLabel.setToolTipText(cascadeTT);
		_sqlCascading.setToolTipText(cascadeTT);
		sql.addOption(cascadeLabel, _sqlCascading);

		JLabel cascadePartialLabel = new JLabel("Partial edge cascading");
		String cascadePartialTT = JUtils.tooltip(
				"This option affects how EASIK creates partial edges when exporting to a db.\n\n\"Cascade deletions\" cause deletions in one table to trigger deletions of any rows in other tables that point to the row(s) being deleted.\n\n\"Restrict deletions\" cause attempted deletions of referenced rows to fail.\n\n\"Set null\" causes references to be set to NULL when the targeted row is deleted.\n\nThis option will be used by default for partial edges of new sketches.  To change the default for new edges of an existing sketch, change the setting in the sketch's Document Information menu.");

		cascadePartialLabel.setToolTipText(cascadePartialTT);
		_sqlCascadingPartial.setToolTipText(cascadePartialTT);
		sql.addOption(cascadePartialLabel, _sqlCascadingPartial);
	}

	/**
	 * Returns a map of human-readable colour description keys to EasikSettings
	 * colour keys. The returned map is ordered: the iteration order of keys is the
	 * intended iteration order.
	 *
	 * @return
	 */
	public LinkedList<ColorField> colorFields() {
		LinkedList<ColorField> colours = new LinkedList<>();

		colours.add(new ColorField("General colours:"));
		colours.add(new ColorField("Selection", "selection"));
		colours.add(new ColorField("Path Selection", "path_selection"));
		colours.add(new ColorField("Overview colours:"));
		colours.add(new ColorField("Canvas Background", "overview_canvas_background"));
		colours.add(new ColorField("Sketch Node Borders", "overview_sketch_border"));
		colours.add(new ColorField("Sketch Node Background", "overview_sketch_bg"));
		colours.add(new ColorField("Sketch Node Foreground", "overview_sketch_fg"));
		colours.add(new ColorField("View Node Borders", "overview_view_border"));
		colours.add(new ColorField("View Node Background", "overview_view_bg"));
		colours.add(new ColorField("View Node Foreground", "overview_view_fg"));
		colours.add(new ColorField("View Node Edges", "edge_overview_view"));
		colours.add(new ColorField("Sketch editing colours:"));
		colours.add(new ColorField("Canvas Background", "edit_canvas_background"));
		colours.add(new ColorField("Entity Borders", "edit_entity_border"));
		colours.add(new ColorField("Entity Background", "edit_entity_bg"));
		colours.add(new ColorField("Entity Foreground", "edit_entity_fg"));
		colours.add(new ColorField("Attribute/keys Background", "edit_attribute_bg"));
		colours.add(new ColorField("Attribute/keys Foreground", "edit_attribute_fg"));
		colours.add(new ColorField("Constraint Borders", "edit_constraint_border"));
		colours.add(new ColorField("Constraint Background", "edit_constraint_bg"));
		colours.add(new ColorField("Constraint Foreground", "edit_constraint_fg"));
		colours.add(new ColorField("Normal Edges", "edit_edge_normal"));
		colours.add(new ColorField("Injective Edges", "edit_edge_injective"));
		colours.add(new ColorField("Partial Edges", "edit_edge_partial"));
		colours.add(new ColorField("Virtual Edges", "edit_edge_virtual"));
		colours.add(new ColorField("Highlighted Virtual Edges", "edit_edge_virtual_highlighted"));
		colours.add(new ColorField("Sketch data manipulation colours:"));
		colours.add(new ColorField("Canvas Background", "manip_canvas_background"));
		colours.add(new ColorField("Entity Borders", "manip_entity_border"));
		colours.add(new ColorField("Entity Background", "manip_entity_bg"));
		colours.add(new ColorField("Entity Foreground", "manip_entity_fg"));
		colours.add(new ColorField("Attribute/keys Background", "manip_attribute_bg"));
		colours.add(new ColorField("Attribute/keys Foreground", "manip_attribute_fg"));
		colours.add(new ColorField("Constraint Borders", "manip_constraint_border"));
		colours.add(new ColorField("Constraint Background", "manip_constraint_bg"));
		colours.add(new ColorField("Constraint Foreground", "manip_constraint_fg"));
		colours.add(new ColorField("Normal Edges", "manip_edge_normal"));
		colours.add(new ColorField("Injective Edges", "manip_edge_injective"));
		colours.add(new ColorField("Partial Edges", "manip_edge_partial"));
		colours.add(new ColorField("Virtual Edges", "manip_edge_virtual"));
		colours.add(new ColorField("Highlighted Virtual Edges", "manip_edge_virtual_highlighted"));
		colours.add(new ColorField("View colours:"));
		colours.add(new ColorField("Canvas Background", "view_canvas_background"));
		colours.add(new ColorField("Query Node Borders", "view_query_border"));
		colours.add(new ColorField("Query Node Background", "view_query_bg"));
		colours.add(new ColorField("Query Node Foreground", "view_query_fg"));

		return colours;
	}

	// JButton extension to handle displaying and updating the color as part of
	// the button

	/**
	 *
	 *
	 * @version 12/09/12
	 * @author Christian Fiddick
	 */
	private class ColorEditButton extends JButton implements ActionListener {
		/**
		 *        
		 */
		private static final long serialVersionUID = 5827895306309935324L;

		/**  */
		private Color color;

		/**  */
		private String key;

		/**
		 *
		 *
		 * @param key
		 */
		public ColorEditButton(String key) {
			super("Edit");

			this.key = key;
			this.color = Easik.getInstance().getSettings().getColor(key);

			updateIcon();
			addActionListener(this);
		}

		/**
		 *
		 */
		public void updateIcon() {
			int iconHeight = 18; // Includes the border, 1px on each edge
			int iconWidth = 25;
			BufferedImage img = new BufferedImage(iconWidth, iconHeight, BufferedImage.TYPE_INT_ARGB);
			int colorRGB = color.getRGB(), edgeRGB = Color.black.getRGB();
			int[] pixels = new int[iconHeight * iconWidth];
			int bottomStart = iconWidth * (iconHeight - 1);

			for (int i = 0; i < pixels.length; i++) {
				pixels[i] = ((i < iconWidth) || ( // Top row
				i >= bottomStart) || ( // Bottom row
				i % iconWidth == 0) || ( // Left column
				i % iconWidth == (iconWidth - 1))) // Right column
						? edgeRGB
						: colorRGB;
			}

			img.setRGB(0, 0, iconWidth, iconHeight, pixels, 0, iconWidth);
			setIcon(new ImageIcon(img));
		}

		/**
		 *
		 */
		public void saveColor() {
			Easik.getInstance().getSettings().setColor(key, color);
		}

		/**
		 *
		 *
		 * @param e1
		 */
		@Override
		public void actionPerformed(ActionEvent e1) {
			colorChooser.setColor(color);

			ActionListener acceptColor = new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					color = colorChooser.getColor();

					updateIcon();
				}
			};

			JColorChooser.createDialog(ProgramSettingsUI.this, "Choose color", true, colorChooser, acceptColor, null)
					.setVisible(true);
		}
	}

	// Very simple container class representing either a name/key color field
	// pair,
	// or a title of the following fields (when key is null).

	/**
	 *
	 *
	 * @version 12/09/12
	 * @author Christian Fiddick
	 */
	private class ColorField {
		/**  */
		public String name, key;

		/**
		 *
		 *
		 * @param title
		 */
		public ColorField(String title) {
			this(title, null);
		}

		/**
		 *
		 *
		 * @param name
		 * @param key
		 */
		public ColorField(String name, String key) {
			this.name = name;
			this.key = key;
		}
	}
}
