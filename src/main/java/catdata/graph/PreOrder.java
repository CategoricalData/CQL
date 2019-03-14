package catdata.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.iterators.IteratorIterable;

import com.google.common.collect.Iterators;

import catdata.Pair;
import catdata.Util;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public class PreOrder<X> {

	private static final Map<Integer, Set<PreOrder<Integer>>> totals = new THashMap<>();

	public static <X> Collection<PreOrder<X>> allTotal(List<X> xs) {
		Set<PreOrder<Integer>> set;
		if (totals.containsKey(xs.size())) {
			set = totals.get(xs.size());
		} else {
			set = allTotal2(allUpTo(xs.size()));
			totals.put(xs.size(), set);
		}

		return set.stream().map(x -> convert(xs, x)).collect(Collectors.toList());
	}

	private static <X> PreOrder<X> convert(List<X> xs, PreOrder<Integer> p) {
		return new PreOrder<>(
				p.g.stream().map(z -> new Pair<>(xs.get(z.first), xs.get(z.second))).collect(Collectors.toList()));
	}

	private static Collection<Integer> allUpTo(int n) {
		List<Integer> ret = new ArrayList<>(n);
		for (int i = 0; i < n; i++) {
			ret.add(i);
		}
		return ret;
	}

	private static <X> Set<PreOrder<X>> allTotal2(Collection<X> xs) {
		List<Pair<X, X>> dom = new LinkedList<>();

		for (X x : xs) {
			for (X y : xs) {
				if (x.equals(y)) {
					continue;
				}
				dom.add(new Pair<>(x, y));
			}
		}

		Set<PreOrder<X>> ret = new THashSet<>();
		Set<Set<Pair<X, X>>> cands = Util.powerSet(dom);
		for (Set<Pair<X, X>> cand : cands) {
			PreOrder<X> p = new PreOrder<>();

			for (X x : xs) {
				for (X y : xs) {
					if (cand.contains(new Pair<>(x, y))) {
						p.add(x, y);
					} else {
						p.add(y, x);
					}
				}
			}
			ret.add(p);
		}
		return ret;
	}
	/*
	 * public static <X> Set<PreOrder<X>> allTotal(Collection<X> xs) { List<Pair<X,
	 * X>> dom = new LinkedList<>(); List<Boolean> cod = new LinkedList<>();
	 * cod.add(true); cod.add(false);
	 * 
	 * for (X x : xs) { for (X y : xs) { if (x.equals(y)) { continue; } dom.add(new
	 * Pair<>(x, y)); } }
	 * 
	 * Set<PreOrder<X>> ret = new HashSet<>(); List<LinkedHashMap<Pair<X,X>,
	 * Boolean>> cands = FinSet.homomorphs(dom, cod); for (Map<Pair<X, X>, Boolean>
	 * cand : cands) { PreOrder<X> p = new PreOrder<>(); for (Pair<X,X> pair :
	 * cand.keySet()) { if (cand.get(pair)) { p.add(pair.first, pair.second); } else
	 * { p.add(pair.second, pair.first); } } ret.add(p); }
	 * 
	 * return ret; }
	 */

	private PreOrder() {
	}

	private PreOrder(Collection<Pair<X, X>> p) {
		g.addAll(p);
	}

	public PreOrder(PreOrder<X> p) {
		g.addAll(p.g);
	}

	private final Set<Pair<X, X>> g = new THashSet<>();

	private Iterator<X> dom() {
		return Iterators.transform(g.iterator(), x -> x.first);
	}

	void add(X lhs, X rhs) {
		g.add(new Pair<>(lhs, lhs));
		g.add(new Pair<>(rhs, rhs));
		g.add(new Pair<>(lhs, rhs));

		while (trans())
			;
	}

	public boolean gte(X lhs, X rhs) {
		return lhs.equals(rhs) || g.contains(new Pair<>(lhs, rhs));
	}

	private boolean trans() {
		Iterable<X> Xs = new IteratorIterable<>(dom());
		boolean changed = false;
		for (X a : Xs) {
			for (X b : Xs) {
				for (X c : Xs) {
					if (gte(a, b) && gte(b, c)) {
						changed = changed | g.add(new Pair<>(a, c));
					}
				}
			}
		}
		return changed;
	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((g == null) ? 0 : g.hashCode());
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
		PreOrder<?> other = (PreOrder<?>) obj;
		if (g == null) {
			if (other.g != null)
				return false;
		} else if (!g.equals(other.g))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "PreOrder [g=" + g + "]";
	}

}
