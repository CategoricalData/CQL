package easik.overview.vertex;

//~--- non-JDK imports --------------------------------------------------------

import easik.DocumentInfo;
import easik.overview.Overview;
import easik.ui.ViewFrame;
import easik.view.View;

/**
 * An entity node represents a table in a db. It has a name, attributes, and
 * unique keys. This class keeps track of all these elements.
 *
 * @author Rob Fletcher 2005
 * @author Kevin Green 2006
 * @version 2006-06-21 Kevin Green
 */
public class ViewNode extends OverviewVertex {
	/**
	 *    
	 */
	private static final long serialVersionUID = 7727430935371718629L;

	/** The view represented by this node */
	private ViewFrame _theFrame;

	/**
	 * Creates a new entity node with the name provided.
	 *
	 * @param nodeName
	 *            The name of the new node
	 * @param inFrame
	 */
	public ViewNode(String nodeName, ViewFrame inFrame) {
		super();

		_theFrame = inFrame;

		_theFrame.getMModel().getDocInfo().setName(nodeName);
		_theFrame.assignNode(this);

		_thumb = null;
	}

	/**
	 * Creates a new enity node with the name provided. Stores visual
	 * representation information.
	 *
	 * @param nodeName
	 *            Name of the new node
	 * @param x
	 *            X Coordinate of the new node
	 * @param y
	 *            Y Coordinate of the new node
	 * @param inFrame
	 */
	public ViewNode(String nodeName, int x, int y, ViewFrame inFrame) {
		super(nodeName, x, y);

		_theFrame = inFrame;

		_theFrame.getMModel().getDocInfo().setName(nodeName);
		_theFrame.assignNode(this);

		_thumb = null;
	}

	/**
	 * Accessor method for Sketch Frame represented by this node
	 *
	 * @return The View Frame represented by this node
	 */
	public ViewFrame getFrame() {
		return _theFrame;
	}

	/**
	 * Accessor method for the view represented by this mode. Alias for
	 * _theFrame.getMModel();
	 * 
	 * @return The view represented by this node.
	 */
	public View getMModel() {
		return _theFrame.getMModel();
	}

	/**
	 * Changes the ViewNode's name. Also updates the name in the Overview. The
	 * actual name, if it already exists, might have a number added and/or
	 * incremented.
	 *
	 * @param name
	 *            the new name (which might be incremented)
	 */
	@Override
	public void setName(String name) {
		// If name is already the same, do not try to update - avoids improper
		// incrementation
		if (_name != name) {
			Overview ov = _theFrame.getMModel().getOverview();
			String oldName = _name;

			// update node mapping in overview
			name = ov.viewRenamed(this, oldName, name);

			// update node name in its document info
			_theFrame.getMModel().getDocInfo().setName(name);

			// update name stored here
			_name = name;
		}
	}

	/**
	 * Refreshes the name stored locally to match what is in this sketch's
	 * document information
	 */
	@Override
	public void updateName() {
		DocumentInfo d = _theFrame.getMModel().getDocInfo();

		if (_name != d.getName()) {
			_name = d.getName();
		}
	}

	/**
	 * Getter method for the overview in which this node exists. Simply calls
	 * _theFrame.getMModel().getOverview().
	 * 
	 * @return The overview in which this node exists.
	 */
	public Overview getOverview() {
		return _theFrame.getMModel().getOverview();
	}
}
