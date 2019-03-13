package catdata.aql.gui;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Function;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import catdata.Program;
import catdata.Util;
import catdata.aql.Kind;
import catdata.aql.exp.AqlEnv;
import catdata.aql.exp.AqlStatic;
import catdata.aql.exp.AqlTyping;
import catdata.aql.exp.Exp;
import catdata.aql.exp.InteriorLabel;
import catdata.aql.exp.Raw;
import catdata.ide.Outline;

public class AqlOutline extends Outline<Program<Exp<?>>, AqlEnv, AqlDisplay> {

	private class TreeLabel {
		private final String s;

		private final boolean prefix;

		private final AqlTyping G;

		private final boolean useTypes;

		public TreeLabel(String s, boolean prefix, AqlTyping G, boolean useTypes) {
			super();
			this.s = s;
			this.prefix = prefix;
			this.G = G;
			this.useTypes = useTypes;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((s == null) ? 0 : s.hashCode());
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
			TreeLabel other = (TreeLabel) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (s == null) {
				if (other.s != null)
					return false;
			} else if (!s.equals(other.s))
				return false;
			return true;
		}

		private AqlOutline getOuterType() {
			return AqlOutline.this;
		}

		public String toString() {
			Kind k = G.prog.exps.get(s).kind();
			if (useTypes) {
				return AqlDisplay.doLookup(prefix, s, k, G);
			}
			return prefix ? k + s : s;
		}

	}

	protected synchronized void threadBody() {
		try {
			String s = codeEditor.topArea.getText();
//			if (System.currentTimeMillis() - codeEditor.last_keystroke > codeEditor.sleepDelay) {

			if (!s.equals(codeEditor.parsed_prog_string)) {
			//	oLabel.setText("?");
				AqlCodeEditor codeEditor2 = (AqlCodeEditor) codeEditor;
				codeEditor2.aqlStatic.doIt(s);
				if (codeEditor2.aqlStatic.env != null && codeEditor2.aqlStatic.env.prog != null) {
					
				Program<Exp<?>> e = codeEditor2.aqlStatic.env.prog; // codeEditor.parse(s);
				oLabel.setText("");

					synchronized (codeEditor.parsed_prog_lock) {
						codeEditor.parsed_prog = e;
						codeEditor.parsed_prog_string = s;
					}
					build();
					//codeEditor.clearSpellCheck();
//				}
				}
			}
					
		} catch (Throwable ex) {
			ex.printStackTrace();
			oLabel.setText("err");
		}
	}

	private synchronized DefaultMutableTreeNode makeTree(List<String> set, @SuppressWarnings("unused") String prog, boolean prefix, boolean alpha,
			boolean useTypes) {
		DefaultMutableTreeNode root = new DefaultMutableTreeNode();
		AqlCodeEditor codeEditor2 = (AqlCodeEditor) codeEditor;
		AqlStatic s = codeEditor2.aqlStatic;

//		codeEditor2.topArea.forceReparsing(codeEditor2.aqlStatic);
		AqlTyping G = s.env.typing; 
		
		for (String k : set) {
			Exp<?> e = s.env.prog.exps.get(k);
			if (e == null) {
				// System.out.println(k);
				Util.anomaly();
			}
			if (e.kind().equals(Kind.COMMENT)) {
				continue;
			}
			DefaultMutableTreeNode n = new DefaultMutableTreeNode();
			n.setUserObject(new TreeLabel(k, prefix, G, useTypes));
			asTree(n, alpha, e);
			root.add(n);
		}
		return root;
	
	}

	private void asTree(DefaultMutableTreeNode root, boolean alpha, Exp<?> e) {
		if (e instanceof Raw) {
			Raw T = (Raw) e;
			for (String k : T.raw().keySet()) {
				List<InteriorLabel<Object>> v = T.raw().get(k);
				add(root, v, k, t -> t, alpha);
			}
		}

	}

	JTree tree;

