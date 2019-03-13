package easik.overview.util.graph;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

import org.jgraph.event.GraphModelEvent;
import org.jgraph.event.GraphModelListener;
import org.jgraph.graph.AttributeMap;
import org.jgraph.graph.GraphCell;
import org.jgraph.graph.GraphConstants;

import easik.graph.EasikGraphModel;
import easik.overview.Overview;
import easik.overview.edge.ViewDefinitionEdge;
import easik.overview.vertex.OverviewVertex;
import easik.overview.vertex.SketchNode;
import easik.overview.vertex.ViewNode;

/**
 *
 *
 * @version 12/09/12
 * @author Christian Fiddick
 */
public class OverviewGraphModel extends EasikGraphModel {
	// The Overview this model is for

	/**
	 *    
	 */
	private static final long serialVersionUID = 7379128426572642639L;

	/**  */
	private Overview _overview;

	/**
	 * Constructs a new overview graph model, which overrides cell attributes to
	 * display things appropriate for Easik.
	 *
	 * @param inOverview
	 */
	public OverviewGraphModel(Overview inOverview) {
		super();

		_overview = inOverview;

		addGraphModelListener(new GraphModelListener() {
			@Override
			public void graphChanged(GraphModelEvent e) {
				_overview.checkDirty();
			}
		});
	}

	/**
	 * Overridden method to get cell attributes; we make sure the appropriate
	 * attributes are applied to the Easik objects before returning them.
	 *
	 * @see DefaultGraphModel.getAttributes(Object)
	 *
	 * @param o
	 *
	 * @return
	 */
	@Override
	public AttributeMap getAttributes(Object o) {
		if (o instanceof GraphCell) {
			GraphCell cell = (GraphCell) o;
			AttributeMap attribs = cell.getAttributes();
			AttributeMap easikAttribs = null;

			if (cell instanceof SketchNode) {
				easikAttribs = sketchAttributes((SketchNode) cell);
			} else if (cell instanceof ViewNode) {
				easikAttribs = viewAttributes((ViewNode) cell);
			} else if (cell instanceof ViewDefinitionEdge) {
				easikAttribs = viewEdgeAttributes((ViewDefinitionEdge) cell);

				// easikAttribs =
				// easik.sketch.util.graph.SketchGraphModel.normalEdgeAttributes();
			}

			if (easikAttribs != null) {
				if (_overview.isCellSelected(cell)) {
					Color selColor = getColor("selection");
					float lineWidth = getWidth("selection", 3);
					int borderWidth = 1;
					Border currentBorder = GraphConstants.getBorder(easikAttribs);

					if (currentBorder instanceof LineBorder) {
						borderWidth = ((LineBorder) currentBorder).getThickness();
					}

					GraphConstants.setBorder(easikAttribs, BorderFactory.createLineBorder(selColor, borderWidth));
					GraphConstants.setForeground(easikAttribs, selColor);
					GraphConstants.setLineColor(easikAttribs, selColor);
					GraphConstants.setLineWidth(easikAttribs, lineWidth);
				}

				if (attribs == null) {
					cell.setAttributes(easikAttribs);

					attribs = easikAttribs;
				} else {
					attribs.applyMap(easikAttribs);
				}

				return attribs;
			}
		}

		return super.getAttributes(o);
	}

	/**
	 *
	 *
	 * @param vertex
	 *
	 * @return
	 */
	private static AttributeMap commonVertexAttributes(OverviewVertex vertex) {
		AttributeMap map = new AttributeMap();

		GraphConstants.setAutoSize(map, true);
		GraphConstants.setInset(map, 5);
		GraphConstants.setFont(map, GraphConstants.DEFAULTFONT.deriveFont(Font.BOLD, 12));
		GraphConstants.setOpaque(map, true);

		ImageIcon icon = vertex.getThumbnail();

		if (icon != null) {
			GraphConstants.setIcon(map, vertex.getThumbnail());
		} else {
			GraphConstants.setIcon(map, new ImageIcon());
		}

		GraphConstants.setVerticalTextPosition(map, SwingConstants.TOP);

		return map;
	}

	/**
	 * Returns an attribute map for SketchNodes
	 *
	 * @param node
	 * @return The attribute mapping
	 */
	private static AttributeMap sketchAttributes(SketchNode node) {
		AttributeMap map = commonVertexAttributes(node);

		GraphConstants.setBorder(map, BorderFactory.createLineBorder(getColor("overview_sketch_border"), getIntWidth("overview_sketch_border", 2)));
		GraphConstants.setBackground(map, getColor("overview_sketch_bg"));
		GraphConstants.setForeground(map, getColor("overview_sketch_fg"));

		return map;
	}

	/**
	 * Returns an attribute map for Overview views
	 *
	 * @param node
	 * @return The attribute mapping
	 */
	private static AttributeMap viewAttributes(ViewNode node) {
		AttributeMap map = commonVertexAttributes(node);

		GraphConstants.setBorder(map, BorderFactory.createLineBorder(getColor("overview_view_border"), getIntWidth("overview_view_border", 2)));
		GraphConstants.setBackground(map, getColor("overview_view_bg"));
		GraphConstants.setForeground(map, getColor("overview_view_fg"));

		return map;
	}

	/**
	 * Returns an attribute map for Overview view edges
	 *
	 *
	 * @param edge
	 * @return a map of attributes to be used for a sketchnode
	 */
	public static AttributeMap viewEdgeAttributes(ViewDefinitionEdge edge) {
		AttributeMap map = commonEdgeAttributes();

		GraphConstants.setLineEnd(map, GraphConstants.ARROW_TECHNICAL);
		GraphConstants.setEndFill(map, true);
		GraphConstants.setEndSize(map, 10);

		Color color = getColor("edge_overview_view");

		GraphConstants.setLineColor(map, color);
		GraphConstants.setForeground(map, color);
		GraphConstants.setLineWidth(map, getWidth("edge_overview_view", 1.5));

		return map;
	}

	/**
	 *
	 */
	@Override
	public void clearSelection() {
		_overview.clearSelection();
	}

	/**
	 *
	 */
	@Override
	public void setDirty() {
		_overview.setDirty(true);
	}
}
