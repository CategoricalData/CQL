package easik.sketch.util;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.io.File;
import java.util.Map;

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
import easik.database.types.EasikType;
import easik.model.attribute.EntityAttribute;
import easik.model.constraint.LimitConstraint;
import easik.model.constraint.ModelConstraint;
import easik.model.keys.UniqueIndexable;
import easik.model.keys.UniqueKey;
import easik.model.path.ModelPath;
import easik.overview.Overview;
import easik.sketch.Sketch;
import easik.sketch.edge.InjectiveEdge;
import easik.sketch.edge.PartialEdge;
import easik.sketch.edge.SketchEdge;
import easik.sketch.util.graph.SketchGraphModel;
import easik.sketch.vertex.EntityNode;
import easik.ui.SketchFrame;

//import org.w3c.dom.Node;

/**
 * Here is a collection of static methods which are used to save and load
 * Sketches from XML.
 *
 * @author Rob Fletcher 2005
 * @author Kevin Green 2006
 * @author Vera Ranieri 2006
 * @version 2006-07-14 Vera Ranieri
 */
public class SketchFileIO {
	/**
	 * Converts a sketch to XML and sends to file.
	 *
	 * @param outputFile The target file for the sketch to be saved in
	 * @param inSketch   The sketch to be saved
	 * @return the success of the save
	 */
	public static boolean sketchToXML(File outputFile, Sketch inSketch) {
		Document sketchAsXML;
		Element sketchAsElement;
		@SuppressWarnings("unused")
		Element rootElement;

		try {
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = docBuilderFactory.newDocumentBuilder();

			sketchAsXML = db.newDocument();
			sketchAsElement = sketchToElement(sketchAsXML, inSketch);

			sketchAsXML.appendChild(sketchAsElement);
		} catch (Exception e) {
			e.printStackTrace();

			return false;
		}

		outputXMLtoFile(outputFile, sketchAsXML);

		return true;
	}

