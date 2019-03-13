package catdata.graph;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import catdata.Util;

/**
 * 
 * A morphism of graphs taking nodes to nodes and edges to (possibly empty) paths.
 * 
 * @author ryan
 */
public final class Match<N1,E1,N2,E2> {

	private final DMG<N1,E1> src;
	private final DMG<N2,E2> dst;

	private final Map<N1, N2> nodes;
	private final Map<E1, List<E2>> edges;
	
	public Match(DMG<N1, E1> src, DMG<N2, E2> dst, Map<N1, N2> nodes, Map<E1, List<E2>> edges) {
		this.src = src;
		this.dst = dst;
		this.nodes = nodes;
		this.edges = edges;
		validate();
	}
	
	private void validate() {
		for (N1 en1 : src.nodes) {
			N2 en2 = nodes.get(en1);
			if (en2 == null) {
				throw new RuntimeException("in " + this + ", " + "source node " + en1 + " has no mapping");
			}
			if (!dst.nodes.contains(en2)) {
				throw new RuntimeException("in " + this + ", " + "source node " + en1 + " maps to " + en2 + ", which is not in target");
			}	
		}
		for (N1 en1 : nodes.keySet()) {
			if (!src.nodes.contains(en1)) {
				throw new RuntimeException("in " + this + ", " + "there is a mapping for " + en1 + " which is not a source node");
			}
		}
		for (E1 en1 : src.edges.keySet()) {
			List<E2> en2 = edges.get(en1);
			if (en2 == null) {
				throw new RuntimeException("in " + this + ", " + "source edge " + en1 + " has no mapping");
			}
			dst.type(nodes.get(src.edges.get(en1).first), en2);
		}
		for (E1 en1 : edges.keySet()) {
			if (!src.edges.keySet().contains(en1)) {
				throw new RuntimeException("in " + this + ", " + "there is a mapping for " + en1 + " which is not a source edge");
			}
		}
	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((dst == null) ? 0 : dst.hashCode());
		result = prime * result + ((edges == null) ? 0 : edges.hashCode());
		result = prime * result + ((nodes == null) ? 0 : nodes.hashCode());
		result = prime * result + ((src == null) ? 0 : src.hashCode());
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
		Match<?,?,?,?> other = (Match<?,?,?,?>) obj;
		if (dst == null) {
			if (other.dst != null)
				return false;
		} else if (!dst.equals(other.dst))
			return false;
		if (edges == null) {
			if (other.edges != null)
				return false;
		} else if (!edges.equals(other.edges))
			return false;
		if (nodes == null) {
			if (other.nodes != null)
				return false;
		} else if (!nodes.equals(other.nodes))
			return false;
		if (src == null) {
			if (other.src != null)
				return false;
		} else if (!src.equals(other.src))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		List<String> nodesStr = nodes.keySet().stream().map(x -> x + " -> " + nodes.get(x)).collect(Collectors.toList());
		List<String> edgesStr = edges.keySet().stream().map(x -> x + " -> " + Util.sep(edges.get(x), ".")).collect(Collectors.toList());
		return "nodes\n\t" + Util.sep(nodesStr, "\n\t") + "\n\nedges\n\t" + Util.sep(edgesStr, "\n\t");
	} 
	
}

