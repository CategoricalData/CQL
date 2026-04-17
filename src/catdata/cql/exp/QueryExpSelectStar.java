package catdata.cql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.Kind;
import catdata.cql.Query;

public class QueryExpSelectStar extends QueryExp {

	public final QueryExp Q;

	public QueryExpSelectStar(QueryExp q) {
		Q = q;
	}

	@Override
	public Pair<SchExp, SchExp> type(AqlTyping G) {
		Pair<SchExp, SchExp> p = Q.type(G);
		return new Pair<>(p.first, new SchExpCod(this));
	}

	@Override
	public <R, P, E extends Exception> R accept(P params, QueryExpVisitor<R, P, E> v) throws E {
		return v.visit(params, this);
	}

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		Q.mapSubExps(f);
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {

	}

	@Override
	protected Map<String, String> options() {
		return Collections.emptyMap();
	}

	@Override
	protected Query<String, String, Sym, Fk, Att, String, Fk, Att> eval0(AqlEnv env, boolean isCompileTime) {
		Query<String, String, Sym, Fk, Att, String, Fk, Att> q = Q.eval(env, isCompileTime);

		return Query.makeSelect(env.defaults, q);

	}

	static int[] i = { 0 };

	@Override
	public int hashCode() {
		return Objects.hash(Q);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		QueryExpSelectStar other = (QueryExpSelectStar) obj;
		return Objects.equals(Q, other.Q);
	}

	@Override
	public String toString() {
		return "select_star " + Q;
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return Q.deps();
	}

}
