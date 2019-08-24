package catdata.apg;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import catdata.Chc;
import catdata.Pair;
import catdata.Unit;
import catdata.Util;
import catdata.graph.UnionFind;
import gnu.trove.map.hash.THashMap;

public class ApgOps {
	
	public static <L,E> ApgTransform<L,E,L,E> id(ApgInstance<L,E> G) {
		return new ApgTransform<>(G, G, Util.map(G.Ls, (k,v)->new Pair<>(k,k)), Util.map(G.Es, (k,v)->new Pair<>(k,k)));
	}
	
	public static <L1,E1,L2,E2,L3,E3> ApgTransform<L1,E1,L3,E3> compose(ApgTransform<L1,E1,L2,E2> h, ApgTransform<L2,E2,L3,E3> j) {
		if (!h.dst.equals(j.src)) {
			Util.anomaly();
		}
		return new ApgTransform<>(h.src, j.dst, Util.map(h.lMap, (k,v)->new Pair<>(k, j.lMap.get(v))), Util.map(h.eMap, (k,v)->new Pair<>(k, j.eMap.get(v))));
	}
	
	///////////////////////////////////////////////////////////////////////////
	
	public static ApgInstance<Void,Void> initial(ApgTypeside ts) {
		return new ApgInstance<>(ts, Collections.emptyMap(), Collections.emptyMap());
	}
	
	public static <L,E> ApgTransform<Void,Void,L,E> initial(ApgInstance<L,E> G) {
		return new ApgTransform<>(initial(G.typeside), G, Collections.emptyMap(), Collections.emptyMap());
	}

	public static ApgInstance<Unit,Unit> terminal(ApgTypeside ts) {
		Map<Unit, ApgTy<Unit>> ls = Collections.singletonMap(Unit.unit, ApgTy.ApgTyP(true, Collections.emptyMap()));
		Map<Unit, Pair<Unit, ApgTerm<Unit>>> es = Collections.singletonMap(Unit.unit, new Pair<>(Unit.unit, ApgTerm.ApgTermTuple(Collections.emptyMap())));;
		return new ApgInstance<>(ts, ls, es);
	}
	
	public static <L,E> ApgTransform<L,E,Unit,Unit> terminal(ApgInstance<L,E> G) {
		Map<L,Unit> lMap = new THashMap<>();
		for (L l : G.Ls.keySet()) {
			lMap.put(l, Unit.unit);
		}
		Map<E,Unit> eMap = new THashMap<>();
		for (E e : G.Es.keySet()) {
			eMap.put(e, Unit.unit);
		}
		return new ApgTransform<>(G, terminal(G.typeside), lMap, eMap);
	}
	
	///////////////////////////////////////////////////////////////////////////
	
	private static <L1,L2> ApgTy<Chc<L1,L2>> inl(ApgTy<L1> t1) {
		if (t1.b != null) {
			return t1.convert();
		} else if (t1.l != null) {
			return ApgTy.ApgTyL(Chc.inLeft(t1.l));
		} else if (t1.m != null) {
			Map<String, ApgTy<Chc<L1,L2>>> m = Util.map(t1.m,(k,v)->new Pair<>(k,inl(v)));
			return ApgTy.ApgTyP(t1.all, m);
		}
		return Util.anomaly();
	}
	private static <L1,L2> ApgTy<Chc<L1,L2>> inr(ApgTy<L2> t2) {
		if (t2.b != null) {
			return t2.convert();
		} else if (t2.l != null) {
			return ApgTy.ApgTyL(Chc.inRight(t2.l));
		} else if (t2.m != null) {
			Map<String, ApgTy<Chc<L1,L2>>> m = Util.map(t2.m,(k,v)->new Pair<>(k,inr(v)));
			return ApgTy.ApgTyP(t2.all, m);
		}
		return Util.anomaly();
	}
	private static <E1,E2> ApgTerm<Chc<E1,E2>> inl(ApgTerm<E1> t1) {
		if (t1.v != null) {
			return t1.convert();
		} else if (t1.e != null) {
			return ApgTerm.ApgTermE(Chc.inLeft(t1.e));
		} else if (t1.m != null) {
			Map<String, ApgTerm<Chc<E1,E2>>> m = Util.map(t1.m,(k,v)->new Pair<>(k,inl(v)));
			return ApgTerm.ApgTermTuple(m);
		} else if (t1.f != null) {
			return ApgTerm.ApgTermInj(t1.f, inl(t1.a));
		}
		return Util.anomaly();
	}
	private static <E1,E2> ApgTerm<Chc<E1,E2>> inr(ApgTerm<E2> t2) {
		if (t2.v != null) {
			return t2.convert();
		} else if (t2.e != null) {
			return ApgTerm.ApgTermE(Chc.inRight(t2.e));
		} else if (t2.m != null) {
			Map<String, ApgTerm<Chc<E1,E2>>> m = Util.map(t2.m,(k,v)->new Pair<>(k,inr(v)));
			return ApgTerm.ApgTermTuple(m);
		} else if (t2.f != null) {
			return ApgTerm.ApgTermInj(t2.f, inr(t2.a));
		}
		return Util.anomaly();
	}
	
