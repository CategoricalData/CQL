package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.Util;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Kind;
import catdata.aql.Transform;
import catdata.aql.fdm.DiffInstance;

public final class TransExpDiffReturn<Gen, Sk, X, Y> extends TransExp<X, Y, Gen, Sk, X, Y, X, Y> {

	public TransExpDiffReturn(InstExp<Gen, Sk, X, Y> i, InstExp<Gen, Sk, X, Y> j) {
		I = i;
		J = j;
	}

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		I.map(f);
		J.map(f);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((I == null) ? 0 : I.hashCode());
		result = prime * result + ((J == null) ? 0 : J.hashCode());
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
		TransExpDiffReturn<?, ?, ?, ?> other = (TransExpDiffReturn<?, ?, ?, ?>) obj;
		if (I == null) {
			if (other.I != null)
				return false;
		} else if (!I.equals(other.I))
			return false;
		if (J == null) {
			if (other.J != null)
				return false;
		} else if (!J.equals(other.J))
			return false;
		return true;
	}

	public final InstExp<Gen, Sk, X, Y> I, J;

	@Override
	public Map<String, String> options() {
		return Collections.emptyMap();
	}

	public <R, P, E extends Exception> R accept(P params, TransExpVisitor<R, P, E> v) throws E {
		return v.visit(params, this);
	}

	@Override
	public Pair<InstExp<X, Y, X, Y>, InstExp<Gen, Sk, X, Y>> type(AqlTyping G) {
		return new Pair<>(new InstExpDiff<>(I, J), I);
	}

	@Override
	public Transform<Ty, En, Sym, Fk, Att, X, Y, Gen, Sk, X, Y, X, Y> eval0(AqlEnv env, boolean isC) {
		if (isC) {
			I.eval(env, true);
			J.eval(env, true);
			throw new IgnoreException();
		}
		return new DiffInstance<>(I.eval(env, false), J.eval(env, false), true, false).h;
	}

	@Override
	public String toString() {
		return "except_return " + I + " " + J;
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return Util.union(I.deps(), J.deps());
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {

	}

}
