package catdata.aql.gui;

import java.awt.BasicStroke;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;

import com.google.common.base.Function;

import catdata.LineException;
import catdata.Pair;
import catdata.Program;
import catdata.Triple;
import catdata.Util;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Instance;
import catdata.aql.Kind;
import catdata.aql.Semantics;
import catdata.aql.exp.AqlEnv;
import catdata.aql.exp.AqlTyping;
import catdata.aql.exp.ColimSchExp;
import catdata.aql.exp.ColimSchExp.ColimSchExpVar;
import catdata.aql.exp.CommentExp;
import catdata.aql.exp.Exp;
import catdata.aql.exp.InstExp;
import catdata.aql.exp.SchExp;
import catdata.aql.exp.SchExpColim;
import catdata.ide.CodeTextPanel;
import catdata.ide.Disp;
import catdata.ide.GuiUtil;
import catdata.ide.LazyPanel;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse.Mode;
import gnu.trove.map.hash.THashMap;

//TODO aql suppress instance equations - do not compute/display if not required - maybe make instance an interface
public final class AqlDisplay implements Disp {

	private final Throwable exn;
	private String title;
	private AqlEnv env;

	@Override
	public Throwable exn() {
		return exn;
	}

	@Override
	public void close() {
		env = null;
	}

	public static String doLookup(boolean prefix, String c, Kind k, AqlTyping typing) {
		String s = prefix ? k + " " + c : c;
		if (!typing.defs.keySet().contains(c)) {
			return s;
		}
		switch (k) {
		case THEORY_MORPHISM:
			Pair p = (Pair) typing.defs.tms.get(c);
			return s + " : " + p.first + " -> " + p.second;			
		case INSTANCE:
			return s + " : " + typing.defs.insts.get(c);
		case MAPPING:
			return s + " : " + typing.defs.maps.get(c).first + " -> " + typing.defs.maps.get(c).second;
		case PRAGMA:
			return s;
		case QUERY:
			return s + " : " + typing.defs.qs.get(c).first + " -> " + typing.defs.qs.get(c).second;
		case SCHEMA:
			return s;
		case TRANSFORM:
			return s + " : " + typing.defs.trans.get(c).first + " -> " + typing.defs.trans.get(c).second;
		case TYPESIDE:
			return s;
		case GRAPH:
			return s;
		case COMMENT:
			return s;
		case SCHEMA_COLIMIT:
			return s;
		case CONSTRAINTS:
			return s + " : " + typing.defs.eds.get(c);
		default:
			throw new RuntimeException("Anomaly: please report");
		}

	}

	private static int getMaxSize(Exp<?> exp, AqlEnv env) {
		switch (exp.kind()) {
		case INSTANCE:
		case TRANSFORM:
			return (Integer) exp.getOrDefault(env, AqlOption.gui_max_table_size);

		case PRAGMA:
		case CONSTRAINTS:
			return (Integer) exp.getOrDefault(env, AqlOption.gui_max_string_size);

		case MAPPING:
		case THEORY_MORPHISM:
		case QUERY:
		case SCHEMA:
		case SCHEMA_COLIMIT:
		case GRAPH:
		case TYPESIDE:
			return (Integer) exp.getOrDefault(env, AqlOption.gui_max_graph_size);

		case COMMENT:
			return 0;

		default:
			throw new RuntimeException("Anomaly: please report");
		}
	}

	private static JComponent wrapDisplay(String c, Exp<?> exp, Semantics obj, AqlEnv env) {
		int maxSize = getMaxSize(exp, env);
		int sampleSize = (int) exp.getOrDefault(env, AqlOption.gui_sample_size);
		boolean doSample = (boolean) exp.getOrDefault(env, AqlOption.gui_sample);

		if (obj.size() > maxSize) {
			String s = doSample ? obj.sample(sampleSize) : null;
			s = s == null ? "" : "\n\nSample (may not include all tables, columns, or rows):\n\n" + s;
			return new CodeTextPanel("", "Display supressed, size " + obj.size()
					+ ".\n\nSee manual for a description of size, or try options gui_max_Z_size = X for X > "
					+ obj.size()
					+ " (the size of this object) and Z one of table, graph, string.  \n\nWarning: sizes that are too large will hang the viewer.\n\nCompute time: "
					+ env.performance.get(c) + s);
		}

		boolean showAtts = (boolean) exp.getOrDefault(env, AqlOption.gui_show_atts);
		int max_rows = (int) exp.getOrDefault(env, AqlOption.gui_rows_to_display);
//		JComponent comp = AqlViewer.view(obj, max_rows, exp, env, showAtts);
		LazyPanel p = new LazyPanel((u) -> AqlViewer.view(c, obj, max_rows, exp, env, showAtts));
		return p;
	}

