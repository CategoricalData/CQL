package catdata.provers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.apache.commons.collections4.list.TreeList;

import catdata.Chc;
import catdata.Pair;
import catdata.Quad;
import catdata.Triple;
import catdata.Util;
import catdata.graph.DAG;
import catdata.graph.PreOrder;
import catdata.graph.UnionFind;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

/**
 * 
 * @author Ryan Wisnesky
 *
 *         Implements "unfailing" aka "ordered" Knuth-Bendix completion.
 * 
 *         Handles empty sorts correctly.
 * 
 *         Will not orient var = const.
 * 
 *         Added special support for associative and commutative theories as
 *         described in "On Using Ground Joinable Equations in Equational
 *         Theorem Proving" (both semantic and syntactic methods)
 * 
 *         E-reduction instantiates free variables with a minimal constant, as
 *         described in "Decision Problems in Ordered Rewriting". This is
 *         necessary to use only-ground-complete systems as decision procedures
 *         via herbrandization. This means E-reduction is LPO specific. (note
 *         June 5, 2018 - this is almost certainly unsound. For example, two
 *         different variables would get instantiated to the same minimal
 *         constant. The paper cited gives only one example where this is sound,
 *         it doesn't say the procedure is always sound.)
 * 
 * @param <C> the type of functions/constants (should be comparable, or silent
 *        errors occur)
 * @param <V> the type of variables (should be comparable, or silent errors
 *        occur)
 * @param <T> the type of types
 * 
 * 
 */
public class LPOUKB<T, C, V> extends DPKB<T, C, V> {

	static <T, C, V> Pair<KBExp<C, V>, KBExp<C, V>> freshen(KBTheory<T, C, V> kb, Iterator<V> fresh,
			Pair<KBExp<C, V>, KBExp<C, V>> eq) {
		Map<V, KBExp<C, V>> subst = freshenMap(kb, fresh, eq).first;
		return new Pair<>(eq.first.substitute(subst), eq.second.substitute(subst));
	}

	private static <T, C, V> Pair<Map<V, KBExp<C, V>>, Map<V, KBExp<C, V>>> freshenMap(KBTheory<T, C, V> kb,
			Iterator<V> fresh, Pair<KBExp<C, V>, KBExp<C, V>> eq) {
		Set<V> vars = (new THashSet<>());
		KBExp<C, V> lhs = eq.first;
		KBExp<C, V> rhs = eq.second;
		lhs.vars(vars);
		rhs.vars(vars);
//		vars.addAll(lhs.vars());
//		vars.addAll(rhs.vars());
		Map<V, KBExp<C, V>> subst = (new THashMap<>());
		// Map<V, KBExp<C, V>> subst_inv = new THashMap<>();
		for (V v : vars) {
			V fr = fresh.next();
			subst.put(v, kb.factory.KBVar(fr));
			// subst_inv.put(fr, kb.factory.KBVar(v));
		}
		return new Pair<>(subst, null /* subst_inv */);
	}

	private void inhabGen(Set<T> inhabited) {
		while (inhabGen1(inhabited))
			;

	}

	private boolean inhabGen1(Set<T> ret) {
		boolean changed = false;
		outer: for (C c : kb.syms.keySet()) {
			for (T t : kb.syms.get(c).first) {
				if (!ret.contains(t)) {
					continue outer;
				}
			}
			changed = changed | ret.add(kb.syms.get(c).second);
		}
		return changed;
	}

	private final Set<T> groundInhabited = (new THashSet<>());

	private final List<C> prec;

	private boolean isComplete = false;
	private boolean isCompleteGround = false;

	// order matters
	private List<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> E;
	private final List<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> R, G;

	private final Iterator<V> fresh;

	private final Set<Pair<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>, Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>>> seen = new THashSet<>();

	private Map<C, List<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>>> AC_symbols;

	private final KBOptions options;

	// private final Map<T, C> min = new THashMap<>();

