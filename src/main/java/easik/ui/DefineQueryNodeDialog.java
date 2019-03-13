package easik.ui;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.util.LinkedList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import easik.view.vertex.QueryNode;

/**
 *
 *
 * @version 12/09/12
 * @author Christian Fiddick
 */
public class DefineQueryNodeDialog extends OptionsDialog {
	private static final long serialVersionUID = 4044546505124282150L;

	/** The inputs for our fields */
	private JTextField name;

	/** The query no which we are editing */
	private QueryNode ourNode;

	/**  */
	private JScrollPane query;

	/**
	 *
	 * @param parent
	 * @param title
	 * @param inNode
	 */
	public DefineQueryNodeDialog(JFrame parent, String title, QueryNode inNode) {
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
		opts.add(new Option(new JLabel("Name"), name = JUtils.textField(ourNode.getName())));

		// Add the query area
		opts.add(new Option(new JLabel("Query"), query = JUtils.textArea(ourNode.getQuery())));

		return opts;
	}

	/**
	 *
	 *
	 * @return
	 */
	@Override
	public String getName() {
		return name.getText();
	}

	/**
	 *
	 *
	 * @return
	 */
	public String getQuery() {
		return JUtils.taText(query);
	}
}
