package catdata.cql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.Util;
import catdata.cql.Kind;
import catdata.cql.Transform;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.fdm.ComposeTransform;

public class TransExpCompose<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2, Gen3, Sk3, X3, Y3>
		extends TransExp<Gen1, Sk1, Gen3, Sk3, X1, Y1, X3, Y3> {

	public final TransExp<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> t1;
	public final TransExp<Gen2, Sk2, Gen3, Sk3, X2, Y2, X3, Y3> t2;

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		t1.map(f);
		t2.map(f);
	}

	public <R, P, E extends Exception> R accept(P params, TransExpVisitor<R, P, E> v) throws E {
		return v.visit(params, this);
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {

	}

	@Override
	public Map<String, String> options() {
		return Collections.emptyMap();
	}

	public TransExpCompose(TransExp<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> t1,
			TransExp<Gen2, Sk2, Gen3, Sk3, X2, Y2, X3, Y3> t2) {
		this.t1 = t1;
		this.t2 = t2;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((t1 == null) ? 0 : t1.hashCode());
		result = prime * result + ((t2 == null) ? 0 : t2.hashCode());
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
		TransExpCompose<?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?> other = (TransExpCompose<?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?>) obj;
		if (t1 == null) {
			if (other.t1 != null)
				return false;
		} else if (!t1.equals(other.t1))
			return false;
		if (t2 == null) {
			if (other.t2 != null)
				return false;
		} else if (!t2.equals(other.t2))
			return false;
		return true;
	}

	@Override
	public Pair<InstExp<Gen1, Sk1, X1, Y1>, InstExp<Gen3, Sk3, X3, Y3>> type(AqlTyping G) {
		Pair<InstExp<Gen1, Sk1, X1, Y1>, InstExp<Gen2, Sk2, X2, Y2>> l = t1.type(G);
		Pair<InstExp<Gen2, Sk2, X2, Y2>, InstExp<Gen3, Sk3, X3, Y3>> r = t2.type(G);
		if (!l.second.equals(r.first)) {
			throw new RuntimeException("Anomaly: in compose transform, dst of t1 is \n\n" + l.second
					+ " \n\n but src of t2 is \n\n" + r.first);
		}
		return new Pair<>(l.first, r.second);
	}

	@Override
	public Transform<String, String, Sym, Fk, Att, Gen1, Sk1, Gen3, Sk3, X1, Y1, X3, Y3> eval0(AqlEnv env,
			boolean isC) {
		if (isC) {
			t1.eval(env, true);
			t2.eval(env, true);
			throw new IgnoreException();
		}
		return new ComposeTransform<>(t1.eval(env, false), t2.eval(env, false));
	}

	@Override
	public String toString() {
		return "[" + t1 + " ; " + t2 + "]";
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return Util.union(t1.deps(), t2.deps());
	}

}