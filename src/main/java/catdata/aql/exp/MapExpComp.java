package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.Util;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Kind;
import catdata.aql.Mapping;

public final class MapExpComp 
extends MapExp {
	
	public <R,P,E extends Exception> R accept(P params, MapExpVisitor<R, P, E> v) throws E {
		return v.visit(params, this);
	}
	
	@Override
	public <R, P, E extends Exception> MapExp coaccept(
			P params, MapExpCoVisitor<R, P, E> v, R r) throws E {
		return v.visitMapExpComp(params, r);
	}
	@Override
	protected void allowedOptions(Set<AqlOption> set) {
		
	}
	
	public final MapExp m1, m2;

	@Override
	public Map<String, String> options() {
		return Collections.emptyMap();
	}
	public MapExpComp(MapExp m1, MapExp m2) {
		this.m1 = m1;
		this.m2 = m2;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((m1 == null) ? 0 : m1.hashCode());
		result = prime * result + ((m2 == null) ? 0 : m2.hashCode());
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
		MapExpComp other = (MapExpComp) obj;
		if (m1 == null) {
			if (other.m1 != null)
				return false;
		} else if (!m1.equals(other.m1))
			return false;
		if (m2 == null) {
			if (other.m2 != null)
				return false;
		} else if (!m2.equals(other.m2))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "[" + m1 + " ; " + m2 + "]";
	}

	@Override
	public Pair<SchExp, SchExp> type(AqlTyping G) {
		if (!G.eq(m1.type(G).second, m2.type(G).first)) {
			throw new RuntimeException("Cod of first arg, " + m1.type(G).second + " is not equal to dom of second arg, " + m2.type(G).first + " in " + this);
		}
		return new Pair<>(m1.type(G).first, m2.type(G).second);
	}

	@Override
	public Mapping<Ty, En, Sym, Fk, Att, En, Fk, Att> eval0(AqlEnv env, boolean isC) {
		Mapping<Ty, En, Sym, Fk, Att, En, Fk, Att> n1 = m1.eval(env, isC);
		Mapping<Ty, En, Sym, Fk, Att, En, Fk, Att> n2 = m2.eval(env, isC);
		if (isC) {
			throw new IgnoreException();
		}
		return Mapping.compose(n1, n2);
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return Util.union(m1.deps(), m2.deps());
	}
	
	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		m1.map(f);
		m2.map(f);
	}

	
}