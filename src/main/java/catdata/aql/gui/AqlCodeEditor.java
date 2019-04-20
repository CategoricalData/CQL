package catdata.aql.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.GridLayout;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

import javax.swing.DefaultListModel;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.commons.lang3.text.WordUtils;
import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.autocomplete.ShorthandCompletion;
import org.fife.ui.rsyntaxtextarea.ErrorStrip.ErrorStripMarkerToolTipProvider;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParserNotice;
import org.fife.ui.rsyntaxtextarea.parser.Parser;
import org.fife.ui.rsyntaxtextarea.parser.ParserNotice;

import catdata.InteriorLabel;
import catdata.ParseException;
import catdata.Program;
import catdata.Raw;
import catdata.Unit;
import catdata.Util;
import catdata.aql.Kind;
import catdata.aql.exp.AqlDoc;
import catdata.aql.exp.AqlEnv;
import catdata.aql.exp.AqlMultiDriver;
import catdata.aql.exp.AqlParserFactory;
import catdata.aql.exp.AqlStatic;
import catdata.aql.exp.AqlTyping;
import catdata.aql.exp.Exp;
import catdata.aql.exp.IAqlParser;
import catdata.ide.CodeEditor;
import catdata.ide.CodeTextPanel;
import catdata.ide.GUI;
import catdata.ide.Language;

@SuppressWarnings("serial")
public final class AqlCodeEditor extends CodeEditor<Program<Exp<?>>, AqlEnv, AqlDisplay> {

	public void format() {
		String input = topArea.getText();
		try {
			Program<Exp<?>> p = parse(input);
			if (p == null) {
				return;
			}

			if (input.contains("//") || input.contains("/*")) {
				int x = JOptionPane.showConfirmDialog(null, "Formatting will erase all comments - continue?",
						"Continue?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
				if (x != JOptionPane.YES_OPTION) {
					return;
				}
			}
			// order does not contain enums or drops
			StringBuilder sb = new StringBuilder();
			if (!p.options.options.isEmpty()) {
				sb.append("options\n");
				sb.append(Util.sep(p.options.options, " = ", "\n"));
			}
			for (String k : p.order) {
				Exp<?> o = p.exps.get(k);
				if (o.kind().equals(Kind.COMMENT)) {
					sb.append("md { (* \"" + o + "\" *) }\n\n");
				} else {
					sb.append(o.kind() + " " + k + " = " + o.toString() + "\n\n");
				}
			}
			topArea.setText(sb.toString().trim());
			topArea.setCaretPosition(0);
			topArea.forceReparsing(aqlStatic);

		} catch (Exception ex) {
			return;

		}
	}

	@Override
	public void abortAction() {
		super.abortAction();
		if (driver != null) {
			driver.abort();
		}

	}

	public AqlCodeEditor(String title, int id, String content) {
		super(title, id, content, new BorderLayout());
		// aqlStatic = new AqlStatic(new Program<>(Collections.emptyList(), ""));
		JMenuItem im = new JMenuItem("Infer Map/Query/Trans/Inst");
		im.addActionListener(x -> infer());
		topArea.getPopupMenu().add(im, 0);

		DocumentListener listener = new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				doUpdate();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				doUpdate();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
			}
		};
		topArea.getDocument().addDocumentListener(listener);

		JMenuItem html = new JMenuItem("Emit HTML");
		html.addActionListener(x -> emitDoc());
		topArea.getPopupMenu().add(html, 0);

