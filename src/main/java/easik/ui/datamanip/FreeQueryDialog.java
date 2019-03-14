package easik.ui.datamanip;

//~--- non-JDK imports --------------------------------------------------------

import java.awt.BorderLayout;
import java.awt.Dimension;
//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import easik.ui.JUtils;

/**
 *
 *
 * @version 12/09/12
 * @author Christian Fiddick
 */
public class FreeQueryDialog extends JDialog {
	/**
	 *    
	 */
	private static final long serialVersionUID = 8985226585358083750L;

	/** Flag indicating the state of the OK button */
	private boolean _ok = false;

	/** This dialog's parent frame */
	@SuppressWarnings("unused")
	private JFrame _parent;

	/** JScrollPane for entering query */
	private JScrollPane querySpace;

	/**
	 * Sets up text area with a default statement fragment for the user to complete.
	 *
	 * @param parent      The parent frame of the modal dialog
	 * @param defaultText The default text to appear in dialog's query space
	 */
	public FreeQueryDialog(JFrame parent, String defaultText) {
		super(parent, "Enter Query", true);

		_parent = parent;
		querySpace = JUtils.textArea(defaultText, 8, 150);

		querySpace.setPreferredSize(new Dimension(450, 100));
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		JButton ok = new JButton("OK"), cancel = new JButton("Cancel");

		ok.setActionCommand("ok");
		cancel.setActionCommand("cancel");

		ButtonListener bl = new ButtonListener();

		ok.addActionListener(bl);
		cancel.addActionListener(bl);

		JPanel buttons = new JPanel();

		buttons.add(ok);
		buttons.add(cancel);
		add(querySpace, BorderLayout.CENTER);
		add(buttons, BorderLayout.SOUTH);

		_ok = false;

		// get a good size such that no scrolling is needed
		Dimension size = getPreferredSize();

		setSize(size);
		setVisible(true);
	}

	/**
	 * Returns true if the user accepted the options dialog (that is, clicked the OK
	 * button).
	 *
	 * @return true if the user clicked OK (and the fields verified successfully),
	 *         false if the user cancelled or closed the dialog.
	 */
	public boolean isAccepted() {
		return _ok;
	}

	/**
	 * Returns a map of column names to its input.
	 * 
	 * @return The map of column names to input if verify has benn called and
	 *         returned true, otherwise null.
	 */
	public String getInput() {
		return JUtils.taText(querySpace);
	}

	/**
	 *
	 *
	 * @version 12/09/12
	 * @author Christian Fiddick
	 */
	private class ButtonListener implements ActionListener {
		// Fired when the user clicks OK or Cancel

		/**
		 *
		 *
		 * @param e
		 */
		@Override
		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand().equals("ok")) {
				_ok = true;

				FreeQueryDialog.this.dispose();
			} else if (e.getActionCommand().equals("cancel")) {
				FreeQueryDialog.this.dispose();
			}
		}
	}
}
