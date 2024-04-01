package catdata.cql.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Paint;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.google.common.base.Function;

import catdata.Pair;
import catdata.ParseException;
import catdata.Program;
import catdata.Triple;
import catdata.Util;
import catdata.cql.Constraints;
import catdata.cql.Instance;
import catdata.cql.Kind;
import catdata.cql.Query;
import catdata.cql.Schema;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.exp.AqlEnv;
import catdata.cql.exp.AqlMultiDriver;
import catdata.cql.exp.Att;
import catdata.cql.exp.CombinatorParser;
import catdata.cql.exp.Exp;
import catdata.cql.exp.Fk;
import catdata.cql.exp.SchExpMsCatalog;
import catdata.cql.exp.Sym;
import catdata.ide.CodeTextPanel;
import edu.uci.ics.jung.algorithms.layout.CircleLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.DelegateForest;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse.Mode;
import edu.uci.ics.jung.visualization.renderers.EdgeLabelRenderer;
import edu.uci.ics.jung.visualization.renderers.VertexLabelRenderer;

public class WarehouseUI<N, T> {

	// TODO: add command check to sources

	public static abstract class Example {
		public String getOptions() {
			return """
					options
							require_consistency=false
							allow_sql_import_all_unsafe=true
							sql_constraints_simple=true
							simplify_names=false
							allow_aggregation_unsafe=true
							""";
		}

		public abstract String getName();

		public abstract String getSources();

		public abstract String getLinks();

		public abstract String getTargets();

		@Override
		public String toString() {
			return getName();
		}

		public boolean push() {
			return true;
		}
	}

	public static void show() {
		JFrame f = new JFrame("Universal Warehousing");
		f.add(new WarehouseUI<>().makeUI());
		f.setSize(1360, 768);
		f.setLocationRelativeTo(null);
		f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		f.setVisible(true);
	}

	CardLayout cl = new CardLayout();
//	private static int cnt = 0;
	Map<WarehouseTreeNode, JComponent> cache = new HashMap<>();

	public class WarehouseTreeNode {
		public final String name, name2;
		public final WarehouseWhich w;

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Objects.hash(name, name2, w);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			WarehouseTreeNode other = (WarehouseTreeNode) obj;
			return Objects.equals(name, other.name) && Objects.equals(name2, other.name2) && w == other.w;
		}

		public WarehouseTreeNode(String name, WarehouseWhich w, String name2) {
			this.name = name;
			this.name2 = name2;
			this.w = w;
		}

		public Paint getColor() {
			// String ret = "";

			switch (w) {
			case Master_instance:
				return Color.white;
			// case SOURCE_Enriched_instance:
			// return Color.;
			case SOURCE_Instance:
				return Color.black;
			case SOURCE_Migrated_instance:
				return Color.DARK_GRAY;
			case Unioned_instance:
				return Color.gray;
			case Universal_instance:
				return Color.LIGHT_GRAY;

			case SOURCE_Roundtrip_instance:
				return Color.gray;
			default:
				break;

			}
			;
			return Color.PINK;
		}

		public String toString2() {
			// String ret = "";

			switch (w) {
			case Master_instance:
				return "Master";
			case Master_schema:
				break;
			case POINT:
				return "";
			case SOURCE:
				break;
			case SOURCE_Constraints:
				break;
			case SOURCE_Enriched_instance:
				return name + " enriched";
			case SOURCE_Inclusion_mapping:
				return "sigma";
			case SOURCE_Instance:
				return name;
			case SOURCE_Migrated_constraints:
				break;
			case SOURCE_Migrated_instance:
				return name + " migrated";
			case SOURCE_Projection_query:
				break;
			case SOURCE_Roundtrip_instance:
				return name + " roundtrip";
			case SOURCE_Roundtrip_lineage:
				return "lineage";
			case SOURCE_Schema:
				break;
			case Source_exchange:
				break;
			case Sources:
				break;
			case Summary:
				break;
			case TARGET:
				break;
			case TARGET_Constraints:
				break;
			case TARGET_Instance:
				break;
			case TARGET_Query:
				break;
			case TARGET_Roundtrip_instance:
				break;
			case TARGET_Roundtrip_lineage:
				break;
			case TARGET_Schema:
				break;
			case TARGET_Verification:
				break;
			case Targets:
				break;
			case Universal_transform:
				return "chase";
			case Unioned_instance:
				return "Union";
			case Universal_constraints:
				break;
			case Universal_instance:
				return "Universal";
			case Universal_instance_dedup:
				break;
			case Universal_schema:
				break;
			case Universal_to_master_mapping:
				return "sigma";
			case SOURCE_Coproduct_inclusion:
				return "include";
			default:
				break;

			}
			;
			return "oops";
		}

