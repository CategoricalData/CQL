package easik.model.keys;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import easik.graph.EasikGraphModel;
import easik.model.Model;
import easik.model.attribute.EntityAttribute;
import easik.model.edge.ModelEdge;
import easik.model.ui.ModelFrame;
import easik.model.vertex.ModelVertex;
import easik.ui.JUtils;
import easik.ui.Option;
import easik.ui.OptionsDialog;

/**
 * Displays a UI for creating/editing a unique key
 */
public class UniqueKeyUI<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>>
    extends OptionsDialog {
  /**
   *    
   */
  private static final long serialVersionUID = -7553906668723261457L;

  /** The width and height of the dialog box */
  @SuppressWarnings("hiding")
  private static final int WIDTH = 350, HEIGHT = 450;

  /**
   * The JList of attributes and outgoing edges
   */
  @SuppressWarnings("rawtypes")
  private JList _attListField, _edgeListField;

  /**
   * The current entity node
   */
  private N _entity;

  /**
   * The JTextField used to store the key name
   */
  private JTextField _keyNameField;

  /**
   * The unique key being edited in this UI
   */
  private UniqueKey<F, GM, M, N, E> _uniqueKey;

  /**
   * Creates a dialog box prompting the user for input. Used for creation of new
   * unique key.
   *
   * @param frame    The frame to attach this option dialog to
   * @param inEntity The Entity node for which the user is creating a unique key
   */
  public UniqueKeyUI(JFrame frame, N inEntity) {
    super(frame, "New Unique Key");

    setSize(WIDTH, HEIGHT);

    _entity = inEntity;
  }

  /**
   * Creates a dialog box prompting the user for input. Used for editing of
   * existing unique key.
   *
   * @param frame    The frame to attach this option dialog to
   * @param inEntity The N for which the unique key is being built
   * @param inKey    The unique key being edited
   */
  public UniqueKeyUI(JFrame frame, N inEntity, UniqueKey<F, GM, M, N, E> inKey) {
    super(frame, "Edit Unique Key");

    this.setSize(WIDTH, HEIGHT);

    _entity = inEntity;
    _uniqueKey = inKey;
  }

  /**
   * Generates the options/controls to prompt the user for unique key details
   *
   * @return
   */
  @Override
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public List<Option> getOptions() {
    LinkedList<Option> opts = new LinkedList<>();

    opts.add(new Option("Unique key name:", _keyNameField = JUtils.textField("")));

    if (_uniqueKey != null) {
      _keyNameField.setText(_uniqueKey.getKeyName());
    }

    // Add the attributes box (only if there are attributes)
    EntityAttribute[] atts = _entity.getEntityAttributes().toArray(new EntityAttribute[0]);

    _attListField = new JList(atts);

    if (atts.length > 0) {
      opts.add(new Option("Unique key attributes:", JUtils.fixHeight(new JScrollPane(_attListField))));
    }

    // Add the outgoing edges box (only if there are outgoing edges)
    UniqueIndexable[] edges = _entity.getIndexableEdges().toArray(new UniqueIndexable[0]);

    _edgeListField = new JList(edges);

    if (edges.length > 0) {
      opts.add(new Option("Unique key edges:", JUtils.fixHeight(new JScrollPane(_edgeListField))));
    }

    if (_uniqueKey != null) {
      Set<UniqueIndexable> elems = _uniqueKey.getElements();
      ArrayList<Integer> setAtt = new ArrayList<>();
      ArrayList<Integer> setEdges = new ArrayList<>();

      for (int i = 0; i < atts.length; i++) {
        if (elems.contains(atts[i])) {
          setAtt.add(i);
        }
      }

      for (int i = 0; i < edges.length; i++) {
        if (elems.contains(edges[i])) {
          setEdges.add(i);
        }
      }

      int[] setA = new int[setAtt.size()];
      int[] setE = new int[setEdges.size()];

      for (int i = 0; i < setAtt.size(); i++) {
        setA[i] = setAtt.get(i);
      }

      for (int i = 0; i < setEdges.size(); i++) {
        setE[i] = setEdges.get(i);
      }

      _attListField.setSelectedIndices(setA);
      _edgeListField.setSelectedIndices(setE);
    }

    return opts;
  }

  /**
   *
   *
   * @return
   */
  @Override
  public boolean verify() {
    boolean showError = false;
    String errorTitle = "", errorMessage = "";
    Set<UniqueIndexable> selected = getSelectedElements();

    // No name
    if (getKeyName().equals("")) {
      showError = true;
      errorTitle = "No key name";
      errorMessage = "No key name entered.\nPlease enter a name for this unique key.";

      _keyNameField.requestFocusInWindow();
    }

    // No attributes selected
    else if (selected.isEmpty()) {
      showError = true;
      errorTitle = "No attributes/edges selected";
      errorMessage = "One or more attributes or edges must be selected.\n"
          + "Please select the attributes and/or edges to be\n" + "added to the unique key.";

      _attListField.requestFocusInWindow();
    }

    // Key name already in use:
    else if (((_uniqueKey == null) || !_uniqueKey.getKeyName().equals(getKeyName()))
        && _entity.isKeyNameUsed(getKeyName())) {
      showError = true;
      errorTitle = "Key name already exists";
      errorMessage = "A unique key with that key name already exists.\n\nPlease choose another name.";

      _keyNameField.requestFocusInWindow();
    }

    // Set of attributes is not a unique set:
    else if (((_uniqueKey == null) || !_uniqueKey.getElements().equals(selected))
        && (_entity.uniqueKeyOn(selected) != null)) {
      showError = true;
      errorTitle = "Unique key already exists";
      errorMessage = "A unique key with those attributes/edges already exists.\n"
          + "You must change your attribute selection or remove the\n"
          + "existing unique key before continuing.";

      _attListField.requestFocusInWindow();
    }

    if (showError) {
      JOptionPane.showMessageDialog(this, errorMessage, errorTitle, JOptionPane.ERROR_MESSAGE);

      return false;
    }

    return true;
  }

  /**
   * Returns a list of the EntityAttribute objects that make up the key
   *
   * @return ordered Set of the entity attributes in the key
   */
  public Set<UniqueIndexable> getSelectedElements() {
    LinkedHashSet<UniqueIndexable> selected = new LinkedHashSet<>();

    for (Object o : _attListField.getSelectedValuesList()) {
      selected.add((UniqueIndexable) o);
    }

    for (Object o : _edgeListField.getSelectedValuesList()) {
      selected.add((UniqueIndexable) o);
    }

    return selected;
  }

  /**
   * Returns the name of the key
   *
   * @return The name of the key
   */
  public String getKeyName() {
    return _keyNameField.getText().trim();
  }
}
