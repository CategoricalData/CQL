package catdata.cql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.cql.AqlOptions;
import catdata.cql.Eq;
import catdata.cql.Instance;
import catdata.cql.Kind;
import catdata.cql.Query;
import catdata.cql.Schema;
import catdata.cql.Term;
import catdata.cql.AqlOptions.AqlOption;

public class QueryExpFromInst extends QueryExp {

	InstExp I;

	public QueryExpFromInst(InstExp i) {
		I = i;
	}

	@Override
	public Pair<SchExp, SchExp> type(AqlTyping G) {
		var x = I.type(G);
		return new Pair<SchExp, SchExp>(x, new SchExpUnit(x.type(G)));
	}

	@Override
	public <R, P, E extends Exception> R accept(P params, QueryExpVisitor<R, P, E> v) throws E {
		return v.visit(params, this);
	}

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		I.mapSubExps(f);
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {

	}

	@Override
	protected Map<String, String> options() {
		return Collections.emptyMap();
	}

	@Override
	protected Query<String, String, Sym, Fk, Att, String, Fk, Att> eval0(AqlEnv env, boolean isCompileTime) {
		Instance i = (Instance) I.eval(env, isCompileTime);

		Map<String, Triple<LinkedHashMap<String, Chc<String, String>>, Collection<Eq<String, String, Sym, Fk, Att, String, String>>, AqlOptions>> m
		 = new HashMap<>();
		LinkedHashMap<String, Chc<String, String>> gens = new LinkedHashMap<>();
		List<Eq<String, String, Sym, Fk, Att, String, String>> eqs = new LinkedList<>();
		
		i.gens().forEach((g,t)->{gens.put((String)g, Chc.inLeft((String)t));});
		i.sks().forEach((g,t)->{gens.put((String)g, Chc.inRight((String)t));});
		i.eqs((l,r)->{eqs.add(new Eq(Collections.emptyMap(),(Term)l,(Term)r));});
		
		m.put("", new Triple<>(gens, eqs, env.defaults));
		
		Query<String, String, Sym, Fk, Att, String, Fk, Att> ret = new Query<String, String, Sym, Fk, Att, String, Fk, Att>(
				Collections.emptyMap(), Collections.emptyMap(), m, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), i.schema(), Schema.unit(i.schema().typeSide),
				env.defaults);
		//
		return ret;
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
		QueryExpFromInst other = (QueryExpFromInst) obj;
		return Objects.equals(I, other.I);
	}

	@Override
	public String toString() {
		return "fromInstance " + I;
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return I.deps();
	}

}
