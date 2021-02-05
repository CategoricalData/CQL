package easik.overview.util;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.util.HashMap;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import easik.DocumentInfo;
import easik.Easik;
import easik.EasikConstants;
import easik.model.edge.ModelEdge.Cascade;
import easik.overview.edge.ViewDefinitionEdge;
import easik.overview.vertex.SketchNode;
import easik.overview.vertex.ViewNode;
import easik.sketch.Sketch;
import easik.sketch.edge.SketchEdge;
import easik.sketch.util.SketchFileIO;
import easik.sketch.util.SketchHandler;
import easik.ui.ApplicationFrame;
import easik.ui.SketchFrame;
import easik.ui.ViewFrame;
import easik.view.edge.InjectiveViewEdge;
import easik.view.edge.NormalViewEdge;
import easik.view.edge.PartialViewEdge;
import easik.view.edge.View_Edge;
import easik.view.util.QueryException;
import easik.view.vertex.QueryNode;

/**
 * The OverviewHandler is the overloaded handler for reading the sketches in
 * from XML. There is very little error checking in here to deal with bad XML.
 *
 * TODO: Implement schema checking
 *
 * @author Rob Fletcher 2005
 * @author Kevin Green 2006
 * @author Vera Ranieri 2006
 * @version 2006-07-14 Vera Ranieri
 */
public class OverviewHandler extends DefaultHandler {
	/** The name of the current sketch */
	private String _currNode;

	/** The node who's sketch is currently being parsed */
	private SketchNode _newSketchNode;

	/** The node who's View is currently being parsed */
	private ViewNode _newViewNode;

	/** The document information for the overview */
	private DocumentInfo _overviewDocInfo;

	/** Flag set if we are currently parsing a view section of XML */
	private boolean _parsingView;

	/** Map of queryNodes for the currently being parsed view */
	private HashMap<String, QueryNode> _queryNodes;

	/** The sketch nodes of the overview indexed by name */
	private HashMap<String, SketchNode> _sketchNodes;

	/**
	 * The sketch hadler being called when parsing a sketch section of XML - set to
	 * null if parsing anything else
	 */
	private SketchHandler _sketchParser;

	/** The frame the new overview will be placed in */
	private ApplicationFrame _theFrame;

	/** The document info for the view currently being parsed */
	private DocumentInfo _viewDocInfo;

	/** The view edges indexed by name */
	private HashMap<String, ViewDefinitionEdge> _viewDefEdge;

	/** The view nodes of the overview indexed by name */
	private HashMap<String, ViewNode> _viewNodes;

	/** The edges of the view, indexed by name */
	private HashMap<String, View_Edge> _viewEdges;

	/** The warings produced by loading */
	private String _warnings = "";

	/**
	 * Default Constructor
	 *
	 * @param inFrame
	 */
	public OverviewHandler(ApplicationFrame inFrame) {
		_sketchNodes = new HashMap<>();
		_viewNodes = new HashMap<>();
		_viewDefEdge = new HashMap<>();
		_viewEdges = new HashMap<>();
		_overviewDocInfo = new DocumentInfo(inFrame);
		_theFrame = inFrame;
	}

	/**
	 * Returns map of sketches, indexed by name.
	 * 
	 * @return Map of sketches
	 */
	public Map<String, SketchNode> getSketches() {
		return _sketchNodes;
	}

	/**
	 * Returns map of views, indexed by name.
	 * 
	 * @return Map of views
	 */
	public Map<String, ViewNode> getViews() {
		return _viewNodes;
	}

	/**
	 * Returns map of view edges, indexed by name.
	 * 
	 * @return Map of view edges
	 */
	public Map<String, ViewDefinitionEdge> getViewEdges() {
		return _viewDefEdge;
	}

	/**
	 * Get the document information of the current sketch
	 * 
	 * @return The DocumentInfo object associated with this sketch
	 */
	public DocumentInfo getDocumentInfo() {
		return _overviewDocInfo;
	}

