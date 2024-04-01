package easik.ui.datamanip;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import easik.database.types.EasikType;
import easik.database.types.Int;
import easik.sketch.vertex.EntityNode;
import easik.ui.JUtils;
import easik.ui.Option;
import easik.ui.OptionsDialog;
import easik.ui.datamanip.jdbc.DatabaseUtil;

/**
 * Sets up a dialog for the user to enter data. The display will include a label
 * indicating each field's corresponding column, and the datatype expected for
 * the entry. Default values for fields can be included in the form of a map
 * from column name to the default text can be included.
 */
public class RowEntryDialog extends OptionsDialog {
  /**
   *    
   */
  private static final long serialVersionUID = 5354460104832678713L;

  /**
   * A map of column names to their correspoinding data type
   */
  private HashMap<String, EasikType> attsToTypes;

  /**
   * Map of default column data for text fields, indexed by column name
   */
  private HashMap<String, String> defaults;

  /**
   * Set of DialogRow objects which are used by the verify method to grab input
   */
  private Collection<DialogRow> dialogRows;

  /**
   * A map foreign key name to a JTable holding the data of the table to which the
   * foreign key points
   */
  private LinkedHashMap<String, EntityNode> foreignKeys;

  /**
   * Input from the user in the form of a ColumnEntry object set
   */
  private Set<ColumnEntry> input;

  /**
   *
   *
   * @param parent
   * @param title
   * @param inAttToType
   * @param fKeys
   */
  public RowEntryDialog(final JFrame parent, final String title, final Map<String, EasikType> inAttToType,
      final Map<String, EntityNode> fKeys) {
    this(parent, title, inAttToType, fKeys, new HashMap<String, String>(10));
  }

  /**
   * Sets up dialog for inserting data into a table
   *
   * @param parent      The parent frame of the modal dialog
   * @param title       a title
   * @param inAttToType A map of attribute (column) names, to the type of input
   *                    they expect
   * @param fKeys       A map of column names which represent foreign keys to the
   *                    EntityNode to which it points
   * @param defs        Default values for entry fields in the form of a map from
   *                    column name to default text
   */
  public RowEntryDialog(final JFrame parent, final String title, final Map<String, EasikType> inAttToType,
      final Map<String, EntityNode> fKeys, final Map<String, String> defs) {
    super(parent, title);

    attsToTypes = new HashMap<>(inAttToType);
    foreignKeys = new LinkedHashMap<>(fKeys);
    defaults = new HashMap<>(defs);
    dialogRows = new HashSet<>(10);

    // Database dbd = inDBD;
    // JFrame _parent = parent;
    setSize(465, 400);
    this.showDialog();
  }

  /**
   * Called when the user clicks the OK button. If the input for a given field is
   * deemed valid, it is added to the input map. If any input is invalid. The
   * whole input map is set to null and the method returns false.
   *
   * @return true if all inputs check out, false otherwise.
   */
  @Override
  public boolean verify() {
    input = new HashSet<>(10);

    JComponent inputComponent;
    String fieldInput;
    String column;

    for (final DialogRow row : dialogRows) {
      column = row.columnName;
      inputComponent = row.input;

      // verify "special" types - we selected value from a popup, so know
      // it to be valid
      // If this column is a foreign key, picked from table -> Assume
      // valid and is an INT
      if (foreignKeys.containsKey(column)) {
        fieldInput = ((JTextField) inputComponent).getText();

        input.add(new ColumnEntry(column, fieldInput, new Int()));
      }
      /*
       * else if (inputComponent instanceof JDateChooser) { // grab input from date
       * chooser - null -> bad input final String date = Long.toString(((JDateChooser)
       * inputComponent).getDate().getTime());
       * 
       * if (date == null) { // bad input JOptionPane.showMessageDialog(this,
       * "<html>Invalid date selected for <b>" + column + "</b>", column +
       * ": Invalid input", JOptionPane.ERROR_MESSAGE);
       * 
       * input = null;
       * 
       * return false; }
       * 
       * input.add(new ColumnEntry(column, date, row.type)); }
       */
      else if (inputComponent instanceof TimeChooser) {
        final TimeChooser t = (TimeChooser) inputComponent;

        input.add(new ColumnEntry(column, Long.toString(t.getMillis()), row.type));
      }
      /*
       * else if (inputComponent instanceof DateTimeChooser) { final DateTimeChooser
       * dtc = (DateTimeChooser) inputComponent; final Date date =
       * dtc.dateChooser.getDate();
       * 
       * if (date == null) { // bad input JOptionPane.showMessageDialog(this,
       * "<html>Invalid date/time for <b>" + column + "</b>", column +
       * ": Invalid input", JOptionPane.ERROR_MESSAGE);
       * 
       * input = null;
       * 
       * return false; }
       * 
       * final long dateTime = dtc.getMillis();
       * 
       * input.add(new ColumnEntry(column, Long.toString(dateTime), row.type)); }
       */
      else {
        fieldInput = ((JTextField) inputComponent).getText();

        // Allow "" (treated as NULL), or ask the type expected to
        // validate the input
        if ("".equals(fieldInput) || row.type.verifyInput(fieldInput)) {
          input.add(new ColumnEntry(column, fieldInput, row.type));
        } else { // otherwise we have bad input
          JOptionPane.showMessageDialog(this,
              "<html>Invalid input '" + fieldInput + "' for <b>" + column + "</b> (" + row.type + ')',
              column + ": Invalid input", JOptionPane.ERROR_MESSAGE);

          input = null;

          return false;
        }
      }
    }

    return true;
  }

