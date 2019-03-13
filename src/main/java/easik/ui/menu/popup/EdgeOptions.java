package easik.ui.menu.popup;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.util.LinkedList;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import easik.model.edge.ModelEdge.Cascade;
import easik.sketch.edge.InjectiveEdge;
import easik.sketch.edge.PartialEdge;
import easik.sketch.edge.SketchEdge;
import easik.sketch.vertex.EntityNode;
import easik.ui.JUtils;
import easik.ui.Option;
import easik.ui.OptionsDialog;
import easik.ui.SketchFrame;

/**
 *
 *
 * @version 12/09/12
 * @author Christian Fiddick
 */
public class EdgeOptions extends OptionsDialog {
	// Constants for the width and height. LONG_ is for the dialog when it
	// includes a direction dropdown (which is only for new, non-self edges).

	/**
	 *    
	 */
	private static final long serialVersionUID = -7146098699149643288L;

	/**  */
	@SuppressWarnings("hiding")
	private static final int WIDTH = 300, HEIGHT = 160, LONG_WIDTH = 450, LONG_HEIGHT = 200;

	/**  */
	private SketchEdge _edge;

	/**  */
	@SuppressWarnings("rawtypes")
	private JComboBox _edgeDirection, _cascadeMode;

	/*
	 * Various JThings containing entered information. Note that not all of
	 * these are used for every edge type.
	 */

	/**  */
	private JTextField _edgeName;

	/**  */
	private EntityNode _source, _target;

	/**  */
	private SketchFrame _theFrame;

	/**  */
	private Edge _type;

	/** The type of edges that can be added. */
	public enum Edge {
		NORMAL, INJECTIVE, PARTIAL, SELF
	}

	;

	/**
	 * Creates and displays a new modal edge options dialog.
	 *
	 * @param sketchFrame
	 *            the SketchFrame to attach this modal dialog box to
	 * @param edge
	 *            takes an existing edge to set initial values from when
	 *            modifying an edge
	 */
	public EdgeOptions(SketchFrame sketchFrame, SketchEdge edge) {
		super(sketchFrame, "Edit edge");

		_theFrame = sketchFrame;
		_edge = edge;
		_type = (edge instanceof PartialEdge) ? ((edge.getSource() == edge.getTarget()) ? Edge.SELF : Edge.PARTIAL) : (edge instanceof InjectiveEdge) ? Edge.INJECTIVE : Edge.NORMAL;

		setSize(WIDTH, HEIGHT);
		showDialog();
	}

	/**
	 * Creates and displays a new modal edge options dialog for a new edge.
	 *
	 * @param sketchFrame
	 *            the SketchFrame to attach this modal dialog box to
	 * @param edgeType
	 *            the edge type (e.g. <code>Edge.INJECTIVE</code>)
	 * @param source
	 *            the source entity node
	 * @param target
	 *            the target entity node (can be null for an
	 *            <code>Edge.SELF</code> edge type)
	 */
	public EdgeOptions(SketchFrame sketchFrame, Edge edgeType, EntityNode source, EntityNode target) {
		super(sketchFrame, "New edge");

		_theFrame = sketchFrame;
		_type = edgeType;
		_source = source;
		_target = target;

		int width = WIDTH, height = HEIGHT;

		if (_type != Edge.SELF) {
			width = LONG_WIDTH;
			height = LONG_HEIGHT;
		}

		setSize(width, height);
		showDialog();
	}