	/**
	 * Overloaded method that is called any time the start of an element is found
	 * 
	 * @param namespace
	 * @see org.xml.sax.helpers.DefaultHandler
	 * @param localName
	 * @see org.xml.sax.helpers.DefaultHandler
	 * @param qName
	 * @see org.xml.sax.helpers.DefaultHandler
	 * @param atts
	 * @see org.xml.sax.helpers.DefaultHandler
	 */
	@Override
	public void startElement(String namespace, String localName, String qName, Attributes atts) {
		_currNode = qName;

		// We have an instance of a sketch handler, so let it deal with current
		// call
		if (_sketchParser != null) {
			_sketchParser.startElement(namespace, localName, qName, atts);
		}

		// Initialize a sketch handler, and note this sketch's attributes
		else if (qName.equals("easketch")) {
			String sketchName = atts.getValue("name");
			int sketchX = Integer.parseInt(atts.getValue("x"));
			int sketchY = Integer.parseInt(atts.getValue("y"));
			String cascade = atts.getValue("cascade"), pCascade = atts.getValue("partial-cascade");

			if (cascade == null) {
				cascade = Easik.getInstance().getSettings().getProperty("sql_cascade", "restrict");
			}

			if (pCascade == null) {
				pCascade = Easik.getInstance().getSettings().getProperty("sql_cascade_partial", "set_null");
			}

			Cascade c = cascade.equals("cascade") ? Cascade.CASCADE : Cascade.RESTRICT;
			Cascade cp = pCascade.equals("cascade") ? Cascade.CASCADE
					: pCascade.equals("restrict") ? Cascade.RESTRICT : Cascade.SET_NULL;

			_sketchParser = SketchFileIO.getNewSketchHandler(_theFrame.getOverview());

			SketchFrame frame = _sketchParser.getFrame();
			Sketch sketch = frame.getMModel();

			sketch.setDefaultCascading(c);
			sketch.setDefaultPartialCascading(cp);

			_newSketchNode = new SketchNode(sketchName, sketchX, sketchY, frame);

			_sketchNodes.put(sketchName, _newSketchNode);
		}

		// Flag that we are parsing for views so that the view document infos
		// know not to override the overview's
		else if (qName.equals("view")) {
			_parsingView = true;
			_queryNodes = new HashMap<>();

			String viewName = atts.getValue("name");
			int x = Integer.parseInt(atts.getValue("x"));
			int y = Integer.parseInt(atts.getValue("y"));
			String edgeLabel = atts.getValue("viewDefinitionEdge");
			String sketchName = atts.getValue("on_sketch");
			ViewFrame viewFrame = new ViewFrame(_theFrame.getOverview(),
					_sketchNodes.get(sketchName).getFrame().getMModel());

			_newViewNode = new ViewNode(viewName, x, y, viewFrame);

			_viewDefEdge.put(edgeLabel, new ViewDefinitionEdge(_newViewNode, _sketchNodes.get(sketchName), edgeLabel));

			_viewDocInfo = new DocumentInfo(viewFrame);

			_viewNodes.put(viewName, _newViewNode);

			_sketchNodes.get(sketchName).getFrame().getMModel().addView(_newViewNode);

		} else if (qName.equals("queryNode")) {
			String name = atts.getValue("name");
			int x = Integer.parseInt(atts.getValue("x"));
			int y = Integer.parseInt(atts.getValue("y"));
			String query = atts.getValue("query");

			// this could throw an exception if the xml file has been changed
			// outside of EASIK
			// EASIK wont let you create queryNodes that will throw this
			// exception so they can't be saved
			try {
				_queryNodes.put(name, new QueryNode(name, x, y, _newViewNode.getFrame().getMModel(), query));
			} catch (QueryException e) {
				e.printStackTrace();
			}
		} else if (qName.equals("View_Edge")) {
			View_Edge newEdge;
			String edgeType = atts.getValue("type");

			QueryNode source = _queryNodes.get(atts.getValue("source"));
			QueryNode target = _queryNodes.get(atts.getValue("target"));
			String id = atts.getValue("id");
			String cascadeAtt = atts.getValue("cascade");

			if (cascadeAtt == null) {
				// This is from an export before Easik had per-edge cascading
				// (in other words, before r583)
				// We use the global preferences for cascading
				String key = "sql_cascade", def = "restrict";

				if (edgeType.equals("partial")) {
					key = "sql_cascade_partial";
					def = "set_null";
				}

				cascadeAtt = Easik.getInstance().getSettings().getProperty(key, def);
			}

			@SuppressWarnings("unused")
			SketchEdge.Cascade cascade = cascadeAtt.equals("set_null") ? SketchEdge.Cascade.SET_NULL
					: cascadeAtt.equals("cascade") ? SketchEdge.Cascade.CASCADE : SketchEdge.Cascade.RESTRICT;

			if (edgeType.equals("injective")) {
				newEdge = new InjectiveViewEdge(source, target, id);
			} else if (edgeType.equals("partial")) {
				newEdge = new PartialViewEdge(source, target, id);
			} else {
				newEdge = new NormalViewEdge(source, target, id);
			}

			_viewEdges.put(id, newEdge);
		}
	}