  /**
   * Returns a map of column names to its input.
   *
   * @return The map of column names to input if verify has benn called and
   *         returned true, otherwise null.
   */
  public Set<ColumnEntry> getInput() {
    return input;
  }

  /**
   *
   *
   * @return
   */
  @Override
  public List<Option> getOptions() {
    final List<Option> options = new LinkedList<>();

    if (attsToTypes.isEmpty() && foreignKeys.isEmpty()) {
      if (getTitle().startsWith("Update")) {
        options.add(new Option(
            "<html><i>(No attributes or outgoing edges)</i><br>Cannot update foreign keys involved in constraint."));
      } else {
        options.add(new Option("<html><i>(No attributes or outgoing edges)</i><br>Press OK to add a new row."));
      }
      setSize(300, 145);

      return options;
    }

    for (final String name : attsToTypes.keySet()) {
      final EasikType type = attsToTypes.get(name);
      final JComponent c;

      // set up special case types where we restrict user input to a
      // selection, and
      // add a corresponding DialogRow to our set of DialogRows
      // if (type instanceof easik.database.types.Date)
      // {
      // c = JUtils.fixHeight(new JDateChooser());
      // }
      // else
      if (type instanceof easik.database.types.Time) {
        c = JUtils.fixHeight(new TimeChooser());
      }
      // else if (type instanceof easik.database.types.Timestamp)
      // {
      // c = JUtils.fixHeight(new DateTimeChooser());
      // }

      // else allow any input
      else {
        c = JUtils.textField("");

        c.setToolTipText(type.toString());

        if (defaults.keySet().contains(name)) {
          ((JTextField) c).setText(defaults.get(name));
        }
      }

      options.add(new Option("<html><b>" + name + "</b>", c));
      dialogRows.add(new DialogRow(c, name, type));
    }

    for (final String fkName : foreignKeys.keySet()) {
      // Grab target table name for "type"
      final String targetType = foreignKeys.get(fkName).getName();
      final JTextField idDisplay = JUtils.textField(5);

      if (defaults.keySet().contains(fkName)) {
        idDisplay.setText(defaults.get(fkName));
      }

      idDisplay.setEditable(false);

      final JButton b = new JButton("Choose " + targetType);

      b.addActionListener(new ForeignKeyListener(foreignKeys.get(fkName), idDisplay));
      options.add(new Option("<html><b>" + fkName + "</b>", idDisplay, b));
      dialogRows.add(new DialogRow(idDisplay, fkName, new Int()));
    }

    if (options.size() < 5) {
      setSize(465, 275);
    }

    return options;
  }

  /**
   * JComponent for entering a date and time. Constains JSpinners for hour:min:sec
   * and a JDateChooser.
   */
  /*
   * private class DateTimeChooser extends JPanel { private static final long
   * serialVersionUID = -385103577352757708L;
   * 
   * // private JDateChooser dateChooser;
   * 
   * private TimeChooser timeChooser;
   * 
   * private DateTimeChooser() { this.setLayout(new GridLayout(2, 1));
   * 
   * dateChooser = new JDateChooser(); timeChooser = new TimeChooser();
   * 
   * this.add(dateChooser); this.add(timeChooser); JUtils.borderless(this); }
   * 
   * private long getMillis() { return dateChooser.getDate(timeChooser).getTime();
   * } }
   */

  /**
   * Bundle for row in dialog.
   */
  private class DialogRow {
    /**  */
    private String columnName;

    /**  */
    @SuppressWarnings("hiding")
    private JComponent input;

    /**  */
    private EasikType type;

    /**
     *
     *
     * @param in
     * @param cn
     * @param t
     */
    private DialogRow(final JComponent in, final String cn, final EasikType t) {
      input = in;
      columnName = cn;
      type = t;
    }
  }