	/**
	 *
	 *
	 * @return
	 */
	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public List<Option> getOptions() {
		LinkedList<Option> opts = new LinkedList<>();

		_edgeName = JUtils.textField((_edge != null) ? _edge.getName() : _theFrame.getMModel().getNewEdgeName(_type == Edge.INJECTIVE));

		opts.add(new Option("Edge name", _edgeName));

		if ((_edge == null) && (_type != Edge.SELF)) { // A new edge, but not a
														// self-referencing edge
			_edgeDirection = new JComboBox(new String[] { _source.getName() + " to " + _target.getName(), _target.getName() + " to " + _source.getName() });

			opts.add(new Option("Edge direction", _edgeDirection));
		}

		boolean partial = ((_type == Edge.SELF) || (_type == Edge.PARTIAL));

		_cascadeMode = new JComboBox();

		String cascadeTT = "This option affects how this edge is handled when exporting to a db.\n\n\"Cascade\" cause deletions in this table to trigger deletions of any rows in other tables that point to the row(s) being deleted.\n\n\"Restrict\" causes attempted deletions of referenced rows to fail.";

		if (partial) {
			_cascadeMode.addItem("Set null");

			cascadeTT = "This option affects how this edge is handled when exporting to a db.\n\n\"Cascade\" cause deletions in this table to trigger deletions of any rows in other tables that point to the row(s) being deleted.\n\n\"Restrict\" cause attempted deletions of referenced rows to fail.\n\n\"Set null\" causes references to be set to NULL when the targeted row is deleted.";
		}

		_cascadeMode.addItem("Restrict");
		_cascadeMode.addItem("Cascade");

		cascadeTT = JUtils.tooltip(cascadeTT);

		JLabel cascadeLabel = new JLabel("On deletion");

		cascadeLabel.setToolTipText(cascadeTT);
		_cascadeMode.setToolTipText(cascadeTT);

		if (_edge != null) {
			if (_edge.getCascading() == Cascade.CASCADE) {
				_cascadeMode.setSelectedIndex(partial ? 2 : 1);
			} else if (_edge.getCascading() == Cascade.RESTRICT) {
				_cascadeMode.setSelectedIndex(partial ? 1 : 0);
			} else {
				_cascadeMode.setSelectedIndex(0);
			}
		} else {
			if (partial) {
				Cascade defCascade = _theFrame.getMModel().getDefaultPartialCascading();

				// String cascade =
				// Easik.getInstance().getSettings().getProperty("sql_cascade_partial",
				// "set_null");
				_cascadeMode.setSelectedIndex((defCascade == Cascade.CASCADE) ? 2 : (defCascade == Cascade.RESTRICT) ? 1 : 0);
			} else {
				Cascade defCascade = _theFrame.getMModel().getDefaultCascading();

				// String cascade =
				// Easik.getInstance().getSettings().getProperty("sql_cascade",
				// "restrict");
				_cascadeMode.setSelectedIndex((defCascade == Cascade.CASCADE) ? 1 : 0);
			}
		}

		opts.add(new Option(cascadeLabel, _cascadeMode));

		return opts;
	}

	/**
	 *
	 *
	 * @return
	 */
	public Cascade getCascadeMode() {
		int index = _cascadeMode.getSelectedIndex();

		return (index == 0) ? (((_type == Edge.SELF) || (_type == Edge.PARTIAL)) ? Cascade.SET_NULL : Cascade.RESTRICT) : (index == 1) ? (((_type == Edge.SELF) || (_type == Edge.PARTIAL)) ? Cascade.RESTRICT : Cascade.CASCADE) : Cascade.SET_NULL;
	}

	/**
	 *
	 *
	 * @return
	 */
	@Override
	public String getName() {
		return _edgeName.getText();
	}

	/**
	 * Returns true if the direction is to be reversed. Only useful for new
	 * edges.
	 *
	 * @return
	 */
	public boolean isReversed() {
		return (_edgeDirection != null) ? _edgeDirection.getSelectedIndex() != 0 : false;
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
		else if (((_edge == null) || !s.equals(_edge.getName())) && _theFrame.getMModel().isEdgeNameUsed(s)) {
			JOptionPane.showMessageDialog(this, "Edge name \"" + s + "\" is already in use", "Error", JOptionPane.ERROR_MESSAGE);

			return false;
		}

		return true;
	}
}