	/**
	 * Overloaded method that is called any time the end of an element is found
	 * 
	 * @param uri
	 * @see org.xml.sax.helpers.DefaultHandler
	 * @param localName
	 * @see org.xml.sax.helpers.DefaultHandler
	 * @param qName
	 * @see org.xml.sax.helpers.DefaultHandler
	 */
	@Override
	public void endElement(String uri, String localName, String qName) {
		_currNode = null;

		if (qName.equals("easketch")) { // grab what the current sketch handler
										// instance parsed
			_warnings = get_warnings() + _newSketchNode.getFrame().getMModel().initializeFromData(
					_sketchParser.getSyncLock(), _sketchParser.getEntities(), _sketchParser.getEdges(),
					_sketchParser.getConstraints(), _sketchParser.getDocumentInfo(), _sketchParser.getConnParams());
			_newSketchNode.getFrame().getMModel().updateThumb();

			_sketchParser = null;
			_newSketchNode = null;
		} else if (_sketchParser != null) { // we are in the middle of parsing a
											// sketch - hand off to sketch
											// handler
			_sketchParser.endElement(uri, localName, qName);
		} else if (qName.equals("view")) {
			_parsingView = false;

			_newViewNode.getFrame().getMModel().initializeFromData(_queryNodes, _viewDocInfo, _viewEdges);
			_newViewNode.getFrame().getMModel().updateThumb();

			_newViewNode = null;
			_viewDocInfo = null;
			_queryNodes = null;
		}
	}

	/**
	 * @see org.xml.sax.helpers.DefaultHandler
	 * @param ch
	 * @see org.xml.sax.helpers.DefaultHandler
	 * @param start
	 * @see org.xml.sax.helpers.DefaultHandler
	 * @param length
	 * @see org.xml.sax.helpers.DefaultHandler
	 */
	@Override
	public void characters(char[] ch, int start, int length) {
		if (_currNode == null) {
			return;
		}

		// we're parsing a sketch, so hand it off to the current sketch handler
		if ((_sketchParser != null)
				&& (_currNode.equals("title") || _currNode.equals("author") || _currNode.equals("description")
						|| _currNode.equals("creationDate") || _currNode.equals("lastModificationDate"))) {
			_sketchParser.characters(ch, start, length);

			return;
		}

		String value = new String(ch, start, length);

		if (_parsingView) {
			if (_currNode.equals("title")) {
				_viewDocInfo.setName(value);
			} else if (_currNode.equals("author")) {
				_viewDocInfo.addAuthor(value);
			} else if (_currNode.equals("description")) {
				_viewDocInfo.setDesc(value);
			} else if (_currNode.equals("creationDate") || _currNode.equals("lastModificationDate")) {
				java.util.Date date;

				try {
					date = EasikConstants.XML_DATETIME.parse(value);
				} catch (java.text.ParseException pe) {
					date = null;
				}

				if (_currNode.equals("creationDate")) {
					_viewDocInfo.setCreationDate(date);
				} else {
					_viewDocInfo.setModificationDate(date);
				}
			}

			return;
		}

		// add to the overview doc info
		if (_currNode.equals("title")) {
			_overviewDocInfo.setName(value);
		} else if (_currNode.equals("author")) {
			_overviewDocInfo.addAuthor(value);
		} else if (_currNode.equals("description")) {
			_overviewDocInfo.setDesc(value);
		} else if (_currNode.equals("creationDate") || _currNode.equals("lastModificationDate")) {
			java.util.Date date;

			try {
				date = EasikConstants.XML_DATETIME.parse(value);
			} catch (java.text.ParseException pe) {
				date = null;
			}

			if (_currNode.equals("creationDate")) {
				_overviewDocInfo.setCreationDate(date);
			} else {
				_overviewDocInfo.setModificationDate(date);
			}
		}
	}

	/**
	 * @return the _warnings
	 */
	public String get_warnings() {
		return _warnings;
	}
}
