package catdata.graph;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

/**
 * Directed labeled multi-graphs.  
 * 
 * @author ryan
 *
 * @param <N> type of nodes
 * @param <E> type of edges
 */
public class DMG<N,E> {
	public final Collection<N> nodes;
	public final Map<E, Pair<N,N>> edges;
	
	public DMG(Collection<N> nodes, Map<E, Pair<N,N>> edges) {
		this.nodes = nodes;
		this.edges = edges;
		validate();
	}
	
	private void validate() {
		for (E e : edges.keySet()) {
			if (!nodes.contains(edges.get(e).first)) {
				throw new RuntimeException("Not a node: " + edges.get(e).first);
			}
			if (!nodes.contains(edges.get(e).second)) {
				throw new RuntimeException("Not a node: " + edges.get(e).second);
			}
		}
	}
	
	public Collection<E> edges(N src, N dst) {
		List<E> ret = new LinkedList<>();
		for (E e : edges.keySet()) {
			if (edges.get(e).first.equals(src) && edges.get(e).second.equals(dst)) {
				ret.add(e);
			}
		}
		return ret;
	}
	
	public Pair<N,N> type(N src, List<E> path) {
		Util.assertNotNull(src, path);
		N dst = src;
		for (E e : path) {
			if (!edges.containsKey(e)) {
				throw new RuntimeException("Not an edge: " + e);
			}
			if (!dst.equals(edges.get(e).first)) {
				throw new RuntimeException("Ill-typed: " + path + ", edge " + e + " has source " + edges.get(e).first + " but is applied to " + src);
			}
			dst = edges.get(e).second;
		}		
		return new Pair<>(src, dst);
	}
	
	public DMG(Collection<N> nodes, Set<Triple<E, N, N>> edges) {
		this.nodes = new THashSet<>(nodes);	
		this.edges = new THashMap<>();
		for (Triple<E, N, N> e : edges) {
			if (this.edges.containsKey(e.first)) {
				throw new RuntimeException("Duplicate element: " + e.first);
			}
			this.edges.put(e.first, new Pair<>(e.second, e.third));
		}
		validate();
	}
	
	@Override
	public String toString() {
		if (nodes.isEmpty()) {
			return "";
		}
		
		List<String> l = new LinkedList<>();
		for (E e  : edges.keySet()) {
			l.add("\t" + e + ": " + edges.get(e).first + " -> " + edges.get(e).second);
		}
		return "nodes\n\t" + Util.sep(nodes, "\n\t") + "\nedges\n\n" + Util.sep(l, "\n");
	}
	
	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((edges == null) ? 0 : edges.hashCode());
		result = prime * result + ((nodes == null) ? 0 : nodes.hashCode());
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
		DMG<?,?> other = (DMG<?,?>) obj;
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
		return true;
	}
	
}