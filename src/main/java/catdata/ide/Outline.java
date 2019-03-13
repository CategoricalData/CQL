package catdata.ide;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import catdata.Prog;
import catdata.Unit;
import catdata.Util;

public abstract class Outline<Progg extends Prog, Env, DDisp extends Disp> {

	public final CodeEditor<Progg, Env, DDisp> codeEditor;

	final JPanel p;

	protected abstract JComponent getComp();

	protected abstract void setComp(List<String> order);

	public synchronized void build() {
		Point p = jsp.getViewport().getViewPosition();
		synchronized (this.codeEditor.parsed_prog_lock) {
			if (this.codeEditor.parsed_prog == null) {
				return;
			}
			List<String> set = new LinkedList<>(this.codeEditor.parsed_prog.keySet());
			if (this.codeEditor.outline_alphabetical) {
				set.sort(Util.AlphabeticalComparator);
			}
			setComp(set);
		}
		this.codeEditor.revalidate();
		jsp.getViewport().setViewPosition(p);
		
	}

	public JLabel oLabel = new JLabel("", JLabel.CENTER);
	protected JScrollPane jsp;
	public final JCheckBox validateBox = new JCheckBox("Prove", true);

	protected Outline(CodeEditor<Progg, Env, DDisp> codeEditor) {
		Util.assertNotNull(codeEditor);
		this.codeEditor = codeEditor;
		jsp = new JScrollPane(getComp());
		jsp.setBorder(BorderFactory.createEmptyBorder());
		p = new JPanel(new BorderLayout());
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
			codeEditor.outline_alphabetical(alphaBox.isSelected());
		});
		alphaBox.setHorizontalTextPosition(SwingConstants.LEFT);
		alphaBox.setHorizontalAlignment(SwingConstants.RIGHT);
		validateBox.setHorizontalTextPosition(SwingConstants.LEFT);
		validateBox.setHorizontalAlignment(SwingConstants.LEFT);
		
		gbc.weightx = 1;
		gbc.anchor = GridBagConstraints.EAST;
		q.add(alphaBox, gbc);
		p.add(q, BorderLayout.NORTH);
		p.add(jsp, BorderLayout.CENTER);
		p.setBorder(BorderFactory.createEtchedBorder());

		this.codeEditor.parsed_prog_lock = Unit.unit;
		this.codeEditor.parsed_prog_string = "";
		// build();

		p.setMinimumSize(new Dimension(0, 0));

		

	}

	public void startThread() {
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				long x = 0;
				for (;;) {
					long l = System.currentTimeMillis();
					threadBody();
					long r = System.currentTimeMillis();
					x = r - l;
					
					try {
						long todo = codeEditor.sleepDelay - x;
						if (todo > 100) { 
							Thread.sleep(todo);
						}
					} catch (Throwable e1) {
						return;
					}
					if (Outline.this.codeEditor.isClosed) {
						return;
					}
				}
			}

			

		});
		t.setDaemon(true);
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
	}
	
	

	protected void threadBody() {
		try {
			String s = Outline.this.codeEditor.topArea.getText();
			if (!s.equals(Outline.this.codeEditor.parsed_prog_string)) {
				Progg e = Outline.this.codeEditor.parse(s);
				oLabel.setText("");
				if (!equiv(e, Outline.this.codeEditor.parsed_prog)) {
					if (System.currentTimeMillis()
							- Outline.this.codeEditor.last_keystroke > codeEditor.sleepDelay) {
						synchronized (Outline.this.codeEditor.parsed_prog_lock) {
							Outline.this.codeEditor.parsed_prog = e;
							Outline.this.codeEditor.parsed_prog_string = s;
						}
						build();
						//Outline.this.codeEditor.clearSpellCheck(); 
					}
				}
			}
		} catch (Throwable ex) {
			oLabel.setText("err");
		}
	}

	@SuppressWarnings("unused")
	protected boolean equiv(Progg now, Progg then) {
		return false;
	}

	public void setFont(Font font) {
		getComp().setFont(font);
	}

}