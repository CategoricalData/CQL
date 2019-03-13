package catdata.ide;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.ButtonGroup;
import javax.swing.InputMap;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.fife.ui.rsyntaxtextarea.ErrorStrip;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextAreaEditorKit;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.RTextScrollPane;

import catdata.ide.GuiUtil.MissingIcon;

@SuppressWarnings("serial")
public class UIExperiments extends JRootPane implements HyperlinkListener, SyntaxConstants {

	private RTextScrollPane scrollPane;
	private RSyntaxTextArea textArea;

	public static void main(String[] args) {
		JFrame f = new JFrame();
		JPanel cp = new JPanel(new BorderLayout());

		UIExperiments u = new UIExperiments();
//		      RSyntaxTextArea textArea = u.textArea;
//		      textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
//		      textArea.setCodeFoldingEnabled(true);
		// RTextScrollPane sp = new RTextScrollPane(textArea);
		cp.add(u);

		f.setContentPane(cp);
		f.setTitle("Text Editor Demo");
		f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		f.pack();
		f.setLocationRelativeTo(null);
		f.setVisible(true);

		// Start all Swing applications on the EDT.
		SwingUtilities.invokeLater(() -> {
			new UIExperiments().setVisible(true);
		});

	}

	
	public UIExperiments() {
		textArea = createTextArea();
		setText("whasabi");
		textArea.setSyntaxEditingStyle(SYNTAX_STYLE_JAVA);

		scrollPane = new RTextScrollPane(textArea, true);
		Gutter gutter = scrollPane.getGutter();
		gutter.setBookmarkingEnabled(true);
		 
		 gutter.setBookmarkIcon(new MissingIcon(Color.BLACK, 14, 14));
		getContentPane().add(scrollPane);
		ErrorStrip errorStrip = new ErrorStrip(textArea);
errorStrip.setBackground(java.awt.Color.blue);
		getContentPane().add(errorStrip, BorderLayout.LINE_END);
		setJMenuBar(createMenuBar());
	}

	private void addSyntaxItem(String name, String res, String style, ButtonGroup bg, JMenu menu) {
		JRadioButtonMenuItem item = new JRadioButtonMenuItem(new ChangeSyntaxStyleAction(name, res, style));
		bg.add(item);
		menu.add(item);
	}

	

	private static Action createCopyAsStyledTextAction(String themeName) throws IOException {
		String resource = "/org/fife/ui/rsyntaxtextarea/themes/" + themeName + ".xml";
		Theme theme = Theme.load(UIExperiments.class.getResourceAsStream(resource));
		return new RSyntaxTextAreaEditorKit.CopyAsStyledTextAction(themeName, theme);
	}

