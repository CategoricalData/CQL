package easik.overview.vertex;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import javax.swing.tree.DefaultMutableTreeNode;

import easik.overview.Overview;
import easik.sketch.Sketch;
import easik.ui.SketchFrame;

/**
 * An entity node represents a table in a db. It has a name, attributes, and
 * unique keys. This class keeps track of all these elements.
 *
 * @author Rob Fletcher 2005
 * @author Kevin Green 2006
 * @version 2006-06-21 Kevin Green
 */
public class SketchNode extends OverviewVertex {
	/**
	 *    
	 */
	private static final long serialVersionUID = -72128890599547261L;

	/** The sketch represented by this node */
	private SketchFrame _theFrame;

	/**
	 * Creates a new entity node with the name provided.
	 *
	 * @param nodeName The name of the new node
	 * @param inFrame
	 */
	public SketchNode(String nodeName, SketchFrame inFrame) {
		this(nodeName, 0, 0, inFrame);
	}

	/**
	 * Creates a new enity node with the name provided. Stores visual representation
	 * information.
	 *
	 * @param nodeName Name of the new node
	 * @param x        X Coordinate of the new node
	 * @param y        Y Coordinate of the new node
	 * @param inFrame
	 */
	public SketchNode(String nodeName, int x, int y, SketchFrame inFrame) {
		super(null, x, y);

		_theFrame = inFrame;

		setName(nodeName);
		_theFrame.assignNode(this);

		_thumb = null;
	}

	/**
	 * Accessor method for Sketch Frame represented by this node
	 *
	 * @return The Sketch Frame represented by this node
	 */
	public SketchFrame getFrame() {
		return _theFrame;
	}

	/**
	 * Alias for getFrame().getMModel()
	 * 
	 * @return The Sketch represented by this node
	 */
	public Sketch getMModel() {
		return getFrame().getMModel();
	}

	/**
	 * Changes the ViewNode's name. Also updates the name in the Overview. The
	 * actual name, if it already exists, might have a number added and/or
	 * incremented.
	 *
	 * @param name the new name (which might be incremented)
	 */
	@Override
	public void setName(String name) {
		// If name is already the same, do not try to update - avoids improper
		// incrementation
		if ((_name == null) || !_name.equals(name)) {
			Overview ov = _theFrame.getMModel().getOverview();
			String oldName = _name;

			// update node mapping in overview
			name = ov.sketchRenamed(this, oldName, name);

			// update node name in its document info
			_theFrame.getMModel().getDocInfo().setName(name);

			// update name stored here
			_name = name;
		}
	}

	/**
	 * Refreshes the name stored locally to match what is in this sketch's document
	 * information
	 */
	@Override
	public void updateName() {
		setName(_theFrame.getMModel().getDocInfo().getName());
	}

	/**
	 *
	 *
	 * @return
	 */
	@Override
	public DefaultMutableTreeNode getTreeNode() {
		if (_treeNode == null) {
			_treeNode = new DefaultMutableTreeNode(this);
		}

		return _treeNode;
	}
}
