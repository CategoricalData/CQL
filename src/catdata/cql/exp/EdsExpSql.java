package catdata.cql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.cql.Constraints;
import catdata.cql.Kind;
import catdata.cql.SqlTypeSide;
import catdata.cql.AqlOptions.AqlOption;

public class EdsExpSql extends EdsExp {

	SchExp S;

	@Override
	public <R, P, E extends Exception> R accept(P params, EdsExpVisitor<R, P, E> v) throws E {
		return v.visit(params, this);
	}

	@Override
	public Map<String, String> options() {
		return Collections.emptyMap();
	}

	@Override
	public boolean isVar() {
		return false;
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return S.deps();
	}

	public EdsExpSql(SchExp S) {
		this.S = S;
	}

	@Override
	public synchronized Constraints eval0(AqlEnv env, boolean isC) {
		return SqlTypeSide.makeEds(S.eval(env, isC), env.defaults);
	}


	@Override
	public String toString() {
		return "sql " + S;
	}

	@Override
	public SchExp type(AqlTyping G) {
		S.type(G);
		return S;
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
	}

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		S.mapSubExps(f);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((S == null) ? 0 : S.hashCode());
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
		EdsExpSql other = (EdsExpSql) obj;
		if (S == null) {
			if (other.S != null)
				return false;
		} else if (!S.equals(other.S))
			return false;
		return true;
	}
}