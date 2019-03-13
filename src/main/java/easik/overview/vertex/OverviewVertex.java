package easik.overview.vertex;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.Image;
import java.awt.geom.Rectangle2D;

import javax.swing.ImageIcon;
import javax.swing.tree.DefaultMutableTreeNode;

import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.DefaultPort;
import org.jgraph.graph.GraphConstants;

/**
 * The superclass of all nodes on the sketch, has a name and a coordinate.
 *
 * @author Rob Fletcher 2005
 */
public abstract class OverviewVertex extends DefaultGraphCell {
	/**
	 *    
	 */
	private static final long serialVersionUID = -7104805985175066197L;

	/** This port's node to which edges connect */
	protected DefaultPort _port = new DefaultPort();

	/**
	 * The x and y coordinates we wish this vertex to be initially rendered at
	 */
	protected int _initX, _initY;

	/** The name of this node */
	protected String _name;

	/** A thumbnail of this node's contents */
	protected ImageIcon _thumb;

	/** The tree node for the overview tree representing this vertex */
	protected DefaultMutableTreeNode _treeNode;

	/**
	 * Create a blank sketch vertex, no name, and located at (0, 0)
	 *
	 */
	public OverviewVertex() {
		this("", 0, 0);
	}

	/**
	 * Create a vertex with basic attributes
	 * 
	 * @param name
	 *            Label
	 * @param x
	 *            X coordinate
	 * @param y
	 *            Y coordinate
	 */
	public OverviewVertex(String name, int x, int y) {
		_name = name;
		_initX = x;
		_initY = y;

		add(_port);
		setUserObject(this);
	}

	/**
	 * Must be implemented by subclasses if there is oppertunity for this
	 * vertex's name to be changed elsewhere, leaving the locally stored name
	 * outdated.
	 */
	public abstract void updateName();

	/**
	 * Must be implemented by subclasses if name changes need to be reflected
	 * elsewhere.
	 *
	 * @param name
	 */
	public abstract void setName(String name);

	/**
	 * To string method returns the name
	 * 
	 * @return Current name
	 */
	@Override
	public String toString() {
		return _name;
	}

	/**
	 * Accessor for the name field
	 * 
	 * @return the name
	 */
	public String getName() {
		return _name;
	}

	/**
	 * Accessor for the X coordinate
	 * 
	 * @return The x coordinate
	 */
	public int getX() {
		Rectangle2D bounds = GraphConstants.getBounds(getAttributes());

		if (bounds != null) {
			return (int) bounds.getX();
		} 
			return _initX;
		}
	

	/**
	 * Accessor for the Y Coordinate
	 * 
	 * @return The Y Coordinate
	 */
	public int getY() {
		Rectangle2D bounds = GraphConstants.getBounds(getAttributes());

		if (bounds != null) {
			return (int) bounds.getY();
		} 
			return _initY;
		
	}

	/**
	 *
	 *
	 * @return
	 */
	public int getLastKnownX() {
		return _initX;
	}

	/**
	 *
	 *
	 * @return
	 */
	public int getLastKnownY() {
		return _initY;
	}

	/**
	 *
	 */
	public void savePosition() {
		_initX = getX();
		_initY = getY();
	}

	/**
	 * Gets the current thumbnail as an ImageIcon of the sketch this node
	 * represents
	 *
	 * @return
	 */
	public ImageIcon getThumbnail() {
		return _thumb;
	}

	/**
	 * Sets the current thumbnail to be rendered on this node
	 *
	 * @param inThumb
	 *            the Image to use for the thumbnail
	 */
	public void setThumbnail(Image inThumb) {
		_thumb = (inThumb == null) ? null : new ImageIcon(inThumb);
	}

	/**
	 * Returns this vertex wrapped in a DefaultMutableTreeNode object. This
	 * method should be overridden by children classes
	 *
	 * @return
	 */
	public DefaultMutableTreeNode getTreeNode() {
		if (_treeNode == null) {
			_treeNode = new DefaultMutableTreeNode(this);
		}

		return _treeNode;
	}

	/**
	 *
	 *
	 * @return
	 */
	public DefaultPort getPort() {
		return _port;
	}
}
