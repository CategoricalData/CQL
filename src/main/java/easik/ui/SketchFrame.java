package easik.ui;

//~--- non-JDK imports --------------------------------------------------------
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
//~--- JDK imports ------------------------------------------------------------
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import easik.Easik;
import easik.EasikTools;
import easik.database.base.PersistenceDriver;
import easik.model.ModelStateManager;
import easik.model.constraint.CommutativeDiagram;
import easik.model.constraint.ModelConstraint;
import easik.model.constraint.ProductConstraint;
import easik.model.constraint.SumConstraint;
import easik.model.states.BasicEditingState;
import easik.model.ui.ModelFrame;
import easik.overview.Overview;
import easik.overview.vertex.SketchNode;
import easik.sketch.Sketch;
import easik.sketch.edge.SketchEdge;
import easik.sketch.util.graph.SketchGraphModel;
import easik.sketch.vertex.EntityNode;
import easik.ui.datamanip.jdbc.ExecPreparedDeleteAction;
import easik.ui.datamanip.jdbc.ExecPreparedInsertAction;
import easik.ui.menu.AboutAction;
import easik.ui.menu.AddCommutativeDiagramAction;
import easik.ui.menu.AddEqualizerConstraintAction;
//import easik.ui.menu.AddLimitConstraintAction;
import easik.ui.menu.AddProductConstraintAction;
import easik.ui.menu.AddPullbackConstraintAction;
import easik.ui.menu.AddSumConstraintAction;
import easik.ui.menu.DocumentInfoAction;
import easik.ui.menu.ExportDatabaseAction;
import easik.ui.menu.ExportFileAction;
import easik.ui.menu.ExportImageAction;
import easik.ui.menu.FileQuitAction;
import easik.ui.menu.HelpAction;
import easik.ui.menu.RedoAction;
import easik.ui.menu.SketchExportAction;
import easik.ui.menu.UndoAction;
import easik.ui.menu.popup.AddRowAction;
import easik.ui.menu.popup.DatabaseOptions;
import easik.ui.menu.popup.DeleteFromSketchAction;
import easik.ui.menu.popup.DeleteRowAction;
import easik.ui.menu.popup.DisconnectAction;
import easik.ui.menu.popup.EdgeOptions.Edge;
import easik.ui.menu.popup.EditSketchEdgeAction;
import easik.ui.menu.popup.NewEntityAction;
import easik.ui.menu.popup.NewSketchEdgeAction;
import easik.ui.menu.popup.OverrideConstraintAction;
import easik.ui.menu.popup.RenameInSketchAction;
import easik.ui.menu.popup.UpdateRowAction;
import easik.ui.menu.popup.ViewDataAction;
import easik.ui.tree.ModelInfoTreeUI;
import easik.ui.tree.popup.AddAttributeAction;
import easik.ui.tree.popup.AddPathAction;
import easik.ui.tree.popup.AddUniqueKeyAction;

/**
 * This is the swing starting point for the program. It contains a sketch,
 * through which the user can design a db schema, and once satisfied, connect to
 * a MySQL server and manipulate the data contained therein.
 *
 * @author Rob Fletcher 2005
 * @author Kevin Green 2006
 * @author Vera Ranieri 2006
 * @version 2006-08-04 Kevin Green
 */
public class SketchFrame extends ModelFrame<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> {
	private static final long serialVersionUID = 2167432738937649186L;

	/**  */
	private JMenuItem _AddAttributePopItem, _AddUniqueKeyPopItem, _NextPopItem;

	/**
	 * Menu (items) that we need a reference to enabling us to toggle their
	 * visibility
	 */
	private JMenuItem _AddCommutativeDiagramMenuItem, _AddSumMenuItem, _AddProductMenuItem, _AddPullbackMenuItem,
			_AddEqualizerMenuItem;
	// _AddLimMenuItem;

	/**  */
	private JMenuItem _AddEntityPopItem, _AddEntityItem, _AddEdgePopItem, _AddInjEdgePopItem, _AddParEdgePopItem,
			_AddSelfParEdgePopItem;

	/**  */
	private JMenuItem _AddRowPopItem, _UpdateRowPopItem, _AddByQueryPopItem;

	/**  */
	@SuppressWarnings("unused")
	private JMenuItem _DeleteAllPopItem;

	/**  */
	private JMenuItem _DeleteByQueryPopItem, _DeleteRowsPopItem, _ViewDataPopItem, _ProgramSettingsItem;

	/**  */
	private JMenuItem _DeleteConstraintPopItem;

	/**  */
	private JMenuItem _FinishPopItem, _CancelPopItem, _AddPathPopItem;

	/**  */
	private JMenuItem _RenameEntityPopItem, _DeleteEntityPopItem, _EditEdgePopItem, _DeleteEdgePopItem;

	/**  */
	private JCheckBoxMenuItem _ShowAttributesToggle, _ShowConstraintToggle;

	/**  */
	private JMenuItem _UndoItem, _RedoItem;

	/** The components making up the button pane */
	private JPanel _buttons;

	/** The label above our info tree which displays connection information */
	private JLabel _connectionStatusLabel;

	/** Popup menus for various selections */
	private JPopupMenu _editSketchPopMenu, _editEntityPopMenu, _editConstraintPopMenu, _editEdgePopMenu,
			_editMixedPopMenu, _addConstraintPopMenu, _manipulateEntityPopMenu;

	/** The information tree */
	private ModelInfoTreeUI<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> _infoTreeUI;

	/**  */
	private JLabel _instructionBar;

	/** The split pane */
	private JSplitPane _mainSplitPane;

