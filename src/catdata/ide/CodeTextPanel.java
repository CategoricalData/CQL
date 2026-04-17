package catdata.ide;

import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.border.Border;
import javax.swing.event.UndoableEditEvent;
import javax.swing.text.Document;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

@SuppressWarnings("serial")
/*
 * @author ryan A text editor.
 */
public class CodeTextPanel extends JPanel {

	public final JTextArea area;

	public void setText(String s) {
		area.setText(s);
		area.setCaretPosition(0);
		// System.out.println(s.substring(0, Integer.min(5, s.length())));
	}

	public String getText() {
		return area.getText();
	}

	public CodeTextPanel(String title, String text) {
		this(BorderFactory.createEtchedBorder(), title, text);
	}
	
	public CodeTextPanel(String title, String text, Font f, boolean b) {
		this(BorderFactory.createEtchedBorder(), title, text, f, b);
	}

	JScrollPane p;
	
	public CodeTextPanel(Border bb, String title, String text) {
		this(bb, title, text, new Font("Courier", Font.PLAIN, 13), true);
	}

	public CodeTextPanel(Border bb, String title, String text, Font f, boolean bx) {
		super(new GridLayout(1, 1));
		if (bb != null) {
			Border b = BorderFactory.createTitledBorder(bb, title);
			setBorder(b);
		}
		area = new JTextArea(text);
		p = new JScrollPane(area);
		// p.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		add(p);
		p.setBorder(BorderFactory.createEmptyBorder());
		if (text.length() > 1024*256) {
			text = text.substring(0, 1024*256) + " TRUNCATED";
		}
		area.setFont(f);
		setText(text);

		
		// area.setEditable(false);

		UndoManager m = new UndoManager();
		m.setLimit(16); // since common case is lots of dots in bottom panels
		// area.setundoManager = new UndoManager();
		Document doc = area.getDocument();
		doc.addUndoableEditListener((UndoableEditEvent e) -> m.addEdit(e.getEdit()));

		InputMap im = area.getInputMap(JComponent.WHEN_FOCUSED);
		ActionMap am = area.getActionMap();

		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "Undo");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "Redo");

		am.put("Undo", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					if (m.canUndo()) {
						m.undo();
					}
				} catch (CannotUndoException exp) {
					exp.printStackTrace();
				}
			}
		});
		am.put("Redo", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					if (m.canRedo()) {
						m.redo();
					}
				} catch (CannotUndoException exp) {
					exp.printStackTrace();
				}
			}
		});

		setWordWrap(true);
		if (bx) {
			IdeOptions.theCurrentOptions.apply(this.area);
		}
	}

	private void setWordWrap(boolean b) {
		area.setLineWrap(b);
		area.setWrapStyleWord(b);
		// p.revalidate();
		// getScrollableTracksViewportWidth
		// area.getScrgetViewport().set
	}

}
