package catdata.aql;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import catdata.ParseException;
import catdata.Program;
import catdata.aql.exp.AqlEnv;
import catdata.aql.exp.AqlMultiDriver;
import catdata.aql.exp.AqlParserFactory;
import catdata.aql.exp.AqlTyping;
import catdata.aql.exp.Exp;
import catdata.aql.gui.AqlDisplay;
import catdata.ide.CodeTextPanel;

@SuppressWarnings("serial")
public class KnowledgeTransformer extends JPanel {

	public abstract class Example {

		@Override
		public abstract String toString();

		public void apply() {
			srcSchema1.set(getSrcSchema1());
			srcSchema2.set(getSrcSchema2());
			overlap.set(getLinkQuery());
			// mapping1.set(getMapping1());
			// mapping2.set(getMapping2());

			srcInst1.set(getSrcInst1());
			srcInst2.set(getSrcInst2());
			linkInst.set(getLinkQuery());
			// transform1.set(getTransform1());
			// transform2.set(getTransform2());

			// FrozenSchema.set(getFrozenSchema());
			// piMapping.set(getMappingPi());
			// deltaMapping.set(getMappingDelta());
			targetSchema.set(getTargetSchema());

			merge.p.setText(getMergeQuery());
			query.p.setText(getFinalQuery());
			typeside.p.setText(getTypeside());

		}

		public abstract String getTypeside();
		
		public abstract String getLinkQuery();

		public abstract String getSrcSchema1();

		public abstract String getSrcSchema2();

		public abstract String getTargetSchema();

		public abstract String getFinalQuery();

		// public abstract String getMapping1();

		// public abstract String getMapping2();

		public abstract String getMergeQuery();

		public abstract String getSrcInst1();

		public abstract String getSrcInst2();

		// public abstract String getTransform1();

		// public abstract String getTransform2();

		// public abstract String getFrozenSchema();

		// public abstract String getMappingPi();

		// public abstract String getMappingDelta();
	}

	public class Pharm1 extends Example {

		@Override
		public String getLinkQuery() {
			return "schema_colimit Colim = quotient Source1 + Source2 : T {\n" + "	entity_equations\n"
					+ "		Source1.Observation = Source2.Observation\n" + "		Source1.Person = Source2.Patient\n"
					+ "		Source1.ObsType = Source2.Type\n" + "	path_equations\n"
					+ "		Observation.Source1_Observation_f = Observation.Source2_Observation_f\n"
					+ "		Observation.g = Observation.g1.g2\n" + "	options\n" + "		simplify_names = true	\n"
					+ "}";
		}

		@Override
		public String getSrcSchema1() {
			return "schema Source1 = literal : T {\n" + "	entities \n" + "		Observation Person Gender ObsType\n"
					+ "	foreign_keys \n" + "		f: Observation -> Person\n" + "		h: Person -> Gender\n"
					+ "		g: Observation -> ObsType\n" + "	attributes	\n" + "	    att: Person -> String\n"
					+ "	    att: Gender -> String\n" + "	    att: ObsType -> String\n" + "} ";
		}

		@Override
		public String getSrcSchema2() {
			return "schema Source2 = literal : T {\n" + "	entities \n" + "		Observation Patient Method Type\n"
					+ "	foreign_keys \n" + "		f : Observation -> Patient\n" + "		g1: Observation -> Method\n"
					+ "		g2: Method -> Type\n" + "	attributes\n" + "	    att: Patient -> String\n"
					+ "	    att: Type -> String\n" + "}";
		}

		@Override
		public String getMergeQuery() {
			return "instance M = quotient_query J { entity P -> {from p1 p2:P where similar(p1.Person_att,p2.Patient_att)=true@Boolean}}";
		}

		@Override
		public String getSrcInst1() {
			return "instance I1 = literal : Source1 {\n" + "	generators\n" + "		one two three : Observation\n"
					+ "		Peter Paul : Person\n" + "	    M F :Gender\n"
					+ "	    BloodPressure BodyWeight HeartRate: ObsType\n" + "	equations\n"
					+ "		Peter.att = Peter@String Paul.att = Paul@String\n"
					+ "		M.att = M@String F.att = F@String\n" + "		BloodPressure.att = BloodPressure@String\n"
					+ "		BodyWeight.att = BodyWeight@String\n" + "		HeartRate.att = HeartRate@String\n"
					+ "		one.f = Peter two.f = Peter three.f = Paul\n" + "		Peter.h = M Paul.h = M\n"
					+ "		one.g = BloodPressure two.g = BodyWeight three.g = HeartRate\n" + "} ";
		}

