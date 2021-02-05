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
 * Menu action for redoing the last undone change.
 */
public class RedoAction extends AbstractAction {
	/**
	 *    
	 */
	private static final long serialVersionUID = 4038451403532026959L;

	/**  */
	private JGraph graph;

	/**
	 *
	 *
	 * @param graph
	 */
	private RedoAction(JGraph graph) {
		super("Redo");

		putValue(Action.SHORT_DESCRIPTION, "Redo the last undone change");

		this.graph = graph;

		putValue(Action.MNEMONIC_KEY, KeyEvent.VK_R);
	}

	/**
	 *
	 *
	 * @param sketch
	 */
	public RedoAction(Sketch sketch) {
		this((JGraph) sketch);
	}

	/**
	 *
	 *
	 * @param view
	 */
	public RedoAction(View view) {
		this((JGraph) view);
	}

	/**
	 * When action is performed, do a redo.
	 *
	 * @param e The action event
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		EasikGraphModel model = (EasikGraphModel) graph.getModel();

		if (model.canRedo()) {
			model.redo();
		}
	}
}
