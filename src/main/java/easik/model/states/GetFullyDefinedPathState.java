package easik.model.states;

//~--- non-JDK imports --------------------------------------------------------
import java.util.ArrayList;
//~--- JDK imports ------------------------------------------------------------
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Map;

import org.jgraph.graph.GraphConstants;

import easik.graph.EasikGraphModel;
import easik.model.Model;
import easik.model.edge.ModelEdge;
import easik.model.edge.ModelEdge.Cascade;
import easik.model.path.ModelPath;
import easik.model.ui.ModelFrame;
import easik.model.vertex.ModelVertex;
import easik.sketch.edge.InjectiveEdge;
import easik.sketch.edge.NormalEdge;

/**
 * The GetPathState is a state which can be used to retrieve a single path from
 * the user. It ensures that the only edges which are selectable are those which
 * are attached to the previous edge.
 *
 * @author Rob Fletcher 2005
 * @author Kevin Green 2006
 * @author Vera Ranieri 2006
 * @version 2006-07-13 Kevin Green
 * @version 06-2014 Federico Mora
 */
public class GetFullyDefinedPathState<F extends ModelFrame<F, GM, M, N, E>, GM extends EasikGraphModel, M extends Model<F, GM, M, N, E>, N extends ModelVertex<F, GM, M, N, E>, E extends ModelEdge<F, GM, M, N, E>> extends ModelState<F, GM, M, N, E> {
	/** Stores whether the next state is the finish state */
	protected boolean _finishState;

	/** Stores whether the next state is to add a new path */
	protected boolean _nextState;

	/** The path created from the edges chosen */
	protected ModelPath<F, GM, M, N, E> _pathToReturn;

	/** Contains elements of the sketch that are selectable */
	@SuppressWarnings("rawtypes")
	protected Map _selectable;

	/** Stores the desired start node */
	protected N _sourceNode;

	/** Stores the desired target node */
	protected N _targetNode;

	protected boolean _cascade;

	/** Contains elements of the sketch that are not selectable */
	@SuppressWarnings("rawtypes")
	protected Map _unselectable;

	/**
	 * Default Constructor
	 *
	 * @param next
	 *            boolean determining whether the user should be allowed to
	 *            select next on this round. True if next could be selected,
	 *            false otherwise.
	 * @param finish
	 *            boolean determining whether the user should be allowed to
	 *            select finish on this round. True if finish could be selected,
	 *            false otherwise.
	 * @param inSketch
	 *            the sketch in which this is occurring
	 */
	public GetFullyDefinedPathState(boolean next, boolean finish, M inModel, boolean cascade) {
		this(next, finish, inModel, null, null, cascade);
	}

	/**
	 * Gets a path that must begin at the specified source node.
	 *
	 * @param next
	 *            boolean determining whether the user should be allowed to
	 *            select next on this round. True if next could be selected,
	 *            false otherwise.
	 * @param finish
	 *            boolean determining whether the user should be allowed to
	 *            select finish on this round. True if finish could be selected,
	 *            false otherwise.
	 * @param inSketch
	 *            the sketch in which this is occurring
	 * @param sourceNode
	 *            N at which the path must begin; only edges with this node as
	 *            source will be initially selectable. null means the path can
	 *            start anywhere.
	 * @param targetNode
	 *            N at which the path must end; until a path ending at
	 *            targetNode is selected, the user will be unable to click
	 *            next/finish. null means the path can end anywhere.
	 * @param cascade
	 */
	public GetFullyDefinedPathState(boolean next, boolean finish, M inModel, N sourceNode, N targetNode, boolean cascade) {
		super(inModel);

		_pathToReturn = null;
		_nextState = next;
		_finishState = finish;
		_sourceNode = sourceNode;
		_targetNode = targetNode;
		_cascade = cascade;
	}

	/**
	 * When pushed on, the first thing done is clearing the selection and then
	 * disabling selection for all items except for edges.
	 */
	@Override
	@SuppressWarnings("rawtypes")
	public void pushedOn() {
		setNextButton(false);
		setCancelButton(true);
		setFinishButton(false);

		_selectable = new Hashtable();
		_unselectable = new Hashtable();

		GraphConstants.setSelectable(_selectable, true);
		GraphConstants.setSelectable(_unselectable, false);

		// Initially, we allow all edges and entities to be selected
		_ourModel.clearSelection();
		_ourModel.refresh();

		// Gets pushed on by all "add constraint" states. We don't want to be
		// able to imbed adding constraints
		_ourModel.getFrame().enableAddConstraintItems(false);
	}

