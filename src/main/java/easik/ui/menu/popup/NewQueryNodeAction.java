package easik.ui.menu.popup;

//~--- non-JDK imports --------------------------------------------------------

import java.awt.Point;
//~--- JDK imports ------------------------------------------------------------
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import easik.ui.DefineQueryNodeDialog;
import easik.ui.ViewFrame;
import easik.view.util.QueryException;
import easik.view.vertex.QueryNode;

/**
 * Action for the new query node option in the popup menu.
 *
 * @author Rob Fletcher 2005
 */
public class NewQueryNodeAction extends AbstractAction {
	/**
	 *    
	 */
	private static final long serialVersionUID = 3903448103245162805L;

	/** The point at which to place the new entity */
	Point _newPoint;

	/** The frame in which the new entity will be displayed. */
	ViewFrame _theFrame;

	/**
	 * Prepare the menu option, as well as pass a reference to the last clicked
	 * point, which is used when positioning the new entity.
	 *
	 * @param inPoint The sketch's last-rightclicked-position
	 * @param inFrame
	 */
	public NewQueryNodeAction(Point inPoint, ViewFrame inFrame) {
		super("Add view node...");

		_theFrame = inFrame;

		putValue(Action.SHORT_DESCRIPTION, "Add a new view node to the view");

		_newPoint = inPoint;
	}

	/**
	 * Create the new query node.
	 * 
	 * @param e The action event
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		String defaultName = _theFrame.getMModel().getNewName();
		QueryNode newNode;
		try {
			newNode = new QueryNode(defaultName, (int) _newPoint.getX(), (int) _newPoint.getY(), _theFrame.getMModel(),
					"");
			DefineQueryNodeDialog dqnd = new DefineQueryNodeDialog(_theFrame, "New Query Node", newNode);

			if (!dqnd.isAccepted()) {
				return;
			}

			String name = dqnd.getName();
			while (name.equals("") || _theFrame.getMModel().isNameUsed(name)) {
				JOptionPane
						.showMessageDialog(_theFrame,
								"Error while naming entity.\n" + "Please ensure that entity name is:\n"
										+ "1) Not blank\n" + "2) Not already in use",
								"Error", JOptionPane.ERROR_MESSAGE);

				name = (String) JOptionPane.showInputDialog(_theFrame, "Name for new entity:", "Get name",
						JOptionPane.QUESTION_MESSAGE, null, null, name);

				if (name == null) {
					return;
				}

				name = name.trim();
			}
			String query = dqnd.getQuery();
			newNode.setName(name);
			newNode.setQuery(query);
			_theFrame.getMModel().addEntity(newNode);
			_theFrame.getMModel().setDirty();
		} catch (QueryException e1) {
			// this can technically throw an exception in two spots but will
			// only really throw in the setQuery call
			// because the first call, a constructor call to queryNode, gives an
			// emptry query which is ok
			JOptionPane.showMessageDialog(_theFrame.getParent(),
					"New Query node not created. Not valid query.\n" + e1.getMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
		}

	}
}
