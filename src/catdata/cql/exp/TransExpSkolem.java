package catdata.cql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.cql.Instance;
import catdata.cql.Kind;
import catdata.cql.Transform;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.fdm.SkolemInstance;

public class TransExpSkolem<Gen, Sk, X, Y> extends TransExp<Gen, Pair<X, Att>, Gen, Sk, X, Pair<X, Att>, X, Y> {

	public final InstExp<Gen, Sk, X, Y> I;

	public TransExpSkolem(InstExp<Gen, Sk, X, Y> i) {
		I = i;
	}

	@Override
	public Pair<InstExp<Gen, Pair<X, Att>, X, Pair<X, Att>>, InstExp<Gen, Sk, X, Y>> type(AqlTyping G) {
		I.type(G);
		return new Pair<>(new InstExpSkolem<>(I), I);
	}

	@Override
	public <R, P, E extends Exception> R accept(P params, TransExpVisitor<R, P, E> v) throws E {
		return v.visit(params, this);
	}

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		I.map(f);
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
	}

	@Override
	protected Map<String, String> options() {
		return Collections.emptyMap();
	}

	@Override
	protected Transform<String, String, Sym, Fk, Att, Gen, Pair<X, Att>, Gen, Sk, X, Pair<X, Att>, X, Y> eval0(
			AqlEnv env, boolean isCompileTime) {
		Instance<String, String, Sym, Fk, Att, Gen, Sk, X, Y> ii = I.eval(env, isCompileTime);

		return new SkolemInstance<>(ii).trans;
	}

	@Override
	public String toString() {
		return "skolem " + I;
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return I.deps();
	}

	@Override
	public int hashCode() {
		return Objects.hash(I);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TransExpSkolem other = (TransExpSkolem) obj;
		return Objects.equals(I, other.I);
	}

}