		@Override
		public String toString() {

			if (w.equals(WarehouseWhich.SOURCE) || w.equals(WarehouseWhich.TARGET)) {
				return name;
			}
			if (name2 != null) {
				return name2;
			}
			return w.toString().replace("SOURCE_", "").replace("TARGET_", "");
		}

		public JComponent makeUI() {
			var viewer = new CqlViewer(64, new AqlEnv(new Program<>(Collections.emptyList(), "")), true);
			JTabbedPane pane = new JTabbedPane();
			switch (w) {
			case POINT:
				var zzz = warehouse.pointToPoint(name, name2);
				pane.addTab("Text", new CodeTextPanel("", zzz.toString()));
				viewer.visit(name2, pane, zzz);
				break;
			case Targets:
				break;
			case Source_exchange:
				break;
			case SOURCE:
				break;
			case TARGET:
				break;
			case Sources:
				break;
			case Summary:
				pane.addTab("Text", new CodeTextPanel("", "todo"));
				break;
			case SOURCE_Schema:
				pane.addTab("Text", new CodeTextPanel("", warehouse.getSources().get(name).first.toString()));
				viewer.visit(name, pane, warehouse.getSources().get(name).first);
				break;
			case SOURCE_Constraints:
				pane.addTab("Text", new CodeTextPanel("", warehouse.getSources().get(name).second.toString()));
				break;
			case SOURCE_Migrated_instance:
				viewer.visit(WarehouseWhich.SOURCE_Migrated_instance.toString(), pane, warehouse.getFwdInstance(name));
				pane.addTab("Text", new CodeTextPanel("", warehouse.getFwdInstance(name).toString()));
				break;
			case SOURCE_Instance:
				viewer.visit(name, pane, warehouse.getSources().get(name).third);
				pane.addTab("Text", new CodeTextPanel("", warehouse.getSources().get(name).third.toString()));
				break;
			case SOURCE_Inclusion_mapping:
				viewer.visit(name, pane, warehouse.getInclusionMapping(name));
				pane.addTab("Text", new CodeTextPanel("", warehouse.getInclusionMapping(name).toString()));
				break;
			case SOURCE_Enriched_instance:
				viewer.visit(WarehouseWhich.SOURCE_Enriched_instance.toString(), pane, warehouse.getEnriched(name));
				pane.addTab("Text", new CodeTextPanel("", warehouse.getEnriched(name).toString()));
				break;
			case SOURCE_Roundtrip_instance:
				viewer.visit(name, pane, warehouse.getRoundTripUnit(name).dst());
				pane.addTab("Text", new CodeTextPanel("", warehouse.getRoundTripUnit(name).dst().toString()));
				break;
			case SOURCE_Projection_query:
				var w = warehouse.getRoundTripQuery(name);
				viewer.visit(name, pane, w);
				pane.addTab("Text", new CodeTextPanel("", w.toString()));
				break;
			case SOURCE_Roundtrip_lineage:
				viewer.visit(name, pane, warehouse.getRoundTripUnit(name));
				break;
			case TARGET_Constraints:
				pane.addTab("Text", new CodeTextPanel("", warehouse.getTargets().get(name).second.toString()));
				break;
			case TARGET_Query:
				viewer.visit(name, pane, warehouse.getTargets().get(name).third);
				pane.addTab("Text", new CodeTextPanel("", warehouse.getTargets().get(name).third.toString()));
				break;
			case TARGET_Roundtrip_lineage:
				pane.addTab("Text", new CodeTextPanel("", "todo"));
				break;
			case TARGET_Roundtrip_instance:
				pane.addTab("Text", new CodeTextPanel("", "todo"));
				break;
			case TARGET_Schema:
				viewer.visit(name, pane, warehouse.getTargets().get(name).first);
				pane.addTab("Text", new CodeTextPanel("", warehouse.getTargets().get(name).first.toString()));
				break;
			case Unioned_instance:
				viewer.visit(WarehouseWhich.Unioned_instance.toString(), pane, warehouse.getUnionInstance());
				pane.addTab("Text", new CodeTextPanel("", warehouse.getUnionInstance().toString()));
				break;
			case Universal_instance:
				viewer.visit(WarehouseWhich.Universal_instance.toString(), pane, warehouse.getUniversalInstance());
				pane.addTab("Text", new CodeTextPanel("", warehouse.getUniversalInstance().toString()));
				break;
			case Universal_transform:
				viewer.visit(WarehouseWhich.Universal_instance.toString(), pane,
						warehouse.getTransformUnionToUniversal());
				pane.addTab("Text", new CodeTextPanel("", warehouse.getTransformUnionToUniversal().toString()));
				break;
			case Universal_instance_dedup:
				pane.addTab("Text", new CodeTextPanel("", "todo"));
				break;
			case Universal_schema:
				viewer.visit(WarehouseWhich.Universal_schema.toString(), pane, warehouse.getUniversalSchema());
				pane.addTab("Text", new CodeTextPanel("", warehouse.getUniversalSchema().toString()));
				break;
			case SOURCE_Migrated_constraints:
				pane.addTab("Text", new CodeTextPanel("", warehouse.getFwdConstraints(name).toString()));
				break;
			case Universal_constraints:
				pane.addTab("Text", new CodeTextPanel("", warehouse.getUniversalConstraints().toString()));
				break;
			case TARGET_Instance:
				viewer.visit(name, pane, warehouse.getResult(name, true));
				pane.addTab("Text", new CodeTextPanel("", warehouse.getResult(name, true).toString()));
				break;

			case Master_instance:
				viewer.visit(WarehouseWhich.Master_instance.toString(), pane, warehouse.getMasterInstance());
				pane.addTab("Text", new CodeTextPanel("", warehouse.getMasterInstance().toString()));

				break;
			case Master_schema:
				viewer.visit(WarehouseWhich.Master_schema.toString(), pane, warehouse.getMasterSchema());
				pane.addTab("Text", new CodeTextPanel("", warehouse.getUniversalSchema().toString()));
				break;
			case Universal_to_master_mapping:
				viewer.visit(WarehouseWhich.Master_schema.toString(), pane, warehouse.getColimit().fromPsuedo);
				pane.addTab("Text", new CodeTextPanel("", warehouse.getColimit().fromPsuedo.toString()));
				break;
			case TARGET_Verification:
				var v = warehouse.isTargetConstraintsOk(name);
				String s = (v.left && v.l) ? "Verification Succeeded" : ("Verification Failed " + (v.left ? "" : v.r));
//					(v.left && !v.l) ? "Verification Failed" : "" 
				pane.addTab("Text", new CodeTextPanel("", s));
				break;
			default:
				break;

			}

			return pane;
		}

