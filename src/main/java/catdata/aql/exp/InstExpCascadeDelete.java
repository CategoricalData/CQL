package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.Util;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.FilterAlgebra;
import catdata.aql.Instance;
import catdata.aql.Kind;
import catdata.aql.Schema;

public class InstExpCascadeDelete<Gen, Sk, X, Y> extends InstExp<X, Y, X, Y> {

	final InstExp<Gen, Sk, X, Y> I;

	final SchExp sch;

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		I.map(f);
		sch.map(f);
	}

	public InstExpCascadeDelete(InstExp<Gen, Sk, X, Y> i, SchExp sch) {
		I = i;
		this.sch = sch;
	}

	@Override
	public <R, P, E extends Exception> R accept(P param, InstExpVisitor<R, P, E> v) throws E {
		return v.visit(param, this);
	}

	@Override
	public SchExp type(AqlTyping G) {
		SchExp x = I.type(G);
		TyExp xx = sch.type(G);
		if (!xx.equals(x.type(G))) {
			throw new RuntimeException("Typeside mismatch on cascade delete.");
		}
		return sch;
	}

	@Override
	public Collection<InstExp<?, ?, ?, ?>> direct(AqlTyping G) {
		return Collections.singleton(I);
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
	}

	@Override
	protected Map<String, String> options() {
		return Collections.emptyMap();
	}

	@Override
	public Instance<Ty, En, Sym, Fk, Att, X, Y, X, Y> eval0(AqlEnv env, boolean isC) {
		Schema<Ty, En, Sym, Fk, Att> sch0 = sch.eval(env, isC);
		Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> i = I.eval(env, isC);
		if (isC) {
			throw new IgnoreException();
		}
		return FilterAlgebra.filterInstance(i, sch0);
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return Util.union(I.deps(), sch.deps());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((I == null) ? 0 : I.hashCode());
		result = prime * result + ((sch == null) ? 0 : sch.hashCode());
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
		InstExpCascadeDelete<?, ?, ?, ?> other = (InstExpCascadeDelete<?, ?, ?, ?>) obj;
		if (I == null) {
			if (other.I != null)
				return false;
		} else if (!I.equals(other.I))
			return false;
		if (sch == null) {
			if (other.sch != null)
				return false;
		} else if (!sch.equals(other.sch))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "cascade_delete " + I + " : " + sch;
	}

}