	private JMenuBar createMenuBar() {

		JMenuBar mb = new JMenuBar();

		JMenu menu = new JMenu("Language");
		ButtonGroup bg = new ButtonGroup();
		addSyntaxItem("ActionScript", "ActionScriptExample.txt", SYNTAX_STYLE_ACTIONSCRIPT, bg, menu);
		addSyntaxItem("C", "CExample.txt", SYNTAX_STYLE_CPLUSPLUS, bg, menu);
		addSyntaxItem("CSS", "CssExample.txt", SYNTAX_STYLE_CSS, bg, menu);
		addSyntaxItem("Dockerfile", "DockerfileExample.txt", SYNTAX_STYLE_DOCKERFILE, bg, menu);
		addSyntaxItem("Hosts", "HostsExample.txt", SYNTAX_STYLE_HOSTS, bg, menu);
		addSyntaxItem("HTML", "HtmlExample.txt", SYNTAX_STYLE_HTML, bg, menu);
		addSyntaxItem("INI", "IniExample.txt", SYNTAX_STYLE_INI, bg, menu);
		addSyntaxItem("Java", "JavaExample.txt", SYNTAX_STYLE_JAVA, bg, menu);
		addSyntaxItem("JavaScript", "JavaScriptExample.txt", SYNTAX_STYLE_JAVASCRIPT, bg, menu);
		addSyntaxItem("JSP", "JspExample.txt", SYNTAX_STYLE_JSP, bg, menu);
		addSyntaxItem("JSON", "JsonExample.txt", SYNTAX_STYLE_JSON_WITH_COMMENTS, bg, menu);
		addSyntaxItem("Less", "LessExample.txt", SYNTAX_STYLE_LESS, bg, menu);
		addSyntaxItem("Perl", "PerlExample.txt", SYNTAX_STYLE_PERL, bg, menu);
		addSyntaxItem("PHP", "PhpExample.txt", SYNTAX_STYLE_PHP, bg, menu);
		addSyntaxItem("Ruby", "RubyExample.txt", SYNTAX_STYLE_RUBY, bg, menu);
		addSyntaxItem("SQL", "SQLExample.txt", SYNTAX_STYLE_SQL, bg, menu);
		addSyntaxItem("TypeScript", "TypeScriptExample.txt", SYNTAX_STYLE_TYPESCRIPT, bg, menu);
		addSyntaxItem("XML", "XMLExample.txt", SYNTAX_STYLE_XML, bg, menu);
		addSyntaxItem("YAML", "YamlExample.txt", SYNTAX_STYLE_YAML, bg, menu);
		menu.getItem(2).setSelected(true);
		mb.add(menu);

		menu = new JMenu("View");
		JCheckBoxMenuItem cbItem = new JCheckBoxMenuItem(new CodeFoldingAction());
		cbItem.setSelected(true);
		menu.add(cbItem);
		cbItem = new JCheckBoxMenuItem(new ViewLineHighlightAction());
		cbItem.setSelected(true);
		menu.add(cbItem);
		cbItem = new JCheckBoxMenuItem(new ViewLineNumbersAction());
		cbItem.setSelected(true);
		menu.add(cbItem);
		cbItem = new JCheckBoxMenuItem(new AnimateBracketMatchingAction());
		cbItem.setSelected(true);
		menu.add(cbItem);
		cbItem = new JCheckBoxMenuItem(new BookmarksAction());
		cbItem.setSelected(true);
		menu.add(cbItem);
		cbItem = new JCheckBoxMenuItem(new WordWrapAction());
		menu.add(cbItem);
		cbItem = new JCheckBoxMenuItem(new ToggleAntiAliasingAction());
		cbItem.setSelected(true);
		menu.add(cbItem);
		cbItem = new JCheckBoxMenuItem(new MarkOccurrencesAction());
		cbItem.setSelected(true);
		menu.add(cbItem);
		cbItem = new JCheckBoxMenuItem(new TabLinesAction());
		menu.add(cbItem);
		mb.add(menu);

	
	

		return mb;

	}

