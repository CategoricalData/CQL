package catdata.provers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.list.TreeList;

import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.Unit;
import catdata.Util;
import catdata.aql.ConsList;
import gnu.trove.map.hash.THashMap;

/**
 * works correctly on empty sorts
 */
public class MonoidalProver<T, C, V> extends DPKB<T, C, V> {

	private final Thue thue;

	public MonoidalProver(KBTheory<T, C, V> th) {
		super(th);

		// !_1 = (might be superflous) TODO aql
		List<Pair<List<Chc<Chc<Unit, T>, C>>, List<Chc<Chc<Unit, T>, C>>>> rules = new LinkedList<>();
		rules.add(new Pair<>(Collections.singletonList(Chc.inLeft(Chc.inLeft(Unit.unit))), Collections.emptyList()));

		// e : t -> 1 = !_1 - don't have any
		Map<Chc<Chc<Unit, T>, C>, Pair<Chc<Unit, T>, Chc<Unit, T>>> typing = new THashMap<>();
		Collection<Chc<Unit, T>> types = (new ArrayList<>(th.tys.size() + 1));
		types.add(Chc.inLeft(Unit.unit));

		// (e : t -> t').!_t' = !_t
		for (C c : th.syms.keySet()) {
			if (th.syms.get(c).first.size() > 1) {
				throw new RuntimeException(c + " is not unary or zero-ary");
			}
			Chc<Unit, T> t = th.syms.get(c).first.isEmpty() ? Chc.inLeft(Unit.unit)
					: Chc.inRight(th.syms.get(c).first.get(0));
			Chc<Unit, T> t0 = Chc.inRight(th.syms.get(c).second);
			typing.put(Chc.inRight(c), new Pair<>(t, t0));
			List<Chc<Chc<Unit, T>, C>> lhs = (Util.list(Chc.inRight(c), Chc.inLeft(t0)));
			List<Chc<Chc<Unit, T>, C>> rhs = (Collections.singletonList(Chc.inLeft(t)));
			rules.add(new Pair<>(lhs, rhs));
		}
		typing.put(Chc.inLeft(Chc.inLeft(Unit.unit)), new Pair<>(Chc.inLeft(Unit.unit), Chc.inLeft(Unit.unit)));
		for (T t : th.tys) {
			types.add(Chc.inRight(t));
			typing.put(Chc.inLeft(Chc.inRight(t)), new Pair<>(Chc.inRight(t), Chc.inLeft(Unit.unit)));

		}
		// System.out.println(rules);
		for (Triple<Map<V, T>, KBExp<C, V>, KBExp<C, V>> eq : th.eqs) {
			rules.add(new Pair<>(trans(eq.first, eq.second), trans(eq.first, eq.third)));
		}
		// System.out.println(rules);
		thue = new Thue<>(rules, typing.keySet());
		// thue = new ThueSlow(rules);
		//
		// kb.complete();
	}

	private List<Chc<Chc<Unit, T>, C>> trans(Map<V, T> ctx, KBExp<C, V> term) {

		List<Chc<Chc<Unit, T>, C>> ret = (new TreeList<>());

		while (true) {
			if (term.isVar()) {
				break;
			} else if (term.getArgs().isEmpty() && ctx.isEmpty()) {
				ret.add(0, Chc.inRight(term.f()));
				break;
			} else if (term.getArgs().isEmpty() && !ctx.isEmpty()) {
				Chc<Unit, T> t = ctx.isEmpty() ? Chc.inLeft(Unit.unit) : Chc.inRight(ctx.values().iterator().next());
				ret.add(0, Chc.inRight(term.f()));
				ret.add(0, Chc.inLeft(t));
				break;
			} else {
				ret.add(0, Chc.inRight(term.f()));
				term = term.getArgs().get(0);
			}
		}

		return ret; // Util.reverse(ret);
	}

	@Override
	public synchronized boolean eq(Map<V, T> ctx, KBExp<C, V> lhs, KBExp<C, V> rhs) {

		if (!lhs.isVar() && lhs.getArgs().size() > 1) {
			throw new RuntimeException("On eq, not monoidal: " + lhs + ", please report.");
		} else if (!rhs.isVar() && rhs.getArgs().size() > 1) {
			throw new RuntimeException("On eq, not monoidal: " + rhs + ", please report.");
		} else if (ctx.size() > 1) {
			throw new RuntimeException("On eq, not monoidal: " + ctx + ", please report.");
		}
		// System.out.println(lhs + " =? " + rhs);
		// System.out.println(trans(ctx, lhs) + " =? " + trans(ctx, rhs));
		return thue.equiv(trans(ctx, lhs), trans(ctx, rhs));
	}

	@Override
	public String toString() {
		return thue.toString();
	}

	@Override
	public synchronized void add(C c, T tt) {
		this.kb.syms.put(c, new Pair<>(Collections.emptyList(), tt));

		Chc<Unit, T> t = Chc.inLeft(Unit.unit);
		Chc<Unit, T> t0 = Chc.inRight(tt);
		ConsList<Chc<Chc<Unit, T>, C>> lhs = ConsList.new0(Util.list(Chc.inRight(c), Chc.inLeft(t0)), false);
		ConsList<Chc<Chc<Unit, T>, C>> rhs = ConsList.new0(Collections.singletonList(Chc.inLeft(t)), false);
		this.thue.rules.add(lhs.list.get(0));
		this.thue.rules.add(new Pair<>(lhs, rhs));
		this.thue.complete();

	}

	@Override
	public boolean supportsTrivialityCheck() {
		return false;
	}
}
