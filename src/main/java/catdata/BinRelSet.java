package catdata;

import java.util.Iterator;
import java.util.Set;
import java.util.function.Predicate;

import gnu.trove.set.hash.THashSet;

//TODO aql need compose at all?
public class BinRelSet<X, Y> implements Iterable<Pair<X, Y>> {

	/*
	 * // does not allow empty sets public class It implements Iterator<Pair<X, Y>>
	 * {
	 * 
	 * Iterator<X> xIt; X x; Iterator<Y> yIt; Set<Y> ys;
	 * 
	 * public It() { xIt = R.keySet().iterator(); }
	 * 
	 * @Override public void remove() { yIt.remove(); if (ys.isEmpty()) {
	 * xIt.remove(); } }
	 * 
	 * @Override public boolean hasNext() { if (yIt != null && yIt.hasNext()) {
	 * return true; } if (!xIt.hasNext()) { return false; } x = xIt.next(); yIt =
	 * R.get(x).iterator(); ys = R.get(x);
	 * 
	 * return true; }
	 * 
	 * @Override public Pair<X, Y> next() { Y y = yIt.next(); return new Pair<X,
	 * Y>(x, y); }
	 * 
	 * }
	 * 
	 * public LinkedHashMap<X, LinkedHashSet<Y>> R;
	 * 
	 * public BinRelSet(Set<Pair<X, Y>> r) { R = Util.toRel(r); }
	 * 
	 * public BinRelSet(BinRelSet<X, Y> r) { R = new LinkedHashMap<>(); for (X x :
	 * r.R.keySet()) { R.put(x, new LinkedHashSet<>(r.R.get(x))); } }
	 * 
	 * public BinRelSet() { R = new LinkedHashMap<>(); }
	 * 
	 * public boolean add(X x, Y y) { LinkedHashSet<Y> ys = R.get(x); if (ys ==
	 * null) { ys = new LinkedHashSet<>(); R.put(x, ys); } return ys.add(y); }
	 * 
	 * public void remove(X x, Y y) { Set<Y> ys = R.get(x); if (ys == null) {
	 * return; } ys.remove(y); if (ys.isEmpty()) { R.remove(x); } }
	 * 
	 * public <Z> BinRelSet<X, Z> compose(BinRelSet<Y, Z> rhs) { LinkedHashMap<X,
	 * LinkedHashSet<Z>> ret = new LinkedHashMap<>(); for (X x : R.keySet()) {
	 * LinkedHashSet<Z> set = new LinkedHashSet<>(); for (Y y : R.get(x)) {
	 * LinkedHashSet<Z> w = rhs.R.get(y); if (w != null) { set.addAll(w); } } if
	 * (!set.isEmpty()) { ret.put(x, set); } } BinRelSet<X, Z> b = new
	 * BinRelSet<>(); b.R = ret; return b; }
	 * 
	 * public void addAll(BinRelSet<X, Y> a) { for (X x : a.R.keySet()) { for (Y y :
	 * a.R.get(x)) { add(x, y); } } }
	 * 
	 * public void removeIf(Predicate<Pair<X, Y>> p) { Iterator<X> xIt =
	 * R.keySet().iterator(); while (xIt.hasNext()) { X x = xIt.next(); Iterator<Y>
	 * yIt = R.get(x).iterator(); while (yIt.hasNext()) { Y y = yIt.next(); if
	 * (p.test(new Pair<>(x, y))) { yIt.remove(); } } if (R.get(x).isEmpty()) {
	 * xIt.remove(); } } }
	 * 
	 * public boolean isEmpty() { return R.isEmpty(); }
	 * 
	 * @Override public Iterator<Pair<X, Y>> iterator() { return new It(); }
	 */

	public final Set<Pair<X, Y>> R;

	@Override
	public String toString() {
		return "BinRelSet [R=" + R + "]";
	}

	public BinRelSet(Set<Pair<X, Y>> r) {
		R = new THashSet<>(r);
	}

	public BinRelSet() {
		R = new THashSet<>();
	}

	public boolean add(X x, Y y) {
		return R.add(new Pair<>(x, y));
	}

	public void remove(X x, Y y) {
		R.remove(new Pair<>(x, y));
	}

	public <Z> BinRelSet<X, Z> compose(BinRelSet<Y, Z> rhs) {
		return new BinRelSet<>(Util.compose(R, rhs.R));
	}

	@Override
	public Iterator<Pair<X, Y>> iterator() {
		return R.iterator();
	}

	public void addAll(BinRelSet<X, Y> a) {
		R.addAll(a.R);
	}

	public void removeIf(Predicate<Pair<X, Y>> p) {
		R.removeIf(p);
	}

	public boolean isEmpty() {
		return R.isEmpty();
	}

}
