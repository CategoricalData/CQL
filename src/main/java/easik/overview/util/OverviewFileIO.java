package easik.overview.util;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.io.File;

import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import easik.DocumentInfo;
import easik.EasikConstants;
import easik.model.edge.ModelEdge.Cascade;
import easik.overview.Overview;
import easik.overview.edge.ViewDefinitionEdge;
import easik.overview.vertex.SketchNode;
import easik.overview.vertex.ViewNode;
import easik.sketch.util.SketchFileIO;
import easik.view.View;
import easik.view.edge.InjectiveViewEdge;
import easik.view.edge.PartialViewEdge;
import easik.view.edge.View_Edge;
import easik.view.vertex.QueryNode;

/**
 * Here is a collection of static methods which are used to save and load
 * Sketches from XML.
 *
 * @author Rob Fletcher 2005
 * @author Kevin Green 2006
 * @author Vera Ranieri 2006
 * @version 2006-07-14 Vera Ranieri
 */
public class OverviewFileIO {
	/**
	 * Converts an overview to an XML file. Returns the success of the save.
	 *
	 * @param outputFile The file we will output to
	 * @param overview   The sketch we're reading to
	 * @return True if successful, false otherwise
	 */
	public static boolean overviewToXML(File outputFile, Overview overview) {
		Document overviewAsXML;

		try {
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = docBuilderFactory.newDocumentBuilder();

			overviewAsXML = db.newDocument();

			Element rootElement = overviewAsXML.createElement("easketch_overview");
			Element header = overviewAsXML.createElement("header");
			Element sketches = overviewAsXML.createElement("sketches");
			Element views = overviewAsXML.createElement("views");

			// Add Header info to document
			DocumentInfo d = overview.getDocInfo();
			Element name = overviewAsXML.createElement("title");

			name.appendChild(overviewAsXML.createTextNode(d.getName()));
			header.appendChild(name);

			for (String aut : d.getAuthors()) {
				Element author = overviewAsXML.createElement("author");

				author.appendChild(overviewAsXML.createTextNode(aut));
				header.appendChild(author);
			}

			Element desc = overviewAsXML.createElement("description");

			desc.appendChild(overviewAsXML.createTextNode(d.getDesc()));
			header.appendChild(desc);

			Element creationDate = overviewAsXML.createElement("creationDate");

			creationDate
					.appendChild(overviewAsXML.createTextNode(EasikConstants.XML_DATETIME.format(d.getCreationDate())));
			header.appendChild(creationDate);

			Element modDate = overviewAsXML.createElement("lastModificationDate");

			modDate.appendChild(
					overviewAsXML.createTextNode(EasikConstants.XML_DATETIME.format(d.getModificationDate())));
			header.appendChild(modDate);

			// Loop through sketches, add them to the document
			for (SketchNode currentSketch : overview.getSketches()) {
				if (currentSketch != null) {
					Element thisSketch = SketchFileIO.sketchToElement(overviewAsXML,
							currentSketch.getFrame().getMModel());

					thisSketch.setAttribute("name", currentSketch.toString());
					thisSketch.setAttribute("x", currentSketch.getX() + "");
					thisSketch.setAttribute("y", currentSketch.getY() + "");

					Cascade c = currentSketch.getFrame().getMModel().getDefaultCascading();
					Cascade cp = currentSketch.getFrame().getMModel().getDefaultPartialCascading();

					thisSketch.setAttribute("cascade", (c == Cascade.CASCADE) ? "cascade" : "restrict");
					thisSketch.setAttribute("partial-cascade",
							(cp == Cascade.CASCADE) ? "cascade" : (cp == Cascade.RESTRICT) ? "restrict" : "set_null");
					sketches.appendChild(thisSketch);
				}
			}

			for (ViewNode currentView : overview.getViews()) {
				if (currentView != null) {
					Element thisView = viewToElement(overviewAsXML, currentView.getFrame().getMModel());

					thisView.setAttribute("name", currentView.toString());
					thisView.setAttribute("x", currentView.getX() + "");
					thisView.setAttribute("y", currentView.getY() + "");

					ViewDefinitionEdge thisViewEdge = overview.getViewEdge(currentView.getFrame().getMModel());

					thisView.setAttribute("viewDefinitionEdge", thisViewEdge.getName());
					thisView.setAttribute("on_sketch", thisViewEdge.getTargetNode().getName());
					views.appendChild(thisView);
				}
			}

			// Add root elements to document
			overviewAsXML.appendChild(rootElement);
			rootElement.appendChild(header);
			rootElement.appendChild(sketches);
			rootElement.appendChild(views);
		} catch (Exception e) {
			e.printStackTrace();

			overviewAsXML = null;
		}

		outputXMLtoFile(outputFile, overviewAsXML);

		return true;
	}

	/**
	 * Output the document as XML
	 *
	 * @param outputFile output file
	 * @param xml        output XML
	 */
	private static void outputXMLtoFile(File outputFile, Document xml) {
		try {
			TransformerFactory tFactory = TransformerFactory.newInstance();
			Transformer transformer = tFactory.newTransformer();

			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.transform(new DOMSource(xml), new StreamResult(outputFile));
		} catch (Exception e) {
			System.err.println("Error exporting data.");
		}
	}

