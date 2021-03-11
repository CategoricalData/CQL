package easik.model.attribute;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import javax.swing.tree.DefaultMutableTreeNode;

import easik.database.types.EasikType;
import easik.graph.EasikGraphModel;
import easik.model.Model;
import easik.model.edge.ModelEdge;
import easik.model.keys.UniqueIndexable;
import easik.model.ui.ModelFrame;
import easik.model.vertex.ModelVertex;

/**
 * This class is used to store information related to entity attributes.
 */
public class EntityAttribute<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>>
    extends DefaultMutableTreeNode implements UniqueIndexable {
  /**  */
  private final static long serialVersionUID = 17;

  /**
   * The data type of the attribute
   */
  private EasikType _dataType;

  /**
   * The entity node this attribute belongs to
   */
  private N _entity;

  /**
   * The name of the attribute
   */
  private String _name;

  /**
   * Default Constructor
   *
   * @param inName     Name of attribute
   * @param inDataType EasikType of attribute
   * @param inEntity   entity
   * @see easik.database.types.EasikType
   */
  public EntityAttribute(final String inName, final EasikType inDataType, final N inEntity) {
    _name = inName;
    _dataType = inDataType;
    _entity = inEntity;
  }

  /**
   *
   *
   * @return
   */
  @Override
  public Object clone() {
    return super.clone();
  }

  /**
   * Overwrite standard toString function. (provides functionality for inserting
   * attributes to the tree)
   * 
   * @return The name of this attribute formatted for display on the tree
   */
  @Override
  public String toString() {
    return _name + " {" + getType() + '}';
  }

  /**
   * Sets the attribute name.
   *
   * @param inName Attribute Name
   */
  public void setName(final String inName) {
    _name = inName;
  }

  /**
   * Returns name of attribute.
   *
   * @return Name of attribute.
   */
  @Override
  public String getName() {
    return _name;
  }

  /**
   * Returns data type of this attribute
   *
   * @return EasikType object
   * @see easik.database.types.EasikType
   */
  public EasikType getType() {
    return _dataType;
  }

  /**
   * Sets the data type of the attribute
   *
   * @param inType The data type of the attribute
   */
  public void setType(final EasikType inType) {
    _dataType = inType;
  }

  /**
   * Returns the entity this attribute is attached to
   * 
   * @return the entity
   */
  public N getEntity() {
    return _entity;
  }
}
