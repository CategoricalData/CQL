package easik.ui;

//~--- non-JDK imports --------------------------------------------------------

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.ImageProducer;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import catdata.Pair;
import catdata.Util;
import catdata.aql.exp.AqlEnv;
import catdata.aql.exp.EasikAql;
import catdata.ide.CodeTextPanel;
import catdata.ide.Example;
import catdata.ide.Examples;
import catdata.ide.GUI;
import catdata.ide.GuiUtil;
import catdata.ide.Language;
import easik.Easik;
import easik.overview.Overview;
import easik.overview.vertex.SketchNode;
import easik.overview.vertex.ViewNode;
import easik.ui.menu.AboutAction;
import easik.ui.menu.ExportDatabaseAction;
import easik.ui.menu.FileNewOverviewAction;
import easik.ui.menu.FileOpenAction;
import easik.ui.menu.FileQuitAction;
import easik.ui.menu.FileSaveAction;
import easik.ui.menu.FileSaveAsAction;
import easik.ui.menu.HelpAction;
import easik.ui.menu.OverviewDocumentInfoAction;
import easik.ui.menu.RecentFileAction;
import easik.ui.menu.SketchExportAction;
import easik.ui.menu.popup.DeleteFromOverviewAction;
import easik.ui.menu.popup.ImportSketchAction;
import easik.ui.menu.popup.NewSketchAction;
import easik.ui.menu.popup.NewViewAction;
import easik.ui.menu.popup.OpenSketchAction;
import easik.ui.menu.popup.OpenSketchDataAction;
import easik.ui.menu.popup.OpenViewAction;
import easik.ui.menu.popup.RenameInOverviewAction;
import easik.ui.tree.OverviewInfoTreeUI;

/**
 * This is the swing starting point for the program. This frame contains the
 * entire application. It is accessible through the EASIK singleton by all other
 * classes.
 *
 * @author Rob Fletcher 2005
 * @author Kevin Green 2006
 * @author Vera Ranieri 2006
 * @version 2006-08-04 Kevin Green
 */
public class ApplicationFrame extends EasikFrame {
	private static final long serialVersionUID = 7589983379155396953L;

	/**
	 * The EASIK logo image (reused by SketchFrames and the about screen)
	 */
	public static Image EASIK_Icon = null;

	/**
	 * Various menu items
	 */
	@SuppressWarnings("unused")
	private JMenuItem _AddSketchPopItem, _ImportSketchPopItem, _NewViewPopItem, _NewViewMenuItem, _OpenSketchPopItem, _OpenSketchPopDefItem, _OpenSketchManipulatePopItem, _OpenSketchManipulatePopDefItem, _AddInjEdgePopItem, _RenamePopItem, _DeleteSketchPopItem, _ExportSketchPopItem, _ExportSketchDatabasePopItem, _DeleteMixedPopItem, _AddAttributePopItem, _AddUniqueKeyPopItem,
			_HideConstraintPopItem, _NextPopItem, _FinishPopItem, _CancelPopItem, _AddPathPopItem, _ProgramSettingsItem, _OpenViewPopItem, _RenameViewPopItem, _DeleteViewPopItem;// _ExportViewImage,
																																													// _ExportSketchImage;

	/**
	 * The popup menu for right-clicking on the overview canvas
	 */
	private JPopupMenu _canvasPopupMenu;

	/**
	 * The information tree
	 */
	private OverviewInfoTreeUI _infoTreeUI;

	/**  */
	private JPanel _mainPane;

	/**
	 * The split pane
	 */
	private JSplitPane _mainSplitPane;

	/**
	 * Stores the object we call when creating a new window, to adjust its menus
	 * in an OS-specific way
	 */
	// private EasikMenuAdjuster _menuAdj;

	/**
	 * The popup menu for mixed selections
	 */
	private JPopupMenu _mixedPopupMenu;

	/**
	 * The Overview under this frame
	 */
	private Overview _overview;

	/**
	 * The scroll pane containing the overview
	 */
	private JScrollPane _overviewScrollPane;

	/**
	 * The point where the popup menu should appear
	 */
	private Point _popupPosition;

	/**
	 * The recent files menu created from the recent files stored in the ini
	 * file
	 */
	private JMenu _recentFilesMenu;

	/**
	 * The right and main panels of the frame
	 */
	private JPanel _rightPane;

	/**
	 * The popup menu for right-clicking on a sketch
	 */
	private JPopupMenu _sketchPopupMenu;