	public static <L1,E1,L2,E2> ApgInstance<Chc<L1,L2>,Chc<E1,E2>> coproduct(ApgInstance<L1,E1> G1, ApgInstance<L2,E2> G2) {
		if (!G1.typeside.equals(G2.typeside)) {
			return Util.anomaly();
		}
		Map<Chc<L1, L2>, ApgTy<Chc<L1, L2>>> ls = new THashMap<>();
		for (Entry<L1, ApgTy<L1>> l1 : G1.Ls.entrySet()) {
			ls.put(Chc.inLeft (l1.getKey()), inl(l1.getValue()));	
		}		
		for (Entry<L2, ApgTy<L2>> l2 : G2.Ls.entrySet()) {
			ls.put(Chc.inRight(l2.getKey()), inr(l2.getValue()));
		}
			
		Map<Chc<E1, E2>, Pair<Chc<L1, L2>, ApgTerm<Chc<E1, E2>>>> es = new THashMap<>();
		for (Entry<E1, Pair<L1, ApgTerm<E1>>> e1 : G1.Es.entrySet()) {
			es.put(Chc.inLeft (e1.getKey()), new Pair<>(Chc.inLeft (e1.getValue().first), inl(e1.getValue().second)));	
		}	
		for (Entry<E2, Pair<L2, ApgTerm<E2>>> e2 : G2.Es.entrySet()) {
			es.put(Chc.inRight(e2.getKey()), new Pair<>(Chc.inRight(e2.getValue().first), inr(e2.getValue().second)));
		}

		return new ApgInstance<>(G1.typeside, ls, es);
	}
	
	public static <L1,E1,L2,E2> ApgTransform<L1,E1,Chc<L1,L2>,Chc<E1,E2>> inl(ApgInstance<L1,E1> G1, ApgInstance<L2,E2> G2) {
		if (!G1.typeside.equals(G2.typeside)) {
			return Util.anomaly();
		}
		ApgInstance<Chc<L1,L2>,Chc<E1,E2>> G = coproduct(G1, G2);
		return new ApgTransform<>(G1, G, Util.map(G1.Ls,(k,v)->new Pair<>(k,Chc.inLeft(k))), Util.map(G1.Es,(k,v)->new Pair<>(k,Chc.inLeft(k))));
	}
	
	public static <L1,E1,L2,E2> ApgTransform<L2,E2,Chc<L1,L2>,Chc<E1,E2>> inr(ApgInstance<L1,E1> G1, ApgInstance<L2,E2> G2) {
		if (!G1.typeside.equals(G2.typeside)) {
			return Util.anomaly();
		}
		ApgInstance<Chc<L1,L2>,Chc<E1,E2>> G = coproduct(G1, G2);
		return new ApgTransform<>(G2, G, Util.map(G2.Ls,(k,v)->new Pair<>(k,Chc.inRight(k))), Util.map(G2.Es,(k,v)->new Pair<>(k,Chc.inRight(k))));
	}
	