	/**
	 * Load an overview from XML.
	 *
	 * @param inputFile      the file from which the XML will be read
	 * @param outputOverview
	 * @return Returns the success or failure of the reading.
	 */
	public static boolean graphicalOverviewFromXML(File inputFile, Overview outputOverview) {
		OverviewHandler overviewHandler = new OverviewHandler(outputOverview.getFrame());

		if (!initializeOverviewHandlerFromXML(inputFile, overviewHandler)) {
			return false;
		}

		outputOverview.initializeFromData(overviewHandler.getSketches(), overviewHandler.getViews(),
				overviewHandler.getViewEdges(), overviewHandler.getDocumentInfo());
		outputOverview.getFrame().getInfoTreeUI().refreshTree();
		outputOverview.getFrame().setTreeName(overviewHandler.getDocumentInfo().getName());

		String warning = overviewHandler.get_warnings();
		SketchNode sk = overviewHandler.getSketches().values().iterator().next();
		if (warning != null && !warning.isEmpty()) {
			JOptionPane.showMessageDialog(sk.getFrame().getOverview(), warning, "Warning", JOptionPane.ERROR_MESSAGE);
		}
		return true;
	}

	/**
	 * Method to initialize an OverviewHandler for a supplied XML file
	 *
	 * @param inputFile       The XML file containing the sketch information
	 * @param overviewHandler
	 * @return true if SketchHandler was initialized, false if an exception
	 *         occurred.
	 *
	 * @since 2006-05-17 Vera Ranieri
	 */
	public static boolean initializeOverviewHandlerFromXML(File inputFile, OverviewHandler overviewHandler) {
		SAXParser parser;
		SAXParserFactory parseFactory = SAXParserFactory.newInstance();

		try {
			parser = parseFactory.newSAXParser();

			parser.parse(inputFile, overviewHandler);
		} catch (Exception e) {
			System.err.println("Could not open XML file for loading");
			e.printStackTrace();

			return false;
		}

		return true;
	}

	/**
	 * Converts a view to an Element
	 * 
	 * @param document The Document in which our information will be placed.
	 * @param view     The view we're reading
	 * @return All of the information needed to rebuild the view contained in an
	 *         Element. Returns null in the event that the element could not be
	 *         created.
	 * 
	 * @version 2014, Federico Mora
	 */
	public static Element viewToElement(Document document, View view) {
		try {
			Element rootElement = document.createElement("view");
			Element header = document.createElement("header");
			DocumentInfo d = view.getDocInfo();
			Element name = document.createElement("title");

			name.appendChild(document.createTextNode(d.getName()));
			header.appendChild(name);

			for (String aut : d.getAuthors()) {
				Element author = document.createElement("author");

				author.appendChild(document.createTextNode(aut));
				header.appendChild(author);
			}

			Element desc = document.createElement("description");

			desc.appendChild(document.createTextNode(d.getDesc()));
			header.appendChild(desc);

			Element creationDate = document.createElement("creationDate");

			creationDate.appendChild(document.createTextNode(EasikConstants.XML_DATETIME.format(d.getCreationDate())));
			header.appendChild(creationDate);

			Element modDate = document.createElement("lastModificationDate");

			modDate.appendChild(document.createTextNode(EasikConstants.XML_DATETIME.format(d.getModificationDate())));
			header.appendChild(modDate);
			rootElement.appendChild(header);

			Element queryNodes = document.createElement("queryNodes");

			// Loop through query nodes, add them to the document
			for (QueryNode currentNode : view.getEntities()) {
				if (currentNode == null) {
					continue;
				}

				Element thisNode = document.createElement("queryNode");

				thisNode.setAttribute("name", currentNode.toString());
				thisNode.setAttribute("x", currentNode.getX() + "");
				thisNode.setAttribute("y", currentNode.getY() + "");

				String query = currentNode.getQuery();

				thisNode.setAttribute("query", (query == null) ? "" : query);
				queryNodes.appendChild(thisNode);
			}

			rootElement.appendChild(queryNodes);

			Element edges = document.createElement("ViewEdges");

			for (View_Edge currentEdge : view.getEdges().values()) {
				Element thisEdge = document.createElement("ViewEdge");

				thisEdge.setAttribute("id", currentEdge.getName());
				thisEdge.setAttribute("source", currentEdge.getSourceQueryNode().getName());
				thisEdge.setAttribute("target", currentEdge.getTargetQueryNode().getName());
				thisEdge.setAttribute("type", (currentEdge instanceof PartialViewEdge) ? "partial"
						: (currentEdge instanceof InjectiveViewEdge) ? "injective" : "normal");
				thisEdge.setAttribute("cascade", (currentEdge.getCascading() == View_Edge.Cascade.SET_NULL) ? "set_null"
						: (currentEdge.getCascading() == View_Edge.Cascade.CASCADE) ? "cascade" : "restrict");
				edges.appendChild(thisEdge);
			}

			rootElement.appendChild(edges);

			return rootElement;
		} catch (Exception e) {
			return null;
		}
	}
}