	/** The frames current mode - sketch editing or data manipulation */
	private Mode _mode;

	/** The SketchNode representing this frame */
	private SketchNode _myNode;

	/** The next/finish/cancel buttons */
	private JButton _nextButton, _finishButton, _cancelButton;

	/** The current sketch */
	private Sketch _ourSketch;

	/** The scroll pane of the sketch */
	private JScrollPane _ourSketchScrollPane;

	/** The point where the popup menu should appear */
	private Point _popupPosition;

	/**
	 * The recent files menu created from the recent files stored in the settings
	 * file
	 */
	@SuppressWarnings("unused")
	private JMenu _recentFilesMenu;

	/** The right, button, and main panels of the frame */
	private JPanel _rightPane, _mainPane, _buttonPane;

	/**  */
	private JMenuItem addEdge, addInjEdge, addParEdge, addSelfParEdge, addAtt, addUK, rename, del, editEdge,
			addRowMenuItem, insertQueryMenuItem, deleteRowMenuItem, deleteQueryMenuItem;

	/** Edit menu and items */

	// FIXME: can these be local variables in buildMenu() and
	// still be referenced by anonymous inner class?
	private JMenu menuEditEdit, menuEditManip, menuView, menuEditAdd, menuEditAddEdge, menuConstraint;

	/**  */
	private JMenu menuSQL;