	/**
	 * Returns the edges that are to be selectable. Only edges extending from
	 * the current path are selectable; if there is no path, then any edge
	 * qualifies.
	 *
	 * @return
	 */
	@Override
	public Object[] getSelectables() {
		ArrayList<Object> selectable = new ArrayList<>();
		/*
		 * if we are allowing the creation of edges and nodes during the
		 * constraint creation time then everything has to be selectable
		 * 
		 */

		for (Object cell : _ourModel.getRoots()) {
			selectable.add(cell);
		}

		return selectable.toArray();

	}

	/**
	 * Update the selection so that the only selectable items will be those
	 * within reach of the existing edges.
	 */
	@Override
	public void selectionUpdated() {
		_ourModel.refresh();

		Object[] newSelection = _ourModel.getSelectionCells();

		// First check to see if the selection is empty
		if (newSelection.length == 0) {
			// We can't have a next/finish button available:
			setNextButton(false);
			setFinishButton(false);
		} else {
			// make sure selection consists of Normal Edges or injective edges
			// and if we want it to be cascade make sure it is cascade.
			for (Object o : newSelection) {
				if (!(o instanceof NormalEdge)) {
					if (!(o instanceof InjectiveEdge)) {
						return;
					} 
						if (_cascade) {
							if (((InjectiveEdge) o).getCascading() != Cascade.CASCADE) {
								return;
							}
						}
					
				} else {
					if (_cascade) {
						if (((NormalEdge) o).getCascading() != Cascade.CASCADE) {
							return;
						}
					}
				}

			}

			// If we have an explicit target, you can hit next only if the
			// selected path codomain matches
			@SuppressWarnings("unchecked")
			boolean codoOK = (_targetNode == null) ? true // No target node:
															// always okay
					: ((E) newSelection[newSelection.length - 1]).getTargetEntity() == _targetNode;

			// Since we have something selected, then we can enable the 'next'
			// button, if it should be.
			setNextButton(_nextState && codoOK);

			// If the user can finish this round, then the Finish button should
			// be activated.
			setFinishButton(_finishState && codoOK);
		}
	}

	/**
	 * When the state gets popped, then it should tell the new top item what
	 * path it had collected before being popped. Since this is called AFTER
	 * popping, it can use peek() to get the top item.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void poppedOff() {
		((PathAcceptingState<F, GM, M, N, E>) _ourModel.getStateManager().peekState()).passPath(_pathToReturn);
		_ourModel.getGraphLayoutCache().reload();
		_ourModel.clearSelection();
	}

	/**
	 * If path collection has been cancelled, then pop off, and set the path to
	 * be null.
	 */
	@Override
	public void cancelClicked() {
		_pathToReturn = null;

		resetSelection();
		_ourModel.getStateManager().popState();
	}

	/**
	 * When next is clicked, pop off after preparing an array containing the
	 * edges in the path. Convert to the proper graph edge of the sketch.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void nextClicked() {
		resetSelection();

		LinkedList<E> path = new LinkedList<>();
		Object[] selCells = _ourModel.getSelectionCells();

		if ((selCells.length == 1) && (selCells[0] instanceof ModelVertex)) {
			_pathToReturn = new ModelPath<>((N) selCells[0]);
		} else {
			for (int i = 0; i < selCells.length; i++) {
				path.add((E) selCells[i]);
			}

			_pathToReturn = new ModelPath<>(path);
		}

		_ourModel.getStateManager().popState();
	}

	/**
	 * When finish is clicked, the stack is popped off after an array containing
	 * the path is created.
	 */
	@Override
	public void finishClicked() {
		nextClicked();
	}

	/**
	 * State string identifier.
	 *
	 * @return String literal "Select a path"
	 */
	@Override
	public String toString() {
		return "Select a path";
	}

	/**
	 * Set everything to be selectable
	 */
	private void resetSelection() {
		_ourModel.getGraphLayoutCache().edit(_ourModel.getRoots(), _selectable);
	}
}
