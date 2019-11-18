package catdata.aql.exp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Constraints;
import catdata.aql.ED;
import catdata.aql.Kind;
import catdata.aql.Query;
import catdata.aql.exp.TyExp.TyExpSch;

public class QueryExpFromEds extends QueryExp {

	private final EdsExp eds;
	private final int n;

	public QueryExpFromEds(EdsExp eds, int n) {
		this.eds = eds;
		this.n = n;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((eds == null) ? 0 : eds.hashCode());
		result = prime * result + n;
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
		QueryExpFromEds other = (QueryExpFromEds) obj;
		if (n != other.n)
			return false;
		if (eds == null) {
			if (other.eds != null)
				return false;
		} else if (!eds.equals(other.eds))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "fromConstraints " + eds + " " + n;
	}

	@Override
	public Pair<SchExp, SchExp> type(AqlTyping G) {
		SchExp x = eds.type(G);
		TyExp t = new TyExpSch(x);
		List<String> e = new ArrayList<>(2);
		e.add(ED.FRONT.str);
		e.add(ED.BACK.str);
		List<Pair<String, Pair<String, String>>> f = Collections
				.singletonList(new Pair<>(ED.UNIT.str, new Pair<>(ED.BACK.str, ED.FRONT.str)));
		SchExpRaw z = new SchExpRaw(t, Collections.emptyList(), e, f, Collections.emptyList(), Collections.emptyList(),
				Collections.emptyList(), Collections.emptyList(), new Object());
		return new Pair<>(x, z);
	}

	@Override
	public <R, P, E extends Exception> R accept(P params, QueryExpVisitor<R, P, E> v) {
		return v.visit(params, this);
	}

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		eds.map(f);
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
	}

	@Override
	protected Map<String, String> options() {
		return Collections.emptyMap();
	}

	@Override
	protected Query<Ty, En, Sym, Fk, Att, En, Fk, Att> eval0(AqlEnv env, boolean isC) {
		Constraints c = eds.eval(env, isC);
		if (n >= c.eds.size()) {
			throw new RuntimeException("Not enough EDS: " + c.eds.size());
		}
		return c.eds.get(n).getQ(c.schema);
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return eds.deps();
	}

}
