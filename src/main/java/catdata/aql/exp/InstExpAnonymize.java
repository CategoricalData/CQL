package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Instance;
import catdata.aql.Kind;
import catdata.aql.fdm.Anonymized;

public final class InstExpAnonymize<Gen, Sk, X, Y>
		extends InstExp<Gen, Sk, X, Y> {

	public <R, P, E extends Exception> R accept(P param, InstExpVisitor<R, P, E> v) throws E {
		return v.visit(param, this);
	}

	public final InstExp<Gen, Sk, X, Y> I;

	@Override
	public Collection<InstExp<?, ?, ?, ?>> direct(AqlTyping G) {
		return Collections.singleton(I);
	}

	@Override
	public Map<String, String> options() {
		return Collections.emptyMap();
	}

	public InstExpAnonymize(InstExp<Gen, Sk, X, Y> i) {
		I = i;
	}

	@Override
	public SchExp type(AqlTyping G) {
		return I.type(G);
	}

	@Override
	public synchronized Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> eval0(AqlEnv env, boolean isC) {
		if (isC) {
			I.eval(env, true);
			throw new IgnoreException();
		}
		return new Anonymized<>(I.eval(env, false));
	}

	@Override
	public String toString() {
		return "anonymize " + I;
	}

	@Override
	public int hashCode() {
		return I.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		InstExpAnonymize other = (InstExpAnonymize) obj;
		if (I == null) {
			if (other.I != null)
				return false;
		} else if (!I.equals(other.I))
			return false;
		return true;
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return I.deps();
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
	}

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		I.map(f);
	}

}