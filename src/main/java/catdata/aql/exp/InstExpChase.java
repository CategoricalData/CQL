package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Instance;
import catdata.aql.Kind;

public final class InstExpChase<Gen, Sk, X, Y> extends InstExp<Object, Object, Object, Object> {

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		I.map(f);
		eds.map(f);
	}

	public <R, P, E extends Exception> R accept(P param, InstExpVisitor<R, P, E> v) throws E {
		return v.visit(param, this);
	}

	@Override
	public Collection<InstExp<?, ?, ?, ?>> direct(AqlTyping G) {
		return Collections.singleton(I);
	}

	public final InstExp<Gen, Sk, X, Y> I;

	public final EdsExp eds;

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
		set.add(AqlOption.require_consistency);
		set.add(AqlOption.allow_java_eqs_unsafe);
		set.addAll(AqlOptions.proverOptionNames());
	}

	public final Map<String, String> options;

	@Override
	public Map<String, String> options() {
		return options;
	}

	public InstExpChase(EdsExp eds, InstExp<Gen, Sk, X, Y> i, List<Pair<String, String>> options) {
		I = i;
		this.eds = eds;
		this.options = Util.toMapSafely(options);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((I == null) ? 0 : I.hashCode());
		result = prime * result + ((eds == null) ? 0 : eds.hashCode());
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
		InstExpChase<?, ?, ?, ?> other = (InstExpChase<?, ?, ?, ?>) obj;
		if (I == null) {
			if (other.I != null)
				return false;
		} else if (!I.equals(other.I))
			return false;
		if (eds == null) {
			if (other.eds != null)
				return false;
		} else if (!eds.equals(other.eds))
			return false;
		if (options == null) {
			if (other.options != null)
				return false;
		} else if (!options.equals(other.options))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "chase " + eds + " " + I;
	}

	@Override
	public SchExp type(AqlTyping G) {
		if (!G.eq(I.type(G), eds.type(G))) {
			throw new RuntimeException("type of " + I + ", namely " + I.type(G) + " is not equal to type of " + eds
					+ ", namely " + eds.type(G));
		}
		return I.type(G);
	}

	@SuppressWarnings("unchecked")
	@Override
	public synchronized Instance<Ty, En, Sym, Fk, Att, Object, Object, Object, Object> eval0(AqlEnv env, boolean isC) {
		if (isC) {
			eds.eval(env, true);
			I.eval(env, true);
			throw new IgnoreException();
		}
		Instance<Ty, En, Sym, Fk, Att, ?, ?, ?, ?> ret = eds.eval(env, false).chase(
				(Instance<Ty, En, Sym, Fk, Att, Gen, Sk, Integer, Y>) I.eval(env, false),
				new AqlOptions(options, null, env.defaults));
		return (Instance<Ty, En, Sym, Fk, Att, Object, Object, Object, Object>) ret;
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return Util.union(eds.deps(), I.deps());
	}

}