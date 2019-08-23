package catdata.apg;

import java.util.Map;
import java.util.Map.Entry;

import catdata.Chc;
import catdata.Pair;
import catdata.Util;
import catdata.aql.Var;
import gnu.trove.map.hash.TCustomHashMap;
import gnu.trove.strategy.HashingStrategy;

public class ApgTerm<E> {
	
	
	public final E e;
	public final Object v;
	
	public final Map<String,ApgTerm<E>> m;
	
	public final ApgTerm<E> a;
	public final String f;
	
	private ApgTerm(E e, Object v, Map<String, ApgTerm<E>> m, String f, 
			ApgTerm<E> a) {
		this.e = e;
		this.v = v;
		this.m = m;
		this.f = f;
		this.a = a;
	}
	
	private static synchronized <E> ApgTerm<E> mkApgTerm(E e, Object v, Map<String, ApgTerm<E>> m, String f, 
			ApgTerm<E> a) {
		ApgTerm<E> ret = new ApgTerm<>(e, v, m, f, a);
		
		ApgTerm<E> ret2 = cache.get(ret);
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

	public static synchronized <E> ApgTerm<E> ApgTermE(E str) {
		return mkApgTerm(str, null, null, null, null);
	}
	
	public static synchronized <E> ApgTerm<E> ApgTermV(Object str) {
		return mkApgTerm(null, str, null, null, null);
	}
	
	public static synchronized <E> ApgTerm<E> ApgTermTuple(Map<String,ApgTerm<E>> str) {
		return mkApgTerm(null, null, str, null, null);
	}
	
	public static synchronized <E> ApgTerm<E> ApgTermInj(String f, ApgTerm<E> str) {
		return mkApgTerm(null, null, null, f, str);
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
			for (Entry<String, ApgTerm<E>> z : m.entrySet()) {
				result = prime * result + (z.getValue().hashCode2());
				result = prime * result + (z.getKey().hashCode());
			}
		}
		result = prime * result + ((v == null) ? 0 : v.hashCode());
		return result;
	}

	public boolean equals2(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ApgTerm<E> other = (ApgTerm<E>) obj;
		
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
			for (Entry<String, ApgTerm<E>> f : m.entrySet()) {
				if (!other.m.get(f.getKey()).equals2(f.getValue())) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public String toString() {
		if (e != null) {
			return e.toString();
		}
		if (v != null) {
			return v.toString();
		}
		if (m != null) {
			return "(" + Util.sep(m, ":", ", ") + ")";
		}
		if (f != null) {
			return "<" + f + ":" + a + ">"; // + Util.sep(m, ": ", " ");
		}
		return Util.anomaly();
	}

	public <X> ApgTerm<X> convert() {
		return (ApgTerm<X>) this;
	}
	
	
}