	/**
	 * Creates a sketch frame, sets some sizes, sets up the properties builds the
	 * menus, and lays out the swing components. Sketch frame's default mode allows
	 * the user to edit the sketch, as opposed to editing a db defined by the
	 * current sketch layout.
	 *
	 * @param inOverview The overview in which this frame's sketch will exist
	 */
	public SketchFrame(final Overview inOverview) {
		super("EASIK - Untitled");

		final int defaultWidth = Integer.parseInt(_settings.getProperty("sketch_display_width", "700"));
		final int defaultHeight = Integer.parseInt(_settings.getProperty("sketch_display_height", "500"));
		final int defaultLocationX = Integer.parseInt(_settings.getProperty("sketch_frame_location_x", "0"));
		final int defaultLocationY = Integer.parseInt(_settings.getProperty("sketch_frame_location_y", "25"));

		setLocation(defaultLocationX, defaultLocationY);
		setSize(defaultWidth, defaultHeight);

		if (ApplicationFrame.EASIK_Icon != null) {
			setIconImage(ApplicationFrame.EASIK_Icon);
		}

		// Initialize all variables
		_editSketchPopMenu = new JPopupMenu();
		_editEntityPopMenu = new JPopupMenu();
		_editEdgePopMenu = new JPopupMenu();
		_editMixedPopMenu = new JPopupMenu();
		_editConstraintPopMenu = new JPopupMenu();
		_addConstraintPopMenu = new JPopupMenu();
		_manipulateEntityPopMenu = new JPopupMenu();
		_popupPosition = new Point(0, 0);
		_nextButton = new JButton("Next");
		_cancelButton = new JButton("Cancel");
		_finishButton = new JButton("Finish");
		_rightPane = new JPanel();
		_buttonPane = new JPanel();
		_instructionBar = new JLabel("Instruction bar");
		_buttons = new JPanel();
		_mainPane = new JPanel();
		_ourSketch = new Sketch(this, inOverview);
		_infoTreeUI = new ModelInfoTreeUI<>(this);
		_mode = Mode.EDIT;

		_infoTreeUI.refreshTree();

		// Build Menus
		buildMenu();
		buildPopupMenu();

		// Setup Sketch Pane
		_ourSketchScrollPane = new JScrollPane(_ourSketch);

		_ourSketchScrollPane.setMinimumSize(new Dimension(300, 300));

		// Setup button pane
		_buttonPane = new JPanel();

		_buttonPane.setLayout(new BoxLayout(_buttonPane, BoxLayout.Y_AXIS));
		_buttonPane.add(_instructionBar);
		_buttonPane.add(_buttons);
		_buttons.setLayout(new GridLayout(1, 0));
		_buttons.add(_nextButton);
		_buttons.add(_finishButton);
		_buttons.add(_cancelButton);

		// Button action listener setup
		_nextButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e1) {
				_ourSketch.getStateManager().nextClicked();
			}
		});
		_finishButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e1) {
				_ourSketch.getStateManager().finishClicked();
			}
		});
		_cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e1) {
				_ourSketch.getStateManager().cancelClicked();
			}
		});

		// Setup entire right pane
		_rightPane.setLayout(new BorderLayout());

		_treeName = new JLabel(_ourSketch.getDocInfo().getName());
		_connectionStatusLabel = new JLabel("Disconnected");

		_rightPane.add(_treeName, BorderLayout.NORTH);
		_rightPane.add(_connectionStatusLabel, BorderLayout.NORTH);
		_rightPane.add(_infoTreeUI, BorderLayout.CENTER);

		// Setup main pane
		_mainPane.setLayout(new BorderLayout());
		_mainPane.add(_ourSketchScrollPane, BorderLayout.CENTER);
		_mainPane.add(_buttonPane, BorderLayout.SOUTH);

		// Setup entire window
		_mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, _mainPane, _rightPane);

		final int dividerPos = Integer
				.parseInt(_settings.getProperty("sketch_divider_position", String.valueOf((defaultWidth - 255))));

		_mainSplitPane.setDividerLocation(dividerPos);
		_mainSplitPane.setDividerSize(10);
		_mainSplitPane.setResizeWeight(1);
		_mainSplitPane.setOneTouchExpandable(true);
		_mainSplitPane.setContinuousLayout(true);

		getContentPane().add(_mainSplitPane, BorderLayout.CENTER);

		// JPanel aqlPanel = new JPanel();
		// JButton toAqlButton = new JButton("To CQL");
		// aqlPanel.add(toAqlButton);
		// getContentPane().add(aqlPanel, BorderLayout.NORTH);

		// Update the display in the overview whenever we lost focus
		addWindowFocusListener(new WindowFocusListener() {
			@Override
			public void windowGainedFocus(final WindowEvent e) {
			}

			@Override
			public void windowLostFocus(final WindowEvent e) {
				_ourSketch.getOverview().refreshAll();
			}
		});

		// Call to state manager's initialize method, which can happen now that
		// the frame has been set up
		_ourSketch.getStateManager().initialize();
	}

	/**
	 * Sets up the frame to allow sketch editing, and makes visible.
	 */
	public void enableSketchEdit() {
		if (_ourSketch.getDatabase().hasConnection() || _ourSketch.isSynced()) {
			if (JOptionPane.showConfirmDialog(this,
					'\'' + _ourSketch.getName() + "' is synced with a db. Continue and break sync?", "Warning",
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.CANCEL_OPTION) {
				return;
			}

			_ourSketch.getDatabase().cleanDatabaseDriver();
			_ourSketch.setSynced(false);
		}

		// enable/disable menus and menu items that may have been dissabled if
		// we have been in data manip mode
		_AddCommutativeDiagramMenuItem.setEnabled(true);
		_AddSumMenuItem.setEnabled(true);
		_AddProductMenuItem.setEnabled(true);
		_AddPullbackMenuItem.setEnabled(true);
		_AddEqualizerMenuItem.setEnabled(true);
		// _AddLimMenuItem.setEnabled(true);
		menuEditEdit.setVisible(true);
		menuConstraint.setVisible(true);
		menuEditManip.setVisible(false);
		menuSQL.setVisible(false);

		_mode = Mode.EDIT;

		setEasikTitle();
		setVisible(true);
		_ourSketch.refresh();
	}

	/**
	 * Sets up the frame to allow data manipulation of a db defined by the current
	 * sketch layout. If we can't get a connection, or the user cannot confirm that
	 * the sketch indeed represents the db, we do nothing.
	 */
	public void enableDataManip(boolean show) {
		final String lineSep = EasikTools.systemLineSeparator();

		if (!getMModel().getDatabase().hasConnection()) {
			String type;
			if (!getMModel().getConnectionParams().containsKey("type")) {
				type = getMModel().getDatabaseType(); // start by getting the
				// database type
			} else {
				type = getMModel().getConnectionParams().get("type");
			}

			if (type == null) {
				return;
			}

			final DatabaseOptions dbopts = new DatabaseOptions(type, getMModel().getFrame());

			if (!dbopts.isAccepted()) {
				return;
			}

			// set the database access object up for database exporting
			if (!getMModel().getDatabase().setDatabaseExport(type, dbopts.getParams())) {
				_ourSketch.getDatabase().cleanDatabaseDriver();

				return;
			}

			if (!getMModel().getDatabase().hasActiveDriver()) {
				_ourSketch.getDatabase().cleanDatabaseDriver(); // should have
																// been loaded
																// by now
				return;
			}

			getMModel().getDatabase().getJDBCConnection();

		}

		if (!_ourSketch.getDatabase().hasActiveDriver()) {
			_ourSketch.getDatabase().cleanDatabaseDriver();
			JOptionPane.showMessageDialog(this,
					"This Sketch does not have an outgoing connection.\nYou must connect through 'Export to DBMS' menu option...",
					"Manipulate db...", JOptionPane.INFORMATION_MESSAGE);

			return;
		}

		if (!_ourSketch.isSynced()) {
			final String[] choices = { "Yes", "Cancel" };
			final int choice = JOptionPane.showOptionDialog(this,
					"It appears that this sketch has either not been exported to the db," + lineSep
							+ "or modifications have been made since an export." + lineSep
							+ "Are you sure that the db is accurately represented by this sketch?",
					"Confirm Connection", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, choices,
					choices[0]);

			if (choice != 0) {
				_ourSketch.getDatabase().cleanDatabaseDriver();

				return;
			}
		}

		_ourSketch.setSynced(true);

		// enable menus and menu items that may have been dissabled if we have
		// been in data manip mode
		_AddCommutativeDiagramMenuItem.setEnabled(false);
		_AddSumMenuItem.setEnabled(false);
		_AddProductMenuItem.setEnabled(false);
		_AddPullbackMenuItem.setEnabled(false);
		_AddEqualizerMenuItem.setEnabled(false);
		// _AddLimMenuItem.setEnabled(false);
		menuEditEdit.setVisible(false);
		menuConstraint.setVisible(false);
		menuEditManip.setVisible(true);
		menuSQL.setVisible(true);

		_mode = Mode.MANIPULATE;

		setEasikTitle();
		if (show) {
			setVisible(true);
		}
		_ourSketch.refresh();
	}

	/**
	 * Trys to close window as long it is not dirty.
	 */
	@Override
	public void closeWindow() {
		// We're exiting, so update the width/height of the main window, and
		// save the settings
		_settings.setProperty("sketch_display_width", String.valueOf(getWidth()));
		_settings.setProperty("sketch_display_height", String.valueOf(getHeight()));
		_settings.setProperty("sketch_divider_position", String.valueOf(_mainSplitPane.getDividerLocation()));
		_settings.setProperty("sketch_frame_location_x", String.valueOf(getX()));
		_settings.setProperty("sketch_frame_location_y", String.valueOf(getY()));
		_settings.store();

		// Refresh displayed thumbnail
		_ourSketch.clearSelection();
		_ourSketch.getOverview().refreshAll();

		final ModelStateManager<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> stateManager = _ourSketch
				.getStateManager();
		final SketchGraphModel sgm = _ourSketch.getGraphModel();

		while (sgm.inInsignificantUpdate()) {
			sgm.cancelInsignificantUpdate();
		}

		while (!(stateManager.peekState() instanceof BasicEditingState)) {
			stateManager.popState();
		}

		setVisible(false);
	}

	/**
	 * Sets if the button pane is visible or not
	 *
	 * @param isVis True if visible, false if hidden.
	 */
	@Override
	public void setButtonPaneVisibility(final boolean isVis) {
		_buttonPane.setVisible(isVis);
	}

	/**
	 * The instruction bar tells the user what to do when the button pane in
	 * enables, as it is not always obvious... This method changes that text.
	 *
	 * @param inText The new text
	 */
	@Override
	public void setInstructionText(final String inText) {
		_instructionBar.setText(inText);
	}

	/**
	 * Returns the cancel button.
	 *
	 * @return The cancel button
	 */
	@Override
	public JButton getCancelButton() {
		return _cancelButton;
	}

	/**
	 * Returns the next button
	 *
	 * @return The next button
	 */
	@Override
	public JButton getNextButton() {
		return _nextButton;
	}

	/**
	 * Returns the finish button
	 *
	 * @return The finish button
	 * @since 2006-05-30 Vera Ranieri
	 */
	@Override
	public JButton getFinishButton() {
		return _finishButton;
	}

	/**
	 * Returns the current sketch
	 *
	 * @return The sketch
	 */
	@Override
	public Sketch getMModel() {
		return _ourSketch;
	}

	/**
	 * Returns the Overview
	 *
	 * @return The overview
	 */
	@Override
	public Overview getOverview() {
		return _ourSketch.getOverview();
	}

	/**
	 * Returns the InfoTreeUI object
	 *
	 * @return The InfoTreeUI object
	 */
	@Override
	public ModelInfoTreeUI<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> getInfoTreeUI() {
		return _infoTreeUI;
	}

	/**
	 * Sets the InfoTreeUI
	 *
	 * @param inInfoTreeUI The new InfoTreeUI
	 */
	public void setInfoTreeUI(
			final ModelInfoTreeUI<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> inInfoTreeUI) {
		_rightPane.remove(_infoTreeUI);

		_infoTreeUI = inInfoTreeUI;

		_rightPane.add(_infoTreeUI);
		_mainSplitPane.setDividerLocation(_mainSplitPane.getDividerLocation());
	}

	/**
	 * Creates the popup menu
	 */
	private void buildPopupMenu() {
		_editSketchPopMenu.add(_AddEntityPopItem = new JMenuItem(new NewEntityAction<>(_popupPosition, this)));
		_editEntityPopMenu.add(_AddEdgePopItem = new JMenuItem(new NewSketchEdgeAction(this, Edge.NORMAL)));
		_editEntityPopMenu.add(_AddInjEdgePopItem = new JMenuItem(new NewSketchEdgeAction(this, Edge.INJECTIVE)));
		_editEntityPopMenu.add(_AddParEdgePopItem = new JMenuItem(new NewSketchEdgeAction(this, Edge.PARTIAL)));
		_editEntityPopMenu.add(_AddSelfParEdgePopItem = new JMenuItem(new NewSketchEdgeAction(this, Edge.SELF)));
		_editEntityPopMenu.add(_AddAttributePopItem = new JMenuItem(new AddAttributeAction<>(this)));
		_editEntityPopMenu.add(_AddUniqueKeyPopItem = new JMenuItem(new AddUniqueKeyAction<>(this)));
		_editEntityPopMenu.add(_RenameEntityPopItem = new JMenuItem(new RenameInSketchAction(this)));
		_editEntityPopMenu.add(_DeleteEntityPopItem = new JMenuItem(new DeleteFromSketchAction(this)));
		_editEdgePopMenu.add(_EditEdgePopItem = new JMenuItem(new EditSketchEdgeAction(this)));
		_editEdgePopMenu.add(_DeleteEdgePopItem = new JMenuItem(new DeleteFromSketchAction(this)));
		_editConstraintPopMenu.add(_AddPathPopItem = new JMenuItem(new AddPathAction<>(this)));
		_editConstraintPopMenu.add(_DeleteConstraintPopItem = new JMenuItem(new DeleteFromSketchAction(this)));
		_editMixedPopMenu.add(_DeleteAllPopItem = new JMenuItem(new DeleteFromSketchAction(this)));
		_addConstraintPopMenu.add(_NextPopItem = new JMenuItem("Next"));
		_addConstraintPopMenu.add(_FinishPopItem = new JMenuItem("Finish"));
		_addConstraintPopMenu.add(_CancelPopItem = new JMenuItem("Cancel"));
		_manipulateEntityPopMenu.add(_AddRowPopItem = new JMenuItem(new AddRowAction(_ourSketch)));
		_manipulateEntityPopMenu.add(_AddByQueryPopItem = new JMenuItem(new ExecPreparedInsertAction(_ourSketch)));
		_manipulateEntityPopMenu.add(new JSeparator());
		_manipulateEntityPopMenu.add(_UpdateRowPopItem = new JMenuItem(new UpdateRowAction(_ourSketch)));
		_manipulateEntityPopMenu.add(new JSeparator());
		_manipulateEntityPopMenu.add(_DeleteRowsPopItem = new JMenuItem(new DeleteRowAction(_ourSketch)));
		_manipulateEntityPopMenu.add(_DeleteByQueryPopItem = new JMenuItem(new ExecPreparedDeleteAction(_ourSketch)));
		_manipulateEntityPopMenu.add(new JSeparator());
		_manipulateEntityPopMenu.add(_ViewDataPopItem = new JMenuItem(new ViewDataAction(_ourSketch)));
		_NextPopItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e1) {
				_ourSketch.getStateManager().nextClicked();
			}
		});
		_FinishPopItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e1) {
				_ourSketch.getStateManager().finishClicked();
			}
		});
		_CancelPopItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e1) {
				_ourSketch.getStateManager().cancelClicked();
			}
		});
		_ourSketch.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(final MouseEvent evt) {
				mouseReleased(evt);
			}

			@Override
			public void mouseReleased(final MouseEvent evt) {
				if (evt.isPopupTrigger()) {
					final Object rightClickedOn = _ourSketch.getFirstCellForLocation(evt.getX(), evt.getY());

					// When we have a right click, we check to se if it is part
					// of the selection. If
					// it isn't, we change the selection to include only that
					// which was
					// under the click, otherwise we'll trigger the popup for
					// the current selection
					if (rightClickedOn != null) {
						final Object[] selection = _ourSketch.getSelectionCells();

						if (!Arrays.asList(selection).contains(rightClickedOn)) {
							_ourSketch.setSelectionCell(_ourSketch.getFirstCellForLocation(evt.getX(), evt.getY()));
						}
					} else {
						_ourSketch.setSelectionCells(new Object[0]);
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
	 * Based on the current selection, decided which popup menu is appropiriate to
	 * display. A call is made to its "set items" method, and the menu is returned.
	 * 
	 * @return The appropriate popup menu based on current selection, ready to
	 *         display
	 */
	public JPopupMenu setPopMenu() {
		/*
		 * we want to be able to add edges and nodes as we go for ease of use so this
		 * section is commented out
		 * 
		 * Federico Mora
		 * 
		 * // If adding constraint, does not matter what is selected - give back that
		 * menu if (_ourSketch.getStateManager().peekState() instanceof GetPathState) {
		 * setAddConstraintPopItems();
		 * 
		 * return _addConstraintPopMenu; }
		 */

		final Object[] currentSelection = _ourSketch.getSelectionCells();

		if (currentSelection.length == 0) {
			if (_mode == Mode.EDIT) {
				setEditSketchPopItems();

				return _editSketchPopMenu;
			}
			return null;

		}

		// Check for mixed selection
		final Object selected = currentSelection[0];

		for (final Object o : currentSelection) {
			// different types of constraints are considered the same type
			if ((o instanceof ModelConstraint) && (selected instanceof ModelConstraint)) {
				continue;
			}

			if (!(o.getClass() == selected.getClass())) {
				if (_mode == Mode.EDIT) {
					return _editMixedPopMenu;
				}
				return null;

			}
		}

		if (selected instanceof EntityNode) {
			if (_mode == Mode.EDIT) {
				setEditEntityPopItems();

				return _editEntityPopMenu;
			}
			setManipulateEntityPopItems();

			return _manipulateEntityPopMenu;

		} else if (selected instanceof SketchEdge) {
			if (_mode == Mode.EDIT) {
				setEditEdgePopItems();

				return _editEdgePopMenu;
			}
			return null;

		}

		if (selected instanceof ModelConstraint) {
			if (_mode == Mode.EDIT) {
				setEditConstraintPopItems();

				return _editConstraintPopMenu;
			}
		}

		return null;
	}

	/**
	 *
	 */
	public void setEditEntityPopItems() {
		// dissable all elements
		for (final Component c : _editEntityPopMenu.getComponents()) {
			c.setEnabled(false);
		}

		// we always want delete
		_DeleteEntityPopItem.setEnabled(true);

		final Object[] currentSelection = _ourSketch.getSelectionCells();

		if (currentSelection.length == 0) {
			return;
		}

		if (currentSelection.length == 1) {
			_RenameEntityPopItem.setEnabled(true);
			_AddAttributePopItem.setEnabled(true);
			_AddUniqueKeyPopItem.setEnabled(true);
			_AddSelfParEdgePopItem.setEnabled(true);
		}

		if (currentSelection.length == 2) {
			_AddEdgePopItem.setEnabled(true);
			_AddInjEdgePopItem.setEnabled(true);
			_AddParEdgePopItem.setEnabled(true);
		}
	}

	/**
	 *
	 */
	public void setEditEdgePopItems() {
		// dissable all elements
		for (final Component c : _editEdgePopMenu.getComponents()) {
			c.setEnabled(false);
		}

		// we always want delete
		_DeleteEdgePopItem.setEnabled(true);

		if (_ourSketch.getSelectionCount() == 1) {
			_EditEdgePopItem.setEnabled(true);
		}
	}

	/**
	 *
	 */
	public void setEditConstraintPopItems() {
		// dissable all elements
		for (final Component c : _editConstraintPopMenu.getComponents()) {
			c.setEnabled(false);
		}

		// we always want delete
		_DeleteConstraintPopItem.setEnabled(true);

		final Object[] currentSelection = _ourSketch.getSelectionCells();

		if (currentSelection.length == 1) {
			if ((currentSelection[0] instanceof SumConstraint) || (currentSelection[0] instanceof ProductConstraint)
					|| (currentSelection[0] instanceof CommutativeDiagram)) {
				_AddPathPopItem.setEnabled(true);
			}
		}
	}

	/**
	 * Set the visibility of popup menu items based on what is selected.
	 */
	public void setAddConstraintPopItems() {
		_NextPopItem.setEnabled(_nextButton.isEnabled());
		_FinishPopItem.setEnabled(_finishButton.isEnabled());
		_CancelPopItem.setEnabled(_cancelButton.isEnabled());
	}

	/**
	 * Set the visibility of popup menu items based on what is selected.
	 */
	public void setEditSketchPopItems() {
		_AddEntityPopItem.setEnabled(true);
	}

	/**
	 * Sets the visibility of popup menu items based on what is selected. Note:
	 * *MUST* only get called when selection is known to be of entity nodes
	 */
	public void setManipulateEntityPopItems() {
		// dissable all elements
		for (final Component c : _manipulateEntityPopMenu.getComponents()) {
			c.setEnabled(false);
		}

		final Object[] currentSelection = _ourSketch.getSelectionCells();

		if (currentSelection.length == 1) {
			if (_ourSketch.editable((EntityNode) currentSelection[0])) {
				_AddRowPopItem.setEnabled(true);
				_AddByQueryPopItem.setEnabled(true);
				_DeleteRowsPopItem.setEnabled(true);
				_DeleteByQueryPopItem.setEnabled(true);
			}

			_UpdateRowPopItem.setEnabled(true);
			_ViewDataPopItem.setEnabled(true);
		}
	}

	/**
	 * Builds the menu
	 */
	private void buildMenu() {
		final JMenuBar mainMenu;
		final JMenu menuFile;
		final JMenu menuHelp;

		mainMenu = new JMenuBar();

		// Make the File Menu
		menuFile = new JMenu("File");

		mainMenu.add(menuFile);
		menuFile.setMnemonic(KeyEvent.VK_F);

		final JMenu exportMenu = new JMenu("Export to");

		menuFile.add(exportMenu);

		// Export to server:
		addMenuItem(exportMenu, new JMenuItem(new ExportDatabaseAction(this, _ourSketch)), KeyEvent.VK_D);

		// Export to SQL text dump:
		addMenuItem(exportMenu, new JMenuItem(new ExportFileAction(this, _ourSketch)), KeyEvent.VK_F);
		addMenuItem(exportMenu, new JMenuItem(new ExportImageAction<>(this)), null);
		addMenuItem(exportMenu, new JMenuItem(new SketchExportAction(this)), null);
		menuFile.addSeparator();
		addMenuItem(menuFile, new JMenuItem(new DocumentInfoAction(this)), KeyEvent.VK_I);
		menuFile.addSeparator();
		addMenuItem(menuFile, new JMenuItem(new FileQuitAction(this)), KeyEvent.VK_W);

		// Make the Edit menu for "edit mode"
		menuEditEdit = new JMenu("Edit");

		mainMenu.add(menuEditEdit);
		addMenuItem(menuEditEdit, _UndoItem = new JMenuItem(new UndoAction(_ourSketch)), KeyEvent.VK_Z);
		addMenuItem(menuEditEdit, _RedoItem = new JMenuItem(new RedoAction(_ourSketch)), KeyEvent.VK_Y);
		menuEditEdit.addSeparator();
		menuEditEdit.add(menuEditAdd = new JMenu("Add"));
		addMenuItem(menuEditAdd, _AddEntityItem = new JMenuItem(new NewEntityAction<>(null, this)), null);
		menuEditAdd.add(menuEditAddEdge = new JMenu("Add edge"));
		addMenuItem(menuEditAddEdge, addEdge = new JMenuItem(new NewSketchEdgeAction(this, Edge.NORMAL)), null);
		addMenuItem(menuEditAddEdge, addInjEdge = new JMenuItem(new NewSketchEdgeAction(this, Edge.INJECTIVE)), null);
		addMenuItem(menuEditAddEdge, addParEdge = new JMenuItem(new NewSketchEdgeAction(this, Edge.PARTIAL)), null);
		addMenuItem(menuEditAddEdge, addSelfParEdge = new JMenuItem(new NewSketchEdgeAction(this, Edge.SELF)), null);
		addMenuItem(menuEditAdd, addAtt = new JMenuItem(new AddAttributeAction<>(this)), null);
		addMenuItem(menuEditAdd, addUK = new JMenuItem(new AddUniqueKeyAction<>(this)), null);
		addMenuItem(menuEditEdit, del = new JMenuItem(new DeleteFromSketchAction(this)), null);
		addMenuItem(menuEditEdit, rename = new JMenuItem(new RenameInSketchAction(this)), KeyEvent.VK_R);
		menuEditEdit.add(new JSeparator());
		addMenuItem(menuEditEdit, editEdge = new JMenuItem(new EditSketchEdgeAction(this)), KeyEvent.VK_E);
		menuEditEdit.add(new JSeparator());
		addMenuItem(menuEditEdit, _ProgramSettingsItem = new JMenuItem("Preferences"), KeyEvent.VK_P); // If
																										// you
																										// change
																										// this
																										// title,
																										// update
																										// OSX.java
		_ProgramSettingsItem.setToolTipText("Set global EASIK preferences");
		_ProgramSettingsItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e1) {
				final ProgramSettingsUI myUI = new ProgramSettingsUI(SketchFrame.this);

				myUI.showDialog();
			}
		});

		// add listener to enable appropriate menu items
		menuEditEdit.addMenuListener(new MenuListener() {
			@Override
			public void menuCanceled(final MenuEvent e) {
			}

			@Override
			public void menuDeselected(final MenuEvent e) {
			}

			@Override
			public void menuSelected(final MenuEvent e) {
				final Object[] selection = _ourSketch.getSelectionCells();
				final SketchGraphModel model = _ourSketch.getGraphModel();
				final boolean undoable = model.canUndo() && !_ourSketch.getDatabase().hasConnection()
						&& !_ourSketch.isSynced();
				final boolean redoable = model.canRedo() && !_ourSketch.getDatabase().hasConnection()
						&& !_ourSketch.isSynced();

				_UndoItem.setEnabled(undoable);
				_RedoItem.setEnabled(redoable);
				menuEditAdd.setEnabled(true);
				menuEditAddEdge.setEnabled(true);
				_AddEntityItem.setEnabled(false);
				addEdge.setEnabled(false);
				addInjEdge.setEnabled(false);
				addParEdge.setEnabled(false);
				addSelfParEdge.setEnabled(false);
				addAtt.setEnabled(false);
				addUK.setEnabled(false);
				rename.setEnabled(false);
				del.setEnabled(false);
				editEdge.setEnabled(false);

				if (selection.length == 0) {
					_AddEntityItem.setEnabled(true);
					menuEditAddEdge.setEnabled(false);
				}

				if (selection.length >= 1) {
					del.setEnabled(true);

					if (selection.length == 1) {
						if (selection[0] instanceof EntityNode) {
							rename.setEnabled(true);
						}

						if (selection[0] instanceof EntityNode) {
							addSelfParEdge.setEnabled(true);
							addAtt.setEnabled(true);
							addUK.setEnabled(true);
						} else if (selection[0] instanceof SketchEdge) {
							editEdge.setEnabled(true);
						}
					}

					// if selection contains an edge, dissable edit|add...
					for (int i = 0; i < selection.length; i++) {
						if (selection[i] instanceof SketchEdge) {
							menuEditAdd.setEnabled(false);
						}
					}

					if (selection.length >= 3) {
						menuEditAdd.setEnabled(false);
					}
				}

				if ((selection.length == 2) && (selection[0] instanceof EntityNode)
						&& (selection[1] instanceof EntityNode)) {
					addEdge.setEnabled(true);
					addInjEdge.setEnabled(true);
					addParEdge.setEnabled(true);
				}
			}
		});

		// Make the Edit menu for "manipulate mode"
		@SuppressWarnings("unused")
		final JMenuItem updateRowMenuItem;
		@SuppressWarnings("unused")
		final JMenuItem viewDataMenuItem;

		menuEditManip = new JMenu("Edit");

		mainMenu.add(menuEditManip);
		menuEditManip.add(addRowMenuItem = new JMenuItem(new AddRowAction(_ourSketch)));
		menuEditManip.add(insertQueryMenuItem = new JMenuItem(new ExecPreparedInsertAction(_ourSketch)));
		menuEditManip.addSeparator();
		menuEditManip.add(updateRowMenuItem = new JMenuItem(new UpdateRowAction(_ourSketch)));
		menuEditManip.addSeparator();
		menuEditManip.add(deleteRowMenuItem = new JMenuItem(new DeleteRowAction(_ourSketch)));
		menuEditManip.add(deleteQueryMenuItem = new JMenuItem(new ExecPreparedDeleteAction(_ourSketch)));
		menuEditManip.addSeparator();
		menuEditManip.add(viewDataMenuItem = new JMenuItem(new ViewDataAction(_ourSketch)));
		menuEditManip.addMenuListener(new MenuListener() {
			@Override
			public void menuCanceled(final MenuEvent e) {
			}

			@Override
			public void menuDeselected(final MenuEvent e) {
			}

			@Override
			public void menuSelected(final MenuEvent e) {
				final Object[] selection = _ourSketch.getSelectionCells();
				final boolean enable = (selection.length == 1) && (selection[0] instanceof EntityNode);

				for (final Component c : menuEditManip.getMenuComponents()) {
					c.setEnabled(enable);
				}

				if (!enable) {
					return;
				}

				if (!_ourSketch.editable((EntityNode) selection[0])) {
					addRowMenuItem.setEnabled(false);
					insertQueryMenuItem.setEnabled(false);
					deleteRowMenuItem.setEnabled(false);
					deleteQueryMenuItem.setEnabled(false);
				}
			}
		});

		menuView = new JMenu("View");

		mainMenu.add(menuView);
		menuView.add(_ShowAttributesToggle = new JCheckBoxMenuItem("Attributes/keys visible"));
		_ShowAttributesToggle.setToolTipText("Toggle display of attributes and unique keys");
		_ShowAttributesToggle
				.setState("show".equals(Easik.getInstance().getSettings().getProperty("attrib_display", "show")));
		_ShowAttributesToggle.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e1) {
				_ourSketch.refresh();
			}
		});
		menuView.add(_ShowConstraintToggle = new JCheckBoxMenuItem("Constraints visible"));
		_ShowConstraintToggle.setToolTipText("Toggle display of constraints");
		_ShowConstraintToggle.setState(true);
		_ShowConstraintToggle.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e1) {
				ModelConstraint.setAllConstraintsVisible(_ourSketch.getConstraints(), _ShowConstraintToggle.getState());
			}
		});

		// Create constraints menu
		menuConstraint = new JMenu("Constraints");

		menuConstraint.setMnemonic(KeyEvent.VK_C);
		mainMenu.add(menuConstraint);
		addMenuItem(menuConstraint,
				_AddCommutativeDiagramMenuItem = new JMenuItem(new AddCommutativeDiagramAction<>(this)), null);
		addMenuItem(menuConstraint, _AddSumMenuItem = new JMenuItem(new AddSumConstraintAction<>(this)), null);
		addMenuItem(menuConstraint, _AddProductMenuItem = new JMenuItem(new AddProductConstraintAction<>(this)), null);
		addMenuItem(menuConstraint, _AddPullbackMenuItem = new JMenuItem(new AddPullbackConstraintAction<>(this)),
				null);
		addMenuItem(menuConstraint, _AddEqualizerMenuItem = new JMenuItem(new AddEqualizerConstraintAction(this)),
				null);
		// addMenuItem(menuConstraint, _AddLimMenuItem = new JMenuItem(new
		// AddLimitConstraintAction<SketchFrame,
		// SketchGraphModel,Sketch,EntityNode, SketchEdge>(this)), null);

		// Make SQL connection menu
		menuSQL = new JMenu("SQL Connection");

		menuSQL.setMnemonic(KeyEvent.VK_S);
		mainMenu.add(menuSQL);
		addMenuItem(menuSQL, new JMenuItem(new DisconnectAction(_ourSketch)), null);
		addMenuItem(menuSQL, new JMenuItem(new OverrideConstraintAction(_ourSketch)), null); // DTRIG
																								// CF2012

		// Create help menu
		menuHelp = new JMenu("Help");

		mainMenu.add(menuHelp);
		menuHelp.setMnemonic(KeyEvent.VK_H);
		menuHelp.add(new HelpAction());
		menuHelp.add(new AboutAction());

		setJMenuBar(mainMenu);
	}

	/**
	 * Returns the value of the show attributes check box menu item
	 *
	 * @return The value of the show attributes check box menu item
	 */
	@Override
	public boolean getShowAttsVal() {
		return _ShowAttributesToggle.getState();
	}

	/**
	 * Sets the state of the "show constraints" check box menu item
	 * 
	 * @param state The state of the "show constraints" check box menu item
	 */
	public void setShowConstraints(final boolean state) {
		_ShowConstraintToggle.setSelected(state);
	}

	/**
	 * Assigns then SketchNode representing this frame in the overview
	 *
	 * @param inNode
	 */
	public void assignNode(final SketchNode inNode) {
		_myNode = inNode;
	}

	/**
	 * Returns the SketchNode representing this frame in the overview
	 *
	 * @return
	 */
	@Override
	public SketchNode getNode() {
		return _myNode;
	}

	/**
	 * Sets the text of our connection status label located above the info tree to
	 * reflect the given db driver.
	 * 
	 * @param dbd The driver whos connection options we will display
	 */
	public void setConnectionStatus(final PersistenceDriver dbd) {
		if ((dbd != null) && dbd.hasConnection()) {
			_connectionStatusLabel.setText("<html>Connected:<br>   " + dbd.getOption("username") + '@'
					+ dbd.getOption("hostname") + ':' + dbd.getOption("database") + "</html>");
		} else {
			_connectionStatusLabel.setText("Disconnected");
		}
	}

	/**
	 * Returns the frames current edit mode. The edit mode determines what popup
	 * options and menu items are available.
	 * 
	 * @return The frames current edit mode.
	 */
	@Override
	public Mode getMode() {
		return _mode;
	}

	/**
	 * Sets the edit mode of the current frame. This should only be called when
	 * opening an existing sketch; normally, enableDataManip() or enableSketchEdit()
	 * should be called to change the mode.
	 *
	 * @param newMode
	 */
	public void setMode(final Mode newMode) {
		_mode = newMode;
	}

	/**
	 * Sets the title of this frame to "EASIK - &gt;edit mode&lt; - &gt;sketch
	 * name&lt;"
	 */
	public void setEasikTitle() {
		if (_mode == Mode.MANIPULATE) {
			setTitle("EASIK - Manipulate Data - " + _ourSketch.getName());
		} else { // we're in EDIT mode
			setTitle("EASIK - Edit Sketch - " + _ourSketch.getName());
		}
	}

	/**
	 * Sets the enableness of the "add ____ constraint" menu items
	 * 
	 * @param state The state of the add constraint menu items.
	 */
	@Override
	public void enableAddConstraintItems(final boolean state) {
		_AddCommutativeDiagramMenuItem.setEnabled(state);
		_AddSumMenuItem.setEnabled(state);
		_AddProductMenuItem.setEnabled(state);
		_AddPullbackMenuItem.setEnabled(state);
		_AddEqualizerMenuItem.setEnabled(state);
		// _AddLimMenuItem.setEnabled(state);
	}

	/**
	 * Dialog for if the user tries to open sketch for editing while there is an
	 * active connection to a db. Gives the option to stay connected and maintain
	 * limited editing power, or disconnecting for full power.
	 */
	@SuppressWarnings("unused")
	private class MaintainConnectionDialog extends OptionsDialog {
		/**
		 *        
		 */
		private static final long serialVersionUID = 8010178403058366504L;

		/**  */
		JRadioButton keepConnection, throwConnection;

		/**
		 *
		 */
		public MaintainConnectionDialog() {
			super(SketchFrame.this, "Cannot Edit Sketch With Active Connection");

			final ButtonGroup bg = new ButtonGroup();

			keepConnection = new JRadioButton("Keep connection and edit with subset of tools");
			throwConnection = new JRadioButton("Disconnect to have all tools");

			throwConnection.setSelected(true);
			bg.add(keepConnection);
			bg.add(throwConnection);
			setSize(435, 160);
			showDialog();
		}

		/**
		 *
		 *
		 * @return
		 */
		@Override
		public List<Option> getOptions() {
			final List<Option> opts = new LinkedList<Option>();

			opts.add(new Option("", throwConnection));
			opts.add(new Option("", keepConnection));

			return opts;
		}

		/**
		 *
		 *
		 * @return
		 */
		public int getSelection() {
			if (throwConnection.isSelected()) {
				return 1;
			}
			return 0;

		}
	}
}
