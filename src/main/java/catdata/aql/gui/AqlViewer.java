package catdata.aql.gui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Paint;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import com.google.common.base.Function;

import catdata.Chc;
import catdata.Pair;
import catdata.Quad;
import catdata.Triple;
import catdata.Unit;
import catdata.Util;
import catdata.apg.ApgInstance;
import catdata.apg.ApgMapping;
import catdata.apg.ApgSchema;
import catdata.apg.ApgTerm;
import catdata.apg.ApgTransform;
import catdata.apg.ApgTy;
import catdata.apg.ApgTypeside;
import catdata.aql.Algebra;
import catdata.aql.AqlJs;
import catdata.aql.ColimitSchema;
import catdata.aql.Collage;
import catdata.aql.Comment;
import catdata.aql.Constraints;
import catdata.aql.DP;
import catdata.aql.Instance;
import catdata.aql.Mapping;
import catdata.aql.Mor;
import catdata.aql.Pragma;
import catdata.aql.Query;
import catdata.aql.Schema;
import catdata.aql.Semantics;
import catdata.aql.SemanticsVisitor;
import catdata.aql.Term;
import catdata.aql.Transform;
import catdata.aql.TypeSide;
import catdata.aql.Var;
import catdata.aql.exp.AqlEnv;
import catdata.aql.exp.AqlParserFactory;
import catdata.aql.exp.Att;
import catdata.aql.exp.En;
import catdata.aql.exp.Exp;
import catdata.aql.exp.Fk;
import catdata.aql.exp.Gen;
import catdata.aql.exp.RawTerm;
import catdata.aql.exp.Sk;
import catdata.aql.exp.Sym;
import catdata.aql.exp.Ty;
import catdata.graph.DMG;
import catdata.ide.CodeTextPanel;
import catdata.ide.GuiUtil;
import catdata.ide.Split;
import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse.Mode;
import edu.uci.ics.jung.visualization.renderers.VertexLabelRenderer;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public final class AqlViewer implements SemanticsVisitor<Unit, JTabbedPane, RuntimeException> {

	private int maxrows;
	private AqlEnv env;

	private boolean showAtts;

	public AqlViewer(int maxrows, @SuppressWarnings("unused") AqlEnv env, boolean showAtts) {
		this.maxrows = maxrows;
		this.showAtts = showAtts;
		this.env = env;
	}

	public static String html(Object obj) {
		return obj.toString().replace("\n", "<br>").replace("\t", "&nbsp;");
	}

	public static JComponent view(String k, Semantics s, int maxrows, Exp<?> exp, AqlEnv env, boolean showAtts) {
		JTabbedPane ret = new JTabbedPane();

		
		String l = "Compute time: " + env.performance.get(k) + " s.\n\nSize: " + s.size();
		if (s.size() < 1024 ) {
			new AqlViewer(maxrows, env, showAtts).visit(k, ret, s);
			ret.addTab("Text", new CodeTextPanel("", s.toString()));
		} else {
//			ret.addTab("Text", new CodeTextPanel("", "Suppressed, size " + s.size() + "."));
			ret.addTab("Text", new CodeTextPanel("", s.toString()));
			new AqlViewer(maxrows, env, showAtts).visit(k, ret, s);
		}

		ret.addTab("Expression", new CodeTextPanel("", l + "\n\n" + exp.toString()));
		// System.out.println(exp.getClass() + " " + env.performance.get(k));
		return ret;
	}

	private static <N, E> JComponent viewGraph(DMG<N, E> g) {
		Graph<N, E> sgv = new DirectedSparseMultigraph<>();

		for (N n : g.nodes) {
			sgv.addVertex(n);
		}
		for (E e : g.edges.keySet()) {
			sgv.addEdge(e, g.edges.get(e).first, g.edges.get(e).second);
		}

		if (sgv.getVertexCount() == 0) {
			return new JPanel();
		}
		Layout<N, E> layout = new FRLayout<>(sgv);

		layout.setSize(new Dimension(600, 400));
		VisualizationViewer<N, E> vv = new VisualizationViewer<>(layout);
		Function<N, Paint> vertexPaint = x -> Color.black;
		DefaultModalGraphMouse<N, E> gm = new DefaultModalGraphMouse<>();
		gm.setMode(Mode.TRANSFORMING);
		vv.setGraphMouse(gm);
		gm.setMode(Mode.PICKING);
		vv.getRenderContext().setVertexFillPaintTransformer(vertexPaint);

		Function<E, String> et = Object::toString;
		Function<N, String> vt = Object::toString;
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

	private <Ty, En, Sym, Fk, Att> JComponent viewCayley(Schema<Ty, En, Sym, Fk, Att> schema) {

		JPanel p = new JPanel();
		JPanel pan = new JPanel(new GridLayout(1, 3));
		JButton b = new JButton("Bounded Cayley Graph");
		pan.add(new JLabel("Depth:"));
		JTextField f = new JTextField();
		pan.add(f);
		pan.add(b);
		p.add(pan);
		b.addActionListener(x -> {
			String s = f.getText();
			try {
				Integer i = Integer.parseInt(s);
				if (i < 0) {
					return;
				}
				p.remove(pan);
				p.add(viewCayley2(schema, i));
				p.revalidate();
				p.repaint();

			} catch (Exception e) {

			}
		});
		return p;
	}

	private <Ty, En, Sym, Fk, Att> JComponent viewCayley2(TypeSide<Ty, Sym> t) {

		JPanel p = new JPanel();
		JPanel pan = new JPanel(new GridLayout(1, 3));
		JButton b = new JButton("Bounded Cayley Graph");
		pan.add(new JLabel("Depth:"));
		JTextField f = new JTextField();
		pan.add(f);
		pan.add(b);
		p.add(pan);
		b.addActionListener(x -> {
			String s = f.getText();
			try {
				Integer i = Integer.parseInt(s);
				if (i < 0) {
					return;
				}
				p.remove(pan);
				p.add(viewCayley2(t, i));
				p.revalidate();
				p.repaint();

			} catch (Exception e) {

			}
		});
		return p;
	}

	private <Ty, Sym> JComponent viewCayley2(TypeSide<Ty, Sym> T, int bound) {

		Map<Ty, Set<Term<Ty, Void, Sym, Void, Void, Void, Void>>> m = T.makeModel(new THashMap<>(), bound);

		Graph<Term<Ty, Void, Sym, Void, Void, Void, Void>, Quad<Integer, Integer, Integer, Sym>> sgv = new DirectedSparseMultigraph<>();
		int i = 0;
		boolean triggered = false;
		for (Set<Term<Ty, Void, Sym, Void, Void, Void, Void>> en : m.values()) {
			for (Term<Ty, Void, Sym, Void, Void, Void, Void> t : en) {
				sgv.addVertex(t);
				i++;
				if (i >= maxrows) {
					return new JLabel("too many nodes to display");
				}
			}
		}

		if (sgv.getVertexCount() == 0) {
			return new JPanel();
		}

		for (Sym f : T.syms.keySet()) {
			List<Ty> s = T.syms.get(f).first;
			Ty t = T.syms.get(f).second;
			List<Set<Term<Ty, Void, Sym, Void, Void, Void, Void>>> l = new ArrayList<>(s.size());
			for (Ty ty : s) {
				l.add(m.get(ty));
			}
			List<List<Term<Ty, Void, Sym, Void, Void, Void, Void>>> rs = Util.prod(l);

			Set<Term<Ty, Void, Sym, Void, Void, Void, Void>> os = m.get(t);
			for (Term<Ty, Void, Sym, Void, Void, Void, Void> o : os) {
				int j = 0;
				for (List<Term<Ty, Void, Sym, Void, Void, Void, Void>> r : rs) {

					boolean trig = false;
					if (T.semantics().eq(Collections.emptyMap(), o, Term.Sym(f, r))) {
						if (trig) {
							Util.anomaly();
						}
						trig = true;
						int pos = 0;
						for (Term<Ty, Void, Sym, Void, Void, Void, Void> arg : r) {
							sgv.addEdge(new Quad<>(i++, pos++, j, f), arg, o);
						}
						j++;
					}
				}
			}

		}

		// Layout<Chc<Ty, En>, Chc<Fk, Att>> layout = new KKLayout<>(sgv);
		Layout<Term<Ty, Void, Sym, Void, Void, Void, Void>, Quad<Integer, Integer, Integer, Sym>> layout = new FRLayout<>(
				sgv);

		layout.setSize(new Dimension(600, 400));
		VisualizationViewer<Term<Ty, Void, Sym, Void, Void, Void, Void>, Quad<Integer, Integer, Integer, Sym>> vv = new VisualizationViewer<>(
				layout);
		DefaultModalGraphMouse<Triple<En, En, List<Fk>>, Pair<Integer, Fk>> gm = new DefaultModalGraphMouse<>();
		gm.setMode(Mode.TRANSFORMING);
		vv.setGraphMouse(gm);
		gm.setMode(Mode.PICKING);

		Function<Quad<Integer, Integer, Integer, Sym>, String> et = x -> x.fourth.toString() + "_" + x.third + "^"
				+ x.second;
		Function<Term<Ty, Void, Sym, Void, Void, Void, Void>, String> vt = x -> x.toString();

		vv.getRenderContext().setEdgeLabelTransformer(et);
		vv.getRenderContext().setVertexLabelTransformer(vt);

		vv.getRenderContext().setVertexLabelRenderer(new VertexLabelRenderer() {
			@Override
			public <V> Component getVertexLabelRendererComponent(JComponent arg0, Object arg1, Font arg2, boolean arg3,
					V arg4) {
				return new JLabel(arg1.toString());
			}
		});

		GraphZoomScrollPane zzz = new GraphZoomScrollPane(vv);
		JPanel ret = new JPanel(new GridLayout(1, 1));
		ret.add(zzz);
		ret.setBorder(BorderFactory.createEtchedBorder());

		vv.getRenderContext().setLabelOffset(16);
		// vv.getRenderContext().set
		vv.setBackground(Color.white);

		if (triggered) {
			ret.setBorder(BorderFactory.createTitledBorder("Partial"));
		}

		return ret;
	}

	private <Ty, En, Sym, Fk, Att> JComponent viewCayley2(Schema<Ty, En, Sym, Fk, Att> schema, int bound) {
		Graph<Triple<En, En, List<Fk>>, Fk> sgv2 = new DirectedSparseMultigraph<>();

		int i = 0;
		boolean triggered = false;
		for (En en : schema.ens) {
			sgv2.addVertex(new Triple<>(en, en, new LinkedList<>()));
			i++;
			if (i >= maxrows) {
				triggered = true;
				break;
			}
		}
		if (triggered) {
			return new JLabel("too many nodes to display");
		}

		for (int round = 0; round < bound; round++) {
			List<Triple<En, En, List<Fk>>> add = new LinkedList<>();

			for (Triple<En, En, List<Fk>> current : sgv2.getVertices()) {
				outer: for (Fk fk : schema.fksFrom(current.second)) {

					List<Fk> l = new LinkedList<>(current.third);
					l.add(fk);

					for (Triple<En, En, List<Fk>> other : sgv2.getVertices()) {
						if (!other.first.equals(current.first) || !other.second.equals(current.second)) {
							continue;
						}

					}
					add.add(new Triple<>(current.first, schema.fks.get(fk).second, l));
				}
			}
			for (Triple<En, En, List<Fk>> x : add) {
				sgv2.addVertex(x);
			}
		}

		Graph<Triple<En, En, List<Fk>>, Pair<Integer, Fk>> sgv = new DirectedSparseMultigraph<>();
		outer: for (Triple<En, En, List<Fk>> x : sgv2.getVertices()) {
			for (Triple<En, En, List<Fk>> y : sgv.getVertices()) {
				if (!y.first.equals(x.first) || !y.second.equals(x.second)) {
					continue;
				}
				Map<Var, Chc<Ty, En>> ctx = Collections.singletonMap(Var.Var(x.first.toString()), Chc.inRight(x.first));
				Term<Ty, En, Sym, Fk, Att, Void, Void> t = Term.Fks(x.third, Term.Var(Var.Var(x.first.toString())));
				Term<Ty, En, Sym, Fk, Att, Void, Void> s = Term.Fks(y.third, Term.Var(Var.Var(y.first.toString())));

				boolean b = schema.dp.eq(ctx, t, s);
				System.out.println(t + " vs " + s + " = " + b);
				if (b) {
					continue outer;
				}
			}
			sgv.addVertex(x);
		}
		int temp = 0;
		for (Triple<En, En, List<Fk>> x : sgv.getVertices()) {
			for (Fk fk : schema.fksFrom(x.second)) {
				for (Triple<En, En, List<Fk>> y : sgv.getVertices()) {
					if (!x.first.equals(y.first)) {
						continue;
					}
					if (!y.second.equals(schema.fks.get(fk).first)) {
						continue;
					}
					if (!x.second.equals(schema.fks.get(fk).second)) {
						continue; // not sure this is necessary; could be implied by others
					}
					List<Fk> l = new LinkedList<>(y.third);
					l.add(fk);
					Map<Var, Chc<Ty, En>> ctx = Collections.singletonMap(Var.Var(x.first.toString()),
							Chc.inRight(x.first));
					Term<Ty, En, Sym, Fk, Att, Void, Void> t = Term.Fks(x.third, Term.Var(Var.Var(x.first.toString())));
					Term<Ty, En, Sym, Fk, Att, Void, Void> s = Term.Fks(l, Term.Var(Var.Var(y.first.toString())));
					if (schema.dp.eq(ctx, t, s)) {
						sgv.addEdge(new Pair<>(temp++, fk), y, x);
					}
				}
			}
		}

		if (sgv.getVertexCount() == 0) {
			return new JPanel();
		}
		// Layout<Chc<Ty, En>, Chc<Fk, Att>> layout = new KKLayout<>(sgv);
		Layout<Triple<En, En, List<Fk>>, Pair<Integer, Fk>> layout = new FRLayout<>(sgv);

		layout.setSize(new Dimension(600, 400));
		VisualizationViewer<Triple<En, En, List<Fk>>, Pair<Integer, Fk>> vv = new VisualizationViewer<>(layout);
		// Function<Chc<Ty, En>, Paint> vertexPaint = x -> x.left ? Color.gray :
		// Color.black;
		DefaultModalGraphMouse<Triple<En, En, List<Fk>>, Pair<Integer, Fk>> gm = new DefaultModalGraphMouse<>();
		gm.setMode(Mode.TRANSFORMING);
		vv.setGraphMouse(gm);
		gm.setMode(Mode.PICKING);
		// vv.getRenderContext().setVertexFillPaintTransformer(vertexPaint);

		Function<Triple<En, En, List<Fk>>, String> et = x -> x.third.isEmpty() ? "id" : Util.sep(x.third, ".");
		Function<Pair<Integer, Fk>, String> vt = x -> x.second.toString();

		vv.getRenderContext().setEdgeLabelTransformer(vt);
		vv.getRenderContext().setVertexLabelTransformer(et);

		vv.getRenderContext().setVertexLabelRenderer(new VertexLabelRenderer() {
			@Override
			public <V> Component getVertexLabelRendererComponent(JComponent arg0, Object arg1, Font arg2, boolean arg3,
					V arg4) {
				return new JLabel(arg1.toString());
			}
		});

		GraphZoomScrollPane zzz = new GraphZoomScrollPane(vv);
		JPanel ret = new JPanel(new GridLayout(1, 1));
		ret.add(zzz);
		ret.setBorder(BorderFactory.createEtchedBorder());

		vv.getRenderContext().setLabelOffset(16);
		// vv.getRenderContext().set
		vv.setBackground(Color.white);

		if (triggered) {
			ret.setBorder(BorderFactory.createTitledBorder("Partial"));
		}

		return ret;
	}

	private <Ty, En, Sym, Fk, Att> JComponent viewSchema(Schema<Ty, En, Sym, Fk, Att> schema) {
		Graph<Chc<Ty, En>, Chc<Fk, Att>> sgv = new DirectedSparseMultigraph<>();

		int i = 0;
		boolean triggered = false;
		for (En en : schema.ens) {
			sgv.addVertex(Chc.inRight(en));
			i++;
			if (i >= maxrows) {
				triggered = true;
				break;
			}
		}
		if (showAtts) {
			i = 0;
			for (Ty ty : schema.typeSide.tys) {
				sgv.addVertex(Chc.inLeft(ty));
				i++;
				if (i >= maxrows * maxrows) {
					triggered = true;
					break;
				}
			}
			for (Att att : schema.atts.keySet()) {
				sgv.addEdge(Chc.inRight(att), Chc.inRight(schema.atts.get(att).first),
						Chc.inLeft(schema.atts.get(att).second));
				i++;
				if (i >= maxrows * maxrows) {
					triggered = true;
					break;
				}
			}
		}
		for (Fk fk : schema.fks.keySet()) {
			sgv.addEdge(Chc.inLeft(fk), Chc.inRight(schema.fks.get(fk).first), Chc.inRight(schema.fks.get(fk).second));
			i++;
			if (i >= maxrows * maxrows) {
				triggered = true;
				break;
			}
		}
		// }

		if (sgv.getVertexCount() == 0) {
			return new JPanel();
		}
		// Layout<Chc<Ty, En>, Chc<Fk, Att>> layout = new KKLayout<>(sgv);
		Layout<Chc<Ty, En>, Chc<Fk, Att>> layout = new FRLayout<>(sgv);

		layout.setSize(new Dimension(600, 400));
		VisualizationViewer<Chc<Ty, En>, Chc<Fk, Att>> vv = new VisualizationViewer<>(layout);
		Function<Chc<Ty, En>, Paint> vertexPaint = x -> x.left ? Color.gray : Color.black;
		DefaultModalGraphMouse<Chc<Ty, En>, Chc<Fk, Att>> gm = new DefaultModalGraphMouse<>();
		gm.setMode(Mode.TRANSFORMING);
		vv.setGraphMouse(gm);
		gm.setMode(Mode.PICKING);
		vv.getRenderContext().setVertexFillPaintTransformer(vertexPaint);

		Function<Chc<Fk, Att>, String> et = Chc::toStringMash;
		Function<Chc<Ty, En>, String> vt = x -> {
			if (x.left) {
				return x.l.toString();
			}
			En en = x.r;
			// if (!showAtts) {
			// return en.toString();
			// }
			StringBuffer sb = new StringBuffer();
			sb.append("<html><center>");
			sb.append(en.toString());
			sb.append("</center><hr/>");
			// int i = 0;
			int l = 0;
			for (Att att : Util.alphabetical(schema.attsFrom(en))) {
				if (l != 0) {
					sb.append("<br/>");
				}
				sb.append(att.toString());
				l++;
				// l = Math.max(l, att.toString().length());
			}
			sb.append("</html>");
			return sb.toString();

		};
		vv.getRenderContext().setEdgeLabelTransformer(et);
		vv.getRenderContext().setVertexLabelTransformer(vt);

		vv.getRenderContext().setVertexLabelRenderer(new VertexLabelRenderer() {
			@Override
			public <V> Component getVertexLabelRendererComponent(JComponent arg0, Object arg1, Font arg2, boolean arg3,
					V arg4) {
				return new JLabel(arg1.toString());
			}
		});

		GraphZoomScrollPane zzz = new GraphZoomScrollPane(vv);
		JPanel ret = new JPanel(new GridLayout(1, 1));
		ret.add(zzz);
		ret.setBorder(BorderFactory.createEtchedBorder());

		vv.getRenderContext().setLabelOffset(16);
		// vv.getRenderContext().set
		vv.setBackground(Color.white);

		if (triggered) {
			ret.setBorder(BorderFactory.createTitledBorder("Partial"));
		}

		return ret;
	}

	@SuppressWarnings("unused")
	private static class Node<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> {
		public En1 en1;
		public En2 en2;
		public Att1 att1;
		public Att2 att2;
		public Fk1 fk1;
		public Fk2 fk2;
		public Ty ty;

		/*
		 * public static <Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> Node<Ty, En1, Sym,
		 * Fk1, Att1, En2, Fk2, Att2> Ty(Ty ty) { Node<Ty, En1, Sym, Fk1, Att1, En2,
		 * Fk2, Att2> n = new Node<>(); n.ty = ty; return n; }
		 */
		public static <Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> Node<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> En1(
				En1 en1) {
			Node<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> n = new Node<>();
			n.en1 = en1;
			return n;
		}

		public static <Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> Node<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> En2(
				En2 en2) {
			Node<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> n = new Node<>();
			n.en2 = en2;
			return n;
		}

		public static <Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> Node<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> Att1(
				Att1 att1) {
			Node<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> n = new Node<>();
			n.att1 = att1;
			return n;
		}

		public static <Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> Node<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> Att2(
				Att2 att2) {
			Node<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> n = new Node<>();
			n.att2 = att2;
			return n;
		}

		public static <Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> Node<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> Fk1(
				Fk1 fk1) {
			Node<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> n = new Node<>();
			n.fk1 = fk1;
			return n;
		}

		public static <Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> Node<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> Fk2(
				Fk2 fk2) {
			Node<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> n = new Node<>();
			n.fk2 = fk2;
			return n;
		}

		public String toString() {
			if (en1 != null) {
				return en1.toString();
			} else if (en2 != null) {
				return en2.toString();
			}
			if (att1 != null) {
				return att1.toString();
			} else if (att2 != null) {
				return att2.toString();
			}
			if (fk1 != null) {
				return fk1.toString();
			} else if (fk2 != null) {
				return fk2.toString();
			} else if (ty != null) {
				return ty.toString();
			}
			return Util.anomaly();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((att1 == null) ? 0 : att1.hashCode());
			result = prime * result + ((att2 == null) ? 0 : att2.hashCode());
			result = prime * result + ((en1 == null) ? 0 : en1.hashCode());
			result = prime * result + ((en2 == null) ? 0 : en2.hashCode());
			result = prime * result + ((fk1 == null) ? 0 : fk1.hashCode());
			result = prime * result + ((fk2 == null) ? 0 : fk2.hashCode());
			result = prime * result + ((ty == null) ? 0 : ty.hashCode());
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
			Node<?, ?, ?, ?, ?, ?, ?, ?> other = (Node<?, ?, ?, ?, ?, ?, ?, ?>) obj;
			if (att1 == null) {
				if (other.att1 != null)
					return false;
			} else if (!att1.equals(other.att1))
				return false;
			if (att2 == null) {
				if (other.att2 != null)
					return false;
			} else if (!att2.equals(other.att2))
				return false;
			if (en1 == null) {
				if (other.en1 != null)
					return false;
			} else if (!en1.equals(other.en1))
				return false;
			if (en2 == null) {
				if (other.en2 != null)
					return false;
			} else if (!en2.equals(other.en2))
				return false;
			if (fk1 == null) {
				if (other.fk1 != null)
					return false;
			} else if (!fk1.equals(other.fk1))
				return false;
			if (fk2 == null) {
				if (other.fk2 != null)
					return false;
			} else if (!fk2.equals(other.fk2))
				return false;
			if (ty == null) {
				if (other.ty != null)
					return false;
			} else if (!ty.equals(other.ty))
				return false;
			return true;
		}

		public Paint color() {
			if (en1 != null) {
				return Color.BLACK;
			} else if (en2 != null) {
				return Color.GRAY;
			}
			if (att1 != null) {
				return Color.BLUE;
			} else if (att2 != null) {
				return Color.CYAN;
			}
			if (fk1 != null) {
				return Color.red;
			} else if (fk2 != null) {
				return Color.magenta;
			} else if (ty != null) {
				return Color.white;
			}
			return Util.anomaly();
		}

	}

	private <Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> JComponent viewMappingGraph(
			Mapping<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> M) {
		Graph<Node<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2>, Chc<Integer, Integer>> sgv = new DirectedSparseMultigraph<>();

		if (M.src.size() > maxrows) {
			JLabel ppp = new JLabel("Size too large, set max_rows");
			return ppp;
		}

		int i = 0;
		for (En1 en1 : M.src.ens) {
			sgv.addVertex(Node.En1(en1));
			sgv.addEdge(Chc.inRight(i++), Node.En1(en1), Node.En2(M.ens.get(en1)));
		}
		for (En2 en2 : M.dst.ens) {
			sgv.addVertex(Node.En2(en2));
		}
		// for (Ty ty : M.dst.typeSide.tys) {
		// sgv.addVertex(Node.Ty(ty));
		// }
		for (Fk1 fk1 : M.src.fks.keySet()) {
			Pair<En2, List<Fk2>> l = M.fks.get(fk1);
			sgv.addEdge(Chc.inLeft(i++), Node.En1(M.src.fks.get(fk1).first), Node.Fk1(fk1));
			sgv.addEdge(Chc.inLeft(i++), Node.Fk1(fk1), Node.En1(M.src.fks.get(fk1).second));

			if (l.second.isEmpty()) {
				sgv.addEdge(Chc.inRight(i++), Node.Fk1(fk1), Node.En2(l.first));
			} else {
				for (Fk2 fk2 : l.second) {
					sgv.addEdge(Chc.inRight(i++), Node.Fk1(fk1), Node.Fk2(fk2));
				}
			}
		}
		if (showAtts) {
			for (Att1 att1 : M.src.atts.keySet()) {
				Triple<Var, En2, Term<Ty, En2, Sym, Fk2, Att2, Void, Void>> l = M.atts.get(att1);
				sgv.addEdge(Chc.inLeft(i++), Node.En1(M.src.atts.get(att1).first), Node.Att1(att1));
				// sgv.addEdge(Chc.inLeft(i++), Node.Att1(att1),
				// Node.Ty(M.src.atts.get(att1).second));

				for (Att2 x : l.third.atts()) {
					sgv.addEdge(Chc.inRight(i++), Node.Att1(att1), Node.Att2(x));
				}
				// for (Fk2 x : l.third.fks()) {
				// sgv.addEdge(Chc.inRight(i++), Node.Att1(att1), Node.Fk2(x));
				// }
				/*
				 * boolean b = false; for (Sym x : l.third.syms()) {
				 * sgv.addEdge(Chc.inRight(i++), Node.Att1(att1),
				 * Node.Ty(M.dst.typeSide.syms.get(x).second)); b = true; break; } if (!b) { for
				 * (Pair<Object, Ty> x : l.third.objs()) { sgv.addEdge(Chc.inRight(i++),
				 * Node.Att1(att1), Node.Ty(x.second)); b = true; break; } }
				 */
			}
		}
		for (Fk2 fk2 : M.dst.fks.keySet()) {
			sgv.addEdge(Chc.inLeft(i++), Node.En2(M.dst.fks.get(fk2).first), Node.Fk2(fk2));
			sgv.addEdge(Chc.inLeft(i++), Node.Fk2(fk2), Node.En2(M.dst.fks.get(fk2).second));

		}
		if (showAtts) {
			for (Att2 att2 : M.dst.atts.keySet()) {
				sgv.addEdge(Chc.inLeft(i++), Node.En2(M.dst.atts.get(att2).first), Node.Att2(att2));
				// sgv.addEdge(Chc.inLeft(i++), Node.Att2(att2),
				// Node.Ty(M.dst.atts.get(att2).second));
			}
		}

		if (sgv.getVertexCount() == 0) {
			return new JPanel();
		}
		FRLayout<Node<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2>, Chc<Integer, Integer>> layout = new FRLayout<>(sgv);

		layout.setSize(new Dimension(600, 400));
		VisualizationViewer<Node<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2>, Chc<Integer, Integer>> vv = new VisualizationViewer<>(
				layout);
		Function<Node<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2>, Paint> vertexPaint = x -> x.color();
		DefaultModalGraphMouse<Chc<Ty, En>, Chc<Fk, Att>> gm = new DefaultModalGraphMouse<>();
		gm.setMode(Mode.TRANSFORMING);
		vv.setGraphMouse(gm);
		gm.setMode(Mode.PICKING);
		vv.getRenderContext().setVertexFillPaintTransformer(vertexPaint);

		// vv.getRenderContext().setVert
		vv.getRenderContext().setEdgeStrokeTransformer(x -> {
			if (!x.left) {
				Stroke dashed = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] { 4 },
						0);
				return dashed;
			}
			return new BasicStroke();
		});

		Function<Chc<Integer, Integer>, String> et = x -> "";
		Function<Node<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2>, String> vt = x -> x.toString();
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

	@SuppressWarnings("rawtypes")
	private static <Ty, En, Sym, Fk, Att, Gen, Sk> JComponent viewDP(DP<Ty, En, Sym, Fk, Att, Gen, Sk> dp,
			Function<Unit, Collage> col, AqlJs /* <Ty, Sym> */ js) {
		CodeTextPanel input = new CodeTextPanel("Input (either equation-in-ctx or term-in-ctx)", "");
		CodeTextPanel output = new CodeTextPanel("Output", "");

		JButton eq = new JButton("Decide Equation-in-ctx");
		// JButton nf = new JButton("Normalize Term-in-ctx");
		JButton print = new JButton("Show Info");
		JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
		buttonPanel.add(eq);
		// buttonPanel.add(nf);
		buttonPanel.add(print);

		Split split = new Split(.5, JSplitPane.VERTICAL_SPLIT); // TODO: aql does not position correctly
		split.add(input);
		split.add(output);

		JPanel main = new JPanel(new BorderLayout());
		main.add(split, BorderLayout.CENTER);
		main.add(buttonPanel, BorderLayout.NORTH);

		print.addActionListener(x -> output.setText(dp.toStringProver()));
		eq.addActionListener(x -> {
			try {
				if (!input.getText().contains("=")) {
					return;
				}
				final Triple<List<Pair<String, String>>, RawTerm, RawTerm> y = AqlParserFactory.getParser()
						.parseEq(input.getText());

				@SuppressWarnings("unchecked")
				final Triple<Map<Var, Chc<Ty, En>>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>> z = RawTerm
						.infer2(y.first, y.second, y.third, col.apply(Unit.unit), js);

				boolean isEq = dp.eq(z.first, z.second, z.third);
				output.setText(Boolean.toString(isEq));
			} catch (Exception ex) {
				ex.printStackTrace();
				output.setText(ex.getMessage());
			}
		});

		return main;
	}

	private static <Ty, En, Sym, Fk, Att, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> JComponent viewTransform(
			Transform<Ty, En, Sym, Fk, Att, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> t) {
		List<JComponent> list = new LinkedList<>();

		List<En> ens = Util.alphabetical(t.src().schema().ens);
		List<Ty> tys = Util.alphabetical(t.src().schema().typeSide.tys);

		for (En en : ens) {
			List<Object> header = new LinkedList<>();
			header.add("Input");
			header.add("Output");
			for (Att att : t.dst().schema().attsFrom(en)) {
				header.add(att);
			}
			int n = t.src().algebra().size(en);
			if (n == 0) {
				continue;
			}
			Object[][] data = new Object[n][header.size()];
			int i = 0;
			for (X1 x1 : t.src().algebra().en(en)) {
				Object[] row = new Object[header.size()];
				row[0] = t.src().algebra().printX(en, x1);
				X2 x2 = t.repr(en, x1);
				row[1] = t.dst().algebra().printX(en, x2);
				for (int j = 2; j < header.size(); j++) {
					row[j] = t.src().algebra().att((Att) header.get(j), x1);
				}
				data[i] = row;
				i++;
			}
			list.add(GuiUtil.makeTable(BorderFactory.createEmptyBorder(), en + " (" + n + ")", data, header.toArray()));
		}
		Map<Ty, List<Sk1>> z = Util.newListsFor(t.src().schema().typeSide.tys);
		t.src().sks().entrySet((sk, ty) -> {
			z.get(ty).add(sk);
		});
		for (Ty ty : tys) {
			List<String> header = new LinkedList<>();
			header.add("Input");
			header.add("Output");
			if (!z.containsKey(ty)) {
				continue;
			}
			int n = z.get(ty).size();
			if (n == 0) {
				continue;
			}
			Object[][] data = new Object[n][2];
			int i = 0;
			for (Sk1 y1 : z.get(ty)) {
				Object[] row = new Object[2];
				Term<Ty, En, Sym, Fk, Att, Gen1, Sk1> a = Term.Sk(y1);
				row[0] = a.toString(); // a.toString(t.src().algebra().p, gen_printer)
				Term<Ty, En, Sym, Fk, Att, Gen2, Sk2> y0 = t.trans(a);
				row[1] = y0.toString();
				data[i] = row;
				i++;
			}
			list.add(GuiUtil.makeTable(BorderFactory.createEmptyBorder(), ty + " (" + n + ")", data, header.toArray()));
		}

		return GuiUtil.makeGrid(list);
	}

	private <Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> Map<Ty, Object[][]> makeTyTables(Map<Ty, Set<Y>> m,
			Algebra<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> alg) {
		Map<Ty, Object[][]> ret = new LinkedHashMap<>();

		List<Ty> tys = Util.alphabetical(alg.schema().typeSide.tys);

		for (Ty ty : tys) {
			if (!m.containsKey(ty)) {
				continue;
			}
			int n = Integer.min(maxrows, m.get(ty).size());

			Object[][] data = new Object[n][1];
			int i = 0;
			for (Y y : Util.alphabetical(m.get(ty))) {
				Object[] row = new Object[1];
				row[0] = alg.printY(ty, y);
				data[i] = row;
				i++;
				if (i == n) {
					break;
				}
			}
			ret.put(ty, data);
		}
		return ret;
	}

	public static <Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> Map<En, Pair<List<String>, Object[][]>> makeEnTables(
			Algebra<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> alg, boolean simplify, int limit,
			Map<En, Set<Pair<Integer, Integer>>> nulls) {
		Map<En, Pair<List<String>, Object[][]>> ret = new LinkedHashMap<>();

		List<En> ens = Util.alphabetical(alg.schema().ens);
		//if (ens.size() > 32) {
		//	ens = ens.subList(0, 31);
	//	}

		Map<X, Integer> referredTo = new THashMap<>();
		Map<Y, Integer> referredTo2 = new THashMap<>();

		Set<X> set = new LinkedHashSet<>();
		Map<En, List<X>> all = new LinkedHashMap<>();
		int fresh = 0;

		for (En en : ens) {
			// System.out.println(en + "--");
			int n = Integer.min(limit, alg.size(en));
			List<X> lll = new ArrayList<>(n);
			int p = 0;
			for (X xx : alg.en(en)) {
				// System.out.println(xx);
				lll.add(xx);
				p++;

				if (p == n) {
					break;
				}
			}
			
			lll.sort((x, y) -> Util.AlphabeticalComparator.compare(x.toString(), y.toString()));
			for (X xx : lll) {
				referredTo.put(xx, fresh++);
			}
			all.put(en, lll);
		}

		fresh = 0;
		for (En en : ens) {
			List<Att> atts0 = Util
					.alphabetical(alg.schema().attsFrom(en).stream().map((Att x) -> x).collect(Collectors.toList()));
			List<Fk> fks0 = Util
					.alphabetical(alg.schema().fksFrom(en).stream().map((Fk x) -> x).collect(Collectors.toList()));

			List<String> atts0x = atts0.stream().map(Object::toString).collect(Collectors.toList());
			List<String> fks0x = fks0.stream().map(Object::toString).collect(Collectors.toList());
			List<String> header = Util.<String>append(atts0x, fks0x);

			// boolean aabr = true;
			if (header.size() > 20) {
				header = header.subList(0, 20); // easier on the eye
				header.add(0, "Row (Cols Abbr)");
			} else {
				header.add(0, "Row");
			}

			int n = Integer.min(limit, alg.size(en));
			Object[][] data = new Object[n][];
			if (n != 0) {
				int i = 0;
				for (X x : all.get(en)) {
					if (x == null) {
						Util.anomaly();
					}
					List<Object> row = new LinkedList<>();
					row.add(x);
					int j = 1;

					for (Att att0 : atts0) {
						try {
							Term<Ty, Void, Sym, Void, Void, Void, Y> t = alg.att(att0, x);
							for (Y y : t.sks()) {
								// X z = alg.gen(g);
								if (!referredTo2.containsKey(y)) {
									referredTo2.put(y, fresh++);
								}
								Set<Pair<Integer, Integer>> v = nulls.get(en);
								if (v == null) {
									v = new THashSet<>();
									nulls.put(en, v);
								}
								// System.out.println("add " + i + " , " + j + " bc " + y);
								v.add(new Pair<>(i, j));
								// set.add(z);
							}
							row.add(t);
						} catch (Exception ex) {
							ex.printStackTrace();
							row.add(Term.Var(Var.Var("ERR")));
						}

						j++;
					}
					for (Fk fk0 : fks0) {
						X y = alg.fk(fk0, x);
						if (y == null) {
							Util.anomaly();
						}
						if (!referredTo.containsKey(y)) {
							referredTo.put(y, fresh++);
						}
						set.add(y);
						row.add(y);
					}
					data[i] = row.toArray();
					
					i++;
					if (i == n) {
						break;
					}
				}
				if (i != n) {
					throw new RuntimeException("Anomaly: expected at least " + n + " elements on " + en + " but got " + i);
				}
			}
	
			// Arrays.deepToString(data));
			ret.put(en, new Pair<>(header, data));
		}

		if (!simplify) {
			BiFunction<En, Object, Object> f = (y, x) -> referredTo.get(x);
			BiFunction<Ty, Object, Object> g = (y, xx) -> ((Term<Ty, Void, Sym, Void, Void, Void, Y>) xx)
					.mapGenSk(x -> x, x -> "?" + referredTo2.get(x)).toString();
			enTableSimpl(alg, ret, f, g, f);
			return ret;
		}
		
		BiFunction<En, Object, Object> a = (y, x) -> alg.printX(y, (X) x);
		BiFunction<Ty, Object, Object> b = (y, x) -> ((Term<Ty, Void, Sym, Void, Void, Void, Y>) x)
				.toString(z -> alg.printY(alg.talg().sks.get(z), z).toString(), Util.voidFn());

		enTableSimpl(alg, ret, a, b, a);

		return ret;
	}

	private synchronized static <Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> void enTableSimpl(
			Algebra<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> alg, Map<En, Pair<List<String>, Object[][]>> ret,
			BiFunction<En, Object, Object> f, BiFunction<Ty, Object, Object> g, BiFunction<En, Object, Object> h) {

		for (En en : ret.keySet()) {
			Object[][] arr = ret.get(en).second;

			for (int i = 0; i < arr.length; i++) {
				Object[] o = arr[i];
				if (o == null) {
					Util.anomaly();
				}
				int j = 0;
				o[0] = f.apply(en, o[0]);
				for (Att att : alg.schema().attsFrom(en)) {
					Ty e = alg.schema().atts.get(att).second;
					o[1 + j] = g.apply(e, o[1 + j]);
					j++;
				}
				for (Fk fk : alg.schema().fksFrom(en)) {
					En e = alg.schema().fks.get(fk).second;
					// System.out.println("On fk " + fk + " tgt is " + e + " and o " +
					// Arrays.deepToString(o));
					o[1 + j] = h.apply(e, o[1 + j]);
					j++;
				}
			}

			// System.out.println("en(after) " + en + " " + Arrays.deepToString(arr));
		}

	}

	private <X, Y> Component viewAlgebra(float z,
			Algebra<catdata.aql.exp.Ty, catdata.aql.exp.En, catdata.aql.exp.Sym, catdata.aql.exp.Fk, catdata.aql.exp.Att, catdata.aql.exp.Gen, catdata.aql.exp.Sk, X, Y> algebra) {

		JPanel top = new JPanel(new GridBagLayout());
		top.setBorder(BorderFactory.createEmptyBorder());

		JCheckBox simp = new JCheckBox("", false);
		int a = Integer.min(maxrows, algebra.sizeOfBiggest());
		JSlider sl = new JSlider(0, a, Integer.min(32, a));

		JLabel limit = new JLabel("Row limit:", JLabel.RIGHT);

		JLabel lbl = new JLabel("Provenance:", JLabel.RIGHT);
		JLabel ids = new JLabel(
				" " + algebra.size() + " IDs, " + algebra.talg().sks.size() + " nulls, " + z + " seconds.",
				JLabel.LEFT);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;

		gbc.anchor = GridBagConstraints.WEST;
		top.add(ids, gbc);

		gbc.anchor = GridBagConstraints.EAST;
		top.add(lbl, gbc);
		gbc.anchor = GridBagConstraints.WEST;
		top.add(simp, gbc);
		gbc.anchor = GridBagConstraints.EAST;
		top.add(limit, gbc);
		gbc.anchor = GridBagConstraints.WEST;
		top.add(sl, gbc);

		JPanel out = new JPanel(new BorderLayout());
		out.setBorder(BorderFactory.createEtchedBorder());

		Map<Pair<Boolean, Integer>, JScrollPane> cache = new THashMap<>();
		viewAlgebraHelper(top, algebra, out, simp, sl, cache);

		sl.addChangeListener((x) -> {
			viewAlgebraHelper(top, algebra, out, simp, sl, cache);
		});
		simp.addChangeListener((x) -> {
			viewAlgebraHelper(top, algebra, out, simp, sl, cache);
		});

		return out;
	}

	private <X, Y> void viewAlgebraHelper(JComponent top, Algebra<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> algebra,
			JPanel out, JCheckBox simp, JSlider sl, Map<Pair<Boolean, Integer>, JScrollPane> cache) {
		boolean b = simp.isSelected();
		int l = sl.getValue();
		Pair<Boolean, Integer> p = new Pair<>(b, l);
		JScrollPane jsp = cache.get(p);
		if (jsp == null) {
			jsp = makeList2(algebra, b, l);
			cache.put(p, jsp);
		}
		out.removeAll();
		out.add(jsp, BorderLayout.CENTER);
		out.add(top, BorderLayout.SOUTH);
		out.revalidate();
		out.repaint();
	}

	public static <X> List<String> toString(Collection<X> list) {
		List<String> l = new ArrayList<>(list.size());
		for (X x : list) {
			l.add(x.toString());
		}
		return l;
	}

	private <X, Y> JScrollPane makeList2(
			Algebra<catdata.aql.exp.Ty, catdata.aql.exp.En, catdata.aql.exp.Sym, catdata.aql.exp.Fk, catdata.aql.exp.Att, catdata.aql.exp.Gen, catdata.aql.exp.Sk, X, Y> algebra,
			boolean b, int l) {
		List<JComponent> list = makeList(algebra, b, l);

		JPanel c = new JPanel();
		c.setLayout(new BoxLayout(c, BoxLayout.PAGE_AXIS));

		for (JComponent x : list) {
			x.setAlignmentX(Component.LEFT_ALIGNMENT);
			x.setMinimumSize(x.getPreferredSize());
			c.add(x);
		}

		JPanel p = new JPanel(new GridLayout(1, 1));
		p.add(c);
		JScrollPane jsp = new JScrollPane(p);

		c.setBorder(BorderFactory.createEmptyBorder());
		p.setBorder(BorderFactory.createEmptyBorder());
		jsp.setBorder(BorderFactory.createEmptyBorder());
		return jsp;
	}

	private <X, Y> List<JComponent> makeList(Algebra<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> algebra, boolean simplify,
			int limit) {

		List<JComponent> list = new LinkedList<>();

		Map<En, Set<Pair<Integer, Integer>>> nulls = new THashMap<>();
		Map<En, Pair<List<String>, Object[][]>> entables = makeEnTables(algebra, simplify, limit, nulls);

		for (En en : entables.keySet()) {
			Pair<List<String>, Object[][]> x = entables.get(en);
			String str;
			if (x.second.length < algebra.size(en)) {
				str = en + " (" + x.second.length + " of " + algebra.size(en) + ")";
			} else {
				str = en + " (" + x.second.length + ")";
			}
			if (x.second.length > 0) {
				JComponent p = GuiUtil.makeBoldHeaderTable(nulls.get(en), toString(algebra.schema().attsFrom(en)),
						BorderFactory.createEmptyBorder(), str, x.second, x.first.toArray(new String[x.first.size()]));
				list.add(p);
			}
		}

		List<String> header = Collections.singletonList("Row");

		if (simplify) {
			Map<Ty, Set<Y>> m = Util.revS(algebra.talg().sks);

			Map<Ty, Object[][]> tytables = makeTyTables(m, algebra);

			for (Ty ty : tytables.keySet()) {
				if (!m.containsKey(ty)) {
					continue;
				}
				Object[][] arr = tytables.get(ty);
				String str;
				if (arr.length < m.get(ty).size()) {
					str = ty + " (" + arr.length + " of " + m.get(ty).size() + ")";
				} else {
					str = ty + " (" + arr.length + ")";
				}
				JPanel z = GuiUtil.makeTable(BorderFactory.createEmptyBorder(), str, arr, header.toArray());
				list.add(z);
			}
		}

		if (entables.size() != algebra.schema().ens.size()) {
			list.add(new JLabel("Display suppressed; showing only some tables"));
		}

		return list;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T, C> Unit visit(String k, JTabbedPane ret, TypeSide<T, C> T) {
		ret.addTab("DP", viewDP(T.semantics(), x -> T.collage(), T.js));
		ret.addTab("Model", makeHomSet((TypeSide<Ty, Sym>) T));
		ret.addTab("Cayley", viewCayley2(T));
		return Unit.unit;
	}

	private <En, Sym, Fk, Att, Gen, Sk, X, Y> JPanel makeHomSet(Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> T) {
		JPanel ret = new JPanel(new GridLayout(1, 1));
		JSplitPane pane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		ret.add(pane);

		JPanel top = new JPanel(new GridLayout(3, 1));
		JPanel p1 = new JPanel();
		JPanel p2 = new JPanel();
		JPanel p3 = new JPanel();

		JTextField dst = new JTextField(32);
		p2.add(new JLabel("Enter a type: "));
		p2.add(dst);

		CodeTextPanel bot = new CodeTextPanel(BorderFactory.createEtchedBorder(), "Result", "");

		JButton go = new JButton("Saturate");
		go.addActionListener(x -> {
			String l = dst.getText().trim();
			if (l.isEmpty()) {
				return;
			}
			Ty r0 = Ty.Ty(dst.getText().trim());
			Ty r = r0;
			Runnable runnable = () -> {
				try {
					Set<Term<Ty, En, Sym, Fk, Att, Gen, Sk>> z = T.getModel().get(r);
					if (z == null) {
						bot.setText("anomaly");
						Util.anomaly();
					}
					if (z.isEmpty()) {
						bot.setText("empty");
					} else {
						bot.setText(Util.sep(z, "\n\n"));
					}
				} catch (Exception ex) {
					ex.printStackTrace();
					bot.setText(ex.getMessage());
				}
				// finished = true;
			};
			Thread t = new Thread(runnable);
			try {
				t.start();
				t.join(10000); // TODO aql

				t.stop();
				if (bot.getText().equals("")) {
					bot.setText("Timeout (10s)");
				}

			} catch (Exception ex) {
				ex.printStackTrace();
				throw new RuntimeException("Timout (10s)");
			}

		});
		p3.add(go);
		top.add(p1);
		top.add(p2);
		top.add(p3);

		pane.add(top);
		pane.add(bot);

		return ret;
	}

	private JPanel makeHomSet(TypeSide<Ty, Sym> T) {
		JPanel ret = new JPanel(new GridLayout(1, 1));
		JSplitPane pane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		ret.add(pane);

		JPanel top = new JPanel(new GridLayout(3, 1));
		JPanel p1 = new JPanel();
		JPanel p2 = new JPanel();
		JPanel p3 = new JPanel();

		JTextField src = new JTextField(32);
		JTextField dst = new JTextField(32);
		p1.add(new JLabel("Source Types (sep by ,):"));
		p1.add(src);
		p2.add(new JLabel("Target Type:"));
		p2.add(dst);

		CodeTextPanel bot = new CodeTextPanel(BorderFactory.createEtchedBorder(), "Result", "");

		JButton go = new JButton("Compute hom set");
		go.addActionListener(x -> {
			String b = dst.getText().trim();
			if (b.isEmpty()) {
				return;
			}
			String[] l = src.getText().split(",");
			Ty r = Ty.Ty(b);
			List<Ty> l0 = new LinkedList<>();
			for (String j : l) {
				String j2 = j.trim();
				if (!j2.isEmpty()) {
					l0.add(Ty.Ty(j2));
				}
			}
			Runnable runnable = () -> {
				try {
					Set<Term<Ty, Void, Sym, Void, Void, Void, Void>> z = T.hom(l0, r);
					// Collection<Pair<OplCtx<S, V>, OplTerm<C, V>>> z =
					// kb.hom0(Thread.currentThread(), l0, r);
					// List<String> u = z.stream().map(o -> OplTerm.strip(o.first + " |- " +
					// OplToKB.convert(o.second))).collect(Collectors.toList());
					if (z.isEmpty()) {
						bot.setText("empty");
					} else {
						bot.setText(Util.sep(z, "\n\n"));
					}
				} catch (Exception ex) {
					ex.printStackTrace();
					bot.setText(ex.getMessage());
				}
				// finished = true;
			};
			Thread t = new Thread(runnable);
			try {
				t.start();
				t.join(10000); // TODO aql

				t.stop();
				if (bot.getText().equals("")) {
					bot.setText("Timeout (10s)");
				}

			} catch (Exception ex) {
				ex.printStackTrace();
				throw new RuntimeException("Timout (10s)");
			}

		});
		p3.add(go);
		top.add(p1);
		top.add(p2);
		top.add(p3);

		pane.add(top);
		pane.add(bot);

		return ret;
	}

	@SuppressWarnings("hiding")
	@Override
	public <Ty, En, Sym, Fk, Att> Unit visit(String k, JTabbedPane ret, Schema<Ty, En, Sym, Fk, Att> S) {
		
		ret.addTab("Graph", viewSchema(S));
		// ret.add("Graph2", viewSchemaAlt(S));
		ret.addTab("DP", viewDP(S.dp(), x -> S.collage(), S.typeSide.js));
		// ret.add(new CodeTextPanel("", schema.collage().toString()), "Temp");
		ret.addTab("Acyclic?", acyclicPanel(S));

		ret.addTab("Cayley", viewCayley(S));

		return Unit.unit;
	}

	private <Ty, En, Sym, Fk, Att> Component acyclicPanel(Schema<Ty, En, Sym, Fk, Att> s) {
		JPanel p = new JPanel();
		JButton b = new JButton("Acyclic?");
		p.add(b);
		b.addActionListener(x -> {
			String ac = s.acyclic();
			String acyclic = ac == null ? "true" : ac;
			p.remove(b);
			p.add(new JLabel(acyclic));
			p.revalidate();
			p.repaint();
		});
		return p;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> Unit visit(String k, JTabbedPane ret,
			Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> I) {
		ret.addTab("Tables", viewAlgebra(env.performance.get(k),
				(Algebra<catdata.aql.exp.Ty, catdata.aql.exp.En, catdata.aql.exp.Sym, catdata.aql.exp.Fk, catdata.aql.exp.Att, catdata.aql.exp.Gen, catdata.aql.exp.Sk, X, Y>) I
						.algebra()));

		if (I.algebra().talg().sks.size() < 1024) {
			ret.addTab("TyAlg", new CodeTextPanel("", I.algebra().talgToString()));
			ret.addTab("Hom-sets", makeHomSet((Instance<catdata.aql.exp.Ty, En, Sym, Fk, Att, Gen, Sk, X, Y>) I));

			if (I.size() < 1024) {
				ret.addTab("DP", viewDP(I.dp(), x -> I.collage(), I.schema().typeSide.js));
			} else {
				ret.addTab("DP", new CodeTextPanel("", "Suppressed, size " + I.algebra().talg().sks.size() + "."));
			}

		} else {
			ret.addTab("TyAlg", new CodeTextPanel("", "Suppressed, size " + I.algebra().talg().sks.size() + "."));
			ret.addTab("Hom-sets", new CodeTextPanel("", "Suppressed, size " + I.algebra().talg().sks.size() + "."));
			ret.addTab("DP", new CodeTextPanel("", "Suppressed, size " + I.algebra().talg().sks.size() + "."));

		}
		var z = new CategoryOfElements<>(I).makePanel("", Color.black);
		ret.addTab("Graph", z);
		// ret.addTab("DOT", z.second);

		// ret.addTab("TPTP", new CodeTextPanel("", I.tptp()));

		return Unit.unit;
	}

	@SuppressWarnings("hiding")
	@Override
	public <Ty, En, Sym, Fk, Att, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> Unit visit(String k, JTabbedPane ret,
			Transform<Ty, En, Sym, Fk, Att, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> h) {
		ret.addTab("Tables", viewTransform(h));
		return Unit.unit;
	}

	@Override
	public Unit visit(String k, JTabbedPane ret, Pragma P) {
		return Unit.unit;
	}

	@Override
	public Unit visit(String k, JTabbedPane ret, Comment P) {
		return Unit.unit;
	}

	@Override
	public Unit visit(String k, JTabbedPane ret, Mor P) {
		return Unit.unit;
	}

	@SuppressWarnings("hiding")
	@Override
	public <Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> Unit visit(String k, JTabbedPane ret,
			Query<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> Q) {
		/*
		 * try { Q = Q.unnest(); JComponent comp = new CodeTextPanel("", Q.toString());
		 * ret.addTab("Unnest", comp); } catch (Exception ex) { ex.printStackTrace();
		 * ret.addTab("Unnest Exn", new CodeTextPanel("Exception", ex.getMessage()));
		 * return Unit.unit; }
		 * 
		 * try { JComponent comp = makeQueryPanel(Q); ret.addTab("SQL", comp); } catch
		 * (Exception ex) { ex.printStackTrace(); ret.addTab("SQL Exn", new
		 * CodeTextPanel("Exception", ex.getMessage())); }
		 */
		return Unit.unit;
	}

	public <Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> JComponent makeQueryPanel(
			Query<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> q) {
		try {

			// List<String> l = q.toSQLViews("", "", "id", "varchar", "\"").first;
			List<String> l = new LinkedList<>();
			return new CodeTextPanel("", Util.sep(l, ";\n\n"));
		} catch (Exception ex) {
			// ex.printStackTrace();
			return new CodeTextPanel("SQL exn", ex.getMessage());
		}
	}

	@SuppressWarnings("hiding")
	@Override
	public <Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> Unit visit(String k, JTabbedPane ret,
			Mapping<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> M) {
		// ret.addTab("Translate", viewMorphism(M.semantics(), M.src.typeSide.js));
		ret.addTab("Graph", viewMappingGraph(M));

		ret.addTab("Collage", new CodeTextPanel("", M.collage().toString()));
		return Unit.unit;
	}

	@Override
	public <N, e> Unit visit(String k, JTabbedPane ret, catdata.aql.Graph<N, e> G) {
		ret.add("Graph", viewGraph(G.dmg));
		return Unit.unit;
	}

	@Override
	public <N> Unit visit(String k, JTabbedPane arg, ColimitSchema<N> S) throws RuntimeException {
		return Unit.unit;
	}

	@SuppressWarnings("hiding")
	@Override
	public Unit visit(String k, JTabbedPane arg, Constraints S) throws RuntimeException {
		return Unit.unit;
	}

	@Override
	public Unit visit(String k, JTabbedPane pane, ApgTypeside t) throws RuntimeException {

		Object[][] rowData = new Object[t.Bs.size()][3];
		Object[] colNames = new Object[2];
		colNames[0] = "Base Type";
		colNames[1] = "Java Class";
		int j = 0;
		for (Entry<String, Pair<Class<?>, java.util.function.Function<String, Object>>> lt : t.Bs.entrySet()) {
			rowData[j][0] = lt.getKey();
			rowData[j][1] = lt.getValue().first.getName();
			j++;
		}
		JPanel x = GuiUtil.makeTable(BorderFactory.createEmptyBorder(), null, rowData, colNames);

		JPanel c = new JPanel();
		c.setLayout(new BoxLayout(c, BoxLayout.PAGE_AXIS));

		x.setAlignmentX(Component.LEFT_ALIGNMENT);
		x.setMinimumSize(x.getPreferredSize());
		c.add(x);

		JPanel p = new JPanel(new GridLayout(1, 1));
		p.add(c);
		JScrollPane jsp = new JScrollPane(p);

		c.setBorder(BorderFactory.createEmptyBorder());
		p.setBorder(BorderFactory.createEmptyBorder());
		jsp.setBorder(BorderFactory.createEmptyBorder());

		pane.addTab("Table", p);

		return Unit.unit;
	}

	@Override
	public <L, e> Unit visit(String k, JTabbedPane pane, ApgInstance<L, e> G) throws RuntimeException {
		apgInst0(pane, G);
		apgInst1(pane, G);

		/*
		 * Graph<Pair<String,Object>, Pair<String, Integer>> sgv = new
		 * DirectedSparseMultigraph<>();
		 *
		 *
		 * int i = 0; for (En1 en1 : G.) { sgv.addVertex(Node.En1(en1));
		 * sgv.addEdge(Chc.inRight(i++), Node.En1(en1), Node.En2(M.ens.get(en1))); } for
		 * (En2 en2 : M.dst.ens) { sgv.addVertex(Node.En2(en2)); } // for (Ty ty :
		 * M.dst.typeSide.tys) { // sgv.addVertex(Node.Ty(ty)); // } for (Fk1 fk1 :
		 * M.src.fks.keySet()) { Pair<En2, List<Fk2>> l = M.fks.get(fk1);
		 * sgv.addEdge(Chc.inLeft(i++), Node.En1(M.src.fks.get(fk1).first),
		 * Node.Fk1(fk1)); sgv.addEdge(Chc.inLeft(i++), Node.Fk1(fk1),
		 * Node.En1(M.src.fks.get(fk1).second));
		 *
		 * if (l.second.isEmpty()) { sgv.addEdge(Chc.inRight(i++), Node.Fk1(fk1),
		 * Node.En2(l.first)); } else { for (Fk2 fk2 : l.second) {
		 * sgv.addEdge(Chc.inRight(i++), Node.Fk1(fk1), Node.Fk2(fk2)); } } } if
		 * (showAtts) { for (Att1 att1 : M.src.atts.keySet()) { Triple<Var, En2,
		 * Term<Ty, En2, Sym, Fk2, Att2, Void, Void>> l = M.atts.get(att1);
		 * sgv.addEdge(Chc.inLeft(i++), Node.En1(M.src.atts.get(att1).first),
		 * Node.Att1(att1)); // sgv.addEdge(Chc.inLeft(i++), Node.Att1(att1), //
		 * Node.Ty(M.src.atts.get(att1).second));
		 *
		 * for (Att2 x : l.third.atts()) { sgv.addEdge(Chc.inRight(i++),
		 * Node.Att1(att1), Node.Att2(x)); } // for (Fk2 x : l.third.fks()) { //
		 * sgv.addEdge(Chc.inRight(i++), Node.Att1(att1), Node.Fk2(x)); // } /* boolean
		 * b = false; for (Sym x : l.third.syms()) { sgv.addEdge(Chc.inRight(i++),
		 * Node.Att1(att1), Node.Ty(M.dst.typeSide.syms.get(x).second)); b = true;
		 * break; } if (!b) { for (Pair<Object, Ty> x : l.third.objs()) {
		 * sgv.addEdge(Chc.inRight(i++), Node.Att1(att1), Node.Ty(x.second)); b = true;
		 * break; } }
		 */
		/*
		 * } } for (Fk2 fk2 : M.dst.fks.keySet()) { sgv.addEdge(Chc.inLeft(i++),
		 * Node.En2(M.dst.fks.get(fk2).first), Node.Fk2(fk2));
		 * sgv.addEdge(Chc.inLeft(i++), Node.Fk2(fk2),
		 * Node.En2(M.dst.fks.get(fk2).second));
		 *
		 * } if (showAtts) { for (Att2 att2 : M.dst.atts.keySet()) {
		 * sgv.addEdge(Chc.inLeft(i++), Node.En2(M.dst.atts.get(att2).first),
		 * Node.Att2(att2)); // sgv.addEdge(Chc.inLeft(i++), Node.Att2(att2), //
		 * Node.Ty(M.dst.atts.get(att2).second)); } }
		 *
		 * if (sgv.getVertexCount() == 0) { return new JPanel(); } FRLayout<Node<Ty,
		 * En1, Sym, Fk1, Att1, En2, Fk2, Att2>, Chc<Integer, Integer>> layout = new
		 * FRLayout<>(sgv);
		 *
		 * layout.setSize(new Dimension(600, 400)); VisualizationViewer<Node<Ty, En1,
		 * Sym, Fk1, Att1, En2, Fk2, Att2>, Chc<Integer, Integer>> vv = new
		 * VisualizationViewer<>( layout); Function<Node<Ty, En1, Sym, Fk1, Att1, En2,
		 * Fk2, Att2>, Paint> vertexPaint = x -> x.color();
		 * DefaultModalGraphMouse<Chc<Ty, En>, Chc<Fk, Att>> gm = new
		 * DefaultModalGraphMouse<>(); gm.setMode(Mode.TRANSFORMING);
		 * vv.setGraphMouse(gm); gm.setMode(Mode.PICKING);
		 * vv.getRenderContext().setVertexFillPaintTransformer(vertexPaint);
		 *
		 * // vv.getRenderContext().setVert
		 * vv.getRenderContext().setEdgeStrokeTransformer(x -> { if (!x.left) { Stroke
		 * dashed = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0,
		 * new float[] { 4 }, 0); return dashed; } return new BasicStroke(); });
		 *
		 * Function<Chc<Integer, Integer>, String> et = x -> ""; Function<Node<Ty, En1,
		 * Sym, Fk1, Att1, En2, Fk2, Att2>, String> vt = x -> x.toString();
		 * vv.getRenderContext().setEdgeLabelTransformer(et);
		 * vv.getRenderContext().setVertexLabelTransformer(vt);
		 *
		 * GraphZoomScrollPane zzz = new GraphZoomScrollPane(vv); JPanel ret = new
		 * JPanel(new GridLayout(1, 1)); ret.add(zzz);
		 * ret.setBorder(BorderFactory.createEtchedBorder());
		 *
		 * vv.getRenderContext().setLabelOffset(16); vv.setBackground(Color.white);
		 *
		 * return ret;
		 */
		return Unit.unit;
	}

	private <e, L> void apgInst0(JTabbedPane pane, ApgInstance<L, e> G) {
		List<JComponent> list = new LinkedList<>();

		Object[][] rowData;
		Object[] colNames;

		rowData = new Object[G.Es.size()][3];
		colNames = new Object[3];
		colNames[0] = "Element";
		colNames[1] = "Label";
		colNames[2] = "Value";
		int j = 0;
		for (Entry<e, Pair<L, ApgTerm<L, e>>> lt : G.Es.entrySet()) {
			rowData[j][0] = lt.getKey();
			rowData[j][1] = lt.getValue().first;
			rowData[j][2] = lt.getValue().second;
			j++;
		}
		list.add(GuiUtil.makeTable(BorderFactory.createEmptyBorder(), "Data", rowData, colNames));

		JPanel c = new JPanel();
		c.setLayout(new BoxLayout(c, BoxLayout.PAGE_AXIS));

		for (JComponent x : list) {
			x.setAlignmentX(Component.LEFT_ALIGNMENT);
			x.setMinimumSize(x.getPreferredSize());
			c.add(x);
		}

		JPanel p = new JPanel(new GridLayout(1, 1));
		p.add(c);
		JScrollPane jsp = new JScrollPane(p);

		c.setBorder(BorderFactory.createEmptyBorder());
		p.setBorder(BorderFactory.createEmptyBorder());
		jsp.setBorder(BorderFactory.createEmptyBorder());
		pane.addTab("Tables", p);
	}

	private <e, L> void apgInst1(JTabbedPane pane, ApgInstance<L, e> G) {
		List<JComponent> list = new LinkedList<>();

		Map<L, Set<e>> map = Util.revS(Util.map(G.Es, (w, v) -> new Pair<>(w, v.first)));

		for (Entry<L, ApgTy<L>> lt : G.Ls.entrySet()) {
			ApgTy<L> t = lt.getValue();
			L l = lt.getKey();
			Object[][] rowData;
			Object[] colNames;

			if (t.m != null && t.all) {
				colNames = new Object[t.m.size() + 1];
				colNames[0] = l;
				int i = 1;
				for (Entry<String, ApgTy<L>> x : t.m.entrySet()) {
					colNames[i++] = x.getKey() + " : " + x.getValue();
				}
				Set<e> set = map.get(l);
				if (set == null) {
					set = Collections.emptySet();
				}
				rowData = new Object[set.size()][i];
				int j = 0;
				for (e elem : set) {
					ApgTerm<L, e> w = G.Es.get(elem).second;
					rowData[j][0] = elem;
					int u = 1;
					for (String f : t.m.keySet()) {
						rowData[j][u++] = w.fields.get(f);
					}
					j++;
				}
			} else {
				colNames = new Object[2];
				colNames[0] = l;
				colNames[1] = t.toString();
				Set<e> set = map.get(l);
				rowData = new Object[set.size()][2];
				int j = 0;
				for (e elem : set) {
					ApgTerm<L, e> w = G.Es.get(elem).second;
					rowData[j][0] = elem;
					rowData[j][1] = w;
					j++;
				}
			}
			list.add(GuiUtil.makeTable(BorderFactory.createEmptyBorder(), null, rowData, colNames));

		}

		JPanel c = new JPanel();
		c.setLayout(new BoxLayout(c, BoxLayout.PAGE_AXIS));

		for (JComponent x : list) {
			x.setAlignmentX(Component.LEFT_ALIGNMENT);
			x.setMinimumSize(x.getPreferredSize());
			c.add(x);
		}

		JPanel p = new JPanel(new GridLayout(1, 1));
		p.add(c);
		JScrollPane jsp = new JScrollPane(p);

		c.setBorder(BorderFactory.createEmptyBorder());
		p.setBorder(BorderFactory.createEmptyBorder());
		jsp.setBorder(BorderFactory.createEmptyBorder());
		pane.addTab("Labels", p);
	}

	@Override
	public <l1, e1, l2, e2> Unit visit(String k, JTabbedPane pane, ApgTransform<l1, e1, l2, e2> t)
			throws RuntimeException {
		List<JComponent> list = new LinkedList<>();

		Object[][] rowData = new Object[t.lMap.size()][2];
		Object[] colNames = new Object[2];
		colNames[0] = "Input Label";
		colNames[1] = "Output Label";
		int j = 0;
		for (Entry<l1, l2> lt : t.lMap.entrySet()) {
			rowData[j][0] = lt.getKey();
			rowData[j][1] = lt.getValue();
			j++;
		}
		list.add(GuiUtil.makeTable(BorderFactory.createEmptyBorder(), "Schema", rowData, colNames));

		rowData = new Object[t.eMap.size()][2];
		colNames = new Object[2];
		colNames[0] = "Input Element";
		colNames[1] = "Output Element";
		j = 0;
		for (Entry<e1, e2> lt : t.eMap.entrySet()) {
			rowData[j][0] = lt.getKey();
			rowData[j][1] = lt.getValue();
			j++;
		}
		list.add(GuiUtil.makeTable(BorderFactory.createEmptyBorder(), "Data", rowData, colNames));

		JPanel c = new JPanel();
		c.setLayout(new BoxLayout(c, BoxLayout.PAGE_AXIS));

		for (JComponent x : list) {
			x.setAlignmentX(Component.LEFT_ALIGNMENT);
			x.setMinimumSize(x.getPreferredSize());
			c.add(x);
		}

		JPanel p = new JPanel(new GridLayout(1, 1));
		p.add(c);
		JScrollPane jsp = new JScrollPane(p);

		c.setBorder(BorderFactory.createEmptyBorder());
		p.setBorder(BorderFactory.createEmptyBorder());
		jsp.setBorder(BorderFactory.createEmptyBorder());
		pane.addTab("Labels", p);
		return Unit.unit;
	}

	private <L> void apgSch0(JTabbedPane pane, ApgSchema<L> Ls) {
		List<JComponent> list = new LinkedList<>();

		Object[][] rowData = new Object[Ls.size()][2];
		Object[] colNames = new Object[2];
		colNames[0] = "Label";
		colNames[1] = "Type";
		int j = 0;
		for (Entry<L, ApgTy<L>> lt : Ls.entrySet()) {
			rowData[j][0] = lt.getKey();
			rowData[j][1] = lt.getValue();
			j++;
		}
		list.add(GuiUtil.makeTable(BorderFactory.createEmptyBorder(), "Schema", rowData, colNames));

		JPanel c = new JPanel();
		c.setLayout(new BoxLayout(c, BoxLayout.PAGE_AXIS));

		for (JComponent x : list) {
			x.setAlignmentX(Component.LEFT_ALIGNMENT);
			x.setMinimumSize(x.getPreferredSize());
			c.add(x);
		}

		JPanel p = new JPanel(new GridLayout(1, 1));
		p.add(c);
		JScrollPane jsp = new JScrollPane(p);

		c.setBorder(BorderFactory.createEmptyBorder());
		p.setBorder(BorderFactory.createEmptyBorder());
		jsp.setBorder(BorderFactory.createEmptyBorder());
		pane.addTab("Tables", p);
	}

	@Override
	public <L> Unit visit(String k, JTabbedPane arg, ApgSchema<L> t) throws RuntimeException {
		apgSch0(arg, t);

		return Unit.unit;
	}

	@Override
	public <L1, L2> Unit visit(String k, JTabbedPane arg, ApgMapping<L1, L2> t) throws RuntimeException {
		// TODO Auto-generated method stub

		return Unit.unit;
	}

	///////////////////////////////////////////////////////

}