	public static <L,E,L1,E1,L2,E2> ApgTransform<Chc<L1,L2>,Chc<E1,E2>,L,E> Case(ApgTransform<L1,E1,L,E> h, ApgTransform<L2,E2,L,E> j) {
		if (!h.dst.equals(j.dst)) {
			return Util.anomaly();
		}
		ApgInstance<L ,E > G  = h.dst;
		ApgInstance<L1,E1> G1 = h.src;
		ApgInstance<L2,E2> G2 = j.src;
		ApgInstance<Chc<L1, L2>, Chc<E1, E2>> X = coproduct(G1, G2);
		return new ApgTransform<>(X, G, Util.map(X.Ls,(k,v)->new Pair<>(k, k.left ? h.lMap.get(k.l) : j.lMap.get(k.r))), Util.map(X.Es,(k,v)->new Pair<>(k, k.left ? h.eMap.get(k.l) : j.eMap.get(k.r))));
	}
	
	///////////////////////////////////////////////////////////////////////////
		
	public static <L1,E1,L2,E2> ApgInstance<Pair<L1,L2>,Pair<E1,E2>> 
	  product(ApgInstance<L1,E1> G1, ApgInstance<L2,E2> G2) {
		if (!G1.typeside.equals(G2.typeside)) {
			Util.anomaly();
		}
		Map<Pair<L1, L2>, ApgTy<Pair<L1, L2>>> ls = new THashMap<>();
		Map<Pair<E1, E2>, Pair<Pair<L1, L2>, ApgTerm<Pair<E1, E2>>>> es = new THashMap<>();
		
		for (Entry<L1, ApgTy<L1>> l1 : G1.Ls.entrySet()) {
			for (Entry<L2, ApgTy<L2>> l2 : G2.Ls.entrySet()) {
				Map<String, ApgTy<Pair<L1, L2>>> m = new THashMap<>(4);
				m.put("left", l_tensor(l1.getValue(), l2.getKey()));
				m.put("right", r_tensor(l1.getKey(), l2.getValue()));
				ls.put(new Pair<>(l1.getKey(), l2.getKey()), ApgTy.ApgTyP(true, m));
			}			
		}
		for (Entry<E1, Pair<L1, ApgTerm<E1>>> l1 : G1.Es.entrySet()) {
			for (Entry<E2, Pair<L2, ApgTerm<E2>>> l2 : G2.Es.entrySet()) {
				Map<String, ApgTerm<Pair<E1, E2>>> m = new THashMap<>(4);
				m.put("left", l_tensor(l1.getValue().second, l2.getKey()));
				m.put("right", r_tensor(l1.getKey(), l2.getValue().second));
				Pair<Pair<L1, L2>, ApgTerm<Pair<E1, E2>>> 
				p = new Pair<>(new Pair<>(l1.getValue().first, l2.getValue().first), ApgTerm.ApgTermTuple(m));
				es.put(new Pair<>(l1.getKey(), l2.getKey()), p);
			}			
		}
		
		return new ApgInstance<>(G1.typeside, ls, es);
	} 
	
	public static <L1,E1,L2,E2> ApgTransform<Pair<L1,L2>,Pair<E1,E2>,L1,E1> fst(ApgInstance<L1,E1> G1, ApgInstance<L2,E2> G2) {
		if (!G1.typeside.equals(G2.typeside)) {
			return Util.anomaly();
		}
		ApgInstance<Pair<L1, L2>, Pair<E1, E2>> G = product(G1, G2);
		return new ApgTransform<>(G, G1, Util.map(G.Ls,(k,v)->new Pair<>(k,k.first)), Util.map(G.Es,(k,v)->new Pair<>(k,k.first)));
	}
	
	public static <L1,E1,L2,E2> ApgTransform<Pair<L1,L2>,Pair<E1,E2>,L2,E2> snd(ApgInstance<L1,E1> G1, ApgInstance<L2,E2> G2) {
		if (!G1.typeside.equals(G2.typeside)) {
			return Util.anomaly();
		}
		ApgInstance<Pair<L1, L2>, Pair<E1, E2>> G = product(G1, G2);
		return new ApgTransform<>(G, G2, Util.map(G.Ls,(k,v)->new Pair<>(k,k.second)), Util.map(G.Es,(k,v)->new Pair<>(k,k.second)));
	}
	
