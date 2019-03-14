package easik.model.attribute;

import java.awt.Component;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.util.LinkedList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import easik.database.types.BigInt;
import easik.database.types.Blob;
import easik.database.types.Char;
import easik.database.types.Custom;
import easik.database.types.Date;
import easik.database.types.Decimal;
import easik.database.types.DoublePrecision;
import easik.database.types.EasikType;
import easik.database.types.Int;
import easik.database.types.SmallInt;
import easik.database.types.Text;
import easik.database.types.Time;
import easik.database.types.Timestamp;
import easik.database.types.Varchar;
import easik.graph.EasikGraphModel;
import easik.model.Model;
import easik.model.edge.ModelEdge;
import easik.model.ui.ModelFrame;
import easik.model.vertex.ModelVertex;
import easik.ui.JUtils;
import easik.ui.Option;
import easik.ui.OptionTab;
import easik.ui.OptionsDialog;
import easik.ui.TabbedOptionsDialog;

/**
 * Options dialog class that prompts the user for attribute information (name
 * and type) for a new or existing attribute.
 */
public class AttributeUI<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>>
		extends OptionsDialog implements TabbedOptionsDialog {
	/**
	 *    
	 */
	private static final long serialVersionUID = -4279807107604398008L;

	/** The attribute being modified, or null if adding an attribute */
	private EntityAttribute<F, GM, M, N, E> _att = null;

	/**  */
	private JSpinner _decPrec, _decScale, _varcharSize, _charSize;

	/** References to the various fields on the page */
	private JTextField _name, _custom;

	/** The N this attribute is being added to */
	private N _node;

	/** The window's Sketch Frame */
	private F _theFrame;

	/**  */
	private JRadioButton tInt, tSmallInt, tBigInt, tFloat, tDouble, tDecimal, tVarchar, tChar, tText, tBlob, tDate,
			tTime, tTimestamp, tBoolean, tCustom;

	/**  */
	private ButtonGroup types;

	/**
	 * Creates a dialog box prompting the user for an attribute type to add.
	 *
	 * @param inFrame the F to which the modal dialog box should be attached
	 * @param node    the N this attribute is being added to; used to check the name
	 *                for uniqueness. If null, the check will be skipped.
	 *
	 */
	public AttributeUI(F inFrame, N node) {
		super(inFrame, "Add Attribute");

		this.setSize(500, 400);

		_theFrame = inFrame;
		_node = node;

		showDialog();
	}

	/**
	 * Creates a dialog box prompting the user for an attribute type, when modifying
	 * the passed-in attribute. The passed-in value will be the default option on
	 * the page, so that clicking "OK" should result in no changes.
	 *
	 * @param inFrame the F to which the modal dialog box should be attached
	 * @param node
	 * @param inAtt   the attribute being modified
	 */
	public AttributeUI(F inFrame, N node, EntityAttribute<F, GM, M, N, E> inAtt) {
		super(inFrame, "Modify Attribute");

		this.setSize(500, 400);

		_theFrame = inFrame;
		_node = node;
		_att = inAtt;

		showDialog();
	}

	/**
	 * Pre-tab options. We include a title and the attribute name field.
	 *
	 * @return
	 */
	@Override
	public List<Option> getOptions() {
		LinkedList<Option> opts = new LinkedList<>();

		opts.add(new Option(new JLabel("Attribute name:"),
				_name = JUtils.textField((_att == null) ? "" : _att.getName())));

		return opts;
	}

	/**
	 * Returns the tabs for the page; we separate the options into rough categories:
	 * numeric, string, date/time, and other (for boolean/custom).
	 *
	 * @return
	 */
	@Override
	public List<OptionTab> getTabs() {
		LinkedList<OptionTab> tabs = new LinkedList<>();
		EasikType currentType = (_att != null) ? _att.getType() : null;
		JPanel pInts = new JPanel(), pFloats = new JPanel(), pChars = new JPanel(), pTS = new JPanel(),
				pCustom = new JPanel();

		pInts.setLayout(new BoxLayout(pInts, BoxLayout.Y_AXIS));
		pFloats.setLayout(new BoxLayout(pFloats, BoxLayout.Y_AXIS));
		pChars.setLayout(new BoxLayout(pChars, BoxLayout.Y_AXIS));
		pTS.setLayout(new BoxLayout(pTS, BoxLayout.Y_AXIS));
		pCustom.setLayout(new BoxLayout(pCustom, BoxLayout.Y_AXIS));

		OptionTab numeric = new OptionTab("Numeric");
		OptionTab text = new OptionTab("Character/data");
		OptionTab datetime = new OptionTab("Date/time");
		OptionTab other = new OptionTab("Other");

		types = new ButtonGroup();

		types.add(tInt = new JRadioButton("INTEGER"));
		tInt.setToolTipText(
				"An integer value field (usually a 32-bit int) that stores integer values from -2147483648 to 2147483647");

		if (currentType instanceof Int) {
			tInt.setSelected(true);
			numeric.setInitial(true);
		}

		types.add(tSmallInt = new JRadioButton("SMALLINT"));
		tSmallInt.setToolTipText(
				"An integer value field (usually a 16-bit int) that stores integer values from (at least) -32768 to 32767");

		if (currentType instanceof SmallInt) {
			tSmallInt.setSelected(true);
			numeric.setInitial(true);
		}

		types.add(tBigInt = new JRadioButton("BIGINT"));
		tBigInt.setToolTipText(
				"<html>An integer value field (usually a 64-bit int) that stores integer values from -9223372036854775808<br>to 9223372036854775807");

		if (currentType instanceof BigInt) {
			tBigInt.setSelected(true);
			numeric.setInitial(true);
		}

		pInts.add(tInt);
		pInts.add(tSmallInt);
		pInts.add(tBigInt);
		numeric.addOption(new Option("Integers", pInts));
		types.add(tDouble = new JRadioButton("DOUBLE PRECISION"));
		tDouble.setToolTipText(
				"<html>A floating point value with at least 15 digits of precision (typically a standard 64-bit<br>floating-point value with 53 bits of precision).");

		if (currentType instanceof DoublePrecision) {
			tDouble.setSelected(true);
			numeric.setInitial(true);
		}

		types.add(tFloat = new JRadioButton("FLOAT"));
		tFloat.setToolTipText(
				"<html>A floating point value with at least 6 digits of precision (typically a standard 32-bit<br>floating-point value with 24 bits of precision).  This is sometimes known as a REAL, but a REAL is<br>also sometimes an alias for a DOUBLE PRECISION.");

		if (currentType instanceof easik.database.types.Float) {
			tFloat.setSelected(true);
			numeric.setInitial(true);
		}

		pFloats.add(tDouble);
		pFloats.add(tFloat);
		numeric.addOption(new Option("Floating-point", pFloats));
		types.add(tDecimal = new JRadioButton("NUMERIC"));

		// We allow a max precision of 38, since that seems to be the lowest
		// maximum (in MS SQL and Oracle)
		// Pg supports up to 1000, and MySQL supports up to 63.
		// *Any* default here doesn't make much sense; 10,2 is an arbitrary
		// choice.
		// This default allows anything from 0.00 to 99,999,999.99
		int defPrec = 10, defScale = 2;

		if (currentType instanceof Decimal) {
			tDecimal.setSelected(true);
			numeric.setInitial(true);

			Decimal d = (Decimal) currentType;

			defPrec = d.getPrecision();
			defScale = d.getScale();
		}

		_decPrec = new JSpinner(new SpinnerNumberModel(defPrec, 1, 38, 1));

		// The scale limits, for most databases, are 0 <= scale <= precision.
		// Oracle, however
		// permits the scale to be larger than the precision. We don't.
		_decScale = new JSpinner(new SpinnerNumberModel(defScale, 0, 38, 1));

		// Add a change listener to update the scale maximum to the current
		// precision value:
		_decPrec.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				int scaleMax = ((SpinnerNumberModel) _decPrec.getModel()).getNumber().intValue();

				((SpinnerNumberModel) _decScale.getModel()).setMaximum(scaleMax);

				if (((SpinnerNumberModel) _decScale.getModel()).getNumber().intValue() > scaleMax) {
					_decScale.setValue(new Integer(scaleMax));
				}
			}
		});

		JPanel decPanel = new JPanel();

		decPanel.setLayout(new BoxLayout(decPanel, BoxLayout.X_AXIS));
		decPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		decPanel.add(tDecimal);
		decPanel.add(new JLabel("(Precision: "));
		decPanel.add(JUtils.fixWidth(JUtils.fixHeight(_decPrec)));
		decPanel.add(new JLabel(", Scale: "));
		decPanel.add(JUtils.fixWidth(JUtils.fixHeight(_decScale)));
		decPanel.add(new JLabel(")"));
		decPanel.setToolTipText(
				"<html>A fixed-point numeric type.  Also known as DECIMAL.<br><br>This type is substantially slower than integer and floating-point types, but guarantees precision<br>for the range of values it supports.  The \"precision\" value is the total number of digits storable, <br>and the \"scale\" is the number of digits stored after the decimal point.  '12345.67' has precision 7<br>and scale 2.");
		tDecimal.setToolTipText(decPanel.getToolTipText());
		numeric.addOption(new Option("Fixed-point", decPanel));
		types.add(tVarchar = new JRadioButton("VARCHAR"));

		int vcs = 255;

		if (currentType instanceof Varchar) {
			tVarchar.setSelected(true);
			text.setInitial(true);

			vcs = ((Varchar) currentType).getSize();
		}

		_varcharSize = new JSpinner(new SpinnerNumberModel(vcs, 1, 255, 1));

		JPanel vcPanel = new JPanel();

		vcPanel.setLayout(new BoxLayout(vcPanel, BoxLayout.X_AXIS));
		vcPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		vcPanel.add(tVarchar);
		vcPanel.add(new JLabel("Size: "));
		vcPanel.add(JUtils.fixHeight(JUtils.fixWidth(_varcharSize)));
		vcPanel.setToolTipText(
				"<html>Stores a string of characters of up to \"size\" characters.  Unlike a CHAR, a VARCHAR column is<br>typically stored using the minimum storage space required, while a CHAR field pads shorter strings<br>to always store values of \"size\" length.");
		tVarchar.setToolTipText(vcPanel.getToolTipText());
		pChars.add(vcPanel);
		types.add(tChar = new JRadioButton("CHAR"));

		int cs = 255;

		if (currentType instanceof Char) {
			tChar.setSelected(true);
			text.setInitial(true);

			cs = ((Char) currentType).getSize();
		}

		_charSize = new JSpinner(new SpinnerNumberModel(cs, 1, 255, 1));

		JPanel cPanel = new JPanel();

		cPanel.setLayout(new BoxLayout(cPanel, BoxLayout.X_AXIS));
		cPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		cPanel.add(tChar);
		cPanel.add(new JLabel("Size: "));
		cPanel.add(JUtils.fixHeight(JUtils.fixWidth(_charSize)));
		cPanel.setToolTipText(
				"<html>Stores a string of characters of up to \"size\" characters.  Unlike a VARCHAR, a CHAR column is<br>typically padded up to the specified size to make it a fixed-width column (the padding is removed on<br>retrieval).  Note that some databases implicitly convert CHAR columns to VARCHAR if other<br>variable-width columns exist in the table.");
		tChar.setToolTipText(cPanel.getToolTipText());
		pChars.add(cPanel);
		text.addOption(new Option("Characters", pChars));
		types.add(tText = new JRadioButton("TEXT (stores up to 4GB)"));
		tText.setToolTipText("Stores large amounts of text data.  Also known as a CLOB.");

		if (currentType instanceof Text) {
			tText.setSelected(true);
			text.setInitial(true);
		}

		text.addOption(new Option("Text data", tText));
		types.add(tBlob = new JRadioButton("BLOB (stores up to 4GB)"));
		tBlob.setToolTipText("Stores large amounts of binary data (bytes).  Will result in a BYTEA under PostgreSQL.");

		if (currentType instanceof Blob) {
			tBlob.setSelected(true);
			text.setInitial(true);
		}

		text.addOption(new Option("Binary data", tBlob));
		types.add(tDate = new JRadioButton("DATE"));
		tDate.setToolTipText("A date field that does not include a time, such as '2008/07/14'");

		if (currentType instanceof Date) {
			tDate.setSelected(true);
			datetime.setInitial(true);
		}

		datetime.addOption(new Option("Date only", tDate));
		types.add(tTime = new JRadioButton("TIME"));
		tTime.setToolTipText("A field that stores just a time (e.g. '12:13:14')");

		if (currentType instanceof Time) {
			tTime.setSelected(true);
			datetime.setInitial(true);
		}

		datetime.addOption(new Option("Time only", tTime));
		types.add(tTimestamp = new JRadioButton("TIMESTAMP (date and time)"));
		tTimestamp.setToolTipText(
				"<html>A field that stores a date and time (e.g. '2008/07/14 12:13:14').  Note that this is converted to a<br>DATETIME when using MySQL.");

		if (currentType instanceof Timestamp) {
			tTimestamp.setSelected(true);
			datetime.setInitial(true);
		}

		pTS.add(tTimestamp);
		datetime.addOption(new Option("Date & time", pTS));
		types.add(tBoolean = new JRadioButton("BOOLEAN"));
		tBoolean.setToolTipText(
				"<html>A column that stores true/false values.  Note that this type may be converted to a small integer<br>type by databases (such as MySQL) that do not fully support BOOLEAN data types");

		if (currentType instanceof easik.database.types.Boolean) {
			tBoolean.setSelected(true);
			other.setInitial(true);
		}

		other.addOption(new Option("Boolean", tBoolean));
		types.add(tCustom = new JRadioButton("Custom type:"));

		if (currentType instanceof Custom) {
			tCustom.setSelected(true);
			other.setInitial(true);

			_custom = JUtils.textField(((Custom) currentType).getCustom());
		} else {
			_custom = JUtils.textField("");
		}

		JPanel custPanel = new JPanel();

		custPanel.setLayout(new BoxLayout(custPanel, BoxLayout.X_AXIS));
		custPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		custPanel.setToolTipText(
				"<html>Any SQL type supported by the SQL db that will be exported to can be entered here.  No<br>verification of this field is performed: you must ensure that what you specify here is a valid type<br>for the SQL db type you will be using!");
		tCustom.setToolTipText(custPanel.getToolTipText());
		_custom.setToolTipText(custPanel.getToolTipText());
		custPanel.add(tCustom);
		custPanel.add(_custom);
		other.addOption(new Option("Custom:", custPanel));
		tabs.add(numeric);
		tabs.add(text);
		tabs.add(datetime);
		tabs.add(other);

		return tabs;
	}

	/**
	 *
	 *
	 * @return
	 */
	@Override
	public boolean verify() {
		JRadioButton selected = getSelectedButton();

		if (selected == null) {
			JOptionPane.showMessageDialog(this, "No attribute type selected!", "Attribute error",
					JOptionPane.ERROR_MESSAGE);

			return false;
		} else if ((selected == tCustom) && _custom.getText().trim().equals("")) {
			JOptionPane.showMessageDialog(this,
					"No custom attribute type entered!\n\n"
							+ "You must enter the SQL type signature when selecting a custom type",
					"Attribute error", JOptionPane.ERROR_MESSAGE);

			return false;
		}

		String name = getName();

		if (name.equals("")) {
			JOptionPane.showMessageDialog(this, "No attribute name entered!", "Attribute error",
					JOptionPane.ERROR_MESSAGE);

			return false;
		}

		// Adding or changing the name
		else if ((_att == null) || !_att.getName().equals(name)) {
			if (_node.isAttNameUsed(name)) {
				JOptionPane.showMessageDialog(_theFrame,
						"Invalid attribute name: an attribute with that name already exists.", "Attribute error",
						JOptionPane.ERROR_MESSAGE);

				return false;
			}
		}

		return true;
	}

	/**
	 *
	 *
	 * @return
	 */
	private JRadioButton getSelectedButton() {
		return tInt.isSelected() ? tInt
				: tSmallInt.isSelected() ? tSmallInt
						: tBigInt.isSelected() ? tBigInt
								: tFloat.isSelected() ? tFloat
										: tDouble.isSelected() ? tDouble
												: tDecimal.isSelected() ? tDecimal
														: tVarchar.isSelected() ? tVarchar
																: tChar.isSelected() ? tChar
																		: tText.isSelected() ? tText
																				: tBlob.isSelected() ? tBlob
																						: tDate.isSelected() ? tDate
																								: tTime.isSelected()
																										? tTime
																										: tTimestamp
																												.isSelected()
																														? tTimestamp
																														: tBoolean
																																.isSelected()
																																		? tBoolean
																																		: tCustom
																																				.isSelected()
																																						? tCustom
																																						: null;
	}

	/**
	 * Returns the name entered by the user. Leading and trailing whitespace is
	 * trimmed.
	 *
	 * @return
	 */
	@Override
	public String getName() {
		return _name.getText().trim();
	}

	/**
	 * Creates and returns new EasikType object reflecting the user's choice.
	 * Returns null if the user hasn't accepted the dialog yet.
	 *
	 * @return
	 */
	public EasikType getCustomType() {
		if (!isAccepted()) {
			return null;
		}

		return tInt.isSelected() ? new Int()
				: tSmallInt.isSelected() ? new SmallInt()
						: tBigInt.isSelected() ? new BigInt()
								: tFloat.isSelected() ? new easik.database.types.Float()
										: tDouble.isSelected() ? new DoublePrecision()
												: tDecimal.isSelected()
														? new Decimal(((SpinnerNumberModel) _decPrec.getModel())
																.getNumber().intValue(),
																((SpinnerNumberModel) _decScale.getModel()).getNumber()
																		.intValue())
														: tVarchar.isSelected()
																? new Varchar(
																		((SpinnerNumberModel) _varcharSize.getModel())
																				.getNumber().intValue())
																: tChar.isSelected() ? new Char(
																		((SpinnerNumberModel) _charSize.getModel())
																				.getNumber().intValue())
																		: tText.isSelected() ? new Text()
																				: tBlob.isSelected() ? new Blob()
																						: tDate.isSelected()
																								? new Date()
																								: tTime.isSelected()
																										? new Time()
																										: tTimestamp
																												.isSelected()
																														? new Timestamp()
																														: tBoolean
																																.isSelected()
																																		? new easik.database.types.Boolean()
																																		: tCustom
																																				.isSelected()
																																						? new Custom(
																																								_custom.getText()
																																										.trim())
																																						: null;
	}
}
