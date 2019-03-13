package catdata.provers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.iterators.IteratorChain;
import org.apache.commons.collections4.iterators.IteratorIterable;

import catdata.Pair;
import catdata.Triple;
import catdata.graph.UnionFind;
import gnu.trove.map.hash.THashMap;

public class CongruenceProverUniform<T, C, V> extends DPKB<T, C, V> {

	@Override
	public String toString() {
		return "CongruenceProverUniform [uf=" + ufs + ", pred=" + pred + "]";
	}

	private Map<T, UnionFind<KBExp<C, V>>> ufs;

	// in paper this doesn't check label - appears to be typo (!)
	private boolean congruent(KBExp<C, V> u, KBExp<C, V> v) {
		//if (!u.f().equals(v.f())) {
		//	return false;
		//}
		Iterator<KBExp<C, V>> it = v.getArgs().iterator();
		Iterator<T> jt = kb.syms.get(u.f()).first.iterator();
		for (KBExp<C, V> arg : u.getArgs()) {
			KBExp<C, V> arg2 = it.next();
			T t = jt.next(); //arg.type(kb.syms, Collections.emptyMap());
			if (!ufs.get(t).connected(arg, arg2)) {
				return false;
			}
		}
		return true;
	}

	private void merge1(KBExp<C, V> u, KBExp<C, V> v) {
		T t = this.kb.syms.get(u.f()).second; //u.type(this.kb.syms, Collections.emptyMap());
		UnionFind<KBExp<C, V>> uf = ufs.get(t);
		if (uf.connected(u, v)) {
			return;
		}

		
		IteratorChain<KBExp<C, V>> lpu = new IteratorChain<>();
		IteratorChain<KBExp<C, V>> lpv = new IteratorChain<>();
		
		Map<KBExp<C, V>, Set<KBExp<C, V>>> m = pred.get(t);
		for (KBExp<C, V> exp : m.keySet()) {
			Set<KBExp<C, V>> z = null;
			if (uf.connected(u, exp)) {
				z = m.get(exp);
				lpu.addIterator(z.iterator());
			}
			if (uf.connected(v, exp)) {
				if (z == null) {
					z = m.get(exp);
				}
				lpv.addIterator(z.iterator());
			}
		}
	
		uf.union(u, v);

		IteratorIterable<KBExp<C, V>> qq = new IteratorIterable<>(lpv, true);
		for (KBExp<C, V> x : new IteratorIterable<>(lpu, false)) {
			for (KBExp<C, V> y : qq) {
				if (x.f().equals(y.f())) {
					T tt = kb.syms.get(x.f()).second; 
					if (!ufs.get(tt).connected(x, y) && congruent(x, y)) {
						merge1(x, y);
					}
				}
			}
		}
	}

	private final Map<T, Map<KBExp<C, V>, Set<KBExp<C, V>>>> pred = new THashMap<>();

	public CongruenceProverUniform(KBTheory<T, C, V> th) {
		super(th);
		for (T t : th.tys) {
			pred.put(t, new THashMap<>());
			g.put(t, new THashMap<>());
			//cache.put(t, new HashMap<>());
		}
		for (Triple<Map<V, T>, KBExp<C, V>, KBExp<C, V>> eq : kb.eqs) {
			if (!eq.first.isEmpty()) {
				throw new RuntimeException("Congruence closure does not work with universal quantification.");
			}
			T t = eq.second.type(th.syms, eq.first);
			eq.second.allSubExps0(th.syms, t, pred);
			eq.third.allSubExps0(th.syms, t, pred);
		}
		for (C c : th.syms.keySet()) {
			Pair<List<T>, T> t = th.syms.get(c);
			if (!t.first.isEmpty()) {
				continue;
			}
			(th.factory.KBApp(c, Collections.emptyList())).allSubExps0(th.syms, t.second,
					pred);
		}
		for (C c : th.syms.keySet()) {
			Pair<List<T>, T> t = th.syms.get(c);
			g.get(t.second).put(c, new THashMap<>());
		}
		for (C c : th.syms.keySet()) {
			Pair<List<T>, T> t = th.syms.get(c);
			if (t.first.size() != 1) {
				continue;
			}
			for (C d : th.syms.keySet()) {
				Pair<List<T>, T> t0 = th.syms.get(d);
				if (t0.first.size() != 0 || !t0.second.equals(t.first.get(0))) {
					continue;
				}
				KBExp<C, V> dd = th.factory.KBApp(d, Collections.emptyList());
				KBExp<C, V> zz = th.factory.KBApp(c, Collections.singletonList(dd));
				zz.allSubExps0(th.syms, t.second, pred);
				
			}
			
			
		} 
		doCong();
		
	}
	

	private void doCong() {
		ufs = new THashMap<>(kb.tys.size());
		for (T t : kb.tys) {
			ufs.put(t, new UnionFind<>(pred.get(t).keySet().size(), pred.get(t).keySet()));
		}
		for (Triple<Map<V, T>, KBExp<C, V>, KBExp<C, V>> eq : kb.eqs) {
			merge1(eq.second, eq.third);
		}
		for (T t : pred.keySet()) {
			for (KBExp<C, V> x : pred.get(t).keySet()) {
				C c = x.f();
				List<KBExp<C, V>> l = new ArrayList<>(x.getArgs().size());
				Iterator<T> it = kb.syms.get(c).first.iterator();
				for (KBExp<C, V> y : x.getArgs()) {
					T ty = it.next();
					int z = ufs.get(ty).findNoAdd(y);
					l.add(kb.factory.freshConst(ty, z));
				}
				int z = ufs.get(t).findNoAdd(x);
				g.get(t).get(c).put(l,
						kb.factory.freshConst(t, z));
			}
		}
	}
	
	

	Map<T, Map<C, Map<List<KBExp<C, V>>, KBExp<C, V>>>> g = new THashMap<>();

	//Map<T, Map<KBExp<C, V>, KBExp<C, V>>> cache = new HashMap<>(); 
	
	public KBExp<C, V> nf(T t, KBExp<C, V> lhs) {
		KBExp<C, V> ret = null; //cache.get(t).get(lhs);
		C c = lhs.f();
		List<KBExp<C, V>> l = new ArrayList<>(lhs.getArgs().size());
		Iterator<T> tys = this.kb.syms.get(c).first.iterator();
		for (KBExp<C, V> x : lhs.getArgs()) {
			l.add(nf(tys.next(), x));
		}
		ret = g.get(t).get(c).get(l);
		if (ret != null) {
	//		cache.get(t).put(lhs, ret);
			return ret;
		}
		ret = kb.factory.KBApp(c, l);
	//	cache.get(t).put(lhs, ret);
		return ret;
	}

	@Override
	public synchronized boolean eq(Map<V, T> ctx, KBExp<C, V> lhs, KBExp<C, V> rhs) {
		if (!ctx.isEmpty()) {
			throw new RuntimeException("Congruence prover can only be used with ground equations.");
		}
		
		T t = kb.syms.get(lhs.f()).second; //lhs.type(this.kb.syms, ctx);

		int i = ufs.get(t).findNoAdd(lhs);
		if (i != -1) {
			int j = ufs.get(t).findNoAdd(rhs);
			if (j != -1) {
				return i == j;
			}
		}
		KBExp<C, V> l = nf(t, lhs);
		KBExp<C, V> r = nf(t, rhs);
		boolean b = l.equals(r);
		
		return b;

	}

	@Override
	public synchronized void add(C c, T t) {
		this.kb.syms.put(c, new Pair<>(Collections.emptyList(), t));
		this.g.get(t).put(c, Collections.emptyMap());
	}

}