		public void show() {
			if (!cache.containsKey(this)) {
				JComponent c = makeUI();
				cache.put(this, c);
			}

			parent.removeAll();
			parent.add(cache.get(this));
			parent.validate();
			parent.repaint();
		}

	}

	JComponent parent = new JPanel(new GridLayout(1, 1));

	public static enum WarehouseKind {
		Virtualized, Materialized, Input, Temporary, NONE;
	}

	public static enum WarehouseWhich {
		SOURCE_Schema(WarehouseKind.Input), SOURCE_Instance(WarehouseKind.Input),
		SOURCE_Constraints(WarehouseKind.Input), SOURCE_Migrated_constraints(WarehouseKind.NONE),
		SOURCE_Migrated_instance(WarehouseKind.Materialized), SOURCE_Inclusion_mapping(WarehouseKind.NONE),
		SOURCE_Projection_query(WarehouseKind.NONE), SOURCE_Coproduct_inclusion(WarehouseKind.NONE),
		SOURCE_Roundtrip_lineage(WarehouseKind.Virtualized), SOURCE_Roundtrip_instance(WarehouseKind.Virtualized),
		SOURCE_Enriched_instance(WarehouseKind.Virtualized), Sources(WarehouseKind.NONE),
		TARGET_Schema(WarehouseKind.Input), TARGET_Constraints(WarehouseKind.Input), TARGET_Query(WarehouseKind.Input),
		TARGET_Roundtrip_lineage(WarehouseKind.Input), TARGET_Roundtrip_instance(WarehouseKind.Input),
		Targets(WarehouseKind.NONE), Universal_schema(WarehouseKind.NONE), Master_schema(WarehouseKind.NONE),
		Unioned_instance(WarehouseKind.Virtualized), Universal_instance(WarehouseKind.Materialized),
		Universal_transform(WarehouseKind.NONE), Master_instance(WarehouseKind.Materialized),
		Universal_to_master_mapping(WarehouseKind.NONE), Universal_constraints(WarehouseKind.NONE),
		Summary(WarehouseKind.NONE), SOURCE(WarehouseKind.NONE), TARGET(WarehouseKind.NONE),
		TARGET_Verification(WarehouseKind.NONE), TARGET_Instance(WarehouseKind.Virtualized),
		Source_exchange(WarehouseKind.Materialized), Universal_instance_dedup(WarehouseKind.Materialized),
		POINT(WarehouseKind.NONE);

		public WarehouseKind kind;

		private WarehouseWhich(WarehouseKind kind) {
			this.kind = kind;
		}

		@Override
		public String toString() {
			if (kind != WarehouseKind.NONE) {
				return super.toString() + " (" + kind.toString() + ")";
			}
			return super.toString();
		}

	}

	DefaultMutableTreeNode root = new DefaultMutableTreeNode(new WarehouseTreeNode(null, WarehouseWhich.Summary, null));
	JTree tree = new JTree(root);

	public void makeViewer(JSplitPane jsp) {
		addNodes();

		tree.setRootVisible(false);

		tree.setShowsRootHandles(true);
		tree.setEditable(false);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.addTreeSelectionListener((TreeSelectionEvent e) -> {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
			if (node == null)
				return;
			((WarehouseTreeNode) node.getUserObject()).show();
		});

		for (int i = 0; i < tree.getRowCount(); i++) {
			tree.expandRow(i);
		}

		jsp.add(new JScrollPane(tree));
		jsp.add(parent); // new JScrollPane(parent));
	}

	Graph<DefaultMutableTreeNode, DefaultMutableTreeNode> sgv;

	private class MyVertexT implements VertexLabelRenderer, EdgeLabelRenderer {

		public MyVertexT() {
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> Component getVertexLabelRendererComponent(JComponent arg0, Object arg1, Font arg2, boolean arg3,
				T arg4) {
			WarehouseTreeNode p = (WarehouseTreeNode) ((DefaultMutableTreeNode) arg4).getUserObject();
			if (arg3) {
				var z = new TreePath(((DefaultTreeModel) tree.getModel()).getPathToRoot((DefaultMutableTreeNode) arg4));
				tree.setSelectionPath(z);
				tree.scrollPathToVisible(z);
				tree.invalidate();
				p.show();
			}
			return new JLabel(p.toString2());
		}

		@Override
		public <E> Component getEdgeLabelRendererComponent(JComponent arg0, Object arg1, Font arg2, boolean arg3,
				E arg4) {
			WarehouseTreeNode p = (WarehouseTreeNode) ((DefaultMutableTreeNode) arg4).getUserObject();
			if (arg3) {
				var z = new TreePath(((DefaultTreeModel) tree.getModel()).getPathToRoot((DefaultMutableTreeNode) arg4));
				tree.setSelectionPath(z);
				tree.scrollPathToVisible(z);
				tree.invalidate();
				p.show();
			}
			return new JLabel(p.toString2());
		}

		@Override
		public boolean isRotateEdgeLabels() {
			return true;
		}

		@Override
		public void setRotateEdgeLabels(boolean arg0) {

		}

	}

	private JComponent viewGraph() {

		if (sgv.getVertexCount() == 0) {
			return new JPanel();
		}
		Layout<DefaultMutableTreeNode, DefaultMutableTreeNode> layout = new CircleLayout<>(sgv);

		layout.setSize(new Dimension(860, 860));
		VisualizationViewer<DefaultMutableTreeNode, DefaultMutableTreeNode> vv = new VisualizationViewer<>(layout);
		Function<DefaultMutableTreeNode, Paint> vertexPaint = x -> ((WarehouseTreeNode) x.getUserObject()).getColor();
		DefaultModalGraphMouse<DefaultMutableTreeNode, DefaultMutableTreeNode> gm = new DefaultModalGraphMouse<>();
		gm.setMode(Mode.TRANSFORMING);
		vv.setGraphMouse(gm);
		gm.setMode(Mode.PICKING);
		vv.getRenderContext().setVertexFillPaintTransformer(vertexPaint);

		vv.getRenderContext().setVertexLabelRenderer(new MyVertexT());
		vv.getRenderContext().setEdgeLabelRenderer(new MyVertexT());

		Function<DefaultMutableTreeNode, String> et = x -> ((WarehouseTreeNode) x.getUserObject()).toString();
		Function<DefaultMutableTreeNode, String> vt = x -> ((WarehouseTreeNode) x.getUserObject()).toString2();
		vv.getRenderContext().setEdgeLabelTransformer(et);
		vv.getRenderContext().setVertexLabelTransformer(vt);
		GraphZoomScrollPane zzz = new GraphZoomScrollPane(vv);
		JPanel ret = new JPanel(new GridLayout(1, 1));
		ret.add(zzz);
		ret.setBorder(BorderFactory.createEtchedBorder());

		vv.getRenderContext().setLabelOffset(16);
		vv.setBackground(Color.white);

		return ret;
	}

	private void addNodes() {
		sgv = new DelegateForest<>();
		root.removeAllChildren();
		var srcs = new DefaultMutableTreeNode(new WarehouseTreeNode(null, WarehouseWhich.Sources, null));
		var usch = new DefaultMutableTreeNode(new WarehouseTreeNode(null, WarehouseWhich.Universal_schema, null));
		var coprd = new DefaultMutableTreeNode(new WarehouseTreeNode(null, WarehouseWhich.Unioned_instance, null));
		var ueds = new DefaultMutableTreeNode(new WarehouseTreeNode(null, WarehouseWhich.Universal_constraints, null));
		var uinst = new DefaultMutableTreeNode(new WarehouseTreeNode(null, WarehouseWhich.Universal_instance, null));
		var utrans = new DefaultMutableTreeNode(new WarehouseTreeNode(null, WarehouseWhich.Universal_transform, null));
		var msch = new DefaultMutableTreeNode(new WarehouseTreeNode(null, WarehouseWhich.Master_schema, null));
		var minst = new DefaultMutableTreeNode(new WarehouseTreeNode(null, WarehouseWhich.Master_instance, null));
		var mm = new DefaultMutableTreeNode(
				new WarehouseTreeNode(null, WarehouseWhich.Universal_to_master_mapping, null));
		var dsts = new DefaultMutableTreeNode(new WarehouseTreeNode(null, WarehouseWhich.Targets, null));

		root.add(srcs);
		root.add(usch);
		root.add(ueds);
		root.add(coprd);
		// sgv.addVertex(coprd);
		root.add(uinst);
		// sgv.addVertex(uinst);
		root.add(msch);
		root.add(utrans);
		// sgv.addEdge(utrans, coprd, uinst);
		root.add(minst);
		// sgv.addVertex(minst);
		root.add(mm);
//		sgv.addEdge(mm, uinst, minst);
		root.add(dsts);

		Map<String, DefaultMutableTreeNode> map = new HashMap<>();
		Map<Pair<String, String>, DefaultMutableTreeNode> map2 = new HashMap<>();
		// root.add(core);
		if (warehouse != null) {
			for (var n : warehouse.getSources().keySet()) {
				var src = new DefaultMutableTreeNode(new WarehouseTreeNode(n, WarehouseWhich.SOURCE, null));
				srcs.add(src);

				src.add(new DefaultMutableTreeNode(new WarehouseTreeNode(n, WarehouseWhich.SOURCE_Schema, null)));
				src.add(new DefaultMutableTreeNode(new WarehouseTreeNode(n, WarehouseWhich.SOURCE_Constraints, null)));
				var w = new DefaultMutableTreeNode(new WarehouseTreeNode(n, WarehouseWhich.SOURCE_Instance, null));
				src.add(w);
				sgv.addVertex(w);
				map.put(n, w);

				var jj = new DefaultMutableTreeNode(
						new WarehouseTreeNode(n, WarehouseWhich.SOURCE_Inclusion_mapping, null));
				src.add(jj);
				var j = new DefaultMutableTreeNode(
						new WarehouseTreeNode(n, WarehouseWhich.SOURCE_Projection_query, null));
				src.add(j);
				var ww = new DefaultMutableTreeNode(
						new WarehouseTreeNode(n, WarehouseWhich.SOURCE_Migrated_instance, null));
				src.add(ww);
				// sgv.addVertex(ww);

				// sgv.addEdge(jj, w, ww);

				src.add(new DefaultMutableTreeNode(
						new WarehouseTreeNode(n, WarehouseWhich.SOURCE_Migrated_constraints, null)));

				var gg = new DefaultMutableTreeNode(
						new WarehouseTreeNode(n, WarehouseWhich.SOURCE_Roundtrip_instance, null));
				src.add(gg);
//				sgv.addVertex(gg);

				var cpi = new DefaultMutableTreeNode(
						new WarehouseTreeNode(n, WarehouseWhich.SOURCE_Coproduct_inclusion, null));
				src.add(cpi);
				// sgv.addEdge(cpi, ww, coprd);

				var eee = new DefaultMutableTreeNode(
						new WarehouseTreeNode(n, WarehouseWhich.SOURCE_Roundtrip_lineage, null));
				src.add(eee);

				var tt = new DefaultMutableTreeNode(
						new WarehouseTreeNode(n, WarehouseWhich.SOURCE_Enriched_instance, null));
				src.add(tt);

				var p2p = new DefaultMutableTreeNode(new WarehouseTreeNode(n, WarehouseWhich.Source_exchange, null));
				src.add(p2p);

				for (var n2 : warehouse.getSources().keySet()) {
					// if (n.equals(n2)) {
					// continue;
					// }
					var rrr = new DefaultMutableTreeNode(new WarehouseTreeNode(n, WarehouseWhich.POINT, n2));
					p2p.add(rrr);
					if (warehouse.pointToPoint(n, n2).size() > 0) {
						map2.put(new Pair<>(n, n2), rrr);
					}
//					sgv.addEdge(d, src, new DefaultMutableTreeNode(new WarehouseTreeNode(n, WarehouseWhich.SOURCE, null));

				}
			}

			for (var e : map2.entrySet()) {

				sgv.addEdge(e.getValue(), map.get(e.getKey().first), map.get(e.getKey().second));
			}

			for (var q : warehouse.getTargets().keySet()) {

				var dst = new DefaultMutableTreeNode(new WarehouseTreeNode(q, WarehouseWhich.TARGET, null));
				dsts.add(dst);

				dst.add(new DefaultMutableTreeNode(new WarehouseTreeNode(q, WarehouseWhich.TARGET_Schema, null)));
				dst.add(new DefaultMutableTreeNode(new WarehouseTreeNode(q, WarehouseWhich.TARGET_Constraints, null)));
				dst.add(new DefaultMutableTreeNode(new WarehouseTreeNode(q, WarehouseWhich.TARGET_Query, null)));
				dst.add(new DefaultMutableTreeNode(new WarehouseTreeNode(q, WarehouseWhich.TARGET_Verification, null)));
				dst.add(new DefaultMutableTreeNode(new WarehouseTreeNode(q, WarehouseWhich.TARGET_Instance, null)));
				dst.add(new DefaultMutableTreeNode(
						new WarehouseTreeNode(q, WarehouseWhich.TARGET_Roundtrip_instance, null)));
				dst.add(new DefaultMutableTreeNode(
						new WarehouseTreeNode(q, WarehouseWhich.TARGET_Roundtrip_lineage, null)));
			}
		}

		tree.setModel(new DefaultTreeModel(root));
		for (int i = 0; i < tree.getRowCount(); i++) {
			tree.expandRow(i);
		}
		tree.setExpandsSelectedPaths(true);
		tree.revalidate();
	}

	public Warehouse<String, String> warehouse; // = new WarehouseImpl();
	JTextField epath = new JTextField("/Users/ryan/Downloads/E/PROVER/eprover");

	public JPanel makeUI() {

		JButton setSourceButton = new JButton("Set Sources");
//		JButton delSourceButton = new JButton("Remove Sources");
		JButton showSourceButton = new JButton("Show Sources");

		JButton setEntityLinksButton = new JButton("Set Links");
//		JButton delEntityLinksButton = new JButton("Remove Links");
		JButton showEntityLinksButton = new JButton("Show Links");
//		JButton suggestEntityLinksButton = new JButton("Suggest Links");

		JButton setTargetButton = new JButton("Set Targets");
//		JButton delTargetButton = new JButton("Remove Targets");
		JButton showTargetButton = new JButton("Show Targets");
//		JButton suggestTargetButton = new JButton("Suggest Targets");

		showSourceButton.addActionListener(x -> doShowSources());
		showEntityLinksButton.addActionListener(x -> doShowLinks());
		showTargetButton.addActionListener(x -> doShowTargets());

		setSourceButton.addActionListener(x -> doAddSourcesWrapper());
		setEntityLinksButton.addActionListener(x -> doAddLinksWrapper());
		setTargetButton.addActionListener(x -> doAddTargetsWrapper());

		// delSourceButton.addActionListener(x -> doRemoveSourcesWrapper());
		// delEntityLinksButton.addActionListener(x -> doRemoveLinksWrapper());
		// delTargetButton.addActionListener(x -> doRemoveTargetsWrapper());

		// suggestEntityLinksButton.setEnabled(false);
		// suggestTargetButton.setEnabled(false);

		JComboBox<Example> exampleBox = new JComboBox<>();
		exampleBox.addItem(new SynchExample());
		exampleBox.addItem(new SynchExampleHidden());
		exampleBox.addItem(new SynchExampleSql());
		exampleBox.addItem(new SynchExample2());
		exampleBox.addItem(new PharmaExample());
		exampleBox.addItem(new NISTExample());
		exampleBox.addItem(new EHRExample());
		//exampleBox.addItem(new OilExample());
		
		exampleBox.setSelectedIndex(-1);
		exampleBox.addActionListener(x -> doExample((Example) exampleBox.getSelectedItem()));

		exampleBox.addActionListener(exampleBox);

		// JPanel p = new JPanel(new GridLayout(1, 2));

		JPanel topPanel = new JPanel(new GridLayout(1, 7));
		topPanel.add(setSourceButton);
		topPanel.add(showSourceButton);
		topPanel.add(setEntityLinksButton);
		topPanel.add(showEntityLinksButton);
		topPanel.add(setTargetButton);
		topPanel.add(showTargetButton);
		topPanel.add(epath);
		topPanel.add(exampleBox);

		// topPanel.add(delSourceButton);
		// topPanel.add(suggestEntityLinksButton);
		// topPanel.add(delEntityLinksButton);
		// topPanel.add(suggestTargetButton);
		// topPanel.add(delTargetButton);

//		topPanel.add(new JLabel("Example", JLabel.RIGHT));

		JSplitPane jsp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

		JPanel ret = new JPanel(new BorderLayout());
		ret.add(topPanel, BorderLayout.NORTH);
		makeViewer(jsp);
		ret.add(jsp, BorderLayout.CENTER);
		return ret;
	}

	AqlEnv last_env;

	private void doSetSources(Program<Exp<?>> prog) {
		var x = new AqlMultiDriver(prog, null);
		x.start();
		last_env = x.env;
		List<String> o = new LinkedList<>();
		Map<String, Triple<Schema<String, String, Sym, Fk, Att>, Constraints, Instance<String, String, Sym, Fk, Att, Object, Object, Object, Object>>> map = new HashMap<>();
		for (var e : last_env.prog.order) {
			var v0 = prog.exps.get(e);
			if (!v0.kind().equals(Kind.SCHEMA)) {
				continue;
			}
			if ((v0 instanceof SchExpMsCatalog)) {
				continue;
			}
			map.put(e, new Triple<>(last_env.defs.schs.get(e), last_env.defs.eds.get(e + "Constraints"),
					last_env.defs.insts.get(e + "Instance")));
			o.add(e);

		}

		warehouse.setSources(map);
	}

	private void doShow(String title, String content) {
		if (content.isBlank()) {
			return;
		}
		JFrame f = new JFrame(title);
		f.add(new CodeTextPanel(null, content));
		f.setSize(600, 400);
		f.setLocationRelativeTo(null);
		f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		f.setVisible(true);
	}

	private void doShowSources() {
		doShow("Sources", printSources());
	}

	private void doShowLinks() {
		doShow("Links", printLinks());
	}

	private String printSources() {
		StringBuffer sb = new StringBuffer();
		for (var x : warehouse.getSources().entrySet()) {
			sb.append("schema " + x.getKey() + " = literal : sql {\n" + x.getValue().first);
			sb.append("\n}\n\n");
			sb.append("constraints " + x.getKey() + "Constraints = literal : " + x.getKey() + " {\n"
					+ x.getValue().second);
			sb.append("\n}\n\n");
			sb.append("instance " + x.getKey() + "Instance = literal : " + x.getKey() + " {\n" + x.getValue().third);
			sb.append("\n}\n\n");
		}
		return sb.toString();
	}

	private String printLinks() {
		return warehouse.getLinks().toString();
	}

	private String printTargets() {
		StringBuffer sb = new StringBuffer();
		for (var x : warehouse.getTargets().entrySet()) {
			sb.append("schema " + x.getKey() + " = literal : sql {\n" + x.getValue().first);
			sb.append("\n}\n\n");
			sb.append("constraints " + x.getKey() + "Constraints = literal : " + x.getKey() + " {\n"
					+ x.getValue().second);
			sb.append("\n}\n\n");
			sb.append("query " + x.getKey() + "Query = literal : getSchema Warehouse -> " + x.getKey() + " {\n"
					+ x.getValue().third);
			sb.append("\n}\n\n");
		}
		return sb.toString();
	}

	private void doShowTargets() {
		doShow("Targets", printTargets());
	}

	private void doSetLinks(Links<String, String> links) {
		warehouse.setLinks(links);
	}

	private void doSetTargets(Program<Exp<?>> prog) {
		prog.exps.put("Warehouse", new ColimSchExpLit(warehouse.getColimit()));
		prog.order.add(0, "Warehouse");
		prog.options.options.put(AqlOption.allow_aggregation_unsafe, true);
		// last_env.defs.scs.put("Warehouse", warehouse.getColimit());
		var x = new AqlMultiDriver(prog, null);
		// System.out.println(prog);

		x.start();
		last_env = x.env;

		Map<String, Triple<Schema<String, String, Sym, Fk, Att>, Constraints, Query<String, String, Sym, Fk, Att, String, Fk, Att>>> map = new HashMap<>();
		for (var v : prog.exps.entrySet()) {
			if (!v.getValue().kind().equals(Kind.SCHEMA)) {
				continue;
			}
			map.put(v.getKey(), new Triple<>(last_env.defs.schs.get(v.getKey()),
					last_env.defs.eds.get(v.getKey() + "Constraints"), last_env.defs.qs.get(v.getKey() + "Query")));

		}
		warehouse.setTargets(map);
	}

	private void doAddTargetsWrapper() {
		CodeTextPanel field = new CodeTextPanel("", printTargets());
		field.setPreferredSize(new Dimension(400, 400));
		int n = JOptionPane.showConfirmDialog(null, field, "Set Targets", JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE, null);
		if (n != JOptionPane.OK_OPTION) {
			return;
		}
		String s = field.getText().trim();
		if (s.isBlank()) {
			return;
		}
		try {
			var x = new CombinatorParser().parseProgram(field.getText());
			doSetTargets(x);
			doIt();
		} catch (Exception ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(null, ex.getMessage());
		}
	}

	private void doRemoveTargetsWrapper() {
		var field = new JTextField(Util.sep(warehouse.targets.keySet(), " "));
		int n = JOptionPane.showConfirmDialog(null, field, "Remove Targets", JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE, null);
		if (n != JOptionPane.OK_OPTION) {
			return;
		}
		String s = field.getText().trim();
		if (s.isBlank()) {
			return;
		}
		try {
			doRemoveTargets(Arrays.asList(s.split("\\s+")));
			doIt();
		} catch (Exception ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(null, ex.getMessage());
		}
	}

	private void doRemoveSourcesWrapper() {
		var field = new JTextField(Util.sep(warehouse.sources.keySet(), " "));
		int n = JOptionPane.showConfirmDialog(null, field, "Remove Sources", JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE, null);
		if (n != JOptionPane.OK_OPTION) {
			return;
		}
		String s = field.getText().trim();
		if (s.isBlank()) {
			return;
		}
		try {
			doRemoveSources(Arrays.asList(s.split("\\s+")));
			doIt();
		} catch (Exception ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(null, ex.getMessage());
		}
	}

	private void doRemoveLinksWrapper() {
		var field = new JTextField(Util.sep(warehouse.links.keySet(), " "));
		int n = JOptionPane.showConfirmDialog(null, field, "Remove Links", JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE, null);
		if (n != JOptionPane.OK_OPTION) {
			return;
		}
		String s = field.getText().trim();
		if (s.isBlank()) {
			return;
		}
		try {
			doRemoveLinks(Arrays.asList(s.split("\\s+")));
			doIt();
		} catch (Exception ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(null, ex.getMessage());
		}
	}

	private void doRemoveTargets(Collection<String> c) {
		warehouse.removeTargets(c);
	}

	private void doRemoveSources(Collection<String> c) {
		warehouse.removeSources(c);
	}

	private void doRemoveLinks(Collection<String> c) {
		warehouse.removeLinks(c);
	}

	private void doAddLinksWrapper() {
		CodeTextPanel field = new CodeTextPanel("", printLinks());
		field.setPreferredSize(new Dimension(400, 400));
		int n = JOptionPane.showConfirmDialog(null, field, "Set Links", JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE, null);
		if (n != JOptionPane.OK_OPTION) {
			return;
		}
		String s = field.getText().trim();
		if (s.isBlank()) {
			return;
		}
		try {
			Links<String, String> x = CombinatorParser.parseLinks(field.getText());
			doSetLinks(x);
			doIt();
		} catch (Exception ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(null, ex.getMessage());
		}
	}

	private void doAddSourcesWrapper() {
		CodeTextPanel field = new CodeTextPanel("", printSources());
		field.setPreferredSize(new Dimension(400, 400));
		int n = JOptionPane.showConfirmDialog(null, field, "Set Sources", JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE, null);
		if (n != JOptionPane.OK_OPTION) {
			return;
		}
		String s = field.getText().trim();
		if (s.isBlank()) {
			return;
		}
		try {
			var x = new CombinatorParser().parseProgram(field.getText());
			doSetSources(x);
			doIt();
		} catch (Exception ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(null, ex.getMessage());
		}
	}

	private void doIt() {
		try {

			warehouse.run();

			cache.clear();
			parent.removeAll();
			parent.add(new JPanel());
			parent.validate();
			parent.repaint();
			tree.clearSelection();
			// tree.setSelect

			addNodes();

		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, e.getMessage());
		}
	}

	
	public void doExample(Example ex) {
		if (ex == null) {
			return;
		}
		JFrame f2 = new JFrame("Running");
		JProgressBar j = new JProgressBar(0,100);
		//j.setM
		//j.setIndeterminate(true);
		j.setValue(0);
		j.setStringPainted(true);
		JPanel p = new JPanel(new GridLayout(1,1));
		p.add(j);
		f2.add(p);
		f2.setSize(200, 40);
		f2.setLocationRelativeTo(null);
		f2.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		f2.setVisible(true);
		
		Runnable r = new Runnable() {
			public void run() {
				try {
					warehouse = new WarehouseImpl(epath.getText(), ex.getOptions(), ex.push());
					cache.clear();
					//System.out.println(epath.getText() + "\n\n" + ex.getOptions() + "\n\n" + ex.getSources());
					doSetSources(new CombinatorParser().parseProgram(ex.getOptions() + "\n\n" + ex.getSources()));
					j.setValue(25);
					
				//	System.out.println("asfskldfjhsdlkf");
					
					warehouse.setLinks(CombinatorParser.parseLinks(ex.getLinks()));
					j.setValue(50);
					
					
					
					warehouse.run();
					j.setValue(75);
					
					doSetTargets(new CombinatorParser().parseProgram(ex.getTargets()));

					j.setValue(100);
					
					addNodes();

					JFrame f = new JFrame(ex.getName() + " Point-to-Point Exchange");
					f.add(viewGraph());
					f.setSize(900, 900);
					f.setLocationRelativeTo(null);
					f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
					f.setVisible(true);

					f = new JFrame("Example Definition");
					f.add(new CodeTextPanel("", ex.getOptions() + "\n\n" + ex.getSources() + "\n\n" + ex.getLinks()
							+ "\n\n" + ex.getTargets()));
					f.setSize(600, 600);
					f.setLocationRelativeTo(null);
					f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
					f.setVisible(true);
					
					f2.dispose();

				} catch (Exception e) {
					e.printStackTrace();
					JOptionPane.showMessageDialog(null, e.getMessage());
				}
			};
		};
		Thread t = new Thread(r);
		t.start();
		
		
		
		
	}
}