	protected synchronized JTree getComp() {
		if (tree != null) {
			return tree;
		}
		tree = new JTree(new DefaultMutableTreeNode());
		tree.setShowsRootHandles(true);
		tree.setRootVisible(false);
		tree.setCellRenderer(makeRenderer());
		tree.setEditable(true);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseReleased(MouseEvent e) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();

				if (node == null) {
					return;
				}
				Object o = node.getUserObject();
				if (o instanceof TreeLabel) {
					TreeLabel l = (TreeLabel) o;
					if (codeEditor.parsed_prog.exps.containsKey(l.s)) {
						Integer line = codeEditor.parsed_prog.getLine(l.s);
						codeEditor.setCaretPos(line);
						codeEditor.addToHistory(line);
					}
				} else if (o instanceof InteriorLabel) {
					InteriorLabel<?> l = (InteriorLabel<?>) o;
					codeEditor.setCaretPos(l.loc);
					codeEditor.addToHistory(l.loc);
				}

			}
		});

		return tree;
	}

	private DefaultTreeCellRenderer makeRenderer() {
		DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
		renderer.setLeafIcon(null);
		renderer.setOpenIcon(null);
		renderer.setClosedIcon(null);
		return renderer;
	}

	private TreePath conv(TreePath path) {
		TreePath parent = path.getParentPath();
		if (parent == null) {
			return new TreePath(getComp().getModel().getRoot());
		}
		TreePath rest = conv(parent);
		DefaultMutableTreeNode last = (DefaultMutableTreeNode) rest.getLastPathComponent();
		DefaultMutableTreeNode us = (DefaultMutableTreeNode) path.getLastPathComponent();
		Enumeration<TreeNode> cs = last.children();
		if (cs == null) {
			return null;
		}
		while (cs.hasMoreElements()) {
			DefaultMutableTreeNode m = (DefaultMutableTreeNode) cs.nextElement();
			if (nodeEq(m, us)) {
				return rest.pathByAddingChild(m);
			}
		}
		return null;
	}

	private boolean nodeEq(DefaultMutableTreeNode m, DefaultMutableTreeNode n) {
		if (!n.getUserObject().equals(m.getUserObject())) {
			return false;
		}
		if (m.getChildCount() != n.getChildCount()) {
			return false;
		}
		Enumeration<TreeNode> e1 = m.children();
		Enumeration<TreeNode> e2 = m.children();
		if (e1 == null && e2 == null) {
			return true;
		}
		if (e1 == null || e2 == null) {
			return false;
		}
		while (e1.hasMoreElements()) {
			boolean b = nodeEq((DefaultMutableTreeNode) e1.nextElement(), (DefaultMutableTreeNode) e2.nextElement());
			if (!b) {
				return false;
			}
		}
		return true;
	}

	protected synchronized void setComp(List<String> set) {
		TreePath t1 = getComp().getSelectionPath();

		Enumeration<TreePath> p = getComp().getExpandedDescendants(new TreePath(getComp().getModel().getRoot()));

		getComp().setModel(new DefaultTreeModel(makeTree(set, codeEditor.getText(), codeEditor.outline_prefix_kind,
				codeEditor.outline_alphabetical, codeEditor.outline_types)));
		tree.setCellRenderer(makeRenderer());

		if (p == null) {
			return;
		}
		while (p.hasMoreElements()) {
			try {
				TreePath path = p.nextElement();
				if (conv(path) != null) {
					getComp().expandPath(conv(path));
				}
			} catch (Exception ex) {
			}
		}

		if (t1 != null) {
			TreePath t2 = conv(t1);
			if (t2 != null) {
				getComp().setSelectionPath(t2);
				getComp().scrollPathToVisible(t2);
			}
		}

	}

	@Override
	protected boolean equiv(Program<Exp<?>> now, Program<Exp<?>> then) {
		return false; 
	}

	public AqlOutline(AqlCodeEditor codeEditor) {
		super(codeEditor);
		//codeEditor2 = codeEditor;
	}

	//final AqlCodeEditor codeEditor2;

	private <X, Y, Z> void add(DefaultMutableTreeNode root, Collection<X> x, Y y, Function<X, Z> f, boolean alpha) {
		if (x.size() > 0) {
			DefaultMutableTreeNode n = new DefaultMutableTreeNode();
			n.setUserObject(y);
			for (X t : Util.alphaMaybe(alpha, x)) {
				DefaultMutableTreeNode m = new DefaultMutableTreeNode();
				m.setUserObject(f.apply(t));
				if (t instanceof Exp) {
					asTree(m, alpha, (Exp<?>) t);
				} else if (t instanceof InteriorLabel) {
					InteriorLabel<?> l = (InteriorLabel<?>) t;
					if (l.s instanceof Exp) {
						asTree(m, alpha, (Exp<?>) l.s);
					}
				}
				n.add(m);
			}
			root.add(n);
		}
	}

	

}