	/**
	 * The popup menu for right-clicking on a view
	 */
	private JPopupMenu _viewPopupMenu;

	/**
	 * Creates the application frame, sets some sizes, sets up the properties
	 * builds the menus, and lays out the swing components.
	 */
	public ApplicationFrame() {
		super("EASIK - Untitled");

		final int defaultWidth = Integer.parseInt(_settings.getProperty("overview_display_width", "700"));
		final int defaultHeight = Integer.parseInt(_settings.getProperty("overview_display_height", "500"));
		final int defaultLocationX = Integer.parseInt(_settings.getProperty("overview_frame_location_x", "0"));
		final int defaultLocationY = Integer.parseInt(_settings.getProperty("overview_frame_location_y", "25"));

		this.setLocation(defaultLocationX, defaultLocationY);
		this.setSize(defaultWidth, defaultHeight);

		try {
			final URL easikLogo = Easik.class.getResource("/easik-icon.png");

			EASIK_Icon = Toolkit.getDefaultToolkit().createImage((ImageProducer) easikLogo.getContent());

			this.setIconImage(EASIK_Icon);
		} catch (Exception e) {
			System.err.println("Error loading image");
		}

		// Initialize all variables
		_overview = new Overview(this);
		_canvasPopupMenu = new JPopupMenu();
		_sketchPopupMenu = new JPopupMenu();
		_viewPopupMenu = new JPopupMenu();
		_mixedPopupMenu = new JPopupMenu();
		_popupPosition = new Point(0, 0);
		_rightPane = new JPanel();
		_mainPane = new JPanel();
		_infoTreeUI = new OverviewInfoTreeUI(this);

		_infoTreeUI.refreshTree();

		// Build Menus
		buildMenu();
		buildPopupMenu();

		// Setup Sketch Pane
		_overviewScrollPane = new JScrollPane(_overview);

		_overviewScrollPane.setMinimumSize(new Dimension(300, 300));

		// Setup entire right pane
		_rightPane.setLayout(new BorderLayout());

		_treeName = new JLabel(_overview.getDocInfo().getName());

		_rightPane.add(_treeName, BorderLayout.NORTH);
		_rightPane.add(_infoTreeUI, BorderLayout.CENTER);

		// Setup main pane
		_mainPane.setLayout(new BorderLayout());
		_mainPane.add(_overviewScrollPane, BorderLayout.CENTER);

		// Setup entire window
		_mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, _mainPane, _rightPane);

		final int dividerPos = Integer.parseInt(_settings.getProperty("overview_divider_position", String.valueOf((defaultWidth - 255))));

		_mainSplitPane.setDividerLocation(dividerPos);
		_mainSplitPane.setDividerSize(10);
		_mainSplitPane.setResizeWeight(1);
		_mainSplitPane.setOneTouchExpandable(true);
		_mainSplitPane.setContinuousLayout(true);
		getContentPane().add(_mainSplitPane, BorderLayout.CENTER);
		
		JPanel aqlPanel = new JPanel(new GridLayout(1,4));
		JButton toAqlButton = new JButton("To CQL");
		if (GUI.topFrame == null) {
			toAqlButton.setEnabled(false);
		}
		toAqlButton.addActionListener(x -> {
			try {
				File selFile = File.createTempFile("aql_easik", Language.CQL.fileExtension());
				FileSaveAction.saveFileAql(getOverview(), selFile);
				String str = GuiUtil.readFile(selFile);
				if (str == null) {
					return;
				}
				GUI.newAction(getOverview().getDocInfo().getName(), EasikAql.easikToAql(str), Language.CQL);
				GUI.topFrame.toFront();
				GUI.topFrame.requestFocus();
			} catch (IOException e1) {
				e1.printStackTrace();
				JOptionPane.showMessageDialog(null, "Error, could not create temporary file to transfer to CQL");
			} 
		});
		
