package catdata.aql.gui;

import java.awt.BorderLayout;
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

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;

import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.autocomplete.ShorthandCompletion;

import catdata.ParseException;
import catdata.Program;
import catdata.Util;
import catdata.aql.Kind;
import catdata.aql.exp.AqlDoc;
import catdata.aql.exp.AqlEnv;
import catdata.aql.exp.AqlMultiDriver;
import catdata.aql.exp.AqlParserFactory;
import catdata.aql.exp.AqlStatic;
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

		JMenuItem im = new JMenuItem("Infer Mapping");
		im.addActionListener(x -> infer(Kind.MAPPING));
		topArea.getPopupMenu().add(im, 0);
		JMenuItem iq = new JMenuItem("Infer Query");
		iq.addActionListener(x -> infer(Kind.QUERY));
		topArea.getPopupMenu().add(iq, 0);
		JMenuItem it = new JMenuItem("Infer Transform");
		it.addActionListener(x -> infer(Kind.TRANSFORM));
		topArea.getPopupMenu().add(it, 0);
		JMenuItem ii = new JMenuItem("Infer Instance");
		ii.addActionListener(x -> infer(Kind.INSTANCE));
		topArea.getPopupMenu().add(ii, 0);

		getOutline();

		JMenuItem html = new JMenuItem("Emit HTML");
		html.addActionListener(x -> emitDoc());
		topArea.getPopupMenu().add(html, 0);
		aqlStatic = new AqlStatic(topArea, getOutline().validateBox);
		topArea.addParser(aqlStatic);
		getOutline().validateBox.addActionListener(x -> {
			getOutline().oLabel.setText("");
			((AqlCodeEditor) getOutline().codeEditor).topArea.forceReparsing(aqlStatic);
		});

	}

	final AqlStatic aqlStatic;

	// DefaultParseResult result;

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
				"typeside ? = literal {\n\timports\n\ttypes\n\tsconstants\n\tfunctions\n\tequations\n\tjava_types\n\tjava_constants\n\tjava_functions\n\toptions\n} ",
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

	public void infer(Kind k) {
		try {
			Inferrer.infer(this, k);
		} catch (Throwable thr) {
			thr.printStackTrace();
			respArea.setText(thr.getMessage());
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

	protected synchronized AqlOutline getOutline() {
		if (outline == null) {
			outline = new AqlOutline(this);
		}
		return (AqlOutline) outline;
	}

}