	/**
	 * Creates the text area for this application.
	 *
	 * @return The text area.
	 */
	private RSyntaxTextArea createTextArea() {

		RSyntaxTextArea textArea = new RSyntaxTextArea(25, 70);
		textArea.setTabSize(3);
		textArea.setCaretPosition(0);
		textArea.addHyperlinkListener(this);
		textArea.requestFocusInWindow();
		textArea.setMarkOccurrences(true);
		textArea.setCodeFoldingEnabled(true);
		textArea.setClearWhitespaceLinesEnabled(false);

		InputMap im = textArea.getInputMap();
		ActionMap am = textArea.getActionMap();
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0), "decreaseFontSize");
		am.put("decreaseFontSize", new RSyntaxTextAreaEditorKit.DecreaseFontSizeAction());
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0), "increaseFontSize");
		am.put("increaseFontSize", new RSyntaxTextAreaEditorKit.IncreaseFontSizeAction());

		int ctrlShift = InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK;
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, ctrlShift), "copyAsStyledText");
		am.put("copyAsStyledText", new RSyntaxTextAreaEditorKit.CopyAsStyledTextAction());

		try {

			im.put(KeyStroke.getKeyStroke(KeyEvent.VK_M, ctrlShift), "copyAsStyledTextMonokai");
			am.put("copyAsStyledTextMonokai", createCopyAsStyledTextAction("monokai"));

			im.put(KeyStroke.getKeyStroke(KeyEvent.VK_E, ctrlShift), "copyAsStyledTextEclipse");
			am.put("copyAsStyledTextEclipse", createCopyAsStyledTextAction("eclipse"));
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

		return textArea;
	}

	/**
	 * Focuses the text area.
	 */
	void focusTextArea() {
		textArea.requestFocusInWindow();
	}

	/**
	 * Called when a hyperlink is clicked in the text area.
	 *
	 * @param e The event.
	 */
	@Override
	public void hyperlinkUpdate(HyperlinkEvent e) {
		if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
			URL url = e.getURL();
			if (url == null) {
				UIManager.getLookAndFeel().provideErrorFeedback(null);
			} else {
				JOptionPane.showMessageDialog(this, "URL clicked:\n" + url.toString());
			}
		}
	}

	private void setText(String x) {
		textArea.setText(x);
		textArea.setCaretPosition(0);
		textArea.discardAllEdits();
	}

	
	/**
	 * Toggles whether matched brackets are animated.
	 */
	private class AnimateBracketMatchingAction extends AbstractAction {

		AnimateBracketMatchingAction() {
			putValue(NAME, "Animate Bracket Matching");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			textArea.setAnimateBracketMatching(!textArea.getAnimateBracketMatching());
		}

	}

	/**
	 * Toggles whether bookmarks are enabled.
	 */
	private class BookmarksAction extends AbstractAction {

		BookmarksAction() {
			putValue(NAME, "Bookmarks");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			scrollPane.setIconRowHeaderEnabled(!scrollPane.isIconRowHeaderEnabled());
		}

	}

	/**
	 * Changes the syntax style to a new value.
	 */
	public class ChangeSyntaxStyleAction extends AbstractAction {

		private String res;
		private String style;

		ChangeSyntaxStyleAction(String name, String res, String style) {
			putValue(NAME, name);
			this.res = res;
			this.style = style;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			setText(res);
			textArea.setCaretPosition(0);
			textArea.setSyntaxEditingStyle(style);
		}

	}

	/**
	 * Toggles whether code folding is enabled.
	 */
	private class CodeFoldingAction extends AbstractAction {

		CodeFoldingAction() {
			putValue(NAME, "Code Folding");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			textArea.setCodeFoldingEnabled(!textArea.isCodeFoldingEnabled());
		}

	}

	/**
	 * Toggles whether "mark occurrences" is enabled.
	 */
	private class MarkOccurrencesAction extends AbstractAction {

		MarkOccurrencesAction() {
			putValue(NAME, "Mark Occurrences");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			textArea.setMarkOccurrences(!textArea.getMarkOccurrences());
		}

	}

	/**
	 * Toggles whether "tab lines" are enabled.
	 */
	private class TabLinesAction extends AbstractAction {

		private boolean selected;

		TabLinesAction() {
			putValue(NAME, "Tab Lines");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			selected = !selected;
			textArea.setPaintTabLines(selected);
		}

	}

	
	/**
	 * Toggles anti-aliasing.
	 */
	private class ToggleAntiAliasingAction extends AbstractAction {

		ToggleAntiAliasingAction() {
			putValue(NAME, "Anti-Aliasing");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			textArea.setAntiAliasingEnabled(!textArea.getAntiAliasingEnabled());
		}

	}

	/**
	 * Toggles whether the current line is highlighted.
	 */
	private class ViewLineHighlightAction extends AbstractAction {

		ViewLineHighlightAction() {
			putValue(NAME, "Current Line Highlight");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			textArea.setHighlightCurrentLine(!textArea.getHighlightCurrentLine());
		}

	}

	/**
	 * Toggles line number visibility.
	 */
	private class ViewLineNumbersAction extends AbstractAction {

		ViewLineNumbersAction() {
			putValue(NAME, "Line Numbers");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			scrollPane.setLineNumbersEnabled(!scrollPane.getLineNumbersEnabled());
		}

	}

	/**
	 * Toggles word wrap.
	 */
	private class WordWrapAction extends AbstractAction {

		WordWrapAction() {
			putValue(NAME, "Word Wrap");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			textArea.setLineWrap(!textArea.getLineWrap());
		}

	}

}