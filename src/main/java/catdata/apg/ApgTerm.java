package catdata.apg;

import java.util.Map;
import java.util.Map.Entry;

import catdata.Pair;
import catdata.Util;
import catdata.aql.Var;
import gnu.trove.map.hash.TCustomHashMap;
import gnu.trove.strategy.HashingStrategy;

public class ApgTerm<F,L,B,E,V> {
	
	public ApgTy<F,L,B> type(Map<Var, ApgTy<F,L,B>> ctx1, Map<E, L> ctx2, Map<V, B> ctx3) {
		if (x != null) {
			if (!ctx1.containsKey(x)) {
				throw new RuntimeException("Variable " + x + " not found in " + ctx1.keySet());
			}
			return ctx1.get(x);
		}
		if (e != null) {
			if (!ctx2.containsKey(e)) {
				throw new RuntimeException("Element " + e + " not found in " + ctx2.keySet());
			}
			return ApgTy.ApgTyL(ctx2.get(e));
		}
		if (v != null) {
			if (!ctx3.containsKey(v)) {
				throw new RuntimeException("Constant " + v + " not found in " + ctx3.keySet());
			}
			return ApgTy.ApgTyB(ctx3.get(v));
		}
		if (m != null && vs == null) {
			ApgTy.ApgTyP(true, Util.map(m, (k,v)->new Pair<>(k,v.type(ctx1, ctx2, ctx3))));
		}
		if (f != null && n == null) {
			ApgTy<F, L, B> t = a.type(ctx1, ctx2, ctx3);
			if (t.m == null) {
				throw new RuntimeException("Not a product: " + t);
			}
			if (!t.m.containsKey(f)) {
				throw new RuntimeException("Field " + f + " not found in " + t.m.keySet());
			}
			return t.m.get(f);
		}
		
		
		return Util.anomaly();
	}
	
	

	private final Var x;
	private final E e;
	private final V v;
	
	private final Map<F,ApgTerm<F,L,B,E,V>> m;
	
	private final Map<F,Var> vs;
	
	private final ApgTerm<F,L,B,E,V> a;
	private final F f;
	private final Map<F,ApgTy<F,L,B>> n; 
	
	private ApgTerm(Var x, E e, V v, Map<F, ApgTerm<F, L, B, E, V>> m, F f, Map<F, Var> vs, Map<F, ApgTy<F, L, B>> n,
			ApgTerm<F, L, B, E, V> a) {
		this.x = x;
		this.e = e;
		this.v = v;
		this.m = m;
		this.f = f;
		this.vs = vs;
		this.n = n;
		this.a = a;
	}
	
	private static synchronized <F, L, B, E, V> ApgTerm<F, L, B, E, V> mkApgTerm(Var x, E e, V v, Map<F, ApgTerm<F, L, B, E, V>> m, F f, Map<F, Var> vs, Map<F, ApgTy<F, L, B>> n,
			ApgTerm<F, L, B, E, V> a) {
		ApgTerm<F, L, B, E, V> ret = new ApgTerm<>(x, e, v, m, f, vs, n, a);
		
		ApgTerm<F, L, B, E, V> ret2 = cache.get(ret);
		if (ret2 != null) {
			return ret2;
		}
		cache.put(ret, ret);
		return ret;
	}
	
	@SuppressWarnings("rawtypes")
	private static HashingStrategy<ApgTerm> strategy = new HashingStrategy<>() {
		private static final long serialVersionUID = 1L;

		@Override
		public int computeHashCode(ApgTerm  t) {
			return t.hashCode2();
		}

		@Override
		public boolean equals(ApgTerm s, ApgTerm t) {
			return s.equals2(t);
		}
	};

	private static Map<ApgTerm, ApgTerm> cache = new TCustomHashMap<>(strategy);

	public static synchronized <F,L,B,E,V> ApgTerm<F,L,B,E,V> ApgTermX(Var str) {
		return mkApgTerm(str, null, null, null, null, null, null, null);
	}
	
