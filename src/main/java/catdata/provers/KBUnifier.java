package catdata.provers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import catdata.Pair;
import gnu.trove.map.hash.THashMap;

/**
 * 
 * @author Ryan Wisnesky
 *
 *         Naive implementation of first-order unification.
 *
 * @param <C> type of constant/function symbols
 * @param <V> type of variables
 */
public class KBUnifier<C, V> {

	public static <C, V> Map<V, KBExp<C, V>> findSubst(KBExp<C, V> s, KBExp<C, V> t) {
		try {
			return findSubst0(Collections.singletonList(new Pair<>(s, t)), new THashMap<>());
		} catch (OkExn ex) {
			return null;
		}

	}

	static class OkExn extends Exception {

		private static final long serialVersionUID = 1L;

	}

	public static <C, V> Map<V, KBExp<C, V>> findSubst0(List<Pair<KBExp<C, V>, KBExp<C, V>>> l, Map<V, KBExp<C, V>> m)
			throws OkExn { // throws InterruptedException {
		if (l.isEmpty()) {
			return m;
		}
		Pair<KBExp<C, V>, KBExp<C, V>> p = l.get(0);
		KBExp<C, V> s = p.first;
		KBExp<C, V> t = p.second;

		if (s.isVar()) {
			V v = s.getVar();
			if (!m.containsKey(v)) {
				m.put(v, t);
				return findSubst0(l.subList(1, l.size()), m);
			}
			if (!m.get(v).equals(t)) {
				throw new OkExn();
			}
			return findSubst0(l.subList(1, l.size()), m);
		}
		if (t.isVar()) {
			throw new OkExn();
		}
		if (!s.f().equals(t.f())) {
			throw new OkExn();
		}
		List<Pair<KBExp<C, V>, KBExp<C, V>>> ret = new ArrayList<>(s.getArgs().size() + l.size() - 1);
		Iterator<KBExp<C, V>> it = s.getArgs().iterator();
		Iterator<KBExp<C, V>> jt = t.getArgs().iterator();
		while (it.hasNext()) {
			ret.add(new Pair<>(it.next(), jt.next()));
		}
		ret.addAll(l.subList(1, l.size()));
		return findSubst0(ret, m);
	}

	public static <C, V> Map<V, KBExp<C, V>> unify0(KBExp<C, V> s, KBExp<C, V> t) { // throws InterruptedException {
		if (s.isVar()) {
			V v = s.getVar();
			if (!t.isVar() && t.getVars().contains(v)) {
				return null; // occurs check failed
			}
			return singleton(v, t);
		}
		if (t.isVar()) {
			V v = t.getVar();
			if (s.getVars().contains(v)) {
				return null; // occurs check failed
			}
			return singleton(v, s);
		}
		if (!s.f().equals(t.f())) {
			return null;
		}
		Map<V, KBExp<C, V>> ret = new THashMap<>();
		Iterator<KBExp<C, V>> it1 = s.getArgs().iterator();
		Iterator<KBExp<C, V>> it2 = t.getArgs().iterator();
		while (it1.hasNext()) {
			Map<V, KBExp<C, V>> m = unify0(it1.next().substitute(ret), it2.next().substitute(ret));
			if (m == null) {
				return null;
			}
			if (andThen(ret, m) == null) {
				return null;
			}
		}

		return ret;
	}

	private static <C, V> Map<V, KBExp<C, V>> singleton(V v, KBExp<C, V> t) {
		return Collections.singletonMap(v, t);
	}

	public static <C, V> Map<V, KBExp<C, V>> andThen(Map<V, KBExp<C, V>> ret, Map<V, KBExp<C, V>> t) {
		ret.replaceAll((k, v) -> v.substitute(t));
		for (V k : t.keySet()) {
			if (!ret.containsKey(k)) {
				ret.put(k, t.get(k));
			}
		}
		return ret;
	}

}
