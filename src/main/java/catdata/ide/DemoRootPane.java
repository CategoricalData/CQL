package catdata.ide;

import javax.swing.JRootPane;

/*
import org.fife.rsta.ac.AbstractSourceTree;
import org.fife.rsta.ac.LanguageSupport;
import org.fife.rsta.ac.LanguageSupportFactory;
import org.fife.rsta.ac.java.JavaLanguageSupport;
import org.fife.rsta.ac.java.tree.JavaOutlineTree;
import org.fife.rsta.ac.js.tree.JavaScriptOutlineTree;
import org.fife.rsta.ac.xml.tree.XmlOutlineTree;
import org.fife.ui.rsyntaxtextarea.ErrorStrip;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
*/
class DemoRootPane extends JRootPane { // implements HyperlinkListener, SyntaxConstants {
  /*
   * private JScrollPane treeSP; private AbstractSourceTree tree; private
   * RSyntaxTextArea textArea;
   * 
   * public DemoRootPane() {
   * 
   * LanguageSupportFactory lsf = LanguageSupportFactory.get(); LanguageSupport
   * support = lsf.getSupportFor(SYNTAX_STYLE_JAVA); //JavaLanguageSupport jls =
   * (JavaLanguageSupport) support;
   * 
   * 
   * // Dummy tree keeps JViewport's "background" looking right initially JTree
   * dummy = new JTree((TreeNode) null); treeSP = new JScrollPane(dummy);
   * 
   * textArea = createTextArea(); //setText("CExample.txt", SYNTAX_STYLE_C);
   * RTextScrollPane scrollPane = new RTextScrollPane(textArea, true);
   * scrollPane.setIconRowHeaderEnabled(true);
   * scrollPane.getGutter().setBookmarkingEnabled(true);
   * 
   * final JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeSP,
   * scrollPane); SwingUtilities.invokeLater(() -> sp.setDividerLocation(0.25));
   * sp.setContinuousLayout(true); setContentPane(sp);
   * 
   * setJMenuBar(createMenuBar());
   * 
   * ErrorStrip errorStrip = new ErrorStrip(textArea);
   * errorStrip.setBackground(java.awt.Color.blue); JPanel cp = new JPanel(new
   * BorderLayout()); cp.add(sp); cp.add(errorStrip, BorderLayout.LINE_END);
   * setContentPane(cp); }
   * 
   * private void addItem(Action a, ButtonGroup bg, JMenu menu) {
   * JRadioButtonMenuItem item = new JRadioButtonMenuItem(a); bg.add(item);
   * menu.add(item); }
   * 
   * private JMenuBar createMenuBar() {
   * 
   * JMenuBar mb = new JMenuBar();
   * 
   * JMenu menu = new JMenu("Language"); ButtonGroup bg = new ButtonGroup();
   * addSyntaxItem("ActionScript", "ActionScriptExample.txt",
   * SYNTAX_STYLE_ACTIONSCRIPT, bg, menu); addSyntaxItem("C", "CExample.txt",
   * SYNTAX_STYLE_CPLUSPLUS, bg, menu); addSyntaxItem("CSS", "CssExample.txt",
   * SYNTAX_STYLE_CSS, bg, menu); addSyntaxItem("Dockerfile",
   * "DockerfileExample.txt", SYNTAX_STYLE_DOCKERFILE, bg, menu);
   * addSyntaxItem("Hosts", "HostsExample.txt", SYNTAX_STYLE_HOSTS, bg, menu);
   * addSyntaxItem("HTML", "HtmlExample.txt", SYNTAX_STYLE_HTML, bg, menu);
   * addSyntaxItem("INI", "IniExample.txt", SYNTAX_STYLE_INI, bg, menu);
   * addSyntaxItem("Java", "JavaExample.txt", SYNTAX_STYLE_JAVA, bg, menu);
   * addSyntaxItem("JavaScript", "JavaScriptExample.txt", SYNTAX_STYLE_JAVASCRIPT,
   * bg, menu); addSyntaxItem("JSP", "JspExample.txt", SYNTAX_STYLE_JSP, bg,
   * menu); addSyntaxItem("JSON", "JsonExample.txt",
   * SYNTAX_STYLE_JSON_WITH_COMMENTS, bg, menu); addSyntaxItem("Less",
   * "LessExample.txt", SYNTAX_STYLE_LESS, bg, menu); addSyntaxItem("Perl",
   * "PerlExample.txt", SYNTAX_STYLE_PERL, bg, menu); addSyntaxItem("PHP",
   * "PhpExample.txt", SYNTAX_STYLE_PHP, bg, menu); addSyntaxItem("Ruby",
   * "RubyExample.txt", SYNTAX_STYLE_RUBY, bg, menu); addSyntaxItem("SQL",
   * "SQLExample.txt", SYNTAX_STYLE_SQL, bg, menu); addSyntaxItem("TypeScript",
   * "TypeScriptExample.txt", SYNTAX_STYLE_TYPESCRIPT, bg, menu);
   * addSyntaxItem("XML", "XMLExample.txt", SYNTAX_STYLE_XML, bg, menu);
   * addSyntaxItem("YAML", "YamlExample.txt", SYNTAX_STYLE_YAML, bg, menu);
   * menu.getItem(2).setSelected(true); mb.add(menu);
   * 
   * 
   * menu = new JMenu("View"); menu.add(new JCheckBoxMenuItem(new
   * ToggleLayeredHighlightsAction(this))); mb.add(menu);
   * 
   * return mb;
   * 
   * }
   * 
   * class ToggleLayeredHighlightsAction extends AbstractAction {
   * 
   * private static final long serialVersionUID = 1L; private DemoRootPane demo;
   * 
   * public ToggleLayeredHighlightsAction(DemoRootPane demo) { this.demo = demo;
   * putValue(NAME, "Layered Selection Highlights"); }
   * 
   * @Override public void actionPerformed(ActionEvent e) { DefaultHighlighter h =
   * (DefaultHighlighter)demo.getTextArea(). getHighlighter();
   * h.setDrawsLayeredHighlights(!h.getDrawsLayeredHighlights()); }
   * 
   * }
   * 
   * public static void main(String[] args) { JFrame f = new JFrame(); JPanel cp =
   * new JPanel(new BorderLayout());
   * 
   * DemoRootPane u = new DemoRootPane(); // RSyntaxTextArea textArea =
   * u.textArea; //
   * textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA); //
   * textArea.setCodeFoldingEnabled(true); // RTextScrollPane sp = new
   * RTextScrollPane(textArea); cp.add(u);
   * 
   * f.setContentPane(cp); f.setTitle("Text Editor Demo");
   * f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE); f.pack();
   * f.setLocationRelativeTo(null); f.setVisible(true);
   * 
   * // Start all Swing applications on the EDT. SwingUtilities.invokeLater(() ->
   * { new UIExperiments().setVisible(true); });
   * 
   * }
   * 
   * public class ChangeSyntaxStyleAction extends AbstractAction {
   * 
   * private static final long serialVersionUID = 1L; private String res; private
   * String style;
   * 
   * ChangeSyntaxStyleAction(String name, String res, String style) {
   * putValue(NAME, name); this.res = res; this.style = style; }
   * 
   * @Override public void actionPerformed(ActionEvent e) { setText(res, style);
   * textArea.setCaretPosition(0); textArea.setSyntaxEditingStyle(style); }
   * 
   * } private void addSyntaxItem(String name, String res, String style,
   * ButtonGroup bg, JMenu menu) { JRadioButtonMenuItem item = new
   * JRadioButtonMenuItem(new ChangeSyntaxStyleAction(name, res, style));
   * bg.add(item); menu.add(item); }
   * 
   * 
   * 
   * private RSyntaxTextArea createTextArea() { RSyntaxTextArea textArea = new
   * RSyntaxTextArea(25, 80); LanguageSupportFactory.get().register(textArea);
   * textArea.setCaretPosition(0); textArea.addHyperlinkListener(this);
   * textArea.requestFocusInWindow(); textArea.setMarkOccurrences(true);
   * textArea.setCodeFoldingEnabled(true); textArea.setTabsEmulated(true);
   * textArea.setTabSize(3); //textArea.setBackground(new java.awt.Color(224, 255,
   * 224)); //textArea.setUseSelectedTextColor(true);
   * //textArea.setLineWrap(true);
   * ToolTipManager.sharedInstance().registerComponent(textArea); return textArea;
   * }
   * 
   * void focusTextArea() { textArea.requestFocusInWindow(); }
   * 
   * RSyntaxTextArea getTextArea() { return textArea; }
   * 
   * @Override public void hyperlinkUpdate(HyperlinkEvent e) { if
   * (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) { URL url =
   * e.getURL(); if (url == null) {
   * UIManager.getLookAndFeel().provideErrorFeedback(null); } else {
   * JOptionPane.showMessageDialog(this, "URL clicked:\n" + url.toString()); } } }
   * 
   * public void openFile(File file) { try { BufferedReader r = new
   * BufferedReader(new FileReader(file)); textArea.read(r, null);
   * textArea.setCaretPosition(0); r.close(); } catch (IOException ioe) {
   * ioe.printStackTrace(); UIManager.getLookAndFeel().provideErrorFeedback(this);
   * return; } }
   * 
   * private void refreshSourceTree() {
   * 
   * if (tree != null) { tree.uninstall(); }
   * 
   * String language = textArea.getSyntaxEditingStyle(); if
   * (SyntaxConstants.SYNTAX_STYLE_JAVA.equals(language)) { tree = new
   * JavaOutlineTree(); } else if
   * (SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT.equals(language)) { tree = new
   * JavaScriptOutlineTree(); } else if
   * (SyntaxConstants.SYNTAX_STYLE_XML.equals(language)) { tree = new
   * XmlOutlineTree(); } else { tree = null; }
   * 
   * if (tree != null) { tree.listenTo(textArea); treeSP.setViewportView(tree); }
   * else { JTree dummy = new JTree((TreeNode) null);
   * treeSP.setViewportView(dummy); } treeSP.revalidate();
   * 
   * }
   * 
   * void setText(String resource, String style) {
   * 
   * textArea.setSyntaxEditingStyle(style);
   * 
   * ClassLoader cl = getClass().getClassLoader(); BufferedReader r; try {
   * 
   * r = new BufferedReader( new
   * InputStreamReader(cl.getResourceAsStream("examples/" + resource),
   * StandardCharsets.UTF_8)); textArea.read(r, null); r.close();
   * textArea.setCaretPosition(0); textArea.discardAllEdits();
   * 
   * refreshSourceTree();
   * 
   * } catch (RuntimeException re) { throw re; // FindBugs } catch (Exception e) {
   * textArea.setText("Type here to see syntax highlighting"); }
   * 
   * }
   */
}
