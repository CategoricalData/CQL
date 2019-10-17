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
import catdata.aql.Kind;
import catdata.aql.Transform;
import catdata.aql.Var;
import catdata.aql.fdm.CoEvalTransform;

public class TransExpCoEval<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> extends
		TransExp<Triple<Var, X1, En>, Chc<Triple<Var, X1, En>, Y1>, Triple<Var, X2, En>, Chc<Triple<Var, X2, En>, Y2>, Integer, Chc<Chc<Triple<Var, X1, En>, Y1>, Pair<Integer, Att>>, Integer, Chc<Chc<Triple<Var, X2, En>, Y2>, Pair<Integer, Att>>> {

	public final QueryExp Q;
	public final TransExp<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> t;
	private final Map<String, String> options1, options2;
	private final List<Pair<String, String>> o1, o2;

	@Override
	public Map<String, String> options() {
		return Collections.emptyMap();
	}

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		Q.map(f);
		t.map(f);
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {

		set.add(AqlOption.require_consistency);
		set.add(AqlOption.allow_java_eqs_unsafe);
	}

	public TransExpCoEval(QueryExp q, TransExp<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> t, List<Pair<String, String>> o1,
			List<Pair<String, String>> o2) {
		this.t = t;
		Q = q;
		options1 = Util.toMapSafely(o1);
		options2 = Util.toMapSafely(o2);
		this.o1 = o1;
		this.o2 = o2;
	}

	@Override
	public Pair<InstExp<Triple<Var, X1, En>, Chc<Triple<Var, X1, En>, Y1>, Integer, Chc<Chc<Triple<Var, X1, En>, Y1>, Pair<Integer, Att>>>, InstExp<Triple<Var, X2, En>, Chc<Triple<Var, X2, En>, Y2>, Integer, Chc<Chc<Triple<Var, X2, En>, Y2>, Pair<Integer, Att>>>> type(
			AqlTyping G) {
		if (!t.type(G).first.type(G).equals(Q.type(G).second)) {
			throw new RuntimeException(
					"Target of query is " + t.type(G).second.type(G) + " but transform is on " + t.type(G).first);
		}
		return new Pair<>(new InstExpCoEval<>(Q, t.type(G).first, o1), new InstExpCoEval<>(Q, t.type(G).second, o2));
	}

	@Override
	public Transform<Ty, En, Sym, Fk, Att, Triple<Var, X1, En>, Chc<Triple<Var, X1, En>, Y1>, Triple<Var, X2, En>, Chc<Triple<Var, X2, En>, Y2>, Integer, Chc<Chc<Triple<Var, X1, En>, Y1>, Pair<Integer, Att>>, Integer, Chc<Chc<Triple<Var, X2, En>, Y2>, Pair<Integer, Att>>> eval0(
			AqlEnv env, boolean isC) {
		if (isC) {
			Q.eval(env, true);
			t.eval(env, true);
			throw new IgnoreException();
		}
		return new CoEvalTransform<>(Q.eval(env, false), t.eval(env, false),
				new AqlOptions(options1, env.defaults), new AqlOptions(options2, env.defaults));
	}

	@Override
	public String toString() {
		return "coeval " + Q + " " + t;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((Q == null) ? 0 : Q.hashCode());
		result = prime * result + ((o1 == null) ? 0 : o1.hashCode());
		result = prime * result + ((o2 == null) ? 0 : o2.hashCode());
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
		TransExpCoEval<?, ?, ?, ?, ?, ?, ?, ?> other = (TransExpCoEval<?, ?, ?, ?, ?, ?, ?, ?>) obj;
		if (Q == null) {
			if (other.Q != null)
				return false;
		} else if (!Q.equals(other.Q))
			return false;
		if (o1 == null) {
			if (other.o1 != null)
				return false;
		} else if (!o1.equals(other.o1))
			return false;
		if (o2 == null) {
			if (other.o2 != null)
				return false;
		} else if (!o2.equals(other.o2))
			return false;
		if (t == null) {
			if (other.t != null)
				return false;
		} else if (!t.equals(other.t))
			return false;
		return true;
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return Util.union(Q.deps(), t.deps());
	}

	public <R, P, E extends Exception> R accept(P params, TransExpVisitor<R, P, E> v) throws E {
		return v.visit(params, this);
	}

}