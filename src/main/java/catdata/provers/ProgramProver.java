package catdata.provers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.list.TreeList;

import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import gnu.trove.set.hash.THashSet;

/*Loosen requirement for program in MVP that are of form

l -> r

in worst case, must try all combinations of orientations, but maybe can do better.
*/
//TODO aql merge constants and functions in typeside
public class ProgramProver<T, C, V> extends DPKB<T, C, V> {

	private final Iterator<V> fresh;
	private Set<T> groundInhabited = (new THashSet<>());

	public ProgramProver(boolean check, Iterator<V> fresh, KBTheory<T, C, V> th) {
		super(th);

		this.fresh = fresh;

		if (check) {
			isProgram(fresh, kb, true);
		}

		inhabGen(groundInhabited);

	}

	// TODO: aql find some way to cache the fact that something has been proved to
	// be a program
	public static <T, C, V> boolean isProgram(Iterator<V> fresh, KBTheory<T, C, V> theory, boolean throwerror) {
		for (Triple<Map<V, T>, KBExp<C, V>, KBExp<C, V>> eq : theory.eqs) {
			List<V> vars = new TreeList<>();
			eq.second.vars(vars);
			if (vars.size() != eq.second.getVars().size()) {
				if (throwerror) {
					throw new RuntimeException(
							"Not left linear (cotains duplicated variable on lhs): " + eq.second + " = " + eq.third);
				}
				return false;
			}
		}

		for (Triple<Map<V, T>, KBExp<C, V>, KBExp<C, V>> ab0 : theory.eqs) {

			for (Triple<Map<V, T>, KBExp<C, V>, KBExp<C, V>> gd0 : theory.eqs) {

				if (Collections.disjoint(ab0.second.symbols(), gd0.second.symbols())) {
					continue;
				}

				// Pair<KBExp<C, V>, KBExp<C, V>> ab = LPOUKB.freshen(fresh, new
				// Pair<>(ab0.second, ab0.third));
				Pair<KBExp<C, V>, KBExp<C, V>> gd = LPOUKB.freshen(theory, fresh, new Pair<>(gd0.second, gd0.third));

				Set<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, KBExp<C, V>>>> cps = gd.first.cp((new LinkedList<>()),
						ab0.second, ab0.third, gd.first, gd.second);
				for (Triple<KBExp<C, V>, KBExp<C, V>, Map<V, KBExp<C, V>>> cp : cps) {
					if (!cp.first.equals(cp.second)) {
						if (throwerror) {
							throw new RuntimeException("Is not a program: equation " + ab0.second + " = " + ab0.third
									+ " overlaps with " + gd0.second + " = " + gd0.third + ", the critical pair is "
									+ cp.first + " and " + cp.second);
						}
						return false;

					}
				}
			}
		}

		return true;
	}

	// private final Map<KBExp<C,V>, KBExp<C,V>> cache = (new HashMap<>());

	private KBExp<C, V> red(KBExp<C, V> e, Set<T> g) {
		while (true) {

			KBExp<C, V> e0 = step(e, g);
			if (e.equals(e0)) {
				return e0;
			}
			e = e0;
		}
	}

	private KBExp<C, V> step(KBExp<C, V> ee, Set<T> g) {
		Util.assertNotNull(ee);
		// if (Thread.currentThread().isInterrupted()) {
		// throw new RuntimeInterruptedException(new InterruptedException());
		// }
		if (ee.isVar()) {
			return step1(ee, g);
		}
		List<KBExp<C, V>> args0 = (new ArrayList<>(ee.getArgs().size()));
		for (KBExp<C, V> arg : ee.getArgs()) {
			args0.add(step(arg, g)); // needs to be step for correctness
		}
		KBExp<C, V> ret = kb.factory.KBApp(ee.f(), args0);
		return step1(ret, g);

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

	private synchronized KBExp<C, V> step1(KBExp<C, V> e0, Set<T> g) {
		KBExp<C, V> e = e0;
		for (Triple<Map<V, T>, KBExp<C, V>, KBExp<C, V>> r0 : kb.eqs) {
			Pair<KBExp<C, V>, KBExp<C, V>> r = new Pair<>(r0.second, r0.third);
			if (!Collections.disjoint(r.first.getVars(), e.getVars())
					|| !Collections.disjoint(r.second.getVars(), e.getVars())) {
				r = LPOUKB.freshen(kb, fresh, r);
			}

			KBExp<C, V> lhs = r.first;
			KBExp<C, V> rhs = r.second;
			Map<V, KBExp<C, V>> s = KBUnifier.findSubst(lhs, e);
			if (s == null) {
				continue;
			}
			System.out.println("s " + s + " applies " + applies(r0.first, s, g) + " g " + g);
			if (!applies(r0.first, s, g)) {
				continue;
			}
			e = rhs.substitute(s);
		}

		return e;
	}

	private synchronized boolean applies(Map<V, T> ruleCtx, Map<V, KBExp<C, V>> subst, Collection<T> inhab) {
		Util.assertNotNull(subst);
		Set<T> need = (new THashSet<>());
		for (V ruleVar : ruleCtx.keySet()) {
			Util.assertNotNull(ruleVar);
			if (!subst.containsKey(ruleVar)) {
				need.add(ruleCtx.get(ruleVar));
			}
		}
		return inhab.containsAll(need);
	}

	@Override
	public boolean eq(Map<V, T> ctx, KBExp<C, V> lhs, KBExp<C, V> rhs) {
		Util.assertNotNull(ctx, lhs, rhs);
		Set<T> g = (new THashSet<>());
		g.addAll(ctx.values());
		inhabGen(g);
		// System.out.println(g);
		return red(lhs, g).equals(red(rhs, g));
	}

	@Override
	public String toString() {
		return "Program prover " + this.kb.toString();
	}

	@Override
	public synchronized void add(C c, T t) {
		this.kb.syms.put(c, new Pair<>(Collections.emptyList(), t));
		groundInhabited.add(t);
		inhabGen(groundInhabited);
	}

}
