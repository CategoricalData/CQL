package catdata.aql.exp;

import java.util.Collection;
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
import catdata.aql.Term;
import catdata.aql.Transform;
import catdata.aql.Var;
import catdata.aql.fdm.CoEvalEvalCoUnitTransform;
import catdata.aql.fdm.Row;

public class TransExpCoEvalEvalCoUnit<Gen, Sk, X, Y> extends
		TransExp<Triple<Var, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>>, En>, Chc<Triple<Var, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>>, En>, Y>, Gen, Sk, Integer, Chc<Chc<Triple<Var, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>>, En>, Y>, Pair<Integer, Att>>, X, Y> {

	public final QueryExp Q;
	public final InstExp<Gen, Sk, X, Y> I;
	public final Map<String, String> options;

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		Q.map(f);
		I.map(f);
	}

	public <R, P, E extends Exception> R accept(P params, TransExpVisitor<R, P, E> v) throws E {
		return v.visit(params, this);
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
		set.add(AqlOption.eval_max_temp_size);
		set.add(AqlOption.eval_reorder_joins);
		set.add(AqlOption.eval_max_plan_depth);
		set.add(AqlOption.eval_join_selectivity);
		set.add(AqlOption.eval_use_indices);
		set.add(AqlOption.eval_use_sql_above);
		set.add(AqlOption.eval_approx_sql_unsafe);
		set.add(AqlOption.eval_sql_persistent_indices);
		set.add(AqlOption.query_remove_redundancy);
		set.add(AqlOption.varchar_length);
		set.add(AqlOption.start_ids_at);
		set.add(AqlOption.require_consistency);
		set.add(AqlOption.allow_java_eqs_unsafe);
		set.addAll(AqlOptions.proverOptionNames());
	}

	@Override
	public Map<String, String> options() {
		return options;
	}

	public TransExpCoEvalEvalCoUnit(QueryExp q, InstExp<Gen, Sk, X, Y> i, Map<String, String> options) {
		Q = q;
		I = i;
		this.options = options;
	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((I == null) ? 0 : I.hashCode());
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
		TransExpCoEvalEvalCoUnit<?, ?, ?, ?> other = (TransExpCoEvalEvalCoUnit<?, ?, ?, ?>) obj;
		if (I == null) {
			if (other.I != null)
				return false;
		} else if (!I.equals(other.I))
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
	public String toString() {
		return "counit_query " + Q + " " + I;
	}

	@Override
	public Pair<InstExp<Triple<catdata.aql.Var, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>>, En>, Chc<Triple<catdata.aql.Var, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>>, En>, Y>, Integer, Chc<Chc<Triple<catdata.aql.Var, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>>, En>, Y>, Pair<Integer, Att>>>, InstExp<Gen, Sk, X, Y>> type(
			AqlTyping G) {
		if (!Q.type(G).first.equals(I.type(G))) {
			throw new RuntimeException("Q has src schema " + Q.type(G).first + " but instance has schema " + I.type(G));
		}
		return new Pair<>(new InstExpCoEval<>(Q, new InstExpEval<>(Q, I, Util.toList(options)), Util.toList(options)),
				I);
	}

	@Override
	public Transform<Ty, En, Sym, Fk, Att, Triple<catdata.aql.Var, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>>, En>, Chc<Triple<catdata.aql.Var, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>>, En>, Y>, Gen, Sk, Integer, Chc<Chc<Triple<catdata.aql.Var, Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>>, En>, Y>, Pair<Integer, Att>>, X, Y> eval0(
			AqlEnv env, boolean isC) {
		if (isC) {
			Q.eval(env, true);
			I.eval(env, true);
			throw new IgnoreException();
		}
		return new CoEvalEvalCoUnitTransform<>(Q.eval(env, false), I.eval(env, false),
				new AqlOptions(options, env.defaults));
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return Util.union(Q.deps(), I.deps());
	}

}