package catdata.aql;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import gnu.trove.map.hash.TCustomHashMap;
import gnu.trove.strategy.HashingStrategy;

public class ConsList<X> {

	@Override
	public int hashCode() {
		return System.identityHashCode(this);
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj;
	}

	public final List<X> list;

	@SuppressWarnings("rawtypes")
	private static HashingStrategy<List> strategy = new HashingStrategy<>() {
		private static final long serialVersionUID = 1L;

		@Override
		public int computeHashCode(List t) {
			return t.hashCode();
		}

		@Override
		public boolean equals(List s, List t) {
			return s.equals(t);
		}
	};

	@SuppressWarnings("rawtypes")
	public static Map<List, ConsList> cache = new TCustomHashMap<>(strategy);

	public static synchronized <X> ConsList<X> new0(List<X> l, Boolean copy) {
		ConsList<X> ret = cache.get(l);
		if (ret != null) {
			return ret;
		}
		ret = new ConsList<>(l, copy);
		cache.put(ret.list, ret);
		return ret;
	}

	public static <X> ConsList<X> new0(ConsList<X> a, ConsList<X> b) {
		return new0(a.list, b.list);
	}

	public static <X> ConsList<X> new0(List<X> a, List<X> b) {
		List<X> ret = new ArrayList<>(a.size() + b.size());
		ret.addAll(a);
		ret.addAll(b);
		return new0(ret, false);
	}

	public static <X> ConsList<X> new0(List<X> a, List<X> b, List<X> c) {
		List<X> ret = new ArrayList<>(a.size() + b.size() + c.size());
		ret.addAll(a);
		ret.addAll(b);
		ret.addAll(c);
		return new0(ret, false);
	}

	public static <X> ConsList<X> new0(ConsList<X> a, ConsList<X> b, ConsList<X> c) {
		return new0(a.list, b.list, c.list);
	}

	private ConsList(List<X> list, Boolean copy) {
		if (copy) {
			this.list = new ArrayList<>(list);
		} else {
			this.list = list;
		}
		this.size = list.size();
	}

	private final int size;

	public int size() {
		return size;
	}

	public boolean isEmpty() {
		return size == 0;
	}

	public Iterator<X> iterator() {
		return list.iterator();
	}

	public X get(int index) {
		return list.get(index);
	}

	public int indexOf(X o) {
		return list.indexOf(o);
	}

	public int lastIndexOf(X o) {
		return list.lastIndexOf(o);
	}

	public ConsList<X> subList(int fromIndex, int toIndex, boolean copy) {
		return ConsList.new0(list.subList(fromIndex, toIndex), copy);
	}

	@Override
	public String toString() {
		return list.toString();
	}

}