	public static <L,E,L1,E1,L2,E2> ApgTransform<L,E,Pair<L1,L2>,Pair<E1,E2>> pair(ApgTransform<L,E,L1,E1> h, ApgTransform<L,E,L2,E2> j) {
		if (!h.src.equals(j.src)) {
			return Util.anomaly();
		}
		ApgInstance<L ,E > G  = h.src;
		ApgInstance<L1,E1> G1 = h.dst;
		ApgInstance<L2,E2> G2 = j.dst;
		ApgInstance<Pair<L1, L2>, Pair<E1, E2>> X = product(G1, G2);
		return new ApgTransform<>(G, X, Util.map(G.Ls,(k,v)->new Pair<>(k, new Pair<>(h.lMap.get(k),j.lMap.get(k)))), Util.map(G.Es,(k,v)->new Pair<>(k, new Pair<>(h.eMap.get(k),j.eMap.get(k)))));
	}
	
	private static <L1,L2> ApgTy<Pair<L1,L2>> l_tensor(ApgTy<L1> t1, L2 l2) {
		if (t1.b != null) {
			return t1.convert();
		} else if (t1.l != null) {
			return ApgTy.ApgTyL(new Pair<>(t1.l, l2));
		} else if (t1.m != null) {
			return ApgTy.ApgTyP(t1.all, Util.map(t1.m,(k,v)->new Pair<>(k,l_tensor(v, l2))));
		}
		return Util.anomaly();
	}
	private static <L1,L2> ApgTy<Pair<L1,L2>> r_tensor(L1 l1, ApgTy<L2> t2) {
		if (t2.b != null) {
			return t2.convert();
		} else if (t2.l != null) {
			return ApgTy.ApgTyL(new Pair<>(l1, t2.l));
		} else if (t2.m != null) {
			return ApgTy.ApgTyP(t2.all, Util.map(t2.m,(k,v)->new Pair<>(k,r_tensor(l1, v))));
		}
		return Util.anomaly();
	}
	private static <E1,E2> ApgTerm<Pair<E1,E2>> l_tensor(ApgTerm<E1> t1, E2 e2) {
		if (t1.v != null) {
			return t1.convert();
		} else if (t1.e != null) {
			return ApgTerm.ApgTermE(new Pair<>(t1.e, e2));
		} else if (t1.m != null) {
			return ApgTerm.ApgTermTuple(Util.map(t1.m,(k,v)->new Pair<>(k,l_tensor(v, e2))));
		} else if (t1.f != null) {
			return ApgTerm.ApgTermInj(t1.f, l_tensor(t1.a, e2));
		}
		return Util.anomaly();
	}
	private static <E1,E2> ApgTerm<Pair<E1,E2>> r_tensor(E1 e1, ApgTerm<E2> t2) {
		if (t2.v != null) {
			return t2.convert();
		} else if (t2.e != null) {
			return ApgTerm.ApgTermE(new Pair<>(e1, t2.e));
		} else if (t2.m != null) {
			return ApgTerm.ApgTermTuple(Util.map(t2.m,(k,v)->new Pair<>(k,r_tensor(e1, v))));
		} else if (t2.f != null) {
			return ApgTerm.ApgTermInj(t2.f, r_tensor(e1, t2.a));
		}
		return Util.anomaly();
	}
	
	///////////////////////////////////////////////////////////////////////////
	
	private static <L1,L2,E1,E2> ApgTy<L1> filter(ApgTy<L1> t1, ApgTransform<L1,E1,L2,E2> h, ApgTransform<L1,E1,L2,E2> k) {
		if (t1.b != null) {
			return t1.convert();
		} else if (t1.l != null) {
			if (h.lMap.get(t1.l).equals(k.lMap.get(t1.l))) {			
				Map<String, ApgTy<L1>> m = new THashMap<>(4);
				m.put("element", ApgTy.ApgTyL(t1.l));
				m.put("unit", ApgTy.ApgTyP(true, Collections.emptyMap()));
				return ApgTy.ApgTyP(false, m);
			}
			return ApgTy.ApgTyP(true, Collections.emptyMap());
		} else if (t1.m != null) {
			return ApgTy.ApgTyP(t1.all, Util.map(t1.m,(w,v)->new Pair<>(w, filter(v, h, k))));
		}
		return Util.anomaly();
	}
	