		@Override
		public String getSrcInst2() {
			return "instance I2 = literal : Source2 {\n" + "	generators\n" + "		o1 o2 o3 o4 : Observation\n"
					+ "		Pete Jane : Patient\n" + "		m1 m2 m3 m4 : Method\n" + "		BP Wt : Type\n"
					+ "	equations\n" + "	     Pete.att = Pete@String Jane.att = Jane@String\n"
					+ "	     BP.att = BloodPressure@String Wt.att = BodyWeight@String\n"
					+ "	     o1.f = Pete o2.f = Pete o3.f = Jane o4.f = Jane\n"
					+ "	     o1.g1 = m1 o2.g1 = m2 o3.g1 = m3 o4.g1 = m1\n"
					+ "	     m1.g2 = BP m2.g2 = BP m3.g2 = Wt m4.g2 = Wt\n" + "} ";
		}

		@Override
		public String toString() {
			return "Pharma1";

		}

		@Override
		public String getTargetSchema() {
			return "schema Target = literal : T { entities Persons attributes nm : Persons -> String }";
		}

		@Override
		public String getFinalQuery() {
			return "query Q = literal : Merged -> Target {entity Persons -> {from p:P attributes nm -> p.Person_att}  }\n";
		}

		@Override
		public String getTypeside() {
			return "typeside T = literal {\n" + 
					"	imports sql\n" + 
					"	java_functions\n" + 
					"		similar : String,String->Boolean = \"return (input[0].startsWith(input[1]) || input[1].startsWith(input[0]));\"" + 
					"}";
		}

	}

	private TypesideViewer typeside = new TypesideViewer(false);
	private SchemaViewer srcSchema1 = new SchemaViewer("Source1", false), 
			srcSchema2 = new SchemaViewer("Source2", false), // linkSchema = new SchemaViewer("Link", false),
			resultSchema = new SchemaViewer("Merged", true), targetSchema = new SchemaViewer("Target", false);

	private SchemaMappingViewer src1ToResultMapping = new SchemaMappingViewer("G1 : Source1 -> Merged", true),
			src2ToResultMapping = new SchemaMappingViewer("G2 : Source2 -> Merged", true);

	private DataViewer srcInst1 = new DataViewer("Source1", false), srcInst2 = new DataViewer("Source2", false),
			linkInst = new DataViewer("Link", false), resultInst = new DataViewer("Merged", true),
			targetInst = new DataViewer("Target", true);

	private DataMappingViewer src1ToResultTransform = new DataMappingViewer("Sigma_G1(Source1) -> Merged", true),
			src2ToResultTransform = new DataMappingViewer("Sigma_G2(Source2) -> Merged", true),
			resultToTempTransform = new DataMappingViewer("Merged <- Coeval_Q(Frozen)", true);

	private QueryViewer query = new QueryViewer("Q : Merged -|-> Target", false);
	private QueryViewer merge = new QueryViewer("Row Merges", false);
	private QueryViewer overlap = new QueryViewer("Column Merges", false);

//	private SchemaCuratorTab curateSchema;
//	private DataCuratorTab curateData;
//	private AnalyzerTab analyze;

	public class SchemaIntegratorTab extends JPanel {
		public SchemaIntegratorTab() {
			super(new GridLayout(3, 5));

			add(overlap);
			add(new JPanel());
			add(srcSchema1);
			add(new JPanel());
			add(typeside);

			JPanel p = new JPanel(new GridLayout(8, 2));
			p.add(new JButton("Suggest"));
			p.add(new JLabel());
			for (int i = 0; i < 13; i++) {
				p.add(new JLabel());
			}
			JPanel q = new JPanel(new BorderLayout()); // new GridLayout(8, 2));
			q.add(new JButton("Suggest"), BorderLayout.SOUTH);

			add(new JLabel());
			add(p);
			add(src1ToResultMapping);

			add(q);
			add(new JLabel());

			add(srcSchema2);

			add(src2ToResultMapping);

//			add(deltaMapping);
			add(resultSchema);
			add(query);
			add(targetSchema);
		}
	}

