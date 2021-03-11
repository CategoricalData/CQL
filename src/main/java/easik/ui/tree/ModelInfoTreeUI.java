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
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.undo.AbstractUndoableEdit;

import easik.graph.EasikGraphModel;
import easik.model.Model;
import easik.model.attribute.EntityAttribute;
import easik.model.constraint.CommutativeDiagram;
import easik.model.constraint.EqualizerConstraint;
import easik.model.constraint.LimitConstraint;
import easik.model.constraint.ModelConstraint;
import easik.model.constraint.ProductConstraint;
import easik.model.constraint.PullbackConstraint;
import easik.model.constraint.SumConstraint;
import easik.model.edge.ModelEdge;
import easik.model.keys.UniqueKey;
import easik.model.path.ModelPath;
import easik.model.ui.ModelFrame;
import easik.model.vertex.ModelVertex;
import easik.sketch.Sketch;
import easik.ui.menu.AddCommutativeDiagramAction;
//import easik.ui.menu.AddLimitConstraintAction;
import easik.ui.menu.AddProductConstraintAction;
import easik.ui.menu.AddPullbackConstraintAction;
import easik.ui.menu.AddSumConstraintAction;
import easik.ui.menu.popup.NewEntityAction;
import easik.ui.tree.popup.AddAttributeAction;
import easik.ui.tree.popup.AddPathAction;
import easik.ui.tree.popup.AddUniqueKeyAction;
import easik.ui.tree.popup.DeleteAttributeAction;
import easik.ui.tree.popup.DeleteConstraintAction;
import easik.ui.tree.popup.DeleteEntityAction;
import easik.ui.tree.popup.DeletePathAction;
import easik.ui.tree.popup.DeleteUniqueKeyAction;
import easik.ui.tree.popup.EditAttributeAction;
import easik.ui.tree.popup.EditUniqueKeyAction;
import easik.ui.tree.popup.RenameEntityAction;

/**
 * Class to display the information tree. This tree contains information about
 * entities, attributes and constraints.
 *
 * @author Kevin Green 2006
 * @author Vera Ranieri 2006
 * @version 2006-08-02 Kevin Green
 */
