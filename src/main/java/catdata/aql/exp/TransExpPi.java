package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Chc;
import catdata.Pair;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Kind;
import catdata.aql.Term;
import catdata.aql.Transform;
import catdata.aql.fdm.Row;

public final class TransExpPi<Gen1, Sk1, GEn, Sk2, X1, Y1, X2, Y2> 
extends TransExp<Row<En, Chc<X1, Term<Ty, En, Sym, Fk, Att, Gen1, Sk1>>>, Y1, Row<En, Chc<X2, Term<Ty, En, Sym, Fk, Att, GEn, Sk2>>>, Y2, Row<En, Chc<X1, Term<Ty, En, Sym, Fk, Att, Gen1, Sk1>>>, Y1, Row<En, Chc<X2, Term<Ty, En, Sym, Fk, Att, GEn, Sk2>>>, Y2> { 
	
	public final MapExp F;
	public final TransExp<Gen1, Sk1, GEn, Sk2, X1, Y1, X2, Y2> t;

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		F.map(f);
		t.map(f);
	}

	
	public final Map<String, String> options1, options2;	 //TODO aql options weirdness
	
	@Override
	public Map<String, String> options() {
		return Collections.emptyMap();
	}
	
	public TransExpPi(MapExp F, TransExp<Gen1, Sk1, GEn, Sk2, X1, Y1, X2, Y2> t, Map<String, String> options1, Map<String, String> options2) {
		this.F = F;
		this.t = t;
		this.options1 = options1;
		this.options2 = options2;
	}
	
	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((F == null) ? 0 : F.hashCode());
		result = prime * result + ((options1 == null) ? 0 : options1.hashCode());
		result = prime * result + ((options2 == null) ? 0 : options2.hashCode());
		result = prime * result + ((t == null) ? 0 : t.hashCode());
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
		TransExpPi other = (TransExpPi) obj;
		if (F == null) {
			if (other.F != null)
				return false;
		} else if (!F.equals(other.F))
			return false;
		if (options1 == null) {
			if (other.options1 != null)
				return false;
		} else if (!options1.equals(other.options1))
			return false;
		if (options2 == null) {
			if (other.options2 != null)
				return false;
		} else if (!options2.equals(other.options2))
			return false;
		if (t == null) {
			if (other.t != null)
				return false;
		} else if (!t.equals(other.t))
			return false;
		return true;
	}


	@Override
	public Pair<InstExp<Row<En, Chc<X1, Term<Ty, En, Sym, Fk, Att, Gen1, Sk1>>>, Y1, Row<En, Chc<X1, Term<Ty, En, Sym, Fk, Att, Gen1, Sk1>>>, Y1>, InstExp<Row<En, Chc<X2, Term<Ty, En, Sym, Fk, Att, GEn, Sk2>>>, Y2, Row<En, Chc<X2, Term<Ty, En, Sym, Fk, Att, GEn, Sk2>>>, Y2>> type(AqlTyping G) {
		Pair<InstExp<Gen1, Sk1, X1, Y1>, InstExp<GEn, Sk2, X2, Y2>> x = t.type(G);
		if (!G.eq(x.first.type(G), F.type(G).first)) {
			throw new RuntimeException("In " + this + ", mapping domain is " + F.type(G).first + " but transform domain schema is " + x.first.type(G));
		}
		InstExpPi<Gen1, Sk1, X1, Y1> a = new InstExpPi<>(F, x.first, options1);
		InstExpPi<GEn, Sk2, X2, Y2> b = new InstExpPi<>(F, x.second, options2);
		return new Pair(a,b);
	} 

	@Override
	public synchronized Transform<Ty, En, Sym, Fk, Att, Row<En, Chc<X1, Term<Ty, En, Sym, Fk, Att, Gen1, Sk1>>>, Y1, Row<En, Chc<X2, Term<Ty, En, Sym, Fk, Att, GEn, Sk2>>>, Y2, Row<En, Chc<X1, Term<Ty, En, Sym, Fk, Att, Gen1, Sk1>>>, Y1, Row<En, Chc<X2, Term<Ty, En, Sym, Fk, Att, GEn, Sk2>>>, Y2> eval0(AqlEnv env, boolean isC) {
		QueryExp q = new QueryExpDeltaCoEval(F, Util.toList(options1));
		return new TransExpEval(q, t, Util.toList(options2)).eval0(env, isC);
	}

	@Override
	public String toString() {
		return "pi " + F + " " + t;
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return Util.union(F.deps(), t.deps());
	}
		
	public <R,P,E extends Exception> R accept(P params, TransExpVisitor<R, P, E> v) throws E {
		return v.visit(params, this);
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
		set.add(AqlOption.eval_max_temp_size);
		set.add(AqlOption.eval_reorder_joins);
		set.add(AqlOption.eval_max_plan_depth);
		set.add(AqlOption.eval_join_selectivity);
		set.add(AqlOption.eval_use_indices);
		set.add(AqlOption.eval_use_sql_above);
		set.add(AqlOption.eval_approx_sql_unsafe);
		set.add(AqlOption.eval_sql_persistent_indices);
		set.add(AqlOption.query_remove_redundancy);
		set.add(AqlOption.varchar_length);
		set.add(AqlOption.start_ids_at);
		set.add(AqlOption.require_consistency);
		set.add(AqlOption.allow_java_eqs_unsafe);
		set.addAll(AqlOptions.proverOptionNames());
	}

}