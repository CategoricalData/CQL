package easik.ui.menu.popup;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import easik.ui.JUtils;
import easik.ui.Option;
import easik.ui.OptionsDialog;
import easik.ui.SketchFrame;

/**
 * Action to gather options for export to XML Schema
 * <p/>
 * Shamelessly stolen from {@link easik.ui.menu.popup.ExportOptions} and
 * slightly modified.
 *
 * @author Brett Giles 2009
 */
public class XSDWriteOptions extends OptionsDialog {
	/**
	 *    
	 */
	private static final long serialVersionUID = -5586114824941879038L;

	/**  */
	@SuppressWarnings("unused")
	private String _dbType;

	/**
	 * Thid dialog's parent frame
	 */
	private SketchFrame _theFrame;

	/**
	 * Tag name for the top level element of the XML Schema
	 * <p/>
	 * Note this implies a single top level node design for the schema.
	 */
	private JTextField _topLevelTag;

	/**
	 * Use a target namespace or not --- NOT FULLY IMPLEMENTED
	 */
	private JCheckBox _useTargetNameSpace;

	/**
	 * checkbox to set whether to have qualified or unqualified elements in the
	 * XMLDBDriver.
	 */
	private JCheckBox _useUnqualifiedAttributes;

	/**
	 * checkbox to set whether to have qualified or unqualified attributes in the
	 * XMLDBDriver.
	 */
	private JCheckBox _useUnqualifiedElements;

	/**
	 * Creates and displays a new modal db options dialog.
	 *
	 * @param sketchFrame the SketchFrame to attach this modal dialog box to
	 */
	public XSDWriteOptions(final SketchFrame sketchFrame) {
		super(sketchFrame, "XML Schema parameters");

		setSize(425, 350);

		_theFrame = sketchFrame;

		showDialog();
	}

	/**
	 * Gets XMLDBDriver Write options.
	 *
	 * @return
	 */
	@Override
	public List<Option> getOptions() {
		final List<Option> opts = new LinkedList<>();
		final Map<String, String> saved = _theFrame.getMModel().getConnectionParams();
		final JPanel xsdopts = new JPanel();

		xsdopts.setLayout(new BoxLayout(xsdopts, BoxLayout.Y_AXIS));

		// FIXME -- we should add some help (mouseover? help button?) to
		// describe these in more detail
		_useTargetNameSpace = new JCheckBox("Use target namespace");

		xsdopts.add(_useTargetNameSpace);
		_useTargetNameSpace.setEnabled(true);

		_useUnqualifiedAttributes = new JCheckBox("Use unqualified attributes");

		xsdopts.add(_useUnqualifiedAttributes);
		_useUnqualifiedAttributes.setEnabled(true);
		_useUnqualifiedAttributes.setSelected(true);

		_useUnqualifiedElements = new JCheckBox("Use unqualified elements");

		xsdopts.add(_useUnqualifiedElements);
		_useUnqualifiedElements.setEnabled(true);
		_useUnqualifiedElements.setSelected(true);
		opts.add(new Option(new JLabel("XMLDBDriver options"), xsdopts));

		String topLevelTag = saved.get("topLevelTag");

		if (topLevelTag == null) {
			topLevelTag = _theFrame.getMModel().getDocInfo().getName().replaceAll("\\W+", "_").replaceFirst("^_+", "")
					.replaceFirst("_+$", "");
		}

		_topLevelTag = JUtils.textField(topLevelTag);

		opts.add(new Option(new JLabel("Top Level Tag"), _topLevelTag));

		return opts;
	}

	/**
	 * Take the parameters from the dialog and return them as a Map.
	 * <p/>
	 * The parameters are put in as:
	 * <ul>
	 * <li>targetNS: This is a boolean value whether to add a target namespace or
	 * not.</li>
	 * <li>unqualifiedAttributes: Assume unqualified attributes if this is
	 * true.</li>
	 * <li>unqualifiedElements: Assume unqualified elements if this is true.</li>
	 * <li>topLevelTag: The tag name of the top level element in the schema.</li>
	 * </ul>
	 *
	 * @return the map of the parameters.
	 */
	public Map<String, Object> getParams() {
		final Map<String, Object> options = new HashMap<>(5);

		if (_useTargetNameSpace.isSelected()) {
			options.put("targetNS", "true");
		} else {
			options.put("targetNS", "false");
		}

		if (_useUnqualifiedAttributes.isSelected()) {
			options.put("unqualifiedAttributes", "true");
		} else {
			options.put("unqualifiedAttributes", "false");
		}

		if (_useUnqualifiedElements.isSelected()) {
			options.put("unqualifiedElements", "true");
		} else {
			options.put("unqualifiedElements", "false");
		}

		options.put("topLevelTag", _topLevelTag.getText());

		// @todo - next statement just goes away to nevernever land
		// _theFrame.getMModel().getConnectionParams().put("topLevelTag",
		// _topLevelTag.getText());
		return options;
	}
}
