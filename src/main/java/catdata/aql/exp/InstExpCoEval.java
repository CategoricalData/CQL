package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Instance;
import catdata.aql.Kind;
import catdata.aql.Var;
import catdata.aql.fdm.CoEvalInstance;

public final class InstExpCoEval<Gen, Sk, X, Y> extends
		InstExp<Triple<Var, X, En>, Chc<Triple<Var, X, En>, Y>, Integer, Chc<Chc<Triple<Var, X, En>, Y>, Pair<Integer, Att>>> {

	public final QueryExp Q;
	public final InstExp<Gen, Sk, X, Y> J;
	public final Map<String, String> options;

	public <R, P, E extends Exception> R accept(P param, InstExpVisitor<R, P, E> v) throws E {
		return v.visit(param, this);
	}

	@Override
	public Collection<InstExp<?, ?, ?, ?>> direct(AqlTyping G) {
		return Collections.singleton(J);
	}

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		J.map(f);
		Q.map(f);
	}

	@Override
	public Map<String, String> options() {
		return options;
	}

	public InstExpCoEval(QueryExp q, InstExp<Gen, Sk, X, Y> j, List<Pair<String, String>> options) {
		Q = q;
		J = j;
		this.options = Util.toMapSafely(options);
	}

	@Override
	public String toString() {
		return "coeval " + Q + " " + J;
	}

	@Override
	public SchExp type(AqlTyping G) {
		if (!J.type(G).equals(Q.type(G).second)) { // TODO aql schema equality
			throw new RuntimeException(
					"Schema of instance is " + J.type(G) + " but target of query is " + Q.type(G).second);
		}
		return Q.type(G).first;
	}

	@Override
	public synchronized Instance<Ty, En, Sym, Fk, Att, Triple<catdata.aql.Var, X, En>, Chc<Triple<catdata.aql.Var, X, En>, Y>, Integer, Chc<Chc<Triple<catdata.aql.Var, X, En>, Y>, Pair<Integer, Att>>> eval0(
			AqlEnv env, boolean isC) {
		if (isC) {
			Q.eval(env, true);
			J.eval(env, true);
			throw new IgnoreException();
		}
		return new CoEvalInstance<>(Q.eval(env, false), J.eval(env, false),
				new AqlOptions(options, null, env.defaults));
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return Util.union(J.deps(), Q.deps());
	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((J == null) ? 0 : J.hashCode());
		result = prime * result + ((Q == null) ? 0 : Q.hashCode());
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
		InstExpCoEval<?, ?, ?, ?> other = (InstExpCoEval<?, ?, ?, ?>) obj;
		if (J == null) {
			if (other.J != null)
				return false;
		} else if (!J.equals(other.J))
			return false;
		if (Q == null) {
			if (other.Q != null)
				return false;
		} else if (!Q.equals(other.Q))
			return false;
		if (options == null) {
			if (other.options != null)
				return false;
		} else if (!options.equals(other.options))
			return false;
		return true;
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
		set.add(AqlOption.require_consistency);
		set.add(AqlOption.allow_java_eqs_unsafe);
		set.addAll(AqlOptions.proverOptionNames());
	}

}