package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
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
import catdata.aql.fdm.SigmaTransform;

public final class TransExpSigma<Gen1, Gen2, Sk1, Sk2, X1, Y1, X2, Y2> extends
		TransExp<Gen1, Sk1, Gen2, Sk2, Integer, Chc<Sk1, Pair<Integer, Att>>, Integer, Chc<Sk2, Pair<Integer, Att>>> {

	public final MapExp F;
	public final TransExp<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> t;

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		F.map(f);
		t.map(f);
	}

	public final Map<String, String> options1, options2;

	@Override
	public Map<String, String> options() {
		return Collections.emptyMap();
	}

	public TransExpSigma(MapExp F, TransExp<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> t, Map<String, String> options1,
			Map<String, String> options2) {
		this.F = F;
		this.t = t;
		this.options1 = options1;
		this.options2 = options2;
	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((F == null) ? 0 : F.hashCode());
		result = prime * result + ((options1 == null) ? 0 : options1.hashCode());
		result = prime * result + ((options2 == null) ? 0 : options2.hashCode());
		result = prime * result + ((t == null) ? 0 : t.hashCode());
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
		TransExpSigma<?, ?, ?, ?, ?, ?, ?, ?> other = (TransExpSigma<?, ?, ?, ?, ?, ?, ?, ?>) obj;
		if (F == null) {
			if (other.F != null)
				return false;
		} else if (!F.equals(other.F))
			return false;
		if (options1 == null) {
			if (other.options1 != null)
				return false;
		} else if (!options1.equals(other.options1))
			return false;
		if (options2 == null) {
			if (other.options2 != null)
				return false;
		} else if (!options2.equals(other.options2))
			return false;
		if (t == null) {
			if (other.t != null)
				return false;
		} else if (!t.equals(other.t))
			return false;
		return true;
	}

	@Override
	public Pair<InstExp<Gen1, Sk1, Integer, Chc<Sk1, Pair<Integer, Att>>>, InstExp<Gen2, Sk2, Integer, Chc<Sk2, Pair<Integer, Att>>>> type(
			AqlTyping G) {
		Pair<InstExp<Gen1, Sk1, X1, Y1>, InstExp<Gen2, Sk2, X2, Y2>> x = t.type(G);
		if (!G.eq(x.first.type(G), F.type(G).first)) {
			throw new RuntimeException("In " + this + ", mapping domain is " + F.type(G).first
					+ " but transform domain schema is " + x.first.type(G));
		}
		InstExp<Gen1, Sk1, Integer, Chc<Sk1, Pair<Integer, Att>>> a = new InstExpSigma<>(F, x.first, options1);
		InstExp<Gen2, Sk2, Integer, Chc<Sk2, Pair<Integer, Att>>> b = new InstExpSigma<>(F, x.second, options2);
		return new Pair<>(a, b);
	}

	@Override
	public synchronized Transform<Ty, En, Sym, Fk, Att, Gen1, Sk1, Gen2, Sk2, Integer, Chc<Sk1, Pair<Integer, Att>>, Integer, Chc<Sk2, Pair<Integer, Att>>> eval0(
			AqlEnv env, boolean isC) {
		if (isC) {
			F.eval(env, true);
			t.eval(env, true);
			throw new IgnoreException();
		}
		return new SigmaTransform<>(F.eval(env, false), t.eval(env, false),
				new AqlOptions(options1, null, env.defaults), new AqlOptions(options2, null, env.defaults));
	}

	@Override
	public String toString() {
		return "sigma " + F + " " + t;
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return Util.union(F.deps(), t.deps());
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

}