	public class DataIntegratorTab extends JPanel {
		public DataIntegratorTab() {
			super(new GridLayout(3, 5));

			add(merge);
//			add(linkInst);
			add(new JPanel());
//			add(transform1);
			add(srcInst1);
			add(new JPanel());
			add(new JPanel());

			JPanel p = new JPanel(new GridLayout(8, 2));
			JPanel q = new JPanel(new GridLayout(8, 2));
			p.add(new JButton("Suggest"));
			p.add(new JLabel());
			p.add(new JLabel());
			// p.add(new JButton("Write as Query"));
			for (int i = 0; i < 13; i++) {
				p.add(new JLabel());
			}

			add(new JPanel());
//			add(transform2);
			add(p);
			add(src1ToResultTransform);
			add(q);
			add(new JPanel());
//			add(finalToTempTransform);

			add(srcInst2);
			add(src2ToResultTransform);
			add(resultInst);
			add(resultToTempTransform);
			// add(FrozenInst);
			add(targetInst);
		}
	}

	public class CleanTab extends JPanel {
		public CleanTab() {
			super(new GridLayout(1, 1));
			CodeTextPanel ret = new CodeTextPanel("Key Points",
					"Scan for and/or impose the data integrity previously assumed/desired , by removing and/or cleaning violating tuples.");
			add(ret);
		}
	}

	public class StructureTab extends JPanel {
		public StructureTab() {
			super(new GridLayout(1, 1));
			CodeTextPanel ret = new CodeTextPanel("Key Points",
					"(Optional) Convert English texts or other unstructued data into data on the knowledge graph schemas just defined.");
			add(ret);
		}
	}

	public class SchemaCuratorTab extends JPanel {
		public SchemaCuratorTab() {
			super(new GridLayout(1, 1));
			CodeTextPanel ret = new CodeTextPanel("Key Points",
					"(Optional) Name the equivalence classes of entities and paths that arise in the merged schema, and remove redundant edges.");
			add(ret);
		}
	}

	public class DataCuratorTab extends JPanel {
		public DataCuratorTab() {
			super(new GridLayout(1, 1));
			CodeTextPanel ret = new CodeTextPanel("Key Points",
					"Deal with conflicts that occur during integration, such as when Pete and Peter are age 15 and 16, respectively.");
			add(ret);
		}
	}

	public class AnalyzerTab extends JPanel {
		public AnalyzerTab() {
			super(new GridLayout(1, 1));
			CodeTextPanel ret = new CodeTextPanel("Key Points",
					"(Optional) Demonstrate machine learning directly on the output knowledge graph, ideally using the symbolic information about cones etc.");
			add(ret);
		}
	}

	public class GraphViewer extends JPanel {
		CodeTextPanel panel;
		// CodeTextPanel visual;

		public GraphViewer() {
			super(new GridLayout(1, 1));
			// JTabbedPane p = new JTabbedPane();
			panel = new CodeTextPanel("", "");
			// visual = new CodeTextPanel("", "");
			// visual = new CodeTextPanel("", "");
			// p.add(panel, "Graph");
			// p.add(visual, "Graph");
			add(panel);
		}

		public void set(String x) {
			panel.setText(x);
		}
	}

	public class ConeViewer extends JPanel {
		CodeTextPanel panel;
		CodeTextPanel visual;

		public ConeViewer(Boolean direction) {
			super(new GridLayout(1, 1));
			/*
			 * JTabbedPane p = new JTabbedPane(); panel = new CodeTextPanel("", ""); visual
			 * = new CodeTextPanel("", ""); p.add(panel, "Text"); p.add(visual,
			 * "Graph Map"); add(p);
			 */
			panel = new CodeTextPanel("", "");
			add(panel);
		}

		public void set(String x) {
			panel.setText(x);
		}
	}

	static Color color = new Color(235, 235, 235);

	public class TypesideViewer extends JPanel {
		GraphViewer graphv = new GraphViewer();
		ConeViewer diagv = new ConeViewer(null), conev = new ConeViewer(true), coconev = new ConeViewer(false);

