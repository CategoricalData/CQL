package catdata.cql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.Util;
import catdata.cql.Constraints;
import catdata.cql.Kind;
import catdata.cql.Mapping;
import catdata.cql.AqlOptions.AqlOption;

public class EdsExpSigma extends EdsExp {

	MapExp F;

	EdsExp C;

	public EdsExpSigma(MapExp f, EdsExp c) {
		F = f;
		C = c;
		if (c == null) Util.anomaly();
		if (f == null) Util.anomaly();
	}

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
		return Util.union(F.deps(), C.deps());
	}

	@Override
	public synchronized Constraints eval0(AqlEnv env, boolean isC) {
		Mapping f = F.eval(env, isC);
		Constraints c = C.eval(env, isC);
		return c.sigma(f, env.defaults);
	}

	@Override
	public String toString() {
		return "sigma " + F + " " + C;
	}

	@Override
	public SchExp type(AqlTyping G) {
		var st = F.type(G);
		var s = C.type(G);
		if (!st.first.equals(s)) {
			throw new RuntimeException("In " + this + ", mapping source is " + st.first + " but constraint schema is " + s);
		}
		return st.second;
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
	}

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		F.mapSubExps(f);
		C.mapSubExps(f);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((C == null) ? 0 : C.hashCode());
		result = prime * result + ((F == null) ? 0 : F.hashCode());
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
		EdsExpSigma other = (EdsExpSigma) obj;
		if (C == null) {
			if (other.C != null)
				return false;
		} else if (!C.equals(other.C))
			return false;
		if (F == null) {
			if (other.F != null)
				return false;
		} else if (!F.equals(other.F))
			return false;
		return true;
	}

}