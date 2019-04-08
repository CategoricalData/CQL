package catdata.ide;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.apache.commons.collections4.list.TreeList;
import org.fife.rsta.ui.GoToDialog;
import org.fife.rsta.ui.search.FindDialog;
import org.fife.rsta.ui.search.ReplaceDialog;
import org.fife.rsta.ui.search.SearchEvent;
import org.fife.rsta.ui.search.SearchListener;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.ErrorStrip;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.folding.CurlyFoldParser;
import org.fife.ui.rsyntaxtextarea.folding.Fold;
import org.fife.ui.rsyntaxtextarea.folding.FoldParserManager;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.fife.ui.rtextarea.SearchResult;
//import org.jparsec.error.ParserException;
import org.jparsec.error.ParserException;

import catdata.InteriorLabel;
import catdata.LineException;
import catdata.LocException;
import catdata.ParseException;
import catdata.Prog;
import catdata.Unit;
import catdata.Util;

/**
 * 
 * @author ryan
 * 
 *         The FQL code editor
 */
public abstract class CodeEditor<Progg extends Prog, Env, DDisp extends Disp> extends JPanel
		implements SearchListener, Runnable {

	public String getClickedWord() {
		String content = topArea.getText();
		int caretPosition = topArea.getCaretPosition();
		try {
			if (content.length() == 0) {
				return "";
			}
			// replace non breaking character with space
			content = content.replace(String.valueOf((char) 160), " ");
			int selectionStart = content.lastIndexOf(" ", caretPosition - 1);
			if (selectionStart == -1) {
				selectionStart = 0;
			} else {
				// ignore space character
				selectionStart += 1;
			}
			content = content.substring(selectionStart);
			int i = 0;
			String temp;
			int length = content.length();
			while (i != length && !(temp = content.substring(i, i + 1)).equals(" ") && !temp.equals("\n")) {
				i++;
			}
			content = content.substring(0, i);
			// int selectionEnd = content.length() + selectionStart;
			return content;
		} catch (StringIndexOutOfBoundsException e) {
			return "";
		}

	}

	public void showGotoDialog2() {
		String selected = getClickedWord().trim();
		// synchronized (parsed_prog_lock) {
		if (parsed_prog != null) {
			int line = parsed_prog.getLine(selected);
			if (line != -1) {
				setCaretPos(line);
			} else {
				showGotoDialog();
			}
		}
		// }
	}

	public void showGotoDialog() {
		JPanel panel = new JPanel(new BorderLayout());

		JList<String> list = GuiUtil.makeList();

		DefaultListModel<String> model = foo();

		list.setModel(model);

		JTextField field = new JTextField();

		// field.setBorder(BorderFactory.createTitledBorder("Goto definition:"));

		panel.add(field, BorderLayout.NORTH);
		panel.add(new JScrollPane(list), BorderLayout.CENTER);

		JDialog dialog = new JDialog((Dialog) null, "Goto Definition:", true);

		JPanel bot = new JPanel(new GridLayout(1, 4));
		JButton ok = new JButton("Goto");
		JButton cancel = new JButton("Cancel");
		bot.add(new JLabel(""));
		bot.add(new JLabel(""));
		bot.add(ok);
		bot.add(cancel);

		Boolean[] canceled = new Boolean[1];
		canceled[0] = false;
		cancel.addActionListener(x -> {
			canceled[0] = true;
			dialog.setVisible(false);
		});
		ok.addActionListener(x -> dialog.setVisible(false));

		panel.add(bot, BorderLayout.SOUTH);

		KeyAdapter adapter = new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					dialog.setVisible(false);
				} else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					list.clearSelection();
					dialog.setVisible(false);
				}
			}

		};

		field.addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent e) {
				SwingUtilities.invokeLater(() -> {
					DefaultListModel<String> model = foo();
					list.setModel(model);
				});
			}
		});

		field.addKeyListener(adapter);
		list.addKeyListener(adapter);
		dialog.addKeyListener(adapter);
		panel.addKeyListener(adapter);
		bot.addKeyListener(adapter);

		list.addMouseListener(new MouseAdapter() {

			@Override
			public void mousePressed(MouseEvent me) {
				if (me.getClickCount() != 2) {
					return;
				}
				dialog.setVisible(false);
			}
		});

		dialog.setContentPane(panel);
		dialog.setSize(new Dimension(300, 600));
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);

		if (canceled[0]) {
			return;
		}

		String selected = list.getSelectedValue();
		if (selected == null) {
			if (list.getModel().getSize() == 1) {
				selected = list.getModel().getElementAt(0);
			} else if (!field.getText().isEmpty()) {
				selected = field.getText();
			} else {
				return;
			}
		}

		// synchronized (parsed_prog_lock) {
		if (parsed_prog != null) {
			Integer line = parsed_prog.getLine(selected);
			if (line != null) {
				setCaretPos(line);
			}
		}
		// }
	}

	public void copyAsRtf() {
		topArea.copyAsStyledText();
	}

	public abstract Language lang();

	final Integer id;
	protected volatile String title;

	private DDisp display;

	private static final long serialVersionUID = 1L;

	public final RSyntaxTextArea topArea;

	protected final CodeTextPanel respArea = new CodeTextPanel("", "");

	private FindDialog findDialog;
	private ReplaceDialog replaceDialog;

	private void initSearchDialogs() {
		findDialog = new FindDialog((Dialog) null, this);
		replaceDialog = new ReplaceDialog((Dialog) null, this);

		// This ties the properties of the two dialogs together (match case,
		SearchContext context = findDialog.getSearchContext();
		replaceDialog.setSearchContext(context);

		tweak(findDialog); // indDialog.get
		tweak(replaceDialog);
	}

	private void tweak(JDialog frame) {
		ActionListener escListener = (ActionEvent e) -> frame.setVisible(false);

		frame.getRootPane().registerKeyboardAction(escListener, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
				JComponent.WHEN_IN_FOCUSED_WINDOW);
		KeyStroke ctrlW = KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK);
		KeyStroke commandW = KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.META_DOWN_MASK);
		frame.getRootPane().registerKeyboardAction(escListener, ctrlW, JComponent.WHEN_IN_FOCUSED_WINDOW);
		frame.getRootPane().registerKeyboardAction(escListener, commandW, JComponent.WHEN_IN_FOCUSED_WINDOW);
	}

	public void setText(String s) {
		SwingUtilities.invokeLater(() -> {
			topArea.setText(s);
			setCaretPos(0);
		});
	}

	public String getText() {
		return topArea.getText();
	}

	protected abstract String getATMFlhs();

	protected abstract String getATMFrhs();

	protected abstract void doTemplates();

	protected final RTextScrollPane sp;

	/*
	 * protected synchronized Outline<Progg, Env, DDisp> getOutline() { if (outline
	 * == null) { outline = new ListOutline<>(this, parsed_prog); } return outline;
	 * }
	 */

	volatile List<Integer> history = Collections.synchronizedList(new TreeList<>());
	int position = 0;

	private void hist() {
		if (position < 0) {
			position = 0;
		} else if (position >= history.size()) {
			position = history.size() - 1;
		}
		setCaretPos(history.get(position));
	}

	public void backAction() {
		position++;
		hist();
	}

	public void fwdAction() {
		position--;
		hist();
	}

	ErrorStrip errorStrip;

	protected CodeEditor(String title, Integer id, String content, LayoutManager l) {
		super(l);
		this.id = id;
		this.title = title;
		p = new JPanel(new BorderLayout());
		Util.assertNotNull(id);
		history.add(0);
		last_keystroke = System.currentTimeMillis();
		// respArea.setWordWrap(true);
		AbstractTokenMakerFactory atmf = (AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance();

		atmf.putMapping(getATMFlhs(), getATMFrhs());
		FoldParserManager.get().addFoldParserMapping(getATMFlhs(), new CurlyFoldParser());

		startThread();

		topArea = new RSyntaxTextArea();
		errorStrip = new ErrorStrip(topArea);
		errorStrip.setShowMarkedOccurrences(false);
		errorStrip.setShowMarkAll(true);

		topArea.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				int i = topArea.getCaretPosition();
				addToHistory(i);
			}

		});

		topArea.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				last_keystroke = System.currentTimeMillis();
			}
		});

		if (getATMFrhs() != null) {
			topArea.setSyntaxEditingStyle(getATMFlhs());
		}
		topArea.setText(content);
		setCaretPos(0);
		topArea.setAutoscrolls(true);

		InputMap inputMap = topArea.getInputMap();

		topArea.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {
			}

			@Override
			public void keyPressed(KeyEvent e) {
			}

			@Override
			public void keyReleased(KeyEvent e) {
				if (KeyEvent.VK_DOWN == e.getKeyCode() && isCaretOnLastLine()) {
					setCaretPos(Integer.max(0, topArea.getText().length()));
				} else if (KeyEvent.VK_UP == e.getKeyCode() && isCaretOnFirstLine()) {
					setCaretPos(0);
				}
			}

			private boolean isCaretOnFirstLine() {
				return topArea.getCaretLineNumber() == 0;
			}

			private boolean isCaretOnLastLine() {
				return topArea.getCaretLineNumber() == Integer.max(0, topArea.getLineCount() - 1);
			}

		});

		KeyStroke key2;
		key2 = System.getProperty("os.name").contains("Windows")
				? KeyStroke.getKeyStroke(KeyEvent.VK_A, Event.META_MASK)
				: KeyStroke.getKeyStroke(KeyEvent.VK_A, Event.CTRL_MASK);
		inputMap.put(key2, DefaultEditorKit.beginLineAction);

		KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_E, Event.META_MASK);
		key2 = KeyStroke.getKeyStroke(KeyEvent.VK_E, Event.CTRL_MASK);

		inputMap.put(key, DefaultEditorKit.endLineAction);
		inputMap.put(key2, DefaultEditorKit.endLineAction);

		Action alx = new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				topArea.redoLastAction();
			}
		};

		if (System.getProperty("os.name").contains("Windows")) {
			key = KeyStroke.getKeyStroke(KeyEvent.VK_Z, Event.SHIFT_MASK | Event.CTRL_MASK);
			inputMap.put(key, alx);
		} else {
			key = KeyStroke.getKeyStroke(KeyEvent.VK_Z, Event.SHIFT_MASK | Event.META_MASK);
			inputMap.put(key, alx);
		}

		key = KeyStroke.getKeyStroke(KeyEvent.VK_K, Event.META_MASK);
		key2 = KeyStroke.getKeyStroke(KeyEvent.VK_K, Event.CTRL_MASK);

		Action al = new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				int len = topArea.getLineEndOffsetOfCurrentLine();
				int offs = topArea.getCaretPosition();
				try {
					if (len - offs - 1 > 0) {
						topArea.getDocument().remove(offs, len - offs - 1);
					}
				} catch (BadLocationException e1) {
					e1.printStackTrace();
				}
			}
		};
		topArea.getActionMap().put("RemoveToEndOfLine", al);
		inputMap.put(key, "RemoveToEndOfLine");
		inputMap.put(key2, "RemoveToEndOfLine");

		topArea.setCloseCurlyBraces(true);
		topArea.setCodeFoldingEnabled(true);
		sp = new RTextScrollPane(topArea);
		sp.setFoldIndicatorEnabled(true);

		// sp.getGutter().set
		sp.setBorder(BorderFactory.createEmptyBorder());
		sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		sp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

		topArea.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				GUI.setDirty(CodeEditor.this.id, true);
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				GUI.setDirty(CodeEditor.this.id, true);
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				GUI.setDirty(CodeEditor.this.id, true);
			}
		});

		key = KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, Event.META_MASK);
		key2 = KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, Event.CTRL_MASK);

		al = new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				IdeOptions.theCurrentOptions.fontSizeUp();
			}
		};
		topArea.getActionMap().put("IncreaseFont", al);
		inputMap.put(key, "IncreaseFont");
		inputMap.put(key2, "IncreaseFont");

		key = KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, Event.META_MASK);
		key2 = KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, Event.CTRL_MASK);

		al = new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				IdeOptions.theCurrentOptions.fontSizeDown();
			}
		};
		topArea.getActionMap().put("DecreaseFont", al);
		inputMap.put(key, "DecreaseFont");
		inputMap.put(key2, "DecreaseFont");

		doTemplates();

		JMenuItem rtf = new JMenuItem("Copy as RTF");
		rtf.addActionListener(x -> topArea.copyAsStyledText());
		topArea.getPopupMenu().add(rtf, 0);

		JMenuItem foldall = new JMenuItem("Fold All");
		foldall.addActionListener(x -> foldAll(true));
		topArea.getPopupMenu().add(foldall, 0);

		JMenuItem unfoldall = new JMenuItem("UnFold All");
		unfoldall.addActionListener(x -> foldAll(false));
		topArea.getPopupMenu().add(unfoldall, 0);

		JMenuItem gotox = new JMenuItem("Goto Definition");
		gotox.addActionListener(x -> showGotoDialog2());
		topArea.getPopupMenu().add(gotox, 0);

		respArea.setMinimumSize(new Dimension(0, 0));
		respArea.setPreferredSize(new Dimension(600, 200));
		topArea.setOpaque(true);
		respArea.setOpaque(true);

		IdeOptions.theCurrentOptions.apply(this);

		initSearchDialogs();

		last_keystroke = System.currentTimeMillis();

		jsp = new JScrollPane(getComp());
		jsp.setBorder(BorderFactory.createEmptyBorder());

		JPanel q = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		q.add(validateBox, gbc);
		gbc.weightx = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		q.add(oLabel, gbc);
		JCheckBox alphaBox = new JCheckBox("Sort");
		alphaBox.addActionListener(x -> {
			outline_alphabetical(alphaBox.isSelected());
		});
		alphaBox.setHorizontalTextPosition(SwingConstants.LEFT);
		alphaBox.setHorizontalAlignment(SwingConstants.RIGHT);
		validateBox.setHorizontalTextPosition(SwingConstants.LEFT);
		validateBox.setHorizontalAlignment(SwingConstants.LEFT);
		validateBox.addActionListener(x -> {
			outline_alphabetical(alphaBox.isSelected()); //just triggers
		});

		gbc.weightx = 1;
		gbc.anchor = GridBagConstraints.EAST;
		q.add(alphaBox, gbc);
		p.add(q, BorderLayout.NORTH);
		p.add(jsp, BorderLayout.CENTER);
		p.setBorder(BorderFactory.createEtchedBorder());

		// build();

		p.setMinimumSize(new Dimension(0, 0));

	}

	public synchronized void addToHistory(int i) {
		history.add(0, i);
		if (history.size() > 128) {
			history = new LinkedList<>(history.subList(0, 32));
		}
	}

	protected void situate() {
		if (outline_elongated) {
			situateElongated();
		} else {
			situateNotElongated();
		}
	}

	private void situateElongated() {

		JSplitPane xx1 = new Split(.8, JSplitPane.VERTICAL_SPLIT);
		xx1.setDividerSize(6);
		// xx1.setResizeWeight(.8);
		JPanel cp = new JPanel(new BorderLayout());
		cp.add(sp);
		cp.add(errorStrip, BorderLayout.LINE_END);
		cp.setBorder(BorderFactory.createEtchedBorder());
		xx1.add(cp);
		xx1.add(respArea);
		xx1.setBorder(BorderFactory.createEmptyBorder());
		JComponent newtop = xx1;

		if (enable_outline) {
			JSplitPane xx2 = new Split(.8, JSplitPane.HORIZONTAL_SPLIT);
			xx2.setDividerSize(6);
			// xx1.setForeground(xx2.getForeground());
			if (outline_on_left) {
				// xx2.setResizeWeight(.2);
				xx2.add(p);
				xx2.add(xx1);
			} else {
				// xx2.setResizeWeight(.8);
				xx2.add(xx1);
				xx2.add(p);
			}
			xx2.setBorder(BorderFactory.createEmptyBorder());
			newtop = xx2;
		}

		this.removeAll();
		add(newtop);
		// revalidate();
	}

	// public final JComponent outline;

	private void situateNotElongated() {
		JPanel cp = new JPanel(new BorderLayout());
		cp.add(sp);
		cp.add(errorStrip, BorderLayout.LINE_END);
		cp.setBorder(BorderFactory.createEtchedBorder());

		JComponent newtop = cp;

		if (enable_outline) {
			JSplitPane xx2 = new Split(.8, JSplitPane.HORIZONTAL_SPLIT);
			xx2.setDividerSize(6);

			if (outline_on_left) {
				xx2.setResizeWeight(.2);
				xx2.add(p);
				xx2.add(cp);
			} else {
				xx2.setResizeWeight(.8);
				xx2.add(cp);
				xx2.add(p);
			}
			xx2.setBorder(BorderFactory.createEmptyBorder());
			newtop = xx2;
		}

		JSplitPane xx1 = new Split(.8, JSplitPane.VERTICAL_SPLIT);
		xx1.setDividerSize(6);
		xx1.setResizeWeight(.8);
		xx1.add(newtop);
		xx1.add(respArea);
		xx1.setBorder(BorderFactory.createEmptyBorder());
		// newtop.setBackground(xx1.getBackground());

		respArea.setMinimumSize(new Dimension(0, 0));

		this.removeAll();
		add(xx1);
		revalidate();
	}

	// private final SpellChecker spc;

	public void foldAll(boolean b) {
		int i = topArea.getFoldManager().getFoldCount();
		for (int j = 0; j < i; j++) {
			Fold fold = topArea.getFoldManager().getFold(j);
			fold.setCollapsed(b);
		}
		topArea.revalidate();
		topArea.repaint();
		sp.revalidate();
		sp.repaint();
		setCaretPos(topArea.getCaretPosition());
		topArea.getFoldManager().reparse();
	}

	public void setFontSize(int size) {
		if (size < 1) {
			return;
		}
		Font font = new Font(topArea.getFont().getFontName(), topArea.getFont().getStyle(), size);
		topArea.setFont(font);

		Font font2 = new Font(respArea.area.getFont().getFontName(), respArea.area.getFont().getStyle(), size + 1);
		respArea.area.setFont(font2);
	}

	void gotoLine() {
		if (findDialog.isVisible()) {
			findDialog.setVisible(false);
		}
		if (replaceDialog.isVisible()) {
			replaceDialog.setVisible(false);
		}
		GoToDialog dialog = new GoToDialog((Frame) null);
		dialog.setMaxLineNumberAllowed(topArea.getLineCount());
		dialog.setVisible(true);
		int line = dialog.getLineNumber();
		if (line > 0) {
			try {
				setCaretPos(topArea.getLineStartOffset(line - 1));
			} catch (BadLocationException ble) { // Never happens
				UIManager.getLookAndFeel().provideErrorFeedback(topArea);
				ble.printStackTrace();
			}
		}
	}

	void replaceAction() {
		if (findDialog.isVisible()) {
			findDialog.setVisible(false);

		}

		replaceDialog.setVisible(true);
	}

	// TODO -> private
	protected void doExample(Example e) {
		if (abortBecauseDirty()) {
			return;
		}
		String program = e.getText();
		topArea.setText(program);
		setCaretPos(0);
		respArea.setText("");
		GUI.setDirty(id, false);
		if (display != null) {
			display.close();
		}
		display = null;
		// topArea.for
	}

	public void abortAction() {
		interruptAndNullify();
		respArea.setText("Aborted");
	}

	protected final String[] toUpdate = new String[] { null };
	protected volatile String toDisplay = null;
	private Thread thread, temp;

	public void runAction() {
		if (started) {
			return;
		}
//		started = true;
		// toDisplay = null;
		interruptAndNullify();
		respArea.setText("Begin\n");
		toDisplay = null;
		thread = new Thread(this);
		temp = new Thread(() -> {
			try {
				int count = 0;
				while (!Thread.currentThread().isInterrupted()) {
					if (toDisplay != null) {
						respArea.setText(toDisplay);
						return;
					} else if (thread != null) {
						synchronized (toUpdate) {
							if (toUpdate[0] != null) {
								if ((count % 8) == 0) {
									respArea.setText(toUpdate[0] + "\n");
								} else {
									respArea.setText(respArea.getText() + ".");
								}
							} else {
								if (respArea.getText().length() > 1024 * 16) {
									respArea.setText("");
								}
								respArea.setText(respArea.getText() + ".");
							}
						}
					}
					count++;
					Thread.sleep(250);

				}
			} catch (InterruptedException ie) {
			} catch (Exception tt) {
				tt.printStackTrace();
				respArea.setText(tt.getMessage());
			}
		});
		temp.setPriority(Thread.MIN_PRIORITY);
		temp.start();
		thread.setPriority(Thread.MIN_PRIORITY);
		thread.start();
	}

	private volatile boolean started = false;

	@Override
	public void run() {
		if (started) {
			return;
		}
		started = true;
		String program = topArea.getText();

		Progg init;
		Env env;
		init = tryParse(program);
		if (init == null) {
			started = false;
			return;
		}

		long start;
		long middle;

		try {
			start = System.currentTimeMillis();
			env = makeEnv(program, init);
			middle = System.currentTimeMillis();

			toDisplay = "Computation finished, creating viewer... (max " + init.timeout() + " seconds)";
			DateFormat format = DateFormat.getTimeInstance();
			String foo2 = title;
			foo2 += " - " + format.format(new Date(start));
			foo2 += " - " + format.format(new Date(start));
			String foo = foo2;
			long t = init.timeout() * 1000;
			display = Util.timeout(() -> makeDisplay(foo, init, env, start, middle), t);

			if (display.exn() == null) {
				toDisplay = textFor(env); // "Done";
				respArea.setText(textFor(env)); // "Done");
			} else {
				started = false;
				throw display.exn();
			}
		} catch (LineException e) {
			toDisplay = "Error in " + e.kind + " " + e.decl + ": " + e.getLocalizedMessage();
			respArea.setText(toDisplay);
			e.printStackTrace();
			Integer theLine = init.getLine(e.decl);
			setCaretPos(theLine);
		} catch (LocException e) {
			toDisplay = "Error: " + e.getLocalizedMessage();
			respArea.setText(toDisplay);
			e.printStackTrace();
			setCaretPos(e.loc);
		} catch (Throwable re) {
			toDisplay = "Error: " + re.getLocalizedMessage();
			respArea.setText(toDisplay);
			re.printStackTrace();
		}
		started = false;
		interruptAndNullify();
	}

	private void interruptAndNullify() {
		if (thread != null) {
			thread.interrupt();
		}
		thread = null;
		if (temp != null) {
			temp.interrupt();
		}
		temp = null;
	}

	protected abstract String textFor(Env env);

	protected abstract DDisp makeDisplay(String foo, Progg init, Env env, long start, long middle);

	protected abstract Env makeEnv(String program, Progg init);

	public abstract Progg parse(String program) throws ParseException;

	public void setCaretPos(int p) {
		try {
			SwingUtilities.invokeLater(() -> {
				topArea.requestFocusInWindow();
				topArea.setCaretPosition(p);
				GuiUtil.centerLineInScrollPane(topArea);
			});
			// addToHistory(p); //seems to break lots of stuff
		} catch (Throwable ex) {
			ex.printStackTrace();
		}
	}

	private void moveTo(int col, int line) {
		topArea.requestFocusInWindow();
		try {
			setCaretPos(
					topArea.getDocument().getDefaultRootElement().getElement(line - 1).getStartOffset() + (col - 1));
		} catch (Exception ex) {
		}
	}

	protected Progg tryParse(String program) {
		try {
			return parse(program);
		} catch (ParseException e) {
			int col = e.column;
			int line = e.line;

			moveTo(col, line);

			toDisplay = "Syntax error: " + e.getLocalizedMessage();
			e.printStackTrace();
			return null;
		} catch (ParserException e) { // legacy - for fql, etc
			int col = e.getLocation().column;
			int line = e.getLocation().line;

			moveTo(col, line);

			toDisplay = "Syntax error: " + e.getLocalizedMessage();
			e.printStackTrace();
			return null;
		} catch (LocException e) {
			setCaretPos(e.loc);
			toDisplay = "Type error: " + e.getLocalizedMessage();
			e.printStackTrace();
			return null;
		} catch (Throwable e) {
			toDisplay = "Error: " + e.getLocalizedMessage();
			e.printStackTrace();
			return null;
		}
	}

	public boolean abortBecauseDirty() {
		try {
			if (!GUI.getDirty(id)) {
				return false;
			}
		} catch (NullPointerException npe) { // TODO aql weird
			npe.printStackTrace();
			return true;
		}
		int choice = JOptionPane.showOptionDialog(null, "Unsaved changes - continue to close?", "Close?",
				JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null, new Object[] { "Yes", "No" }, "No");
		return (choice != JOptionPane.YES_OPTION);
	}

	public void optionsHaveChanged() {
		IdeOptions.theCurrentOptions.apply(this);
		// clearSpellCheck();
	}

	/*
	 * public void clearSpellCheck() { SwingUtilities.invokeLater(() -> { try {
	 * topArea.forceReparsing(spc); } catch (Throwable t) { // t.printStackTrace();
	 * } }); }
	 */
	@SuppressWarnings("static-method")
	protected Collection<String> reservedWords() {
		return Collections.emptySet();
	}

	@Override
	public String getSelectedText() {
		return topArea.getSelectedText();
	}

	@Override
	public void searchEvent(SearchEvent e) {

		SearchEvent.Type type = e.getType();
		SearchContext context = e.getSearchContext();
		SearchResult result = null;

		// topArea.mark
		switch (type) {

		case MARK_ALL:
			result = SearchEngine.markAll(topArea, context);
			break;
		case FIND:
			result = SearchEngine.find(topArea, context);
			if (!result.wasFound()) {
				UIManager.getLookAndFeel().provideErrorFeedback(topArea);
			}
			break;
		case REPLACE:
			topArea.setMarkOccurrences(false);

			result = SearchEngine.replace(topArea, context);
			if (!result.wasFound()) {
				UIManager.getLookAndFeel().provideErrorFeedback(topArea);
			}
			break;
		case REPLACE_ALL:
			topArea.setMarkOccurrences(false);

			result = SearchEngine.replaceAll(topArea, context);
			JOptionPane.showMessageDialog(null, result.getCount() + " occurrences replaced.");
			break;
		}

	}

	public volatile boolean isClosed = false;

	public volatile Progg parsed_prog;
	public volatile String parsed_prog_string;
	// public volatile Unit parsed_prog_lock;

	public void close() {
		isClosed = true;
		if (findDialog != null) {
			findDialog.removeSearchListener(this);
			findDialog.dispose();
		}
		if (replaceDialog != null) {
			replaceDialog.removeSearchListener(this);
			replaceDialog.dispose();
		}
		findDialog = null;
		replaceDialog = null;
		topArea.clearParsers();
		List<PropertyChangeListener> l = new ArrayList<>(Arrays.asList(topArea.getPropertyChangeListeners()));
		for (PropertyChangeListener x : l) {
			topArea.removePropertyChangeListener(x);
		}
	}

	@SuppressWarnings("unused")
	protected boolean omit(String s, Progg p) {
		return false;
	}

	// TODO aql pull these from options rather than cache here?
	private volatile Boolean enable_outline = false;
	public volatile Boolean outline_alphabetical = false;
	private volatile Boolean outline_on_left = true;
	public volatile Boolean outline_prefix_kind = true;
	private volatile Boolean outline_elongated = true;
	public volatile long sleepDelay = 2;
	public volatile Boolean outline_types = true;
	public volatile Long last_keystroke = null;

	public void set_delay(int i) {
		if (i < 1) {
			i = 1;
		}
		sleepDelay = (long) i * 1000;
	}

	public void outline_types(Boolean bool) {
		outline_types = bool;
		// getOutline().build();
	}

	public void enable_outline(Boolean bool) {
		enable_outline = bool;
		situate();
	}

	public void outline_alphabetical(Boolean bool) {
		outline_alphabetical = bool;
		if (q.isEmpty()) {
			parsed_prog_string = "";
			//parsed_prog = null;
			q.add(Unit.unit);
		}
		//doUpdate();
	}

	public void outline_on_left(Boolean bool) {
		outline_on_left = bool;
		situate();
	}

	public void outline_prefix_kind(Boolean bool) {
		outline_prefix_kind = bool;
		// getOutline().build();
	}

	public void outline_elongated(Boolean bool) {
		outline_elongated = bool;
		situate();
	}

	///////////////////////////////////////////////////////////

	protected abstract void doUpdate();

	protected DefaultMutableTreeNode makeTree(List<String> set) {
		DefaultMutableTreeNode root = new DefaultMutableTreeNode();

		for (String k : set) {
			DefaultMutableTreeNode n = new DefaultMutableTreeNode();
			n.setUserObject(new TreeLabel(k, k));
			root.add(n);
		}
		return root;
	}

	public final JPanel p;

	private void setComp(List<String> set) {
		TreePath t1 = tree.getSelectionPath();

		Enumeration<TreePath> p = tree.getExpandedDescendants(new TreePath(tree.getModel().getRoot()));
		
		DefaultTreeModel zz = new DefaultTreeModel(makeTree(set));
		SwingUtilities.invokeLater(() -> {
			tree.setModel(zz);
		
			if (p == null) {
				return;
			}
			while (p.hasMoreElements()) {
				try {
					TreePath path = p.nextElement();
					if (conv(path) != null) {
						tree.expandPath(conv(path));
					}
				} catch (Exception ex) {
				}
			}
	
			if (t1 != null) {
				TreePath t2 = conv(t1);
				if (t2 != null) {
					tree.setSelectionPath(t2);
					tree.scrollPathToVisible(t2);
				}
			}
		});
	}

	private TreePath conv(TreePath path) {
		TreePath parent = path.getParentPath();
		if (parent == null) {
			return new TreePath(tree.getModel().getRoot());
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

	public void build() {

		if (parsed_prog == null) {
			return;
		}
		List<String> set = new LinkedList<>(parsed_prog.keySet());
		if (outline_alphabetical) {
			set.sort(Util.AlphabeticalComparator);
		}
		// Point p = jsp.getViewport().getViewPosition();
		setComp(set);
		// revalidate();
		// jsp.getViewport().setViewPosition(p);
	}

	public final JLabel oLabel = new JLabel("", JLabel.CENTER);
	public final JScrollPane jsp;
	public final JCheckBox validateBox = new JCheckBox("Prove", true);
	/*
	 * public void startThread() { Thread t = new Thread(new Runnable() {
	 * 
	 * @Override public void run() { long x = 0; for (;;) { long l =
	 * System.currentTimeMillis(); // threadBody(); long r =
	 * System.currentTimeMillis(); x = r - l;
	 * 
	 * try { long todo = sleepDelay - x; if (todo > 100) { Thread.sleep(todo); } }
	 * catch (Throwable e1) { return; } if (isClosed) { return; } } }
	 * 
	 * }); t.setDaemon(true); t.setPriority(Thread.MIN_PRIORITY); t.start(); }
	 */
	protected JTree tree;

	protected DefaultTreeCellRenderer makeRenderer() {
		DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
		renderer.setLeafIcon(null);
		renderer.setOpenIcon(null);
		renderer.setClosedIcon(null);
		return renderer;
	}

	protected synchronized JTree getComp() {
		if (tree != null) {
			return tree;
		}
		tree = new JTree(new DefaultMutableTreeNode());
		tree.setCellRenderer(makeRenderer());
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
				if (o instanceof CodeEditor.TreeLabel) {
					TreeLabel l = (TreeLabel) o;
					if (parsed_prog != null) {
						int line = parsed_prog.getLine(l.name);
						if (line != -1) {
							setCaretPos(line);
							addToHistory(line);
						}
					}
				} else if (o instanceof InteriorLabel) {
					InteriorLabel<?> l = (InteriorLabel<?>) o;
					setCaretPos(l.loc);
					addToHistory(l.loc);
				}

			}
		});

		return tree;
	}

	protected void threadBody() {
		while (!isClosed) {
			try {
				String s = topArea.getText();
				if (!s.equals(parsed_prog_string)) {
					Progg e = parse(s);
					parsed_prog_string = s;
					parsed_prog = e;
					oLabel.setText("");

					// if (System.currentTimeMillis() - last_keystroke > sleepDelay) {
					build();
					// }

				}
			} catch (Throwable ex) {
				ex.printStackTrace();
				oLabel.setText("err");
			}
		}
	}

	protected LinkedBlockingDeque<Unit> q = new LinkedBlockingDeque<>(1);
	protected volatile long qt = -1;


	

	public void setOutlineFont(Font font) {
		getComp().setFont(font);
	}

	

	public class TreeLabel {
		public final String s;
		public final String name;

		public TreeLabel(String s, String name) {
			super();
			Util.assertNotNull(s);
			this.s = s;
			this.name = name;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (s.hashCode());
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
			if (!s.equals(other.s))
				return false;
			return true;
		}

		public String toString() {
			return s;
		}

	}

	private void startThread() {
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				threadBody();
			}

		});
		t.setDaemon(true);
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
	}

	protected DefaultListModel<String> foo() {
		return new DefaultListModel<>();
	}

}
