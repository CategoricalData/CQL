package catdata.cql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.cql.Constraints;
import catdata.cql.Kind;
import catdata.cql.AqlOptions.AqlOption;

public class EdsExpTinkerpop extends EdsExp {

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
		return Collections.emptyList();
	}

	public EdsExpTinkerpop() {
	}

	@Override
	public synchronized Constraints eval0(AqlEnv env, boolean isC) {
		return InstExpTinkerpop.makeEds().eval(env, isC);
	}

	@Override
	public int hashCode() {
		return 0;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EdsExpTinkerpop other = (EdsExpTinkerpop) obj;
		return true;
	}

	@Override
	public String toString() {
		return "tinkerpop";
	}

	@Override
	public SchExp type(AqlTyping G) {
		return new SchExpTinkerpop();
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
	}

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {

	}
}