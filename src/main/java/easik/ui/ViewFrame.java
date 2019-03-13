package easik.ui;

//~--- non-JDK imports --------------------------------------------------------

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.Arrays;

import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import easik.model.ui.ModelFrame;
import easik.overview.Overview;
import easik.overview.vertex.ViewNode;
import easik.sketch.Sketch;
import easik.sketch.vertex.EntityNode;
import easik.ui.menu.AboutAction;
import easik.ui.menu.DocumentInfoAction;
import easik.ui.menu.ExportImageAction;
import easik.ui.menu.FileClearViewAction;
import easik.ui.menu.FileQuitAction;
import easik.ui.menu.HelpAction;
import easik.ui.menu.popup.DefineQueryNodeAction;
import easik.ui.menu.popup.DeleteFromViewAction;
import easik.ui.menu.popup.NewQueryNodeAction;
import easik.ui.menu.popup.ViewAddAction;
import easik.ui.menu.popup.ViewDeleteAction;
import easik.ui.menu.popup.ViewQueryAction;
import easik.ui.menu.popup.ViewUpdateAction;
import easik.ui.tree.ModelInfoTreeUI;
import easik.view.View;
//~--- JDK imports ------------------------------------------------------------
import easik.view.edge.View_Edge;
import easik.view.util.graph.ViewGraphModel;
import easik.view.vertex.QueryNode;

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
public class ViewFrame extends ModelFrame<ViewFrame, ViewGraphModel, View, QueryNode, View_Edge> {
	private static final long serialVersionUID = 1568425950131090821L;

	/**  */
	private JMenuItem _AddQueryNodePopItem;

	/**  */
	private JMenuItem _DefineQueryNodePopItem;

	/**  */
	private JMenuItem _DeletePopItem;

	/** Popup menu items */
	private JMenuItem _ViewQueryPopItem;
	private JMenuItem _ViewAddPopItem;
	private JMenuItem _ViewDeletePopItem;
	private JMenuItem _ViewUpdatePopItem;

	/** Popup menu */
	private JPopupMenu _editModePopMenu, _manipModePopMenu;

	/** The information tree */
	private ModelInfoTreeUI<ViewFrame, ViewGraphModel, View, QueryNode, View_Edge> _infoTreeUI;

	/**  */
	private JPanel _mainPane;

	/** The split pane */
	private JSplitPane _mainSplitPane;

	/** The ViewNode representing this frame */
	private ViewNode _myNode;

	/** The current sketch */
	private View _ourView;

	/** The point where the popup menu should appear */
	private Point _popupPosition;

	/** The right and main panels of the frame */
	private JPanel _rightPane;

	/** The scroll pane of the view */
	private JScrollPane _sketchScrollPane;

	/**
	 * Creates a view frame, sets some sizes, sets up the properties builds the
	 * menus, and lays out the swing components.
	 *
	 * @param inOverview
	 *            The overivew in which this frame's sketch will exist
	 * @param inSketch
	 *            The sketch to which our new view points
	 */
	public ViewFrame(Overview inOverview, Sketch inSketch) {
		super("EASIK - Untitled");

		int defaultWidth = Integer.parseInt(_settings.getProperty("view_display_width", "700"));
		int defaultHeight = Integer.parseInt(_settings.getProperty("view_display_height", "500"));
		int defaultLocationX = Integer.parseInt(_settings.getProperty("view_frame_location_x", "0"));
		int defaultLocationY = Integer.parseInt(_settings.getProperty("view_frame_location_y", "25"));

		this.setLocation(defaultLocationX, defaultLocationY);
		this.setSize(defaultWidth, defaultHeight);

		if (ApplicationFrame.EASIK_Icon != null) {
			this.setIconImage(ApplicationFrame.EASIK_Icon);
		}

		// Initialize all variables
		_editModePopMenu = new JPopupMenu();
		_manipModePopMenu = new JPopupMenu();
		_popupPosition = new Point(0, 0);
		_rightPane = new JPanel();
		_mainPane = new JPanel();
		_ourView = new View(this, inSketch, inOverview);
		_infoTreeUI = new ModelInfoTreeUI<>(this);

		_infoTreeUI.refreshTree();

		// Build Menus
		buildMenu();
		buildPopupMenus();

		// Setup View Pane
		_sketchScrollPane = new JScrollPane(_ourView);

		_sketchScrollPane.setMinimumSize(new Dimension(300, 300));

		// Setup entire right pane
		_rightPane.setLayout(new BorderLayout());

		_treeName = new JLabel(_ourView.getDocInfo().getName());

		_rightPane.add(_treeName, BorderLayout.NORTH);
		_rightPane.add(_infoTreeUI, BorderLayout.CENTER);

		// Setup main pane
		_mainPane.setLayout(new BorderLayout());
		_mainPane.add(_sketchScrollPane, BorderLayout.CENTER);

		// Setup entire window
		_mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, _mainPane, _rightPane);

