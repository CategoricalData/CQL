package easik.ui;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;

/**
 * Implementing this class by providing the getTabs() method tells OptionsDialog
 * that this options dialog uses tabs, and it will call getTabs() to get the
 * tabs (and contained options) instead of getOptions().
 */
public interface TabbedOptionsDialog {
	/**
	 * Returns a list of OptionTabs to display.
	 *
	 * @return
	 */
	public List<OptionTab> getTabs();
}
