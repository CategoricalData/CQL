package easik.ui.menu;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.jgraph.JGraph;

import easik.graph.EasikGraphModel;
import easik.sketch.Sketch;
import easik.view.View;

/**
 * Menu action for undoing the last edit.
 */
public class UndoAction extends AbstractAction {
	/**
	 *    
	 */
	private static final long serialVersionUID = 5340958706690208226L;

	/**  */
	private JGraph graph;

	/**
	 *
	 *
	 * @param graph
	 */
	private UndoAction(JGraph graph) {
		super("Undo");

		this.graph = graph;

		putValue(Action.MNEMONIC_KEY, KeyEvent.VK_U);
	}

	/**
	 *
	 *
	 * @param sketch
	 */
	public UndoAction(Sketch sketch) {
		this((JGraph) sketch);

		putValue(Action.SHORT_DESCRIPTION, "Undo the last change made to the sketch");
	}

	/**
	 *
	 *
	 * @param view
	 */
	public UndoAction(View view) {
		this((JGraph) view);

		putValue(Action.SHORT_DESCRIPTION, "Undo the last change made to the view");
	}

	/**
	 * When action is performed, do an undo.
	 *
	 * @param e
	 *            The action event
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		EasikGraphModel model = (EasikGraphModel) graph.getModel();

		if (model.canUndo()) {
			model.undo();
		}
	}
}
