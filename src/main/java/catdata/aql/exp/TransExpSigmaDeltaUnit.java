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
import catdata.aql.fdm.SigmaDeltaUnitTransform;

public final class TransExpSigmaDeltaUnit<Gen, Sk, X, Y> extends
		TransExp<Gen, Sk, Pair<En, Integer>, Chc<Sk, Pair<Integer, Att>>, X, Y, Pair<En, Integer>, Chc<Sk, Pair<Integer, Att>>> {

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		F.map(f);
		I.map(f);
	}

	public <R, P, E extends Exception> R accept(P params, TransExpVisitor<R, P, E> v) throws E {
		return v.visit(params, this);
	}

	public final MapExp F;
	public final InstExp<Gen, Sk, X, Y> I;

	public final Map<String, String> options;

	@Override
	public Map<String, String> options() {
		return options;
	}

	public TransExpSigmaDeltaUnit(MapExp f, InstExp<Gen, Sk, X, Y> i, Map<String, String> options) {
		F = f;
		I = i;
		this.options = options;
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
		TransExpSigmaDeltaUnit<?, ?, ?, ?> other = (TransExpSigmaDeltaUnit<?, ?, ?, ?>) obj;
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

	@Override
	public Pair<InstExp<Gen, Sk, X, Y>, InstExp<Pair<En, Integer>, Chc<Sk, Pair<Integer, Att>>, Pair<En, Integer>, Chc<Sk, Pair<Integer, Att>>>> type(
			AqlTyping G) {
		SchExp x = I.type(G);
		if (!G.eq(x, F.type(G).first)) {
			throw new RuntimeException(
					"In " + this + ", mapping domain is " + F.type(G).first + " but instance schema is " + x);
		}
		return new Pair<>(I, new InstExpDelta<>(F, new InstExpSigma<>(F, I, options)));
	}

	@Override
	public synchronized SigmaDeltaUnitTransform<Ty, En, Sym, Fk, Att, Gen, Sk, En, Fk, Att, X, Y> eval0(AqlEnv env,
			boolean isC) {
		if (isC) {
			F.eval(env, true);
			I.eval(env, true);
			throw new IgnoreException();
		}
		return new SigmaDeltaUnitTransform<>(F.eval(env, false), I.eval(env, false),
				new AqlOptions(options, env.defaults));
	}

	@Override
	public String toString() {
		return "unit " + F + " " + I;
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return Util.union(F.deps(), I.deps());
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
		set.add(AqlOption.require_consistency);
		set.add(AqlOption.allow_java_eqs_unsafe);
		set.addAll(AqlOptions.proverOptionNames());
	}
}