public class ModelInfoTreeUI<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>>
    extends JScrollPane {
  private static final long serialVersionUID = -5090284794437931334L;

  /** Tree popup menu item */
  private JMenuItem _addAttributeItem;

  /** Tree popup menu item */
  private JMenuItem _addCommutativeItem;

  /** Tree popup menu item */
  private JMenuItem _addEntityItem;

  /** Tree popup menu item */
  private JMenuItem _addEqualizerItem;

  /** Tree popup menu item */
  // private JMenuItem _addLimItem;

  /** Tree popup menu item */
  private JMenuItem _addPathItem;

  /** Tree popup menu item */
  private JMenuItem _addProductItem;

  /** Tree popup menu item */
  private JMenuItem _addPullbackItem;

  /** Tree popup menu item */
  private JMenuItem _addSumItem;

  /** Tree popup menu item */
  private JMenuItem _addUniqueKeyItem;

  /** Tree popup menu item */
  private JMenuItem _deleteAttributeItem;

  /** Tree popup menu item */
  private JMenuItem _deleteConstraintItem;

  /** Tree popup menu item */
  private JMenuItem _deleteEntityItem;

  /** Tree popup menu item */
  private JMenuItem _deletePathItem;

  /** Tree popup menu item */
  private JMenuItem _deleteUniqueKeyItem;

  /** Tree popup menu item */
  private JMenuItem _editAttributeItem;

  /** Tree popup menu item */
  private JMenuItem _editUniqueKeyItem;

  /** The information tree */
  private JTree _infoTree;

  /** The information tree model */
  private DefaultTreeModel _infoTreeModel;

  /** The position to place new nodes */
  private Point _newPosition;

  /** The popup menu for the tree */
  private JPopupMenu _popupMenu;

  /** Tree popup menu item */
  private JMenuItem _renameEntityItem;

  /** The frame in which this tree exists */
  private F _theFrame;

  /** The top node of the tree */
  private DefaultMutableTreeNode _topNode;

  /** The constraints tree node */
  private DefaultMutableTreeNode _tree_constraints;

  /** The commutative diagram tree node */
  private DefaultMutableTreeNode _tree_constraints_commutative;

  /** The equalizer constraint tree node */
  private DefaultMutableTreeNode _tree_constraints_equalizer;

  /** The triangle constraint tree node */
  private DefaultMutableTreeNode _tree_constraints_limit;

  /** The product constraint tree node */
  private DefaultMutableTreeNode _tree_constraints_product;

  /** The pullback constraint tree node */
  private DefaultMutableTreeNode _tree_constraints_pullback;

  /** The sum constraint tree node */
  private DefaultMutableTreeNode _tree_constraints_sum;

  /** The entities tree node */
  private DefaultMutableTreeNode _tree_entities;

  /**
   * An ArrayList of Strings which represent an expansion state of the info tree.
   * The tree can then be reverted back to the state as defined in expanState.
   */
  private ArrayList<String> expanState;

  /**
   * Default constructor
   *
   * @param inFrame
   */
  public ModelInfoTreeUI(F inFrame) {
    // Create the top node.
    _topNode = new DefaultMutableTreeNode("EA Sketch");
    _theFrame = inFrame;
    _newPosition = new Point(50, 50);

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

    buildPopupMenu();

    // Create Category Heads
    _tree_entities = new DefaultMutableTreeNode("Entities");

    _topNode.add(_tree_entities);

    _tree_constraints = new DefaultMutableTreeNode("Constraints");

    _topNode.add(_tree_constraints);

    _tree_constraints_commutative = new DefaultMutableTreeNode("Commutative Diagrams");

    _tree_constraints.add(_tree_constraints_commutative);

    _tree_constraints_product = new DefaultMutableTreeNode("Product Constraints");

    _tree_constraints.add(_tree_constraints_product);

    _tree_constraints_pullback = new DefaultMutableTreeNode("Pullback Constraints");

    _tree_constraints.add(_tree_constraints_pullback);

    _tree_constraints_equalizer = new DefaultMutableTreeNode("Equalizer Constraints");

    _tree_constraints.add(_tree_constraints_equalizer);

    _tree_constraints_sum = new DefaultMutableTreeNode("Sum Constraints");

    _tree_constraints.add(_tree_constraints_sum);

    _tree_constraints_limit = new DefaultMutableTreeNode("Limit Constraints");

    _tree_constraints.add(_tree_constraints_limit);

    // Initialize ArrayList of expansion state information
    expanState = new ArrayList<>();
  }

  /**
   * Creates the popup menu
   */
  private void buildPopupMenu() {
    _popupMenu.add(_addAttributeItem = new JMenuItem(new AddAttributeAction<>(_theFrame)));
    _popupMenu.add(_editAttributeItem = new JMenuItem(new EditAttributeAction<>(_theFrame)));
    _popupMenu.add(_deleteAttributeItem = new JMenuItem(new DeleteAttributeAction<>(_theFrame)));
    _popupMenu.add(_addUniqueKeyItem = new JMenuItem(new AddUniqueKeyAction<>(_theFrame)));
    _popupMenu.add(_editUniqueKeyItem = new JMenuItem(new EditUniqueKeyAction<>(_theFrame)));
    _popupMenu.add(_deleteUniqueKeyItem = new JMenuItem(new DeleteUniqueKeyAction<>(_theFrame)));
    _popupMenu.add(_deleteConstraintItem = new JMenuItem(new DeleteConstraintAction<>(_theFrame)));
    _popupMenu.add(_addEntityItem = new JMenuItem(new NewEntityAction<>(_newPosition, _theFrame)));
    _popupMenu.add(_renameEntityItem = new JMenuItem(new RenameEntityAction<>(_theFrame)));
    _popupMenu.add(_deleteEntityItem = new JMenuItem(new DeleteEntityAction<>(_theFrame)));
    _popupMenu.add(_addCommutativeItem = new JMenuItem(new AddCommutativeDiagramAction<>(_theFrame)));
    _popupMenu.add(_addProductItem = new JMenuItem(new AddProductConstraintAction<>(_theFrame)));
    _popupMenu.add(_addPullbackItem = new JMenuItem(new AddPullbackConstraintAction<>(_theFrame)));
    _popupMenu.add(_addSumItem = new JMenuItem(new AddSumConstraintAction<>(_theFrame)));
    // _popupMenu.add(_addLimItem = new JMenuItem(new
    // AddLimitConstraintAction<F, GM, M, N, E>(_theFrame)));
    _popupMenu.add(_addPathItem = new JMenuItem(new AddPathAction<>(_theFrame)));
    _popupMenu.add(_deletePathItem = new JMenuItem(new DeletePathAction<>(_theFrame)));
    _infoTree.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent evt) {
        mouseReleased(evt);
      }

      @Override
      public void mouseReleased(MouseEvent evt) {
        if (evt.isPopupTrigger()) {
          _infoTree.setSelectionRow(_infoTree.getClosestRowForLocation(evt.getX(), evt.getY()));

          if (setPopMenuItems()) {
            storeExpansion();
            _newPosition.setLocation(_theFrame.getMModel().getNewPosition(10));
            _popupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
          }
        }
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

    // If we're in manipulation mode we do not want to be able to edit the
    // sketch
    else if (_theFrame.getMode() == F.Mode.MANIPULATE) {
      return false;
    }

    // Get currently selected object
    DefaultMutableTreeNode curSelected = (DefaultMutableTreeNode) _theFrame.getInfoTreeUI().getInfoTree()
        .getSelectionPath().getLastPathComponent();

    // Hide all elements
    for (Component c : _popupMenu.getComponents()) {
      c.setVisible(false);
    }

    // Check what is currently selected
    if (curSelected instanceof ModelVertex) {
      _addAttributeItem.setVisible(true);
      _addUniqueKeyItem.setVisible(true);
      _renameEntityItem.setVisible(true);
      _deleteEntityItem.setVisible(true);
    } else if (curSelected instanceof EntityAttribute) {
      if (_theFrame.getMModel() instanceof Sketch) {
        _editAttributeItem.setVisible(true);
        _deleteAttributeItem.setVisible(true);
      }
    } else if (curSelected instanceof UniqueKey) {
      if (_theFrame.getMModel() instanceof Sketch) {
        _editUniqueKeyItem.setVisible(true);
        _deleteUniqueKeyItem.setVisible(true);
      }
    } else if (curSelected instanceof ModelConstraint) {
      if (_theFrame.getMModel() instanceof Sketch) {
        if ((curSelected instanceof SumConstraint) || (curSelected instanceof ProductConstraint)
            || (curSelected instanceof CommutativeDiagram)) {
          _addPathItem.setVisible(true);
        }

        _deleteConstraintItem.setVisible(true);
      }
    } else if (curSelected instanceof ModelPath) {
      Object myConst = curSelected.getParent();

      if ((myConst instanceof SumConstraint) || (myConst instanceof ProductConstraint)
          || (myConst instanceof CommutativeDiagram)) {
        _deletePathItem.setVisible(true);
      }
    } else if (curSelected == _tree_entities) {
      _addEntityItem.setVisible(true);
    } else if (curSelected == _tree_constraints) {
      _addCommutativeItem.setVisible(true);
      _addProductItem.setVisible(true);
      _addPullbackItem.setVisible(true);
      _addSumItem.setVisible(true);
      // _addLimItem.setVisible(true);
    } else if (curSelected == _tree_constraints_commutative) {
      _addCommutativeItem.setVisible(true);
    } else if (curSelected == _tree_constraints_product) {
      _addProductItem.setVisible(true);
    } else if (curSelected == _tree_constraints_pullback) {
      _addPullbackItem.setVisible(true);
    } else if (curSelected == _tree_constraints_equalizer) {
      _addEqualizerItem.setVisible(true);
    } else if (curSelected == _tree_constraints_sum) {
      _addSumItem.setVisible(true);
    } else if (curSelected == _tree_constraints_limit) {
      // _addLimItem.setVisible(true);
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
   * @param inNode The node to be refreshed
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
  public DefaultMutableTreeNode getEntities() {
    return _tree_entities;
  }

  /**
   * Returns the Contraints tree node
   *
   * @return The Contraints tree node
   */
  public DefaultMutableTreeNode getContraints() {
    return _tree_constraints;
  }

  /**
   * Adds an entity and its attributes to the tree
   *
   * @param entity The entity to be added to the tree
   */
  public void addNode(final N entity) {
    EasikGraphModel model = _theFrame.getMModel().getGraphModel();

    model.beginUpdate();
    _addEntity(entity);
    model.postEdit(new AbstractUndoableEdit() {
      private static final long serialVersionUID = -731839601016126887L;

      @Override
      public void undo() {
        super.undo();
        _removeEntity(entity);
      }

      @Override
      public void redo() {
        super.redo();
        _addEntity(entity);
      }
    });
    model.endUpdate();
  }

  /**
   * Internal method to actually add an entity from the tree. Separated because of
   * undo/redo support.
   *
   * @param entity
   */
  private void _addEntity(N entity) {
    _tree_entities.add(entity.getTreeNode());
    refreshTree(_tree_entities);
    _infoTree.scrollPathToVisible(new TreePath(entity.getTreeNode().getPath()));
  }

  /**
   * Removes an entity from the tree. Undoable.
   *
   * @param toRemove Entity to be removed from the tree
   */
  public void removeNode(final N toRemove) {
    EasikGraphModel model = _theFrame.getMModel().getGraphModel();

    model.beginUpdate();
    _removeEntity(toRemove);
    model.postEdit(new AbstractUndoableEdit() {
      private static final long serialVersionUID = 5641595793303538595L;

      @Override
      public void undo() {
        super.undo();
        _addEntity(toRemove);
      }

      @Override
      public void redo() {
        super.redo();
        _removeEntity(toRemove);
      }
    });
    model.endUpdate();
  }

  /**
   * Internal method to actually remove an entity from the tree. Separated because
   * of undo/redo support.
   *
   * @param toRemove
   */
  private void _removeEntity(N toRemove) {
    _tree_entities.remove(toRemove.getTreeNode());
    refreshTree(_tree_entities);
  }

  /**
   * Add a constraint to the info tree. This action will factor into the Sketch's
   * undo/redo history as a single action.
   *
   * @param constraint The constraint to add
   * @since 2006-05-30 Vera Ranieri
   */
  public void addConstraint(final ModelConstraint<F, GM, M, N, E> constraint) {
    EasikGraphModel model = _theFrame.getMModel().getGraphModel();

    model.beginUpdate();
    _addConstraint(constraint);
    model.postEdit(new AbstractUndoableEdit() {
      private static final long serialVersionUID = -6225673569024807131L;

      @Override
      public void undo() {
        super.undo();
        _removeConstraint(constraint);
      }

      @Override
      public void redo() {
        super.redo();
        _addConstraint(constraint);
      }
    });
    model.endUpdate();

    DefaultMutableTreeNode node = constraint.getTreeNode();

    _infoTree.scrollPathToVisible(new TreePath(node.getPath()));
  }

  /**
   * Internal method that actually does the work of adding the constraint to the
   * info tree; this is separate from the public method so that the public method
   * can handle undo/redo controlling.
   *
   * @param constraint
   */
  private void _addConstraint(ModelConstraint<F, GM, M, N, E> constraint) {
    DefaultMutableTreeNode node = constraint.getTreeNode();

    if (constraint instanceof ProductConstraint) {
      _tree_constraints_product.add(node);
    } else if (constraint instanceof SumConstraint) {
      _tree_constraints_sum.add(node);
    } else if (constraint instanceof PullbackConstraint) {
      _tree_constraints_pullback.add(node);
    } else if (constraint instanceof EqualizerConstraint) {
      _tree_constraints_equalizer.add(node);
    } else if (constraint instanceof CommutativeDiagram) {
      _tree_constraints_commutative.add(node);
    } else if (constraint instanceof LimitConstraint) {
      _tree_constraints_limit.add(node);
    }

    // Add the paths to the constraint
    for (ModelPath<F, GM, M, N, E> path : constraint.getPaths()) {
      node.add(path);
    }

    refreshTree((DefaultMutableTreeNode) node.getParent());
  }

  /**
   * Remove a constraint from the info tree. This action will factor into the
   * Sketch's undo/redo history as a single action.
   *
   * @param constraint The constraint to remove
   * @since 2006-05-30 Vera Ranieri
   */
  public void removeConstraint(final ModelConstraint<F, GM, M, N, E> constraint) {
    storeExpansion();

    EasikGraphModel model = _theFrame.getMModel().getGraphModel();

    model.beginUpdate();
    _removeConstraint(constraint);
    model.postEdit(new AbstractUndoableEdit() {
      private static final long serialVersionUID = -6234979876902120545L;

      @Override
      public void undo() {
        super.undo();
        _addConstraint(constraint);
      }

      @Override
      public void redo() {
        super.redo();
        _removeConstraint(constraint);
      }
    });
    model.endUpdate();
    revertExpansion();
  }

  /**
   * Internal method that actually does the work of adding the constraint to the
   * info tree; this is separate from the public method so that the public method
   * can handle undo/redo controlling.
   *
   * @param c
   */
  private void _removeConstraint(ModelConstraint<F, GM, M, N, E> c) {
    DefaultMutableTreeNode constraint = c.getTreeNode();

    if (c instanceof CommutativeDiagram) {
      _tree_constraints_commutative.remove(constraint);
    } else if (c instanceof ProductConstraint) {
      _tree_constraints_product.remove(constraint);
    } else if (c instanceof PullbackConstraint) {
      _tree_constraints_pullback.remove(constraint);
    } else if (c instanceof EqualizerConstraint) {
      _tree_constraints_equalizer.remove(constraint);
    } else if (c instanceof SumConstraint) {
      _tree_constraints_sum.remove(constraint);
    } else if (c instanceof LimitConstraint) {
      _tree_constraints_limit.remove(constraint);
    }

    refreshTree();
  }
}