	private static <L1,L2,E1,E2> ApgTerm<E1> filter(ApgTerm<E1> t1, ApgTransform<L1,E1,L2,E2> h, ApgTransform<L1,E1,L2,E2> k) {
		if (t1.v != null) {
			return t1.convert();
		} else if (t1.e != null) {
			L1 l = h.src.Es.get(t1.e).first;
			if (h.lMap.get(l).equals(k.lMap.get(l))) {	
				if (h.eMap.get(t1.e).equals(k.eMap.get(t1.e))) {
					return ApgTerm.ApgTermInj("element", t1);
				}
				return ApgTerm.ApgTermInj("unit", ApgTerm.ApgTermTuple(Collections.emptyMap()));
			}
			return ApgTerm.ApgTermTuple(Collections.emptyMap());
		} else if (t1.f != null) {
			return ApgTerm.ApgTermInj(t1.f, filter(t1.a,h,k)); 
		} else if (t1.m != null) {
			return ApgTerm.ApgTermTuple(Util.map(t1.m,(w,v)->new Pair<>(w, filter(v, h, k))));
		}
		return Util.anomaly();
	}
	
	
	public static <E1,E2,L1,L2> ApgInstance<L1,E1> equalize(ApgTransform<L1,E1,L2,E2> h, ApgTransform<L1,E1,L2,E2> k) {
		if (!h.src.equals(k.src) || !h.dst.equals(k.dst)) {
			Util.anomaly();
		}
		Map<L1, ApgTy<L1>> Ls = new THashMap<>();
		Map<E1, Pair<L1, ApgTerm<E1>>> Es = new THashMap<>();
		
		for (Entry<L1, ApgTy<L1>> l1 : h.src.Ls.entrySet()) {
			if (h.lMap.get(l1.getKey()).equals(k.lMap.get(l1.getKey()))) {		
				Ls.put(l1.getKey(), filter(l1.getValue(), h, k));
			}
		}
		for (Entry<E1, Pair<L1, ApgTerm<E1>>> e1 : h.src.Es.entrySet()) {
			L1 l1 = e1.getValue().first;
			if (!h.lMap.get(l1).equals(k.lMap.get(l1))) {		
				continue;
			}
			if (!h.eMap.get(e1.getKey()).equals(k.eMap.get(e1.getKey()))) {		
				continue;
			}
			Es.put(e1.getKey(), new Pair<>(e1.getValue().first, filter(e1.getValue().second, h, k)));
		}
		
		return new ApgInstance<>(h.src.typeside, Ls, Es);
	}
	
	public static <E1,E2,L1,L2> ApgTransform<L1,E1,L1,E1> equalizeT(ApgTransform<L1,E1,L2,E2> h, ApgTransform<L1,E1,L2,E2> k) {
		if (!h.src.equals(k.src) || !h.dst.equals(k.dst)) {
			Util.anomaly();
		}
		ApgInstance<L1,E1> X = equalize(h,k);
		
		return new ApgTransform<>(X, h.src, Util.id(X.Ls.keySet()), Util.id(X.Es.keySet()));
		
	}
	
	public static <E,L,E1,E2,L1,L2> ApgTransform<L,E,L1,E1> equalizeU(ApgTransform<L,E,L1,E1> w, ApgTransform<L1,E1,L2,E2> h, ApgTransform<L1,E1,L2,E2> k) {
		if (!h.src.equals(k.src) || !h.dst.equals(k.dst)) {
			Util.anomaly();
		} else if (!compose(w,h).equals(compose(w,k))) {
			throw new RuntimeException("Not equal: " + compose(w,h) + " and " + compose(w,k));
		}
		return new ApgTransform<>(w.src, equalize(h,k), w.lMap, w.eMap);
	}
	
	//////////////////////////////////////////////////////////
	
