package easik.ui.tree;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

import easik.overview.vertex.SketchNode;
import easik.overview.vertex.ViewNode;
import easik.ui.ApplicationFrame;
import easik.ui.menu.popup.NewSketchAction;
import easik.ui.tree.popup.DeleteFromOverviewFromTreeAction;
import easik.ui.tree.popup.NewViewFromTreeAction;
import easik.ui.tree.popup.RenameInOverviewFromTreeAction;

/**
 * Class to display the information tree. This tree contains information about
 * sketches and views.
 */
public class OverviewInfoTreeUI extends JScrollPane {
	private static final long serialVersionUID = -332100369285945047L;

	/** Tree popup menu items */
	private JMenuItem _addSketchItem;

	/**  */
	private JMenuItem _addViewItem;

	/**  */
	private JMenuItem _deleteSketchItem;

	/**  */
	private JMenuItem _deleteViewItem;

	/** The information tree */
	private JTree _infoTree;

	/** The information tree model */
	private DefaultTreeModel _infoTreeModel;

	/** The randomly generated position to place new nodes */
	private Point _newPosition;

	/** The popup menu for the tree */
	private JPopupMenu _popupMenu;

	/**  */
	private JMenuItem _renameSketchItem;

	/**  */
	private JMenuItem _renameViewItem;

	/** The frame in which this exists */
	private ApplicationFrame _theFrame;

	/** The top node of the tree */
	private DefaultMutableTreeNode _topNode;

	/** The sketches tree node */
	private DefaultMutableTreeNode _tree_sketches;

	/** The views tree node */
	private DefaultMutableTreeNode _tree_views;

	/**
	 * An ArrayList of Strings which represent an expansion state of the info
	 * tree. The tree can then be reverted back to the state as defined in
	 * expanState.
	 */
	private ArrayList<String> expanState;

	/**
	 * Constructs new overview info tree.
	 * 
	 * @param inFrame
	 *            The application frame in which this exists.
	 */
	public OverviewInfoTreeUI(ApplicationFrame inFrame) {
		// Create the top node.
		_topNode = new DefaultMutableTreeNode("EA Overview");
		_theFrame = inFrame;

		// Create a tree that allows one selection at a time.
		_infoTreeModel = new DefaultTreeModel(_topNode);
		_infoTree = new JTree(_infoTreeModel);

		_infoTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		_infoTree.setRootVisible(false); // Hides root node
		_infoTree.setShowsRootHandles(true); // Shows nodes off root

		DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();

		renderer.setOpenIcon(null);
		renderer.setClosedIcon(null);
		renderer.setLeafIcon(null);
		_infoTree.setCellRenderer(renderer);

		// Add tree into scroll pane and then to this pane
		this.setViewportView(_infoTree);
		this.setMinimumSize(new Dimension(150, 200));

		_popupMenu = new JPopupMenu();
		_newPosition = new Point(50, 50);

		buildPopupMenu();

		// Create Category Heads
		_tree_sketches = new DefaultMutableTreeNode("Sketches");

		_topNode.add(_tree_sketches);

		_tree_views = new DefaultMutableTreeNode("Views");

		_topNode.add(_tree_views);

		// Initialize ArrayList of expansion state information
		expanState = new ArrayList<>();
	}

	/**
	 * Creates the popup menu
	 */
	private void buildPopupMenu() {
		_popupMenu.add(_addSketchItem = new JMenuItem(new NewSketchAction(_newPosition, _theFrame.getOverview())));
		_popupMenu.add(_addViewItem = new JMenuItem(new NewViewFromTreeAction(_theFrame.getOverview())));
		_popupMenu.add(_renameSketchItem = new JMenuItem(new RenameInOverviewFromTreeAction(_theFrame, "Rename Sketch")));
		_popupMenu.add(_deleteSketchItem = new JMenuItem(new DeleteFromOverviewFromTreeAction(_theFrame, "Delete Sketch")));
		_popupMenu.add(_renameViewItem = new JMenuItem(new RenameInOverviewFromTreeAction(_theFrame, "Rename View")));
		_popupMenu.add(_deleteViewItem = new JMenuItem(new DeleteFromOverviewFromTreeAction(_theFrame, "Delete View")));
		_infoTree.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent evt) {
				if (evt.isPopupTrigger()) {
					_infoTree.setSelectionRow(_infoTree.getClosestRowForLocation(evt.getX(), evt.getY()));
					_newPosition.setLocation(_theFrame.getOverview().getNewSketchPosition(10));

					if (setPopMenuItems()) {
						_popupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
					}
				}
			}

