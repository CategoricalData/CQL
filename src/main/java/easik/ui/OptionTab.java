package easik.ui;

//~--- JDK imports ------------------------------------------------------------

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;

/**
 * Simple class to store a store information about a tab and the options
 * contained on that tab.
 *
 * @see OptionsDialog
 */
public class OptionTab {
	/**  */
	private Icon _icon = null;

	/**  */
	private String _title, _tooltip = null;

	/**  */
	private boolean _hasMnemonic, _isInitial;

	/**  */
	private int _mnemonic;

	/**  */
	private LinkedList<Option> _options;

	/**
	 * Creates a new OptionTab with the specified tab name.
	 *
	 * @param title the tab title
	 */
	public OptionTab(String title) {
		this(title, null, null);
	}

	/**
	 * Creates a new OptionTab with the specified tab name and tooltip text.
	 *
	 * @param title the tab title
	 * @param tip   the tooltip text (may be null)
	 */
	public OptionTab(String title, String tip) {
		this(title, null, tip);
	}

	/**
	 * Creates a new OptionTab with the specified tab name, icon, and tooltip text.
	 *
	 * @param title the tab title
	 * @param icon  the Icon to display next to the title
	 * @param tip   the tooltip text (may be null)
	 */
	public OptionTab(String title, Icon icon, String tip) {
		_title = title;
		_icon = icon;
		_tooltip = tip;
		_options = new LinkedList<>();
	}

	/**
	 * Returns the title of the tab.
	 *
	 * @return
	 */
	public String getTitle() {
		return _title;
	}

	/**
	 * Returns the icon associated with the tab, or null if there is no icon.
	 *
	 * @return
	 */
	public Icon getIcon() {
		return _icon;
	}

	/**
	 * Returns the tooltip text associated with the tab, or null if there is no
	 * tooltip text.
	 *
	 * @return
	 */
	public String getToolTip() {
		return _tooltip;
	}

	/**
	 * Sets the keyboard mnemonic for accessing the specified tab.
	 *
	 * @param mnemonic the KeyEvent code for accessing the tab
	 * @see javax.swing.JTabbedPane.setMnemonicAt(int, int)
	 */
	public void setMnemonic(int mnemonic) {
		_hasMnemonic = true;
		_mnemonic = mnemonic;
	}

	/**
	 *
	 *
	 * @return
	 */
	public int getMnemonic() {
		return _mnemonic;
	}

	/**
	 *
	 *
	 * @return
	 */
	public boolean hasMnemonic() {
		return _hasMnemonic;
	}

	/**
	 * Gets a list of options to be displayed on this tab.
	 *
	 * @return list of Options to be displayed
	 */
	public List<Option> getOptions() {
		return Collections.unmodifiableList(_options);
	}

	/**
	 * Adds an option to the list of options to be displayed.
	 *
	 * @param o the Option object to add
	 */
	public void addOption(Option o) {
		_options.add(o);
	}

	/**
	 * Constructs a new option with the given label and component, and adds that
	 * option to the list of options to display.
	 *
	 * @param label the JLabel for the option
	 * @param comp  the JComponent associated with the option (i.e. the value field)
	 */
	public void addOption(JLabel label, JComponent comp) {
		_options.add(new Option(label, comp));
	}

	/**
	 * Clears the list of options for this tab.
	 */
	public void clearOptions() {
		_options.clear();
	}

	/**
	 * Sets whether or not this tab should be the initial tab displayed in the
	 * option dialog. This does *not* clear other OptionTab initial properties: if
	 * multiple OptionTabs have the initial property, only the first will be the
	 * initial tab.
	 *
	 * @param init true if this tab is the initial tab, false otherwise
	 */
	public void setInitial(boolean init) {
		_isInitial = init;
	}

	/**
	 * Returns true if this is the initial tab.
	 *
	 * @return
	 */
	public boolean isInitial() {
		return _isInitial;
	}
}