	public static <E1,E2,L> ApgInstance<L,Set<E2>> coequalize(ApgTransform<L,E1,L,E2> h, ApgTransform<L,E1,L,E2> k) {
		if (!h.src.equals(k.src) || !h.dst.equals(k.dst)) {
			Util.anomaly();
		}
		for (Entry<L, L> l : h.lMap.entrySet()) {
			if (!l.getKey().equals(l.getValue())) {
				Util.anomaly();
			}
		}
		for (Entry<L, L> l : k.lMap.entrySet()) {
			if (!l.getKey().equals(l.getValue())) {
				Util.anomaly();
			}
		}
		
		UnionFind<E2> uf = new UnionFind<>(h.dst.Es.size(), h.dst.Es.keySet());
		for (E1 e1 : h.src.Es.keySet()) {
			E2 e2h = h.eMap.get(e1);
			E2 e2k = k.eMap.get(e1);
			L lh = h.dst.Es.get(e2h).first;
			L lk = k.dst.Es.get(e2k).first;
			if (!lh.equals(lk)) {
				throw new RuntimeException("Cannot co-equalize: " + e1 + " sent to " + e2h + " and " + e2k + " of non-equal labels " + lh + " and " + lk);
			}
			uf.union(e2h, e2k);
		}
		Map<E2, Set<E2>> m = uf.toMap();
		
		Map<Set<E2>, Pair<L, ApgTerm<Set<E2>>>> Es = new THashMap<>();
		for (E2 e2 : h.dst.Es.keySet()) {
			if (!uf.find(e2).equals(e2)) {
				continue;
			}
			Pair<L, ApgTerm<E2>> lh = h.dst.Es.get(e2);
			Pair<L, ApgTerm<E2>> lk = k.dst.Es.get(e2);
			if (!lh.first.equals(lk.first)) {
				Util.anomaly();
			}
			ApgTerm<Set<E2>> p = eqc(m, lh.second);
			ApgTerm<Set<E2>> q = eqc(m, lh.second);
			if (!p.equals(q)) {
				Util.anomaly();
			}
			Es.put(m.get(e2), new Pair<>(lh.first, p));
		}
		
		return new ApgInstance<>(h.src.typeside, k.dst.Ls, Es);
	}
	
	private static <E2> ApgTerm<Set<E2>> eqc(Map<E2, Set<E2>> w, ApgTerm<E2> t1) {
		if (t1.v != null) {
			return t1.convert();
		} else if (t1.e != null) {
			return ApgTerm.ApgTermE(w.get(t1.e));
		} else if (t1.m != null) {
			Map<String, ApgTerm<Set<E2>>> m = Util.map(t1.m,(k,v)->new Pair<>(k,eqc(w, v)));
			return ApgTerm.ApgTermTuple(m);
		} else if (t1.f != null) {
			return ApgTerm.ApgTermInj(t1.f,eqc(w,t1.a));
		}
		return Util.anomaly();
	}

	public static <E1,E2,L> ApgTransform<L,E2,L,Set<E2>> coequalizeT(ApgTransform<L,E1,L,E2> h, ApgTransform<L,E1,L,E2> k) {
		if (!h.src.equals(k.src) || !h.dst.equals(k.dst)) {
			Util.anomaly();
		}
		ApgInstance<L, Set<E2>> X = coequalize(h,k);
		Map<E2, Set<E2>> eMap = new THashMap<>();
		for (E2 e1 : h.dst.Es.keySet()) {
			for (Set<E2> eqc : X.Es.keySet()) {
				if (eqc.contains(e1)) {
					eMap.put(e1, eqc);
					break;
				}
			}
		}
		return new ApgTransform<>(h.dst, X, Util.id(X.Ls.keySet()), eMap);
	}
	
	public static <E,E1,E2,L> ApgTransform<L,Set<E2>,L,E> coequalizeU(ApgTransform<L,E2,L,E> w, ApgTransform<L,E1,L,E2> h, ApgTransform<L,E1,L,E2> k) {
		if (!h.src.equals(k.src) || !h.dst.equals(k.dst)) {
			Util.anomaly();
		} else if (!compose(h,w).equals(compose(k,w))) {
			throw new RuntimeException("Not equal: " + compose(h,w) + " and " + compose(k,w));
		}
		ApgInstance<L, Set<E2>> X = coequalize(h,k);
		Map<Set<E2>,E> eMap = new THashMap<>();
		for (Set<E2> eqc : X.Es.keySet()) {
			eMap.put(eqc, w.eMap.get(Util.get0X(eqc)));
		}
		return new ApgTransform<>(X, w.dst, Util.id(X.Ls.keySet()), eMap);
	}
}







