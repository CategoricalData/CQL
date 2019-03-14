package easik.ui.menu.popup;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.util.LinkedList;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.JTextField;

import easik.ui.JUtils;
import easik.ui.Option;
import easik.ui.OptionsDialog;
import easik.ui.ViewFrame;
import easik.view.edge.InjectiveViewEdge;
import easik.view.edge.PartialViewEdge;
import easik.view.edge.View_Edge;

/**
 *
 *
 * @version 2014
 * @author Federico Mora
 */
public class ViewEdgeOptions extends OptionsDialog {
	// Constants for the width and height. LONG_ is for the dialog when it
	// includes a direction dropdown (which is only for new, non-self edges).

	/**
	 *    
	 */
	private static final long serialVersionUID = -7146098699149643288L;

	/**  */
	@SuppressWarnings({ "unused", "hiding" })
	private static final int WIDTH = 300, HEIGHT = 160, LONG_WIDTH = 450, LONG_HEIGHT = 200;

	private View_Edge _viewEdge;

	/*
	 * Various JThings containing entered information. Note that not all of these
	 * are used for every edge type.
	 */

	/**  */
	private JTextField _viewEdgeName;

	private ViewFrame _theViewFrame;

	/**  */
	@SuppressWarnings("unused")
	private Edge _type;

	/** The type of edges that can be added. */
	public enum Edge {
		NORMAL, INJECTIVE, PARTIAL, SELF
	}

	;

	/**
	 * Creates and displays a new modal edge options dialog.
	 *
	 * @param ViewFrame the ViewFrame to attach this modal dialog box to
	 * @param edge      takes an existing edge to set initial values from when
	 *                  modifying an edge
	 * @author Federico Mora
	 */
	public ViewEdgeOptions(ViewFrame viewFrame, View_Edge edge) {
		super(viewFrame, "Rename");

		_theViewFrame = viewFrame;
		_viewEdge = edge;

		_type = (edge instanceof PartialViewEdge) ? ((edge.getSource() == edge.getTarget()) ? Edge.SELF : Edge.PARTIAL)
				: (edge instanceof InjectiveViewEdge) ? Edge.INJECTIVE : Edge.NORMAL;

		setSize(WIDTH, HEIGHT);
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

		_viewEdgeName = JUtils
				.textField((_viewEdge != null) ? _viewEdge.getName() : _theViewFrame.getMModel().getNewName());

		opts.add(new Option("Edge name", _viewEdgeName));

		return opts;
	}

	/**
	 *
	 *
	 * @return
	 */
	@Override
	public String getName() {
		return _viewEdgeName.getText();
	}

	/**
	 *
	 *
	 * @return
	 */
	@Override
	public boolean verify() {
		String s = getName();

		// An edge name must be specified
		if (s.equals("")) {
			JOptionPane.showMessageDialog(this, "No edge name specified", "Error", JOptionPane.ERROR_MESSAGE);

			return false;
		}

		// the edge name must not already exist
		else if (((_viewEdge == null) || !s.equals(_viewEdge.getName())) && _theViewFrame.getMModel().isNameUsed(s)) {
			JOptionPane.showMessageDialog(this, "Edge name \"" + s + "\" is already in use", "Error",
					JOptionPane.ERROR_MESSAGE);

			return false;
		}

		return true;
	}
}
