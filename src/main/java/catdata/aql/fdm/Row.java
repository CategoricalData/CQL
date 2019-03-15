package catdata.aql.fdm;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;

import catdata.Util;
import catdata.aql.Var;
import gnu.trove.map.hash.THashMap;

// these have to be tagged with the entity to be unique across entities
public class Row<En2, X> {

	public <Z, T> Row<En2, Z> map(BiFunction<X, T, Z> f) {
		if (tail == null) {
			return new Row<>(en2);
		}
		return new Row<>(tail.map(f), v, f.apply(x, (T) t), en2, t);
	}

	private final En2 en2;
	private final Var v;
	private final X x;
	private final Row<En2, X> tail;
	public final Object t;

	private Map<Var, X> map;

	public synchronized Map<Var, X> asMap() {
		if (map != null) {
			return map;
		}
		Row<En2, X> r = this;
		map = new THashMap<>();
		for (;;) {
			if (r.tail == null) {
				return map;
			}
			map.put(r.v, r.x);
			r = r.tail;
		}
	}
/*
	public final boolean containsKey(Var vv) {
		if (tail == null) {
			return false;
		} else if (v.equals(vv)) {
			return true;
		}
		return tail.containsKey(vv);
	}
*/
	public final X get(Var vv) {
		if (v.equals(vv)) {
			return x;
		}
		return tail.get(vv);
	}

	public Row(En2 en2) {
		this.en2 = en2;
		this.v = null;
		this.x = null;
		this.tail = null;
		this.t = null;
	}

	public Row(Row<En2, X> tail, Var v, X x, En2 en2, Object t) {
		this.v = v;
		this.x = x;
		this.tail = tail;
		this.en2 = en2;
		this.t = t;
	}

	public static <X, En2> Row<En2, X> mkRow(List<Var> order, Map<Var, X> ctx, En2 en2, Map ctx2, Map ctx3) {
		Row<En2, X> r = new Row<>(en2);
		for (Var v : order) {
			Object t1 = ctx2.get(v);
			if (t1 == null) {
				t1 = ctx3.get(v);
			}
			r = new Row<>(r, v, ctx.get(v), en2, t1);
		}
		return r;
	}

	@Override
	public String toString() {
		if (tail == null) {
			return "";
		}
		return "(" + v + "=" + x + ")" + tail.toString();
	}

	public String toString(Function<X, String> printX) {
		return map((x, y) -> printX.apply(x)).toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((en2 == null) ? 0 : en2.hashCode());
		result = prime * result + ((tail == null) ? 0 : tail.hashCode());
		result = prime * result + ((v == null) ? 0 : v.hashCode());
		result = prime * result + ((x == null) ? 0 : x.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Row<?, ?> other = (Row<?, ?>) obj;
		if (en2 == null) {
			if (other.en2 != null)
				return false;
		} else if (!en2.equals(other.en2))
			return false;
		if (v == null) {
			if (other.v != null)
				return false;
		} else if (!v.equals(other.v))
			return false;
		if (x == null) {
			if (other.x != null)
				return false;
		} else if (!x.equals(other.x))
			return false;
		if (tail == null) {
			if (other.tail != null)
				return false;
		} else if (!tail.equals(other.tail))
			return false;
		return true;
	}

	// use provable equality as equals fn?
	public boolean rowEquals(BiPredicate<X, X> f, Row<En2, X> e) {
		if (!this.en2.equals(e.en2)) {
			return false;
		}
		if (tail == null) {
			return e.tail == null;
		} else if (v.equals(e.v)) {
			return f.test(x, e.x);
		}
		return Util.anomaly();
	}

	public En2 en2() {
		return en2;
	}
}