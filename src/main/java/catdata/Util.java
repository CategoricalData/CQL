package catdata;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.iterators.IteratorIterable;
import org.apache.commons.collections4.list.TreeList;

import com.google.common.collect.Iterators;

import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public class Util {

	public static void writeFile(String text, String file) throws IOException {
		assertNotNull(text);
		File f = new File(file);
		if (!f.exists()) {
			f.createNewFile();
		}
		FileWriter w = new FileWriter(file);
		// System.out.println( new File(file).getAbsolutePath() );
		w.write(text);
		w.close();
	}

	public static <K, V> Map<K, V> fromNullable(Map<K, V> m) {
		Map<K, V> ret = new THashMap<>(m.size());
		for (Entry<K, V> k : m.entrySet()) {
			if (k.getValue() != null) {
				ret.put(k.getKey(), k.getValue());
			}
		}
		return ret;
	}

	public static String quote(String s) {
		s = s.replace("\\", "\\" + "\\"); // \ --> \\
		s = s.replace("\"", "\\\""); // " --> \"
		return "\"" + s + "\"";
	}

	//public static volatile int i = 0;
	private static final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(),new ThreadFactory() {
        public Thread newThread(Runnable r) {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);
            return t;
        }
    });
	public static <X> X timeout(Callable<X> c, long timeout) {
		Future<X> future = executor.submit(c);
		try {
			return future.get(timeout, TimeUnit.MILLISECONDS);
		} catch (TimeoutException ex) {
			future.cancel(true);
			throw new RuntimeException("Timout after " + (timeout / 1000)
							+ " seconds. \n\nPossible solution: add options timeout=X where X > " + (timeout / 1000)
							+ " is how many seconds to wait.");
		} catch (ExecutionException ex) {
			ex.printStackTrace();
			throw new RuntimeException(ex);
		} catch (InterruptedException e) {
			throw new RuntimeInterruptedException(e);
		}  catch (ThreadDeath d) {
			throw new RuntimeInterruptedException(d);
		}
		
	}

	private static Map<String, Class<?>> load_cache = new THashMap<>();

	public synchronized static Class<?> load(String clazz) {
		if (clazz == null) {
			return Util.anomaly();
		}
		if (load_cache.containsKey(clazz)) {
			return load_cache.get(clazz);
		}
		try {
			Class<?> z = Class.forName(clazz);
			load_cache.put(clazz, z);
			return z;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new RuntimeException(clazz + " is not on the java classpath");
		}
	}

	public static <X, Y> List<Pair<X, Y>> toList(Map<X, Y> map) {
		List<Pair<X, Y>> ret = new ArrayList<>(map.size());
		for (Entry<X, Y> e : map.entrySet()) {
			ret.add(new Pair<>(e.getKey(), e.getValue()));
		}
		return ret;
	}

	public static <X> Set<X> toSetSafely(List<X> set) {
		Set<X> ret = new THashSet<>(set.size());
		for (X x : set) {
			if (ret.contains(x)) {
				throw new RuntimeException("Duplicate elmement " + x);
			}
			ret.add(x);
		}
		return ret;
	}

	public static <X> List<X> concat(List<List<X>> l) {
		List<X> ret = new LinkedList<>();
		for (List<X> x : l) {
			ret.addAll(x);
		}
		return ret;
	}

	public static class UpToF<X> implements Iterator<X> {
		public UpToF(int start, int m, Function<Integer, X> f) {
			this.m = m;
			this.n = start;
			this.f = f;
		}

		private final int m;
		private int n;
		private Function<Integer, X> f;

		@Override
		public boolean hasNext() {
			return n < m;
		}

		@Override
		public X next() {
			return f.apply(n++);
		}
	};

	public static class UpTo implements Iterator<Integer> {
		public UpTo(int start, int m) {
			this.m = m;
			this.n = start;
		}

		private final int m;
		private int n;

		@Override
		public boolean hasNext() {
			return n < m;
		}

		@Override
		public Integer next() {
			return n++;
		}
	};

	public static <X> List<X> diff(Collection<X> l, Collection<?> r) {
		List<X> ret = new TreeList<>(l);
		ret.removeAll(r);
		return ret;
	}

	public static <X, Y> Map<X, Y> diff(Map<X, Y> l, Map<?, ?> r) {
		Map<X, Y> ret = new THashMap<>(l);
		ret.keySet().removeAll(r.keySet());
		return ret;
	}

	public static <X> Function<Void, X> voidFn() {
		return v -> {
			throw new RuntimeException("Anomaly: please report");
		};
	}

	public static void assertNotNull(Object... O) {
		for (Object o : O) {
			if (o == null) {
				throw new RuntimeException("Anomaly: please report ");
			}
		}
	}

	public static <X, Y, Z> Pair<LinkedHashMap<X, Y>, LinkedHashMap<X, Z>> split(Map<X, Chc<Y, Z>> map) {
		LinkedHashMap<X, Y> m1 = new LinkedHashMap<>();
		LinkedHashMap<X, Z> m2 = new LinkedHashMap<>();
		for (X x : map.keySet()) {
			Chc<Y, Z> e = map.get(x);
			if (e.left) {
				m1.put(x, e.l);
			} else {
				m2.put(x, e.r);
			}
		}
		return new Pair<>(m1, m2);
	}

	public static <X> X abort(Void v) {
		if (v == null) {
			throw new RuntimeException("Anomaly: please report");
		}
		throw new RuntimeException("Called on non-null void");
	}

	public static <X> List<X> newIfNull(List<X> l) {
		return l == null ? Collections.emptyList() : l;
	}

	public static String sep(Collection<?> c, String sep) {
		return sep(c.iterator(), sep);
	}

	public static String sep(Iterator<?> c, String sep) {
		return sep(c, sep, Object::toString);
	}

	public static <X> String sep(Iterator<X> c, String sep, Function<X, String> fun) {
		StringBuffer ret = new StringBuffer("");
		boolean b = false;
		while (c.hasNext()) {
			X o = c.next();
			if (b) {
				ret.append(sep);
			}
			b = true;

			ret.append(fun.apply(o));
		}
		return ret.toString();
	}

	public static <X, Y> boolean isBijection(Map<X, Y> m, Set<X> X, Set<Y> Y) {
		if (!m.keySet().equals(X)) {
			return false;
		}
		if (!new THashSet<>(m.values()).equals(Y)) {
			return false;
		}
		Map<Y, X> n = rev(m, Y);
		if (n == null) {
			return false;
		}

		Map<X, X> a = compose0(m, n);
		Map<Y, Y> b = compose0(n, m);

		return a.equals(id(X)) && (!b.equals(id(Y)));
	}

	public static <X> Map<X, X> id(Collection<X> X) {
		Map<X, X> ret = new THashMap<>(X.size());
		for (X x : X) {
			ret.put(x, x);
		}
		return ret;
	}

	private static <X, Y> X rev(Map<X, Y> m, Y y) {
		X x = null;
		for (X x0 : m.keySet()) {
			Y y0 = m.get(x0);
			if (y0.equals(y)) {
				if (x != null) {
					return null;
				}
				x = x0;
			}
		}
		return x;
	}

	public static <A, B, C> Map<A, C> compose0(Map<A, B> x, Map<B, C> y) {
		Map<A, C> ret = new THashMap<>(x.size());

		for (Entry<A, B> a : x.entrySet()) {
			ret.put(a.getKey(), y.get(a.getValue()));
		}

		return ret;
	}

	private static <X, Y> Map<Y, X> rev(Map<X, Y> m, Set<Y> Y) {
		Map<Y, X> ret = new THashMap<>(Y.size());

		for (Y y : Y) {
			X x = rev(m, y);
			if (x == null) {
				return null;
			}
			ret.put(y, x);
		}

		return ret;
	}

	public static <X> List<X> append(List<X> x, List<X> y) {
		List<X> ret = (new ArrayList<>(x.size() + y.size()));
		ret.addAll(x);
		ret.addAll(y);
		return ret;
	}

	@SuppressWarnings({ "unchecked" })
	public static <X> X[] sing(X x) {
		return (X[]) new Object[] { x };
	}

	@SuppressWarnings("unchecked")
	public static <X, Y> Y[] map(X[] xs, Function<X, Y> f) {
		Y[] ret = (Y[]) new Object[xs.length];
		for (int i = 0; i < xs.length; i++) {
			ret[i] = f.apply(xs[i]);
		}
		return ret;
	}

	public static <X, Y> Y fold(X[] xs, Y y, Function<Pair<X, Y>, Y> f) {
		for (X x : xs) {
			y = f.apply(new Pair<>(x, y));
		}
		return y;
	}

	@SafeVarargs
	public static <X> List<X> list(X... xs) {
		List<X> ret = new LinkedList<>();
		ret.addAll(Arrays.asList(xs));
		return ret;
	}

	public static <K, V> Map<K, V> listToMap(List<Pair<K, V>> list) {
		Map<K, V> map = (new THashMap<>(list.size()));
		for (Pair<K, V> p : list) {
			if (map.containsKey(p.first)) {
				throw new RuntimeException("Duplicate entry for " + p.first + " in " + list);
			}
			map.put(p.first, p.second);
		}
		return map;
	}

	/**
	 * 
	 * @param m  target
	 * @param m2 source
	 */
	public static <K, V> void putAllSafely(Map<K, V> m, Map<K, V> m2) {
		for (K k : m2.keySet()) {
			V v2 = m2.get(k);
			if (!m.containsKey(k)) {
				m.put(k, v2);
				continue;
			}
			V v = m.get(k);
			if (v == null) {
				throw new RuntimeException("Anomaly: please report: k=" + k + " m=" + m + " and m2=" + m2);
			}
			if (!v.equals(v2)) {
				throw new RuntimeException("Collision on " + k + " was " + v + " becomes " + v2);
			}
		}

	}

	public static <X> Iterable<X> iterConcat(Iterable<X> i, Iterable<X> j) {
		return new IteratorIterable<>(Iterators.concat(i.iterator(), j.iterator()), true);
	}

	public static <X> X get0Y(Collection<X> c) {
		for (X x : c) {
			return x;
		}
		return null;
	}

	public static <K, X, V> Map<K, Chc<X, V>> inRight(Map<K, V> map) {
		if (map.isEmpty()) {
			return Collections.emptyMap();
		}
		Map<K, Chc<X, V>> ret = new THashMap<>(map.size());
		for (K k : map.keySet()) {
			ret.put(k, Chc.inRight(map.get(k)));
		}
		return ret;
	}

	public static <K, X, V> Map<K, Chc<V, X>> inLeft(Map<K, V> map) {
		if (map.isEmpty()) {
			return Collections.emptyMap();
		}
		Map<K, Chc<V, X>> ret = new THashMap<>(map.size());
		for (K k : map.keySet()) {
			ret.put(k, Chc.inLeft(map.get(k)));
		}
		return ret;
	}

	public static <X> X get0X(Collection<X> c) {
		for (X x : c) {
			return x;
		}
		throw new RuntimeException();
	}

	public static <X> X get0(Collection<X> c) {
		if (c.size() != 1) {
			throw new RuntimeException("Size != 1 on " + c);
		}
		for (X x : c) {
			return x;
		}
		throw new RuntimeException();
	}

	static <X extends Comparable<X>, Y> String printNicely(Set<Map<X, Y>> map) {
		List<String> l = map.stream().map(Util::printNicely).collect(Collectors.toList());
		Collections.sort(l);
		return sep(l, "\n");
	}

	private static <X extends Comparable<X>, Y> String printNicely(Map<X, Y> map) {
		List<X> l = new ArrayList<>(map.keySet());
		Collections.sort(l);
		boolean first = true;
		String ret = "";
		for (X key : l) {
			if (!first) {
				ret += ", ";
			}
			ret += key + "=" + map.get(key);
			first = false;
		}
		return ret;
	}

	public static boolean isInt(String s) {
		try {
			Integer.parseInt(s);
			return true;
		} catch (NumberFormatException ex) {
			return false;
		}
	}

	public static String maybeQuote(String s) {
		if (s.trim().isEmpty()) {
			return "\"" + s.replace("\"", "\\\"") + "\"";
		}
		if (isInt(s)) {
			return s;
		}
		Character x = s.charAt(0);
		if (!Character.isLetter(x) && !x.equals('_')) {
			return "\"" + s.replace("\"", "\\\"") + "\"";
		}
		for (Character c : s.toCharArray()) {
			if (!Character.isLetterOrDigit(c) && !c.equals('_')) {
				return "\"" + s.replace("\"", "\\\"") + "\"";
			}
		}
		return s;
	}

	/*
	 * 
	 * public static List<List<Integer>> multiply_many(List<List<Integer>> l,
	 * List<List<List<Integer>>> r) { List<List<Integer>> ret = l; for
	 * (List<List<Integer>> x : r) { ret = mat_conv2(mult(mat_conv1(x),
	 * mat_conv1(l))); } return ret; }
	 * 
	 * public static List<List<Integer>> multiply(List<List<Integer>> l,
	 * List<List<Integer>> r) { return mat_conv2(mult(mat_conv1(l), mat_conv1(r)));
	 * }
	 * 
	 * private static int[][] mat_conv1(List<List<Integer>> l) { int[][] ret = new
	 * int[l.size()][]; int w = 0; for (List<Integer> r : l) { int[] q = new
	 * int[r.size()]; int cnt = 0; for (int x : r) { q[cnt++] = x; } ret[w++] = q; }
	 * return ret; }
	 * 
	 * // public for OPL example public static List<List<Integer>> mat_conv2(int[][]
	 * l) { List<List<Integer>> ret = new LinkedList<>(); for (int[] r : l) {
	 * List<Integer> q = new LinkedList<>(); for (int x : r) { q.add(x); }
	 * ret.add(q); } return ret; }
	 * 
	 * private static int[][] mult(int[][] A, int[][] B) { int mA = A.length; int nA
	 * = A[0].length; int mB = B.length; int nB = B[0].length; if (nA != mB) throw
	 * new RuntimeException("Illegal matrix dimensions: " + mat_conv2(A) + " and " +
	 * mat_conv2(B)); int[][] C = new int[mA][nB]; for (int i = 0; i < mA; i++) for
	 * (int j = 0; j < nB; j++) for (int k = 0; k < nA; k++) C[i][j] += A[i][k] *
	 * B[k][j]; return C; }
	 */
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static final Comparator<Object> LengthComparator = (Object o1, Object o2) -> {
		if (o1.toString().length() > o2.toString().length()) {
			return 1;
		} else if (o1.toString().length() < o2.toString().length()) {
			return -1;
		}
		return o1.toString().compareTo(o2.toString());
	};

	public static final Comparator<Object> AlphabeticalComparator = new AlphanumComparator();

