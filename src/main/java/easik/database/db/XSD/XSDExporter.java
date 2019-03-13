package easik.database.db.XSD;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import easik.Easik;
import easik.EasikTools;
import easik.database.api.xmldb.XMLDBExporter;
import easik.database.base.PersistenceDriver;
import easik.model.constraint.CommutativeDiagram;
import easik.model.constraint.EqualizerConstraint;
import easik.model.constraint.LimitConstraint;
import easik.model.constraint.ModelConstraint;
import easik.model.constraint.ProductConstraint;
import easik.model.constraint.PullbackConstraint;
import easik.model.constraint.SumConstraint;
import easik.model.path.ModelPath;
import easik.sketch.Sketch;
import easik.sketch.edge.SketchEdge;
import easik.sketch.util.graph.SketchGraphModel;
import easik.sketch.vertex.EntityNode;
import easik.ui.SketchFrame;
import easik.xml.xsd.XMLNameSpace;
import easik.xml.xsd.XMLSchema;
import easik.xml.xsd.nodes.XSDAnnotation;
import easik.xml.xsd.nodes.elements.XSDAbstractElement;
import easik.xml.xsd.nodes.elements.XSDChoiceCompositor;
import easik.xml.xsd.nodes.elements.XSDElement;
import easik.xml.xsd.nodes.elements.XSDSequenceCompositor;
import easik.xml.xsd.nodes.types.XSDComplexType;
import easik.xml.xsd.nodes.types.XSDType;

/**
 * Class to export a sketch as an XML Schema description Done as part of the
 * coursework for the author's class on database systems, summer 2009.
 *
 * @author Brett G. Giles, brett.giles@ucalgary.ca, brett.giles@drogar.com
 * @since 2009-08-24
 */
public class XSDExporter extends XMLDBExporter {
	/**  */
	private String nsPath;

	/**
	 * A variable to hold the XMLSchema built from the Sketch.
	 */
	private XMLSchema theSchema;

	/**
	 * The top level element.
	 */
	private XSDElement topLevel;

	/**  */
	private boolean useTargetNS;

	/**
	 * Create an exporter based on a sketch, the persistence driver and the
	 * options
	 *
	 * @param sk
	 *            The sketch to export
	 * @param db
	 *            The persistance driver - will always be the XMLDBDriver
	 *            direver here.
	 * @param exportOpts
	 *            Options from {@link easik.ui.menu.popup.XSDWriteOptions}
	 *
	 * @throws PersistenceDriver.LoadException
	 */
	public XSDExporter(final Sketch sk, final XSD db, final Map<String, ?> exportOpts) throws PersistenceDriver.LoadException {
		super(sk, db, exportOpts);

		useTargetNS = Boolean.valueOf(exportOpts.get("targetNS").toString());

		final boolean unqElts = Boolean.valueOf(exportOpts.get("unqualifiedElements").toString());
		final boolean unqAtts = Boolean.valueOf(exportOpts.get("unqualifiedAttributes").toString());

		theSchema = new XMLSchema(new XMLNameSpace("", null));

		theSchema.setAttributeFormDefaultUnQualified(unqAtts);
		theSchema.setElementFormDefaultUnQualified(unqElts);

		final Object topLevelName = exportOpts.get("topLevelTag");

		topLevel = new XSDElement((null == topLevelName) ? "easikxsdexport" : topLevelName.toString());

		topLevel.setContents(new XSDSequenceCompositor());
		theSchema.addElement(topLevel);
	}