	public static synchronized <F,L,B,E,V> ApgTerm<F,L,B,E,V> ApgTermE(E str) {
		return mkApgTerm(null, str, null, null, null, null, null, null);
	}
	
	public static synchronized <F,L,B,E,V> ApgTerm<F,L,B,E,V> ApgTermV(V str) {
		return mkApgTerm(null, null, str, null, null, null, null, null);
	}
	
	public static synchronized <F,L,B,E,V> ApgTerm<F,L,B,E,V> ApgTermTuple(Map<F,ApgTerm<F,L,B,E,V>> str) {
		return mkApgTerm(null, null, null, str, null, null, null, null);
	}
	
	public static synchronized <F,L,B,E,V> ApgTerm<F,L,B,E,V> ApgTermProj(ApgTerm<F,L,B,E,V> str, F f) {
		return mkApgTerm(null, null, null, null, f, null, null, str);
	}
	
	public static synchronized <F,L,B,E,V> ApgTerm<F,L,B,E,V> ApgTermCase(Map<F,ApgTerm<F,L,B,E,V>> str, Map<F,Var> vs) {
		return mkApgTerm(null, null, null, str, null, vs, null, null);
	}
	
	public static synchronized <F,L,B,E,V> ApgTerm<F,L,B,E,V> ApgTermInj(ApgTerm<F,L,B,E,V> a,
	F f, Map<F,ApgTy<F,L,B>> n) {
		return mkApgTerm(null, null, null, null, f, null, n, a);
	}
	
	@Override
	public int hashCode() {
		return System.identityHashCode(this);
	}

	@Override
	public boolean equals(Object x) {
		return this == x;
	}

	public int hashCode2() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((a == null) ? 0 : a.hashCode2());
		result = prime * result + ((e == null) ? 0 : e.hashCode());
		result = prime * result + ((f == null) ? 0 : f.hashCode());
		if (m != null) {
			for (Entry<F, ApgTerm<F, L, B, E, V>> z : m.entrySet()) {
				result = prime * result + (z.getValue().hashCode2());
				result = prime * result + (z.getKey().hashCode());
			}
		}
		result = prime * result + ((n == null) ? 0 : n.hashCode());
		result = prime * result + ((v == null) ? 0 : v.hashCode());
		result = prime * result + ((vs== null) ? 0 :vs.hashCode());
		result = prime * result + ((x == null) ? 0 : x.hashCode());
		return result;
	}

	public boolean equals2(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ApgTerm<F,L,B,E,V> other = (ApgTerm<F,L,B,E,V>) obj;
		
		if (x == null) {
			if (other.x != null)
				return false;
		} else if (!x.equals(other.x))
			return false;
		if (e == null) {
			if (other.e != null)
				return false;
		} else if (!e.equals(other.e))
			return false;
		if (f == null) {
			if (other.f != null)
				return false;
		} else if (!f.equals(other.f))
			return false;
		if (v == null) {
			if (other.v != null)
				return false;
		} else if (!v.equals(other.v))
			return false;
		if (vs == null) {
			if (other.vs != null)
				return false;
		} else if (!vs.equals(other.vs))
			return false;
		if (n == null) {
			if (other.n != null)
				return false;
		} else if (!n.equals(other.n))
			return false;
		if (a == null) {
			if (other.a != null)
				return false;
		} else if (!a.equals2(other.a))
			return false;
		if (m == null) {
			if (other.m != null)
				return false;
		} else {
			if (!m.keySet().equals(other.m.keySet())) {
				return false;
			}
			for (Entry<F, ApgTerm<F, L, B, E, V>> f : m.entrySet()) {
				if (!other.m.get(f.getKey()).equals2(f.getValue())) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public String toString() {
		if (x != null) {
			return x.toString();
		}
		if (e != null) {
			return e.toString();
		}
		if (v != null) {
			return v.toString();
		}
		if (m != null && vs == null) {
			return "(" + Util.sep(m, ": ", ", ") + ")";
		}
		if (f != null && n == null) {
			return a + "." + f;
		}
		return Util.anomaly();
	}
}
