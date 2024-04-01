package catdata.cql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Chc;
import catdata.Pair;
import catdata.Unit;
import catdata.cql.Instance;
import catdata.cql.Kind;
import catdata.cql.Pragma;
import catdata.cql.AqlOptions.AqlOption;

@SuppressWarnings("hiding")
public final class PragmaExpConsistent<X, Y> extends PragmaExp {
	public final InstExp<String, String, X, Y> I;

	public <R, P, E extends Exception> R accept(P params, PragmaExpVisitor<R, P, E> v) throws E {
		return v.visit(params, this);
	}

	@Override
	public Unit type(AqlTyping G) {
		I.type(G);
		return Unit.unit;
	}

	@Override
	public boolean isVar() {
		return true;
	}

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		I.map(f);
	}

	@Override
	public Map<String, String> options() {
		return Collections.emptyMap();
	}

	public PragmaExpConsistent(InstExp<String, String, X, Y> i) {
		I = i;
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
		PragmaExpConsistent<?, ?> other = (PragmaExpConsistent<?, ?>) obj;
		if (I == null) {
			if (other.I != null)
				return false;
		} else if (!I.equals(other.I))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "assert_consistent " + I;
	}

	@Override
	public Pragma eval0(AqlEnv env, boolean isC) {
		return new Pragma() {

			@Override
			public void execute() {
				Instance<String, String, catdata.cql.exp.Sym, catdata.cql.exp.Fk, catdata.cql.exp.Att, String, String, X, Y> J = I
						.eval(env, isC);
				if (!J.algebra().hasFreeTypeAlgebra()) {
					throw new RuntimeException("Not necessarily consistent: type algebra is\n\n" + J.algebra().talg());
				}
			}

			@Override
			public String toString() {
				return "Consistent";
			}

		};
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return I.deps();
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
	}

}