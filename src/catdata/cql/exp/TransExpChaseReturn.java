package catdata.cql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Chc;
import catdata.Pair;
import catdata.Util;
import catdata.cql.Kind;
import catdata.cql.Transform;
import catdata.cql.AqlOptions.AqlOption;

public class TransExpChaseReturn<N, Gen, Sk, X, Y>
		extends TransExp<Gen, Sk, Pair<N, Gen>, Pair<N, Sk>, X, Y, Integer, Chc<Pair<N, Sk>, Pair<Integer, Att>>> {

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((C == null) ? 0 : C.hashCode());
		result = prime * result + ((I == null) ? 0 : I.hashCode());
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
		TransExpChaseReturn other = (TransExpChaseReturn) obj;
		if (C == null) {
			if (other.C != null)
				return false;
		} else if (!C.equals(other.C))
			return false;
		if (I == null) {
			if (other.I != null)
				return false;
		} else if (!I.equals(other.I))
			return false;
		return true;
	}

	EdsExp C;

	InstExp<Gen, Sk, X, Y> I;

	public TransExpChaseReturn(EdsExp c, InstExp<Gen, Sk, X, Y> i) {
		C = c;
		I = i;
	}

	@Override
	public Pair<InstExp<Gen, Sk, X, Y>, InstExp<Pair<N, Gen>, Pair<N, Sk>, Integer, Chc<Pair<N, Sk>, Pair<Integer, Att>>>> type(
			AqlTyping G) {
		I.type(G);
		C.type(G);
		return new Pair(I, new InstExpChase<>(C, I, Collections.emptyList()));
	}

	@Override
	public <R, P, E extends Exception> R accept(P params, TransExpVisitor<R, P, E> v) throws E {
		return v.visit(params, this);
	}

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		I.mapSubExps(f);
		C.mapSubExps(f);
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
	}

	@Override
	protected Map<String, String> options() {
		return Collections.emptyMap();
	}

	@Override
	protected Transform<String, String, Sym, Fk, Att, Gen, Sk, Pair<N, Gen>, Pair<N, Sk>, X, Y, Integer, Chc<Pair<N, Sk>, Pair<Integer, Att>>> eval0(
			AqlEnv env, boolean isCompileTime) {
		var i = I.eval(env, isCompileTime);
		var c = C.eval(env, isCompileTime);
		var d = c.chase(i, env.defaults).second;
		return d;
	}

	@Override
	public String toString() {
		return "chase_return " + C + " " + I;
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return Util.union(C.deps(), I.deps());
	}
}
