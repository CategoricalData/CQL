package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Kind;
import catdata.aql.Transform;
import catdata.aql.fdm.DistinctTransform;

public final class TransExpDistinct<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2>
		extends TransExp<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> {

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		t.map(f);
	}

	public final TransExp<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> t;

	@Override
	public Map<String, String> options() {
		return Collections.emptyMap();
	}

	public TransExpDistinct(TransExp<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> t) {
		this.t = t;
	}

	public <R, P, E extends Exception> R accept(P params, TransExpVisitor<R, P, E> v) throws E {
		return v.visit(params, this);
	}

	@Override
	public int hashCode() {
		return t.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TransExpDistinct<?, ?, ?, ?, ?, ?, ?, ?> other = (TransExpDistinct<?, ?, ?, ?, ?, ?, ?, ?>) obj;
		if (t == null) {
			if (other.t != null)
				return false;
		} else if (!t.equals(other.t))
			return false;
		return true;
	}

	@Override
	public Pair<InstExp<Gen1, Sk1, X1, Y1>, InstExp<Gen2, Sk2, X2, Y2>> type(AqlTyping G) {
		return new Pair<>(new InstExpDistinct<>(t.type(G).first), new InstExpDistinct<>(t.type(G).second));
	}

	@Override
	public Transform<Ty, En, Sym, Fk, Att, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> eval0(AqlEnv env, boolean isC) {
		if (isC) {
			t.eval(env, true);
			throw new IgnoreException();
		}
		return new DistinctTransform<>(t.eval(env, false), env.defaults, env.defaults);
	}

	@Override
	public String toString() {
		return "distinct " + t;
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return t.deps();
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {

	}

}