	public AqlDisplay(String title, Program<Exp<?>> p, AqlEnv env, long start, long middle) {
		this.env = env;
		this.title = title;

		yyy.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				JList<?> list = (JList<?>) e.getSource();
				if (list.locationToIndex(e.getPoint()) == -1 && !e.isShiftDown() && !isMenuShortcutKeyDown(e)) {
					list.clearSelection();
				}
			}

			private boolean isMenuShortcutKeyDown(InputEvent event) {
				return (event.getModifiers() & Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) != 0;
			}
		});

		exn = env.exn;
		for (String c : p.order) {
			Exp<?> exp = p.exps.get(c);
			if (exp.kind() == Kind.COMMENT) {
				continue;
			}
			if (env.defs.keySet().contains(c)) {
				Semantics obj = (Semantics) env.defs.get(c, exp.kind());

				try {
					frames.add(new Pair<>(doLookup(true, c, exp.kind(), env.typing), wrapDisplay(c, exp, obj, env)));
				} catch (RuntimeException ex) {
					ex.printStackTrace();
					throw new LineException(ex.getMessage(), c, exp.kind().toString());
				}
			}
		}
		long end = System.currentTimeMillis();
		float c1 = ((middle - start) / (1000f));
		float c2 = ((end - middle) / (1000f));
		String pre = exn == null ? "" : "(ERROR, PARTIAL RESULT) | ";
		JComponent report = report(p, env, p.order, c1, c2, pre + title);
		frames.add(0, new Pair<>("Summary", report));

		display(pre + title, p.order, report);
	}

	private JComponent report(Program<Exp<?>> prog, AqlEnv env, List<String> order, float c1, float c2, String pre) {
		DecimalFormat df = new DecimalFormat("#.#");
		df.setRoundingMode(RoundingMode.CEILING);
		List<String> l = new LinkedList<>();
		Object[][] rowData = new Object[env.defs.insts.size()][3];
		int i = 0;
		List<String> missing = new LinkedList<>();
		for (String k : order) {
			if (env.defs.insts.containsKey(k)) {
				Instance<?, ?, ?, ?, ?, ?, ?, ?, ?> I = (Instance<?, ?, ?, ?, ?, ?, ?, ?, ?>) env.get(Kind.INSTANCE, k);
				String s = k + "\t" + I.size() + "\t" + env.performance.get(k);
				l.add(s);
				rowData[i][0] = k;
				rowData[i][1] = I.size();
				rowData[i][2] = env.performance.get(k);
				i++;
			} else if (prog.exps.get(k).kind().equals(Kind.INSTANCE)) {
				missing.add(k);
			}
		}

		JPanel t = GuiUtil.makeTable(BorderFactory.createEmptyBorder(), "", rowData, "instance", "rows", "seconds");
		JPanel pan = new JPanel(new GridLayout(1, 1));
		pan.add(new JScrollPane(t));
		String tsv = "instance\trows\tseconds\n" + Util.sep(l, "\n");
		JTabbedPane jtb = new JTabbedPane();
		String text = pre;
		if (!missing.isEmpty()) {
			text += "\n\nInstances not computed: " + Util.sep(Util.alphabetical(missing), ", ");
		}
		text += "\n\nComputation wall-clock time: " + df.format(c1) + "s\nGUI building time: " + df.format(c2) + "s\n";
		Map<Kind, Float> perfs = new THashMap<>();
		for (Kind k : Kind.values()) {
			perfs.put(k, 0f);
		}
		for (String s : env.performance.keySet()) {
			Kind k = prog.exps.get(s).kind();
			perfs.put(k, perfs.get(k) + env.performance.get(s));
		}
		for (Kind k : Kind.values()) {
			if (perfs.get(k) < .05f) {
				continue;
			}
			text += "\n" + k + " computation total time: " + df.format(perfs.get(k)) + "s";
		}

		if (!prog.options.options.isEmpty()) {
			text += "\n\nGlobal options:\n";
			text += Util.sep(prog.options.options, " = ", "\n");
			//text += "\n";
		}
		
		text += ("\n\nJVM Used Change: ");
		//text += (env.fd / (1024*1024));
		//text += (" MB to Free, ");
		//text += (env.md / (1024*1024));
		//text += (" MB to Limit, ");
		text += (env.ud / (1024*1024)) + " MB";
		text += ".  Used Max: " + (env.uh/ (1024*1024))+ " MB.";//.  Free Minimum MB: " + (env.fl / (1024*1024)) + ".";  
		jtb.addTab("Text", new CodeTextPanel("", text));
		jtb.addTab("Performance", pan);
		jtb.addTab("TSV", new CodeTextPanel("", tsv));
		jtb.addTab("Graphs", moreViewer(prog));
		return jtb;
	}

	private JPanel moreViewer(Program<Exp<?>> prog) {
		JPanel ret = new JPanel(); // new GridLayout(3,1));
		JButton b1 = new JButton("Dependency Graph");
		JButton b2 = new JButton("Schema Graph");
		JButton b3 = new JButton("Instance Graph");
		ret.add(b1);
		ret.add(b2);
		ret.add(b3);
		b1.addActionListener(x -> showDepGraph(prog));
		b2.addActionListener(x -> showSchGraph(prog));
		b3.addActionListener(x -> showInstGraph(prog));
		return ret;
	}

	public static Paint getColor(Kind k) {
		switch (k) {
		case CONSTRAINTS:
			return Color.BLUE;
		case TYPESIDE:
			return Color.WHITE;
		case SCHEMA:
			return Color.GRAY;
		case INSTANCE:
			return Color.black;
		case MAPPING:
			return Color.LIGHT_GRAY;
		case TRANSFORM:
			return Color.DARK_GRAY;
		case QUERY:
			return Color.RED;
		case PRAGMA:
			return Color.GREEN;
		case GRAPH:
			return Color.YELLOW;
		case COMMENT:
			return Color.PINK;
		case SCHEMA_COLIMIT:
			return Color.ORANGE;
		case THEORY_MORPHISM:
			return Color.gray;
		}
		return Util.anomaly();
	}

	public JComponent showGraph0(Graph<Exp<?>, Triple<Exp<?>, Exp<?>, Exp<?>>> sgv,
			Function<Exp<?>, Paint> vertexPaint) {
		if (sgv.getVertexCount() == 0) {
			return new JPanel();
		}
		Layout<Exp<?>, Triple<Exp<?>, Exp<?>, Exp<?>>> layout = new edu.uci.ics.jung.algorithms.layout.FRLayout2<>(sgv);

		layout.setSize(new Dimension(600, 400));
		VisualizationViewer<Exp<?>, Triple<Exp<?>, Exp<?>, Exp<?>>> vv = new VisualizationViewer<>(layout);

		vv.getPickedVertexState().addItemListener((ItemEvent e) -> {
			if (e.getStateChange() != ItemEvent.SELECTED) {
				return;
			}
			vv.getPickedEdgeState().clear();
			Exp<?> str = (Exp<?>) e.getItem();
			if (str instanceof SchExpColim) {
				str = ((SchExpColim) str).exp;
			}
			if (indices.containsKey(str.toString())) {
				yyy.setSelectedValue(indices.get(str.toString()), true);
			}
		});

		vv.getPickedEdgeState().addItemListener((ItemEvent e) -> {
			if (e.getStateChange() != ItemEvent.SELECTED) {
				return;
			}
			vv.getPickedVertexState().clear();
			@SuppressWarnings("unchecked")
			Exp<?> str = ((Triple<Exp<?>, Exp<?>, Exp<?>>) e.getItem()).first;
			if (str instanceof SchExpColim) {
				str = ((SchExpColim) str).exp;
			}
			if (indices.containsKey(str.toString())) {
				yyy.setSelectedValue(indices.get(str.toString()), true);
			}

		});

		DefaultModalGraphMouse<Exp<?>, Triple<Exp<?>, Exp<?>, Exp<?>>> gm = new DefaultModalGraphMouse<>();
		gm.setMode(Mode.TRANSFORMING);
		vv.setGraphMouse(gm);
		gm.setMode(Mode.PICKING);
		vv.getRenderContext().setVertexFillPaintTransformer(vertexPaint);
		vv.getRenderContext().setEdgeStrokeTransformer(x -> {
			// if (x.first.toString().startsWith("literal")) {
			// Stroke dashed = new BasicStroke(1, BasicStroke.CAP_BUTT,
			// BasicStroke.JOIN_BEVEL, 0, new float[]{4}, 0);
			// return dashed;
			// }
			return new BasicStroke();
		});

		Function<Exp<?>, String> vt = x -> {
			String ret = x.toString();
			if (ret.startsWith("literal")) {
				return "literal";
			} else if (ret.startsWith("quotient")) {
				return "quotient";
			} else if (ret.startsWith("coproduct")) {
				return "coproduct";
			} else if (ret.startsWith("coproduct_sigma")) {
				return "coproduct_sigma";
			} else if (ret.contains(" ")) {
				return ret.substring(0, ret.indexOf(" "));
			} else if (ret.length() > 16) {
				return ret.substring(0, 16) + "...";
			}
			return ret;
		};

		Function<Triple<Exp<?>, Exp<?>, Exp<?>>, String> et = x -> {
			return vt.apply(x.first);
		};

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

	private void showGraph(JComponent j, String str) {
		JFrame f = new JFrame();

		ActionListener escListener = (ActionEvent e) -> {
			env = null;
			f.dispose();
		};
		f.getRootPane().registerKeyboardAction(escListener, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
				JComponent.WHEN_IN_FOCUSED_WINDOW);
		KeyStroke ctrlW = KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK);
		KeyStroke commandW = KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.META_DOWN_MASK);
		f.getRootPane().registerKeyboardAction(escListener, ctrlW, JComponent.WHEN_IN_FOCUSED_WINDOW);
		f.getRootPane().registerKeyboardAction(escListener, commandW, JComponent.WHEN_IN_FOCUSED_WINDOW);

		f.add(j);

		f.setSize(600, 540);
		f.setTitle(str + " for " + title);
		f.setLocationRelativeTo(null);
		f.setVisible(true);
	}

	private void showSchGraph(Program<Exp<?>> p) {
		Graph<Exp<?>, Triple<Exp<?>, Exp<?>, Exp<?>>> sgv = new DirectedSparseMultigraph<>();

		for (String e : p.keySet()) {
			if (!p.kind(e).equals("schema")) {
				continue;
			}
			Exp<?> n0 = p.exps.get(e).Var(e);
			sgv.addVertex(n0);
		}
		for (String e : p.exps.keySet()) {
			if (!p.kind(e).equals("schema")) {
				continue;
			}
			Exp<?> m0 = p.exps.get(e).Var(e);
			for (Exp<?> n0 : p.exps.get(e).imports()) {
				if (!sgv.containsVertex(n0)) {
					sgv.addVertex(n0);
				}
				sgv.addEdge(new Triple<>(new CommentExp("import", false), n0, m0), n0, m0);
			}
		}

		for (String e : p.exps.keySet()) {
			if (!p.kind(e).equals("mapping")) {
				continue;
			}
			Pair<SchExp, SchExp> x = env.typing.defs.maps.get(e);
			if (!sgv.containsVertex(x.first)) {
				sgv.addVertex(x.first);
			}
			if (!sgv.containsVertex(x.second)) {
				sgv.addVertex(x.second);
			}
			Exp<?> m0 = p.exps.get(e).Var(e);
			sgv.addEdge(new Triple<>(m0, x.first, x.second), x.first, x.second);
		}
		for (String e : p.exps.keySet()) {
			if (!p.kind(e).equals("query")) {
				continue;
			}
			Pair<SchExp, SchExp> x = env.typing.defs.qs.get(e);
			if (!sgv.containsVertex(x.first)) {
				sgv.addVertex(x.first);
			}
			if (!sgv.containsVertex(x.second)) {
				sgv.addVertex(x.second);
			}
			Exp<?> m0 = p.exps.get(e).Var(e);
			sgv.addEdge(new Triple<>(m0, x.first, x.second), x.first, x.second);
		}
		for (String e : p.exps.keySet()) {
			if (!p.kind(e).equals("schema_colimit")) {
				continue;
			}
			ColimSchExp y = (ColimSchExp) env.prog.exps.get(e);
			for (Pair<SchExp, SchExp> x : y.gotos(new ColimSchExpVar(e))) {
				if (!sgv.containsVertex(x.first)) {
					sgv.addVertex(x.first);
				}
				if (!sgv.containsVertex(x.second)) {
					sgv.addVertex(x.second);
				}
				sgv.addEdge(new Triple<>(y, x.first, x.second), x.first, x.second);
			}
		}
		showGraph(showGraph0(sgv, x -> getColor(x.kind())), "Schema graph");
	}

	private Exp<?> unresolve(Program<Exp<?>> p, Exp<?> e) {
		if (p.invert().containsKey(e)) {
			return e.Var(p.invert().get(e));
		}
		return e;
	}

	private void showInstGraph(Program<Exp<?>> p) {
		Graph<Exp<?>, Triple<Exp<?>, Exp<?>, Exp<?>>> sgv = new DirectedSparseMultigraph<>();

		for (String e : p.keySet()) {
			if (!p.kind(e).equals("instance")) {
				continue;
			}
			Exp<?> n0 = p.exps.get(e).Var(e);
			sgv.addVertex(n0);
		}
		for (String e : p.exps.keySet()) {
			if (!p.kind(e).equals("instance")) {
				continue;
			}
			Exp<?> m0 = p.exps.get(e).Var(e);
			for (Exp<?> n0 : p.exps.get(e).imports()) {
				if (!sgv.containsVertex(n0)) {
					sgv.addVertex(n0);
				}
				sgv.addEdge(new Triple<>(new CommentExp("import", false), n0, m0), n0, m0);
			}

			InstExp<?, ?, ?, ?> x = (InstExp<?, ?, ?, ?>) p.exps.get(e);
			for (Exp<?> n1 : x.direct(env.typing)) {
				// System.out.println("n1 " + n1);
				Exp<?> n0 = unresolve(p, n1);
				// System.out.println("unr " + n0);
				if (!sgv.containsVertex(n0)) {
					sgv.addVertex(n0);
				}
				sgv.addEdge(new Triple<>(x, n0, m0), n0, m0);
			}
		}
		for (String e : p.exps.keySet()) {
			if (!p.kind(e).equals("transform")) {
				continue;
			}
			Pair<InstExp<?, ?, ?, ?>, InstExp<?, ?, ?, ?>> x = env.typing.defs.trans.get(e);
			Exp<?> xx = unresolve(p, x.first);
			if (!sgv.containsVertex(xx)) {
				sgv.addVertex(xx);
			}
			Exp<?> yy = unresolve(p, x.second);
			if (!sgv.containsVertex(yy)) {
				sgv.addVertex(yy);
			}
			Exp<?> m0 = p.exps.get(e).Var(e);
			sgv.addEdge(new Triple<>(m0, xx, yy), xx, yy);
		}

		Map<SchExp, Paint> map = new THashMap<>();
		Function<Exp<?>, Paint> vertexPaint = y -> {
			InstExp<?, ?, ?, ?> z = (InstExp<?, ?, ?, ?>) y;
			SchExp x = z.type(env.typing);
			if (map.containsKey(x)) {
				return map.get(x);
			}
			Color c = nColor();
			map.put(x, c);
			return c;
		};

		showGraph(showGraph0(sgv, vertexPaint), "Instance graph");
	}

	private int cindex = 0;
	private static final Color[] colors_arr = new Color[] { Color.BLACK, Color.white, Color.GRAY, Color.RED,
			Color.GREEN, Color.BLUE, Color.MAGENTA, Color.yellow, Color.CYAN, Color.ORANGE, Color.PINK, };

	private Color nColor() {
		if (cindex < colors_arr.length) {
			return colors_arr[cindex++];
		}
		cindex = 0;
		return nColor();

	}

	private void showDepGraph(Program<Exp<?>> p) {
		Graph<Exp<?>, Triple<Exp<?>, Exp<?>, Exp<?>>> sgv = new DirectedSparseMultigraph<>();
		for (String n : p.keySet()) {
			Exp<?> n0 = p.exps.get(n).Var(n);
			sgv.addVertex(n0);
		}
		for (String n : p.keySet()) {
			Exp<?> n0 = p.exps.get(n).Var(n);
			for (Pair<String, Kind> v : p.exps.get(n).deps()) {
				Exp<?> m0 = p.exps.get(v.first).Var(v.first);
				sgv.addEdge(new Triple<>(p.exps.get(n), n0, m0), n0, m0);
			}
		}
		showGraph(showGraph0(sgv, x -> getColor(x.kind())), "Dependency graph");
	}

	private JFrame frame = null;
	private final List<Pair<String, JComponent>> frames = new LinkedList<>();
	private final Map<String, String> indices = new THashMap<>();

	private final CardLayout cl = new CardLayout();
	private final JPanel x = new JPanel(cl);
	private final JList<String> yyy = new JList<>() {
		private static final long serialVersionUID = 1L;

		@Override
		public int locationToIndex(Point location) {
			int index = super.locationToIndex(location);
			if (index != -1 && !getCellBounds(index, index).contains(location)) {
				return -1;
			}
			return index;

		}
	};

	private String current;

	private final JComponent lookup(String s) {
		for (Pair<String, JComponent> p : frames) {
			if (p.first.equals(s)) {
				return p.second;
			}
		}
		return null;
	}

	private void display(String s, List<String> order0, JComponent report) {
		frame = new JFrame();
		int index = 0;
		Vector<String> ooo = new Vector<>();
		List<String> order = new ArrayList<>(order0);
		order.add(0, "Summary");
		for (Pair<String, JComponent> p : frames) {
			x.add(p.second, p.first);
			ooo.add(p.first);
			indices.put(order.get(index++), p.first);
		}
		x.add(report, "Summary");
		cl.show(x, "Summary");
		current = "Summary";

		yyy.setListData(ooo);
		yyy.setSelectedIndex(0);
		JPanel temp1 = new JPanel(new GridLayout(1, 1));
		temp1.setBorder(BorderFactory.createEmptyBorder());
		JScrollPane yyy1 = new JScrollPane(yyy);
		temp1.add(yyy1);
		yyy.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		yyy.addListSelectionListener((ListSelectionEvent e) -> {
			int i = yyy.getSelectedIndex();
			if (i == -1) {
				cl.show(x, "blank");
			} else {
				cl.show(x, ooo.get(i));
			}
		});

		yyy.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {
			}

			@Override
			public void keyPressed(KeyEvent e) {
			}

			@Override
			public void keyReleased(KeyEvent e) {
				JComponent c = lookup(current);
				if (!(c instanceof JTabbedPane)) {
					return;
				}
				JTabbedPane p = (JTabbedPane) c;
				int i = p.getSelectedIndex();
				int j = -1;
				if (e.getKeyCode() == KeyEvent.VK_LEFT) {
					j = i - 1;
					if (j < 0) {
						j = p.getTabCount() - 1;
					}
					p.setSelectedIndex(j);
					p.revalidate();
				} else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
					j = i + 1;
					if (j >= p.getTabCount()) {
						j = 0;
					}
					p.setSelectedIndex(j);
					p.revalidate();
				}

			}

		});

		yyy.addListSelectionListener(e -> {
			int i = yyy.getSelectedIndex();
			if (i == -1) {
				cl.show(x, "Summary");
				current = "Summary";
			} else {
				cl.show(x, ooo.get(i));
				current = ooo.get(i);
			}

		});

		JPanel north = new JPanel(new GridLayout(1, 1));
		JSplitPane px = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		px.setDividerLocation(200);
		px.setDividerSize(4);
		frame = new JFrame(/* "Viewer for " + */s);

		// TODO CQL what is other split pane?
		JSplitPane temp2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		temp2.setResizeWeight(1);
		temp2.setDividerSize(0);
		temp2.setBorder(BorderFactory.createEmptyBorder());
		temp2.add(temp1);
		temp2.add(north);

		px.add(temp2);

		px.add(x);

		frame.setContentPane(px);
		frame.setSize(900, 600);

		ActionListener escListener = e -> {
			env = null;
			frame.dispose();
		};

		frame.getRootPane().registerKeyboardAction(escListener, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
				JComponent.WHEN_IN_FOCUSED_WINDOW);
		KeyStroke ctrlW = KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_MASK);
		KeyStroke commandW = KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.META_MASK);
		frame.getRootPane().registerKeyboardAction(escListener, ctrlW, JComponent.WHEN_IN_FOCUSED_WINDOW);
		frame.getRootPane().registerKeyboardAction(escListener, commandW, JComponent.WHEN_IN_FOCUSED_WINDOW);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.setLocationRelativeTo(null);
		if (exn != null) {
			frame.setLocation(frame.getLocation().x + 400, frame.getLocation().y);
		}
		frame.setVisible(true);

	}

}