  /**
   * Listens on the foreign key-selecting button and opens a singleton
   * record-selection dialog. The primary ID of the selected record is then set in
   * the _display text field for the user.
   */
  private class ForeignKeyListener implements ActionListener {
    /**  */
    private JTextField _display;

    /**  */
    private EntityNode _table;

    /**
     *
     *
     * @param inTable
     * @param display
     */
    public ForeignKeyListener(final EntityNode inTable, final JTextField display) {
      _table = inTable;
      _display = display;
    }

    // Fired when the user clicks on a button in a field containing a
    // foreign key

    /**
     *
     *
     * @param e
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
      final int selected = DatabaseUtil.selectRowPK(RowEntryDialog.this, _table);

      // if 0, there were no valid foreign keys to choose
      if (selected == 0) {
        return;
      }

      _display.setText(Integer.toString(selected));
    }
  }

  /**
   * This is GPL'd,so can't user
   *
   * @version 12/09/12
   * @author Christian Fiddick
   */

  // private class JDateChooser extends com.toedter.calendar.JDateChooser
  // {
  // private static final long serialVersionUID = 118510522229212099L;
  //
  // /**
  // * Returns the selected date, with time components set to the values
  // selected in the TimeChooser object
  // *
  // * @param tc The TimeChooser object who's time fields are used to set
  // times in our date
  // * @return The Date object selected with hours, minutes, seconds and
  // milliseconds set to reflect our TimeChooser
  // */
  // public Date getDate(final TimeChooser tc)
  // {
  // final Date date = super.getDate();
  // final Calendar c = Calendar.getInstance();
  //
  // c.clear();
  // c.setTime(date);
  // c.set(Calendar.HOUR_OF_DAY, (Integer) tc.h.getValue());
  // c.set(Calendar.MINUTE, (Integer) tc.m.getValue());
  // c.set(Calendar.SECOND, (Integer) tc.s.getValue());
  // c.set(Calendar.MILLISECOND, (Integer) tc.ms.getValue());
  //
  // return c.getTime();
  // }
  //
  // /**
  // * Returns the date selected, with time fields set to midnight
  // *
  // * @return
  // */
  // public Date getDate()
  // {
  // return getDate(new TimeChooser());
  // }
  // }

  /**
   * JPanel for entering time via JSpinners.
   */
  private class TimeChooser extends JPanel {
    /**
     *        
     */
    private static final long serialVersionUID = 1974068320030470941L;

    /**  */
    private JSpinner h, m, s, ms;

    /**
     * Default constructor. Initializes all spinners to 0.
     */
    private TimeChooser() {
      this(0, 0, 0, 0);
    }

    /**
     * Sets up time chooser with default values for the spinners. Bad defaults get
     * set to 0.
     *
     * @param hr   hour
     * @param min  minute
     * @param sec  second
     * @param msec msec
     */
    private TimeChooser(final int hr, final int min, final int sec, final int msec) {
      final int hour = ((hr > 23) || (hr < 0)) ? 0 : hr;
      final int minute = ((min > 59) || (min < 0)) ? 0 : min;
      final int second = ((sec > 59) || (sec < 0)) ? 0 : sec;
      final int msecond = ((msec > 999) || (msec < 0)) ? 0 : msec;

      setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));

      h = new JSpinner(new SpinnerNumberModel(hour, 0, 23, 1));

      this.add(h);
      this.add(new JLabel(":"));

      m = new JSpinner(new SpinnerNumberModel(minute, 0, 59, 1));

      this.add(m);
      this.add(new JLabel(":"));

      s = new JSpinner(new SpinnerNumberModel(second, 0, 59, 1));

      this.add(s);
      this.add(new JLabel("."));

      ms = new JSpinner(new SpinnerNumberModel(msecond, 0, 999, 1));

      this.add(ms);
      h.setToolTipText("h");
      m.setToolTipText("m");
      s.setToolTipText("s");
      ms.setToolTipText("ms");
    }

    /**
     * Returns the miliseconds represented by this time chooser, corrected via
     * Java's Calendar object so that when sent to an SQL server, the value
     * interpreted is the same as the user sees on the JSpinners
     *
     * @return long msecions
     */
    private long getMillis() {
      final Calendar c = Calendar.getInstance();

      c.clear();
      c.set(Calendar.HOUR_OF_DAY, (Integer) h.getValue());
      c.set(Calendar.MINUTE, (Integer) m.getValue());
      c.set(Calendar.SECOND, (Integer) s.getValue());
      c.set(Calendar.MILLISECOND, (Integer) ms.getValue());

      return c.getTimeInMillis();
    }
  }
}