		q.add(Unit.unit);

	}

	AqlStatic aqlStatic;

	public void emitDoc() {
		try {
			if (last_env == null || last_prog == null) {
				respArea.setText("Must compile before emitting documentation.");
			}
			String html = AqlDoc.doc(last_env, last_prog);

			File file = File.createTempFile("catdata" + title, ".html");
			FileWriter w = new FileWriter(file);
			w.write(html);
			w.close();

			JTabbedPane p = new JTabbedPane();
			JEditorPane pane = new JEditorPane("text/html", html);
			pane.setEditable(false);
			pane.setCaretPosition(0);
			// p.addTab("Rendered", new JScrollPane(pane));

			p.addTab("HTML", new CodeTextPanel("", html));
			JFrame f = new JFrame("HTML for " + title);
			JPanel ret = new JPanel(new GridLayout(1, 1));
			f.setContentPane(ret);
			ret.add(p);
			f.setSize(600, 800);
			f.setLocation(100, 600);
			f.setVisible(true);
			f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

			Desktop.getDesktop().browse(file.toURI());

		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	@Override
	public Language lang() {
		return Language.CQL;
	}

	@Override
	protected String getATMFlhs() {
		return "text/" + Language.CQL.name();
	}

	@Override
	protected String getATMFrhs() {
		return "catdata.aql.gui.AqlTokenMaker";
	}

	@Override
	public synchronized void close() {
		super.close();
		for (AutoCompletion ac : acs) {
			ac.uninstall();
		}
	}

	volatile List<AutoCompletion> acs;

	@Override
	protected synchronized void doTemplates() {
		CompletionProvider provider = createCompletionProvider();
		AutoCompletion ac = new AutoCompletion(provider);
		KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE,
				InputEvent.META_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
		ac.setTriggerKey(key);
		ac.install(topArea);
		if (acs == null) {
			acs = Collections.synchronizedList(new LinkedList<>());
		}
		acs.add(ac); // System.out.println("...");
	}

	// private void addCompletionProviders(CompletionProvider prover) {

	// }

	private static CompletionProvider createCompletionProvider() {
		DefaultCompletionProvider provider = new DefaultCompletionProvider();

		provider.addCompletion(new ShorthandCompletion(provider, "typeside",
				"typeside ? = literal {\n\timports\n\ttypes\n\tconstants\n\tfunctions\n\tjava_types\n\tjava_constants\n\tjava_functions\n\tequations\n\toptions\n} ",
				""));

		provider.addCompletion(new ShorthandCompletion(provider, "schema",
				"schema ? = literal : ? {\n\timports\n\tentities\n\tforeign_keys\n\tpath_equations\n\tattributes\n\tobservation_equations\n\toptions\n} ",
				""));

		provider.addCompletion(new ShorthandCompletion(provider, "instance",
				"instance ? = literal : ? {\n\timports\n\tgenerators\n\tequations\n\toptions\n} ", ""));

		provider.addCompletion(new ShorthandCompletion(provider, "graph",
				"graph ? = literal : ? {\n\timports\n\tnodes\n\tedges\n} ", ""));

		provider.addCompletion(new ShorthandCompletion(provider, "mapping",
				"mapping ? = literal : ? -> ? {\n\timports\n\tentities\n\tforeign_keys\n\tattributes\n\toptions\n} ",
				""));

		provider.addCompletion(new ShorthandCompletion(provider, "transform",
				"transform ? = literal : ? -> ? {\n\timports\n\tgenerators\n\toptions\n} ", ""));

		provider.addCompletion(new ShorthandCompletion(provider, "query",
				"query ? = literal : ? -> ? {\n" + "\n entities" + "\n  e -> {for x:X y:Y "
						+ "\n        where f(x)=f(x) g(y)=f(y) " + "\n        return att -> at(a) att2 -> at(a) "
						+ "\n        options" + "\n  }" + "\n" + "\n foreign_keys" + "\n  f -> {x -> a.g y -> f(y) "
						+ "\n        options" + "\n  }" + "\n options" + "\n}",
				""));

		provider.addCompletion(new ShorthandCompletion(provider, "import_csv",
				"import_csv path : schema (resp. inst -> inst) {imports options} ", ""));

		provider.addCompletion(new ShorthandCompletion(provider, "export_csv",
				"export_csv_instance (resp. export_csv_transform) inst (resp. trans) path {options} ", ""));

		provider.addCompletion(new ShorthandCompletion(provider, "import_csv",
				"import_jdbc classname url prefix : schema (resp. inst -> inst) {\nen -> sql ty -> sql (resp + att -> sql fk -> sql) ...}",
				""));

		provider.addCompletion(new ShorthandCompletion(provider, "export_csv",
				"export_jdbc_instance (resp export_jdbc_transform) classname url prefix {options} ", ""));

		return provider;

	}

	@Override
	public Program<Exp<?>> parse(String program) throws ParseException {
		this.last_parser = AqlParserFactory.getParser();
		return this.last_parser.parseProgram(program);
	}

	@Override
	protected AqlDisplay makeDisplay(String foo, Program<Exp<?>> init, AqlEnv env, long start, long middle) {
		AqlDisplay ret = new AqlDisplay(foo, init, env, start, middle);
		if (env.exn != null) {
			GUI.topFrame.toFront();
		}
		return ret;
	}

	// private String last_str;
	private IAqlParser last_parser;
	private Program<Exp<?>> last_prog; // different that env's
	public AqlEnv last_env;
	private AqlMultiDriver driver;

	@Override
	protected AqlEnv makeEnv(String str, Program<Exp<?>> init) {
		driver = new AqlMultiDriver(init, toUpdate, last_env);
		driver.start();
		last_env = driver.env; // constructor blocks
		last_prog = init;
		// topArea.forceReparsing(parser);
		// clearSpellCheck();
		if (last_env.exn != null && last_env.defs.keySet().isEmpty()) {
			throw last_env.exn;
		}
		return last_env;
	}

	@Override
	protected String textFor(AqlEnv env) {
		return "Done.";
	}

	public void infer() {
		try {
			String s = Inferrer.infer(topArea.getSelectedText(), aqlStatic.env);
			if (s != null) {
				topArea.insert(s, topArea.getSelectionEnd());
			}
		} catch (Throwable thr) {
			thr.printStackTrace();
			String z = "Anomaly, please report";
			if (thr.getMessage() != null) {
				z = thr.getMessage();
			}
			respArea.setText(z);
		}
	}

	@Override
	protected boolean omit(String s, Program<Exp<?>> p) {
		return p.exps.get(s).kind().equals(Kind.COMMENT);
	}

	@Override
	protected Collection<String> reservedWords() {
		return Collections.emptyList();

	}

	protected void doUpdate() {
		if (q.isEmpty()) {
			q.add(Unit.unit);
		}
		// qt = System.currentTimeMillis();
	}
	
	

	public void clearSpellCheck() {
		SwingUtilities.invokeLater(() -> {
			try {
				topArea.clearParsers();
				topArea.addParser(aqlStatic);
				topArea.forceReparsing(aqlStatic);
				topArea.revalidate();
	
			//	errorStrip.setMarkerToolTipProvider(provider);
			//	topArea.setmark
			} catch (Throwable t) {
			}
		});
	}

	@Override
	protected void threadBody() {
		aqlStatic = new AqlStatic(new Program<>(Collections.emptyList(), ""));
		while (!isClosed) {
			String s;
			try {
				q.takeFirst();
				s = topArea.getText();
				// todo: shortcut on ""?
			} catch (InterruptedException e) {
				return; /// ?
			}
			if (parsed_prog_string != null && parsed_prog_string.equals(s)) {
				continue;
			}
			parsed_prog_string = s;
			// aqlStatic = new AqlStatic(new Program<>(Collections.emptyList(), ""));
			// topArea.forceReparsing(aqlStatic);
			oLabel.setText(" ? ");

			try {
				parsed_prog = parse(s);
				if (q.peek() != null) {
					continue;
				}
				// parsed_prog = z;
				aqlStatic = new AqlStatic(parsed_prog);				
				topArea.clearParsers();
				topArea.addParser(aqlStatic);
				clearSpellCheck();
				aqlStatic.typeCheck(topArea);
				if (q.peek() != null) {
					continue;
				}
				build();
				if (!validateBox.isSelected() || q.peek() != null) {
					continue;
				}
				aqlStatic.validate(topArea);
				clearSpellCheck();
//				topArea.revalidate();
				// System.out.println(aqlStatic.exns);
				if (aqlStatic.result.getNotices().isEmpty()) {
					oLabel.setText("  ");
				} else {
					oLabel.setText("err");
				}
			} catch (ParseException exn) {
				try {
					DefaultParserNotice notice = new StaticParserNotice((Parser) aqlStatic, exn.getMessage(), exn.line,
							Color.red);
					
					aqlStatic.result.addNotice(notice);
					clearSpellCheck();
					oLabel.setText("err");
				} catch (Throwable thr) {
					thr.printStackTrace();
					oLabel.setText("anomaly please report");
				}
				// continue;
			} catch (Throwable exn) {
				try {
					// exn.printStackTrace();
					// DefaultParserNotice notice = new StaticParserNotice((Parser)aqlStatic,
					// exn.getMessage(), exn.line, Color.red);
					clearSpellCheck();
					oLabel.setText("err");
				} catch (Throwable thr) {
					thr.printStackTrace();
					oLabel.setText("anomaly please report");
				}
			}
			// oLabel.setText(text);
		}
	}

	public static class StaticParserNotice extends DefaultParserNotice {
		Color c;

		public StaticParserNotice(Parser parser, String msg, int line, Color c) {
			super(parser, truncate(msg), line);
			this.c = c;
		}

		@Override
		public Color getColor() {
			return c;
		}
	}

	

	@Override
	protected DefaultMutableTreeNode makeTree(List<String> set) {
		DefaultMutableTreeNode root = new DefaultMutableTreeNode();
		AqlTyping G = aqlStatic.env.typing;

		for (String k : set) {
			Exp<?> e = aqlStatic.env.prog.exps.get(k);
			if (e == null) {
				// System.out.println(k + " " + s);
				Util.anomaly();
			}
			if (e.kind().equals(Kind.COMMENT)) {
				continue;
			}
			String k0 = k;
			if (G.prog.exps.containsKey(k)) {
				Kind kk = G.prog.exps.get(k).kind();
				if (outline_types) {
					k0 = AqlDisplay.doLookup(outline_prefix_kind, k, kk, G);
				} else {
					k0 = outline_prefix_kind ? kk + k : k;
				}
			}

			DefaultMutableTreeNode n = new DefaultMutableTreeNode();
			n.setUserObject(new TreeLabel(k0, k));
			asTree(n, e);
			root.add(n);
		}
		return root;

	}

	private void asTree(DefaultMutableTreeNode root, Exp<?> e) {
		if (e instanceof Raw) {
			Raw T = (Raw) e;
			for (String k : T.raw().keySet()) {
				List<InteriorLabel<Object>> v = T.raw().get(k);
				add(root, v, k, t -> t);
			}
		}
	}

//	final AqlCodeEditor codeEditor;

	private <X, Y, Z> void add(DefaultMutableTreeNode root, Collection<X> x, Y y, Function<X, Z> f) {
		if (x.size() > 0) {
			DefaultMutableTreeNode n = new DefaultMutableTreeNode();
			n.setUserObject(y);
			for (X t : Util.alphaMaybe(outline_alphabetical, x)) {
				DefaultMutableTreeNode m = new DefaultMutableTreeNode();
				m.setUserObject(f.apply(t));
				if (t instanceof Exp) {
					asTree(m, (Exp<?>) t);
				} else if (t instanceof InteriorLabel) {
					InteriorLabel<?> l = (InteriorLabel<?>) t;
					if (l.s instanceof Exp) {
						asTree(m, (Exp<?>) l.s);
					}
				}
				n.add(m);
			}
			root.add(n);
		}
	}

	protected DefaultListModel<String> foo() {
		DefaultListModel<String> model = new DefaultListModel<>();

		// synchronized (parsed_prog_lock) {
		if (parsed_prog != null) {
			for (String s : Util.alphabetical(parsed_prog.keySet())) {
				if (Kind.COMMENT.toString() != parsed_prog.kind(s)) {
					model.addElement(s);
				}
			}
		}
		// }
		return model;
	}

}
