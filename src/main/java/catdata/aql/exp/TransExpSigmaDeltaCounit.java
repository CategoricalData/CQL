package catdata.aql.exp;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Chc;
import catdata.Pair;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Kind;
import catdata.aql.Transform;
import catdata.aql.fdm.SigmaDeltaCounitTransform;

public class TransExpSigmaDeltaCounit<Gen, Sk, X, Y>
		extends TransExp<Pair<En, X>, Y, Gen, Sk, Integer, Chc<Y, Pair<Integer, Att>>, X, Y> {

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		F.map(f);
		I.map(f);
	}

	public <R, P, E extends Exception> R accept(P params, TransExpVisitor<R, P, E> v) throws E {
		return v.visit(params, this);
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
		set.add(AqlOption.require_consistency);
		set.add(AqlOption.allow_java_eqs_unsafe);
		set.addAll(AqlOptions.proverOptionNames());
	}

	public final MapExp F;
	public final InstExp<Gen, Sk, X, Y> I;
	public final Map<String, String> options;

	@Override
	public Map<String, String> options() {
		return options;
	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((F == null) ? 0 : F.hashCode());
		result = prime * result + ((I == null) ? 0 : I.hashCode());
		result = prime * result + ((options == null) ? 0 : options.hashCode());
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
		TransExpSigmaDeltaCounit<?, ?, ?, ?> other = (TransExpSigmaDeltaCounit<?, ?, ?, ?>) obj;
		if (F == null) {
			if (other.F != null)
				return false;
		} else if (!F.equals(other.F))
			return false;
		if (I == null) {
			if (other.I != null)
				return false;
		} else if (!I.equals(other.I))
			return false;
		if (options == null) {
			if (other.options != null)
				return false;
		} else if (!options.equals(other.options))
			return false;
		return true;
	}

	public TransExpSigmaDeltaCounit(MapExp f, InstExp<Gen, Sk, X, Y> i, Map<String, String> options) {
		F = f;
		I = i;
		this.options = options;
	}

	@Override
	public synchronized Transform<Ty, En, Sym, Fk, Att, Pair<En, X>, Y, Gen, Sk, Integer, Chc<Y, Pair<Integer, Att>>, X, Y> eval0(
			AqlEnv env, boolean isC) {
		if (isC) {
			F.eval(env, true);
			I.eval(env, true);
			throw new IgnoreException();
		}
		return new SigmaDeltaCounitTransform<>(F.eval(env, false), I.eval(env, false),
				new AqlOptions(options, null, env.defaults));
	}

	@Override
	public String toString() {
		return "counit " + F + " " + I;
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return Util.union(F.deps(), I.deps());
	}

	@Override
	public Pair<InstExp<Pair<En, X>, Y, Integer, Chc<Y, Pair<Integer, Att>>>, InstExp<Gen, Sk, X, Y>> type(
			AqlTyping G) {
		SchExp x = I.type(G);
		if (!G.eq(x, F.type(G).second)) {
			throw new RuntimeException(
					"In " + this + ", mapping codomain is " + F.type(G).second + " but instance schema is " + x);
		}
		return new Pair<>(new InstExpSigma<>(F, new InstExpDelta<>(F, I), options), I);
	}

}