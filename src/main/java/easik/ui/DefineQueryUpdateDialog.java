package easik.ui;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.util.LinkedList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import easik.view.vertex.QueryNode;

/**
 *
 * @author Sarah van der Laan 2013
 */
public class DefineQueryUpdateDialog extends OptionsDialog {

	private static final long serialVersionUID = 4044546505124282150L;

	/** The inputs for our fields */
	private JScrollPane updateStatement;

	/** The query no which we are editing */
	private QueryNode ourNode;

	/**
	 *
	 * @param parent
	 * @param title
	 * @param inNode
	 */
	public DefineQueryUpdateDialog(JFrame parent, String title, QueryNode inNode) {
		super(parent, title);

		setSize(300, 200);

		ourNode = inNode;

		showDialog();
	}

	/**
	 *
	 *
	 * @return
	 */
	@Override
	public List<Option> getOptions() {
		LinkedList<Option> opts = new LinkedList<>();

		// Add the name field
		opts.add(new Option(new JLabel("Update Statement"), updateStatement = JUtils.textArea(ourNode.getUpdate())));

		return opts;
	}

	/**
	 *
	 *
	 * @return
	 */
	public String getUpdate() {
		return JUtils.taText(updateStatement);
	}

}