	/**
	 * Converts a sketch to an Element.
	 *
	 * @param document The Document in which our information is being placed.
	 * @param sketch
	 * @return All of the information needed to rebuild our sketch containted in an
	 *         Element. Returns null in the event that the element could not be
	 *         created.
	 */
	public static Element sketchToElement(Document document, Sketch sketch) {
		try {
			Element rootElement = document.createElement("easketch");
			Element header = document.createElement("header");

			// Add Header info to document
			DocumentInfo d = sketch.getDocInfo();
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

			Map<String, String> connParams = sketch.getConnectionParams();

			for (String key : connParams.keySet()) {
				Element connParam = document.createElement("connectionParam");

				connParam.setAttribute("name", key);
				connParam.setAttribute("value", connParams.get(key));
				header.appendChild(connParam);
			}

			if (sketch.isSynced()) {
				header.appendChild(document.createElement("synchronized"));
			}

			rootElement.appendChild(header);

			Element entities = document.createElement("entities");

			// Loop through entities, add them to the document
			for (EntityNode currentEntity : sketch.getEntities()) {
				if (currentEntity == null) {
					continue;
				}

				Element thisEntity = document.createElement("entity");

				thisEntity.setAttribute("name", currentEntity.toString());
				thisEntity.setAttribute("x", currentEntity.getX() + "");
				thisEntity.setAttribute("y", currentEntity.getY() + "");
				entities.appendChild(thisEntity);

				// Loop through attributes, add them to the document
				for (EntityAttribute<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> curAttribute : currentEntity
						.getEntityAttributes()) {
					Element attributeElmt = document.createElement("attribute");

					attributeElmt.setAttribute("name", curAttribute.getName());

					EasikType attType = curAttribute.getType();

					attributeElmt.setAttribute("attributeTypeClass", attType.getClass().getName());

					Map<String, String> typeAttribs = attType.attributes();

					for (String key : typeAttribs.keySet()) {
						attributeElmt.setAttribute(key, typeAttribs.get(key));
					}

					thisEntity.appendChild(attributeElmt);
				}

				// We can't go through unique keys yet: they have to come
				// *after* edges
			}

			rootElement.appendChild(entities);

			Element edges = document.createElement("edges");

			for (SketchEdge currentEdge : sketch.getEdges().values()) {
				Element thisEdge = document.createElement("edge");

				thisEdge.setAttribute("id", currentEdge.getName());
				thisEdge.setAttribute("source", currentEdge.getSourceEntity().getName());
				thisEdge.setAttribute("target", currentEdge.getTargetEntity().getName());
				thisEdge.setAttribute("type", (currentEdge instanceof PartialEdge) ? "partial"
						: (currentEdge instanceof InjectiveEdge) ? "injective" : "normal");
				thisEdge.setAttribute("cascade",
						(currentEdge.getCascading() == SketchEdge.Cascade.SET_NULL) ? "set_null"
								: (currentEdge.getCascading() == SketchEdge.Cascade.CASCADE) ? "cascade" : "restrict");
				edges.appendChild(thisEdge);
			}

			rootElement.appendChild(edges);

			Element keys = document.createElement("keys");

			// Loop through unique keys for every node, add them to the document
			for (EntityNode currentEntity : sketch.getEntities()) {
				for (UniqueKey<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> curKey : currentEntity
						.getUniqueKeys()) {
					Element uniqueKeyElmt = document.createElement("uniqueKey");

					uniqueKeyElmt.setAttribute("name", curKey.getKeyName());
					uniqueKeyElmt.setAttribute("noderef", currentEntity.toString());
					keys.appendChild(uniqueKeyElmt);

					for (UniqueIndexable curElem : curKey.getElements()) {
						if (curElem instanceof EntityAttribute) {
							Element attributeElmt = document.createElement("attref");

							attributeElmt.setAttribute("name", curElem.getName());
							uniqueKeyElmt.appendChild(attributeElmt);
						} else if (curElem instanceof SketchEdge) {
							Element edgeElmt = document.createElement("edgekeyref");

							edgeElmt.setAttribute("id", curElem.getName());
							uniqueKeyElmt.appendChild(edgeElmt);
						} else {
							System.err.println("Unknown unique key item encountered: element '" + curElem.getName()
									+ "' is neither EntityAttribute nor SketchEdge");
						}
					}
				}
			}

			rootElement.appendChild(keys);

			Element constraints = document.createElement("constraints");

			// Now add the constraints
			for (ModelConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> curConstraint : sketch
					.getConstraints().values()) {
				Element thisConstraint = document.createElement(curConstraint.getType());

				thisConstraint.setAttribute("x", curConstraint.getX() + "");
				thisConstraint.setAttribute("y", curConstraint.getY() + "");
				thisConstraint.setAttribute("isVisible", curConstraint.isVisible() ? "true" : "false");

				if (curConstraint instanceof LimitConstraint) {
					LimitConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> lc = (LimitConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge>) curConstraint;

					// TODO A better way? really long
					// cone - AB
					Element pathElem = document.createElement("path");

					pathElem.setAttribute("domain", lc.getCone().AB.getDomain().getName());
					pathElem.setAttribute("codomain", lc.getCone().AB.getCoDomain().getName());

					for (SketchEdge edge : lc.getCone().AB.getEdges()) {
						Element edgeElem = document.createElement("edgeref");

						edgeElem.setAttribute("id", edge.getName());
						pathElem.appendChild(edgeElem);
					}

					thisConstraint.appendChild(pathElem);

					// cone - BC
					pathElem = document.createElement("path");

					pathElem.setAttribute("domain", lc.getCone().BC.getDomain().getName());
					pathElem.setAttribute("codomain", lc.getCone().BC.getCoDomain().getName());

					for (SketchEdge edge : lc.getCone().BC.getEdges()) {
						Element edgeElem = document.createElement("edgeref");

						edgeElem.setAttribute("id", edge.getName());
						pathElem.appendChild(edgeElem);
					}

					thisConstraint.appendChild(pathElem);

					// cone - AC
					pathElem = document.createElement("path");

					pathElem.setAttribute("domain", lc.getCone().AC.getDomain().getName());
					pathElem.setAttribute("codomain", lc.getCone().AC.getCoDomain().getName());

					for (SketchEdge edge : lc.getCone().AC.getEdges()) {
						Element edgeElem = document.createElement("edgeref");

						edgeElem.setAttribute("id", edge.getName());
						pathElem.appendChild(edgeElem);
					}

					thisConstraint.appendChild(pathElem);

					// limit cone 1 - AB
					pathElem = document.createElement("path");

					pathElem.setAttribute("domain", lc.getLimitCone1().AB.getDomain().getName());
					pathElem.setAttribute("codomain", lc.getLimitCone1().AB.getCoDomain().getName());

					for (SketchEdge edge : lc.getLimitCone1().AB.getEdges()) {
						Element edgeElem = document.createElement("edgeref");

						edgeElem.setAttribute("id", edge.getName());
						pathElem.appendChild(edgeElem);
					}

					thisConstraint.appendChild(pathElem);

					// limit cone 1 - BC
					pathElem = document.createElement("path");

					pathElem.setAttribute("domain", lc.getLimitCone1().BC.getDomain().getName());
					pathElem.setAttribute("codomain", lc.getLimitCone1().BC.getCoDomain().getName());

					for (SketchEdge edge : lc.getLimitCone1().BC.getEdges()) {
						Element edgeElem = document.createElement("edgeref");

						edgeElem.setAttribute("id", edge.getName());
						pathElem.appendChild(edgeElem);
					}

					thisConstraint.appendChild(pathElem);

					// limit cone 1 - AC
					pathElem = document.createElement("path");

					pathElem.setAttribute("domain", lc.getLimitCone1().AC.getDomain().getName());
					pathElem.setAttribute("codomain", lc.getLimitCone1().AC.getCoDomain().getName());

					for (SketchEdge edge : lc.getLimitCone1().AC.getEdges()) {
						Element edgeElem = document.createElement("edgeref");

						edgeElem.setAttribute("id", edge.getName());
						pathElem.appendChild(edgeElem);
					}

					thisConstraint.appendChild(pathElem);

					// limit cone 2 - AB
					pathElem = document.createElement("path");

					pathElem.setAttribute("domain", lc.getLimitCone2().AB.getDomain().getName());
					pathElem.setAttribute("codomain", lc.getLimitCone2().AB.getCoDomain().getName());

					for (SketchEdge edge : lc.getLimitCone2().AB.getEdges()) {
						Element edgeElem = document.createElement("edgeref");

						edgeElem.setAttribute("id", edge.getName());
						pathElem.appendChild(edgeElem);
					}

					thisConstraint.appendChild(pathElem);

					// limit cone 2 - BC
					pathElem = document.createElement("path");

					pathElem.setAttribute("domain", lc.getLimitCone2().BC.getDomain().getName());
					pathElem.setAttribute("codomain", lc.getLimitCone2().BC.getCoDomain().getName());

					for (SketchEdge edge : lc.getLimitCone2().BC.getEdges()) {
						Element edgeElem = document.createElement("edgeref");

						edgeElem.setAttribute("id", edge.getName());
						pathElem.appendChild(edgeElem);
					}

					thisConstraint.appendChild(pathElem);

					// limit cone 2 - AC
					pathElem = document.createElement("path");

					pathElem.setAttribute("domain", lc.getLimitCone2().AC.getDomain().getName());
					pathElem.setAttribute("codomain", lc.getLimitCone2().AC.getCoDomain().getName());

					for (SketchEdge edge : lc.getLimitCone2().AC.getEdges()) {
						Element edgeElem = document.createElement("edgeref");

						edgeElem.setAttribute("id", edge.getName());
						pathElem.appendChild(edgeElem);
					}

					thisConstraint.appendChild(pathElem);

					// Add constraint to constraints
					constraints.appendChild(thisConstraint);

					continue;
				}

				for (ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> path : curConstraint
						.getPaths()) {
					// Add pathref to constraint
					Element pathElem = document.createElement("path");

					pathElem.setAttribute("domain", path.getDomain().getName());
					pathElem.setAttribute("codomain", path.getCoDomain().getName());

					for (SketchEdge edge : path.getEdges()) {
						Element edgeElem = document.createElement("edgeref");

						edgeElem.setAttribute("id", edge.getName());
						pathElem.appendChild(edgeElem);
					}

					thisConstraint.appendChild(pathElem);
				}

				// Add constraint to constraints
				constraints.appendChild(thisConstraint);
			}

			rootElement.appendChild(constraints);

			return rootElement;
		} catch (Exception e) {
			return null;
		}
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
	 *
	 *
	 * @param inOverview
	 *
	 * @return
	 */
	public static SketchHandler getNewSketchHandler(Overview inOverview) {
		SketchFrame newFrame = new SketchFrame(inOverview);

		return new SketchHandler(newFrame);
	}

	/**
	 * Load a sketch from XML.
	 *
	 * @param inputFile    the file from which the XML will be read
	 * @param outputSketch The sketch which will be getting the new values
	 * @return Returns the success or failure of the reading.
	 */
	public static boolean graphicalSketchFromXML(File inputFile, Sketch outputSketch) {
		SketchHandler sketchHandler = new SketchHandler(outputSketch.getFrame());

		if (!initializeSketchHandlerFromXML(inputFile, sketchHandler)) {
			return false;
		}

		// TODO: fix new header part when ready.
		outputSketch.initializeFromData(sketchHandler.getSyncLock(), sketchHandler.getEntities(),
				sketchHandler.getEdges(), sketchHandler.getConstraints(), sketchHandler.getDocumentInfo(),
				sketchHandler.getConnParams());

		return true;
	}

	/**
	 * Method to initialize a SketchHandler for a supplied XML file
	 *
	 * @param inputFile     The XML file containing the sketch information
	 * @param sketchHandler An instance of a sketchHandler.
	 * @return true if SketchHandler was initialized, false if an exception
	 *         occurred.
	 *
	 * @since 2006-05-17 Vera Ranieri
	 */
	public static boolean initializeSketchHandlerFromXML(File inputFile, SketchHandler sketchHandler) {
		SAXParser parser;
		SAXParserFactory parseFactory = SAXParserFactory.newInstance();

		try {
			parser = parseFactory.newSAXParser();

			parser.parse(inputFile, sketchHandler);
		} catch (Exception e) {
			System.err.println("Could not open XML file for loading: " + e.getMessage());

			// sketchHandler.getFrame().setVisible(false); This was here before,
			// changed to dispose the right thing to do?
			sketchHandler.getFrame().dispose();

			return false;
		}

		return true;
	}
}