//			Comparator.comparing(Object::toString);

	public static <X, Y> Map<Y, X> rev0(Map<X, Y> m) {
		return rev(m, new THashSet<>(m.values()));
	}

	public static <X, Y> Map<Y, Set<X>> revS(Map<X, Y> m) {
		Map<Y, Set<X>> ret = new THashMap<>(m.size());
		for (X x : m.keySet()) {
			Y y = m.get(x);
			Set<X> s = ret.computeIfAbsent(y, k -> new THashSet<>());
			s.add(x);
		}
		return ret;
	}

	public static <X> String print(Collection<X[]> c) {
		String ret = "";
		for (X[] x : c) {
			ret += Arrays.toString(x);
		}
		return ret;
	}

	public static <X, Y> Map<X, Y> reify(Function<X, Y> f, Set<X> set) {
		Map<X, Y> ret = new THashMap<>(set.size());
		for (X x : set) {
			ret.put(x, f.apply(x));
		}
		return ret;
	}

	public static String nice(String s) {
		return s;
	}

	/*
	 * public static <X, Y> List<Y> proj2(Collection<Pair<X, Y>> l) { return
	 * l.stream().map(x -> x.second).collect(Collectors.toList()); }
	 * 
	 * public static <X, Y> List<X> proj1(Collection<Pair<X, Y>> l) { return
	 * l.stream().map(x -> x.first).collect(Collectors.toList()); }
	 */
	@SuppressWarnings("unchecked")
	public static <X> String sep(Collection<?> order, Map<?, ?> m, String sep1, String sep2, boolean skip,
			Function<X, String> fn) {
		StringBuffer ret = new StringBuffer("");
		boolean b = false;
		for (Object o : order) {
			Object z = m.get(o);
			if (z == null && skip) {
				continue;
			}
			if (b) {
				ret.append(sep2);
			}
			b = true;
			ret.append(o);
			ret.append(sep1);
			ret.append(fn.apply((X) m.get(o)));
		}
		return ret.toString();
	}

	public static String sep(Map<?, ?> m, String sep1, String sep2) {
		return sep(m.keySet(), m, sep1, sep2, false, Object::toString);
	}

	public static <X> String sep(Map<?, X> m, String sep1, String sep2, Function<X, String> fn) {
		return sep(m.keySet(), m, sep1, sep2, false, fn);
	}

	public static String q(Object o) {
		if (o == null) {
			return "!!!NULL!!!";
		}
		String s = o.toString();
		if ((s.contains("\t") || s.contains("\n") || s.contains("\r") || s.contains("-") || s.isEmpty())
				&& !s.contains("\"")) {
			return "\"" + s + "\"";
		}
		return s;
	}

	public static <X, Y> Y lookup(Collection<Pair<X, Y>> s, X x) {
		Y y = lookupNull(s, x);
		if (y == null) {
			throw new RuntimeException("Cannot find " + nice(x.toString()) + " in " + nice(s.toString()));
		}
		return y;
	}

	public static <X, Y> Y lookupNull(Collection<Pair<X, Y>> s, X x) {
		for (Pair<X, Y> o : s) {
			if (o.first.equals(x)) {
				return o.second;
			}
		}
		return null;
	}

	public static <X, Y, Z> Set<Pair<X, Z>> compose(Collection<Pair<X, Y>> x, Collection<Pair<Y, Z>> y) {
		Set<Pair<X, Z>> ret = new THashSet<>(x.size() + y.size());

		for (Pair<X, Y> p1 : x) {
			for (Pair<Y, Z> p2 : y) {
				if (p1.second.equals(p2.first)) {
					Pair<X, Z> p = new Pair<>(p1.first, p2.second);
					ret.add(p);
				}
			}
		}
		return ret;
	}

	public static <X> Set<Pair<X, X>> refl(Set<X> set) {
		Set<Pair<X, X>> ret = new THashSet<>(set.size());
		for (X x : set) {
			ret.add(new Pair<>(x, x));
		}
		return ret;
	}

	public static <X, Y> Map<X, Y> convert(Set<Pair<X, Y>> t) {
		Map<X, Y> ret = new THashMap<>(t.size());

		for (Pair<X, Y> p : t) {
			if (ret.containsKey(p.first)) {
				throw new RuntimeException("Cannot convert to map (not functional): " + t);
			}
			ret.put(p.first, p.second);
		}

		return ret;
	}

	public static <X, Y> Map<X, Set<Y>> convertMulti(Set<Pair<X, Y>> t) {
		Map<X, Set<Y>> ret = new THashMap<>(t.size());

		for (Pair<X, Y> p : t) {
			if (!ret.containsKey(p.first)) {
				ret.put(p.first, new THashSet<>());
			}
			ret.get(p.first).add(p.second);
		}

		return ret;
	}

	public static <X, Y> void putSafely(Map<X, Y> ret, X k, Y v) {
		if (ret.containsKey(k) && !ret.get(k).equals(v)) {
			throw new RuntimeException("Two distinct bindings for " + k + ": " + v + " and " + ret.get(k));
		}
		ret.put(k, v);
	}

	public static <X, Y> Map<X, Y> toMapSafely(Collection<Pair<X, Y>> t) {
		Map<X, Y> ret = new THashMap<>(t.size());

		for (Pair<X, Y> p : t) {
			putSafely(ret, p.first, p.second);
		}

		return ret;
	}

	public static <X, Y> Set<Pair<X, Y>> convert(Map<X, Y> t) {
		Set<Pair<X, Y>> ret = new THashSet<>(t.size());

		for (Entry<X, Y> p : t.entrySet()) {
			ret.add(new Pair<>(p.getKey(), p.getValue()));
		}

		return ret;
	}

	public static <X, Y> Map<Y, X> invMap(Map<X, Y> m) {
		Map<Y, X> ret = new THashMap<>(m.size());

		for (Entry<X, Y> e : m.entrySet()) {
			if (ret.containsKey(e.getValue())) {
				throw new RuntimeException("Not injective");
			}
			ret.put(e.getValue(), e.getKey());
		}

		return ret;
	}

	public static <X, Y> Y revLookup(Map<Y, X> m, X x) {
		Y ret = null;
		for (Entry<Y, X> e : m.entrySet()) {
			if (e.getValue().equals(x)) {
				if (ret != null && !ret.equals(e.getKey())) {
					throw new RuntimeException("Inverse is not a function: " + m);
				}
				ret = e.getKey();
			}
		}
		return ret;
	}

	public static <X, Y> Function<Y, X> inverse(Function<X, Y> f, Set<X> s) {
		Map<X, Y> m = reify(f, s);
		return y -> revLookup(m, y);
	}

	public static <X, Y> X anyKey(Map<X, Y> m) {
		for (X x : m.keySet()) {
			return x;
		}
		throw new RuntimeException();
	}

	public static String printForPi(Map<?, ?> x) {
		if (x.isEmpty()) {
			return "";
		}
		if (x.size() == 1) {
			return x.get(0).toString();
		}

		String ret = "(";
		boolean first = true;
		for (Entry<?, ?> e : x.entrySet()) {
			if (!first) {
				ret += ", ";
			}
			first = false;
			ret += e.getValue();
		}
		ret += ")";
		return ret;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Pair<Function, Object> stripChcs(Object o) {
		if (o instanceof Chc) {
			Chc c = (Chc) o;
			if (c.left) {
				Pair<Function, Object> p = stripChcs(c.l);
				return new Pair<>(x -> Chc.inLeftNC(p.first.apply(x)), p.second);
			}
			Pair<Function, Object> p = stripChcs(c.r);
			return new Pair<>(x -> Chc.inRightNC(p.first.apply(x)), p.second);

		}
		return new Pair<>(x -> x, o);
	}

	public static <X> List<List<X>> prod(List<Set<X>> in1) {
		List<List<X>> y = new TreeList<>();
		y.add(new LinkedList<>());

		for (Set<X> X : in1) {
			List<List<X>> y0 = new TreeList<>();
			for (List<X> a : y) {
				for (X x : X) {
					List<X> toadd = new TreeList<>(a);
					toadd.add(x);
					y0.add(toadd);
				}
			}
			y = y0;
		}

		return y;
	}

	public static <X> List<X> reverse(List<X> l) {
		List<X> ret = new ArrayList<>(l);
		Collections.reverse(ret);
		return ret;
	}

	public static <T> void assertNoDups(Collection<T> list) {

		Set<T> duplicates = new THashSet<>(list.size());
		Set<T> uniques = new THashSet<>(list.size());

		for (T t : list) {
			if (!uniques.add(t)) {
				duplicates.add(t);
			}
		}

		if (!duplicates.isEmpty()) {
			throw new RuntimeException("List contains duplicates, namely " + duplicates);
		}
	}

	/**
	 * @return
	 */
	public static <Ty> List<Ty> alphabetical(Collection<Ty> tys) {
		List<Ty> ret = new ArrayList<>(tys);
		ret.sort(AlphabeticalComparator);
		return ret;
	}

	public static <Ty> Collection<Ty> alphaMaybe(boolean b, Collection<Ty> tys) {
		if (b) {
			return alphabetical(tys);
		}
		return tys;
	}

	/*
	 * public static <X> List<String> shortest(Collection<X> set) { List<String> ret
	 * = set.stream().map(Object::toString).collect(Collectors.toList());
	 * ret.sort(LengthComparator); return ret; }
	 */
	public static <X, Y, Z> Map<X, Map<Y, Z>> newMapsFor(Collection<X> xs) {
		Map<X, Map<Y, Z>> ret = new THashMap<>(xs.size());
		for (X x : xs) {
			ret.put(x, new THashMap<>());
		}
		return ret;
	}

	public static <X, Y> Map<X, Set<Y>> newSetsFor(Collection<X> xs) {
		Map<X, Set<Y>> ret = (new THashMap<>(xs.size()));
		for (X x : xs) {
			ret.put(x, (new THashSet<>()));
		}
		return ret;
	}

	public static <X> Collection<X> iterToColLazy(Iterable<X> it) {
		return new AbstractCollection<>() {
			@Override
			public Iterator<X> iterator() {
				return it.iterator();
			}

			@Override
			public int size() {
				return -1;
			}
		};
	}

	public static <X> Collection<X> iterToCol(Iterable<X> it, int size) {
		return new AbstractCollection<>() {
			@Override
			public Iterator<X> iterator() {
				return it.iterator();
			}

			@Override
			public int size() {
				return size;
			}
		};
	}

	public static <X, Y> Map<X, Collection<Y>> newSetsFor0(Collection<X> xs) {
		Map<X, Collection<Y>> ret = (new THashMap<>(xs.size()));
		for (X x : xs) {
			ret.put(x, (new THashSet<>()));
		}
		return ret;
	}

	public static <X, Y> Map<X, List<Y>> newListsFor(Collection<X> xs) {
		Map<X, List<Y>> ret = new THashMap<>(xs.size());
		for (X x : xs) {
			ret.put(x, (new TreeList<>()));
		}
		return ret;
	}

	public static <X, Y> Map<X, Y> constMap(Collection<X> xs, Y y) {
		Map<X, Y> ret = (new THashMap<>(xs.size()));
		for (X x : xs) {
			ret.put(x, y);
		}
		return ret;
	}

	public static <X extends Z, Y extends Z, Z> Set<Z> union(Collection<X> x, Collection<Y> y) {
		Set<Z> ret = new THashSet<>(x.size() + y.size());
		ret.addAll(x);
		ret.addAll(y);
		return ret;
	}

	/*
	 * public static <X> List<String> toString(Collection<X> list) { return
	 * list.stream().map(Object::toString).collect(Collectors.toList()); }
	 */
	public static <T> Set<Set<T>> powerSet(Collection<T> originalSet) {
		Set<Set<T>> sets = new THashSet<>((int) Math.pow(2, originalSet.size()));
		if (originalSet.isEmpty()) {
			sets.add(Collections.emptySet());
			return sets;
		}
		List<T> list = new ArrayList<>(originalSet);
		T head = list.get(0);
		Set<T> rest = new THashSet<>(list.subList(1, list.size()));
		for (Set<T> set : powerSet(rest)) {
			Set<T> newSet = new THashSet<>(set.size() + 1);
			newSet.add(head);
			newSet.addAll(set);
			sets.add(newSet);
			sets.add(set);
		}
		return sets;
	}

	public static final Comparator<Object> ToStringComparator = (Object o1, Object o2) -> {
		if (o1.toString().length() > o2.toString().length()) {
			return 1;
		} else if (o1.toString().length() < o2.toString().length()) {
			return -1;
		}
		return o1.toString().compareTo(o2.toString());
	};

	private static <K, V> boolean agreeOnOverlap0(Map<K, V> map, Map<K, V> ret) {
		for (K k : map.keySet()) {
			if (ret.containsKey(k)) {
				if (!ret.get(k).equals(map.get(k))) {
					return false;
				}
			}
		}
		return true;
	}

	public static <K, V> boolean agreeOnOverlap(Map<K, V> ret1, Map<K, V> ret2) {
		return agreeOnOverlap0(ret1, ret2) && agreeOnOverlap0(ret2, ret1);
	}

	public static <X> X anomaly() {
		throw new RuntimeException("Anomaly: please report");
	}

	public static class FilterTransfomIterator<T, U> implements Iterator<U> {
		Iterator<T> in;
		Function<T, Optional<U>> fn;

		public FilterTransfomIterator(Iterator<T> in, Function<T, Optional<U>> fn) {
			this.in = in;
			this.fn = fn;
			repair();
		}

		Optional<U> next;

		@Override
		public boolean hasNext() {
			return next != null && !next.isEmpty();
		}

		@Override
		public U next() {
			U u = next.get();
			repair();
			return u;
		}

		private void repair() {
			while (in.hasNext()) {
				T t = in.next();
				next = fn.apply(t);
				if (!next.isEmpty()) {
					return;
				}
				// next = Optional.empty();
			}
			next = Optional.empty();
		}

	}

	public static <X, Y> Collection<Object> isect(Collection<X> xs, Collection<Y> ys) {
		List<Object> l = new LinkedList<>(xs);
		l.removeIf(x -> !ys.contains(x));
		return l;
	}

	/**
	 * Levenshtein Edit Distance
	 * http://rosettacode.org/wiki/Levenshtein_distance#Java
	 * 
	 * @param s1
	 * @param s2
	 * @return
	 */
	public static int editDistance(String s1, String s2) {
		s1 = s1.toLowerCase();
		s2 = s2.toLowerCase();

		int[] costs = new int[s2.length() + 1];
		for (int i = 0; i <= s1.length(); i++) {
			int lastValue = i;
			for (int j = 0; j <= s2.length(); j++) {
				if (i == 0)
					costs[j] = j;
				else {
					if (j > 0) {
						int newValue = costs[j - 1];
						if (s1.charAt(i - 1) != s2.charAt(j - 1))
							newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1;
						costs[j - 1] = lastValue;
						lastValue = newValue;
					}
				}
			}
			if (i > 0)
				costs[s2.length()] = lastValue;
		}
		return costs[s2.length()];
	}

	/**
	 * Calculates a similarity (a number within 0 and 1) between two strings as 1 /
	 * 1 + editDistance
	 * 
	 */
	public static double similarity(String s1, String s2) { // TODO aql
		return (1) / ((double) 1 + editDistance(s1, s2));
	}

	public static String readFile(Reader r) throws IOException {
		try (BufferedReader reader = new BufferedReader(r)) {
			String line;
			StringBuilder stringBuilder = new StringBuilder();
			String ls = System.getProperty("line.separator");

			while ((line = reader.readLine()) != null) {
				stringBuilder.append(line);
				stringBuilder.append(ls);
			}

			reader.close();
			return stringBuilder.toString();
		}
	}

	public static <T> Iterable<List<T>> permutationsOf(List<T> l) {
		return new Iterable<>() {
			@Override
			public Iterator<List<T>> iterator() {
				return new PermutationIterator<>(l);
			}
		};
	}

	// seems like heap's algorithm
	private static final class PermutationIterator<T> implements Iterator<List<T>> {

		private List<T> nextPermutation;
		private final List<T> allElements = new ArrayList<>();
		private int[] indices;

		PermutationIterator(List<T> allElements) {
			if (allElements.isEmpty()) {
				nextPermutation = null;
				return;
			}

			this.allElements.addAll(allElements);
			this.indices = new int[allElements.size()];

			for (int i = 0; i < indices.length; ++i) {
				indices[i] = i;
			}

			nextPermutation = new ArrayList<>(this.allElements);
		}

		@Override
		public boolean hasNext() {
			return nextPermutation != null;
		}

		@Override
		public List<T> next() {
			if (nextPermutation == null) {
				throw new NoSuchElementException("No permutations left.");
			}

			List<T> ret = nextPermutation;
			generateNextPermutation();
			return ret;
		}

		private void generateNextPermutation() {
			int i = indices.length - 2;

			while (i >= 0 && indices[i] > indices[i + 1]) {
				--i;
			}

			if (i == -1) {
				// No more new permutations.
				nextPermutation = null;
				return;
			}

			int j = i + 1;
			int min = indices[j];
			int minIndex = j;

			while (j < indices.length) {
				if (indices[i] < indices[j] && indices[j] < min) {
					min = indices[j];
					minIndex = j;
				}

				++j;
			}

			swap(indices, i, minIndex);

			++i;
			j = indices.length - 1;

			while (i < j) {
				swap(indices, i++, j--);
			}

			loadPermutation();
		}

		private void loadPermutation() {
			List<T> newPermutation = new ArrayList<>(indices.length);

			for (int i : indices) {
				newPermutation.add(allElements.get(i));
			}

			this.nextPermutation = newPermutation;
		}

		private static void swap(int[] array, int a, int b) {
			int tmp = array[a];
			array[a] = array[b];
			array[b] = tmp;
		}

	}

	public static <X, Y> boolean containsUpToCase(Collection<X> set, Y s) {
		for (X t : set) {
			if (s.toString().toLowerCase().equals(t.toString().toLowerCase())) {
				return true;
			}
		}
		return false;
	}

	public static String[] union(String[] res, String[] res2) {
		String[] ret = new String[res.length + res2.length];
		System.arraycopy(res, 0, ret, 0, res.length);
		System.arraycopy(res2, 0, ret, res.length, res2.length);
		return ret;
	}

	public static void checkClass(String clazz) {
		try {
			if (!clazz.trim().isEmpty()) {
				Class.forName(clazz);
			}
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public static <X, Y> boolean containsKey(Set<Pair<X, Y>> set, X x) {
		for (Pair<X, Y> p : set) {
			if (p.first.equals(x)) {
				return true;
			}
		}
		return false;
	}

	public static <K1, V1, K2, V2> Map<K2, V2> map(Map<K1, V1> m, BiFunction<K1, V1, Pair<K2, V2>> f) {
		Map<K2, V2> ret = new THashMap<>(m.size());
		for (K1 k1 : m.keySet()) {
			Pair<K2, V2> p = f.apply(k1, m.get(k1));
			ret.put(p.first, p.second);
		}
		return ret;
	}

	public static String longestCommonPrefix(List<String> strings) {
		if (strings.size() == 0) {
			return ""; // Or maybe return null?
		}

		for (int prefixLen = 0; prefixLen < strings.get(0).length(); prefixLen++) {
			char c = strings.get(0).charAt(prefixLen);
			for (int i = 1; i < strings.size(); i++) {
				if (prefixLen >= strings.get(i).length() || strings.get(i).charAt(prefixLen) != c) {
					// Mismatch found
					return strings.get(i).substring(0, prefixLen);
				}
			}
		}
		return strings.get(0);
	}

	public static <X, Y> List<Pair<X, Y>> zip(List<X> x, List<Y> y) {
		if (x.size() != y.size()) {
			Util.anomaly();
		}
		List<Pair<X, Y>> ret = new ArrayList<>(x.size());
		Iterator<X> it = x.iterator();
		Iterator<Y> jt = y.iterator();
		while (it.hasNext()) {
			ret.add(new Pair<>(it.next(), jt.next()));
		}
		return ret;
	}

	static class NiceMap<X, Y> implements Map<X, Y> {

		private final Map<X, Y> m;

		@Override
		public String toString() {
			return Util.sep(this, ":", ", ");
		}

		@Override
		public int hashCode() {
			return m.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			return m.equals(obj);
		}

		public NiceMap(Map<X, Y> m) {
			this.m = m;
		}

		@Override
		public int size() {
			return m.size();
		}

		@Override
		public boolean isEmpty() {
			return m.isEmpty();
		}

		@Override
		public boolean containsKey(Object key) {
			return m.containsKey(key);
		}

		@Override
		public boolean containsValue(Object value) {
			return m.containsKey(value);
		}

		@Override
		public Y get(Object key) {
			assertNotNull(key);
			Y y = m.get(key);
			// assertNotNull(y);
			return y;
		}

		@Override
		public Y put(X key, Y value) {
			Y r = m.put(key, value);
			if (r != null && !r.equals(value)) {
				anomaly();
			}
			return r;
		}

		@Override
		public Y remove(Object key) {
			return m.remove(key);
		}

		@Override
		public void putAll(Map<? extends X, ? extends Y> m) {
			for (X x : m.keySet()) {
				put(x, m.get(x));
			}
		}

		@Override
		public void clear() {
			m.clear();
		}

		@Override
		public Set<X> keySet() {
			return m.keySet();
		}

		@Override
		public Collection<Y> values() {
			return m.values();
		}

		@Override
		public Set<Entry<X, Y>> entrySet() {
			return m.entrySet();
		}

	};

	public static <X, Y> Map<X, Y> mk() {
		Map<X, Y> m = (new THashMap<>());
		return new NiceMap<>(m);
	}

}
