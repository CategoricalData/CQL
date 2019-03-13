package easik.ui;

//~--- JDK imports ------------------------------------------------------------

import java.awt.Component;
import java.awt.FlowLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * Simple class to store a label/component pair to be displayed in an
 * OptionsDialog.
 *
 * @see OptionsDialog
 */
public class Option {
	/**  */
	private JComponent component;

	/**  */
	private JLabel label;

	/**
	 * Creates a new label-only option line. This can be used for a title, or
	 * some other long description that should use up the entire line.
	 *
	 * @param label
	 */
	public Option(JLabel label) {
		this(label, new JComponent[0]);
	}

	/**
	 * Creates a new label-only option line, creating a new JLabel from the
	 * provided string.
	 *
	 * @see easik.ui.Option( javax.swing.JLabel)
	 *
	 * @param label
	 */
	public Option(String label) {
		this(new JLabel(label));
	}

	/**
	 * Creates a new label/component pair. If given multiple compnents, will add
	 * them to a JPanel and then set that as the component. Note that this is
	 * better than putting them into a JPanel yourself: the label will be
	 * centered on the first component passed-in.
	 *
	 * @param label
	 *            the JLabel of the option
	 * @param components
	 */
	public Option(JLabel label, JComponent... components) {
		setLabel(label);

		if (components.length > 1) {
			JPanel panel = new JPanel();
			FlowLayout flow = (FlowLayout) panel.getLayout();

			flow.setAlignment(FlowLayout.LEFT);
			flow.setVgap(0);
			flow.setHgap(0);

			for (JComponent jc : components) {
				panel.add(jc);
			}

			JUtils.fixHeight(panel);
			setComponent(panel);
		} else {
			setComponent((components.length > 0) ? components[0] : null);
		}

		// We try to align the label with the first component (descending into
		// JPanels), by creating a border that aligns the middle of the border
		// with the middle of the first component.
		if (components.length > 0) {
			int labelOffset = -label.getPreferredSize().height;
			Component comp = components[0];

			while (comp instanceof JPanel) {
				labelOffset += 2 * (((JPanel) comp).getInsets().top);
				comp = ((JPanel) comp).getComponent(0);
			}

			if (comp != null) {
				labelOffset += comp.getPreferredSize().height;

				if (labelOffset > 0) { // Only do it if the first non-JPanel
										// component is bigger
					label.setBorder(new EmptyBorder(labelOffset / 2, 0, 0, 0));
				}
			}
		}
	}

	/**
	 * Creates a new label/component pair, constructing a new JLabel from the
	 * provided string.
	 *
	 * @param label
	 *            the text of the JLabel
	 * @param components
	 */
	public Option(String label, JComponent... components) {
		this(new JLabel(label), components);
	}

	/**
	 * Sets the label of this option.
	 * 
	 * @param label
	 *            the new label
	 */
	public void setLabel(JLabel label) {
		this.label = label;
	}

	/**
	 * Returns the current label of this option.
	 * 
	 * @return the label
	 */
	public JLabel getLabel() {
		return label;
	}

	/**
	 * Sets the value component for this option.
	 * 
	 * @param comp
	 *            the component
	 */
	public void setComponent(JComponent comp) {
		component = comp;
	}

	/**
	 * Returns the current value component of this option.
	 * 
	 * @return the value component
	 */
	public JComponent getComponent() {
		return component;
	}

	/**
	 * Option.Title is a special Option subclass that creates an Option label
	 * row with specified text, formatted to be the title of an option dialog.
	 */
	public static class Title extends Option {
		/**
		 *
		 *
		 * @param title
		 */
		public Title(String title) {
			super(new JLabel("<html><big><b>" + title + "</b></big></html>"));
		}
	}
}