			@Override
			public void mouseReleased(MouseEvent evt) {
				mousePressed(evt);
			}
		});
	}

	/**
	 * Stores the current expansion state of the info tree.
	 */
	public void storeExpansion() {
		expanState.clear();

		for (int i = 0; i < _infoTree.getRowCount(); i++) {
			expanState.add(TreeExpansionUtil.getExpansionState(_infoTree, i));
		}
	}

	/**
	 * Reverts the expansion state of the info tree to the last stored state.
	 */
	public void revertExpansion() {
		for (int i = 0; i < expanState.size(); i++) {
			TreeExpansionUtil.restoreExpansionState(_infoTree, i, expanState.get(i));
		}
	}

	/**
	 * Sets which of the menu items will be visible
	 *
	 * @return true if the popup should be displayed, false otherwise
	 */
	public boolean setPopMenuItems() {
		// If there is nothing seleceted then just do nothing
		if (_theFrame.getInfoTreeUI().getInfoTree().isSelectionEmpty()) {
			return false;
		}

		// Get currently selected object
		DefaultMutableTreeNode curSelected = (DefaultMutableTreeNode) _theFrame.getInfoTreeUI().getInfoTree().getSelectionPath().getLastPathComponent();

		// Hide all elements
		for (Component c : _popupMenu.getComponents()) {
			c.setVisible(false);
		}

		// Check what is currently selected
		if (curSelected.getUserObject() instanceof SketchNode) {
			_addViewItem.setVisible(true);
			_renameSketchItem.setVisible(true);
			_deleteSketchItem.setVisible(true);
		} else if (curSelected.getUserObject() instanceof ViewNode) {
			_renameViewItem.setVisible(true);
			_deleteViewItem.setVisible(true);
		} else if (curSelected == _tree_sketches) {
			_addSketchItem.setVisible(true);
		} else {
			return false;
		}

		return true;
	}

	/**
	 * Refreshes the tree so updates visualize properly
	 */
	public void refreshTree() {
		storeExpansion();
		_infoTreeModel.reload(_topNode);
		revertExpansion();
	}

	/**
	 * Refreshes the tree so updates visualize properly
	 *
	 * @param inNode
	 *            The node to be refreshed
	 */
	public void refreshTree(DefaultMutableTreeNode inNode) {
		storeExpansion();
		_infoTreeModel.reload(inNode);
		revertExpansion();
	}

	/**
	 * Returns the information tree model
	 *
	 * @return The information tree model
	 */
	public DefaultTreeModel getInfoTreeModel() {
		return _infoTreeModel;
	}

	/**
	 * Returns the information tree
	 *
	 * @return The information tree
	 */
	public JTree getInfoTree() {
		return _infoTree;
	}

	/**
	 * Returns the Entities tree node
	 *
	 * @return The Entities tree node
	 */
	public DefaultMutableTreeNode getSketches() {
		return _tree_sketches;
	}

	/**
	 * Adds an entity and its attributes to the tree
	 *
	 *
	 * @param inSketch
	 */
	public void addSketch(SketchNode inSketch) {
		_tree_sketches.add(inSketch.getTreeNode());
		refreshTree();
	}

	/**
	 * Adds a view to the tree
	 * 
	 * @param inView
	 *            The view to be added to the tree
	 */
	public void addView(ViewNode inView) {
		_tree_views.add(inView.getTreeNode());
		refreshTree();
	}

	/**
	 * Removes an entity from the tree
	 *
	 * @param toRemove
	 *            Entity to be removed from the tree
	 */
	public void removeSketch(SketchNode toRemove) {
		_tree_sketches.remove(toRemove.getTreeNode());
		refreshTree(_tree_sketches);
	}

	/**
	 * Removes a view from the tree
	 * 
	 * @param toRemove
	 *            View to be removed from the tree
	 */
	public void removeView(ViewNode toRemove) {
		_tree_views.remove(toRemove.getTreeNode());
		refreshTree(_tree_views);
	}

	/**
	 * Removes all sketches from the tree
	 */
	public void removeAllSketches() {
		_tree_sketches.removeAllChildren();
		refreshTree(_tree_sketches);
	}

	/**
	 * Removes all views from the tree
	 */
	public void removeAllViews() {
		_tree_views.removeAllChildren();
		refreshTree(_tree_views);
	}
}
