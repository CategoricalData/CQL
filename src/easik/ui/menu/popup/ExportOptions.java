package easik.ui.menu.popup;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import easik.ui.JUtils;
import easik.ui.Option;
import easik.ui.OptionsDialog;
import easik.ui.SketchFrame;

/**
 *
 *
 * @version 12/09/12
 * @author Christian Fiddick
 */
public class ExportOptions extends OptionsDialog {
  /*
   * Various JThings containing entered information.
   */

  /**
   *    
   */
  private static final long serialVersionUID = 1962177469065625930L;

  /**  */
  private JCheckBox _createDatabase, _dropDatabase, _dropSchema, _bigKeys;

  /**  */
  private String _dbType;

  /**  */
  @SuppressWarnings("unused")
  private SketchFrame _theFrame;

  /**
   * Creates and displays a new modal db options dialog.
   *
   * @param type        the SQL driver type, such as "MySQL" or "PostgreSQL"
   * @param sketchFrame the SketchFrame to attach this modal dialog box to
   */
  public ExportOptions(final String type, final SketchFrame sketchFrame) {
    super(sketchFrame, "Database parameters");

    _theFrame = sketchFrame;
    _dbType = type;

    setSize(425, 350);
    showDialog();
  }

  /**
   * Gets db connection options. If the Sketch associated with the SketchFrame
   * already has a driver, we use it for the defaults.
   *
   * @return
   */
  @Override
  public List<Option> getOptions() {
    final List<Option> opts = new LinkedList<>();
    final JPanel dbopts = new JPanel();

    dbopts.setLayout(new BoxLayout(dbopts, BoxLayout.Y_AXIS));

    // FIXME -- we should add some help (mouseover? help button?) to
    // describe these in more detail
    dbopts.add(_createDatabase = new JCheckBox("Create db"));
    dbopts.add(_dropDatabase = new JCheckBox("Drop db before creating"));
    _dropDatabase.setEnabled(_createDatabase.isSelected());
    _createDatabase.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        _dropDatabase.setEnabled(_createDatabase.isSelected());
      }
    });

    if ("PostgreSQL".equals(_dbType)) {
      dbopts.add(_dropSchema = new JCheckBox("Drop and recreate schema"));
    }

    dbopts.add(_bigKeys = new JCheckBox("Use BIGINTs instead of INTs for keys"));
    _bigKeys.setToolTipText(JUtils.tooltip(
        "Enabling this option will cause all primary and foreign key columns to be BIGINTs instead of INTEGERs.  Enable this if you anticipate more than 2 billion row insertions over the lifetime of the table"));
    opts.add(new Option(new JLabel("Creation options"), dbopts));

    return opts;
  }

  /**
   *
   *
   * @return
   */
  public Map<String, Object> getParams() {
    final Map<String, Object> options = new HashMap<>(5);

    if (_createDatabase.isSelected()) {
      options.put("createDatabase", "true");

      if (_dropDatabase.isSelected()) {
        options.put("dropDatabase", "true");
      }
    }

    if ((_dropSchema != null) && _dropSchema.isSelected()) {
      options.put("dropSchema", "true");
    }

    if (_bigKeys.isSelected()) {
      options.put("bigKeys", "true");
    }

    return options;
  }
}
