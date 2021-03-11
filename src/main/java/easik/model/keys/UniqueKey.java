package easik.model.keys;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.undo.AbstractUndoableEdit;

import easik.graph.EasikGraphModel;
import easik.model.Model;
import easik.model.edge.ModelEdge;
import easik.model.ui.ModelFrame;
import easik.model.vertex.ModelVertex;

/**
 * Class used to keep track of attributes used in unique key constraints.
 *
 * @author Kevin Green 2006
 * @since 2006-06-06 Kevin Green
 * @version 2006-07-13 Kevin Green
 */
public class UniqueKey<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>>
    extends DefaultMutableTreeNode {
  /**
   *    
   */
  private static final long serialVersionUID = 6169847511627970455L;

  /**
   * The attributes contributing to this unique key
   */
  private LinkedHashSet<UniqueIndexable> _elements;

  /**
   * The name of this key
   */
  private String _keyName;

  /**
   * The entity node to which this key applies
   */
  private N _parent;

  /**
   * Default constructor
   * 
   * @param modelVertex The EntityNode to which this unique key applies
   * @param elements    A List of the indexable objects (implementing
   *                    UniqueIndexable) used in the key
   * @param name        The name of the key
   */
  @SuppressWarnings("unchecked")
  public UniqueKey(final ModelVertex<F, GM, M, N, E> modelVertex, final Collection<UniqueIndexable> elements,
      final String name) {
    super();

    _parent = (N) modelVertex;
    _elements = new LinkedHashSet<>(elements);
    _keyName = name;
  }

  /**
   * Overwriten method returns attribute list separated by semicolons
   *
   * @return The name of this unique key formatted for use on the information tree
   */
  @Override
  public String toString() {
    String id = getId();

    id = _keyName + " (" + id + ')';

    return id;
  }

  /**
   * Gets the id of this unique key. The id is formatted by getting the name of
   * each attribute and placing a comma between each name
   * 
   * @return The id of this attribute
   */
  public String getId() {
    final StringBuilder id = new StringBuilder(50);

    for (final UniqueIndexable elem : _elements) {
      id.append(elem.getName()).append(',');
    }

    return id.substring(0, id.length() - 1);
  }

  /**
   * Returns the set of unique-indexable elements (attributes and edges) that make
   * up this key. The set is backed by a LinkedHashSet, and so is ordered.
   *
   * @return a Set of the UniqueIndexable objects (attributes/edges) used in the
   *         key
   */
  public Set<UniqueIndexable> getElements() {
    return Collections.unmodifiableSet(_elements);
  }

  /**
   * Sets the elements involved in this unique key to the unique-indexable
   * attributes/edges contained in the passed-in collection (set, list, etc.).
   * Note that this collection will be added to a set, so duplicate
   * EntityAttribute values will be ignored. Also note that the order of the
   * attributes will be preserved.
   *
   * @param elems Collection of UniqueIndexable-implementing objects
   */
  public void setElements(final Collection<UniqueIndexable> elems) {
    final LinkedHashSet<UniqueIndexable> oldElements = _elements;

    _elements = new LinkedHashSet<>(elems);

    final LinkedHashSet<UniqueIndexable> newElements = _elements;

    _parent.getMModel().getGraphModel().postEdit(new AbstractUndoableEdit() {
      /**
       *            
       */
      private static final long serialVersionUID = -6780658287050089538L;

      @Override
      public void undo() {
        super.undo();

        _elements = oldElements;
      }

      @Override
      public void redo() {
        super.redo();

        _elements = newElements;
      }
    });
  }

  /**
   * Returns a UniqueIndexable element from the element list, if it contains it.
   * Note that this might make the unique key invalid (0 elements) or a duplicate
   * of another key: you must take care to call UniqueKey.cleanup(node) after
   * finished removing all elements from the node to fix up the element list.
   *
   * @param elem the UniqueIndexable-implementing object to remove
   * @return true if the element existed and was removed, false if the element did
   *         not exist
   */
  public boolean removeElement(final UniqueIndexable elem) {
    final boolean ret = _elements.remove(elem);

    if (ret) {
      _parent.getMModel().getGraphModel().postEdit(new AbstractUndoableEdit() {
        /**
         *                
         */
        private static final long serialVersionUID = 8689047282879320151L;

        @Override
        public void undo() {
          super.undo();
          _elements.add(elem);
        }

        @Override
        public void redo() {
          super.redo();
          _elements.remove(elem);
        }
      });
    }

    return ret;
  }

  /**
   * Returns the name of the key
   *
   * @return The name of the key
   */
  public String getKeyName() {
    return _keyName;
  }

  /**
   * Sets the key name to the name described in <it>inName</it>
   * 
   * @param inName The name of this attribute
   */
  public void setKeyName(final String inName) {
    final String oldName = _keyName;

    _keyName = inName;

    _parent.getMModel().getGraphModel().postEdit(new AbstractUndoableEdit() {
      /**
       *            
       */
      private static final long serialVersionUID = 3728325633310707957L;

      @Override
      public void undo() {
        super.undo();

        _keyName = oldName;
      }

      @Override
      public void redo() {
        super.redo();

        _keyName = inName;
      }
    });
  }

  /**
   * Returns the parent entity for the key
   *
   * @return The parent entity for the key
   */
  public N getEntity() {
    return _parent;
  }

}