	public LPOUKB(Collection<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> E0, Iterator<V> fresh,
			Collection<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> R0, KBOptions options, List<C> prec,
			KBTheory<T, C, V> kb) {
		super(kb);
		this.options = options;
		this.prec = prec;
		R = new ArrayList<>(2 * R0.size());
		for (Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> r : R0) {
			R.add(freshen(fresh, new Triple<>((r.first), (r.second), r.third)));
		}
		this.fresh = fresh;
		E = new ArrayList<>(2 * E0.size());
		G = new ArrayList<>(2 * E0.size());
		for (Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> x : E0) {
			E.add(x);
			G.add(x);
		}
//		for (Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> e : E0) {
//			E.add(freshen(fresh, new Triple<>(e.first, e.second, e.third)));
//			G.add(freshen(fresh, new Triple<>(e.first, e.second), e.third)));
//		}
		initAC();
		/*
		 * for (T t : kb.tys) { V v = fresh.next(); min.put(t, Chc.inLeft(v)); }
		 */
		inhabGen(groundInhabited);
		for (C f : AC_symbols.keySet()) {
			List<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> lx = AC_symbols.get(f);
			R.add(lx.get(0));
			G.addAll(lx.subList(1, 5));
		}

		complete();
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private void initAC() {
		if (!options.semantic_ac) {
			return;
		}
		AC_symbols = new THashMap<>();
		outer: for (C f : kb.syms.keySet()) {
			if (kb.syms.get(f).first.size() != 2) {
				continue;
			}
			T t1 = kb.syms.get(f).first.get(0);
			T t2 = kb.syms.get(f).first.get(1);
			T t3 = kb.syms.get(f).second;
			if (!(t1.equals(t2) && t2.equals(t3))) {
				continue;
			}
			boolean cand1_found = false;
			boolean cand2_found = false;
			List<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> cands = AC_E(f);
			Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> cand1 = cands.get(0);
			Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> cand2 = cands.get(1);
			for (Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> other : E) {
				if (subsumes(cand1, other) || subsumes(cand1, other.reverse12())) {
					cand1_found = true;
				}
				if (subsumes(cand2, other) || subsumes(cand2, other.reverse12())) {
					cand2_found = true;
				}
				if (cand1_found && cand2_found) {
					List<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> l = Collections
							.synchronizedList(new LinkedList<>());
					l.add(AC_E(f).get(1)); // assoc rewrite rule
					l.add(AC_E(f).get(0)); // comm eq
					l.addAll(AC_E0(f)); // perm eqs
					AC_symbols.put(f, l);
					continue outer;
				}
			}
		}
	}

	private KBExp<C, V> achelper(C f, V xx, V yy, V zz) {
		KBExp<C, V> x = kb.factory.KBVar(xx);
		KBExp<C, V> y = kb.factory.KBVar(yy);
		KBExp<C, V> z = kb.factory.KBVar(zz);
		List<KBExp<C, V>> yz = (new LinkedList<>());
		yz.add(y);
		yz.add(z);
		List<KBExp<C, V>> xfyz = (new LinkedList<>());
		xfyz.add(x);
		xfyz.add(kb.factory.KBApp(f, yz));
		return kb.factory.KBApp(f, xfyz);
	}

	private List<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> AC_E0(C f) {
		List<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> ret = (new LinkedList<>());
		V x = fresh.next();
		V y = fresh.next();
		V z = fresh.next();
		Map<V, T> ctx = (new THashMap<>());
		T t = kb.syms.get(f).second;
		ctx.put(x, t);
		ctx.put(y, t);
		ctx.put(z, t);

		ret.add(freshen(fresh, new Triple<>(achelper(f, x, y, z), achelper(f, y, x, z), ctx)));
		ret.add(freshen(fresh, new Triple<>(achelper(f, x, y, z), achelper(f, z, y, x), ctx)));
		ret.add(freshen(fresh, new Triple<>(achelper(f, x, y, z), achelper(f, y, z, x), ctx)));

		return ret;
	}

	private List<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> AC_E(C f) {
		List<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> ret = (new LinkedList<>());
		V v1 = fresh.next();
		V v2 = fresh.next();
		KBExp<C, V> x = kb.factory.KBVar(v1);
		KBExp<C, V> y = kb.factory.KBVar(v2);
		List<KBExp<C, V>> xy = (new LinkedList<>());
		xy.add(x);
		xy.add(y);
		List<KBExp<C, V>> yx = (new LinkedList<>());
		yx.add(y);
		yx.add(x);
		Map<V, T> ctx = (new THashMap<>());
		T t = kb.syms.get(f).second;
		ctx.put(v1, t);
		ctx.put(v2, t);
		ret.add(new Triple<>(kb.factory.KBApp(f, xy), kb.factory.KBApp(f, yx), ctx));

		v1 = fresh.next();
		v2 = fresh.next();
		x = kb.factory.KBVar(v1);
		y = kb.factory.KBVar(v2);
		V v3 = fresh.next();
		KBExp<C, V> z = kb.factory.KBVar(v3);
		List<KBExp<C, V>> yz = (new LinkedList<>());
		yz.add(y);
		yz.add(z);
		xy = (new LinkedList<>());
		xy.add(x);
		xy.add(y);
		List<KBExp<C, V>> xfyz = (new LinkedList<>());
		xfyz.add(x);
		xfyz.add(kb.factory.KBApp(f, yz));
		List<KBExp<C, V>> fxyz = (new LinkedList<>());
		fxyz.add(kb.factory.KBApp(f, xy));
		fxyz.add(z);
		ctx = (new THashMap<>());
		ctx.put(v1, t);
		ctx.put(v2, t);
		ctx.put(v3, t);
		ret.add(new Triple<>(kb.factory.KBApp(f, fxyz), kb.factory.KBApp(f, xfyz), ctx));

		return ret;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> freshen(Iterator<V> fresh,
			Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> eq) {
		Quad<Map<V, KBExp<C, V>>, Map<V, KBExp<C, V>>, Map<V, V>, Map<V, V>> xxx = freshenMap(fresh, eq);
		Map<V, KBExp<C, V>> subst = xxx.first;
		return new Triple<>(eq.first.substitute(subst), eq.second.substitute(subst), subst(eq.third, xxx.third));
	}

	private static <V, T> Map<V, T> subst(Map<V, T> m, Map<V, V> subst) {
		Map<V, T> ret = (new THashMap<>());

		for (V v : m.keySet()) {
			V vv = subst.get(v);
			if (vv == null) {
				ret.put(v, m.get(v));
			} else {
				ret.put(vv, m.get(v));
			}
		}

		return ret;
	}

	private Quad<Map<V, KBExp<C, V>>, Map<V, KBExp<C, V>>, Map<V, V>, Map<V, V>> freshenMap(Iterator<V> fresh,
			Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> eq) {
		Set<V> vars = new THashSet<>();
		KBExp<C, V> lhs = eq.first;
		KBExp<C, V> rhs = eq.second;
		vars.addAll(lhs.getVars());
		vars.addAll(rhs.getVars());
		Map<V, KBExp<C, V>> subst = (new THashMap<>());
		Map<V, KBExp<C, V>> subst_inv = (new THashMap<>());
		Map<V, V> subst1 = (new THashMap<>());
		Map<V, V> subst_inv1 = (new THashMap<>());
		for (V v : vars) {
			V fr = fresh.next();
			subst.put(v, kb.factory.KBVar(fr));
			subst_inv.put(fr, kb.factory.KBVar(v));
			subst1.put(v, fr);
			subst_inv1.put(fr, v);
		}
		return new Quad<>(subst, subst_inv, subst1, subst_inv1);
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static <X> void remove(Collection<X> X, X x) {
		while (X.remove(x))
			;
	}

	private static <X> void add(Collection<X> X, X x) {
		if (!X.contains(x)) {
			X.add(x);
		}
	}

	private static <X> void addFront(List<X> X, X x) {
		if (!X.contains(x)) {
			X.add(0, x);
		}
	}

	private static <X> void addAll(Collection<X> X, Collection<X> x) {
		for (X xx : x) {
			add(X, xx);
		}
	}

	private void sortByStrLen(List<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> l) {
		if (options.unfailing) {
			List<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> unorientable = (new ArrayList<>(l.size()));
			List<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> orientable = (new ArrayList<>(l.size()));
			for (Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> k : l) {
				if (orientable(k)) {
					orientable.add(k);
				} else {
					unorientable.add(k);
				}
			}
			orientable.sort((x, y) -> {
				return (x.first.size() + x.second.size()) - (y.first.size() + y.second.size());
			});
			l.clear();
			l.addAll(orientable);
			l.addAll(unorientable);
		} else {
			l.sort((x, y) -> {
				return (x.first.size() + x.second.size()) - (y.first.size() + y.second.size());
			});
		}
	}

	private void checkParentDead() {
		if (Thread.currentThread().isInterrupted()) {
			throw new RuntimeException("Precedence tried: " + prec);
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private void complete() {
		while (!step())
			;

		if (!isCompleteGround) {
			throw new RuntimeException("Not ground complete after iteration timeout.  Last state:\n\n" + toString());
		}
	}

	private boolean subsumes(Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> cand,
			Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> other) {
		return (subsumes0(cand, other) != null);
	}

	private Map<V, KBExp<C, V>> subsumes0(Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> cand,
			Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> other) {
		// if (Thread.interrupted()) {
		// throw new InterruptedException();
		// }
		Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> candX = cand;

		if (!Collections.disjoint(candX.first.getVars(), other.first.getVars())
				|| !Collections.disjoint(candX.first.getVars(), other.second.getVars())
				|| !Collections.disjoint(candX.second.getVars(), other.first.getVars())
				|| !Collections.disjoint(candX.second.getVars(), other.second.getVars())) {
			// System.out.println(cand + " and " + other);
			// Util.anomaly();
			candX = freshen(fresh, cand);
		}

		Map<V, KBExp<C, V>> subst = findSubst(other.first, cand.first, other.third,
				Util.union(groundInhabited, cand.third.values()));
		if (subst == null) {
			return null;
		}

		Map<V, KBExp<C, V>> subst2 = findSubst(other.second.substitute(subst), cand.second.substitute(subst),
				other.third, Util.union(groundInhabited, cand.third.values()));
		if (subst2 == null) {
			return null;
		}

		return KBUnifier.andThen(subst, subst2);
	}

	private List<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> filterSubsumed(
			Collection<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> CPX) {
		List<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> CP = (new LinkedList<>());
		outer: for (Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> cand : CPX) {
			for (Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> e : E) {
				if (subsumes(cand, e)) {
					continue outer;
				}
			}
			CP.add(cand);
		}
		return CP;
	}

	private List<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> filterSubsumedBySelf(
			Collection<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> CPX) {
		List<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> CP = new LinkedList<>(CPX);

		Iterator<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> it = CP.iterator();
		while (it.hasNext()) {
			Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> cand = it.next();
			for (Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> e : CP) {
				if (cand.equals(e)) {
					continue;
				}
				if (subsumes(cand, e)) {
					it.remove();
					break;
				}
				if (subsumes(cand.reverse12(), e)) {
					it.remove();
					break;
				}
				if (subsumes(cand, e.reverse12())) {
					it.remove();
					break;
				}
				// TODO: aql this one redundant?
				// if (subsumes(cand.reverse12(), e.reverse12())) {
				// it.remove();
				// break;
				// }
			}
		}
		return CP;
	}

	// is also compose2
	// simplify RHS of a rule
	private void compose() {
		Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> to_remove;
		Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> to_add;
		do {
			to_remove = null;
			to_add = null;
			for (Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> r : R) {
				Set<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> R0 = new THashSet<>(R);
				R0.remove(r);
				KBExp<C, V> new_rhs = red(this::gtX, null, Util.append(E, G), R0, r.second, r.third.values());
				if (!new_rhs.equals(r.second)) {
					to_remove = r;
					to_add = new Triple<>(r.first, new_rhs, r.third);
					break;
				}
			}
			if (to_remove != null) {
				R.remove(to_remove);
				R.add(to_add);
			}
		} while (to_remove != null);
	}

	// TODO: aql caching might be unsound here - reactivate if possible
	private KBExp<C, V> red(BiFunction<Chc<V, C>, Chc<V, C>, Boolean> gt,
			@SuppressWarnings("unused") Map<KBExp<C, V>, KBExp<C, V>> cache,
			Collection<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> Ex,
			Collection<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> Ry, KBExp<C, V> e, Collection<T> inhab0) {
		Set<T> inhab = Util.union(inhab0, groundInhabited);
		inhabGen(inhab);

		while (true) {
			KBExp<C, V> e0 = step(gt, null, Ex, Ry, e, inhab);
			if (e.equals(e0)) {
				// if (cache != null) {
				// cache.put(e0, e);
				// }
				return e0;
			}
			e = e0;
		}
	}

	private KBExp<C, V> step(BiFunction<Chc<V, C>, Chc<V, C>, Boolean> gt, Map<KBExp<C, V>, KBExp<C, V>> cache,
			Collection<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> E,
			Collection<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> R, KBExp<C, V> ee, Set<T> inhab) {
		if (ee.isVar()) {
			return step1(gt, cache, E, R, ee, inhab);
		}
		KBExp<C, V> e = ee;
		List<KBExp<C, V>> args0 = new ArrayList<>(e.getArgs().size());
		for (KBExp<C, V> arg : e.getArgs()) {
			args0.add(step(gt, cache, E, R, arg, inhab)); // must be step
		}
		KBExp<C, V> ret = kb.factory.KBApp(e.f(), args0);
		return step1(gt, cache, E, R, ret, inhab);

	}

	private Map<V, KBExp<C, V>> findSubst(KBExp<C, V> lhs, KBExp<C, V> e, Map<V, T> lhsCtx, Collection<T> allinhab) {
		Map<V, KBExp<C, V>> s = KBUnifier.findSubst(lhs, e);
		if (s == null || !applies(lhsCtx, s, allinhab)) {
			return null;
		}
		return s;
	}

	private KBExp<C, V> step1(BiFunction<Chc<V, C>, Chc<V, C>, Boolean> gt,
			@SuppressWarnings("unused") Map<KBExp<C, V>, KBExp<C, V>> cache,
			Collection<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> E,
			Collection<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> R, KBExp<C, V> e0, Set<T> inhab) {
		KBExp<C, V> e = e0;
		/*
		 * if (cache != null && cache.containsKey(e)) { return cache.get(e); }
		 */
		for (Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> r0 : R) {
			checkParentDead();
			Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> r = r0;
			if (!Collections.disjoint(r.first.getVars(), e.getVars())
					|| !Collections.disjoint(r.second.getVars(), e.getVars())) {
				// System.out.println(r + " and " + e);
				// Util.anomaly();
				r = freshen(fresh, r0);
			}

			KBExp<C, V> lhs = r.first;
			KBExp<C, V> rhs = r.second;
			Map<V, KBExp<C, V>> s = findSubst(lhs, e, r.third, inhab);

			if (s == null) {
				continue;
			}

			e = rhs.substitute(s);
		}
		e = step1Es(gt, E, e, inhab);

		return e;
	}

	private boolean applies(Map<V, T> ruleCtx, Map<V, KBExp<C, V>> subst, Collection<T> inhab) {
		Set<T> need = new THashSet<>();
		for (V ruleVar : ruleCtx.keySet()) {
			if (!subst.containsKey(ruleVar)) {
				need.add(ruleCtx.get(ruleVar));
			}
		}
		return inhab.containsAll(need);
	}

	private KBExp<C, V> step1Es(BiFunction<Chc<V, C>, Chc<V, C>, Boolean> gt,
			Collection<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> E, KBExp<C, V> e, Collection<T> inhab) {
		if (options.unfailing && e.getVars().isEmpty()) {
			for (Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> r0 : E) {
				KBExp<C, V> a = step1EsX(gt, r0, e, inhab);
				if (a != null) {
					e = a;
				}
				KBExp<C, V> b = step1EsX(gt, r0.reverse12(), e, inhab);
				if (b != null) {
					e = b;
				}
			}
		}
		return e;
	}

	private KBExp<C, V> step1EsX(BiFunction<Chc<V, C>, Chc<V, C>, Boolean> gt,
			Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> r0, KBExp<C, V> e, Collection<T> inhab) {
		Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> r = r0;
		// if (!Collections.disjoint(r.first.vars(), e.vars()) ||
		// !Collections.disjoint(r.second.vars(), e.vars())) {
		// Util.anomaly();
		// r = freshen(fresh, r0);
		// }

		KBExp<C, V> lhs = r.first;
		KBExp<C, V> rhs = r.second;

		Map<V, KBExp<C, V>> s0 = findSubst(lhs, e, r.third, inhab);
		if (s0 == null) {
			return null;
		}
		// Map<V, KBExp<C, V>> s = new THashMap<>(s0);

		KBExp<C, V> lhs0 = lhs.substitute(s0);
		KBExp<C, V> rhs0 = rhs.substitute(s0);

		if (gt_lpo(gt, lhs0, rhs0)) {
			return rhs0;
		}
		return null;
	}

	//////////////////////////////////////////////////////////////////////////////////////////////

	private Set<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> allcps2(
			Set<Pair<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>, Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>>> seen,
			Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> ab) {
		Set<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> ret = (new THashSet<>());

		Set<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> E0 = (new THashSet<>(E));
		E0.add(ab);
		E0.add(ab.reverse12());
		Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> ba = ab.reverse12();
		for (Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> gd : E0) {
			// if (Thread.currentThread().isInterrupted()) {
			// throw new InterruptedException();
			// }
			Set<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> s;
			Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> dg = gd.reverse12();

			if (!seen.contains(new Pair<>(ab, gd))) {
				s = cp(ab, gd);
				ret.addAll(s);
				seen.add(new Pair<>(ab, gd));
			}
			if (!seen.contains(new Pair<>(gd, ab))) {
				s = cp(gd, ab);
				ret.addAll(s);
				seen.add(new Pair<>(gd, ab));
			}
			if (!seen.contains(new Pair<>(ab, dg))) {
				s = cp(ab, dg);
				ret.addAll(s);
				seen.add(new Pair<>(ab, dg));
			}
			if (!seen.contains(new Pair<>(dg, ab))) {
				s = cp(dg, ab);
				ret.addAll(s);
				seen.add(new Pair<>(dg, ab));
			}
			////
			if (!seen.contains(new Pair<>(ba, gd))) {
				s = cp(ba, gd);
				ret.addAll(s);
				seen.add(new Pair<>(ba, gd));
			}
			if (!seen.contains(new Pair<>(gd, ba))) {
				s = cp(gd, ba);
				ret.addAll(s);
				seen.add(new Pair<>(gd, ba));
			}
			if (!seen.contains(new Pair<>(ba, dg))) {
				s = cp(ba, dg);
				ret.addAll(s);
				seen.add(new Pair<>(ba, dg));
			}
			if (!seen.contains(new Pair<>(dg, ba))) {
				s = cp(dg, ba);
				ret.addAll(s);
				seen.add(new Pair<>(dg, ba));
			}
		}

		for (Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> gd : R) {
			Set<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> s;

			if (!seen.contains(new Pair<>(ab, gd))) {
				s = cp(ab, gd);
				ret.addAll(s);
				seen.add(new Pair<>(ab, gd));
			}
			if (!seen.contains(new Pair<>(gd, ab))) {
				s = cp(gd, ab);
				ret.addAll(s);
				seen.add(new Pair<>(gd, ab));
			}
			////
			if (!seen.contains(new Pair<>(ba, gd))) {
				s = cp(ba, gd);
				ret.addAll(s);
				seen.add(new Pair<>(ba, gd));
			}
			if (!seen.contains(new Pair<>(gd, ba))) {
				s = cp(gd, ba);
				ret.addAll(s);
				seen.add(new Pair<>(gd, ba));
			}
		}
		return ret;
	}

	private Set<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> allcps(
			Set<Pair<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>, Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>>> seen,
			Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> ab) {
		Set<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> ret = new THashSet<>();
		for (Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> gd : R) {
			checkParentDead();
			Set<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> s;
			if (!seen.contains(new Pair<>(ab, gd))) {
				s = cp(ab, gd);
				ret.addAll(s);
				seen.add(new Pair<>(ab, gd));
			}

			if (!seen.contains(new Pair<>(gd, ab))) {
				s = cp(gd, ab);
				ret.addAll(s);
				seen.add(new Pair<>(gd, ab));
			}
		}
		return ret;
	}

	private Set<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> cp(Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> gd0,
			Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> ab0) {
		// if (Thread.currentThread().isInterrupted()) {
		// throw new InterruptedException();
		// }
		Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> ab = freshen(fresh, ab0);
		Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> gd = freshen(fresh, gd0);

		Set<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, KBExp<C, V>>>> retX = gd.first.cp(new LinkedList<>(), ab.first,
				ab.second, gd.first, gd.second);

		Set<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> ret = new THashSet<>();
		for (Triple<KBExp<C, V>, KBExp<C, V>, Map<V, KBExp<C, V>>> c : retX) {
			// ds !>= gs
			KBExp<C, V> gs = gd.first.substitute(c.third);
			KBExp<C, V> ds = gd.second.substitute(c.third);
			if ((gt_lpo(this::gtX, ds, gs)) || gs.equals(ds)) {
				continue;
			}
			// bs !>= as
			KBExp<C, V> as = ab.first.substitute(c.third);
			KBExp<C, V> bs = ab.second.substitute(c.third);
			if ((gt_lpo(this::gtX, bs, as)) || as.equals(bs)) {
				continue;
			}
			Map<V, T> newCtx = new THashMap<>();
			Util.putAllSafely(newCtx, ab.third);
			Util.putAllSafely(newCtx, gd.third);
			min(newCtx, c.first, c.second);
			Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> toAdd = new Triple<>(c.first, c.second, newCtx);
			ret.add(toAdd);
		}

		return ret;
	}

	////////////////////////////////////////////////////////////////////////////

	private void min(Map<V, T> ctx, KBExp<C, V> lhs, KBExp<C, V> rhs) {
		Iterator<V> it = ctx.keySet().iterator();
		while (it.hasNext()) {
			V v = it.next();
			if (lhs.getVars().contains(v) || rhs.getVars().contains(v)) {
				continue;
			}
			if (groundInhabited.contains(ctx.get(v))) {
				it.remove();
			}
		}

	}

	// simplifies equations
	// can also use E U G with extra checking
	private void simplify() {
		Map<KBExp<C, V>, KBExp<C, V>> cache = new THashMap<>();

		List<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> newE = new LinkedList<>();
		Set<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> newE2 = new THashSet<>();
		for (Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> e : E) {
			KBExp<C, V> lhs_red = red(this::gtX, cache, new LinkedList<>(), R, e.first, e.third.values());
			KBExp<C, V> rhs_red = red(this::gtX, cache, new LinkedList<>(), R, e.second, e.third.values());
			if (!lhs_red.equals(rhs_red)) {
				Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> p = new Triple<>(lhs_red, rhs_red, e.third);
				if (!newE2.contains(p)) {
					newE.add(p);
					newE2.add(p);
				}
			}
		}
		E = newE;
	}

	private static <X> List<List<X>> allSubsetsOrderedBySize(Set<X> set) {
		List<List<X>> ret = new LinkedList<>(
				Util.powerSet(set).stream().map(x -> new LinkedList<>(x)).collect(Collectors.toList()));
		ret.sort((x, y) -> x.size() > y.size() ? 1 : x.size() == y.size() ? 0 : -1);
		return ret;
	}

	private boolean strongGroundJoinableSyntactic(KBExp<C, V> s, KBExp<C, V> t, Map<V, T> ctx) {
		outer: for (List<V> vars : allSubsetsOrderedBySize(ctx.keySet())) {
			if (vars.size() > 3) { // TODO aql just use up to 3 vars for now. 4 takes almost 5 seconds to compute
				continue;
			}
			Collection<PreOrder<V>> cands = PreOrder.allTotal(vars);
			for (PreOrder<V> cand : cands) {
				BiFunction<Chc<V, C>, Chc<V, C>, Boolean> r = wrapPreorderSk(cand, lift(prec));
				Map<V, KBExp<C, V>> m = new THashMap<>();
				UnionFind<V> uf = new UnionFind<>(ctx.keySet().size(), ctx.keySet());
				for (V v : vars) {
					for (V v2 : vars) {
						if (cand.gte(v, v2) && cand.gte(v2, v)) {
							uf.union(v, v2);
						}
					}
				}
				for (V v : vars) {
					m.put(v, kb.factory.KBVar(uf.find(v)));
				}

				KBExp<C, V> s0 = red(r, null, Util.union(E, G), R, s.substitute(m), ctx.values());
				KBExp<C, V> t0 = red(r, null, Util.union(E, G), R, t.substitute(m), ctx.values());

				if (!s0.equals(t0)) {
					continue outer;
				}
			}
			return true;
		}
		return false;
	}

	private static <X, V> BiFunction<Chc<V, X>, Chc<V, X>, Boolean> wrapPreorderSk(PreOrder<V> p,
			BiFunction<X, X, Boolean> gt) {
		return (lhs, rhs) -> {
			if (lhs.equals(rhs)) {
				return false;
			} else if (lhs.left && rhs.left) {
				return p.gte(lhs.l, rhs.l); // && !p.gte(rhs.l, lhs.l);
			} else if (!lhs.left && !rhs.left) {
				return gt.apply(lhs.r, rhs.r);
			}
			return false;
		};
	}

	private boolean strongGroundJoinable(BiFunction<Chc<V, C>, Chc<V, C>, Boolean> gt, KBExp<C, V> s, KBExp<C, V> t,
			Map<V, T> ctx) {
		List<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> R0 = new TreeList<>();

		if (!s.equals(red(gt, null, G, R0, s, ctx.values()))) {
			return false;
		}
		if (!t.equals(red(gt, null, G, R0, t, ctx.values()))) {
			return false;
		}
		for (Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> e : E) { // TODO aql was E0 = empty
			Map<V, KBExp<C, V>> m = subsumes0(new Triple<>(s, t, ctx), e);
			if (m == null) {
				m = subsumes0(new Triple<>(t, s, ctx), e);
			}
			if (m == null) {
				m = subsumes0(new Triple<>(s, t, ctx), e.reverse12());
			}
			if (m == null) {
				m = subsumes0(new Triple<>(s, t, ctx), e.reverse12());
			}
			if (m == null) {
				continue;
			}
			return false;
		}

		return eqUpToSorting(s, t, ctx.values());
	}

	// is not collapse2
	// can also use E U G here
	private void collapseBy(Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> ab) {
		Set<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> AB = Collections.singleton(ab);
		Iterator<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> it = R.iterator();
		while (it.hasNext()) {
			Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> r = it.next();
			if (r.equals(ab)) {
				continue;
			}
			KBExp<C, V> lhs = red(this::gtX, null, new LinkedList<>(), AB, r.first, r.third.values());
			if (!r.first.equals(lhs)) {
				addFront(E, new Triple<>(lhs, r.second, r.third));
				it.remove();
			}
		}
	}

	private Collection<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> reduce(
			Collection<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> set) {
		Set<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> p = new THashSet<>();
		for (Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> e : set) {
			KBExp<C, V> lhs = red(this::gtX, new THashMap<>(), Util.append(E, G), R, e.first, e.third.values());
			KBExp<C, V> rhs = red(this::gtX, new THashMap<>(), Util.append(E, G), R, e.second, e.third.values());
			if (lhs.equals(rhs)) {
				continue;
			}
			p.add(new Triple<>(lhs, rhs, e.third));
		}
		return p;
	}

	// TODO: aql when filtering for subsumed, can also take G into account
	int i = 0;

	private boolean step() {
		if (i++ < 64) {
			// System.out.println(this);
			// System.out.println("----------------------------------");
		}
		checkParentDead();

		if (checkEmpty()) {
			return true;
		}

		Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> st = pick(E);

		KBExp<C, V> s0 = st.first;
		KBExp<C, V> t0 = st.second;
		KBExp<C, V> a, b;
		boolean oriented = false;
		if (gt_lpo(this::gtX, s0, t0)) {
			a = s0;
			b = t0;
			oriented = true;
		} else if (gt_lpo(this::gtX, t0, s0)) {
			a = t0;
			b = s0;
			oriented = true;
		} else if (s0.equals(t0)) {
			remove(E, st);
			return false; // in case x = x coming in
		} else {
			if (options.unfailing) {
				remove(E, st);
				add(E, st); // for sorting, will add to end of list
				a = s0;
				b = t0;
			} else {
				throw new RuntimeException("Unorientable: " + st.first + " = " + st.second);
			}
		}
		Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> ab = new Triple<>(a, b, st.third);
		if (oriented) {
			R.add(ab);
			List<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> CP = filterSubsumed(allcps(seen, ab));
			CP = filterSyn(filterSemAC(CP));
			addAll(E, CP);
			remove(E, st);
			collapseBy(ab);
		} else {
			List<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> CP = filterSubsumed(allcps(seen, ab));
			CP = filterSyn(filterSemAC(CP));
			CP.addAll(filterSubsumed(allcps(seen, ab.reverse12())));
			CP.addAll(filterSubsumed(allcps2(seen, ab)));
			CP.addAll(filterSubsumed(allcps2(seen, ab.reverse12())));
			addAll(E, CP);
		}

		checkParentDead();

		if (options.compose) {
			compose();
			checkParentDead();
		}

		simplify(); // needed for correctness
		checkParentDead();

		if (options.sort_cps) {
			sortByStrLen(E);
			checkParentDead();
		}

		if (options.filter_subsumed_by_self) {
			E = filterSubsumedBySelf(E);
			checkParentDead();
		}

		return false;
	}

	private List<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> filterSemAC(
			List<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> E) {
		if (!options.semantic_ac) {
			return E;
		}
		List<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> newE = new LinkedList<>();
		for (Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> st : E) {
			if (strongGroundJoinable(this::gtX, st.first, st.second, st.third)) {
				G.add(st);
			} else {
				newE.add(st);
			}
		}
		return newE;
	}

	private List<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> filterSyn(
			List<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> E) {
		if (!options.syntactic_ac) {
			return E;
		}
		List<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> newE = new LinkedList<>();
		for (Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> st : E) {
			if (strongGroundJoinableSyntactic(st.first, st.second, st.third)) {
				G.add(st);
			} else {
				newE.add(st);
			}
		}
		return newE;
	}

	private Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> pick(List<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> l) {
		for (Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> x : l) {
			if (orientable(x)) {
				return x;
			}
		}
		return l.get(0);
	}

	private boolean orientable(Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> e) {
		return orientable(this::gtX, e);
	}

	//
	private boolean orientable(BiFunction<Chc<V, C>, Chc<V, C>, Boolean> gt,
			Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> e) {
		return (gt_lpo(gt, e.first, e.second) || gt_lpo(gt, e.second, e.first));
	}

	private boolean checkEmpty() {
		if (E.isEmpty()) {
			isComplete = true;
			isCompleteGround = true;
			return true;
		}
		if (!allUnorientable()) {
			return false;
		}
		if ((options.semantic_ac && allCpsConfluent(false, true)) || allCpsConfluent(false, false)) {
			isComplete = false;
			isCompleteGround = true;
			return true;
		}

		return false;
	}

	private boolean allUnorientable() {
		for (Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> e : E) {
			if (orientable(e)) {
				return false;
			}
		}
		return true;
	}

	private boolean allCpsConfluent(boolean print, boolean ground) {
		for (Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> e : E) {
			List<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> set = filterSubsumed(
					reduce(allcps2(new THashSet<>(), e)));
			if (!allCpsConfluent(print, ground, "equation " + e, set)) {
				return false;
			}
		}
		for (Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> e : R) {
			List<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> set = filterSubsumed(reduce(allcps(new THashSet<>(), e)));
			if (!allCpsConfluent(print, ground, "rule" + e, set)) {
				return false;
			}
		}
		return true;
	}

	private boolean eqUpToSorting(KBExp<C, V> a, KBExp<C, V> b, Collection<T> inhab) {
		List<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> R0 = new ArrayList<>(AC_symbols.keySet().size());
		List<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> E0 = new LinkedList<>();

		for (C f : AC_symbols.keySet()) {
			List<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> lx = AC_symbols.get(f);
			R0.add(lx.get(0));
			E0.addAll(lx.subList(1, 5));
		}

		KBExp<C, V> s0 = red(this::gtX, null, E0, R0, a, inhab);
		KBExp<C, V> t0 = red(this::gtX, null, E0, R0, b, inhab);

		return s0.equals(t0);
	}

	private boolean allCpsConfluent(@SuppressWarnings("unused") boolean print, boolean ground,
			@SuppressWarnings("unused") String s, Collection<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> set) {
		outer: for (Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> e : set) {
			KBExp<C, V> lhs = red(this::gtX, new THashMap<>(), Util.append(E, G), R, e.first, e.third.values());
			KBExp<C, V> rhs = red(this::gtX, new THashMap<>(), Util.append(E, G), R, e.second, e.third.values());

			if (!lhs.equals(rhs)) {
				if (!ground) {
					return false;
				}
				for (Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> ex : G) {
					if (subsumes(new Triple<>(lhs, rhs, e.third), ex)
							|| subsumes(new Triple<>(rhs, lhs, e.third), ex)) {
						continue outer;
					}
				}
				if (options.semantic_ac && eqUpToSorting(e.first, e.second, e.third.values())) {
					continue;
				}
				if (options.syntactic_ac && strongGroundJoinableSyntactic(e.first, e.second, e.third)) {
					continue;
				}
				return false;
			}

		}
		return true;
	}

	@Override
	public String toString() {
		List<String> a = E.stream().map(x -> Util.sep(x.third, ":", " ") + " " + x.first + " = " + x.second)
				.collect(Collectors.toList());
		List<String> b = R.stream().map(x -> Util.sep(x.third, ":", " ") + " " + x.first + " -> " + x.second)
				.collect(Collectors.toList());

		return (Util.sep(a, "\n") + "\n" + Util.sep(b, "\n")).trim();
	}

	@Override
	public boolean eq(Map<V, T> ctx, KBExp<C, V> lhs, KBExp<C, V> rhs) {
		if (!ctx.isEmpty() && !isComplete) {
			throw new RuntimeException("System not complete, cannot decide non-ground equality");
		}
		return qnf(lhs, ctx).equals(qnf(rhs, ctx));
	}

	private KBExp<C, V> qnf(KBExp<C, V> e, Map<V, T> ctx) {
		// try {
		if (isComplete) {
			return red(this::gtX, null, Collections.emptyList(), R, e, ctx.values());
		} else if (isCompleteGround && ctx.isEmpty()) {
			return red(this::gtX, null, Util.append(E, G), R, e, ctx.values());
		}
		throw new RuntimeException("Not ground complete, cannot decide equations in contexts.");
		// } catch (InterruptedException e1) {
		// throw new RuntimeInterruptedException(e1);
		// }
	}

	/*
	 * public KBExp<C, V> inject(KBExp<C, V> x) { if (x.isVar()) { return (KBExp<C,
	 * V>) x; } List<KBExp<C, V>> new_args = new ArrayList<>(x.getArgs().size());
	 * for (KBExp<C, V> arg : x.getArgs()) { new_args.add(inject(arg)); } return
	 * kb2.factory.KBApp(Chc.inRight(x.f()), new_args); }
	 */
	//////////////////////////////////////////////////////////////////////////////////////////////////

	private static <X> BiFunction<X, X, Boolean> lift(List<X> prec) {
		return (x, y) -> {
			int i = prec.indexOf(x);
			int j = prec.indexOf(y);
			if (i == -1 || j == -1) {
				throw new RuntimeException("Anomaly: please report");
			}
			return i > j;
		};
	}

	// In entropic, if ((a o b) o (c o d)) rewrites to ((a o b) o d), the bug is
	// probably here
	private static <X, V> BiFunction<Chc<V, X>, Chc<V, X>, Boolean> wrapMinSk(BiFunction<X, X, Boolean> gt) {
		return (lhs, rhs) -> {
			if (lhs.equals(rhs)) {
				return false;
			}
			if (lhs.left && rhs.left) {
				return lhs.l.toString().compareTo(rhs.l.toString()) > 0;
			} else if (lhs.left) {
				return true;
			} else if (rhs.left) {
				return false;
			}

			return gt.apply(lhs.r, rhs.r);
		};
	}

	private boolean gtX(Chc<V, C> lhs, Chc<V, C> rhs) {
		BiFunction<Chc<V, C>, Chc<V, C>, Boolean> b = wrapMinSk(lift(prec));
		return b.apply(lhs, rhs);
	}

	/////

	private static <V, C> boolean gt_lpo(BiFunction<Chc<V, C>, Chc<V, C>, Boolean> gt, KBExp<C, V> s, KBExp<C, V> t) {
		return gt_lpo1(gt, s, t) || gt_lpo2(gt, s, t);
	}

	private static <V, C> boolean gt_lpo1(BiFunction<Chc<V, C>, Chc<V, C>, Boolean> gt, KBExp<C, V> s, KBExp<C, V> t) {
		if (s.isVar()) {
			return false;
		}
		for (KBExp<C, V> si : s.getArgs()) {
			if (si.equals(t) || gt_lpo(gt, si, t)) {
				return true;
			}
		}
		return false;
	}

	private static <V, C> boolean gt_lpo2(BiFunction<Chc<V, C>, Chc<V, C>, Boolean> gt, KBExp<C, V> S, KBExp<C, V> T) {
		if (S.isVar() || T.isVar()) {
			return false;
		}
		for (KBExp<C, V> ti : T.getArgs()) {
			if (!gt_lpo(gt, S, ti)) {
				return false;
			}
		}
		if (S.f().equals(T.f())) {
			return gt_lpo_lex(gt, S.getArgs(), T.getArgs());
		}
		return gt.apply(Chc.inRight(S.f()), Chc.inRight(T.f()));
	}

	private static <V, C> boolean gt_lpo_lex(BiFunction<Chc<V, C>, Chc<V, C>, Boolean> gt, List<KBExp<C, V>> ss,
			List<KBExp<C, V>> tt) {
		if (ss.size() != tt.size()) {
			throw new RuntimeException("Anomaly: please report");
		}
		if (ss.isEmpty()) {
			return false;
		}
		KBExp<C, V> s0 = ss.get(0), t0 = tt.get(0);
		return gt_lpo(gt, s0, t0)
				|| s0.equals(t0) && gt_lpo_lex(gt, ss.subList(1, ss.size()), tt.subList(1, tt.size()));
	}

	///////////////////////////////////////////////////////////////////////////////////////////////////
	// Constraint satisfaction problem for LPO orientability, used to infer
	/////////////////////////////////////////////////////////////////////////////////////////////////// precedences
	// http://www.jaist.ac.jp/~hirokawa/publications/03rta.pdf
	// "Tsukuba Termination Tool" Nao Hirokawa and Aart Middeldorp

	private static <X> Set<DAG<X>> tru() {
		return Collections.singleton(new DAG<>());
	}

	private static <X> Set<DAG<X>> fals() {
		return Collections.emptySet();
	}

	private static <X> Set<DAG<X>> and(Set<DAG<X>> a, Set<DAG<X>> b) {
		Set<DAG<X>> ret = new THashSet<>();
		for (DAG<X> x : a) {
			for (DAG<X> y : b) {
				DAG<X> xy = union(x, y);
				if (xy != null) {
					ret.add(xy);
				}
			}
		}
		return min(ret);
	}

	private static <X> DAG<X> union(DAG<X> a, DAG<X> b) {
		DAG<X> ret = new DAG<>();
		Set<X> xs = Util.union(a.vertices(), b.vertices());
		for (X x1 : xs) {
			for (X x2 : xs) {
				if (x1.equals(x2)) {
					continue;
				}
				if (a.hasPath(x1, x2) || b.hasPath(x1, x2)) {
					boolean added = ret.addEdge(x1, x2);
					if (!added) {
						return null;
					}
				}
			}
		}
		return ret;
	}

	private static <X> Set<DAG<X>> or(Set<DAG<X>> a, Set<DAG<X>> b) {
		return min(Util.union(a, b));
	}

	private static <X> Set<DAG<X>> min(Set<DAG<X>> a) {
		Set<DAG<X>> ret = new THashSet<>();
		for (DAG<X> x : a) {
			if (minimal(x, a)) {
				ret.add(x);
			}
		}
		return ret;
	}

	private static <X> boolean minimal(DAG<X> x, Set<DAG<X>> a) {
		for (DAG<X> aa : a) {
			if (!x.equals(aa) && lessThanOrEqual(aa, x)) {
				return false;
			}
		}
		return true;
	}

	private static <X> boolean lessThanOrEqual(DAG<X> a, DAG<X> b) {
		Set<X> xs = Util.union(a.vertices(), b.vertices());
		for (X x1 : xs) {
			for (X x2 : xs) {
				if (a.hasPath(x1, x2) && !b.hasPath(x1, x2)) {
					return false;
				}
			}
		}
		return true;
	}

	private static <X, V> Set<DAG<X>> eq(KBExp<X, V> s, KBExp<X, V> t) {
		if (s.equals(t)) {
			return tru();
		}
		return fals();
	}

	private static <X> Set<DAG<X>> gtInfer(X lhs, X rhs) {
		if (lhs.equals(rhs)) {
			throw new RuntimeException("Anomaly: please report");
		}
		DAG<X> d = new DAG<>();
		d.addEdge(lhs, rhs);
		return Collections.singleton(d);
	}

	private static <X, V> Set<DAG<X>> gt_lpoInfer(KBExp<X, V> s, KBExp<X, V> t) {
		return or(gt_lpo1Infer(s, t), gt_lpo2Infer(s, t));
	}

	private static <X, V> Set<DAG<X>> gt_lpo1Infer(KBExp<X, V> s, KBExp<X, V> t) {
		if (s.isVar()) {
			return fals();
		}
		Set<DAG<X>> ret = fals();
		for (KBExp<X, V> si : s.getArgs()) {
			ret = or(ret, eq(si, t));
			ret = or(ret, gt_lpoInfer(si, t));
		}
		return ret;
	}

	private static <X, V> Set<DAG<X>> gt_lpo2Infer(KBExp<X, V> S, KBExp<X, V> T) {
		if (S.isVar() || T.isVar()) {
			return fals();
		}

		Set<DAG<X>> ret = tru();
		for (KBExp<X, V> ti : T.getArgs()) {
			ret = and(ret, gt_lpoInfer(S, ti));
		}

		Set<DAG<X>> zz;
		zz = S.f().equals(T.f()) ? and(ret, gt_lpo_lexInfer(S.getArgs(), T.getArgs()))
				: and(ret, gtInfer(S.f(), T.f()));
		return zz;
	}

	private static <X, V> Set<DAG<X>> gt_lpo_lexInfer(List<KBExp<X, V>> ss, List<KBExp<X, V>> tt) {
		if (ss.size() != tt.size()) {
			throw new RuntimeException("Anomaly: please report");
		}
		if (ss.isEmpty()) {
			return fals();
		}
		KBExp<X, V> s0 = ss.get(0), t0 = tt.get(0);
		return or(gt_lpoInfer(s0, t0),
				and(eq(s0, t0), gt_lpo_lexInfer(ss.subList(1, ss.size()), tt.subList(1, tt.size()))));
	}

	public static <C, V, T> List<C> inferPrec(Map<C, Integer> symbols,
			Collection<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>>> R0) {
		Set<DAG<C>> ret = tru();
		for (Triple<KBExp<C, V>, KBExp<C, V>, Map<V, T>> R : R0) {
			ret = and(ret, gt_lpoInfer(R.first, R.second));
		}
		if (ret.isEmpty()) {
			throw new RuntimeException(
					"There is no LPO precedence that can orient all rules in their given left to right order.  (Unfailing) completion can still be used, but you will have to specify a precedence manually.  Or, try swapping the left and right hand sides of equations.\n\n"
							+ Util.sep(R0, "\n"));
		}
		// System.out.println(Util.sep(ret, "\n"));
		// System.out.println("-------");

		List<Pair<Integer, List<C>>> rets = new ArrayList<>(ret.size());
		for (DAG<C> cand : ret) {
			Pair<Integer, List<C>> cand0 = toPrec(symbols, cand);
			rets.add(cand0);
		}
		rets.sort((x, y) -> Integer.compare(y.first, x.first));
		return Util.get0X(rets).second;
		// TODO: aql just pick one randomly and make it total randomly.
	}

	// arity-0 < arity-2 < arity-1 < arity-3 < arity-4
	private static <C> Pair<Integer, List<C>> toPrec(Map<C, Integer> cs, DAG<C> g) {

		List<C> ret = new TreeList<>(g.topologicalSort()); // biggest first
		List<C> extra = new TreeList<>(cs.keySet()); // biggest first
		extra.removeAll(g.vertices());

		int i = 0;
		for (C c : ret) {
			i += cs.get(c);
		}
		ret.addAll(0, extra);
		return new Pair<>(i, (Util.reverse(ret)));
	}

	@Override
	public void add(C c, T t) {
		throw new RuntimeException("Cannot add constants with LPOUKB");
	}

}
