package easik.model.util.graph;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Stack;

import easik.sketch.Sketch;
import easik.sketch.edge.InjectiveEdge;
import easik.sketch.edge.NormalEdge;
import easik.sketch.edge.SketchEdge;
import easik.sketch.vertex.EntityNode;

/**
 * Kosaraju's Algorithm, used to find strongly connected components
 * 
 * Federico Mora
 */
public class KosarajuSCC {
	Stack<EntityNode> stack = new Stack<>();
	ArrayList<String> scc = new ArrayList<>();
	LinkedList<EntityNode> visited = new LinkedList<>();
	LinkedList<EntityNode> sccList = new LinkedList<>();
	Sketch graph;

	public KosarajuSCC(Sketch g) {
		graph = g;
		Iterator<EntityNode> iter = graph.getEntities().iterator();
		while (stack.size() < graph.getEntities().size()) {
			EntityNode v = iter.next();
			if (!visited.contains(v)) {
				dfs(v);
			}
		}
		visited.clear();
		while (stack.size() > 0) {
			sccList.clear();
			EntityNode v = stack.pop();
			transposeDFS(v);
			String s = "";
			if (sccList.size() > 1) {
				for (EntityNode n : sccList) {
					s += n.getName() + ", ";
				}
				scc.add(s);
			}
		}

	}

	/**
	 * Depth first search
	 **/
	public void dfs(EntityNode v) {
		visited.add(v);
		for (SketchEdge sk : v.getOutgoingEdges()) {
			if (sk instanceof NormalEdge || sk instanceof InjectiveEdge) {
				EntityNode t = sk.getTargetEntity();
				if (!visited.contains(t)) {
					dfs(t);
				}
			}
		}
		stack.push(v);
	}

	/**
	 * Depth first search of the transpose
	 * 
	 * @param v
	 */
	public void transposeDFS(EntityNode v) {
		visited.add(v);
		sccList.add(v);
		for (SketchEdge sk : graph.getEdges().values()) {
			if (sk instanceof NormalEdge || sk instanceof InjectiveEdge) {
				if (sk.getTargetEntity().equals(v)) {
					EntityNode t = sk.getSourceEntity();
					if (!visited.contains(t)) {
						transposeDFS(t);
					}
				}
			}
		}
	}

	public String getSCC() {
		String s = "";
		if (scc.size() == 0) {
			return s;
		}
		for (int i = 0; i < scc.size(); i++) {
			s += scc.get(i);
			if ((scc.size() - i) > 1) {
				s += "\n\t";
			}
		}
		return s;
	}
}