		public TypesideViewer(boolean disable) {
			super(new BorderLayout());
			String s = "Typeside ";
			if (!disable) {
				setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), s));
			} else {
				setBorder(BorderFactory.createTitledBorder(BorderFactory.createDashedBorder(Color.DARK_GRAY, 5f, 10f),
						s));
				p.area.setBackground(color);
				p.area.setEditable(false);
				graphv.panel.setBackground(color);
				diagv.panel.setBackground(color);
				coconev.panel.setBackground(color);
				conev.panel.setBackground(color);
				coconev.panel.area.setEditable(false);
				diagv.panel.area.setEditable(false);
				conev.panel.area.setEditable(false);
				graphv.panel.area.setEditable(false);
			}

			// JTabbedPane jtb = new JTabbedPane();
			// jtb.add(p, "Text");
			// jtb.add(graphv, "Graph");
			// jtb.add(diagv, "Path Eqs");
			// jtb.add(conev, "Limits");
			// jtb.add(coconev, "Co-Limits");
			add(p, BorderLayout.CENTER);
		}

		CodeTextPanel p = new CodeTextPanel("", "");

		public void set(String m) {
			p.setText(m);
		}
	}
	
	public class SchemaViewer extends JPanel {
		GraphViewer graphv = new GraphViewer();
		ConeViewer diagv = new ConeViewer(null), conev = new ConeViewer(true), coconev = new ConeViewer(false);

		public SchemaViewer(String s, boolean disable) {
			super(new BorderLayout());
			s = "Schema " + s;
			if (!disable) {
				setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), s));
			} else {
				setBorder(BorderFactory.createTitledBorder(BorderFactory.createDashedBorder(Color.DARK_GRAY, 5f, 10f),
						s));
				p.area.setBackground(color);
				p.area.setEditable(false);
				graphv.panel.setBackground(color);
				diagv.panel.setBackground(color);
				coconev.panel.setBackground(color);
				conev.panel.setBackground(color);
				coconev.panel.area.setEditable(false);
				diagv.panel.area.setEditable(false);
				conev.panel.area.setEditable(false);
				graphv.panel.area.setEditable(false);
			}

			// JTabbedPane jtb = new JTabbedPane();
			// jtb.add(p, "Text");
			// jtb.add(graphv, "Graph");
			// jtb.add(diagv, "Path Eqs");
			// jtb.add(conev, "Limits");
			// jtb.add(coconev, "Co-Limits");
			add(p, BorderLayout.CENTER);
		}

		CodeTextPanel p = new CodeTextPanel("", "");

		public void set(String m) {
			p.setText(m);
		}
	}

	public class SchemaMappingViewer extends JPanel {

		public SchemaMappingViewer(String s, boolean disable) {
			super(new BorderLayout());
			if (!disable) {
				setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), s));
			} else {
				p.area.setEditable(false);
				setBorder(BorderFactory.createTitledBorder(BorderFactory.createDashedBorder(Color.DARK_GRAY, 5f, 10f),
						s));
				p.area.setBackground(color);
				p.area.setEditable(false);

			}

			// JTabbedPane jtb = new JTabbedPane();
			// jtb.add(p, "Text");
			// jtb.add(new CodeTextPanel("", ""), "Graph Map");
			add(p, BorderLayout.CENTER);
		}

		CodeTextPanel p = new CodeTextPanel("", "");

		public void set(String m) {
			p.setText(m);
		}
	}

	public class DataViewer extends JPanel {
		public DataViewer(String s, boolean disable) {
			super(new BorderLayout());
			s = "Database " + s;
			if (!disable) {
				setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), s));
			} else {
				setBorder(BorderFactory.createTitledBorder(BorderFactory.createDashedBorder(Color.DARK_GRAY, 5f, 10f),
						s));
				p.area.setBackground(color);
				p.area.setEditable(false);

			}
			// JTabbedPane jtb = new JTabbedPane();
			// jtb.add(p, "Text");
			// jtb.add(new CodeTextPanel("", ""), "Tables");
			// jtb.add(new CodeTextPanel("", ""), "Graph");
			add(p, BorderLayout.CENTER);
		}

		CodeTextPanel p = new CodeTextPanel("", "");

		public void set(String m) {
			p.setText(m);
		}

	}

	public class DataMappingViewer extends JPanel {
		public DataMappingViewer(String s, boolean disable) {
			super(new BorderLayout());
			if (!disable) {
				setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), s));
			} else {
				setBorder(BorderFactory.createTitledBorder(BorderFactory.createDashedBorder(Color.DARK_GRAY, 5f, 10f),
						s));
				p.area.setBackground(color);
				p.area.setEditable(false);

			}
			// JTabbedPane jtb = new JTabbedPane();
			// jtb.add(p, "Text");
			// jtb.add(new CodeTextPanel("", ""), "Tables");
			// jtb.add(new CodeTextPanel("", ""), "Graph Map");
			add(p, BorderLayout.CENTER);
		}

		CodeTextPanel p = new CodeTextPanel("", "");

		public void set(String m) {
			p.setText(m);
		}

	}

	public class QueryViewer extends JPanel {
		public QueryViewer(String s, boolean disable) {
			super(new BorderLayout());
			if (!disable) {
				setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), s));
			} else {
				setBorder(BorderFactory.createTitledBorder(BorderFactory.createDashedBorder(Color.DARK_GRAY, 5f, 10f),
						s));
				p.area.setBackground(color);
				p.area.setEditable(false);
			}
			// JTabbedPane jtb = new JTabbedPane();
			// jtb.add(p, "Text");
			add(p, BorderLayout.CENTER);
		}

		public CodeTextPanel p = new CodeTextPanel("", "");

		public void set(String m) {
			p.setText(m);
		}

	}

	public KnowledgeTransformer() {
		super(new BorderLayout());
		JComboBox<Example> examples = new JComboBox<>(new Example[] { null, new Pharm1() });
		examples.addActionListener(x -> {
			Example e = (Example) examples.getSelectedItem();
			if (e == null) {
				return;
			}
			e.apply();
		});
		JPanel topPanel = new JPanel(new GridLayout(1, 10));
		JButton b = new JButton("Run");
		topPanel.add(b);
		b.addActionListener(x -> doIt());
		topPanel.add(new JButton("New"));
		topPanel.add(new JButton("Open"));
		topPanel.add(new JButton("Save"));
		topPanel.add(new JButton("Save As"));

		topPanel.add(new JLabel());
		topPanel.add(new JLabel());
		topPanel.add(new JLabel());

//		topPanel.add(new JLabel("Example:"));
		topPanel.add(examples);
		add(topPanel, BorderLayout.NORTH);

		CodeTextPanel status = new CodeTextPanel(BorderFactory.createEmptyBorder(), "", "Ready.");
		add(status, BorderLayout.SOUTH);

		JTabbedPane jtp = new JTabbedPane();

		jtp.add(new SchemaIntegratorTab(), "Schema-Level Design");
		jtp.add(new DataIntegratorTab(), "Data-Level Design");
		jtp.add(new AnalyzerTab(), "Predictive Analytics");

		add(jtp, BorderLayout.CENTER);
	}

	private void doIt() {
		
		
		String s = typeside.p.getText() + "\n\n" + srcSchema1.p.getText() + "\n\n" + srcSchema2.p.getText() + "\n\n" + overlap.p.getText() + "\n\n"
				+ "schema Merged = getSchema Colim\n\nmapping G1 = getMapping Colim Source1\n\nmapping G2 = getMapping Colim Source2\n\n"
				+ targetSchema.p.getText() + "\n\n";
		if (!srcInst1.p.getText().isBlank()) {
			s += srcInst1.p.getText() + "\n\n" + srcInst2.p.getText() + "\n\n"		
				+ "instance J1 = sigma G1 I1\n\ninstance J2 = sigma G2 I2\n\n" 
				+ "instance J = coproduct J1 + J2 : Merged \n\n";
			if (!merge.p.getText().isBlank()) {
				 s += merge.p.getText() + "\n\n";
			}
			if (!query.p.getText().isBlank()) {
				 s += query.p.getText() + "\n\ninstance Result = eval Q M";
			}

		}
		
		Program<Exp<?>> program;
		try {
			long start = System.currentTimeMillis();
			program = AqlParserFactory.getParser().parseProgram(s);
			AqlEnv env = new AqlEnv(program);
			System.out.println(s);
			AqlMultiDriver d = new AqlMultiDriver(program, env);
			d.start();
			env.typing = new AqlTyping(program, false);
			new AqlDisplay("foo", program, d.env, start, System.currentTimeMillis()); 
		} catch (ParseException e) {			System.out.println(s);
			JOptionPane.showMessageDialog(null, e.getMessage());
			e.printStackTrace();
		}
	
		
	}

	public static void main(String[] args) {
		JFrame f = new JFrame("Conexus Knowledge Graph Transformer");
		f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		f.setContentPane(new KnowledgeTransformer());
		f.setVisible(true);
		f.setPreferredSize(new Dimension(1400, 800));
		f.pack();
		f.setLocationRelativeTo(null);
	}

}