		JButton fromAqlButton = new JButton("From last run of CQL");
		if (GUI.topFrame == null) {
			fromAqlButton.setEnabled(false);
		}
		fromAqlButton.addActionListener(x -> {
			try {
				Pair<AqlEnv, String> p = GUI.getCurrent();
				if (p == null) {
					return;
				} else if (p.first == null) {
					JOptionPane.showMessageDialog(null, "Please run the CQL program first");
					return;
				}
				File selFile = File.createTempFile("aql_easik", "easik");
				FileWriter w = new FileWriter(selFile);
				Set<String> warnings = new HashSet<>();
				String str = EasikAql.aqlToEasik(p.first, p.second, warnings);
				w.write(str);
				w.close();
				if (!warnings.isEmpty()) {
					JOptionPane.showMessageDialog(null, new CodeTextPanel("CQL to EASIK warnings", Util.sep(warnings, "\n")));
				}
				getOverview().openOverview(selFile);
			} catch (IOException e1) {
				e1.printStackTrace();
				JOptionPane.showMessageDialog(null, "Error, could not create temporary file to transfer from CQL");
			} 
		});
		
		
		
		List<Example> l = new LinkedList<>();
		l.addAll(Examples.getExamples(Language.EASIK));
		l.addAll(Examples.getExamples(Language.SKETCH));
		JComboBox<Example> box = new JComboBox<>(l.toArray(new Example[0]));
		box.setSelectedIndex(-1);
		box.addActionListener((ActionEvent e) -> {
			Example ex = (Example) box.getSelectedItem();	
			boolean proceed = true;
			if (getOverview().getDirty()) {
				proceed = JUtils.confirmLoss(this);
			}
			if (!proceed) {
				return;
			}
			try {
				File selFile = File.createTempFile("aql_easik", ex.lang().fileExtension());
				FileWriter w = new FileWriter(selFile);
				w.write(ex.getText());
				w.close();
				if (ex.lang().equals(Language.EASIK)) {
					getOverview().openOverview(selFile);
				} else if (ex.lang().equals(Language.SKETCH)) {
					new ImportSketchAction(_popupPosition, _overview).actionPerformed0(selFile);
				}
			} catch (IOException e1) {
				e1.printStackTrace();
				JOptionPane.showMessageDialog(null, "Error, could not create temporary file for example");
			} 
		});

