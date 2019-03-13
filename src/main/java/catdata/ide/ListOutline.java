package catdata.ide;

import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Vector;

import javax.swing.JList;

import catdata.Prog;

public class ListOutline<Progg extends Prog, Env, DDisp extends Disp> extends Outline<Progg, Env, DDisp> {

	JList<String> list;

	protected synchronized JList<String> getComp() {
		if (list != null) {
			return list;
		}

		list = GuiUtil.makeList();

		return list;
	}

	protected void setComp(List<String> set) {
		Vector<String> listData = new Vector<>();
		for (String s : set) {
			if (!this.codeEditor.omit(s, this.codeEditor.parsed_prog)) {
				String pre = this.codeEditor.outline_prefix_kind ? this.codeEditor.parsed_prog.kind(s) + " " : "";
				listData.add(pre + s);
			}
		}
		getComp().setListData(listData);
	}

	@SuppressWarnings({ "rawtypes" })
	ListOutline(CodeEditor<Progg, Env, DDisp> codeEditor) {
		super(codeEditor);

		list.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				JList list = (JList) e.getSource();
				if (list.locationToIndex(e.getPoint()) == -1 && !e.isShiftDown() && !isMenuShortcutKeyDown(e)) {
					list.clearSelection();
				}
			}

			private boolean isMenuShortcutKeyDown(InputEvent event) {
				return (event.getModifiers() & Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) != 0;
			}
		});

		list.addListSelectionListener(e -> {
			synchronized (this.codeEditor.parsed_prog) {
				String s = list.getSelectedValue();
				if (s == null) {
					return;
				}
				if (this.codeEditor.outline_prefix_kind) {
					s = s.substring(s.indexOf(" ") + 1, s.length());
				}
				Integer line = this.codeEditor.parsed_prog.getLine(s);
				if (line != null) {
					this.codeEditor.setCaretPos(line);
				}
			}
		});

	}

}