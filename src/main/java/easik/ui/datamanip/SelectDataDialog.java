package easik.ui.datamanip;

//~--- JDK imports ------------------------------------------------------------

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

/**
 * JDialog set up to allow selection of records. The data being displayed is
 * handed to this class in the form of a JTable. The use
 */
public class SelectDataDialog extends JDialog {
	/**
	 *    
	 */
	private static final long serialVersionUID = 1879087330825814111L;

	/**  */
	boolean _ok = false;

	/**  */
	private JTable guiTable;

	/**
	 * Sets up a data selection dialog on a dialog instead of a frame.
	 *
	 * @param parent
	 *            the parent JDialog of the modal dialog
	 * @param title
	 *            The title of the dialog window
	 * @param table
	 *            The data of the table to display
	 */
	public SelectDataDialog(final JDialog parent, final String title, final JTable table) {
		super(parent, title, true);

		init(table);
	}

	/**
	 * Sets up a data selection dialog.
	 *
	 * @param parent
	 *            the parent frame of the modal dialog
	 * @param title
	 *            The title of the dialog window
	 * @param table
	 *            The data of the table to display
	 */
	public SelectDataDialog(final JFrame parent, final String title, final JTable table) {
		super(parent, title, true);

		init(table);
	}

	/**
	 *
	 *
	 * @param table
	 */
	private void init(final JTable table) {
		guiTable = table;

		guiTable.setDefaultEditor(Object.class, null); // make cells uneditable
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		final JButton ok = new JButton("OK"), cancel = new JButton("Cancel");

		getRootPane().setDefaultButton(ok);
		ok.setActionCommand("ok");
		cancel.setActionCommand("cancel");

		final ButtonListener bl = new ButtonListener();

		ok.addActionListener(bl);
		cancel.addActionListener(bl);

		final JPanel buttons = new JPanel();

		buttons.add(ok);
		buttons.add(cancel);

		// make double click on table fire OK button
		guiTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(final MouseEvent e) {
				if ((e.getClickCount() > 1) && (e.getButton() == MouseEvent.BUTTON1)) // double
																						// click
				{
					ok.doClick();
				}
			}

			@Override
			public void mousePressed(final MouseEvent e) {
				mouseClicked(e);
			}
		});
		add(new JScrollPane(guiTable), BorderLayout.CENTER);
		add(buttons, BorderLayout.SOUTH);

		_ok = false;

		this.pack();
		this.setSize(this.getPreferredSize());
		setLocationRelativeTo(getParent());
		setVisible(true);
	}

	/**
	 * Returns true if the user accepted the options dialog (that is, clicked
	 * the OK button).
	 *
	 * @return true if the user clicked OK (and the fields verified
	 *         successfully), false if the user cancelled or closed the dialog.
	 */
	public boolean isAccepted() {
		return _ok;
	}

	/**
	 * Gets the rows selected from the data table. This information can then be
	 * used by DatabaseUtils to find selected primary IDs.
	 *
	 * @return An int array of row numbers selected in the display
	 */
	public int[] getSelectedPKs() {
		final int[] selectedRows = guiTable.getSelectedRows();
		final int[] selectedPKs = new int[selectedRows.length];
		final int PKcolumn = 0; // convention for DatabaseUtil.getTable()

		for (int i = 0; i < selectedRows.length; i++) {
			selectedPKs[i] = Integer.parseInt((String) guiTable.getValueAt(selectedRows[i], PKcolumn));
		}

		return selectedPKs;
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
		public void actionPerformed(final ActionEvent e) {
			if ("ok".equals(e.getActionCommand())) {
				_ok = true;

				SelectDataDialog.this.dispose();
			} else if ("cancel".equals(e.getActionCommand())) {
				guiTable.setCellSelectionEnabled(false);
				SelectDataDialog.this.dispose();
			}
		}
	}
}