		int dividerPos = Integer.parseInt(_settings.getProperty("view_divider_position", "" + (defaultWidth - 255)));

		_mainSplitPane.setDividerLocation(dividerPos);
		_mainSplitPane.setDividerSize(10);
		_mainSplitPane.setResizeWeight(1);
		_mainSplitPane.setOneTouchExpandable(true);
		_mainSplitPane.setContinuousLayout(true);
		getContentPane().add(_mainSplitPane, BorderLayout.CENTER);

		_ourView.getStateManager().initialize();

		// Update the display in the overview whenever we lost focus
		addWindowFocusListener(new WindowFocusListener() {
			@Override
			public void windowGainedFocus(WindowEvent e) {
			}

			@Override
			public void windowLostFocus(WindowEvent e) {
				_ourView.updateThumb();
				_ourView.getOverview().refresh();
			}
		});
	}

	/**
	 * Forces a refresh of our title bar when we set visible
	 *
	 * @param vis
	 */
	@Override
	public void setVisible(boolean vis) {
		this.setEasikTitle();
		super.setVisible(vis);
	}

	/**
	 * Trys to close window as long it is not dirty.
	 */
	@Override
	public void closeWindow() {
		// We're exiting, so update the width/height of the main window, and
		// save the settings
		_settings.setProperty("view_display_width", "" + getWidth());
		_settings.setProperty("view_display_height", "" + getHeight());
		_settings.setProperty("view_divider_position", "" + _mainSplitPane.getDividerLocation());
		_settings.setProperty("view_frame_location_x", "" + getX());
		_settings.setProperty("view_frame_location_y", "" + getY());
		_settings.store();

		// Refresh displayed thumbnail
		_ourView.clearSelection();
		_ourView.updateThumb();
		_ourView.getOverview().refresh();
		this.setVisible(false);
	}

	/**
	 * Returns the current sketch
	 *
	 * @return The sketch
	 */
	@Override
	public View getMModel() {
		return _ourView;
	}

	/**
	 * Returns the Overview
	 *
	 * @return The overview
	 */
	@Override
	public Overview getOverview() {
		return _ourView.getOverview();
	}

	/**
	 * Returns the InfoTreeUI object
	 *
	 * @return The InfoTreeUI object
	 */
	@Override
	public ModelInfoTreeUI<ViewFrame, ViewGraphModel, View, QueryNode, View_Edge> getInfoTreeUI() {
		return _infoTreeUI;
	}

	/**
	 * Sets the InfoTreeUI
	 *
	 * @param inInfoTreeUI
	 *            The new InfoTreeUI
	 */
	public void setInfoTreeUI(ModelInfoTreeUI<ViewFrame, ViewGraphModel, View, QueryNode, View_Edge> inInfoTreeUI) {
		_rightPane.remove(_infoTreeUI);

		_infoTreeUI = inInfoTreeUI;

		_rightPane.add(_infoTreeUI);
		_mainSplitPane.setDividerLocation(_mainSplitPane.getDividerLocation());
	}

	/**
	 * Popuplates the popup menus with new menu items.
	 */
	private void buildPopupMenus() {
		// added by Sarah van der Laan 2013
		_manipModePopMenu.add(_ViewAddPopItem = new JMenuItem(new ViewAddAction(_ourView)));
		_manipModePopMenu.add(_ViewDeletePopItem = new JMenuItem(new ViewDeleteAction(_ourView)));
		_manipModePopMenu.add(_ViewUpdatePopItem = new JMenuItem(new ViewUpdateAction(_ourView)));

		_manipModePopMenu.add(_ViewQueryPopItem = new JMenuItem(new ViewQueryAction(_ourView)));

		_editModePopMenu.add(_AddQueryNodePopItem = new JMenuItem(new NewQueryNodeAction(_popupPosition, this)));
		_editModePopMenu.add(_DefineQueryNodePopItem = new JMenuItem(new DefineQueryNodeAction(this)));
		// commented out for automatic insertion of existing edges
		// _editModePopMenu.add(_AddEdgePopItem = new JMenuItem(new
		// NewViewEdgeAction(this)));
		_editModePopMenu.add(_DeletePopItem = new JMenuItem(new DeleteFromViewAction(this)));

		_ourView.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent evt) {
				mouseReleased(evt);
			}

			@Override
			public void mouseReleased(MouseEvent evt) {
				if (evt.isPopupTrigger()) {
					Object rightClickedOn = _ourView.getFirstCellForLocation(evt.getX(), evt.getY());

					// When we have a right click, we check to see if it is part
					// of the selection. If
					// it isn't, we change the selection to include only that
					// which was
					// under the click, otherwise we'll trigger the popup for
					// the current selection
					if (rightClickedOn != null) {
						Object[] selection = _ourView.getSelectionCells();

						if (!Arrays.asList(selection).contains(rightClickedOn)) {
							_ourView.setSelectionCell(_ourView.getFirstCellForLocation(evt.getX(), evt.getY()));
						}
					} else {
						_ourView.setSelectionCells(new Object[0]);
					}

					_popupPosition.setLocation(evt.getX(), evt.getY());

					JPopupMenu menu = setPopMenu();

					if (menu != null) {
						menu.show(evt.getComponent(), evt.getX(), evt.getY());
					}
				}
			}
		});
	}

	/**
	 * Figures out which popup menu should be displayed, based on the editing
	 * mode of the sketch on which this is a view. Ensures the the appropriate
	 * menu items are enabled based on the current selection.
	 * 
	 * @return The popup menu that should be displayed. (i.e. the editing popup
	 *         if the sketch of which this view in on is in edit mode and the
	 *         manipulate option otherwise.)
	 */
	public JPopupMenu setPopMenu() {
		Object[] currentSelection = _ourView.getSelectionCells();

		_ViewAddPopItem.setToolTipText("Add a row to the view table refrenced by this query node");

		if (_ourView.getSketch().getFrame().getMode() == SketchFrame.Mode.MANIPULATE) {
			boolean enableViewQuery = currentSelection.length == 1;

			_ViewQueryPopItem.setEnabled(enableViewQuery);

			// Disable all elements
			for (final Component c : _manipModePopMenu.getComponents()) {
				c.setEnabled(false);
			}

			if (currentSelection.length == 0) {

				return null;
			}

			final Object selected = currentSelection[0];

			if (selected instanceof QueryNode) {
				setQueryPopItems((QueryNode) selected);
				return _manipModePopMenu;
			}
			return null;
		}

		// Disable all elements
		for (final Component c : _editModePopMenu.getComponents()) {
			c.setEnabled(false);
		}

		if (currentSelection.length == 0) {
			_AddQueryNodePopItem.setEnabled(true);

			return _editModePopMenu;
		}

		// check for mixed selection
		final Object selected1 = currentSelection[0];

		for (final Object o : currentSelection) {
			if (o.getClass() != selected1.getClass()) {
				// Disable all elements
				for (final Component c : _editModePopMenu.getComponents()) {
					c.setEnabled(false);
				}

				// We always want delete
				_DeletePopItem.setEnabled(true);

				return _editModePopMenu;
			}
		}

		if (selected1 instanceof QueryNode) {
			_DefineQueryNodePopItem.setEnabled(true);
			_DeletePopItem.setEnabled(true);

			return _editModePopMenu;
		} else if (selected1 instanceof View_Edge) {
			return null;
		}

		return null;
	}

	/**
	*
	*/
	public void setQueryPopItems(QueryNode selected) {
		// check to see if all nodes needed for this node to be updatable are
		// present
		boolean updateable = selected.isUpdateable();
		if (!updateable) {
			_ViewAddPopItem.setToolTipText("Queried entity node is not updatable");
			_ViewQueryPopItem.setEnabled(true);
			return;
		} 
			for (EntityNode en : selected.getQueriedEntity().getDepend()) {
				// System.out.println("Does view contain " + en.getName() +
				// "?");
				updateable = updateable && selected.getMModel().getEntityNodePairs().containsKey(en);
				// System.out.println(selected.getMModel().getEntityNodePairs().containsKey(en));
			}
			if (!updateable) {
				// was originally updatable but doesnt have require entitites
				// don't allow insert
				_ViewAddPopItem.setToolTipText(selected.getName() + " needs " + selected.getQueriedEntity().getDepend().toString() + " to be in view to be updatable.");
				_ViewQueryPopItem.setEnabled(true);
				_ViewUpdatePopItem.setEnabled(true);
				_ViewDeletePopItem.setEnabled(true);
				return;
			} 
				// everything allowed
				_ViewQueryPopItem.setEnabled(true);
				_ViewUpdatePopItem.setEnabled(true);
				_ViewDeletePopItem.setEnabled(true);
				_ViewAddPopItem.setEnabled(true);
				return;
			
		
	}

	/**
	 * Builds the menu
	 */
	private void buildMenu() {
		JMenuBar mainMenu;
		@SuppressWarnings("unused")
		JMenu menuFile, menuAction, menuHelp;

		mainMenu = new JMenuBar();

		// Make the File Menu
		menuFile = new JMenu("File");

		final JMenu exportMenu = new JMenu("Export to");

		menuFile.add(exportMenu);
		menuFile.addSeparator();

		addMenuItem(exportMenu, new JMenuItem(new ExportImageAction<>(this)), null);

		mainMenu.add(menuFile);
		menuFile.setMnemonic(KeyEvent.VK_F);
		addMenuItem(menuFile, new JMenuItem(new FileClearViewAction(this)), null);
		menuFile.addSeparator();
		addMenuItem(menuFile, new JMenuItem(new DocumentInfoAction(this)), KeyEvent.VK_I);
		menuFile.addSeparator();
		addMenuItem(menuFile, new JMenuItem(new FileQuitAction(this)), KeyEvent.VK_W);

		// Create help menu
		menuHelp = new JMenu("Help");

		mainMenu.add(menuHelp);
		menuHelp.setMnemonic(KeyEvent.VK_H);
		menuHelp.add(new HelpAction());
		menuHelp.add(new AboutAction());

		setJMenuBar(mainMenu);
	}

	/**
	 * Assigns then SketchNode representing this frame in the overview
	 *
	 * @param inNode
	 */
	public void assignNode(ViewNode inNode) {
		_myNode = inNode;
	}

	/**
	 * Returns the SketchNode representing this frame in the overview
	 *
	 * @return
	 */
	@Override
	public ViewNode getNode() {
		return _myNode;
	}

	/**
	 * Called by view when the view is set as dirty (or clean). We simply set
	 * the windowModified property for OS-specific window changes (for example,
	 * the "modified" dot in OS X
	 *
	 * @param dirty
	 */
	public void setDirty(final boolean dirty) {
		getRootPane().putClientProperty("windowModified", dirty ? Boolean.TRUE : Boolean.FALSE);
	}

	/**
	 * Sets the title of this frame to "EASIK - <view name>"
	 */
	public void setEasikTitle() {
		this.setTitle("EASIK - " + _ourView.getName());
	}

	@Override
	public void enableAddConstraintItems(boolean state) {
		// does nothing because views don't addConstraints yet (or maybe ever)
	}

	@Override
	public boolean getShowAttsVal() {
		return true;
	}
}
