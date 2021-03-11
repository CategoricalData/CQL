package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Chc;
import catdata.Pair;
import catdata.Program;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Constraints;
import catdata.aql.DP;
import catdata.aql.ED;
import catdata.aql.Kind;
import catdata.aql.Schema;
import catdata.aql.SqlTypeSide;
import catdata.aql.Term;
import gnu.trove.map.hash.THashMap;

public final class SchExpFromMsCatalog extends SchExp {

	InstExp<String, Void, String, Void> I;
	Map<String, String> options;

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return I.deps();
	}

	public <R, P, E extends Exception> R accept(P param, SchExpVisitor<R, P, E> v) throws E {
		return v.visit(param, this);
	}

	@Override
	public <R, P, E extends Exception> SchExp coaccept(P params, SchExpCoVisitor<R, P, E> v, R r) throws E {
		return v.visitSchExpFromMsCatalog(params, r);
	}

	@Override
	public Map<String, String> options() {
		return options;
	}

	public SchExpFromMsCatalog(InstExp I, List<Pair<String, String>> ops) {
		this.I = I;
		this.options = Util.toMapSafely(ops);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((I == null) ? 0 : I.hashCode());
		result = prime * result + ((options == null) ? 0 : options.hashCode());
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
		SchExpFromMsCatalog other = (SchExpFromMsCatalog) obj;
		if (I == null) {
			if (other.I != null)
				return false;
		} else if (!I.equals(other.I))
			return false;
		if (options == null) {
			if (other.options != null)
				return false;
		} else if (!options.equals(other.options))
			return false;
		return true;
	}

	@Override
	public Schema<String, String, Sym, Fk, Att> eval0(AqlEnv env, boolean isC) {
		var J = new EdsExpFromMsCatalog(I, Util.toList(options));
		return J.eval(env, isC).schema;
	}

	@Override
	public String toString() {
		return "from_ms_catalog " + I;
	}

	@Override
	public SchExp resolve(AqlTyping G, Program<Exp<?>> prog) {
		return this;
	}

	@Override
	public TyExp type(AqlTyping G) {
		return I.type(G).type(G);
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
	}

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		I.mapSubExps(f);
	}
}