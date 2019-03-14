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

public class InstExpCod<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> extends InstExp<Gen2, Sk2, X2, Y2> {

	public <R, P, E extends Exception> R accept(P param, InstExpVisitor<R, P, E> v) throws E {
		return v.visit(param, this);
	}

	@Override
	public Map<String, String> options() {
		return Collections.emptyMap();
	}

	public final TransExp<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> t;

	public InstExpCod(TransExp<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> t) {
		this.t = t;
	}

	@Override
	public String toString() {
		return "cod_t " + t;
	}

	@Override
	public int hashCode() {
		return t.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		InstExpCod<?, ?, ?, ?, ?, ?, ?, ?> other = (InstExpCod<?, ?, ?, ?, ?, ?, ?, ?>) obj;
		if (t == null) {
			if (other.t != null)
				return false;
		} else if (!t.equals(other.t))
			return false;
		return true;
	}

	@Override
	public synchronized Instance<Ty, En, Sym, Fk, Att, Gen2, Sk2, X2, Y2> eval0(AqlEnv env, boolean isC) {
		return t.eval(env, isC).dst();
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return t.deps();
	}

	@Override
	public SchExp type(AqlTyping G) {
		return t.type(G).first.type(G);
	}

	@Override
	public Collection<InstExp<?, ?, ?, ?>> direct(AqlTyping G) {
		return Collections.singleton((InstExp<?, ?, ?, ?>) t.type(G).second);
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
	}

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		t.map(f);
	}

}