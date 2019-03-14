package catdata.graph;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

//WeightedQuickUnionPathCompressionUF a la Robert Sedgewick and Kevin Wayne
public class UnionFindClassic<X> {

	@Override
	public String toString() {
		return "UnionFindClassic [parent=" + parent + ", size=" + size + "]";
	}

	public Collection<X> values() {
		return parent.keySet();
	}

	private final Map<X, X> parent;
	private final Map<X, Integer> size;

	public UnionFindClassic(int n, Iterable<X> xs) {
		parent = new THashMap<>(n);
		size = new THashMap<>(n);

		for (X x : xs) {
			parent.put(x, x);
			size.put(x, 1);
		}
	}

	private void add(X x) {
		if (parent.containsKey(x)) {
			return;
		}
		parent.put(x, x);
		size.put(x, 1);
	}

	public X find(X p) {
		add(p);
		X root = p;
		while (!root.equals(parent.get(root))) {
			root = parent.get(root);
		}
		while (!(p.equals(root))) {
			X newp = parent.get(p);
			parent.put(p, root);
			p = newp;
		}
		return root;
	}

	public boolean connected(X p, X q) {
		return find(p).equals(find(q));
	}

	// return root that got clobbered;
	public void union(X p, X q) {
		X rootP = find(p);
		X rootQ = find(q);
		if (rootP.equals(rootQ)) {
			return;
		}

		// make smaller root point to larger one
		if (size.get(rootP) < size.get(rootQ)) {
			parent.put(rootP, rootQ);
			size.put(rootQ, size.get(rootQ) + size.get(rootP));
			return;
		}
		parent.put(rootQ, rootP);
		size.put(rootP, size.get(rootP) + size.get(rootQ));
	}

	public Map<X, Set<X>> toMap() {
		Map<X, Set<X>> ret = new THashMap<>();
		for (X x : parent.keySet()) {
			ret.put(x, eqc(x));
		}
		return ret;
	}

	public Set<X> eqc(X x) {
		Set<X> ret = new THashSet<>();
		for (X x0 : parent.keySet()) {
			if (connected(x, x0)) {
				ret.add(x0);
			}
		}
		return ret;
	}

}