		aqlPanel.add(toAqlButton);
		aqlPanel.add(fromAqlButton);
		aqlPanel.add(new JLabel("Load Example", SwingConstants.RIGHT));
		aqlPanel.add(box);
		getContentPane().add(aqlPanel, BorderLayout.NORTH);
		

		
	}

	/**
	 * Tries to close window as long it is not dirty.
	 */
	@Override
	public void closeWindow() {
		boolean proceed = true;

		if (_overview.getDirty()) {
			final String[] choices = { "Save and Exit", "Exit without Saving", "Cancel" };
			String title = _overview.getDocInfo().getName();

			this.updateTitle();

			final int result = JOptionPane.showOptionDialog(this, "The document \"" + title + "\" has unsaved changes.", "Warning", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, choices, choices[0]);

			if (result == 0) // Save and exit
			{
				proceed = FileSaveAction.saveFile(this); // If this returns
															// false, the save
															// failed.
			} else if (result == 1) // Exit without saving
			{
				proceed = true;
			} else // Cancel, or closed the dialog
			{
				proceed = false;
			}
		}

		if (proceed) {
			// We're exiting, so update the width/height of the main window, and
			// save the settings
			_settings.setProperty("overview_display_width", String.valueOf(getWidth()));
			_settings.setProperty("overview_display_height", String.valueOf(getHeight()));
			_settings.setProperty("overview_divider_position", String.valueOf(_mainSplitPane.getDividerLocation()));
			_settings.setProperty("overview_frame_location_x", String.valueOf(getX()));
			_settings.setProperty("overview_frame_location_y", String.valueOf(getY()));
			_settings.store();
			this.dispose();
			Easik.clear();
		}
	}

	/**
	 * Returns the Overview
	 *
	 * @return The overview
	 */
	@Override
	public Overview getOverview() {
		return _overview;
	}

	/**
	 * Returns the InfoTreeUI object
	 *
	 * @return The InfoTreeUI object
	 */
	public OverviewInfoTreeUI getInfoTreeUI() {
		return _infoTreeUI;
	}

	/**
	 * Sets the InfoTreeUI
	 *
	 * @param inInfoTreeUI
	 *            The new InfoTreeUI
	 */
	public void setInfoTreeUI(final OverviewInfoTreeUI inInfoTreeUI) {
		_rightPane.remove(_infoTreeUI);

		_infoTreeUI = inInfoTreeUI;

		_rightPane.add(_infoTreeUI);
		_mainSplitPane.setDividerLocation(_mainSplitPane.getDividerLocation());
	}

	/**
	 * Updates the title of the overview frame to the title stored in the
	 * document UI. Normally called by DocumentInfo.
	 */
	public void updateTitle() {
		String title = _overview.getDocInfo().getName();

		if ((title == null) || "".equals(title)) {
			if (_overview.getFile() != null) {
				title = _overview.getFile().getName();
			} else {
				title = "Untitled";
			}
		}

		setTitle("EASIK - " + title);
	}

	/**
	 * Creates the popup menu
	 */
	private void buildPopupMenu() {
		_canvasPopupMenu.add(_AddSketchPopItem = new JMenuItem(new NewSketchAction(_popupPosition, _overview)));
		_canvasPopupMenu.add(_ImportSketchPopItem = new JMenuItem(new ImportSketchAction(_popupPosition, _overview)));
		_sketchPopupMenu.add(_OpenSketchPopDefItem = new JMenuItem(new OpenSketchAction(_overview, true)));
		_sketchPopupMenu.add(_OpenSketchManipulatePopDefItem = new JMenuItem(new OpenSketchDataAction(_overview, true)));
		_sketchPopupMenu.add(_OpenSketchPopItem = new JMenuItem(new OpenSketchAction(_overview)));
		_sketchPopupMenu.add(_OpenSketchManipulatePopItem = new JMenuItem(new OpenSketchDataAction(_overview)));
		_sketchPopupMenu.addSeparator();
		_sketchPopupMenu.add(_NewViewPopItem = new JMenuItem(new NewViewAction(_overview)));
		_sketchPopupMenu.addSeparator();
		_sketchPopupMenu.add(_RenamePopItem = new JMenuItem(new RenameInOverviewAction(_overview)));
		_sketchPopupMenu.add(_DeleteSketchPopItem = new JMenuItem(new DeleteFromOverviewAction(_overview)));
		_sketchPopupMenu.addSeparator();
		_sketchPopupMenu.add(_ExportSketchPopItem = new JMenuItem(new SketchExportAction(this)));
		_sketchPopupMenu.add(_ExportSketchDatabasePopItem = new JMenuItem(new ExportDatabaseAction(this, null)));
		// _sketchPopupMenu.add(_ExportSketchImage = new JMenuItem(new
		// ExportImageAction<SketchFrame, SketchGraphModel, Sketch, EntityNode,
		// SketchEdge>(null)));
		_viewPopupMenu.add(_OpenViewPopItem = new JMenuItem(new OpenViewAction(_overview)));
		_viewPopupMenu.add(_RenameViewPopItem = new JMenuItem(new RenameInOverviewAction(_overview)));
		_viewPopupMenu.add(_DeleteViewPopItem = new JMenuItem(new DeleteFromOverviewAction(_overview)));
		_viewPopupMenu.addSeparator();
		// _viewPopupMenu.add(_ExportViewImage = new JMenuItem(new
		// ExportImageAction<ViewFrame, ViewGraphModel, View, QueryNode,
		// View_Edge>(null)));
		_mixedPopupMenu.add(_DeleteMixedPopItem = new JMenuItem(new DeleteFromOverviewAction(_overview)));
		_overview.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(final MouseEvent evt) {
				mouseReleased(evt);
			}

			@Override
			public void mouseReleased(final MouseEvent evt) {
				if (evt.isPopupTrigger()) {
					final Object rightClickedOn = _overview.getFirstCellForLocation(evt.getX(), evt.getY());

					// When we have a right click, we check to se if it is part
					// of the selection. If
					// it isn't, we change the selection to include only that
					// which was
					// under the click, otherwise we'll trigger the popup for
					// the current selection
					if (rightClickedOn != null) {
						final Object[] selection = _overview.getSelectionCells();

						if (!Arrays.asList(selection).contains(rightClickedOn)) {
							_overview.setSelectionCell(_overview.getFirstCellForLocation(evt.getX(), evt.getY()));
						}
					} else {
						_overview.setSelectionCells(new Object[0]);
					}

					_popupPosition.setLocation(evt.getX(), evt.getY());

					final JPopupMenu menu = setPopMenu();

					if (menu != null) {
						menu.show(evt.getComponent(), evt.getX(), evt.getY());
					}
				}
			}
		});
	}

	/**
	 *
	 *
	 * @return
	 */
	private JPopupMenu setPopMenu() {
		final Object[] currentSelection = _overview.getSelectionCells();

		if (currentSelection.length == 0) {
			setCanvasPopItems();

			return _canvasPopupMenu;
		}

		// check for mixed selection
		final Object selected = currentSelection[0];

		for (final Object o : currentSelection) {
			if (o.getClass() != selected.getClass()) {
				setMixedPopItems();

				return _mixedPopupMenu;
			}
		}

		if (selected instanceof SketchNode) {
			setSketchPopItems();

			return _sketchPopupMenu;
		} else if (selected instanceof ViewNode) {
			setViewPopItems();

			return _viewPopupMenu;
		}

		return null;
	}

	/**
	 *
	 */
	public void setCanvasPopItems() {
		_AddSketchPopItem.setEnabled(true);
		_ImportSketchPopItem.setEnabled(true);
	}

	/**
	 *
	 */
	public void setSketchPopItems() {
		// disable all elements
		for (final Component c : _sketchPopupMenu.getComponents()) {
			c.setEnabled(false);
		}

		_OpenSketchManipulatePopDefItem.setVisible(false);
		_OpenSketchManipulatePopItem.setVisible(true);
		_OpenSketchPopDefItem.setVisible(false);
		_OpenSketchPopItem.setVisible(true);

		// We always want to be able to delete
		_DeleteSketchPopItem.setEnabled(true);

		final Object[] currentSelection = _overview.getSelectionCells();

		if (currentSelection.length == 1) {
			_RenamePopItem.setEnabled(true);

			if (((SketchNode) currentSelection[0]).getFrame().getMode() == SketchFrame.Mode.EDIT) {
				_NewViewPopItem.setEnabled(true);
			}

			_ExportSketchPopItem.setEnabled(true);

			// Enable sketch export to db
			_ExportSketchDatabasePopItem.setEnabled(true);
			_ExportSketchDatabasePopItem.setEnabled(true);

			// Enable sketch export to image
			// _ExportSketchImage.setEnabled(true);

			// The the export item to work with current selection
			_sketchPopupMenu.remove(_ExportSketchDatabasePopItem);
			// _sketchPopupMenu.remove(_ExportSketchImage);

			// _ExportSketchImage = new JMenuItem(new
			// ExportImageAction<SketchFrame, SketchGraphModel, Sketch,
			// EntityNode, SketchEdge>(((SketchNode)
			// currentSelection[0]).getFrame()));
			_ExportSketchDatabasePopItem = new JMenuItem(new ExportDatabaseAction(this, ((SketchNode) currentSelection[0]).getFrame().getMModel()));

			_sketchPopupMenu.add(_ExportSketchDatabasePopItem);
			// _sketchPopupMenu.add(_ExportSketchImage);

			if (((SketchNode) currentSelection[0]).getFrame().getMModel().isSynced()) {
				_OpenSketchManipulatePopDefItem.setEnabled(true);
				_OpenSketchManipulatePopDefItem.setVisible(true);
				_OpenSketchManipulatePopDefItem.setArmed(true);
				_OpenSketchManipulatePopItem.setVisible(false);
				_OpenSketchPopItem.setEnabled(true);
			} else {
				_OpenSketchPopDefItem.setEnabled(true);
				_OpenSketchPopDefItem.setVisible(true);
				_OpenSketchPopDefItem.setArmed(true);
				_OpenSketchPopItem.setVisible(false);
				_OpenSketchManipulatePopItem.setEnabled(true);
			}
		}
	}

	/**
	 *
	 */
	public void setMixedPopItems() {
		// disable all elements
		for (final Component c : _mixedPopupMenu.getComponents()) {
			c.setEnabled(false);
		}

		@SuppressWarnings("unused")
		final Object[] currentSelection = _overview.getSelectionCells();

		// We always want delete
		_DeleteMixedPopItem.setEnabled(true);
	}

	/**
	 *
	 */
	public void setViewPopItems() {
		// disable all elements
		for (final Component c : _viewPopupMenu.getComponents()) {
			c.setEnabled(false);
		}

		final Object[] currentSelection = _overview.getSelectionCells();

		// We always want delete
		_DeleteViewPopItem.setEnabled(true);

		if (currentSelection.length == 1) {
			_RenameViewPopItem.setEnabled(true);
			_OpenViewPopItem.setEnabled(true);

			// Enable sketch export to image
			// _ExportViewImage.setEnabled(true);

			// The the export item to work with current selection
			// _viewPopupMenu.remove(_ExportViewImage);

			// _ExportViewImage = new JMenuItem(new ExportImageAction<ViewFrame,
			// ViewGraphModel, View, QueryNode, View_Edge>(((ViewNode)
			// currentSelection[0]).getFrame()));

			// _viewPopupMenu.add(_ExportViewImage);
		}
	}

	/**
	 * Builds the menu
	 */
	private void buildMenu() {
		final JMenuBar mainMenu;
		final JMenu menuFile;
		final JMenu menuEdit;
		@SuppressWarnings("unused")
		JMenu menuAction;
		final JMenu menuHelp;

		mainMenu = new JMenuBar();

		// Make the File Menu
		menuFile = new JMenu("File");

		addMenuItem(menuFile, new FileNewOverviewAction(this), KeyEvent.VK_N);
		addMenuItem(menuFile, new FileOpenAction(this), KeyEvent.VK_O);
		menuFile.addSeparator();
		addMenuItem(menuFile, new FileSaveAction(this), KeyEvent.VK_S);
		addMenuItem(menuFile, new FileSaveAsAction(this), KeyEvent.VK_S, InputEvent.SHIFT_DOWN_MASK);
		menuFile.addSeparator();

		_recentFilesMenu = new JMenu("Open recent...");

		menuFile.add(_recentFilesMenu);
		updateRecentFilesMenu();
		menuFile.addSeparator();
		addMenuItem(menuFile, new OverviewDocumentInfoAction(this), KeyEvent.VK_I);
		menuFile.addSeparator();
		addMenuItem(menuFile, new FileQuitAction(this), KeyEvent.VK_Q);
		mainMenu.add(menuFile);
		menuFile.setMnemonic(KeyEvent.VK_F);

		menuEdit = new JMenu("Edit");

		menuEdit.add(new JMenuItem(new NewSketchAction(null, _overview)));
		menuEdit.add(new JMenuItem(new ImportSketchAction(null, _overview)));
		menuEdit.addSeparator();
		menuEdit.add(_NewViewMenuItem = new JMenuItem(new NewViewAction(_overview)));
		mainMenu.add(menuEdit);
		menuEdit.addMenuListener(new MenuListener() {
			@Override
			public void menuSelected(final MenuEvent e1) {
				final Object[] selection = _overview.getSelectionCells();

				_NewViewMenuItem.setEnabled((selection.length == 1) && (selection[0] instanceof SketchNode));
			}

			@Override
			public void menuDeselected(final MenuEvent e1) {
			}

			@Override
			public void menuCanceled(final MenuEvent e1) {
			}
		});
		menuEdit.addSeparator();
		addMenuItem(menuEdit, _ProgramSettingsItem = new JMenuItem("Preferences"), KeyEvent.VK_E); // If
																									// you
																									// change
																									// this
																									// title,
																									// update
																									// OSX.java
		_ProgramSettingsItem.setToolTipText("Set Global Easik Properties");
		_ProgramSettingsItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e1) {
				final ProgramSettingsUI myUI = new ProgramSettingsUI(ApplicationFrame.this);

				myUI.showDialog();
			}
		});
		menuEdit.setMnemonic(KeyEvent.VK_E);

		// Create help menu
		menuHelp = new JMenu("Help");

		mainMenu.add(menuHelp);
		menuHelp.setMnemonic(KeyEvent.VK_H);
		menuHelp.add(new HelpAction());
		menuHelp.add(new AboutAction());
		setJMenuBar(mainMenu);
	}

	/**
	 * Updates the recent files menu items
	 */
	public void updateRecentFilesMenu() {
		_recentFilesMenu.removeAll();

		for (final String file : _settings.getPropertySet("recent_files")) {
			File f = new File(file);

			if (!f.exists()) {
				continue;
			}

			_recentFilesMenu.add(new JMenuItem(new RecentFileAction(file, this)));
		}
	}

	/**
	 * Adds the specified File to the recent files menu, then updates the recent
	 * files menu items to reflect the new list of files.
	 *
	 * @param selFile
	 *            the file to add to the recent files list
	 */
	public void addRecentFile(final File selFile) {
		Easik.getInstance().getSettings().addRecentFile(selFile);
		updateRecentFilesMenu();
	}

	/**
	 * Called by Overview when the overview is set as dirty (or clean). We
	 * simply set the windowModified property for OS-specific window changes
	 * (for example, the "modified" dot in OS X
	 *
	 * @param dirty
	 */
	public void setDirty(final boolean dirty) {
		getRootPane().putClientProperty("windowModified", dirty ? Boolean.TRUE : Boolean.FALSE);
	}

}