	/**
	 * Writes the export script to a file.
	 * <p/>
	 * Uses {@link #exportToString()} to do the work.
	 *
	 * @param outFile
	 *            the file to write to
	 *
	 * @throws IOException
	 */
	@Override
	public void exportToFile(final File outFile) throws IOException {
		final PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(outFile)));

		nsPath = outFile.getCanonicalPath();

		out.print(exportToString());
		out.flush();
		out.close();
	}

	/**
	 * Returns the export as a string.
	 * <p/>
	 * First, set the target namespace if we are using one as the file selected.
	 * <p/>
	 * Then, {@link #createEntities()} , {@link #addConstraints()} ,
	 * {@link #addTypesandElementsToSchema()} and {@link #setTheKeys()} .
	 *
	 * @return sql export
	 */
	@Override
	public String exportToString() {
		if (useTargetNS) {
			try {
				final String uriString = "file:///" + nsPath.replaceAll("\\\\", "/");

				theSchema.setTargetNS(new XMLNameSpace("sch", new URI(uriString)));
			} catch (Exception e) {
				// ignore
			}
		}

		createEntities();
		addConstraints();
		addTypesandElementsToSchema();
		setTheKeys();

		return theSchema.toString();
	}

	/**
	 *
	 *
	 * @throws PersistenceDriver.LoadException
	 */
	@Override
	public void exportToNative() throws PersistenceDriver.LoadException {
		// To change body of implemented methods use File | Settings | File
		// Templates.
	}

	/**
	 * First pass - create the XML Schema types and the associated elements.
	 * <p/>
	 * Loop through the entities in the sketch. For each one, create a complex
	 * type and a corresponding element.
	 *
	 * @see easik.xml.xsd.nodes.types.XSDComplexType
	 * @see easik.xml.xsd.nodes.elements.XSDElement
	 */
	private void createEntities() {
		final Collection<EntityNode> nodeCollection = sketch.getEntities();

		for (final EntityNode entity : nodeCollection) {
			final XSDComplexType complexType = new XSDComplexType(entity, null);

			entity.setXsdType(complexType);
		}

		for (final EntityNode entity : nodeCollection) {
			entity.setXsdElement(new XSDElement(entity, topLevel));
		}
	}

	/**
	 * Loop through the sketch's categorical constraints and add the Schema
	 * elements for them
	 *
	 * @todo Implement this via a factory pattern rather than using instanceOf
	 *       <p/>
	 *       Delegate to the various createConstraint methods based on the type
	 *       of the constraint.
	 */
	private void addConstraints() {
		final List<ModelConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge>> constraints = (List<ModelConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge>>) sketch.getConstraints().values();

		for (final ModelConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> c : constraints) {
			if (c instanceof CommutativeDiagram) {
				createConstraint((CommutativeDiagram<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge>) c);
			} else if (c instanceof ProductConstraint) {
				createConstraint((ProductConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge>) c);
			} else if (c instanceof PullbackConstraint) {
				createConstraint((PullbackConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge>) c);
			} else if (c instanceof EqualizerConstraint) {
				createConstraint((EqualizerConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge>) c);
			} else if (c instanceof SumConstraint) {
				createConstraint((SumConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge>) c);
			} else if (c instanceof LimitConstraint) {
				createConstraint((LimitConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge>) c);
			} else {
				System.err.println("Unknown constraint type encountered: " + c.getClass());
			}
		}
	}

	/**
	 * After constraints have been handled, the types and elements remaining can
	 * be added to the schema.
	 * <p/>
	 * Note that adding constraints may remove elements from the sketch
	 * entities, e.g., in equalizers or sums, the isA relationship means that
	 * containment can be used.
	 */
	private void addTypesandElementsToSchema() {
		final Collection<EntityNode> nodeCollection = sketch.getEntities();

		for (final EntityNode entity : nodeCollection) {
			theSchema.addType(entity.getXsdType());
			topLevel.addAtom(entity.getXsdElement());
		}
	}

	/**
	 * Setting the keys including primary, foreign keyrefs and uniqueness
	 *
	 * @see easik.xml.xsd.nodes.elements.XSDElement#setKeys(easik.sketch.vertex.EntityNode)
	 */
	private void setTheKeys() {
		final Collection<EntityNode> nodeCollection = sketch.getEntities();

		for (final EntityNode entity : nodeCollection) {
			final XSDElement elt = entity.getXsdElement();

			if (null != elt) {
				elt.setKeys(entity);
			}
		}
	}

	/**
	 * Add an annotation explaining the commutativity of the diagram.
	 * <p/>
	 * Today, this is simply done by creating an annotation. For example in the
	 * standard cd in constraints.easik gives this annotation:
	 * 
	 * <pre>
	 * &lt;xs:annotation>
	 *  &lt;xs:documentation>
	 *     Domain(f1); A(f2) ;Codomain =
	 *     Domain(f3); B(f4) ;Codomain
	 *   &lt;/xs:documentation>
	 * &lt;/xs:annotation>
	 * </pre>
	 *
	 * @param cd
	 *            the commutative diagram constraint.
	 */
	private void createConstraint(final CommutativeDiagram<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> cd) {
		final List<ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge>> paths = cd.getPaths();
		final EntityNode dom = paths.get(0).getDomain();
		final XSDType domType = dom.getXsdType();
		final String keyrefName = Easik.getInstance().getSettings().getProperty("xml_keyref_name");
		final List<String> values = new ArrayList<>(paths.size());

		for (final ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> path : paths) {
			final LinkedList<SketchEdge> tmpPath = new LinkedList<>(path.getEdges());

			tmpPath.removeFirst();

			if (tmpPath.size() == 0) {
				values.add(dom.getName() + '(' + path.getFirstEdge().getForeignKeyName(keyrefName) + ')');
			} else {
				values.add(dom.getName() + '(' + path.getFirstEdge().getForeignKeyName(keyrefName) + ')' + xmlJoinPath(tmpPath, true));
			}
		}

		domType.addAnnotation(new XSDAnnotation(EasikTools.join(" = " + lineSep, values)));
	}

	/**
	 * Add an annotation explaining the product.
	 * <p/>
	 * Today, this is simply done by creating an annotation. For example in the
	 * standard product constraint in constraints.easik gives this annotation:
	 * 
	 * <pre>
	 * &lt;xs:annotation>
	 *  &lt;xs:documentation>
	 *     ForAll.elem1 in (P1), ForAll.elem2 in (P2)
	 *     Exists.p=(elem1,elem2) in Product
	 *   &lt;/xs:documentation>
	 * &lt;/xs:annotation>
	 * </pre>
	 *
	 * @param prod
	 *            the product diagram constraint.
	 */
	private void createConstraint(final ProductConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> prod) {
		final List<ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge>> paths = prod.getPaths();
		final EntityNode dom = paths.get(0).getDomain();
		final XSDType domType = dom.getXsdType();
		final List<String> elts = new ArrayList<>(paths.size());
		int id = 0;
		@SuppressWarnings("unused")
		final String keyrefName = Easik.getInstance().getSettings().getProperty("xml_keyref_name");
		final List<String> values = new ArrayList<>(paths.size());

		for (final ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> path : paths) {
			id++;

			final LinkedList<SketchEdge> tmpPath = new LinkedList<>(path.getEdges());

			tmpPath.removeFirst();

			final String elem = "elem" + id;

			elts.add(elem);

			if (tmpPath.size() == 0) {
				values.add("ForAll." + elem + " in (" + path.getCoDomain().getName() + ')');
			} else {
				values.add("ForAll." + elem + " in " + xmlJoinPath(tmpPath, true));
			}
		}

		final String documentation = EasikTools.join(", ", values);
		final String elements = "Exists.p=(" + EasikTools.join(",", elts) + ") in " + dom.getName();

		domType.addAnnotation(new XSDAnnotation(documentation + lineSep + elements));
	}

	/**
	 * Add an annotation explaining the pullback.
	 * <p/>
	 * Today, this is simply done by creating an annotation. For example in the
	 * standard pullback constraint in constraints.easik gives this annotation:
	 * 
	 * <pre>
	 * &lt;xs:annotation>
	 *  &lt;xs:documentation>
	 *     ForAll.elemA in B, ForAll.elemB in C :
	 *     elemA.f1 in A=elemB.isA_1 in A
	 *     ==> Exists.p=(elemA,elemB) in Pullback
	 *   &lt;/xs:documentation>
	 * &lt;/xs:annotation>
	 * </pre>
	 *
	 * @param pb
	 *            the product diagram constraint.
	 */
	private void createConstraint(final PullbackConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> pb) {
		final List<ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge>> paths = pb.getPaths();
		final EntityNode dom = paths.get(0).getDomain();
		final XSDType domType = dom.getXsdType();
		final List<String> elts = new ArrayList<>(paths.size());
		@SuppressWarnings("unused")
		final String keyrefName = Easik.getInstance().getSettings().getProperty("xml_keyref_name");
		final List<String> values = new ArrayList<>();
		final List<String> equalities = new ArrayList<>();

		// WPBEDIT CF2012
		for (int i = 0; i < pb.getWidth(); i++) {
			final ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> path = pb.getFullPath(i);
			final LinkedList<SketchEdge> tmpPath = new LinkedList<>(path.getEdges());

			tmpPath.removeFirst();
			values.add("ForAll.elem" + i + " in " + xmlPBJoinPath(tmpPath, false));
			equalities.add("elem" + i + '.' + xmlPBelemJoinPath(tmpPath, true));
			elts.add("elem" + i);
		}

		final String valdocumentation = EasikTools.join(", ", values);
		final String equalDoc = EasikTools.join("=", equalities);
		final String elements = "==> Exists.p=(" + EasikTools.join(",", elts) + ") in " + dom.getName();

		domType.addAnnotation(new XSDAnnotation(valdocumentation + " : " + lineSep + equalDoc + lineSep + elements));
	}

	/**
	 * Add an annotation explaining the equalizer and effect the isA.
	 * <p/>
	 * Today, this has two parts. First the annotation. For example in the
	 * standard equalizer constraint in constraints.easik gives this annotation:
	 * 
	 * <pre>
	 * &lt;xs:annotation>
	 *  &lt;xs:documentation>
	 *     Equalizer(f3); B(f4) ;C(f5) ;Codomain =
	 *     Equalizer(f1); D(f2) ;Codomain
	 *   &lt;/xs:documentation>
	 * &lt;/xs:annotation>
	 * </pre>
	 * <p/>
	 * In addition, the equalizer entity has an "isA" relationship to another
	 * entity at the start of the diagram, so this is reflected in the typing.
	 * First, an element of equalizer type is added to the target of the "isA"
	 * relationship. Second, since there is no need for a separate element in
	 * the schema for the equalizer entity, its element is removed from the
	 * sketch entity.
	 *
	 * @param eq
	 *            the equalizer diagram constraint.
	 * @todo Why not do this with standard isA relationships as well?
	 * @todo The equalizer element should be added in some way so there is a
	 *       "minoccurs" of zero.
	 * @todo The equalizer does not really need a "key" element, but what if
	 *       other constraints refer to it.
	 */
	private void createConstraint(final EqualizerConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> eq) {
		final List<ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge>> paths = eq.getEqualizerPaths();
		final EntityNode equalizer = eq.getEqualizerEntity();
		final XSDType domType = equalizer.getXsdType();
		final String keyrefName = Easik.getInstance().getSettings().getProperty("xml_keyref_name");
		final List<String> values = new ArrayList<>(paths.size());

		for (final ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> path : paths) {
			final LinkedList<SketchEdge> tmpPath = new LinkedList<>(path.getEdges());

			tmpPath.removeFirst();

			if (tmpPath.size() == 0) {
				values.add(equalizer.getName() + '(' + path.getFirstEdge().getForeignKeyName(keyrefName) + ')');
			} else {
				values.add(equalizer.getName() + '(' + path.getFirstEdge().getForeignKeyName(keyrefName) + ')' + xmlJoinPath(tmpPath, true));
			}
		}

		final EntityNode isaNode = paths.get(0).getDomain();
		final XSDComplexType isaNodeType = (XSDComplexType) isaNode.getXsdType();
		final XSDElement element = equalizer.getXsdElement();

		element.setParent(isaNode.getXsdElement());
		isaNodeType.addAtom(element);
		equalizer.setXsdElement(null);
		domType.addAnnotation(new XSDAnnotation(EasikTools.join(" = " + lineSep, values)));
	}

	/**
	 * Add an annotation explaining the sum constraint and effect the isAs.
	 * <p/>
	 * Today, this has two parts. First the annotation is truly just
	 * documentation in this case. The second part of the constraint creation,
	 * where the isA parts are added as elements of the type suffices to explain
	 * everything that is needed. In the standard sum constraint in
	 * constraints.easik gives this annotation:
	 * 
	 * <pre>
	 * &lt;xs:annotation>
	 *  &lt;xs:documentation>
	 *     Sum is a disjoint generalization of Summand1 and Summand2 and Summand3
	 *   &lt;/xs:documentation>
	 * &lt;/xs:annotation>
	 * </pre>
	 * <p/>
	 * Each of the Summand entities have an "isA" relationship to the Sum entity
	 * at the start of the diagram, so this is reflected in the typing. First,
	 * an element of each Summand type is added to the target of the "isA"
	 * relationship. Second, since there is no need for a separate element in
	 * the schema for the Summand entity, its element is removed from the sketch
	 * entity.
	 *
	 * @param sum
	 *            the summand diagram constraint.
	 * @todo Why not do this with standard isA relationships as well?
	 */
	private static void createConstraint(final SumConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> sum) {
		final Map<String, StringBuilder> codMap = new HashMap<>(10);
		final Map<String, XSDType> codToXMLType = new HashMap<>(10);
		XSDChoiceCompositor summands = new XSDChoiceCompositor(new ArrayList<XSDAbstractElement>(sum.getPaths().size()));
		String cdTypeName = "";
		XSDComplexType lastCodomainType = null;

		for (final ModelPath<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> path : sum.getPaths()) {
			final EntityNode coDomain = path.getCoDomain();
			final EntityNode dom = path.getDomain();
			final String coDomainName = coDomain.getName();
			final StringBuilder coDomainAnnotation;

			if (codMap.containsKey(coDomainName)) {
				coDomainAnnotation = codMap.get(coDomainName);

				coDomainAnnotation.append(" and ").append(dom.getName());
			} else {
				coDomainAnnotation = new StringBuilder(100);

				coDomainAnnotation.append(coDomain.getName()).append(" is a disjoint generalization of ").append(dom.getName());
			}

			final XSDComplexType codomainType = (XSDComplexType) coDomain.getXsdType();

			if ((null != lastCodomainType) && !cdTypeName.equals(codomainType.getName())) {
				if (!summands.isEmpty()) {
					lastCodomainType.addAtom(summands);

					summands = new XSDChoiceCompositor(new ArrayList<XSDAbstractElement>(sum.getPaths().size()));
				}
			}

			lastCodomainType = codomainType;
			cdTypeName = codomainType.getName();

			codToXMLType.put(coDomainName, codomainType);
			codMap.put(coDomainName, coDomainAnnotation);

			final XSDElement element = dom.getXsdElement();

			element.setParent(coDomain.getXsdElement());
			summands.addSubElement(element);
			dom.setXsdElement(null);
		}

		if (null != lastCodomainType) {
			if (!summands.isEmpty()) {
				lastCodomainType.addAtom(summands);

				summands = null;
			}
		}

		for (final Map.Entry<String, StringBuilder> entry : codMap.entrySet()) {
			final XSDType typ = codToXMLType.get(entry.getKey());

			typ.addAnnotation(new XSDAnnotation(entry.getValue().toString()));
		}
	}

	/**
	 *
	 *
	 * @param constraint
	 */
	public void createConstraint(final LimitConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> constraint) {
		// TODO CF2012 Incomplete
	}

	/**
	 * protected method to assist in building "path" strings for the constraint
	 * annotations
	 *
	 * @param inEdges
	 *            List of sketchedges
	 * @param includeLast
	 *            Include the last or not
	 * @return path like string
	 */
	protected String xmlJoinPath(final List<SketchEdge> inEdges, final boolean includeLast) {
		final LinkedList<SketchEdge> edges = new LinkedList<>(inEdges);
		final StringBuilder joinClause = new StringBuilder("; ");

		joinClause.append(quoteId(edges.get(0).getSourceEntity().getName()));

		if (!includeLast && !edges.isEmpty()) {
			edges.removeLast();
		}

		for (final SketchEdge e : edges) {
			final EntityNode target = e.getTargetEntity();

			joinClause.append('(').append(e.getName()).append(") ;").append(target.getName());
		}

		return joinClause.toString();
	}

	/**
	 * protected method to assist in building "pullbackpath" strings for the
	 * pullback constraint annotations
	 *
	 * @param inEdges
	 *            List of sketchedges
	 * @param includeLast
	 *            Include the last or not
	 * @return path like string
	 */
	protected String xmlPBJoinPath(final List<SketchEdge> inEdges, final boolean includeLast) {
		final LinkedList<SketchEdge> edges = new LinkedList<>(inEdges);
		final StringBuilder joinClause = new StringBuilder(quoteId(edges.get(0).getSourceEntity().getName()));

		if (!includeLast && !edges.isEmpty()) {
			edges.removeLast();
		}

		for (final SketchEdge e : edges) {
			final EntityNode target = e.getTargetEntity();

			joinClause.append('(').append(e.getName()).append(")->").append(target.getName());
		}

		return joinClause.toString();
	}

	/**
	 * protected method to assist in building "pullback element path" strings
	 * for the pullback constraint annotations
	 *
	 * @param inEdges
	 *            List of sketchedges
	 * @param includeLast
	 *            Include the last or not
	 * @return path like string
	 */
	protected static String xmlPBelemJoinPath(final List<SketchEdge> inEdges, final boolean includeLast) {
		final Deque<SketchEdge> edges = new LinkedList<>(inEdges);
		final StringBuilder joinClause = new StringBuilder(10);

		if (!includeLast && !edges.isEmpty()) {
			edges.removeLast();
		}

		for (final SketchEdge e : edges) {
			final EntityNode target = e.getTargetEntity();

			joinClause.append(e.getName()).append(" in ").append(target.getName());
		}

		return joinClause.toString();
	}

	// this driver is supposed to implement these constraint methods (they do
	// nothing)
	// this driver uses its own private constraint methods right now

	/**
	 *
	 *
	 * @param constraint
	 * @param id
	 *
	 * @return
	 */
	@Override
	public List<String> createConstraint(SumConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> constraint, String id) {
		return null;
	}

	/**
	 *
	 *
	 * @param constraint
	 * @param id
	 *
	 * @return
	 */
	@Override
	public List<String> createConstraint(PullbackConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> constraint, String id) {
		return null;
	}

	/**
	 *
	 *
	 * @param constraint
	 * @param id
	 *
	 * @return
	 */
	@Override
	public List<String> createConstraint(EqualizerConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> constraint, String id) {
		return null;
	}

	/**
	 *
	 *
	 * @param constraint
	 * @param id
	 *
	 * @return
	 */
	@Override
	public List<String> createConstraint(ProductConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> constraint, String id) {
		return null;
	}

	/**
	 *
	 *
	 * @param constraint
	 * @param id
	 *
	 * @return
	 */
	@Override
	public List<String> createConstraint(CommutativeDiagram<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> constraint, String id) {
		return null;
	}

	/**
	 *
	 *
	 * @param constraint
	 * @param id
	 *
	 * @return
	 */
	@Override
	public List<String> createConstraint(LimitConstraint<SketchFrame, SketchGraphModel, Sketch, EntityNode, SketchEdge> constraint, String id) {
		return null;
	}
}
